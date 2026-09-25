import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/shell/app_shell.dart';
import 'package:customer/app/shell/app_shell_controller.dart';
import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/property_pages.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

BuildingSummary _building(List<String> roles) => BuildingSummary(
      id: 'b1',
      name: 'Rose Tower',
      address: 'Road 1',
      status: 'ACTIVE',
      roles: roles,
      ownedUnitCount: 0,
    );

Future<ActiveBuildingService> _pumpShell(WidgetTester tester, BuildingSummary? building) async {
  final active = Get.put(ActiveBuildingService());
  if (building != null) active.select(building);
  Get.put(AppShellController(active));
  await tester.pumpWidget(GetMaterialApp(
    theme: AppTheme.lightTheme,
    translations: AppTranslations(),
    locale: const Locale('en'),
    initialRoute: AppRoutes.appShell,
    getPages: [
      GetPage(name: AppRoutes.appShell, page: () => const AppShell()),
      GetPage(name: PropertyPages.home, page: () => const Scaffold(body: Text('my-buildings'))),
    ],
  ));
  await tester.pump();
  return active;
}

List<String> _navLabels(WidgetTester tester) => tester
    .widgetList<NavigationDestination>(find.byType(NavigationDestination))
    .map((d) => d.label)
    .toList();

void main() {
  tearDown(Get.reset);

  testWidgets('owner sees the owner nav; unbuilt tabs show the placeholder', (tester) async {
    await _pumpShell(tester, _building(['OWNER']));
    expect(_navLabels(tester), ['Dashboard', 'Properties', 'Payments', 'Community', 'More']);
    await tester.tap(find.byKey(const ValueKey('nav-payments')));
    await tester.pumpAndSettle();
    expect(find.text('Coming soon'), findsOneWidget);
  });

  testWidgets('admin + owner resolves to the manager nav and switching building re-resolves', (tester) async {
    final active = await _pumpShell(tester, _building(['OWNER', 'BUILDING_ADMIN']));
    expect(_navLabels(tester), ['Dashboard', 'Finance', 'Units', 'Work', 'More']);
    active.select(_building(['TENANT']));
    await tester.pump();
    expect(_navLabels(tester), ['Home', 'Payments', 'Community', 'Profile']);
  });

  testWidgets('no active building sends the user to My Buildings', (tester) async {
    await _pumpShell(tester, null);
    await tester.pumpAndSettle();
    expect(find.text('my-buildings'), findsOneWidget);
  });
}
