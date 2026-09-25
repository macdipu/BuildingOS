import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/widgets/building_card.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

BuildingSummary _building(String status) => BuildingSummary(
      id: 'b1',
      name: 'Green Valley',
      address: 'Road 12, Banani',
      status: status,
      roles: const ['BUILDING_ADMIN', 'OWNER'],
      ownedUnitCount: 2,
    );

Future<void> _pump(WidgetTester tester, BuildingSummary b, VoidCallback onOpen) =>
    tester.pumpWidget(GetMaterialApp(
      theme: AppTheme.lightTheme,
      translations: AppTranslations(),
      locale: const Locale('en'),
      home: Scaffold(body: SingleChildScrollView(child: BuildingCard(building: b, onOpen: onOpen))),
    ));

void main() {
  testWidgets('active building shows name, address, owned units and opens; no warning', (tester) async {
    var opened = false;
    await _pump(tester, _building('ACTIVE'), () => opened = true);
    expect(find.text('Green Valley'), findsOneWidget);
    expect(find.text('Road 12, Banani'), findsOneWidget);
    expect(find.textContaining('2'), findsWidgets);
    expect(find.byKey(const ValueKey('building-access-warning')), findsNothing);
    await tester.tap(find.text('Open Building'));
    expect(opened, isTrue);
  });

  testWidgets('suspended building shows the read-only access warning', (tester) async {
    await _pump(tester, _building('SUSPENDED'), () {});
    expect(find.byKey(const ValueKey('building-access-warning')), findsOneWidget);
    expect(find.textContaining('read-only'), findsOneWidget);
  });
}
