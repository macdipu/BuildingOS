import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:customer/core/domain/models/password.dart';
import 'package:customer/core/presentation/controllers/base_controller.dart';
import 'package:customer/core/presentation/controllers/locale_controller.dart';
import 'package:customer/features/authentication/domain/model/auth_login_req.dart';

import '../../../../../core/presentation/widgets/snackbar/custom_snackbar.dart';
import '../../../domain/model/otp_start_result.dart';
import '../../../domain/use_case/do_login_use_case.dart';
import '../../../domain/use_case/start_otp_use_case.dart';
import '../../../domain/use_case/verify_otp_use_case.dart';
import '../../../../../core/domain/models/phone_number.dart';

class LoginScreenController extends BaseController {
  final DoLoginUseCase loginUseCase = Get.find<DoLoginUseCase>();
  final StartOtpUseCase startOtpUseCase = Get.find<StartOtpUseCase>();
  final VerifyOtpUseCase verifyOtpUseCase = Get.find<VerifyOtpUseCase>();
  final LocaleController _localeController = Get.find<LocaleController>();

  final phoneNumberController = TextEditingController();
  final pinController = TextEditingController();
  final otpController = TextEditingController();

  final RxBool obscureText = true.obs;
  final Rxn<PhoneNumber> phoneNumber = Rxn<PhoneNumber>();
  final Rxn<String> otpAttemptId = Rxn<String>();

  String get currentLangCode => _localeController.currentLangCode.value;

  List<Function> get devAutoFill {
    assert(() {
      phoneNumberController.text = '01521583534';
      pinController.text = '123458';
      return true;
    }());
    return [];
  }

  void toggleLocale() => _localeController.toggleLocale();

  void toggleObscureText() => obscureText.value = !obscureText.value;

  Future<bool> login() async {
    if (!await _checkingValidations()) return false;

    bool succeeded = false;
    await doAction<bool>(
      action: () => loginUseCase(
        AuthLoginReq(
          phoneNumber: PhoneNumber(phoneNumberController.text),
          password: Password(pinController.text),
        ),
      ),
      onSuccess: (_) => succeeded = true,
    );
    return succeeded;
  }

  Future<bool> requestOtp() async {
    final phone = _validatedPhone();
    if (phone == null) return false;
    phoneNumber.value = phone;

    bool succeeded = false;
    await doAction<OtpStartResult>(
      action: () => startOtpUseCase(phone),
      onSuccess: (result) {
        otpAttemptId.value = result.attemptId;
        succeeded = true;
      },
      onError: (message) => CustomSnackbar.error(message ?? 'Failed to send OTP'),
    );
    return succeeded;
  }

  Future<bool> verifyOtp() async {
    final phone = phoneNumber.value;
    final attemptId = otpAttemptId.value;
    if (phone == null || attemptId == null) {
      CustomSnackbar.error('Request an OTP first');
      return false;
    }
    if (otpController.text.isEmpty) {
      CustomSnackbar.error('OTP cannot be empty');
      return false;
    }

    bool succeeded = false;
    await doAction<bool>(
      action: () => verifyOtpUseCase(VerifyOtpParams(
        attemptId: attemptId,
        phoneNumber: phone,
        code: otpController.text,
      )),
      onSuccess: (_) => succeeded = true,
      onError: (message) => CustomSnackbar.error(otpErrorMessage(message)),
    );
    return succeeded;
  }

  PhoneNumber? _validatedPhone() {
    if (phoneNumberController.text.isEmpty) {
      CustomSnackbar.error('Phone number cannot be empty');
      return null;
    }
    try {
      return PhoneNumber(phoneNumberController.text);
    } catch (e) {
      CustomSnackbar.error(e.toString());
      return null;
    }
  }

  Future<bool> _checkingValidations() async {
    if (phoneNumberController.text.isEmpty) {
      CustomSnackbar.error('Phone number cannot be empty');
      return false;
    }

    try {
      phoneNumber.value = PhoneNumber(phoneNumberController.text);
    } catch (e) {
      CustomSnackbar.error(e.toString());
      return false;
    }

    if (pinController.text.isEmpty) {
      CustomSnackbar.error('Password cannot be empty');
      return false;
    }

    return true;
  }

  @override
  void onClose() {
    phoneNumberController.dispose();
    pinController.dispose();
    otpController.dispose();
    super.onClose();
  }
}

String otpErrorMessage(String? code) {
  switch (code) {
    case 'OTP_INVALID_CODE':
      return 'Incorrect code. Please try again.';
    case 'OTP_EXPIRED':
      return 'This code has expired. Please request a new one.';
    case 'OTP_ALREADY_CONSUMED':
      return 'This code has already been used. Please request a new one.';
    case 'OTP_ATTEMPTS_EXHAUSTED':
      return 'Too many incorrect attempts. Please request a new code.';
    case 'OTP_UNKNOWN_ATTEMPT':
      return 'OTP session not found. Please request a new code.';
    case 'OTP_PHONE_MISMATCH':
      return 'This code was not issued for this phone number.';
    default:
      return 'OTP verification failed. Please try again.';
  }
}
