const dartReservedWords = {
  'assert',
  'break',
  'case',
  'catch',
  'class',
  'const',
  'continue',
  'default',
  'do',
  'else',
  'enum',
  'extends',
  'false',
  'final',
  'finally',
  'for',
  'if',
  'in',
  'is',
  'new',
  'null',
  'rethrow',
  'return',
  'super',
  'switch',
  'this',
  'throw',
  'true',
  'try',
  'var',
  'void',
  'while',
  'with',
  'async',
  'await',
  'yield',
  'abstract',
  'as',
  'covariant',
  'dynamic',
  'export',
  'external',
  'factory',
  'Function',
  'get',
  'hide',
  'implements',
  'import',
  'interface',
  'late',
  'library',
  'mixin',
  'operator',
  'part',
  'required',
  'set',
  'show',
  'static',
  'typedef',
  'base',
  'deferred',
  'extension',
  'of',
  'on',
  'sealed',
  'sync',
  'type',
  'when',
};

String toPascalCase(String name) => name
    .split('_')
    .map((word) => '${word[0].toUpperCase()}${word.substring(1)}')
    .join();

Map<String, String> featureFiles(String name, String packageName) {
  final type = toPascalCase(name);
  return {
    for (final entry in _templates.entries)
      entry.key.replaceAll('@name', name): entry.value
          .replaceAll('@name', name)
          .replaceAll('@type', type)
          .replaceAll('@package', packageName),
  };
}

const _templates = <String, String>{
  'domain/entities/@name_item.dart': r'''
class @typeItem {
  final String? id;
  final String? name;

  const @typeItem({this.id, this.name});
}

class @typeItemList {
  final List<@typeItem> items;

  const @typeItemList({this.items = const []});
}
''',
  'domain/repositories/@name_repository.dart': r'''
import 'package:@package/core/usecases/usecase.dart';
import '../entities/@name_item.dart';

abstract class @typeRepository {
  ResultFuture<@typeItemList> get@typeList({bool forceRefresh = false});
}
''',
  'domain/usecases/@name_use_case.dart': r'''
import 'package:@package/core/usecases/usecase.dart';
import '../entities/@name_item.dart';
import '../repositories/@name_repository.dart';

class @typeUseCase extends UseCaseWithParams<@typeItemList, bool> {
  final @typeRepository _repo;

  const @typeUseCase(this._repo);

  @override
  ResultFuture<@typeItemList> call(bool forceRefresh) =>
      _repo.get@typeList(forceRefresh: forceRefresh);
}
''',
  'data/models/@name_list_response.dart': r'''
class @typeListResponse {
  final bool? success;
  final List<@typeItemData> data;
  final String? errorMessage;

  const @typeListResponse({this.success, this.data = const [], this.errorMessage});

  factory @typeListResponse.fromJson(Map<String, dynamic> json) {
    // TODO: Adapt the response envelope to your API.
    return @typeListResponse(
      success: json['Success'] as bool?,
      data: (json['Data'] as List<dynamic>? ?? [])
          .map((item) => @typeItemData.fromJson(item as Map<String, dynamic>))
          .toList(),
      errorMessage: json['ErrorMessage'] as String?,
    );
  }
}

class @typeItemData {
  final String? id;
  final String? name;

  const @typeItemData({this.id, this.name});

  // TODO: Keep these mappings in sync when adding entity fields.
  factory @typeItemData.fromJson(Map<String, dynamic> json) =>
      @typeItemData(id: json['id'] as String?, name: json['name'] as String?);

  Map<String, dynamic> toJson() => {'id': id, 'name': name};
}
''',
  'data/mappers/@name_mapper.dart': r'''
import '../../domain/entities/@name_item.dart';
import '../models/@name_list_response.dart';

extension @typeItemDataMapper on @typeItemData {
  @typeItem toEntity() => @typeItem(id: id, name: name);
}

extension @typeItemDataListMapper on List<@typeItemData> {
  @typeItemList toEntity() =>
      @typeItemList(items: map((item) => item.toEntity()).toList());
}
''',
  'data/datasources/@name_remote_datasource.dart': r'''
import 'package:@package/core/errors/api_exceptions.dart';
import 'package:@package/core/network/client/base_http_repository.dart';
import '../models/@name_list_response.dart';

abstract class @typeRemoteDataSource {
  Future<List<@typeItemData>> fetchItems();
}

class @typeRemoteDataSourceImpl extends BaseHttpRepository implements @typeRemoteDataSource {
  // TODO: Set an absolute API URL (ApiClient currently has no Dio baseUrl).
  static const _endpoint = '';

  @typeRemoteDataSourceImpl(super.client);

  @override
  Future<List<@typeItemData>> fetchItems() async {
    if (_endpoint.isEmpty) {
      throw const ServerException('Configure the @name API endpoint.');
    }
    final response = await client.authorizedGet(_endpoint);
    final code = response.messageCode ?? 0;
    if (code < 200 || code >= 300) {
      throw ServerException(response.message ?? 'Failed to fetch data');
    }
    if (code == 204) return const [];
    final dto = @typeListResponse.fromJson(response.response as Map<String, dynamic>);
    if (dto.success == false) {
      throw ServerException(dto.errorMessage ?? 'Failed to fetch data');
    }
    return dto.data;
  }
}
''',
  'data/datasources/@name_local_datasource.dart': r'''
import 'dart:convert';
import 'package:@package/core/database/client/base_cache_repository.dart';
import '../models/@name_list_response.dart';

class @typeLocalDataSource extends BaseCacheRepository {
  static const _cacheKey = 'feature:@name:v1';
  static const _cacheDuration = Duration(days: 1);

  @typeLocalDataSource(super.cache);

  Future<List<@typeItemData>?> read() async {
    final cached = await cache.get(_cacheKey);
    if (cached == null) return null;
    return (jsonDecode(cached) as List<dynamic>)
        .map((item) => @typeItemData.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> write(List<@typeItemData> items) => cache.put(
        _cacheKey,
        jsonEncode(items.map((item) => item.toJson()).toList()),
        _cacheDuration,
      );
}
''',
  'data/repositories/@name_repository_impl.dart': r'''
import 'package:dartz/dartz.dart';
import 'package:@package/core/errors/api_exceptions.dart';
import 'package:@package/core/errors/failure.dart';
import 'package:@package/core/usecases/usecase.dart';
import '../../domain/entities/@name_item.dart';
import '../../domain/repositories/@name_repository.dart';
import '../datasources/@name_local_datasource.dart';
import '../datasources/@name_remote_datasource.dart';
import '../mappers/@name_mapper.dart';
import '../models/@name_list_response.dart';

class @typeRepositoryImpl implements @typeRepository {
  final @typeRemoteDataSource _remote;
  final @typeLocalDataSource _local;

  const @typeRepositoryImpl(this._remote, this._local);

  @override
  ResultFuture<@typeItemList> get@typeList({bool forceRefresh = false}) async {
    if (!forceRefresh) {
      try {
        final cached = await _local.read();
        if (cached != null) return Right(cached.toEntity());
      } on Object {
        // A corrupt or unavailable cache must not prevent a remote fetch.
      }
    }
    final List<@typeItemData> items;
    try {
      items = await _remote.fetchItems();
    } on ServerException catch (e) {
      return Left(ServerFailure(e.message));
    } on FormatException {
      return const Left(ParsingFailure('The server returned invalid data.'));
    } on TypeError {
      return const Left(ParsingFailure('The server returned an unexpected data format.'));
    } on Object {
      return const Left(ConnectionFailure('Unable to load data. Please try again.'));
    }
    try {
      await _local.write(items);
    } on Object {
      // Cache persistence is best effort; fresh data is still usable.
    }
    return Right(items.toEntity());
  }
}
''',
  'presentation/controllers/@name_screen_controller.dart': r'''
import 'dart:async';
import 'package:get/get.dart';
import 'package:@package/core/controllers/base_controller.dart';
import 'package:@package/core/utils/state_status.dart';
import '../../domain/entities/@name_item.dart';
import '../../domain/usecases/@name_use_case.dart';

class @typeScreenController extends BaseController {
  final @typeUseCase _useCase = Get.find<@typeUseCase>();
  final items = <@typeItem>[].obs;

  @override
  void onInit() {
    super.onInit();
    unawaited(getData());
  }

  Future<void> refreshData() => getData(forceRefresh: true);

  Future<void> getData({bool forceRefresh = false}) async {
    if (isLoading.value) return;
    errorMessage.value = null;
    try {
      await doAction<@typeItemList>(
        action: () => _useCase(forceRefresh),
        onSuccess: (result) {
          if (isClosed) return;
          items.assignAll(result.items);
          status.value = items.isEmpty ? StateStatus.empty : StateStatus.success;
        },
        // The screen keeps an accessible inline error and retry action.
        onError: (_) {},
      );
    } on Object {
      if (!isClosed) {
        errorMessage.value = 'Unable to load data. Please try again.';
        status.value = StateStatus.error;
      }
    } finally {
      isLoading.value = false;
    }
  }
}
''',
  'presentation/pages/@name_screen.dart': r'''
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:@package/app/theme/app_dimensions.dart';
import '../controllers/@name_screen_controller.dart';

class @typeScreen extends StatelessWidget {
  const @typeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final controller = Get.find<@typeScreenController>();
    return Scaffold(
      appBar: AppBar(title: const Text('@type')),
      body: SafeArea(
        child: Obx(() {
          final loading = controller.isLoading.value;
          final error = controller.errorMessage.value;
          // Copy the observable list while inside Obx to track list updates.
          final items = controller.items.toList();
          if (loading && items.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }
          return Column(
            children: [
              if (loading) const LinearProgressIndicator(),
              if (error != null)
                Semantics(
                  liveRegion: true,
                  child: Padding(
                    padding: AppDimens.spacing.p16,
                    child: Column(
                      children: [
                        Text(error, style: Theme.of(context).textTheme.bodyMedium),
                        FilledButton(
                          onPressed: loading ? null : controller.refreshData,
                          child: const Text('Retry'),
                        ),
                      ],
                    ),
                  ),
                ),
              Expanded(
                child: RefreshIndicator(
                  onRefresh: controller.refreshData,
                  child: CustomScrollView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    slivers: [
                      if (items.isEmpty)
                        SliverFillRemaining(
                          hasScrollBody: false,
                          child: Center(child: Text(error == null ? 'No data found' : 'Pull down to retry')),
                        )
                      else
                        SliverList(
                          delegate: SliverChildBuilderDelegate(
                            (context, index) {
                              final item = items[index];
                              return Card(
                                child: ListTile(
                                  title: Text(item.name ?? ''),
                                  subtitle: Text(item.id ?? ''),
                                ),
                              );
                            },
                            childCount: items.length,
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            ],
          );
        }),
      ),
    );
  }
}
''',
  'presentation/bindings/@name_binding.dart': r'''
import 'package:get/get.dart';
import 'package:@package/core/database/client/preference_cache.dart';
import 'package:@package/core/network/client/api_client.dart';
import '../../data/datasources/@name_local_datasource.dart';
import '../../data/datasources/@name_remote_datasource.dart';
import '../../data/repositories/@name_repository_impl.dart';
import '../../domain/repositories/@name_repository.dart';
import '../../domain/usecases/@name_use_case.dart';
import '../controllers/@name_screen_controller.dart';

class @typeBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<@typeRemoteDataSource>(
      () => @typeRemoteDataSourceImpl(Get.find<ApiClient>()),
      fenix: true,
    );
    Get.lazyPut<@typeLocalDataSource>(
      () => @typeLocalDataSource(Get.find<PreferenceCache>()),
      fenix: true,
    );
    Get.lazyPut<@typeRepository>(
      () => @typeRepositoryImpl(
        Get.find<@typeRemoteDataSource>(),
        Get.find<@typeLocalDataSource>(),
      ),
      fenix: true,
    );
    Get.lazyPut<@typeUseCase>(() => @typeUseCase(Get.find<@typeRepository>()), fenix: true);
    Get.lazyPut<@typeScreenController>(() => @typeScreenController(), fenix: true);
  }
}
''',
  'presentation/@name_pages.dart': r'''
import 'package:get/get.dart';
import 'bindings/@name_binding.dart';
import 'pages/@name_screen.dart';

class @typePages {
  static const routeName = '/@name';

  static final routes = [
    GetPage(
      name: routeName,
      page: () => const @typeScreen(),
      binding: @typeBinding(),
    ),
  ];
}
''',
};
