import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:customer/app/config/app_config.dart';
import 'package:customer/core/auth/session_expiry_notifier.dart';
import 'package:customer/core/database/client/preference_cache.dart';
import 'package:customer/core/database/preference/shared_preference_constants.dart';
import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/errors/api_exceptions.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:customer/features/authentication/data/datasources/auth_local_datasource.dart';
import 'package:customer/features/authentication/data/datasources/auth_remote_datasource.dart';
import 'package:customer/features/authentication/data/mappers/user_info_mapper.dart';
import 'package:customer/features/authentication/data/models/user_info_model.dart';
import 'package:customer/features/authentication/data/repositories/auth_repository_impl.dart';
import 'package:customer/features/authentication/domain/entities/user_info.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';

// Replace only device storage and the server; exercise the real Dio client,
// response/error parsing, remote/local data sources, repository, and in-memory
// session update.
class _MemoryCache extends PreferenceCache {
  final values = <String, String>{};

  @override
  Future<String?> get(String key, {bool secure = false}) async => values[key];

  @override
  Future<void> forever(String key, String value, {bool secure = false}) async {
    values[key] = value;
  }

  @override
  Future<void> flushAll() async => values.clear();
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
  late SessionExpiryNotifier sessionExpiry;
  late AuthRepositoryImpl repository;
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
    final urls = ApiUrl(config.getApiClientConfig());
    client = ApiClient(
      config.getApiClientConfig(),
      cache,
      urls,
      sessionExpiry = SessionExpiryNotifier(),
    );
    await client.setToken();
    repository = AuthRepositoryImpl(
      AuthRemoteDataSource(client, urls),
      AuthLocalDataSource(cache),
    );
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
    final stored = UserInfoModel.fromJsonString(
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
      cache.values[SharedPreferenceConstant.customerInfo] = const UserInfo(
        accessToken: existing,
        refreshToken: null,
      ).toModel().toJsonString();
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

  test('start rate limit maps to OTP_RATE_LIMITED', () async {
    responseStatus = 429;
    responseBody = {
      'success': false,
      'code': 'OTP_RATE_LIMITED',
      'message': 'Please wait before requesting another code',
    };

    final result = await repository.startOtp(phone);

    result.fold(
      (failure) => expect(failure.message, 'OTP_RATE_LIMITED'),
      (_) => fail('Expected rate limit failure'),
    );
  });

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

  test('failed token refresh clears the session and signals expiry', () async {
    final payload = base64Url
        .encode(utf8.encode(jsonEncode({
          'exp': DateTime.now()
                  .add(const Duration(hours: 1))
                  .millisecondsSinceEpoch ~/
              1000,
        })))
        .replaceAll('=', '');
    cache.values[SharedPreferenceConstant.customerInfo] = UserInfo(
      accessToken: 'header.$payload.signature',
      refreshToken: 'refresh',
    ).toModel().toJsonString();
    await client.setToken();
    responseStatus = 401;
    responseBody = {'code': 'UNAUTHORIZED'};
    final expired = sessionExpiry.onExpired.first;

    await expectLater(
      client.authorizedPost('http://127.0.0.1:${server.port}/api/v1/me', {}),
      throwsA(isA<UnauthorizedException>()),
    );
    await expired.timeout(const Duration(seconds: 5));

    expect(
        requests.map((r) => r.path), ['/api/v1/me', '/api/auth/refresh-token']);
    expect(client.hasToken(), isFalse);
    expect(cache.values, isEmpty);
  });
}
