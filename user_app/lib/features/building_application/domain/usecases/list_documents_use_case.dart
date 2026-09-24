import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/application_document.dart';

class ListDocumentsUseCase
    extends UseCaseWithParams<List<ApplicationDocument>, String> {
  final BuildingApplicationRepository _repo;

  const ListDocumentsUseCase(this._repo);

  @override
  ResultFuture<List<ApplicationDocument>> call(String applicationId) =>
      _repo.listDocuments(applicationId);
}
