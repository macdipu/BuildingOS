import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:file_picker/file_picker.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';
import 'unit_form_page.dart';

class BatchUnitsPage extends StatefulWidget {
  const BatchUnitsPage({
    super.key,
    required this.buildingId,
    required this.floors,
  });
  final String buildingId;
  final List<BuildingFloor> floors;
  @override
  State<BatchUnitsPage> createState() => _BatchUnitsPageState();
}

class _BatchUnitsPageState extends State<BatchUnitsPage> {
  final form = GlobalKey<FormState>();
  final pattern = TextEditingController(text: '{floor}{letter}'),
      count = TextEditingController(text: '4'),
      start = TextEditingController(text: '1'),
      area = TextEditingController(),
      reason = TextEditingController();
  final selected = <String>{};
  String type = 'FLAT';
  String? template;
  BatchPreview? preview;
  String? operation;
  bool busy = false;
  bool uncertain = false;
  Object? error;
  List<UnitDraft>? submittedRows;
  String? submittedReason;
  @override
  void dispose() {
    for (final c in [pattern, count, start, area, reason]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> run(Future<void> Function() action) async {
    if (busy) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await action();
    } catch (e) {
      if (mounted)
        setState(() {
          error = e;
          if (e is PropertyException && e.preview != null) {
            preview = e.preview;
            uncertain = false;
            operation = null;
          }
        });
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  void accept(BatchPreview value) {
    if (mounted)
      setState(() {
        preview = value;
        operation = null;
        uncertain = false;
      });
  }

  Future<void> generate() async {
    if (!form.currentState!.validate()) return;
    if (selected.isEmpty) throw const PropertyException('FLOOR_REQUIRED');
    accept(
      await Get.find<GenerateUnitPreview>()(
        widget.buildingId,
        GenerateUnits(
          floorIds: selected.toList(),
          unitsPerFloor: int.parse(count.text.trim()),
          numberPattern: pattern.text.trim(),
          start: int.parse(start.text.trim()),
          type: type,
          areaSqft: num.tryParse(area.text.trim()) ?? 1,
          templateFloorId: template,
        ),
      ),
    );
  }

  Future<void> import() async {
    final picked = await FilePicker.pickFiles(
      type: FileType.custom,
      allowedExtensions: ['csv', 'xlsx'],
    );
    final path = picked?.files.single.path;
    if (path == null) return;
    accept(await Get.find<ImportUnitPreview>()(widget.buildingId, path));
  }

  Future<void> commit() async {
    if (preview?.valid != true) return;
    if (!uncertain && reason.text.trim().isEmpty)
      throw const PropertyException('REASON_REQUIRED');
    if (!await confirmProperty(context, TextEnum.uoBatchInfo.tr)) return;
    if (!uncertain) {
      operation = operationId();
      submittedRows = preview!.rows.map((r) => r.draft).toList();
      submittedReason = reason.text.trim();
    }
    uncertain =
        true; // Keep exactly this payload and key on network-uncertain retry.
    await Get.find<CommitUnitBatch>()(
      widget.buildingId,
      submittedRows!,
      operation!,
      submittedReason!,
    );
    if (mounted) Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoBatch.tr)),
    body: ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(TextEnum.uoBatchInfo.tr),
        if (busy) const LinearProgressIndicator(),
        if (error != null)
          Padding(
            padding: const EdgeInsets.all(12),
            child: Text(propertyError(error)),
          ),
        if (!uncertain)
          AbsorbPointer(
            absorbing: busy,
            child: Form(
              key: form,
              child: Column(
                children: [
                  for (final f in widget.floors)
                    CheckboxListTile(
                      title: Text(f.label),
                      value: selected.contains(f.id),
                      onChanged: (v) => setState(() {
                        if (v == true) {
                          selected.add(f.id);
                        } else {
                          selected.remove(f.id);
                        }
                      }),
                    ),
                  DropdownButtonFormField<String>(
                    initialValue: template ?? '',
                    decoration: InputDecoration(
                      labelText: TextEnum.uoTemplate.tr,
                    ),
                    items: [
                      DropdownMenuItem(
                        value: '',
                        child: Text(TextEnum.uoNone.tr),
                      ),
                      ...widget.floors.map(
                        (f) =>
                            DropdownMenuItem(value: f.id, child: Text(f.label)),
                      ),
                    ],
                    onChanged: (v) =>
                        setState(() => template = v == '' ? null : v),
                  ),
                  if (template == null) ...[
                    propertyField(
                      pattern,
                      TextEnum.uoPattern.tr,
                      validator: requiredText,
                    ),
                    propertyField(
                      count,
                      TextEnum.uoCount.tr,
                      validator: (v) {
                        final n = int.tryParse(v ?? '');
                        return n == null || n < 1 || n > 500
                            ? TextEnum.errInvalidRequest.tr
                            : null;
                      },
                      keyboard: TextInputType.number,
                    ),
                    propertyField(
                      start,
                      TextEnum.uoStart.tr,
                      validator: (v) => int.tryParse(v ?? '') == null
                          ? TextEnum.errInvalidRequest.tr
                          : null,
                      keyboard: TextInputType.number,
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
                      keyboard: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                    ),
                  ],
                  Wrap(
                    spacing: 8,
                    children: [
                      OutlinedButton(
                        onPressed: busy ? null : () => run(generate),
                        child: Text(TextEnum.uoPreview.tr),
                      ),
                      OutlinedButton(
                        onPressed: busy ? null : () => run(import),
                        child: Text(TextEnum.uoImport.tr),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        if (preview != null) ...[
          const SizedBox(height: 24),
          Text(
            '${TextEnum.uoPreview.tr} (${preview!.rows.length})',
            style: Theme.of(context).textTheme.titleLarge,
          ),
          if (!preview!.valid) Text(TextEnum.uoInvalidRows.tr),
          for (var index = 0; index < preview!.rows.length; index++)
            Card(
              child: ListTile(
                title: Text(
                  '${preview!.rows[index].rowNumber}. ${preview!.rows[index].draft.number}',
                ),
                subtitle: Text(
                  '${preview!.rows[index].draft.floorLabel ?? ''} · ${preview!.rows[index].draft.areaSqft ?? ''}\n${preview!.rows[index].errors.map((e) => propertyError(PropertyException(e.code))).join('\n')}',
                ),
                trailing: !uncertain
                    ? IconButton(
                        tooltip: TextEnum.uoEdit.tr,
                        icon: const Icon(Icons.edit_outlined),
                        onPressed: busy
                            ? null
                            : () async {
                                final edited = await Navigator.push<UnitDraft>(
                                  context,
                                  MaterialPageRoute(
                                    builder: (_) => UnitFormPage(
                                      buildingId: widget.buildingId,
                                      floors: widget.floors,
                                      draft: preview!.rows[index].draft,
                                      previewOnly: true,
                                    ),
                                  ),
                                );
                                if (edited == null || !mounted) return;
                                setState(() {
                                  final rows = [...preview!.rows];
                                  rows[index] = BatchRow(
                                    index + 1,
                                    edited,
                                    const [],
                                  );
                                  preview = BatchPreview(false, rows);
                                  operation = null;
                                });
                              },
                      )
                    : null,
              ),
            ),
          if (!uncertain)
            OutlinedButton(
              onPressed: busy
                  ? null
                  : () => run(
                      () async => accept(
                        await Get.find<PreviewUnitRows>()(
                          widget.buildingId,
                          preview!.rows.map((r) => r.draft).toList(),
                        ),
                      ),
                    ),
              child: Text(TextEnum.uoRecheck.tr),
            ),
          if (!uncertain)
            propertyField(reason, TextEnum.uoReason.tr, maxLength: 1000),
          FilledButton(
            onPressed: busy || preview?.valid != true
                ? null
                : () => run(commit),
            child: Text(uncertain ? TextEnum.uoRetry.tr : TextEnum.uoCommit.tr),
          ),
        ],
      ],
    ),
  );
}
