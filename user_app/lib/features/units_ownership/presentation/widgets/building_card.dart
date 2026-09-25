import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';

import '../../domain/entities/property_models.dart';
import '../property_text.dart';

/// BRD §42 building card, laid out after Stitch mockup 04: lifecycle pill,
/// name, address, roles, owned-unit count, access warning, Open Building.
class BuildingCard extends StatelessWidget {
  const BuildingCard({super.key, required this.building, required this.onOpen});

  final BuildingSummary building;
  final VoidCallback onOpen;

  /// §42 "access warning when relevant": a suspended building is read-only.
  /// Subscription warnings are not shown here: subscriptions are per user
  /// (BOS-010 D-22), not per building.
  bool get showsAccessWarning => building.status == 'SUSPENDED';

  @override
  Widget build(BuildContext context) {
    final active = building.status == 'ACTIVE';
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              _Pill(
                label: propertyLabel(building.status),
                background: active ? context.successContainer : context.surfaceContainerHigh,
                foreground: active ? context.onSuccessContainer : context.onSurfaceVariant,
              ),
              const Spacer(),
              Icon(Icons.corporate_fare, color: context.onSurfaceVariant),
            ]),
            const SizedBox(height: 12),
            Text(building.name, style: context.headlineSmall),
            const SizedBox(height: 4),
            Row(children: [
              Icon(Icons.location_on_outlined, size: 16, color: context.onSurfaceVariant),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  building.address,
                  style: context.bodySmall?.copyWith(color: context.onSurfaceVariant),
                ),
              ),
            ]),
            const SizedBox(height: 12),
            _Fact(icon: Icons.shield_outlined, text: building.roles.map(propertyLabel).join(' & ')),
            const SizedBox(height: 4),
            _Fact(
              icon: Icons.door_front_door_outlined,
              text: '${TextEnum.uoOwnedCount.tr}: ${building.ownedUnitCount}',
            ),
            if (showsAccessWarning) ...[
              const SizedBox(height: 12),
              Container(
                key: const ValueKey('building-access-warning'),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: context.warningContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(children: [
                  Icon(Icons.lock_outline, size: 18, color: context.onWarningContainer),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      TextEnum.buildingSuspendedWarning.tr,
                      style: context.bodySmall?.copyWith(color: context.onWarningContainer),
                    ),
                  ),
                ]),
              ),
            ],
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: onOpen,
                icon: const Icon(Icons.arrow_forward),
                label: Text(TextEnum.openBuilding.tr),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _Pill extends StatelessWidget {
  const _Pill({required this.label, required this.background, required this.foreground});

  final String label;
  final Color background, foreground;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(9999)),
        child: Text(label, style: context.labelSmall?.copyWith(color: foreground)),
      );
}

class _Fact extends StatelessWidget {
  const _Fact({required this.icon, required this.text});

  final IconData icon;
  final String text;

  @override
  Widget build(BuildContext context) => Row(children: [
        Icon(icon, size: 16, color: context.onSurfaceVariant),
        const SizedBox(width: 6),
        Expanded(child: Text(text, style: context.bodyMedium)),
      ]);
}
