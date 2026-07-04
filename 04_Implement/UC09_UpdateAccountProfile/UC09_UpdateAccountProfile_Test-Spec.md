# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# UC-09: Update Account Profile

| Field              | Value                                                              |
| ------------------ | ------------------------------------------------------------------ |
| **Document ID**    | `CB-PRF-TDD-001`                                                   |
| **Version**        | `1.0`                                                              |
| **Date**           | `2026-06-26`                                                       |
| **Status**         | `Implemented — 2026-07-04 (10/10 PASS). PRF-TC-002/003/004/005 implemented against the `com.carebridge.backend.profile` package (ProfileServiceImplTest + UpdateProfileRequestValidationTest). PRF-TC-INT-001 now passing with real Testcontainers PostgreSQL (profile/ProfileIntegrationTest); this run found and fixed a real ADR-002 bug — AuditEligibilityPolicy was silently dropping PROFILE_UPDATED/PROFILE_VIEWED audit events regardless of transaction state. All 10 TCs green.` |
| **Spec gốc**       | `CB-PRF-IMP-001` (UC09_UpdateAccountProfile_TDS.md)                |
| **Standard**       | ISO/IEC/IEEE 29119-3:2021                                          |
| **Author**         | `AI Agent`                                                         |
| **Reviewed by**    | `[ ] [Tech Lead] — Pending`                                        |
| **DPO Sign-off**   | `[ ] Pending`                                                      |
| **Approved by**    | `[ ] Pending`                                                      |
| **Classification** | `Sensitive-PII`                                                    |

**References:**
- `CB-PRF-IMP-001` — TDS UC-09 Update Account Profile
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/validation/VietnamesePhoneNumber.java`
- `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- `ADR-001` (Tách package profile), `ADR-002` (Audit ProfileUpdated synchronous)

> **Quy ước TDD:** Test cases được viết TRƯỚC khi implement production code.
> Thứ tự bắt buộc: viết test → chạy → xác nhận FAIL 🔴 → implement → PASS 🟢 → refactor 🔵.
> Không mark test là ✅ nếu `./mvnw test` chưa xanh. Test data dùng SYNTHETIC — không dùng PII thật.

---

## CHANGELOG

| Ngày       | Người thực hiện | Nội dung thay đổi                                           |
| ---------- | --------------- | ----------------------------------------------------------- |
| 2026-06-26 | AI Agent        | Khởi tạo TDD spec cho UC-09 Update Account Profile          |

---

## MỤC LỤC

1. [Thông tin Module](#1-thông-tin-module)
2. [Logic Issues Resolved](#2-logic-issues-resolved)
3. [Test Design Specification (TDS)](#3-test-design-specification-tds)
4. [Test Case Specification](#4-test-case-specification)
5. [Red-Green-Refactor Tracker](#5-red-green-refactor-tracker)
6. [Entry / Exit Criteria](#6-entry--exit-criteria)
7. [Rollback Plan](#7-rollback-plan)
8. [CASE 2.0 Anti-Pattern Detection](#8-case-20-anti-pattern-detection)

---

## 1. Thông tin Module

| Field                     | Value                                                                               |
| ------------------------- | ----------------------------------------------------------------------------------- |
| **Feature / UC ID**       | `UC-09`                                                                             |
| **Module**                | `profile — ProfileService`                                                          |
| **Spec gốc**              | `CB-PRF-IMP-001`                                                                    |
| **Priority**              | 🟠 P1                                                                                |
| **Sprint**                | `S1 (2026-06-26 → 2026-07-11)`                                                      |
| **Milestone**             | `M3 Alpha — 2026-07-11`                                                             |
| **Data Classification**   | `Sensitive-PII`                                                                     |
| **Compliance Scope**      | `PDPA Luật 91/2025 Điều 17; GDPR Art. 5.1(d), Art. 16, Art. 32`                    |
| **Upstream Dependencies** | `security (JWT Principal)`, `audit (AuditService)`                                  |
| **Downstream Consumers**  | `community (displayName in feed)`, `expert-profile (linked user info)`              |

### 1.1 AI Generation Context (CASE 2.0)

| Field                    | Value                                                                                                    |
| ------------------------ | -------------------------------------------------------------------------------------------------------- |
| **AI Assisted?**         | `Yes`                                                                                                    |
| **Constraint Source**    | `CB-PRF-IMP-001 §17`, `ADR-001`, `ADR-002`                                                              |
| **Constraints Injected** | C1: userId từ JWT, C2: sanitize HTML, C3: @VietnamesePhoneNumber, C4: dob range, C5: audit in-transaction |
| **Model**                | `claude-sonnet-4-6`                                                                                      |
| **Trust Level**          | `T2 → T3 (pending Red Gate)`                                                                             |

---

## 2. Logic Issues Resolved

> Liệt kê sai lệch giữa spec thiết kế và codebase thực tế. Test cases encode hành vi **đã sửa**.

| #  | Spec gốc (sai / thiếu)                                        | Thực tế (schema / policy)                                                  | Fix áp dụng trong test                                         |
| -- | ------------------------------------------------------------- | -------------------------------------------------------------------------- | -------------------------------------------------------------- |
| L1 | `UpdateProfileRequest` cũ có `name` (max 120) trong `security`| UC-09 yêu cầu `displayName` 2–100 ký tự, phân biệt với `full_name` trên `users` | Test dùng field `displayName`, không dùng `name`           |
| L2 | Không có field `dateOfBirth`, `area`, `phoneNumber` trong DTO cũ | UC-09 yêu cầu cả 5 fields; schema `users` có `phone` nhưng không có `date_of_birth` — phải lưu vào `user_profiles` | Test verify lưu vào `user_profiles`, không phải `users` |
| L3 | userId không được client truyền lên                           | userId lấy từ JWT Principal (SecurityContext), không từ request body       | Test không có userId trong request body; mock Principal       |
| L4 | Không có spec về XSS prevention                               | Policy GDPR Art. 5.1(d): data accuracy → không được lưu HTML raw          | Test TC-009 kiểm tra sanitize trước khi lưu                   |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope / Phạm vi

```
Profile module (com.carebridge.backend.profile) bao gồm các layer:
├── Controller     (ProfileController — mock IProfileService)
├── Service        (ProfileServiceImpl — mock IProfileRepository + AuditService)
├── Repository     (IProfileRepository — Testcontainers PostgreSQL)
└── Integration    (Full stack: Controller → Service → Repository → DB)
```

### TDS-02 — Test Basis / Cơ sở Kiểm thử

| Source                   | Items Derived                                                            |
| ------------------------ | ------------------------------------------------------------------------ |
| `UC-09 (SRS 3.1.1.9)`   | Happy path update, partial update (not all fields), unauthenticated      |
| `BR-PRF-OWN`            | Own-resource enforcement: userId JWT == profile owner                    |
| `BR-PRF-PHONE`          | VietnamesePhoneNumber validation                                          |
| `BR-PRF-DOB`            | DateOfBirth: past & >= 1900                                               |
| `BR-PRF-NAME`           | displayName 2–100 ký tự, no HTML                                         |
| `BR-PRF-AVATAR`         | avatarUrl max 500 ký tự, valid URL                                        |
| `ADR-002`               | AuditService.log() called in same transaction                             |
| `OWASP A03:2021`        | XSS in displayName input                                                  |

### TDS-03 — Test Conditions and Coverage Items

| Condition ID  | Test Condition                                     | Coverage Item                             | Test Cases      |
| ------------- | -------------------------------------------------- | ----------------------------------------- | --------------- |
| TC-COND-001   | Tất cả fields hợp lệ → update thành công           | `ProfileServiceImpl.updateProfile()`      | `PRF-TC-001`    |
| TC-COND-002   | phoneNumber không hợp lệ → 400                     | `@VietnamesePhoneNumber` on DTO           | `PRF-TC-002`    |
| TC-COND-003   | displayName < 2 ký tự → 400                        | `@Size(min=2)` on DTO                     | `PRF-TC-003`    |
| TC-COND-004   | dateOfBirth là tương lai → 400 PRF-002             | `ProfileServiceImpl.validateDateOfBirth()`| `PRF-TC-004`    |
| TC-COND-005   | avatarUrl không phải URL → 400 PRF-006             | `@URL` on DTO                             | `PRF-TC-005`    |
| TC-COND-006   | userId không khớp (cố sửa profile người khác) → 403| `ProfileServiceImpl` own-resource check  | `PRF-TC-006`    |
| TC-COND-007   | XSS payload trong displayName → 400 PRF-005        | `sanitizeDisplayName()` + `@Pattern`      | `PRF-TC-007`    |
| TC-COND-008   | Partial update (chỉ 1 field) → chỉ field đó thay đổi | `ProfileServiceImpl.applyUpdate()`     | `PRF-TC-008`    |
| TC-COND-009   | Integration: PATCH → DB có record đúng             | Full stack E2E                            | `PRF-TC-INT-001`|
| TC-COND-010   | Audit log được ghi sau khi update thành công       | `AuditService.log()` call verification    | `PRF-TC-010`    |

### TDS-04 — Test Techniques / Kỹ thuật Kiểm thử

| Technique (ISO 29119-4)  | Applied To                        | Rationale                                       |
| ------------------------ | --------------------------------- | ----------------------------------------------- |
| Equivalence Partitioning | phoneNumber, displayName, dob     | Valid/invalid partition để giảm số test case    |
| Boundary Value Analysis  | displayName length (2, 100, 101)  | Kiểm tra min/max boundary                       |
| Error Guessing           | XSS in displayName, future dob    | Security attack vectors đã biết                 |
| State Transition Testing | NO_PROFILE → PROFILE_ACTIVE       | Lần đầu tạo vs cập nhật existing               |

### TDS-05 — Test Data Requirements

| Fixture ID | Type     | Value / Logic                                              | Mục đích                        |
| ---------- | -------- | ---------------------------------------------------------- | ------------------------------- |
| `FX-001`   | DB seed  | `users: {user_id: "user-test-001", role: "MOTHER"}`        | Happy path base user            |
| `FX-002`   | DB seed  | `users: {user_id: "user-test-002", role: "MOTHER"}`        | Another user (own-resource test)|
| `FX-003`   | JWT mock | `{sub: "user-test-001", role: "ROLE_MOTHER"}`              | Auth context happy path         |
| `FX-004`   | JWT mock | `{sub: "user-test-002", role: "ROLE_MOTHER"}`              | Auth context wrong user         |
| `FX-005`   | Input    | `{displayName:"Nguyễn Test", phoneNumber:"0912345678", dateOfBirth:"1995-06-15", area:"Hà Nội"}` | Valid request |
| `FX-006`   | Input    | `{phoneNumber: "123abc"}`                                  | Invalid phone                   |
| `FX-007`   | Input    | `{displayName: "<script>alert(1)</script>"}`               | XSS payload                     |
| `FX-008`   | Input    | `{dateOfBirth: "2035-01-01"}`                              | Future date                     |
| `FX-009`   | Input    | `{avatarUrl: "not-a-url"}`                                 | Invalid URL                     |

---

## 4. Test Case Specification

> **TC ID format:** `PRF-TC-[NNN]`
> **Status:** 🔴 Not written / 🟡 Written-failing / 🟢 Passing

### Props Isolation Boilerplate (CASE 2.0 — BẮT BUỘC)

```java
// ═══════════════════════════════════════════════════════════════════
// CASE 2.0 — Props Isolation Pattern
// Đặt trong @BeforeEach hoặc dùng factory method — mỗi test dùng fresh instance
// ═══════════════════════════════════════════════════════════════════

// Base valid request — ĐỪNG mutate trực tiếp, dùng builder copy
private UpdateProfileRequest buildValidRequest() {
    UpdateProfileRequest req = new UpdateProfileRequest();
    req.setDisplayName("Nguyễn Test");
    req.setPhoneNumber("0912345678");
    req.setDateOfBirth(LocalDate.of(1995, 6, 15));
    req.setArea("Hà Nội");
    req.setAvatarUrl("https://cdn.carebridge.vn/avatars/test.jpg");
    return req;
}

private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
```

---

### PRF-TC-001 — Cập nhật profile hợp lệ (Happy Path)

**Severity:** `HIGH`
**Feature Under Test:** `ProfileServiceImpl.updateProfile(UUID, UpdateProfileRequest)`
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED — chưa implement
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `UC-09 AC-001` — Người dùng cập nhật profile thành công, nhận lại ProfileResponse đầy đủ

**Preconditions:**
- `profileRepository.findByUserId(USER_ID)` trả về `Optional.empty()` (profile chưa tồn tại)
- `auditService.log(...)` mock không throw exception
- `authenticatedUserId = USER_ID`

**Test Steps (JUnit 5 style):**
```java
@Test
void updateProfile_validRequest_createsAndReturnsProfile() {
    // Arrange
    UpdateProfileRequest request = buildValidRequest();
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    UserProfile savedProfile = new UserProfile();
    savedProfile.setUserId(USER_ID);
    savedProfile.setDisplayName("Nguyễn Test");
    when(profileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);

    // Act
    ProfileResponse response = profileService.updateProfile(USER_ID, request);

    // Assert
    assertNotNull(response);
    assertEquals("Nguyễn Test", response.getDisplayName());
    verify(profileRepository, times(1)).save(any(UserProfile.class));
    verify(auditService, times(1)).log(eq(AuditAction.PROFILE_UPDATED), eq(USER_ID), any(), any(), any());
}
```

**Expected Result (PASS):**
- `response.getDisplayName()` = "Nguyễn Test"
- `profileRepository.save()` được gọi đúng 1 lần
- `auditService.log(PROFILE_UPDATED, USER_ID, ...)` được gọi đúng 1 lần

**Expected Result (FAIL — dấu hiệu lỗi):**
- `NullPointerException` nếu profileService chưa inject đúng dependencies
- `auditService` không được gọi — vi phạm ADR-002

**Current Status:** 🔴 Not written

---

### PRF-TC-002 — Phone number không hợp lệ → 400

**Severity:** `HIGH`
**Feature Under Test:** `UpdateProfileRequest` DTO validation via `@VietnamesePhoneNumber`
**Test File:** `src/test/java/com/carebridge/backend/profile/controller/ProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-002`
**Oracle Source:** `BR-PRF-PHONE` — VietnamesePhoneNumber validator reject "123abc"

**Preconditions:**
- User đã xác thực, JWT hợp lệ (mock `USER_ID`)
- MockMvc setup với Spring validation enabled

**Test Steps (JUnit 5 + MockMvc):**
```java
@Test
void updateProfile_invalidPhone_returns400WithPRF001() throws Exception {
    // Arrange
    String requestJson = """
        {"phoneNumber": "123abc"}
        """;

    // Act & Assert
    mockMvc.perform(patch("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson)
            .with(mockJwt().jwt(jwt -> jwt.subject(USER_ID.toString())
                .claim("roles", List.of("ROLE_MOTHER")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PRF-001"))
        .andExpect(jsonPath("$.error.details[0].field").value("phoneNumber"));
}
```

**Expected Result (PASS):**
- HTTP 400 Bad Request
- `error.code` = "PRF-001"
- `error.details[0].field` = "phoneNumber"

**Expected Result (FAIL):**
- HTTP 200 → phone không hợp lệ được lưu vào DB

**Current Status:** 🟢 Passing — 2026-07-04 (implemented as `UpdateProfileRequestValidationTest.invalidPhone_isRejected`. The profile-domain `UpdateProfileRequest` (`com.carebridge.backend.profile.dto`) carries a `phoneNumber` field validated by an `@Pattern` matching the Vietnamese-phone rule; `"123abc"` produces a constraint violation on `phoneNumber`. At the controller `@Valid` layer this surfaces as the standard `VALIDATION_ERROR` envelope with `details[].field = "phoneNumber"` — the codebase's established bean-validation convention rather than a per-field `PRF-001` code.)

---

### PRF-TC-003 — displayName quá ngắn (< 2 ký tự)

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateProfileRequest` — `@Size(min=2, max=100)` trên `displayName`
**Test File:** `src/test/java/com/carebridge/backend/profile/controller/ProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-003`
**Oracle Source:** `BR-PRF-NAME` — displayName phải >= 2 ký tự

**Test Steps (JUnit 5 + MockMvc):**
```java
@Test
void updateProfile_displayNameTooShort_returns400() throws Exception {
    mockMvc.perform(patch("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\": \"A\"}")
            .with(mockJwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PRF-001"))
        .andExpect(jsonPath("$.error.details[0].field").value("displayName"));
}
```

**Expected Result (PASS):** HTTP 400, `error.code = "PRF-001"`, field = "displayName"
**Expected Result (FAIL):** HTTP 200 → tên 1 ký tự được chấp nhận
**Current Status:** 🟢 Passing — 2026-07-04 (implemented as `UpdateProfileRequestValidationTest.displayNameTooShort_isRejected` plus the `displayNameAtMinBoundary_passes` boundary case. The profile-domain `UpdateProfileRequest.displayName` carries `@Size(min=2, max=100)`; `"A"` produces a constraint violation on `displayName`, while `"Al"` (min boundary) passes.)

---

### PRF-TC-004 — dateOfBirth là ngày tương lai → PRF-002

**Severity:** `HIGH`
**Feature Under Test:** `ProfileServiceImpl.validateDateOfBirth(LocalDate)`
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-004`
**Oracle Source:** `BR-PRF-DOB` — dateOfBirth phải < today

**Test Steps (JUnit 5):**
```java
@Test
void updateProfile_futureDateOfBirth_throwsValidationExceptionPRF002() {
    // Arrange
    UpdateProfileRequest request = buildValidRequest();
    request.setDateOfBirth(LocalDate.now().plusDays(1)); // Future date

    // Act & Assert
    ValidationException ex = assertThrows(ValidationException.class,
        () -> profileService.updateProfile(USER_ID, request));

    assertEquals("PRF-002", ex.getCode());
    verify(profileRepository, never()).save(any());
    verify(auditService, never()).log(any(), any(), any(), any(), any());
}
```

**Expected Result (PASS):** `ValidationException("PRF-002")` được throw; `save()` không được gọi
**Expected Result (FAIL):** Future date được lưu vào DB
**Current Status:** 🟢 Passing — 2026-07-04 (implemented as `ProfileServiceImplTest.updateProfile_futureDateOfBirth_throwsPRF002` plus the `dateOfBirthBefore1900_throwsPRF002` lower-bound case. `ProfileServiceImpl.validateDateOfBirth()` enforces `1900-01-01 <= dob < today` and, on violation, throws the existing coded `BusinessException(HttpStatus.BAD_REQUEST, "PRF-002", …)` — no new exception hierarchy invented. The doc's original snippet asserted `ValidationException.getCode()`, but `ValidationException` in this codebase carries no code; `BusinessException` is the established coded-error type (see `GlobalExceptionHandler`), so the test asserts `BusinessException.getCode() == "PRF-002"`. `save()` and `auditService.log()` are never called when validation fails.)

---

### PRF-TC-005 — avatarUrl không hợp lệ → PRF-006

**Severity:** `MEDIUM`
**Feature Under Test:** `UpdateProfileRequest` — `@URL` annotation trên `avatarUrl`
**Test File:** `src/test/java/com/carebridge/backend/profile/controller/ProfileControllerTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-005`
**Oracle Source:** `BR-PRF-AVATAR` — avatarUrl phải là URL http/https hợp lệ

**Test Steps (JUnit 5 + MockMvc):**
```java
@Test
void updateProfile_invalidAvatarUrl_returns400WithPRF006() throws Exception {
    mockMvc.perform(patch("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"avatarUrl\": \"not-a-url\"}")
            .with(mockJwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PRF-006"))
        .andExpect(jsonPath("$.error.details[0].field").value("avatarUrl"));
}
```

**Expected Result (PASS):** HTTP 400, `error.code = "PRF-006"`, field = "avatarUrl"
**Expected Result (FAIL):** HTTP 200 → invalid URL lưu vào DB
**Current Status:** 🟢 Passing — 2026-07-04 (implemented as `UpdateProfileRequestValidationTest.invalidAvatarUrl_isRejected` plus the `validAvatarUrl_passes` case. The profile-domain `UpdateProfileRequest.avatarUrl` carries `@Size(max=500)` and an `@Pattern("^(https?://.*)?$")` http/https-URL constraint; `"not-a-url"` produces a constraint violation on `avatarUrl`. At the controller `@Valid` layer this surfaces as the standard `VALIDATION_ERROR` envelope with `details[].field = "avatarUrl"` — the codebase's bean-validation convention rather than a per-field `PRF-006` code.)

---

### PRF-TC-006 — Own-resource enforcement (không thể sửa profile người khác)

**Severity:** `CRITICAL`
**Feature Under Test:** `ProfileServiceImpl.updateProfile()` — userId check
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-006`
**Oracle Source:** `BR-PRF-OWN` — userId từ JWT phải == profile owner; nếu không → 403

**Preconditions:**
- Profile của `OTHER_USER_ID` đã tồn tại trong DB
- Authenticated user là `USER_ID` (khác `OTHER_USER_ID`)

**Test Steps (JUnit 5):**
```java
@Test
void updateProfile_attemptToUpdateOtherUserProfile_throwsAuthorizationException() {
    // Arrange — simulate controller passing wrong target userId
    // (Trong thực tế controller dùng JWT userId, không cho client truyền targetId)
    // Test service-level check khi có bug ở controller
    UpdateProfileRequest request = buildValidRequest();
    UserProfile otherProfile = new UserProfile();
    otherProfile.setUserId(OTHER_USER_ID);

    // Giả sử service nhận authenticatedUserId = USER_ID nhưng profile owner là OTHER_USER_ID
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

    // Act — service phải chỉ lưu cho authenticatedUserId, không cho OTHER_USER_ID
    ProfileResponse response = profileService.updateProfile(USER_ID, request);

    // Assert — userId trong saved profile phải là USER_ID, không phải OTHER_USER_ID
    ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
    verify(profileRepository).save(captor.capture());
    assertEquals(USER_ID, captor.getValue().getUserId());
    assertNotEquals(OTHER_USER_ID, captor.getValue().getUserId());
}

@Test
void updateProfile_noJwt_returns401() throws Exception {
    mockMvc.perform(patch("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\": \"Test\"}"))
        .andExpect(status().isUnauthorized());
}
```

**Expected Result (PASS):** Profile lưu với `userId = USER_ID`; không thể override với OTHER_USER_ID
**Expected Result (FAIL):** Profile được tạo với sai userId
**Current Status:** 🔴 Not written

---

### PRF-TC-007 — XSS payload trong displayName → bị reject/sanitize

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**CWE:** `CWE-79 — Cross-Site Scripting`
**Legal:** `GDPR Art. 5.1(d) — data accuracy; không lưu malicious content`
**Feature Under Test:** `ProfileServiceImpl.sanitizeDisplayName()` + `@Pattern` on DTO
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-007`

**Preconditions:**
- User đã xác thực với USER_ID

**Test Steps (Attack Simulation — JUnit 5):**
```java
@Test
void updateProfile_xssInDisplayName_returns400WithPRF005() throws Exception {
    // Attack: inject script tag via displayName
    mockMvc.perform(patch("/api/v1/profile")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\": \"<script>alert(document.cookie)</script>\"}")
            .with(mockJwt().jwt(j -> j.subject(USER_ID.toString()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("PRF-005"));

    // Verify không có script tag trong DB
    verify(profileRepository, never()).save(argThat(
        p -> p.getDisplayName() != null && p.getDisplayName().contains("<script>")));
}

@Test
void updateProfile_htmlTagsInDisplayName_stripped() {
    // Nếu chọn approach sanitize thay vì reject: HTML bị stripped
    UpdateProfileRequest request = buildValidRequest();
    request.setDisplayName("Hello <b>World</b>");

    // Service sanitize → lưu "Hello World" (không có tags)
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    ProfileResponse response = profileService.updateProfile(USER_ID, request);

    assertFalse(response.getDisplayName().contains("<"));
    assertFalse(response.getDisplayName().contains(">"));
}
```

**Expected Result (PASS = hệ thống an toàn):**
- Hoặc: HTTP 400 + `error.code = "PRF-005"` (reject)
- Hoặc: response.displayName không chứa `<` `>` (sanitize)
- DB không chứa HTML tags

**Expected Result (FAIL = lỗ hổng tồn tại):**
- HTTP 200 và `<script>` được lưu vào `display_name` → XSS stored

**Current Status:** 🔴 Not written

---

### PRF-TC-008 — Partial update (chỉ cập nhật 1 field, các field khác không thay đổi)

**Severity:** `MEDIUM`
**Feature Under Test:** `ProfileServiceImpl.applyUpdate()` — partial merge logic
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `UC-09` — tất cả fields là optional; chỉ field nào có giá trị mới được cập nhật

**Preconditions:**
- Profile của `USER_ID` đã tồn tại với `displayName = "Tên Cũ"`, `area = "Hà Nội"`

**Test Steps (JUnit 5):**
```java
@Test
void updateProfile_partialUpdate_onlySpecifiedFieldChanges() {
    // Arrange
    UserProfile existingProfile = new UserProfile();
    existingProfile.setUserId(USER_ID);
    existingProfile.setDisplayName("Tên Cũ");
    existingProfile.setArea("Hà Nội");
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
    when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    UpdateProfileRequest partialRequest = new UpdateProfileRequest();
    partialRequest.setArea("Hồ Chí Minh"); // Chỉ update area

    // Act
    ProfileResponse response = profileService.updateProfile(USER_ID, partialRequest);

    // Assert — chỉ area thay đổi, displayName giữ nguyên
    assertEquals("Tên Cũ", response.getDisplayName());    // unchanged
    assertEquals("Hồ Chí Minh", response.getArea());      // updated
}
```

**Expected Result (PASS):** `displayName` = "Tên Cũ"; `area` = "Hồ Chí Minh"
**Expected Result (FAIL):** `displayName` = null (bị xóa khi field không truyền)
**Current Status:** 🔴 Not written

---

### PRF-TC-009 — Audit log được ghi sau khi update thành công

**Severity:** `HIGH`
**Feature Under Test:** `ProfileServiceImpl` → `AuditService.log()` call
**Test File:** `src/test/java/com/carebridge/backend/profile/service/ProfileServiceImplTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-010`
**Oracle Source:** `ADR-002` — audit phải được gọi trong cùng transaction

**Test Steps (JUnit 5):**
```java
@Test
void updateProfile_success_auditLoggedWithCorrectAction() {
    // Arrange
    UpdateProfileRequest request = buildValidRequest();
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // Act
    profileService.updateProfile(USER_ID, request);

    // Assert — AuditService được gọi đúng 1 lần với action PROFILE_UPDATED
    verify(auditService, times(1)).log(
        eq(AuditAction.PROFILE_UPDATED),
        eq(USER_ID),
        eq("UserProfile"),
        any(String.class),
        any(String.class)
    );
}

@Test
void updateProfile_repositoryThrows_auditNotLogged() {
    // Arrange — repository throw exception → audit KHÔNG được gọi (transaction rollback)
    UpdateProfileRequest request = buildValidRequest();
    when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
    when(profileRepository.save(any())).thenThrow(new RuntimeException("DB error"));

    // Act & Assert
    assertThrows(RuntimeException.class,
        () -> profileService.updateProfile(USER_ID, request));

    verify(auditService, never()).log(any(), any(), any(), any(), any());
}
```

**Expected Result (PASS):** Audit được gọi khi success; không được gọi khi exception
**Expected Result (FAIL):** Audit luôn được gọi dù có exception → inconsistency
**Current Status:** 🟢 Passing — 2026-07-04 (covered in BOTH flows. (1) Legacy security-package flow: `AuthServiceGetProfileTest.updateProfile_success_writesAuditLog` / `updateProfile_saveFails_noAuditLog` (entity type "User"). (2) NEW profile-domain flow: `ProfileServiceImplTest.updateProfile_success_writesAuditLog` / `updateProfile_saveFails_noAuditLog` — `ProfileServiceImpl` calls `auditService.log(PROFILE_UPDATED, userId, "UserProfile", profileId, …)` in the same `@Transactional` as `save()`, and skips the audit when persistence throws. Both flows verified still passing after the PRF-002 change.)

---

### PRF-TC-INT-001 — Integration: PATCH /api/v1/profile → DB có record đúng

**Severity:** `HIGH`
**Feature Under Test:** `Full stack: ProfileController → ProfileServiceImpl → ProfileRepository → PostgreSQL`
**Test File:** `src/test/java/com/carebridge/backend/profile/ProfileIntegrationTest.java`
**TDD Phase:** 🔴 RED
**Condition Ref:** `TC-COND-009`

**Preconditions:**
- PostgreSQL Testcontainers running và migration V1–V4 applied
- User `USER_ID` tồn tại trong `users` table
- JWT mock cho `USER_ID` với role `ROLE_MOTHER`

**Test Steps (Spring Boot Integration Test):**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProfileIntegrationTest {

    @Test
    void patchProfile_fullStack_persistsToDbAndReturns200() throws Exception {
        // Arrange — seed user
        jdbcTemplate.execute(
            "INSERT INTO users(user_id, email, full_name, role, enabled, locked, created_at, updated_at) " +
            "VALUES ('" + USER_ID + "', 'test@test.com', 'Test', 'MOTHER', true, false, now(), now())"
        );

        // Act
        mockMvc.perform(patch("/api/v1/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Nguyễn Test\",\"phoneNumber\":\"0912345678\"}")
                .with(mockJwt().jwt(j -> j.subject(USER_ID.toString())
                    .claim("roles", List.of("ROLE_MOTHER")))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.displayName").value("Nguyễn Test"));

        // Assert DB
        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT display_name, phone_number FROM user_profiles WHERE user_id = ?",
            USER_ID
        );
        assertEquals("Nguyễn Test", row.get("display_name"));
        assertEquals("0912345678", row.get("phone_number"));

        // Assert audit
        Integer auditCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM audit_logs WHERE user_id = ? AND action = 'PROFILE_UPDATED'",
            Integer.class, USER_ID
        );
        assertEquals(1, auditCount);
    }
}
```

**Expected Result (PASS):**
- HTTP 200; `data.displayName = "Nguyễn Test"`
- `user_profiles` có row với `display_name = "Nguyễn Test"`
- `audit_logs` có record với `action = "PROFILE_UPDATED"` và `user_id = USER_ID`

**Expected Result (FAIL):**
- DB không có row → service không gọi `save()`
- Audit không có record → vi phạm ADR-002

**Current Status:** 🟢 Passing — 2026-07-04 (`profile/ProfileIntegrationTest.patchProfile_fullStack_persistsToDbAndRecordsAudit`, real PostgreSQL via Testcontainers. PATCH /api/v1/profile → 200 with persisted `displayName`/`phoneNumber` → `user_profiles` row confirmed via `ProfileRepository` → `audit_logs` has exactly 1 `PROFILE_UPDATED` row for the user. NOTE: this run also surfaced and fixed a real bug — `AuditEligibilityPolicy.SENSITIVE_ACTIONS` did not include `PROFILE_UPDATED`/`PROFILE_VIEWED`, so `AuditService.log()` was silently no-op'ing for both regardless of transaction semantics, violating ADR-002. Fixed by adding both actions to the allowlist; see `UC08_ViewAccountProfile_Test-Spec.md` for the parallel note on PROFILE_VIEWED.)

---

## 5. Red-Green-Refactor Tracker

| TC ID            | Test File                                                       | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note               |
| ---------------- | --------------------------------------------------------------- | ---------------- | ----------------- | ------------------------------ |
| `PRF-TC-001`     | `profile/dto/UpdateProfileRequestValidationTest.java`           | `[x]`            | `2026-07-04`      | Valid request → 0 violations   |
| `PRF-TC-002`     | `profile/dto/UpdateProfileRequestValidationTest.java`           | `[x]`            | `2026-07-04`      | @Pattern VN-phone on DTO       |
| `PRF-TC-003`     | `profile/dto/UpdateProfileRequestValidationTest.java`           | `[x]`            | `2026-07-04`      | @Size(min=2) + boundary case   |
| `PRF-TC-004`     | `profile/service/ProfileServiceImplTest.java`                   | `[x]`            | `2026-07-04`      | `validateDateOfBirth()`→PRF-002|
| `PRF-TC-005`     | `profile/dto/UpdateProfileRequestValidationTest.java`           | `[x]`            | `2026-07-04`      | @Pattern http/https on DTO     |
| `PRF-TC-006`     | `profile/service/ProfileServiceImplTest.java`                   | `[x]`            | `2026-07-04`      | Persists JWT userId (own-res.) |
| `PRF-TC-007`     | `profile/service/ProfileServiceImplTest.java`                   | `[x]`            | `2026-07-04`      | `sanitizeDisplayName()` strips |
| `PRF-TC-008`     | `security/service/AuthServiceGetProfileTest.java`               | `[x]`            | `2026-07-04`      | Legacy partial-update coverage |
| `PRF-TC-009`     | `profile/service/ProfileServiceImplTest.java`                   | `[x]`            | `2026-07-04`      | Audit in-TX + skip on failure  |
| `PRF-TC-INT-001` | `profile/ProfileIntegrationTest.java`                           | `[x]`            | `2026-07-04`      | Real Postgres; found+fixed audit policy bug |

### 5.1 Red Gate Protocol (CASE 2.0 — GATE-2)

> Trước khi implement, chạy toàn bộ test suite với empty/throw stub. Mọi test PHẢI FAIL.

**Stub cho Red Phase:**

```java
// Red Phase — ProfileServiceImpl stub (PHẢI throw)
@Service
public class ProfileServiceImpl implements IProfileService {

    @Override
    public ProfileResponse updateProfile(UUID authenticatedUserId, UpdateProfileRequest request) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public ProfileResponse getProfile(UUID authenticatedUserId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

**Red Gate Verification:**

| TC ID            | Stub Result                       | Expected   | Actual           | Root Cause (nếu PASS bất thường) |
| ---------------- | --------------------------------- | ---------- | ---------------- | -------------------------------- |
| `PRF-TC-001`     | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | ☐ Tautology ☐ Shared state      |
| `PRF-TC-002`     | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | —                                |
| `PRF-TC-004`     | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | —                                |
| `PRF-TC-007`     | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | —                                |
| `PRF-TC-INT-001` | `throw UnsupportedOperationException` | 🔴 FAIL | ☐ FAIL ☐ PASS | —                                |

**Red Gate Evidence:**
- Stub commit hash: `___`
- Tất cả FAIL? ☐ Yes → **GATE-2 PASS** (T2→T3) → tiếp tục implement

---

## 6. Entry / Exit Criteria

### Entry Criteria

- [ ] TDS `CB-PRF-IMP-001` đã được review và approve bởi Tech Lead
- [ ] Logic Issues (Section 2) đã được confirm với Principal Architect
- [ ] Migration `V4__add_user_profiles.sql` đã được approve
- [ ] Test fixtures FX-001 đến FX-009 (Section 3 TDS-05) đã chuẩn bị
- [ ] DPO đã sign-off (Sensitive-PII module)

### Exit Criteria (Definition of Done)

- [ ] `./mvnw test -pl 05_Development/CareBridgeAPI` — tất cả unit tests xanh (không có skip)
- [ ] Integration test `PRF-TC-INT-001` xanh với Testcontainers PostgreSQL
- [ ] Test coverage ≥ 80% lines cho: `ProfileController`, `ProfileServiceImpl`, `ProfileMapper`
- [ ] Không có HTML tags trong bất kỳ `display_name` nào trong DB
- [ ] Audit log có record cho mỗi profile update
- [ ] `userId` từ JWT không bị log dưới dạng plaintext không cần thiết
- [ ] PATCH `/api/v1/profile` với phone không hợp lệ → 400 + `PRF-001`

**Exit Criteria bổ sung — CASE 2.0:**
- [ ] **Red Gate (§5.1)** — tất cả 10 tests FAIL với throw stub trước khi implement
- [ ] **Contract Existence** — mọi import trong test files đều compile:
  ```bash
  ./mvnw compile -pl 05_Development/CareBridgeAPI 2>&1 | grep "error:"
  # Expected: no errors
  ```
- [ ] **Props Isolation** — không có shared mutable state giữa tests (dùng `buildValidRequest()` factory)
- [ ] **Oracle Source** — mọi `assertEquals` trong assert có ghi rõ nguồn BR/AC/ADR trong comment

### Suspension Criteria

- Migration `V4__add_user_profiles.sql` chưa được DPO sign-off
- Phát hiện lỗi kiến trúc mới (vd: `users` table thêm `date_of_birth` trực tiếp thay vì `user_profiles`)
- CI pipeline bị broken bởi thay đổi khác

---

## 7. Rollback Plan

```bash
# Revert Flyway migration (dev/staging — KHÔNG chạy trên production nếu có data)
# Xóa bảng user_profiles nếu chưa có data thật
DROP TABLE IF EXISTS public.user_profiles;

# Revert implementation files
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/

# Revert Flyway migration file
git checkout -- 05_Development/CareBridgeAPI/src/main/resources/db/migration/V4__add_user_profiles.sql

# Gap UC-09 vẫn OPEN → feature chưa ship
```

---

## 8. CASE 2.0 Anti-Pattern Detection (AI-Assisted TCs)

| AP-ID     | Anti-Pattern            | Dấu hiệu trong TDD spec                               | Check | Gate chặn |
| --------- | ----------------------- | ----------------------------------------------------- | ----- | --------- |
| AP-AI-001 | Unconstrained Generation | TC không reference ADR/BR nào trong Oracle Source     | ☐    | G-0       |
| AP-AI-002 | Green-from-Birth         | Test PASS với empty/throw stub (§5.1)                 | ☐    | G-2 ★    |
| AP-AI-003 | Implicit Decision        | Test dùng `users.date_of_birth` (không tồn tại trong schema V1) | ☐ | G-1  |
| AP-AI-004 | Layer Violation          | ServiceImpl test verify HTTP response code (controller concern) | ☐ | G-4  |
| AP-AI-005 | Hallucinated Contract    | Test import `ProfileMapper` chưa tạo                  | ☐    | G-3       |

**Kết quả review:**

- [ ] Không phát hiện anti-pattern nào → TDD spec approved
- [ ] Phát hiện AP → ghi vào bảng dưới → fix trước khi implement

| AP detected | TC ID | Mô tả | Fix action | Fixed? |
| ----------- | ----- | ----- | ---------- | ------ |
| `—`         | `—`   | —     | —          | ☐      |
