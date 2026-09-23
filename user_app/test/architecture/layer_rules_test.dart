import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

const _package = 'customer';

final _directive = RegExp(
  r'''^\s*(?:import|export)\s+['"]([^'"]+)['"]''',
  multiLine: true,
);

class _SourceFile {
  _SourceFile(this.path, this.imports);

  final String path;
  final List<String> imports;
}

List<_SourceFile> _libFiles() {
  final lib = Directory('lib').absolute;
  final libPath = lib.uri.normalizePath().path;
  return lib
      .listSync(recursive: true)
      .whereType<File>()
      .where((file) => file.path.endsWith('.dart'))
      .map((file) {
    final uri = file.absolute.uri.normalizePath();
    final imports = _directive
        .allMatches(file.readAsStringSync())
        .map((match) => _resolve(match.group(1) ?? '', uri, libPath))
        .toList();
    return _SourceFile(uri.path.substring(libPath.length), imports);
  }).toList();
}

String _resolve(String target, Uri from, String libPath) {
  if (target.startsWith('package:$_package/')) {
    return target.substring('package:$_package/'.length);
  }
  if (target.contains(':')) return target;
  final resolved = from.resolve(target).normalizePath().path;
  return resolved.startsWith(libPath)
      ? resolved.substring(libPath.length)
      : resolved;
}

String? _featureOf(String path) {
  final parts = path.split('/');
  return parts.length > 2 && parts.first == 'features' ? parts[1] : null;
}

String? _layerOf(String path) {
  final parts = path.split('/');
  return parts.length > 3 && parts.first == 'features' ? parts[2] : null;
}

List<String> _violations(bool Function(_SourceFile file, String import) rule) {
  return [
    for (final file in _libFiles())
      for (final import in file.imports)
        if (rule(file, import)) '${file.path} -> $import',
  ];
}

void main() {
  test('feature domain layers are pure Dart', () {
    const forbiddenPackages = [
      'package:flutter/',
      'package:get/',
      'package:dio/'
    ];
    final violations = _violations((file, import) {
      if (_layerOf(file.path) != 'domain') return false;
      if (forbiddenPackages.any(import.startsWith)) return true;
      final layer = _layerOf(import);
      return layer == 'data' || layer == 'presentation';
    });
    expect(violations, isEmpty);
  });

  test('feature data layers never import presentation', () {
    final violations = _violations((file, import) =>
        _layerOf(file.path) == 'data' && _layerOf(import) == 'presentation');
    expect(violations, isEmpty);
  });

  test('features never import another feature', () {
    final violations = _violations((file, import) {
      final source = _featureOf(file.path);
      final target = _featureOf(import);
      return source != null && target != null && source != target;
    });
    expect(violations, isEmpty);
  });

  test('core never imports features', () {
    final violations = _violations((file, import) =>
        file.path.startsWith('core/') && import.startsWith('features/'));
    expect(violations, isEmpty);
  });

  test('core network and database never import app or features', () {
    final violations = _violations((file, import) =>
        (file.path.startsWith('core/network/') ||
            file.path.startsWith('core/database/')) &&
        (import.startsWith('app/') || import.startsWith('features/')));
    expect(violations, isEmpty);
  });
}
