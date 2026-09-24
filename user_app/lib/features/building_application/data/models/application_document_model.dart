class ApplicationDocumentModel {
  final String id;
  final String fileName;
  final String contentType;
  final int sizeBytes;
  final String? uploadedAt;

  const ApplicationDocumentModel({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.sizeBytes,
    this.uploadedAt,
  });

  factory ApplicationDocumentModel.fromJson(Map<String, dynamic> json) =>
      ApplicationDocumentModel(
        id: json['id'] as String,
        fileName: json['fileName'] as String,
        contentType: json['contentType'] as String,
        sizeBytes: (json['sizeBytes'] as num).toInt(),
        uploadedAt: json['uploadedAt'] as String?,
      );
}
