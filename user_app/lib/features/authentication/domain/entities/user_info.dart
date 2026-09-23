import 'package:customer/core/entities/phone_number.dart';

class UserInfo {
  final PhoneNumber? phoneNumber;
  final String? fullName;
  final String? ogrName;
  final String? role;
  final String accessToken;
  final String? refreshToken;
  final String? email;
  final List<String> platformRoles;

  const UserInfo({
    this.phoneNumber,
    this.fullName,
    this.ogrName,
    this.role,
    required this.accessToken,
    required this.refreshToken,
    this.email,
    this.platformRoles = const [],
  });
}
