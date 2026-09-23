import 'package:customer/core/network/client/api_client_config.dart';

part 'authentication_api_urls.dart';

class ApiUrl implements AuthenticationApiUrls {
  ApiUrl(this._config);

  final ApiClientConfig _config;

  String get baseUrl => "${_config.baseUrl}api/";
  String get apiVersion => _config.apiVersion;

  @override
  String get otpStartUrl => "${baseUrl}v1/auth/otp/start";

  @override
  String get otpVerifyUrl => "${baseUrl}v1/auth/otp/verify";

  String get refreshTokenUrl => '${baseUrl}auth/refresh-token';
}
