import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:get/get.dart';

/// The building the shell is scoped to (BRD §39 "active building"). Set by the
/// splash when the user has exactly one building, or when they pick one.
class ActiveBuildingService extends GetxService {
  final Rxn<BuildingSummary> building = Rxn<BuildingSummary>();

  void select(BuildingSummary value) => building.value = value;

  void clear() => building.value = null;
}
