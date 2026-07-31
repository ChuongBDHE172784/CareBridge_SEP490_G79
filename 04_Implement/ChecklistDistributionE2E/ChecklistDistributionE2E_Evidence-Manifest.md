# Checklist Distribution E2E - Evidence Manifest

**Status:** Partially Implemented  
**As of:** 2026-07-30  
**Release conclusion:** Student-project goal accepted complete with CHK-041 `WAIVED / ACCEPTED RISK`; this evidence is **not** sufficient for production or pilot release readiness.

**Approved environment decision (2026-07-30):** the owner approved a deterministic synthetic production-representative fixture and local disposable PostgreSQL 18 as that CHK-041 environment. This removed the external-backup/host prerequisite only; at that point CHK-041 remained OPEN pending every unchanged gate. The later student-scope waiver below supersedes the blocker disposition, not the technical thresholds, and creates no production-capacity claim.

## Authoritative live evidence

| Field | Value |
|---|---|
| Run ID | `20260730T064329247Z-7f23c4cc65db4e5492709fd2e2253012` |
| Server environment | `e2e-checklist-20260730-1345` (`disposable=true`) |
| Device | `emulator-5554`, Android x64 |
| API origin | `http://10.0.2.2:18080` |
| Canonical target | `integration_test/checklist_distribution_e2e_test.dart` |
| Verdict | PASS; Flutter exit 0; target 1/1; visible 1; skipped 0; malformed 0; overall done 1/success |
| Sanitized log SHA-256 | `2daa30ea7ef0720169860e35f632d59351e86c2fe6941eb275121a13c9ef4c5c` |
| Local sanitized evidence | `D:\tmp\carebridge-checklist-live-20260730-1345\evidence\20260730T064329247Z-7f23c4cc65db4e5492709fd2e2253012-checklist-e2e-evidence.json` |
| Cleanup | Port 18080 closed; defines, ready and stop artifacts absent |
| Leak scan | 0 missing redaction markers; 0 encoded credential prefixes; 0 JWTs; 0 raw credential assignments; hash matched |

The live artifact remains outside the repository by design. It contains sanitized evidence only; the short-lived credential artifact was deleted by the host cleanup boundary.

## Final verification manifest

| Gate | Result |
|---|---|
| Backend focused clean snapshot | 24 passed, 0 failed/errors; 1 environment-gated seeder test skipped |
| Disposable API/PostgreSQL host | 1/1 passed; 24 Flyway migrations validated and applied |
| Flutter focused regression | Prior checklist suite 34/34; user-create/metadata/wiring delta 9/9 passed |
| Flutter full regression | 575/575 passed |
| Flutter targeted analysis | No issues found |
| Flutter full analysis | No issues found |
| Flutter Android debug build | PASS; `app-debug.apk` produced (future Kotlin Gradle Plugin migration warning only) |
| Web regression | 76/76 passed |
| Web lint/build | 0 lint errors, 1 pre-existing warning; production build passed |
| Runner hermetic suite | 23/23 passed |
| Independent reviews | Backend terminal delta P0=0/P1=0; runner final P0=0/P1=0/P2=0; final Mobile/UI/runbook delta APPROVE with P0=0/P1=0 and three non-blocking test-hardening P2s |

## Authoritative scenario matrix

`PASS` means the approved behavior has automated/local or live evidence recorded in the Test-Spec. `OPEN` means the release condition has not been executed in its required environment. `WAIVED / ACCEPTED RISK` means the condition did not pass or was not completed, but the named owner explicitly accepted that residual risk for the stated scope; it must never be read as PASS.

| Scenario | Status | Primary evidence |
|---|---|---|
| CHK-001 | PASS | Phase 2 API/Web authoring acceptance |
| CHK-002 | PASS | Phase 2 FAMILY normalization acceptance |
| CHK-003 | PASS | Phase 2 multi-role API acceptance |
| CHK-004 | PASS | Phase 2 target validation/snapshot acceptance |
| CHK-005 | PASS | Approval immutability API/PostgreSQL tests |
| CHK-006 | PASS | Version clone lineage tests |
| CHK-007 | PASS | Migrated review/activation tests |
| CHK-008 | PASS | Pregnancy lifecycle boundary tests |
| CHK-009 | PASS | Postpartum/DST lifecycle tests |
| CHK-010 | PASS | Baby-care anchor/range tests |
| CHK-011 | PASS | PostgreSQL lifecycle concurrency evidence |
| CHK-012 | PASS | Recipient distribution integration tests |
| CHK-013 | PASS | Family recipient-owned instance tests |
| CHK-014 | PASS | Family permission truth-table security tests |
| CHK-015 | PASS | Family multi-group union/isolation tests |
| CHK-016 | PASS | Revoke/regrant/relink lifecycle tests |
| CHK-017 | PASS | Fail-closed context ownership/404 tests |
| CHK-018 | PASS | Reconciliation concurrency and golden-key tests |
| CHK-019 | PASS | Distribution conflict quarantine tests |
| CHK-020 | PASS | Legacy template backfill tests |
| CHK-021 | PASS | Legacy target/context migration tests |
| CHK-022 | PASS | Occurrence grouping/custom migration tests |
| CHK-023 | PASS | Legacy status/timestamp/unscheduled tests |
| CHK-024 | PASS | Quarantine privacy/integrity tests |
| CHK-025 | PASS | Legacy compatibility/cutover tests |
| CHK-026 | PASS | Unified Today provider/API tests |
| CHK-027 | PASS | Timezone/bucket boundary tests |
| CHK-028 | PASS | Unified action/idempotency tests |
| CHK-029 | PASS | V2 API/PostgreSQL origin-target tests plus Mother Today production composer: explicit context/target, idempotent retry and refresh wiring |
| CHK-030 | PASS | System immutability and terminal action tests |
| CHK-031 | PASS | React authoring controls tests |
| CHK-032 | PASS | Mother Home Today widget tests + live run |
| CHK-033 | PASS | Family Home context/isolation tests + live run |
| CHK-034 | PASS | Mother/View Content navigation tests; lifecycle-to-generic content CTA regression restored and 6/6 file tests pass |
| CHK-035 | PASS | Route/import dead-entry sweep |
| CHK-036 | PASS | Accessible loading/error/offline widget tests |
| CHK-037 | PASS | Typed audit allowlist/integration tests |
| CHK-038 | PASS | PostgreSQL business/audit atomicity 9/9 |
| CHK-039 | PASS | Retention/legal-hold/security tests |
| CHK-040 | PASS | Reconciliation health/replay tests |
| CHK-041 | WAIVED / ACCEPTED RISK | Owner accepted closure for student-project scope on 2026-07-30. Latest disposable run failed Today with Hikari `total=10, active=10, waiting=40`; later reconciliation/abort/roll-forward phases did not execute. Not valid for production/pilot release. |
| CHK-042 | PASS | Authoritative live run: Mother completion, stable refresh, persistence and audit |
| CHK-043 | PASS | Authoritative live run: Family completion and cross-family isolation |

## CHK-041 resumed-environment readiness audit — 2026-07-30

This read-only audit rechecked the current workspace, `D:\tmp`, the process environment, installed Windows commands/services and WSL before attempting the external rehearsal. No credential values were printed or copied.

| Prerequisite | Current evidence | Readiness |
|---|---|---|
| PostgreSQL 18 disposable server capability | Cached embedded server reports PostgreSQL `18.1` | Available for local disposable tests only |
| `psql` and `pg_restore` | PostgreSQL 18.1 portable client installed at `D:\tmp\carebridge-pg18-client\pgsql\bin`; `psql`, `pg_restore` and `pg_dump` all report `18.1` | Available |
| Docker disposable host | No Windows Docker command/service; WSL Docker shim reports that no supported daemon integration is available | Missing |
| Rehearsal connection/authentication | User-scoped `CAREBRIDGE_PSQL_PATH` now points to the verified portable client; `CAREBRIDGE_CHECKLIST_REHEARSAL_DB_URL` and `PGPASSFILE` remain unset | Partially available |
| Approved production-representative input | External sanitized backup absent; owner approved a deterministic synthetic production-representative fixture on 2026-07-30 | Approved substitution; sealed fixture artifact still pending |
| Independent immutable inputs | No approved dataset fingerprint, Flyway-history hash, quarantine baseline or cohort-disabled attestation found in scope | Missing |
| Local/synthetic evidence | Existing embedded-PostgreSQL thresholds and synthetic verifier controls remain green | Prior evidence cannot close CHK-041; one new sealed approved-reference run is required |

The portable client came from the official EnterpriseDB PostgreSQL `18.1-1` Windows x64 binary archive. The downloaded archive length (`334,929,717` bytes) and MD5 (`fcd00f82a177b12b31c01a83f3e4ff53`) match the HTTPS response metadata; its recorded SHA-256 is `b2a7302ecb78088209edd96a936ec360fc12c58ec27dc8cc4e8ce5aefb9f8d3b`. A fail-closed verifier preflight resolved this `psql` successfully and stopped at `CHECKLIST_REHEARSAL_DATABASE_URL_REQUIRED`; it created no evidence directory.

The next executable production step remains `CapturePre`, but it is prohibited until the approved backup is restored into a disposable environment and the independent fingerprint/authentication prerequisites in the runbook are supplied.

## CHK-041 owner-accepted waiver evidence — 2026-07-30

| Evidence | Result |
|---|---|
| Disposable run | `chk041-20260730t120807436z-5ff3efcea9d94061ba9ef93de3ac43cf` on local PostgreSQL 18 |
| Completed before failure | Clean chain, corrected clean chain, normal expand, `CapturePre`, remainder and `VerifyPost` |
| Failure | Today concurrent load returned non-200 after Hikari timeout: `total=10, active=10, idle=0, waiting=40` |
| Not executed | Final Today metrics, reconciliation load, `ABORT_PROVEN`, corrective forward-only run and `ROLL_FORWARD_PASS` |
| Cleanup | 0 portable PostgreSQL processes; 0 active `carebridge-chk041-*` run roots |
| Post-failure narrow verification | 11 tests, 0 failures/errors/skips (`CareTaskTodayTaskProviderTest`, fixture contract and runner contract) |
| `runner-failure.json` SHA-256 | `d9163847c5ee1e5bacd29af20974f13a4488ec593b1a2771e8bedd8a042a7562` |
| `runner-progress.log` SHA-256 | `a3a6bfba68afe492679857d70fc8f8cf15231ccc8d36c552d26a5240bf22ad3e` |
| `normal/today-raw-flyway.log` SHA-256 | `5763314f86ce7bbd7a495308c36c937c36e569eff51157a7f529f6cc7ad3be51` |
| `VerifyPost` verdict SHA-256 | `fd424c8c4f3df634b959116ed11f3f6a3cc5861316ffa04a333aa8684797c714` |
| Owner decision | Performance is non-critical for this student project; close the goal with CHK-041 explicitly waived, not passed |

Any future production or pilot release must reopen CHK-041 and execute every unchanged threshold and safety phase.

## Open non-blocking engineering follow-ups

- Optimize Today provider/label resolution so cost does not grow with all historical non-cancelled checklist instances.
- Extend terminal projection tests for SKIPPED, day boundaries, null/out-of-day timestamps and provider timestamp mapping.
- Capture one request-scoped clock instant for both bucket computation and response `asOf`.
- Validate extreme `LocalDate` inputs before horizon arithmetic.
- Add a Mother Home integration widget test that drives the real create CTA through POST and verifies the shared Today panel reload.
- Extend composer boundary tests for dual-context rejection, changed-payload client ID rotation and default UUID validity.
- Add a narrow-viewport 200% text-scale regression for the Today heading plus create action.

These are P2 follow-ups from independent review and do not reopen CHK-042/043. CHK-041 is waived only for the student-project goal and remains a mandatory gate for any production/pilot release.
