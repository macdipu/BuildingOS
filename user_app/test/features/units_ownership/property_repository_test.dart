import 'package:flutter_test/flutter_test.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/client/resource.dart';
import 'package:customer/features/units_ownership/data/datasources/property_remote_datasource.dart';
import 'package:customer/features/units_ownership/data/repositories/property_repository_impl.dart';
import 'package:customer/features/units_ownership/domain/entities/property_models.dart';

class FakeClient implements ApiClient {
  Resource next = Resource(
    messageCode: 200,
    response: {'success': true, 'data': []},
  );
  String? path;
  Map<String, dynamic>? body;
  @override
  Future<Resource> authorizedGet(
    String uri, {
    Map<String, dynamic>? queryParams,
  }) async {
    path = uri;
    return next;
  }

  @override
  Future<Resource> authorizedPost(
    String uri,
    Map<String, dynamic> data, {
    bool? isFormData = false,
  }) async {
    path = uri;
    body = data;
    return next;
  }

  @override
  Future<Resource> authorizedPut(
    String uri,
    Map<String, dynamic> data, {
    bool? isFormData = false,
  }) async {
    path = uri;
    body = data;
    return next;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  late FakeClient client;
  late PropertyRepositoryImpl repo;
  setUp(() {
    client = FakeClient();
    repo = PropertyRepositoryImpl(
      PropertyRemoteDataSource(client, 'http://local/api/'),
    );
  });
  test(
    'selector maps authoritative roles and count; never invents buildings',
    () async {
      expect(await repo.listBuildings(), isEmpty);
      client.next = Resource(
        messageCode: 200,
        response: {
          'success': true,
          'data': [
            {
              'id': 'b',
              'name': 'Home',
              'address': 'Road',
              'status': 'SUSPENDED',
              'roles': ['OWNER'],
              'ownedUnitCount': 2,
            },
          ],
        },
      );
      final buildings = await repo.listBuildings();
      expect(client.path, 'http://local/api/v1/me/buildings');
      expect(buildings.single.ownedUnitCount, 2);
      expect(buildings.single.roles, ['OWNER']);
    },
  );
  test('read-only capabilities never permit mutations', () async {
    client.next = Resource(
      messageCode: 200,
      response: {
        'success': true,
        'data': {
          'id': 'b',
          'name': 'Home',
          'status': 'SUSPENDED',
          'capabilities': {
            'readOnly': true,
            'manageUnits': true,
            'manageMembers': true,
          },
        },
      },
    );
    final b = await repo.getBuilding('b');
    expect(b.canEditMembers, false);
    expect(b.canEditUnits, false);
  });
  test(
    'claim sends only the stable operation id, never ownership data',
    () async {
      await repo.claim('i', 'same-operation');
      expect(
        client.path,
        'http://local/api/v1/me/building-invitations/i/claim',
      );
      expect(client.body, {'operationId': 'same-operation'});
    },
  );
  test(
    'commit conflict retains server row errors and canonical draft',
    () async {
      client.next = Resource(
        messageCode: 409,
        response: {
          'success': false,
          'meta': {'code': 'BATCH_CONFLICT'},
          'data': {
            'valid': false,
            'rows': [
              {
                'rowNumber': 1,
                'number': '4A',
                'floorId': 'f',
                'type': 'FLAT',
                'areaSqft': 90,
                'errors': [
                  {'field': 'number', 'code': 'UNIT_NUMBER_TAKEN'},
                ],
              },
            ],
          },
        },
      );
      try {
        await repo.commitBatch(
          'b',
          [
            const UnitDraft(
              number: '4A',
              floorId: 'f',
              type: 'FLAT',
              areaSqft: 90,
            ),
          ],
          'op',
          'setup',
        );
        fail('Must reject');
      } on PropertyException catch (e) {
        expect(e.code, 'BATCH_CONFLICT');
        expect(e.preview!.rows.single.errors.single.code, 'UNIT_NUMBER_TAKEN');
        expect(e.preview!.rows.single.draft.number, '4A');
      }
      expect(client.body!['operationId'], 'op');
    },
  );
  test('revoke includes optimistic membership version and reason', () async {
    await repo.revokeMember('b', 'm', 3, 'Requested');
    expect(client.body, {'expectedVersion': 3, 'reason': 'Requested'});
    expect(client.path, 'http://local/api/v1/buildings/b/members/m/revoke');
  });
  test('unit list sends BRD §48 filter, search and sort params only when set', () async {
    await repo.listUnits('b');
    expect(client.path, 'http://local/api/v1/buildings/b/units');
    await repo.listUnits(
      'b',
      query: const UnitListQuery(
        floorId: 'f1',
        type: 'FLAT',
        ownerUserId: 'u1',
        search: ' 4a ',
        sort: UnitSortField.floor,
        descending: true,
      ),
    );
    final uri = Uri.parse(client.path!);
    expect(uri.path, '/api/v1/buildings/b/units');
    expect(uri.queryParameters, {
      'floorId': 'f1',
      'type': 'FLAT',
      'ownerUserId': 'u1',
      'q': '4a',
      'sort': 'floor,desc',
    });
  });
  test('denied response cannot become empty success', () async {
    client.next = Resource(
      messageCode: 403,
      response: {'success': false, 'code': 'ACCESS_DENIED'},
    );
    await expectLater(
      repo.listUnits('b'),
      throwsA(
        isA<PropertyException>().having((e) => e.code, 'code', 'ACCESS_DENIED'),
      ),
    );
  });
}
