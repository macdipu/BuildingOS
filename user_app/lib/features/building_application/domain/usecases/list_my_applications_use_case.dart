import 'package:customer/core/usecases/usecase.dart';
import '../repositories/building_application_repository.dart';
import '../entities/building_application.dart';

class ListMyApplicationsUseCase
    extends UseCaseWithoutParams<List<BuildingApplication>> {
  final BuildingApplicationRepository _repo;

  const ListMyApplicationsUseCase(this._repo);

  @override
  ResultFuture<List<BuildingApplication>> call() => _repo.listMine();
}
