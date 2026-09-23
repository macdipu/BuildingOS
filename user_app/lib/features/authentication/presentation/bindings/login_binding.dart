import 'package:customer/core/database/client/preference_cache.dart';
import 'package:customer/core/network/client/api_client.dart';
import 'package:customer/core/network/urls/api_urls.dart';
import 'package:customer/features/authentication/data/datasources/auth_local_datasource.dart';
import 'package:customer/features/authentication/data/datasources/auth_remote_datasource.dart';
import 'package:customer/features/authentication/data/repositories/auth_repository_impl.dart';
import 'package:customer/features/authentication/domain/repositories/auth_repository.dart';
import 'package:customer/features/authentication/domain/usecases/start_otp_use_case.dart';
import 'package:customer/features/authentication/domain/usecases/verify_otp_use_case.dart';
import 'package:customer/features/authentication/presentation/controllers/login_screen_controller.dart';
import 'package:get/get.dart';

class LoginBinding extends Bindings {
  @override
  void dependencies() {
    Get.lazyPut<AuthRemoteDataSource>(
      () => AuthRemoteDataSource(Get.find<ApiClient>(), Get.find<ApiUrl>()),
      fenix: true,
    );

    Get.lazyPut<AuthLocalDataSource>(
      () => AuthLocalDataSource(Get.find<PreferenceCache>()),
      fenix: true,
    );

    Get.lazyPut<AuthRepository>(
      () => AuthRepositoryImpl(
        Get.find<AuthRemoteDataSource>(),
        Get.find<AuthLocalDataSource>(),
      ),
      fenix: true,
    );

    Get.lazyPut<StartOtpUseCase>(
      () => StartOtpUseCase(Get.find<AuthRepository>()),
      fenix: true,
    );

    Get.lazyPut<VerifyOtpUseCase>(
      () => VerifyOtpUseCase(Get.find<AuthRepository>()),
      fenix: true,
    );

    Get.lazyPut<LoginScreenController>(() => LoginScreenController(),
        fenix: true);
  }
}
