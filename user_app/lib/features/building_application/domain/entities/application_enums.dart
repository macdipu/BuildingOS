enum ApplicationStatus {
  draft,
  submitted,
  underReview,
  moreInformationRequired,
  rejected,
  approved,
  unknown;

  bool get isEditable =>
      this == ApplicationStatus.draft ||
      this == ApplicationStatus.moreInformationRequired;
}

enum BuildingType { residential, commercial, mixed }

enum ApplicantRelationship {
  owner,
  committeeMember,
  propertyManager,
  developer,
  other,
}

enum ManagementType {
  selfManaged,
  ownersCommittee,
  managementCompany,
  developerManaged,
}
