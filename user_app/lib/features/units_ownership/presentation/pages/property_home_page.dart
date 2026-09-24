import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';
import 'building_units_page.dart';
import 'unit_detail_page.dart';

class PropertyHomePage extends StatefulWidget {
  const PropertyHomePage({super.key});
  @override
  State<PropertyHomePage> createState() => _PropertyHomePageState();
}

class _PropertyHomePageState extends State<PropertyHomePage> {
  final claims = <String, String>{};
  Future<(List<BuildingSummary>, List<OwnedProperty>, List<OwnerInvitation>)>
  load() async {
    final buildings = await Get.find<ListMyBuildings>()();
    final properties = await Get.find<ListMyProperties>()();
    final invitations = await Get.find<ListMyInvitations>()();
    return (buildings, properties, invitations);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoBuildings.tr)),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) => ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Align(
            alignment: Alignment.centerRight,
            child: IconButton(
              tooltip: TextEnum.uoRefresh.tr,
              onPressed: reload,
              icon: const Icon(Icons.refresh),
            ),
          ),
          Text(
            TextEnum.uoBuildings.tr,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          if (data.$1.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(TextEnum.uoEmpty.tr),
            ),
          for (final b in data.$1)
            Card(
              child: ListTile(
                title: Text(b.name),
                subtitle: Text(
                  '${b.address}\n${propertyLabel(b.status)} · ${b.roles.map(propertyLabel).join(', ')}\n${TextEnum.uoOwnedCount.tr}: ${b.ownedUnitCount}',
                ),
                trailing: const Icon(Icons.chevron_right),
                onTap: () async {
                  await Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => BuildingUnitsPage(buildingId: b.id),
                    ),
                  );
                  if (mounted) reload();
                },
              ),
            ),
          const SizedBox(height: 24),
          Text(
            TextEnum.uoProperties.tr,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          if (data.$2.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(TextEnum.uoEmpty.tr),
            ),
          for (final p in data.$2)
            Card(
              child: ListTile(
                title: Text('${p.buildingName} · ${p.unitNumber}'),
                subtitle: Text(
                  '${p.floorLabel} · ${propertyLabel(p.unitType)}\n${TextEnum.uoArea.tr}: ${p.areaSqft} · ${TextEnum.uoShare.tr}: ${p.share}',
                ),
                onTap: () async {
                  await Navigator.push(
                    context,
                    MaterialPageRoute<void>(
                      builder: (_) => UnitDetailPage(
                        buildingId: p.buildingId,
                        unitId: p.unitId,
                      ),
                    ),
                  );
                  if (mounted) reload();
                },
              ),
            ),
          const SizedBox(height: 24),
          Text(
            TextEnum.uoInvitations.tr,
            style: Theme.of(context).textTheme.titleLarge,
          ),
          if (data.$3.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Text(TextEnum.uoEmpty.tr),
            ),
          for (final i in data.$3)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(i.label),
                    Text('${TextEnum.uoExpires.tr}: ${i.expiresAt}'),
                    PropertyAction(
                      key: ValueKey(i.id),
                      label: TextEnum.uoClaim.tr,
                      action: () async {
                        if (!await confirmProperty(
                          context,
                          TextEnum.uoClaimInfo.tr,
                        ))
                          return;
                        await Get.find<ClaimOwnerInvitation>()(
                          i.id,
                          claims.putIfAbsent(i.id, operationId),
                        );
                        reload();
                      },
                    ),
                  ],
                ),
              ),
            ),
        ],
      ),
    ),
  );
}
