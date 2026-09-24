import 'package:customer/features/building_application/domain/entities/application_draft.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/building_application/domain/entities/draft_field.dart';
import 'package:customer/features/building_application/domain/entities/draft_validator.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const validator = DraftValidator();
  const complete = ApplicationDraft(
    buildingName: 'Rose Garden',
    buildingType: BuildingType.residential,
    address: 'House 12, Road 5',
    area: 'Dhanmondi',
    district: 'Dhaka',
    estimatedUnits: 36,
    applicantRelationship: ApplicantRelationship.owner,
    contactName: 'Rahim',
    contactPhone: '+8801712345678',
  );

  test(
      'an empty draft saves but lists every field required by the server for submission',
      () {
    expect(validator.validate(const ApplicationDraft(), forSubmit: false),
        isEmpty);
    expect(validator.missingForSubmission(const ApplicationDraft()), [
      DraftField.buildingName,
      DraftField.buildingType,
      DraftField.address,
      DraftField.area,
      DraftField.district,
      DraftField.estimatedUnits,
      DraftField.applicantRelationship,
      DraftField.contactName,
      DraftField.contactPhone,
    ]);
    expect(validator.validate(complete, forSubmit: true), isEmpty);
  });

  test('OTHER relationship needs a note', () {
    const other =
        ApplicationDraft(applicantRelationship: ApplicantRelationship.other);
    expect(validator.missingForSubmission(other),
        contains(DraftField.relationshipNote));
  });

  test('format rules match the server', () {
    final errors = validator.validate(
      const ApplicationDraft(
        contactPhone: '01212345678',
        contactEmail: 'nope',
        totalFloors: 0,
        estimatedUnits: -1,
        latitude: 23.7,
      ),
      forSubmit: false,
    );
    expect(errors, {
      DraftField.contactPhone: DraftFieldError.invalidPhone,
      DraftField.contactEmail: DraftFieldError.invalidEmail,
      DraftField.totalFloors: DraftFieldError.notPositive,
      DraftField.estimatedUnits: DraftFieldError.notPositive,
      DraftField.coordinates: DraftFieldError.invalidCoordinates,
    });
    for (final phone in ['01712345678', '8801712345678', '+8801912345678']) {
      expect(
          validator.validate(ApplicationDraft(contactPhone: phone),
              forSubmit: false),
          isEmpty,
          reason: phone);
    }
    expect(
        validator.validate(ApplicationDraft(buildingName: 'x' * 201),
            forSubmit: false),
        {DraftField.buildingName: DraftFieldError.tooLong});
  });
}
