import '../../domain/entities/property_models.dart';

class PropertyMapper {
  static BuildingSummary building(Map<String, dynamic> j) => BuildingSummary(
    id: j['id'],
    name: j['name'],
    address: j['address'],
    status: j['status'],
    roles: (j['roles'] as List).cast<String>(),
    ownedUnitCount: j['ownedUnitCount'],
  );
  static BuildingAccess access(Map<String, dynamic> j) => BuildingAccess(
    id: j['id'],
    name: j['name'],
    status: j['status'],
    readOnly: j['capabilities']['readOnly'] == true,
    manageUnits: j['capabilities']['manageUnits'] == true,
    manageMembers: j['capabilities']['manageMembers'] == true,
  );
  static OwnedProperty property(Map<String, dynamic> j) => OwnedProperty(
    buildingId: j['buildingId'],
    buildingName: j['buildingName'],
    unitId: j['unitId'],
    unitNumber: j['unitNumber'],
    floorLabel: j['floorLabel'],
    unitType: j['unitType'],
    areaSqft: j['areaSqft'],
    share: j['share'],
  );
  static OwnerInvitation invitation(Map<String, dynamic> j) => OwnerInvitation(
    id: j['id'],
    buildingId: j['buildingId'],
    label: j['buildingName'] ?? j['phone'],
    status: j['status'] ?? 'PENDING',
    expiresAt: j['expiresAt'],
  );
  static BuildingMember member(Map<String, dynamic> j) => BuildingMember(
    id: j['id'],
    userId: j['userId'],
    role: j['role'],
    status: j['status'],
    version: j['version'],
  );
  static BuildingFloor floor(Map<String, dynamic> j) => BuildingFloor(
    id: j['id'],
    label: j['label'],
    kind: j['kind'],
    displayOrder: j['displayOrder'],
    version: j['version'],
  );
  static PropertyUnit unit(Map<String, dynamic> j) => PropertyUnit(
    id: j['id'],
    buildingId: j['buildingId'],
    number: j['number'],
    floorId: j['floorId'],
    floorLabel: j['floorLabel'],
    type: j['type'],
    areaSqft: j['areaSqft'],
    version: j['version'],
    bedrooms: j['bedrooms'],
    defaultMaintenanceRate: j['defaultMaintenanceRate'],
    notes: j['notes'],
  );
  static UnitDraft draft(Map<String, dynamic> j) => UnitDraft(
    number: j['number'] ?? '',
    floorId: j['floorId'],
    floorLabel: j['floorLabel'],
    type: j['type'] ?? '',
    areaSqft: j['areaSqft'],
    bedrooms: j['bedrooms'],
    defaultMaintenanceRate: j['defaultMaintenanceRate'],
    notes: j['notes'],
  );
  static BatchPreview preview(Map<String, dynamic> j) => BatchPreview(
    j['valid'] == true,
    (j['rows'] as List)
        .map(
          (r) => BatchRow(
            r['rowNumber'],
            draft(r),
            (r['errors'] as List)
                .map((e) => BatchFieldError(e['field'], e['code']))
                .toList(),
          ),
        )
        .toList(),
  );
  static Map<String, dynamic> unitBody(UnitDraft d) => {
    'number': d.number,
    'floorId': d.floorId,
    'floorLabel': d.floorLabel,
    'type': d.type,
    'areaSqft': d.areaSqft,
    'bedrooms': d.bedrooms,
    'defaultMaintenanceRate': d.defaultMaintenanceRate,
    'notes': d.notes,
  };
  static Map<String, dynamic> floorBody(FloorDraft d) => {
    'label': d.label,
    'kind': d.kind,
    'displayOrder': d.displayOrder,
  };
  static Map<String, dynamic> generatorBody(GenerateUnits s) => {
    'floorIds': s.floorIds,
    'unitsPerFloor': s.unitsPerFloor,
    'numberPattern': s.numberPattern,
    'start': s.start,
    'type': s.type,
    'areaSqft': s.areaSqft,
    'templateFloorId': s.templateFloorId,
  };
}
