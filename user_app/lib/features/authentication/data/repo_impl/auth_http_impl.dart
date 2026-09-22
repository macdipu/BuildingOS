
import 'package:dartz/dartz.dart';
import 'package:customer/core/data/http/client/api_client.dart';
import 'package:customer/core/data/http/client/base_http_repository.dart';
import 'package:customer/core/data/http/urls/api_urls.dart';
import 'package:customer/core/domain/error/api_exceptions.dart';
import 'package:customer/core/domain/error/failure.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import 'package:customer/features/authentication/domain/model/auth_login_req.dart';

import '../../domain/model/otp_start_result.dart';
import '../../domain/model/user_info.dart';
import '../../domain/repository/auth_repository.dart';

class AuthHttpImpl extends BaseHttpRepository implements AuthRepository {
  final AuthenticationApiUrls _urls;

  AuthHttpImpl(ApiClient client, this._urls) : super(client);

  @override
  Future<Either<Failure, UserInfo>> login(AuthLoginReq req) async {
    try {
      final response = await client.post(_urls.emailLoginUrl, req.toJson());
      if (response.messageCode == 200) {
        return Right(UserInfo.fromApiJson(response.response as Map<String, dynamic>));
      } else {
        return Left(ServerFailure(response.message ?? 'Login failed'));
      }
    } catch (e) {
      return Left(ConnectionFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, OtpStartResult>> startOtp(PhoneNumber phoneNumber) async {
    try {
      final response = await client.post(_urls.otpStartUrl, {'phone': phoneNumber.withoutCountryCode});
      if (response.messageCode == 200) {
        final data = (response.response as Map<String, dynamic>)['data'] as Map<String, dynamic>;
        return Right(OtpStartResult(
          attemptId: data['attemptId'] as String,
          expiresAt: data['expiresAt'] as String,
        ));
      } else {
        return Left(ServerFailure(response.message ?? 'Failed to send OTP'));
      }
    } catch (e) {
      return Left(ConnectionFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  }) async {
    try {
      final response = await client.post(_urls.otpVerifyUrl, {
        'attemptId': attemptId,
        'phone': phoneNumber.withoutCountryCode,
        'code': code,
      });
      if (response.messageCode == 200) {
        final data = (response.response as Map<String, dynamic>)['data'] as Map<String, dynamic>;
        return Right(UserInfo.fromOtpVerifyJson(data));
      } else {
        return Left(ServerFailure(response.message ?? 'OTP verification failed'));
      }
    } on UnauthorizedException catch (e) {
      final body = e.body;
      final code = body is Map && body['code'] is String ? body['code'] as String : 'OTP_VERIFICATION_FAILED';
      return Left(ServerFailure(code));
    } catch (e) {
      return Left(ConnectionFailure(e.toString()));
    }
  }

  @override
  Future<void> jwtUpdated() async {
    await client.setToken();
  }
}
