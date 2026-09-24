import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/application_draft.dart';
import '../entities/building_application.dart';

class CreateApplicationUseCase
    extends UseCaseWithParams<BuildingApplication, ApplicationDraft> {
  final BuildingApplicationRepository _repo;

  const CreateApplicationUseCase(this._repo);

  @override
  ResultFuture<BuildingApplication> call(ApplicationDraft draft) =>
      _repo.create(draft);
}
