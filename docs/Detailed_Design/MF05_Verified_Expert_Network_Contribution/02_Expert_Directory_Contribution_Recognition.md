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
actor "User" as U
participant "ExpertProfileController" as ProfileController
participant "CommunityAnswerController" as AnswerController
participant "ExpertEventHandlerImpl" as Handler
participant "ContributionPointServiceImpl" as PointService
participant "ContributionPointController" as PointController
database "PostgreSQL" as DB

== UC-64 Configure Expert Availability and Service Scope ==
Exp -> AvailController : POST /api/v1/expert/availability\n{startAt, endAt, channelType}
AvailController -> DB : INSERT INTO expert_availability (status=AVAILABLE)
AvailController --> Exp : HTTP 201 Created

== UC-65 Browse Verified Expert Directory / UC-66 View Profile ==
U -> ProfileController : GET /api/v1/expert/directory?specialty=&availability=
ProfileController -> DB : SELECT * FROM expert_profiles\nWHERE verification_status='APPROVED' AND trust_status='ACTIVE'
DB --> ProfileController : experts[]
ProfileController --> U : HTTP 200 OK {experts[]}
U -> ProfileController : GET /api/v1/expert/profiles/{expertProfileId}
ProfileController --> U : HTTP 200 OK {profile, ratingAvg, availability}

== UC-67 View Expert Question Queue (tái sử dụng feed MF-04, lọc theo specialty) ==
Exp -> ProfileController : GET /api/v1/community/feed?topic=<specialty của expert>
ProfileController --> Exp : HTTP 200 OK {matchedQuestions[]}

== UC-68 Post Verified Expert Answer ==
Exp -> AnswerController : POST /api/v1/community/questions/{questionId}/answers\n{body}
AnswerController -> AnswerController : expertLabeled = true\n(TrustStatus=ACTIVE)
AnswerController -> DB : INSERT INTO community_answers (status=PENDING, expertLabeled=true)
AnswerController -> Handler : onCommunityAnswerPosted(event)
Handler -> PointService : awardPoints(expertUserId, 5,\n"Posted expert answer", "EXPERT_ANSWER", questionId)
PointService -> DB : INSERT INTO contribution_points ...
AnswerController --> Exp : HTTP 201 Created

== UC-69 View Contribution Points and Badges ==
Exp -> PointController : GET /api/v1/expert/contribution-points/total
PointController -> PointService : summary(userId)
PointService -> DB : SELECT SUM(points), sourceType FROM contribution_points\nWHERE user_id=? GROUP BY sourceType
DB --> PointService : rows[]
PointService -> PointService : derive badge level từ totalPoints\n(ngưỡng cấu hình, không phải bảng riêng)
PointService --> PointController : ContributionSummaryResponse
PointController --> Exp : HTTP 200 OK {totalPoints, breakdown, derivedBadgeLevel}

@enduml
```

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
