import 'package:customer/res/strings/string_enum.dart';
import '../domain/entities/property_models.dart';

String propertyError(Object? error) {
  final code = error is PropertyException ? error.code : 'UNAVAILABLE';
  if (code.contains('DENIED') ||
      code.contains('NOT_FOUND') ||
      code == 'FORBIDDEN')
    return TextEnum.uoDenied.tr;
  if (code.contains('INVITATION') &&
      (code.contains('EXPIRED') ||
          code.contains('REVOKED') ||
          code.contains('STATE')))
    return TextEnum.uoInviteExpired.tr;
  if (code.contains('TAKEN') ||
      code.contains('CONFLICT') ||
      code.contains('VERSION') ||
      code.contains('STALE') ||
      code.contains('SHARE'))
    return TextEnum.uoConflict.tr;
  if (code.contains('TOO_LARGE') || code.contains('LIMIT'))
    return TextEnum.uoFileTooLarge.tr;
  if (code.contains('UNSUPPORTED') ||
      code.contains('FORMULA') ||
      code.contains('SHEET'))
    return TextEnum.uoFileUnsupported.tr;
  if (code.contains('INVALID') ||
      code.contains('REQUIRED') ||
      code.contains('PRECISION'))
    return TextEnum.errInvalidRequest.tr;
  if (code == 'CONNECTION') return TextEnum.errConnection.tr;
  if (code == 'BUILDING_READ_ONLY') return TextEnum.uoReadOnly.tr;
  return TextEnum.errUnavailable.tr;
}

String propertyLabel(String value) => switch (value) {
  'FLAT' => TextEnum.uoFlat.tr,
  'PARKING' => TextEnum.uoParking.tr,
  'STORAGE' => TextEnum.uoStorage.tr,
  'COMMERCIAL' => TextEnum.uoCommercial.tr,
  'COMMON' => TextEnum.uoCommon.tr,
  'OTHER' => TextEnum.uoOther.tr,
  'BASEMENT' => TextEnum.uoBasement.tr,
  'GROUND' => TextEnum.uoGround.tr,
  'REGULAR' => TextEnum.uoRegular.tr,
  'ROOF' => TextEnum.uoRoof.tr,
  'ONBOARDING' => TextEnum.uoOnboarding.tr,
  'ACTIVE' => TextEnum.uoActive.tr,
  'SUSPENDED' => TextEnum.uoSuspended.tr,
  'OWNER' => TextEnum.uoOwner.tr,
  'BUILDING_ADMIN' => TextEnum.uoAdmin.tr,
  'PENDING' => TextEnum.uoPending.tr,
  'CLAIMED' => TextEnum.uoClaimed.tr,
  'REVOKED' => TextEnum.uoRevoked.tr,
  'EXPIRED' => TextEnum.uoExpired.tr,
  _ => TextEnum.statusUnknown.tr,
};
