# CAREBRIDGE TEST-DRIVEN DEVELOPMENT SPECIFICATION — PKG-04 Emergency Map & Facility Search

## Metadata

| Field | Value |
|-------|-------|
| **Document ID** | `CB-MAP-PKG-04-TDS` |
| **Version** | `1.0` |
| **Date** | 2026-07-08 |
| **Status** | `DRAFT` |
| **Package** | `PKG-04 — Emergency Map & Facility Search` |
| **Included UCs** | `UC-77, UC-78` |
| **Spec gốc** | `CB-MAP-PKG-06-TDS` (renamed + split) |
| **Priority** | 🔴 P0 |
| **Sprint** | `Sprint 2 (2026-07-17 → 2026-07-31)` |
| **Milestone** | `Demo Gate B` |

---

## CHANGELOG

| Ngày | Người | Nội dung |
|------|-------|----------|
| 2026-07-04 | Claude | Khởi tạo TDS (PKG-06 tạm) |
| 2026-07-08 | Lâm | Tách PKG-06 → PKG-04 (Map), PKG-05 (Location), PKG-06 (NearbyCare); đổi tên |

---

## 1. Module Info

| Field | Value |
|-------|-------|
| **Feature / UC IDs** | `UC-77, UC-78` |
| **Module** | `map — CareFacilityService + EmergencyMapService` |
| **Spec gốc** | `CB-MAP-PKG-04-TDS` |
| **Priority** | 🔴 P0 |
| **Upstream Dependencies** | `TV1 auth`, `PKG-01 expert (ExpertProfile)`, `PKG-05 location (LocationSnapshot)`, `TrackAsia SDK` |
| **Downstream Consumers** | `PKG-03 expertavailability (reads active experts)`, Mobile App (UC-017) |

---

## 2. Entity Definitions

### 2.1 CareFacility

```java
@Entity
@Table(name = "care_facilities", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CareFacility {

    @Id
    @Column(name = "facility_id", nullable = false, updatable = false)
    private UUID facilityId;

    @Column(name = "partner_id")
    private UUID partnerId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "facility_type", length = 50)
    private String facilityType;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "opening_hours_json", columnDefinition = "jsonb")
    private String openingHoursJson;

    @Column(name = "source_type", length = 30)
    private String sourceType;

    @Column(name = "verification_status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private FacilityVerificationStatus verificationStatus = FacilityVerificationStatus.UNVERIFIED;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Enum FacilityVerificationStatus:**
```java
public enum FacilityVerificationStatus {
    UNVERIFIED,   // Mới seed, chưa xác minh
    VERIFIED,     // Đã xác minh bởi partner hoặc admin
    SUSPENDED,    // Tạm dừng
    CLOSED        // Đã đóng cửa
}
```

**Constraint:** table has FK to `partner_organizations(partner_id)` for `VERIFIED` status; `UNVERIFIED` allows null partnerId (admin can later link).

### 2.2 EmergencyEvent

```java
@Entity
@Table(name = "emergency_events", schema = "public")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmergencyEvent {

    @Id
    @Column(name = "emergency_event_id", nullable = false, updatable = false)
    private UUID emergencyEventId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_reference_id")
    private UUID sourceReferenceId;

    @Column(name = "risk_level", length = 20)
    private EmergencyRiskLevel riskLevel;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "selected_facility_id")
    private UUID selectedFacilityId;

    @Column(name = "selected_expert_id")
    private UUID selectedExpertId;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EmergencyEventStatus status = EmergencyEventStatus.OPEN;

    @Column(name = "opened_at", nullable = false)
    @CreatedDate
    private Instant openedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @LastModifiedDate
    private Instant updatedAt;
}
```

**Enum EmergencyEventStatus:**
```
OPEN        // Đang xử lý
RESPONDED   // Đã nhận yêu cầu (expert/mother đã phản hồi)
CLOSED      // Đã kết thúc
```

**Enum EmergencyRiskLevel:**
```
GREEN   // Low risk — informational
YELLOW  // Medium risk — caution needed
RED     // High risk — immediate action
```

---

## 3. ADRs

### ADR-MAP-001: Facilities seeded at app startup, not Flyway

**Context:** CareFacility requires TrackAsia embedding; seed data from external API.
**Decision:** Seed facility data via `CommandLineRunner` at startup in dev, via admin API in prod. No Flyway INSERT for facilities.
**Consequences:** ✅ Flexible; ⚠️ Requires network at startup.

### ADR-MAP-002: Route calculation via TrackAsia SDK, fallback to straight-line

**Context:** UC-79 requires ETA. Full routing requires TrackAsia.
**Decision:** Summer 1 = straight-line distance (Haversine) as placeholder. TrackAsia integration (V2) uses `RouteProvider` contract. `route_distance_meters` and `route_eta_seconds` in response can be null in Sprint 1.
**Consequences:** ✅ Delivers UI-first Sprint 1; ⚠️ ETA inaccurate for non-straight-line routes.

### ADR-MAP-003: care_facilities.verification_status controls visibility in map

**Context:** Only VERIFIED facilities should appear in emergency map search.
**Decision:** Repository query filters `verificationStatus = 'VERIFIED'`. ADMIN role can query all.
**Consequences:** ✅ Mothers only see verified safe facilities; ✅ Admin can audit unverified.

---

## 4. Repository Layer

### 4.1 CareFacilityRepository

```java
@Repository
public interface CareFacilityRepository extends JpaRepository<CareFacility, UUID> {

    /** Find VERIFIED facilities near a point — UC-78 */
    @Query("SELECT f FROM CareFacility f " +
           "WHERE f.verificationStatus = 'VERIFIED' " +
           "AND f.latitude BETWEEN :minLat AND :maxLat " +
           "AND f.longitude BETWEEN :minLng AND :maxLng")
    List<CareFacility> findNearbyVerified(
        @Param("minLat") BigDecimal minLat,  @Param("maxLat") BigDecimal maxLat,
        @Param("minLng") BigDecimal minLng,  @Param("maxLng") BigDecimal maxLng,
        Pageable pageable);

    /** Find by type — for filtering */
    @Query("SELECT f FROM CareFacility f WHERE f.verificationStatus = 'VERIFIED' " +
           "AND (:facilityType IS NULL OR f.facilityType = :facilityType)")
    List<CareFacility> findByType(@Param("facilityType") String facilityType,
                                  Pageable pageable);

    /** Admin: all facilities regardless of status */
    List<CareFacility> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Count verified facilities by partner (partner dashboard) */
    long countByPartnerIdAndVerificationStatus(UUID partnerId,
                                                FacilityVerificationStatus status);
}
```

### 4.2 EmergencyEventRepository

```java
@Repository
public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, UUID> {

    /** Find open events for a user — UC-77 */
    @Query("SELECT e FROM EmergencyEvent e " +
           "WHERE e.userId = :userId AND e.status = 'OPEN' " +
           "ORDER BY e.openedAt DESC")
    List<EmergencyEvent> findOpenByUser(@Param("userId") UUID userId, Pageable pageable);

    /** Find open events assigned to a facility — partner view */
    @Query("SELECT e FROM EmergencyEvent e WHERE e.selectedFacilityId = :facilityId " +
           "AND e.status IN ('OPEN', 'RESPONDED')")
    List<EmergencyEvent> findActiveByFacility(@Param("facilityId") UUID facilityId,
                                               Pageable pageable);
}
```

---

## 5. Service Layer

### 5.1 Service Interface

```java
public interface CareFacilityService {

    /** UC-78: Search nearby verified facilities */
    List<CareFacilityResponse> searchNearby(BigDecimal lat, BigDecimal lng,
                                             double radiusKm,
                                             String facilityType,
                                             int limit);

    /** UC-78: Facility detail */
    Optional<CareFacilityResponse> getFacilityDetail(UUID facilityId);

    /** Admin: Approve/reject facility */
    CareFacilityResponse updateFacilityStatus(UUID facilityId,
                                               FacilityVerificationStatus newStatus);

    /** Admin: Seed facilities from TrackAsia / static data */
    List<CareFacilityResponse> seedFacilities(List<FacilitySeedRequest> seeds);

    /** Admin: Partner views their facilities */
    List<CareFacilityResponse> getPartnerFacilities(UUID partnerUserId);
}
```

```java
public interface EmergencyMapService {

    /** UC-77: Open emergency map (handoff from TV5) */
    EmergencyMapResponse openEmergencyMap(UUID triageHandoffId, UUID userId);

    /** UC-77: Close emergency event */
    void closeEmergencyEvent(UUID emergencyEventId, UUID userId);

    /** UC-77: Refresh map (pull latest user location + facilities) */
    EmergencyMapView getMapView(UUID emergencyEventId, UUID userId);
}
```

### 5.2 Core Workflows

#### UC-77: Open Emergency Map (handoff)

```
Input: triageHandoffId (from TV5), userId (mother)
1. Load emergency_event by triageHandoffId / userId
   → if OPEN exists, load it; else create new with source_type=TRIAGE_HANDOFF
2. Load user's latest LocationSnapshot (PKG-05 owns this table)
   If PKG-05 is not yet implemented, return user location from user_profile
3. Call care_facility search (see UC-78) with location + default radius (5km)
4. For each facility, compute distance via Haversine (or TrackAsia route in Sprint 2)
5. Emit AuditEvent: "EMERGENCY_MAP_OPENED"
6. Return EmergencyMapView: {userLocation, nearestFacilities, routeGuide}
```

#### UC-78: Search Nearby Facilities

```
Input: lat, lng, radiusKm, [facilityType], [limit]
1. Validate lat/lng ranges → 400 MAP-001 if invalid
2. Compute bounding box (±0.009° per km approx)
3. Query CareFacilityRepository.findNearbyVerified(bbox)
4. For each candidate, compute Haversine distance to user
5. Sort by distance, take top N
6. For each: {facilityId, name, type, address, lat/lng, distanceKm, phone, openingHoursJson, rating}
7. If facilityType = "HOSPITAL", prioritize hospitals over clinics
IF TrackAsia configured:
   - Map to TrackAsia geocode first
   - Use TrackAsia route API for accurate distance + ETA
   - Fallback to Haversine if TrackAsia fails
8. Return List<CareFacilityResponse>
```

#### CareFacility → Patient Relationship (Part C)

CareFacility is the **service provider entity** representing physical care locations (clinics, hospitals, community health centers). It participates in these relationships:

| Relationship | Direction | Description |
|-------------|-----------|-------------|
| **Provider → Patient** | CareFacility has many Patients | Patients are registered/treated at a facility (likely via ConsultationBooking or EmergencyEvent). A patient's `selected_facility_id` in emergency_events FK points to the CareFacility. |
| **Provider ← Admin/Parter** | PartnerOrganization manages facilities | `partner_organizations.partner_id` FK in care_facilities.partner_id. Partners can register and manage their facilities. |
| **Provider ← Expert** | Expert may be affiliated with a facility | ExpertProfile points to facility via `workplace` field (text) — not a DB FK currently. |
| **Provider ← TrackAsia** | Location data enrichment | Facilities seeded with TrackAsia map data; TrackAsia ID stored as `source_type`. |

**UC-77 flow connects patient to facility:**
```
Mother → TV5 triage → RED RISK → TV5 emits TriageHandoff
   → TV4 UC-77: emergency_event created (status=OPEN)
   → UC-78: find nearest CareFacility with VERIFIED status
   → UC-79: route to it (TrackAsia/Haversine)
   → UC-80: expert linked to it if needed
```

---

## 6. Controller Specification

```java
@RestController
@RequestMapping("/api/v1/nearbycare")
@RequiredArgsConstructor
@Validated
public class CareFacilityController {
    private final CareFacilityService facilityService;
    private final EmergencyMapService mapService;
```

### UC-78: GET /api/v1/nearbycare/facilities

```
Request Auth: @AuthenticationPrincipal OR JWT (any logged-in user; or anonymous = fallback)
Query Params:
  lat (required, BigDecimal), lng (required, BigDecimal)
  radiusKm (optional, default=5, min=0.1, max=50)
  facilityType (optional: HOSPITAL, CLINIC, PHARMACY, COMMUNITY_HEALTH_CENTER)
  limit (optional, default=20, max=100)
Success: 200 — List<CareFacilityResponse>
Errors:  400 MAP-001 (invalid lat/lng)
        401 no token (anonymous allowed for facility search — no sensitive data)
```

### UC-78: GET /api/v1/nearbycare/facilities/{facilityId}

```
Request Auth: Public
Success: 200 — CareFacilityResponse (detail with opening hours)
Errors:  404 MAP-002
```

### UC-77: POST /api/v1/nearbycare/map/open

```
Request Auth: @PreAuthorize("hasRole('MOTHER') or hasRole('FAMILY') or hasRole('SYSTEM_ADMIN')")
Request Body: { triageHandoffId: UUID }  (null if direct open from mobile)
Success: 201 — EmergencyMapView {emergencyEventId, userLocation, facilities, selectedFacility}
Errors:  400 MAP-003 (no location available)
        404 AUTH-001
        403 wrong role
```

### UC-77: PUT /api/v1/nearbycare/map/{eventId}/select-facility

```
Request Auth: JWT + owner (or ADMIN)
Request Body: { facilityId: UUID, expertProfileId: UUID (optional) }
Effect:   Sets emergency_event.selected_facility_id AND/OR selected_expert_id
Success: 200 — EmergencyMapView (updated)
Errors:  404 MAP-004 (event or facility not found)
```

### UC-77: POST /api/v1/nearbycare/map/{eventId}/close

```
Request Auth: JWT + owner (or ADMIN)
Effect:   Sets emergency_event.status = CLOSED, closed_at = now
Success: 200 — { emergencyEventId }
Errors:  404
```

### ADMIN: PUT /api/v1/admin/facilities/{id}/verify

```
Request Auth: @PreAuthorize("hasRole('SYSTEM_ADMIN') or hasRole('CONTENT_ADMIN')")
Request Body: { verificationStatus: 'VERIFIED'|'CLOSED' }
Success: 200 — CareFacilityResponse
Errors:  403, 404 MAP-002
```

### ADMIN: POST /api/v1/admin/facilities/seed

```
Request Auth: @PreAuthorize("hasRole('SYSTEM_ADMIN')")
Request Body: List<FacilitySeedRequest>
Success: 201 — List<CareFacilityResponse>
Errors:  403 wrong role
```

### PARTNER: GET /api/v1/partner/facilities (own facilities)

```
Request Auth: @PreAuthorize("hasRole('PARTNER')")
Success: 200 — List<CareFacilityResponse>
```

---

## 7. Error Codes

| Code | HTTP | Trigger |
|------|------|---------|
| `MAP-001` | 400 | Invalid latitude/longitude |
| `MAP-002` | 404 | CareFacility not found |
| `MAP-003` | 400 | No location data available for user |
| `MAP-004` | 404 | Emergency event not found |
| `MAP-005` | 400 | No verified facility within radius |
| `MAP-006` | 403 | User not authorized for this event |

---

## 8. DTOs

```java
// Request
public record FacilitySeedRequest(
    @NotBlank String name,
    String facilityType,    // HOSPITAL, CLINIC, PHARMACY, COMMUNITY
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String openingHoursJson,
    String sourceType,
    UUID partnerId
) {}

public record SelectFacilityRequest(
    @NotNull UUID facilityId,
    UUID expertProfileId
) {}

// Response
public record CareFacilityResponse(
    UUID facilityId,
    String name,
    String facilityType,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String openingHoursJson,
    String sourceType,
    FacilityVerificationStatus verificationStatus,
    Instant createdAt,
    BigDecimal distanceKm,        // only set on search results
    String rating                 // only set on detail/fetch
) {}

public record EmergencyMapView(
    UUID emergencyEventId,
    EmergencyRiskLevel riskLevel,
    EmergencyEventStatus status,
    BigDecimal userLatitude,
    BigDecimal userLongitude,
    List<CareFacilityResponse> nearbyFacilities,
    UUID selectedFacilityId,
    UUID selectedExpertId,
    String routeSummary         // ETA text, e.g. "7 min (2.3 km)" — null Sprint 1
) {}
```

---

## 9. AI Constraints (CASE 2.0)

| # | Constraint |
|---|-----------|
| C1 | GET /facilities — public but returns no user data; safe to access without login |
| C2 | POST /map/open — requires MOTHER/FAMILY role OR ADMIN |
| C3 | PUT /select-facility — user must be OWNER of the emergency event |
| C4 | Only VERIFIED facilities in public search results |
| C5 | Admin verify endpoint emits AuditEvent |
| C6 | No map/route data returned for SUSPENDED experts |

---

## 10. Anti-Pattern Detection

| Pattern | Description | Check |
|---------|-------------|-------|
| AP-MAP-001 | No radius clamp | ✅ Validate radiusKm ≤ 50km |
| AP-MAP-002 | Returns SUSPENDED expert in nearby | ✅ Filter by trustStatus = ACTIVE |
| AP-MAP-003 | Missing audit on facility verification | ✅ Audit event on verify/close |
| AP-MAP-004 | Hard delete on emergency event | ✅ Use status = CLOSED, never DELETE |

---

## 11. Implementation Order

1. `entity/FacilityVerificationStatus.java` — enum
2. `entity/EmergencyEventStatus.java`, `EmergencyRiskLevel.java` — enums
3. `entity/EmergencyEvent.java` — JPA entity (PK)
4. `repository/CareFacilityRepository.java` — custom queries
5. `repository/EmergencyEventRepository.java`
6. `exception/MapException.java` — MAP-001..006 codes
7. `dto/request/*` + `dto/response/*`
8. `mapper/FacilityMapper.java`
9. `service/CareFacilityServiceImpl.java`
10. `service/EmergencyMapServiceImpl.java`
11. `controller/CareFacilityController.java` + handoff controller
12. Tests (see Test-Spec)

**Migration:** NOT NEEDED — V1 tables exist (`care_facilities`, `emergency_events`, FK to users/experts).

---

*CareBridge TDS v1.0 — PKG-04 Emergency Map & Facility Search*
