# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-05 Location & Emergency Alert

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-LOC-PKG-05-TDS` |
| **Version** | `1.0` |
| **Date** | 2026-07-08 |
| **Status** | `DRAFT` |
| **Package** | `PKG-05 — Location & Emergency Alert` |
| **Included UCs** | `UC-77, UC-80` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 2 (2026-07-17 → 2026-07-31)` |
| **Milestone** | `Demo Gate B` |

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-08 | Lâm | Khởi tạo TDS — tách PKG-06 (original) |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-77, UC-80` |
| **Module** | `location — LocationSnapshotServiceImpl + EmergencyAlertServiceImpl` |
| **Spec gốc** | `CB-LOC-PKG-05-TDS` |
| **Priority** | 🔴 P0 |
| **Upstream Dependencies** | `TV1 auth (User)`, `PKG-04 map (CareFacility)`, `Firebase Cloud Messaging` |
| **Downstream Consumers** | `PKG-04 map (openEmergencyMap reads last snapshot)`, `TV1 notification (family alert sends notification)` |

---

## 2. Entity Definition: LocationSnapshot

V1 already has `location_snapshots` table — no migration needed.

```java
@Entity
@Table(name = "location_snapshots", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationSnapshot {

    @Id
    @Column(name = "location_snapshot_id", nullable = false, updatable = false)
    private UUID locationSnapshotId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "context_type", length = 50)
    private String contextType;  // EMERGENCY, CHECK_IN, ONGOING

    @Column(name = "context_id")
    private UUID contextId;      // FK to emergency_event or triage_assessment

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "captured_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant capturedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;   // null = permanent

    @Column(name = "consent_status", length = 20)
    private String consentStatus; // GRANTED, REVOKED, PENDING
}
```

**Constraints (from V1 DDL + business rules):**
- FK: `user_id` → `users(user_id)` (V1 has FK with ON DELETE CASCADE)
- Latitude: -90.0 ≤ value ≤ 90.0
- Longitude: -180.0 ≤ value ≤ 180.0
- expiresAt: optional; if null → no expiry
- consentStatus: required; if PENDING → alert cannot be sent
- context_type = EMERGENCY → context_id is emergency_event_id

---

## 3. ADRs

### ADR-LOC-001: Snapshots owned by PKG-05, read by PKG-04

**Context:** Both map and location need snapshot access.
**Decision:** PKG-05 owns CRUD for LocationSnapshot. PKG-04 reads via LocationSnapshotService (interface injection).
**Consequences:** ✅ Single-writer pattern.

### ADR-LOC-002: Family Emergency Alert — Firebase push + fallback SMS

**Context:** UC-80 must alert family members immediately.
**Decision:** Primary: FCM push notification. Fallback: Gmail SMTP / SMS gateway (V2). Always also create Notification records in DB.
**Consequences:** ✅ Reliable delivery; FCM required for mobile app.

### ADR-LOC-003: Snapshot TTL handled by cleanup job

**Context:** Snapshots accumulate if not cleaned.
**Decision:** `ScheduledTask` at 2 AM UTC deletes `capturedAt > 30 days AND context_type != EMERGENCY`. Emergency snapshots kept until event is closed.
**Consequences:** ✅ Bounded storage growth.

---

## 4. Service Layer

### 4.1 Service Interface

```java
public interface LocationSnapshotService {

    /** UC-77 (internal): Save location snapshot when emergency event opens */
    LocationSnapshot saveSnapshot(UUID userId, BigDecimal lat, BigDecimal lng,
                                   BigDecimal accuracyMeters,
                                   String contextType, UUID contextId);

    /** UC-77: Get latest snapshot for a user */
    Optional<LocationSnapshot> getLatestSnapshot(UUID userId);

    /** UC-77: Get slots by contextId (e.g., all snaps for an emergency) */
    List<LocationSnapshot> findByContextId(UUID contextId);

    /** Internal: Check user has granted location consent */
    boolean hasConsent(UUID userId);
}
```

```java
public interface EmergencyAlertService {

    /** UC-80: Send emergency alert to family members */
    void sendFamilyEmergencyAlert(UUID userId, UUID emergencyEventId,
                                   String messageTemplate);

    /** UC-80: Get count of family alerts sent today (rate limit check) */
    long countAlertsSentToday(UUID userId);

    /** Internal: Mark alert as read */
    void markAlertRead(UUID alertId);
}
```

### 4.2 Core Workflows

#### UC-77 (internal + location): Save location snapshot

```
Input: userId, lat, lng, accuracy, contextType, contextId
1. Validate lat ∈ [-90,90], lng ∈ [-180,180] → LOC-001 if invalid
2. Create LocationSnapshot entity
3. Save via repository
4. If contextType = EMERGENCY: emit AuditEvent + trigger UC-80 flow (alert family)
   NOTE: UC-80 alert is NOT automatic from UC-77 — mother explicitly triggers
5. Return LocationSnapshotResponse
```

#### UC-80: Family Emergency Alert

```
Input: userId (mother), emergencyEventId, messageTemplate
1. Validate emergencyEvent belongs to user → LOC-003 if not
2. Verify emergencyEvent.status = OPEN → LOC-004 if closed
3. Load user's family members: query UserService for children under same family_id
   (PKG-01 ExpertProfileService or UserRepository for family_id)
4. For each family member that is a registered user in system:
   a. Create NotificationRecord (TV1 notification table)
   b. Send FCM push to family member's device tokens (via FirebaseService)
   c. Register alert in audit_logs
5. Rate limit: max 5 alerts per hour per user → LOC-005 if exceeded
6. Return { alertCount: N, sentTo: [list] }
```

**Note on "family members" definition:**
- A MOTHER account may have linked CHILD profiles (baby_profiles table)
- A FAMILY account is a separate user sharing access with MOTHER
- Alert goes to: (1) all linked FAMILY accounts, (2) recorded in mother's notification history
- For TV1: NotificationPreference for EMERGENCY must be enabled

---

## 5. Repository Layer

### 5.1 LocationSnapshotRepository

```java
@Repository
public interface LocationSnapshotRepository extends JpaRepository<LocationSnapshot, UUID> {

    /** Find latest snapshot for user */
    Optional<LocationSnapshot> findTopByUserIdOrderByCapturedAtDesc(UUID userId);

    /** Find all snapshots for a context (emergency / triage) */
    List<LocationSnapshot> findByContextTypeAndContextIdOrderByCapturedAtDesc(
        String contextType, UUID contextId, Pageable pageable);

    /** Delete expired snapshots (cleanup job) */
    @Modifying
    @Query("DELETE FROM LocationSnapshot s " +
           "WHERE s.contextType != 'EMERGENCY' " +
           "AND s.capturedAt < :cutoff")
    int deleteOldSnapshots(@Param("cutoff") Instant cutoff);

    /** Count snapshots by context — alert rate limit */
    @Query("SELECT COUNT(s) FROM LocationSnapshot s " +
           "WHERE s.userId = :userId AND s.contextType = 'EMERGENCY' " +
           "AND s.capturedAt > :since")
    long countEmergencySince(@Param("userId") UUID userId, @Param("since") Instant since);
}
```

### 5.2 EmergencyAlertRepository (if needed)

Emergency alert records are stored in `notifications` table (TV1 domain). No separate table needed.

---

## 6. Controller Specification

```java
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Validated
public class LocationSnapshotController {
```

### UC-77 (internal): POST /api/v1/location/snapshot

```
Request Auth: @PreAuthorize("isAuthenticated()")  (any logged-in user can post their location)
Request Body: {
  latitude, longitude, accuracyMeters,
  contextType: "EMERGENCY" | "CHECK_IN" | "ONGOING",
  contextId: UUID (optional)
}
Success: 201 — LocationSnapshotResponse
Errors:  400 LOC-001 (invalid coords)
        401 no token
```

### UC-77 (read): GET /api/v1/location/snapshot/me/latest

```
Request Auth: @PreAuthorize("isAuthenticated()")
Success: 200 — LocationSnapshotResponse (or 204 if none)
```

### UC-80: POST /api/v1/nearbycare/emergency-alert

```
Request Auth: @PreAuthorize("hasRole('MOTHER') or hasRole('FAMILY')")
Request Body: {
  emergencyEventId: UUID (required),
  message: String (optional, default: "Tôi đang cần giúp đỡ khẩn cấp!")
}
Success: 201 — { alertCount: N, sentTo: [familyUserIds] }
Errors:  403 LOC-003 (event not owned), LOC-004 (event closed), LOC-005 (rate limited)
        404 LOC-002 (event not found)
```

---

## 7. Child List for UC-80

When mother triggers UC-80, the system sends emergency alerts to:

| Child entity(s) | Source | Condition for alert |
|-----------------|--------|---------------------|
| `baby_profiles` linked to userId | direct FK baby.user_id | All babies of mother |
| `user_roles = FAMILY` under same family group | complex; TV1 manages family | All FAMILY accounts sharing this mother's family_id |
| Setting: NotificationPreference.emergencyAlertEnabled | true only | If false, skip individual |

**Rate limiting:** Beacon app → max 1 alert per 5 minutes (5/hour average) to avoid spamming family. Admin override possible.

---

## 8. DTOs

```java
public record LocationSnapshotRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    BigDecimal accuracyMeters,
    String contextType,   // EMERGENCY, CHECK_IN, ONGOING
    UUID contextId
) {}

public record LocationSnapshotResponse(
    UUID locationSnapshotId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal accuracyMeters,
    String contextType,
    UUID contextId,
    Instant capturedAt,
    Instant expiresAt,
    String consentStatus
) {}

public record EmergencyAlertRequest(
    @NotNull UUID emergencyEventId,
    String message
) {}

public record EmergencyAlertResponse(
    int alertCount,
    List<UUID> sentTo,
    long rateLimitRemaining
) {}
```

---

## 9. Error Codes

| Code | HTTP | Trigger |
|------|------|---------|
| `LOC-001` | 400 | Invalid latitude/longitude range |
| `LOC-002` | 404 | Emergency event not found |
| `LOC-003` | 403 | Not owner of emergency event |
| `LOC-004` | 403 | Emergency event already closed |
| `LOC-005` | 429 | Rate limit exceeded (max 5/hour) |
| `LOC-006` | 403 | Location consent not granted (PENDING/REVOKED) |

---

## 10. AI Constraints (CASE 2.0)

| # | Constraint |
|---|-----------|
| C1 | Snapshot save: any authenticated user can post their own location |
| C2 | Emergency alert: MOTHER/FAMILY role only |
| C3 | Latitude/longitude validated in DTO + service layer |
| C4 | Consent status checked before alert sends |
| C5 | Rate limit enforced before FCM call |
| C6 | Audit event on every emergency alert dispatch |
| C7 | Mobile app must include FCM token in profile for push delivery |

---

## 11. Anti-Pattern Detection

| Pattern | Description | Check |
|---------|-------------|-------|
| AP-LOC-001 | No rate limit on alert | ✅ 5/hour per user enforced |
| AP-LOC-002 | FCM send repeated on retry | ✅ Idempotency: check `Notifications` table has recent entry |
| AP-LOC-003 | Location without consent | ✅ consentStatus check |
| AP-LOC-004 | Cleanup job deletes emergency snapshots | ✅ Cleanup WHERE contextType != EMERGENCY |
| AP-LOC-005 | Hard delete on snapshots | ✅ Use repository delete only (acceptable for TTL expired) |

---

## 12. Implementation Order

1. `entity/LocationSnapshot.java` — JPA entity (V1 table exists)
2. `repository/LocationSnapshotRepository.java`
3. `exception/LocationException.java` — LOC-001..006
4. `dto/request/LocationSnapshotRequest.java`
5. `dto/response/LocationSnapshotResponse.java`
6. `dto/response/EmergencyAlertResponse.java`
7. `mapper/LocationSnapshotMapper.java`
8. `service/LocationSnapshotServiceImpl.java`
9. `service/EmergencyAlertServiceImpl.java`
10. `controller/LocationSnapshotController.java`
11. `controller/EmergencyAlertController.java`
12. Tests (see Test-Spec)

**Migration:** NOT NEEDED — V1 table exists (`location_snapshots`).

---

*CareBridge TDS v1.0 — PKG-05 Location & Emergency Alert*
