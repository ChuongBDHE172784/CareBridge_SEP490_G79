# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-01 Expert

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-EXPERT-PKG-01-TDD` |
| **Version** | `1.0` |
| **Date** | 2026-07-03 |
| **Status** | `DRAFT` |
| **Package** | `PKG-01 — Expert Profile & Directory` |
| **Included UCs** | `UC-60, UC-61, UC-65, UC-66, UC-69` |
| **Spec gốc** | `CB-EXPERT-PKG-01-TDS` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 1 (2026-07-03 → 2026-07-10)` |
| **Milestone** | `Demo Gate A` |

> **Stack kiểm thử:** JUnit 5 · Mockito · MockMvc · Spring Boot Test · Vitest (React) · Flutter test
> **Test data:** SYNTHETIC — no PII

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-03 | Lâm | Khởi tạo Test-Spec cho PKG-01 |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-60, UC-61, UC-65, UC-66, UC-69` |
| **Module** | `expert — ExpertProfileServiceImpl` |
| **Spec gốc** | `CB-EXPERT-PKG-01-TDS` |
| **Priority** | 🔴 P0 |
| **Data Classification** | Internal |
| **Upstream Dependencies** | `PKG-TV1 (auth, SecurityUtils)` |
| **Downstream Consumers** | `PKG-expertverification`, PKG-expertavailability, TV3 (ExpertBadgeReadPort) |

### 1.1 AI Generation Context

| Field | Value |
|-------|-------|
| **AI Assisted?** | Yes |
| **Constraint Source** | `CB-EXPERT-PKG-01-TDS §20`, `ADR-EXP-001`, `ADR-EXP-002` |
| **Constraints Injected** | Role enforcement, owner extraction, uniqueness, audit |
| **Model** | `claude-opus-4-8[1m]` |
| **Trust Level** | `T2 → T3 (pending Red Gate)` |

---

## 2. Logic Issues Resolved

| # | Spec gốc (thiếu) | V1 Reality | Fix applied in test |
|---|-------------------|-----------|---------------------|
| L1 | V1 có `verification_status` nhưng không có `trust_status` | V1 lines 786-800: only 8 columns | Migration adds `trust_status` and `contribution_points`; entity has defaults |
| L2 | No FK constraint on `user_id` | V1 has no FK → users | Enforce at app layer via SecurityUtils.requireCurrentUserId |
| L3 | `contributionPoints` not in V1 | V1 doesn't track points | New column via migration; default 0 in entity |
| L4 | TDS says `rating_avg` is BigDecimal(3,2) | V1: `numeric` (unconstrained) | Map as BigDecimal; no precision enforcement in code |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
├── Service Unit Tests (Mockito — mock Repository, no DB)
│   └── ExpertProfileServiceImplTest
├── Controller Tests (MockMvc — mock Service, test auth + validation)
│   └── ExpertProfileControllerTest
├── Security Tests (MockMvc — test 401/403 scenarios)
│   └── Included in ControllerTest
└── Integration Tests (Testcontainers PostgreSQL)
    └── ExpertProfileIntegrationTest (small — verify unique constraint)
```

### TDS-02 — Test Basis

| Source | Items Derived |
|--------|--------------|
| `SRS §3.5.x.x` UC-60/61/65/66/69 | Happy path, state transitions |
| `CB-EXPERT-PKG-01-TDS ADR-EXP-001` | One profile per user; update gating |
| `CB-EXPERT-PKG-01-TDS ADR-EXP-002` | Directory: VERIFIED + ACTIVE only |
| `BR-EXP-001` | Unique profile per user |
| `BR-RBAC` | Role-based access |
| `V1__init_schema.sql` | Column types, defaults |

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Coverage | Test Cases |
|-------------|---------------|----------|-----------|
| TC-COND-01 | Happy path: create profile (first time) | UC-60 | TC-001 |
| TC-COND-02 | Happy path: update profile (PENDING state) | UC-61 | TC-002 |
| TC-COND-03 | Happy path: directory returns VERIFIED experts | UC-65 | TC-003 |
| TC-COND-04 | Happy path: public profile detail | UC-66 | TC-004 |
| TC-COND-05 | Happy path: contribution points | UC-69 | TC-005 |
| TC-COND-06 | Validation: blank specialty → 400 | @NotBlank | TC-006 |
| TC-COND-07 | Auth: wrong role → 403 | @PreAuthorize | TC-007 |
| TC-COND-08 | Auth: no token → 401 | Security filter | TC-008 |
| TC-COND-09 | Business rule: duplicate profile → 409 | BR-EXP-001 | TC-009 |
| TC-COND-10 | Business rule: update during UNDER_REVIEW → 403 | BR-EXP-001 | TC-010 |
| TC-COND-11 | Not found: get non-existent profile → 404 | Repository | TC-011 |
| TC-COND-12 | Directory: PENDING experts excluded | ADR-EXP-002 | TC-012 |
| TC-COND-13 | Account: disabled expert cannot create | Security filter | TC-013 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| Equivalence Partitioning | specialty (valid/invalid/blank), experienceYears (range 0-80) | Input domain |
| Boundary Value Analysis | experienceYears: -1, 0, 80, 81 | @Min/@Max boundaries |
| State Transition Testing | verificationStatus: PENDING → UNDER_REVIEW → VERIFIED | FSM: state gating on updates |
| Error Guessing | null userId injection, SQL injection in specialty | OWASP A01/A03 |

### TDS-05 — Test Data (Synthetic)

| Fixture ID | Type | Value | Purpose |
|-----------|------|-------|---------|
| FX-EXP-001 | JWT | sub: `00000000-0000-0000-0000-000000000001`, role: `EXPERT` | Happy path auth |
| FX-EXP-002 | JWT | sub: `00000000-0000-0000-0000-000000000002`, role: `MOTHER` | Auth failure |
| FX-EXP-003 | JWT | (no sub) | Unauthenticated |
| FX-EXP-004 | Entity seed | `ExpertProfile(userId=001, verificationStatus=PENDING, trustStatus=ACTIVE)` | Existing PENDING profile |
| FX-EXP-005 | Entity seed | `ExpertProfile(userId=001, verificationStatus=VERIFIED, trustStatus=ACTIVE)` | Existing VERIFIED profile |
| FX-EXP-006 | Entity seed | `ExpertProfile(userId=001, verificationStatus=UNDER_REVIEW, ...)` | Blocked update |
| FX-EXP-007 | Input | `{specialty: "Sản", experienceYears: 10}` | Valid create |
| FX-EXP-008 | Input | `{specialty: ""}` | Invalid — blank |

### TDS-06 — Integration Test Justification

**Testcontainers: NO** — No complex FK cascading, unique constraint tested at app layer via `existsByUserId()`. If DB-level unique test needed later, add with `@SpringBootTest`.

---

## 4. Test Case Specification

### Props Isolation Boilerplate (CASE 2.0)

```java
// ═══════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// ═══════════════════════════════════════════════════════════

private static final UUID TEST_EXPERT_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID TEST_EXPERT_PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
private static final UUID TEST_OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
private static final UUID TEST_ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

private ExpertProfile makeExpertProfile(Consumer<ExpertProfile.Builder> override) {
  ExpertProfile.Builder builder = ExpertProfile.builder()
      .expertProfileId(TEST_EXPERT_PROFILE_ID)
      .userId(TEST_EXPERT_USER_ID)
      .specialty("Sản - Phụ khoa")
      .professionalTitle("Bác sĩ CKII")
      .experienceYears((short) 10)
      .workplace("BV Từ Dũ")
      .verificationStatus(VerificationStatus.PENDING)
      .trustStatus(ExpertTrustStatus.ACTIVE)
      .contributionPoints(0);
  override.accept(builder);
  return builder.build();
}

private CreateExpertProfileRequest makeCreateRequest(Consumer<CreateExpertProfileRequest.Builder> override) {
  CreateExpertProfileRequest.Builder builder = CreateExpertProfileRequest.builder()
      .specialty("Sản - Phụ khoa")
      .professionalTitle("Bác sĩ CKII")
      .experienceYears((short) 10)
      .workplace("BV Từ Dũ")
      .consultationScope("Tư vấn thai kỳ");
  override.accept(builder);
  return builder.build();
}
```

---

### TC-EXP-001 — UC-60: Create Expert Profile (Happy Path)

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertProfileServiceImpl.createProfile()`
**Test File:** `src/test/java/com/carebridge/backend/expert/service/ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-01`

**Preconditions:**
- No existing profile for `TEST_EXPERT_USER_ID`

**Test Steps:**
```java
@Test
void createProfile_withValidInput_returnsProfileResponse() {
  CreateExpertProfileRequest request = makeCreateRequest(b -> {});
  when(mockRepository.existsByUserId(TEST_EXPERT_USER_ID)).thenReturn(false);
  when(mockRepository.save(any(ExpertProfile.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

  ExpertProfileResponse result = service.createProfile(TEST_EXPERT_USER_ID, request);

  assertThat(result).isNotNull();
  assertThat(result.getExpertProfileId()).isEqualTo(TEST_EXPERT_PROFILE_ID);
  assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
  assertThat(result.getContributionPoints()).isEqualTo(0);
  verify(mockRepository).save(any(ExpertProfile.class));
}
```

**Expected:**
- Returns `ExpertProfileResponse` with `verificationStatus=PENDING`
- `repository.save()` called once

**Status:** 🔴 Not written

---

### TC-EXP-002 — UC-61: Update Expert Profile (Happy Path, PENDING)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.updateProfile()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-02`

```java
@Test
void updateProfile_whenPending_updatesAllowedFields() {
  ExpertProfile existing = makeExpertProfile(b -> b
      .verificationStatus(VerificationStatus.PENDING));
  when(mockRepository.findByUserId(TEST_EXPERT_USER_ID))
      .thenReturn(Optional.of(existing));
  when(mockRepository.save(any())).thenAnswer(i -> i.getArgument(0));

  UpdateExpertProfileRequest request = UpdateExpertProfileRequest.builder()
      .specialty("Nhi khoa").build();

  ExpertProfileResponse result = service.updateProfile(TEST_EXPERT_USER_ID, request);

  assertThat(result.getSpecialty()).isEqualTo("Nhi khoa");
}
```

**Status:** 🔴 Not written

---

### TC-EXP-003 — UC-65: Directory Returns Only Verified (Happy Path)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.getDirectory()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-03`

```java
@Test
void getDirectory_returnsOnlyVerifiedAndActive() {
  ExpertProfile verified = makeExpertProfile(b -> b
      .expertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
      .verificationStatus(VerificationStatus.VERIFIED)
      .trustStatus(ExpertTrustStatus.ACTIVE));
  ExpertProfile pending = makeExpertProfile(b -> b
      .expertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
      .verificationStatus(VerificationStatus.PENDING)
      .trustStatus(ExpertTrustStatus.ACTIVE));

  when(mockRepository.findByVerificationStatusAndTrustStatus(
      VerificationStatus.VERIFIED, ExpertTrustStatus.ACTIVE))
      .thenReturn(List.of(verified));

  List<ExpertPublicProfileResponse> result = service.getDirectory(null);

  assertThat(result).hasSize(1);
  assertThat(result.get(0).getIsVerified()).isTrue();
}
```

**Status:** 🔴 Not written

---

### TC-EXP-004 — UC-66: Public Profile Detail (Happy Path)

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.getProfile()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-04`

```java
@Test
void getProfile_withValidId_returnsPublicProfile() {
  ExpertProfile profile = makeExpertProfile(b -> {});
  when(mockRepository.findById(TEST_EXPERT_PROFILE_ID))
      .thenReturn(Optional.of(profile));

  ExpertPublicProfileResponse result = service.getProfile(TEST_EXPERT_PROFILE_ID);

  assertThat(result).isNotNull();
  assertThat(result.getIsVerified()).isFalse(); // PENDING → false
  assertThat(result.getConsultationScope()).isNull(); // stripped
}
```

**Status:** 🔴 Not written

---

### TC-EXP-005 — UC-69: Contribution Points (Happy Path)

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileServiceImpl.getContribution()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-05`

```java
@Test
void getContribution_returnsPointsAndBadgeList() {
  ExpertProfile profile = makeExpertProfile(b -> b.contributionPoints(150));
  when(mockRepository.findByUserId(TEST_EXPERT_USER_ID))
      .thenReturn(Optional.of(profile));

  ExpertContributionResponse result = service.getContribution(TEST_EXPERT_USER_ID);

  assertThat(result.getContributionPoints()).isEqualTo(150);
}
```

**Status:** 🔴 Not written

---

### TC-EXP-006 — Validation: Blank Specialty → 400

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileController.createProfile()` via MockMvc
**Test File:** `src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java`
**Condition Ref:** `TC-COND-06`

```java
@Test
@WithMockUser(username = TEST_EXPERT_USER_ID_STR, roles = {"EXPERT"})
void createProfile_withBlankSpecialty_returns400() throws Exception {
  String requestJson = "{ \"specialty\": \"\", \"experienceYears\": 10 }";

  mockMvc.perform(post("/api/v1/experts/profile")
      .contentType(MediaType.APPLICATION_JSON)
      .content(requestJson))
    .andExpect(status().isBadRequest());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-007 — Security: Wrong Role → 403

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertProfileController` @PreAuthorize
**Test File:** `ExpertProfileControllerTest.java`
**Condition Ref:** `TC-COND-07`

```java
@Test
@WithMockUser(username = TEST_EXPERT_USER_ID_STR, roles = {"MOTHER"})
void createProfile_withWrongRole_returns403() throws Exception {
  String requestJson = "{ \"specialty\": \"Sản\" }";

  mockMvc.perform(post("/api/v1/experts/profile")
      .contentType(MediaType.APPLICATION_JSON)
      .content(requestJson))
    .andExpect(status().isForbidden());
  verify(mockExpertProfileService, never()).createProfile(any(), any());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-008 — Security: Unauthenticated → 401

**Severity:** `CRITICAL`
**Feature Under Test:** Security filter chain
**Test File:** `ExpertProfileControllerTest.java`
**Condition Ref:** `TC-COND-08`

```java
@Test
void createProfile_withoutToken_returns401() throws Exception {
  mockMvc.perform(post("/api/v1/experts/profile")
      .contentType(MediaType.APPLICATION_JSON)
      .content("{}"))
    .andExpect(status().isUnauthorized());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-009 — Business Rule: Duplicate Profile → 409

**Severity:** `CRITICAL`
**Feature Under Test:** `ExpertProfileServiceImpl.createProfile()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-09` | Oracle: `BR-EXP-001`

```java
@Test
void createProfile_whenAlreadyExists_throwsDuplicateException() {
  when(mockRepository.existsByUserId(TEST_EXPERT_USER_ID)).thenReturn(true);
  CreateExpertProfileRequest request = makeCreateRequest(b -> {});

  assertThatThrownBy(() -> service.createProfile(TEST_EXPERT_USER_ID, request))
      .isInstanceOf(ExpertProfileAlreadyExistsException.class);

  verify(mockRepository, never()).save(any());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-010 — Business Rule: Update During UNDER_REVIEW → 403

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.updateProfile()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-10` | Oracle: `ADR-EXP-001`

```java
@Test
void updateProfile_whenUnderReview_throwsForbiddenException() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .verificationStatus(VerificationStatus.UNDER_REVIEW));
  when(mockRepository.findByUserId(TEST_EXPERT_USER_ID))
      .thenReturn(Optional.of(profile));

  UpdateExpertProfileRequest request = UpdateExpertProfileRequest.builder()
      .specialty("Nhi khoa").build();

  assertThatThrownBy(() -> service.updateProfile(TEST_EXPERT_USER_ID, request))
      .isInstanceOf(ExpertProfileUpdateNotAllowedException.class);

  verify(mockRepository, never()).save(any());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-011 — Not Found: Get Non-existent Profile → 404

**Severity:** `HIGH`
**Feature Under Test:** `ExpertProfileServiceImpl.getProfile()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-11`

```java
@Test
void getProfile_withNonExistentId_throwsNotFoundException() {
  UUID nonExistentId = UUID.randomUUID();
  when(mockRepository.findById(nonExistentId)).thenReturn(Optional.empty());

  assertThatThrownBy(() -> service.getProfile(nonExistentId))
      .isInstanceOf(ExpertProfileNotFoundException.class);
}
```

**Status:** 🔴 Not written

---

### TC-EXP-012 — Directory: PENDING Experts Excluded

**Severity:** `MEDIUM`
**Feature Under Test:** `ExpertProfileServiceImpl.getDirectory()`
**Test File:** `ExpertProfileServiceImplTest.java`
**Condition Ref:** `TC-COND-12` | Oracle: `ADR-EXP-002`

```java
@Test
void getDirectory_excludesPendingAndSuspended() {
  ExpertProfile verified = makeExpertProfile(b -> b
      .expertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
      .verificationStatus(VerificationStatus.VERIFIED)
      .trustStatus(ExpertTrustStatus.ACTIVE));
  ExpertProfile suspended = makeExpertProfile(b -> b
      .expertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000011"))
      .verificationStatus(VerificationStatus.VERIFIED)
      .trustStatus(ExpertTrustStatus.SUSPENDED));
  ExpertProfile pending = makeExpertProfile(b -> b
      .expertProfileId(UUID.fromString("00000000-0000-0000-0000-000000000012"))
      .verificationStatus(VerificationStatus.PENDING)
      .trustStatus(ExpertTrustStatus.ACTIVE));

  when(mockRepository.findByVerificationStatusAndTrustStatus(
      VerificationStatus.VERIFIED, ExpertTrustStatus.ACTIVE))
      .thenReturn(List.of(verified));

  List<ExpertPublicProfileResponse> result = service.getDirectory(null);

  assertThat(result).hasSize(1);
  assertThat(result.get(0).getExpertProfileId()).isEqualTo(verified.getExpertProfileId());
}
```

**Status:** 🔴 Not written

---

### TC-EXP-013 — Security: Disabled Account → 401/403

**Severity:** `HIGH`
**Feature Under Test:** Security filter account state check
**Test File:** `ExpertProfileControllerTest.java`
**Condition Ref:** `TC-COND-13`

```java
@Test
@WithMockUser(username = TEST_EXPERT_USER_ID_STR, roles = {"EXPERT"})
void createProfile_whenAccountDisabled_returns401Or403() throws Exception {
  // Setup: user account_status = 'SUSPENDED'
  // Mock SecurityUtils or SecurityContext to reflect disabled state
  // Verify endpoint returns 401 or 403
}
```

**Status:** 🔴 Not written

---

## 5. Red-Green-Refactor Tracker

| Test Case | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-----------|:------:|:--------:|:-----------:|
| TC-EXP-001 | `[ ]` | — | — |
| TC-EXP-002 | `[ ]` | — | — |
| TC-EXP-003 | `[ ]` | — | — |
| TC-EXP-004 | `[ ]` | — | — |
| TC-EXP-005 | `[ ]` | — | — |
| TC-EXP-006 | `[ ]` | — | — |
| TC-EXP-007 | `[ ]` | — | — |
| TC-EXP-008 | `[ ]` | — | — |
| TC-EXP-009 | `[ ]` | — | — |
| TC-EXP-010 | `[ ]` | — | — |
| TC-EXP-011 | `[ ]` | — | — |
| TC-EXP-012 | `[ ]` | — | — |
| TC-EXP-013 | `[ ]` | — | — |

### 5.1 Red Gate Protocol

| Test | Expected RED | Confirmed? |
|------|-------------|-----------|
| TC-EXP-001 | FAIL — `NoSuchBeanDefinitionException` (service not registered) or `NullPointerException` | `[ ] Yes` |
| TC-EXP-007 | FAIL — no `hasRole('EXPERT')` yet → 200 instead of 403 | `[ ] Yes` |
| TC-EXP-009 | FAIL — no `existsByUserId()` check yet → duplicate inserts | `[ ] Yes` |

---

## 6. Entry / Exit Criteria

### Entry Criteria
```markdown
[ ] CB-EXPERT-PKG-01-TDS Status = DRAFT
[ ] This Test-Spec Status = DRAFT
[ ] V1 Schema mapping verified (§12 TDS — 15/15 fields match)
[ ] Test data fixtures (FX-EXP-001..008) defined
[ ] No blocking Schema Gap
```

### Exit Criteria (Definition of Done)
```markdown
[ ] All 13 test cases pass
[ ] ./mvnw test -Dtest=ExpertProfileServiceImplTest,ExpertProfileControllerTest passes
[ ] Service layer coverage ≥ 80%
[ ] Authorization tests (TC-007, TC-008, TC-013): all pass
[ ] AuditService.emit() verified in relevant tests
[ ] As-Built Reconciliation in TDS §19 filled
```

---

## 7. Test Commands

```bash
# Compile
./mvnw compile -pl 05_Development/CareBridgeAPI

# Run expert module tests only
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=ExpertProfileServiceImplTest,ExpertProfileControllerTest

# Run with coverage
./mvnw test jacoco:report -pl 05_Development/CareBridgeAPI \
  -Dtest=ExpertProfileServiceImplTest

# Run single test method
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=ExpertProfileServiceImplTest#createProfile_withValidInput_returnsProfileResponse

# React Web tests (after UI built)
# cd 05_Development/CareBridgeWebApp && npm run test -- --filter=expert

# Flutter tests (after mobile UI built)
# cd 05_Development/CareBridgeMobileApp && flutter test --name=expert
```

---

## 8. Rollback Plan

| Scenario | Action |
|----------|--------|
| Tests break existing passing tests | Revert change, investigate regression |
| Test reveals V1 constraint violation | Document in Schema Gap register; do NOT silently fix |
| Test reveals logic bug | Fix production code — do NOT change test to match wrong behavior |
| Migration `trust_status` column cause issue | Migration is additive; no rollback needed; remove column usage if needed |

---

## 9. CASE 2.0 Anti-Pattern Detection

```markdown
[ ] AP-AI-001: No AI-assumed expected values — all from BR, TDS, V1 schema
[ ] AP-AI-002: No Green-from-Birth — all tests RED confirmed before implement
[ ] AP-AI-003: No mock-only integration test for DB behavior
[ ] AP-AI-004: Tests verify state/response — not just method calls
[ ] AP-AI-005: @WithMockUser username is valid UUID string
[ ] AP-AI-006: No real PII in test data
[ ] AP-AI-007: Security tests check both 401 AND 403
[ ] AP-AI-008: No entity directly returned in controller response (DTO verified)
[ ] AP-AI-009: existsByUserId() check before create (uniqueness)
[ ] AP-AI-010: SecurityUtils.requireCurrentUserId() used — not request body userId
```
