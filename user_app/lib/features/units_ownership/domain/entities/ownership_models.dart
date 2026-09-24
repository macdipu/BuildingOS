class OwnershipPeriod {
  const OwnershipPeriod({
    required this.id,
    required this.ownerUserId,
    required this.share,
    required this.startAt,
    this.endAt,
    this.notes,
  });
  final String id, ownerUserId, startAt;
  final num share;
  final String? endAt, notes;
}

class CurrentOwnership {
  const CurrentOwnership(this.revision, this.allocated, this.current);
  final int revision;
  final num allocated;
  final List<OwnershipPeriod> current;
}

class OwnershipTransfer {
  const OwnershipTransfer({
    required this.id,
    required this.sourceOwnerUserId,
    required this.recipientUserId,
    required this.share,
    required this.effectiveDate,
    required this.effectiveAt,
    required this.reason,
    this.reference,
  });
  final String id,
      sourceOwnerUserId,
      recipientUserId,
      effectiveDate,
      effectiveAt,
      reason;
  final num share;
  final String? reference;
}

class OwnershipHistory {
  const OwnershipHistory(this.revision, this.periods, this.transfers);
  final int revision;
  final List<OwnershipPeriod> periods;
  final List<OwnershipTransfer> transfers;
}

class OwnershipChange {
  const OwnershipChange({
    required this.recipientUserId,
    required this.share,
    required this.effectiveDate,
    required this.expectedVersion,
    required this.operationId,
    required this.reason,
    this.sourceOwnerUserId,
    this.notes,
    this.reference,
  });
  final String recipientUserId, effectiveDate, operationId, reason;
  final String? sourceOwnerUserId, notes, reference;
  final num share;
  final int expectedVersion;
}

class TransferFile {
  const TransferFile({
    required this.id,
    required this.fileName,
    required this.contentType,
    required this.sizeBytes,
    required this.uploadedAt,
  });
  final String id, fileName, contentType, uploadedAt;
  final int sizeBytes;
}

/// The backend accepts only today's Bangladesh date, independent of device zone.
String ownershipDate(DateTime instant) => instant
    .toUtc()
    .add(const Duration(hours: 6))
    .toIso8601String()
    .substring(0, 10);
