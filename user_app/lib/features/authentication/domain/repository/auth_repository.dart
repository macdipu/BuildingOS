import 'package:dartz/dartz.dart';
import 'package:customer/features/authentication/domain/model/auth_login_req.dart';
import '../../../../core/domain/domain_export.dart';
import '../../../../core/domain/models/phone_number.dart';
import '../model/otp_start_result.dart';
import '../model/user_info.dart';

abstract class AuthRepository {
  Future<Either<Failure, UserInfo>> login(AuthLoginReq req);

  Future<Either<Failure, OtpStartResult>> startOtp(PhoneNumber phoneNumber);

  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  });

  Future<void> jwtUpdated();
}
