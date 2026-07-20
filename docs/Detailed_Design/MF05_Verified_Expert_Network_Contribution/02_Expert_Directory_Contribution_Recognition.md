# MF-05 / Spec 02 — Expert Directory, Contribution & Recognition

| Field | Value |
| --- | --- |
| Feature | MF-05 — Verified Expert Network & Contribution |
| Use Cases Covered | UC-64 Configure Expert Availability and Service Scope, UC-65 Browse Verified Expert Directory, UC-66 View Verified Expert Profile, UC-67 View Expert Question Queue, UC-68 Post Verified Expert Answer, UC-69 View Contribution Points and Badges |
| Primary Actor(s) | Verified Expert, User (directory consumer) |
| Platform | Expert Portal / Expert App, Mobile App |
| Main Flow Summary | A Verified Expert configures availability/service scope so they become visible in the public directory, a User browses/opens that directory, and the expert answers matched community questions with a verified badge — automatically earning contribution points that accumulate into their recognized reputation. |
| Grounding (source code) | `expertavailability/entity/ExpertAvailability.java`, `AvailabilityStatus.java`, `expert/entity/ExpertProfile.java` (`/api/v1/expert/directory`, `/api/v1/expert/verified`), `expert/entity/ContributionPoint.java`, `expert/controller/ContributionPointController.java` (`/api/v1/expert/contribution-points`), `expert/handler/ExpertEventHandlerImpl.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Luồng này tiêu dùng kết quả của MF-05/Spec-01 (`verificationStatus=APPROVED` +
`trustStatus=ACTIVE`) để tạo giá trị cho cộng đồng: Expert bật `ExpertAvailability`
(khung giờ + kênh hỗ trợ, UC-64) để đủ điều kiện xuất hiện trong
`GET /expert/directory` (UC-65) và `GET /expert/verified` (UC-66, chỉ trả expert đã xác
thực). Khi expert trả lời một câu hỏi cộng đồng phù hợp chuyên môn (UC-67/68, tái sử
dụng `CommunityAnswerController` của MF-04 với `expertLabeled=true`), sự kiện đăng câu
trả lời tự động phát sinh `ContributionPoint` (UC-69) — **không có entity "Badge" riêng
trong backend hiện tại**: "huy hiệu" được tính toán/hiển thị từ tổng điểm tích luỹ
(`GET /contribution-points/total`, `/breakdown`) ở tầng response, không phải bảng dữ
liệu độc lập. Tương tự, "hàng đợi câu hỏi chuyên môn" (UC-67) tái sử dụng feed cộng đồng
của MF-04 lọc theo `specialty` của expert, không phải một entity/API hàng đợi riêng.

## 2. Class Diagram

```plantuml
@startuml MF05_02_ExpertDirectory_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class ExpertProfile {
  + expertProfileId: UUID
  + specialty: String
  + consultationScope: String
  + verificationStatus: VerificationStatus
  + trustStatus: TrustStatus
  + ratingAvg: BigDecimal
}

class ExpertAvailability {
  + availabilityId: UUID
  + expertProfileId: UUID
  + startAt: Instant
  + endAt: Instant
  + channelType: String
  + status: AvailabilityStatus
}

enum AvailabilityStatus {
  AVAILABLE
  BUSY
  UNAVAILABLE
}

class CommunityAnswer <<from MF-04>> {
  + id: UUID
  + questionId: UUID
  + authorId: UUID
  + expertLabeled: boolean
}

class ContributionPoint {
  + pointRecordId: UUID
  + userId: UUID
  + points: Integer
  + reason: String
  + sourceId: UUID
  + sourceType: String
  + recordedAt: LocalDateTime
}

class ContributionSummaryResponse <<read-model, không phải bảng riêng>> {
  + userId: UUID
  + totalPoints: int
  + breakdownBySourceType: Map<String, Integer>
  + derivedBadgeLevel: String
}

class ExpertAvailabilityController {
  - expertAvailabilityService: ExpertAvailabilityService
  + setAvailability(request): ResponseEntity
  + myAvailability(): ResponseEntity
}

class ExpertProfileController {
  + directory(filter): ResponseEntity
  + verified(filter): ResponseEntity
  + profile(expertProfileId): ResponseEntity
}

class CommunityAnswerController <<from MF-04>> {
  + post(questionId, request): ResponseEntity
}

class ContributionPointController {
  - contributionPointService: IContributionPointService
  + total(): ResponseEntity
  + breakdown(): ResponseEntity
}

interface IContributionPointService <<interface>> {
  + awardPoints(userId: UUID, points: int, reason: String, sourceType: String, sourceId: UUID): void
  + summary(userId: UUID): ContributionSummaryResponse
}

class ContributionPointServiceImpl implements IContributionPointService {
  - contributionPointRepository: ContributionPointRepository
}

class ExpertEventHandlerImpl {
  + onCommunityAnswerPosted(event): void
}

ExpertProfile "1" *-- "0..*" ExpertAvailability : configures
ExpertAvailability --> AvailabilityStatus
ExpertProfile "1" -- "0..*" CommunityAnswer : posts (expertLabeled)
ExpertProfile "1" *-- "0..*" ContributionPoint : accumulates
ExpertAvailabilityController --> ExpertAvailabilityService : uses
ExpertProfileController ..> ExpertProfile : reads (directory/verified — embedded search/filter)
CommunityAnswerController --> ExpertEventHandlerImpl : triggers on expertLabeled answer
ExpertEventHandlerImpl --> IContributionPointService : awardPoints("EXPERT_ANSWER")
ContributionPointController --> IContributionPointService : uses
ContributionPointServiceImpl ..> ContributionSummaryResponse : builds

@enduml
```

**Hình 1 — Class Diagram: Expert Availability → Directory Visibility → Contribution Points**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_02_ExpertDirectory_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Verified Expert" as Exp
participant "ExpertAvailabilityController" as AvailController
participant "ExpertAvailabilityServiceImpl" as AvailService
participant "ExpertAvailabilityRepository" as AvailRepo
actor "User" as U
participant "ExpertProfileController" as ProfileController
participant "ExpertProfileServiceImpl" as ProfileService
participant "ExpertProfileRepository" as ProfileRepo
participant "CommunityAnswerController" as AnswerController
participant "CommunityAnswerServiceImpl" as AnswerService
participant "CommunityAnswerRepository" as AnswerRepo
participant "ContributionPointController" as PointController
participant "ContributionPointServiceImpl" as PointService
participant "ContributionPointRepository" as PointRepo
database "PostgreSQL" as DB

== UC-64 Configure Expert Availability and Service Scope ==
Exp -> AvailController : 1. POST /api/v1/expert/availability\n{startAt, endAt, channelType}
activate AvailController
AvailController -> AvailService : 2. setAvailability(expertProfileId, request)
activate AvailService
AvailService -> AvailRepo : 3. save(ExpertAvailability{status=AVAILABLE})
activate AvailRepo
AvailRepo -> DB : 4. INSERT INTO expert_availability ...
activate DB
DB --> AvailRepo : 5. saved
deactivate DB
AvailRepo --> AvailService : 6. ExpertAvailability
deactivate AvailRepo
AvailService --> AvailController : 7. ExpertAvailability
deactivate AvailService
AvailController --> Exp : 8. HTTP 201 Created
deactivate AvailController

== UC-65 Browse Verified Expert Directory / UC-66 View Profile ==
U -> ProfileController : 9. GET /api/v1/expert/directory?specialty=&availability=
activate ProfileController
ProfileController -> ProfileService : 10. directory(filter)
activate ProfileService
ProfileService -> ProfileRepo : 11. findByVerificationStatusAndTrustStatus(APPROVED, ACTIVE)
activate ProfileRepo
ProfileRepo -> DB : 12. SELECT * FROM expert_profiles\nWHERE verification_status='APPROVED' AND trust_status='ACTIVE'
activate DB
DB --> ProfileRepo : 13. experts[]
deactivate DB
ProfileRepo --> ProfileService : 14. experts[]
deactivate ProfileRepo
ProfileService --> ProfileController : 15. experts[]
deactivate ProfileService
ProfileController --> U : 16. HTTP 200 OK {experts[]}
deactivate ProfileController

U -> ProfileController : 17. GET /api/v1/expert/profiles/{expertProfileId}
activate ProfileController
ProfileController -> ProfileService : 18. profile(expertProfileId)
activate ProfileService
ProfileService -> ProfileRepo : 19. findById(expertProfileId)
activate ProfileRepo
ProfileRepo -> DB : 20. SELECT * FROM expert_profiles WHERE id=?
activate DB
DB --> ProfileRepo : 21. profile
deactivate DB
ProfileRepo --> ProfileService : 22. profile
deactivate ProfileRepo
ProfileService --> ProfileController : 23. profile
deactivate ProfileService
ProfileController --> U : 24. HTTP 200 OK {profile, ratingAvg, availability}
deactivate ProfileController

== UC-67 View Expert Question Queue (reuse feed MF-04, filter by specialty) ==
Exp -> ProfileController : 25. GET /api/v1/community/feed?topic=<expert specialty>
activate ProfileController
ProfileController --> Exp : 26. HTTP 200 OK {matchedQuestions[]}
deactivate ProfileController

== UC-68 Post Verified Expert Answer ==
Exp -> AnswerController : 27. POST /api/v1/community/questions/{questionId}/answers\n{body}
activate AnswerController
AnswerController -> AnswerService : 28. post(authorId, questionId, request)
activate AnswerService
AnswerService -> ProfileRepo : 29. findByUserId(authorId)
activate ProfileRepo
ProfileRepo -> DB : 30. SELECT * FROM expert_profiles WHERE user_id=?
activate DB
DB --> ProfileRepo : 31. expertProfile{trustStatus}
deactivate DB
ProfileRepo --> AnswerService : 32. expertProfile
deactivate ProfileRepo
AnswerService -> AnswerService : 33. expertLabeled = (verified && trustStatus=ACTIVE)
AnswerService -> AnswerRepo : 34. save(CommunityAnswer{status=PENDING, expertLabeled=true})
activate AnswerRepo
AnswerRepo -> DB : 35. INSERT INTO community_answers ...
activate DB
DB --> AnswerRepo : 36. saved
deactivate DB
AnswerRepo --> AnswerService : 37. CommunityAnswer
deactivate AnswerRepo
AnswerService --> AnswerController : 38. CommunityAnswer{status=PENDING}
deactivate AnswerService
AnswerController --> Exp : 39. HTTP 201 Created
deactivate AnswerController

== UC-69 View Contribution Points and Badges ==
Exp -> PointController : 40. GET /api/v1/expert/contribution-points/total
activate PointController
PointController -> PointService : 41. summary(userId)
activate PointService
PointService -> PointRepo : 42. sumPointsBySourceType(userId)
activate PointRepo
PointRepo -> DB : 43. SELECT SUM(points), sourceType FROM contribution_points\nWHERE user_id=? GROUP BY sourceType
activate DB
DB --> PointRepo : 44. rows[]
deactivate DB
PointRepo --> PointService : 45. rows[]
deactivate PointRepo
PointService -> PointService : 46. derive badge level from totalPoints\n(configured threshold, not a separate table)
PointService --> PointController : 47. ContributionSummaryResponse
deactivate PointService
PointController --> Exp : 48. HTTP 200 OK {totalPoints, breakdown, derivedBadgeLevel}
deactivate PointController

@enduml
```

> Ghi chú grounding: `IExpertEventHandler.onAnswerExpertPosted(...)` (award điểm khi đăng
> trả lời chuyên gia) **chưa được gọi ở bất kỳ đâu trong codebase** ngoài chính định nghĩa
> của nó — không có caller thật trong `CommunityAnswerServiceImpl` hay nơi khác. Vì vậy
> bước 27-39 (UC-68) ở trên **không** vẽ việc tự động cộng điểm; UC-69 (xem điểm) vẫn đọc
> đúng bảng `contribution_points` thật, nhưng liên kết "đăng trả lời chuyên gia → tự động
> cộng điểm" cần được xác nhận/nối lại ở tầng code trước khi coi UC-68→UC-69 là một luồng
> tự động hoàn chỉnh.

**Hình 2 — Sequence Diagram: Configure Availability → Directory Visibility → Answer → Earn Points (Main Flow)**

## 4. State Machine — `ExpertAvailability.status`

```plantuml
@startuml MF05_02_Availability_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> AVAILABLE : Expert tạo khung giờ sẵn sàng (UC-64)

AVAILABLE --> BUSY : Expert tự chuyển hoặc hệ thống đánh dấu\nđang xử lý yêu cầu
BUSY --> AVAILABLE : Expert hoàn tất, quay lại sẵn sàng
AVAILABLE --> UNAVAILABLE : Expert tắt/hết khung giờ (DELETE /availability/{id})
BUSY --> UNAVAILABLE : Hết hạn khung giờ trong lúc BUSY
UNAVAILABLE --> [*]

note right of AVAILABLE
  Đây là toggle trạng thái sẵn sàng theo khung giờ, không phải
  vòng đời nhiều bước — 3 trạng thái đúng như enum thật trong
  code (AvailabilityStatus), không mở rộng thêm.
end note

@enduml
```

**Hình 3 — State Machine: `ExpertAvailability.status` (toggle, 3 trạng thái)**

## 5. Business Rules Applied

- Directory visibility (UC-65/66) yêu cầu đồng thời `verificationStatus=APPROVED`, `trustStatus=ACTIVE` (spec 01) **và** có ít nhất một `ExpertAvailability` đang `AVAILABLE`.
- UC-68 — `expertLabeled=true` chỉ được gắn khi trust status hợp lệ tại thời điểm đăng bài, kiểm tra lại (không cache) mỗi lần post.
- UC-69 — điểm đóng góp thể hiện mức độ tham gia (participation), không phải năng lực chuyên môn lâm sàng (đúng theo mô tả MF-05 trong SRS).
- BR-RBAC — chỉ chính expert mới cấu hình được availability và xem contribution points của mình.
