# Test Specification
## UC19 — View Sharing History

| Field | Value |
|---|---|
| **Document ID** | CB-CONSENT-IMP-019-TEST |
| **Version** | 1.0 |
| **Date** | 2026-06-26 |
| **Status** | Implemented |
| **Author** | AI Agent |
| **Related TDS** | CB-CONSENT-IMP-019 |
| **SRS Reference** | SRS 3.1.1.19 |

---

## 1. Mục Tiêu Kiểm Thử (Test Objectives)

Xác minh rằng `GET /api/v1/consent/grants` hoạt động đúng theo các yêu cầu của UC19:

1. Trả về tất cả consent grants (active + revoked) của người dùng hiện tại.
2. Không trả về grants của bất kỳ user nào khác.
3. Kết quả được sắp xếp theo `createdAt DESC`.
4. Từ chối request không có JWT hợp lệ.
5. Trả về empty array (không phải null) khi user không có grants.

---

## 2. Phạm Vi Kiểm Thử (Test Scope)

| Loại Test | Bao gồm |
|---|---|
| Unit Tests | ConsentService.listConsents() logic |
| Integration Tests | Endpoint → DB round-trip |
| Security Tests | JWT validation, cross-user isolation |
| Boundary Tests | Empty list case |

**Ngoài phạm vi**: Performance testing, load testing (xử lý riêng).

---

## 3. Môi Trường Kiểm Thử (Test Environment)

| Component | Yêu cầu |
|---|---|
| Framework | JUnit 5 + Mockito + Spring Boot Test |
| DB (Integration) | PostgreSQL test container hoặc H2 |
| Auth | MockMvc với JWT test token |
| Assertion | AssertJ |

### 3.1 Test Data Setup

```java
// ConsentTestFactory.java
public class ConsentTestFactory {

    public static final UUID USER_ID_OWNER =
        UUID.fromString("00000000-0000-0000-0000-000000000019");
    public static final UUID USER_ID_OTHER =
        UUID.fromString("00000000-0000-0000-0000-000000000099");

    public static ConsentGrant makeActiveGrant(UUID userId) {
        ConsentGrant g = new ConsentGrant();
        g.setUserId(userId);
        g.setDataType(ConsentDataType.HEALTH_RECORD);
        g.setPurpose(ConsentPurpose.TREATMENT);
        g.setRecipient("Dr. Test");
        g.setScope("read");
        g.setConsentGivenAt(Instant.now().minus(30, ChronoUnit.DAYS));
        g.setExpiryAt(Instant.now().plus(30, ChronoUnit.DAYS));
        g.setRevokedAt(null);
        g.setCreatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        return g;
    }

    public static ConsentGrant makeRevokedGrant(UUID userId) {
        ConsentGrant g = makeActiveGrant(userId);
        g.setDataType(ConsentDataType.LOCATION);
        g.setRevokedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        g.setRevokedBy(userId);
        g.setCreatedAt(Instant.now().minus(60, ChronoUnit.DAYS));
        return g;
    }
}
```

---

## 4. Test Cases — Unit Tests

### CONSENT-TC-019-001: Happy Path — Trả về list grants (active + revoked)

| Field | Value |
|---|---|
| **TC ID** | CONSENT-TC-019-001 |
| **Priority** | P0 — Critical |
| **Type** | Unit |
| **Related BR** | BR-CONSENT-020, BR-CONSENT-021 |

**Preconditions:**
- User `USER_ID_OWNER` có 2 grants: 1 active, 1 revoked
- JWT hợp lệ cho `USER_ID_OWNER`

**Test Steps:**

```java
@Test
@DisplayName("CONSENT-TC-019-001: listConsents returns both active and revoked grants")
void listConsents_returnsAllGrants_includingRevoked() {
    // Arrange
    UUID userId = ConsentTestFactory.USER_ID_OWNER;
    ConsentGrant active = ConsentTestFactory.makeActiveGrant(userId);
    ConsentGrant revoked = ConsentTestFactory.makeRevokedGrant(userId);

    when(consentGrantRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(active, revoked));

    // Act
    List<ConsentGrantResponse> result = consentService.listConsents(userId);

    // Assert
    assertThat(result).hasSize(2);
    assertThat(result).anyMatch(r -> "ACTIVE".equals(r.getStatus()));
    assertThat(result).anyMatch(r -> "REVOKED".equals(r.getStatus()));
    verify(auditService).emit(eq(AuditEventType.VIEW_SHARING_HISTORY), eq(userId));
}
```

**Expected Result:** 200 OK, `data` có 2 phần tử, một ACTIVE, một REVOKED.

---

### CONSENT-TC-019-002: Empty List — User Không Có Grants

| Field | Value |
|---|---|
| **TC ID** | CONSENT-TC-019-002 |
| **Priority** | P1 — High |
| **Type** | Unit |

**Preconditions:**
- User `USER_ID_OWNER` không có grant nào trong DB
- JWT hợp lệ

**Test Steps:**

```java
@Test
@DisplayName("CONSENT-TC-019-002: listConsents returns empty array, not null, when no grants")
void listConsents_returnsEmptyList_whenNoGrants() {
    // Arrange
    UUID userId = ConsentTestFactory.USER_ID_OWNER;

    when(consentGrantRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(Collections.emptyList());

    // Act
    List<ConsentGrantResponse> result = consentService.listConsents(userId);

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
}
```

**Expected Result:** 200 OK, `data: []` (empty array, không phải null).

---

### CONSENT-TC-019-003: Cross-User Isolation — Không Hiển Thị Grants Của User Khác

| Field | Value |
|---|---|
| **TC ID** | CONSENT-TC-019-003 |
| **Priority** | P0 — Critical (Security) |
| **Type** | Unit |
| **Related BR** | BR-CONSENT-020 |

**Preconditions:**
- `USER_ID_OTHER` có grants trong DB
- JWT hợp lệ cho `USER_ID_OWNER`

**Test Steps:**

```java
@Test
@DisplayName("CONSENT-TC-019-003: listConsents never returns another user's grants")
void listConsents_neverReturnsOtherUsersGrants() {
    // Arrange
    UUID ownerId = ConsentTestFactory.USER_ID_OWNER;
    UUID otherId = ConsentTestFactory.USER_ID_OTHER;

    // Repository is called with OWNER's userId, returns empty
    when(consentGrantRepository.findByUserIdOrderByCreatedAtDesc(ownerId))
        .thenReturn(Collections.emptyList());

    // Act
    List<ConsentGrantResponse> result = consentService.listConsents(ownerId);

    // Assert — owner sees nothing (their own list is empty)
    assertThat(result).isEmpty();

    // Verify repository was ONLY called with ownerId, never otherId
    verify(consentGrantRepository).findByUserIdOrderByCreatedAtDesc(ownerId);
    verify(consentGrantRepository, never()).findByUserIdOrderByCreatedAtDesc(otherId);
}
```

**Expected Result:** Repository chỉ được gọi với `ownerId`. Grants của `USER_ID_OTHER` không xuất hiện.

---

### CONSENT-TC-019-004: No JWT — 401 Unauthorized

| Field | Value |
|---|---|
| **TC ID** | CONSENT-TC-019-004 |
| **Priority** | P0 — Critical (Security) |
| **Type** | Unit / MVC |
| **Error Code** | CONSENT-020 |

**Preconditions:**
- Request không có `Authorization` header

**Test Steps:**

```java
@Test
@DisplayName("CONSENT-TC-019-004: GET /consent/grants without JWT returns 401")
void getGrants_withoutJwt_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/consent/grants"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.code").value("CONSENT-020"));
}
```

**Expected Result:** HTTP 401, body chứa `code: "CONSENT-020"`.

---

## 5. Test Cases — Integration Tests

### CONSENT-TC-019-INT-001: DB Count Khớp Response Length

| Field | Value |
|---|---|
| **TC ID** | CONSENT-TC-019-INT-001 |
| **Priority** | P1 — High |
| **Type** | Integration |

**Preconditions:**
- PostgreSQL test container với schema `consent_grants` sẵn sàng
- `USER_ID_OWNER` có N grants (mixed active + revoked) đã được insert vào DB
- JWT hợp lệ cho `USER_ID_OWNER`

**Test Steps:**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ConsentIntegrationTest {

    @Autowired TestRestTemplate restTemplate;
    @Autowired ConsentGrantRepository repo;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("CONSENT-TC-019-INT-001: Response count matches DB count")
    void integration_responseCountMatchesDbCount() {
        // Arrange — insert 3 grants (2 active, 1 revoked) for USER_ID_OWNER
        UUID userId = ConsentTestFactory.USER_ID_OWNER;
        repo.save(ConsentTestFactory.makeActiveGrant(userId));
        repo.save(ConsentTestFactory.makeActiveGrant(userId));
        repo.save(ConsentTestFactory.makeRevokedGrant(userId));

        long dbCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM consent_grants WHERE user_id = ?",
            Long.class, userId);

        // Act
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(generateTestJwt(userId));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/v1/consent/grants", HttpMethod.GET, entity, String.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode data = objectMapper.readTree(response.getBody()).get("data");
        assertThat(data.size()).isEqualTo((int) dbCount);
        assertThat(dbCount).isEqualTo(3L);
    }
}
```

**Expected Result:** `response.data.length == COUNT(*) từ DB == 3`.

---

## 6. Logic Issues & Known Risks

| ID | Mô tả | Cách phòng ngừa |
|---|---|---|
| **L1** | Revoked grants phải xuất hiện trong list — không được filter out | TC-019-001 explicitly asserts REVOKED status present |
| **L2** | Kết quả phải sắp xếp `createdAt DESC` | Assert `result.get(0).getCreatedAt()` > `result.get(1).getCreatedAt()` |

### L2 Test — Ordering Assertion

```java
@Test
@DisplayName("L2: listConsents results are sorted by createdAt DESC")
void listConsents_sortedByCreatedAtDesc() {
    UUID userId = ConsentTestFactory.USER_ID_OWNER;
    Instant older = Instant.now().minus(60, ChronoUnit.DAYS);
    Instant newer = Instant.now().minus(10, ChronoUnit.DAYS);

    ConsentGrant g1 = ConsentTestFactory.makeActiveGrant(userId);
    g1.setCreatedAt(newer);  // newer comes first
    ConsentGrant g2 = ConsentTestFactory.makeRevokedGrant(userId);
    g2.setCreatedAt(older);  // older comes second

    when(consentGrantRepository.findByUserIdOrderByCreatedAtDesc(userId))
        .thenReturn(List.of(g1, g2));  // repository already returns in order

    List<ConsentGrantResponse> result = consentService.listConsents(userId);

    assertThat(result.get(0).getCreatedAt()).isAfter(result.get(1).getCreatedAt());
}
```

---

## 7. Acceptance Criteria Checklist

| Criterion | TC Coverage | Pass/Fail |
|---|---|---|
| 200 với list grants (active + revoked) | CONSENT-TC-019-001 | ☐ |
| 200 với empty array khi no grants | CONSENT-TC-019-002 | ☐ |
| Không hiển thị grants của user khác | CONSENT-TC-019-003 | ☐ |
| 401 khi không có JWT | CONSENT-TC-019-004 | ☐ |
| DB count == response length | CONSENT-TC-019-INT-001 | ☐ |
| Sorted createdAt DESC | L2 test | ☐ |
| AuditService.emit() được gọi | TC-019-001 verify | ☐ |

---

## 8. Traceability Matrix

| TC ID | SRS | BR | TDS Section |
|---|---|---|---|
| CONSENT-TC-019-001 | 3.1.1.19 | BR-CONSENT-020, BR-CONSENT-021 | §8, §11 |
| CONSENT-TC-019-002 | 3.1.1.19 | BR-CONSENT-021 | §9.3 |
| CONSENT-TC-019-003 | 3.1.1.19 | BR-CONSENT-020 | §17 C1 |
| CONSENT-TC-019-004 | 3.1.1.19 | — | §10 |
| CONSENT-TC-019-INT-001 | 3.1.1.19 | BR-CONSENT-020, BR-CONSENT-021 | §14 |

---

*End of UC19 Test Specification — CB-CONSENT-IMP-019-TEST v1.0*
