# MF-01 Story 6.1 — Mobile Gap-Fix Closure Rerun

## Summary

- Date: `2026-07-18`
- Device: Samsung SM-N986N, Android 13
- App package: `com.carebridge.app`
- Backend: local Spring Boot through `adb reverse tcp:8080 tcp:8080`
- Database: disposable PostgreSQL `carebridge_story61`
- Scope: the four previously partial cases and three previously failed cases
- Result: **7/7 PASS**
- Composite mobile manual result: **16/16 PASS** when combined with the nine unchanged PASS cases from the full rerun
- Quality gate: **PASS**

All accounts and lifecycle data used in this run were synthetic. No access token, refresh token, OTP, real password, or real health data is retained in this evidence directory.

## Results

| ID | Final result | Verified behavior | Primary evidence |
| --- | --- | --- | --- |
| MF01-MOB-002 | PASS | Selecting a stage exposes `selected=true`, keeps the cards mutually exclusive, and enables the persistent CTA. | `002-stage-initial.*`, `002-planning-selected.*` |
| MF01-MOB-006 | PASS | A clinician-confirmed pregnancy is created immediately; the final payload does not retain LMP state from an earlier calculation method. | `006-method-lmp-start.xml`, `006-lmp-result.xml`, `006-due-date.xml`, `006-created-immediate.*` |
| MF01-MOB-008 | PASS | An existing PRE lifecycle transitions to PREG in place; the UI refreshes immediately and the database retains one journey with two transitions. | `008-pre.*`, `008-preg-immediate.*` |
| MF01-MOB-010 | PASS | Updating EDD refreshes the current journey immediately without relaunch. Final DB state was EDD `2026-07-21`, version `2`, with three transitions. | `010-update-date.xml`, `010-update-immediate-fixed.*` |
| MF01-MOB-014 | PASS | After proper logout, another Mother with zero journeys is routed to stage selection and never sees the previous account's pregnancy/EDD. | `014-account-a-preg.*`, `014-account-b-isolated.*` |
| MF01-MOB-015 | PASS | At 150% text scale and landscape, all four methods, date selection, result content, recalculation, and create CTA remain reachable; selected semantics are exposed. | `015-wizard-landscape-*`, `015-method-selected.xml`, `015-date-landscape*`, `015-result-landscape-*` |
| MF01-MOB-016 | PASS | Creating PRE continues while the app is backgrounded; resuming shows the newly created canonical journey without relaunch or stale state. | `016-selected.*`, `016-resumed.*` |

`010-update-immediate.*` records the diagnostic rerun that exposed the stale async overwrite. The authoritative final evidence for MF01-MOB-010 is `010-update-immediate-fixed.*`.

## Database invariants observed

| Case | Observation |
| --- | --- |
| MF01-MOB-006 | One PREG journey; `last_menstrual_date` is null for the final clinician EDD method; source/confidence are clinician-confirmed/confirmed. |
| MF01-MOB-008 | One canonical journey, version `1`; transitions are `CREATED NULL→PRE_PREGNANCY` and `STAGE_CHANGED PRE_PREGNANCY→PREGNANCY`. |
| MF01-MOB-010 | EDD `2026-07-21`, version `2`, three append-only transitions. |
| MF01-MOB-014 | Pregnancy fixture account has one journey; the second account has zero journeys. |
| MF01-MOB-016 | One PRE journey, version `0`, one CREATED transition after background completion. |

## Automated closure evidence

| Gate | Result |
| --- | --- |
| Story 6.1 mobile gap regression | PASS — `18/18` |
| Full Flutter regression | PASS — `190/190` |
| Targeted Dart analysis | PASS — no issues in Journey, Mother Home, or the Story 6.1 regression test |
| Dart format verification | PASS — seven changed Dart files already formatted |
| Git diff whitespace check | PASS |

The repository-wide `flutter analyze` still reports 19 pre-existing issues in unrelated Community, Consultation, Direct Chat, Expert Home, and Reminder files. None of the seven changed Dart files is included in that output.

The code-review graph reported no mapped execution flow for the seven Flutter files. Therefore, closure relies on the explicit widget/service regression suite plus the physical-device and disposable-database evidence above.
