# MF-07 / Spec 01 — Emergency Map, Nearby Facility Search & Route Navigation

| Field | Value |
| --- | --- |
| Feature | MF-07 — Emergency Map & Nearby Care Support |
| Use Cases Covered | UC-77 Open Emergency Map, UC-78 Find Nearby Care Facilities, UC-79 View Route, ETA and Quick Call or Navigate |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Implementation Status | Partial — active flow exists, but the focused Mobile provider-label/route widget test currently fails for a nullable facility ID |
| Main Flow Summary | A Mother opens the emergency map after a location-permission and non-dispatch notice, reviews nearby care-facility results with their source/status labels, views route/ETA and triggers a quick call or external navigation through the device's native capability. |
| Grounding (source code) | `map/entity/CareFacility.java`, `FacilityStatus.java`, `map/controller/CareFacilityController.java` (`/api/v1/map/nearby-facilities`, `/facilities`, `/route`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

UC-77 (mở bản đồ) không tạo bản ghi backend — đó là bước xin quyền vị trí và hiển thị
màn hình phía client (theo D-05, hành vi màn hình không tách UC riêng có backend). Luồng
backend thực sự bắt đầu từ UC-78: hệ thống trả về cơ sở chăm sóc gần vị trí được người
dùng cho phép và giữ nhãn nguồn/trạng thái để UI không trình bày dữ liệu ngoài như đã
được CareBridge xác minh. UC-79 dùng toạ độ facility đã chọn để tính route/ETA
(`POST /route`) rồi giao cho thiết bị xử lý gọi nhanh/điều hướng (native call/maps
intent) — CareBridge chỉ tính toán route, **không tự dispatch xe cấp cứu hay đảm bảo
chuyên gia đến** (đúng phạm vi loại trừ trong SRS 4.8).

## 2. Class Diagram

```plantuml
@startuml MF07_01_EmergencyMap_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class CareFacility {
  + facilityId: UUID
  + name: String
  + facilityType: String
  + address: String
  + latitude: BigDecimal
  + longitude: BigDecimal
  + phone: String
  + openingHoursJson: String
  + sourceType: String
  + verificationStatus: FacilityStatus
}

enum FacilityStatus {
  VERIFIED
  PENDING
  REJECTED
  UNVERIFIED
}

class NearbyResponse <<read-model>> {
  + facilities: List<FacilityResponse>
  + userLatitude: BigDecimal
  + userLongitude: BigDecimal
}

class RouteRequest {
  + originLat: BigDecimal
  + originLng: BigDecimal
  + facilityId: UUID
}

class RouteResponse <<read-model>> {
  + distanceMeters: int
  + etaMinutes: int
  + polyline: String
  + facilityPhone: String
}

class CareFacilityController {
  - careFacilityService: ICareFacilityService
  + nearbyFacilities(lat, lng, radius): ResponseEntity
  + facilities(filter): ResponseEntity
  + facilityDetail(id): ResponseEntity
  + getRoute(RouteRequest): ResponseEntity
}

interface ICareFacilityService <<interface>> {
  + findNearby(lat: BigDecimal, lng: BigDecimal, radiusKm: double): NearbyResponse
  + getRoute(request: RouteRequest): RouteResponse
}

class CareFacilityServiceImpl implements ICareFacilityService {
  - careFacilityRepository: CareFacilityRepository
  - trackAsiaClient: TrackAsiaClient
}

CareFacility --> FacilityStatus
CareFacilityController --> ICareFacilityService : uses
CareFacilityServiceImpl ..> NearbyResponse : builds (current code may include UNVERIFIED)
CareFacilityServiceImpl --> TrackAsiaClient : nearby search + route/ETA
CareFacilityServiceImpl ..> RouteResponse : builds

@enduml
```

**Hình 1 — Class Diagram: Care Facility & Route/ETA Read-Model**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF07_01_EmergencyMap_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Mother" as M
participant "Mobile App (Client)" as App
participant "CareFacilityController" as Controller
participant "CareFacilityServiceImpl" as Service
participant "CareFacilityRepository" as FacilityRepo
database "PostgreSQL" as DB
participant "TrackAsiaClient" as TrackAsia
participant "Device Capability\n(Dialer / Maps)" as Device

== UC-77 Open Emergency Map ==
M -> App : 1. Open Emergency Map
activate App
App -> App : 1a. Request device location permission + display\ndisclaimer "not an emergency service"
activate App
App --> App : 1b. permission state
deactivate App
App -> App : 1c. Guard — only proceed when permission granted
activate App
App --> App : 1d. allowed | show permission guidance
deactivate App

== UC-78 Find Nearby Care Facilities ==
App -> Controller : 2. GET /api/v1/map/nearby-facilities?lat=&lng=&radiusMeters=&type=
activate Controller
Controller -> Service : 3. searchNearby(lat, lng, radiusMeters, type)
activate Service
alt [TrackAsia returns valid result (preferred source)]
  Service -> TrackAsia : 4a. searchNearby(lat, lng, radiusMeters, type)
  activate TrackAsia
  TrackAsia --> Service : 4b. GeoJSON features[] (nearby POIs)
  deactivate TrackAsia
  Service -> Service : 4c. parseTrackAsiaResults() → FacilityResponse[]\n(sourceType="TRACKASIA", verificationStatus="UNVERIFIED" by default)
  activate Service
  Service --> Service : 4d. FacilityResponse[]
  deactivate Service
else [TrackAsia error/timeout/empty → fallback internal data]
  Service -> FacilityRepo : 4e. findNearby(lat, lng, radiusMeters)
  activate FacilityRepo
  FacilityRepo -> DB : 4f. SELECT * FROM care_facilities\nWHERE lat/lng NOT NULL AND earth_distance(...) <= radiusMeters*1000
  activate DB
  DB --> FacilityRepo : 4g. rows[] (do not filter by verification_status)
  deactivate DB
  FacilityRepo --> Service : 4h. facilities[]
  deactivate FacilityRepo
end
Service --> Controller : 5. NearbyResponse{facilities[], totalCount}
deactivate Service
Controller --> App : 6. HTTP 200 OK {facilities[]}
deactivate Controller
App --> M : 7. Display facilities on map
deactivate App

== UC-79 View Route, ETA and Quick Call or Navigate ==
M -> App : 8. Select 1 facility
activate App
App -> Controller : 9. POST /api/v1/map/route\n{fromLat, fromLng, toLat, toLng, transportMode}
activate Controller
Controller -> Service : 10. getRoute(request)
activate Service
Service -> TrackAsia : 11. route(fromLat, fromLng, toLat, toLng, transportMode)
activate TrackAsia
TrackAsia --> Service : 12. GeoJSON route{routes: [{distance, duration, steps[]}]}
deactivate TrackAsia
Service -> Service : 10a. parse first leg → distanceMeters, etaMinutes,\nRoutePoint list from steps[].maneuver
activate Service
Service --> Service : 10b. route read-model
deactivate Service
Service --> Controller : 13. RouteResponse{distanceMeters, etaMinutes, points[]}
deactivate Service
Controller --> App : 14. HTTP 200 OK {route}
deactivate Controller
App --> M : 15. Display route + ETA
deactivate App

alt [Mother selects Quick Call]
  M ->> Device : 16a. call facilityPhone (native dialer)
else [Mother selects Directions]
  M ->> Device : 16b. open native map app with destination coordinates (maps intent)
end

@enduml
```

**Hình 2 — Sequence Diagram: Open Map → Find Nearby Facilities → View Route/ETA → Quick Call or Navigate (Main Flow)**

> **Ghi chú grounding:** Code thật của
> `CareFacilityServiceImpl.searchNearby(...)` gọi **TrackAsia (nguồn ngoài) trước tiên**, và
> kết quả TrackAsia được gắn cứng `verificationStatus="UNVERIFIED"` — nghĩa là trong đường
> đi chính (happy path), UC-78 **có thể trả về facility chưa được admin xác minh**. Chỉ khi
> TrackAsia lỗi/rỗng, hệ thống mới fallback sang `CareFacilityRepository.findNearby(...)`,
> và truy vấn native đó **không lọc theo `verification_status='VERIFIED'`** — trả về mọi
> facility có toạ độ trong bán kính bất kể trạng thái xác minh. Điều này khác với khẳng định
> Vì vậy UI phải hiển thị nhãn nguồn/trạng thái và không được suy diễn mọi kết quả là đã
> được xác minh. Hành vi lọc theo VERIFIED hiện **không được enforce** ở tầng
> service/repository cho nearby search.
> Ngoài ra, `map/routeprovider/RouteProvider.java` tồn tại như một interface nhưng
> `CareFacilityServiceImpl` phụ thuộc thẳng vào `TrackAsiaClient` cụ thể, không qua
> interface này.


## 4. Business Rules Applied

- UC-77 — người dùng phải được thông báo rõ phạm vi "không dispatch cấp cứu" trước khi vào bản đồ (CC-01).
- UC-78 — vị trí chỉ được dùng sau khi người dùng cấp quyền hoặc chọn khu vực; kết quả phải giữ nhãn nguồn/trạng thái và không được ngầm quảng bá là đã xác minh.
- UC-79 — route/ETA là hỗ trợ tham khảo; gọi nhanh/điều hướng giao hoàn toàn cho năng lực thiết bị (dialer/maps app), CareBridge không tự thực hiện cuộc gọi hay điều hướng.
- Excluded (SRS 4.8) — không dispatch xe cấp cứu, không đảm bảo lịch trình cơ sở y tế.
- Verification gap — cần sửa và chạy lại `nearby_care_contract_test.dart` cho trường hợp provider label/route với facility ID nullable trước khi đánh dấu flow ổn định.
