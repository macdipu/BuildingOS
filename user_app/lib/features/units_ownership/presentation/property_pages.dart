import 'package:get/get.dart';
import 'bindings/property_binding.dart';
import 'pages/property_home_page.dart';

class PropertyPages {
  static const home = '/my-buildings';
  static final routes = [
    GetPage(
      name: home,
      page: () => const PropertyHomePage(),
      binding: PropertyBinding(),
    ),
  ];
}
