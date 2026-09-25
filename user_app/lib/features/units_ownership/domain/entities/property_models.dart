/// Domain values contain no transport or framework dependencies.
class BuildingSummary {
  const BuildingSummary({
    required this.id,
    required this.name,
    required this.address,
    required this.status,
    required this.roles,
    required this.ownedUnitCount,
  });
  final String id, name, address, status;
  final List<String> roles;
  final int ownedUnitCount;
}

class BuildingAccess {
  const BuildingAccess({
    required this.id,
    required this.name,
    required this.status,
    required this.readOnly,
    required this.manageUnits,
    required this.manageMembers,
  });
  final String id, name, status;
  final bool readOnly, manageUnits, manageMembers;
  bool get canEditUnits => !readOnly && manageUnits;
  bool get canEditMembers => !readOnly && manageMembers;
}

class OwnedProperty {
  const OwnedProperty({
    required this.buildingId,
    required this.buildingName,
    required this.unitId,
    required this.unitNumber,
    required this.floorLabel,
    required this.unitType,
    required this.areaSqft,
    required this.share,
  });
  final String buildingId,
      buildingName,
      unitId,
      unitNumber,
      floorLabel,
      unitType;
  final num areaSqft, share;
}

class OwnerInvitation {
  const OwnerInvitation({
    required this.id,
    required this.buildingId,
    required this.label,
    required this.status,
    required this.expiresAt,
  });
  final String id, buildingId, label, status, expiresAt;
}

class BuildingMember {
  const BuildingMember({
    required this.id,
    required this.userId,
    required this.role,
    required this.status,
    required this.version,
  });
  final String id, userId, role, status;
  final int version;
}

class BuildingFloor {
  const BuildingFloor({
    required this.id,
    required this.label,
    required this.kind,
    required this.displayOrder,
    required this.version,
  });
  final String id, label, kind;
  final int displayOrder, version;
}

class PropertyUnit {
  const PropertyUnit({
    required this.id,
    required this.buildingId,
    required this.number,
    required this.floorId,
    required this.floorLabel,
    required this.type,
    required this.areaSqft,
    required this.version,
    this.bedrooms,
    this.defaultMaintenanceRate,
    this.notes,
  });
  final String id, buildingId, number, floorId, floorLabel, type;
  final num areaSqft;
  final int version;
  final int? bedrooms;
  final num? defaultMaintenanceRate;
  final String? notes;
  UnitDraft get draft => UnitDraft(
    number: number,
    floorId: floorId,
    type: type,
    areaSqft: areaSqft,
    bedrooms: bedrooms,
    defaultMaintenanceRate: defaultMaintenanceRate,
    notes: notes,
  );
}

class UnitDraft {
  const UnitDraft({
    required this.number,
    required this.floorId,
    required this.type,
    required this.areaSqft,
    this.floorLabel,
    this.bedrooms,
    this.defaultMaintenanceRate,
    this.notes,
  });
  final String number, type;
  final String? floorId, floorLabel, notes;
  final num? areaSqft, defaultMaintenanceRate;
  final int? bedrooms;
}

class FloorDraft {
  const FloorDraft(this.label, this.kind, this.displayOrder);
  final String label, kind;
  final int displayOrder;
}

class BatchRow {
  const BatchRow(this.rowNumber, this.draft, this.errors);
  final int rowNumber;
  final UnitDraft draft;
  final List<BatchFieldError> errors;
}

class BatchFieldError {
  const BatchFieldError(this.field, this.code);
  final String field, code;
}

class BatchPreview {
  const BatchPreview(this.valid, this.rows);
  final bool valid;
  final List<BatchRow> rows;
}

class GenerateUnits {
  const GenerateUnits({
    required this.floorIds,
    required this.unitsPerFloor,
    required this.numberPattern,
    required this.start,
    required this.type,
    required this.areaSqft,
    this.templateFloorId,
  });
  final List<String> floorIds;
  final int unitsPerFloor, start;
  final String numberPattern, type;
  final num areaSqft;
  final String? templateFloorId;
}

class PropertyException implements Exception {
  const PropertyException(this.code, {this.preview});
  final String code;
  final BatchPreview? preview;
}

enum UnitSortField { unitNumber, floor, type }

/// BRD §48 unit list filter/search/sort. Null filters = any.
class UnitListQuery {
  const UnitListQuery({
    this.floorId,
    this.type,
    this.ownerUserId,
    this.search,
    this.sort = UnitSortField.unitNumber,
    this.descending = false,
  });
  final String? floorId, type, ownerUserId, search;
  final UnitSortField sort;
  final bool descending;

  bool get hasFilters => floorId != null || type != null || ownerUserId != null;

  UnitListQuery copyWith({
    String? Function()? floorId,
    String? Function()? type,
    String? Function()? ownerUserId,
    String? Function()? search,
    UnitSortField? sort,
    bool? descending,
  }) => UnitListQuery(
    floorId: floorId == null ? this.floorId : floorId(),
    type: type == null ? this.type : type(),
    ownerUserId: ownerUserId == null ? this.ownerUserId : ownerUserId(),
    search: search == null ? this.search : search(),
    sort: sort ?? this.sort,
    descending: descending ?? this.descending,
  );

  @override
  bool operator ==(Object other) =>
      other is UnitListQuery &&
      other.floorId == floorId &&
      other.type == type &&
      other.ownerUserId == ownerUserId &&
      other.search == search &&
      other.sort == sort &&
      other.descending == descending;

  @override
  int get hashCode => Object.hash(floorId, type, ownerUserId, search, sort, descending);
}
