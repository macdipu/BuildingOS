import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/widgets/property_widgets.dart';

Widget host(Widget child, {String locale = 'en'}) => GetMaterialApp(
  locale: Locale(locale),
  translations: AppTranslations(),
  home: Scaffold(body: child),
);
void main() {
  tearDown(() => Get.reset());
  test(
    'decimal validation rejects excess precision and accepts optional zero metadata',
    () {
      expect(decimalText('1.001'), isNotNull);
      expect(decimalText('NaN'), isNotNull);
      expect(decimalText('0'), isNotNull);
      expect(decimalText('1e2'), isNotNull);
      expect(decimalText('12.25'), isNull);
      expect(decimalText('0', zero: true), isNull);
      expect(decimalText('', optional: true), isNull);
    },
  );
  test('operation ids are distinct UUIDv4 values', () {
    final a = operationId(), b = operationId();
    expect(a, isNot(b));
    expect(
      RegExp(
        r'^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$',
      ).hasMatch(a),
      true,
    );
  });
  testWidgets('reload removes stale protected records when access is revoked', (
    tester,
  ) async {
    var denied = false;
    await tester.pumpWidget(
      host(
        PropertyLoad<String>(
          load: () async {
            if (denied) throw const PropertyException('ACCESS_DENIED');
            return 'Protected unit';
          },
          builder: (value, reload) => Column(
            children: [
              Text(value),
              TextButton(onPressed: reload, child: const Text('reload')),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Protected unit'), findsOneWidget);
    denied = true;
    await tester.tap(find.text('reload'));
    await tester.pumpAndSettle();
    expect(find.text('Protected unit'), findsNothing);
    expect(find.text(TextEnum.uoDenied.en), findsOneWidget);
  });
  testWidgets('confirmation is explicit and localized in Bangla', (
    tester,
  ) async {
    bool? approved;
    await tester.pumpWidget(
      host(
        Builder(
          builder: (context) => TextButton(
            onPressed: () async {
              approved = await confirmProperty(
                context,
                TextEnum.uoBatchInfo.tr,
              );
            },
            child: const Text('open'),
          ),
        ),
        locale: 'bn',
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    expect(find.text(TextEnum.uoBatchInfo.bn), findsOneWidget);
    expect(approved, isNull);
    await tester.tap(find.text(TextEnum.cancel.bn));
    await tester.pumpAndSettle();
    expect(approved, false);
  });
}
