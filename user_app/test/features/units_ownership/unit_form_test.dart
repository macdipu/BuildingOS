import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/domain/repositories/property_repository.dart';
import 'package:customer/features/units_ownership/domain/usecases/property_use_cases.dart';
import 'package:customer/features/units_ownership/presentation/pages/unit_form_page.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

class _Repo implements PropertyRepository {
  final created = <(UnitDraft, String)>[];

  @override
  Future<BuildingAccess> getBuilding(String id) async => const BuildingAccess(
        id: 'b',
        name: 'Building',
        status: 'ACTIVE',
        readOnly: false,
        manageUnits: true,
        manageMembers: true,
      );

  @override
  Future<PropertyUnit> createUnit(String buildingId, UnitDraft draft, String reason) async {
    created.add((draft, reason));
    return PropertyUnit(
      id: 'u${created.length}',
      buildingId: buildingId,
      number: draft.number,
      floorId: draft.floorId ?? '',
      floorLabel: 'Ground',
      type: draft.type,
      areaSqft: draft.areaSqft ?? 0,
      version: 0,
    );
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

TextField _field(WidgetTester tester, String label) => tester.widget<TextField>(
      find.descendant(of: find.widgetWithText(TextFormField, label), matching: find.byType(TextField)),
    );

void main() {
  late _Repo repo;
  setUp(() {
    repo = _Repo();
    Get.put(GetBuildingAccess(repo));
    Get.put(CreatePropertyUnit(repo));
  });
  tearDown(Get.reset);

  testWidgets('Save & Add Another saves, clears per-unit fields and keeps floor, type and rate', (tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(GetMaterialApp(
      translations: AppTranslations(),
      locale: const Locale('en'),
      home: const UnitFormPage(
        buildingId: 'b',
        floors: [BuildingFloor(id: 'f1', label: 'Ground', kind: 'GROUND', displayOrder: 0, version: 0)],
        draft: UnitDraft(number: '', floorId: 'f1', type: 'FLAT', areaSqft: null, defaultMaintenanceRate: 2500),
      ),
    ));
    await tester.enterText(find.widgetWithText(TextFormField, 'Unit number'), '4A');
    await tester.enterText(find.widgetWithText(TextFormField, 'Area (sq ft)'), '1200');
    await tester.enterText(find.widgetWithText(TextFormField, 'Reason'), 'Onboarding');
    await tester.tap(find.byKey(const ValueKey('save-add-another')));
    await tester.pumpAndSettle();

    expect(repo.created.single.$1.number, '4A');
    expect(repo.created.single.$2, 'Onboarding');
    expect(find.byType(UnitFormPage), findsOneWidget);
    expect(_field(tester, 'Unit number').controller?.text, isEmpty);
    expect(_field(tester, 'Area (sq ft)').controller?.text, isEmpty);
    expect(_field(tester, 'Reason').controller?.text, 'Onboarding');
    expect(find.text('Unit 4A saved'), findsOneWidget);
    await tester.pump(const Duration(seconds: 5));
    await tester.pumpAndSettle();

    await tester.enterText(find.widgetWithText(TextFormField, 'Unit number'), '4B');
    await tester.enterText(find.widgetWithText(TextFormField, 'Area (sq ft)'), '1100');
    await tester.tap(find.byKey(const ValueKey('save-add-another')));
    await tester.pumpAndSettle();
    expect(repo.created.last.$1.number, '4B');
    expect(repo.created.last.$1.floorId, 'f1');
    expect(repo.created.last.$1.type, 'FLAT');
    expect(repo.created.last.$1.defaultMaintenanceRate, 2500);
  });

  testWidgets('editing an existing unit has no Save & Add Another', (tester) async {
    await tester.pumpWidget(GetMaterialApp(
      translations: AppTranslations(),
      locale: const Locale('en'),
      home: const UnitFormPage(
        buildingId: 'b',
        floors: [BuildingFloor(id: 'f1', label: 'Ground', kind: 'GROUND', displayOrder: 0, version: 0)],
        unit: PropertyUnit(
          id: 'u1',
          buildingId: 'b',
          number: '4A',
          floorId: 'f1',
          floorLabel: 'Ground',
          type: 'FLAT',
          areaSqft: 1200,
          version: 1,
        ),
      ),
    ));
    expect(find.byKey(const ValueKey('save-add-another')), findsNothing);
  });
}
