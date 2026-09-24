import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/application_document.dart';

class UploadDocumentParams {
  final String applicationId;
  final String filePath;

  const UploadDocumentParams(this.applicationId, this.filePath);
}

class UploadDocumentUseCase
    extends UseCaseWithParams<ApplicationDocument, UploadDocumentParams> {
  final BuildingApplicationRepository _repo;

  const UploadDocumentUseCase(this._repo);

  @override
  ResultFuture<ApplicationDocument> call(UploadDocumentParams params) =>
      _repo.uploadDocument(params.applicationId, params.filePath);
}
