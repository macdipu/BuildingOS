/// Form fields, named as the API reports them in `missingFields`.
enum DraftField {
  buildingName,
  buildingType,
  address,
  area,
  district,
  postalCode,
  totalFloors,
  estimatedUnits,
  applicantRelationship,
  relationshipNote,
  contactName,
  contactPhone,
  contactEmail,
  coordinates,
}

enum DraftFieldError {
  required,
  tooLong,
  notPositive,
  invalidPhone,
  invalidEmail,
  invalidCoordinates
}
