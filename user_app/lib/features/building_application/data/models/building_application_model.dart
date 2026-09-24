class BuildingApplicationModel {
  final Map<String, dynamic> json;

  const BuildingApplicationModel(this.json);

  factory BuildingApplicationModel.fromJson(Map<String, dynamic> json) {
    if (json['id'] is! String ||
        json['applicationNumber'] is! String ||
        json['status'] is! String) {
      throw const FormatException(
          'Building application is missing id, number or status');
    }
    return BuildingApplicationModel(json);
  }

  String get id => json['id'] as String;
  String get applicationNumber => json['applicationNumber'] as String;
  String get status => json['status'] as String;
  String? text(String key) => json[key] as String?;
  int? integer(String key) => (json[key] as num?)?.toInt();
  double? decimal(String key) => (json[key] as num?)?.toDouble();
  List<String> get missingFields =>
      (json['missingFields'] as List<dynamic>? ?? const []).cast<String>();
}
