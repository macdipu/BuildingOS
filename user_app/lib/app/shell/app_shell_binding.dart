import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/features/units_ownership/presentation/bindings/property_binding.dart';
import 'package:get/get.dart';
import 'app_shell_controller.dart';

class AppShellBinding extends Bindings {
  @override
  void dependencies() {
    // Units and Properties tabs host units_ownership pages directly.
    PropertyBinding().dependencies();
    Get.lazyPut<AppShellController>(() => AppShellController(Get.find<ActiveBuildingService>()));
  }
}
