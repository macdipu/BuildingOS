import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/startup/splash_binding.dart';
import 'package:customer/app/startup/splash_controller.dart';
import 'package:customer/app/startup/splash_screen.dart';
import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/property_pages.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

Widget _stub(String label) => Scaffold(body: Text(label));

Future<void> _pumpSplash(WidgetTester tester, SplashController controller) async {
  await tester.pumpWidget(GetMaterialApp(
    theme: AppTheme.lightTheme,
    translations: AppTranslations(),
    locale: const Locale('en'),
    initialRoute: AppRoutes.splash,
    getPages: [
      // Registered through the production hook, not a test-only Get.put, so a
      // registration the screen never builds would leave the app on splash (DEF-01).
      GetPage(
        name: AppRoutes.splash,
        page: () => const SplashScreen(),
        binding: BindingsBuilder(() => SplashBinding.register(controller)),
      ),
      GetPage(name: AppRoutes.login, page: () => _stub('login')),
      GetPage(name: AppRoutes.appShell, page: () => _stub('shell')),
      GetPage(name: PropertyPages.home, page: () => _stub('my-buildings')),
    ],
  ));
}

SplashController _controller({required bool session, List<BuildingSummary> buildings = const []}) =>
    SplashController(
      restoreSession: () async => session,
      listBuildings: () async => buildings,
      listInvitations: () async => const [],
      listApplicationStatuses: () async => const [],
      activeBuilding: Get.put(ActiveBuildingService()),
    );

void main() {
  tearDown(Get.reset);

  testWidgets('shows brand and loading indicator, then routes a signed-out user to login', (tester) async {
    await _pumpSplash(tester, _controller(session: false));
    expect(find.text('BuildingOS'), findsOneWidget);
    expect(find.byType(LinearProgressIndicator), findsOneWidget);
    await tester.pumpAndSettle();
    expect(find.text('login'), findsOneWidget);
  });

  testWidgets('a single-building user lands on the shell with that building active', (tester) async {
    const building = BuildingSummary(
      id: 'b1',
      name: 'Tower',
      address: 'Road 1',
      status: 'ACTIVE',
      roles: ['BUILDING_ADMIN'],
      ownedUnitCount: 0,
    );
    await _pumpSplash(tester, _controller(session: true, buildings: const [building]));
    await tester.pumpAndSettle();
    expect(find.text('shell'), findsOneWidget);
    expect(Get.find<ActiveBuildingService>().building.value?.id, 'b1');
  });
}
