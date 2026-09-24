import 'package:customer/core/network/client/api_client_config.dart';

part 'authentication_api_urls.dart';
part 'building_application_api_urls.dart';

class ApiUrl implements AuthenticationApiUrls, BuildingApplicationApiUrls {
  ApiUrl(this._config);

  final ApiClientConfig _config;

  String get baseUrl => "${_config.baseUrl}api/";
  String get apiVersion => _config.apiVersion;

  @override
  String get otpStartUrl => "${baseUrl}v1/auth/otp/start";

  @override
  String get otpVerifyUrl => "${baseUrl}v1/auth/otp/verify";

  @override
  String get myBuildingApplicationsUrl => "${baseUrl}v1/me/building-applications";

  @override
  String get buildingApplicationsUrl => "${baseUrl}v1/building-applications";

  @override
  String buildingApplicationUrl(String id) => "${baseUrl}v1/building-applications/$id";

  @override
  String submitBuildingApplicationUrl(String id) => "${baseUrl}v1/building-applications/$id/submit";

  @override
  String applicationDocumentsUrl(String id) => "${baseUrl}v1/building-applications/$id/documents";

  @override
  String applicationDocumentUrl(String id, String documentId) =>
      "${baseUrl}v1/building-applications/$id/documents/$documentId";

  String get refreshTokenUrl => '${baseUrl}auth/refresh-token';
}
