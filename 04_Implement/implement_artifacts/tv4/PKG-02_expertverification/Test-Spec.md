# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-02 ExpertVerification

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-EXPVERIFY-PKG-02-TDD` |
| **Version** | `1.0` |
| **Date** | 2026-07-03 |
| **Status** | `DRAFT` |
| **Package** | `PKG-02 — Expert Verification & Trust Status` |
| **Included UCs** | `UC-62, UC-63, UC-70, UC-71` |
| **Spec gốc** | `CB-EXPVERIFY-PKG-02-TDS` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 1 (2026-07-03 → 2026-07-10)` |
| **Milestone** | `Demo Gate A` |

> **Stack kiểm thử:** JUnit 5 · Mockito · MockMvc · Spring Boot Test
> **Test data:** SYNTHETIC

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-03 | Lâm | Khởi tạo Test-Spec |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-62, UC-63, UC-70, UC-71` |
| **Module** | `expertverification — ExpertCredentialServiceImpl + ExpertVerificationServiceImpl` |
| **Spec gốc** | `CB-EXPVERIFY-PKG-02-TDS` |
| **Priority** | 🔴 P0 |
| **Upstream Dependencies** | `PKG-01_expert`, TV1 auth |
| **Downstream Consumers** | TV3 (ExpertBadgeReadPort), PKG-03_expertavailability |

---

## 2. Logic Issues Resolved

| # | Spec gốc (thiếu) | V1 Reality | Fix |
|---|-------------------|-----------|-----|
| L1 | V1 has no `reviewed_by` / `reviewed_at` | V1 lines 802-815: only 9 columns | Migration adds both (nullable) |
| L2 | No FK on `expert_profile_id` | No FK constraint | App layer check via expertservice |
| L3 | `review_status` values: V1 = 'PENDING' only | Need UNDER_REVIEW, APPROVED, REJECTED, EXPIRED | Enum extends V1; PENDING is default |
| L4 | UC-71 cascade: suspend must also disable availability | Not in V1 | Service layer cascade in same transaction |

---

## 3. Test Design Specification

### TDS-01 — Scope

```
├── Service Unit Tests (Mockito)
│   ├── ExpertCredentialServiceImplTest
│   └── ExpertVerificationServiceImplTest
├── Controller Tests (MockMvc)
│   ├── ExpertCredentialControllerTest
│   └── ExpertVerificationControllerTest
└── Integration Tests — deferred (no complex FK cascading)
```

### TDS-02 — Test Basis

| Source | Items |
|--------|-------|
| `SRS §3.5.x.x` UC-62/63/70/71 | State transitions, owner checks |
| `ADR-VER-001` | Verification state machine |
| `ADR-VER-003` | Trust status cascade |
| `BR-VER-001/002/003` | RBAC, uniqueness |
| `V1__init_schema.sql` | Column types |

### TDS-03 — Test Conditions

| ID | Condition | Coverage | Test Cases |
|----|-----------|----------|-----------|
| TC-COND-01 | UC-62: EXPERT uploads document (happy) | CredentialService | TC-001 |
| TC-COND-02 | UC-70: Admin approves verification | VerificationService | TC-002 |
| TC-COND-03 | UC-70: Admin rejects verification | VerificationService | TC-003 |
| TC-COND-04 | UC-71: Admin suspends expert (cascade) | VerificationService | TC-004 |
| TC-COND-05 | UC-71: Admin reinstates expert | VerificationService | TC-005 |
| TC-COND-06 | UC-63: Expert views verification status | CredentialService | TC-006 |
| TC-COND-07 | Validation: blank credential type → 400 | Controller | TC-007 |
| TC-COND-08 | Auth: MOTHER cannot upload doc → 403 | Controller | TC-008 |
| TC-COND-09 | Auth: no token → 401 | Controller | TC-009 |
| TC-COND-10 | Business rule: upload while UNDER_REVIEW → 409 | CredentialService | TC-010 |
| TC-COND-11 | Business rule: wrong owner uploads → 403 | Controller | TC-011 |
| TC-COND-12 | Cascade: SUSPENDED expert not in directory | Cross-package | TC-012 |
| TC-COND-13 | Cascade: SUSPENDED expert cannot post answer | Cross-package | TC-013 |
| TC-COND-14 | Not found: review non-existent profile → 404 | Controller | TC-014 |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|-----------|-----------|
| State Transition Testing | verificationStatus, trustStatus, reviewStatus | FSM coverage |
| Equivalence Partitioning | credentialType (valid/invalid strings), decision (valid/invalid) | Input domain |
| Error Guessing | Null fileUrl, expired credential dates | Edge cases |

### TDS-05 — Test Data (Synthetic)

| ID | Type | Value | Purpose |
|----|------|-------|---------|
| FX-VER-001 | JWT | sub: `...0001`, role: `EXPERT` | Expert actions |
| FX-VER-002 | JWT | sub: `...0002`, role: `MOTHER` | Auth failure |
| FX-VER-003 | JWT | sub: `...0004`, role: `SYSTEM_ADMIN` | Admin actions |
| FX-VER-004 | Entity | `ExpertProfile(userId=0001, profileId=0010, verificationStatus=PENDING)` | Target profile |
| FX-VER-005 | Entity | `ExpertCredential(profileId=0010, reviewStatus=PENDING)` | Pending credential |
| FX-VER-006 | Entity | `ExpertCredential(profileId=0010, reviewStatus=UNDER_REVIEW)` | Blocked upload |
| FX-VER-007 | Input | `{decision: "APPROVED", reviewNote: "OK"}` | Admin review |
| FX-VER-008 | Input | `{action: "SUSPEND", reason: "..."}` | Trust action |

---

## 4. Test Case Specification

### Props Isolation

```java
private static final UUID TEST_EXPERT_USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID TEST_PROFILE_ID  = UUID.fromString("00000000-0000-0000-0000-000000000010");
private static final UUID TEST_CRED_ID     = UUID.fromString("00000000-0000-0000-0000-000000000020");
private static final UUID TEST_ADMIN_USER  = UUID.fromString("00000000-0000-0000-0000-000000000004");
```

---

### TC-VER-001 — UC-62: Upload Document (Happy Path)

**Severity:** `CRITICAL`
**Feature:** `ExpertCredentialServiceImpl.uploadDocument()`

```java
@Test
void uploadDocument_withValidInput_returnsCredentialResponse() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .expertProfileId(TEST_PROFILE_ID)
      .verificationStatus(VerificationStatus.PENDING));
  when(mockProfileRepo.findByUserId(TEST_EXPERT_USER))
      .thenReturn(Optional.of(profile));
  when(mockCredRepo.findTopByExpertProfileIdOrderByCreatedAtDesc(TEST_PROFILE_ID))
      .thenReturn(Optional.empty()); // no existing UNDER_REVIEW
  when(mockCredRepo.save(any())).thenAnswer(i -> i.getArgument(0));

  ExpertCredentialResponse result = service.uploadDocument(
      TEST_PROFILE_ID, TEST_EXPERT_USER, "LICENSE", "https://storage/...",
      CredentialUploadRequest.builder().credentialNumber("BS001").issuedDate(LocalDate.now().minusYears(5)).build());

  assertThat(result).isNotNull();
  assertThat(result.getReviewStatus()).isEqualTo(CredentialReviewStatus.PENDING);
  verify(mockCredRepo).save(any());
}
```

**Status:** 🔴 Not written

---

### TC-VER-002 — UC-70: Admin Approves Verification

**Severity:** `CRITICAL`
**Feature:** `ExpertVerificationServiceImpl.adminReview(APPROVED)`

```java
@Test
void adminReview_approved_setsVerifiedStatus() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .expertProfileId(TEST_PROFILE_ID)
      .verificationStatus(VerificationStatus.PENDING));
  when(mockProfileRepo.findById(TEST_PROFILE_ID)).thenReturn(Optional.of(profile));
  when(mockProfileRepo.save(any())).thenAnswer(i -> i.getArgument(0));

  AdminReviewResponse result = verificationService.adminReview(
      TEST_PROFILE_ID, AdminReviewRequest.builder()
          .decision(ReviewDecision.APPROVED).reviewNote("OK").build(), TEST_ADMIN_USER);

  assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
  assertThat(result.getVerifiedAt()).isNotNull();
}
```

**Status:** 🔴 Not written

---

### TC-VER-003 — UC-70: Admin Rejects Verification

**Severity:** `HIGH`
**Feature:** `ExpertVerificationServiceImpl.adminReview(REJECTED)`

```java
@Test
void adminReview_rejected_setsRejectedStatus() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .verificationStatus(VerificationStatus.PENDING));
  when(mockProfileRepo.findById(TEST_PROFILE_ID)).thenReturn(Optional.of(profile));
  when(mockProfileRepo.save(any())).thenAnswer(i -> i.getArgument(0));

  AdminReviewResponse result = verificationService.adminReview(
      TEST_PROFILE_ID, AdminReviewRequest.builder()
          .decision(ReviewDecision.REJECTED).reviewNote("Bằng hết hạn").build(), TEST_ADMIN_USER);

  assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
  assertThat(result.getReviewNote()).isEqualTo("Bằng hết hạn");
}
```

**Status:** 🔴 Not written

---

### TC-VER-004 — UC-71: Admin Suspends Expert (Cascade)

**Severity:** `CRITICAL`
**Feature:** `ExpertVerificationServiceImpl.updateTrustStatus(SUSPEND)`

```java
@Test
void updateTrustStatus_suspend_cascadesToAvailability() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .trustStatus(ExpertTrustStatus.ACTIVE));
  when(mockProfileRepo.findById(TEST_PROFILE_ID)).thenReturn(Optional.of(profile));
  when(mockProfileRepo.save(any())).thenAnswer(i -> i.getArgument(0));
  when(mockAvailRepo.markAllUnavailableByProfileId(TEST_PROFILE_ID)).thenReturn(3);

  ExpertTrustResponse result = verificationService.updateTrustStatus(
      TEST_PROFILE_ID, TrustAction.SUSPEND, TEST_ADMIN_USER, "Vi phạm");

  assertThat(result.getTrustStatus()).isEqualTo(ExpertTrustStatus.SUSPENDED);
  verify(mockAvailRepo).markAllUnavailableByProfileId(TEST_PROFILE_ID);
}
```

**Status:** 🔴 Not written

---

### TC-VER-005 — UC-71: Reinstates Expert

**Severity:** `HIGH`

```java
@Test
void updateTrustStatus_reinstate_setsActive() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .trustStatus(ExpertTrustStatus.SUSPENDED));
  when(mockProfileRepo.findById(TEST_PROFILE_ID)).thenReturn(Optional.of(profile));
  when(mockProfileRepo.save(any())).thenAnswer(i -> i.getArgument(0));

  ExpertTrustResponse result = verificationService.updateTrustStatus(
      TEST_PROFILE_ID, TrustAction.REINSTATE, TEST_ADMIN_USER, "Đã hết hạn");

  assertThat(result.getTrustStatus()).isEqualTo(ExpertTrustStatus.ACTIVE);
}
```

**Status:** 🔴 Not written

---

### TC-VER-006 — UC-63: Expert Views Verification Status

**Severity:** `HIGH`

```java
@Test
void getVerificationStatus_returnsFullStatus() {
  ExpertProfile profile = makeExpertProfile(b -> b
      .verificationStatus(VerificationStatus.UNDER_REVIEW)
      .verifiedAt(Instant.now()));
  ExpertCredential cred = ExpertCredential.builder()
      .credentialId(TEST_CRED_ID).expertProfileId(TEST_PROFILE_ID)
      .credentialType("LICENSE").reviewStatus(CredentialReviewStatus.UNDER_REVIEW)
      .issuedDate(LocalDate.now().minusYears(3))
      .expiryDate(LocalDate.now().plusYears(2))
      .build();
  when(mockProfileRepo.findByUserId(TEST_EXPERT_USER)).thenReturn(Optional.of(profile));
  when(mockCredRepo.findByExpertProfileId(TEST_PROFILE_ID)).thenReturn(List.of(cred));

  VerificationStatusResponse result = credentialService.getVerificationStatus(TEST_EXPERT_USER);

  assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.UNDER_REVIEW);
  assertThat(result.getCredentials()).hasSize(1);
  assertThat(result.getCredentials().get(0).getDaysUntilExpiry()).isGreaterThan(0);
}
```

**Status:** 🔴 Not written

---

### TC-VER-007 through TC-VER-014: Security + Validation

**TC-VER-007** — `@WithMockUser(roles = "EXPERT")`, blank credentialType → 400
**TC-VER-008** — `@WithMockUser(roles = "MOTHER")`, upload → 403, verifyNoInteractions(service)
**TC-VER-009** — no token → 401
**TC-VER-010** — existing credential UNDER_REVIEW → `CredentialAlreadyUnderReviewException` (409)
**TC-VER-011** — Expert with userId=0002 tries to upload for profileId=0010 (wrong owner) → 403
**TC-VER-012** — After SUSPEND, PKG-01 getDirectory() → expert excluded (cross-package)
**TC-VER-013** — After SUSPEND, ExpertBadgeReadPort.isVerified() → false (cross-package to TV3)
**TC-VER-014** — `PUT /admin/review` with non-existent profileId → 404

---

## 5. Red-Green-Refactor Tracker

| Test Case | 🔴 RED | 🟢 GREEN | 🔵 REFACTOR |
|-----------|:------:|:--------:|:-----------:|
| TC-VER-001..006 | `[ ]` | — | — |
| TC-VER-007..014 | `[ ]` | — | — |

### Red Gate

| Test | Expected RED | Confirmed? |
|------|-------------|-----------|
| TC-VER-001 | FAIL — no ExpertCredentialService bean | `[ ]` |
| TC-VER-002 | FAIL — no AdminReview logic → no status change | `[ ]` |

---

## 6. Entry / Exit Criteria

```markdown
Entry:
[ ] PKG-01 ExpertProfile entity + service exists
[ ] TV1 SecurityUtils.requireCurrentUserId works
[ ] V1 schema verified (2 new columns in migration)

Exit:
[ ] All 14 tests pass
[ ] Cascade trustStatus → availability verified
[ ] Audit events emitted in review/trust tests
```

---

## 7. Test Commands

```bash
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest=ExpertCredentialServiceImplTest,ExpertVerificationServiceImplTest,\
ExpertCredentialControllerTest,ExpertVerificationControllerTest
```

---

## 8. Rollback Plan

| Scenario | Action |
|----------|--------|
| Credential file URL returns 404 | Graceful display — show "document unavailable" |
| Admin rejects with empty reason | Allow — reviewNote is optional text |
| AuditEvent not emitted | Reject — must emit for compliance |

---

## 9. CASE 2.0 Anti-Pattern Detection

```markdown
[ ] AP-AI-001 through AP-AI-007: Standard checks
[ ] AP-VER-001: @PreAuthorize on all admin endpoints
[ ] AP-VER-002: Owner check via ProfileRepository.findByUserId() before credential ops
[ ] AP-VER-003: Audit event on every review/trust change
[ ] AP-VER-004: No hard DELETE on credentials
[ ] AP-VER-005: fileUrl stripped from non-admin DTOs
[ ] AP-VER-006: Cross-package cascade (SUSPEND → availability + directory) tested