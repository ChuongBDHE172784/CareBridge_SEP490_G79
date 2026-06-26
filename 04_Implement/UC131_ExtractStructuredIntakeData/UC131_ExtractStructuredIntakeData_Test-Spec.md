# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC131 — Extract Structured Intake Data

**Document ID:** `CB-AI-IMP-001-TEST`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Standard:** ISO/IEC/IEEE 29119-3:2021
**Author:** `AI Agent — Tech Lead`
**Reviewed by:** `[ ] Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `04_Implement/UC131_ExtractStructuredIntakeData/UC131_ExtractStructuredIntakeData_TDS.md` (CB-AI-IMP-001)
- `02_Requirements/SRS/Functional_Specifications.md §3.1.2.5`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent — Tech Lead | Khởi tạo tài liệu — TDD spec cho UC131 Extract Structured Intake Data |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `CB-AI-IMP-001` |
| **Module** | `Extract Structured Intake Data — ai (internal)` |
| **Spec gốc** | `CB-AI-IMP-001` |
| **Priority** | 🟠 P1 |
| **Sprint** | `S1 (2026-06-26 → 2026-07-10)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII` |
| **Compliance Scope** | `PDPA / Luật 91/2025` |
| **Upstream Dependencies** | `UC60 IntakeSessionCompleted event, Gemini AI` |
| **Downstream Consumers** | `UC62 OpenEmergencyFlow (via EmergencyEscalationTriggered)` |

### 1.1 AI Generation Context (CASE 2.0)

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` |
| **Constraint Source** | `CB-AI-IMP-001 §17`, `ADR-AI-001, ADR-AI-002` |
| **Constraints Injected** | C1 (emergency first), C2 (no raw PII), C3 (event-driven only), C4 (duplicate check), C5 (SYSTEM created_by) |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | emergencyFlag handling không rõ thứ tự | ADR-AI-002: publish event TRƯỚC khi lưu DB | Test verify event published trước khi DB insert |
| L2 | Duplicate extraction không được mention | DuplicateExtractionException nếu sessionId đã có | Test verify idempotency |
| L3 | createdBy field không rõ | ADR-AI-001: SYSTEM (không có userId) | Test verify createdBy = "SYSTEM" |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UC131 Extract Structured Intake Data bao gồm:
├── Service (StructuredIntakeService — mock GeminiExtractionClient và EventPublisher)
├── Event Handler (IntakeSessionCompletedHandler — verify event wiring)
└── Integration (Testcontainers PostgreSQL — verify DB persistence + event ordering)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS UC-131 §3.1.2.5` | Gemini trích xuất symptomList, duration, intensity, emergencyFlag |
| `ADR-AI-001` | @EventListener only — no HTTP endpoint |
| `ADR-AI-002` | emergencyFlag=true → route to emergency NGAY |
| `BR-AI-005` | Structured fields: symptomList, durationDays, intensity, emergencyFlag |
| `BR-AI-006` | emergencyFlag=true → EmergencyEscalationTriggered published |
| `BR-PRIVACY-002` | No raw symptom text in structured_intake_data |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Normal extraction → save structured data | `StructuredIntakeService.extract()` | `AI-TC-001` |
| TC-COND-002 | emergencyFlag=true → publish event TRƯỚC save | Event ordering | `AI-TC-002` |
| TC-COND-003 | Duplicate sessionId → AI-002 | `existsBySessionId()` check | `AI-TC-003` |
| TC-COND-004 | Gemini extraction fail → AI-003 | Exception handling | `AI-TC-004` |
| TC-COND-005 | structured_intake_data không chứa raw text | BR-PRIVACY-002 | `AI-TC-005` |
| TC-COND-006 | createdBy = "SYSTEM" | ADR-AI-001 | `AI-TC-006` |
| TC-COND-007 | Event handler trigger on IntakeSessionCompleted | @EventListener wiring | `AI-TC-INT-001` |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| State Transition Testing | emergencyFlag true/false | Verify emergency routing |
| Error Guessing | Duplicate sessionId | Idempotency |
| Equivalence Partitioning | extraction result (success/fail) | Cover error paths |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | Mock | `GeminiExtractionClient.extract() → {symptomList: ['headache'], duration: 2, emergencyFlag: false}` | Normal extraction |
| `FX-002` | Mock | `GeminiExtractionClient.extract() → {emergencyFlag: true}` | Emergency case |
| `FX-003` | Mock | `GeminiExtractionClient.extract() → throw ExtractionException` | Failure case |
| `FX-004` | Event | `IntakeSessionCompleted {sessionId: 'session-001', userId: 'user-001', rawAiResponse: '...'}` | Event fixture |
| `FX-005` | DB seed | `structured_intake_data {session_id: 'session-001'}` | Duplicate test |

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// StructuredIntakeTestFactory.java
class StructuredIntakeTestFactory {

    static IntakeSessionCompleted makeEvent() {
        return new IntakeSessionCompleted(
            UUID.randomUUID(),
            "IntakeSessionCompleted",
            Instant.now(),
            "1.0",
            new IntakeSessionCompleted.Payload(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "GREEN",
                "SYNTHETIC_DISCLAIMER"
            ),
            new IntakeSessionCompleted.Metadata(UUID.randomUUID(), "user-001")
        );
    }

    static StructuredIntakeData makeExtracted() {
        StructuredIntakeData data = new StructuredIntakeData();
        data.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        data.setSessionId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        data.setSymptomList(List.of("headache", "fever"));
        data.setDurationDays(2);
        data.setIntensity("LOW");
        data.setEmergencyFlag(false);
        data.setCreatedBy("SYSTEM");
        return data;
    }
}
```

---

### AI-TC-001 — Normal extraction → structured data saved

**Severity:** `HIGH`
**Feature Under Test:** `StructuredIntakeService.extract()`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-AI-005 / SRS UC-131 §3.1.2.5`

**Preconditions:**
- GeminiExtractionClient mock: FX-001 (normal result, emergencyFlag=false)
- Repository mock: `existsBySessionId()` → false

**Test Steps:**
1. Arrange: mock Gemini → FX-001; mock repo existsBySessionId → false
2. Act: `structuredIntakeService.extract(makeEvent())`
3. Assert: repo.save() called once; saved data has symptomList, duration, intensity

**Expected Result (PASS):**
- `StructuredIntakeData.symptomList` không rỗng
- `StructuredIntakeData.emergencyFlag` = false
- `repository.save()` called exactly once

**Current Status:** 🔴 Not written

---

### AI-TC-002 — emergencyFlag=true → event published BEFORE DB save

**Severity:** `CRITICAL`
**Legal:** `ADR-AI-002 / BR-SAFETY`
**Feature Under Test:** `StructuredIntakeService — emergency event ordering`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `ADR-AI-002`

**Preconditions:**
- GeminiExtractionClient mock: FX-002 (emergencyFlag=true)
- InOrder mock verification available (Mockito InOrder)

**Test Steps:**
1. Arrange: mock Gemini → emergencyFlag=true; InOrder verifier
2. Act: `structuredIntakeService.extract(makeEvent())`
3. Assert InOrder: `eventPublisher.publishEvent(EmergencyEscalationTriggered)` BEFORE `repository.save()`

**Expected Result (PASS — SAFETY CRITICAL):**
- EmergencyEscalationTriggered published TRƯỚC save()

**Expected Result (FAIL = safety violation):**
- save() called before event published — vi phạm BR-SAFETY

**Current Status:** 🔴 Not written
**Implementation Note:** Dùng Mockito `InOrder` để verify ordering. CRITICAL test — không được skip.

---

### AI-TC-003 — Duplicate sessionId → DuplicateExtractionException (AI-002)

**Severity:** `MEDIUM`
**Feature Under Test:** `StructuredIntakeService — idempotency`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `CB-AI-IMP-001 §10 AI-002`

**Test Steps:**
1. Arrange: mock `existsBySessionId()` → true (FX-005)
2. Act: `structuredIntakeService.extract(makeEvent())`
3. Assert: `DuplicateExtractionException` thrown; `repository.save()` NOT called

**Expected Result (PASS):**
- `DuplicateExtractionException` thrown
- No DB insert

**Current Status:** 🔴 Not written

---

### AI-TC-004 — Gemini extraction fail → ExtractionFailedException (AI-003)

**Severity:** `HIGH`
**Feature Under Test:** `StructuredIntakeService — Gemini error handling`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`

**Test Steps:**
1. Arrange: mock Gemini → throw ExtractionException (FX-003)
2. Act: `structuredIntakeService.extract(makeEvent())`
3. Assert: `ExtractionFailedException` thrown; repo.save() NOT called

**Expected Result (PASS):**
- `ExtractionFailedException` thrown
- No DB record inserted

**Current Status:** 🔴 Not written

---

### AI-TC-005 — No raw PII text in structured_intake_data

**Severity:** `CRITICAL`
**Legal:** `BR-PRIVACY-002 / PDPA`
**Feature Under Test:** `StructuredIntakeData fields — no raw text`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`

**Test Steps:**
1. Arrange: mock Gemini → FX-001
2. Act: `structuredIntakeService.extract(makeEvent())`
3. Capture saved StructuredIntakeData
4. Assert: NO field contains "SYNTHETIC" or raw symptom free-text — only structured fields

**Expected Result (PASS):**
- `symptomList` contains structured enum/keyword list, not free text
- No field = raw symptoms string from intake

**Implementation Note:** symptomList phải là `List<String>` với từ khoá chuẩn hoá — không copy paste raw text

**Current Status:** 🔴 Not written

---

### AI-TC-006 — createdBy = "SYSTEM"

**Severity:** `MEDIUM`
**Feature Under Test:** `StructuredIntakeData.createdBy`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeServiceTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `ADR-AI-001`

**Test Steps:**
1. Act: extract và capture saved StructuredIntakeData
2. Assert: `createdBy` = "SYSTEM"

**Expected Result (PASS):**
- `StructuredIntakeData.createdBy` = "SYSTEM"

**Current Status:** 🔴 Not written

---

### AI-TC-INT-001 — @EventListener trigger on IntakeSessionCompleted

**Severity:** `HIGH`
**Feature Under Test:** `IntakeSessionCompletedHandler wiring`
**Test File:** `src/test/java/com/carebridge/backend/ai/StructuredIntakeIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- Spring context loaded (Testcontainers PostgreSQL)
- Flyway V36 migration applied
- GeminiExtractionClient mocked

**Test Steps:**
1. Seed: intake_session với status = COMPLETED
2. Publish: `IntakeSessionCompleted` event programmatically
3. Wait: 2s for async processing
4. Assert: `structured_intake_data` has record with session_id

**Expected Result (PASS):**
- DB record exists in `structured_intake_data`

**DB Assertion:**
```java
Optional<StructuredIntakeData> result = structuredRepo.findBySessionId(sessionId);
assertThat(result).isPresent();
assertThat(result.get().getCreatedBy()).isEqualTo("SYSTEM");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `AI-TC-001` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | — |
| `AI-TC-002` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | CRITICAL — verify InOrder |
| `AI-TC-003` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | — |
| `AI-TC-004` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | — |
| `AI-TC-005` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | — |
| `AI-TC-006` | `StructuredIntakeServiceTest.java` | `[ ]` | `—` | — |
| `AI-TC-INT-001` | `StructuredIntakeIntegrationTest.java` | `[ ]` | `—` | — |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
// StructuredIntakeService.java — Red Phase stub
@Service
public class StructuredIntakeService implements IStructuredIntakeService {

    @Override
    public StructuredIntakeData extract(IntakeSessionCompleted event) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `AI-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-003` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-004` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-005` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-006` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |
| `AI-TC-INT-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | — |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-AI-IMP-001` đã được review và approve
- [ ] UC60 intake_sessions table tồn tại (V35 applied)
- [ ] Migration V36 schema đã approved
- [ ] Logic Issues (Section 2) đã được confirm

### Exit Criteria (DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh
- [ ] Test coverage ≥ 80% lines cho StructuredIntakeService
- [ ] AI-TC-002 (emergency ordering) PASS — CRITICAL
- [ ] AI-TC-005 (no raw PII) PASS — CRITICAL
- [ ] Không có HTTP endpoint cho UC131

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với throw stub
- [ ] **Contract Existence** — `./mvnw compile 2>&1 | grep "error:"` → no output
- [ ] **Props Isolation** — mọi instance dùng `StructuredIntakeTestFactory`

### Suspension Criteria

- GeminiExtractionClient không available
- V36 migration fail trên staging
- @EventListener infrastructure broken

---

## 7. Rollback Plan

```bash
# Revert migration V36
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DROP TABLE IF EXISTS structured_intake_data CASCADE;"
psql -h $DB_HOST -U $DB_USER -d carebridge \
  -c "DELETE FROM flyway_schema_history WHERE version = '36';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/ai/
git checkout -- src/main/resources/db/migration/V36__create_structured_intake_data.sql
git checkout -- src/test/java/com/carebridge/backend/ai/
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | AI-TC-002 không verify event ordering | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với throw stub | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Code thêm HTTP endpoint không có ADR-AI-001 | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Handler có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import ExtractionResultRepository không có trong §8 | ☐ | G-3 |

**Kết quả review:**
- [ ] Không phát hiện anti-pattern nào → TDD spec approved

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| — | — | — | — | — |

---

*TDD Spec v1.0 — UC131 Extract Structured Intake Data — CB-AI-IMP-001-TEST*
