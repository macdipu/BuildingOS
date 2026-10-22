# STORY-003: Notice attachments

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
Publishers attach files (e.g. PDF circulars, images) to notices; audience members open them through
short-lived signed URLs.

## Scope
- Attachment metadata in communication-service; bytes in S3-compatible storage behind a storage port (Q-BOS-007-11), following the §131 upload workflow and limits.
- Flutter: Attachment field on Create Announcement (§78), attachment list/preview on detail (§79, mockup 40).
- Reusable attachment port for meetings (STORY-007).

## Acceptance Criteria
1. Upload follows §131 (type/size limits, metadata recorded, file linked to the notice); disallowed types/sizes are rejected with §128 errors.
2. Only users who can read the notice can obtain a signed URL; URLs are short-lived (§131, §8.8 "Signed URLs").
3. Attachments of an edited notice are versioned with it (§150.2 prior version kept).
4. Adding/removing attachments on a published notice is audited as an edit (§150.2).
5. Storage keys never encode personal data; no file bytes or signed URLs in logs (ARCHITECTURE.md Data Architecture).

## Tasks
- BE: storage port + MinIO adapter, upload/download-URL use cases — M
- Mobile: picker, upload progress, preview/open — S
- QA: authz on signed URLs, limits, cross-building access — S

## Dependencies
STORY-001 (STORY-002 for edit versioning); Q-BOS-007-11.

## Out of Scope
Document-service extraction (§8.8) — later; virus scanning beyond what §131 states.

## References
BRD §8.6 Attachments, §8.8, §78, §79, §128, §131, §150.2; gaps/mobile-ops-community.md (39 PDF attachments).
