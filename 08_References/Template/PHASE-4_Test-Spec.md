# TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# Mẫu Đặc tả Kiểm thử Hướng Phát triển

**Document ID:** `FPT-EDU-TDD-TEMPLATE-001`
**Version:** `1.0`
**Date:** `YYYY-MM-DD`
**Status:** `Draft | In Review | Approved`
**Standard:** ISO/IEC/IEEE 29119-3:2021 — Software Testing Part 3: Test Documentation
**Author:** `[Tên] — [Vai trò]`
**Reviewed by:** `[ ] [Tên] — Pending`
**DPO Sign-off:** `[ ] Pending`
**Approved by:** `[ ] Pending`
**Classification:** `Internal — Confidential`

**References:**
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql` â€” primary CareBridge database schema source
- `04_testing/SOFTWARE_TEST_PLAN.md` (FPT-EDU-STP-001 v2.0) — Master Test Plan
- `01_Requirements/SRS.md` — Functional requirements
- `03_implement/[TECH-SPEC-ID]_[FeatureName].md` — Technical Specification
- `02_Design/ADR/ADR-0XX` — Architecture Decision Records liên quan
- `[Điều luật]` — Legal basis (Luật 91/2025, NĐ 356/2025)

> **Quy ước TDD:** Tài liệu này mô tả test cases TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test (`.java`) → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` (backend) hoặc `npm test` (web) chưa xanh.
> Không dùng PII thật trong test data — chỉ dùng SYNTHETIC data.

---

## CHANGELOG

> **Policy 4.4 — Immutable History:** Không bao giờ xóa thông tin cũ.

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| `YYYY-MM-DD` | `[Tên]` | Khởi tạo tài liệu — TDD spec cho [Feature] |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
   - 1.1 [AI Generation Context (CASE 2.0)](#11-ai-generation-context-case-20) ⭐ *Mới*
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
   - Props Isolation Boilerplate ⭐ *Mới*
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
   - 5.1 [Red Gate Protocol (CASE 2.0)](#51-red-gate-protocol-case-20--gate-2) ⭐ *Mới*
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection-ai-assisted-tcs) ⭐ *Mới*

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | `GAP-XXXX` |
| **Module** | `[Tên module — Bounded Context]` |
| **Spec gốc** | `[FPT-EDU-TECH-SPEC-XXX-NNN]` |
| **Priority** | 🔴 P0 / 🟠 P1 / 🟡 P2 |
| **Sprint** | `S[N] (YYYY-MM-DD → YYYY-MM-DD)` |
| **Milestone** | `M3 Alpha — 2026-07-11` |
| **Data Classification** | `Sensitive-PII / PII / Internal` |
| **Compliance Scope** | `Luật 91/2025 Điều X / NĐ 356/2025 Điều X` |
| **Upstream Dependencies** | `[Service/Module phụ thuộc]` |
| **Downstream Consumers** | `[Service/Module tiêu thụ kết quả]` |

### 1.1 AI Generation Context (CASE 2.0)

> ⭐ **Section mới — CASE 2.0.** Nếu test cases được AI hỗ trợ generate, ghi lại context để traceability.
> Bỏ qua section này nếu tests được viết 100% bởi người.

| Field | Value |
|-------|-------|
| **AI Assisted?** | `Yes` / `No` |
| **Constraint Source** | `TDS-xxx §9`, `ADR-xxx §AI Prompt Constraint` |
| **Constraints Injected** | _(Liệt kê constraints chính đã inject vào prompt)_ |
| **Model** | `[Model name — version]` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> For CareBridge schema disputes, use `V1__init_schema.sql` and approved migrations as the final persistence oracle; ERD is only supporting evidence.

> **Bắt buộc điền trước khi viết test.**
> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.
> Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (schema / policy) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | _(đoạn code / logic trong spec)_ | _(field name / enum / CLAUDE.md policy)_ | _(hành vi đúng cần test)_ |
| L2 | | | |

---

## 3. Test Design Specification (TDS)

> Include `V1__init_schema.sql` and approved Flyway migrations in the test basis whenever database schema facts, constraints, or persistence side effects are part of the oracle.

### TDS-01 — Scope / Phạm vi

> Mô tả phạm vi của TDD spec này: component nào được kiểm thử, layer nào được bao phủ.

```
[Module] bao gồm các layer:
├── Domain (pure logic — no deps)
├── Application / Use Cases (mock JPA Repository với Mockito)
├── Services (mock JPA Repository với Mockito)
├── Controller (mock Service với @WebMvcTest)
└── Integration (Testcontainers PostgreSQL + Redis với @SpringBootTest)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

> Điều kiện kiểm thử được derive từ các nguồn sau:

| Source | Items Derived |
|--------|--------------|
| `SRS UC-XX` | _(hành vi người dùng / business rule)_ |
| `ADR-[MODULE]-0XX` | _(architecture constraint)_ |
| `BR-[MODULE]-XXX` | _(business rule số hiệu)_ |
| GDPR Art. XX | _(yêu cầu bảo vệ dữ liệu nếu module PII)_ |
| PDPA / BR-RBAC / BR-SECURITY | _(compliance scope của project)_ |
| `CB-[MODULE]-IMP-NNN` §X | _(algorithm / logic từ TDS kỹ thuật)_ |

### TDS-03 — Test Conditions and Coverage Items

> Mỗi condition map sang ≥ 1 test case cụ thể.

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | _(điều kiện nghiệp vụ)_ | _(method / class / API endpoint)_ | `[MODULE]-TC-001` |
| TC-COND-002 | | | |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4) | Applied To | Rationale |
|------------------------|------------|-----------|
| Equivalence Partitioning | _(input domain)_ | _(lý do)_ |
| Boundary Value Analysis | _(boundary case)_ | _(lý do)_ |
| State Transition Testing | _(FSM / status enum)_ | _(lý do)_ |
| Error Guessing | _(security / attack vectors)_ | _(lý do)_ |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-001` | DB seed | `{ id, status: 'APPROVED', ... }` | Happy path |
| `FX-002` | DB seed | `{ id, status: 'SUPERSEDED' }` | Version reject |
| `FX-003` | env | `HMAC_SECRET=test-secret-32chars` | HMAC computation |
| `FX-004` | JWT | `{ sub: 'sub-001', role: 'DPO' }` | Auth context |

---

## 4. Test Case Specification

> **TC ID format:** `[MODULE]-TC-[NNN]`
> **Severity:** CRITICAL / HIGH / MEDIUM / LOW (theo CVSS)
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> ⭐ **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua factory. Không shared mutable state giữa các test cases. Đây là biện pháp chống AP-AI-002 (Green-from-Birth).

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt ở đầu file test — mỗi @Test dùng makeEntity()
// ═══════════════════════════════════════════════════════════

// [ModuleName]TestFactory.java
class [ModuleName]TestFactory {

    // Giá trị baseline hợp lệ — đồng bộ với FX-001 (§3 TDS-05)
    static [Entity] makeEntity() {
        [Entity] entity = new [Entity]();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        // entity.setField(...);
        return entity;
    }

    // Overload để override specific fields
    static [Entity] makeEntity(Consumer<[Entity]> overrides) {
        [Entity] entity = makeEntity();
        overrides.accept(entity);
        return entity;
    }

    // Factory cho related entities (nếu cần)
    static [RelatedEntity] makeRelated() {
        // ...
        return related;
    }
}
```

---

### [MODULE]-TC-001 — [Tên test case ngắn gọn]

**Severity:** `CRITICAL | HIGH | MEDIUM | LOW`
**CWE:** `CWE-XXX — [Tên CWE nếu là security test]`
**Legal:** `[Luật / Nghị định / ADR áp dụng nếu có]`
**Feature Under Test:** `[ClassName.methodName() / API endpoint / React component]`
**Test File:** `[đường dẫn file .spec.ts hoặc .test.tsx]`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `BR-xxx-001` / `US-xxx-001/AC-001` / `ADR-xxx §Decision` ← ⭐ _giá trị kỳ vọng đến từ đâu? Không dùng AI assumption._

**Preconditions:**
- _(trạng thái DB / service / env cần có trước khi chạy test)_
- _(fixture ID cần thiết: FX-001, FX-003, ...)_

**Test Steps:**
1. _(Arrange: chuẩn bị mock / seed data)_
2. _(Act: gọi method / API endpoint)_
3. _(Assert: kiểm tra kết quả)_

**Expected Result (PASS — hành vi đúng):**
- _(kết quả trả về / DB state / exception message)_

**Expected Result (FAIL — dấu hiệu lỗi):**
- _(điều gì xảy ra nếu implementation sai)_

**Current Status:** 🔴 Not written
**Implementation Note:** _(ghi chú cho developer khi implement để pass test này)_

---

### [MODULE]-TC-002 — ...

_(Lặp lại block trên cho mỗi test case)_

---

### SECURITY TEST CASES

> Test cases kiểm tra attack vectors — điền thêm field OWASP và CWE.

---

### [MODULE]-TC-0XX — [Tên attack vector]

**Severity:** `CRITICAL`
**OWASP:** `A0X:2021 — [Category]`
**CWE:** `CWE-XXX — [Tên]`
**Legal:** `[Compliance requirement bị vi phạm nếu test FAIL]`
**Feature Under Test:** `[Endpoint / Guard / Service]`
**Test File:** `[file]`
**TDD Phase:** 🔴 RED

**Preconditions:**
- _(trạng thái cho phép tấn công)_

**Test Steps (Attack Simulation):**
1. _(Chuẩn bị điều kiện tấn công)_
2. _(Thực hiện tấn công)_
3. _(Kiểm tra response / DB state)_

**Expected Result (PASS = hệ thống an toàn):**
- `403 Forbidden` hoặc exception cụ thể

**Expected Result (FAIL = lỗ hổng tồn tại):**
- _(mô tả hành vi nguy hiểm nếu guard không hoạt động)_

**Current Status:** 🔴 Not written

---

### INTEGRATION TEST CASES

> Dùng Testcontainers (`PostgreSqlContainer` + `RedisContainer`). Timeout: 120s.

---

### [MODULE]-TC-INT-001 — [Tên scenario end-to-end]

**Severity:** `HIGH`
**Feature Under Test:** `Full flow: [bước đầu → bước cuối]`
**Test File:** `src/test/java/com/carebridge/backend/[module]/[Feature]IntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-XXX`

**Preconditions:**
- PostgreSQL container running (`@Testcontainers` auto-start)
- Flyway migration applied tự động khi Spring context start
- Seed: _(fixtures cần thiết — insert qua JPA hoặc SQL script)_

**Test Steps:**
1. _(seed minimal data)_
2. _(call API step 1)_
3. _(call API step 2)_
4. _(assert DB state)_

**Expected Result (PASS):**
- _(DB assertion: count, field values)_
- _(API response shape)_

**Expected Result (FAIL):**
- _(dấu hiệu lỗi integration)_

**DB Assertion:**
```java
// Dùng JPA repository hoặc JdbcTemplate để assert DB state
[Entity] record = [repo].findById(savedId).orElseThrow();
assertThat(record).isNotNull();
assertThat(record.getStatus()).isEqualTo([ExpectedStatus].[ENUM_VALUE]);
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| TC ID | Test File | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
|-------|-----------|-----------------|-------------------|------------------|
| `[MODULE]-TC-001` | `[path].spec.ts:line` | `[ ]` | `[hash]` | _(extract method, typing, etc.)_ |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> ⭐ **Section mới — CASE 2.0.** Đây là gate QUAN TRỌNG NHẤT.
> Trước khi implement, chạy toàn bộ test suite với empty/throw stub.
> Mọi test PHẢI FAIL. Nếu test PASS ngay → **AP-AI-002 detected** → reject và rewrite.

**Stub cho Red Phase:**

```java
// Red Phase — implementation stub (PHẢI throw)
@Service
public class [ClassName] implements I[ClassName] {

    @Override
    public [ReturnType] [methodName]([ParamType] input) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    // Lặp lại cho mỗi method trong interface
}
```

**Red Gate Verification:**

| TC ID | Stub Result | Expected | Actual | Root Cause (nếu PASS bất thường) |
|-------|-------------|----------|--------|----------------------------------|
| `[MODULE]-TC-001` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state ☐ Hallucinated import |
| `[MODULE]-TC-002` | `throw('Not implemented')` | 🔴 FAIL | ☐ FAIL ☐ PASS | |

**Red Gate Evidence:**

- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement
- Log file: `[path to red-gate-evidence.log]`

> **Nếu bất kỳ test PASS:** Dừng lại. Xác định root cause từ bảng trên. Rewrite test từ TC-ID spec với Props Isolation Pattern.

---

## 6. Entry / Exit Criteria

### Entry Criteria (Điều kiện bắt đầu)

- [ ] TDS `CB-[MODULE]-IMP-NNN` đã được review và approve
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Flyway migration `V{n}__[name].sql` đã được approved và chạy thành công trên staging
- [ ] Test fixtures (Section 3 TDS-05) đã được chuẩn bị

### Exit Criteria (Điều kiện kết thúc — DoD)

- [ ] `./mvnw test` — tất cả unit tests xanh (không có skip)
- [ ] `./mvnw verify` — tất cả integration tests xanh (Testcontainers)
- [ ] Test coverage ≥ 80% lines cho Service class mới tạo
- [ ] Không có business logic trong Controller (chỉ có validation + mapping)
- [ ] Không có PII/secret xuất hiện plaintext trong logs
- [ ] _(tiêu chí nghiệp vụ cụ thể của feature này)_

**Exit Criteria bổ sung — CASE 2.0:**

- [ ] **Red Gate (§5.1)** — tất cả tests FAIL với empty/throw stub trước khi implement
- [ ] **Contract Existence** — mọi class được inject đều tồn tại trong codebase:
  ```bash
  ./mvnw compile 2>&1 | grep "error:"
  # Expected: no output
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests:
  ```bash
  # Kiểm tra: biến tạo ngoài @Test có bị mutate không?
  grep -n "^    [A-Z].*=.*new \|^    [a-z].*=.*new " [test-file]
  # Mọi instance PHẢI nằm trong @Test hoặc dùng factory method
  ```
- [ ] **Oracle Source** — mọi expected value trong assert có ghi rõ nguồn (BR/AC/ADR)

### Suspension Criteria (Điều kiện tạm dừng)

- Blocker dependency chưa sẵn sàng (migration, external service)
- Phát hiện lỗi kiến trúc mới cần Principal Architect review
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert migration thủ công (dev only — KHÔNG chạy trên production)
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DROP TABLE IF EXISTS [table_name] CASCADE;"
psql -h $DB_HOST -U $DB_USER -d $DB_NAME \
  -c "DELETE FROM flyway_schema_history WHERE version = '{n}';"

# Revert implementation files
git checkout -- src/main/java/com/carebridge/backend/[module]/
git checkout -- src/main/resources/db/migration/V{n}__[migration_name].sql
git checkout -- src/test/java/com/carebridge/backend/[module]/

# Gap vẫn OPEN → giữ nguyên entry trong PHASE_GAP_ANALYSIS.md
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

> ⭐ **Section mới — CASE 2.0.** Checklist cho reviewer khi test cases được AI hỗ trợ generate.
> Bỏ qua section này nếu tests được viết 100% bởi người.

| AP-ID | Anti-Pattern | Dấu hiệu trong TDD spec | Check | Gate chặn |
|-------|-------------|--------------------------|-------|-----------|
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/TDS constraint nào | ☐ | G-0 |
| AP-AI-002 | Green-from-Birth | Test PASS với empty/throw stub (§5.1) | ☐ | G-2 ★ |
| AP-AI-003 | Implicit Decision | Test assume architecture decision không có ADR | ☐ | G-1 |
| AP-AI-004 | Layer Violation | Test verify controller có business logic | ☐ | G-4 |
| AP-AI-005 | Hallucinated Contract | Test import service/type không tồn tại trong codebase | ☐ | G-3 |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
|------------|-------|-------|------------|--------|
| `AP-AI-___` | `TC-___` | [mô tả issue] | [hành động fix] | ☐ |

---

*TDD Template v2.0 — Tích hợp CASE 2.0 Anti-Pattern Detection & Red Gate Protocol*
*Sections đánh dấu ⭐ là bổ sung mới từ CASE 2.0 methodology.*
