import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../domain/entities/application_enums.dart';
import '../../domain/entities/draft_field.dart';
import '../building_application_texts.dart';
import '../controllers/application_form_controller.dart';
import '../widgets/draft_text_field.dart';
import '../widgets/enum_dropdown.dart';

class ApplicationFormScreen extends StatelessWidget {
  const ApplicationFormScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationFormController>();
    return Scaffold(
      appBar: AppBar(
        title: Text(controller.isEditing
            ? TextEnum.editApplication.tr
            : TextEnum.newApplication.tr),
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: const [
            _BuildingSection(),
            _LocationSection(),
            _ContactSection(),
          ],
        ),
      ),
      bottomNavigationBar: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Obx(() => FilledButton(
                onPressed: controller.isLoading.value ? null : controller.save,
                child: controller.isLoading.value
                    ? const SizedBox.square(
                        dimension: 20,
                        child: CircularProgressIndicator(strokeWidth: 2))
                    : Text(TextEnum.saveDraft.tr),
              )),
        ),
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);

  final String text;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(top: 8, bottom: 12),
        child: Text(text, style: Theme.of(context).textTheme.titleMedium),
      );
}

class _BuildingSection extends StatelessWidget {
  const _BuildingSection();

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationFormController>();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SectionTitle(TextEnum.sectionBuilding.tr),
        const DraftTextField(field: DraftField.buildingName),
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Obx(() => EnumDropdown<BuildingType>(
                label: DraftField.buildingType.label,
                values: BuildingType.values,
                value: controller.buildingType.value,
                labelOf: (type) => type.label,
                errorText: controller.errorFor(DraftField.buildingType),
                onChanged: (type) => controller.buildingType.value = type,
              )),
        ),
        const DraftTextField(
            field: DraftField.estimatedUnits,
            keyboardType: TextInputType.number),
        const DraftTextField(
            field: DraftField.totalFloors, keyboardType: TextInputType.number),
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Obx(() => EnumDropdown<ManagementType>(
                label: TextEnum.fieldManagementType.tr,
                values: ManagementType.values,
                value: controller.managementType.value,
                labelOf: (type) => type.label,
                optional: true,
                onChanged: (type) => controller.managementType.value = type,
              )),
        ),
      ],
    );
  }
}

class _LocationSection extends StatelessWidget {
  const _LocationSection();

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationFormController>();
    const decimal =
        TextInputType.numberWithOptions(decimal: true, signed: true);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SectionTitle(TextEnum.sectionLocation.tr),
        const DraftTextField(field: DraftField.address, maxLines: 2),
        const DraftTextField(field: DraftField.area),
        const DraftTextField(field: DraftField.district),
        const DraftTextField(
            field: DraftField.postalCode, keyboardType: TextInputType.number),
        const DraftTextField(
            field: DraftField.coordinates, keyboardType: decimal),
        DraftTextField(
          field: DraftField.coordinates,
          keyboardType: decimal,
          label: TextEnum.fieldLongitude.tr,
          controllerOverride: controller.longitude,
        ),
      ],
    );
  }
}

class _ContactSection extends StatelessWidget {
  const _ContactSection();

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationFormController>();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _SectionTitle(TextEnum.sectionContact.tr),
        Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: Obx(() => EnumDropdown<ApplicantRelationship>(
                label: DraftField.applicantRelationship.label,
                values: ApplicantRelationship.values,
                value: controller.relationship.value,
                labelOf: (relationship) => relationship.label,
                errorText:
                    controller.errorFor(DraftField.applicantRelationship),
                onChanged: (relationship) =>
                    controller.relationship.value = relationship,
              )),
        ),
        Obx(() => controller.relationship.value == ApplicantRelationship.other
            ? const DraftTextField(field: DraftField.relationshipNote)
            : const SizedBox.shrink()),
        const DraftTextField(field: DraftField.contactName),
        const DraftTextField(
            field: DraftField.contactPhone, keyboardType: TextInputType.phone),
        const DraftTextField(
            field: DraftField.contactEmail,
            keyboardType: TextInputType.emailAddress),
      ],
    );
  }
}
