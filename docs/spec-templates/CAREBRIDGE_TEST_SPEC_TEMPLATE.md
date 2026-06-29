# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION TEMPLATE
# Mẫu Đặc tả Kiểm thử Hướng Phát triển — CareBridge Stack

| Trường | Giá trị |
|--------|---------|
| **Document ID** | `CB-[DOMAIN]-PKG-[NN]-TDD` hoặc `CB-[DOMAIN]-TDD-[NNN]` |
| **Version** | `1.0` |
| **Date** | `YYYY-MM-DD` |
| **Status** | `DRAFT` |
| **Spec gốc** | `CB-[DOMAIN]-PKG-[NN]-TDS (docs/spec-packages/PKG-[NN]-*/PKG-[NN]_TDS.md)` |
| **Package** | `PKG-[NN] — [Package Name]` |
| **Included UCs** | `UC-XXX, UC-YYY` |
| **Author** | `[Tên + Role]` |
| **Reviewed by** | `[ ] [Tech Lead]` |
| **Approved by** | `[ ] Pending` |

> **Stack kiểm thử:** JUnit 5 · Mockito · MockMvc · Spring Boot Test Slices · Testcontainers PostgreSQL (khi cần) · Vitest (React) · Flutter test framework · Chrome (browser acceptance)
>
> **Quy ước TDD:** Viết test TRƯỚC khi viết production code.
> Thứ tự bắt buộc: viết test `.java` → chạy `./mvnw test` → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là 🟢 nếu `./mvnw test` chưa xanh.
> Test data phải SYNTHETIC — không dùng PII thực.

---

## CHANGELOG

| Ngày | Người thực hiện | Nội dung thay đổi |
|------|-----------------|-------------------|
| YYYY-MM-DD | [Tên — Role] | Khởi tạo TDD spec cho [Package] |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification](#3-test-design-specification)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Test Commands](#7-test-commands)
8. [Rollback Plan](#8-rollback-plan)
9. [CASE 2.0 Anti-Pattern Detection](#9-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Trường | Giá trị |
|--------|---------|
| **Feature / UC IDs** | `UC-XXX, UC-YYY` |
| **Module** | `[domain] — [ServiceImplName]` |
| **Spec gốc** | `CB-[DOMAIN]-PKG-[NN]-TDS` |
| **Priority** | 🔴 P0 / 🟠 P1 / 🟡 P2 |
| **Sprint** | `S[N] (YYYY-MM-DD → YYYY-MM-DD)` |
| **Milestone** | `[Milestone name]` |
| **Data Classification** | `Internal / Confidential` |
| **Upstream Dependencies** | `[PKG-XX]` |
| **Downstream Consumers** | `[PKG-YY]` |

### 1.1 AI Generation Context (CASE 2.0)

| Trường | Giá trị |
|--------|---------|
| **AI Assisted?** | `Yes / No` |
| **Constraint Source** | `CB-[DOMAIN]-PKG-[NN]-TDS §20`, `ADR-[DOMAIN]-[NNN]` |
| **Constraints Injected** | _(Liệt kê constraints đã inject vào prompt)_ |
| **Model** | `claude-sonnet-4-6` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

> **Bắt buộc điền trước khi viết test.**
> Liệt kê mọi sai lệch giữa spec thiết kế và schema/policy/codebase thực tế.
> Test cases sẽ encode hành vi **đã sửa**, không phải hành vi trong spec gốc.

| # | Spec gốc (sai / thiếu) | Thực tế (V1 schema / code / policy) | Fix áp dụng trong test |
|---|------------------------|-------------------------------------|------------------------|
| L1 | _(logic issue trong spec)_ | _(V1 reality hoặc code behavior)_ | _(behavior đúng cần test)_ |

---

## 3. Test Design Specification

### TDS-01 — Scope / Phạm vi

```
[Package Name] bao gồm các test layers:
├── Service Unit Tests  (Mockito — mock Repository, no DB)
├── Controller Tests    (MockMvc — mock Service, test auth + validation)
├── Security Tests      (MockMvc — test 401/403 scenarios)
├── Integration Tests   (Testcontainers PostgreSQL — chỉ khi justified, xem TDS-06)
├── Web UI Tests        (Vitest — nếu package có React components)
└── Mobile Tests        (Flutter test — nếu package có Flutter screens)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source | Items Derived |
|--------|--------------|
| `SRS §3.x.x.x` UC-XXX | _(behavior người dùng / business rule)_ |
| `CB-[DOMAIN]-PKG-[NN]-TDS ADR-[NNN]` | _(architecture constraint)_ |
| `BR-[DOMAIN]-[NNN]` | _(business rule)_ |
| `V1__init_schema.sql` | _(DB constraints)_ |
| `rbac-role-mapping.md` | _(role-based access rules)_ |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage Item | Test Cases |
|-------------|---------------|---------------|-----------|
| TC-COND-001 | [Happy path condition] | `[ServiceImpl.method()]` | `[MODULE]-TC-001` |
| TC-COND-002 | [Validation failure] | `@NotBlank / @Size` | `[MODULE]-TC-002` |
| TC-COND-003 | [Auth failure — wrong role] | `@PreAuthorize` | `[MODULE]-TC-003` |
| TC-COND-004 | [Auth failure — unauthenticated] | Security config | `[MODULE]-TC-004` |
| TC-COND-005 | [Business rule violation] | `[ServiceImpl.method()]` | `[MODULE]-TC-005` |
| TC-COND-006 | [Not found case] | `repository.findById()` | `[MODULE]-TC-006` |

### TDS-04 — Test Techniques (ISO 29119-4)

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| Equivalence Partitioning | [Input fields với multiple valid/invalid categories] | [Lý do] |
| Boundary Value Analysis | [Fields với min/max length hoặc numeric bounds] | [Lý do] |
| State Transition Testing | [Enum status fields — FSM] | [Lý do] |
| Error Guessing | [Security boundaries, role checks, null handling] | OWASP A01 |

### TDS-05 — Test Data Requirements

| Fixture ID | Type | Value / Logic | Mục đích |
|-----------|------|---------------|---------|
| `FX-[MODULE]-001` | JWT | `{ sub: "[valid-uuid]", role: "[ROLE]" }` | Happy path auth |
| `FX-[MODULE]-002` | JWT | `{ sub: "[valid-uuid]", role: "[WRONG_ROLE]" }` | Unauthorized write |
| `FX-[MODULE]-003` | DB seed | `[Entity] { id: UUID, [field]: value, ... }` | Existing record test |
| `FX-[MODULE]-004` | Input | `{ [field]: "[valid value]", ... }` | Valid create/update |
| `FX-[MODULE]-005` | Input | `{ [field]: "" }` | Empty/blank validation |

> **Synthetic data policy:** Tất cả UUIDs và data phải SYNTHETIC. Không dùng real user data. Không hardcode production UUIDs.

### TDS-06 — Integration Test Justification

> Testcontainers PostgreSQL chỉ dùng khi **ÍT NHẤT MỘT** điều kiện sau thỏa:
> - Test verifies complex DB constraint (CASCADE, unique index, FK)
> - Test verifies migration behavior
> - Test verifies @Transactional rollback
> - Mockito không thể simulate behavior cần test

Áp dụng Testcontainers cho package này: `YES / NO` — vì: _[lý do]_

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

> **CASE 2.0 Rule:** Mỗi test PHẢI tạo fresh instance qua builder/factory. Không shared mutable state giữa test cases.

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern (Java/JUnit 5)
// Đặt ở đầu test class — mỗi @Test method gọi builder mới
// ═══════════════════════════════════════════════════════════

// UUID constants cho test fixtures (synthetic — không phải production UUIDs)
private static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID TEST_ENTITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

// Factory method — luôn tạo fresh object
private [EntityName] make[Entity](Consumer<[Entity].Builder> override) {
    [Entity].Builder builder = [Entity].builder()
        .id(TEST_ENTITY_ID)
        .[field]("[base value]")
        // ... default values
        ;
    override.accept(builder);
    return builder.build();
}

private [CreateRequest] make[CreateRequest](Consumer<[CreateRequest].Builder> override) {
    // Tương tự cho DTOs
}
```

---

### [MODULE]-TC-001 — [Tên test case: Happy Path]

**Severity:** `CRITICAL / HIGH / MEDIUM / LOW`
**Feature Under Test:** `[ServiceImplName].[methodName]()`
**Test File:** `src/test/java/com/carebridge/backend/[domain]/service/[ServiceImplTest].java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-XXX normal flow / BR-[DOMAIN]-[NNN]`

**Preconditions:**
- `[Mock setup: when(repository.existsBy...).thenReturn(false)]`

**Test Steps:**
```java
@Test
void [methodName]_withValidInput_returnsResponse() {
    // Arrange
    [CreateRequest] request = make[CreateRequest](b -> b.[field]("[value]"));
    when(mockRepository.existsBy[Field]("[value]")).thenReturn(false);
    when(mockRepository.save(any())).thenReturn(make[Entity](b -> {}));

    // Act
    [ResponseDTO] result = service.[methodName](TEST_USER_ID, request);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.[field]()).isEqualTo("[value]");
    verify(mockRepository).save(any([Entity].class));
    verify(mockAuditService).emit(any());  // nếu audit required
}
```

**Expected Result:**
- Return value: `[ResponseDTO] với [field]="[value]"`
- Side effects: `repository.save()` called once; audit event emitted

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-002 — [Validation: Blank/Empty Field]

**Severity:** `HIGH`
**Feature Under Test:** `[ControllerName].createXxx()` via MockMvc
**Test File:** `src/test/java/com/carebridge/backend/[domain]/controller/[ControllerTest].java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `@NotBlank annotation / [DOMAIN]-001 error code`

**Test Steps:**
```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"[ROLE]"})
void create_withBlank[Field]_returns400() throws Exception {
    String requestJson = """
        { "[field]": "" }
        """;

    mockMvc.perform(post("/api/v1/[path]")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isBadRequest());
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-003 — [Security: Wrong Role → 403]

**Severity:** `CRITICAL`
**Feature Under Test:** `@PreAuthorize("hasRole('[ROLE]')")` on `[ControllerName]`
**Test File:** `src/test/java/com/carebridge/backend/[domain]/controller/[ControllerTest].java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-RBAC / rbac-role-mapping.md §3`

**Test Steps:**
```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000003", roles = {"[WRONG_ROLE]"})
void createXxx_withWrongRole_returns403() throws Exception {
    mockMvc.perform(post("/api/v1/[path]")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[minimal valid body]"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(mockService);
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-004 — [Security: Unauthenticated → 401]

**Severity:** `CRITICAL`
**Feature Under Test:** Security filter chain
**Test File:** `src/test/java/com/carebridge/backend/[domain]/controller/[ControllerTest].java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-004`

**Test Steps:**
```java
@Test
void createXxx_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/[path]")
            .contentType(MediaType.APPLICATION_JSON)
            .content("[minimal valid body]"))
        .andExpect(status().isUnauthorized());
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-005 — [Business Rule Violation]

**Severity:** `HIGH`
**Feature Under Test:** `[ServiceImplName].[methodName]()`
**Test File:** `src/test/java/com/carebridge/backend/[domain]/service/[ServiceImplTest].java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-[DOMAIN]-[NNN]`

**Test Steps:**
```java
@Test
void [methodName]_whenBusinessRuleViolated_throwsException() {
    // Arrange — setup violation condition
    when(mockRepository.[checkMethod]("[value]")).thenReturn(true);
    [CreateRequest] request = make[CreateRequest](b -> b.[field]("[duplicate]"));

    // Act & Assert
    assertThatThrownBy(() -> service.[methodName](TEST_USER_ID, request))
        .isInstanceOf([ExceptionClass].class);

    verify(mockRepository, never()).save(any());
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-006 — [Not Found Case → 404]

**Severity:** `HIGH`
**Feature Under Test:** `[ServiceImplName].updateXxx()` / `getXxx()`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-006`

**Test Steps:**
```java
@Test
void updateXxx_withNonExistentId_throwsNotFoundException() {
    UUID nonExistentId = UUID.randomUUID();
    when(mockRepository.findById(nonExistentId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.updateXxx(nonExistentId, anyRequest))
        .isInstanceOf([NotFoundException].class);
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-INT-001 — [Integration Test] (Testcontainers — chỉ nếu TDS-06 = YES)

**Severity:** `HIGH`
**Feature Under Test:** Full stack via `@SpringBootTest` + real PostgreSQL
**Test File:** `src/test/java/com/carebridge/backend/[domain]/[FeatureIntegrationTest].java`
**TDD Phase:** 🔴 RED — chưa implement

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class [Feature]IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    // [Test methods verifying DB constraints]
}
```

**Current Status:** 🔴 Not written

---

### [MODULE]-TC-ACCOUNT-001 — [Account Disabled/Locked Regression]

> Bắt buộc cho mọi endpoint yêu cầu authentication.

**Severity:** `HIGH`
**Feature Under Test:** `JwtAuthenticationFilter` account state check
**Test File:** `src/test/java/com/carebridge/backend/[domain]/controller/[ControllerTest].java`
**TDD Phase:** 🔴 RED — chưa implement

```java
@Test
void endpoint_withDisabledAccount_returns401OrForbidden() throws Exception {
    // Setup user with enabled=false or locked=true
    // Verify endpoint returns 401/403 appropriately
}
```

**Current Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| Test Case | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR |
|-----------|:----------------:|:-----------------:|:-----------:|
| [MODULE]-TC-001 | `[ ]` | — | — |
| [MODULE]-TC-002 | `[ ]` | — | — |
| [MODULE]-TC-003 | `[ ]` | — | — |
| [MODULE]-TC-004 | `[ ]` | — | — |
| [MODULE]-TC-005 | `[ ]` | — | — |
| [MODULE]-TC-006 | `[ ]` | — | — |
| [MODULE]-TC-INT-001 | `[ ]` | — | — |
| [MODULE]-TC-ACCOUNT-001 | `[ ]` | — | — |

### 5.1 Red Gate Protocol (CASE 2.0 — Gate 2)

> Trước khi viết production code, phải xác nhận test thực sự FAIL.

| Test | Expected Result khi chạy trước khi implement | Actual (điền sau khi chạy) | Confirmed? |
|------|---------------------------------------------|---------------------------|-----------|
| [MODULE]-TC-001 | 🔴 FAIL — `NoSuchBeanDefinitionException` hoặc `NullPointerException` | `☐ FAIL ☐ PASS` | `[ ] Yes` |
| [MODULE]-TC-003 | 🔴 FAIL — security test fails (no @PreAuthorize yet) | `☐ FAIL ☐ PASS` | `[ ] Yes` |

**Tất cả tests RED xác nhận?** `[ ] Yes — proceed to implement`

---

## 6. Entry / Exit Criteria

### Entry Criteria (Trước khi viết tests)

```
[ ] Package TDS Status = APPROVED
[ ] Test-Spec Status = IN_REVIEW (ít nhất)
[ ] V1 Schema mapping verified
[ ] Test data fixtures (FX-xxx) defined
[ ] No blocking Schema Gap
```

### Exit Criteria / Definition of Done (Sau khi implement)

```
[ ] Tất cả test cases trong §4 có status 🟢 Passing
[ ] ./mvnw test chạy không có failure
[ ] Coverage: service layer ≥ 80% (line coverage)
[ ] Authorization tests: 401/403 cases đều pass
[ ] Account disabled/locked regression test pass
[ ] AuditService.emit() được verify trong relevant tests
[ ] As-Built Reconciliation §19 trong TDS đã điền
[ ] Schema Gap register updated nếu phát hiện gap mới
```

---

## 7. Test Commands

```bash
# Chạy tất cả tests trong package
./mvnw test -pl 05_Development/CareBridgeAPI

# Chạy chỉ tests của module này
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=[ServiceImplTest],[ControllerTest]

# Chạy với coverage report (JaCoCo)
./mvnw test jacoco:report -pl 05_Development/CareBridgeAPI

# Chạy integration tests (nếu có Testcontainers)
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=[FeatureIntegrationTest]

# React/Web tests (nếu áp dụng)
# cd 05_Development/CareBridgeWebApp && npm run test

# Flutter tests (nếu áp dụng)
# cd 05_Development/CareBridgeMobileApp && flutter test
```

---

## 8. Rollback Plan

| Scenario | Action |
|----------|--------|
| Tests break existing passing tests | Revert change, investigate regression |
| Testcontainer test fails in CI | Check Docker available in CI runner; skip with `@Disabled` + comment |
| New test reveals V1 constraint violation | Document in Schema Gap register; do NOT silently fix |
| Test reveals logic bug in production code | Fix production code — do NOT change test to match wrong behavior |

---

## 9. CASE 2.0 Anti-Pattern Detection

> Review sau khi tests xanh. Đánh dấu `[x]` nếu đã verify không có anti-pattern.

### Safety Checklist

```
[ ] AP-AI-001: Không có test mà expectation đến từ AI assumption
           (mọi expected value phải từ BR, TDS, V1 schema)

[ ] AP-AI-002: Không có Green-from-Birth test
           (mọi test phải xác nhận RED trước khi implement — xem §5.1)

[ ] AP-AI-003: Không có Mock-only integration test
           (integration tests phải dùng real Testcontainers nếu test DB behavior)

[ ] AP-AI-004: Không có test verify mock thay vì behavior
           (test phải verify state/response — không phải chỉ verify method calls)

[ ] AP-AI-005: @WithMockUser username là valid UUID string
           (ví dụ: "00000000-0000-0000-0000-000000000001" — không phải "user" hay "moderator")

[ ] AP-AI-006: Test data không chứa PII thực

[ ] AP-AI-007: Mọi security test check cả 401 (unauthenticated) VÀ 403 (wrong role)
```

---

*CareBridge Test-Spec Template v1.0 — Adapted for JUnit 5 / Mockito / MockMvc / Spring Boot / Flutter*
