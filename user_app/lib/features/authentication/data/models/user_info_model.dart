import 'dart:convert';

import 'package:customer/core/entities/phone_number.dart';

class UserInfoModel {
  final PhoneNumber? phoneNumber;
  final String? fullName;
  final String? ogrName;
  final String? role;
  final String accessToken;
  final String? refreshToken;
  final String? email;
  final List<String> platformRoles;

  const UserInfoModel({
    this.phoneNumber,
    this.fullName,
    this.ogrName,
    this.role,
    required this.accessToken,
    required this.refreshToken,
    this.email,
    this.platformRoles = const [],
  });

  Map<String, dynamic> toJson() => {
        'phone_number': phoneNumber?.withoutCountryCode,
        'fullName': fullName,
        'ogrName': ogrName,
        'role': role,
        'access_token': accessToken,
        'refresh_token': refreshToken,
        'email': email,
        'platform_roles': platformRoles,
      };

  factory UserInfoModel.fromJson(Map<String, dynamic> json) => UserInfoModel(
        phoneNumber: json['phone_number'] != null
            ? PhoneNumber(json['phone_number'])
            : null,
        fullName: json['fullName'],
        ogrName: json['ogrName'],
        role: json['role'],
        accessToken: json['access_token'],
        refreshToken: json['refresh_token'],
        email: json['email'],
        platformRoles: (json['platform_roles'] as List<dynamic>?)
                ?.map((e) => e.toString())
                .toList() ??
            const [],
      );

  factory UserInfoModel.fromOtpVerifyJson(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>;
    return UserInfoModel(
      phoneNumber: user['phone'] != null ? PhoneNumber(user['phone']) : null,
      accessToken: json['accessToken'] as String,
      refreshToken: null,
      platformRoles: (user['platformRoles'] as List<dynamic>? ?? [])
          .map((e) => e.toString())
          .toList(),
    );
  }

  String toJsonString() => jsonEncode(toJson());

  factory UserInfoModel.fromJsonString(String json) =>
      UserInfoModel.fromJson(jsonDecode(json));
}
