import 'dart:async';

import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/core/services/app_settings/app_settings_repository_impl.dart';
import 'package:customer/core/services/app_settings/app_settings_repository.dart';

class LocaleController extends GetxController {
  LocaleController({AppSettingsRepository? settingsRepository})
      : _settingsRepository = settingsRepository ?? AppSettingsRepositoryImpl();

  final AppSettingsRepository _settingsRepository;

  final RxString currentLangCode = 'bn'.obs;
  Future<void> _pendingSave = Future.value();

  @override
  void onInit() {
    super.onInit();
    _loadLocale();
  }

  Future<void> _loadLocale() async {
    try {
      final saved = await _settingsRepository.getLocale();
      final code = saved ?? 'en';
      currentLangCode.value = code;
      await Get.updateLocale(Locale(code));
    } catch (e) {
      debugPrint('Error loading locale: $e');
    }
  }

  void toggleLocale() {
    final newCode = currentLangCode.value == 'bn' ? 'en' : 'bn';
    currentLangCode.value = newCode;
    unawaited(Get.updateLocale(Locale(newCode)));
    // Chained so rapid toggles persist in order and the last choice wins on disk.
    _pendingSave = _pendingSave.then((_) => _persistLocale(newCode));
  }

  Future<void> _persistLocale(String code) async {
    try {
      await _settingsRepository.setLocale(code);
    } catch (e) {
      debugPrint('Error saving locale: $e');
    }
  }
}
