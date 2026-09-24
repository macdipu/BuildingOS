import 'dart:io';

import 'package:customer/core/errors/api_exceptions.dart';
import 'package:customer/core/errors/failure.dart';
import 'package:customer/core/usecases/usecase.dart';
import 'package:dartz/dartz.dart';
import '../../domain/entities/application_document.dart';
import '../../domain/entities/application_draft.dart';
import '../../domain/entities/application_error.dart';
import '../../domain/entities/building_application.dart';
import '../../domain/repositories/building_application_repository.dart';
import '../datasources/building_application_remote_datasource.dart';
import '../mappers/building_application_mapper.dart';

class BuildingApplicationRepositoryImpl
    implements BuildingApplicationRepository {
  final BuildingApplicationRemoteDataSource _remote;

  const BuildingApplicationRepositoryImpl(this._remote);

  @override
  ResultFuture<List<BuildingApplication>> listMine() => _guard(
      () async => (await _remote.listMine()).map((m) => m.toEntity()).toList());

  @override
  ResultFuture<BuildingApplication> get(String id) =>
      _guard(() async => (await _remote.get(id)).toEntity());

  @override
  ResultFuture<BuildingApplication> create(ApplicationDraft draft) =>
      _guard(() async => (await _remote.create(draft.toJson())).toEntity());

  @override
  ResultFuture<BuildingApplication> update(String id, ApplicationDraft draft) =>
      _guard(() async => (await _remote.update(id, draft.toJson())).toEntity());

  @override
  ResultFuture<BuildingApplication> submit(String id) =>
      _guard(() async => (await _remote.submit(id)).toEntity());

  @override
  ResultFuture<List<ApplicationDocument>> listDocuments(String applicationId) =>
      _guard(() async => (await _remote.listDocuments(applicationId))
          .map((m) => m.toEntity())
          .toList());

  @override
  ResultFuture<ApplicationDocument> uploadDocument(
          String applicationId, String filePath) =>
      _guard(() async =>
          (await _remote.upload(applicationId, filePath)).toEntity());

  @override
  ResultVoid removeDocument(String applicationId, String documentId) =>
      _guard(() => _remote.remove(applicationId, documentId));

  Future<Either<Failure, T>> _guard<T>(Future<T> Function() call) async {
    try {
      return Right(await call());
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on ForbiddenException {
      return const Left(ServerFailure(ApplicationErrorCode.notFound));
    } on SocketException {
      return const Left(ConnectionFailure(ApplicationErrorCode.connection));
    } on FormatException {
      return const Left(ParsingFailure(ApplicationErrorCode.unexpected));
    } on TypeError {
      return const Left(ParsingFailure(ApplicationErrorCode.unexpected));
    } on Object {
      return const Left(ConnectionFailure(ApplicationErrorCode.connection));
    }
  }
}
