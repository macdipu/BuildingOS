import '../../domain/entities/property_models.dart';
import '../../domain/repositories/property_repository.dart';
import '../datasources/property_remote_datasource.dart';
import '../mappers/property_mapper.dart';

class PropertyRepositoryImpl implements PropertyRepository {
  const PropertyRepositoryImpl(this.remote);
  final PropertyRemoteDataSource remote;
  String _building(String id) => 'buildings/${Uri.encodeComponent(id)}';
  String _id(String id) => Uri.encodeComponent(id);
  Future<List<T>> _list<T>(
    String path,
    T Function(Map<String, dynamic>) map,
  ) async => (await remote.request(path) as List)
      .map((j) => map(j as Map<String, dynamic>))
      .toList();
  @override
  Future<List<BuildingSummary>> listBuildings() =>
      _list('me/buildings', PropertyMapper.building);
  @override
  Future<List<OwnedProperty>> listProperties() =>
      _list('me/properties', PropertyMapper.property);
  @override
  Future<List<OwnerInvitation>> listInvitations() =>
      _list('me/building-invitations', PropertyMapper.invitation);
  @override
  Future<void> claim(String id, String operationId) async {
    await remote.request(
      'me/building-invitations/${_id(id)}/claim',
      method: 'POST',
      body: {'operationId': operationId},
    );
  }

  @override
  Future<BuildingAccess> getBuilding(String buildingId) async =>
      PropertyMapper.access(await remote.request(_building(buildingId)));
  @override
  Future<List<BuildingFloor>> listFloors(String buildingId) =>
      _list('${_building(buildingId)}/floors', PropertyMapper.floor);
  @override
  Future<BuildingFloor> createFloor(
    String buildingId,
    FloorDraft draft,
    String reason,
  ) async => PropertyMapper.floor(
    await remote.request(
      '${_building(buildingId)}/floors',
      method: 'POST',
      body: {...PropertyMapper.floorBody(draft), 'reason': reason},
    ),
  );
  @override
  Future<BuildingFloor> updateFloor(
    String buildingId,
    String id,
    FloorDraft draft,
    int version,
    String reason,
  ) async => PropertyMapper.floor(
    await remote.request(
      '${_building(buildingId)}/floors/${_id(id)}',
      method: 'PUT',
      body: {
        ...PropertyMapper.floorBody(draft),
        'expectedVersion': version,
        'reason': reason,
      },
    ),
  );
  @override
  Future<List<PropertyUnit>> listUnits(String buildingId) =>
      _list('${_building(buildingId)}/units', PropertyMapper.unit);
  @override
  Future<PropertyUnit> getUnit(String buildingId, String id) async =>
      PropertyMapper.unit(
        await remote.request('${_building(buildingId)}/units/${_id(id)}'),
      );
  @override
  Future<PropertyUnit> createUnit(
    String buildingId,
    UnitDraft draft,
    String reason,
  ) async => PropertyMapper.unit(
    await remote.request(
      '${_building(buildingId)}/units',
      method: 'POST',
      body: {...PropertyMapper.unitBody(draft), 'reason': reason},
    ),
  );
  @override
  Future<PropertyUnit> updateUnit(
    String buildingId,
    String id,
    UnitDraft draft,
    int version,
    String reason,
  ) async => PropertyMapper.unit(
    await remote.request(
      '${_building(buildingId)}/units/${_id(id)}',
      method: 'PUT',
      body: {
        ...PropertyMapper.unitBody(draft),
        'expectedVersion': version,
        'reason': reason,
      },
    ),
  );
  @override
  Future<List<BuildingMember>> listMembers(String buildingId) =>
      _list('${_building(buildingId)}/members', PropertyMapper.member);
  @override
  Future<List<OwnerInvitation>> listBuildingInvitations(String buildingId) =>
      _list('${_building(buildingId)}/invitations', PropertyMapper.invitation);
  @override
  Future<void> invite(String buildingId, String phone, String reason) async {
    await remote.request(
      '${_building(buildingId)}/invitations',
      method: 'POST',
      body: {'phone': phone, 'role': 'OWNER', 'reason': reason},
    );
  }

  @override
  Future<void> revokeInvitation(
    String buildingId,
    String id,
    String reason,
  ) async {
    await remote.request(
      '${_building(buildingId)}/invitations/${_id(id)}/revoke',
      method: 'POST',
      body: {'reason': reason},
    );
  }

  @override
  Future<void> revokeMember(
    String buildingId,
    String id,
    int version,
    String reason,
  ) async {
    await remote.request(
      '${_building(buildingId)}/members/${_id(id)}/revoke',
      method: 'POST',
      body: {'expectedVersion': version, 'reason': reason},
    );
  }

  @override
  Future<BatchPreview> previewRows(
    String buildingId,
    List<UnitDraft> rows,
  ) async => PropertyMapper.preview(
    await remote.request(
      '${_building(buildingId)}/unit-batches/preview',
      method: 'POST',
      body: {'rows': rows.map(PropertyMapper.unitBody).toList()},
    ),
  );
  @override
  Future<BatchPreview> generate(String buildingId, GenerateUnits spec) async =>
      PropertyMapper.preview(
        await remote.request(
          '${_building(buildingId)}/unit-batches/preview',
          method: 'POST',
          body: {'generate': PropertyMapper.generatorBody(spec)},
        ),
      );
  @override
  Future<BatchPreview> importSheet(String buildingId, String filePath) async =>
      PropertyMapper.preview(
        await remote.uploadSheet(
          '${_building(buildingId)}/unit-batches/preview',
          filePath,
        ),
      );
  @override
  Future<int> commitBatch(
    String buildingId,
    List<UnitDraft> rows,
    String operationId,
    String reason,
  ) async {
    final result = await remote.request(
      '${_building(buildingId)}/unit-batches/commit',
      method: 'POST',
      body: {
        'rows': rows.map(PropertyMapper.unitBody).toList(),
        'operationId': operationId,
        'reason': reason,
      },
    );
    return result['unitCount'] as int;
  }
}
