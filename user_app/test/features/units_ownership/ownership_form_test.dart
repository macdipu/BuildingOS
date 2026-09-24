import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:customer/features/units_ownership/domain/entities/ownership_models.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/domain/repositories/ownership_repository.dart';
import 'package:customer/features/units_ownership/domain/repositories/property_repository.dart';
import 'package:customer/features/units_ownership/domain/usecases/ownership_use_cases.dart';
import 'package:customer/features/units_ownership/domain/usecases/property_use_cases.dart';
import 'package:customer/features/units_ownership/presentation/pages/ownership_form_page.dart';

class Members implements PropertyRepository {
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
  Future<List<BuildingMember>> listMembers(String id) async => const [
    BuildingMember(
      id: 'm1',
      userId: 'Alice',
      role: 'OWNER',
      status: 'ACTIVE',
      version: 0,
    ),
    BuildingMember(
      id: 'm2',
      userId: 'Bob',
      role: 'OWNER',
      status: 'ACTIVE',
      version: 0,
    ),
  ];
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class Ownership implements OwnershipRepository {
  final calls = <OwnershipChange>[];
  @override
  Future<CurrentOwnership> current(String b, String u) async =>
      const CurrentOwnership(7, 60, [
        OwnershipPeriod(
          id: 'p',
          ownerUserId: 'Alice',
          share: 60,
          startAt: '2026-09-24',
        ),
      ]);
  @override
  Future<void> transfer(String b, String u, OwnershipChange change) async {
    calls.add(change);
    throw const PropertyException('CONNECTION');
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  tearDown(Get.reset);
  testWidgets(
    'transfer requires review and retries exactly the same confirmed payload',
    (tester) async {
      final ownership = Ownership();
      final members = Members();
      Get.put(GetBuildingAccess(members));
      Get.put(ListBuildingMembers(members));
      Get.put(GetCurrentOwnership(ownership));
      Get.put(TransferUnitOwnership(ownership));
      await tester.pumpWidget(
        GetMaterialApp(
          translations: AppTranslations(),
          locale: const Locale('en'),
          home: const OwnershipFormPage(
            buildingId: 'b',
            unitId: 'u',
            unitNumber: '4A',
            transfer: true,
          ),
        ),
      );
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).first);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Alice (60%)').last);
      await tester.pumpAndSettle();
      await tester.tap(find.byType(DropdownButtonFormField<String>).last);
      await tester.pumpAndSettle();
      await tester.tap(find.text('Bob').last);
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextFormField).at(0), '20');
      await tester.enterText(find.byType(TextFormField).at(1), 'Sale');
      await tester.ensureVisible(find.text(TextEnum.uoConfirm.en));
      await tester.tap(find.text(TextEnum.uoConfirm.en));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 350));
      expect(ownership.calls, isEmpty);
      expect(find.textContaining('From: Alice'), findsOneWidget);
      await tester.tap(
        find.widgetWithText(FilledButton, TextEnum.uoConfirm.en).last,
      );
      await tester.pumpAndSettle();
      expect(ownership.calls, hasLength(1));
      expect(ownership.calls.single.share, 20);
      expect(ownership.calls.single.expectedVersion, 7);
      await tester.ensureVisible(find.text(TextEnum.uoRetry.en));
      await tester.tap(find.text(TextEnum.uoRetry.en));
      await tester.pumpAndSettle();
      expect(ownership.calls, hasLength(2));
      expect(identical(ownership.calls[0], ownership.calls[1]), true);
    },
  );
}
