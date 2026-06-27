# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-89 Upload Verification Documents

**Document ID:** `CB-EXP-TDD-002`
**Version:** `1.0`
**Date:** `2026-06-26`
**Status:** `Draft`
**Author:** `AI Agent`

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| 2026-06-26 | AI Agent | Khởi tạo TDD spec cho UC-89 |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs)

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `UC-89` |
| **Module** | `UploadVerificationDocuments — expert` |
| **Priority** | 🟠 P1 |
| **Data Classification** | `Confidential` |

---

## 2. Logic Issues Resolved

| # | Spec gốc | Thực tế | Fix |
|---|----------|---------|-----|
| L1 | SRS: "uploads degrees, certificates" — no limit specified | ADR-EXP-003: max 10 docs per profile | Test doc quota |
| L2 | SRS: no MIME restriction mentioned | ADR-FILE-001: only PDF/JPEG/PNG/HEIC | Test .exe → 400 |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
UploadVerificationDocuments bao gồm các layer:
├── Service (mock Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| SRS UC-89 | Hành vi người dùng |
| ADR-EXP | Architecture constraints |
| BR-RBAC | Role-based access control |
| CB-EXP-IMP-002 | TDS technical specification |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | Happy path | Service method | DOC-TC-001 |
| TC-COND-002 | Auth/permission check | Controller | DOC-TC-00X |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | Input validation | Valid/invalid input classes |
| Boundary Value Analysis | Limits (pagination, size) | Edge values |
| Error Guessing | Security vectors | OWASP Top 10 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| FX-001 | DB seed | Valid entity | Happy path |
| FX-002 | JWT | ROLE_MOTHER token | Auth context |

---

## 4. Test Case Specification

### Props Isolation Boilerplate

```java
class DocUploadTestFactory {
    static MockMultipartFile makePdfDoc() {
        return new MockMultipartFile("file", "degree.pdf",
            "application/pdf", new byte[1024]);
    }
    static MockMultipartFile makeLargeFile() {
        return new MockMultipartFile("file", "big.pdf",
            "application/pdf", new byte[21 * 1024 * 1024]); // 21MB
    }
    static MockMultipartFile makeInvalidFile() {
        return new MockMultipartFile("file", "virus.exe",
            "application/octet-stream", new byte[1024]);
    }
}
```

---

### DOC-TC-001 — PDF upload → 201

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** Response with `status=PENDING_REVIEW`, non-null id, storageKey is UUID format

**Current Status:** 🔴 Not written

---

### DOC-TC-002 — File > 20MB → 400

**Severity:** `HIGH`
**Oracle Source:** `ADR-FILE-001`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (EXP-007)

**Current Status:** 🔴 Not written

---

### DOC-TC-003 — .exe file → 400

**Severity:** `HIGH`
**Oracle Source:** `ADR-FILE-001`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ValidationException (EXP-006)

**Current Status:** 🔴 Not written

---

### DOC-TC-004 — Quota exceeded (11th doc) → 409

**Severity:** `HIGH`
**Oracle Source:** `ADR-EXP-003`
**TDD Phase:** 🔴 RED

**Test Steps:**
1. Expert profile has 10 existing documents
2. Upload 11th document

**Expected Result:** throws ConflictException (EXP-005)

**Current Status:** 🔴 Not written

---

### DOC-TC-005 — Non-owner → 403

**Severity:** `CRITICAL`
**TDD Phase:** 🔴 RED

**Expected Result:** throws ForbiddenException (EXP-008)

**Current Status:** 🔴 Not written

---

### DOC-TC-006 — storageKey is UUID, not filename

**Severity:** `HIGH`
**Oracle Source:** `ADR-FILE-003`
**TDD Phase:** 🔴 RED

```java
ExpertDocumentResponse resp = service.uploadDocument(..., makePdfDoc(), DEGREE);
assertThat(resp.getStorageKey()).matches(
    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
);
assertThat(resp.getStorageKey()).doesNotContain("degree.pdf");
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | 🔴 RED | 🟢 GREEN |
|-------|--------|----------|
| `DOC-TC-001` | `[ ]` | `___` |
| `DOC-TC-003` | `[ ]` | `___` |
| `DOC-TC-004` | `[ ]` | `___` |
| `DOC-TC-006` | `[ ]` | `___` |

---

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

**Stub cho Red Phase:**

```java
@Service
public class DocumentUploadService implements IDocumentUploadService {
    @Override
    public ExpertDocumentResponse uploadDocument(UUID expertProfileId, MultipartFile file, DocumentType type) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual |
|-------|-------------|----------|--------|
| DOC-TC-001 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |
| DOC-TC-002 | throw('Not implemented') | 🔴 FAIL | ☐ FAIL ☐ PASS |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3)

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [ ] TDS đã được review và approve
- [ ] Flyway migration đã chạy thành công trên staging
- [ ] Test fixtures đã được chuẩn bị

### Exit Criteria (DoD)
- [ ] `./mvnw test` — tất cả unit tests xanh
- [ ] `./mvnw verify` — integration tests xanh
- [ ] Test coverage ≥ 80% cho Service class
- [ ] Không có PII trong logs
- [ ] **Red Gate (§5.1)** passed
- [ ] **Props Isolation** — no shared mutable state

---

## 7. Rollback Plan

```bash
# Revert migration (dev only)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS expert_documents CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '034';"

# Revert code
git checkout -- src/main/java/com/carebridge/backend/expert/
git checkout -- src/test/java/com/carebridge/backend/expert/
```

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Gate |
|-------|-------|------|
| AP-AI-002 | ☐ | G-2 ★ |
| AP-AI-005 | ☐ IStorageService exists | G-3 |
