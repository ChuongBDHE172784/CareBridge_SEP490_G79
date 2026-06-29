# Test Specification — UC-11: View Notifications

| Field            | Value                          |
|------------------|--------------------------------|
| Document ID      | CB-NOTIF-IMP-011-TEST          |
| Version          | 1.0                            |
| Date             | 2026-06-26                     |
| Status           | Implemented                          |
| Author           | AI Agent                       |
| Related TDS      | CB-NOTIF-IMP-011               |
| Based on EDS     | v2.0 / CASE 2.0                |
| Test ID Prefix   | NOTIF-TC-011                   |

---

## 1. Thông tin Module

### 1.1 Phạm vi kiểm thử

| Hạng mục             | Nằm trong phạm vi                                     | Ngoài phạm vi                              |
|----------------------|-------------------------------------------------------|--------------------------------------------|
| Layer                | Controller, Service, Repository (unit + integration)  | Push notification delivery (external)      |
| Endpoint             | GET /api/v1/notifications                             | POST/PUT/DELETE notifications              |
| Bộ lọc               | ALL, UNREAD, READ                                     | Full-text search                           |
| Phân quyền           | ROLE_MOTHER, ROLE_EXPERT, ROLE_ADMIN, Anonymous       | Inter-service auth                         |
| Pagination           | page, size, totalElements, totalPages, unreadCount    | Cursor-based pagination                    |
| Platform             | API layer only (backend)                              | Mobile/Web UI rendering                    |

### 1.2 Test Levels

| Level           | Tool / Framework                           | Scope                                    |
|-----------------|--------------------------------------------|------------------------------------------|
| Unit            | JUnit 5 + Mockito                          | Service logic, mapper, validation        |
| Integration     | @SpringBootTest + MockMvc + H2/Testcontainers | Controller → Service → DB            |
| Contract        | Manual cURL / Postman                      | Request/Response schema validation       |

### 1.3 Module Dependencies

| Dependency              | Vai trò trong test                                      |
|-------------------------|----------------------------------------------------------|
| INotificationRepository | Mock trong unit tests, real trong integration tests      |
| SecurityUtils           | Mock với fixed userId UUID                              |
| AuditService            | Mock (verify emit() được gọi)                           |
| Flyway migration V2     | Applied trước khi integration tests chạy                |

---

## 2. Logic Issues Resolved

### L1 — Ownership Enforcement

**Issue**: User A không được nhìn thấy notification của User B.

**Resolution**: `SecurityUtils.requireCurrentUserId(principal)` trích xuất `userId` từ JWT claim. Service layer truyền `userId` này trực tiếp vào tất cả repository queries. Không có parameter nào từ request body hay URL path được dùng để xác định `userId`. Test case `NOTIF-TC-011-004` xác minh điều này ở service level.

**Verification**: Verify rằng repository không bao giờ được gọi với `userId` khác với `userId` từ JWT.

---

### L2 — Unread Count trong Response

**Issue**: Client cần biết tổng số notification chưa đọc ngay trong response để hiển thị badge, bất kể filter hiện tại là gì.

**Resolution**: `PaginatedResponse<T>` bổ sung field `unreadCount: long`. Service luôn gọi `notificationRepository.countUnreadByUserId(userId)` độc lập với bộ lọc `status`. Kể cả khi filter=READ, `unreadCount` vẫn trả về số notification chưa đọc thực tế.

**Verification**: Test với status=READ, assert rằng `unreadCount` vẫn phản ánh đúng số bản ghi `is_read=false` trong DB.

---

### L3 — Pagination Default Values

**Issue**: Client có thể không truyền `page` và `size`. Default không nhất quán sẽ gây confusion.

**Resolution**: `NotificationListRequest` định nghĩa default: `page=0`, `size=20`, `status=ALL`. Validation constraint: `page ≥ 0`, `1 ≤ size ≤ 100`. Khi thiếu param, Spring binding áp dụng default.

**Verification**: Test gọi endpoint không có bất kỳ param nào, assert response dùng page=0, size=20.

---

## 3. Test Design Specification (TDS)

### 3.1 Test Basis (Cơ sở Kiểm thử)

| Tài liệu               | Phiên bản | Sections liên quan                          |
|------------------------|-----------|---------------------------------------------|
| SRS §3.1.1.11          | 1.0       | FR-01 to FR-04, SEC-01                     |
| TDS CB-NOTIF-IMP-011   | 1.0       | §8 Interface Spec, §9 API, §10 Error Codes |
| ADR-011-001/002/003    | 1.0       | Architecture decisions                      |

### 3.2 Test Conditions

| Condition ID | Điều kiện                                     | Nguồn                  |
|--------------|-----------------------------------------------|------------------------|
| TC-COND-01   | JWT hợp lệ và chưa hết hạn                   | SRS-11-FR-04, SEC-01   |
| TC-COND-02   | JWT hết hạn hoặc không có                     | ERR-NOTIF-010          |
| TC-COND-03   | page >= 0, 1 <= size <= 100                   | SRS-11-FR-02, L3       |
| TC-COND-04   | page < 0 hoặc size < 1 hoặc size > 100       | ERR-NOTIF-011          |
| TC-COND-05   | status = ALL                                  | SRS-11-FR-03           |
| TC-COND-06   | status = UNREAD                               | SRS-11-FR-03           |
| TC-COND-07   | status = READ                                 | SRS-11-FR-03           |
| TC-COND-08   | userId từ JWT khác userId của notification    | L1 Ownership           |
| TC-COND-09   | User không có notification nào               | SRS-11-FR-01           |
| TC-COND-10   | unreadCount phản ánh đúng DB state           | L2 Unread Count        |

### 3.3 Test Techniques

| Technique                         | Áp dụng cho                                       |
|-----------------------------------|---------------------------------------------------|
| Equivalence Partitioning          | page, size, status params                         |
| Boundary Value Analysis           | size: 0, 1, 100, 101; page: -1, 0                |
| Decision Table                    | Bộ lọc status kết hợp với ownership              |
| State Transition                  | is_read: false → (UC-12 MarkAsRead) → true       |
| Negative Testing                  | Missing JWT, invalid params, ownership violation  |

### 3.4 Test Fixtures

```java
/**
 * Factory class theo CASE 2.0 Props Isolation pattern.
 * Sử dụng Consumer<T> override để tạo test data linh hoạt,
 * tránh tạo nhiều constructor overloads.
 */
public class NotificationTestFactory {

    public static final UUID DEFAULT_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID OTHER_USER_ID   = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /**
     * Tạo một Notification với giá trị mặc định hợp lệ.
     * Override bất kỳ field nào bằng Consumer<Notification>.
     *
     * Ví dụ:
     *   makeNotification(n -> n.setIsRead(true))
     *   makeNotification(n -> { n.setUserId(OTHER_USER_ID); n.setCategory(NotificationCategory.EMERGENCY); })
     */
    public static Notification makeNotification(Consumer<Notification> overrides) {
        Notification n = Notification.builder()
                .notificationId(UUID.randomUUID())
                .userId(DEFAULT_USER_ID)
                .title("Test Notification Title")
                .body("Test notification body content.")
                .category(NotificationCategory.REMINDER)
                .isRead(false)
                .readAt(null)
                .metadata(Map.of("testKey", "testValue"))
                .createdAt(OffsetDateTime.now().minusHours(1))
                .updatedAt(OffsetDateTime.now().minusHours(1))
                .build();
        if (overrides != null) {
            overrides.accept(n);
        }
        return n;
    }

    /** Convenience: tạo notification với giá trị mặc định, không override */
    public static Notification makeNotification() {
        return makeNotification(null);
    }

    /** Tạo danh sách N notifications cho cùng một userId */
    public static List<Notification> makeNotificationList(int count, UUID userId) {
        return IntStream.range(0, count)
                .mapToObj(i -> makeNotification(n -> {
                    n.setUserId(userId);
                    n.setTitle("Notification #" + i);
                    n.setCreatedAt(OffsetDateTime.now().minusMinutes(i));
                }))
                .collect(Collectors.toList());
    }

    /** Tạo NotificationListRequest với giá trị mặc định */
    public static NotificationListRequest makeRequest(Consumer<NotificationListRequest> overrides) {
        NotificationListRequest req = new NotificationListRequest();
        // defaults: page=0, size=20, status=ALL (from field initializers)
        if (overrides != null) {
            overrides.accept(req);
        }
        return req;
    }
}
```

### 3.5 Mock Setup (Unit Tests)

```java
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock INotificationRepository notificationRepository;
    @Mock NotificationMapper notificationMapper;
    @Mock AuditService auditService;

    @InjectMocks NotificationServiceImpl notificationService;

    // Standard mock setup trước mỗi test
    private void mockEmptyPage(UUID userId) {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(
                eq(userId), any(Pageable.class)))
            .thenReturn(Page.empty());
        when(notificationRepository.countUnreadByUserId(userId))
            .thenReturn(0L);
        when(notificationMapper.toResponseList(anyList()))
            .thenReturn(Collections.emptyList());
    }
}
```

---

## 4. Test Case Specification

---

### NOTIF-TC-011-001 — Happy Path: GET trả về danh sách phân trang

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-001                                                    |
| **Tên**           | Happy path — GET /api/v1/notifications với JWT hợp lệ              |
| **Level**         | Unit (Service) + Integration (Controller)                           |
| **Priority**      | P0 — Critical                                                       |
| **Condition**     | TC-COND-01, TC-COND-03, TC-COND-05                                  |
| **Precondition**  | User tồn tại, có 2 notifications trong DB (cả đọc và chưa đọc)    |

**Input**:
```
GET /api/v1/notifications?page=0&size=20&status=ALL
Authorization: Bearer <valid_jwt_for_DEFAULT_USER_ID>
```

**Steps**:
1. Tạo 2 notifications cho `DEFAULT_USER_ID` (1 isRead=true, 1 isRead=false)
2. Gửi GET request với JWT hợp lệ và params trên
3. Assert response

**Expected Output**:
```json
{
  "success": true,
  "data": {
    "content": [ /* 2 items */ ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "unreadCount": 1
  }
}
```

**Assertions**:
- HTTP Status: 200 OK
- `data.totalElements` == 2
- `data.content` is array with 2 elements
- `data.unreadCount` == 1 (chỉ 1 notification chưa đọc)
- `data.page` == 0
- `data.size` == 20
- Mỗi item có: `notificationId`, `title`, `body`, `category`, `isRead`, `createdAt`
- Items sắp xếp giảm dần theo `createdAt`

**Unit Test Code**:
```java
@Test
@DisplayName("NOTIF-TC-011-001: Happy path — ALL notifications returned with correct pagination")
void getNotifications_happyPath_returnsAllWithPagination() {
    // Arrange
    UUID userId = NotificationTestFactory.DEFAULT_USER_ID;
    List<Notification> notifications = NotificationTestFactory.makeNotificationList(2, userId);
    Page<Notification> page = new PageImpl<>(notifications, PageRequest.of(0, 20), 2);
    NotificationListRequest request = NotificationTestFactory.makeRequest(null); // defaults: ALL, page=0, size=20

    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
        .thenReturn(page);
    when(notificationRepository.countUnreadByUserId(userId)).thenReturn(1L);
    when(notificationMapper.toResponseList(notifications)).thenReturn(List.of(
        NotificationResponse.builder().notificationId(notifications.get(0).getNotificationId()).build(),
        NotificationResponse.builder().notificationId(notifications.get(1).getNotificationId()).build()
    ));

    // Act
    PaginatedResponse<NotificationResponse> result = notificationService.getNotifications(userId, request);

    // Assert
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getUnreadCount()).isEqualTo(1L);
    assertThat(result.getPage()).isEqualTo(0);
    assertThat(result.getSize()).isEqualTo(20);
    verify(auditService).emit(any(AuditEvent.class));
}
```

---

### NOTIF-TC-011-002 — GET với filter status=UNREAD

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-002                                                    |
| **Tên**           | Filter UNREAD — chỉ trả về notification chưa đọc                   |
| **Level**         | Unit (Service)                                                      |
| **Priority**      | P1 — High                                                           |
| **Condition**     | TC-COND-01, TC-COND-06                                              |
| **Precondition**  | User có 3 notifications: 2 isRead=false, 1 isRead=true             |

**Input**:
```
GET /api/v1/notifications?page=0&size=20&status=UNREAD
Authorization: Bearer <valid_jwt>
```

**Expected Output**:
```json
{
  "success": true,
  "data": {
    "content": [ /* 2 items, all isRead=false */ ],
    "totalElements": 2,
    "unreadCount": 2
  }
}
```

**Assertions**:
- HTTP Status: 200 OK
- `data.totalElements` == 2
- Tất cả items trong `content` có `isRead == false`
- Repository gọi `findByUserIdAndIsReadOrderByCreatedAtDesc(userId, false, pageable)` — KHÔNG gọi `findByUserIdOrderByCreatedAtDesc`

**Unit Test Code**:
```java
@Test
@DisplayName("NOTIF-TC-011-002: Filter UNREAD — only unread notifications returned")
void getNotifications_filterUnread_returnsOnlyUnreadRecords() {
    // Arrange
    UUID userId = NotificationTestFactory.DEFAULT_USER_ID;
    List<Notification> unreadNotifications = List.of(
        NotificationTestFactory.makeNotification(n -> n.setRead(false)),
        NotificationTestFactory.makeNotification(n -> n.setRead(false))
    );
    Page<Notification> page = new PageImpl<>(unreadNotifications, PageRequest.of(0, 20), 2);
    NotificationListRequest request = NotificationTestFactory.makeRequest(r -> r.setStatus(NotificationStatus.UNREAD));

    when(notificationRepository.findByUserIdAndIsReadOrderByCreatedAtDesc(eq(userId), eq(false), any(Pageable.class)))
        .thenReturn(page);
    when(notificationRepository.countUnreadByUserId(userId)).thenReturn(2L);
    when(notificationMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());

    // Act
    PaginatedResponse<NotificationResponse> result = notificationService.getNotifications(userId, request);

    // Assert
    assertThat(result.getTotalElements()).isEqualTo(2);
    assertThat(result.getUnreadCount()).isEqualTo(2L);
    verify(notificationRepository).findByUserIdAndIsReadOrderByCreatedAtDesc(eq(userId), eq(false), any(Pageable.class));
    verify(notificationRepository, never()).findByUserIdOrderByCreatedAtDesc(any(), any());
}
```

---

### NOTIF-TC-011-003 — GET với page params không hợp lệ → 400

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-003                                                    |
| **Tên**           | Invalid pagination params — size=999 → 400 NOTIF-011               |
| **Level**         | Unit (Validation) + Integration (Controller)                        |
| **Priority**      | P1 — High                                                           |
| **Condition**     | TC-COND-04                                                          |

**Input Variants** (Boundary Value Analysis):

| Sub-test | Input            | Expected          |
|----------|------------------|-------------------|
| 003-a    | size=0           | 400, NOTIF-011    |
| 003-b    | size=101         | 400, NOTIF-011    |
| 003-c    | size=999         | 400, NOTIF-011    |
| 003-d    | page=-1          | 400, NOTIF-011    |
| 003-e    | size=1 (valid)   | 200 OK            |
| 003-f    | size=100 (valid) | 200 OK            |

**Expected Output (400)**:
```json
{
  "success": false,
  "code": "NOTIF-011",
  "message": "Invalid pagination parameters",
  "errors": [
    { "field": "size", "message": "Page size must be <= 100" }
  ]
}
```

**Unit Test Code**:
```java
@Test
@DisplayName("NOTIF-TC-011-003: size=999 → ConstraintViolationException → 400")
void getNotifications_invalidSize_throwsConstraintViolation() {
    // Arrange: use @Validated on service or test via MockMvc controller layer
    mockMvc.perform(get("/api/v1/notifications?size=999")
            .header("Authorization", "Bearer " + validJwt))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("NOTIF-011"));
}

@Test
@DisplayName("NOTIF-TC-011-003d: page=-1 → 400")
void getNotifications_negativePage_returns400() {
    mockMvc.perform(get("/api/v1/notifications?page=-1")
            .header("Authorization", "Bearer " + validJwt))
        .andExpect(status().isBadRequest());
}
```

---

### NOTIF-TC-011-004 — Ownership: không thể xem notification của user khác

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-004                                                    |
| **Tên**           | Ownership enforcement — service chỉ query theo userId từ JWT       |
| **Level**         | Unit (Service)                                                      |
| **Priority**      | P0 — Critical (Security)                                            |
| **Condition**     | TC-COND-01, TC-COND-08                                              |
| **Precondition**  | Tồn tại notifications cho `OTHER_USER_ID` trong DB                 |

**Test Scenario**:
- User A (DEFAULT_USER_ID) gọi API với JWT hợp lệ của mình
- DB có notifications thuộc OTHER_USER_ID
- Verify rằng service CHỈ query với `DEFAULT_USER_ID`, không bao giờ query với `OTHER_USER_ID`

**Expected Behavior**:
- Service layer luôn dùng `userId` từ `SecurityUtils.requireCurrentUserId(principal)`, không nhận `userId` từ request param
- Repository không được gọi với `OTHER_USER_ID`

**Unit Test Code**:
```java
@Test
@DisplayName("NOTIF-TC-011-004: Ownership — repository only queried with current user's ID")
void getNotifications_ownershipEnforced_repositoryCalledWithCurrentUserIdOnly() {
    // Arrange
    UUID currentUserId = NotificationTestFactory.DEFAULT_USER_ID;
    UUID otherUserId = NotificationTestFactory.OTHER_USER_ID;
    NotificationListRequest request = NotificationTestFactory.makeRequest(null);

    when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(currentUserId), any(Pageable.class)))
        .thenReturn(Page.empty());
    when(notificationRepository.countUnreadByUserId(currentUserId)).thenReturn(0L);
    when(notificationMapper.toResponseList(anyList())).thenReturn(Collections.emptyList());

    // Act
    notificationService.getNotifications(currentUserId, request);

    // Assert: repository never called with otherUserId
    verify(notificationRepository, never())
        .findByUserIdOrderByCreatedAtDesc(eq(otherUserId), any(Pageable.class));
    verify(notificationRepository, never())
        .findByUserIdAndIsReadOrderByCreatedAtDesc(eq(otherUserId), anyBoolean(), any(Pageable.class));

    // Assert: repository called exactly once with currentUserId
    verify(notificationRepository, times(1))
        .findByUserIdOrderByCreatedAtDesc(eq(currentUserId), any(Pageable.class));
}
```

---

### NOTIF-TC-011-005 — Không có JWT → 401

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-005                                                    |
| **Tên**           | No JWT → 401 Unauthorized (NOTIF-010)                              |
| **Level**         | Integration (Spring Security filter chain)                          |
| **Priority**      | P0 — Critical (Security)                                            |
| **Condition**     | TC-COND-02                                                          |

**Input Variants**:

| Sub-test | Input                              | Expected      |
|----------|------------------------------------|---------------|
| 005-a    | No Authorization header            | 401 NOTIF-010 |
| 005-b    | Authorization: Bearer (empty)      | 401 NOTIF-010 |
| 005-c    | Authorization: Bearer invalid.jwt  | 401 NOTIF-010 |
| 005-d    | Authorization: Bearer expired.jwt  | 401 NOTIF-010 |

**Expected Output**:
```json
{
  "success": false,
  "code": "NOTIF-010",
  "message": "Authentication required"
}
```

**Integration Test Code**:
```java
@Test
@DisplayName("NOTIF-TC-011-005a: No Authorization header → 401")
void getNotifications_noJwt_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/notifications"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("NOTIF-010"));
}

@Test
@DisplayName("NOTIF-TC-011-005c: Invalid JWT → 401")
void getNotifications_invalidJwt_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/notifications")
            .header("Authorization", "Bearer this.is.invalid"))
        .andExpect(status().isUnauthorized());
}
```

---

### NOTIF-TC-011-INT-001 — Integration: DB trả về count đúng

| Field             | Value                                                               |
|-------------------|---------------------------------------------------------------------|
| **Test Case ID**  | NOTIF-TC-011-INT-001                                               |
| **Tên**           | Integration — DB count khớp với response totalElements             |
| **Level**         | Integration (@SpringBootTest + Testcontainers/H2)                  |
| **Priority**      | P1 — High                                                           |
| **Condition**     | TC-COND-01, TC-COND-05, TC-COND-10                                 |
| **Precondition**  | Flyway V2 đã apply, bảng `notifications` tồn tại                  |

**Test Scenario**:
1. Insert 3 notifications vào DB cho `DEFAULT_USER_ID` (2 UNREAD, 1 READ)
2. Insert 2 notifications cho `OTHER_USER_ID`
3. Call GET /api/v1/notifications?status=ALL với JWT của `DEFAULT_USER_ID`
4. Verify response chỉ trả về 3 notifications

**Expected Output**:
```json
{
  "data": {
    "totalElements": 3,
    "content": [ /* 3 items, all belong to DEFAULT_USER_ID */ ],
    "unreadCount": 2
  }
}
```

**Integration Test Code**:
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired INotificationRepository notificationRepository;
    @Autowired JwtTestHelper jwtHelper;

    @Test
    @DisplayName("NOTIF-TC-011-INT-001: DB returns correct count — 3 notifications for current user, 2 for other")
    void getNotifications_integration_dbReturnsCorrectCount() throws Exception {
        // Arrange: insert test data
        UUID defaultUserId = NotificationTestFactory.DEFAULT_USER_ID;
        UUID otherUserId = NotificationTestFactory.OTHER_USER_ID;

        notificationRepository.saveAll(NotificationTestFactory.makeNotificationList(3, defaultUserId));
        notificationRepository.saveAll(NotificationTestFactory.makeNotificationList(2, otherUserId));

        // Mark first notification as read
        Notification firstNotif = notificationRepository
            .findByUserIdOrderByCreatedAtDesc(defaultUserId, PageRequest.of(0, 1)).getContent().get(0);
        firstNotif.setRead(true);
        notificationRepository.save(firstNotif);

        String jwt = jwtHelper.generateToken(defaultUserId, Role.ROLE_MOTHER);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications?page=0&size=20&status=ALL")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(3))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(3))
            .andExpect(jsonPath("$.data.unreadCount").value(2))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    @DisplayName("NOTIF-TC-011-INT-001b: Empty result for user with no notifications")
    void getNotifications_integration_emptyForNewUser() throws Exception {
        UUID newUserId = UUID.randomUUID();
        String jwt = jwtHelper.generateToken(newUserId, Role.ROLE_MOTHER);

        mockMvc.perform(get("/api/v1/notifications")
                .header("Authorization", "Bearer " + jwt))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content").isEmpty())
            .andExpect(jsonPath("$.data.unreadCount").value(0));
    }
}
```

---

## 5. Red-Green-Refactor Tracker

### 5.1 Bảng Trạng thái

| Test Case ID          | Tên ngắn                                 | 🔴 Red (Stub)  | 🟢 Green (Impl)  | 🔵 Refactor     |
|-----------------------|------------------------------------------|----------------|------------------|-----------------|
| NOTIF-TC-011-001      | Happy path GET all                       | Pending        | Pending          | Pending         |
| NOTIF-TC-011-002      | Filter UNREAD                            | Pending        | Pending          | Pending         |
| NOTIF-TC-011-003      | Invalid page params → 400                | Pending        | Pending          | Pending         |
| NOTIF-TC-011-003a     | size=0 → 400                             | Pending        | Pending          | Pending         |
| NOTIF-TC-011-003b     | size=101 → 400                           | Pending        | Pending          | Pending         |
| NOTIF-TC-011-003d     | page=-1 → 400                            | Pending        | Pending          | Pending         |
| NOTIF-TC-011-004      | Ownership enforcement                    | Pending        | Pending          | Pending         |
| NOTIF-TC-011-005      | No JWT → 401                             | Pending        | Pending          | Pending         |
| NOTIF-TC-011-005c     | Invalid JWT → 401                        | Pending        | Pending          | Pending         |
| NOTIF-TC-011-INT-001  | Integration DB count correct             | Pending        | Pending          | Pending         |
| NOTIF-TC-011-INT-001b | Empty result for new user                | Pending        | Pending          | Pending         |

> **Quy ước cột**: 🔴 = Test written, fails (no impl). 🟢 = Test passes (impl done). 🔵 = Refactor complete, tests still green.

### 5.2 Red Gate Protocol

**Mục đích**: Đảm bảo test được viết TRƯỚC implementation (TDD). Test phải fail đỏ trước khi viết bất kỳ dòng code nào.

**Stub để kích hoạt Red Gate**:

```java
// File: NotificationServiceImpl.java — STUB VERSION (Red Gate)
// Viết stub này TRƯỚC khi implement. Chạy test → phải thấy AssertionError/Exception đỏ.

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<NotificationResponse> getNotifications(
            UUID userId, NotificationListRequest request) {
        // RED GATE: throw để tất cả test fail đỏ
        throw new UnsupportedOperationException(
            "NOTIF-TC-011 RED GATE: NotificationServiceImpl.getNotifications() not yet implemented. " +
            "All tests MUST fail at this stage. Replace with real implementation only after tests are written."
        );
    }
}
```

**Checklist Red Gate**:
- [ ] Stub được commit với message `test(notif): RED GATE UC-11 notification service stub`
- [ ] Tất cả test cases (001–005, INT-001) fail với UnsupportedOperationException hoặc AssertionError
- [ ] Screenshot/log của màn hình đỏ được lưu làm bằng chứng
- [ ] Chỉ sau khi xác nhận đỏ, mới bắt đầu implement thực sự

**Chuyển sang Green**:
```bash
# Chạy test để verify red
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest="NotificationServiceImplTest,NotificationControllerIntegrationTest" \
  -Dsurefire.failIfNoSpecifiedTests=false

# Sau khi implement: verify green
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest="NotificationServiceImplTest,NotificationControllerIntegrationTest"
```

---

## 6. Entry / Exit Criteria

### 6.1 Entry Criteria (Điều kiện để bắt đầu test)

| Criterion                                                         | Verification Method                             |
|-------------------------------------------------------------------|-------------------------------------------------|
| Flyway V2 migration đã được apply thành công                     | `SELECT * FROM flyway_schema_history WHERE version='2'` |
| Tất cả source files trong §11 TDS đã compile không lỗi           | `./mvnw clean compile` exit code 0              |
| Test fixtures (NotificationTestFactory) đã được tạo              | File tồn tại trong test directory               |
| Integration test DB (H2 / Testcontainers) đã cấu hình            | `application-test.yml` có datasource config     |
| JWT test helper đã cấu hình cho môi trường test                  | `JwtTestHelper` bean available trong test context |

### 6.2 Exit Criteria (Điều kiện để kết thúc test thành công)

| Criterion                                               | Ngưỡng          | Current Status |
|---------------------------------------------------------|-----------------|----------------|
| Tất cả P0 test cases PASS                               | 100% (4/4 cases)| Pending        |
| Tất cả P1 test cases PASS                               | 100% (3/3 cases)| Pending        |
| Integration tests PASS                                   | 100% (2/2)      | Pending        |
| Test coverage (line) cho notification package           | ≥ 80%           | Pending        |
| Không có open Critical/High defects                     | 0 defects       | Pending        |
| Security: ownership test (TC-011-004) verified PASS     | Mandatory       | Pending        |
| Performance: P95 < 500ms trên môi trường dev            | Verified        | Pending        |

### 6.3 Suspension Criteria

Test bị tạm dừng khi:
- Flyway migration fail
- Spring Security config không load được
- DB connection không khả dụng
- JVM / Spring context fail to start

---

## 7. Rollback Plan

### 7.1 Test Rollback nếu Implementation Broken

```bash
# Nếu implementation gây regression:
# 1. Revert implementation commit
git revert <implementation-commit-hash>

# 2. Verify tests return to known-good state
./mvnw test -pl 05_Development/CareBridgeAPI \
  -Dtest="NotificationServiceImplTest"

# 3. Không revert migration V2 (bảng notifications không phá vỡ các UC khác)
# Migration V2 có thể giữ nguyên vì chỉ thêm bảng mới, không xóa/sửa bảng cũ
```

### 7.2 Database Rollback

```sql
-- CHỈ dùng trong môi trường DEV/TEST, KHÔNG dùng trong production
-- Nếu cần rollback bảng notifications hoàn toàn:

DROP INDEX IF EXISTS idx_notifications_user_unread;
DROP INDEX IF EXISTS idx_notifications_user_id;
DROP INDEX IF EXISTS idx_notifications_created_at;
DROP TABLE IF EXISTS notifications;

-- Đánh dấu migration V2 đã rollback trong Flyway history
DELETE FROM flyway_schema_history WHERE version = '2';
-- (Chú ý: chỉ làm điều này trong dev, Flyway không hỗ trợ rollback natively)
```

### 7.3 Rollback Decision Matrix

| Tình huống                                    | Hành động                                           |
|-----------------------------------------------|-----------------------------------------------------|
| Unit tests fail sau khi implement             | Debug implementation, KHÔNG revert migration        |
| Integration tests fail do DB schema sai       | Check Flyway migration, re-apply nếu cần            |
| Security tests fail (ownership bypass)        | Halt deployment, hotfix ngay lập tức, P0 escalate   |
| Performance tests fail (>500ms P95)           | Check query explain plan, add/fix index             |

---

## 8. CASE 2.0 Anti-Pattern Detection

### 8.1 Bảng Anti-Pattern

| AP ID      | Anti-Pattern Tên                        | Mô tả vấn đề                                                                      | Dấu hiệu nhận biết trong code                                   | Hành động khắc phục                                              |
|------------|-----------------------------------------|-----------------------------------------------------------------------------------|-----------------------------------------------------------------|------------------------------------------------------------------|
| AP-AI-001  | God Service / Business Logic in Controller | Controller chứa if/switch để xử lý business logic thay vì delegate cho Service. | `if (status == UNREAD) { repo.findUnread() }` trong Controller  | Extract tất cả logic sang `NotificationServiceImpl`              |
| AP-AI-002  | Entity Exposure                         | Trả về JPA Entity (`Notification`) trực tiếp trong API response.                 | `return ResponseEntity.ok(notificationRepository.findAll())`    | Bắt buộc dùng `NotificationMapper.toResponse()` và DTO          |
| AP-AI-003  | Hardcoded User Identity                 | Query DB với userId hardcoded hoặc lấy từ request body thay vì JWT.              | `repo.findByUserId(request.getUserId())`                        | Chỉ dùng `SecurityUtils.requireCurrentUserId(principal)`         |
| AP-AI-004  | Missing ReadOnly Transaction            | Service method GET không có `@Transactional(readOnly=true)`.                     | `@Transactional` (thiếu `readOnly=true`) hoặc không có gì       | Thêm `@Transactional(readOnly=true)` vào `getNotifications()`    |
| AP-AI-005  | Unbounded Pagination                    | Không có giới hạn trên cho `size`, cho phép client query hàng nghìn records.    | `size` không có `@Max` validation                               | Enforce `@Max(100)` trên `size` field trong `NotificationListRequest` |

### 8.2 Checklist Xác minh Anti-Pattern

Trước khi mark task là Complete, reviewer phải verify:

- [ ] **AP-AI-001**: `NotificationController.getNotifications()` chỉ gọi `notificationService.getNotifications()` — không có logic khác
- [ ] **AP-AI-002**: Return type của controller là `ApiResponse<PaginatedResponse<NotificationResponse>>` — không phải entity
- [ ] **AP-AI-003**: `userId` trong service call đến từ `SecurityUtils.requireCurrentUserId(principal)` — không từ request
- [ ] **AP-AI-004**: `NotificationServiceImpl.getNotifications()` có annotation `@Transactional(readOnly = true)`
- [ ] **AP-AI-005**: `NotificationListRequest.size` có `@Max(100)` và test NOTIF-TC-011-003 PASS

### 8.3 Props Isolation Checklist (CASE 2.0)

- [ ] Test data tạo qua `NotificationTestFactory.makeNotification(Consumer<Notification>)` — không tạo object trực tiếp trong test methods
- [ ] Không có hardcoded UUID literals trong test methods (dùng constants từ `NotificationTestFactory`)
- [ ] Mỗi test case độc lập (không phụ thuộc thứ tự chạy)
- [ ] `@Transactional` trên integration test class để rollback data sau mỗi test
- [ ] Mock objects reset giữa các tests (`@ExtendWith(MockitoExtension.class)` tự handle)

### 8.4 Security Anti-Patterns (Healthcare Context)

| SAP ID    | Security Anti-Pattern                               | Relevant to UC-11                             |
|-----------|-----------------------------------------------------|-----------------------------------------------|
| SAP-001   | Returning notifications of other users              | CRITICAL — xử lý bằng TC-011-004             |
| SAP-002   | Logging notification body content (PII)             | Chỉ log userId, count — không log body/title  |
| SAP-003   | Cache notifications across users                    | Không cache; response per-user từ DB          |
| SAP-004   | Exposing internal IDs that leak business data       | UUID (random) — không expose sequential IDs   |
