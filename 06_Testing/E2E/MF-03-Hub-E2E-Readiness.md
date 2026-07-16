# MF-03 Hub E2E Readiness

## Current assessment

- MF-03 mobile E2E now launches the canonical `MotherJourneyScreen` → embedded `BabyProfileDetailScreen` flow; `/baby-care-hub` remains available only for compatibility.

- Web now has a Playwright harness and an MF-03 hub route.
- The former hub API-backed smoke passed on real device `SM N986N` (Android 13). The migrated canonical API-backed scenario is implemented and awaiting a fresh device run.
- Canonical fixture coverage switches Baby A→B, taps Journal, Growth, Milestone, and Vaccination actions, asserts every destination receives Baby B's ID, and opens scoped Milestone/Vaccination detail records.

## Target scenarios

1. Login as Mother and open the canonical Hành trình → Nuôi con flow.
2. Switch active baby from Baby A to Baby B.
3. Verify profile, journal, growth, milestone and vaccination cards reload with Baby B data only.
4. Add a journal entry and verify it appears after reload.
5. Open growth history and milestone detail; verify back navigation preserves the selected baby.
6. Deep-link to a Baby A resource while Baby B is active and verify neutral denial/not-found handling.
7. Repeat the critical flow on mobile and verify loading, error, empty and accessibility labels.

## Required setup before executable E2E

- Reconnect the Android device and restore `adb reverse tcp:8080 tcp:8080`.
- Run the migrated integration test with `MF03_API_E2E=true` and approved seeded token dart-defines.
- Keep the backend and seeded mother/baby fixture available for the duration of the run.

## Gate

Canonical mobile routing is implemented: Growth/Vaccination routes are registered, and Baby Journey interactions are actionable and baby-scoped. Full device execution remains an environment check.

MF-03 E2E gate: **PARTIAL — canonical migration and deterministic Journal/Growth/Milestone/Vaccination widget interactions are green; the migrated API-backed integration still requires a fresh run on the real Android device**.
