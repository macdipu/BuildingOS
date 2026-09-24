import 'dart:async';

import 'package:customer/core/controllers/base_controller.dart';
import 'package:customer/core/utils/state_status.dart';
import 'package:get/get.dart';
import '../../domain/entities/building_application.dart';
import '../../domain/usecases/list_my_applications_use_case.dart';
import '../building_application_pages.dart';
import '../building_application_texts.dart';

class ApplicationListController extends BaseController {
  ApplicationListController({required ListMyApplicationsUseCase listMine})
      : _listMine = listMine;

  final ListMyApplicationsUseCase _listMine;
  final applications = <BuildingApplication>[].obs;

  @override
  void onInit() {
    super.onInit();
    unawaited(load());
  }

  Future<void> load() async {
    if (isLoading.value) return;
    errorMessage.value = null;
    await doAction<List<BuildingApplication>>(
      action: _listMine.call,
      onSuccess: (items) {
        applications.assignAll(items);
        status.value = items.isEmpty ? StateStatus.empty : StateStatus.success;
      },
      onError: (code) => errorMessage.value = applicationErrorText(code ?? ''),
    );
  }

  Future<void> openNew() async {
    final created = await Get.toNamed(BuildingApplicationPages.form);
    if (created is BuildingApplication) {
      await Get.toNamed(BuildingApplicationPages.detail, arguments: created.id);
    }
    await load();
  }

  Future<void> open(BuildingApplication application) async {
    await Get.toNamed(BuildingApplicationPages.detail,
        arguments: application.id);
    await load();
  }
}
