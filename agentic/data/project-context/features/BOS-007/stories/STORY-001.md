# STORY-001: Publish and read building notices (communication-service foundation)

Status: PROPOSED (planning draft 2026-10-22; not approved)

## Parent Epic
EPIC-BOS-007

## Objective
An authorized publisher creates a notice (draft or publish now) for a building audience; members of
that audience see it on the Community tab notice board, open it, and the publisher sees Read %. This
is the first usable slice and stands up the communication-service skeleton every later story reuses.

## Scope
- communication-service skeleton per docs/ARCHITECTURE.md (feature-first, one use case per action,
  own Postgres DB + Flyway, platform-web envelope/errors/correlation, JWT validation, readiness),
  gateway routes, `communication_outbox` + publisher (§15) — deployable shape per Q-BOS-007-01.
- Use cases: CreateAnnouncementDraft, PublishAnnouncement (now), ListAnnouncements (ACTIVE), GetAnnouncement, MarkAnnouncementRead.
- Fields: title, body, priority, category, requires_acknowledgement (stored; Acknowledge action is STORY-002), pinned=false default, allow_comments=false (§28, §150.2).
- Audiences in this story: ALL, OWNERS, TENANTS, COMMITTEE, MANAGERS (§28); SPECIFIC_UNITS/USERS in STORY-002.
- Membership/role lookup against building-service per Q-BOS-007-03.
- Flutter: new feature `notice_board` (data/domain/presentation, GetX); Community tab opens Community
  Home with Notices segment (§150.9); list (mockup 39, Active only), detail (mockup 40 without
  Acknowledge/Edit/Expire), Create Announcement (§78 subset: Title, Message, Audience, Priority,
  Category, Save Draft, Publish).

## Acceptance Criteria
1. Only callers holding `announcement.create` in the building may create/publish; others get 403 with a §128 error (§150.2, §150.10). With today's roles this is BUILDING_ADMIN (Q-BOS-007-04).
2. `category` accepts exactly GENERAL, EMERGENCY, CIRCULAR, RESOLUTION, MAINTENANCE and is a label only; push/preference bypass is driven by priority EMERGENCY (§150.2, C-12). `priority` accepts the values decided in Q-BOS-007-07.
3. Save Draft stores the notice unpublished and invisible to the audience (§78); Publish makes it ACTIVE (§77, §150.7 `POST /announcements/{id}/publish`).
4. On publish the audience is resolved from current building membership and stored as a snapshot; Read % = distinct readers in the snapshot / snapshot size (§150.2).
5. A member sees a notice only if they hold `announcement.read` in that building and are in its audience; tenants see only notices targeted to them (§150.10 "TENANT announcement.read (targeted)").
6. Opening detail records one `AnnouncementRead` per user (repeat opens are idempotent); Read % updates (§150.2, Scenario J).
7. Every query filters by `building_id` and current membership; a notice from building A is never returned under building B (404, not 403, to avoid existence leaks) (§110, §150.12, §149.13).
8. Publish writes the announcement and an `announcement.published` outbox row in one transaction (§15, §150.8); payload carries ids, building_id, priority, category and audience snapshot reference, not body text beyond what push needs (§150.4 minimal data).
9. Publish is audited with actor, building, announcement id and action (§150.2, §114).
10. List is cursor/page-paginated per §118, sorted pinned-first then newest (§150.2).
11. Flutter list cards show Title, Audience, Time, Priority, Read % (Read % only for publishers) (§77); `+ New Announcement` visible only with `announcement.create` (§77, §150.10).
12. Flutter caches the last fetched board for offline read; publish requires connectivity (§150.6).
13. No notice entity, endpoint or screen is shared with lease termination notice (§150.1).

## Tasks
Proposed lanes (no TASK files yet):
- BE/DB: communication-service skeleton, DB, outbox, auth/membership port, announcement aggregate + 5 use cases, OpenAPI contract, Kafka contract `announcement.published.v1` — L
- BE: gateway routes; building-service internal directory/role endpoint if Q-BOS-007-03 picks sync lookup — M
- Mobile: `notice_board` feature, Community Home shell, list/detail/create — M
- QA: authorization matrix, tenant-isolation, outbox atomicity, Read % math, contract tests — M

## Dependencies
- Q-BOS-007-01, -03, -04, -05, -07 decided.
- Existing: auth-service JWT, building-service memberships, gateway, Flutter per-role nav (C-1).

## Out of Scope
Scheduling, expiry, pin, edit, delete, acknowledge, specific units/users audience (STORY-002);
attachments (STORY-003); push (STORY-004); comments (Q-BOS-007-10).

## References
BRD §8.6, §15, §18, §28, §77–79, §110, §114, §118, §128, §149.13, §150.1, §150.2, §150.6–150.10;
UI-INDEX C-1, C-12; gaps/mobile-ops-community.md (39, 40); docs/ARCHITECTURE.md.
