import 'dart:async';
import 'package:flutter/services.dart';
import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/core/utils/state_status.dart';
import 'package:customer/core/widgets/brand/brand_mark.dart';
import 'package:customer/core/widgets/text_field/custom_text_field.dart';
import 'package:customer/core/widgets/buttons/common_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/res/strings/string_enum.dart';

import 'package:customer/features/authentication/presentation/controllers/login_screen_controller.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final LoginScreenController _controller = Get.find();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const SizedBox(height: 8),
              _switchLocaleButton(),
              const SizedBox(height: 32),
              _branding(),
              const SizedBox(height: 40),
              _heading(),
              const SizedBox(height: 32),
              _phoneNumber(),
              const SizedBox(height: 8),
              _otpHint(),
              const SizedBox(height: 24),
              _submitButton(),
              const SizedBox(height: 24),
            ],
          ),
        ),
      ),
    );
  }

  Widget _switchLocaleButton() {
    return Align(
      alignment: Alignment.centerRight,
      child: Obx(() {
        final code = _controller.currentLangCode.isEmpty
            ? Get.locale?.languageCode ?? Get.deviceLocale?.languageCode ?? 'en'
            : _controller.currentLangCode;

        final label = code == 'bn' ? 'বাংলা' : 'English';

        return TextButton.icon(
          onPressed: _controller.toggleLocale,
          icon: Icon(
            Icons.language,
            size: 14,
            color: context.primary,
          ),
          label: Text(
            label,
            style: TextStyle(fontSize: 14, color: context.primary),
          ),
          style: TextButton.styleFrom(
            minimumSize: const Size(48, 36),
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          ),
        );
      }),
    );
  }

  // Mockup 02: brand row, then "Welcome back" + subtitle.
  Widget _branding() {
    return Row(
      children: [
        const BrandMark(size: 40),
        const SizedBox(width: 12),
        Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(TextEnum.splashBrand.tr, style: context.headlineSmall),
            Text(
              TextEnum.splashTagline.tr,
              style: context.labelSmall?.copyWith(color: context.onSurfaceVariant),
            ),
          ],
        ),
      ],
    );
  }

  Widget _heading() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(TextEnum.loginWelcome.tr, style: context.headlineLarge),
        const SizedBox(height: 8),
        Text(
          TextEnum.loginSubtitle.tr,
          style: context.bodyMedium?.copyWith(color: context.onSurfaceVariant),
        ),
      ],
    );
  }

  Widget _otpHint() {
    return Row(
      children: [
        Icon(Icons.lock_clock_outlined, size: 16, color: context.onSurfaceVariant),
        const SizedBox(width: 6),
        Expanded(
          child: Text(
            TextEnum.loginOtpHint.tr,
            style: context.bodySmall?.copyWith(color: context.onSurfaceVariant),
          ),
        ),
      ],
    );
  }

  Widget _phoneNumber() {
    return CustomTextField(
      controller: _controller.phoneNumberController,
      label: TextEnum.phoneNumber.tr,
      prefixText: '+88',
      prefixStyle: const TextStyle(fontSize: 16),
      keyboardType: TextInputType.phone,
      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
    );
  }

  Widget _submitButton() {
    return Obx(
      () => CommonButton.elevated(
        title: TextEnum.loginContinue.tr,
        isLoading: _controller.status.value == StateStatus.loading,
        onTap: () async {
          final success = await _controller.requestOtp();
          if (success) {
            unawaited(Get.toNamed(AppRoutes.loginOtpVerify));
          }
        },
      ),
    );
  }
}
