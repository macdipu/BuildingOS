import 'package:customer/app/theme/app_theme.dart';
import 'package:customer/app/theme/text_theme.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Stitch light theme (BOS-011 UI-T01)', () {
    final theme = AppTheme.lightTheme;
    final scheme = theme.colorScheme;

    test('color roles match DESIGN.md tokens', () {
      const expected = <String, int>{
        'primary': 0xFF004AC6,
        'onPrimary': 0xFFFFFFFF,
        'primaryContainer': 0xFF2563EB,
        'secondary': 0xFF565E74,
        'tertiary': 0xFF006243,
        'error': 0xFFBA1A1A,
        'surface': 0xFFF8F9FF,
        'onSurface': 0xFF0B1C30,
        'onSurfaceVariant': 0xFF434655,
        'surfaceContainerLow': 0xFFEFF4FF,
        'surfaceContainerHighest': 0xFFD3E4FE,
        'outline': 0xFF737686,
        'outlineVariant': 0xFFC3C6D7,
        'inverseSurface': 0xFF213145,
      };
      final actual = <String, int>{
        'primary': scheme.primary.toARGB32(),
        'onPrimary': scheme.onPrimary.toARGB32(),
        'primaryContainer': scheme.primaryContainer.toARGB32(),
        'secondary': scheme.secondary.toARGB32(),
        'tertiary': scheme.tertiary.toARGB32(),
        'error': scheme.error.toARGB32(),
        'surface': scheme.surface.toARGB32(),
        'onSurface': scheme.onSurface.toARGB32(),
        'onSurfaceVariant': scheme.onSurfaceVariant.toARGB32(),
        'surfaceContainerLow': scheme.surfaceContainerLow.toARGB32(),
        'surfaceContainerHighest': scheme.surfaceContainerHighest.toARGB32(),
        'outline': scheme.outline.toARGB32(),
        'outlineVariant': scheme.outlineVariant.toARGB32(),
        'inverseSurface': scheme.inverseSurface.toARGB32(),
      };
      expect(actual, expected);
    });

    test('headings use Plus Jakarta Sans, body and labels use Inter', () {
      final t = theme.textTheme;
      for (final s in [t.displayLarge, t.headlineLarge, t.headlineMedium, t.headlineSmall, t.titleLarge]) {
        expect(s?.fontFamily, AppTextTheme.displayFont);
      }
      for (final s in [t.bodyLarge, t.bodyMedium, t.bodySmall, t.labelLarge, t.labelMedium, t.labelSmall]) {
        expect(s?.fontFamily, AppTextTheme.bodyFont);
      }
      expect(t.headlineMedium?.fontSize, 22);
      expect(t.headlineMedium?.fontWeight, FontWeight.w600);
      expect(t.bodyMedium?.fontSize, 14);
      expect(t.labelSmall?.fontSize, 11);
    });

    test('currency styles use tabular figures', () {
      expect(AppTextTheme.currencyMedium.fontFeatures, contains(const FontFeature.tabularFigures()));
      expect(AppTextTheme.currencyLarge.fontSize, 24);
    });
  });

  test('dark theme builds from Stitch-derived scheme', () {
    final scheme = AppTheme.darkTheme.colorScheme;
    expect(scheme.brightness, Brightness.dark);
    expect(scheme.primary.toARGB32(), 0xFFB4C5FF);
  });
}
