# CB-EXPPROF-PKG-01-TestSpec — Expert Profile Tests

| Field | Value |
|-------|-------|
| **Document ID** | CB-EXPPROF-PKG-01-TestSpec |
| **Version** | 1.0 |
| **Date** | 2026-07-03 |
| **Status** | DRAFT |
| **Spec gốc** | CB-EXPPROF-PKG-01-TDS |
| **Package** | PKG-01 — Expert Profile |
| **Included UCs** | UC-60, UC-61, UC-65, UC-70 |
| **Author** | Lâm — TV4 |
| **Priority** | 🔴 P0 |

---

## CHANGELOG

| Date | Author | Content |
|------|--------|---------|
| 2026-07-03 | Lâm — TV4 | Create Test-Spec Sprint 0 |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | UC-60, UC-61, UC-65, UC-70 |
| **Module** | expert — ExpertProfileServiceImpl |
| **Spec gốc** | CB-EXPPROF-PKG-01-TDS |
| **Priority** | 🔴 P0 |
| **Sprint** | S0 (2026-07-03) |
| **Data Classification** | Internal |
| **Upstream Deps** | PKG-AUTH (User entity, SecurityUtils) |
| **Downstream Consumers** | PKG-02 (ExpertVerification), PKG-06 (NearbyCare) |

### AI Generation Context

| Field | Value |
|-------|-------|
| **AI Assisted?** | No |
| **Constraint Source** | CB-EXPPROF-PKG-01-TDS §8 |
| **Model** | claude-sonnet-4-6 |
| **Trust Level** | T2 → T3 (pending Red Gate) |

---

## 2. Logic Issues Resolved

| # | Spec gốc (sai/thiếu) | Thực tế (V1/code) | Fix applied |
|---|---------------------|-------------------|-------------|
| L1 | Rating avg có thể null | V1: `numeric` nullable — ✅ match | No fix needed |
| L2 | verified_by là nullable UUID | V1: `verified_by uuid` — nullable | Test must allow null verifiedBy |

---

## 3. Test Design Specification

### Test Scope

```
├── Service Unit Tests (Mockito — mock Repository)
├── Controller Tests (MockMvc — mock Service, test auth + validation)
├── Security Tests (401/403 via MockMvc)
└── Integration Tests (NO — no complex DB constraint; unique check is application-layer)
```

### Test Basis

| Source | Items Derived |
|--------|--------------|
| UC-60 | Expert creates profile — userId from JWT |
| UC-61 | Expert views/updates own profile |
| UC-65 | Public directory — only APPROVED |
| UC-70 | Admin approve/reject |
| BR-EXPPROF-001 | One profile per user (unique userId) |
| ADR-EXPPROF-003 | Owner extraction from SecurityUtils |

### Test Conditions

| Condition ID | Description | Coverage | Test Cases |
|-------------|-------------|---------|-----------|
| TC-COND-001 | Happy path: create profile | createProfile() | TC-001 |
| TC-COND-002 | Duplicate profile rejected | existsByUserId() | TC-002 |
| TC-COND-003 | Wrong role → 403 | @PreAuthorize | TC-003 |
| TC-COND-004 | Unauthenticated → 401 | Security config | TC-004 |
| TC-COND-005 | Profile not found | findById() | TC-005 |
| TC-COND-006 | Update own profile | updateProfile() | TC-006 |
| TC-COND-007 | Admin approves expert | approveExpert() | TC-007 |
| TC-COND-008 | Admin rejects expert | rejectExpert() | TC-008 |
| TC-COND-009 | Public directory only APPROVED | findVerifiedPublic() | TC-009 |
| TC-COND-010 | Non-admin cannot approve | @PreAuthorize | TC-010 |

### Test Data (Props Isolation)

```java
private static final UUID EXPERT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID ADMIN_USER = UUID.fromString("00000000-0000-0000-0000-000000000002");
private static final UUID PROFILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
```

---

## 4. Test Cases

### TC-001 — Create Profile (Happy Path)

**Severity:** CRITICAL
**Feature:** ExpertProfileServiceImpl.createProfile()
**Test File:** ExpertProfileServiceTest.java
**Condition:** TC-COND-001

```java
@Test
void createProfile_withValidInput_returnsProfileResponse() {
    CreateExpertProfileRequest request = makeRequest(b -> b
            .specialty("Sản khoa")
            .professionalTitle("Bác sĩ CK I"));
    when(repository.existsByUserId(EXPERT_USER)).thenReturn(false);
    ExpertProfile saved = makeProfile(p -> p.expertProfileId(PROFILE_ID).userId(EXPERT_USER));
    when(repository.save(any())).thenReturn(saved);

    ExpertProfileResponse result = service.createProfile(EXPERT_USER, request);

    assertThat(result).isNotNull();
    assertThat(result.expertProfileId()).isEqualTo(PROFILE_ID);
    assertThat(result.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
    verify(repository).save(any(ExpertProfile.class));
}
```

### TC-002 — Duplicate Profile Rejected

**Severity:** CRITICAL
**Oracle:** BR-EXPPROF-001 (one profile per user)
**Test File:** ExpertProfileServiceTest.java

```java
@Test
void createProfile_whenExists_throwsExpertException() {
    when(repository.existsByUserId(EXPERT_USER)).thenReturn(true);

    assertThatThrownBy(() -> service.createProfile(EXPERT_USER, makeRequest(b -> {})))
            .isInstanceOf(ExpertException.class)
            .satisfies(e -> assertThat(((ExpertException) e).getCode()).isEqualTo("EXPERT-001"));

    verify(repository, never()).save(any());
}
```

### TC-003 — Wrong Role → 403 on CREATE

**Severity:** CRITICAL
**Oracle:** RBAC role mapping, @PreAuthorize C1
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"MOTHER"})
void createProfile_withMotherRole_returns403() throws Exception {
    mockMvc.perform(post("/api/v1/expert/profiles")
            .contentType(APPLICATION_JSON)
            .content("{\"specialty\":\"test\"}"))
            .andExpect(status().isForbidden());
    verify(service, never()).createProfile(any(), any());
}
```

### TC-004 — Unauthenticated → 401

**Oracle:** adr-auth-001
**Test File:** ExpertProfileControllerTest.java

```java
@Test
void createProfile_withoutToken_returns401() throws Exception {
    mockMvc.perform(post("/api/v1/expert/profiles")
            .contentType(APPLICATION_JSON)
            .content("{\"specialty\":\"test\"}"))
            .andExpect(status().isUnauthorized());
}
```

### TC-005 — Get Profile Not Found → 404

**Oracle:** BR-EXPPROF-002
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser
void getMyProfile_whenNoProfile_returns404() throws Exception {
    when(service.getMyProfile(EXPERT_USER)).thenThrow(new ExpertException(NOT_FOUND, "EXPERT-002", "Not found"));

    mockMvc.perform(get("/api/v1/expert/profiles/me"))
            .andExpect(status().isNotFound());
}
```

### TC-006 — Update Own Profile

**Oracle:** UC-61
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"EXPERT"})
void updateProfile_withValidData_returns200() throws Exception {
    when(service.updateProfile(any(), any())).thenReturn(makeDetailResponse(p -> p
            .expertProfileId(PROFILE_ID)
            .specialty("Nhi khoa")));

    mockMvc.perform(patch("/api/v1/expert/profiles/me")
            .contentType(APPLICATION_JSON)
            .content("{\"specialty\":\"Nhi khoa\"}"))
            .andExpect(status().isOk());
}
```

### TC-007 — Admin Approves Expert

**Severity:** HIGH
**Oracle:** UC-70
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000002", roles = {"SYSTEM_ADMIN"})
void approveExpert_withAdminRole_returns200() throws Exception {
    doNothing().when(service).approveExpert(PROFILE_ID, ADMIN_USER);

    mockMvc.perform(post("/api/v1/expert/profiles/{id}/approve", PROFILE_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Expert approved"));
}
```

### TC-008 — Non-Admin Cannot Approve

**Oracle:** RBAC matrix §8
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser(username = "00000000-0000-0000-0000-000000000001", roles = {"EXPERT"})
void approveExpert_withExpertRole_returns403() throws Exception {
    mockMvc.perform(post("/api/v1/expert/profiles/{id}/approve", PROFILE_ID))
            .andExpect(status().isForbidden());
    verify(service, never()).approveExpert(any(), any());
}
```

### TC-009 — Directory Returns Only APPROVED

**Oracle:** ADR-EXPPROF-002, UC-65
**Test File:** ExpertProfileServiceTest.java

```java
@Test
void getPublicDirectory_returnsOnlyApprovedExperts() {
    ExpertProfile approved = makeProfile(p -> p.verificationStatus(VerificationStatus.APPROVED));
    ExpertProfile pending = makeProfile(p -> p.verificationStatus(VerificationStatus.PENDING));
    when(repository.findVerifiedPublic()).thenReturn(List.of(approved, pending));

    ExpertDirectoryResponse result = service.getPublicDirectory(null, 0, 10);

    assertThat(result.experts()).hasSize(1);
    assertThat(result.experts().get(0).expertProfileId()).isEqualTo(approved.getExpertProfileId());
}
```

### TC-010 — Public View of UNAPPROVED Expert → 404

**Oracle:** ADR-EXPPROF-002
**Test File:** ExpertProfileControllerTest.java

```java
@Test
@WithMockUser
void getPublicProfile_whenNotApproved_returns404() throws Exception {
    ExpertProfile pending = makeProfile(p -> p.verificationStatus(VerificationStatus.PENDING));
    when(service.getPublicProfile(PROFILE_ID)).thenThrow(
            new ExpertException(NOT_FOUND, "EXPERT-004", "Expert profile not available"));

    mockMvc.perform(get("/api/v1/expert/profiles/{id}", PROFILE_ID))
            .andExpect(status().isNotFound());
}
```

---

## 5. Red-Green-Refactor Tracker

| TC | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|----|--------|----------|-------------|
| TC-001 | ☐ | — | — |
| TC-002 | ☐ | — | — |
| TC-003 | ☐ | — | — |
| TC-004 | ☐ | — | — |
| TC-005 | ☐ | — | — |
| TC-006 | ☐ | — | — |
| TC-007 | ☐ | — | — |
| TC-008 | ☐ | — | — |
| TC-009 | ☐ | — | — |
| TC-010 | ☐ | — | — |

---

## 6. Entry/Exit Criteria

### Entry
- [ ] CB-EXPPROF-PKG-01-TDS Status = DRAFT (reviewed)
- [ ] V1 tables verified (§2)
- [ ] No blocking schema gap

### Exit
- [ ] All 10 test cases pass
- [ ] `./mvnw test -Dtest=ExpertProfile*` green
- [ ] Authorization tests pass (TC-003, TC-004, TC-008)
- [ ] As-Built Reconciliation filled

---

## 7. Rollback Plan

| Scenario | Action |
|----------|--------|
| Test breaks existing passing test | Revert, investigate |
| New test reveals V1 constraint | Document in SCHEMA_GAP_REGISTER |
| Test reveals logic bug | Fix production code |

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Check | Verified? |
|-------|-------|-----------|
| AP-001 | No test expects AI assumption — all from BR/TDS/V1 | ☐ |
| AP-002 | All tests confirm RED before implement | ☐ |
| AP-003 | No mock-only integration test (none needed) | ☐ |
| AP-004 | Tests verify state/response, not method calls only | ☐ |
| AP-005 | @WithMockUser usernames are valid UUIDs | ☐ |
| AP-006 | No real PII in test data | ☐ |
| AP-007 | Security tests check BOTH 401 AND 403 | ☐ |

---

## 9. Test Commands

```bash
# Run ExpertProfile tests only
cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=ExpertProfile*

# Run all expert package tests
./mvnw test -Dtest="com.carebridge.backend.expert.**"

# Full backend test suite
./mvnw test
```

---

*CareBridge Test-Spec v1.0 — Adapted for JUnit 5 / Mockito / MockMvc*
