import 'dart:async';

import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/startup/start_destination.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/property_pages.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

typedef SessionRestorer = Future<bool> Function();

class SplashController extends GetxController {
  SplashController({
    required SessionRestorer restoreSession,
    required Future<List<BuildingSummary>> Function() listBuildings,
    required Future<List<OwnerInvitation>> Function() listInvitations,
    required Future<List<ApplicationStatus>> Function() listApplicationStatuses,
    required ActiveBuildingService activeBuilding,
  })  : _restoreSession = restoreSession,
        _listBuildings = listBuildings,
        _listInvitations = listInvitations,
        _listApplicationStatuses = listApplicationStatuses,
        _activeBuilding = activeBuilding;

  final SessionRestorer _restoreSession;
  final Future<List<BuildingSummary>> Function() _listBuildings;
  final Future<List<OwnerInvitation>> Function() _listInvitations;
  final Future<List<ApplicationStatus>> Function() _listApplicationStatuses;
  final ActiveBuildingService _activeBuilding;

  @override
  void onReady() {
    super.onReady();
    unawaited(_start());
  }

  Future<void> _start() async {
    final destination = await resolve();
    switch (destination.route) {
      case StartRoute.login:
        unawaited(Get.offAllNamed(AppRoutes.login));
      case StartRoute.dashboard:
        final building = destination.building;
        if (building != null) _activeBuilding.select(building);
        unawaited(Get.offAllNamed(AppRoutes.appShell));
      case StartRoute.myBuildings:
        unawaited(Get.offAllNamed(PropertyPages.home));
    }
  }

  Future<StartDestination> resolve() async {
    if (!await _restoreSession()) return const StartDestination(StartRoute.login);
    try {
      final results = await Future.wait<Object>([
        _listBuildings(),
        _listInvitations(),
        _listApplicationStatuses(),
      ]);
      final invitations = results[1] as List<OwnerInvitation>;
      return resolveStartDestination(
        buildings: results[0] as List<BuildingSummary>,
        pendingInvitations: invitations.where((i) => i.status == 'PENDING').length,
        applications: results[2] as List<ApplicationStatus>,
      );
    } catch (error) {
      // A rejected session is cleared by ApiClient (login follows via SessionExpiryNotifier);
      // anything else lands on My Buildings, which owns its own retry/error state.
      debugPrint('Splash start resolution failed: $error');
      if (!await _restoreSession()) return const StartDestination(StartRoute.login);
      return const StartDestination(StartRoute.myBuildings);
    }
  }
}
