import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/entities/ownership_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../../domain/usecases/ownership_use_cases.dart';
import '../widgets/property_widgets.dart';
import 'ownership_form_page.dart';
import 'transfer_files_page.dart';

class OwnershipPage extends StatelessWidget {
  const OwnershipPage({
    super.key,
    required this.buildingId,
    required this.unitId,
    required this.unitNumber,
  });
  final String buildingId, unitId, unitNumber;
  Future<(BuildingAccess, CurrentOwnership, OwnershipHistory)> load() async => (
    await Get.find<GetBuildingAccess>()(buildingId),
    await Get.find<GetCurrentOwnership>()(buildingId, unitId),
    await Get.find<GetOwnershipHistory>()(buildingId, unitId),
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text('${TextEnum.uoOwnership.tr} · $unitNumber')),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) {
        Future<void> open(Widget page) async {
          await Navigator.push(
            context,
            MaterialPageRoute<void>(builder: (_) => page),
          );
          if (context.mounted) reload();
        }

        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(TextEnum.uoHistoryScope.tr),
            if (data.$1.readOnly) Text(TextEnum.uoReadOnly.tr),
            IconButton(
              tooltip: TextEnum.uoRefresh.tr,
              onPressed: reload,
              icon: const Icon(Icons.refresh),
            ),
            Text(
              '${TextEnum.uoAllocated.tr}: ${data.$2.allocated}',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            if (data.$2.current.isEmpty) Text(TextEnum.uoEmpty.tr),
            for (final p in data.$2.current)
              ListTile(
                title: SelectableText(p.ownerUserId),
                subtitle: Text('${p.share}% · ${p.startAt}'),
              ),
            if (data.$1.canEditUnits)
              Wrap(
                spacing: 8,
                children: [
                  OutlinedButton(
                    onPressed: () => open(
                      OwnershipFormPage(
                        buildingId: buildingId,
                        unitId: unitId,
                        unitNumber: unitNumber,
                        transfer: false,
                      ),
                    ),
                    child: Text(TextEnum.uoAssign.tr),
                  ),
                  OutlinedButton(
                    onPressed: data.$2.current.isEmpty
                        ? null
                        : () => open(
                            OwnershipFormPage(
                              buildingId: buildingId,
                              unitId: unitId,
                              unitNumber: unitNumber,
                              transfer: true,
                            ),
                          ),
                    child: Text(TextEnum.uoTransfer.tr),
                  ),
                ],
              ),
            const SizedBox(height: 24),
            Text(
              TextEnum.uoHistory.tr,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            if (data.$3.periods.isEmpty) Text(TextEnum.uoEmpty.tr),
            for (final p in data.$3.periods)
              Card(
                child: ListTile(
                  title: SelectableText(p.ownerUserId),
                  subtitle: Text(
                    '${p.share}%\n${p.startAt} → ${p.endAt ?? TextEnum.uoPresent.tr}\n${p.notes ?? ''}',
                  ),
                ),
              ),
            for (final t in data.$3.transfers)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        '${TextEnum.uoTransfer.tr} · ${t.share}%',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      SelectableText(
                        '${TextEnum.uoFrom.tr}: ${t.sourceOwnerUserId}\n${TextEnum.uoTo.tr}: ${t.recipientUserId}',
                      ),
                      Text('${t.effectiveAt}\n${t.reason}'),
                      if (t.reference?.isNotEmpty == true) Text(t.reference!),
                      OutlinedButton(
                        onPressed: () => open(
                          TransferFilesPage(
                            buildingId: buildingId,
                            unitId: unitId,
                            transferId: t.id,
                          ),
                        ),
                        child: Text(TextEnum.uoDocuments.tr),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    ),
  );
}
