import 'package:customer/app/theme/theme_extensions.dart';
import 'package:flutter/material.dart';

/// Stitch mockup brand tile (splash 01, login 02): rounded primary tile + app icon.
class BrandMark extends StatelessWidget {
  const BrandMark({super.key, this.size = 72});

  final double size;

  @override
  Widget build(BuildContext context) => Container(
        width: size,
        height: size,
        decoration: BoxDecoration(
          color: context.primaryContainer,
          borderRadius: BorderRadius.circular(size * 0.22),
        ),
        child: Icon(Icons.domain, size: size * 0.55, color: Colors.white),
      );
}
