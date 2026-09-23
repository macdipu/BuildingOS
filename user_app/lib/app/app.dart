import 'dart:async';

import 'package:customer/app/routes/app_pages.dart';
import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/core/auth/session_expiry_notifier.dart';
import 'package:customer/core/controllers/locale_controller.dart';
import 'package:customer/core/controllers/theme_controller.dart';
import 'package:customer/core/services/navigation/navigation_history_observer.dart';
import 'package:customer/core/services/navigation/navigation_service.dart';
import 'package:customer/core/widgets/images/app_svg.dart';
import 'package:customer/res/resources.dart';
import 'package:customer/res/strings/app_translations.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:get/get.dart';

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final ThemeController _themeController = Get.put(ThemeController());
  final LocaleController _localeController = Get.put(LocaleController());
  late final StreamSubscription<void> _sessionExpirySubscription;

  @override
  void initState() {
    super.initState();
    _sessionExpirySubscription = Get.find<SessionExpiryNotifier>()
        .onExpired
        .listen((_) => Get.offAllNamed(AppRoutes.login));
    unawaited(AppSvg.precache(Resources.drawable.splashImage));
  }

  @override
  void dispose() {
    unawaited(_sessionExpirySubscription.cancel());
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return GetMaterialApp(
      locale: Locale(_localeController.currentLangCode.value),
      fallbackLocale: const Locale('en'),
      supportedLocales: AppTranslations.supportedLocales,
      translations: AppTranslations(),
      title: TextEnum.appName.tr,
      theme: AppTheme.lightTheme,
      darkTheme: AppTheme.darkTheme,
      themeMode: _themeController.themeMode,
      initialRoute: AppPages.initial,
      getPages: AppPages.routes,
      navigatorKey: NavigationService.navigatorKey,
      navigatorObservers: [NavigationHistoryObserver()],
      debugShowCheckedModeBanner: false,
      localizationsDelegates: const [
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
    );
  }
}
