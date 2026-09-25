import 'ownership_page.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';
import 'unit_form_page.dart';

class UnitDetailPage extends StatelessWidget {
  const UnitDetailPage({
    super.key,
    required this.buildingId,
    required this.unitId,
  });
  final String buildingId, unitId;
  Future<(BuildingAccess, PropertyUnit)> load() async => (
    await Get.find<GetBuildingAccess>()(buildingId),
    await Get.find<GetPropertyUnit>()(buildingId, unitId),
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoUnits.tr)),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) {
        final u = data.$2;
        final text = Theme.of(context).textTheme;
        final muted = Theme.of(context).colorScheme.onSurfaceVariant;
        // Mockup 12: header card (number + facts), then specifications. Tenant,
        // lease, rent and maintenance tabs arrive with BOS-003/004.
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: Text(u.number, style: text.headlineLarge)),
                        IconButton(
                          tooltip: TextEnum.uoRefresh.tr,
                          onPressed: reload,
                          icon: const Icon(Icons.refresh),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Icon(Icons.apartment, size: 16, color: muted),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            [
                              propertyLabel(u.type),
                              '${u.areaSqft} ${TextEnum.uoSqft.tr}',
                              u.floorLabel,
                            ].join(' • '),
                            style: text.bodyMedium?.copyWith(color: muted),
                          ),
                        ),
                      ],
                    ),
                    if (data.$1.readOnly) ...[
                      const SizedBox(height: 8),
                      Text(TextEnum.uoReadOnly.tr, style: text.bodySmall),
                    ],
                  ],
                ),
              ),
            ),
            Card(
              child: Column(
                children: [
                  ListTile(
                    leading: const Icon(Icons.tune),
                    title: Text(TextEnum.uoSpecifications.tr, style: text.titleMedium),
                  ),
                  ListTile(
                    dense: true,
                    title: Text(TextEnum.uoArea.tr),
                    trailing: Text('${u.areaSqft}'),
                  ),
                  if (u.bedrooms != null)
                    ListTile(
                      dense: true,
                      title: Text(TextEnum.uoBedrooms.tr),
                      trailing: Text('${u.bedrooms}'),
                    ),
                  if (u.defaultMaintenanceRate != null)
                    ListTile(
                      dense: true,
                      title: Text(TextEnum.uoRate.tr),
                      trailing: Text('${u.defaultMaintenanceRate}'),
                    ),
                  if (u.notes?.isNotEmpty == true)
                    ListTile(dense: true, title: Text(u.notes!)),
                ],
              ),
            ),
            const SizedBox(height: 8),
            OutlinedButton(
              onPressed: () async {
                await Navigator.push(
                  context,
                  MaterialPageRoute<void>(
                    builder: (_) => OwnershipPage(
                      buildingId: buildingId,
                      unitId: unitId,
                      unitNumber: u.number,
                    ),
                  ),
                );
                if (context.mounted) reload();
              },
              child: Text(TextEnum.uoOwnership.tr),
            ),
            const SizedBox(height: 8),
            if (data.$1.canEditUnits)
              PropertyAction(
                label: TextEnum.uoEditUnit.tr,
                action: () async {
                  final floors = await Get.find<ListBuildingFloors>()(
                    buildingId,
                  );
                  if (!context.mounted) return;
                  await Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => UnitFormPage(
                        buildingId: buildingId,
                        floors: floors,
                        unit: u,
                      ),
                    ),
                  );
                  if (context.mounted) reload();
                },
              ),
          ],
        );
      },
    ),
  );
}
