import 'package:flutter/material.dart';

class EnumDropdown<T extends Enum> extends StatelessWidget {
  const EnumDropdown({
    super.key,
    required this.label,
    required this.values,
    required this.value,
    required this.labelOf,
    required this.onChanged,
    this.errorText,
    this.optional = false,
  });

  final String label;
  final List<T> values;
  final T? value;
  final String Function(T) labelOf;
  final ValueChanged<T?> onChanged;
  final String? errorText;
  final bool optional;

  @override
  Widget build(BuildContext context) {
    return DropdownButtonFormField<T?>(
      initialValue: value,
      isExpanded: true,
      decoration: InputDecoration(
          labelText: label,
          errorText: errorText,
          border: const OutlineInputBorder()),
      items: [
        if (optional) DropdownMenuItem<T?>(child: const Text('—')),
        for (final item in values)
          DropdownMenuItem(value: item, child: Text(labelOf(item))),
      ],
      onChanged: onChanged,
    );
  }
}
