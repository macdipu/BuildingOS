import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/errors/failure.dart';
import 'package:customer/features/authentication/domain/entities/otp_start_result.dart';
import 'package:customer/features/authentication/domain/entities/user_info.dart';
import 'package:dartz/dartz.dart';

abstract class AuthRepository {
  Future<Either<Failure, OtpStartResult>> startOtp(PhoneNumber phoneNumber);

  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  });
}
