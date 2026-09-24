import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../domain/entities/application_enums.dart';
import '../../domain/entities/building_application.dart';
import '../../domain/entities/draft_field.dart';
import '../building_application_texts.dart';
import '../controllers/application_detail_controller.dart';
import '../widgets/confirm_dialog.dart';
import '../widgets/documents_section.dart';
import '../widgets/error_retry.dart';
import '../widgets/status_chip.dart';

class ApplicationDetailScreen extends StatelessWidget {
  const ApplicationDetailScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationDetailController>();
    return Obx(() {
      final application = controller.application.value;
      final error = controller.errorMessage.value;
      return Scaffold(
        appBar: AppBar(
          title: Text(application?.details.buildingName ??
              TextEnum.buildingApplications.tr),
          actions: [
            if (application != null && application.status.isEditable)
              IconButton(
                  tooltip: TextEnum.edit.tr,
                  icon: const Icon(Icons.edit_outlined),
                  onPressed: controller.edit),
          ],
        ),
        bottomNavigationBar:
            application == null || !application.status.isEditable
                ? null
                : _SubmitBar(application: application),
        body: SafeArea(
          child: application == null
              ? Center(
                  child: error != null
                      ? ErrorRetry(message: error, onRetry: controller.load)
                      : const CircularProgressIndicator(),
                )
              : RefreshIndicator(
                  onRefresh: controller.load,
                  child: ListView(
                    padding: const EdgeInsets.all(16),
                    children: [
                      _Header(application: application),
                      _ReviewerNote(application: application),
                      _Summary(application: application),
                      const SizedBox(height: 16),
                      DocumentsSection(editable: application.status.isEditable),
                    ],
                  ),
                ),
        ),
      );
    });
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.application});

  final BuildingApplication application;

  @override
  Widget build(BuildContext context) => Row(
        children: [
          Expanded(
            child: Text(
                TextEnum.applicationNumber
                    .trParams({'number': application.applicationNumber}),
                style: Theme.of(context).textTheme.titleSmall),
          ),
          StatusChip(status: application.status),
        ],
      );
}

class _ReviewerNote extends StatelessWidget {
  const _ReviewerNote({required this.application});

  final BuildingApplication application;

  @override
  Widget build(BuildContext context) {
    final (title, text) = switch (application.status) {
      ApplicationStatus.moreInformationRequired => (
          TextEnum.reviewerMessage.tr,
          application.infoRequestMessage
        ),
      ApplicationStatus.rejected => (
          TextEnum.rejectionReason.tr,
          application.rejectionReason
        ),
      _ => (null, null),
    };
    if (title == null || text == null) return const SizedBox.shrink();
    final scheme = Theme.of(context).colorScheme;
    return Card(
      color: application.status == ApplicationStatus.rejected
          ? scheme.errorContainer
          : scheme.tertiaryContainer,
      margin: const EdgeInsets.only(top: 12),
      child: ListTile(title: Text(title), subtitle: Text(text)),
    );
  }
}

class _Summary extends StatelessWidget {
  const _Summary({required this.application});

  final BuildingApplication application;

  @override
  Widget build(BuildContext context) {
    final d = application.details;
    final rows = <(String, String?)>[
      (TextEnum.fieldBuildingType.tr, d.buildingType?.label),
      (
        TextEnum.fieldAddress.tr,
        [d.address, d.area, d.district].whereType<String>().join(', ')
      ),
      (TextEnum.fieldEstimatedUnits.tr, d.estimatedUnits?.toString()),
      (TextEnum.fieldRelationship.tr, d.applicantRelationship?.label),
      (TextEnum.fieldContactName.tr, d.contactName),
      (TextEnum.fieldContactPhone.tr, d.contactPhone),
    ];
    return Card(
      margin: const EdgeInsets.only(top: 12),
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8),
        child: Column(children: [
          for (final (label, value) in rows)
            ListTile(
                dense: true,
                title: Text(label),
                subtitle: Text(value == null || value.isEmpty ? '—' : value)),
        ]),
      ),
    );
  }
}

class _SubmitBar extends StatelessWidget {
  const _SubmitBar({required this.application});

  final BuildingApplication application;

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationDetailController>();
    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (!application.canSubmit)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(
                  '${TextEnum.missingFieldsHint.tr}\n'
                  '${application.missingFields.map((f) => _fieldLabel(f)).join(', ')}',
                  textAlign: TextAlign.center,
                ),
              ),
            FilledButton(
              onPressed: !application.canSubmit || controller.isLoading.value
                  ? null
                  : () async {
                      final ok = await confirm(context,
                          message: TextEnum.confirmSubmit.tr,
                          confirmLabel: TextEnum.submitApplication.tr);
                      if (ok) await controller.submit();
                    },
              child: Text(TextEnum.submitApplication.tr),
            ),
          ],
        ),
      ),
    );
  }

  static String _fieldLabel(String apiField) {
    for (final field in DraftField.values) {
      if (field.name == apiField) return field.label;
    }
    return apiField;
  }
}
