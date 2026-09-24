import 'application_enums.dart';

/// Applicant-editable fields (AP-01). Every field may be empty while drafting.
class ApplicationDraft {
  final String? buildingName;
  final BuildingType? buildingType;
  final String? address;
  final String? area;
  final String? district;
  final String? postalCode;
  final int? totalFloors;
  final int? estimatedUnits;
  final ApplicantRelationship? applicantRelationship;
  final String? relationshipNote;
  final String? contactName;
  final String? contactPhone;
  final String? contactEmail;
  final ManagementType? managementType;
  final double? latitude;
  final double? longitude;

  const ApplicationDraft({
    this.buildingName,
    this.buildingType,
    this.address,
    this.area,
    this.district,
    this.postalCode,
    this.totalFloors,
    this.estimatedUnits,
    this.applicantRelationship,
    this.relationshipNote,
    this.contactName,
    this.contactPhone,
    this.contactEmail,
    this.managementType,
    this.latitude,
    this.longitude,
  });
}
