import 'package:customer/app/theme/app_dimensions.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';

class ErrorRetry extends StatelessWidget {
  const ErrorRetry({super.key, required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Padding(
        padding: AppDimens.spacing.p16,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(message,
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.bodyMedium),
            const SizedBox(height: 12),
            FilledButton(onPressed: onRetry, child: Text(TextEnum.retry.tr)),
          ],
        ),
      ),
    );
  }
}
