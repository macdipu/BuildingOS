class ApplicationDocument {
  final String id;
  final String fileName;
  final String contentType;
  final int sizeBytes;
  final DateTime? uploadedAt;

  const ApplicationDocument({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.sizeBytes,
    this.uploadedAt,
  });
}
