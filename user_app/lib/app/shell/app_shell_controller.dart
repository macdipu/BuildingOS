import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/app/shell/nav_sets.dart';
import 'package:get/get.dart';

class AppShellController extends GetxController {
  AppShellController(this._activeBuilding);

  final ActiveBuildingService _activeBuilding;
  final RxInt currentIndex = 0.obs;
  final RxList<ShellTab> tabs = <ShellTab>[].obs;

  ActiveBuildingService get activeBuilding => _activeBuilding;

  @override
  void onInit() {
    super.onInit();
    _resolve();
    ever(_activeBuilding.building, (_) => _resolve());
  }

  void _resolve() {
    tabs.assignAll(navFor(_activeBuilding.building.value?.roles ?? const []));
    currentIndex.value = 0;
  }

  void changeTab(int index) {
    currentIndex.value = index;
  }
}
