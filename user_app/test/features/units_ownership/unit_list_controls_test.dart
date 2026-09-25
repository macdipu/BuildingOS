import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/widgets/unit_list_controls.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

void main() {
  Future<void> pump(WidgetTester tester, Widget child) => tester.pumpWidget(GetMaterialApp(
        theme: AppTheme.lightTheme,
        translations: AppTranslations(),
        locale: const Locale('en'),
        home: Scaffold(body: Padding(padding: const EdgeInsets.all(16), child: child)),
      ));

  testWidgets('search submits text and sort menu reports field + direction', (tester) async {
    String? searched;
    (UnitSortField, bool)? sorted;
    await pump(
      tester,
      UnitSearchBar(
        controller: TextEditingController(),
        filtersActive: false,
        onSearch: (v) => searched = v,
        onFilter: () {},
        onSort: (f, d) => sorted = (f, d),
        sort: UnitSortField.unitNumber,
        descending: false,
      ),
    );
    await tester.enterText(find.byKey(const ValueKey('unit-search')), '4A');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    expect(searched, '4A');

    await tester.tap(find.byKey(const ValueKey('unit-sort')));
    await tester.pumpAndSettle();
    // CheckedPopupMenuItem's label ignores pointers; the tap lands on its menu item.
    await tester.tap(find.text('Floor · Descending'), warnIfMissed: false);
    await tester.pumpAndSettle();
    expect(sorted, (UnitSortField.floor, true));
  });

  testWidgets('filter sheet returns chosen type and owner; owner filter hidden without owners', (tester) async {
    UnitListQuery? result;
    const floors = [BuildingFloor(id: 'f1', label: 'Ground', kind: 'GROUND', displayOrder: 0, version: 0)];
    const owners = [BuildingMember(id: 'm1', userId: 'owner-1', role: 'OWNER', status: 'ACTIVE', version: 0)];
    await pump(
      tester,
      Builder(
        builder: (context) => ElevatedButton(
          onPressed: () async => result = await showUnitFilterSheet(
            context,
            query: const UnitListQuery(),
            floors: floors,
            owners: owners,
          ),
          child: const Text('open'),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    expect(find.byKey(const ValueKey('filter-owner')), findsOneWidget);

    await tester.tap(find.byKey(const ValueKey('filter-type')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Flat').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('filter-owner')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('owner-1').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('filter-apply')));
    await tester.pumpAndSettle();
    expect(result?.type, 'FLAT');
    expect(result?.ownerUserId, 'owner-1');
    expect(result?.floorId, isNull);
  });
}
