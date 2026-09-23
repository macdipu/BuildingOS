class OtpStartResultModel {
  final String attemptId;
  final String expiresAt;

  const OtpStartResultModel({
    required this.attemptId,
    required this.expiresAt,
  });

  factory OtpStartResultModel.fromJson(Map<String, dynamic> json) =>
      OtpStartResultModel(
        attemptId: json['attemptId'] as String,
        expiresAt: json['expiresAt'] as String,
      );
}
