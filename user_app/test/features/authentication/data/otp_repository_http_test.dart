import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:customer/app/flavours/app_config.dart';
import 'package:customer/core/data/cache/client/preference_cache.dart';
import 'package:customer/core/data/cache/preference/shared_preference_constants.dart';
import 'package:customer/core/data/http/client/api_client.dart';
import 'package:customer/core/data/http/urls/api_urls.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import 'package:customer/features/authentication/data/repo_impl/auth_cache_impl.dart';
import 'package:customer/features/authentication/data/repo_impl/auth_http_impl.dart';
import 'package:customer/features/authentication/domain/model/user_info.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

// Replace only device storage and the server; exercise the real Dio client,
// response/error parsing, cache decorator, and in-memory session update.
class _MemoryCache extends PreferenceCache {
  final values = <String, String>{};

  @override
  Future<String?> get(String key) async => values[key];

  @override
  Future<void> forever(String key, String value) async {
    values[key] = value;
  }
}

class _LocalConfig extends AppConfig {
  const _LocalConfig(this.port);

  final int port;

  @override
  String get apiBaseUrl => 'http://127.0.0.1:$port/';
}

void main() {
  late HttpServer server;
  late StreamSubscription<HttpRequest> subscription;
  late _MemoryCache cache;
  late ApiClient client;
  late AuthCacheImpl repository;
  late int responseStatus;
  late Map<String, dynamic> responseBody;
  late List<
      ({
        String method,
        String path,
        String? authorization,
        Map<String, dynamic> body
      })> requests;
  final phone = PhoneNumber('+8801306999005');

  setUp(() async {
    Get.testMode = true;
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    responseStatus = 200;
    responseBody = {};
    requests = [];
    subscription = server.listen((request) async {
      requests.add((
        method: request.method,
        path: request.uri.path,
        authorization: request.headers.value(HttpHeaders.authorizationHeader),
        body: jsonDecode(await utf8.decoder.bind(request).join())
            as Map<String, dynamic>,
      ));
      request.response.statusCode = responseStatus;
      request.response.headers.contentType = ContentType.json;
      request.response.write(jsonEncode(responseBody));
      await request.response.close();
    });
    final config = _LocalConfig(server.port);
    Get.put<AppConfig>(config);
    cache = _MemoryCache();
    final urls = ApiUrl();
    client = ApiClient(config, cache, urls);
    await client.setToken();
    repository = AuthCacheImpl(cache, AuthHttpImpl(client, urls));
  });

  tearDown(() async {
    await server.close(force: true);
    await subscription.cancel();
    Get.reset();
  });

  test('start sends a public normalized phone request and parses the envelope',
      () async {
    responseBody = {
      'data': {'attemptId': 'attempt-1', 'expiresAt': '2026-09-23T12:05:00Z'},
      'traceId': 'test-trace',
    };

    final result = await repository.startOtp(phone);

    result.fold(
      (failure) => fail(failure.message),
      (challenge) {
        expect(challenge.attemptId, 'attempt-1');
        expect(challenge.expiresAt, '2026-09-23T12:05:00Z');
      },
    );
    expect(requests.single.method, 'POST');
    expect(requests.single.path, '/api/v1/auth/otp/start');
    expect(requests.single.authorization, isNull);
    expect(requests.single.body, {'phone': '01306999005'});
    expect(cache.values, isEmpty);
  });

  test('verify persists the user and installs the token before returning',
      () async {
    responseBody = {
      'data': {
        'accessToken': 'test-access-token',
        'tokenType': 'Bearer',
        'expiresInSeconds': 900,
        'user': {
          'id': 'test-user',
          'phone': '01306999005',
          'platformRoles': ['SUPER_ADMIN', 'PLATFORM_ADMIN'],
        },
      },
      'traceId': 'test-trace',
    };

    final result = await repository.verifyOtp(
      attemptId: 'attempt-1',
      phoneNumber: phone,
      code: '000000',
    );

    expect(result.isRight(), isTrue);
    expect(requests.single.method, 'POST');
    expect(requests.single.path, '/api/v1/auth/otp/verify');
    expect(requests.single.authorization, isNull);
    expect(requests.single.body, {
      'attemptId': 'attempt-1',
      'phone': '01306999005',
      'code': '000000',
    });
    final stored = UserInfo.fromJsonString(
      cache.values[SharedPreferenceConstant.customerInfo]!,
    );
    expect(stored.phoneNumber, phone);
    expect(stored.platformRoles, ['SUPER_ADMIN', 'PLATFORM_ADMIN']);
    expect(stored.accessToken, 'test-access-token');
    expect(stored.refreshToken, isNull);
    expect(client.getToken()?.getToken(), stored.accessToken);
    expect(client.getToken()?.getRefreshToken(), isEmpty);
  });

  for (final reason in [
    'UNKNOWN_ATTEMPT',
    'PHONE_MISMATCH',
    'ALREADY_CONSUMED',
    'EXPIRED',
    'ATTEMPTS_EXHAUSTED',
    'INVALID_CODE',
  ]) {
    test(
        '401 OTP_$reason survives HTTP parsing without overwriting the session',
        () async {
      const existing = 'existing-session';
      cache.values[SharedPreferenceConstant.customerInfo] = UserInfo(
        accessToken: existing,
        refreshToken: null,
      ).toJsonString();
      await client.setToken();
      final original = Map<String, String>.of(cache.values);
      responseStatus = 401;
      responseBody = {
        'code': 'OTP_$reason',
        'message': 'OTP verification failed',
        'traceId': 'test-trace',
      };

      final result = await repository.verifyOtp(
        attemptId: 'attempt-1',
        phoneNumber: phone,
        code: '111111',
      );

      result.fold(
        (failure) => expect(failure.message, 'OTP_$reason'),
        (_) => fail('Expected rejection'),
      );
      expect(requests, hasLength(1),
          reason: 'Public OTP rejection must not trigger refresh');
      expect(requests.single.authorization, isNull);
      expect(cache.values, original);
      expect(client.getToken()?.getToken(), existing);
    });
  }

  test('server failure cannot create a session', () async {
    responseStatus = 500;
    responseBody = {'code': 'INTERNAL_ERROR', 'message': 'Unexpected error'};

    final result = await repository.verifyOtp(
      attemptId: 'attempt-1',
      phoneNumber: phone,
      code: '000000',
    );

    expect(result.isLeft(), isTrue);
    expect(cache.values, isEmpty);
    expect(client.hasToken(), isFalse);
  });
}
