import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/errors/api_exceptions.dart';
import 'package:customer/core/network/client/base_http_repository.dart';
import 'package:customer/core/network/client/resource.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:customer/features/authentication/data/models/otp_start_result_model.dart';
import 'package:customer/features/authentication/data/models/user_info_model.dart';

class AuthRemoteDataSource extends BaseHttpRepository {
  final AuthenticationApiUrls _urls;

  AuthRemoteDataSource(super.client, this._urls);

  Future<OtpStartResultModel> startOtp(PhoneNumber phoneNumber) async {
    final response = await client
        .post(_urls.otpStartUrl, {'phone': phoneNumber.withoutCountryCode});
    if (response.messageCode == 429) {
      throw const ServerException('OTP_RATE_LIMITED');
    }
    if (response.messageCode != 200) {
      throw ServerException(response.message ?? 'Failed to send OTP');
    }
    return OtpStartResultModel.fromJson(_data(response));
  }

  Future<UserInfoModel> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  }) async {
    final Resource response;
    try {
      response = await client.post(_urls.otpVerifyUrl, {
        'attemptId': attemptId,
        'phone': phoneNumber.withoutCountryCode,
        'code': code,
      });
    } on UnauthorizedException catch (e) {
      final body = e.body;
      throw ServerException(body is Map && body['code'] is String
          ? body['code'] as String
          : 'OTP_VERIFICATION_FAILED');
    }
    if (response.messageCode != 200) {
      throw ServerException(response.message ?? 'OTP verification failed');
    }
    return UserInfoModel.fromOtpVerifyJson(_data(response));
  }

  Future<void> reloadToken() => client.setToken();

  Map<String, dynamic> _data(Resource response) =>
      (response.response as Map<String, dynamic>)['data']
          as Map<String, dynamic>;
}
