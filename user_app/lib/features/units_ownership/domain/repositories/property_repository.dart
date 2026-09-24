import '../entities/property_models.dart';

abstract class PropertyRepository {
  Future<List<BuildingSummary>> listBuildings();
  Future<List<OwnedProperty>> listProperties();
  Future<List<OwnerInvitation>> listInvitations();
  Future<void> claim(String id, String operationId);
  Future<BuildingAccess> getBuilding(String buildingId);
  Future<List<BuildingFloor>> listFloors(String buildingId);
  Future<BuildingFloor> createFloor(
    String buildingId,
    FloorDraft draft,
    String reason,
  );
  Future<BuildingFloor> updateFloor(
    String buildingId,
    String id,
    FloorDraft draft,
    int version,
    String reason,
  );
  Future<List<PropertyUnit>> listUnits(String buildingId);
  Future<PropertyUnit> getUnit(String buildingId, String id);
  Future<PropertyUnit> createUnit(
    String buildingId,
    UnitDraft draft,
    String reason,
  );
  Future<PropertyUnit> updateUnit(
    String buildingId,
    String id,
    UnitDraft draft,
    int version,
    String reason,
  );
  Future<List<BuildingMember>> listMembers(String buildingId);
  Future<List<OwnerInvitation>> listBuildingInvitations(String buildingId);
  Future<void> invite(String buildingId, String phone, String reason);
  Future<void> revokeInvitation(String buildingId, String id, String reason);
  Future<void> revokeMember(
    String buildingId,
    String id,
    int version,
    String reason,
  );
  Future<BatchPreview> previewRows(String buildingId, List<UnitDraft> rows);
  Future<BatchPreview> generate(String buildingId, GenerateUnits spec);
  Future<BatchPreview> importSheet(String buildingId, String filePath);
  Future<int> commitBatch(
    String buildingId,
    List<UnitDraft> rows,
    String operationId,
    String reason,
  );
}
