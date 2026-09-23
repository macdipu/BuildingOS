import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/usecases/usecase.dart';
import 'package:customer/features/authentication/domain/entities/otp_start_result.dart';
import 'package:customer/features/authentication/domain/repositories/auth_repository.dart';

class StartOtpUseCase extends UseCaseWithParams<OtpStartResult, PhoneNumber> {
  final AuthRepository authRepository;

  StartOtpUseCase(this.authRepository);

  @override
  ResultFuture<OtpStartResult> call(PhoneNumber params) {
    return authRepository.startOtp(params);
  }
}
