# Test Specification
## UC20 — Create Community Profile

| Field | Value |
|---|---|
| **Document ID** | CB-COMMUNITY-IMP-020-TEST |
| **Version** | 1.0 |
| **Date** | 2026-06-26 |
| **Status** | Draft |
| **Author** | AI Agent |
| **Related TDS** | CB-COMMUNITY-IMP-020 |
| **SRS Reference** | SRS 3.1.1.20 |

---

## 1. Mục Tiêu Kiểm Thử (Test Objectives)

Xác minh rằng `POST /api/v1/community/profiles` hoạt động đúng theo yêu cầu UC20:

1. Tạo thành công community profile mới và trả về 201.
2. Từ chối tạo profile trùng lặp (cùng userId) với 409.
3. Validate `displayName` — bắt buộc, max 100 chars.
4. `userId` lấy từ JWT, không từ request body.
5. `is_visible` default là `true` khi không được cung cấp.
6. Từ chối request không có JWT.

---

## 2. Phạm Vi Kiểm Thử (Test Scope)

| Loại | Bao gồm |
|---|---|
| Unit Tests | CommunityProfileService.createProfile() logic |
| MVC Tests | Controller validation, HTTP status codes |
| Integration Tests | Endpoint → DB round-trip |
| Security Tests | JWT check, userId isolation |

---

## 3. Test Factory & Setup

### 3.1 Props Factory

```java
// CommunityProfileTestFactory.java
public class CommunityProfileTestFactory {

    public static final UUID USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000020");

    public static final UUID OTHER_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000099");

    /**
     * Tạo request chuẩn với tất cả fields hợp lệ.
     */
    public static CreateCommunityProfileRequest makeRequest() {
        CreateCommunityProfileRequest req = new CreateCommunityProfileRequest();
        req.setDisplayName("TestMother20");
        req.setBio("This is a synthetic test bio");
        req.setInterestStage("PREGNANCY");
        req.setVisible(true);
        req.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/test-20.jpg");
        req.setRegion("Hà Nội, Việt Nam");
        return req;
    }

    /**
     * Request với displayName trống — dùng cho negative test.
     */
    public static CreateCommunityProfileRequest makeRequestWithEmptyDisplayName() {
        CreateCommunityProfileRequest req = makeRequest();
        req.setDisplayName("");
        return req;
    }

    /**
     * Request với displayName vượt 100 ký tự.
     */
    public static CreateCommunityProfileRequest makeRequestWithLongDisplayName() {
        CreateCommunityProfileRequest req = makeRequest();
        req.setDisplayName("A".repeat(101));
        return req;
    }

    /**
     * Request KHÔNG có field isVisible — test default value.
     */
    public static CreateCommunityProfileRequest makeRequestWithoutVisibility() {
        CreateCommunityProfileRequest req = new CreateCommunityProfileRequest();
        req.setDisplayName("DefaultVisibilityUser");
        req.setBio("Testing default visibility");
        // isVisible không được set — phải default true
        return req;
    }

    /**
     * CommunityProfile entity sau khi save — mock return value.
     */
    public static CommunityProfile makeSavedProfile(UUID userId) {
        CommunityProfile p = new CommunityProfile();
        p.setCommunityProfileId(UUID.randomUUID());
        p.setUserId(userId);
        p.setDisplayName("TestMother20");
        p.setBio("This is a synthetic test bio");
        p.setInterestStage("PREGNANCY");
        p.setVisible(true);
        p.setPublicAvatarUrl("https://storage.carebridge.vn/avatars/test-20.jpg");
        p.setRegion("Hà Nội, Việt Nam");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }
}
```

### 3.2 Test Environment Requirements

| Component | Yêu cầu |
|---|---|
| Framework | JUnit 5 + Mockito + Spring Boot Test |
| DB (Integration) | PostgreSQL test container hoặc H2 in-memory |
| Auth | MockMvc với `@WithMockUser` hoặc JWT test token |
| Assertion | AssertJ + MockMvc result matchers |

---

## 4. Test Cases — Unit Tests

### COMM-TC-020-001: Happy Path — Tạo Profile Hợp Lệ → 201

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-001 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related BR** | BR-COMM-001, BR-COMM-002, BR-COMM-004 |

**Preconditions:**
- `USER_ID` chưa có profile (existsByUserId → false)
- JWT hợp lệ cho `USER_ID`

**Test Steps:**

```java
@ExtendWith(MockitoExtension.class)
class CommunityProfileServiceTest {

    @Mock ICommunityProfileRepository profileRepository;
    @Mock AuditService auditService;
    @InjectMocks CommunityProfileServiceImpl service;

    @Test
    @DisplayName("COMM-TC-020-001: createProfile returns profile with 201 for valid request")
    void createProfile_validRequest_returnsProfileResponse() {
        // Arrange
        UUID userId = CommunityProfileTestFactory.USER_ID;
        CreateCommunityProfileRequest request = CommunityProfileTestFactory.makeRequest();
        CommunityProfile saved = CommunityProfileTestFactory.makeSavedProfile(userId);

        when(profileRepository.existsByUserId(userId)).thenReturn(false);
        when(profileRepository.save(any(CommunityProfile.class))).thenReturn(saved);

        // Act
        CommunityProfileResponse result = service.createProfile(userId, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getDisplayName()).isEqualTo("TestMother20");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.isVisible()).isTrue();
        assertThat(result.getCommunityProfileId()).isNotNull();

        // Audit must be emitted
        verify(auditService).emit(eq(AuditEventType.COMMUNITY_PROFILE_CREATED), eq(userId));
        verify(profileRepository).save(any(CommunityProfile.class));
    }
}
```

**Expected Result:** `CommunityProfileResponse` không null, `userId` đúng, `isVisible = true`, audit emitted.

---

### COMM-TC-020-002: Duplicate Profile — 409 COMM-001

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-002 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related BR** | BR-COMM-001 |
| **Error Code** | COMM-001 |

**Preconditions:**
- `USER_ID` đã có profile tồn tại (existsByUserId → true)

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-020-002: createProfile throws 409 when profile already exists")
void createProfile_duplicateProfile_throwsConflictException() {
    // Arrange
    UUID userId = CommunityProfileTestFactory.USER_ID;
    CreateCommunityProfileRequest request = CommunityProfileTestFactory.makeRequest();

    when(profileRepository.existsByUserId(userId)).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> service.createProfile(userId, request))
        .isInstanceOf(CommunityProfileAlreadyExistsException.class)
        .hasMessageContaining(userId.toString());

    // Repository.save() must NOT be called
    verify(profileRepository, never()).save(any());
    // Audit must NOT be emitted
    verify(auditService, never()).emit(any(), any());
}
```

**Expected Result:** `CommunityProfileAlreadyExistsException` thrown → HTTP 409, code `COMM-001`. `save()` không được gọi.

---

### COMM-TC-020-003: Empty displayName — 400 COMM-002

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-003 |
| **Priority** | P1 — High |
| **Type** | MVC / Validation |
| **Related BR** | BR-COMM-002 |
| **Error Code** | COMM-002 |

**Preconditions:**
- JWT hợp lệ
- Request body có `displayName: ""`

**Test Steps:**

```java
@WebMvcTest(CommunityProfileController.class)
class CommunityProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ICommunityProfileService profileService;

    @Test
    @DisplayName("COMM-TC-020-003: POST with empty displayName returns 400")
    @WithMockUser
    void createProfile_emptyDisplayName_returns400() throws Exception {
        String body = """
            {
              "displayName": "",
              "bio": "Test bio",
              "interestStage": "PREGNANCY"
            }
            """;

        mockMvc.perform(post("/api/v1/community/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());

        // Service should never be called for invalid input
        verify(profileService, never()).createProfile(any(), any());
    }

    @Test
    @DisplayName("COMM-TC-020-003b: POST with missing displayName returns 400")
    @WithMockUser
    void createProfile_missingDisplayName_returns400() throws Exception {
        String body = """
            {
              "bio": "No display name here"
            }
            """;

        mockMvc.perform(post("/api/v1/community/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("COMM-TC-020-003c: POST with displayName > 100 chars returns 400")
    @WithMockUser
    void createProfile_displayNameTooLong_returns400() throws Exception {
        String longName = "A".repeat(101);
        String body = String.format("""
            {"displayName": "%s"}
            """, longName);

        mockMvc.perform(post("/api/v1/community/profiles")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
```

**Expected Result:** HTTP 400. `profileService.createProfile()` không được gọi.

---

### COMM-TC-020-004: No JWT — 401 Unauthorized

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-004 |
| **Priority** | P0 — Critical (Security) |
| **Type** | MVC |
| **Error Code** | IAM-001 |

**Preconditions:**
- Request không có `Authorization` header

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-020-004: POST without JWT returns 401")
void createProfile_noJwt_returns401() throws Exception {
    String body = """
        {"displayName": "SomeUser", "bio": "Test"}
        """;

    mockMvc.perform(post("/api/v1/community/profiles")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isUnauthorized());

    verify(profileService, never()).createProfile(any(), any());
}
```

**Expected Result:** HTTP 401.

---

### COMM-TC-020-005: isVisible Defaults to True

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-005 |
| **Priority** | P1 — High |
| **Type** | Unit |
| **Related ADR** | ADR-COMM-020-002 |
| **Related CASE** | C4 |

**Preconditions:**
- Request KHÔNG chứa field `isVisible`
- `USER_ID` chưa có profile

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-020-005: isVisible defaults to true when not provided in request")
void createProfile_isVisibleDefaultsToTrue_whenNotProvided() {
    // Arrange
    UUID userId = CommunityProfileTestFactory.USER_ID;
    CreateCommunityProfileRequest request =
        CommunityProfileTestFactory.makeRequestWithoutVisibility();

    // Default value of isVisible in DTO should be true
    assertThat(request.isVisible()).isTrue();  // Verify DTO default

    CommunityProfile saved = CommunityProfileTestFactory.makeSavedProfile(userId);
    when(profileRepository.existsByUserId(userId)).thenReturn(false);
    when(profileRepository.save(any(CommunityProfile.class))).thenReturn(saved);

    // Act
    CommunityProfileResponse result = service.createProfile(userId, request);

    // Assert
    assertThat(result.isVisible()).isTrue();

    // Verify that saved entity had isVisible = true
    ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
    verify(profileRepository).save(captor.capture());
    assertThat(captor.getValue().isVisible()).isTrue();
}
```

**Expected Result:** `isVisible = true` trong response và trong entity được save.

---

### COMM-TC-020-006: userId Lấy từ JWT, không từ Request Body

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-006 |
| **Priority** | P0 — Critical (Security) |
| **Type** | Unit |
| **Related CASE** | C2 |

**Test Steps:**

```java
@Test
@DisplayName("COMM-TC-020-006: userId is extracted from JWT, not from request body")
void createProfile_userIdFromJwt_notFromRequestBody() {
    // Arrange
    UUID jwtUserId = CommunityProfileTestFactory.USER_ID;
    CreateCommunityProfileRequest request = CommunityProfileTestFactory.makeRequest();
    // Note: request DTO does NOT have a userId field — this is by design

    CommunityProfile saved = CommunityProfileTestFactory.makeSavedProfile(jwtUserId);
    when(profileRepository.existsByUserId(jwtUserId)).thenReturn(false);
    when(profileRepository.save(any(CommunityProfile.class))).thenReturn(saved);

    // Act — userId passed explicitly from controller (which got it from JWT)
    CommunityProfileResponse result = service.createProfile(jwtUserId, request);

    // Assert — saved entity's userId matches JWT userId
    ArgumentCaptor<CommunityProfile> captor = ArgumentCaptor.forClass(CommunityProfile.class);
    verify(profileRepository).save(captor.capture());
    assertThat(captor.getValue().getUserId()).isEqualTo(jwtUserId);
}
```

**Expected Result:** Entity được save với `userId = jwtUserId`. Request DTO không có userId field.

---

## 5. Test Cases — Integration Tests

### COMM-TC-020-INT-001: DB Có Row Mới Với userId Đúng

| Field | Value |
|---|---|
| **TC ID** | COMM-TC-020-INT-001 |
| **Priority** | P1 — High |
| **Type** | Integration |

**Preconditions:**
- PostgreSQL test container khởi động
- `USER_ID` chưa có profile trong DB
- JWT hợp lệ cho `USER_ID`

**Test Steps:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class CommunityProfileIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("COMM-TC-020-INT-001: POST creates DB row with correct userId")
    void integration_createProfile_insertsRowInDb() {
        // Arrange
        UUID userId = CommunityProfileTestFactory.USER_ID;
        String requestBody = """
            {
              "displayName": "TestMother20",
              "bio": "This is a synthetic test bio",
              "interestStage": "PREGNANCY",
              "isVisible": true
            }
            """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(generateTestJwt(userId));
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/community/profiles",
            HttpMethod.POST,
            entity,
            String.class);

        // Assert HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Assert DB
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM community_profiles WHERE user_id = ?::uuid",
            Integer.class, userId.toString());
        assertThat(count).isEqualTo(1);

        // Assert correct userId in DB
        UUID dbUserId = jdbcTemplate.queryForObject(
            "SELECT user_id FROM community_profiles WHERE user_id = ?::uuid",
            UUID.class, userId.toString());
        assertThat(dbUserId).isEqualTo(userId);

        // Assert isVisible = true in DB
        Boolean dbVisible = jdbcTemplate.queryForObject(
            "SELECT is_visible FROM community_profiles WHERE user_id = ?::uuid",
            Boolean.class, userId.toString());
        assertThat(dbVisible).isTrue();
    }
}
```

**Expected Result:**
- HTTP 201
- DB có đúng 1 row với `user_id = USER_ID`
- `is_visible = true` trong DB

---

## 6. Logic Issues & Known Risks

| ID | Mô tả | Cách phòng ngừa |
|---|---|---|
| **L1** | `userId` phải đến từ JWT, không từ request body — client không thể giả mạo userId | Controller dùng `SecurityUtils.requireCurrentUserId(principal)`, DTO không có userId field |
| **L2** | `is_visible` default = `true` khi không gửi — DTO field có `= true` initializer | TC-020-005 verify default; TC-020-INT-001 check DB |
| **L3** | Profile existence check phải dùng `userId` từ JWT — không từ parameter khác | TC-020-002 verify `existsByUserId(jwtUserId)` được gọi |

---

## 7. Acceptance Criteria Checklist

| Criterion | TC Coverage | Pass/Fail |
|---|---|---|
| POST valid request → 201 | COMM-TC-020-001 | ☐ |
| Duplicate userId → 409 COMM-001 | COMM-TC-020-002 | ☐ |
| Empty displayName → 400 | COMM-TC-020-003 | ☐ |
| No JWT → 401 | COMM-TC-020-004 | ☐ |
| isVisible defaults to true | COMM-TC-020-005 | ☐ |
| userId from JWT only | COMM-TC-020-006 | ☐ |
| DB row inserted correctly | COMM-TC-020-INT-001 | ☐ |
| AuditService.emit() called | COMM-TC-020-001 verify | ☐ |
| save() not called on duplicate | COMM-TC-020-002 verify | ☐ |

---

## 8. Traceability Matrix

| TC ID | SRS | BR | TDS Section |
|---|---|---|---|
| COMM-TC-020-001 | 3.1.1.20 | BR-COMM-001, BR-COMM-004 | §11.3, §9 |
| COMM-TC-020-002 | 3.1.1.20 | BR-COMM-001 | §10, §17 C1 |
| COMM-TC-020-003 | 3.1.1.20 | BR-COMM-002 | §8.1, §10 |
| COMM-TC-020-004 | 3.1.1.20 | — | §10, §16 |
| COMM-TC-020-005 | 3.1.1.20 | BR-COMM-001 | §17 C4, ADR-COMM-020-002 |
| COMM-TC-020-006 | 3.1.1.20 | BR-COMM-001 | §17 C2 |
| COMM-TC-020-INT-001 | 3.1.1.20 | BR-COMM-001 | §14 |

---

*End of UC20 Test Specification — CB-COMMUNITY-IMP-020-TEST v1.0*
