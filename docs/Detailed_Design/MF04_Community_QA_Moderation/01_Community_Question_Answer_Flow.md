# MF-04 / Spec 01 — Community Question & Answer Flow

| Field | Value |
| --- | --- |
| Feature | MF-04 — Community Q&A & Moderation |
| Use Cases Covered | UC-46 Browse Community Feed and Topics, UC-47 View Community Question Detail, UC-48 Create Community Question, UC-50 Post Community Answer |
| Primary Actor(s) | User (Mother / Family / Expert) |
| Platform | Mobile App / Expert Portal |
| Main Flow Summary | A User browses the moderated feed, opens a question's detail, posts a new topic-based question (optionally anonymous), and answers an existing question — either as a personal-experience answer or, if verified, a badge-labeled expert answer. Everything a user posts enters `PENDING` and only becomes visible after passing the community safety pipeline (spec 02). |
| Grounding (source code) | `community/entity/CommunityQuestion.java`, `QuestionStatus.java`, `community/entity/CommunityAnswer.java`, `AnswerStatus.java`, `community/controller/CommunityFeedController.java`, `CommunityQuestionController.java` (`/api/v1/community/questions`), `CommunityAnswerController.java` (`/api/v1/community/questions/{questionId}/answers`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng tạo nội dung chính của cộng đồng. Một `CommunityQuestion` được tạo với
`topicId`, ngữ cảnh giai đoạn (`stage`, `pregnancyWeek`/`babyAgeMonths`), mức độ khẩn
(`urgency`) và tuỳ chọn hiển thị ẩn danh công khai (`anonymous=true`) trong khi vẫn giữ
`authorId` nội bộ để truy vết trách nhiệm (BR-PRIVACY, UC-48). Người dùng khác trả lời
bằng `CommunityAnswer` — nếu là chuyên gia đã xác thực thì `expertLabeled=true` (liên kết
MF-05); nếu là chia sẻ kinh nghiệm cá nhân thì `personalExperience=true`, không được gắn
nhãn chuyên gia (UC-50). Cả hai entity khởi tạo ở `PENDING` và cần đi qua kiểm duyệt
(spec 02) trước khi cộng đồng nhìn thấy — reaction/bookmark/follow (UC-52/53/54) và
edit/delete bài của chính mình (UC-49/51) là thao tác phụ, không lặp lại thành spec
riêng ở đây.

## 2. Class Diagram

```plantuml
@startuml MF04_01_QuestionAnswer_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

class CommunityTopic {
  + id: UUID
  + name: String
  + isHidden: boolean
  + sortOrder: int
}

class CommunityQuestion {
  + id: UUID
  + topicId: UUID
  + authorId: UUID
  + title: String
  + body: String
  + stage: PregnancyStage
  + pregnancyWeek: Short
  + urgency: UrgencyLevel
  + anonymous: boolean
  + status: QuestionStatus
  + likeCount: int
  + answerCount: int
}

enum QuestionStatus {
  PENDING
  APPROVED
  HIDDEN
  LOCKED
  DELETED
}

class CommunityAnswer {
  + id: UUID
  + questionId: UUID
  + authorId: UUID
  + body: String
  + expertLabeled: boolean
  + personalExperience: boolean
  + status: AnswerStatus
  + likeCount: int
}

enum AnswerStatus {
  PENDING
  APPROVED
  HIDDEN
  DELETED
}

class CreateQuestionRequest {
  + topicId: UUID
  + title: String
  + body: String
  + anonymous: boolean
}

class PostAnswerRequest {
  + body: String
  + personalExperience: boolean
}

class CommunityFeedController {
  + feed(filter, search, page): ResponseEntity
}

class CommunityQuestionController {
  - communityQuestionService: CommunityQuestionService
  + create(CreateQuestionRequest): ResponseEntity
  + detail(id): ResponseEntity
}

class CommunityAnswerController {
  - communityAnswerService: CommunityAnswerService
  + post(questionId, PostAnswerRequest): ResponseEntity
}

interface CommunityQuestionService <<interface>> {
  + create(authorId: UUID, request): CommunityQuestion
  + detail(id: UUID): CommunityQuestion
}

class CommunityQuestionServiceImpl implements CommunityQuestionService {
  - communityQuestionRepository: CommunityQuestionRepository
  - contentSafetyPipeline: ContentSafetyPipeline
  - auditService: AuditService
}

interface CommunityAnswerService <<interface>> {
  + post(authorId: UUID, questionId: UUID, request): CommunityAnswer
}

class CommunityAnswerServiceImpl implements CommunityAnswerService {
  - communityAnswerRepository: CommunityAnswerRepository
  - expertProfileRepository: ExpertProfileRepository
  - contentSafetyPipeline: ContentSafetyPipeline
}

CommunityTopic "1" *-- "0..*" CommunityQuestion : categorizes
CommunityQuestion "1" *-- "0..*" CommunityAnswer : has
CommunityQuestion --> QuestionStatus
CommunityAnswer --> AnswerStatus
CommunityFeedController ..> CommunityQuestion : reads (embedded search/filter)
CommunityQuestionController --> CommunityQuestionService : uses
CommunityAnswerController --> CommunityAnswerService : uses
CommunityQuestionServiceImpl --> ContentSafetyPipeline : NS-04 pre-check
CommunityAnswerServiceImpl --> ExpertProfileRepository : xác định expertLabeled

@enduml
```

**Hình 1 — Class Diagram: Community Question & Answer**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF04_01_QuestionAnswer_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "User" as U
participant "CommunityFeedController" as FeedController
participant "CommunityFeedServiceImpl" as FeedService
participant "CommunityQuestionController" as QController
participant "CommunityQuestionServiceImpl" as QService
participant "CommunityQuestionRepository" as QRepo
participant "CommunityAnswerController" as AController
participant "CommunityAnswerServiceImpl" as AService
participant "CommunityAnswerRepository" as ARepo
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-46 Browse Community Feed and Topics ==
U -> FeedController : 1. GET /api/v1/community/feed?topic=&keyword=&stage=
activate FeedController
FeedController -> FeedService : 2. feed(filter)
activate FeedService
FeedService -> QRepo : 3. search(filter, status=APPROVED)
activate QRepo
QRepo -> DB : 4. SELECT * FROM community_questions\nWHERE status='APPROVED' AND ...
activate DB
DB --> QRepo : 5. rows[]
deactivate DB
QRepo --> FeedService : 6. questions[]
deactivate QRepo
FeedService --> FeedController : 7. questions[]
deactivate FeedService
FeedController --> U : 8. HTTP 200 OK {questions[]}
deactivate FeedController

== UC-48 Create Community Question ==
U -> QController : 9. POST /api/v1/community/questions\n{topicId, title, body, anonymous}
activate QController
QController -> QService : 10. create(authorId, request)
activate QService
QService -> QRepo : 11. save(CommunityQuestion{status=PENDING})
activate QRepo
QRepo -> DB : 12. INSERT INTO community_questions ...
activate DB
DB --> QRepo : 13. saved
deactivate DB
QRepo --> QService : 14. CommunityQuestion
deactivate QRepo
QService -> Audit : 15. log(COMMUNITY_QUESTION_CREATED)
activate Audit
Audit --> QService : 16. void
deactivate Audit
QService --> QController : 17. CommunityQuestion{status=PENDING}
deactivate QService
QController --> U : 18. HTTP 201 Created
deactivate QController

note over QService
  status=PENDING → wait for NS-04 safety pipeline (spec 02)
  before displaying on public feed.
end note

== UC-47 View Community Question Detail ==
U -> QController : 19. GET /api/v1/community/questions/{id}
activate QController
QController -> QService : 20. detail(id)
activate QService
QService -> QRepo : 21. findByIdWithAnswers(id)
activate QRepo
QRepo -> DB : 22. SELECT * FROM community_questions\nJOIN community_answers WHERE ...
activate DB
DB --> QRepo : 23. question + answers[]
deactivate DB
QRepo --> QService : 24. question + answers[]
deactivate QRepo
QService --> QController : 25. question + answers[]
deactivate QService
QController --> U : 26. HTTP 200 OK {question, answers[]}
deactivate QController

== UC-50 Post Community Answer ==
U -> AController : 27. POST /api/v1/community/questions/{questionId}/answers\n{body, personalExperience=true}
activate AController
AController -> AService : 28. post(authorId, questionId, request)
activate AService
AService -> AService : 29. lookup ExpertProfile(authorId) →\nexpertLabeled = (verified && trustStatus=ACTIVE)
AService -> ARepo : 30. save(CommunityAnswer{status=PENDING, expertLabeled})
activate ARepo
ARepo -> DB : 31. INSERT INTO community_answers ...
activate DB
DB --> ARepo : 32. saved
deactivate DB
ARepo --> AService : 33. CommunityAnswer
deactivate ARepo
AService -> Audit : 34. log(COMMUNITY_ANSWER_POSTED)
activate Audit
Audit --> AService : 35. void
deactivate Audit
AService --> AController : 36. CommunityAnswer{status=PENDING}
deactivate AService
AController --> U : 37. HTTP 201 Created
deactivate AController

@enduml
```

**Hình 2 — Sequence Diagram: Browse Feed → View Detail → Create Question → Post Answer (Main Flow)**

## 4. State Machine — `CommunityQuestion.status` / `CommunityAnswer.status`

```plantuml
@startuml MF04_01_QuestionAnswerStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING : User đăng câu hỏi/câu trả lời (UC-48 / UC-50)

PENDING --> APPROVED : NS-04 safety pipeline / moderator duyệt (spec 02, UC-57)
PENDING --> HIDDEN : Moderator ẩn do vi phạm chính sách (spec 02, UC-57)

APPROVED --> HIDDEN : Bị báo cáo và moderator xử lý (UC-55 → UC-57)
APPROVED --> LOCKED : Câu hỏi bị khoá do đang điều tra\n[chỉ CommunityQuestion]
HIDDEN --> APPROVED : Moderator khôi phục sau rà soát

APPROVED --> DELETED : Chủ bài viết tự xoá (UC-49 / UC-51)\n[chỉ khi chưa bị khoá]
LOCKED --> DELETED : sau khi điều tra kết thúc, moderator xử lý (UC-58)

DELETED --> [*]

note right of LOCKED
  LOCKED chỉ áp dụng cho CommunityQuestion — CommunityAnswer
  chỉ có PENDING / APPROVED / HIDDEN / DELETED (không có LOCKED).
end note

@enduml
```

**Hình 3 — State Machine: `CommunityQuestion.status` (5 trạng thái) & `CommunityAnswer.status` (4 trạng thái)**

## 5. Business Rules Applied

- BR-PRIVACY — `authorId` luôn được lưu nội bộ để truy vết trách nhiệm ngay cả khi `anonymous=true` hiển thị công khai.
- BR-COMMUNITY-01 — nội dung công khai (câu hỏi/câu trả lời) chịu kiểm duyệt trước và sau khi xuất bản.
- UC-50 — chỉ chuyên gia có `TrustStatus=ACTIVE` và `VerificationStatus=APPROVED` (MF-05) mới được gắn `expertLabeled=true`.
- UC-49/51 — chỉ chủ bài viết mới sửa/xoá được, và không được phép khi bài đang `LOCKED` do kiểm duyệt/điều tra.
