# EPIC-BOS-007: Building Communication — Notice Board, Meetings, Notifications, Push (Community Chat deferred)

Status: PROPOSED (planning draft 2026-10-22; not approved)

No approval is implied by this document. Sizes are relative (S/M/L/XL), not hours or dates.
Every rule below cites the BRD (`../../BuildingOS_BRD_Agentic_Development.md`), a decision file, or
the Stitch UI gap files; anything the BRD leaves open is in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md).

## Objective

Let authorized building staff publish official, auditable notices and run meetings with recorded
decisions, and let every building member receive them reliably: in the app (notice board, meeting
list, notification inbox) and as FCM push on all their devices, honoring per-building preferences,
with EMERGENCY notices always delivered. Covers MVP success criterion 20 "Publish announcements and
maintain meeting decisions" (§146) and MVP item 25 "Announcement" (§124). Community chat (§150.3) is
specified but its MVP inclusion is OPEN with default deferred (§125), so it is planned as the last,
deferred stories.

## Business Context

- Notice = Announcement in the community UI (§150.1); lease termination notice (§20) is a separate
  Rental concept and must share no entities, endpoints or screens with the notice board (§150.1).
- Communication owns announcements, meetings, agenda, decisions, attachments, audience targeting,
  read acknowledgement, pinning and chat (§8.6). Notification owns FCM push, device registration,
  per-user per-building preferences, scheduled reminders, inbox, delivery tracking and retry (§8.7).
- §145 names both `communication-service` and `notification-service` in the MVP service set and allows
  `communication-service` to be co-deployed temporarily while keeping domain boundaries.
- Delivered foundation this epic builds on: auth-service (OTP, global user, platform roles, JWT,
  service JWT per BOS-010 D-38), building-service (buildings, memberships, units, ownership, outbox,
  MinIO documents), subscription-service (per-user subscriptions and `GET /api/v1/me/entitlements`,
  BOS-010 D-22..D-27), back-office web (F6), Flutter `user_app` (auth, building application,
  units/ownership, per-role nav with a `community` tab for Committee/Owner/Tenant — §37, UI-INDEX C-1).
- Flutter already contains `core/services/push_notification/firebase_notification_service.dart`
  (FCM init + token fetch); no backend device registry exists yet.
- Building-service currently models only `BuildingRole { BUILDING_ADMIN, OWNER }`; COMMITTEE,
  PROPERTY_MANAGER, TENANT, ACCOUNTANT, VIEWER/AUDITOR used by §150.10 do not exist yet (Q-BOS-007-04).

## Scope

### Owning services
| Concern | Service | Status |
|---|---|---|
| Notices, meetings, agenda, decisions, attachment metadata, (deferred) chat | `communication-service` (§8.6, §145) | **NEW service — needs technical approval (Q-BOS-007-01)**. No existing service owns this domain; building-service is not a fit (§8.6 vs §8.2 boundaries). |
| Push, device registry, preferences, inbox, delivery tracking | `notification-service` (§8.7, §145) | **NEW service — needs technical approval (Q-BOS-007-01)**; device-registry owner is Q-BOS-007-02 (§8.1 vs §150.4). |
| Membership/role authority, audience resolution source | building-service (existing; ADR-F4-001 PROPOSED) | Extend: internal directory endpoint(s) and/or membership events (Q-BOS-007-03). |
| Entitlements | subscription-service (existing) | Configuration only: free-tier feature keys per D-23/D-27 (Q-BOS-007-05). |
| Routing | api-gateway (existing) | Explicit new routes (and, for deferred chat, WebSocket). |
| Attachment bytes | S3-compatible storage (MinIO, existing infra) | Storage port behind communication-service until document-service exists (Q-BOS-007-11). |

### Data entities
- Communication: `Announcement` (title, body, priority, category GENERAL|EMERGENCY|CIRCULAR|RESOLUTION|MAINTENANCE per C-12/§150.2, publish_at, expires_at, requires_acknowledgement, pinned, allow_comments, status, version — §28, §150.2), `AnnouncementVersion` (prior versions on edit — §150.2), `AnnouncementAudience` (resolved audience snapshot — §150.2), `AnnouncementRead` (per-user read/ack — §150.2), `Meeting`, `AgendaItem`, `MeetingParticipant`, `MeetingDecision` (decision_text, owner, deadline, status — §29), attachment metadata, `communication_outbox` (§15). Deferred: `Comment` (§8.6), `ChatChannel`, `ChatChannelMember`, `ChatMessage`, `ChatMessageReport`, `UserBlock` (§150.3).
- Notification: `DeviceRegistration` (§150.4), `NotificationPreference` (§150.4), `Notification` (inbox), `NotificationDelivery` (§150.4), `NotificationTemplate` (§8.7), consumer-dedup table (§96 / backlog "consumer idempotency ships with each event-producing feature").

### Events (Kafka, transactional outbox §15, versioned envelope `contracts/kafka/event-envelope.schema.json`)
- Published by communication-service on `communication.events` (§150.8): `announcement.published`, `announcement.updated`, `announcement.pinned`, `announcement.expired`; `meeting.created`, `meeting.updated`, `meeting.decision.recorded` (§8.6; Q-BOS-007-15). Deferred chat events per §150.8.
- Published by notification-service: `notification.delivered`, `notification.failed` (§150.8).
- Consumed by notification-service in this epic: `announcement.published`, `meeting.created` (§8.7, §150.4). Domain reminders (`rent.invoice.overdue`, `maintenance.invoice.overdue`, `payment.recorded`, `work.assigned`, `work.overdue`) are consumed once their producing epics (BOS-003/004/006) emit them — see Out of Scope.
- Consumed by communication-service: membership granted/revoked, role changed (§150.8) — required for chat; source per Q-BOS-007-03 (building-service emits only `ownership.transferred` today).

### APIs (§150.7, §18, §118, §128)
- Notices: `GET /api/v1/buildings/{buildingId}/announcements?status=ACTIVE|SCHEDULED|EXPIRED`, `POST /api/v1/buildings/{buildingId}/announcements`, `PATCH /api/v1/announcements/{id}`, `POST /api/v1/announcements/{id}/publish|pin|unpin|expire|acknowledge`, plus detail `GET /api/v1/announcements/{id}` and read-marking (shape TBD in design).
- Meetings: §80–82 imply CRUD + lifecycle actions; no paths in BRD — proposed in technical design under the §18 pattern.
- Notifications: `POST/DELETE /api/v1/me/devices[/{deviceId}]`, `GET/PUT /api/v1/me/notification-preferences`, `GET /api/v1/me/notifications`, `POST /api/v1/me/notifications/{id}/read`, `POST /api/v1/me/notifications/read-all`.
- Deferred chat: §150.7 chat REST + `WS /ws/chat`.

### Flutter screens (feature-first GetX, new features under `user_app/lib/features/`)
| Screen | BRD | Mockup |
|---|---|---|
| Community Home (Notices segment; Chat segment only with chat) | §150.9 | — |
| Announcement List (Active/Scheduled/Expired) | §77, §150.2 | `mobile/39-notice-board-circulars` |
| Create Announcement | §78 | none (BRD + DESIGN.md) |
| Announcement Detail + Acknowledge | §79, §150.2 | `mobile/40-notice-detail-acknowledgment` |
| Meetings List / Create / Detail | §80–82 | none |
| Notifications (inbox) | §85 | none |
| Notification Settings | §150.9, §89 Notification | none |
| Deferred: Chat List, Conversation, Channel Info, Moderation Reports | §150.9 | none |

UI-only mockup elements are excluded (gap file `ui/stitch/gaps/mobile-ops-community.md`): AGM agenda
card / digital quorum / RSVP (polls are future scope, C-13), external-government-body issuer, "Passes"
tab (visitor, C-13), ledger-hash acknowledgement framing, executive seal, per-flat occupant card,
"Add to Calendar / Set Reminder". BRD-only items are in scope: Active/Scheduled/Expired tabs (§77),
`Edit` if allowed and `Expire Now` (§79).

### Back-office impact
- Small: free-tier / plan feature keys for notices, meetings, push and (deferred) chat are data in the
  existing plan editor (D-08, D-23, D-27); no new back-office screen in the MVP stories.
- Platform support staff get no chat access by default (§150.3, §149.12); no support tooling here.

## Out of Scope
- Society polls / AGM voting and visitor gate pass — future scope, no stories (C-13, §3.2).
- Resident directory (mobile 42, §87) — unassigned in backlog.
- Calendar screen (§88) and "Add to Calendar" (mockup 40 UI-only).
- Domain reminder schedules (§115 rent/maintenance/lease/work timings) and their producing events —
  owned by BOS-003/004/006; this epic delivers the consumer framework they plug into (Q-BOS-007-20).
- Email/SMS adapters (§8.7 "future adapters").
- Notice comments (§8.6 optional) — Q-BOS-007-10, default deferred.
- Voice/video calling, end-to-end encrypted chat, cross-building chat (§3.2, §150.12).
- Tamper-evident / hash-chained acknowledgement ledger (gap file, mockup 40).

## Stories
1. [STORY-001](stories/STORY-001.md) Publish and read building notices (communication-service foundation) — L
2. [STORY-002](stories/STORY-002.md) Notice lifecycle: schedule, expire, pin, edit history, delete, acknowledge, targeted audiences — L
3. [STORY-003](stories/STORY-003.md) Notice attachments — M
4. [STORY-004](stories/STORY-004.md) Notification service, device registry and notice push (EMERGENCY bypass) — L
5. [STORY-005](stories/STORY-005.md) Notification inbox and per-building preferences — M
6. [STORY-006](stories/STORY-006.md) Meetings: create, invite, list and detail — L
7. [STORY-007](stories/STORY-007.md) Meeting conduct: minutes, decisions, files, close — M
8. [STORY-008](stories/STORY-008.md) **DEFERRED** Community chat core (building/audience channels, real time, offline, push) — XL
9. [STORY-009](stories/STORY-009.md) **DEFERRED** Chat direct/custom groups, moderation, reports and blocks — L

## Cross-Story Dependencies
```
STORY-001 ──> STORY-002 ──> (STORY-005 notices preference IMPORTANT_ONLY uses priority)
     │──────> STORY-003 ──> STORY-007 (meeting files reuse attachment port)
     │──────> STORY-004 ──> STORY-005
     └──────> STORY-006 ──> STORY-007
STORY-004 ──> STORY-006 (meeting.created push AC)
STORY-001 + STORY-004 + STORY-005 ──> STORY-008 ──> STORY-009   (deferred; Q-BOS-007-22)
```
External: BOS-002/BOS-010 membership + roles (Q-BOS-007-03/04), BOS-003 tenants (TENANTS audience
non-empty), BOS-006 work-service (Create Work from Decision), subscription-service entitlement
configuration (Q-BOS-007-05).

## Acceptance Criteria
1. An authorized publisher publishes a notice; only its resolved audience can list/read it; Read % is
   computed against the audience resolved at publish time (§150.2).
2. Scenario J (§150.13): an EMERGENCY notice reaches every audience member's active devices regardless
   of preference; it appears on the board (pinned where the publisher pinned it — Q-BOS-007-13); Read %
   updates as members open it.
3. Publishing, editing, expiring, pinning and deleting a notice are audited (§150.2, §114); edits keep
   the prior version (§150.2).
4. A meeting can be created, invitations published to participants (push via `meeting.created`), and
   decisions recorded and listed with owner/deadline/status (§29, §80–82, §150.4).
5. Every push also creates an inbox entry (except chat), payloads carry only the §150.4 minimal fields,
   and tapping a push for another building switches `activeBuildingId` after membership re-check
   (§150.4, Scenario N).
6. All reads/writes are filtered by `building_id` and current membership (§110, §150.12); events go
   through an outbox (§15); consumers are idempotent (§96, backlog).
7. Chat (STORY-008/009) is not built until Q-BOS-007-22 is decided.

## Risks
- Two new deployables (communication, notification) are the largest technical-approval and ops cost;
  co-deployment is allowed for communication only (§145) — Q-BOS-007-01.
- Role model gap: most §150.10 roles do not exist in building-service; publisher authorization and
  role audiences are only partially exercisable until they do — Q-BOS-007-04.
- Membership change propagation to communication/notification (sync calls vs events) affects
  correctness of audience resolution and, later, chat access revocation — Q-BOS-007-03.
- Entitlement conflict: §150.11 building-level entitlements vs per-user model (D-22) and free tier =
  maintenance only (D-23) — Q-BOS-007-05.
- FCM delivery depends on Firebase project configuration per platform; push P95 < 10 s (§150.12)
  needs verification in a real environment, not local only.

## References
- BRD §8.1, §8.6, §8.7, §8.8, §14.2, §15, §16, §18, §28, §29, §37, §77–82, §85, §89, §94, §96, §104,
  §110, §114, §115, §118, §124, §125, §128, §131, §136, §145, §146, §149.12, §149.13, §149.16, §150.
- `../../ui/stitch/UI-INDEX.md` (C-1, C-12, C-13), `../../ui/stitch/gaps/mobile-ops-community.md` (39, 40).
- `../BOS-001/BACKLOG.md`, `../BOS-010/DECISIONS.md` (D-08, D-22..D-27, D-38), `../../ARCHITECTURE.md`,
  `docs/ARCHITECTURE.md`.
- [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md).
