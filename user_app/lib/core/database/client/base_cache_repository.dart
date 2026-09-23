import 'package:customer/core/database/client/base_cache.dart';

abstract class BaseCacheRepository {
  final BaseCache cache;
  BaseCacheRepository(this.cache);
}
