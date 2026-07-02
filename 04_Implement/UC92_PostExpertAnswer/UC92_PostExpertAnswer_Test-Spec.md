# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-92 Post Expert Answer

**Document ID:** `CB-EXP-TDD-092`
**Version:** `1.0`
**Date:** `2026-07-02`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `04_Implement/UC92_PostExpertAnswer/UC92_PostExpertAnswer_TDS.md` (companion TDS)
- `02_Requirements/SRS/3_Functional_Specification.md` §3.2.1.6 (lines 859-878)
- `CLAUDE.md` — "AI provides guidance only; never diagnose, prescribe, or delay emergency routing"

> **Quy ước TDD:** viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-07-02 | AI Agent | Khởi tạo Test-Spec cho UC-92 |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-92` |
| **Module** | `PostExpertAnswer` — `expert` bounded context (writes `community.CommunityAnswer`) |
| **Spec gốc** | `CB-EXP-IMP-092` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 4` |
| **Data Classification** | `Internal` |
| **Compliance Scope** | `BR-RBAC`, `BR-CONSULTATION`, `BR-SAFETY` |
| **Upstream Dependencies** | `community_answers` (extended), `community_questions`, `expert_profiles` |
| **Downstream Consumers** | UC-91 queue exclusion, UC-100 moderation (existing, out of scope), community feed badge |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-EXP-IMP-092 §17` |
| **Constraints Injected** | C1-C6 |
| **Model** | `Claude Sonnet 5` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | SRS says "safe-scope boundaries" without a mechanism | No AI-moderation integration exists in `community`/`expert` packages; CLAUDE.md forbids AI auto-diagnosis | Tests assert `ExpertAnswerContentSafetyPolicy.evaluate()` is NON-BLOCKING — flagged content still returns 201, never 4xx |
| L2 | Existing `ADR-COM-005` says `is_expert_labeled` "only set by Moderator/System" — UC-92 changes this | UC-92 formally adds a THIRD authorized setter (Verified Expert via new endpoint), but ONLY server-side, never client-supplied | Tests assert `PostExpertAnswerRequest` DTO has NO `isExpertLabeled`/`expertLabeled` field at all (compile-time enforcement) and that extra JSON fields are ignored at runtime |
| L3 | SRS doesn't specify whether expert answers skip moderation | `ADR-COM-006` (existing) — ALL new answers start `PENDING` | Tests assert `status == PENDING` on every successful post, with no code path to bypass |
| L4 | New DB column `expert_scope_flagged` not yet in `V1__init_schema.sql` | Requires new migration `V20260703100000` | Integration test asserts column exists and defaults to `false` |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
PostExpertAnswer bao gồm các layer:
├── Domain (ExpertAnswerPolicy, ExpertAnswerContentSafetyPolicy — pure logic)
├── Services (ExpertAnswerServiceImpl — mock repos với Mockito)
├── Controller (ExpertAnswerController — @WebMvcTest)
├── Integration (Testcontainers PostgreSQL + Flyway migration V20260703100000)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS UC-92` (lines 859-878) | Normal flow, E1-E3 |
| `V1__init_schema.sql` + `V20260703100000` (new) | `community_answers` exact columns incl. new `expert_scope_flagged` |
| `CB-EXP-IMP-092 §3/§8/§10/§16/§17` | ADRs, interfaces, error codes, auth matrix, constraints |
| `ADR-COM-005`, `ADR-COM-006` (existing code, `CommunityAnswer.java` comments) | Invariants this UC must NOT break |
| CLAUDE.md | "AI provides guidance only" — non-blocking safety policy requirement |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Verified expert posts valid answer to APPROVED question | `ExpertAnswerServiceImpl.postExpertAnswer()` | `EXPA-TC-001` |
| TC-COND-002 | `expertLabeled` forced true regardless of any client input | `ExpertAnswerMapper` | `EXPA-TC-002` |
| TC-COND-003 | `status` always PENDING on creation | `ExpertAnswerServiceImpl` | `EXPA-TC-003` |
| TC-COND-004 | Question not APPROVED → COM-007 | `ExpertAnswerServiceImpl` | `EXPA-TC-004` |
| TC-COND-005 | Caller not VERIFIED expert → EXPQ-004 | `ExpertAnswerPolicy` | `EXPA-TC-005` |
| TC-COND-006 | Body validation boundaries (9/10/3000/3001 chars) | `PostExpertAnswerRequest` | `EXPA-TC-006`..`009` |
| TC-COND-007 | Content-safety flag is non-blocking | `ExpertAnswerContentSafetyPolicy` | `EXPA-TC-010`, `EXPA-TC-011` |
| TC-COND-008 | Audit log emitted on success | `ExpertAnswerServiceImpl` | `EXPA-TC-012` |
| TC-COND-009 | Domain event `ExpertAnswerPosted` published | `ExpertAnswerServiceImpl` | `EXPA-TC-013` |
| TC-COND-010 | Existing `CommunityAnswerController` (non-expert path) unaffected | Regression | `EXPA-TC-SEC-002` |
| TC-COND-011 | No JWT / wrong role | `ExpertAnswerController` | `EXPA-TC-014` |
| TC-COND-012 | Web form validates and submits | `PostExpertAnswerForm.tsx` | `EXPA-TC-WEB-001`, `EXPA-TC-WEB-002` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | question status (APPROVED / PENDING / HIDDEN / LOCKED) | Only APPROVED accepts answers |
| Boundary Value Analysis | body length (9, 10, 3000, 3001 chars) | Exact validation bounds from DTO |
| State Transition | `AnswerStatus` (must stay PENDING from this endpoint) | Safety invariant |
| Error Guessing | attempt to inject `expertLabeled`/`status` via extra JSON fields | Security/tamper resistance |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Reuse | `ExpertCommunityTestFactory.makeVerifiedExpert()` (shared, from UC-91) | Happy path |
| `FX-002` | Reuse | `ExpertCommunityTestFactory.makeUnverifiedExpert()` | E1 |
| `FX-003` | Reuse | `ExpertCommunityTestFactory.makeApprovedQuestion(topicId, false)` | Happy path |
| `FX-007` | DB seed | `CommunityQuestion{status: PENDING}` | E2 (COM-007) |
| `FX-008` | Request | `PostExpertAnswerRequest{body:"a".repeat(9)}` | Boundary — too short |
| `FX-009` | Request | `PostExpertAnswerRequest{body:"a".repeat(3001)}` | Boundary — too long |
| `FX-010` | Request | `PostExpertAnswerRequest{body:"Bạn nên chẩn đoán xác định với bác sĩ..."}` | Content-safety flag trigger (illustrative keyword) |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
// Extends the SHARED ExpertCommunityTestFactory from UC-91
// (src/test/java/com/carebridge/backend/expert/support/ExpertCommunityTestFactory.java)
// Add UC-92-specific factory methods here:

class ExpertCommunityTestFactory {
    // ... makeVerifiedExpert(), makeUnverifiedExpert(), makeTopic(), makeApprovedQuestion() — see UC-91 Test-Spec §4

    static CommunityQuestion makePendingQuestion() {
        CommunityQuestion q = makeApprovedQuestion(UUID.randomUUID(), false);
        q.setStatus(QuestionStatus.PENDING);
        return q;
    }

    static PostExpertAnswerRequest makeValidAnswerRequest() {
        PostExpertAnswerRequest req = new PostExpertAnswerRequest();
        req.setBody("Nội dung tư vấn hợp lệ, dài hơn mười ký tự, mang tính tham khảo.");
        req.setIsPersonalExperience(false);
        return req;
    }

    static PostExpertAnswerRequest makeAnswerRequest(String body) {
        PostExpertAnswerRequest req = new PostExpertAnswerRequest();
        req.setBody(body);
        req.setIsPersonalExperience(false);
        return req;
    }
}
```

---

### EXPA-TC-001 — Happy path: verified expert posts valid answer

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertAnswerServiceImpl.postExpertAnswer()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `SRS UC-92 Normal Flow` / `CB-EXP-IMP-092 §8`

**Preconditions:** FX-001 (verified expert), FX-003 (approved question)

**Test Steps:**
1. Mock `ExpertProfileRepository.findByUserId` → FX-001
2. Mock `CommunityQuestionRepository.findByIdAndStatus(id, APPROVED)` → FX-003
3. Mock `CommunityAnswerRepository.save(any())` → echoes input with generated id
4. Call `service.postExpertAnswer(expertUserId, questionId, makeValidAnswerRequest())`

**Expected Result (PASS):** Returns `ExpertAnswerResponse` with `expertLabeled=true`, `status="PENDING"`
**Expected Result (FAIL):** Exception, or `expertLabeled=false`, or `status != PENDING`

**Current Status:** 🔴 Not written

---

### EXPA-TC-002 — `expertLabeled` cannot be influenced by client input (CRITICAL invariant)

**Severity:** `CRITICAL`
**CWE:** `CWE-639 — Authorization Bypass Through User-Controlled Key`
**Feature Under Test:** `ExpertAnswerMapper` / DTO contract
**Test File:** `src/test/java/com/carebridge/backend/expert/mapper/ExpertAnswerMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `CB-EXP-IMP-092 §17 C1`

**Test Steps:**
1. Deserialize a raw JSON payload containing `{"body":"...", "isPersonalExperience":false, "expertLabeled":false, "isExpertLabeled":false}` into `PostExpertAnswerRequest`
2. Assert via reflection that `PostExpertAnswerRequest.class.getDeclaredFields()` contains no field named `expertLabeled` or `isExpertLabeled`
3. Call `mapper.toEntity(request, expertUserId, questionId)`

**Expected Result (PASS):** `entity.isExpertLabeled() == true` regardless of any JSON field named similarly (Jackson silently ignores unknown properties, DTO has no such field)
**Expected Result (FAIL):** DTO has an `expertLabeled` field, OR entity ends up `false`

**Current Status:** 🔴 Not written

---

### EXPA-TC-003 — `status` always PENDING on creation, never client-settable

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertAnswerMapper`
**Test File:** `ExpertAnswerMapperTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `ADR-COM-006` (existing) + `CB-EXP-IMP-092 §17 C2`

**Test Steps:**
1. `mapper.toEntity(makeValidAnswerRequest(), expertUserId, questionId)`

**Expected Result (PASS):** `entity.getStatus() == AnswerStatus.PENDING`
**Expected Result (FAIL):** Any other status

**Current Status:** 🔴 Not written

---

### EXPA-TC-004 — E2: question not APPROVED → COM-007

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertAnswerServiceImpl.postExpertAnswer()`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `CB-EXP-IMP-092 §10 COM-007` (reused from existing `QuestionNotAnswerableException`)

**Test Steps:**
1. Mock `findByIdAndStatus(id, APPROVED)` → `Optional.empty()` (question is FX-007, PENDING)
2. Call `service.postExpertAnswer(...)`

**Expected Result (PASS):** Throws `QuestionNotAnswerableException` containing `COM-007`
**Expected Result (FAIL):** Answer saved despite non-approved question

**Current Status:** 🔴 Not written

---

### EXPA-TC-005 — E1: unverified expert rejected before question lookup

**Severity:** `CRITICAL`
**CWE:** `CWE-862`
**Feature Under Test:** `ExpertAnswerServiceImpl.postExpertAnswer()`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `CB-EXP-IMP-092 §17 C4`

**Test Steps:**
1. Mock `ExpertProfileRepository.findByUserId` → FX-002 (PENDING verification)
2. Call `service.postExpertAnswer(...)`

**Expected Result (PASS):** Throws `ExpertNotVerifiedException` [EXPQ-004]; `Mockito.verifyNoInteractions(communityQuestionRepository, communityAnswerRepository)` — confirms C4 ordering (auth check before question/answer touches)
**Expected Result (FAIL):** Question repository queried despite unverified caller

**Current Status:** 🔴 Not written

---

### EXPA-TC-006 — Boundary: body exactly 10 chars (valid, lower bound)

**Severity:** `MEDIUM`
**Feature Under Test:** `PostExpertAnswerRequest` bean validation
**Test File:** `src/test/java/com/carebridge/backend/expert/dto/PostExpertAnswerRequestValidationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `CB-EXP-IMP-092 §8 (10-3000 chars, mirrors existing PostCommunityAnswerRequest)`

**Test Steps:** Validate `body = "a".repeat(10)`
**Expected Result (PASS):** No validation violations
**Current Status:** 🔴 Not written

---

### EXPA-TC-007 — Boundary: body 9 chars (invalid, below lower bound)

**Severity:** `MEDIUM`
**Feature Under Test:** same
**Test File:** same
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`

**Test Steps:** Validate `body = "a".repeat(9)`
**Expected Result (PASS):** 1 violation on `body`
**Current Status:** 🔴 Not written

---

### EXPA-TC-008 — Boundary: body exactly 3000 chars (valid, upper bound)

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-006`
**Test Steps:** Validate `body = "a".repeat(3000)`
**Expected Result (PASS):** No violations
**Current Status:** 🔴 Not written

---

### EXPA-TC-009 — Boundary: body 3001 chars (invalid, above upper bound)

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-006`
**Test Steps:** Validate `body = "a".repeat(3001)`
**Expected Result (PASS):** 1 violation on `body`
**Current Status:** 🔴 Not written

---

### EXPA-TC-010 — Content-safety flag: matched pattern sets flag but STILL returns 201 (non-blocking)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertAnswerServiceImpl` + `ExpertAnswerContentSafetyPolicy`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`
**Oracle Source:** `CB-EXP-IMP-092 §3 ADR-EXP-092-03` + `§17 C3` + CLAUDE.md "AI provides guidance only"

**Test Steps:**
1. Use FX-010 (body contains a diagnostic-certainty phrase)
2. Call `service.postExpertAnswer(...)`

**Expected Result (PASS):** Method returns successfully (no exception), `response.expertScopeFlagged == true`, `response.status == "PENDING"` (unchanged — flag does NOT alter status)
**Expected Result (FAIL):** Exception thrown / POST rejected because of flagged content (violates C3 — this would be AP-EXP-092-A)

**Current Status:** 🔴 Not written

---

### EXPA-TC-011 — Content-safety flag: clean body → flag false

**Severity:** `MEDIUM` | **Test File:** same | **TDD Phase:** 🔴 RED | **Condition Ref:** `TC-COND-007`
**Test Steps:** Use `makeValidAnswerRequest()` (no flagged phrases)
**Expected Result (PASS):** `expertScopeFlagged == false`
**Current Status:** 🔴 Not written

---

### EXPA-TC-012 — Audit log emitted with correct action on success

**Severity:** `HIGH`
**Feature Under Test:** `ExpertAnswerServiceImpl.postExpertAnswer()`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `CB-EXP-IMP-092 §17 C6`

**Test Steps:** Happy path call, then `Mockito.verify(auditService).log(eq(AuditAction.EXPERT_ANSWER_POSTED), eq(expertUserId), any(), any(), any())`
**Expected Result (PASS):** Exactly 1 invocation with `EXPERT_ANSWER_POSTED`
**Current Status:** 🔴 Not written

---

### EXPA-TC-013 — Domain event `ExpertAnswerPosted` published on success

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertAnswerServiceImpl.postExpertAnswer()`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `CB-EXP-IMP-092 §7.3`

**Test Steps:** Happy path call with a captured `ApplicationEventPublisher` mock; assert `publishEvent(any(ExpertAnswerPosted.class))` called once with `payload.answerId` matching saved id
**Expected Result (PASS):** Event published with correct payload
**Current Status:** 🔴 Not written

---

### EXPA-TC-014 — No JWT → 401, wrong role → 403

**Severity:** `HIGH`
**CWE:** `CWE-306`
**Feature Under Test:** `ExpertAnswerController`
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertAnswerControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-011`

**Test Steps:**
1. `POST /api/v1/expert/questions/{id}/answers` no Authorization header
2. `POST` with a `MOTHER`-role JWT

**Expected Result (PASS):** (1) → 401; (2) → 403 `EXPQ-004`
**Current Status:** 🔴 Not written

---

### SECURITY TEST CASES

---

### EXPA-TC-SEC-001 — XSS payload in body is stored as-is (escaping is a frontend render-time responsibility, not silently mutated server-side)

**Severity:** `HIGH`
**OWASP:** `A03:2021`
**CWE:** `CWE-79`
**Feature Under Test:** `ExpertAnswerServiceImpl`
**Test File:** `ExpertAnswerServiceImplTest.java`
**TDD Phase:** 🔴 RED

**Test Steps:** Post `body = "<script>alert(1)</script> nội dung hợp lệ đủ dài"`
**Expected Result (PASS = safe & correct):** Body persisted verbatim (server does not attempt content mutation, consistent with C3 "never silently rewrite"); responsibility for output-encoding is documented as a frontend rendering requirement (React auto-escapes by default — verified separately in Web test `EXPA-TC-WEB-003`)
**Current Status:** 🔴 Not written

---

### EXPA-TC-SEC-002 — Regression: existing `CommunityAnswerController` (non-expert path) never sets `expertLabeled=true`

**Severity:** `CRITICAL`
**Feature Under Test:** `CommunityAnswerServiceImpl.postAnswer()` (EXISTING, must remain unaffected by UC-92 changes)
**Test File:** `src/test/java/com/carebridge/backend/community/service/CommunityAnswerServiceImplTest.java` (extend existing test file, do not replace)
**TDD Phase:** 🔴 RED (new assertion added to existing/new test)
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-COM-005` (existing) — regression guard for `ADR-EXP-092-01`

**Test Steps:** Call the EXISTING `communityAnswerService.postAnswer(authorId, questionId, request)` with a MOTHER-role author
**Expected Result (PASS):** `entity.isExpertLabeled() == false` always — confirms UC-92's new endpoint did not accidentally change the shared `CommunityAnswerMapper` used by the original endpoint
**Expected Result (FAIL):** `expertLabeled == true` leaking through the non-expert path (privilege escalation)

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

---

### EXPA-TC-INT-001 — Full flow: expert posts answer, DB reflects PENDING + expertLabeled=true + new column

**Severity:** `CRITICAL`
**Feature Under Test:** `Full flow: HTTP → Controller → Service → Repository → PostgreSQL (post-migration V20260703100000)`
**Test File:** `src/test/java/com/carebridge/backend/expert/ExpertAnswerIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001, TC-COND-003`

**Preconditions:** PostgreSQL Testcontainer, Flyway migrated (incl. `V20260703100000`), seeded verified expert + approved question

**Test Steps:**
1. `POST /api/v1/expert/questions/{id}/answers` with expert JWT and valid body
2. Query DB directly

**Expected Result (PASS):** `201`; DB row has `is_expert_labeled=true`, `status='PENDING'`, `expert_scope_flagged` column exists (boolean, default false unless triggered)

**DB Assertion:**
```java
CommunityAnswer saved = communityAnswerRepository.findById(responseId).orElseThrow();
assertThat(saved.isExpertLabeled()).isTrue();
assertThat(saved.getStatus()).isEqualTo(AnswerStatus.PENDING);
```

**Current Status:** 🔴 Not written

---

## Web Test Cases (Vitest + Testing Library)

### EXPA-TC-WEB-001 — Answer form submits valid payload and shows PENDING confirmation

**Severity:** `HIGH`
**Feature Under Test:** `PostExpertAnswerForm.tsx`
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/components/PostExpertAnswerForm.test.tsx`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Render form with mocked `usePostExpertAnswer` mutation
2. Type valid body (≥ 10 chars), submit
3. Assert mutation called with `{ body, isPersonalExperience }` (no `expertLabeled`/`status` field ever constructed client-side)
4. Assert success toast/banner shows "submitted for moderation" (PENDING messaging, not "published")

**Expected Result (PASS):** As above
**Current Status:** 🔴 Not written

---

### EXPA-TC-WEB-002 — Zod schema rejects body < 10 chars client-side before submit

**Severity:** `MEDIUM`
**Feature Under Test:** `postExpertAnswerSchema` (Zod)
**Test File:** `05_Development/CareBridgeWebApp/src/features/expert/models/expertAnswer.test.ts`
**TDD Phase:** 🔴 RED

**Test Steps:** `postExpertAnswerSchema.safeParse({ body: "short", isPersonalExperience: false })`
**Expected Result (PASS):** `success: false`, error path `["body"]`
**Current Status:** 🔴 Not written

---

### EXPA-TC-WEB-003 — Answer body renders without executing injected script (React default escaping)

**Severity:** `HIGH`
**OWASP:** `A03:2021`
**Feature Under Test:** Answer display component (question detail page, wherever posted answers render)
**Test File:** `05_Development/CareBridgeWebApp/src/features/community/components/AnswerCard.test.tsx` (or equivalent existing component test file)
**TDD Phase:** 🔴 RED

**Test Steps:** Render with `body = "<img src=x onerror=alert(1)>"`
**Expected Result (PASS = safe):** Rendered as literal text, no script execution, no `dangerouslySetInnerHTML` used
**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `EXPA-TC-001` | `ExpertAnswerServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-002` | `ExpertAnswerMapperTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-003` | same | `[ ]` | `[ ]` | |
| `EXPA-TC-004` | `ExpertAnswerServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-005` | same | `[ ]` | `[ ]` | |
| `EXPA-TC-006`-`009` | `PostExpertAnswerRequestValidationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-010`-`011` | `ExpertAnswerServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-012` | same | `[ ]` | `[ ]` | |
| `EXPA-TC-013` | same | `[ ]` | `[ ]` | |
| `EXPA-TC-014` | `ExpertAnswerControllerTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-SEC-001` | `ExpertAnswerServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-SEC-002` | `CommunityAnswerServiceImplTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-INT-001` | `ExpertAnswerIntegrationTest.java:TBD` | `[ ]` | `[ ]` | |
| `EXPA-TC-WEB-001`-`003` | Web test files (see above) | `[ ]` | `[ ]` | |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class ExpertAnswerServiceImpl implements IExpertAnswerService {

    @Override
    public ExpertAnswerResponse postExpertAnswer(UUID expertUserId, UUID questionId, PostExpertAnswerRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `EXPA-TC-001` | throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPA-TC-004`, `005`, `010`-`013` | throws | 🔴 FAIL | ☐ FAIL ☐ PASS | |
| `EXPA-TC-002`, `003` | N/A — mapper tested in isolation, no service stub involved; must still FAIL until `ExpertAnswerMapper` exists | 🔴 FAIL (class not found) | ☐ FAIL ☐ PASS | |
| `EXPA-TC-SEC-002` | Existing code — should already PASS today (regression guard, not new Red) | 🟢 PASS expected even pre-implementation (tests existing behavior) | ☐ PASS ☐ FAIL | If FAIL today, existing bug — escalate immediately, do not proceed with UC-92 |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL (except SEC-002 baseline)? ☐ Yes → **GATE-2 PASS**
- Log file: `[path]`

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS `CB-EXP-IMP-092` reviewed and **Approved**
- [ ] Logic Issues (§2) confirmed
- [ ] Migration `V20260703100000__add_expert_scope_flagged_to_community_answers.sql` approved and run on staging
- [ ] `EXPA-TC-SEC-002` baseline confirmed green on current `main`/`PhuongNT` BEFORE starting (proves no pre-existing regression)

### Exit Criteria (DoD)
- [ ] `./mvnw test` all green
- [ ] `./mvnw verify` integration green
- [ ] Coverage ≥ 80% for `ExpertAnswerServiceImpl`
- [ ] No business logic in `ExpertAnswerController`
- [ ] `npm run test:run` green for web test files
- [ ] `EXPA-TC-SEC-002` still green after implementation (no regression introduced)
- [ ] No PII/secret in logs

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] Red Gate (§5.1)
- [ ] Contract Existence (`./mvnw compile`)
- [ ] Props Isolation
- [ ] Oracle Source cited for every assert

### Suspension Criteria
- TDS not Approved
- Migration not yet applied to test DB
- `ExpertProfile` entity from UC-91 not yet merged (UC-92 depends on it)

---

## 7. Rollback Plan

```bash
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "ALTER TABLE community_answers DROP COLUMN IF EXISTS expert_scope_flagged;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '20260703100000';"
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/main/resources/db/migration/V20260703100000__add_expert_scope_flagged_to_community_answers.sql
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với stub throw | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Content-safety policy assumed blocking without citing ADR-EXP-092-03 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Controller test asserts content-safety heuristic logic directly | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test imports `ExpertAnswer` entity (does not exist — correct name is extended `CommunityAnswer`) | ☐ | G-3 |
| **AP-EXP-092-A** | **AI auto-diagnosing/prescribing in expert answer** | Any test or implementation path where system REJECTS or REWRITES expert body based on medical-content judgment | ☐ | **G-1 — CRITICAL, project-specific** |
| **AP-EXP-092-B** | **Auto-approving expert answer without `PENDING` default** | Any test or implementation path where `status` is set to `APPROVED` inside `ExpertAnswerServiceImpl`/`ExpertAnswerMapper` | ☐ | **G-1 — CRITICAL, project-specific** |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | None detected at Draft stage | — | — |

---

*Status: Draft — do not implement until both this file and the companion TDS are marked `Approved`.*
