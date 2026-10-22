# STORY-004: Work order comments and attachments (+ contractor documents)

Status: PROPOSED (planning draft 2026-10-22; not approved). Size: M (storage pattern exists in
building-service; uncertainty medium until Q-BOS-006-15).

## Parent Epic
EPIC-BOS-006 ([EPIC.md](../EPIC.md))

## Objective
Staff can discuss work and attach files (photos, invoices, reports) to work orders and contractors.

## Scope
- WorkComment, WorkAttachment (§8.5); contractor documents (§76 "Documents").
- Upload flow per §131 (intent → signed URL → direct upload → confirm metadata) behind a
  `DocumentStorage` port on MinIO/S3 (D-28 pattern, Q-BOS-006-15).
- Use cases: AddWorkComment, ListWorkComments, RequestWorkAttachmentUpload,
  ConfirmWorkAttachment, GetWorkAttachmentDownloadUrl, same pair for contractor documents.
- Flutter: comments thread + attachments section on Work Order Detail (`Add Attachment`,
  `Add Comment`, §74); attachments on Create Work Order (§73); Documents on Contractor Detail (§76).

## Acceptance Criteria
1. `Add Comment` / `Add Attachment` available on Work Order Detail for users with `work.read` +
   a write permission (`work.create` or `work.assign`) — exact set confirmed with Q-BOS-006-02.
2. File bytes never pass through work-service; it issues signed upload/download URLs and stores
   metadata (§131, §8.8).
3. Download URLs are short-lived and issued only after building-scoped authorization; another
   building's attachment id → 404 (§5, §110).
4. Type/size limits enforced server-side; failures return `FILE_UPLOAD_FAILED` / `VALIDATION_ERROR`
   (§128).
5. Comments and attachments are append-only on COMPLETED/CANCELLED work orders (no edit/delete of
   history; §119 "Historical entities should normally transition status").
6. No file contents, signed URLs or tokens in ordinary logs (ARCHITECTURE.md Data Architecture).
7. Attachment add is audited with actor + timestamp (§114 "Sensitive document access optionally"
   — download audit only if the operator opts in).

## Tasks
- BE/DB — comment + attachment tables, storage port + MinIO adapter, use cases (M)
- Mobile — comments thread, attachment picker/upload/viewer, contractor documents (M)
- QA — signed-URL authorization, isolation, limit tests (S)

## Dependencies
STORY-003 (detail screen), STORY-001 (contractor). Q-BOS-006-15.

## Out of Scope
Document-service (§8.8 future), geo-tagged before/after pairing (gap 38), OCR (§3.2), versioning
(DocumentVersion, §8.8).

## References
BRD §8.5, §8.8, §73, §74, §76, §110, §114, §119, §128, §131; BOS-010 D-28.
