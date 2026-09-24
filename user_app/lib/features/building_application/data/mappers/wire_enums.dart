import '../../domain/entities/application_enums.dart';

/// API enums are UPPER_SNAKE_CASE; Dart enums are lowerCamelCase.
String toWire(Enum value) => value.name
    .replaceAllMapped(RegExp('[A-Z]'), (m) => '_${m[0]}')
    .toUpperCase();

T? fromWire<T extends Enum>(List<T> values, String? wire) {
  if (wire == null) return null;
  for (final value in values) {
    if (toWire(value) == wire) return value;
  }
  return null;
}

ApplicationStatus statusFromWire(String wire) =>
    fromWire(ApplicationStatus.values, wire) ?? ApplicationStatus.unknown;
