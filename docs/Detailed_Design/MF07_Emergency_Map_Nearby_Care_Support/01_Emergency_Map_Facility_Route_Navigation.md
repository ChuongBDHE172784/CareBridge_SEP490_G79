# MF-07 / Spec 01 — Emergency Map, Nearby Facility and Navigation

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-49 Find Nearby Care Facility; UC-50 Call or Navigate to Care Facility |
| Use Case Group | Mobile App |
| Platform | Mother and Family Mobile; Backend; TrackAsia |
| Primary Actors | Mother / Family |
| In Scope | Provider status and ETA limitations stay visible |
| Explicitly Excluded | Nearby-expert requests |
| Implementation Trace | UI: EmergencyMapScreen; Controller: CareFacilityController; Service: CareFacilityServiceImpl; Repository: CareFacilityRepository; Entity: CareFacility |

## 1. Tổng quan luồng chính (Main Flow Overview)

Provider status and ETA limitations stay visible. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF07_01_EmergencyMapNearbyFacilityandNavigation_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "EmergencyMapScreen" as UI1 <<UI>>
class "CareFacilityController" as Controller1 <<Controller>> {
  - careFacilityService: ICareFacilityService
  + getFacility(id: UUID): ResponseEntity<ApiResponse<FacilityResponse>>
  + getAllFacilities(): ResponseEntity<ApiResponse<List<FacilityResponse>>>
  + getRoute(request: RouteRequest): ResponseEntity<ApiResponse<RouteResponse>>
}
class "CareFacilityServiceImpl" as Service1 <<Service>> {
  - trackAsiaClient: TrackAsiaClient
  - facilityRepository: CareFacilityRepository
  - parseTrackAsiaResults(root: JsonNode, originLat: double, originLng: double, ...): List<FacilityResponse>
  + getFacilityById(id: UUID): FacilityResponse
  + searchNearby(lat: BigDecimal, lng: BigDecimal, radiusMeters: Integer, ...): NearbyResponse
  + verifyFacility(facilityId: UUID, status: com.carebridge.backend.map.facilitystatus.FacilityStatus, adminId: UUID): void
  - normalizeFacilityType(value: String): String
}
interface "ICareFacilityService" as Service1Contract <<Service>>
interface "CareFacilityRepository" as Repository1 {
  + findByVerificationStatus(status: FacilityStatus): List<CareFacility>
  + findByActiveTrueOrderByNameAsc(): List<CareFacility>
  + findByFacilityIdAndActiveTrue(facilityId: UUID): Optional<CareFacility>
  + findByExternalSourceIdAndActiveTrue(externalSourceId: String): Optional<CareFacility>
  + findByProvinceIdAndActiveTrueOrderByNameAsc(provinceId: String): List<CareFacility>
  + findByProvinceIdAndDistrictIdAndActiveTrueOrderByNameAsc(provinceId: String, districtId: String): List<CareFacility>
}
class "CareFacility" as Entity1 <<Entity>> {
  - facilityId: UUID
  - partnerId: UUID
  - name: String
  - facilityType: String
  - facilityLevel: String
  - ownershipType: String
  - address: String
}
interface "JpaRepository<CareFacility, UUID>" as Repository1Base <<Framework>>
class "PostgreSQL" as DB <<Database>>
class "TrackAsia" as External <<External Service>>

Service1Contract <|.. Service1 : implements
Repository1Base <|-- Repository1 : extends
UI1 ..> Controller1 : invokes API
Controller1 --> Service1Contract : delegates
Service1 --> Repository1 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Service1 ..> External : invokes when required
@enduml
```

**Figure 1 — Class Diagram: Emergency Map, Nearby Facility and Navigation**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF07_01_EmergencyMapNearbyFacilityandNavigation_SequenceDiagram
skinparam shadowing false

actor "Mother / Family" as Actor
boundary ":EmergencyMapScreen" as UI1
control ":CareFacilityController" as Controller1
participant ":CareFacilityServiceImpl" as Service1 <<service>>
participant ":CareFacilityRepository" as Repository1 <<repository>>
database "PostgreSQL" as DB
participant ":TrackAsia" as External1 <<external system>>

group UC-49 Find Nearby Care Facility
  Actor -> UI1 : 1. startFindNearbyCareFacility()
  activate UI1
  UI1 -> Controller1 : 2. searchNearby(latitude, longitude, radius)
  activate Controller1
  Controller1 -> Service1 : 3. searchNearby(latitude, longitude, radius)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findByActiveTrueOrderByNameAsc()
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 -> External1 : 4a-4. geocodeAndEstimateRoute()
    activate External1
    External1 --> Service1 : 4a-5. integrationResult
    deactivate External1
    Service1 --> Controller1 : 4a-6. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-7. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-8. displayFindNearbyCareFacilityResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 4b. domainError
    deactivate Service1
    Controller1 --> UI1 : 4b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI1 --> Actor : 4b-2. displayActionableError()
    deactivate UI1
  end
end

group UC-50 Call or Navigate to Care Facility
  Actor -> UI1 : 5. startCallOrNavigateToCareFacility()
  activate UI1
  UI1 -> Controller1 : 6. getFacility(facilityId)
  activate Controller1
  Controller1 -> Service1 : 7. getFacilityById(facilityId)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 8a. findByFacilityIdAndActiveTrue()
    activate Repository1
    Repository1 -> DB : 8a-1. SELECT
    activate DB
    DB --> Repository1 : 8a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 8a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 8a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 8a-5. 200 OK
    deactivate Controller1
    UI1 -> External1 : 8a-6. openDialerOrNavigation()
    activate External1
    External1 --> UI1 : 8a-7. deviceActionResult
    deactivate External1
    UI1 --> Actor : 8a-8. displayCallOrNavigateToCareFacilityResult()
    deactivate UI1
  else [request is invalid, forbidden or unavailable]
    Service1 --> Controller1 : 8b. domainError
    deactivate Service1
    Controller1 --> UI1 : 8b-1. 400 / 401 / 403 / 404
    deactivate Controller1
    UI1 --> Actor : 8b-2. displayActionableError()
    deactivate UI1
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Emergency Map, Nearby Facility and Navigation Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-49 Find Nearby Care Facility; UC-50 Call or Navigate to Care Facility.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Provider status and ETA limitations stay visible.
- The following remains outside this contract: Nearby-expert requests.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
