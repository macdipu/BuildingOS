import '../entities/ownership_models.dart';

abstract class OwnershipRepository {
  Future<CurrentOwnership> current(String buildingId, String unitId);
  Future<OwnershipHistory> history(String buildingId, String unitId);
  Future<void> assign(String buildingId, String unitId, OwnershipChange change);
  Future<void> transfer(
    String buildingId,
    String unitId,
    OwnershipChange change,
  );
  Future<List<TransferFile>> listFiles(
    String buildingId,
    String unitId,
    String transferId,
  );
  Future<void> upload(
    String buildingId,
    String unitId,
    String transferId,
    String path,
    String reason,
  );
  Future<List<int>> download(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
  );
  Future<void> remove(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
    String reason,
  );
}
