import '../../domain/entities/application_document.dart';
import '../../domain/entities/application_draft.dart';
import '../../domain/entities/application_enums.dart';
import '../../domain/entities/building_application.dart';
import '../models/application_document_model.dart';
import '../models/building_application_model.dart';
import 'wire_enums.dart';

extension BuildingApplicationModelMapper on BuildingApplicationModel {
  BuildingApplication toEntity() => BuildingApplication(
        id: id,
        applicationNumber: applicationNumber,
        status: statusFromWire(status),
        details: ApplicationDraft(
          buildingName: text('buildingName'),
          buildingType: fromWire(BuildingType.values, text('buildingType')),
          address: text('address'),
          area: text('area'),
          district: text('district'),
          postalCode: text('postalCode'),
          totalFloors: integer('totalFloors'),
          estimatedUnits: integer('estimatedUnits'),
          applicantRelationship: fromWire(
              ApplicantRelationship.values, text('applicantRelationship')),
          relationshipNote: text('relationshipNote'),
          contactName: text('contactName'),
          contactPhone: text('contactPhone'),
          contactEmail: text('contactEmail'),
          managementType:
              fromWire(ManagementType.values, text('managementType')),
          latitude: decimal('latitude'),
          longitude: decimal('longitude'),
        ),
        missingFields: missingFields,
        rejectionReason: text('rejectionReason'),
        infoRequestMessage: text('infoRequestMessage'),
        submittedAt: DateTime.tryParse(text('submittedAt') ?? ''),
        createdAt: DateTime.tryParse(text('createdAt') ?? ''),
      );
}

extension ApplicationDocumentModelMapper on ApplicationDocumentModel {
  ApplicationDocument toEntity() => ApplicationDocument(
        id: id,
        fileName: fileName,
        contentType: contentType,
        sizeBytes: sizeBytes,
        uploadedAt: DateTime.tryParse(uploadedAt ?? ''),
      );
}

extension ApplicationDraftJson on ApplicationDraft {
  /// Blank strings are sent as null so the server treats them as not yet filled.
  Map<String, dynamic> toJson() {
    String? clean(String? v) => v == null || v.trim().isEmpty ? null : v.trim();
    return {
      'buildingName': clean(buildingName),
      'buildingType': buildingType == null ? null : toWire(buildingType!),
      'address': clean(address),
      'area': clean(area),
      'district': clean(district),
      'postalCode': clean(postalCode),
      'totalFloors': totalFloors,
      'estimatedUnits': estimatedUnits,
      'applicantRelationship':
          applicantRelationship == null ? null : toWire(applicantRelationship!),
      'relationshipNote': clean(relationshipNote),
      'contactName': clean(contactName),
      'contactPhone': clean(contactPhone),
      'contactEmail': clean(contactEmail),
      'managementType': managementType == null ? null : toWire(managementType!),
      'latitude': latitude,
      'longitude': longitude,
    };
  }
}
