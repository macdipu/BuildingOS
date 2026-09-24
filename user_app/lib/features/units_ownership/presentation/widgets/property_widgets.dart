import 'dart:math';
import 'package:flutter/material.dart';
import 'package:customer/res/strings/string_enum.dart';
import '../property_text.dart';

String operationId() {
  final random = Random.secure();
  final bytes = List<int>.generate(16, (_) => random.nextInt(256));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  final s = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
  return '${s.substring(0, 8)}-${s.substring(8, 12)}-${s.substring(12, 16)}-${s.substring(16, 20)}-${s.substring(20)}';
}

/// New loads replace the previous future; no stale records are shown after access fails.
class PropertyLoad<T> extends StatefulWidget {
  const PropertyLoad({super.key, required this.load, required this.builder});
  final Future<T> Function() load;
  final Widget Function(T, VoidCallback) builder;
  @override
  State<PropertyLoad<T>> createState() => _PropertyLoadState<T>();
}

class _PropertyLoadState<T> extends State<PropertyLoad<T>> {
  late Future<T> future;
  @override
  void initState() {
    super.initState();
    future = load();
  }

  Future<T> load() {
    final result = Future<T>.sync(widget.load);
    // Attach immediately: a failed refresh can complete before the next frame
    // attaches FutureBuilder to the new future. Keep the original for rendering.
    result.then<void>((_) {}, onError: (Object _, StackTrace __) {});
    return result;
  }

  void reload() => setState(() {
    future = load();
  });
  @override
  Widget build(BuildContext context) => FutureBuilder<T>(
    future: future,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done)
        return const Center(child: CircularProgressIndicator());
      if (snapshot.hasError)
        return PropertyError(error: snapshot.error, retry: reload);
      return widget.builder(snapshot.data as T, reload);
    },
  );
}

class PropertyError extends StatelessWidget {
  const PropertyError({super.key, required this.error, required this.retry});
  final Object? error;
  final VoidCallback retry;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.all(24),
    child: Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(propertyError(error)),
        const SizedBox(height: 12),
        OutlinedButton(onPressed: retry, child: Text(TextEnum.uoRetry.tr)),
      ],
    ),
  );
}

class PropertyAction extends StatefulWidget {
  const PropertyAction({
    super.key,
    required this.label,
    required this.action,
    this.onSuccess,
  });
  final String label;
  final Future<void> Function() action;
  final VoidCallback? onSuccess;
  @override
  State<PropertyAction> createState() => _PropertyActionState();
}

class _PropertyActionState extends State<PropertyAction> {
  bool busy = false;
  Object? error;
  Future<void> run() async {
    if (busy) return;
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await widget.action();
      if (mounted) widget.onSuccess?.call();
    } catch (e) {
      if (mounted) setState(() => error = e);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      FilledButton(
        onPressed: busy ? null : run,
        child: busy
            ? const SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(strokeWidth: 2),
              )
            : Text(widget.label),
      ),
      if (error != null)
        Padding(
          padding: const EdgeInsets.all(8),
          child: Text(propertyError(error)),
        ),
    ],
  );
}

Future<bool> confirmProperty(BuildContext context, String text) async =>
    await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(TextEnum.uoConfirm.tr),
        content: Text(text),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(TextEnum.cancel.tr),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(context, true),
            child: Text(TextEnum.uoConfirm.tr),
          ),
        ],
      ),
    ) ??
    false;
String? requiredText(String? value) =>
    value == null || value.trim().isEmpty ? TextEnum.errRequired.tr : null;
String? decimalText(
  String? value, {
  bool optional = false,
  bool zero = false,
  int precision = 2,
}) {
  final s = value?.trim() ?? '';
  if (s.isEmpty && optional) return null;
  final n = num.tryParse(s);
  if (!RegExp('^\\d+(?:\\.\\d{1,$precision})?\$').hasMatch(s) ||
      n == null ||
      !n.isFinite ||
      (zero ? n < 0 : n <= 0))
    return TextEnum.errInvalidRequest.tr;
  return null;
}

Widget propertyField(
  TextEditingController controller,
  String label, {
  String? Function(String?)? validator,
  TextInputType? keyboard,
  int? maxLength,
}) => Padding(
  padding: const EdgeInsets.symmetric(vertical: 8),
  child: TextFormField(
    controller: controller,
    decoration: InputDecoration(
      labelText: label,
      border: const OutlineInputBorder(),
    ),
    validator: validator,
    keyboardType: keyboard,
    maxLength: maxLength,
  ),
);
Widget propertyDropdown(
  String label,
  String? value,
  List<String> options,
  void Function(String?) onChanged,
) => Padding(
  padding: const EdgeInsets.symmetric(vertical: 8),
  child: DropdownButtonFormField<String>(
    initialValue: options.contains(value) ? value : null,
    decoration: InputDecoration(
      labelText: label,
      border: const OutlineInputBorder(),
    ),
    items: options
        .map((s) => DropdownMenuItem(value: s, child: Text(propertyLabel(s))))
        .toList(),
    onChanged: onChanged,
    validator: requiredText,
  ),
);
