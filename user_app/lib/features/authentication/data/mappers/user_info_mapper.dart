import 'package:customer/features/authentication/data/models/user_info_model.dart';
import 'package:customer/features/authentication/domain/entities/user_info.dart';

extension UserInfoModelMapper on UserInfoModel {
  UserInfo toEntity() => UserInfo(
        phoneNumber: phoneNumber,
        fullName: fullName,
        ogrName: ogrName,
        role: role,
        accessToken: accessToken,
        refreshToken: refreshToken,
        email: email,
        platformRoles: platformRoles,
      );
}

extension UserInfoEntityMapper on UserInfo {
  UserInfoModel toModel() => UserInfoModel(
        phoneNumber: phoneNumber,
        fullName: fullName,
        ogrName: ogrName,
        role: role,
        accessToken: accessToken,
        refreshToken: refreshToken,
        email: email,
        platformRoles: platformRoles,
      );
}
