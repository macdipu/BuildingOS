import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';

class RemoveDocumentParams {
  final String applicationId;
  final String documentId;

  const RemoveDocumentParams(this.applicationId, this.documentId);
}

class RemoveDocumentUseCase
    extends UseCaseWithParams<void, RemoveDocumentParams> {
  final BuildingApplicationRepository _repo;

  const RemoveDocumentUseCase(this._repo);

  @override
  ResultVoid call(RemoveDocumentParams params) =>
      _repo.removeDocument(params.applicationId, params.documentId);
}
