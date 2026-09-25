import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/app/session/active_building_service.dart';
import 'package:customer/features/building_application/presentation/building_application_pages.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'package:customer/features/units_ownership/presentation/building_opener.dart';
import 'package:get/get.dart';

class ShellBuildingOpener implements BuildingOpener {
  ShellBuildingOpener(this._active);

  final ActiveBuildingService _active;

  @override
  Future<void> open(BuildingSummary building) async {
    _active.select(building);
    await Get.offAllNamed(AppRoutes.appShell);
  }

  @override
  Future<void> openBuildingApplications() async {
    await Get.toNamed(BuildingApplicationPages.list);
  }
}
