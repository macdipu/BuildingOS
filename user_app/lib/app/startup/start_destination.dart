import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';

enum StartRoute { login, dashboard, myBuildings }

class StartDestination {
  const StartDestination(this.route, [this.building]);

  final StartRoute route;

  /// Set only for [StartRoute.dashboard].
  final BuildingSummary? building;
}

const _inProgress = {
  ApplicationStatus.draft,
  ApplicationStatus.submitted,
  ApplicationStatus.underReview,
  ApplicationStatus.moreInformationRequired,
};

/// BRD §39: one building and no global action required -> Dashboard;
/// several buildings, pending invitations or building applications -> My Buildings.
StartDestination resolveStartDestination({
  required List<BuildingSummary> buildings,
  required int pendingInvitations,
  required List<ApplicationStatus> applications,
}) {
  final hasOpenApplication = applications.any(_inProgress.contains);
  if (buildings.length == 1 && pendingInvitations == 0 && !hasOpenApplication) {
    return StartDestination(StartRoute.dashboard, buildings.single);
  }
  return const StartDestination(StartRoute.myBuildings);
}
