import 'package:flutter/material.dart';

// =========================================================
// ADAPTIVE COLOR CLASS
// =========================================================

class AdaptiveColor {
  final Color light;
  final Color dark;

  const AdaptiveColor({required this.light, required this.dark});

  Color resolve(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return isDark ? dark : light;
  }

  // Convenience method for cleaner syntax
  Color call(BuildContext context) => resolve(context);

  // Add opacity support
  AdaptiveColor withAlpha(double opacity) {
    return AdaptiveColor(
      light: light.withValues(alpha: opacity),
      dark: dark.withValues(alpha: opacity),
    );
  }
}

// =========================================================
// APP COLORS - ORGANIZED & MAINTAINABLE
// =========================================================

class AppColors {
  AppColors._(); // Private constructor to prevent instantiation

  // =========================================================
  // BRAND COLORS
  // Light values: Stitch "Proptech Enterprise" tokens
  // (agentic/data/project-context/ui/stitch/design-system/DESIGN.md). DESIGN.md has no
  // dark spec (BOS-011 Q-03): dark values use its *-fixed tokens where defined, else the
  // Material 3 dark scheme seeded from primary 0xFF004AC6.
  // =========================================================

  static const Color brandPrimary = Color(0xFF004AC6);
  static const Color brandSecondary = Color(0xFF565E74);
  static const Color brandAccent = Color(0xFF2563EB);
  static const Color brandGreen = Color(0xFF006243);
  static const Color brandGray = Color(0xFFE5EEFF);

  // =========================================================
  // MATERIAL 3 SYSTEM COLORS - ADAPTIVE
  // =========================================================

  // Primary
  static const primary = AdaptiveColor(
    light: Color(0xFF004AC6),
    dark: Color(0xFFB4C5FF),
  );

  static const onPrimary = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF00174B),
  );

  static const primaryContainer = AdaptiveColor(
    light: Color(0xFF2563EB),
    dark: Color(0xFF003EA8),
  );

  static const onPrimaryContainer = AdaptiveColor(
    light: Color(0xFFEEEFFF),
    dark: Color(0xFFDBE1FF),
  );

  // Secondary
  static const secondary = AdaptiveColor(
    light: Color(0xFF565E74),
    dark: Color(0xFFBEC6E0),
  );

  static const onSecondary = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF131B2E),
  );

  static const secondaryContainer = AdaptiveColor(
    light: Color(0xFFDAE2FD),
    dark: Color(0xFF3F465C),
  );

  static const onSecondaryContainer = AdaptiveColor(
    light: Color(0xFF5C647A),
    dark: Color(0xFFDAE2FD),
  );

  // Tertiary (Accent)
  static const tertiary = AdaptiveColor(
    light: Color(0xFF006243),
    dark: Color(0xFF68DBA9),
  );

  static const onTertiary = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF002114),
  );

  static const tertiaryContainer = AdaptiveColor(
    light: Color(0xFF007D57),
    dark: Color(0xFF005137),
  );

  static const onTertiaryContainer = AdaptiveColor(
    light: Color(0xFFBDFFDC),
    dark: Color(0xFF85F8C4),
  );

  // Error
  static const error = AdaptiveColor(
    light: Color(0xFFBA1A1A),
    dark: Color(0xFFFFB4AB),
  );

  static const onError = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF690005),
  );

  static const errorContainer = AdaptiveColor(
    light: Color(0xFFFFDAD6),
    dark: Color(0xFF93000A),
  );

  static const onErrorContainer = AdaptiveColor(
    light: Color(0xFF93000A),
    dark: Color(0xFFFFDAD6),
  );

  // Surface
  static const surface = AdaptiveColor(
    light: Color(0xFFF8F9FF),
    dark: Color(0xFF121318),
  );

  static const onSurface = AdaptiveColor(
    light: Color(0xFF0B1C30),
    dark: Color(0xFFE3E2E9),
  );

  static const onSurfaceVariant = AdaptiveColor(
    light: Color(0xFF434655),
    dark: Color(0xFFC5C6D0),
  );

  // Layered surface tiers - low to high emphasis, for card/dialog/sheet
  // hierarchy so dark mode reads as soft stacked panels, not flat black.
  static const surfaceContainerLowest = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF0D0E13),
  );

  static const surfaceContainerLow = AdaptiveColor(
    light: Color(0xFFEFF4FF),
    dark: Color(0xFF1A1B21),
  );

  static const surfaceContainer = AdaptiveColor(
    light: Color(0xFFE5EEFF),
    dark: Color(0xFF1E1F25),
  );

  static const surfaceContainerHigh = AdaptiveColor(
    light: Color(0xFFDCE9FF),
    dark: Color(0xFF292A2F),
  );

  static const surfaceContainerHighest = AdaptiveColor(
    light: Color(0xFFD3E4FE),
    dark: Color(0xFF34343A),
  );

  // Background
  static const background = AdaptiveColor(
    light: Color(0xFFF8F9FF),
    dark: Color(0xFF121318),
  );

  static const onBackground = AdaptiveColor(
    light: Color(0xFF0B1C30),
    dark: Color(0xFFE3E2E9),
  );

  // Outlines
  static const outline = AdaptiveColor(
    light: Color(0xFF737686),
    dark: Color(0xFF8F909A),
  );

  static const outlineVariant = AdaptiveColor(
    light: Color(0xFFC3C6D7),
    dark: Color(0xFF45464F),
  );

  // Shadow & Scrim
  static const shadow = AdaptiveColor(
    light: Colors.black,
    dark: Colors.black,
  );

  static const scrim = AdaptiveColor(
    light: Colors.black,
    dark: Colors.black,
  );

  // Inverse
  static const inverseSurface = AdaptiveColor(
    light: Color(0xFF213145),
    dark: Color(0xFFE3E2E9),
  );

  static const onInverseSurface = AdaptiveColor(
    light: Color(0xFFEAF1FF),
    dark: Color(0xFF2F3036),
  );

  static const inversePrimary = AdaptiveColor(
    light: Color(0xFFB4C5FF),
    dark: Color(0xFF004AC6),
  );

  // =========================================================
  // SEMANTIC COLORS - ADAPTIVE
  // =========================================================

  static const success = AdaptiveColor(
    light: Color(0xFF059669),
    dark: Color(0xFF34D399),
  );

  static const onSuccess = AdaptiveColor(
    light: Colors.white,
    dark: Color(0xFF002114),
  );

  static const successContainer = AdaptiveColor(
    light: Color(0xFFECFDF5),
    dark: Color(0xFF064E3B),
  );

  static const onSuccessContainer = AdaptiveColor(
    light: Color(0xFF065F46),
    dark: Color(0xFFD1FAE5),
  );

  static const warning = AdaptiveColor(
    light: Color(0xFFD97706),
    dark: Color(0xFFFBBF24),
  );

  static const onWarning = AdaptiveColor(
    light: Colors.black,
    dark: Colors.black,
  );

  static const warningContainer = AdaptiveColor(
    light: Color(0xFFFFFBEB),
    dark: Color(0xFF78350F),
  );

  static const onWarningContainer = AdaptiveColor(
    light: Color(0xFF92400E),
    dark: Color(0xFFFEF3C7),
  );

  static const info = AdaptiveColor(
    light: Color(0xFF3B82F6),
    dark: Color(0xFF60A5FA),
  );

  static const onInfo = AdaptiveColor(
    light: Colors.white,
    dark: Colors.black,
  );

  static const infoContainer = AdaptiveColor(
    light: Color(0xFFE6F0FF),
    dark: Color(0xFF1E3A66),
  );

  static const onInfoContainer = AdaptiveColor(
    light: Color(0xFF0A2E5C),
    dark: Color(0xFFE6F0FF),
  );

  // =========================================================
  // COMMON UI COLORS - ADAPTIVE
  // =========================================================

  static const red = AdaptiveColor(
    light: Color(0xFFEF4444),
    dark: Color(0xFFF87171),
  );

  static const orange = AdaptiveColor(
    light: Color(0xFFF97316),
    dark: Color(0xFFFB923C),
  );

  static const yellow = AdaptiveColor(
    light: Color(0xFFEAB308),
    dark: Color(0xFFFACC15),
  );

  static const green = AdaptiveColor(
    light: Color(0xFF22C55E),
    dark: Color(0xFF4ADE80),
  );

  static const blue = AdaptiveColor(
    light: Color(0xFF3B82F6),
    dark: Color(0xFF60A5FA),
  );

  static const purple = AdaptiveColor(
    light: Color(0xFF9333EA),
    dark: Color(0xFFA78BFA),
  );

  static const pink = AdaptiveColor(
    light: Color(0xFFEC4899),
    dark: Color(0xFFF472B6),
  );

  static const cyan = AdaptiveColor(
    light: Color(0xFF06B6D4),
    dark: Color(0xFF22D3EE),
  );

  static const indigo = AdaptiveColor(
    light: Color(0xFF6366F1),
    dark: Color(0xFF818CF8),
  );

  static const emerald = AdaptiveColor(
    light: Color(0xFF10B981),
    dark: Color(0xFF34D399),
  );

  // =========================================================
  // TEXT COLORS - ADAPTIVE
  // =========================================================

  static const text = AdaptiveColor(
    light: Color(0xFF0B1C30),
    dark: Color(0xFFE3E2E9),
  );

  static const textSecondary = AdaptiveColor(
    light: Color(0xFF434655),
    dark: Color(0xFFC5C6D0),
  );

  static const textTertiary = AdaptiveColor(
    light: Color(0xFF9CA3AF),
    dark: Color(0xFF6B7280),
  );

  static const textDisabled = AdaptiveColor(
    light: Color(0xFFD1D5DB),
    dark: Color(0xFF4B5563),
  );

  // =========================================================
  // BORDER & DIVIDER COLORS - ADAPTIVE
  // =========================================================

  static const border = AdaptiveColor(
    light: Color(0xFFC3C6D7),
    dark: Color(0xFF45464F),
  );

  static const divider = AdaptiveColor(
    light: Color(0xFFC3C6D7),
    dark: Color(0xFF45464F),
  );

  // =========================================================
  // SPECIAL PURPOSE COLORS
  // =========================================================

  // Complaint/Report specific colors
  static const complaint = AdaptiveColor(
    light: Color(0xFFE11D48),
    dark: Color(0xFFFB7185),
  );

  static const complaintBackground = AdaptiveColor(
    light: Color(0xFFFFF1F2),
    dark: Color(0xFF4C0519),
  );

  // Hero gradients
  static const heroGradientStart = AdaptiveColor(
    light: Color(0xFF004AC6),
    dark: Color(0xFF00174B),
  );

  static const heroGradientEnd = AdaptiveColor(
    light: Color(0xFF2563EB),
    dark: Color(0xFF003EA8),
  );

  // =========================================================
  // STATIC GRADIENT COLORS (for complex gradients)
  // =========================================================

  static const Color cyan50 = Color(0xFFECFEFF);
  static const Color blue50 = Color(0xFFEFF6FF);
  static const Color cyan500 = Color(0xFF06B6D4);
  static const Color cyan600 = Color(0xFF0891B2);
  static const Color blue500 = Color(0xFF3B82F6);
  static const Color blue600 = Color(0xFF2563EB);
  static const Color emerald500 = Color(0xFF10B981);
  static const Color emerald600 = Color(0xFF059669);
  static const Color green500 = Color(0xFF22C55E);
  static const Color green600 = Color(0xFF16A34A);
  static const Color purple500 = Color(0xFF9333EA);
  static const Color purple600 = Color(0xFF7C3AED);
  static const Color pink500 = Color(0xFFEC4899);
  static const Color pink600 = Color(0xFFDB2777);

  // With alpha
  static const Color cyan500Alpha30 = Color(0x4D06B6D4);
  static const Color blue600Alpha30 = Color(0x4D2563EB);
  static const Color purple500Alpha30 = Color(0x4D9333EA);
  static const Color pink500Alpha30 = Color(0x4DEC4899);
  static const Color complaint50 = Color(0xFFFFEEF0);
  static const Color complaint500 = Color(0xFFEF4444);
  static const Color complaint600 = Color(0xFFDC2626);
  static const Color complaint500Alpha20 = Color(0x33EF4444);
  static const Color complaint500Alpha30 = Color(0x4DEF4444);
  static const Color darkHeroStart = Color(0xFF00174B);
  static const Color darkHeroEnd = Color(0xFF003EA8);

  // Additional static colors for themes
  static const Color lightTertiaryContainer = Color(0xFF007D57);
  static const Color darkTertiaryContainer = Color(0xFF005137);
  static const Color darkErrorContainer = Color(0xFF93000A);
  static const Color darkSurface = Color(0xFF121318);
  static const Color darkBackground = Color(0xFF0D0E13);
  static const Color indigo500 = Color(0xFF6366F1);
}
