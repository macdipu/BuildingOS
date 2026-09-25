import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/startup/splash_controller.dart';
import 'package:customer/app/startup/start_destination.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:flutter_test/flutter_test.dart';

BuildingSummary _building(String id) => BuildingSummary(
      id: id,
      name: 'Tower $id',
      address: 'Road 1',
      status: 'ACTIVE',
      roles: const ['OWNER'],
      ownedUnitCount: 1,
    );

OwnerInvitation _invitation(String status) => OwnerInvitation(
      id: 'i-$status',
      buildingId: 'b1',
      label: 'Tower b1',
      status: status,
      expiresAt: '2026-10-01T00:00:00Z',
    );

SplashController _controller({
  bool session = true,
  List<BuildingSummary> buildings = const [],
  List<OwnerInvitation> invitations = const [],
  List<ApplicationStatus> applications = const [],
  Object? buildingsError,
  List<bool>? sessionSequence,
}) {
  final sequence = sessionSequence?.toList();
  return SplashController(
    restoreSession: () async => sequence != null && sequence.isNotEmpty ? sequence.removeAt(0) : session,
    listBuildings: () async {
      if (buildingsError != null) throw buildingsError;
      return buildings;
    },
    listInvitations: () async => invitations,
    listApplicationStatuses: () async => applications,
    activeBuilding: ActiveBuildingService(),
  );
}

void main() {
  group('BRD §39 start routing', () {
    test('no session goes to login', () async {
      expect((await _controller(session: false).resolve()).route, StartRoute.login);
    });

    test('exactly one building and nothing pending goes to dashboard with that building', () async {
      final result = await _controller(buildings: [_building('b1')]).resolve();
      expect(result.route, StartRoute.dashboard);
      expect(result.building?.id, 'b1');
    });

    test('several buildings go to My Buildings', () async {
      final result = await _controller(buildings: [_building('b1'), _building('b2')]).resolve();
      expect(result.route, StartRoute.myBuildings);
    });

    test('no building yet goes to My Buildings', () async {
      expect((await _controller().resolve()).route, StartRoute.myBuildings);
    });

    test('a pending invitation goes to My Buildings; a claimed one does not', () async {
      expect((await _controller(buildings: [_building('b1')], invitations: [_invitation('PENDING')]).resolve()).route,
          StartRoute.myBuildings);
      expect((await _controller(buildings: [_building('b1')], invitations: [_invitation('CLAIMED')]).resolve()).route,
          StartRoute.dashboard);
    });

    test('an in-progress building application goes to My Buildings; a closed one does not', () async {
      expect(
          (await _controller(buildings: [_building('b1')], applications: [ApplicationStatus.underReview]).resolve())
              .route,
          StartRoute.myBuildings);
      expect(
          (await _controller(buildings: [_building('b1')], applications: [ApplicationStatus.rejected]).resolve())
              .route,
          StartRoute.dashboard);
    });

    test('a load failure with the session revoked goes to login, otherwise My Buildings', () async {
      expect(
          (await _controller(buildingsError: Exception('401'), sessionSequence: [true, false]).resolve()).route,
          StartRoute.login);
      expect((await _controller(buildingsError: Exception('offline')).resolve()).route, StartRoute.myBuildings);
    });
  });
}
