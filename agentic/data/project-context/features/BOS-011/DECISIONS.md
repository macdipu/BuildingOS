# BOS-011 decision register

| ID | Decision | Source | Status |
|---|---|---|---|
| D-01 | Adopt Stitch blue palette + typography (`ui/stitch/design-system/DESIGN.md`: primary `#004ac6`, Plus Jakarta Sans headings, Inter body) as the `user_app` brand, replacing navy/yellow/orange. | Operator, chat 2026-09-25: "yes use stitch blue" | RESOLVED |
| D-02 | Bottom navigation is per role as BRD §37 specifies (resolves UI-INDEX conflict C-1 in favor of BRD), not the single mockup bar. | Operator, chat 2026-09-25: "per-role nav" | RESOLVED |
| D-03 | Stitch mockup-only elements stay out of scope (UI-INDEX C-10, C-11 for delivered screens). | AGENTS.md UI rule 2 | RESOLVED (rule) |
| Q-01 | A user with several roles in the active building (e.g. `BUILDING_ADMIN` + `OWNER`): which nav set wins? Decision: admin/manager set (`Dashboard/Finance/Units/Work/More`) > Committee > Owner > Tenant. | §37 says "by permission", no precedence | RESOLVED (Operator, chat 2026-09-25: "accept Q-01 to Q-03 proposals") |
| Q-02 | Tabs whose module is not built yet (Finance, Payments, Work, Community): hide, or show a "coming soon" placeholder? Decision: show tab with placeholder so nav matches §37. | §37; modules BOS-003..008 not started | RESOLVED (Operator, chat 2026-09-25: "accept Q-01 to Q-03 proposals") |
| Q-03 | Dark mode: DESIGN.md defines light tokens only. Decision: derive dark scheme from primary `#004ac6` via Material 3 seed until a dark spec exists. | DESIGN.md light-only; app has adaptive colors today | RESOLVED (Operator, chat 2026-09-25: "accept Q-01 to Q-03 proposals") |

Role data available today: building-service `BuildingRole { BUILDING_ADMIN, OWNER }`
(`backend/building-service/.../building/domain/model/BuildingRole.java`). Tenant and Committee
nav sets are configured but unreachable until those roles exist (BOS-003 / later).
