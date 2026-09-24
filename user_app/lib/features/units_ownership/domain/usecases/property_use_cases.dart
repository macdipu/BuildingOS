import '../entities/property_models.dart';
import '../repositories/property_repository.dart';

class ListMyBuildings {
  const ListMyBuildings(this._repository);
  final PropertyRepository _repository;
  Future<List<BuildingSummary>> call() => _repository.listBuildings();
}

class ListMyProperties {
  const ListMyProperties(this._repository);
  final PropertyRepository _repository;
  Future<List<OwnedProperty>> call() => _repository.listProperties();
}

class ListMyInvitations {
  const ListMyInvitations(this._repository);
  final PropertyRepository _repository;
  Future<List<OwnerInvitation>> call() => _repository.listInvitations();
}

class ClaimOwnerInvitation {
  const ClaimOwnerInvitation(this._repository);
  final PropertyRepository _repository;
  Future<void> call(String id, String operationId) =>
      _repository.claim(id, operationId);
}

class GetBuildingAccess {
  const GetBuildingAccess(this._repository);
  final PropertyRepository _repository;
  Future<BuildingAccess> call(String buildingId) =>
      _repository.getBuilding(buildingId);
}

class ListBuildingFloors {
  const ListBuildingFloors(this._repository);
  final PropertyRepository _repository;
  Future<List<BuildingFloor>> call(String buildingId) =>
      _repository.listFloors(buildingId);
}

class CreateBuildingFloor {
  const CreateBuildingFloor(this._repository);
  final PropertyRepository _repository;
  Future<BuildingFloor> call(
    String buildingId,
    FloorDraft draft,
    String reason,
  ) => _repository.createFloor(buildingId, draft, reason);
}

class UpdateBuildingFloor {
  const UpdateBuildingFloor(this._repository);
  final PropertyRepository _repository;
  Future<BuildingFloor> call(
    String buildingId,
    String id,
    FloorDraft draft,
    int version,
    String reason,
  ) => _repository.updateFloor(buildingId, id, draft, version, reason);
}

class ListBuildingUnits {
  const ListBuildingUnits(this._repository);
  final PropertyRepository _repository;
  Future<List<PropertyUnit>> call(String buildingId) =>
      _repository.listUnits(buildingId);
}

class GetPropertyUnit {
  const GetPropertyUnit(this._repository);
  final PropertyRepository _repository;
  Future<PropertyUnit> call(String buildingId, String id) =>
      _repository.getUnit(buildingId, id);
}

class CreatePropertyUnit {
  const CreatePropertyUnit(this._repository);
  final PropertyRepository _repository;
  Future<PropertyUnit> call(
    String buildingId,
    UnitDraft draft,
    String reason,
  ) => _repository.createUnit(buildingId, draft, reason);
}

class UpdatePropertyUnit {
  const UpdatePropertyUnit(this._repository);
  final PropertyRepository _repository;
  Future<PropertyUnit> call(
    String buildingId,
    String id,
    UnitDraft draft,
    int version,
    String reason,
  ) => _repository.updateUnit(buildingId, id, draft, version, reason);
}

class ListBuildingMembers {
  const ListBuildingMembers(this._repository);
  final PropertyRepository _repository;
  Future<List<BuildingMember>> call(String buildingId) =>
      _repository.listMembers(buildingId);
}

class ListBuildingInvitations {
  const ListBuildingInvitations(this._repository);
  final PropertyRepository _repository;
  Future<List<OwnerInvitation>> call(String buildingId) =>
      _repository.listBuildingInvitations(buildingId);
}

class InviteBuildingOwner {
  const InviteBuildingOwner(this._repository);
  final PropertyRepository _repository;
  Future<void> call(String buildingId, String phone, String reason) =>
      _repository.invite(buildingId, phone, reason);
}

class RevokeOwnerInvitation {
  const RevokeOwnerInvitation(this._repository);
  final PropertyRepository _repository;
  Future<void> call(String buildingId, String id, String reason) =>
      _repository.revokeInvitation(buildingId, id, reason);
}

class RevokeOwnerMembership {
  const RevokeOwnerMembership(this._repository);
  final PropertyRepository _repository;
  Future<void> call(String buildingId, String id, int version, String reason) =>
      _repository.revokeMember(buildingId, id, version, reason);
}

class PreviewUnitRows {
  const PreviewUnitRows(this._repository);
  final PropertyRepository _repository;
  Future<BatchPreview> call(String buildingId, List<UnitDraft> rows) =>
      _repository.previewRows(buildingId, rows);
}

class GenerateUnitPreview {
  const GenerateUnitPreview(this._repository);
  final PropertyRepository _repository;
  Future<BatchPreview> call(String buildingId, GenerateUnits spec) =>
      _repository.generate(buildingId, spec);
}

class ImportUnitPreview {
  const ImportUnitPreview(this._repository);
  final PropertyRepository _repository;
  Future<BatchPreview> call(String buildingId, String filePath) =>
      _repository.importSheet(buildingId, filePath);
}

class CommitUnitBatch {
  const CommitUnitBatch(this._repository);
  final PropertyRepository _repository;
  Future<int> call(
    String buildingId,
    List<UnitDraft> rows,
    String operationId,
    String reason,
  ) => _repository.commitBatch(buildingId, rows, operationId, reason);
}
