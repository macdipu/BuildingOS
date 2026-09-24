import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';
import 'unit_form_page.dart';
import 'unit_detail_page.dart';
import 'members_page.dart';
import 'batch_units_page.dart';

class BuildingUnitsPage extends StatelessWidget {
  const BuildingUnitsPage({super.key, required this.buildingId});
  final String buildingId;
  Future<(BuildingAccess, List<BuildingFloor>, List<PropertyUnit>)>
  load() async {
    final access = await Get.find<GetBuildingAccess>()(buildingId);
    final floors = await Get.find<ListBuildingFloors>()(buildingId);
    final units = await Get.find<ListBuildingUnits>()(buildingId);
    return (access, floors, units);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoUnits.tr)),
    body: PropertyLoad(
      load: load,
      builder: (data, reload) {
        final access = data.$1;
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
            Text(access.name, style: Theme.of(context).textTheme.headlineSmall),
            Text(propertyLabel(access.status)),
            if (access.readOnly) Text(TextEnum.uoReadOnly.tr),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                IconButton(
                  tooltip: TextEnum.uoRefresh.tr,
                  onPressed: reload,
                  icon: const Icon(Icons.refresh),
                ),
                if (access.canEditUnits) ...[
                  OutlinedButton(
                    onPressed: () =>
                        open(FloorFormPage(buildingId: buildingId)),
                    child: Text(TextEnum.uoCreateFloor.tr),
                  ),
                  OutlinedButton(
                    onPressed: data.$2.isEmpty
                        ? null
                        : () => open(
                            UnitFormPage(
                              buildingId: buildingId,
                              floors: data.$2,
                            ),
                          ),
                    child: Text(TextEnum.uoCreateUnit.tr),
                  ),
                  OutlinedButton(
                    onPressed: data.$2.isEmpty
                        ? null
                        : () => open(
                            BatchUnitsPage(
                              buildingId: buildingId,
                              floors: data.$2,
                            ),
                          ),
                    child: Text(TextEnum.uoBatch.tr),
                  ),
                ],
                if (access.manageMembers)
                  OutlinedButton(
                    onPressed: () => open(MembersPage(buildingId: buildingId)),
                    child: Text(TextEnum.uoMembers.tr),
                  ),
              ],
            ),
            if (data.$2.isEmpty && access.canEditUnits)
              Text(TextEnum.uoNeedFloor.tr),
            if (access.manageUnits) ...[
              const SizedBox(height: 16),
              Text(
                TextEnum.uoFloors.tr,
                style: Theme.of(context).textTheme.titleLarge,
              ),
              for (final f in data.$2)
                ListTile(
                  title: Text(f.label),
                  subtitle: Text(propertyLabel(f.kind)),
                  trailing: access.canEditUnits
                      ? IconButton(
                          tooltip: TextEnum.uoEditFloor.tr,
                          icon: const Icon(Icons.edit_outlined),
                          onPressed: () => open(
                            FloorFormPage(buildingId: buildingId, floor: f),
                          ),
                        )
                      : null,
                ),
            ],
            const SizedBox(height: 16),
            Text(
              TextEnum.uoUnits.tr,
              style: Theme.of(context).textTheme.titleLarge,
            ),
            if (data.$3.isEmpty) Text(TextEnum.uoEmpty.tr),
            for (final u in data.$3)
              Card(
                child: ListTile(
                  title: Text(u.number),
                  subtitle: Text(
                    '${u.floorLabel} · ${propertyLabel(u.type)}\n${TextEnum.uoArea.tr}: ${u.areaSqft}',
                  ),
                  trailing: const Icon(Icons.chevron_right),
                  onTap: () => open(
                    UnitDetailPage(buildingId: buildingId, unitId: u.id),
                  ),
                ),
              ),
          ],
        );
      },
    ),
  );
}
