import 'dart:async';
import 'dart:developer';

import 'package:customer/app/config/app_config.dart';
import 'package:customer/core/auth/session_expiry_notifier.dart';
import 'package:customer/core/database/client/preference_cache.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:customer/core/services/push_notification/notification_service.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

Future<void> bootstrap(FutureOr<Widget> Function() builder) async {
  FlutterError.onError = (details) {
    log(details.exceptionAsString(), stackTrace: details.stack);
  };

  // TODO: Enable Firebase for production:
  // await Firebase.initializeApp();
  // await NotificationService().init();

  _initialize();
  runApp(await builder());
}

void _initialize() {
  Get.lazyPut<AppConfig>(() => const AppConfig(), fenix: true);
  Get.lazyPut<PreferenceCache>(() => PreferenceCache(), fenix: true);
  Get.put<SessionExpiryNotifier>(SessionExpiryNotifier(), permanent: true);
  Get.lazyPut<ApiUrl>(() => ApiUrl(Get.find<AppConfig>().getApiClientConfig()),
      fenix: true);
  Get.lazyPut<ApiClient>(
    () => ApiClient(
      Get.find<AppConfig>().getApiClientConfig(),
      Get.find<PreferenceCache>(),
      Get.find<ApiUrl>(),
      Get.find<SessionExpiryNotifier>(),
    ),
    fenix: true,
  );
  Get.lazyPut<NotificationService>(() => NotificationService(), fenix: true);
}
