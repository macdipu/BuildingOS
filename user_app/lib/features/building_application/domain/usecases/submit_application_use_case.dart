import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/building_application.dart';

class SubmitApplicationUseCase
    extends UseCaseWithParams<BuildingApplication, String> {
  final BuildingApplicationRepository _repo;

  const SubmitApplicationUseCase(this._repo);

  @override
  ResultFuture<BuildingApplication> call(String id) => _repo.submit(id);
}
