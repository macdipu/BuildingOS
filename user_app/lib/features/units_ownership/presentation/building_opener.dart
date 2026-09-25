import '../domain/entities/property_models.dart';

/// Implemented by the app shell so selecting a building makes it the active
/// building (BRD §42 -> Dashboard) without this feature depending on app state.
/// When none is registered, My Buildings falls back to the unit list.
abstract class BuildingOpener {
  Future<void> open(BuildingSummary building);
}
