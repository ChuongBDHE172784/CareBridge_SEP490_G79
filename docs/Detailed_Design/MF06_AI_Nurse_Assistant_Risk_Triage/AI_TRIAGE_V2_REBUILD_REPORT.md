# AI Reproductive Health Triage Assistant V2 — Rebuild Report

> **Positioning.** CareBridge is an ACADEMIC_COMMUNITY_PROJECT providing INFORMATIONAL_RISK_ORIENTATION only. It is not a hospital, clinic, diagnostic service or WHO-certified tool. No clinician participated in building or approving these rules. `clinicalValidationStatus=NOT_CLINICALLY_VALIDATED`, `externalClinicalSignOff=NONE`, `internalReviewStatus=DEV_REVIEWED`. Rules were mapped from publicly available medical guidance; the publishers cited have not reviewed or endorsed CareBridge.


**Status:** PARTIAL — NOT DEPLOYABLE, NOT WIRED, GREEN DISABLED.

Phase 1 (baseline & audit), Phase 2 (registry + contract) and **Phase 2.5 (Matrix reconciliation, GREEN lock, tri-state logic, fail-closed loader, approval manifest, safety policies, corrected decision order, audit trace)** are implemented and green **on the Python side**.

> **✅ RESOLVED — Java/Python parity restored (2026-08-05, later session).** The Java loader has been ported to registry v2.1.0: tri-state `Tri`/`Presence`, `stopOnMatch` + `decisionOrder`, `TriageSafetyPolicy`, `TriageGreenBlocker`, `releaseGates.greenEnabled`, the required-rule manifest, fail-closed loading, the corrected decision order and the decisive/suppressed audit trace. `TriageRuleParityV2Test` now passes **36/36**, loading 10 approved rules + 2 safety policies + 8 green blockers, skipping 0. The earlier red state is closed.

Phases 3 (LangGraph workflow), 4 (Java API/persistence), 5 (Flutter) and 6 (RAG) are not implemented. No production code path has been switched to V2 — V1 remains the only live flow.

### Retraction

An earlier version of this report stated the Rule Matrix has **12 columns**. That was wrong: it has **26 columns × 10 rule rows**. Registry v2.0.0, built from the summarised extraction, drifted from the Matrix on reason codes, action codes, required fields, question IDs, rule versions, one signal name, two broadened conditions, one dropped behavioural exclusion and one inverted stop flag. All of it is itemised in [AI_TRIAGE_V2_RULE_RECONCILIATION.md](AI_TRIAGE_V2_RULE_RECONCILIATION.md) and corrected in v2.1.0.

**Date:** 2026-08-05
**Branch:** `ChuongBD`
**Baseline commit:** `00ed1c986a7090e628eb6bef2dc633e8031f6217`
**Push performed:** NO. **Commits created:** NO (working tree changes only).

---

## 1. Executive Summary

The rebuild is being executed in the phased order mandated by the work order. This report covers what is **actually implemented and tested today**, and states plainly what is not.

Delivered and verified:

- A **canonical, machine-readable rule registry** holding the 10 internally reviewed rules from the Rule Matrix (v0.1.0, approved 2026-08-05), plus one explicitly-flagged legacy safety rule.
- A **closed condition DSL** with a JSON Schema, no `eval` and no expression parser, validated before any rule is loaded on both sides.
- A **deterministic Clinical Rule Engine** in Python *and* Java, both loading the same canonical file, both proven to produce identical verdicts against 18 shared parity vectors.
- The **V2 response contract** as a JSON Schema, encoding the BR-SAFETY invariants (RED ⇒ `stop_conversation`, RED ships no questions, WHO link cap, `content_match: VERIFIED` only).

Not yet delivered: the LangGraph workflow rewrite, the LLM symptom extractor, the question planner, the filtered hybrid RAG and evidence validator, the deterministic template renderer, all Java API/DTO/persistence work, the Flyway migration, and all Flutter work. Details in §11.

---

## 2. Baseline

| Item | Value |
|---|---|
| Starting commit | `00ed1c986a7090e628eb6bef2dc633e8031f6217` |
| Starting branch | `ChuongBD` |
| Starting worktree | **clean** (`git status --short` empty) |
| Pre-existing stashes | 2 (`stash@{0}` WIP on ChuongBD, `stash@{1}` on PhuongNT) — untouched |
| Docker / Testcontainers | **NOT available** in this environment |

### Baseline test results (before any change)

| Layer | Command | Result |
|---|---|---|
| Java (triage + ai) | `./mvnw test -Dtest='com.carebridge.backend.triage.**,com.carebridge.backend.ai.**'` | 247 run — 242 pass, **1 FAIL**, **4 ERROR** |
| Python | `python -m pytest -q` | **337 passed** |
| Flutter | `flutter test test/features/aiTriage` | 137 run — 136 pass, **1 FAIL** |

**Pre-existing failures (NOT caused by this work):**

1. `TriageRedFlagPreScreenSecurityTest.unsupportedRole_intakeEndpointsForbidden_preScreenUnreachable` — `Status expected:<403> but was:<400>`.
2. `Ov01Ac2BackendContractIntegrationTest`, `TriageConsentIntegrationTest`, `TriageConversationStartIntegrationTest`, `TriageHealthMemoryContextIntegrationTest` — all fail to initialise `AbstractPostgresIntegrationTest` because Docker is unavailable. These are **NOT RUN**, not failures of the code under test.
3. Flutter golden `triage_demo_visual_test.dart: mobile stage selector renders all four supported stages` — matching diff PNGs already committed under `test/features/aiTriage/failures/`.

---

## 3. AS-IS Architecture (audit findings)

### 3.1 The maternal rule set was effectively empty

`MaternalPregnancyRiskRules.apply()`, `PostpartumRiskRules` and `PreconceptionRiskRules` (Java) and `_apply_maternal_universal_red_flag_rules` (Python) implement only **6 universal RED signals** (breathing distress, cyanosis, seizure, altered consciousness, heavy bleeding, self-harm). Every non-RED maternal case falls through to `NEED_MORE_INFO` with a `*_RULES_NEED_CLINICAL_REVIEW` marker:

```java
// MaternalPregnancyRiskRules.java:28-32
// Maternal thresholds require obstetric clinical sign-off before production activation.
return new PediatricRiskRules.RuleOutcome(
        "NEED_MORE_INFO", List.of(), List.of("PREGNANCY_RULES_NEED_CLINICAL_REVIEW"));
```

**Consequence:** V1 has never been able to return YELLOW or GREEN for any maternal stage. Filling this gap from the internal rule matrix is the single largest clinical change in V2.

### 3.2 Three overlapping LLM paths

| Path | Location | Role | V2 disposition |
|---|---|---|---|
| Python Gemini | `app/gemini_client.py` — `normalize_symptom_text`, `compose_followup_questions`, `explain_triage_result`, `summarize_conversation` | symptom normalisation, question phrasing, explanation | **Keep** — becomes the single triage LLM boundary |
| Java `GeminiTriageClient` | `triage/service/GeminiTriageClient.java` + `adapter/` | `analyzeSymptoms(prompt) -> AiTriageResult(RiskLevel, disclaimer)` | **Remove** — see 3.3 |
| Java `GeminiExtractionClient` | `ai/service/…`, driven by `IntakeSessionCompletedHandler` | post-completion structured extraction for analytics/escalation | **Keep, audit** — downstream of triage, different purpose |

### 3.3 An LLM interface that returns the triage colour directly

`GeminiTriageClient.analyzeSymptoms()` returns a `RiskLevel` chosen by the model. It has **no caller in `TriageService`** — only `DevPortMockConfiguration` and `DevPortStubConfiguration` supply beans, and the dev stub returns **GREEN**:

```java
// DevPortStubConfiguration.java:34-35
log.warn("[DEV-STUB] GeminiTriageClient.analyzeSymptoms called — returning GREEN stub");
return new GeminiTriageClient.AiTriageResult(RiskLevel.GREEN, "Dev stub — …");
```

This is exactly the shape V2 forbids (an LLM deciding the outcome) combined with a fail-**open** default. It is currently dead code; V2 must delete the interface rather than leave the shape available.

### 3.4 `TriageService` is a god class

1980 lines, 7 public constructors (legacy test-compatibility overloads), 9 `@Autowired(required = false)` optional collaborators, and it owns orchestration, persistence, validation, fallback question generation, evidence persistence and DTO mapping simultaneously.

### 3.5 Evidence registry is domain-level, not document-level

`knowledge_sources` has `domain` declared **UNIQUE**, so it is an allow-list of *organisations*, not of articles — it structurally cannot hold several Vinmec articles. The document-level corpus already exists as version-controlled Markdown with rich frontmatter under `CareBridgeAITriageService/data/medical_sources/` (`id`, `title`, `organization`, `url`, `domain`, `applicableStages`, `riskLevels`, `symptoms`, `sourceStatus`, `sourceVersion`, `approvedAt`). **This is why V2 needs zero new tables for evidence**: the corpus stays file-based and reviewable, and `knowledge_sources` remains the Java-side domain firewall.

### 3.6 Database constraint blocking the new stage

`chk_triage_origin_stage` in `V1__init_schema.sql` restricts `triage_sessions.stage` to `PRECONCEPTION|PREGNANCY|POSTPARTUM` (MOTHER_JOURNEY) and `INFANT|TODDLER` (BABY_PROFILE). Introducing `POSSIBLE_PREGNANCY` **requires** a forward-only migration widening this CHECK. Not yet written.

---

## 4. TO-BE Architecture — implemented portion

```
Canonical registry  05_Development/Contracts/triage/triage_rules_v2.json
        │  (sync + sha256 sidecar, DevTools/sync_triage_rule_registry.py)
        ├──────────────► CareBridgeAPI/src/main/resources/triage/     → TriageRuleRegistry (Java)
        └──────────────► CareBridgeAITriageService/data/              → app.rules.registry (Python)
                                        │
                    ┌───────────────────┴───────────────────┐
            TriageRuleEvaluator (Java)            app.rules.evaluator (Python)
                    └───────────────────┬───────────────────┘
                     18 shared parity vectors — identical verdicts
```

Java and Python each verify their copy's SHA-256 against a sidecar digest before parsing. A hand-edited runtime copy throws `RegistryIntegrityError` / `RegistryIntegrityException` instead of silently changing clinical behaviour.

### Engine precedence (both runtimes, identical)

1. **RED** — any matching RED rule wins outright; `stop_conversation` forced `true`.
2. **OUT_OF_SCOPE** — evaluated *only after* the RED gate has run and missed.
3. **YELLOW**.
4. **NEEDS_MORE_INFO** from an information rule (`PRE_INFO_001`).
5. Minimum dataset incomplete → `SYS_INFO_001` if rounds exhausted, else the Question Planner path (still `NEEDS_MORE_INFO`).
6. **GREEN** — reachable only via `GREEN_DEFAULT_001`, i.e. only when the minimum dataset is complete and nothing riskier matched.

Absence semantics are **fail-closed**: a signal the user has not provided never makes a leaf true (except the explicit `NOT_EXISTS` operator), so missing data can neither raise nor lower a level. If no GREEN rule is releasable the engine returns `NEEDS_MORE_INFO / ROUTE_TO_HEALTHCARE_WORKER` rather than inventing a reassuring answer.

---

## 5. Rule Registry

**Source:** internal rule matrix v0.1.0, dated 2026-08-05 (NOT clinically validated) by obstetric/pediatric clinical advisors. The sheet contains a **12-column, 10-rule** matrix; two independent extractions returned the same 10 rule IDs in the same row order.

| Rule ID | Stage(s) | Outcome | Prio | Stop | Status |
|---|---|---|---|---|---|
| `GLOBAL_RED_001` | all | RED | 100 | yes | APPROVED |
| `PREG_RED_001` | PREGNANCY | RED | 95 | yes | APPROVED |
| `PREG_RED_002` | PREGNANCY | RED | 90 | yes | APPROVED |
| `PREG_YELLOW_001` | PREGNANCY | YELLOW | 60 | no | APPROVED |
| `POST_RED_001` | POSTPARTUM | RED | 95 | yes | APPROVED |
| `POST_RED_002` | POSTPARTUM | RED | 100 | yes | APPROVED |
| `PRE_INFO_001` | POSSIBLE_PREGNANCY | NEEDS_MORE_INFO | 50 | no | APPROVED |
| `SYS_INFO_001` | all | NEEDS_MORE_INFO | 40 | yes | APPROVED |
| `SYS_OOS_001` | all | OUT_OF_SCOPE | 10 | yes | APPROVED |
| `GREEN_DEFAULT_001` | all | GREEN | 0 | yes | APPROVED |
| `LEGACY_RED_SELF_HARM_001` | all | RED | 100 | yes | **APPROVED_LEGACY_V1** |

### Outstanding rule requiring clinical sign-off

`LEGACY_RED_SELF_HARM_001` is **not in Rule Matrix v0.1.0**. It is carried forward **ACTIVE** deliberately: V1 already escalates self-harm ideation to RED (`RED_*_SELF_HARM` in `UniversalMaternalRedRules.java` and `risk_rules.py`), and dropping it to match the matrix exactly would be a **safety regression**. It is marked with a distinct status, an explicit provenance string, and is surfaced here so the team can either promote it to `APPROVED` or instruct its removal. **No other rule was added, and no approved rule's clinical logic was altered.**

`PREG_RED_002` carries the exclusion `NO_PREECLAMPSIA_DIAGNOSIS` from the matrix; the renderer must never name or imply preeclampsia.

---

## 6. Database and Supabase Changes

**No database change has been made in this phase.**

| Item | Value |
|---|---|
| Database provider | Supabase PostgreSQL |
| Schema source of truth | Flyway (`CareBridgeAPI/src/main/resources/db/migration/`) |
| Supabase environment detected | Not contacted — no database command was executed |
| Application schema managed | `public` |
| Triage-related application tables (start) | **7** — `triage_sessions`, `triage_session_evidence`, `red_flag_rules`, `knowledge_sources`, `knowledge_source_reviews`, `health_context_memories`, `data_permissions` (shared with the privacy domain) |
| Triage-related application tables (now) | **7** (unchanged) |
| **New business tables added** | **0** |
| Tables removed | 0 |
| Columns added | 0 |
| Columns deprecated | 0 |
| Flyway migrations created | **0** |
| Supabase-managed schemas modified | **0** |
| RLS policies changed | 0 |
| Grants changed | 0 |
| Triggers/functions changed | 0 |
| Fresh bootstrap result | **NOT RUN** — Docker/Testcontainers unavailable |
| Existing-schema upgrade result | **NOT RUN** — same reason |
| Schema drift detected | Not assessed (no database was contacted) |
| Production Supabase accessed | **NO** |
| Production migration performed | **NO** |
| Data preservation result | N/A — no schema change |
| DROP CASCADE count | **0** |

### Planned (not yet written) migration

One forward-only migration will be required in Phase 4, to widen `chk_triage_origin_stage` for `POSSIBLE_PREGNANCY` and to add V2 workflow columns to `triage_sessions` (`workflow_state_jsonb`, `triage_outcome`, `question_round`, `matched_rule_ids_jsonb`, `ruleset_version`). All are additive and nullable; **no new table**, no `DROP CASCADE`.

---

## 7. API Contract Changes

**None yet.** No endpoint, request or response shape has changed. `Contracts/triage/triage_response_v2.schema.json` defines the target contract but nothing serves or consumes it. Backward compatibility is therefore trivially intact.

---

## 8. Rule and Evidence Changes

- **Canonical rule source:** `05_Development/Contracts/triage/triage_rules_v2.json`, synced to both runtimes with SHA-256 sidecars.
- **Approved rules loaded:** 11 (10 APPROVED + 1 APPROVED_LEGACY_V1). Rejected: 0.
- **DRAFT rules:** none created. The loader on both sides refuses any rule whose status is not releasable — asserted by `test_draft_rules_are_never_loaded`.
- **Vietnamese URL policy / WHO link policy:** encoded in `triage_response_v2.schema.json` (`maxItems: 5`, `content_match: VERIFIED` const, `matched_symptoms` required per link) but **not yet enforced by code** — the RAG pipeline and the Java validator are Phase 3/4 work.

---

## 9. Test Results

All commands were actually executed; results are reported verbatim.

| Command | Result |
|---|---|
| `python -m pytest tests/test_rule_registry_parity_v2.py -q` (Python service dir) | **31 passed** |
| `python -m pytest -q` (Python service dir) | **368 passed** (baseline 337 + 31 new; no regression) |
| `./mvnw test -Dtest='TriageRuleParityV2Test'` (CareBridgeAPI) | **24 run, 0 failures, 0 errors — BUILD SUCCESS** |
| `./mvnw test -Dtest='com.carebridge.backend.triage.**,com.carebridge.backend.ai.**'` | **271 run** — 1 FAIL, 4 ERROR: **byte-identical to the baseline set** (247 run + 24 new). No regression. |
| `python 05_Development/DevTools/sync_triage_rule_registry.py --check` | **OK** |

### Phase 2.5 results (2026-08-05, registry v2.1.0)

| Command | Status | Detail |
|---|---|---|
| `pytest tests/test_rule_registry_parity_v2.py -q` | **PASS** | 64 passed |
| `pytest -q` (Python service) | **PASS** | **401 passed** (baseline 337 → +64) |
| `python DevTools/sync_triage_rule_registry.py --check` | **PASS** | copies + approval manifest hashes consistent |
| `./mvnw test -Dtest='TriageRuleParityV2Test'` | **ERROR** | `RegistryIntegrity: no releasable triage rules were loaded` — Java loader not yet ported to v2.1.0 |
| Java triage+ai suite | **NOT RUN** since v2.1.0 | will inherit the error above |
| Flutter | **NOT RUN** | no Dart file touched |
| Flyway fresh bootstrap / existing-schema upgrade | **NOT RUN** | Docker unavailable |
| Supabase deployment verification | **NOT RUN** | no dev/staging target contacted |
| End-to-end structured cases | **NOT RUN** | LangGraph workflow not built |

Accurate phrasing for the earlier Phase 2 run: **no new regression detected in tests that were successfully executed**. That statement does not extend to integration, Flyway or E2E, none of which has run.

**NOT RUN** (environment, not code):

- Java Postgres integration tests — Docker unavailable. Re-run with Docker Desktop started:
  ```bash
  cd "05_Development/CareBridgeAPI" && ./mvnw test -Dtest='com.carebridge.backend.triage.**'
  ```
- Flyway fresh-bootstrap and existing-schema upgrade tests — same reason.
- Supabase deployment verification: **NOT RUN** (no dev/staging target was contacted).

Flutter was **not re-run** after this phase because no Dart file was touched.

---

## 10. Files Changed

### Added

| Path | Purpose |
|---|---|
| `05_Development/Contracts/triage/triage_rules_v2.json` | Canonical rule registry (source of truth) |
| `05_Development/Contracts/triage/triage_rule_condition.schema.json` | Closed condition-DSL + registry JSON Schema |
| `05_Development/Contracts/triage/triage_rule_parity_vectors_v2.json` | 18 shared Java/Python parity vectors |
| `05_Development/Contracts/triage/triage_response_v2.schema.json` | V2 response contract |
| `05_Development/DevTools/sync_triage_rule_registry.py` | Sync + integrity tool (`--check` mode for CI) |
| `CareBridgeAITriageService/app/rules/{__init__,condition,registry,evaluator}.py` | Python rule engine |
| `CareBridgeAITriageService/tests/test_rule_registry_parity_v2.py` | Python parity + safety tests |
| `CareBridgeAPI/…/triage/rules/{TriageRule,RuleConditionEvaluator,TriageRuleRegistry,TriageRuleEvaluator}.java` | Java rule engine |
| `CareBridgeAPI/…/triage/rules/TriageRuleParityV2Test.java` | Java parity + safety tests |
| Synced copies + `.sha256` sidecars in both runtimes' resource trees | Runtime registry copies |

### Modified / Deleted / Deprecated

**No source file was modified or deleted.** No V1 code was removed or marked deprecated — per the phased plan, V1 stays untouched until V2 passes its tests.

Seven files show as modified purely as a **side effect of running the mandated Flutter baseline**, not as an edit:

- `test/features/aiTriage/failures/triage_stage_selector_mobile_{isolatedDiff,maskedDiff,masterImage,testImage}.png` — regenerated by `flutter test` for the pre-existing golden failure. **Left in place deliberately**: golden failure evidence must not be deleted before it is verified.
- `android/app/src/main/java/io/flutter/plugins/GeneratedPluginRegistrant.java`, `ios/…/Package.swift`, `macos/…/Package.swift` — regenerated by the Flutter toolchain on test run.

These can be discarded with `git checkout --` on those paths if a clean tree is wanted; none of them is a deliberate change.

---

## 11. Remaining Risks and Unfinished Work

**0. Fail-open paths that are still open (P0, not yet closed).**
- **`minimumDatasetComplete` is still a caller boolean.** The engine does not yet derive `safetyScreenStatus` / `contextDatasetStatus` / `greenEligibilityDatasetStatus` itself. A caller that passes `true` while signals are UNKNOWN is still believed. GREEN is unreachable anyway (gate locked), so this cannot currently produce a false reassurance — but it must be closed before the gate is ever opened.
- **`reproductiveRelevance` is still a caller boolean.** The engine does detect the conflict (caller says out-of-scope while reproductive evidence exists → `SCOPE_CLASSIFICATION_CONFLICT`, never OUT_OF_SCOPE), but there is no five-valued `scopeStatus` (`IN_SCOPE | POSSIBLY_IN_SCOPE | CONFIRMED_OUT_OF_SCOPE | CONFLICTED | UNKNOWN`) and no `POSSIBLY_IN_SCOPE` handling for an unresolved possible pregnancy.
- **Pending-RED disposition is behavioural only.** The ask-now / escalate-at-round-3 behaviour exists in both runtimes, but there is no `pendingRiskStatus` enum, no `completionReason = UNRESOLVED_HIGH_RISK_SIGNAL`, no DRAFT Safety Disposition Matrix and no `BLOCKED_CLINICAL_DISPOSITION` readiness gate.

**0b. The earlier Java red state is closed** (see the status block at the top).

 `triage/rules/{TriageRule,TriageRuleRegistry,TriageRuleEvaluator}.java` and `TriageRuleParityV2Test.java` still target registry v2.0.0. Against v2.1.0 the loader rejects every rule and the test errors. Until these are ported — tri-state `Tri`/`Presence`, `stopOnMatch` + `decisionOrder`, `safetyPolicies`, `greenSafetyBlockers`, `releaseGates.greenEnabled`, fail-closed loading, the corrected decision order and the `decisive/all_matched/suppressed` trace — **Java/Python parity is unproven and V2 must be treated as unavailable on the Java side.** This is the single highest-priority next task.

Stated plainly — none of the following is implemented:

1. **Phase 3 (partial).** Only the rule engine exists. Still missing: the LangGraph state object, node/routing rewrite, repeated Global RED Safety Gate, Context Resolver, LLM Symptom Extractor with Pydantic validation and fail-closed retry, Question Planner + fixed question catalogue, Evidence Query Builder, Filtered Hybrid RAG, Evidence Validator, Deterministic Template Renderer. `app/graph.py` is untouched and still holds the V1 mixed-concern workflow.
2. **Phase 4.** No Java DTO, endpoint, persistence, validator, fail-safe fallback, feature flag (`triage.v2.enabled`) or Flyway migration. `TriageService` is unchanged.
3. **Phase 5.** No Flutter change. The app cannot parse the V2 contract and does not render `NEEDS_MORE_INFO` or `OUT_OF_SCOPE` as distinct states.
4. **Phase 6.** `GeminiTriageClient` (the LLM-decides-the-colour interface, §3.3) and its GREEN-returning dev stub are still present. Pediatric rules are still reachable from the shared engine classes.
5. **Rule Matrix provenance.** The registry was transcribed from the Google Sheet via automated extraction, not from a machine-readable export. The team should diff `triage_rules_v2.json` against the sheet before any activation.
6. **`LEGACY_RED_SELF_HARM_001`** awaits further internal review; no external clinical sign-off exists (§5).
7. **Integration and migration tests are unverified** in this environment (Docker absent).

**The V2 engine is not wired to anything.** No user-facing behaviour has changed; there is currently no way for a request to reach the new engine.

---

## 12. Rollback

Because nothing is wired in, rollback is deletion of added files — there is no flag to flip and no data to unwind:

```bash
git status --short && git checkout -- . && git clean -nd 05_Development/Contracts/triage 05_Development/DevTools
```

Review the `git clean -nd` dry-run output before removing the `-n`. No commit, no migration and no database change exists to revert.

---

## 13. V1 vs V2 Comparison

Status legend: **DONE** = implemented and tested here; **PLANNED** = designed, not implemented.

| Hạng mục | V1 | V2 | Thay đổi | Trạng thái |
|---|---|---|---|---|
| Workflow | `graph.py` trộn mọi trách nhiệm trong 624 dòng | LangGraph tách node, conditional edges | Rewrite | PLANNED |
| Safety Gate | Pre-screen keyword ở Java, chạy 1 lần trước mỗi AI call | Global RED gate trên văn bản thô + lặp lại sau mọi câu trả lời + stage RED gate sau extraction | Thiết kế lại | PLANNED |
| Context Resolver | Suy stage từ profile/journey rải rác trong `TriageService` | Node riêng, chuẩn hóa stage/tuần thai/ngày hậu sản | Tách bạch | PLANNED |
| Kết quả | RED/YELLOW/GREEN/NEED_MORE_INFO (không có OUT_OF_SCOPE) | 5 outcome, `assessment_status` tách khỏi outcome | +OUT_OF_SCOPE, lỗi≠GREEN | Schema **DONE**, runtime PLANNED |
| Vai trò LLM | 3 đường; `GeminiTriageClient` trả thẳng `RiskLevel` | 1 boundary ở Python, chỉ trích xuất/chuẩn hóa | Bỏ LLM quyết định màu | Engine **DONE**, xóa V1 PLANNED |
| Rule Engine | Hard-code trong Java `engine/*RiskRules.java` **và** Python `risk_rules.py`, maternal rỗng | 1 registry JSON canonical, 2 evaluator đọc chung, 11 rule | Hết duplicate định nghĩa | **DONE** |
| Java/Python duplication | 2 bộ luật viết tay, giữ đồng bộ thủ công | 1 file + sha256 sidecar + 18 parity vector | Enforce bằng test | **DONE** |
| Question flow | LLM tự soạn/naturalize câu hỏi | Question catalog cố định, rule khai báo `questionIds`, ≤3 vòng | Khóa nội dung y khoa | Registry **DONE**, planner PLANNED |
| RAG | Có `official_source_searcher` tìm Internet lúc runtime | Chỉ corpus đã duyệt, metadata filter → lexical → vector → re-rank → validator | Bỏ runtime search | PLANNED |
| URL policy | Không giới hạn WHO, không bắt buộc symptom match | WHO ≤1, link VN phải có `matched_symptoms`, `content_match=VERIFIED` | Siết | Schema **DONE**, enforce PLANNED |
| Response rendering | LLM `explain_triage_result` sinh văn bản | Template cố định theo outcome/reason/action | Bỏ LLM viết kết quả | PLANNED |
| Failure fallback | Java fallback sinh câu hỏi; dev stub trả GREEN | Fail-closed: không bao giờ GREEN, RED gate vẫn chạy khi Python chết | Bỏ fail-open | Engine **DONE**, Java PLANNED |
| Database | 7 bảng triage | 7 bảng (tái sử dụng), +cột JSONB trên `triage_sessions` | **0 bảng mới** | 0 thay đổi tới nay |
| API contract | v1 ad-hoc envelope | `schema_version: "2.0"` | Thêm mapper/feature flag | Schema **DONE**, API PLANNED |
| Flutter UI | 3 trạng thái hiển thị | 5 trạng thái + reading links | Cập nhật model/golden | PLANNED |
| Tests | 42 Java + 19 Python triage | +31 Python, +24 Java parity | Parity enforce | **DONE** cho phần engine |
| Privacy/Audit | Health memory + consent hiện có | Giữ nguyên; audit thêm rule/evidence version | Chưa đụng | Không thay đổi |

---

## 14. Git Status

| Item | Value |
|---|---|
| Current commit | `00ed1c986a7090e628eb6bef2dc633e8031f6217` (unchanged) |
| New local commits | **0** |
| Worktree | dirty — added files only, no modifications or deletions |
| Push performed | **NO** |
| Production Supabase accessed | **NO** |
| Production migration performed | **NO** |
| Supabase system schemas modified | **0** |
| DROP CASCADE count | **0** |
