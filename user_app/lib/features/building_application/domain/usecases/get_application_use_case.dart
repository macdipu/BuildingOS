import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/building_application.dart';

class GetApplicationUseCase
    extends UseCaseWithParams<BuildingApplication, String> {
  final BuildingApplicationRepository _repo;

  const GetApplicationUseCase(this._repo);

  @override
  ResultFuture<BuildingApplication> call(String id) => _repo.get(id);
}
