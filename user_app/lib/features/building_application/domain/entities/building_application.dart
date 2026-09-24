import 'application_draft.dart';
import 'application_enums.dart';

class BuildingApplication {
  final String id;
  final String applicationNumber;
  final ApplicationStatus status;
  final ApplicationDraft details;
  final List<String> missingFields;
  final String? rejectionReason;
  final String? infoRequestMessage;
  final DateTime? submittedAt;
  final DateTime? createdAt;

  const BuildingApplication({
    required this.id,
    required this.applicationNumber,
    required this.status,
    required this.details,
    this.missingFields = const [],
    this.rejectionReason,
    this.infoRequestMessage,
    this.submittedAt,
    this.createdAt,
  });

  bool get canSubmit => status.isEditable && missingFields.isEmpty;
}
