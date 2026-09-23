import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/usecases/usecase.dart';
import 'package:customer/features/authentication/domain/repositories/auth_repository.dart';
import 'package:dartz/dartz.dart';

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
