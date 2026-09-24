part of 'api_urls.dart';

abstract class BuildingApplicationApiUrls {
  String get myBuildingApplicationsUrl;
  String get buildingApplicationsUrl;
  String buildingApplicationUrl(String id);
  String submitBuildingApplicationUrl(String id);
  String applicationDocumentsUrl(String id);
  String applicationDocumentUrl(String id, String documentId);
}
