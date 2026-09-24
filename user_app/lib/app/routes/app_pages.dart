import 'package:customer/features/units_ownership/presentation/property_pages.dart';

import 'package:get/get_navigation/src/routes/get_route.dart';

import 'package:customer/app/shell/app_shell.dart';
import 'package:customer/app/shell/app_shell_binding.dart';
import 'package:customer/features/authentication/presentation/auth_pages.dart';
import 'package:customer/features/building_application/presentation/building_application_pages.dart';
import 'package:customer/app/routes/app_routes.dart';

class AppPages {
  static const initial = AppRoutes.login;

  static final List<GetPage> routes = [
    ...AuthPages.routes,
    ...BuildingApplicationPages.routes,
    ...PropertyPages.routes,
    GetPage(
      name: AppRoutes.appShell,
      page: () => const AppShell(),
      bindings: [
        AppShellBinding(),

      ],
    ),
  ];
}
