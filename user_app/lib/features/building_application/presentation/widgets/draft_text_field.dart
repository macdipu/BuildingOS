import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../domain/entities/draft_field.dart';
import '../building_application_texts.dart';
import '../controllers/application_form_controller.dart';

class DraftTextField extends StatelessWidget {
  const DraftTextField({
    super.key,
    required this.field,
    this.keyboardType,
    this.maxLines = 1,
    this.label,
    this.controllerOverride,
  });

  final DraftField field;
  final TextInputType? keyboardType;
  final int maxLines;
  final String? label;
  final TextEditingController? controllerOverride;

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<ApplicationFormController>();
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Obx(() => TextField(
            controller: controllerOverride ?? controller.texts[field],
            keyboardType: keyboardType,
            maxLines: maxLines,
            decoration: InputDecoration(
              labelText: label ?? field.label,
              errorText: controller.errorFor(field),
              border: const OutlineInputBorder(),
            ),
          )),
    );
  }
}
