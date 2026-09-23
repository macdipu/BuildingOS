import 'package:customer/core/controllers/base_controller.dart';
import 'package:customer/core/controllers/locale_controller.dart';
import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/widgets/snackbar/custom_snackbar.dart';
import 'package:customer/features/authentication/domain/entities/otp_start_result.dart';
import 'package:customer/features/authentication/domain/usecases/start_otp_use_case.dart';
import 'package:customer/features/authentication/domain/usecases/verify_otp_use_case.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';

class LoginScreenController extends BaseController {
  final StartOtpUseCase startOtpUseCase = Get.find<StartOtpUseCase>();
  final VerifyOtpUseCase verifyOtpUseCase = Get.find<VerifyOtpUseCase>();
  final LocaleController _localeController = Get.find<LocaleController>();

  final phoneNumberController = TextEditingController();
  final otpController = TextEditingController();

  final Rxn<PhoneNumber> phoneNumber = Rxn<PhoneNumber>();
  final Rxn<String> otpAttemptId = Rxn<String>();

  String get currentLangCode => _localeController.currentLangCode.value;

  List<Function> get devAutoFill {
    assert(() {
      phoneNumberController.text = '01521583534';
      return true;
    }());
    return [];
  }

  void toggleLocale() => _localeController.toggleLocale();

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
      onError: (message) => CustomSnackbar.error(otpStartErrorMessage(message)),
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

  @override
  void onClose() {
    phoneNumberController.dispose();
    otpController.dispose();
    super.onClose();
  }
}

String otpStartErrorMessage(String? code) {
  switch (code) {
    case 'OTP_RATE_LIMITED':
      return 'Too many code requests. Please wait before trying again.';
    default:
      return code ?? 'Failed to send OTP';
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
