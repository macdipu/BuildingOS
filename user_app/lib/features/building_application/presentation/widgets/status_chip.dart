import 'package:flutter/material.dart';
import '../../domain/entities/application_enums.dart';
import '../building_application_texts.dart';

class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.status});

  final ApplicationStatus status;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final (background, foreground) = switch (status) {
      ApplicationStatus.approved => (
          scheme.primaryContainer,
          scheme.onPrimaryContainer
        ),
      ApplicationStatus.rejected => (
          scheme.errorContainer,
          scheme.onErrorContainer
        ),
      ApplicationStatus.moreInformationRequired => (
          scheme.tertiaryContainer,
          scheme.onTertiaryContainer
        ),
      _ => (scheme.surfaceContainerHighest, scheme.onSurfaceVariant),
    };
    return Chip(
      label: Text(status.label, style: TextStyle(color: foreground)),
      backgroundColor: background,
      side: BorderSide.none,
      visualDensity: VisualDensity.compact,
    );
  }
}
