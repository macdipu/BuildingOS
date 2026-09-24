import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/entities/ownership_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../../domain/usecases/ownership_use_cases.dart';
import '../widgets/property_widgets.dart';

class OwnershipFormPage extends StatefulWidget {
  const OwnershipFormPage({
    super.key,
    required this.buildingId,
    required this.unitId,
    required this.unitNumber,
    required this.transfer,
  });
  final String buildingId, unitId, unitNumber;
  final bool transfer;
  @override
  State<OwnershipFormPage> createState() => _OwnershipFormPageState();
}

class _OwnershipFormPageState extends State<OwnershipFormPage> {
  final form = GlobalKey<FormState>();
  final share = TextEditingController(),
      reason = TextEditingController(),
      reference = TextEditingController(),
      notes = TextEditingController();
  String? source, recipient;
  OwnershipChange? pending;
  @override
  void dispose() {
    for (final c in [share, reason, reference, notes]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<(CurrentOwnership, List<BuildingMember>)> load() async {
    final access = await Get.find<GetBuildingAccess>()(widget.buildingId);
    if (!access.canEditUnits) throw const PropertyException('ACCESS_DENIED');
    return (
      await Get.find<GetCurrentOwnership>()(widget.buildingId, widget.unitId),
      (await Get.find<ListBuildingMembers>()(
        widget.buildingId,
      )).where((m) => m.status == 'ACTIVE' && m.role == 'OWNER').toList(),
    );
  }

  Future<void> submit(CurrentOwnership current) async {
    if (pending == null) {
      if (!form.currentState!.validate()) return;
      final candidate = OwnershipChange(
        recipientUserId: recipient!,
        sourceOwnerUserId: widget.transfer ? source : null,
        share: num.parse(share.text.trim()),
        effectiveDate: ownershipDate(DateTime.now()),
        expectedVersion: current.revision,
        operationId: operationId(),
        reason: reason.text.trim(),
        reference: reference.text.trim(),
        notes: notes.text.trim(),
      );
      final description =
          '${TextEnum.uoReviewChange.tr}\n\n${widget.unitNumber}\n'
          '${widget.transfer ? '${TextEnum.uoFrom.tr}: ${candidate.sourceOwnerUserId}\n' : ''}'
          '${TextEnum.uoTo.tr}: ${candidate.recipientUserId}\n${TextEnum.uoShare.tr}: ${candidate.share}\n'
          '${TextEnum.uoEffective.tr}: ${candidate.effectiveDate}\n${TextEnum.uoReason.tr}: ${candidate.reason}';
      if (!await confirmProperty(context, description)) return;
      if (!mounted) return;
      setState(() => pending = candidate);
    }
    try {
      if (widget.transfer) {
        await Get.find<TransferUnitOwnership>()(
          widget.buildingId,
          widget.unitId,
          pending!,
        );
      } else {
        await Get.find<AssignUnitOwnership>()(
          widget.buildingId,
          widget.unitId,
          pending!,
        );
      }
      if (mounted) Navigator.pop(context);
    } on PropertyException catch (e) {
      // A definitive rejection did not commit. Uncertain network failures retain payload/key.
      if (e.code != 'UNAVAILABLE' && e.code != 'CONNECTION' && mounted)
        setState(() => pending = null);
      rethrow;
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(
        widget.transfer ? TextEnum.uoTransfer.tr : TextEnum.uoAssign.tr,
      ),
    ),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) => Form(
        key: form,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(
              widget.unitNumber,
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            Text(TextEnum.uoImmediate.tr),
            Text(
              '${TextEnum.uoEffective.tr}: ${pending?.effectiveDate ?? ownershipDate(DateTime.now())}',
            ),
            if (data.$2.isEmpty) Text(TextEnum.uoNoMembers.tr),
            if (pending != null) Text(TextEnum.uoRetryUncertain.tr),
            AbsorbPointer(
              absorbing: pending != null,
              child: Column(
                children: [
                  if (widget.transfer)
                    DropdownButtonFormField<String>(
                      initialValue: source,
                      decoration: InputDecoration(
                        labelText: TextEnum.uoSource.tr,
                      ),
                      isExpanded: true,
                      items: data.$1.current
                          .map(
                            (p) => DropdownMenuItem(
                              value: p.ownerUserId,
                              child: Text(
                                '${p.ownerUserId} (${p.share}%)',
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          )
                          .toList(),
                      onChanged: (v) => setState(() => source = v),
                      validator: requiredText,
                    ),
                  DropdownButtonFormField<String>(
                    initialValue: recipient,
                    decoration: InputDecoration(
                      labelText: TextEnum.uoRecipient.tr,
                    ),
                    isExpanded: true,
                    items: data.$2
                        .map(
                          (m) => DropdownMenuItem(
                            value: m.userId,
                            child: Text(
                              m.userId,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        )
                        .toList(),
                    onChanged: (v) => recipient = v,
                    validator: (v) => v == null
                        ? TextEnum.errRequired.tr
                        : widget.transfer && v == source
                        ? TextEnum.errInvalidRequest.tr
                        : null,
                  ),
                  propertyField(
                    share,
                    TextEnum.uoShare.tr,
                    keyboard: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    validator: (v) {
                      final invalid = decimalText(v, precision: 4);
                      if (invalid != null) return invalid;
                      final n = num.parse(v!.trim());
                      final sourceShares = data.$1.current
                          .where((p) => p.ownerUserId == source)
                          .fold<num>(0, (sum, p) => sum + p.share);
                      final available = widget.transfer
                          ? sourceShares
                          : 100 - data.$1.allocated;
                      return n > 100 || n > available
                          ? TextEnum.uoConflict.tr
                          : null;
                    },
                  ),
                  propertyField(
                    reason,
                    TextEnum.uoReason.tr,
                    validator: requiredText,
                    maxLength: 1000,
                  ),
                  if (widget.transfer)
                    propertyField(
                      reference,
                      TextEnum.uoReference.tr,
                      maxLength: 200,
                    )
                  else
                    propertyField(notes, TextEnum.uoNotes.tr, maxLength: 1000),
                ],
              ),
            ),
            if (data.$2.isNotEmpty)
              PropertyAction(
                label: pending != null
                    ? TextEnum.uoRetry.tr
                    : TextEnum.uoConfirm.tr,
                action: () => submit(data.$1),
              ),
            if (pending == null)
              TextButton(
                onPressed: () {
                  source = null;
                  recipient = null;
                  reload();
                },
                child: Text(TextEnum.uoRefresh.tr),
              ),
          ],
        ),
      ),
    ),
  );
}
