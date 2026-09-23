
import 'package:dartz/dartz.dart';
import 'package:customer/core/data/cache/client/base_cache_repository.dart';
import 'package:customer/core/data/cache/preference/shared_preference_constants.dart';
import 'package:customer/core/domain/error/failure.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import '../../domain/model/auth_login_req.dart';
import '../../domain/model/otp_start_result.dart';
import '../../domain/model/user_info.dart';
import '../../domain/repository/auth_repository.dart';
import 'auth_http_impl.dart';

class AuthCacheImpl extends BaseCacheRepository implements AuthRepository {
  final AuthHttpImpl authHttpImpl;

  AuthCacheImpl(super.cache, this.authHttpImpl);

  @override
  Future<Either<Failure, UserInfo>> login(AuthLoginReq req) async {
    final Either<Failure, UserInfo> result = await authHttpImpl.login(req);
    await _persistIfSuccessful(result);
    return result;
  }

  @override
  Future<Either<Failure, OtpStartResult>> startOtp(PhoneNumber phoneNumber) {
    return authHttpImpl.startOtp(phoneNumber);
  }

  @override
  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  }) async {
    final result = await authHttpImpl.verifyOtp(
      attemptId: attemptId,
      phoneNumber: phoneNumber,
      code: code,
    );
    await _persistIfSuccessful(result);
    return result;
  }

  Future<void> _persistIfSuccessful(Either<Failure, UserInfo> result) async {
    if (result.isRight()) {
      final UserInfo? userInfo = result.fold((l) => null, (r) => r);
      await cache.forever(
          SharedPreferenceConstant.customerInfo, userInfo!.toJsonString());

      await authHttpImpl.jwtUpdated();
    }
  }

  @override
  Future<void> jwtUpdated() async {
    await authHttpImpl.jwtUpdated();
  }

}
