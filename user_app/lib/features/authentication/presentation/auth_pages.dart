import 'package:customer/app/routes/app_routes.dart';
import 'package:customer/features/authentication/presentation/bindings/login_binding.dart';
import 'package:customer/features/authentication/presentation/pages/login_otp_verify_screen.dart';
import 'package:customer/features/authentication/presentation/pages/login_screen.dart';
import 'package:get/get.dart';

class AuthPages {
  static final List<GetPage> routes = [
    GetPage(
      name: AppRoutes.login,
      page: () => const LoginScreen(),
      binding: LoginBinding(),
    ),
    GetPage(
      name: AppRoutes.loginOtpVerify,
      page: () => const LoginOtpVerifyScreen(),
      binding: LoginBinding(),
    ),
  ];
}
