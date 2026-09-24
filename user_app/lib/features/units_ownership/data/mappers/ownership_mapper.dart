import '../../domain/entities/ownership_models.dart';

class OwnershipMapper {
  static OwnershipPeriod period(Map<String, dynamic> j) => OwnershipPeriod(
    id: j['id'],
    ownerUserId: j['ownerUserId'],
    share: j['share'],
    startAt: j['startAt'],
    endAt: j['endAt'],
    notes: j['notes'],
  );
  static CurrentOwnership current(Map<String, dynamic> j) => CurrentOwnership(
    j['revision'],
    j['allocated'],
    (j['current'] as List).map((p) => period(p)).toList(),
  );
  static OwnershipTransfer transfer(Map<String, dynamic> j) =>
      OwnershipTransfer(
        id: j['id'],
        sourceOwnerUserId: j['sourceOwnerUserId'],
        recipientUserId: j['recipientUserId'],
        share: j['share'],
        effectiveDate: j['effectiveDate'],
        effectiveAt: j['effectiveAt'],
        reason: j['reason'],
        reference: j['reference'],
      );
  static OwnershipHistory history(Map<String, dynamic> j) => OwnershipHistory(
    j['revision'],
    (j['periods'] as List).map((p) => period(p)).toList(),
    (j['transfers'] as List).map((t) => transfer(t)).toList(),
  );
  static TransferFile file(Map<String, dynamic> j) => TransferFile(
    id: j['id'],
    fileName: j['fileName'],
    contentType: j['contentType'],
    sizeBytes: j['sizeBytes'],
    uploadedAt: j['uploadedAt'],
  );
  static Map<String, dynamic> change(
    OwnershipChange c, {
    required bool transfer,
  }) => {
    if (transfer) 'sourceOwnerUserId': c.sourceOwnerUserId,
    if (transfer)
      'recipientUserId': c.recipientUserId
    else
      'ownerUserId': c.recipientUserId,
    'share': c.share,
    'effectiveDate': c.effectiveDate,
    'expectedVersion': c.expectedVersion,
    'operationId': c.operationId,
    'reason': c.reason,
    if (transfer) 'reference': c.reference else 'notes': c.notes,
  };
}
