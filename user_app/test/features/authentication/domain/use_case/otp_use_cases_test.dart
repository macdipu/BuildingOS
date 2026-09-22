import 'package:dartz/dartz.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:customer/core/domain/error/failure.dart';
import 'package:customer/core/domain/models/phone_number.dart';
import 'package:customer/features/authentication/domain/model/auth_login_req.dart';
import 'package:customer/features/authentication/domain/model/otp_start_result.dart';
import 'package:customer/features/authentication/domain/model/user_info.dart';
import 'package:customer/features/authentication/domain/repository/auth_repository.dart';
import 'package:customer/features/authentication/domain/use_case/start_otp_use_case.dart';
import 'package:customer/features/authentication/domain/use_case/verify_otp_use_case.dart';
import 'package:customer/features/authentication/presentation/login/controller/login_screen_controller.dart'
    show otpErrorMessage;

class _FakeAuthRepository implements AuthRepository {
  Either<Failure, OtpStartResult>? startOtpResult;
  Either<Failure, UserInfo>? verifyOtpResult;

  String? lastVerifiedAttemptId;
  String? lastVerifiedCode;

  @override
  Future<Either<Failure, UserInfo>> login(AuthLoginReq req) async {
    throw UnimplementedError();
  }

  @override
  Future<Either<Failure, OtpStartResult>> startOtp(PhoneNumber phoneNumber) async {
    return startOtpResult!;
  }

  @override
  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  }) async {
    lastVerifiedAttemptId = attemptId;
    lastVerifiedCode = code;
    return verifyOtpResult!;
  }

  @override
  Future<void> jwtUpdated() async {}
}

void main() {
  late _FakeAuthRepository repository;
  final phone = PhoneNumber('01306999005');

  setUp(() {
    repository = _FakeAuthRepository();
  });

  group('StartOtpUseCase', () {
    test('returns the attempt id on success', () async {
      repository.startOtpResult = const Right(
        OtpStartResult(attemptId: 'attempt-1', expiresAt: '2026-09-22T12:05:00Z'),
      );

      final result = await StartOtpUseCase(repository)(phone);

      expect(result.isRight(), isTrue);
      result.fold(
        (l) => fail('expected Right, got Left(${l.message})'),
        (r) => expect(r.attemptId, 'attempt-1'),
      );
    });

    test('propagates a repository failure', () async {
      repository.startOtpResult = const Left(ServerFailure('boom'));

      final result = await StartOtpUseCase(repository)(phone);

      expect(result.isLeft(), isTrue);
      result.fold(
        (l) => expect(l.message, 'boom'),
        (r) => fail('expected Left, got Right($r)'),
      );
    });
  });

  group('VerifyOtpUseCase', () {
    test('returns true and passes attemptId/code through to the repository on success', () async {
      repository.verifyOtpResult = Right(UserInfo(
        accessToken: 'token',
        refreshToken: null,
        platformRoles: const ['SUPER_ADMIN'],
      ));

      final result = await VerifyOtpUseCase(repository)(
        VerifyOtpParams(attemptId: 'attempt-1', phoneNumber: phone, code: '000000'),
      );

      expect(result.isRight(), isTrue);
      result.fold((l) => fail('expected Right, got Left(${l.message})'), (r) => expect(r, isTrue));
      expect(repository.lastVerifiedAttemptId, 'attempt-1');
      expect(repository.lastVerifiedCode, '000000');
    });

    test('surfaces the OTP_* rejection code as the failure message', () async {
      repository.verifyOtpResult = const Left(ServerFailure('OTP_INVALID_CODE'));

      final result = await VerifyOtpUseCase(repository)(
        VerifyOtpParams(attemptId: 'attempt-1', phoneNumber: phone, code: '111111'),
      );

      expect(result.isLeft(), isTrue);
      result.fold(
        (l) => expect(l.message, 'OTP_INVALID_CODE'),
        (r) => fail('expected Left, got Right($r)'),
      );
    });
  });

  group('otpErrorMessage', () {
    test('maps every backend OTP_* rejection reason to a distinct, readable message', () {
      const codes = [
        'OTP_INVALID_CODE',
        'OTP_EXPIRED',
        'OTP_ALREADY_CONSUMED',
        'OTP_ATTEMPTS_EXHAUSTED',
        'OTP_UNKNOWN_ATTEMPT',
        'OTP_PHONE_MISMATCH',
      ];
      final messages = codes.map(otpErrorMessage).toSet();

      expect(messages.length, codes.length, reason: 'each reason should map to a distinct message');
      for (final message in messages) {
        expect(message, isNot(contains('OTP_')), reason: 'message should be human-readable, not a raw code');
      }
    });

    test('falls back to a generic message for an unrecognized or null code', () {
      expect(otpErrorMessage(null), isNotEmpty);
      expect(otpErrorMessage('SOMETHING_NEW'), isNotEmpty);
    });
  });
}
