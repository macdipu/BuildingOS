import 'package:customer/app/theme/theme_extensions.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../../domain/entities/property_models.dart';
import '../../domain/usecases/property_use_cases.dart';
import '../property_text.dart';
import '../widgets/property_widgets.dart';
import '../widgets/unit_list_controls.dart';
import 'unit_form_page.dart';
import 'unit_detail_page.dart';
import 'members_page.dart';
import 'batch_units_page.dart';

/// BRD §48 unit list, laid out after Stitch mockup 10: search, filter (type,
/// floor, owner), sort, unit rows, `+ Add Unit`.
class BuildingUnitsPage extends StatefulWidget {
  const BuildingUnitsPage({super.key, required this.buildingId});
  final String buildingId;

  @override
  State<BuildingUnitsPage> createState() => _BuildingUnitsPageState();
}

typedef _UnitsData = (BuildingAccess, List<BuildingFloor>, List<PropertyUnit>, List<BuildingMember>);

class _BuildingUnitsPageState extends State<BuildingUnitsPage> {
  UnitListQuery query = const UnitListQuery();
  final search = TextEditingController();

  String get buildingId => widget.buildingId;

  Future<_UnitsData> load() async {
    final access = await Get.find<GetBuildingAccess>()(buildingId);
    final floors = await Get.find<ListBuildingFloors>()(buildingId);
    final units = await Get.find<ListBuildingUnits>()(buildingId, query);
    // Owner filter is for admins; owners only ever see their own units.
    final members = access.manageMembers
        ? await Get.find<ListBuildingMembers>()(buildingId)
        : const <BuildingMember>[];
    return (access, floors, units, members);
  }

  void apply(UnitListQuery next) => setState(() => query = next);

  @override
  void dispose() {
    search.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(TextEnum.uoUnits.tr)),
    body: PropertyLoad<_UnitsData>(
      key: ValueKey(query),
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

        final owners = data.$4.where((m) => m.role == 'OWNER' && m.status == 'ACTIVE').toList();
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(access.name, style: context.headlineSmall),
            Text(
              propertyLabel(access.status),
              style: context.bodySmall?.copyWith(color: context.onSurfaceVariant),
            ),
            if (access.readOnly) Text(TextEnum.uoReadOnly.tr),
            const SizedBox(height: 12),
            UnitSearchBar(
              controller: search,
              filtersActive: query.hasFilters,
              onSearch: (text) => apply(query.copyWith(search: () => text.trim().isEmpty ? null : text.trim())),
              onFilter: () async {
                final next = await showUnitFilterSheet(
                  context,
                  query: query,
                  floors: data.$2,
                  owners: owners,
                );
                if (next != null) apply(next);
              },
              onSort: (sort, descending) => apply(query.copyWith(sort: sort, descending: descending)),
              sort: query.sort,
              descending: query.descending,
            ),
            const SizedBox(height: 8),
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
                  FilledButton.icon(
                    key: const ValueKey('add-unit'),
                    onPressed: data.$2.isEmpty
                        ? null
                        : () => open(
                            UnitFormPage(
                              buildingId: buildingId,
                              floors: data.$2,
                            ),
                          ),
                    icon: const Icon(Icons.add),
                    label: Text(TextEnum.uoCreateUnit.tr),
                  ),
                  OutlinedButton(
                    onPressed: () =>
                        open(FloorFormPage(buildingId: buildingId)),
                    child: Text(TextEnum.uoCreateFloor.tr),
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
              Text(TextEnum.uoFloors.tr, style: context.titleMedium),
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
              '${TextEnum.uoUnits.tr} (${data.$3.length})',
              style: context.titleMedium,
            ),
            const SizedBox(height: 8),
            if (data.$3.isEmpty) Text(TextEnum.uoEmpty.tr),
            for (final u in data.$3)
              UnitRow(
                unit: u,
                onTap: () => open(
                  UnitDetailPage(buildingId: buildingId, unitId: u.id),
                ),
              ),
          ],
        );
      },
    ),
  );
}
