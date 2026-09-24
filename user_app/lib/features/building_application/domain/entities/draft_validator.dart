import 'application_draft.dart';
import 'application_enums.dart';
import 'draft_field.dart';

/// Mirrors building-service's ApplicationDetails rules so the form flags problems before a round trip.
/// Format rules apply on every save; completeness only on submit. The server stays authoritative.
class DraftValidator {
  const DraftValidator();

  static final _phone = RegExp(r'^(?:0|\+?880)1[3-9]\d{8}$');
  static final _email = RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$');
  static const _maxLengths = {
    DraftField.buildingName: 200,
    DraftField.address: 500,
    DraftField.area: 120,
    DraftField.district: 120,
    DraftField.postalCode: 16,
    DraftField.relationshipNote: 200,
    DraftField.contactName: 200,
    DraftField.contactEmail: 254,
  };

  Map<DraftField, DraftFieldError> validate(ApplicationDraft draft,
      {required bool forSubmit}) {
    final errors = <DraftField, DraftFieldError>{};
    final texts = {
      DraftField.buildingName: draft.buildingName,
      DraftField.address: draft.address,
      DraftField.area: draft.area,
      DraftField.district: draft.district,
      DraftField.postalCode: draft.postalCode,
      DraftField.relationshipNote: draft.relationshipNote,
      DraftField.contactName: draft.contactName,
      DraftField.contactEmail: draft.contactEmail,
    };
    texts.forEach((field, value) {
      final max = _maxLengths[field]!;
      if (value != null && value.trim().length > max)
        errors[field] = DraftFieldError.tooLong;
    });
    if ((draft.totalFloors ?? 1) <= 0)
      errors[DraftField.totalFloors] = DraftFieldError.notPositive;
    if ((draft.estimatedUnits ?? 1) <= 0)
      errors[DraftField.estimatedUnits] = DraftFieldError.notPositive;
    final phone = draft.contactPhone?.trim();
    if (phone != null && phone.isNotEmpty && !_phone.hasMatch(phone)) {
      errors[DraftField.contactPhone] = DraftFieldError.invalidPhone;
    }
    final email = draft.contactEmail?.trim();
    if (email != null && email.isNotEmpty && !_email.hasMatch(email)) {
      errors[DraftField.contactEmail] = DraftFieldError.invalidEmail;
    }
    if (!_coordinatesValid(draft.latitude, draft.longitude)) {
      errors[DraftField.coordinates] = DraftFieldError.invalidCoordinates;
    }
    if (forSubmit) {
      for (final field in missingForSubmission(draft)) {
        errors.putIfAbsent(field, () => DraftFieldError.required);
      }
    }
    return errors;
  }

  List<DraftField> missingForSubmission(ApplicationDraft draft) {
    bool blank(String? v) => v == null || v.trim().isEmpty;
    return [
      if (blank(draft.buildingName)) DraftField.buildingName,
      if (draft.buildingType == null) DraftField.buildingType,
      if (blank(draft.address)) DraftField.address,
      if (blank(draft.area)) DraftField.area,
      if (blank(draft.district)) DraftField.district,
      if (draft.estimatedUnits == null) DraftField.estimatedUnits,
      if (draft.applicantRelationship == null) DraftField.applicantRelationship,
      if (draft.applicantRelationship == ApplicantRelationship.other &&
          blank(draft.relationshipNote))
        DraftField.relationshipNote,
      if (blank(draft.contactName)) DraftField.contactName,
      if (blank(draft.contactPhone)) DraftField.contactPhone,
    ];
  }

  static bool _coordinatesValid(double? latitude, double? longitude) {
    if (latitude == null && longitude == null) return true;
    if (latitude == null || longitude == null) return false;
    return latitude >= -90 &&
        latitude <= 90 &&
        longitude >= -180 &&
        longitude <= 180;
  }
}
