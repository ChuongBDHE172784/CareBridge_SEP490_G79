# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-03 Expert Availability & Location Share

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-AVAIL-PKG-03-TDS` |
| **Version** | `1.0` |
| **Date** | 2026-07-08 |
| **Status** | `DRAFT` |
| **Package** | `PKG-03 — Expert Availability & Location Share` |
| **Included UCs** | `UC-64, UC-78, UC-79, UC-80, UC-88` |
| **Spec gốc** | `CB-EXP-AVAIL-LOC-PKG-02-TDS` (renamed) |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 1 (2026-07-08 → 2026-07-17)` |
| **Milestone** | `Demo Gate A + nearby care foundation` |

> **Stack kiểm thử:** JUnit 5 · Mockito · MockMvc · Spring Boot Test
> **Test data:** SYNTHETIC

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-04 | Claude | Khởi tạo TDS (PKG-02 tạm) |
| 2026-07-08 | Lâm | Đổi tên PKG-02 → PKG-03; bổ sung đầy đủ entity / DTO / service / controller / repo |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-64, UC-78, UC-79, UC-80, UC-88` |
| **Module** | `expertavailability — ExpertAvailabilityServiceImpl + ExpertLocationShareServiceImpl` |
| **Spec gốc** | `CB-AVAIL-PKG-03-TDS` |
| **Priority** | 🔴 P0 |
| **Upstream Dependencies** | `PKG-01 expert (ExpertProfile), TV1 auth` |
| **Downstream Consumers** | `PKG-04 map (read facilities), PKG-06 nearbycare (read availability)` |

---

## 2. Entity Definitions

### 2.1 ExpertAvailability

```java
@Entity
@Table(name = "expert_availability", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpertAvailability {

    @Id
    @Column(name = "availability_id", nullable = false, updatable = false)
    private UUID availabilityId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Column(name = "channel_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ChannelType channelType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AvailabilityStatus status = AvailabilityStatus.AVAILABLE;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Enum AvailabilityStatus:**
```java
public enum AvailabilityStatus {
    AVAILABLE,     // Đang mở nhận yêu cầu
    BUSY,          // Tạm bận (đang trả lời câu hỏi)
    OFFLINE,       // Không nhận
    EXPIRED        // Đã hết hạn (endAt < now)
}
```

**Enum ChannelType:**
```java
public enum ChannelType {
    ONLINE_CHAT,   // Chat qua app
    VIDEO_CALL,    // Video call
    VOICE_CALL,    // Cuộc gọi thoại
    HOME_VISIT     // Tận nhà — cần địa chỉ
}
```

### 2.2 ExpertLocationShare

```java
@Entity
@Table(name = "expert_location_shares", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpertLocationShare {

    @Id
    @Column(name = "location_share_id", nullable = false, updatable = false)
    private UUID locationShareId;

    @Column(name = "expert_profile_id", nullable = false)
    private UUID expertProfileId;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 6, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "availability_status", length = 20)
    private String availabilityStatus;

    @Column(name = "shared_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant sharedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "consent_reference", columnDefinition = "uuid")
    private UUID consentReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Constraints (extracted from V1 DDL):**
- Latitude: -90.0 ≤ value ≤ 90.0
- Longitude: -180.0 ≤ value ≤ 180.0
- expiresAt: optional; if null → does not expire (until manually revoked)
- consent_reference: optional FK; null allowed (sharing within app context)
- FK: `expert_profile_id` → `expert_profiles(expert_profile_id)` (V1 has FK)

---

## 3. ADRs

### ADR-AVAIL-001: Availability overlap — deferred enforcement

**Context:** UC-64 says expert should not overlap availability windows.
**Decision:** Sprint 1 allows soft overlaps; enforce uniqueness in Sprint 2 via service-layer check + DB unique index.
**Consequences:** ✅ Simple Sprint 1 delivery; ⚠️ Data cleanup needed before enforcement.

### ADR-AVAIL-002: Location share expiry uses app-level TTL, not DB TTL

**Context:** `expires_at` in DB is nullable; some experts want indefinite sharing.
**Decision:** Application checks `expiresAt != null && expiresAt < now()`; if null → share is valid.
**Consequences:** ✅ Flexible; ⚠️ Periodic cleanup job needed for expired rows (deferred).

### ADR-AVAIL-003: Location share PKG-06 reads only — no impact on CREATE/UPDATE

**Context:** PKG-06 reads `expert_location_shares` for nearby matching.
**Decision:** Location shares are created/updated only via PKG-03; PKG-06 only queries (read-only).
**Consequences:** ✅ Clean separation; single-writer pattern.

---

## 4. DTO Definitions

### 4.1 Request DTOs

```java
// Create availability window
public record CreateAvailabilityRequest(
    @NotNull Instant startAt,
    @NotNull Instant endAt,
    @NotBlank String channelType,
    @NotNull AvailabilityStatus status
) {}

// Share/update location
public record ShareLocationRequest(
    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
    @DecimalMin("1.0") BigDecimal accuracyMeters,
    String availabilityStatus,
    Instant expiresAt
) {}

// /api/v1/expert/availability/query — UC-65 (read-only, public)
public record ExpertAvailabilityQuery(
    String channelType,
    String specialty,
    Instant from,
    Instant to
) {}
```

### 4.2 Response DTOs

```java
public record ExpertAvailabilityResponse(
    UUID availabilityId,
    Instant startAt,
    Instant endAt,
    String channelType,
    String status,
    Instant createdAt
) {}

public record LocationShareResponse(
    UUID locationShareId,
    BigDecimal latitude,
    BigDecimal longitude,
    BigDecimal accuracyMeters,
    String availabilityStatus,
    Instant sharedAt,
    Instant expiresAt
) {}

public record NearbyExpertResponse(
    UUID expertProfileId,
    String displayName,
    String specialty,
    BigDecimal distanceKm,
    BigDecimal latitude,
    BigDecimal longitude,
    String availabilityStatus,
    boolean verified
) {}
```

### 4.3 Authorization for DTOs

| DTO | Contains location? | Contains availability? | Notes |
|-----|:---:|:---:|---|
| `ExpertAvailabilityResponse` | ❌ | ✅ | Safe for all roles |
| `LocationShareResponse` | ✅ | ✅ | EXPERT only (owner) |
| `NearbyExpertResponse` | ✅ (coords) | ✅ | PUBLIC (mother/family view) |

---

## 5. Repository Layer

### 5.1 ExpertAvailabilityRepository

```java
@Repository
public interface ExpertAvailabilityRepository extends JpaRepository<ExpertAvailability, UUID> {

    /** Find all active (non-expired) availability windows for an expert */
    @Query("SELECT a FROM ExpertAvailability a " +
           "WHERE a.expertProfileId = :profileId " +
           "AND a.endAt > :now " +
           "ORDER BY a.startAt ASC")
    List<ExpertAvailability> findActiveSlots(@Param("profileId") UUID profileId,
                                              @Param("now") Instant now);

    /** Find overlapping windows — used for overlap check */
    @Query("SELECT COUNT(a) FROM ExpertAvailability a " +
           "WHERE a.expertProfileId = :profileId " +
           "AND a.status IN ('AVAILABLE', 'BUSY') " +
           "AND a.startAt < :end AND a.endAt > :start")
    long countOverlapping(@Param("profileId") UUID profileId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    /** Delete expired slots — maintenance query */
    @Modifying
    @Query("DELETE FROM ExpertAvailability a WHERE a.endAt < :now AND a.status = 'EXPIRED'")
    int deleteExpiredBefore(@Param("now") Instant now);

    /** Find experts with available slots in a channel — UC-65 livesearch */
    @Query("SELECT DISTINCT a.expertProfileId FROM ExpertAvailability a " +
           "WHERE a.channelType = :channel " +
           "AND a.endAt > :now " +
           "AND a.status IN ('AVAILABLE', 'BUSY')")
    List<UUID> findExpertsAvailableInChannel(@Param("channel") ChannelType channel,
                                             @Param("now") Instant now);
}
```

### 5.2 ExpertLocationShareRepository

```java
@Repository
public interface ExpertLocationShareRepository extends JpaRepository<ExpertLocationShare, UUID> {

    /** Find active share for an expert (not expired) */
    @Query("SELECT s FROM ExpertLocationShare s " +
           "WHERE s.expertProfileId = :profileId " +
           "AND s.sharedAt > :cutoff " +
           "ORDER BY s.sharedAt DESC")
    List<ExpertLocationShare> findActiveByExpert(@Param("profileId") UUID profileId,
                                                  @Param("cutoff") Instant cutoff,
                                                  Pageable pageable);

    /** Find active shares within a bounding box — UC-88 proximity search */
    @Query("SELECT s FROM ExpertLocationShare s " +
           "WHERE s.expertProfileId IN :profileIds " +
           "AND s.latitude BETWEEN :minLat AND :maxLat " +
           "AND s.longitude BETWEEN :minLng AND :maxLng " +
           "AND (:expiresAt IS NULL OR s.expiresAt > :expiresAt OR s.expiresAt IS NULL) " +
           "AND s.availabilityStatus = 'AVAILABLE'")
    List<ExpertLocationShare> findInBoundingBox(
        @Param("profileIds") List<UUID> profileIds,
        @Param("minLat") BigDecimal minLat, @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng, @Param("maxLng") BigDecimal maxLng,
        @Param("expiresAt") Instant expiresAt,
        Pageable pageable);

    /** Delete expired shares — maintenance query */
    @Modifying
    @Query("DELETE FROM ExpertLocationShare s " +
           "WHERE s.expiresAt IS NOT NULL AND s.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);

    /** Find experts in channel that have location share */
    @Query("SELECT s.expertProfileId FROM ExpertLocationShare s " +
           "WHERE s.availabilityStatus = 'AVAILABLE' " +
           "AND (:expiresAt IS NULL OR s.expiresAt > :expiresAt OR s.expiresAt IS NULL)")
    List<UUID> findExpertsWithLocationShare(
        @Param("expiresAt") Instant expiresAt,
        Pageable pageable);
}
```

---

## 6. Service Layer

### 6.1 Service Interfaces

```java
public interface ExpertAvailabilityService {

    /** UC-64: EXPERT đăng lịch rảnh */
    ExpertAvailabilityResponse createAvailability(UUID expertUserId,
                                                   CreateAvailabilityRequest req);

    /** UC-64: Xem lịch rảnh của chính mình */
    List<ExpertAvailabilityResponse> getMyAvailability(UUID expertUserId);

    /** UC-65: Xem lịch rảnh của một expert (read-only) */
    List<ExpertAvailabilityResponse> getExpertAvailability(UUID expertProfileId);

    /** UC-64: Xóa slot */
    void deleteSlot(UUID expertUserId, UUID availabilityId);

    /** UC-88: Search experts with availability */
    List<NearbyExpertResponse> searchAvailableExperts(ExpertAvailabilityQuery query);
}
```

```java
public interface ExpertLocationShareService {

    /** UC-79: EXPERT chia sẻ vị trí hiện tại */
    LocationShareResponse shareLocation(UUID expertUserId, ShareLocationRequest req);

    /** UC-78: Dừng chia sẻ */
    void stopSharing(UUID expertUserId);

    /** UC-80/Location: Lấy vị trí hiện tại của expert */
    Optional<ExpertLocationShare> getCurrentLocation(UUID expertUserId);

    /** UC-88: Proximity search */
    List<NearbyExpertResponse> findNearbyExperts(
        BigDecimal userLat, BigDecimal userLng,
        double radiusKm, String channelType);
}
```

### 6.2 Core Workflows

#### UC-64: Expert posts availability notification

```
1. Get ExpertProfile by expertUserId (via PKG-01 ExpertProfileService or repo)
2. Verify profile.verificationStatus = VERIFIED → EXPERT-012 if not
3. Verify profile.trustStatus = ACTIVE → EXPERT-013 if SUSPENDED/RESTRICTED
4. Validate: endAt > startAt → EXPERT-011 if not
5. [Optional] Check overlap via repo.countOverlapping → EXPERT-009 (warning, Sprint 2 enforce)
6. Create ExpertAvailability entity with status
7. Emit AuditEvent: "EXPERT_POSTED_AVAILABILITY"
8. Return ExpertAvailabilityResponse
```

#### UC-79: Expert shares location

```
1. Get ExpertProfile by expertUserId
2. Verify profile.verificationStatus = VERIFIED
3. Validate lat ∈ [-90,90], lng ∈ [-180,180] → EXPERT-014
4. [Optional] Check consentReference → EXPERT-015 if null in context
5. (Optional for UC-79 — defer to PKG-06)
   // UC-88 integration: update existing ExpertLocationShare or insert new
6. Create ExpertLocationShare entity
7. Emit AuditEvent: "EXPERT_SHARED_LOCATION"
8. Return LocationShareResponse
```

#### UC-78 + UC-80+LẬP: Expert stops sharing

```
1. Get ExpertProfile by expertUserId
2. Delete active shares (soft delete: set expiresAt = now)
3. Emit AuditEvent: "EXPERT_STOPPED_LOCATION_SHARE"
4. Return 204 No Content
```

#### UC-88: Nearby expert search (integrated, expertavailability package owns)

```
1. Accept: userLat, userLng, radiusKm, [channelType]
2. Calculate bounding box: minLat, maxLat, minLng, maxLng (approximate: 1° ≈ 111km)
   Note: Full Haversine distance via TrackAsia stubbed in PKG-04 Sprint 2
3. Query ExpertAvailabilityRepository.findExpertsAvailableInChannel → candidate profileIds
4. Query ExpertLocationShareRepository.findInBoundingBox (filter candidates)
5. Join with ExpertBadgeReadPort.isVerified() → sort by distance
6. Return top N (default 20) NearbyExpertResponse
```

---

## 7. Controller Specification

```java
@RestController
@RequestMapping("/api/v1/expert")
@RequiredArgsConstructor
@Validated
public class ExpertAvailabilityController {

    private final ExpertAvailabilityService availabilityService;
    private final ExpertLocationShareService locationService;
    private final ExpertProfileService expertProfileService; // PKG-01
```

### UC-64: POST /api/v1/expert/availability

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Request Body: CreateAvailabilityRequest
Success: 201 Created — ExpertAvailabilityResponse
Errors:  400 EXPERT-011 (endAt ≤ startAt)
         403 EXPERT-012 (not VERIFIED)
         403 EXPERT-013 (not ACTIVE)
         401 no token
```

### UC-64: GET /api/v1/expert/availability/me

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Success: 200 — List<ExpertAvailabilityResponse>
Errors:  403 EXPERT-012 (not VERIFIED)
         401 no token
```

### UC-64/UC-65: GET /api/v1/expert/availability/{expertProfileId}

```
Request Auth: Public (no role check) — UC-65 allows Mother/Family read
Success: 200 — List<ExpertAvailabilityResponse>
Errors:  404 EXPERT-004
```

### UC-64: DELETE /api/v1/expert/availability/{availabilityId}

```
Request Auth: @PreAuthorize("hasRole('EXPERT')") + owner check
Success: 204 No Content
Errors:  404 EXPERT-010
         403 EXPERT-005 (not owner)
```

### UC-79: POST /api/v1/expert/location/share

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Request Body: ShareLocationRequest
Success: 200 — LocationShareResponse
Errors:  400 EXPERT-014 (invalid coords)
         403 EXPERT-015 (no consentReference when required)
         403 EXPERT-012 (not VERIFIED)
         403 EXPERT-013 (not ACTIVE)
```

### UC-78: DELETE /api/v1/expert/location/share (stop sharing)

```
Request Auth: @PreAuthorize("hasRole('EXPERT')")
Success: 204 No Content
Errors:  403 EXPERT-014
```

### UC-65: GET /api/v1/expert/availability/channel/{channelType}

```
Request Auth: Public
Query Params: @RequestParam(defaultValue = "20") int limit
Success: 200 — List<NearbyExpertResponse> (top-N experts available in channel)
Errors:  400 invalid channel type
```

### UC-88 (deferred to PKG-06): GET /api/v1/nearby/experts

```
Deferred to PKG-06. Controller stub → 501 NOT IMPLEMENTED in Sprint 1.
Full implementation in Sprint 2.
```

---

## 8. Error Codes

| Code | HTTP | Trigger | Fix tried |
|------|------|---------|-----------|
| `EXPERT-001` | 400 | Blank required field | — |
| `EXPERT-002` | 409 | Duplicate resource | — |
| `EXPERT-003` | 403 | Update blocked by state | — |
| `EXPERT-004` | 404 | ExpertProfile not found | — |
| `EXPERT-005` | 403 | Not owner / not authorized | — |
| `EXPERT-006` | 400 | Verification field invalid | — |
| `EXPERT-007` | 403 | RBAC — wrong role | — |
| `EXPERT-008` | 403 | Disabled account | — |
| `EXPERT-009` | 409 | Overlapping availability window | Overlap detected; defer to Sprint 2 |
| `EXPERT-010` | 404 | Availability slot not found | — |
| `EXPERT-011` | 400 | endAt must be after startAt | — |
| `EXPERT-012` | 403 | Expert profile not VERIFIED | — |
| `EXPERT-013` | 403 | Expert profile SUSPENDED/RESTRICTED | — |
| `EXPERT-014` | 400 | Invalid latitude/longitude range | — |
| `EXPERT-015` | 403 | Consent reference required / invalid | Required for location share |

---

## 9. Authorization Matrix: Availability + Location

| Endpoint | EXPERT | MOTHER | FAMILY | MODERATOR | ADMIN |
|----------|:------:|:------:|:------:|:---------:|:-----:|
| POST /availability | ✅ (own) | ❌ | ❌ | ❌ | ❌ |
| GET /availability/me | ✅ (own) | ❌ | ❌ | ❌ | ❌ |
| GET /availability/{id} | ✅ | ✅ | ✅ | ✅ | ✅ |
| DELETE /availability/{id} | ✅ (own) | ❌ | ❌ | ❌ | ❌ |
| POST /location/share | ✅ (own) | ❌ | ❌ | ❌ | ❌ |
| DELETE /location/share | ✅ (own) | ❌ | ❌ | ❌ | ❌ |
| GET /availability/channel/{type} | ✅ | ✅ | ✅ | ✅ | ✅ |

---

## 10. Logic Issues & Resolutions

| # | Issue in initial design | Resolution |
|---|------------------------|------------|
| L1 | UC-88 proximity math in service | Defer Haversine to PKG-04; Sprint 1 uses bounding box |
| L2 | location_share.no FK on user (via expertProfileId) | App-layer check: ExpertProfile exists → ExpertProfileService |
| L3 | UC-65 search — Mother reads expert availability without login | Public endpoint; returns expert display name + channel |
| L4 | UC-80 "location snapshot" — different from location_share | `location_snapshots` table is LOCATION package (PKG-05); `expert_location_shares` is expertavailability (PKG-03). **Different tables, different UCs.** |

---

## 11. AI Constraints (CASE 2.0)

| # | Constraint |
|---|-----------|
| C1 | POST availability: `@PreAuthorize("hasRole('EXPERT')")`; Expert must be VERIFIED + ACTIVE |
| C2 | POST location/share: `@PreAuthorize("hasRole('EXPERT')")`; verify VERIFIED |
| C3 | endAt > startAt validated in service layer BEFORE any DB write |
| C4 | Latitude/longitude ranges validated in @Valid (DTO) + service layer (defense in depth) |
| C5 | expertProfileId from SecurityUtils, NOT request body (prevents IDOR) |
| C6 | Public GET endpoints: do NOT return location of MOTHER-only sensitive data |
| C7 | No location data returned for SUSPENDED/RESTRICTED experts |

---

## 12. Anti-Pattern Detection (CASE 2.0)

| Pattern | Description | Check |
|---------|-------------|-------|
| AP-AVAIL-001 | @PreAuthorize missing on all EXPERT endpoints | ❌ All endpoints must have `@PreAuthorize("hasRole('EXPERT')")` |
| AP-AVAIL-002 | Owner check bypass — no profile lookup | ❌ Always fetch ExpertProfile by userId before credential ops |
| AP-AVAIL-003 | Missing audit events on location share/unshare | ❌ Audit event on every share/stop |
| AP-AVAIL-004 | Location exposure to SUSPENDED experts | ❌ isVerified() check before returning location |
| AP-AVAIL-005 | Hard delete on location shares | ❌ Use soft delete (expiresAt) only |
| AP-AVAIL-006 | Coordinates validated only in DTO | ❌ Also validate in service layer (defense in depth) |
| AP-AVAIL-007 | Overlap check bypass in Sprint 1 | ✅ Deliberately skipped (ADR-AVAIL-001) |

---

## 13. Implementation Order (Complete)

1. `entity/AvailabilityStatus.java`, `ChannelType.java` — enums
2. `entity/ExpertAvailability.java` — JPA entity
3. `entity/ExpertLocationShare.java` — JPA entity
4. `repository/ExpertAvailabilityRepository.java` — CRUD + custom queries
5. `repository/ExpertLocationShareRepository.java` — CRUD + proximity query
6. `exception/ExpertException.java` — add EXPERT-009..015 codes
7. `dto/request/CreateAvailabilityRequest.java` — record
8. `dto/request/ShareLocationRequest.java` — record
9. `dto/response/ExpertAvailabilityResponse.java` — record
10. `dto/response/LocationShareResponse.java` — record
11. `dto/response/NearbyExpertResponse.java` — record
12. `dto/mapper/AvailabilityMapper.java` — entity ↔ DTO
13. `service/ExpertAvailabilityServiceImpl.java` — full logic
14. `service/ExpertLocationShareServiceImpl.java` — full logic
15. `controller/ExpertAvailabilityController.java` — REST endpoints
16. Tests (see Test-Spec)

**Migration:** NOT NEEDED — V1 tables exist (`expert_availability`, `expert_location_shares`, FK on `expert_profile_id`).

---

## 14. Integration Contracts With Other Packages

| Contract | Direction | Consumer | What is consumed |
|----------|-----------|----------|-------------------|
| `ExpertProfileService` | Read | PKG-03 | ExpertProfile by userId, verificationStatus, trustStatus |
| `ExpertBadgeReadPort` | Read | UC-65/UC-88 | isVerified(userId), getBadgeInfo(userId) |
| `AuditEventService` | Write | All | Emit availability/location events |

---

*CareBridge TDS v1.0 — PKG-03 Expert Availability & Location Share*
