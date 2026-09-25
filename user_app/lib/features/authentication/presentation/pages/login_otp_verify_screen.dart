import 'dart:async';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:skeletonizer/skeletonizer.dart';
import 'package:customer/core/utils/state_status.dart';
import 'package:customer/core/widgets/appbar/common_appbar.dart';
import 'package:customer/core/widgets/buttons/common_button.dart';
import 'package:customer/core/widgets/buttons/otp_resend_timer.dart';
import 'package:customer/core/widgets/pin/common_pin_input.dart';
import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/res/strings/string_enum.dart';

import 'package:customer/features/authentication/presentation/widgets/intro_header.dart';
import 'package:customer/features/authentication/presentation/controllers/login_screen_controller.dart';

class LoginOtpVerifyScreen extends StatefulWidget {
  const LoginOtpVerifyScreen({super.key});

  @override
  State<LoginOtpVerifyScreen> createState() => _LoginOtpVerifyScreenState();
}

class _LoginOtpVerifyScreenState extends State<LoginOtpVerifyScreen> {
  final LoginScreenController _controller = Get.find();

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: CommonAppbar(),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
          children: [
            Obx(
              () => IntroHeader(
                title: TextEnum.otpVerification.tr,
                introText: TextEnum.otpSentTo.trParams({
                  'phone':
                      '+88 ${_controller.phoneNumber.value?.withoutCountryCode ?? ''}',
                }),
              ),
            ),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: Get.back,
                style: TextButton.styleFrom(
                  minimumSize: const Size(48, 48),
                  padding: EdgeInsets.zero,
                  tapTargetSize: MaterialTapTargetSize.padded,
                ),
                child: Text(
                  TextEnum.changeNumber.tr,
                  style: const TextStyle(
                      fontSize: 14, fontWeight: FontWeight.w500),
                ),
              ),
            ),
            const SizedBox(height: 24),
            _otpInput(),
            const SizedBox(height: 24),
            _verifyButton(),
          ],
        ),
      ),
    );
  }

  Widget _otpInput() {
    return Column(
      children: [
        Obx(
          () => Skeletonizer(
            enabled: _controller.status.value == StateStatus.loading,
            child: CommonPinInputField(
              controller: _controller.otpController,
              onChanged: (_) {},
            ),
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            OtpResendButton(
              controllerTag: 'login_otp_verify',
              duration: 60,
              onResend: () async {
                await _controller.requestOtp();
              },
            ),
          ],
        ),
      ],
    );
  }

  Widget _verifyButton() {
    return Obx(
      () => CommonButton.elevated(
        title: TextEnum.verify.tr,
        isLoading: _controller.status.value == StateStatus.loading,
        onTap: () async {
          final success = await _controller.verifyOtp();
          if (success) {
            unawaited(Get.offAllNamed(AppRoutes.splash));
          }
        },
      ),
    );
  }
}
