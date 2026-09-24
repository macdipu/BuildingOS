import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';

Future<bool> confirm(BuildContext context,
    {required String message, required String confirmLabel}) async {
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      content: Text(message),
      actions: [
        TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: Text(TextEnum.cancel.tr)),
        FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: Text(confirmLabel)),
      ],
    ),
  );
  return confirmed ?? false;
}
