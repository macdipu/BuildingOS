import 'package:customer/app/startup/splash_controller.dart';
import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/core/widgets/brand/brand_mark.dart';
import 'package:customer/res/strings/string_enum.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

/// BRD §39 / mockup `ui/stitch/mobile/01-splash-screen`: logo, name, loading indicator.
class SplashScreen extends GetView<SplashController> {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: context.surface,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            children: [
              const Spacer(),
              const BrandMark(),
              const SizedBox(height: 24),
              Text(
                TextEnum.splashBrand.tr,
                style: context.displayMedium?.copyWith(color: context.onSurface),
              ),
              const SizedBox(height: 8),
              Text(
                TextEnum.splashTagline.tr,
                textAlign: TextAlign.center,
                style: context.bodyMedium?.copyWith(color: context.onSurfaceVariant),
              ),
              const Spacer(),
              ClipRRect(
                borderRadius: BorderRadius.circular(9999),
                child: LinearProgressIndicator(
                  minHeight: 4,
                  backgroundColor: context.surfaceContainerHigh,
                ),
              ),
              const SizedBox(height: 12),
              Text(
                TextEnum.splashLoading.tr,
                style: context.bodySmall?.copyWith(color: context.onSurfaceVariant),
              ),
              const SizedBox(height: 48),
            ],
          ),
        ),
      ),
    );
  }
}
