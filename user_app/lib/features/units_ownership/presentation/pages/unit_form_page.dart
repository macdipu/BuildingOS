import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../widgets/property_widgets.dart';

class UnitFormPage extends StatefulWidget {
  const UnitFormPage({
    super.key,
    required this.buildingId,
    required this.floors,
    this.unit,
    this.draft,
    this.previewOnly = false,
  });
  final String buildingId;
  final List<BuildingFloor> floors;
  final PropertyUnit? unit;
  final UnitDraft? draft;
  final bool previewOnly;
  @override
  State<UnitFormPage> createState() => _UnitFormPageState();
}

class _UnitFormPageState extends State<UnitFormPage> {
  final form = GlobalKey<FormState>();
  final number = TextEditingController(),
      area = TextEditingController(),
      bedrooms = TextEditingController(),
      rate = TextEditingController(),
      notes = TextEditingController(),
      reason = TextEditingController();
  String? floorId;
  String type = 'FLAT';
  @override
  void initState() {
    super.initState();
    final d = widget.draft ?? widget.unit?.draft;
    if (d != null) {
      number.text = d.number;
      area.text = d.areaSqft?.toString() ?? '';
      bedrooms.text = d.bedrooms?.toString() ?? '';
      rate.text = d.defaultMaintenanceRate?.toString() ?? '';
      notes.text = d.notes ?? '';
      floorId = d.floorId;
      type = d.type;
    }
  }

  @override
  void dispose() {
    for (final c in [number, area, bedrooms, rate, notes, reason]) {
      c.dispose();
    }
    super.dispose();
  }

  /// BRD §49 `Save & Add Another`: after a create, keep floor, type and default
  /// rate (and the audit reason) so the next unit on the floor is quick to add.
  Future<void> save({bool addAnother = false}) async {
    if (!form.currentState!.validate()) return;
    final d = UnitDraft(
      number: number.text.trim(),
      floorId: floorId,
      type: type,
      areaSqft: num.parse(area.text.trim()),
      bedrooms: int.tryParse(bedrooms.text.trim()),
      defaultMaintenanceRate: num.tryParse(rate.text.trim()),
      notes: notes.text.trim(),
    );
    if (widget.previewOnly) {
      Navigator.pop(context, d);
      return;
    }
    final access = await Get.find<GetBuildingAccess>()(widget.buildingId);
    if (!access.canEditUnits) throw const PropertyException('ACCESS_DENIED');
    if (widget.unit == null) {
      await Get.find<CreatePropertyUnit>()(
        widget.buildingId,
        d,
        reason.text.trim(),
      );
    } else {
      await Get.find<UpdatePropertyUnit>()(
        widget.buildingId,
        widget.unit!.id,
        d,
        widget.unit!.version,
        reason.text.trim(),
      );
    }
    if (!mounted) return;
    if (addAnother) {
      for (final c in [number, area, bedrooms, notes]) {
        c.clear();
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(TextEnum.uoUnitSaved.trParams({'number': d.number}))),
      );
      return;
    }
    Navigator.pop(context);
  }

  bool get canAddAnother => widget.unit == null && !widget.previewOnly;

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(
        widget.unit == null ? TextEnum.uoCreateUnit.tr : TextEnum.uoEditUnit.tr,
      ),
    ),
    body: Form(
      key: form,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          propertyField(
            number,
            TextEnum.uoNumber.tr,
            validator: requiredText,
            maxLength: 32,
          ),
          DropdownButtonFormField<String>(
            initialValue: widget.floors.any((f) => f.id == floorId)
                ? floorId
                : null,
            decoration: InputDecoration(labelText: TextEnum.uoFloor.tr),
            validator: requiredText,
            items: widget.floors
                .map((f) => DropdownMenuItem(value: f.id, child: Text(f.label)))
                .toList(),
            onChanged: (v) => floorId = v,
          ),
          propertyDropdown(TextEnum.uoType.tr, type, const [
            'FLAT',
            'PARKING',
            'STORAGE',
            'COMMERCIAL',
            'COMMON',
            'OTHER',
          ], (v) => type = v!),
          propertyField(
            area,
            TextEnum.uoArea.tr,
            validator: decimalText,
            keyboard: const TextInputType.numberWithOptions(decimal: true),
          ),
          propertyField(
            bedrooms,
            TextEnum.uoBedrooms.tr,
            validator: (v) =>
                v!.trim().isEmpty ||
                    (int.tryParse(v.trim()) != null && int.parse(v.trim()) >= 0)
                ? null
                : TextEnum.errInvalidRequest.tr,
            keyboard: TextInputType.number,
          ),
          propertyField(
            rate,
            TextEnum.uoRate.tr,
            validator: (v) => decimalText(v, optional: true, zero: true),
            keyboard: const TextInputType.numberWithOptions(decimal: true),
          ),
          propertyField(notes, TextEnum.uoNotes.tr, maxLength: 1000),
          if (!widget.previewOnly)
            propertyField(
              reason,
              TextEnum.uoReason.tr,
              validator: requiredText,
              maxLength: 1000,
            ),
          const SizedBox(height: 8),
          PropertyAction(label: TextEnum.uoSave.tr, action: save),
          if (canAddAnother) ...[
            const SizedBox(height: 8),
            PropertyAction(
              key: const ValueKey('save-add-another'),
              label: TextEnum.uoSaveAddAnother.tr,
              action: () => save(addAnother: true),
            ),
          ],
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(TextEnum.cancel.tr),
          ),
        ],
      ),
    ),
  );
}

class FloorFormPage extends StatefulWidget {
  const FloorFormPage({super.key, required this.buildingId, this.floor});
  final String buildingId;
  final BuildingFloor? floor;
  @override
  State<FloorFormPage> createState() => _FloorFormPageState();
}

class _FloorFormPageState extends State<FloorFormPage> {
  final form = GlobalKey<FormState>();
  final label = TextEditingController(),
      order = TextEditingController(),
      reason = TextEditingController();
  String kind = 'REGULAR';
  @override
  void initState() {
    super.initState();
    label.text = widget.floor?.label ?? '';
    order.text = '${widget.floor?.displayOrder ?? 0}';
    kind = widget.floor?.kind ?? 'REGULAR';
  }

  @override
  void dispose() {
    label.dispose();
    order.dispose();
    reason.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(
        widget.floor == null
            ? TextEnum.uoCreateFloor.tr
            : TextEnum.uoEditFloor.tr,
      ),
    ),
    body: Form(
      key: form,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          propertyField(
            label,
            TextEnum.uoLabel.tr,
            validator: requiredText,
            maxLength: 40,
          ),
          propertyDropdown(TextEnum.uoKind.tr, kind, const [
            'BASEMENT',
            'GROUND',
            'REGULAR',
            'ROOF',
            'COMMON',
          ], (v) => kind = v!),
          propertyField(
            order,
            TextEnum.uoOrder.tr,
            validator: (v) => int.tryParse(v?.trim() ?? '') == null
                ? TextEnum.errInvalidRequest.tr
                : null,
            keyboard: const TextInputType.numberWithOptions(signed: true),
          ),
          propertyField(
            reason,
            TextEnum.uoReason.tr,
            validator: requiredText,
            maxLength: 1000,
          ),
          PropertyAction(
            label: TextEnum.uoSave.tr,
            action: () async {
              if (!form.currentState!.validate()) return;
              final access = await Get.find<GetBuildingAccess>()(
                widget.buildingId,
              );
              if (!access.canEditUnits)
                throw const PropertyException('ACCESS_DENIED');
              final d = FloorDraft(
                label.text.trim(),
                kind,
                int.parse(order.text.trim()),
              );
              if (widget.floor == null) {
                await Get.find<CreateBuildingFloor>()(
                  widget.buildingId,
                  d,
                  reason.text.trim(),
                );
              } else {
                await Get.find<UpdateBuildingFloor>()(
                  widget.buildingId,
                  widget.floor!.id,
                  d,
                  widget.floor!.version,
                  reason.text.trim(),
                );
              }
              if (context.mounted) Navigator.pop(context);
            },
          ),
        ],
      ),
    ),
  );
}
