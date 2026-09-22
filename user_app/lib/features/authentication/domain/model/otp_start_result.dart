class OtpStartResult {
  final String attemptId;
  final String expiresAt;

  const OtpStartResult({
    required this.attemptId,
    required this.expiresAt,
  });
}
