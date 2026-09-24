import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:customer/app/config/app_config.dart';
import 'package:customer/core/auth/session_expiry_notifier.dart';
import 'package:customer/core/database/client/preference_cache.dart';
import 'package:customer/core/database/preference/shared_preference_constants.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:customer/features/building_application/data/datasources/building_application_remote_datasource.dart';
import 'package:customer/features/building_application/data/repositories/building_application_repository_impl.dart';
import 'package:customer/features/building_application/domain/entities/application_draft.dart';
import 'package:customer/features/building_application/domain/entities/application_enums.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

class _MemoryCache extends PreferenceCache {
  final values = <String, String>{};

  @override
  Future<String?> get(String key, {bool secure = false}) async => values[key];

  @override
  Future<void> forever(String key, String value, {bool secure = false}) async =>
      values[key] = value;

  @override
  Future<void> flushAll() async => values.clear();
}

class _LocalConfig extends AppConfig {
  const _LocalConfig(this.port);

  final int port;

  @override
  String get apiBaseUrl => 'http://127.0.0.1:$port/';
}

String _jwt() {
  String part(Map<String, dynamic> json) =>
      base64Url.encode(utf8.encode(jsonEncode(json))).replaceAll('=', '');
  final exp =
      DateTime.now().add(const Duration(hours: 1)).millisecondsSinceEpoch ~/
          1000;
  return '${part({'alg': 'none'})}.${part({'sub': 'user-1', 'exp': exp})}.sig';
}

Map<String, dynamic> _application(
        {String status = 'DRAFT', List<String> missing = const []}) =>
    {
      'id': 'app-1',
      'applicationNumber': 'BA-2026-000001',
      'status': status,
      'buildingName': 'Rose Garden',
      'buildingType': 'MIXED',
      'applicantRelationship': 'COMMITTEE_MEMBER',
      'managementType': 'OWNERS_COMMITTEE',
      'contactPhone': '01712345678',
      'estimatedUnits': 36,
      'latitude': 23.7461,
      'longitude': 90.3742,
      'missingFields': missing,
      'infoRequestMessage':
          status == 'MORE_INFORMATION_REQUIRED' ? 'Upload the deed' : null,
      'createdAt': '2026-09-24T06:00:00Z',
    };

void main() {
  late HttpServer server;
  late StreamSubscription<HttpRequest> subscription;
  late BuildingApplicationRepositoryImpl repository;
  late String token;
  late int responseStatus;
  late Object responseBody;
  late List<
      ({
        String method,
        String path,
        String? authorization,
        String? contentType,
        List<int> body
      })> requests;

  setUp(() async {
    Get.testMode = true;
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    responseStatus = 200;
    responseBody = {'success': true, 'data': _application()};
    requests = [];
    subscription = server.listen((request) async {
      final body = await request
          .fold<List<int>>(<int>[], (all, chunk) => all..addAll(chunk));
      requests.add((
        method: request.method,
        path: request.uri.path,
        authorization: request.headers.value(HttpHeaders.authorizationHeader),
        contentType: request.headers.value(HttpHeaders.contentTypeHeader),
        body: body,
      ));
      request.response.statusCode = responseStatus;
      if (responseStatus != 204) {
        request.response.headers.contentType = ContentType.json;
        request.response.write(jsonEncode(responseBody));
      }
      await request.response.close();
    });
    final config = _LocalConfig(server.port);
    Get.put<AppConfig>(config);
    final cache = _MemoryCache();
    token = _jwt();
    cache.values[SharedPreferenceConstant.customerInfo] =
        jsonEncode({'access_token': token, 'refresh_token': ''});
    final urls = ApiUrl(config.getApiClientConfig());
    final client = ApiClient(
        config.getApiClientConfig(), cache, urls, SessionExpiryNotifier());
    await client.setToken();
    repository = BuildingApplicationRepositoryImpl(
        BuildingApplicationRemoteDataSource(client, urls));
  });

  tearDown(() async {
    await server.close(force: true);
    await subscription.cancel();
    Get.reset();
  });

  Map<String, dynamic> sentJson() =>
      jsonDecode(utf8.decode(requests.single.body)) as Map<String, dynamic>;

  test(
      'create sends wire enums and trimmed values with the bearer token, and parses the envelope',
      () async {
    final result = await repository.create(const ApplicationDraft(
      buildingName: '  Rose Garden ',
      buildingType: BuildingType.mixed,
      applicantRelationship: ApplicantRelationship.committeeMember,
      managementType: ManagementType.ownersCommittee,
      address: '   ',
    ));

    final application =
        result.getOrElse(() => throw StateError('expected success'));
    expect(application.status, ApplicationStatus.draft);
    expect(application.details.buildingType, BuildingType.mixed);
    expect(application.details.applicantRelationship,
        ApplicantRelationship.committeeMember);
    expect(application.details.latitude, 23.7461);
    expect(requests.single.method, 'POST');
    expect(requests.single.path, '/api/v1/building-applications');
    expect(requests.single.authorization, 'Bearer $token');
    final sent = sentJson();
    expect(sent['buildingName'], 'Rose Garden');
    expect(sent['buildingType'], 'MIXED');
    expect(sent['applicantRelationship'], 'COMMITTEE_MEMBER');
    expect(sent['managementType'], 'OWNERS_COMMITTEE');
    expect(sent['address'], isNull);
  });

  test('list parses statuses and missing fields', () async {
    responseBody = {
      'success': true,
      'data': [
        _application(
            status: 'MORE_INFORMATION_REQUIRED', missing: ['district']),
        _application(status: 'UNDER_REVIEW'),
      ],
    };

    final items = (await repository.listMine())
        .getOrElse(() => throw StateError('expected success'));

    expect(requests.single.path, '/api/v1/me/building-applications');
    expect(items.map((a) => a.status), [
      ApplicationStatus.moreInformationRequired,
      ApplicationStatus.underReview
    ]);
    expect(items.first.infoRequestMessage, 'Upload the deed');
    expect(items.first.canSubmit, isFalse);
    expect(items.last.status.isEditable, isFalse);
  });

  test('API error codes survive as failure messages', () async {
    for (final (status, code) in [
      (400, 'APPLICATION_INCOMPLETE'),
      (409, 'NOT_EDITABLE'),
      (404, 'APPLICATION_NOT_FOUND'),
      (413, 'DOCUMENT_TOO_LARGE'),
      (415, 'UNSUPPORTED_DOCUMENT_TYPE'),
    ]) {
      responseStatus = status;
      responseBody = {
        'success': false,
        'code': code,
        'message': 'x',
        'traceId': 't'
      };
      final result = await repository.submit('app-1');
      expect(result.fold((f) => f.message, (_) => 'success'), code,
          reason: '$status');
    }
    expect(requests.last.path, '/api/v1/building-applications/app-1/submit');
  });

  test('upload sends the file as multipart and remove accepts 204', () async {
    final file = File('${Directory.systemTemp.createTempSync().path}/deed.pdf')
      ..writeAsBytesSync(utf8.encode('%PDF-1.4 test'));
    responseStatus = 201;
    responseBody = {
      'success': true,
      'data': {
        'id': 'doc-1',
        'fileName': 'deed.pdf',
        'contentType': 'application/pdf',
        'sizeBytes': 13
      },
    };

    final uploaded = (await repository.uploadDocument('app-1', file.path))
        .getOrElse(() => throw StateError('expected success'));

    expect(uploaded.fileName, 'deed.pdf');
    expect(
        requests.single.path, '/api/v1/building-applications/app-1/documents');
    expect(requests.single.contentType, startsWith('multipart/form-data'));
    final body = latin1.decode(requests.single.body);
    expect(body, contains('name="file"; filename="deed.pdf"'));
    expect(body, contains('%PDF-1.4 test'));

    responseStatus = 204;
    final removed = await repository.removeDocument('app-1', 'doc-1');
    expect(removed.isRight(), isTrue);
    expect(requests.last.method, 'DELETE');
    expect(requests.last.path,
        '/api/v1/building-applications/app-1/documents/doc-1');
  });

  test('an unreachable server is a connection failure', () async {
    await server.close(force: true);
    final result = await repository.listMine();
    expect(result.fold((f) => f.message, (_) => 'success'), 'CONNECTION');
  });
}
