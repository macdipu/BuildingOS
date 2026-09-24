import 'package:get/get.dart';
import 'bindings/building_application_binding.dart';
import 'pages/application_detail_screen.dart';
import 'pages/application_form_screen.dart';
import 'pages/application_list_screen.dart';

class BuildingApplicationPages {
  static const list = '/building-applications';
  static const form = '/building-applications/form';
  static const detail = '/building-applications/detail';

  static final routes = [
    GetPage(
        name: list,
        page: () => const ApplicationListScreen(),
        binding: ApplicationListBinding()),
    GetPage(
        name: form,
        page: () => const ApplicationFormScreen(),
        binding: ApplicationFormBinding()),
    GetPage(
        name: detail,
        page: () => const ApplicationDetailScreen(),
        binding: ApplicationDetailBinding()),
  ];
}
