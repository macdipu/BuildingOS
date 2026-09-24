import 'dart:io';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/client/resource.dart';
import 'package:customer/core/errors/api_exceptions.dart';
import 'package:dio/dio.dart';
import '../../domain/entities/property_models.dart';
import '../mappers/property_mapper.dart';

/// Reuses the authenticated platform client and parses both ApiError and batch envelopes.
class PropertyRemoteDataSource {
  const PropertyRemoteDataSource(this.client, this.baseUrl);
  final ApiClient client;
  final String baseUrl;
  Future<dynamic> request(
    String path, {
    String method = 'GET',
    Map<String, dynamic>? body,
  }) async {
    try {
      final url = '${baseUrl}v1/$path';
      final Resource response = switch (method) {
        'POST' => await client.authorizedPost(url, body ?? {}),
        'PUT' => await client.authorizedPut(url, body ?? {}),
        _ => await client.authorizedGet(url),
      };
      final raw = response.response;
      final payload = raw is Response ? raw.data : raw;
      final status = raw is Response ? raw.statusCode : response.messageCode;
      if (payload is Map && payload['success'] == false) {
        final code =
            payload['code'] ??
            (payload['meta'] is Map ? payload['meta']['code'] : null);
        final preview = payload['data'];
        throw PropertyException(
          code is String ? code : 'UNAVAILABLE',
          preview:
              preview is Map<String, dynamic> && preview.containsKey('rows')
              ? PropertyMapper.preview(preview)
              : null,
        );
      }
      if (status == 403 || status == 404)
        throw const PropertyException('ACCESS_DENIED');
      if (status == null ||
          status < 200 ||
          status >= 300 ||
          payload is! Map ||
          !payload.containsKey('data')) {
        throw const PropertyException('UNAVAILABLE');
      }
      return payload['data'];
    } on PropertyException {
      rethrow;
    } on ForbiddenException {
      throw const PropertyException('ACCESS_DENIED');
    } on SocketException {
      throw const PropertyException('CONNECTION');
    } on Object {
      throw const PropertyException('UNAVAILABLE');
    }
  }

  Future<dynamic> uploadSheet(String path, String filePath) async {
    final file = File(filePath);
    if (await file.length() > 2 * 1024 * 1024)
      throw const PropertyException('FILE_TOO_LARGE');
    final extension = filePath.toLowerCase().split('.').last;
    if (extension != 'csv' && extension != 'xlsx')
      throw const PropertyException('UNSUPPORTED_FILE');
    final mime = extension == 'csv'
        ? 'text/csv'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
    final part = await MultipartFile.fromFile(
      filePath,
      contentType: DioMediaType.parse(mime),
    );
    // ApiClient detects File automatically; a MultipartFile needs isFormData explicitly.
    final response = await client.authorizedPost('${baseUrl}v1/$path', {
      'file': part,
    }, isFormData: true);
    final raw = response.response;
    final payload = raw is Response ? raw.data : raw;
    if (response.messageCode == 403)
      throw const PropertyException('ACCESS_DENIED');
    if (payload is Map && payload['success'] == true) return payload['data'];
    throw PropertyException(
      payload is Map && payload['code'] is String
          ? payload['code']
          : 'UNAVAILABLE',
    );
  }

  dynamic _documentResult(
    Resource response, {
    bool bytes = false,
    bool noContent = false,
  }) {
    final raw = response.response;
    final payload = raw is Response ? raw.data : raw;
    final status = raw is Response ? raw.statusCode : response.messageCode;
    if (status == 403 || status == 404)
      throw const PropertyException('ACCESS_DENIED');
    if (status == 204 && noContent) return null;
    if (status != null && status >= 200 && status < 300) {
      if (bytes && payload is List<int>) return payload;
      if (payload is Map && payload['success'] == true) return payload['data'];
    }
    throw PropertyException(
      payload is Map && payload['code'] is String
          ? payload['code']
          : 'UNAVAILABLE',
    );
  }

  Future<void> uploadDocument(
    String path,
    String filePath,
    String reason,
  ) async {
    try {
      if (await File(filePath).length() > 10 * 1024 * 1024)
        throw const PropertyException('FILE_TOO_LARGE');
      final extension = filePath.toLowerCase().split('.').last;
      final mime = switch (extension) {
        'pdf' => 'application/pdf',
        'jpg' || 'jpeg' => 'image/jpeg',
        'png' => 'image/png',
        _ => null,
      };
      if (mime == null) throw const PropertyException('DOCUMENT_UNSUPPORTED');
      final part = await MultipartFile.fromFile(
        filePath,
        contentType: DioMediaType.parse(mime),
      );
      _documentResult(
        await client.authorizedPost('${baseUrl}v1/$path', {
          'file': part,
          'reason': reason,
        }, isFormData: true),
      );
    } on ForbiddenException {
      throw const PropertyException('ACCESS_DENIED');
    }
  }

  Future<List<int>> download(String path) async {
    try {
      return (_documentResult(
            await client.authorizedGetBytes('${baseUrl}v1/$path'),
            bytes: true,
          )
          as List<int>);
    } on ForbiddenException {
      throw const PropertyException('ACCESS_DENIED');
    }
  }

  Future<void> removeDocument(String path, String reason) async {
    try {
      _documentResult(
        await client.authorizedDeleteWithBody('${baseUrl}v1/$path', {
          'reason': reason,
        }),
        noContent: true,
      );
    } on ForbiddenException {
      throw const PropertyException('ACCESS_DENIED');
    }
  }
}
