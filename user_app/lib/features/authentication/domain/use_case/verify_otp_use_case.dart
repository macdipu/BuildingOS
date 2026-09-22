import 'package:dartz/dartz.dart';
import 'package:customer/core/domain/usecase/usecase.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import 'package:customer/features/authentication/domain/repository/auth_repository.dart';

class VerifyOtpParams {
  final String attemptId;
  final PhoneNumber phoneNumber;
  final String code;

  const VerifyOtpParams({
    required this.attemptId,
    required this.phoneNumber,
    required this.code,
  });
}

class VerifyOtpUseCase extends UseCaseWithParams<bool, VerifyOtpParams> {
  final AuthRepository authRepository;

  VerifyOtpUseCase(this.authRepository);

  @override
  ResultFuture<bool> call(VerifyOtpParams params) async {
    final result = await authRepository.verifyOtp(
      attemptId: params.attemptId,
      phoneNumber: params.phoneNumber,
      code: params.code,
    );
    return result.fold((l) => left(l), (r) => right(true));
  }
}
