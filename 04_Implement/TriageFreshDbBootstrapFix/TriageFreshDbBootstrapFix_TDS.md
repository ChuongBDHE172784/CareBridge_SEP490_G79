# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# TriageFreshDbBootstrapFix — Fresh-DB Flyway Bootstrap Repair + startConversation Persistence Fix

| Field | Value |
|-------|-------|
| **Document ID** | `CB-TRIAGE-FDBB-IMP-001` |
| **Version** | `1.0` |
| **Date** | `2026-07-27` |
| **Status** | `Implemented` *(2026-07-27 — all 4 TCs verified by actual runs: red gate captured, 6-suite fresh-chain run BUILD SUCCESS 15/15, full regression categorized with zero failures attributable to this change; see Test-Spec §4/§5)*. Spec approval: *project owner directive 2026-07-27 (verbal: "làm theo cách bạn cảm thấy phù hợp nhất"); owner also directed: minimize/avoid new DB tables ("hạn chế tạo bảng mới nhất có thể")* |
| **Document Owner** | `AI Agent` |
| **Author** | `AI Agent` |
| **Reviewed by** | `[x] Project owner (verbal directive 2026-07-27)` |
| **DPO Sign-off** | `N/A` — no new PII processing; migration scaffolding + persistence-path bug fix only |
| **Approved by** | `Project owner (verbal, 2026-07-27)` |
| **Last Review** | `2026-07-27` |
| **Based on EDS** | `v2.0` |

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Never delete old information. Every change must be recorded in this table.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-27 | AI Agent | Initial creation — TDS for the three confirmed pre-existing defects D1/D2/D3 (see §1); Status set to Approved per explicit project-owner directive of 2026-07-27. |
| 2026-07-27 | AI Agent — Amelia (Dev Agent) | Implementation complete per §11: (1) `V20260724120000__fresh_db_bridge_bootstrap.sql` + (2) `V20260724214150__restore_legacy_expert_columns_for_fresh_db.sql` added (guarded no-ops per ADR-TFBF-002); (3) harness workarounds removed (`testsupport/bridge-bootstrap.sql` + `db/testfix/` deleted, `AbstractPostgresIntegrationTest` reverted to `classpath:db/migration`, no init script); (4) D3 fixed per ADR-TFBF-003 (`TriageService.startConversation`: id no longer pre-assigned in the builder; assigned only inside the databaseArbitrated branch for the native-insert writer). Verification (all actual runs 2026-07-27): red gate 500/StaleObjectState captured (red-gate-evidence.log); new `TriageConversationStartIntegrationTest` 2/2 green; 6-suite fresh-chain run BUILD SUCCESS 15/15 with Flyway log showing baseline → 120000 → …214150 → 214200 → 20260726150000; full regression 3166 run / 24 F / 41 E — every failure categorized in Test-Spec §4 TC-INT-04, ZERO attributable to this change (all pre-existing: baseline replay-semantics family, AI-moderation table drift, B-filename policy, known seeder/SHA defects). A/B holdout proof: with BOTH new migrations temporarily removed, `CanonicalSafetyActionInvariant`+`CanonicalExpertProfileReference` degrade from 3 errors to 10/10 errors, all `relation "carebridge_migration_bridge.story66_notification_outbox_bridge" does not exist` — the new migrations strictly fix 7 replay tests and break none. Net-zero schema confirmed: end-state inventory assertions list no bridge objects/legacy columns. |

---

## 1. Tổng quan Module

Three confirmed, pre-existing defects block (a) bootstrapping the backend schema on any EMPTY PostgreSQL database and (b) one production triage entry path:

* **D1 — Fresh-DB Flyway bootstrap fails.** On an empty database Flyway takes the baseline path: it executes `B20260724111500__canonical_70_table_baseline.sql` and SKIPS every pre-baseline migration. But the immutable POST-baseline migrations `V20260724210000` (and `211000`/`211500`/`212450`) still reference `carebridge_migration_bridge.*` tables that were only ever created by PRE-baseline migrations (`phase2/V20260722231360`, `V20260722019950`, `V20260722020450`, `phase2/V20260722231950`). First observed error: `ERROR: relation "carebridge_migration_bridge.story66_notification_outbox_bridge" does not exist` inside `V20260724210000`. Additionally `V20260724212450` REQUIRES exactly one row in `carebridge_migration_bridge.story65_branch_history_state` (raises on 0 rows); the synthetic row `('V20260722019950', false, false)` makes its checks reduce to "no legacy shadow tables exist" assertions, which are true on the canonical baseline.
* **D2 — `V20260724214200` gates on legacy `expert_profile_id` columns** in `expert_credentials` / `expert_availability` / `expert_location_shares`. The baseline is an END-STATE snapshot that only carries the canonical `professional_profile_id` column, so on the baseline path `V20260724214200` fails with `CANONICAL_EXPERT_REFERENCE: expert_credentials must contain both legacy and canonical identifiers`.
* **D3 — Production bug in `TriageService.startConversation`** (`triage/service/impl/TriageService.java`, session-creation block ~:325-347): the method builds the `IntakeSession` with `.id(UUID.randomUUID())` although the entity id is `@GeneratedValue(strategy = GenerationType.UUID)`. When the optional `clientRequestId` is ABSENT, the code path calls `intakeSessionRepository.save(session)`; Spring Data sees a non-null id → `em.merge()` → Hibernate `StaleObjectStateException` → HTTP 500 on real PostgreSQL. The `clientRequestId` path goes through `IntakeSessionWriter.insertConversationIfAbsent` (native insert consuming the pre-assigned id) and works.

Current state: D1/D2 are masked by TEST-HARNESS-ONLY workarounds (`src/test/resources/testsupport/bridge-bootstrap.sql` container init script, `src/test/resources/db/testfix/V20260724214150__testfix_restore_legacy_expert_columns.sql`, and two overrides in `AbstractPostgresIntegrationTest`). Those mask the defect for tests only — a real fresh deployment still fails. This feature fixes the REAL chain and removes the harness workarounds so integration tests validate the true chain.

| Field | Value |
|-------|-------|
| **Module Name** | `Triage Fresh-DB Bootstrap Fix` |
| **Bounded Context** | `db/migration (Flyway chain)` + `triage` |
| **Data Classification** | `Internal` — bridge scaffolding tables carry no rows on the fresh path (one synthetic flag row); D3 touches persistence mechanics only |
| **Compliance Scope** | `N/A` (no new data category; existing safety assertions in the chain are preserved verbatim) |
| **Upstream Dependencies** | Flyway chain (`classpath:db/migration`), `IntakeSessionWriter`, `IIntakeSessionRepository` |
| **Downstream Consumers** | Every integration test extending `AbstractPostgresIntegrationTest`; any fresh deployment; `POST /api/v1/triage/intake/conversation/start` clients |

**Source baseline (verified 2026-07-27):**

* `05_Development/CareBridgeAPI/src/main/resources/db/migration/` — chain inspected end-to-end; versions `20260724120000` and `20260724214150` are FREE in the main tree (no collision, incl. other sessions' `V20260726100000`/`V20260726150000`).
* `application.yaml:16-19` — `spring.flyway.out-of-order: true` project-wide, so already-migrated team databases apply the two new lower-version migrations out-of-order (as guarded no-ops).
* `V20260724210000` :555-556 drops the two story66 bridges (no `IF EXISTS`); `V20260724212450` :10-17 requires exactly one story65 state row and :59 drops the table; `V20260724211500` :472-476 drops the story68 tables with `IF EXISTS`.
* All five bridge tables are dropped only at versions ≥ `20260724210000` → at chain position `20260724120000` they still exist on the legacy (non-baseline) path, so `CREATE ... IF NOT EXISTS` / `ON CONFLICT DO NOTHING` are exact no-ops there.
* `TriageService.java` :304-361 (`startConversation`), `IntakeSessionWriter.java` :20-54 (native insert binds `candidate.getId()` — REQUIRES a pre-assigned id), `IntakeSession.java` :19-22 (`@Id @GeneratedValue(strategy = GenerationType.UUID)`).
* `runIntake` (`TriageService.java` :499-510) already uses the correct pattern: builder WITHOUT id + `repository.save()` → Hibernate generates the id. D3 aligns `startConversation`'s save() branch with it.
* Unit-test compatibility verified: `TriageServiceTest`/`TriageServicePreScreenTest`/`TriageServiceHealthMemoryContextTest` stub `save()` to assign an id to the passed entity (JPA-like), so removing the builder-assigned id does not break them.
* Migration-replay suites `CanonicalSafetyActionInvariantMigrationIntegrationTest` and `CanonicalExpertProfileReferenceMigrationIntegrationTest` run raw `Flyway.configure().target(...)` on empty containers — the new migrations execute inside their replays; guard/no-op behavior on the legacy path keeps their seeded scenarios byte-identical.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại | Mô tả yêu cầu | Thành phần Code | Compliance Target | ADR liên quan |
|----------------|------|---------------|-----------------|-------------------|---------------|
| BR-FDBB-001 | Business Rule | An empty database must bootstrap to the canonical schema with plain `flyway migrate` — no manual steps, no test-only init scripts | `V20260724120000__fresh_db_bridge_bootstrap.sql`, `V20260724214150__restore_legacy_expert_columns_for_fresh_db.sql` | — | ADR-TFBF-001 |
| BR-FDBB-002 | Business Rule | Applied/committed migrations are immutable — the fix must be strictly additive to the chain | new migration files only | CLAUDE.md Delivery Rules | ADR-TFBF-001 |
| BR-FDBB-003 | Business Rule | Databases that already ran the full legacy chain must be bit-for-bit untouched (guarded no-ops, incl. out-of-order application) | guard `DO` blocks keyed on `flyway_schema_history` | — | ADR-TFBF-002 |
| BR-FDBB-004 | Business Rule | No new BUSINESS tables; bridge scaffolding lives only in `carebridge_migration_bridge` and is consumed/dropped by the existing chain (net-zero schema on both paths) | both new migrations | Owner directive 2026-07-27 | ADR-TFBF-001 |
| BR-FDBB-005 | Business Rule | Existing safety assertions in post-baseline migrations are never weakened — they must EXECUTE and PASS on the fresh path | synthetic story65 row `('V20260722019950', false, false)` | BR-SAFETY | ADR-TFBF-002 |
| BR-FDBB-006 | Business Rule | `POST /api/v1/triage/intake/conversation/start` without `clientRequestId` must succeed (2xx) on real PostgreSQL; the `clientRequestId` idempotency contract stays byte-identical | `TriageService.startConversation` | — | ADR-TFBF-003 |
| US-FDBB-001 | User Story | As a MOTHER I can start an elective triage conversation without supplying an idempotency key and receive the first assistant question | `TriageService.startConversation` save() branch | — | ADR-TFBF-003 |
| BR-FDBB-007 | Business Rule | Integration tests must validate the TRUE migration chain — all harness workarounds are removed with the fix | `AbstractPostgresIntegrationTest` (reverted), deleted test resources | — | ADR-TFBF-001 |

---

## 3. Architecture Decision Records (ADR)

### ADR-TFBF-001 — Repair the chain FORWARD with guarded post-baseline bootstrap migrations

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `Project owner (verbal directive 2026-07-27) + AI Agent` |
| **Date** | `2026-07-27` |
| **Supersedes** | `—` (retires the documented test-harness workaround of 2026-07-27) |

#### Bối cảnh (Context)
The post-baseline migrations that consume the bridge tables are applied on every team database and therefore immutable (CLAUDE.md: "Never modify an applied migration"). The baseline `B` file is equally applied (fresh CI databases). The only mutable surface is NEW migrations.

#### Các phương án đã xem xét (Options Considered)

| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A | Keep the test-harness init script + testfix location | zero chain risk | production fresh deploy still broken; tests validate a fake chain |
| B | Edit `V20260724210000`/`212450`/`214200` to guard their bridge references | smallest diff | FORBIDDEN — modifies applied migrations; checksum mismatch on every team DB |
| C | New baseline `B` file above `214200` | clean end state | huge new snapshot to author/verify; still leaves the broken window for targets ≤ 214200 (migration-replay tests) |
| D | **Two new guarded migrations inserted INTO the broken window** (`20260724120000` before the first consumer, `20260724214150` before the column gate) that pre-create exactly what the immutable consumers expect, as no-ops everywhere else | fixes fresh path, legacy path and replay tests with ~100 lines; zero new business tables; net-zero schema | two extra history rows on all DBs |

#### Quyết định (Decision)
**Option D.** DDL is copied VERBATIM from the original pre-baseline creators (same content the proven harness init script used). Version `20260724120000` sits between the baseline (`20260724111500`) and the first consumer (`20260724210000`); version `20260724214150` sits between `214100` and the gate `214200`.

#### Hệ quả (Consequences)
**Positive:** empty DBs bootstrap with plain `flyway migrate`; harness workarounds deleted; integration tests exercise the true chain; the chain's own `DROP TABLE`/`DROP SCHEMA` statements consume the scaffolding → net-zero schema on both paths.
**Trade-offs:** two no-op history rows on already-migrated DBs (recorded out-of-order); mitigated by header comments in both files.
**Compliance impact:** none — no data rows besides one synthetic flag row that the chain itself drops at `212450`.

### ADR-TFBF-002 — Guard on `flyway_schema_history` consumer versions; synthetic story65 state row

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent` |
| **Date** | `2026-07-27` |

#### Bối cảnh (Context)
The same migration file executes in three worlds: (1) fresh baseline path, (2) legacy full-chain path mid-replay (migration-replay tests / stale team DBs), (3) fully-migrated team DBs applying it out-of-order. It must be a no-op in (2) — creators already ran — and in (3) — consumers already dropped the objects.

#### Quyết định (Decision)
* Skip everything `IF EXISTS (SELECT 1 FROM public.flyway_schema_history WHERE version = '<first consumer>' AND success)` — `20260724210000` for the bridge bootstrap, `20260724214200` for the legacy-column shim. `flyway_schema_history` is guaranteed to exist at execution time (Flyway records the baseline row first; precedent: `V20260722020450` :151 queries it the same way). The `AND success` clause deliberately lets a repaired-after-failure fresh DB re-attempt.
* Otherwise: `CREATE SCHEMA IF NOT EXISTS` + `CREATE TABLE IF NOT EXISTS` (exact no-ops mid-replay where the real tables/rows exist) and `INSERT ... ON CONFLICT DO NOTHING` for the story65 state row so a REAL pre-baseline row is never overwritten.
* The synthetic row `('V20260722019950', false, false)` sets both `synthetic_*` flags to false, so `V20260724212450`'s branch checks reduce to asserting `public.baby_profiles` / `public.pregnancy_outcome_evidence` do NOT exist as legacy shadows — true on the canonical baseline (these facts live in `care_subjects` / `mother_journey_events`). No safety assertion is bypassed; every `RAISE EXCEPTION` in the chain still executes.
* Tables that post-baseline migrations create THEMSELVES before first reference (`triage_lifecycle_bridge`, `lifecycle_safety_outcome_bridge` in `211000`; `story68_request_bridge`/`story68_context_share_bridge`/`story68_context_citation_bridge` in `211500`) are deliberately NOT duplicated (divergent-DDL risk).

### ADR-TFBF-003 — D3: let JPA generate the id on the save() path; pre-assign only for the DB-arbitrated native insert

| Field | Value |
|-------|-------|
| **Status** | `Accepted` |
| **Deciders** | `AI Agent` |
| **Date** | `2026-07-27` |

#### Bối cảnh (Context)
`IntakeSessionWriter.insertConversationIfAbsent` binds `candidate.getId()` as `triage_session_id` in its native `INSERT ... ON CONFLICT ... DO NOTHING` — that path REQUIRES a caller-supplied id and must not change (BR-FDBB-006). The plain `repository.save()` path must NOT receive a pre-assigned id (`@GeneratedValue` → Spring Data treats non-null id as detached → merge → `StaleObjectStateException`).

#### Quyết định (Decision)
Smallest correct change: remove `.id(UUID.randomUUID())` from the builder; assign `session.setId(UUID.randomUUID())` INSIDE the `databaseArbitrated` branch immediately before calling the writer. The save() branch receives a null id and Hibernate generates it — identical to the proven `runIntake` pattern (`TriageService.java` :499-510). Nothing reads `session.getId()` before persistence on either branch (first read is `canonicalRequest.put("intakeSessionId", ...)` AFTER save/re-fetch; verified :367). The writer's SQL and the idempotent replay contract are untouched.

**Rejected alternative:** generating the id inside `IntakeSessionWriter` — would change a shared component's contract for a one-call-site defect.

---

## 4. Non-Functional Requirements & SLA

`N/A` — no new runtime component. The two migrations add < 1s one-time cost on fresh bootstrap and ~1ms as out-of-order no-ops. D3 removes a needless extra `SELECT` (merge's load) from the save() path.

---

## 5. Static Modeling (Mô hình Tĩnh)

### 5.1. Class Diagram
`N/A` — no new classes; one method's internals change (`TriageService.startConversation`).

### 5.2. Data Structure (Flyway SQL Migration)

Two new files (full bodies are the implementation; guard skeleton shown):

```sql
-- V20260724120000__fresh_db_bridge_bootstrap.sql (schema carebridge_migration_bridge only)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.flyway_schema_history
                WHERE version = '20260724210000' AND success) THEN
        RAISE NOTICE '...skip: bridge consumers already ran...'; RETURN;
    END IF;
    CREATE SCHEMA IF NOT EXISTS carebridge_migration_bridge;
    -- verbatim DDL (5 tables) + story65 synthetic row, see file
END $$;

-- V20260724214150__restore_legacy_expert_columns_for_fresh_db.sql
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.flyway_schema_history
                WHERE version = '20260724214200' AND success) THEN
        RAISE NOTICE '...skip...'; RETURN;
    END IF;
    ALTER TABLE public.expert_credentials    ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
    ALTER TABLE public.expert_availability   ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
    ALTER TABLE public.expert_location_shares ADD COLUMN IF NOT EXISTS expert_profile_id uuid;
END $$;
```

Net schema effect on EVERY path: **zero** — `210000` drops the story66 bridges, `211500` the story68 tables, `212450` the story65 table (and the schema when empty), `214200` drops the three legacy columns.

---

## 6. Dynamic Modeling (Mô hình Động)

### 6.1/6.2. Sequence Diagrams
`N/A` — no endpoint/flow shape changes. D3 changes only WHERE the session id is born:

```
startConversation (after consent gate, validation, idempotency lookup):
  build session (NO id)
  ├─ clientRequestId present  → session.setId(randomUUID())            [unchanged native path]
  │                             writer.insertConversationIfAbsent(...) → re-fetch winner
  └─ clientRequestId absent   → repository.save(session)               [FIXED: id null → persist/INSERT]
```

### 6.3. State Machine
`N/A` — no status/state semantics change.

---

## 7. Domain Event Catalog

`N/A` — no events added, removed, or re-ordered.

---

## 8. Interface Specification (Đặc tả Giao diện)

`N/A` — no interface/DTO/repository signature changes. `IntakeSessionWriter.insertConversationIfAbsent(IntakeSession)` contract explicitly PRESERVED (still consumes `candidate.getId()`).

---

## 9. API Specification

`N/A` — no contract change. Behavioral fix only: `POST /api/v1/triage/intake/conversation/start` WITHOUT `clientRequestId` now returns the same `200 { success, data: IntakeConversationResponse }` envelope as the `clientRequestId` variant instead of HTTP 500 (`StaleObjectStateException`).

---

## 10. Bảng mã lỗi (Error Codes)

`N/A` — no new error codes; removes an undocumented 500.

---

## 11. Quy trình Triển khai (Step-by-Step)

1. Add `src/main/resources/db/migration/V20260724120000__fresh_db_bridge_bootstrap.sql` (D1).
2. Add `src/main/resources/db/migration/V20260724214150__restore_legacy_expert_columns_for_fresh_db.sql` (D2).
3. Remove harness workarounds: delete `src/test/resources/testsupport/bridge-bootstrap.sql`, delete `src/test/resources/db/testfix/` (whole directory), revert `AbstractPostgresIntegrationTest` (`withInitScript` off; `spring.flyway.locations=classpath:db/migration`).
4. Fix D3 in `TriageService.startConversation` per ADR-TFBF-003.
5. New IT `TriageConversationStartIntegrationTest` (TFBF-TC-INT-01/02) — written RED first against the unfixed code.
6. Verify per Test-Spec §6 (targeted 6-suite run + full `./mvnw clean test` regression, Docker available).

Deployment: nothing beyond normal app start (`flyway migrate` on boot for envs with Flyway enabled). Already-migrated DBs record the two files out-of-order as no-ops.

---

## 12. Rollback & Incident Runbook

Applies to dev/staging only (files are new, not yet applied anywhere else):

```bash
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/service/impl/TriageService.java
git rm 05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260724120000__fresh_db_bridge_bootstrap.sql
git rm 05_Development/CareBridgeAPI/src/main/resources/db/migration/V20260724214150__restore_legacy_expert_columns_for_fresh_db.sql
# restore harness workarounds from git history if rollback happens after their deletion
# on any DB that already recorded the new versions:
#   DELETE FROM flyway_schema_history WHERE version IN ('20260724120000','20260724214150');
```

Incident triggers: any team DB failing `flyway migrate` after pulling this change (expected: none — guarded no-ops); `TriageConversationStartIntegrationTest` regression.

---

## 13. Kịch bản Kiểm thử Chi tiết

See `TriageFreshDbBootstrapFix_Test-Spec.md` (TFBF-TC-INT-01..04). Summary: the new IT proves D3 (red → green); the targeted 6-suite run WITHOUT harness workarounds proves D1+D2 end-to-end on an empty container; the full regression compares against the known pre-existing failure set.

---

## 14. Phương pháp Xác minh

```sql
-- Fresh DB after migrate: scaffolding fully consumed by the chain
SELECT count(*) FROM information_schema.schemata WHERE schema_name='carebridge_migration_bridge'; -- expect 0
SELECT count(*) FROM information_schema.columns
 WHERE table_name IN ('expert_credentials','expert_availability','expert_location_shares')
   AND column_name='expert_profile_id';                                                           -- expect 0
-- Already-migrated DB: the two new versions recorded as out-of-order no-ops
SELECT version, success FROM flyway_schema_history WHERE version IN ('20260724120000','20260724214150');
```

D3: `SELECT triage_session_id, client_request_id FROM triage_sessions WHERE user_id = :u` — one row with NULL `client_request_id` after TFBF-TC-INT-01.

---

## 15. Mẫu thử thực tế (API Verification Samples)

```bash
curl -X POST http://localhost:8080/api/v1/triage/intake/conversation/start \
  -H "Authorization: Bearer $MOTHER_JWT" -H "Content-Type: application/json" \
  -d '{"initialText":"Bé sốt nhẹ — SYNTHETIC"}'          # NOTE: no clientRequestId
# Before fix: 500 (StaleObjectStateException). After fix: 200 {"success":true,"data":{"status":"ASK_MORE","intakeSessionId":"<uuid>",...}}
```

---

## 16. Bảng tổng hợp phân quyền (Authorization Matrix)

`N/A` — unchanged. The endpoint keeps its existing JWT + `SecurityUtils.requireCurrentUserId` + consent-gate enforcement; this fix is downstream of all authorization checks.

---

## 17. AI Prompt Constraints (CASE 2.0)

### 17.1 Constraint Summary Table

| # | Constraint | Source (ADR/BR) | Last Verified |
|---|-----------|-----------------|---------------|
| C1 | NEVER modify an applied/committed migration; only the two NEW files touch the chain | BR-FDBB-002 | 2026-07-27 |
| C2 | Both new migrations MUST be guarded no-ops when their consumer version is already in `flyway_schema_history` (out-of-order safety) | ADR-TFBF-002 | 2026-07-27 |
| C3 | Bridge DDL is copied VERBATIM from the original creators; no new business tables; schema `carebridge_migration_bridge` only | BR-FDBB-004 | 2026-07-27 |
| C4 | Never weaken a safety assertion — the synthetic story65 row makes checks pass truthfully, it does not bypass them | BR-FDBB-005 | 2026-07-27 |
| C5 | D3: do NOT change `IntakeSessionWriter` SQL or the clientRequestId idempotency contract; id pre-assignment moves INTO the databaseArbitrated branch only | ADR-TFBF-003 | 2026-07-27 |
| C6 | Delete all three harness workarounds in the same change so tests validate the true chain | BR-FDBB-007 | 2026-07-27 |

### 17.2 Constraint Injection Block
Constraints C1–C6 above were the working constraints for this implementation (self-applied — same agent authored spec and code under the owner directive).

### 17.3/17.4
Quality checklist applied; anti-pattern scan for the generated SQL/Java done via review + actual test runs (§13). No hallucinated contracts: every referenced object exists in the repo (verified in §1 Source baseline).

---

## PHỤ LỤC

### A. Glossary

| Thuật ngữ | Định nghĩa |
|-----------|------------|
| Baseline path | Flyway executing `B20260724111500` on an empty DB, skipping all pre-baseline migrations |
| Legacy / chain path | Full replay from `V1` (empty DB with `target < 20260724111500`, or an existing team DB) |
| Bridge table | Transient table in `carebridge_migration_bridge` carrying state between migration waves; always dropped by a later wave |
| Harness workaround | The three test-only shims (init script, testfix location, base-class overrides) removed by this feature |

### B. Tài liệu tham chiếu

| Document | Link / Path |
|----------|-------------|
| Harness workaround (retired, content reused verbatim) | `src/test/resources/testsupport/bridge-bootstrap.sql` header (git history after deletion) |
| Testfix shim (retired, content reused verbatim) | `src/test/resources/db/testfix/V20260724214150__testfix_restore_legacy_expert_columns.sql` (git history) |
| D3 bug first documented | `src/test/java/com/carebridge/backend/triage/TriageConsentIntegrationTest.java` tc18 comment ("KNOWN LATENT PRODUCTION BUG") |
| Test-Spec | `04_Implement/TriageFreshDbBootstrapFix/TriageFreshDbBootstrapFix_Test-Spec.md` |
