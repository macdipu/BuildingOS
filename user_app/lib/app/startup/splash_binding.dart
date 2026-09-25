import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/startup/splash_controller.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/features/building_application/domain/usecases/list_my_applications_use_case.dart';
import 'package:customer/features/building_application/presentation/bindings/building_application_binding.dart';
import 'package:customer/features/units_ownership/domain/usecases/property_use_cases.dart';
import 'package:customer/features/units_ownership/presentation/bindings/property_binding.dart';
import 'package:get/get.dart';

class SplashBinding extends Bindings {
  @override
  void dependencies() {
    PropertyBinding().dependencies();
    BuildingApplicationDataBinding().dependencies();
    final api = Get.find<ApiClient>();
    Get.lazyPut(
      () => SplashController(
        restoreSession: () async {
          await api.setToken();
          return api.hasToken();
        },
        listBuildings: () => Get.find<ListMyBuildings>()(),
        listInvitations: () => Get.find<ListMyInvitations>()(),
        listApplicationStatuses: () async => (await Get.find<ListMyApplicationsUseCase>()())
            .fold((_) => const [], (apps) => apps.map((a) => a.status).toList()),
        activeBuilding: Get.find<ActiveBuildingService>(),
      ),
    );
  }
}
