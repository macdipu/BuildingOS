# BOS-011 tasks

Status: COMPLETED (release approved macdipu 2026-09-25). One task = one commit.

| Task | Category | Title | Depends on | Status |
|---|---|---|---|---|
| UI-T01 | Mobile | Stitch theme tokens | None | DONE d5e104f |
| UI-T02 | Mobile | Splash screen + start routing | UI-T01 | DONE 0e63643 |
| UI-T03 | Mobile | Per-role bottom navigation | UI-T01, UI-T02; Q-01 and Q-02 answered | DONE d606292 |
| UI-T04 | Mobile | Login + OTP restyle | UI-T01 | DONE daaaa89 |
| UI-T05 | Mobile | My Buildings restyle + access warning | UI-T01 | DONE 5ba8367 |
| UI-T06 | BE | Unit list owner filter, search, sort | None | DONE c381b25 |
| UI-T07 | Mobile | Unit list filter/search/sort + restyle | UI-T01, UI-T06 | DONE 77e194f |
| UI-T08 | Mobile | Unit form Save & Add Another + restyle | UI-T01 | DONE 930bbea |
| UI-T09 | Mobile | Unit detail, ownership, transfer, members restyle | UI-T01 | DONE 563f385 |
| UI-T10 | QA | Visual + regression QA | UI-T01..UI-T09 | DONE (qa/QA.md) |
| UI-T11 | Mobile | DEF-01 splash controller never instantiated | UI-T02 | DONE d8e851f |

Parallel after UI-T01: UI-T02, T04, T05, T08, T09. UI-T06 (BE) can start immediately.
