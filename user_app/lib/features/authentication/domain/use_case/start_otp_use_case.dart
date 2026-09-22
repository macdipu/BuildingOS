import 'package:customer/core/domain/usecase/usecase.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import 'package:customer/features/authentication/domain/repository/auth_repository.dart';

import '../model/otp_start_result.dart';

class StartOtpUseCase extends UseCaseWithParams<OtpStartResult, PhoneNumber> {
  final AuthRepository authRepository;

  StartOtpUseCase(this.authRepository);

  @override
  ResultFuture<OtpStartResult> call(PhoneNumber params) {
    return authRepository.startOtp(params);
  }
}
