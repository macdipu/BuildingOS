import 'package:customer/core/database/client/base_cache_repository.dart';
import 'package:customer/core/database/preference/shared_preference_constants.dart';
import 'package:customer/features/authentication/data/models/user_info_model.dart';

class AuthLocalDataSource extends BaseCacheRepository {
  AuthLocalDataSource(super.cache);

  Future<void> saveUserInfo(UserInfoModel userInfo) => cache.forever(
        SharedPreferenceConstant.customerInfo,
        userInfo.toJsonString(),
        secure: true,
      );
}
