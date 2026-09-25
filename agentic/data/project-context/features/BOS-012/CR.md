# BOS-012 — CR: close BOS-011 QA visual gaps

Type: change_request (brownfield, `user_app` only). Status: IMPLEMENTATION.
Raised: 2026-09-25 by operator (macdipu), chat: "commit bookkeeping, then fix the two visual gaps".
Source: BOS-011 [qa/QA.md](../BOS-011/qa/QA.md) "Visual deviations"; Stitch mockups mobile/02, 03, 11, 12.

## Delta
1. Primary CTAs (`CommonButton` filled/elevated, `FilledButton` via theme) use primary-container
   `#2563EB` like the mockups, not primary `#004AC6`.
2. `PropertyAction` buttons (Save, Save & Add Another, Edit unit, Confirm, …) are full-width, 48px.

No behavior, API or data change. Task: [UI-F01](tasks/UI-F01.md).
