import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/core/widgets/buttons/common_button.dart';
import 'package:customer/features/units_ownership/presentation/widgets/property_widgets.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

const _cta = Color(0xFF2563EB);

Future<void> _pump(WidgetTester tester, Widget child) => tester.pumpWidget(GetMaterialApp(
      theme: AppTheme.lightTheme,
      home: Scaffold(body: Padding(padding: const EdgeInsets.all(16), child: Column(children: [child]))),
    ));

Color? _background(WidgetTester tester, Finder button) {
  final material = tester.widget<Material>(find.descendant(of: button, matching: find.byType(Material)).first);
  return material.color;
}

void main() {
  testWidgets('CommonButton primary CTA uses Stitch primary-container', (tester) async {
    await _pump(tester, CommonButton.elevated(title: 'Continue with Phone', onTap: () {}));
    expect(_background(tester, find.byType(ElevatedButton)), _cta);
  });

  testWidgets('PropertyAction is a full-width 48px Stitch CTA', (tester) async {
    await _pump(tester, PropertyAction(label: 'Save', action: () async {}));
    final button = find.byType(FilledButton);
    expect(_background(tester, button), _cta);
    final size = tester.getSize(button);
    expect(size.width, tester.getSize(find.byType(Column).first).width);
    expect(size.height, greaterThanOrEqualTo(48));
  });
}
