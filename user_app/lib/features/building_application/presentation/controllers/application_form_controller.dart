import 'package:customer/core/controllers/base_controller.dart';
import 'package:customer/core/widgets/snackbar/custom_snackbar.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/widgets.dart';
import 'package:get/get.dart';
import '../../domain/entities/application_draft.dart';
import '../../domain/entities/application_enums.dart';
import '../../domain/entities/building_application.dart';
import '../../domain/entities/draft_field.dart';
import '../../domain/entities/draft_validator.dart';
import '../../domain/usecases/create_application_use_case.dart';
import '../../domain/usecases/update_application_use_case.dart';
import '../building_application_texts.dart';

/// Create (no argument) or edit (a [BuildingApplication] argument) a draft. Saving never submits.
class ApplicationFormController extends BaseController {
  ApplicationFormController({
    required CreateApplicationUseCase create,
    required UpdateApplicationUseCase update,
    DraftValidator validator = const DraftValidator(),
  })  : _create = create,
        _update = update,
        _validator = validator;

  final CreateApplicationUseCase _create;
  final UpdateApplicationUseCase _update;
  final DraftValidator _validator;

  BuildingApplication? _existing;
  final texts = {
    for (final field in DraftField.values)
      if (field != DraftField.buildingType &&
          field != DraftField.applicantRelationship)
        field: TextEditingController(),
  };
  final longitude = TextEditingController();
  final buildingType = Rxn<BuildingType>();
  final relationship = Rxn<ApplicantRelationship>();
  final managementType = Rxn<ManagementType>();
  final errors = <DraftField, DraftFieldError>{}.obs;

  bool get isEditing => _existing != null;

  @override
  void onInit() {
    super.onInit();
    final argument = Get.arguments;
    if (argument is BuildingApplication) prefill(argument);
  }

  void prefill(BuildingApplication application) {
    _existing = application;
    final d = application.details;
    final values = {
      DraftField.buildingName: d.buildingName,
      DraftField.address: d.address,
      DraftField.area: d.area,
      DraftField.district: d.district,
      DraftField.postalCode: d.postalCode,
      DraftField.totalFloors: d.totalFloors?.toString(),
      DraftField.estimatedUnits: d.estimatedUnits?.toString(),
      DraftField.relationshipNote: d.relationshipNote,
      DraftField.contactName: d.contactName,
      DraftField.contactPhone: d.contactPhone,
      DraftField.contactEmail: d.contactEmail,
      DraftField.coordinates: d.latitude?.toString(),
    };
    values.forEach((field, value) => texts[field]!.text = value ?? '');
    longitude.text = d.longitude?.toString() ?? '';
    buildingType.value = d.buildingType;
    relationship.value = d.applicantRelationship;
    managementType.value = d.managementType;
  }

  ApplicationDraft draft() {
    String text(DraftField f) => texts[f]!.text;
    return ApplicationDraft(
      buildingName: text(DraftField.buildingName),
      buildingType: buildingType.value,
      address: text(DraftField.address),
      area: text(DraftField.area),
      district: text(DraftField.district),
      postalCode: text(DraftField.postalCode),
      totalFloors: int.tryParse(text(DraftField.totalFloors).trim()),
      estimatedUnits: int.tryParse(text(DraftField.estimatedUnits).trim()),
      applicantRelationship: relationship.value,
      relationshipNote: relationship.value == ApplicantRelationship.other
          ? text(DraftField.relationshipNote)
          : null,
      contactName: text(DraftField.contactName),
      contactPhone: text(DraftField.contactPhone),
      contactEmail: text(DraftField.contactEmail),
      managementType: managementType.value,
      latitude: double.tryParse(text(DraftField.coordinates).trim()),
      longitude: double.tryParse(longitude.text.trim()),
    );
  }

  String? errorFor(DraftField field) => errors[field]?.label;

  Future<void> save() async {
    if (isLoading.value) return;
    final current = draft();
    errors.assignAll(_validator.validate(current, forSubmit: false));
    if (errors.isNotEmpty) return;
    final existing = _existing;
    await doAction<BuildingApplication>(
      action: () => existing == null
          ? _create(current)
          : _update(UpdateApplicationParams(existing.id, current)),
      onSuccess: (saved) {
        CustomSnackbar.success(TextEnum.draftSaved.tr);
        Get.back(result: saved);
      },
      onError: (code) => CustomSnackbar.error(applicationErrorText(code ?? '')),
    );
  }

  @override
  void onClose() {
    for (final controller in texts.values) {
      controller.dispose();
    }
    longitude.dispose();
    super.onClose();
  }
}
