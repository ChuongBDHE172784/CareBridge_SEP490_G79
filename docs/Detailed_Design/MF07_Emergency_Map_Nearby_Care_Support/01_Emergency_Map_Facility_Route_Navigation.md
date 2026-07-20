# MF-07 / Spec 01 — Emergency Map, Nearby Facility Search & Route Navigation

| Field | Value |
| --- | --- |
| Feature | MF-07 — Emergency Map & Nearby Care Support |
| Use Cases Covered | UC-77 Open Emergency Map, UC-78 Find Nearby Care Facilities, UC-79 View Route, ETA and Quick Call or Navigate |
| Primary Actor(s) | Mother |
| Platform | Mother Mobile App |
| Main Flow Summary | A Mother opens the emergency map (after being informed of the location-permission and non-dispatch scope), the app resolves nearby verified care facilities from her approved location, and she views route/ETA and triggers a quick call or turn-by-turn navigation to a selected facility through the device's native capability. |
| Grounding (source code) | `map/entity/CareFacility.java`, `FacilityStatus.java`, `map/controller/CareFacilityController.java` (`/api/v1/map/nearby-facilities`, `/facilities`, `/route`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

UC-77 (mở bản đồ) không tạo bản ghi backend — đó là bước xin quyền vị trí và hiển thị
màn hình phía client (theo D-05, hành vi màn hình không tách UC riêng có backend). Luồng
backend thực sự bắt đầu từ UC-78: hệ thống trả về `CareFacility` đã `VERIFIED` gần vị
trí được người dùng cho phép. UC-79 dùng toạ độ facility đã chọn để tính route/ETA
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
  + partnerId: UUID
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
  - routingClient: TrackAsiaRoutingClient
}

CareFacility --> FacilityStatus
CareFacilityController --> ICareFacilityService : uses
CareFacilityServiceImpl ..> NearbyResponse : builds (chỉ facility VERIFIED)
CareFacilityServiceImpl --> TrackAsiaRoutingClient : tính route/ETA
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
participant "TrackAsiaClient" as TrackAsia
participant "CareFacilityRepository" as FacilityRepo
database "PostgreSQL" as DB
participant "Device Capability\n(Dialer / Maps)" as Device

== UC-77 Open Emergency Map ==
M -> App : 1. Mở Emergency Map
activate App
App -> App : 2. Xin quyền vị trí thiết bị + hiển thị\ndisclaimer "không phải dịch vụ cấp cứu"
App -> App : 3. Guard — chỉ tiếp tục khi permission granted

== UC-78 Find Nearby Care Facilities ==
App -> Controller : 4. GET /api/v1/map/nearby-facilities?lat=&lng=&radiusMeters=&type=
activate Controller
Controller -> Service : 5. searchNearby(lat, lng, radiusMeters, type)
activate Service
alt 6. TrackAsia trả kết quả hợp lệ (nguồn ưu tiên)
  Service -> TrackAsia : 6. searchNearby(lat, lng, radiusMeters, type)
  activate TrackAsia
  TrackAsia --> Service : 7. GeoJSON features[] (POI gần đây)
  deactivate TrackAsia
  Service -> Service : 8. parseTrackAsiaResults() → FacilityResponse[]\n(sourceType="TRACKASIA", verificationStatus="UNVERIFIED" mặc định)
else 6. TrackAsia lỗi/timeout/rỗng → fallback dữ liệu nội bộ
  Service -> FacilityRepo : 6a. findNearby(lat, lng, radiusMeters)
  activate FacilityRepo
  FacilityRepo -> DB : 6b. SELECT * FROM care_facilities\nWHERE lat/lng NOT NULL AND earth_distance(...) <= radiusMeters*1000
  activate DB
  DB --> FacilityRepo : 6c. rows[] (không lọc theo verification_status)
  deactivate DB
  FacilityRepo --> Service : 6d. facilities[]
  deactivate FacilityRepo
end
Service --> Controller : 9. NearbyResponse{facilities[], totalCount}
deactivate Service
Controller --> App : 10. HTTP 200 OK {facilities[]}
deactivate Controller
App --> M : 11. Hiển thị facility trên bản đồ
deactivate App

== UC-79 View Route, ETA and Quick Call or Navigate ==
M -> App : 12. Chọn 1 facility
activate App
App -> Controller : 13. POST /api/v1/map/route\n{fromLat, fromLng, toLat, toLng, transportMode}
activate Controller
Controller -> Service : 14. getRoute(request)
activate Service
Service -> TrackAsia : 15. route(fromLat, fromLng, toLat, toLng, transportMode)
activate TrackAsia
TrackAsia --> Service : 16. GeoJSON route{routes: [{distance, duration, steps[]}]}
deactivate TrackAsia
Service -> Service : 17. parse leg đầu tiên → distanceMeters, etaMinutes,\ndanh sách RoutePoint từ steps[].maneuver
Service --> Controller : 18. RouteResponse{distanceMeters, etaMinutes, points[]}
deactivate Service
Controller --> App : 19. HTTP 200 OK {route}
deactivate Controller
App --> M : 20. Hiển thị route + ETA
deactivate App

alt 21. Mother chọn Gọi nhanh
  M -> Device : 21. gọi facilityPhone (native dialer)
else 21. Mother chọn Chỉ đường
  M -> Device : 21a. mở app bản đồ native với toạ độ đích (maps intent)
end

@enduml
```

**Hình 2 — Sequence Diagram: Open Map → Find Nearby Facilities → View Route/ETA → Quick Call or Navigate (Main Flow)**

> **Ghi chú grounding (quan trọng — lệch với Business Rules mục 5):** Code thật của
> `CareFacilityServiceImpl.searchNearby(...)` gọi **TrackAsia (nguồn ngoài) trước tiên**, và
> kết quả TrackAsia được gắn cứng `verificationStatus="UNVERIFIED"` — nghĩa là trong đường
> đi chính (happy path), UC-78 **có thể trả về facility chưa được admin xác minh**. Chỉ khi
> TrackAsia lỗi/rỗng, hệ thống mới fallback sang `CareFacilityRepository.findNearby(...)`,
> và truy vấn native đó **không lọc theo `verification_status='VERIFIED'`** — trả về mọi
> facility có toạ độ trong bán kính bất kể trạng thái xác minh. Điều này khác với khẳng định
> ở mục 5 ("chỉ facility VERIFIED được trả về") và với note trong State Machine ở mục 4 —
> hành vi lọc theo VERIFIED hiện **không được enforce ở tầng service/repository** cho luồng
> tìm kiếm gần đây; nó chỉ đúng cho các endpoint khác dùng `findByVerificationStatus(...)`.
> Ngoài ra, `map/routeprovider/RouteProvider.java` tồn tại như một interface nhưng
> `CareFacilityServiceImpl` phụ thuộc thẳng vào `TrackAsiaClient` cụ thể, không qua
> interface này.

## 4. State Machine — `CareFacility.verificationStatus`

```plantuml
@startuml MF07_01_FacilityStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> UNVERIFIED : Facility được nạp từ nguồn dữ liệu\n(partner / hệ thống import)

UNVERIFIED --> PENDING : Đưa vào hàng chờ xác minh
PENDING --> VERIFIED : Admin xác minh thông tin đúng
PENDING --> REJECTED : Admin từ chối (thông tin sai/không hợp lệ)
VERIFIED --> UNVERIFIED : Thông tin cần xác minh lại theo chu kỳ

VERIFIED --> [*]
REJECTED --> [*]

note right of VERIFIED
  UC-78 (tìm cơ sở gần đây) CHỈ trả về facility ở
  verificationStatus = VERIFIED — facility PENDING/UNVERIFIED/
  REJECTED không hiển thị cho Mother.
end note

@enduml
```

**Hình 3 — State Machine: `CareFacility.verificationStatus` Lifecycle**

## 5. Business Rules Applied

- UC-77 — người dùng phải được thông báo rõ phạm vi "không dispatch cấp cứu" trước khi vào bản đồ (CC-01).
- UC-78 — chỉ facility `VERIFIED` được trả về; vị trí sử dụng là vị trí người dùng đã cấp quyền hoặc khu vực tự chọn.
- UC-79 — route/ETA là hỗ trợ tham khảo; gọi nhanh/điều hướng giao hoàn toàn cho năng lực thiết bị (dialer/maps app), CareBridge không tự thực hiện cuộc gọi hay điều hướng.
- Excluded (SRS 4.8) — không dispatch xe cấp cứu, không đảm bảo lịch trình cơ sở y tế.
