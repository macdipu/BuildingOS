import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:skeletonizer/skeletonizer.dart';
import 'package:customer/core/presentation/utils/state_status.dart';
import 'package:customer/core/presentation/widgets/appbar/common_appbar.dart';
import 'package:customer/core/presentation/widgets/buttons/common_button.dart';
import 'package:customer/core/presentation/widgets/buttons/otp_resend_timer.dart';
import 'package:customer/core/presentation/widgets/pin/common_pin_input.dart';
import 'package:customer/res/routes/app_routes.dart';

import '../../widgets/intro_header.dart';
import '../controller/login_screen_controller.dart';

class LoginOtpVerifyScreen extends StatefulWidget {
  const LoginOtpVerifyScreen({super.key});

  @override
  State<LoginOtpVerifyScreen> createState() => _LoginOtpVerifyScreenState();
}

class _LoginOtpVerifyScreenState extends State<LoginOtpVerifyScreen> {
  final LoginScreenController _controller = Get.find();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        appBar: CommonAppbar(),
        body: Container(
          padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
          child: ListView(
            shrinkWrap: true,
            children: [
              const SizedBox(height: 18),
              Obx(
                () => IntroHeader(
                  title: 'OTP Verification',
                  introText: 'Please check, a verification code has been sent to '
                      '+88 ${_controller.phoneNumber.value?.withoutCountryCode ?? ''}',
                ),
              ),
              Align(
                alignment: Alignment.centerLeft,
                child: GestureDetector(
                  onTap: Get.back,
                  child: Text(
                    'Change Number',
                    style: TextStyle(
                      color: Theme.of(context).colorScheme.primary,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 16),
              _otpInput(),
              const SizedBox(height: 16),
              _verifyButton(),
            ],
          ),
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
        title: 'Verify',
        isLoading: _controller.status.value == StateStatus.loading,
        onTap: () async {
          final success = await _controller.verifyOtp();
          if (success) {
            Get.offAllNamed(AppRoutes.appShell);
          }
        },
      ),
    );
  }
}
