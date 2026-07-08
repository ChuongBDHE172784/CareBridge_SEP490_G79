# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-06 Nearby Care Request & Response

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-NEARBY-PKG-06-TDS` |
| **Version** | `1.0` |
| **Date** | 2026-07-08 |
| **Status** | `DRAFT` |
| **Package** | `PKG-06 — Nearby Care Request & Response` |
| **Included UCs** | `UC-81, UC-82` |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 2 (2026-07-17 → 2026-07-31)` |
| **Milestone** | `Demo Gate B` |

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-04 | Claude | Khởi tạo TDS (PKG-06 tạm — UC-77–82 all) |
| 2026-07-08 | Lâm | Tách ra: UC-77/78 → PKG-04 (Map), UC-80 → PKG-05 (Location), UC-81/82 → PKG-06 (NearbyCare) |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-81, UC-82` |
| **Module** | `nearbycare — NearbySupportRequestServiceImpl + NearbySupportResponseServiceImpl` |
| **Spec gốc** | `CB-NEARBY-PKG-06-TDS` |
| **Priority** | 🔴 P0 |
| **Upstream Dependencies** | `PKG-01 expert (ExpertProfile)`, `PKG-03 expertavailability (availability + location share)`, `PKG-04 map (CareFacility, EmergencyEvent)`, `PKG-05 location (LocationSnapshot)`, `TV1 auth` |
| **Downstream Consumers** | Mobile App (Nearby Support screens), Web App |

---

## 2. Entity Definitions

V1 does NOT have `nearby_support_requests` and `nearby_support_responses` tables — **migration needed**.

### 2.1 Entity: NearbySupportRequest

```java
@Entity
@Table(name = "nearby_support_requests", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NearbySupportRequest {

    @Id
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID requestId;

    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "requester_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RequesterType requesterType;  // MOTHER or FAMILY

    @Column(name = "help_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private HelpType helpType;  // GROCERIES, MEDICINE, CHILD_CARE, TRANSPORT, EMOTIONAL_SUPPORT, OTHER

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "request_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal requestLatitude;

    @Column(name = "request_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal requestLongitude;

    @Column(name = "address_text", length = 500)
    private String addressText;

    @Column(name = "urgency_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UrgencyLevel urgencyLevel;  // LOW, MEDIUM, HIGH, CRITICAL

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SupportRequestStatus status = SupportRequestStatus.OPEN;

    @Column(name = "expires_at")
    private Instant expiresAt;  // CRITICAL expires in 1 hour

    @Column(name = "connected_expert_profile_id")
    private UUID connectedExpertProfileId;  // filled when expert accepts

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Enums:**

```java
public enum RequesterType { MOTHER, FAMILY }
public enum HelpType {
    GROCERIES, MEDICINE, CHILD_CARE,
    TRANSPORT, EMOTIONAL_SUPPORT, OTHER
}
public enum UrgencyLevel { LOW, MEDIUM, HIGH, CRITICAL }
public enum SupportRequestStatus {
    OPEN,      // Đang tìm expert
    ACCEPTED,  // Expert đã nhận
    IN_PROGRESS,  // Đang thực hiện
    COMPLETED,    // Hoàn thành
    CANCELLED,    // Requester hủy
    EXPIRED    // Hết thời gian tìm
}
```

### 2.2 Entity: NearbySupportResponse

```java
@Entity
@Table(name = "nearby_support_responses", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NearbySupportResponse {

    @Id
    @Column(name = "response_id", nullable = false, updatable = false)
    private UUID responseId;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "response_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SupportResponseType responseType;  // ACCEPT, DECLINE

    @Column(name = "note", length = 500)
    private String note;  // "I can be there in 20 mins"

    @Column(name = "estimated_arrival_minutes")
    private Integer estimatedArrivalMinutes;

    @Column(name = "responded_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant respondedAt;
}
```

```java
public enum SupportResponseType { ACCEPT, DECLINE }
```

### 2.3 Schema Constraints

| Column | Constraint |
|--------|-----------|
| request_id UUID PK | DEFAULT gen_random_uuid() |
| requester_user_id | FK to users(user_id) |
| requester_type | NOT NULL, CHECK IN ('MOTHER','FAMILY') |
| help_type | NOT NULL, CHECK in enum set |
| urgency_level | NOT NULL |
| status | DEFAULT 'OPEN', CHECK in enum set |
| expires_at | Nullable; CRITICAL = 1h, others = 4h |
| connected_expert_profile_id | FK to expert_profiles; nullable until accepted |

| response_id UUID PK | DEFAULT gen_random_uuid() |
| request_id | FK to nearby_support_requests, CASCADE DELETE |
| expert_profile_id | FK to expert_profiles |
| response_type | NOT NULL |
| estimated_arrival_minutes | Optional integer |

---

## 3. ADRs

### ADR-NCB-001: CRITICAL urgency auto-expires in 1 hour, others in 4 hours

**Context:** Mother in critical need can't wait indefinitely.
**Decision:** Scheduled job checks `status=OPEN AND expires_at < now()` → set `EXPIRED`.
**Consequences:** ✅ Bounded wait times; ⚠️ Mother gets notification of expiry.

### ADR-NCB-002: Expert matching via proximity + availability

**Context:** Need to find expert to help physically.
**Decision:** Use ExpertLocationShare (PKG-03) for expert location + ExpertAvailability for channel. Match by: same helpType (expert.specialty), within 5km, ACTIVE trust status.
**Consequences:** ✅ Efficient local matching; ⚠️ Requires location sharing enabled.

### ADR-NCB-003: NearbyCare vị trí — collapsed into single table

**Context:** Original plan had NearByCare* + NearbyCareVC* tables.
**Decision:** Use single `nearby_support_requests` + `nearby_support_responses` table. `location_snapshots` is separate (PKG-05 owns). CareFacility reference in emergency_event (PKG-04) for facility-linked requests.
**Consequences:** ✅ Simpler schema; single-writer pattern per table.

---

## 4. Service Layer

### 4.1 Service Interfaces

```java
public interface NearbySupportRequestService {

    /** UC-81: Mother/Family creates a nearby support request */
    SupportRequestResponse createRequest(UUID requesterUserId, CreateSupportRequestRequest req);

    /** UC-81: Requester cancels their own request */
    void cancelRequest(UUID requesterUserId, UUID requestId);

    /** UC-81: Requester views their active requests */
    List<SupportRequestResponse> getMyRequests(UUID requesterUserId);

    /** UC-81: Expert view — available requests near me */
    List<SupportRequestResponse> findNearbyRequests(UUID expertUserId);

    /** Internal: Mark ACCEPTED → IN_PROGRESS when expert arrives */
    void markInProgress(UUID expertUserId, UUID requestId);

    /** Internal: Mark COMPLETED */
    void markCompleted(UUID requesterUserId, UUID requestId);
}
```

```java
public interface NearbySupportResponseService {

    /** UC-82: Expert accepts a nearby request */
    SupportRequestDetailResponse acceptRequest(UUID expertUserId, UUID requestId,
                                               AcceptResponseRequest req);

    /** UC-82: Expert declines a nearby request */
    void declineRequest(UUID expertUserId, UUID requestId, String reason);
}
```

### 4.2 Core Workflows

#### UC-81: Mother creates nearby support request

```
Input: requesterUserId, helpType, description, location (lat/lng), urgencyLevel, [addressText]
1. Verify user role is MOTHER or FAMILY
2. Determine expires_at: CRITICAL → now+1h, others → now+4h
3. Save NearbySupportRequest (status=OPEN)
4. Emit AuditEvent: "NEARBY_REQUEST_CREATED"
5. Return SupportRequestResponse with requestId
```

#### UC-81 (expert): Expert browses nearby requests

```
Input: expertUserId
1. Get ExpertProfile by expertUserId (PKG-01)
2. Verify profile.verificationStatus = VERIFIED
3. Get expert's current location via ExpertLocationShare (PKG-03)
4. Compute bounding box: 5km radius around expert location
5. Query NearbySupportRequestRepo.findOpenInBBox(lat, lng, 5km)
6. Filter: status = OPEN AND expires_at > now AND helpType matches specialty
7. Sort by urgency (CRITICAL first) then by distance
8. Return top 20 SupportRequestResponse
```

#### UC-82: Expert accepts a nearby request

```
Input: expertUserId, requestId, { note, estimatedArrivalMinutes }
1. Verify expert is VERIFIED + ACTIVE
2. Lock: request.findById(requestId) with PESSIMISTIC_WRITE
3. Verify request.status = OPEN
4. Set request.status = ACCEPTED, request.connectedExpertProfileId = expertProfileId
5. Create NearbySupportResponse (responseType = ACCEPT)
6. Emit AuditEvent: "EXPERT_ACCEPTED_NEARBY_REQUEST"
7. Emit AuditEvent: "NEARBY_REQUEST_ACCEPTED" (for requester notification)
8. Return SupportRequestDetailResponse with expert info
```

#### UC-82: Expert declines

```
1. Same as accept but responseType = DECLINE
2. Request remains OPEN (other experts can accept)
3. Record decline count (if exceeds 5 auto-move to EXPIRED)
```

---

## 5. Repository Layer

```java
@Repository
public interface NearbySupportRequestRepository extends JpaRepository<NearbySupportRequest, UUID> {

    /** UC-81: Find open requests near a point, sorted by urgency + distance */
    @Query("SELECT r FROM NearbySupportRequest r " +
           "WHERE r.status = 'OPEN' " +
           "AND r.expiresAt > :now " +
           "AND r.requestLatitude BETWEEN :minLat AND :maxLat " +
           "AND r.requestLongitude BETWEEN :minLng AND :maxLng " +
           "AND (:helpType IS NULL OR r.helpType = :helpType) " +
           "ORDER BY " +
           "CASE r.urgencyLevel WHEN 'CRITICAL' THEN 1 WHEN 'HIGH' THEN 2 WHEN 'MEDIUM' THEN 3 ELSE 4 END, " +
           "r.createdAt ASC")
    List<NearbySupportRequest> findOpenNearby(@Param("minLat") BigDecimal minLat,
                                               @Param("maxLat") BigDecimal maxLat,
                                               @Param("minLng") BigDecimal minLng,
                                               @Param("maxLng") BigDecimal maxLng,
                                               @Param("now") Instant now,
                                               @Param("helpType") HelpType helpType,
                                               Pageable pageable);

    /** Find by requester */
    @Query("SELECT r FROM NearbySupportRequest r WHERE r.requesterUserId = :userId " +
           "ORDER BY r.createdAt DESC")
    List<NearbySupportRequest> findByRequester(@Param("userId") UUID userId, Pageable pageable);

    /** Mark expired — scheduled cleanup */
    @Modifying
    @Query("UPDATE NearbySupportRequest r SET r.status = 'EXPIRED' " +
           "WHERE r.status = 'OPEN' AND r.expiresAt < :now")
    int expireStale(@Param("now") Instant now);

    /** Find request for a specific expert (to show expert their in-progress) */
    @Query("SELECT r FROM NearbySupportRequest r " +
           "WHERE r.connectedExpertProfileId = :profileId " +
           "AND r.status IN ('ACCEPTED', 'IN_PROGRESS') " +
           "ORDER BY r.updatedAt DESC")
    List<NearbySupportRequest> findActiveByExpert(@Param("profileId") UUID profileId,
                                                   Pageable pageable);
}
```

```java
@Repository
public interface NearbySupportResponseRepository extends JpaRepository<NearbySupportResponse, UUID> {
    List<NearbySupportResponse> findByRequestIdOrderByRespondedAtDesc(UUID requestId);

    boolean existsByRequestIdAndExpertProfileId(UUID requestId, UUID expertProfileId);
}
```

---

## 6. Controller Specification

```java
@RestController
@RequestMapping("/api/v1/nearbycare/support-request")
@RequiredArgsConstructor
@Validated
public class NearbySupportRequestController {
```

### UC-81: POST /api/v1/nearbycare/support-request

```
Request Auth: @PreAuthorize("hasRole('MOTHER') or hasRole('FAMILY')")
Request Body: CreateSupportRequestRequest
  { helpType, description, latitude, longitude, addressText, urgencyLevel }
Success: 201 — SupportRequestResponse (requestId set)
Errors:  403 wrong role / NCB-001
        400 NCB-002 (invalid coordinates)
```

### UC-81: DELETE /api/v1/nearbycare/support-request/{requestId} (cancel)

```
Request Auth: @PreAuthorize("isAuthenticated()") + owner check
Success: 204 No Content
Errors:  403 NCB-003 (not owner), 404 NCB-004
```

### UC-81: GET /api/v1/nearbycare/support-request/me (list my requests)

```
Request Auth: @PreAuthorize("isAuthenticated()")
Success: 200 — List<SupportRequestSummaryResponse>
```

### UC-81 (expert): GET /api/v1/expert/nearby-requests (browse)

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Success: 200 — List<SupportRequestSummaryResponse> (sorted urgency/distance)
```

### UC-82: POST /api/v1/expert/nearby-requests/{requestId}/respond

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Request Body: { responseType: ACCEPT|DECLINE, note, estimatedArrivalMinutes }
Success: 200 — SupportRequestDetailResponse
Errors:  403 EXPERT-003 (not VERIFIED), NCB-005 (request already taken/expired)
        409 NCB-006 (already responded to this request)
```

### UC-82 (requester): GET /api/v1/nearbycare/support-request/{requestId}

```
Request Auth: @PreAuthorize("isAuthenticated()") + owner/expert check
Success: 200 — SupportRequestDetailResponse (with expert info if accepted)
```

---

## 7. DTOs

```java
public record CreateSupportRequestRequest(
    @NotBlank @HelpTypeConstraint HelpType helpType,
    @Size(max = 1000) String description,
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @Size(max = 500) String addressText,
    @NotNull UrgencyLevel urgencyLevel
) {}

public record AcceptResponseRequest(
    @NotNull SupportResponseType responseType,
    @Size(max = 500) String note,
    Integer estimatedArrivalMinutes  // required if ACCEPT
) {}

public record SupportRequestSummaryResponse(
    UUID requestId,
    RequesterType requesterType,
    HelpType helpType,
    String description,
    BigDecimal distanceKm,                // only for expert view
    UrgencyLevel urgencyLevel,
    SupportRequestStatus status,
    Instant expiresAt,
    Instant createdAt
) {}

public record SupportRequestDetailResponse(
    UUID requestId,
    RequesterType requesterType,
    RequesterSummaryDto requester,         // name, phone, address
    HelpType helpType,
    String description,
    BigDecimal latitude,
    BigDecimal longitude,
    String addressText,
    UrgencyLevel urgencyLevel,
    SupportRequestStatus status,
    Instant expiresAt,
    AcceptedExpertDto acceptedExpert,      // null until accepted
    List<SupportResponseSummary> responses,
    Instant createdAt,
    Instant updatedAt
) {
    public record RequesterSummaryDto(String displayName, String phone) {}
    public record AcceptedExpertDto(UUID expertProfileId, String displayName,
                                     String specialty, String note,
                                     Integer estimatedArrivalMinutes) {}
}
```

---

## 8. Error Codes

| Code | HTTP | Trigger |
|------|------|---------|
| `NCB-001` | 403 | Not MOTHER/FAMILY role |
| `NCB-002` | 400 | Invalid coordinates |
| `NCB-003` | 403 | Not owner of request |
| `NCB-004` | 404 | Request not found |
| `NCB-005` | 409 | Request already accepted/expired |
| `NCB-006` | 409 | Expert already responded to this request |
| `NCB-007` | 403 | Expert not VERIFIED |
| `NCB-008` | 400 | estimatedArrivalMinutes required when ACCEPT |

---

## 9. AI Constraints (CASE 2.0)

| # | Constraint |
|---|-----------|
| C1 | POST support-request: MOTHER/FAMILY role only |
| C2 | Expert browse: EXPERT role only, VERIFIED + ACTIVE |
| C3 | Cancel: owner only (requesterUserId match) |
| C4 | Accept: EXPERT only, VERIFIED + ACTIVE |
| C5 | PESSIMISTIC_WRITE on accept to prevent race condition |
| C6 | Audit event on create/accept/decline/complete |
| C7 | Expired requests never accepted (check status + expiresAt in same transaction) |

---

## 10. Anti-Pattern Detection

| Pattern | Description | Check |
|---------|-------------|-------|
| AP-NCB-001 | Missing pessimistic lock on accept | ✅ PESSIMISTIC_WRITE on findById |
| AP-NCB-002 | Double accept (two experts) | ✅ Pessimistic lock prevents; verify status = OPEN in service |
| AP-NCB-003 | Expert accepts without VERIFIED check | ✅ checkExpertVerified() in service |
| AP-NCB-004 | Hard delete on support request | ✅ status = CANCELLED instead |
| AP-NCB-005 | Location exposure without consent | ✅ check ExpertLocationShare available before posting coords to experts |

---

## 11. Implementation Order

1. `entity/NearbySupportRequest.java` — JPA entity (new table → migration required)
2. `entity/NearbySupportResponse.java` — JPA entity (new table → migration required)
3. `entity/RequesterType.java`, `HelpType.java`, `UrgencyLevel.java`, `SupportRequestStatus.java`, `SupportResponseType.java` — enums
4. `repository/NearbySupportRequestRepository.java` — proximity + bbox queries
5. `repository/NearbySupportResponseRepository.java`
6. `exception/NearbyCareException.java` — NCB-001..008
7. `dto/request/*` — CreateSupportRequestRequest, AcceptResponseRequest
8. `dto/response/*` — SupportRequestSummaryResponse, SupportRequestDetailResponse
9. `mapper/NearbyCareMapper.java`
10. `service/NearbySupportRequestServiceImpl.java`
11. `service/NearbySupportResponseServiceImpl.java`
12. `controller/NearbySupportRequestController.java`
13. Tests (see Test-Spec)

**Migration REQUIRED** — `nearby_support_requests`, `nearby_support_responses` tables do NOT exist in V1.

---

## 12. Schema Gaps (Migration Required)

| Table | Missing in V1? | Columns |
|-------|:---:|---------|
| `nearby_support_requests` | ✅ YES | request_id PK, requester_user_id, requester_type, help_type, description, request_latitude, request_longitude, address_text, urgency_level, status, expires_at, connected_expert_profile_id, timestamps |
| `nearby_support_responses` | ✅ YES | response_id PK, request_id FK, expert_profile_id, response_type, note, estimated_arrival_minutes, responded_at |

Recommended migration file: `V20260708__add_nearby_support_tables.sql`

---

*CareBridge TDS v1.0 — PKG-06 Nearby Care Request & Response*
