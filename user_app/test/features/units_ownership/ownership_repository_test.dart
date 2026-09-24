import 'dart:typed_data';
import 'package:flutter_test/flutter_test.dart';
import 'package:customer/core/network/client/resource.dart';
import 'package:customer/features/units_ownership/data/datasources/property_remote_datasource.dart';
import 'package:customer/features/units_ownership/data/repositories/ownership_repository_impl.dart';
import 'package:customer/features/units_ownership/domain/entities/ownership_models.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';
import 'property_repository_test.dart' show FakeClient;

class FileClient extends FakeClient {
  @override
  Future<Resource> authorizedGetBytes(String uri) async {
    path = uri;
    return next;
  }

  @override
  Future<Resource> authorizedDeleteWithBody(
    String uri,
    Map<String, dynamic> data,
  ) async {
    path = uri;
    body = data;
    return next;
  }
}

void main() {
  late FileClient client;
  late OwnershipRepositoryImpl repo;
  setUp(() {
    client = FileClient();
    repo = OwnershipRepositoryImpl(
      PropertyRemoteDataSource(client, 'http://local/api/'),
    );
  });
  test('ownership date follows Dhaka midnight independent of local zone', () {
    expect(ownershipDate(DateTime.utc(2026, 9, 24, 17, 59)), '2026-09-24');
    expect(ownershipDate(DateTime.utc(2026, 9, 24, 18)), '2026-09-25');
  });
  const change = OwnershipChange(
    recipientUserId: 'recipient',
    sourceOwnerUserId: 'source',
    share: 20.125,
    effectiveDate: '2026-09-24',
    expectedVersion: 7,
    operationId: 'stable',
    reason: 'Sale',
  );
  test(
    'transfer contract preserves ownership revision and retry identity',
    () async {
      await repo.transfer('b', 'u', change);
      final first = Map<String, dynamic>.from(client.body!);
      await repo.transfer('b', 'u', change);
      expect(
        client.path,
        'http://local/api/v1/buildings/b/units/u/ownership-transfers',
      );
      expect(client.body, first);
      expect(client.body!['expectedVersion'], 7);
      expect(client.body!['operationId'], 'stable');
      expect(client.body!['sourceOwnerUserId'], 'source');
      expect(client.body!['recipientUserId'], 'recipient');
      expect(client.body!.containsKey('ownerUserId'), false);
    },
  );
  test('assignment uses owner id and does not silently invite', () async {
    await repo.assign('b', 'u', change);
    expect(client.path, endsWith('/ownerships'));
    expect(client.body!['ownerUserId'], 'recipient');
    expect(client.body!.containsKey('sourceOwnerUserId'), false);
  });
  test(
    'document bytes are retrieved through the full protected object path',
    () async {
      client.next = Resource(
        messageCode: 200,
        response: Uint8List.fromList([37, 80, 68, 70]),
      );
      expect(await repo.download('b', 'u', 't', 'd'), [37, 80, 68, 70]);
      expect(
        client.path,
        'http://local/api/v1/buildings/b/units/u/ownership-transfers/t/documents/d',
      );
    },
  );
  test('removal accepts 204 and includes audited reason body', () async {
    client.next = Resource(messageCode: 204);
    await repo.remove('b', 'u', 't', 'd', 'Incorrect attachment');
    expect(client.body, {'reason': 'Incorrect attachment'});
  });
  test('document denial never returns bytes', () async {
    client.next = Resource(
      messageCode: 403,
      response: Uint8List.fromList([123, 125]),
    );
    await expectLater(
      repo.download('b', 'u', 't', 'd'),
      throwsA(
        isA<PropertyException>().having((e) => e.code, 'code', 'ACCESS_DENIED'),
      ),
    );
  });
}
