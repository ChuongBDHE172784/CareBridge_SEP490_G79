# Test Specification
## UC-12: Mark Notifications As Read

| Field            | Value                                      |
|------------------|--------------------------------------------|
| Document ID      | CB-NOTIF-IMP-012-TEST                      |
| Version          | 1.0                                        |
| Date             | 2026-06-26                                 |
| Status           | Draft                                      |
| Document Owner   | PhuongNT                                   |
| Author           | AI Agent                                   |
| Related TDS      | CB-NOTIF-IMP-012                           |
| Based on EDS     | v2.0                                       |

---

## 1. Tổng Quan Kiểm Thử

### 1.1 Mục tiêu

Tài liệu này định nghĩa đầy đủ các test case cho UC-12 MarkNotificationsAsRead, bao gồm:
- Unit tests cho Service và Repository logic
- API-level tests cho Controller endpoints
- Integration tests xác minh trạng thái Database

### 1.2 Phạm vi kiểm thử

| Loại                | Phạm vi                                                                 |
|---------------------|-------------------------------------------------------------------------|
| Unit Test           | `NotificationService.markAsRead()`, `NotificationService.markAllAsRead()` |
| Controller Test     | `PUT /api/v1/notifications/{id}/read`, `PUT /api/v1/notifications/read-all` |
| Integration Test    | DB state verification: `is_read`, `read_at`, `rowsAffected`            |
| Security Test       | JWT missing/invalid, cross-user ownership                               |

### 1.3 Ngoài phạm vi

- Push notification delivery (thuộc UC-10)
- Notification listing / pagination (thuộc UC-11)
- Performance load testing (covered by NFR-012-001, separate phase)

---

## 2. Logic Issues Resolved

Các vấn đề logic đã được phân tích và giải quyết trước khi viết test:

| Issue ID | Mô tả vấn đề                                               | Quyết định                                                                 | ADR         |
|----------|------------------------------------------------------------|----------------------------------------------------------------------------|-------------|
| L1       | Mark thông báo đã đọc — nên trả về lỗi hay thành công?   | **Idempotent**: trả về `200 OK`, không phải `400`/`409`. Không phát sinh lỗi. | ADR-012-002 |
| L2       | Thứ tự: ownership check trước hay sau UPDATE?             | **Ownership check PHẢI xảy ra TRƯỚC UPDATE** để ngăn TOCTOU race condition. Service phải gọi `findByNotificationIdAndUserId()` trước khi `markAsReadById()`. | TDS §8.3, C1 |
| L3       | `read_at` lấy từ client hay server?                       | **Server time** (`Instant.now()` tại Service layer). Client không được cung cấp timestamp. Ngăn chặn timestamp manipulation. | TDS §17, C5 |
| L4       | Audit emit khi mark thông báo đã đọc (idempotent call)?  | **Chỉ emit khi `affected > 0`**. Không emit audit noise cho idempotent calls. | TDS §8.3, C4 |

---

## 3. Test Infrastructure

### 3.1 Props Isolation — Test Factory

```java
/**
 * Factory class cung cấp test data chuẩn hóa cho UC-12.
 * Sử dụng fixed UUID để đảm bảo tính reproducible và isolation.
 */
class NotificationMarkTestFactory {

    static final UUID TEST_USER_ID   = UUID.fromString("00000000-0000-0000-0000-000000000012");
    static final UUID OTHER_USER_ID  = UUID.fromString("00000000-0000-0000-0000-000000000099");
    static final UUID TEST_NOTIF_ID  = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000012");
    static final UUID NOTIF_ID_READ  = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000012");
    static final UUID NOTIF_OTHER    = UUID.fromString("cccccccc-0000-0000-0000-000000000099");

    /** Tạo thông báo chưa đọc thuộc về TEST_USER_ID */
    static Notification makeUnreadNotification() {
        return Notification.builder()
                .notificationId(TEST_NOTIF_ID)
                .userId(TEST_USER_ID)
                .title("Test Notification")
                .body("Test body")
                .category(NotificationCategory.REMINDER)
                .isRead(false)
                .readAt(null)
                .createdAt(Instant.now())
                .build();
    }

    /** Tạo thông báo đã đọc (để test idempotency) */
    static Notification makeReadNotification() {
        Instant readTime = Instant.now().minusSeconds(3600);
        return Notification.builder()
                .notificationId(NOTIF_ID_READ)
                .userId(TEST_USER_ID)
                .title("Already Read Notification")
                .body("This was already read")
                .category(NotificationCategory.INFO)
                .isRead(true)
                .readAt(readTime)
                .createdAt(readTime.minusSeconds(60))
                .build();
    }

    /** Tạo thông báo thuộc về OTHER_USER_ID (để test cross-user ownership) */
    static Notification makeOtherUserNotification() {
        return Notification.builder()
                .notificationId(NOTIF_OTHER)
                .userId(OTHER_USER_ID)
                .title("Other User's Notification")
                .body("Not yours")
                .category(NotificationCategory.ALERT)
                .isRead(false)
                .readAt(null)
                .createdAt(Instant.now())
                .build();
    }

    /** Tạo list N thông báo chưa đọc cho TEST_USER_ID */
    static List<Notification> makeUnreadList(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> Notification.builder()
                        .notificationId(UUID.randomUUID())
                        .userId(TEST_USER_ID)
                        .title("Notification " + i)
                        .body("Body " + i)
                        .category(NotificationCategory.REMINDER)
                        .isRead(false)
                        .readAt(null)
                        .createdAt(Instant.now().minusSeconds(i * 60L))
                        .build())
                .collect(Collectors.toList());
    }
}
```

### 3.2 Red Gate Stubs — Giai Đoạn Red (TDD)

Các stub này được sử dụng trong **giai đoạn Red** của TDD cycle. Thay thế bằng implementation thực sự ở giai đoạn Green.

```java
/**
 * Red Phase stub — chứng minh tests fail trước khi có implementation.
 * Đặt trong test source set, KHÔNG deploy lên production.
 */
public class NotificationServiceRedStub implements INotificationService {

    @Override
    public void markAsRead(UUID userId, UUID notificationId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public int markAllAsRead(UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

```java
/**
 * Red Phase repository stub.
 */
public class NotificationRepositoryRedStub implements INotificationRepository {

    @Override
    public Optional<Notification> findByNotificationIdAndUserId(UUID notificationId, UUID userId) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public int markAsReadById(UUID notificationId, UUID userId, Instant readAt) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }

    @Override
    public int markAllAsReadByUserId(UUID userId, Instant readAt) {
        throw new UnsupportedOperationException("Not implemented — Red Phase stub");
    }
}
```

### 3.3 Dependencies và Annotations

```java
// Test class annotations
@ExtendWith(MockitoExtension.class)        // Unit tests
@SpringBootTest                             // Integration tests
@AutoConfigureMockMvc                       // Controller tests
@Transactional                              // Integration tests (auto-rollback)
@Sql("/test-data/notifications.sql")        // Load test data

// Mocks
@Mock INotificationRepository notificationRepository;
@Mock AuditService auditService;
@InjectMocks NotificationService notificationService;
```

---

## 4. Test Cases

### NOTIF-TC-012-001: Happy Path — Mark Single Notification As Read

| Field       | Value                                                                    |
|-------------|--------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-001                                                         |
| Priority    | P0 — Critical                                                            |
| Type        | Unit Test (Service) + Controller Test                                    |
| Covers      | FR-012-001, BR-NOTIF-OWN-012, C1, C2, C4                                |
| Preconditions | User authenticated; notification exists in DB with `is_read = false`   |

**Given / When / Then**:
- **Given**: Notification `TEST_NOTIF_ID` tồn tại, thuộc `TEST_USER_ID`, `is_read = false`
- **When**: `PUT /api/v1/notifications/aaaaaaaa-0000-0000-0000-000000000012/read`
- **Then**:
  - HTTP status: `200 OK`
  - Response: `{ "success": true, "data": null }`
  - Repository: `markAsReadById()` được gọi đúng 1 lần với `(TEST_NOTIF_ID, TEST_USER_ID, Instant)`
  - Audit: `auditService.emit("NOTIFICATIONS_READ", ...)` được gọi đúng 1 lần
  - DB: `is_read = true`, `read_at IS NOT NULL`

**Unit Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-001: Mark unread notification → 200 OK, audit emitted")
void markAsRead_unreadNotification_success() {
    // Given
    Notification unread = NotificationMarkTestFactory.makeUnreadNotification();
    when(notificationRepository.findByNotificationIdAndUserId(
            TEST_NOTIF_ID, TEST_USER_ID))
        .thenReturn(Optional.of(unread));
    when(notificationRepository.markAsReadById(
            eq(TEST_NOTIF_ID), eq(TEST_USER_ID), any(Instant.class)))
        .thenReturn(1);

    // When
    notificationService.markAsRead(TEST_USER_ID, TEST_NOTIF_ID);

    // Then
    verify(notificationRepository, times(1))
        .markAsReadById(eq(TEST_NOTIF_ID), eq(TEST_USER_ID), any(Instant.class));
    verify(auditService, times(1))
        .emit(eq("NOTIFICATIONS_READ"), eq(TEST_USER_ID), any(Map.class));
}
```

**Controller Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-001: PUT /{id}/read → 200 OK")
@WithMockUser(username = "00000000-0000-0000-0000-000000000012", roles = "MOTHER")
void markSingleAsRead_validRequest_returns200() throws Exception {
    doNothing().when(notificationService)
        .markAsRead(eq(TEST_USER_ID), eq(TEST_NOTIF_ID));

    mockMvc.perform(put("/api/v1/notifications/{id}/read", TEST_NOTIF_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
}
```

---

### NOTIF-TC-012-002: Mark Already-Read Notification → 200 (Idempotent)

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-002                                                             |
| Priority    | P1 — High                                                                    |
| Type        | Unit Test (Service)                                                          |
| Covers      | BR-NOTIF-IDEMP-012, ADR-012-002, L1                                         |
| Preconditions | Notification exists with `is_read = true`                                  |

**Given / When / Then**:
- **Given**: Notification `NOTIF_ID_READ` tồn tại, thuộc `TEST_USER_ID`, `is_read = true`
- **When**: `notificationService.markAsRead(TEST_USER_ID, NOTIF_ID_READ)` được gọi
- **Then**:
  - Không có exception được ném
  - `markAsReadById()` được gọi (trả về 0 vì `AND is_read = false` trong WHERE)
  - `auditService.emit()` **KHÔNG** được gọi (vì affected = 0)
  - Không có thay đổi DB nào xảy ra

**Unit Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-002: Already-read notification → idempotent, no error, no audit")
void markAsRead_alreadyRead_idempotentNoError() {
    // Given
    Notification alreadyRead = NotificationMarkTestFactory.makeReadNotification();
    when(notificationRepository.findByNotificationIdAndUserId(
            NOTIF_ID_READ, TEST_USER_ID))
        .thenReturn(Optional.of(alreadyRead));
    when(notificationRepository.markAsReadById(
            eq(NOTIF_ID_READ), eq(TEST_USER_ID), any(Instant.class)))
        .thenReturn(0); // WHERE is_read = false → no rows matched

    // When — must NOT throw
    assertDoesNotThrow(() ->
        notificationService.markAsRead(TEST_USER_ID, NOTIF_ID_READ));

    // Then — no audit event for idempotent call
    verify(auditService, never())
        .emit(anyString(), any(UUID.class), any(Map.class));
}
```

---

### NOTIF-TC-012-003: Mark Notification Belonging to Another User → 404 NOTIF-020

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-003                                                             |
| Priority    | P0 — Critical                                                                |
| Type        | Unit Test (Service) + Controller Test                                        |
| Covers      | BR-NOTIF-OWN-012, FR-012-003, C1, Security                                  |
| Preconditions | Notification exists but belongs to `OTHER_USER_ID`                         |

**Given / When / Then**:
- **Given**: Notification `NOTIF_OTHER` tồn tại nhưng thuộc `OTHER_USER_ID`
- **When**: `TEST_USER_ID` gọi mark-as-read cho `NOTIF_OTHER`
- **Then**:
  - `NotificationNotFoundException` được ném (mã `NOTIF-020`)
  - HTTP status: `404 Not Found`
  - Response body: `{ "errorCode": "NOTIF-020" }`
  - `markAsReadById()` **KHÔNG** được gọi (ownership fail trước)
  - Không có audit event

**Unit Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-003: Cross-user ownership → 404 NOTIF-020, no DB update")
void markAsRead_wrongOwner_throws404() {
    // Given — repository returns empty because userId doesn't match
    when(notificationRepository.findByNotificationIdAndUserId(
            NOTIF_OTHER, TEST_USER_ID))
        .thenReturn(Optional.empty());

    // When / Then
    NotificationNotFoundException ex = assertThrows(
        NotificationNotFoundException.class,
        () -> notificationService.markAsRead(TEST_USER_ID, NOTIF_OTHER));

    assertThat(ex.getErrorCode()).isEqualTo("NOTIF-020");

    // Critical: markAsReadById must NOT be called (ownership checked first — C1)
    verify(notificationRepository, never())
        .markAsReadById(any(), any(), any());
    verify(auditService, never())
        .emit(anyString(), any(), any());
}
```

**Controller Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-003: Wrong owner → 404 with NOTIF-020 error code")
@WithMockUser(username = "00000000-0000-0000-0000-000000000012", roles = "MOTHER")
void markSingleAsRead_wrongOwner_returns404() throws Exception {
    doThrow(new NotificationNotFoundException("NOTIF-020", "Not found or not owned"))
        .when(notificationService).markAsRead(any(), any());

    mockMvc.perform(put("/api/v1/notifications/{id}/read", NOTIF_OTHER))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("NOTIF-020"))
        .andExpect(jsonPath("$.success").value(false));
}
```

---

### NOTIF-TC-012-004: Invalid UUID Format → 400 NOTIF-021

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-004                                                             |
| Priority    | P1 — High                                                                    |
| Type        | Controller Test                                                              |
| Covers      | FR-012-006, NOTIF-021                                                        |
| Preconditions | N/A                                                                        |

**Given / When / Then**:
- **Given**: Request với `notificationId = "not-a-valid-uuid"`
- **When**: `PUT /api/v1/notifications/not-a-valid-uuid/read`
- **Then**:
  - HTTP status: `400 Bad Request`
  - Response body: `{ "errorCode": "NOTIF-021", "success": false }`
  - Service layer không được gọi

**Controller Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-004: Non-UUID path variable → 400 NOTIF-021")
@WithMockUser(roles = "MOTHER")
void markSingleAsRead_invalidUUID_returns400() throws Exception {
    mockMvc.perform(put("/api/v1/notifications/{id}/read", "not-a-valid-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("NOTIF-021"))
        .andExpect(jsonPath("$.success").value(false));

    // Service must not be invoked
    verifyNoInteractions(notificationService);
}
```

**Exception Handler Mapping** (GlobalExceptionHandler):

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
        MethodArgumentTypeMismatchException ex) {
    if (ex.getRequiredType() == UUID.class) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error("NOTIF-021", "Invalid notification ID format"));
    }
    return ResponseEntity.badRequest().body(ApiResponse.error("VAL-001", ex.getMessage()));
}
```

---

### NOTIF-TC-012-005: Mark All As Read — Marks All User's Unread Notifications

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-005                                                             |
| Priority    | P0 — Critical                                                                |
| Type        | Unit Test (Service) + Controller Test                                        |
| Covers      | FR-012-002, BR-NOTIF-AUDIT-012, C2, C4                                      |
| Preconditions | User has multiple unread notifications                                      |

**Given / When / Then**:
- **Given**: `TEST_USER_ID` có 3 thông báo chưa đọc trong DB
- **When**: `PUT /api/v1/notifications/read-all`
- **Then**:
  - HTTP status: `200 OK`
  - Response body: `{ "success": true, "data": { "markedCount": 3 } }`
  - `markAllAsReadByUserId()` được gọi đúng 1 lần
  - `auditService.emit()` được gọi với `count = 3`
  - DB: tất cả 3 thông báo có `is_read = true`, `read_at IS NOT NULL`

**Unit Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-005: Mark all → 3 notifications marked, audit with count=3")
void markAllAsRead_threeUnread_returnsCount3() {
    // Given
    when(notificationRepository.markAllAsReadByUserId(
            eq(TEST_USER_ID), any(Instant.class)))
        .thenReturn(3);

    // When
    int result = notificationService.markAllAsRead(TEST_USER_ID);

    // Then
    assertThat(result).isEqualTo(3);
    verify(notificationRepository, times(1))
        .markAllAsReadByUserId(eq(TEST_USER_ID), any(Instant.class));
    verify(auditService, times(1))
        .emit(eq("NOTIFICATIONS_READ"), eq(TEST_USER_ID),
              argThat(map -> Integer.valueOf(3).equals(map.get("count"))
                          && "all".equals(map.get("scope"))));
}
```

**Controller Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-005: PUT /read-all → 200 OK with markedCount")
@WithMockUser(username = "00000000-0000-0000-0000-000000000012", roles = "MOTHER")
void markAllAsRead_validRequest_returns200WithCount() throws Exception {
    when(notificationService.markAllAsRead(any(UUID.class))).thenReturn(3);

    mockMvc.perform(put("/api/v1/notifications/read-all"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.markedCount").value(3));
}
```

**Edge Case — No Unread Notifications**:

```java
@Test
@DisplayName("NOTIF-TC-012-005b: Mark all → no unread notifications → markedCount=0, no audit")
void markAllAsRead_noneUnread_returns0NoAudit() {
    when(notificationRepository.markAllAsReadByUserId(any(), any())).thenReturn(0);

    int result = notificationService.markAllAsRead(TEST_USER_ID);

    assertThat(result).isEqualTo(0);
    verify(auditService, never()).emit(anyString(), any(), any());
}
```

---

### NOTIF-TC-012-006: No JWT → 401 Unauthorized

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-006                                                             |
| Priority    | P0 — Critical                                                                |
| Type        | Security / Controller Test                                                   |
| Covers      | FR-012-005, NFR-012-004                                                      |
| Preconditions | N/A                                                                        |

**Given / When / Then**:
- **Given**: Request không có `Authorization` header
- **When**:
  - `PUT /api/v1/notifications/{id}/read` (không có JWT)
  - `PUT /api/v1/notifications/read-all` (không có JWT)
- **Then**:
  - HTTP status: `401 Unauthorized`
  - Service layer không được gọi

**Controller Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-006a: No JWT → 401 for mark-single")
void markSingle_noJwt_returns401() throws Exception {
    mockMvc.perform(put("/api/v1/notifications/{id}/read", TEST_NOTIF_ID))
        // No @WithMockUser → anonymous request
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(notificationService);
}

@Test
@DisplayName("NOTIF-TC-012-006b: No JWT → 401 for mark-all")
void markAll_noJwt_returns401() throws Exception {
    mockMvc.perform(put("/api/v1/notifications/read-all"))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(notificationService);
}
```

---

### NOTIF-TC-012-INT-001: Integration — Verify is_read=true AND read_at IS NOT NULL in DB

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-INT-001                                                         |
| Priority    | P0 — Critical                                                                |
| Type        | Integration Test (`@SpringBootTest`)                                         |
| Covers      | ADR-012-003, C2, NFR-012-003                                                 |
| Preconditions | PostgreSQL test DB running; test notification seeded                       |

**Given / When / Then**:
- **Given**: Notification `TEST_NOTIF_ID` trong DB với `is_read = false`, `read_at = NULL`
- **When**: `notificationService.markAsRead(TEST_USER_ID, TEST_NOTIF_ID)` gọi thực sự vào DB
- **Then**:
  - DB record: `is_read = true`
  - DB record: `read_at IS NOT NULL`
  - DB record: `read_at >= timestamp_before_call AND read_at <= timestamp_after_call`
  - **KHÔNG** tồn tại trạng thái `is_read = true` với `read_at IS NULL`

**Integration Test Code**:

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql(scripts = "/test-data/seed-notifications-uc12.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class NotificationMarkIntegrationTest {

    @Autowired
    private INotificationService notificationService;

    @Autowired
    private INotificationRepository notificationRepository;

    @Test
    @DisplayName("NOTIF-TC-012-INT-001: After markAsRead → is_read=true AND read_at NOT NULL in DB")
    void markAsRead_dbStateVerification() {
        // Arrange
        Instant before = Instant.now().minusMillis(100);

        // Act
        notificationService.markAsRead(TEST_USER_ID, TEST_NOTIF_ID);

        Instant after = Instant.now().plusMillis(100);

        // Assert DB state
        Notification updated = notificationRepository
            .findByNotificationIdAndUserId(TEST_NOTIF_ID, TEST_USER_ID)
            .orElseThrow();

        // C2: Both fields must be updated atomically
        assertThat(updated.isRead()).isTrue();
        assertThat(updated.getReadAt()).isNotNull();

        // C5: read_at must be server time (within test window)
        assertThat(updated.getReadAt()).isAfterOrEqualTo(before);
        assertThat(updated.getReadAt()).isBeforeOrEqualTo(after);
    }

    @Test
    @DisplayName("NOTIF-TC-012-INT-001b: Consistency check — no is_read=true with NULL read_at")
    void markAsRead_noInconsistentState() {
        // Act
        notificationService.markAsRead(TEST_USER_ID, TEST_NOTIF_ID);

        // Assert global consistency (no half-updated records)
        long inconsistentCount = notificationRepository
            .countByIsReadTrueAndReadAtIsNull();
        assertThat(inconsistentCount).isZero();
    }
}
```

**SQL Seed File** (`/test-data/seed-notifications-uc12.sql`):

```sql
-- Seed for NOTIF-TC-012-INT-001
INSERT INTO notifications (notification_id, user_id, title, body, category, is_read, read_at, created_at, updated_at)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000012',
   '00000000-0000-0000-0000-000000000012',
   'Test Notification', 'Test body', 'REMINDER',
   false, NULL, NOW(), NOW());
```

---

### NOTIF-TC-012-INT-002: Integration — Mark-All: Verify Count of Affected Rows

| Field       | Value                                                                        |
|-------------|------------------------------------------------------------------------------|
| TC ID       | NOTIF-TC-012-INT-002                                                         |
| Priority    | P0 — Critical                                                                |
| Type        | Integration Test (`@SpringBootTest`)                                         |
| Covers      | FR-012-002, NFR-012-003, ADR-012-003                                        |
| Preconditions | PostgreSQL test DB running; 5 unread notifications seeded for TEST_USER_ID |

**Given / When / Then**:
- **Given**: `TEST_USER_ID` có đúng 5 thông báo `is_read = false` trong DB
- **When**: `notificationService.markAllAsRead(TEST_USER_ID)` được gọi
- **Then**:
  - Return value = 5
  - `markedCount` trong response = 5
  - Tất cả 5 records trong DB: `is_read = true`, `read_at IS NOT NULL`
  - Số thông báo `is_read = false` của user = 0

**Integration Test Code**:

```java
@Test
@DisplayName("NOTIF-TC-012-INT-002: Mark-all → correct rowsAffected and DB state")
@Sql(scripts = "/test-data/seed-5-unread-uc12.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
void markAllAsRead_fiveUnread_affectsExactly5Rows() {
    // Act
    int affected = notificationService.markAllAsRead(TEST_USER_ID);

    // Assert count
    assertThat(affected).isEqualTo(5);

    // Assert DB state: no unread left
    long remaining = notificationRepository
        .countByUserIdAndIsReadFalse(TEST_USER_ID);
    assertThat(remaining).isZero();

    // Assert all have read_at set
    List<Notification> allNotifications = notificationRepository
        .findAllByUserId(TEST_USER_ID);
    assertThat(allNotifications)
        .allSatisfy(n -> {
            assertThat(n.isRead()).isTrue();
            assertThat(n.getReadAt()).isNotNull();
        });
}
```

**SQL Seed File** (`/test-data/seed-5-unread-uc12.sql`):

```sql
-- Seed 5 unread notifications for NOTIF-TC-012-INT-002
DO $$
DECLARE
  i INT;
BEGIN
  FOR i IN 1..5 LOOP
    INSERT INTO notifications (notification_id, user_id, title, body, category, is_read, read_at, created_at, updated_at)
    VALUES (
      gen_random_uuid(),
      '00000000-0000-0000-0000-000000000012',
      'Bulk Notification ' || i,
      'Body ' || i,
      'REMINDER',
      false, NULL,
      NOW() - INTERVAL '1 minute' * i,
      NOW() - INTERVAL '1 minute' * i
    );
  END LOOP;
END $$;
```

---

## 5. Test Matrix (Tổng Hợp)

| TC ID                    | Type        | Endpoint / Method              | Expected Result             | Priority | L1 | L2 | L3 | L4 |
|--------------------------|-------------|--------------------------------|-----------------------------|----------|----|----|----|----|
| NOTIF-TC-012-001         | Unit + API  | PUT /{id}/read (unread)        | 200 OK, audit emitted       | P0       |    | X  | X  | X  |
| NOTIF-TC-012-002         | Unit        | markAsRead (already read)      | 200 OK, no audit, no error  | P1       | X  |    |    | X  |
| NOTIF-TC-012-003         | Unit + API  | PUT /{id}/read (wrong owner)   | 404 NOTIF-020               | P0       |    | X  |    |    |
| NOTIF-TC-012-004         | API         | PUT /bad-uuid/read             | 400 NOTIF-021               | P1       |    |    |    |    |
| NOTIF-TC-012-005         | Unit + API  | PUT /read-all                  | 200 OK, markedCount=N       | P0       |    |    | X  | X  |
| NOTIF-TC-012-006         | Security    | PUT /{id}/read (no JWT)        | 401 Unauthorized            | P0       |    |    |    |    |
| NOTIF-TC-012-INT-001     | Integration | markAsRead → DB                | is_read=true, read_at!=NULL | P0       |    |    | X  |    |
| NOTIF-TC-012-INT-002     | Integration | markAllAsRead → DB             | count=5, all marked         | P0       |    |    | X  |    |

> Cột L1-L4 đánh dấu test case nào validates logic issue tương ứng trong §2.

---

## 6. Definition of Done (DoD)

UC-12 được coi là **Done** khi tất cả điều kiện sau được thỏa mãn:

### 6.1 Code Completeness

```
[ ] INotificationRepository có 3 methods mới (findByNotificationIdAndUserId,
    markAsReadById, markAllAsReadByUserId) với @Query và @Modifying
[ ] INotificationService có 2 methods mới (markAsRead, markAllAsRead)
[ ] NotificationService implement đầy đủ logic với ownership check và audit emit
[ ] NotificationController có 2 endpoints mới (PUT /{id}/read, PUT /read-all)
[ ] MarkAllReadResponse DTO được tạo
[ ] GlobalExceptionHandler xử lý NOTIF-020 (404) và NOTIF-021 (400)
```

### 6.2 Test Coverage

```
[ ] Tất cả 8 test cases (NOTIF-TC-012-001 đến NOTIF-TC-012-INT-002) pass
[ ] Line coverage ≥ 85% cho NotificationService (markAsRead, markAllAsRead)
[ ] Branch coverage: cả happy path và error path được cover
[ ] Không có test nào bị skip (@Disabled) trừ khi có documented reason
```

### 6.3 Quality Gates

```
[ ] mvn test chạy thành công (exit code 0)
[ ] Không có @SneakyThrows hoặc empty catch blocks trong production code
[ ] Checkstyle / SpotBugs không có violation mức BLOCKER
[ ] is_read + read_at consistency check (NOTIF-TC-012-INT-001b) pass
```

### 6.4 Documentation

```
[ ] Javadoc đầy đủ cho INotificationService methods
[ ] API response examples trong Swagger/OpenAPI annotations
[ ] Error codes NOTIF-020, NOTIF-021 được document trong GlobalExceptionHandler
```

---

## 7. Rủi Ro và Giảm Thiểu

| Risk ID | Mô tả rủi ro                                                                | Xác suất | Mức độ | Giảm thiểu                                                                    |
|---------|-----------------------------------------------------------------------------|-----------|--------|-------------------------------------------------------------------------------|
| R-001   | Đánh dấu sai user's notification do thiếu ownership check                  | Thấp      | Cao    | C1 constraint + TC-003 test; ownership check bắt buộc trong WHERE clause     |
| R-002   | `is_read = true` nhưng `read_at IS NULL` (inconsistent state)              | Thấp      | Cao    | ADR-012-003 + INT-001b consistency test; single UPDATE cho cả hai fields     |
| R-003   | Client retry gây lỗi khi thông báo đã đọc                                 | Trung bình | Thấp  | Idempotent design (ADR-012-002) + TC-002 test                                |
| R-004   | mark-all chạy chậm khi user có hàng ngàn thông báo                        | Thấp      | Trung  | Index `idx_notifications_user_unread` trên `(user_id) WHERE is_read = false` |
| R-005   | Audit bị bỏ qua khi service throw exception trước khi emit                | Thấp      | Trung  | Đảm bảo audit.emit() sau khi UPDATE thành công, trong @Transactional block  |
| R-006   | Client cung cấp timestamp không đáng tin cho `read_at`                    | Thấp      | Thấp   | C5 constraint: server-side `Instant.now()` chỉ, không nhận từ client        |

---

## 8. Phụ Lục

### 8.1 Danh sách File Cần Tạo/Sửa đổi

| File Path                                                                                          | Hành động | Mô tả                                      |
|----------------------------------------------------------------------------------------------------|-----------|---------------------------------------------|
| `com/.../notification/repository/INotificationRepository.java`                                     | MODIFY    | Thêm 3 query methods mới                   |
| `com/.../notification/service/INotificationService.java`                                           | MODIFY    | Thêm 2 interface methods                   |
| `com/.../notification/service/NotificationService.java`                                            | MODIFY    | Implement markAsRead, markAllAsRead        |
| `com/.../notification/controller/NotificationController.java`                                      | MODIFY    | Thêm 2 endpoint handlers                   |
| `com/.../notification/dto/response/MarkAllReadResponse.java`                                       | CREATE    | Response DTO mới                           |
| `com/.../exception/GlobalExceptionHandler.java`                                                    | MODIFY    | Thêm handler cho NOTIF-020, NOTIF-021      |
| `src/test/.../notification/service/NotificationServiceMarkTest.java`                               | CREATE    | Unit tests cho service                     |
| `src/test/.../notification/controller/NotificationControllerMarkTest.java`                         | CREATE    | Controller/API tests                       |
| `src/test/.../notification/integration/NotificationMarkIntegrationTest.java`                       | CREATE    | Integration tests                          |
| `src/test/resources/test-data/seed-notifications-uc12.sql`                                        | CREATE    | Test data seed cho INT-001                 |
| `src/test/resources/test-data/seed-5-unread-uc12.sql`                                             | CREATE    | Test data seed cho INT-002                 |

### 8.2 Lệnh Chạy Test

```bash
# Chạy tất cả tests cho notification module
./mvnw test -pl CareBridgeAPI \
  -Dtest="NotificationServiceMarkTest,NotificationControllerMarkTest,NotificationMarkIntegrationTest"

# Chạy với coverage report
./mvnw test jacoco:report -pl CareBridgeAPI \
  -Dtest="*Notification*Mark*"

# Kiểm tra coverage threshold
./mvnw verify -pl CareBridgeAPI
```

### 8.3 Mapping Constraints → Test Cases

| Constraint | Test Cases Validating It                              |
|------------|-------------------------------------------------------|
| C1 (Ownership before UPDATE) | TC-001 (verify call order), TC-003 (verify no UPDATE on fail) |
| C2 (Both fields updated)     | INT-001 (DB assertion), TC-001 (mock verification)  |
| C3 (Idempotent)              | TC-002 (no exception), TC-005b (0 unread)           |
| C4 (Emit audit)              | TC-001 (audit called), TC-002 (audit NOT called)    |
| C5 (Server time)             | INT-001 (timestamp within window check)             |

---

*Document ID: CB-NOTIF-IMP-012-TEST | Version 1.0 | Status: Draft | EDS v2.0*
