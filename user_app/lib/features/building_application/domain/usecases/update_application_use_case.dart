import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/application_draft.dart';
import '../entities/building_application.dart';

class UpdateApplicationParams {
  final String id;
  final ApplicationDraft draft;

  const UpdateApplicationParams(this.id, this.draft);
}

class UpdateApplicationUseCase
    extends UseCaseWithParams<BuildingApplication, UpdateApplicationParams> {
  final BuildingApplicationRepository _repo;

  const UpdateApplicationUseCase(this._repo);

  @override
  ResultFuture<BuildingApplication> call(UpdateApplicationParams params) =>
      _repo.update(params.id, params.draft);
}
