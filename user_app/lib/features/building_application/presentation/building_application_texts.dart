import 'package:customer/res/strings/string_enum.dart';
import '../domain/entities/application_enums.dart';
import '../domain/entities/application_error.dart';
import '../domain/entities/draft_field.dart';

extension ApplicationStatusText on ApplicationStatus {
  String get label => switch (this) {
        ApplicationStatus.draft => TextEnum.statusDraft.tr,
        ApplicationStatus.submitted => TextEnum.statusSubmitted.tr,
        ApplicationStatus.underReview => TextEnum.statusUnderReview.tr,
        ApplicationStatus.moreInformationRequired =>
          TextEnum.statusMoreInformationRequired.tr,
        ApplicationStatus.rejected => TextEnum.statusRejected.tr,
        ApplicationStatus.approved => TextEnum.statusApproved.tr,
        ApplicationStatus.unknown => TextEnum.statusUnknown.tr,
      };
}

extension BuildingTypeText on BuildingType {
  String get label => switch (this) {
        BuildingType.residential => TextEnum.typeResidential.tr,
        BuildingType.commercial => TextEnum.typeCommercial.tr,
        BuildingType.mixed => TextEnum.typeMixed.tr,
      };
}

extension ApplicantRelationshipText on ApplicantRelationship {
  String get label => switch (this) {
        ApplicantRelationship.owner => TextEnum.relOwner.tr,
        ApplicantRelationship.committeeMember => TextEnum.relCommitteeMember.tr,
        ApplicantRelationship.propertyManager => TextEnum.relPropertyManager.tr,
        ApplicantRelationship.developer => TextEnum.relDeveloper.tr,
        ApplicantRelationship.other => TextEnum.relOther.tr,
      };
}

extension ManagementTypeText on ManagementType {
  String get label => switch (this) {
        ManagementType.selfManaged => TextEnum.mgmtSelfManaged.tr,
        ManagementType.ownersCommittee => TextEnum.mgmtOwnersCommittee.tr,
        ManagementType.managementCompany => TextEnum.mgmtManagementCompany.tr,
        ManagementType.developerManaged => TextEnum.mgmtDeveloperManaged.tr,
      };
}

extension DraftFieldErrorText on DraftFieldError {
  String get label => switch (this) {
        DraftFieldError.required => TextEnum.errRequired.tr,
        DraftFieldError.tooLong => TextEnum.errTooLong.tr,
        DraftFieldError.notPositive => TextEnum.errNotPositive.tr,
        DraftFieldError.invalidPhone => TextEnum.errInvalidPhone.tr,
        DraftFieldError.invalidEmail => TextEnum.errInvalidEmail.tr,
        DraftFieldError.invalidCoordinates => TextEnum.errInvalidCoordinates.tr,
      };
}

extension DraftFieldText on DraftField {
  String get label => switch (this) {
        DraftField.buildingName => TextEnum.fieldBuildingName.tr,
        DraftField.buildingType => TextEnum.fieldBuildingType.tr,
        DraftField.address => TextEnum.fieldAddress.tr,
        DraftField.area => TextEnum.fieldArea.tr,
        DraftField.district => TextEnum.fieldDistrict.tr,
        DraftField.postalCode => TextEnum.fieldPostalCode.tr,
        DraftField.totalFloors => TextEnum.fieldTotalFloors.tr,
        DraftField.estimatedUnits => TextEnum.fieldEstimatedUnits.tr,
        DraftField.applicantRelationship => TextEnum.fieldRelationship.tr,
        DraftField.relationshipNote => TextEnum.fieldRelationshipNote.tr,
        DraftField.contactName => TextEnum.fieldContactName.tr,
        DraftField.contactPhone => TextEnum.fieldContactPhone.tr,
        DraftField.contactEmail => TextEnum.fieldContactEmail.tr,
        DraftField.coordinates => TextEnum.fieldLatitude.tr,
      };
}

/// Localized text for a failure code from the repository (API `ApiError.code` or client code).
String applicationErrorText(String code) => switch (code) {
      ApplicationErrorCode.incomplete => TextEnum.errApplicationIncomplete.tr,
      ApplicationErrorCode.notEditable => TextEnum.errNotEditable.tr,
      ApplicationErrorCode.invalidTransition =>
        TextEnum.errInvalidTransition.tr,
      ApplicationErrorCode.notFound => TextEnum.errApplicationNotFound.tr,
      ApplicationErrorCode.documentNotFound => TextEnum.errDocumentNotFound.tr,
      ApplicationErrorCode.documentTooLarge => TextEnum.errDocumentTooLarge.tr,
      ApplicationErrorCode.unsupportedDocument =>
        TextEnum.errUnsupportedDocument.tr,
      ApplicationErrorCode.documentLimit => TextEnum.errDocumentLimit.tr,
      ApplicationErrorCode.invalidRequest => TextEnum.errInvalidRequest.tr,
      ApplicationErrorCode.unavailable => TextEnum.errUnavailable.tr,
      ApplicationErrorCode.connection => TextEnum.errConnection.tr,
      _ => TextEnum.errUnexpected.tr,
    };
