import 'package:flutter/material.dart';

/// Stitch "Proptech Enterprise" type scale
/// (agentic/data/project-context/ui/stitch/design-system/DESIGN.md `typography`).
/// Letter spacing there is in em; converted to logical px (em * fontSize).
class AppTextTheme {
  AppTextTheme._();

  static const String displayFont = 'Plus Jakarta Sans';
  static const String bodyFont = 'Inter';

  // Inter has no Bengali glyphs; fall back to the platform Bengali font.
  static const List<String> _fallback = ['Noto Sans Bengali', 'Noto Sans'];

  static const TextStyle _display = TextStyle(fontFamily: displayFont, fontFamilyFallback: _fallback);
  static const TextStyle _body = TextStyle(fontFamily: bodyFont, fontFamilyFallback: _fallback);

  static TextTheme lightTextTheme = TextTheme(
    // display-lg
    displayLarge: _display.copyWith(fontSize: 36, fontWeight: FontWeight.w700, height: 44 / 36, letterSpacing: -0.72),
    // display-lg-mobile
    displayMedium: _display.copyWith(fontSize: 28, fontWeight: FontWeight.w700, height: 36 / 28, letterSpacing: -0.28),
    // currency-lg
    displaySmall: _display.copyWith(fontSize: 24, fontWeight: FontWeight.w700, height: 32 / 24, letterSpacing: -0.24),

    // headline-xl / headline-lg / headline-md
    headlineLarge: _display.copyWith(fontSize: 28, fontWeight: FontWeight.w600, height: 36 / 28, letterSpacing: -0.42),
    headlineMedium: _display.copyWith(fontSize: 22, fontWeight: FontWeight.w600, height: 28 / 22, letterSpacing: -0.22),
    headlineSmall: _display.copyWith(fontSize: 18, fontWeight: FontWeight.w600, height: 24 / 18, letterSpacing: -0.09),

    titleLarge: _display.copyWith(fontSize: 22, fontWeight: FontWeight.w600, height: 28 / 22, letterSpacing: -0.22),
    titleMedium: _body.copyWith(fontSize: 16, fontWeight: FontWeight.w600, height: 24 / 16),
    titleSmall: _body.copyWith(fontSize: 14, fontWeight: FontWeight.w600, height: 20 / 14),

    // label-lg / label-md / label-sm
    labelLarge: _body.copyWith(fontSize: 14, fontWeight: FontWeight.w600, height: 20 / 14),
    labelMedium: _body.copyWith(fontSize: 12, fontWeight: FontWeight.w600, height: 16 / 12, letterSpacing: 0.12),
    labelSmall: _body.copyWith(fontSize: 11, fontWeight: FontWeight.w600, height: 14 / 11, letterSpacing: 0.22),

    // body-lg / body-md / body-sm
    bodyLarge: _body.copyWith(fontSize: 16, fontWeight: FontWeight.w400, height: 24 / 16),
    bodyMedium: _body.copyWith(fontSize: 14, fontWeight: FontWeight.w400, height: 20 / 14),
    bodySmall: _body.copyWith(fontSize: 12, fontWeight: FontWeight.w400, height: 16 / 12),
  );

  static TextTheme darkTextTheme = lightTextTheme;

  /// currency-md: ledger rows. Tabular figures keep amounts aligned (DESIGN.md).
  static const TextStyle currencyMedium = TextStyle(
    fontFamily: bodyFont,
    fontFamilyFallback: _fallback,
    fontSize: 15,
    fontWeight: FontWeight.w600,
    height: 20 / 15,
    fontFeatures: [FontFeature.tabularFigures()],
  );

  /// currency-lg: headline amounts.
  static const TextStyle currencyLarge = TextStyle(
    fontFamily: displayFont,
    fontFamilyFallback: _fallback,
    fontSize: 24,
    fontWeight: FontWeight.w700,
    height: 32 / 24,
    letterSpacing: -0.24,
    fontFeatures: [FontFeature.tabularFigures()],
  );
}
