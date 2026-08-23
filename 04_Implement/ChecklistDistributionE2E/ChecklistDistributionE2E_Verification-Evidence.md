> [!IMPORTANT]
> Historical cross-UC verification evidence for `UC-AD-10`, `UC-AD-11`, and `UC-MH-17`; this is not a canonical current Test-Spec. Current code and the canonical code-first specifications override conflicts.

# Checklist Distribution E2E — Test Specification

**Status:** Complete for accepted student-project scope — 2026-07-30 (42 PASS, CHK-041 `WAIVED / ACCEPTED RISK`; not a technical PASS)
**Date:** 2026-07-29
**Contract under test:** [ChecklistDistributionE2E_Architecture-Evidence.md](ChecklistDistributionE2E_Architecture-Evidence.md)

## 1. Objective and evidence model

Prove that template approval, lifecycle eligibility, recipient isolation, exactly-once distribution, Today projection, task actions, migration, audit and UI cutover satisfy the approved contract. Tests use fixed clocks, IANA timezones, deterministic UUIDs and PostgreSQL where database uniqueness/locking is the behavior under test.

Evidence must include automated reports, database assertions for migration/concurrency, sanitized logs/audits, and screenshots/semantics output for UI. Mock-only evidence cannot close authorization, migration, uniqueness or atomic-audit risks.

## 2. Test data

Reference-environment decision (approved 2026-07-30): CHK-041 may close on a sealed deterministic synthetic production-representative dataset running on a local disposable PostgreSQL 18 cluster. This is an equivalence decision for the missing sanitized external backup/host only; all numeric thresholds, same-dataset/same-threshold abort-to-forward-correction controls, RLS/no-secret checks, cleanup and independent review remain mandatory. Conclusions are limited to the recorded local host profile.

- Owners M1/M2 with separate journeys, babies and care groups G1/G2.
- Family F1 accepted in G1 with no permissions, VIEW-only, COMPLETE-only and both permissions; F2 accepted in G2; revoked/pending members.
- Approved template versions for MOTHER, FAMILY and both; lifecycle-aware and FAMILY-only; mixed MOTHER/BABY items.
- Boundary contexts for exact pregnancy/postpartum/baby substage start/end values.
- Legacy fixtures covering ordered rows, missing due dates, custom rows, `PENDING`, `IN_PROGRESS`, completed, contradictory timestamps, unknown roots and conflicting contexts.

## 3. Atomic acceptance scenarios

| ID | Behavior and expected result | Level | Priority | Risk |
|---|---|---|---|---|
| CHK-001 | Create/update a MOTHER template with valid stage/substage; API round-trip is lossless. | API + Web component | P1 | R-CHK-07/R-CHK-14 |
| CHK-002 | Select FAMILY only; UI clears stage/substage, backend normalizes stale hidden values to null, and neutral-window approval/membership reconciliation distributes without lifecycle dates. | API + Web component | P1 | R-CHK-14 |
| CHK-003 | Select both roles; valid stage/substage remains and separate eligible recipients can be projected. | API | P1 | R-CHK-07 |
| CHK-004 | Require one MOTHER/BABY target per item and preserve mixed-target payload/snapshots; reject missing/unknown target. | API + Web component | P1 | R-CHK-08/R-CHK-14 |
| CHK-005 | Approve DRAFT; all approved fields become immutable and direct update returns `VERSION_IMMUTABLE`. | API + PostgreSQL | P1 | R-CHK-07 |
| CHK-006 | Clone approved version; lineage is stable, version ID changes, version number increments, old snapshots remain unchanged. | API + PostgreSQL | P1 | R-CHK-07 |
| CHK-007 | Migrated template is review-required/distribution-disabled; legacy `NONE`/unsupported stage metadata must be corrected before explicit SYSTEM_ADMIN review and activation, and any later edit invalidates that review. | API + PostgreSQL | P1 | R-CHK-01 |
| CHK-008 | Pregnancy LMP/EDD anchor is explicit; missing/contradictory anchor fails safely; DAY/WEEK/MONTH inclusive lower/upper and adjacent boundaries follow the normative calendar algorithm. | Unit + API | P1 | R-CHK-09 |
| CHK-009 | Postpartum DELIVERY_DATE ranges and local-start dueAt conversion follow the normative day/week/month, month-clamp and DST rules. | Unit + API | P1 | R-CHK-09 |
| CHK-010 | Baby-care ranges use BIRTH_DATE and never Mother lifecycle dates; malformed/negative ranges are rejected. | Unit + API | P1 | R-CHK-09 |
| CHK-011 | Lifecycle correction cancels only obsolete pending work, creates one replacement and preserves started/terminal history. | API concurrency | P0 | R-CHK-04 |
| CHK-012 | Eligible Mother receives exactly one instance and one child per versioned item. | API + PostgreSQL | P1 | R-CHK-03 |
| CHK-013 | Each eligible Family recipient receives a separate recipient-owned instance; Mother work is not reused. | API + PostgreSQL | P1 | R-CHK-02/R-CHK-03 |
| CHK-014 | Assert the exact Family truth table including direct IDs: none=no read/action; VIEW-only=read/no action; COMPLETE-only=no read/action; VIEW+COMPLETE=read/action. | Security API | P0 | R-CHK-02/R-CHK-06 |
| CHK-015 | Same Family account receives the exact union of ACCEPTED+VIEW groups; pending/revoked/unpermitted groups are absent and items/counts never merge across group/context IDs. | Security API | P1 | R-CHK-02 |
| CHK-016 | Membership revoke immediately denies old access without mutating history; regrant reuses the same deterministic instance for the same group/context/window, while a genuinely relinked context creates one new instance. | Security + API | P1 | R-CHK-06 |
| CHK-017 | Context whose journey/baby owner differs from care-group owner is denied, quarantined and not distributed; nonexistent and unauthorized direct IDs return indistinguishable `404 TASK_NOT_FOUND` envelopes. | Security + PostgreSQL | P0 | R-CHK-02 |
| CHK-018 | Repeated outbox events, hourly/startup scans and concurrent workers reuse the persisted parent ID, create no duplicate/orphan rows, and match v1 SHA-256 golden key vectors. | PostgreSQL concurrency | P0 | R-CHK-03 |
| CHK-019 | Same deterministic key with a different canonical payload is not overwritten; conflict is quarantined/alerted. | PostgreSQL integration | P1 | R-CHK-03 |
| CHK-020 | Legacy templates backfill MOTHER, catch-all substage and review/disabled flags without task flood. | Flyway integration | P1 | R-CHK-01 |
| CHK-021 | Legacy targets map BABY_CARE/baby_id to BABY only when baby→journey/group-owner relationships validate; invalid relationships quarantine; other valid rows map MOTHER with source IDs retained. | Flyway integration | P1 | R-CHK-01 |
| CHK-022 | Legacy rows group by owner/context/lineage-version plus deterministic occurrence token (root/instance/date); collisions quarantine, custom rows become standalone user-created instances and order is preserved. | Flyway integration | P0 | R-CHK-01 |
| CHK-023 | `PENDING`, `IN_PROGRESS`, completed/skipped and timestamps map to the normative persisted states without losing terminal or started semantics; unknown due date remains active and appears only in the UNSCHEDULED bucket. | Flyway integration | P0 | R-CHK-01 |
| CHK-024 | Contradictory timestamps, unknown roots and ambiguous contexts enter quarantine. With managed encryption configured, authorized decrypt round-trip matches the source hash, unauthorized roles cannot decrypt, rotation preserves recovery, and every decrypt attempt audits. In approved redacted mode, no source payload is stored: only an opaque random marker whose integrity hash matches, plus source ID/reason; normal roles cannot read it and operations-only resolution remains audited. | Flyway integration | P0 | R-CHK-01 |
| CHK-025 | Legacy reminder endpoint preserves its old response schema and legacy action routes adapt to V2; disabling distribution/UI flags never restores legacy writes or hides V2 completion/actions. | API compatibility | P1 | R-CHK-05 |
| CHK-026 | Today aggregates CHECKLIST, REMINDER and CARE_TASK occurrences with stable taskKind/ID, origin/target/status/timeBucket/care context metadata and deterministic overdue/today/upcoming/unscheduled sections. | API contract | P1 | R-CHK-08/R-CHK-09 |
| CHK-027 | Effective timezone/date metadata and midnight/DST boundaries place each task in exactly one bucket. | Unit + API | P1 | R-CHK-09 |
| CHK-028 | Per-kind action capability matrix is honored; complete/skip retry with same actor/taskKind/task/clientRequestId/payload returns original result, changed payload returns `IDEMPOTENCY_KEY_REUSE`, and authorization is checked before replay. | API + service | P1 | R-CHK-04/R-CHK-08 |
| CHK-029 | User-created task requires explicit target and has `USER_CREATED`; Mother Today provides the production create caller with exactly one context and idempotent retry, while system items retain `SYSTEM_TEMPLATE`. | API + Flutter widget | P1 | R-CHK-08/R-CHK-14 |
| CHK-030 | Recipient edit/delete on a system task returns `SYSTEM_TASK_IMMUTABLE`; authorized complete/skip remains available. | Security API | P1 | R-CHK-08 |
| CHK-031 | Admin role/stage/substage/target controls follow conditional rules and submit normalized payload. | React component | P1 | R-CHK-14 |
| CHK-032 | Mother Home renders each Today section, source/target/state and invokes unified action facade. | Flutter widget | P1 | R-CHK-13/14 |
| CHK-033 | Family Home renders only permitted recipient-owned tasks and preserves group/context labels. | Flutter widget + API | P1 | R-CHK-02/13 |
| CHK-034 | Mother Home and View Content replacement routes land on Today Tasks before old screen is removed. | Flutter navigation | P1 | R-CHK-13 |
| CHK-035 | Repository route/import sweep finds no live PreparationChecklistScreen entry or dead navigation. | Static + Flutter | P1 | R-CHK-13 |
| CHK-036 | Loading, empty, retryable error, terminal error and offline-safe states are accessible and deterministic. | React/Flutter component | P1 | R-CHK-14 |
| CHK-037 | Each distribution/action/cancel/failure/quarantine emits the expected action, actor type/service, recipient/context/task IDs, before/after status, reason and correlation ID through the allowlisted audit policy with no free text in DB/log/error/quarantine output. | API integration | P0 | R-CHK-10 |
| CHK-038 | Injected audit-policy, DTO-serialization or persistence failure rolls back the business mutation; no state without audit and no audit without state. | PostgreSQL integration | P0 | R-CHK-10 |
| CHK-039 | Audit/quarantine read denies normal/admin-content users, allows operations scope, honors legal hold and seven-year createdAt-based retention with audited purge. | Security + policy | P1 | R-CHK-10 |
| CHK-040 | Durable outbox plus overlapping-watermark hourly/startup reconciliation repairs a missed event; stale >2h and exhausted retry alert; replay by correlation is idempotent. | Service/observability | P1 | R-CHK-11 |
| CHK-041 | At 50 RPS/500 tasks/20 groups, Today meets p95 ≤500 ms and p99 ≤1 s; 10,000 candidates finish ≤15 min; blocking Flyway lock ≤5 s; backfill ≥500 rows/s and total migration ≤30 min, with abort/roll-forward criteria verified. | PostgreSQL/load | P1 release blocker | R-CHK-12 |
| CHK-042 | Mother E2E: approve eligible template → distribute → display Today → complete → persist/audit → stable refresh. | Mobile/API E2E | P1 | R-CHK-03/R-CHK-08 |
| CHK-043 | Family E2E: accepted+permitted member receives own instance → views/completes → Mother/other family isolation remains intact. | Mobile/API E2E | P0 | R-CHK-02/R-CHK-03/R-CHK-08 |

Priority indicates risk, not execution timing.

## 4. Concurrency and negative-test protocol

Run CHK-011, CHK-018, CHK-028 and CHK-038 against PostgreSQL with synchronized barriers and at least two independent transactions. Assert row counts, versions, terminal state and audit rows after every run. Repeat concurrency cases nightly to detect flaky races. For every authorized read/action, add direct-ID attempts by M2, F2, revoked F1 and accepted-but-unpermitted F1; responses must not disclose resource existence beyond the standard denial envelope.

## 5. Execution strategy

- **PR:** Run all affected unit, controller, contract, component/widget and focused PostgreSQL integration tests when the aggregate remains under 15 minutes. This includes all relevant P0 authorization and state-policy cases.
- **Nightly:** Repeat concurrency, migration fixtures, reconciliation fault injection, compatibility and both role E2E paths.
- **Weekly/pre-release:** Run production-volume migration/load, query-plan checks, accessibility pass and roll-forward/flag-disable rehearsal.

## 6. Entry and exit gates

Entry requires approved SPEC/TDS, seeded test factories, a disposable PostgreSQL database, fixed-clock support, V2 flag control and no unresolved schema/enum ambiguity.

Exit requires:

- P0 pass rate 100%; P1 at least 95%; no open severity P0/P1 defects.
- Every score-9 risk mitigated; every score-6 risk evidenced or explicitly waived by an owner with date.
- Changed-code line coverage at least 80% plus branch coverage for eligibility, mapping, state and authorization policies.
- Zero cross-family leaks, duplicate distribution rows, lost terminal states, audit-text leakage and dead old-screen routes.
- Flyway count/hash reconciliation and seeded `IN_PROGRESS` preservation pass.
- Backend affected tests/package, Web lint/build and Flutter tests/analyze pass.
- A production/pilot release still requires the approved reference thresholds and CHK-041 to pass. For the student-project goal only, the owner explicitly accepted a dated CHK-041 waiver.

## 7. Planned effort and follow-on

Test engineering estimate: P0 ~60–100 hours, P1 ~65–110 hours, P2 0 hours, P3 0 hours and shared harness/environment setup ~15–30 hours; total ~140–240 hours (roughly 4–8 team-weeks depending on parallel staffing and environment readiness). After implementation stories exist, run ATDD explicitly for P0 scaffolds, then automate the broader plan; neither workflow is implied complete by this document.

## 8. Incremental implementation evidence — 2026-07-29 to 2026-07-30

- Phase 1 foundation/hardening: focused schema/domain/key/audit/migration-verification and Family-permission evidence passed; PostgreSQL/Testcontainers execution remains pending because Docker/`psql` is unavailable.
- Phase 2 authoring/approval: CHK-001..007 and CHK-031 focused contracts were implemented through recorded RED→GREEN cycles. The final independent acceptance pass reported no surviving blocker.
- Latest Phase 2 evidence: backend focused acceptance 32/32 passed, Web Content Management 48/48 passed, backend package passed, Web production build passed, and ESLint reported zero errors (one pre-existing warning outside the checklist files).
- Phase 3 reliability/compatibility evidence: the final explicit checklist backend run covered 36 suites and 148 tests with 0 failures, errors or skips; the approval/authoring regression run covered 4 suites and 51 tests with 0 failures, errors or skips; backend packaging passed. Reminder occurrence, legacy target backfill, reconciliation claim/replay, lifecycle lock-order, audit/operations and migration contract tests were included in these runs. RED evidence was captured before each newly added contract patch.
- 2026-07-30 PostgreSQL evidence is now available: occurrence-repair migration 2/2, Family multi-group lifecycle 8/8, authorization/Today API 9/9 and business/audit atomicity 9/9 passed. Independent Blind, Edge and Acceptance reviews found no remaining code P0/P1 after the final typed-audit and fail-closed context patches.
- CHK-041 standalone reference-volume performance snapshot was GREEN with production-like INFO logging: Today p95 442.29 ms and p99 660.22 ms at 500 tasks/20 groups/250 requests; reconciliation processed 10,000 candidates in 10.43 s (958.68 rows/s); Flyway lock completion was 1.29 s; legacy backfill was 5,983.44 rows/s; occurrence repair was 1,506.43 rows/s; and full seeded migration was 10.70 s. The preceding idle-host run with the repository test profile's synchronous DEBUG logging was RED at Today p95 581.85 ms. This standalone snapshot did not close CHK-041; the later integrated disposable run failed as recorded below and was subsequently waived only for student-project scope.
- Changed-code JaCoCo line coverage for `ChecklistReconciliationService`, `UnifiedTaskAccessPolicy`, `CareGroupServiceImpl` and `JpaChecklistReconciliationSource` is 811/1,004 lines (80.78%). Recorded branch coverage is respectively 70.63%, 76.67%, 57.07% and 64.06%.
- CHK-042/043 are PASS. Authoritative Android/API/PostgreSQL run `20260730T064329247Z-7f23c4cc65db4e5492709fd2e2253012` used `emulator-5554`, origin `http://10.0.2.2:18080` and disposable environment `e2e-checklist-20260730-1345`. The hardened runner recorded Flutter exit 0, target 1/1, visible 1, skipped 0, malformed 0, overall done 1/success and sanitized log SHA-256 `2daa30ea7ef0720169860e35f632d59351e86c2fe6941eb275121a13c9ef4c5c`. Cleanup and leak scans passed. The run covered author/approve/reconcile, Mother and Family production Home screens, separate recipient-owned IDs, direct-ID 404 isolation, unrelated-family exclusion, COMPLETE persistence, stable terminal refresh and typed audits.
- Migration rehearsal gate implementation is GREEN locally: strict RED evidence preceded the patch; final focused service/readiness/controller and real PostgreSQL 18.1 script/RLS suites passed 11/11. The script also has positive/negative control-flow evidence for checksum, fingerprint, RLS, dual outcome, raw throughput, abort proof and sealed roll-forward binding. The production-like rehearsal itself remains OPEN.
- Flutter verification is green: the prior focused regression remains 34/34, the new user-created composer/metadata/wiring delta passed 9/9, and the full suite passed 575/575. Full Flutter analyze reports no issues and `flutter build apk --debug` succeeds. The full-suite RED exposed a missing lifecycle-to-generic-content CTA; the production CTA was restored and the six-test regression file now passes.
- Final regressions: backend focused snapshot 24 passed/0 failed with one environment-gated seeder test skipped, plus the disposable live host test 1/1 PASS; Web 76/76 tests PASS, lint 0 errors with one pre-existing warning, production build PASS; runner hermetic 23/23 PASS with independent P0/P1/P2=0; focused Flutter 34/34 PASS; authoritative live E2E PASS.
- All 43 scenarios are dispositioned for the accepted student-project scope: 42 PASS and CHK-041 `WAIVED / ACCEPTED RISK`. The waiver followed a full disposable attempt that passed clean/corrected chains, `CapturePre`, remainder and `VerifyPost`, but failed Today because Hikari exhausted its 10-connection pool (`40` requests waiting) and returned non-200 responses. Reconciliation load, abort and roll-forward phases were not executed. The post-failure batching/logging changes passed 11/11 narrow tests but were not promoted to a CHK-041 PASS. Production or pilot use must reopen the unchanged gate.

The authoritative per-scenario status and evidence metadata are maintained in [ChecklistDistributionE2E_Evidence-Manifest.md](ChecklistDistributionE2E_Evidence-Manifest.md).
