# Test Specification
## UC21 — Update Community Profile

| Field | Value |
|---|---|
| **Document ID** | CB-COMMUNITY-IMP-021-TEST |
| **Version** | 1.0 |
| **Date** | 2026-06-26 |
| **Status** | Draft |
| **Author** | AI Agent |
| **Related TDS** | CB-COMMUNITY-IMP-021 |
| **SRS Reference** | SRS 3.1.1.21 |
| **Depends on** | UC20 (profile must exist before update) |

---

## 1. Mục Tiêu Kiểm Thử (Test Objectives)

Xác minh rằng `PUT /api/v1/community/profiles/me` hoạt động đúng theo yêu cầu UC21:

1. Cập nhật thành công profile hiện có và trả về 200.
2. Ẩn profile bằng cách set `is_visible = false` — không xóa dữ liệu.
3. Trả về 404 khi profile chưa tồn tại.
4. Từ chối request không có JWT.
5. `userId` luôn lấy từ JWT, không từ URL path.
6. `updated_at` thay đổi sau mỗi lần update.

---

## 2. Phạm Vi Kiểm Thử (Test Scope)

| Loại | Bao gồm |
|---|---|
| Unit Tests | CommunityProfileService.updateProfile() logic |
| MVC Tests | Controller validation, HTTP status codes, routing |
| Integration Tests | Endpoint → DB round-trip |
| Security Tests | JWT check, ownership |
| Business Logic | Hide = set invisible (not delete) |

---

## 3. Test Factory & Setup

### 3.1 Props Factory

```java
// CommunityProfileUpdateTestFactory.java
public class CommunityProfileUpdateTestFactory {

    public static final UUID USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000021");

    public static final UUID NO_PROFILE_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000098");

    /**
     * Request chuẩn để update displayName.
     */
    public static UpdateCommunityProfileRequest makeUpdateRequest() {
        UpdateCommunityProfileRequest req = new UpdateCommunityProfileRequest();
        req.setDisplayName("UpdatedDisplayName21");
        req.setBio("Updated bio content");
        req.setInterestStage("POSTPARTUM");
        req.setVisible(true);
        req.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/user-21-v2.jpg");
        req.setRegion("TP. Hồ Chí Minh");
        return req;
    }

    /**
     * Request để ẩn profile (is_visible = false).
     */
    public static UpdateCommunityProfileRequest makeHideProfileRequest() {
        UpdateCommunityProfileRequest req = makeUpdateRequest();
        req.setVisible(false);
        return req;
    }

    /**
     * Request với displayName vượt 100 chars.
     */
    public static UpdateCommunityProfileRequest makeRequestWithLongDisplayName() {
        UpdateCommunityProfileRequest req = makeUpdateRequest();
        req.setDisplayName("B".repeat(101));
        return req;
    }

    /**
     * Existing profile entity đã được save trước (UC20 prerequisite).
     */
    public static CommunityProfile makeExistingProfile(UUID userId) {
        CommunityProfile p = new CommunityProfile();
        p.setCommunityProfileId(UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567821"));
        p.setUserId(userId);
        p.setDisplayName("OriginalDisplayName21");
        p.setBio("Original bio");
        p.setInterestStage("PREGNANCY");
        p.setVisible(true);
        p.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/user-21.jpg");
        p.setRegion("Hà Nội, Việt Nam");
        p.setCreatedAt(Instant.now().minus(7, ChronoUnit.DAYS));
        p.setUpdatedAt(Instant.now().minus(7, ChronoUnit.DAYS));
        return p;
    }

    /**
     * Profile đã được update (sau khi service.updateProfile() gọi).
     */
    public static CommunityProfile makeUpdatedProfile(UUID userId, boolean isVisible) {
        CommunityProfile p = makeExistingProfile(userId);
        p.setDisplayName("UpdatedDisplayName21");
        p.setBio("Updated bio content");
        p.setInterestStage("POSTPARTUM");
        p.setVisible(isVisible);
        p.setUpdatedAt(Instant.now());  // updated_at changed
        return p;
    }
}
```

### 3.2 Test Environment Requirements

| Component | Yêu cầu |
|---|---|
| Framework | JUnit 5 + Mockito + Spring Boot Test |
| DB (Integration) | PostgreSQL test container |
| Auth | MockMvc với `@WithMockUser` hoặc JWT test token |
| Precondition | Profile cho `USER_ID` phải được insert trước mỗi test |

---

## 4. Test Cases — Unit Tests

### COMM-TC-021-001: Happy Path — Update displayName → 200

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-001 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related BR** | BR-COMM-011, BR-COMM-012, BR-COMM-014 |

**Preconditions:**
- `USER_ID` có profile tồn tại (findByUserId → profile)
- JWT hợp lệ cho `USER_ID`

**Test Steps:**

```java
@ExtendWith(MockitoExtension.class)
class CommunityProfileUpdateServiceTest {

    @Mock ICommunityProfileRepository profileRepository;
    @Mock AuditService auditService;
    @InjectMocks CommunityProfileServiceImpl service;

    @Test
    @DisplayName("COMM-TC-021-001: updateProfile updates displayName successfully → 200")
    void updateProfile_validRequest_returnsUpdatedProfile() {
        // Arrange
        UUID userId = CommunityProfileUpdateTestFactory.USER_ID;
        UpdateCommunityProfileRequest request =
            CommunityProfileUpdateTestFactory.makeUpdateRequest();
        CommunityProfile existing =
            CommunityProfileUpdateTestFactory.makeExistingProfile(userId);
        CommunityProfile updated =
            CommunityProfileUpdateTestFactory.makeUpdatedProfile(userId, true);

        when(profileRepository.findByUserId(userId))
            .thenReturn(Optional.of(existing));
        when(profileRepository.save(any(CommunityProfile.class)))
            .thenReturn(updated);

        // Act
        CommunityProfileResponse result = service.updateProfile(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("UpdatedDisplayName21");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isVisible()).isTrue();

        // Verify interactions
        verify(profileRepository).findByUserId(userId);
        verify(profileRepository).save(any(CommunityProfile.class));
        verify(auditService).emit(eq(AuditEventType.COMMUNITY_PROFILE_UPDATED), eq(userId));
    }
}
```

**Expected Result:** `CommunityProfileResponse` với `displayName = "UpdatedDisplayName21"`. Audit emitted. `save()` được gọi.

---

### COMM-TC-021-002: Hide Profile — is_visible = false → 200, Không Xóa

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-002 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related ADR** | ADR-COMM-021-002 |
| **Related CASE** | C4 |

**Preconditions:**
- `USER_ID` có profile với `is_visible = true`
- JWT hợp lệ

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-021-002: updateProfile with isVisible=false hides profile (does not delete)")
void updateProfile_hideProfile_setsIsVisibleFalse_doesNotDelete() {
    // Arrange
    UUID userId = CommunityProfileUpdateTestFactory.USER_ID;
    UpdateCommunityProfileRequest request =
        CommunityProfileUpdateTestFactory.makeHideProfileRequest();  // isVisible=false
    CommunityProfile existing =
        CommunityProfileUpdateTestFactory.makeExistingProfile(userId);
    CommunityProfile hiddenProfile =
        CommunityProfileUpdateTestFactory.makeUpdatedProfile(userId, false);

    when(profileRepository.findByUserId(userId))
        .thenReturn(Optional.of(existing));
    when(profileRepository.save(any(CommunityProfile.class)))
        .thenReturn(hiddenProfile);

    // Act
    CommunityProfileResponse result = service.updateProfile(userId, request);

    // Assert — response shows isVisible=false
    assertThat(result.isVisible()).isFalse();
    assertThat(result.getCommunityProfileId()).isNotNull();  // still exists

    // Verify: delete() was NEVER called — hiding ≠ deleting
    verify(profileRepository, never()).delete(any());
    verify(profileRepository, never()).deleteById(any());

    // Verify: save() was called (update, not delete)
    verify(profileRepository).save(any(CommunityProfile.class));

    // Verify: entity's isVisible was set to false before saving
    ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
    verify(profileRepository).save(captor.capture());
    assertThat(captor.getValue().isVisible()).isFalse();
}
```

**Expected Result:** 200, `isVisible = false` trong response. `delete()` không bao giờ được gọi. Profile vẫn tồn tại trong DB.

---

### COMM-TC-021-003: Profile Not Found → 404 COMM-011

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-003 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related BR** | BR-COMM-011 |
| **Error Code** | COMM-011 |

**Preconditions:**
- `NO_PROFILE_USER_ID` chưa có profile trong DB (findByUserId → empty)
- JWT hợp lệ

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-021-003: updateProfile throws 404 when profile does not exist")
void updateProfile_profileNotFound_throwsNotFoundException() {
    // Arrange
    UUID userId = CommunityProfileUpdateTestFactory.NO_PROFILE_USER_ID;
    UpdateCommunityProfileRequest request =
        CommunityProfileUpdateTestFactory.makeUpdateRequest();

    when(profileRepository.findByUserId(userId))
        .thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> service.updateProfile(userId, request))
        .isInstanceOf(CommunityProfileNotFoundException.class)
        .hasMessageContaining(userId.toString());

    // Neither save nor delete should be called
    verify(profileRepository, never()).save(any());
    verify(profileRepository, never()).delete(any());
    verify(auditService, never()).emit(any(), any());
}
```

**Expected Result:** `CommunityProfileNotFoundException` thrown → HTTP 404, code `COMM-011`. `save()` không gọi.

---

### COMM-TC-021-004: No JWT → 401 Unauthorized

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-004 |
| **Priority** | P0 — Critical (Security) |
| **Type** | MVC |
| **Error Code** | IAM-001 |

**Preconditions:**
- Request không có `Authorization` header

**Test Steps:**

```java
@WebMvcTest(CommunityProfileController.class)
class CommunityProfileUpdateControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ICommunityProfileService profileService;

    @Test
    @DisplayName("COMM-TC-021-004: PUT without JWT returns 401")
    void updateProfile_noJwt_returns401() throws Exception {
        String body = """
            {
              "displayName": "SomeUpdate",
              "isVisible": true
            }
            """;

        mockMvc.perform(put("/api/v1/community/profiles/me")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());

        verify(profileService, never()).updateProfile(any(), any());
    }
}
```

**Expected Result:** HTTP 401.

---

### COMM-TC-021-005: displayName Validation — Max 100 Chars

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-005 |
| **Priority** | P1 — High |
| **Type** | MVC / Validation |
| **Related BR** | BR-COMM-013 |
| **Error Code** | COMM-012 |

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-021-005: PUT with displayName > 100 chars returns 400")
@WithMockUser
void updateProfile_displayNameTooLong_returns400() throws Exception {
    String longName = "C".repeat(101);
    String body = String.format("""
        {
          "displayName": "%s",
          "isVisible": true
        }
        """, longName);

    mockMvc.perform(put("/api/v1/community/profiles/me")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest());

    verify(profileService, never()).updateProfile(any(), any());
}
```

**Expected Result:** HTTP 400. `updateProfile()` không được gọi.

---

### COMM-TC-021-006: PUT Semantics — Null Fields Are Cleared

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-006 |
| **Priority** | P1 — High |
| **Type** | Unit |
| **Related ADR** | ADR-COMM-021-001 |

**Preconditions:**
- Profile tồn tại với `bio = "Existing bio"` và `region = "Hà Nội"`
- Update request KHÔNG gửi `bio` và `region` (null values)

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-021-006: PUT replaces all fields — null fields are cleared")
void updateProfile_putSemantics_nullFieldsAreCleared() {
    // Arrange
    UUID userId = CommunityProfileUpdateTestFactory.USER_ID;

    // Request with only displayName — bio and region not sent (null)
    UpdateCommunityProfileRequest request = new UpdateCommunityProfileRequest();
    request.setDisplayName("OnlyDisplayName");
    request.setVisible(true);
    // bio = null, region = null intentionally

    CommunityProfile existing =
        CommunityProfileUpdateTestFactory.makeExistingProfile(userId);
    // existing has bio and region set

    when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

    // Act
    service.updateProfile(userId, request);

    // Assert — entity should have null bio and region (PUT semantics)
    ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
    verify(profileRepository).save(captor.capture());
    CommunityProfile saved = captor.getValue();

    assertThat(saved.getDisplayName()).isEqualTo("OnlyDisplayName");
    assertThat(saved.getBio()).isNull();     // cleared by PUT
    assertThat(saved.getRegion()).isNull();  // cleared by PUT
}
```

**Expected Result:** `bio` và `region` được clear về null trong entity được save. PUT replaces all.

---

### COMM-TC-021-007: updated_at Changes After Update

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-007 |
| **Priority** | P1 — High |
| **Type** | Unit |

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-021-007: updateProfile sets updatedAt to current time")
void updateProfile_updatesUpdatedAt() {
    // Arrange
    UUID userId = CommunityProfileUpdateTestFactory.USER_ID;
    UpdateCommunityProfileRequest request =
        CommunityProfileUpdateTestFactory.makeUpdateRequest();
    CommunityProfile existing =
        CommunityProfileUpdateTestFactory.makeExistingProfile(userId);
    Instant originalUpdatedAt = existing.getUpdatedAt();

    when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(existing));
    when(profileRepository.save(any(CommunityProfile.class))).thenAnswer(i -> i.getArgument(0));

    // Wait a small amount to ensure time difference
    Instant beforeUpdate = Instant.now();

    // Act
    service.updateProfile(userId, request);

    // Assert — updatedAt was changed to a time >= beforeUpdate
    ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
    verify(profileRepository).save(captor.capture());
    CommunityProfile saved = captor.getValue();

    assertThat(saved.getUpdatedAt()).isNotEqualTo(originalUpdatedAt);
    assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(beforeUpdate);
}
```

**Expected Result:** `updatedAt` di chuyển sang thời điểm mới, khác với `originalUpdatedAt`.

---

## 5. Test Cases — Integration Tests

### COMM-TC-021-INT-001: DB `updated_at` Thay Đổi Sau Update

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-021-INT-001 |
| **Priority** | P1 — High |
| **Type** | Integration |

**Preconditions:**
- PostgreSQL test container khởi động
- `USER_ID` đã có profile trong DB (insert trước)
- JWT hợp lệ cho `USER_ID`

**Test Steps:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CommunityProfileUpdateIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired ICommunityProfileRepository repo;

    private UUID userId = CommunityProfileUpdateTestFactory.USER_ID;

    @BeforeEach
    void setup() {
        // Insert profile as UC20 prerequisite
        CommunityProfile existing = CommunityProfileUpdateTestFactory.makeExistingProfile(userId);
        repo.save(existing);
    }

    @AfterEach
    void cleanup() {
        repo.deleteByUserId(userId);
    }

    @Test
    @DisplayName("COMM-TC-021-INT-001: PUT updates DB updated_at and is_visible")
    void integration_updateProfile_changesUpdatedAt() throws InterruptedException {
        // Get original updated_at
        Instant originalUpdatedAt = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM community_profiles WHERE user_id = ?::uuid",
            Instant.class, userId.toString());

        // Small sleep to ensure time difference is measurable
        Thread.sleep(10);

        // Act — PUT request
        String requestBody = """
            {
              "displayName": "IntegrationUpdated",
              "bio": "Updated via integration test",
              "isVisible": true
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateTestJwt(userId));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/community/profiles/me",
            HttpMethod.PUT,
            entity,
            String.class);

        // Assert HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert DB — updated_at changed
        Instant newUpdatedAt = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM community_profiles WHERE user_id = ?::uuid",
            Instant.class, userId.toString());
        assertThat(newUpdatedAt).isAfter(originalUpdatedAt);

        // Assert display_name updated
        String dbDisplayName = jdbcTemplate.queryForObject(
            "SELECT display_name FROM community_profiles WHERE user_id = ?::uuid",
            String.class, userId.toString());
        assertThat(dbDisplayName).isEqualTo("IntegrationUpdated");

        // Assert profile still exists (not deleted)
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM community_profiles WHERE user_id = ?::uuid",
            Integer.class, userId.toString());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("COMM-TC-021-INT-001b: Hiding profile sets is_visible=false, row still exists")
    void integration_hideProfile_setsIsVisibleFalse_rowStillExists() {
        // Act — hide profile
        String requestBody = """
            {
              "displayName": "OriginalDisplayName21",
              "isVisible": false
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateTestJwt(userId));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/community/profiles/me",
            HttpMethod.PUT,
            entity,
            String.class);

        // Assert HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert DB — is_visible = false
        Boolean dbVisible = jdbcTemplate.queryForObject(
            "SELECT is_visible FROM community_profiles WHERE user_id = ?::uuid",
            Boolean.class, userId.toString());
        assertThat(dbVisible).isFalse();

        // Assert row still exists
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM community_profiles WHERE user_id = ?::uuid",
            Integer.class, userId.toString());
        assertThat(count).isEqualTo(1);
    }
}
```

**Expected Result:**
- `updated_at` sau update > `updated_at` trước update.
- Profile với `is_visible = false` vẫn tồn tại trong DB (count = 1).

---

## 6. Logic Issues & Known Risks

| ID | Mô tả | Cách phòng ngừa |
|---|---|---|
| **L1** | PUT thay thế TẤT CẢ fields — null fields sẽ bị clear. Client phải gửi toàn bộ state mong muốn. Nếu client chỉ gửi 1 field, các field còn lại bị null | TC-021-006 verify PUT semantics; document rõ ràng trong API docs |
| **L2** | Ẩn profile (is_visible=false) KHÔNG xóa data — GET profile của chính mình vẫn hoạt động. Cần filter ở community feed query | TC-021-002 verify delete() never called; integration TC-021-INT-001b verify COUNT=1 |
| **L3** | Ownership xác định qua JWT, không qua URL path — không có `{userId}` trong endpoint path | Controller inspection: `PUT /profiles/me` không có path variable |

---

## 7. Acceptance Criteria Checklist

| Criterion | TC Coverage | Pass/Fail |
|---|---|---|
| PUT valid request → 200 | COMM-TC-021-001 | ☐ |
| Hide profile → is_visible=false in DB | COMM-TC-021-002 | ☐ |
| Hiding does not delete row | COMM-TC-021-002 (verify delete() never called) | ☐ |
| Profile not found → 404 COMM-011 | COMM-TC-021-003 | ☐ |
| No JWT → 401 | COMM-TC-021-004 | ☐ |
| displayName > 100 → 400 | COMM-TC-021-005 | ☐ |
| PUT clears null fields | COMM-TC-021-006 | ☐ |
| updated_at changes after update | COMM-TC-021-007 + INT-001 | ☐ |
| AuditService.emit() called on update | COMM-TC-021-001 verify | ☐ |
| Ownership from JWT, not URL | L3 inspection | ☐ |

---

## 8. Traceability Matrix

| TC ID | SRS | BR | TDS Section |
|---|---|---|---|
| COMM-TC-021-001 | 3.1.1.21 | BR-COMM-011, BR-COMM-014 | §8.3, §9 |
| COMM-TC-021-002 | 3.1.1.21 | BR-COMM-011 | §17 C4, ADR-COMM-021-002 |
| COMM-TC-021-003 | 3.1.1.21 | BR-COMM-011 | §10, §17 C1 |
| COMM-TC-021-004 | 3.1.1.21 | — | §10, §16 |
| COMM-TC-021-005 | 3.1.1.21 | BR-COMM-013 | §8.1, §10 |
| COMM-TC-021-006 | 3.1.1.21 | — | ADR-COMM-021-001 |
| COMM-TC-021-007 | 3.1.1.21 | — | §8.3 |
| COMM-TC-021-INT-001 | 3.1.1.21 | BR-COMM-011 | §14 |

---

*End of UC21 Test Specification — CB-COMMUNITY-IMP-021-TEST v1.0*
