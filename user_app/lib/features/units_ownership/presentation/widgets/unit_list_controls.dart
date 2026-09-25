import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';

import '../../domain/entities/property_models.dart';
import '../property_text.dart';

const unitTypes = ['FLAT', 'PARKING', 'STORAGE', 'COMMERCIAL', 'COMMON', 'OTHER'];

String _sortLabel(UnitSortField field) => switch (field) {
      UnitSortField.unitNumber => TextEnum.uoSortNumber.tr,
      UnitSortField.floor => TextEnum.uoFloor.tr,
      UnitSortField.type => TextEnum.uoType.tr,
    };

/// Search field + filter + sort controls (mockup 10 header).
class UnitSearchBar extends StatelessWidget {
  const UnitSearchBar({
    super.key,
    required this.controller,
    required this.filtersActive,
    required this.onSearch,
    required this.onFilter,
    required this.onSort,
    required this.sort,
    required this.descending,
  });

  final TextEditingController controller;
  final bool filtersActive;
  final ValueChanged<String> onSearch;
  final VoidCallback onFilter;
  final void Function(UnitSortField, bool) onSort;
  final UnitSortField sort;
  final bool descending;

  @override
  Widget build(BuildContext context) => Row(children: [
        Expanded(
          child: TextField(
            key: const ValueKey('unit-search'),
            controller: controller,
            textInputAction: TextInputAction.search,
            onSubmitted: onSearch,
            decoration: InputDecoration(
              hintText: TextEnum.uoSearchUnits.tr,
              prefixIcon: const Icon(Icons.search),
            ),
          ),
        ),
        const SizedBox(width: 8),
        IconButton.outlined(
          key: const ValueKey('unit-filter'),
          tooltip: TextEnum.uoFilter.tr,
          isSelected: filtersActive,
          onPressed: onFilter,
          icon: const Icon(Icons.tune),
        ),
        PopupMenuButton<(UnitSortField, bool)>(
          key: const ValueKey('unit-sort'),
          tooltip: TextEnum.uoSort.tr,
          icon: const Icon(Icons.sort),
          onSelected: (value) => onSort(value.$1, value.$2),
          itemBuilder: (_) => [
            for (final field in UnitSortField.values)
              for (final desc in [false, true])
                CheckedPopupMenuItem(
                  value: (field, desc),
                  checked: field == sort && desc == descending,
                  child: Text(
                    '${_sortLabel(field)} · ${desc ? TextEnum.uoDescending.tr : TextEnum.uoAscending.tr}',
                  ),
                ),
          ],
        ),
      ]);
}

/// Returns the new query, or null when dismissed.
Future<UnitListQuery?> showUnitFilterSheet(
  BuildContext context, {
  required UnitListQuery query,
  required List<BuildingFloor> floors,
  required List<BuildingMember> owners,
}) =>
    showModalBottomSheet<UnitListQuery>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (_) => _UnitFilterSheet(query: query, floors: floors, owners: owners),
    );

class _UnitFilterSheet extends StatefulWidget {
  const _UnitFilterSheet({required this.query, required this.floors, required this.owners});

  final UnitListQuery query;
  final List<BuildingFloor> floors;
  final List<BuildingMember> owners;

  @override
  State<_UnitFilterSheet> createState() => _UnitFilterSheetState();
}

class _UnitFilterSheetState extends State<_UnitFilterSheet> {
  late String? type = widget.query.type;
  late String? floorId = widget.query.floorId;
  late String? ownerUserId = widget.query.ownerUserId;

  DropdownMenuItem<String?> get _any => DropdownMenuItem(child: Text(TextEnum.uoAny.tr));

  @override
  Widget build(BuildContext context) => Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + MediaQuery.viewInsetsOf(context).bottom),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(TextEnum.uoFilter.tr, style: context.headlineSmall),
            const SizedBox(height: 16),
            DropdownButtonFormField<String?>(
              key: const ValueKey('filter-type'),
              initialValue: type,
              decoration: InputDecoration(labelText: TextEnum.uoType.tr),
              items: [
                _any,
                for (final t in unitTypes) DropdownMenuItem(value: t, child: Text(propertyLabel(t))),
              ],
              onChanged: (v) => setState(() => type = v),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String?>(
              key: const ValueKey('filter-floor'),
              initialValue: floorId,
              decoration: InputDecoration(labelText: TextEnum.uoFloor.tr),
              items: [
                _any,
                for (final f in widget.floors) DropdownMenuItem(value: f.id, child: Text(f.label)),
              ],
              onChanged: (v) => setState(() => floorId = v),
            ),
            if (widget.owners.isNotEmpty) ...[
              const SizedBox(height: 12),
              // Members API exposes user ids only (same as the ownership form).
              DropdownButtonFormField<String?>(
                key: const ValueKey('filter-owner'),
                initialValue: ownerUserId,
                isExpanded: true,
                decoration: InputDecoration(labelText: TextEnum.uoOwner.tr),
                items: [
                  _any,
                  for (final m in widget.owners)
                    DropdownMenuItem(value: m.userId, child: Text(m.userId, overflow: TextOverflow.ellipsis)),
                ],
                onChanged: (v) => setState(() => ownerUserId = v),
              ),
            ],
            const SizedBox(height: 24),
            Row(children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => Navigator.pop(
                    context,
                    widget.query.copyWith(floorId: () => null, type: () => null, ownerUserId: () => null),
                  ),
                  child: Text(TextEnum.uoClearFilters.tr),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton(
                  key: const ValueKey('filter-apply'),
                  onPressed: () => Navigator.pop(
                    context,
                    widget.query.copyWith(
                      floorId: () => floorId,
                      type: () => type,
                      ownerUserId: () => ownerUserId,
                    ),
                  ),
                  child: Text(TextEnum.uoApply.tr),
                ),
              ),
            ]),
          ],
        ),
      );
}

/// Unit row (mockup 10): number, floor · type, area.
class UnitRow extends StatelessWidget {
  const UnitRow({super.key, required this.unit, required this.onTap});

  final PropertyUnit unit;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) => Card(
        child: ListTile(
          onTap: onTap,
          leading: CircleAvatar(
            backgroundColor: context.primaryContainer.withValues(alpha: 0.12),
            foregroundColor: context.primaryContainer,
            child: const Icon(Icons.door_front_door_outlined),
          ),
          title: Text(unit.number, style: context.titleMedium),
          subtitle: Text(
            '${unit.floorLabel} · ${propertyLabel(unit.type)}\n${TextEnum.uoArea.tr}: ${unit.areaSqft}',
          ),
          isThreeLine: true,
          trailing: const Icon(Icons.chevron_right),
        ),
      );
}
