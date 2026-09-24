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
        return ListView(
          padding: const EdgeInsets.all(16),
          children: [
            Text(u.number, style: Theme.of(context).textTheme.headlineMedium),
            Text('${u.floorLabel} · ${propertyLabel(u.type)}'),
            Text('${TextEnum.uoArea.tr}: ${u.areaSqft}'),
            if (u.bedrooms != null)
              Text('${TextEnum.uoBedrooms.tr}: ${u.bedrooms}'),
            if (u.defaultMaintenanceRate != null)
              Text('${TextEnum.uoRate.tr}: ${u.defaultMaintenanceRate}'),
            if (u.notes?.isNotEmpty == true) Text(u.notes!),
            if (data.$1.readOnly) Text(TextEnum.uoReadOnly.tr),
            IconButton(
              tooltip: TextEnum.uoRefresh.tr,
              onPressed: reload,
              icon: const Icon(Icons.refresh),
            ),
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
