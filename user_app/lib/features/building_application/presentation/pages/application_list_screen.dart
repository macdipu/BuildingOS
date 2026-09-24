import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../domain/entities/building_application.dart';
import '../controllers/application_list_controller.dart';
import '../widgets/error_retry.dart';
import '../widgets/status_chip.dart';

class ApplicationListScreen extends StatelessWidget {
  const ApplicationListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationListController>();
    return Scaffold(
      appBar: AppBar(title: Text(TextEnum.buildingApplications.tr)),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: controller.openNew,
        icon: const Icon(Icons.add_home_work_outlined),
        label: Text(TextEnum.registerBuilding.tr),
      ),
      body: SafeArea(
        child: Obx(() {
          final items = controller.applications.toList();
          final error = controller.errorMessage.value;
          if (controller.isLoading.value && items.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }
          if (error != null && items.isEmpty) {
            return Center(
                child: ErrorRetry(message: error, onRetry: controller.load));
          }
          return RefreshIndicator(
            onRefresh: controller.load,
            child: items.isEmpty
                ? ListView(children: [
                    Padding(
                      padding: const EdgeInsets.all(32),
                      child: Text(TextEnum.noApplications.tr,
                          textAlign: TextAlign.center),
                    ),
                  ])
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(16, 16, 16, 96),
                    itemCount: items.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, index) => _ApplicationTile(
                        application: items[index],
                        onTap: () => controller.open(items[index])),
                  ),
          );
        }),
      ),
    );
  }
}

class _ApplicationTile extends StatelessWidget {
  const _ApplicationTile({required this.application, required this.onTap});

  final BuildingApplication application;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final name = application.details.buildingName;
    return Card(
      child: ListTile(
        onTap: onTap,
        title: Text(
            name == null || name.isEmpty ? TextEnum.newApplication.tr : name),
        subtitle: Text(TextEnum.applicationNumber
            .trParams({'number': application.applicationNumber})),
        trailing: StatusChip(status: application.status),
      ),
    );
  }
}
