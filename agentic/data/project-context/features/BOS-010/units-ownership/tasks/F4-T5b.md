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

## Implementation notes
- V9 `ownership_document` (FK to `ownership_transfer (building_id, id)`, removal columns). Opaque key
  `ownership-transfers/{transferId}/{documentId}`; the file name is display metadata only.
- Writes (upload/remove) are admin-only, like every other ownership write; SUSPENDED is read-only.
  Reads: admins, or the transfer's source/recipient with a current OWNER membership; anyone else,
  a wrong unit parent or a revoked party gets 404 `TRANSFER_NOT_FOUND`.
- Same content boundary as application documents (PDF/JPEG/PNG by signature,
  `DOCUMENTS_MAX_SIZE_BYTES`); at most `OWNERSHIP_MAX_DOCUMENTS_PER_TRANSFER` (default 10) active
  documents per transfer (409 `DOCUMENT_LIMIT_REACHED`).
- Upload stores the object, then row + audit (`TRANSFER_DOCUMENT_ATTACHED`) in one transaction; any
  failure after staging deletes the object best-effort. Storage outage → 503, no row.
- Removal needs a reason: row marked removed + audit `TRANSFER_DOCUMENT_REMOVED`, transfer untouched,
  bytes deleted after commit (best-effort; a failure leaves an unreferenced object, no sweeper yet).
- Gateway route and OpenAPI entries belong to F4-T6.

## Status
COMPLETED 2026-09-24 (run RUN-C73FA516545143FAA9B35B168EEFE0C9). TransferDocumentApiIntegrationTest 4
(MinIO + Postgres, incl. real missing-bucket storage failure), UploadTransferDocumentServiceTest 2.
building-service verify 112/0, ArchUnit 7/0, platform-web 5/0, check-contracts passed. F4-T5 complete.
