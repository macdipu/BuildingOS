import 'dart:convert';

import 'package:customer/core/domain/models/phone_number.dart';

class UserInfo {
  final PhoneNumber? phoneNumber;
  final String? fullName;
  final String? ogrName;
  final String? role;
  final String accessToken;
  final String? refreshToken;
  final String? email;
  final List<String> platformRoles;

  UserInfo({
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

  factory UserInfo.fromJson(Map<String, dynamic> json) => UserInfo(
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

  factory UserInfo.fromApiJson(Map<String, dynamic> json) {
    final token = json['token'] as Map<String, dynamic>;
    final user = json['user'] as Map<String, dynamic>;
    return UserInfo(
      phoneNumber: user['phone'] != null ? PhoneNumber(user['phone']) : null,
      fullName: user['name'],
      role: user['role'],
      accessToken: token['accessToken'],
      refreshToken: token['refreshToken'],
      email: user['email'],
    );
  }

  factory UserInfo.fromOtpVerifyJson(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>;
    return UserInfo(
      phoneNumber: user['phone'] != null ? PhoneNumber(user['phone']) : null,
      accessToken: json['accessToken'] as String,
      refreshToken: null,
      platformRoles: (user['platformRoles'] as List<dynamic>? ?? [])
          .map((e) => e.toString())
          .toList(),
    );
  }

  String toJsonString() {
    return jsonEncode(toJson());
  }

  factory UserInfo.fromJsonString(String json) {
    return UserInfo.fromJson(jsonDecode(json));
  }
}
