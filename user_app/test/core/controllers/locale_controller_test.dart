import 'dart:async';

import 'package:customer/core/controllers/locale_controller.dart';
import 'package:customer/core/entities/theme_mode_enum.dart';
import 'package:customer/core/services/app_settings/app_settings_repository.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

class _FakeSettings implements AppSettingsRepository {
  _FakeSettings({this.onSave});

  final Future<void> Function(String locale)? onSave;
  final saved = <String>[];

  @override
  Future<String?> getLocale() async => 'en';

  @override
  Future<void> setLocale(String locale) {
    saved.add(locale);
    return onSave?.call(locale) ?? Future.value();
  }

  @override
  Future<AppThemeMode> getThemeMode() async => AppThemeMode.system;

  @override
  Future<void> setThemeMode(AppThemeMode mode) async {}

  @override
  Future<void> clearSettings() async {}
}

Future<LocaleController> _pumpApp(
    WidgetTester tester, _FakeSettings settings) async {
  final controller = Get.put(LocaleController(settingsRepository: settings));
  await tester.pumpWidget(const GetMaterialApp(home: SizedBox.shrink()));
  await tester.pumpAndSettle();
  return controller;
}

void main() {
  tearDown(Get.reset);

  testWidgets('toggle updates locale before the save completes',
      (tester) async {
    final pendingSave = Completer<void>();
    final settings = _FakeSettings(onSave: (_) => pendingSave.future);
    final controller = await _pumpApp(tester, settings);
    expect(controller.currentLangCode.value, 'en');

    controller.toggleLocale();

    expect(controller.currentLangCode.value, 'bn');
    expect(Get.locale, const Locale('bn'));

    await tester.pump();
    expect(settings.saved, ['bn']);
    expect(controller.currentLangCode.value, 'bn');
    expect(pendingSave.isCompleted, isFalse);

    pendingSave.complete();
    await tester.pumpAndSettle();
  });

  testWidgets('rapid toggles persist in order so the last choice wins',
      (tester) async {
    final firstSave = Completer<void>();
    final settings = _FakeSettings(
      onSave: (locale) => locale == 'bn' ? firstSave.future : Future.value(),
    );
    final controller = await _pumpApp(tester, settings);

    controller.toggleLocale();
    await tester.pumpAndSettle();
    controller.toggleLocale();
    await tester.pumpAndSettle();

    expect(controller.currentLangCode.value, 'en');
    expect(firstSave.isCompleted, isFalse);
    expect(settings.saved, ['bn']);

    firstSave.complete();
    await tester.pumpAndSettle();

    expect(settings.saved, ['bn', 'en']);
  });

  testWidgets('failed save keeps the new locale and does not throw',
      (tester) async {
    final settings =
        _FakeSettings(onSave: (_) => Future.error(StateError('disk full')));
    final controller = await _pumpApp(tester, settings);

    controller.toggleLocale();
    await tester.pumpAndSettle();

    expect(controller.currentLangCode.value, 'bn');
    expect(Get.locale, const Locale('bn'));
    expect(tester.takeException(), isNull);
  });
}
