import 'package:customer/features/authentication/data/models/otp_start_result_model.dart';
import 'package:customer/features/authentication/domain/entities/otp_start_result.dart';

extension OtpStartResultModelMapper on OtpStartResultModel {
  OtpStartResult toEntity() =>
      OtpStartResult(attemptId: attemptId, expiresAt: expiresAt);
}
