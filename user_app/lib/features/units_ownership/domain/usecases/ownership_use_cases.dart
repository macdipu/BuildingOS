import '../entities/ownership_models.dart';
import '../repositories/ownership_repository.dart';

class GetCurrentOwnership {
  const GetCurrentOwnership(this._repository);
  final OwnershipRepository _repository;
  Future<CurrentOwnership> call(String buildingId, String unitId) =>
      _repository.current(buildingId, unitId);
}

class GetOwnershipHistory {
  const GetOwnershipHistory(this._repository);
  final OwnershipRepository _repository;
  Future<OwnershipHistory> call(String buildingId, String unitId) =>
      _repository.history(buildingId, unitId);
}

class AssignUnitOwnership {
  const AssignUnitOwnership(this._repository);
  final OwnershipRepository _repository;
  Future<void> call(String buildingId, String unitId, OwnershipChange change) =>
      _repository.assign(buildingId, unitId, change);
}

class TransferUnitOwnership {
  const TransferUnitOwnership(this._repository);
  final OwnershipRepository _repository;
  Future<void> call(String buildingId, String unitId, OwnershipChange change) =>
      _repository.transfer(buildingId, unitId, change);
}

class ListTransferFiles {
  const ListTransferFiles(this._repository);
  final OwnershipRepository _repository;
  Future<List<TransferFile>> call(
    String buildingId,
    String unitId,
    String transferId,
  ) => _repository.listFiles(buildingId, unitId, transferId);
}

class UploadTransferFile {
  const UploadTransferFile(this._repository);
  final OwnershipRepository _repository;
  Future<void> call(
    String buildingId,
    String unitId,
    String transferId,
    String path,
    String reason,
  ) => _repository.upload(buildingId, unitId, transferId, path, reason);
}

class DownloadTransferFile {
  const DownloadTransferFile(this._repository);
  final OwnershipRepository _repository;
  Future<List<int>> call(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
  ) => _repository.download(buildingId, unitId, transferId, fileId);
}

class RemoveTransferFile {
  const RemoveTransferFile(this._repository);
  final OwnershipRepository _repository;
  Future<void> call(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
    String reason,
  ) => _repository.remove(buildingId, unitId, transferId, fileId, reason);
}
