# F4-T5b — Ownership transfer documents

## Category
DB/Integration (building-service)

## Objective
Attach supporting documents to an ownership transfer (UO-13) using the existing `DocumentStorage`
port, completing F4-T5.

## Scope (TECH-SPEC-F4)
- `ownership_document`: transfer FK, opaque object key, original filename, MIME/size, uploader/time,
  removal audit; no reuse of application-document rows.
- `POST/GET .../units/{u}/ownership-transfers/{t}/documents`: upload / authorized metadata list.
- `GET/DELETE .../ownership-transfers/{t}/documents/{d}`: authorized attachment / audited removal.
- Admins and the transfer's source/recipient only; others get 404. Attachment + nosniff, bounded
  validated content, opaque keys. Removal is audited; the transfer itself is never deleted.

## Acceptance Criteria
- Storage failure returns a retryable error with no metadata row (no partial state); orphan cleanup.
- Content/size rejection; source/recipient access; unrelated owner cannot read.

## Status
PLANNED
