import 'package:flutter/widgets.dart';
import 'package:flutter_svg/flutter_svg.dart';

/// Renders an SVG asset through flutter_svg's cache, so a [precache]d asset
/// draws without a loading placeholder (AnyImageView bypasses this cache).
class AppSvg extends StatelessWidget {
  const AppSvg({
    super.key,
    required this.assetPath,
    this.width,
    this.height,
    this.fit = BoxFit.contain,
  });

  final String assetPath;
  final double? width;
  final double? height;
  final BoxFit fit;

  static Future<void> precache(String assetPath) async {
    await SvgAssetLoader(assetPath).loadBytes(null);
  }

  @override
  Widget build(BuildContext context) {
    return SvgPicture.asset(
      assetPath,
      width: width,
      height: height,
      fit: fit,
    );
  }
}
