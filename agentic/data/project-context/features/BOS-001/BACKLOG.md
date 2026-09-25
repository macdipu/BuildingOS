# BuildingOS delivery backlog
Source: BRD §123; work starts with BOS-001. No sprint dates, capacity promises, or approvals implied.

| Work item | Outcome | Dependencies | Main BRD references |
|---|---|---|---|
| BOS-001 | Reproducible secure local platform foundation | Existing Flutter baseline | 6–18,93–103,123 Phase 0 |
| BOS-002 | Phone/Google identity, membership, building selector, buildings/units/ownership | BOS-001; Q-01/Q-02 | 4–5,19,39–42,48–54,138,143A/E |
| BOS-003 | Tenants, lease lifecycle, recurring rent invoice | BOS-002; Q-03 | 20–21,55–62,112 |
| BOS-004 | Payment allocation, advance ledger, server receipt, reversal, maintenance and expenses | BOS-003; financial authorization | 22–26,63–71,108,113,143B/C |
| BOS-005 | Drift cache/outbox, offline collection, idempotent sync and explicit conflicts | BOS-004 online idempotency | 31–35,143D |
| BOS-006 | Work orders, contractors and basic assets | BOS-002; BOS-004 expense linkage | 27,30,72–76,83–84 |
| BOS-007 | Notice board (announcements), meetings, community chat, FCM push and notification inbox | BOS-002; domain event delivery; chat OPEN decisions in §150.3 | 8.6–8.7,28–29,77–82,85,115,150 |
| BOS-008 | Reporting projections, role dashboards, PDF/Excel | Relevant upstream domain events | 43–47,86,111,116,132–134 |
| BOS-009 | Security, load/failure recovery, DLQ and release evidence | Prior slices | 93–99,102,135–136,142,146 |

Audit is implemented alongside privileged/domain writes from BOS-002 onward, not deferred to BOS-009.
Outbox and consumer idempotency ship with each event-producing feature.
Offline-safe payment is an MVP requirement; BOS-004 alone is not an MVP release.

## Coverage of §146 MVP success criteria
1–2 → BOS-002; 3–5 → BOS-003; 6–11 → BOS-004; 12 → BOS-006;
13–14 → BOS-007; 15 → BOS-008; 16–17 → BOS-005; 18 → BOS-008;
19 → BOS-002/003/004 history tests; 20 → audit across all privileged mutations, verified in BOS-009.

## Prioritization
The first user-facing vertical slice after foundation is sign-in → authorized building selection → units/ownership.
Do not build all 12 deployables before delivering that slice.
Later phases should be refined with the smallest useful story/task hierarchy as evidence becomes available.
