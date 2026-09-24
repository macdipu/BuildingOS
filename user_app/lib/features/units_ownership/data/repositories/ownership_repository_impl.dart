import '../../domain/entities/ownership_models.dart';
import '../../domain/repositories/ownership_repository.dart';
import '../datasources/property_remote_datasource.dart';
import '../mappers/ownership_mapper.dart';

class OwnershipRepositoryImpl implements OwnershipRepository {
  const OwnershipRepositoryImpl(this.remote);
  final PropertyRemoteDataSource remote;
  String _unit(String b, String u) =>
      'buildings/${Uri.encodeComponent(b)}/units/${Uri.encodeComponent(u)}';
  String _files(String b, String u, String t) =>
      '${_unit(b, u)}/ownership-transfers/${Uri.encodeComponent(t)}/documents';
  @override
  Future<CurrentOwnership> current(String buildingId, String unitId) async =>
      OwnershipMapper.current(
        await remote.request('${_unit(buildingId, unitId)}/ownerships'),
      );
  @override
  Future<OwnershipHistory> history(String buildingId, String unitId) async =>
      OwnershipMapper.history(
        await remote.request('${_unit(buildingId, unitId)}/ownership-history'),
      );
  @override
  Future<void> assign(
    String buildingId,
    String unitId,
    OwnershipChange change,
  ) async {
    await remote.request(
      '${_unit(buildingId, unitId)}/ownerships',
      method: 'POST',
      body: OwnershipMapper.change(change, transfer: false),
    );
  }

  @override
  Future<void> transfer(
    String buildingId,
    String unitId,
    OwnershipChange change,
  ) async {
    await remote.request(
      '${_unit(buildingId, unitId)}/ownership-transfers',
      method: 'POST',
      body: OwnershipMapper.change(change, transfer: true),
    );
  }

  @override
  Future<List<TransferFile>> listFiles(
    String buildingId,
    String unitId,
    String transferId,
  ) async =>
      (await remote.request(_files(buildingId, unitId, transferId)) as List)
          .map((j) => OwnershipMapper.file(j))
          .toList();
  @override
  Future<void> upload(
    String buildingId,
    String unitId,
    String transferId,
    String path,
    String reason,
  ) async {
    await remote.uploadDocument(
      _files(buildingId, unitId, transferId),
      path,
      reason,
    );
  }

  @override
  Future<List<int>> download(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
  ) => remote.download(
    '${_files(buildingId, unitId, transferId)}/${Uri.encodeComponent(fileId)}',
  );
  @override
  Future<void> remove(
    String buildingId,
    String unitId,
    String transferId,
    String fileId,
    String reason,
  ) => remote.removeDocument(
    '${_files(buildingId, unitId, transferId)}/${Uri.encodeComponent(fileId)}',
    reason,
  );
}
