import 'package:customer/core/entities/phone_number.dart';
import 'package:customer/core/errors/api_exceptions.dart';
import 'package:customer/core/errors/failure.dart';
import 'package:customer/features/authentication/data/datasources/auth_local_datasource.dart';
import 'package:customer/features/authentication/data/datasources/auth_remote_datasource.dart';
import 'package:customer/features/authentication/data/mappers/otp_start_result_mapper.dart';
import 'package:customer/features/authentication/data/mappers/user_info_mapper.dart';
import 'package:customer/features/authentication/data/models/user_info_model.dart';
import 'package:customer/features/authentication/domain/entities/otp_start_result.dart';
import 'package:customer/features/authentication/domain/entities/user_info.dart';
import 'package:customer/features/authentication/domain/repositories/auth_repository.dart';
import 'package:dartz/dartz.dart';

class AuthRepositoryImpl implements AuthRepository {
  final AuthRemoteDataSource _remote;
  final AuthLocalDataSource _local;

  AuthRepositoryImpl(this._remote, this._local);

  @override
  Future<Either<Failure, OtpStartResult>> startOtp(
      PhoneNumber phoneNumber) async {
    try {
      final result = await _remote.startOtp(phoneNumber);
      return Right(result.toEntity());
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } catch (e) {
      return Left(ConnectionFailure(e.toString()));
    }
  }

  @override
  Future<Either<Failure, UserInfo>> verifyOtp({
    required String attemptId,
    required PhoneNumber phoneNumber,
    required String code,
  }) async {
    final UserInfoModel userInfo;
    try {
      userInfo = await _remote.verifyOtp(
        attemptId: attemptId,
        phoneNumber: phoneNumber,
        code: code,
      );
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } catch (e) {
      return Left(ConnectionFailure(e.toString()));
    }
    await _local.saveUserInfo(userInfo);
    await _remote.reloadToken();
    return Right(userInfo.toEntity());
  }
}
