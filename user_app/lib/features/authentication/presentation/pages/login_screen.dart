import 'dart:async';
import 'package:flutter/services.dart';
import 'package:customer/app/theme/theme_extensions.dart';
import 'package:customer/core/utils/state_status.dart';
import 'package:customer/core/widgets/images/app_svg.dart';
import 'package:customer/core/widgets/text_field/custom_text_field.dart';
import 'package:customer/core/widgets/buttons/common_button.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/res/strings/string_enum.dart';

import 'package:customer/res/resources.dart';
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
              const SizedBox(height: 16),
              _switchLocaleButton(),
              const SizedBox(height: 48),
              _branding(),
              const SizedBox(height: 24),
              _heading(),
              const SizedBox(height: 24),
              _phoneNumber(),
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

  Widget _branding() {
    return Center(
      child: AppSvg(
        assetPath: Resources.drawable.splashImage,
        height: 96,
      ),
    );
  }

  Widget _heading() {
    return Text(
      TextEnum.loginUpperText.tr,
      textAlign: TextAlign.center,
      style: Theme.of(context).textTheme.headlineSmall,
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
        title: TextEnum.next.tr,
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
