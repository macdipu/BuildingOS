import 'dart:io';

import 'package:customer/core/errors/api_exceptions.dart';
import 'package:customer/core/network/client/base_http_repository.dart';
import 'package:customer/core/network/client/resource.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import '../../domain/entities/application_error.dart';
import '../models/application_document_model.dart';
import '../models/building_application_model.dart';

/// building-service applicant endpoints via the gateway. Non-2xx responses become [ServerException] whose message
/// is the API `code` (e.g. APPLICATION_INCOMPLETE), or DEPENDENCY_UNAVAILABLE when there is no parseable body.
class BuildingApplicationRemoteDataSource extends BaseHttpRepository {
  final BuildingApplicationApiUrls _urls;

  BuildingApplicationRemoteDataSource(super.client, this._urls);

  Future<List<BuildingApplicationModel>> listMine() async {
    final response =
        await client.authorizedGet(_urls.myBuildingApplicationsUrl);
    return _dataList(_ok(response))
        .map(BuildingApplicationModel.fromJson)
        .toList();
  }

  Future<BuildingApplicationModel> get(String id) async =>
      BuildingApplicationModel.fromJson(_data(
          _ok(await client.authorizedGet(_urls.buildingApplicationUrl(id)))));

  Future<BuildingApplicationModel> create(Map<String, dynamic> body) async =>
      BuildingApplicationModel.fromJson(_data(_ok(
          await client.authorizedPost(_urls.buildingApplicationsUrl, body))));

  Future<BuildingApplicationModel> update(
          String id, Map<String, dynamic> body) async =>
      BuildingApplicationModel.fromJson(_data(_ok(
          await client.authorizedPut(_urls.buildingApplicationUrl(id), body))));

  Future<BuildingApplicationModel> submit(String id) async =>
      BuildingApplicationModel.fromJson(_data(_ok(await client
          .authorizedPost(_urls.submitBuildingApplicationUrl(id), {}))));

  Future<List<ApplicationDocumentModel>> listDocuments(String id) async {
    final response =
        await client.authorizedGet(_urls.applicationDocumentsUrl(id));
    return _dataList(_ok(response))
        .map(ApplicationDocumentModel.fromJson)
        .toList();
  }

  Future<ApplicationDocumentModel> upload(String id, String filePath) async =>
      ApplicationDocumentModel.fromJson(_data(_ok(await client.authorizedPost(
          _urls.applicationDocumentsUrl(id), {'file': File(filePath)}))));

  Future<void> remove(String id, String documentId) async {
    _ok(await client
        .authorizedDelete(_urls.applicationDocumentUrl(id, documentId)));
  }

  Resource _ok(Resource response) {
    final code = response.messageCode ?? 0;
    if (code >= 200 && code < 300) return response;
    final body = response.response;
    if (body is Map && body['code'] is String)
      throw ServerException(body['code'] as String);
    throw const ServerException(ApplicationErrorCode.unavailable);
  }

  Map<String, dynamic> _data(Resource response) =>
      (response.response as Map<String, dynamic>)['data']
          as Map<String, dynamic>;

  List<Map<String, dynamic>> _dataList(Resource response) =>
      ((response.response as Map<String, dynamic>)['data'] as List<dynamic>)
          .cast<Map<String, dynamic>>();
}
