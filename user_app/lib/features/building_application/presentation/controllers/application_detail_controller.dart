import 'dart:async';
import 'dart:io';

import 'package:customer/core/controllers/base_controller.dart';
import 'package:customer/core/services/utilities/media_service.dart';
import 'package:customer/core/widgets/snackbar/custom_snackbar.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:file_picker/file_picker.dart';
import 'package:get/get.dart';
import 'package:image_picker/image_picker.dart';
import '../../domain/entities/application_document.dart';
import '../../domain/entities/application_error.dart';
import '../../domain/entities/building_application.dart';
import '../../domain/usecases/get_application_use_case.dart';
import '../../domain/usecases/list_documents_use_case.dart';
import '../../domain/usecases/remove_document_use_case.dart';
import '../../domain/usecases/submit_application_use_case.dart';
import '../../domain/usecases/upload_document_use_case.dart';
import '../building_application_pages.dart';
import '../building_application_texts.dart';

enum DocumentSource { camera, gallery, file }

/// One application: status, reviewer message, documents; edit/submit while editable.
class ApplicationDetailController extends BaseController {
  ApplicationDetailController({
    required GetApplicationUseCase getApplication,
    required ListDocumentsUseCase listDocuments,
    required UploadDocumentUseCase uploadDocument,
    required RemoveDocumentUseCase removeDocument,
    required SubmitApplicationUseCase submitApplication,
    MediaService? media,
  })  : _get = getApplication,
        _listDocuments = listDocuments,
        _upload = uploadDocument,
        _remove = removeDocument,
        _submit = submitApplication,
        _media = media ?? MediaService.instance;

  final GetApplicationUseCase _get;
  final ListDocumentsUseCase _listDocuments;
  final UploadDocumentUseCase _upload;
  final RemoveDocumentUseCase _remove;
  final SubmitApplicationUseCase _submit;
  final MediaService _media;

  String applicationId = '';
  final application = Rxn<BuildingApplication>();
  final documents = <ApplicationDocument>[].obs;
  final isUploading = false.obs;

  @override
  void onInit() {
    super.onInit();
    final argument = Get.arguments;
    if (argument is! String || argument.isEmpty) {
      errorMessage.value = applicationErrorText(ApplicationErrorCode.notFound);
      return;
    }
    applicationId = argument;
    unawaited(load());
  }

  Future<void> load() async {
    errorMessage.value = null;
    await doAction<BuildingApplication>(
      action: () => _get(applicationId),
      onSuccess: (loaded) async {
        application.value = loaded;
        await _reloadDocuments();
      },
      onError: (code) => errorMessage.value = applicationErrorText(code ?? ''),
    );
  }

  Future<void> edit() async {
    final current = application.value;
    if (current == null) return;
    final saved =
        await Get.toNamed(BuildingApplicationPages.form, arguments: current);
    if (saved is BuildingApplication) application.value = saved;
  }

  Future<void> submit() async {
    await doAction<BuildingApplication>(
      action: () => _submit(applicationId),
      onSuccess: (submitted) {
        application.value = submitted;
        CustomSnackbar.success(TextEnum.applicationSubmitted.tr);
      },
      onError: (code) async {
        CustomSnackbar.error(applicationErrorText(code ?? ''));
        await load();
      },
    );
  }

  Future<void> addDocument(DocumentSource source) async {
    final picked = await _pick(source);
    if (picked == null) return;
    isUploading.value = true;
    final result =
        await _upload(UploadDocumentParams(applicationId, picked.path));
    isUploading.value = false;
    await result.fold(
      (failure) async =>
          CustomSnackbar.error(applicationErrorText(failure.message)),
      (_) async {
        CustomSnackbar.success(TextEnum.documentAdded.tr);
        await _reloadDocuments();
      },
    );
  }

  Future<void> removeDocument(ApplicationDocument document) async {
    final result =
        await _remove(RemoveDocumentParams(applicationId, document.id));
    await result.fold(
      (failure) async =>
          CustomSnackbar.error(applicationErrorText(failure.message)),
      (_) async => _reloadDocuments(),
    );
  }

  Future<File?> _pick(DocumentSource source) async {
    final result = switch (source) {
      DocumentSource.camera =>
        await _media.pickImage(source: ImageSource.camera, imageQuality: 85),
      DocumentSource.gallery =>
        await _media.pickImage(source: ImageSource.gallery, imageQuality: 85),
      DocumentSource.file => (await _media.pickFiles(
              type: FileType.custom, allowedExtensions: const ['pdf']))
          .map((files) => files.isEmpty ? null : files.first),
    };
    return result.fold((failure) {
      CustomSnackbar.error(failure.message);
      return null;
    }, (file) => file);
  }

  Future<void> _reloadDocuments() async {
    final result = await _listDocuments(applicationId);
    result.fold(
      (failure) => CustomSnackbar.error(applicationErrorText(failure.message)),
      documents.assignAll,
    );
  }
}
