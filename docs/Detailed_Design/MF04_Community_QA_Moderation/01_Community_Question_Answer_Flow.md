# MF-04 / Spec 01 — Community Question & Answer Flow

| Field | Value |
| --- | --- |
| Feature | MF-04 — Community Q&A & Moderation |
| Use Cases Covered | UC-46 Browse Community Feed and Topics, UC-47 View Community Question Detail, UC-48 Create Community Question, UC-50 Post Community Answer |
| Primary Actor(s) | User (Mother / Family / Expert) |
| Platform | Mobile App / Expert Portal |
| Main Flow Summary | A User browses the moderated feed, opens a question's detail, posts a new topic-based question (optionally anonymous), and answers an approved question. New or edited content starts at `AI_PENDING`; the AI scan may approve it automatically or move it to `PENDING` for human review. |
| Grounding (source code) | `community/entity/CommunityQuestion.java`, `QuestionStatus.java`, `community/entity/CommunityAnswer.java`, `AnswerStatus.java`, `community/controller/CommunityFeedController.java`, `CommunityQuestionController.java` (`/api/v1/community/questions`), `CommunityAnswerController.java` (`/api/v1/community/questions/{questionId}/answers`) |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng tạo nội dung chính của cộng đồng. Một `CommunityQuestion` được tạo với
`topicId`, ngữ cảnh giai đoạn (`stage`, `pregnancyWeek`/`babyAgeMonths`), mức độ khẩn
(`urgency`) và tuỳ chọn hiển thị ẩn danh công khai (`anonymous=true`) trong khi vẫn giữ
`authorId` nội bộ để truy vết trách nhiệm (BR-PRIVACY, UC-48). Người dùng khác trả lời
bằng `CommunityAnswer` — nếu là chuyên gia đã xác thực thì `expertLabeled=true` (liên kết
MF-05); nếu là chia sẻ kinh nghiệm cá nhân thì `personalExperience=true`, không được gắn
nhãn chuyên gia (UC-50). Cả hai entity khởi tạo ở `PENDING` và cần đi qua kiểm duyệt
(spec 02) trước khi cộng đồng nhìn thấy. Edit/delete nội dung của chính mình là nhánh
quản lý trong cùng flow. Like/reaction, bookmark và follow còn xuất hiện ở một số màn
hình/API cũ nhưng không nằm trong mô tả MF-04 đã rút gọn, nên không thuộc Spec này.

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
  - communitySafetyPolicy: CommunitySafetyPolicy
  - aiScanEnqueueService: AiScanEnqueueService
  - auditService: AuditService
}

interface CommunityAnswerService <<interface>> {
  + post(authorId: UUID, questionId: UUID, request): CommunityAnswer
}

class CommunityAnswerServiceImpl implements CommunityAnswerService {
  - communityAnswerRepository: CommunityAnswerRepository
  - communitySafetyPolicy: CommunitySafetyPolicy
  - aiScanEnqueueService: AiScanEnqueueService
}

CommunityTopic "1" *-- "0..*" CommunityQuestion : categorizes
CommunityQuestion "1" *-- "0..*" CommunityAnswer : has
CommunityQuestion --> QuestionStatus
CommunityAnswer --> AnswerStatus
CommunityFeedController ..> CommunityQuestion : reads (embedded search/filter)
CommunityQuestionController --> CommunityQuestionService : uses
CommunityAnswerController --> CommunityAnswerService : uses
CommunityQuestionServiceImpl --> CommunitySafetyPolicy : posting guard
CommunityQuestionServiceImpl --> AiScanEnqueueService : enqueue question scan
CommunityAnswerServiceImpl --> CommunitySafetyPolicy : posting guard + expert label
CommunityAnswerServiceImpl --> AiScanEnqueueService : enqueue answer scan

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
participant "CareBridge Community UI" as UI
participant "CommunityFeedController" as FeedController
participant "CommunityQuestionController" as QController
participant "CommunityAnswerController" as AController
participant "CommunityFeedServiceImpl" as FeedService
participant "CommunityQuestionServiceImpl" as QService
participant "CommunityAnswerServiceImpl" as AService
participant "AuditService" as Audit
participant "CommunityQuestionRepository" as QRepo
participant "CommunityAnswerRepository" as ARepo
database "PostgreSQL" as DB

== UC-46 Browse Community Feed and Topics ==
U -> UI : 1. Browse community feed
activate UI
UI -> FeedController : 1a. GET /api/v1/community/feed?topic=&keyword=&stage=
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
FeedController --> UI : 8. HTTP 200 OK {questions[]}
deactivate FeedController
UI --> U : 8a. Display approved questions
deactivate UI

== UC-48 Create Community Question ==
U -> UI : 9. Submit community question
activate UI
UI -> QController : 9a. POST /api/v1/community/questions\n{topicId, title, body, anonymous}
activate QController
QController -> QService : 10. create(authorId, request)
activate QService
QService -> QRepo : 11. save(CommunityQuestion{status=AI_PENDING})
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
QService --> QController : 17. CommunityQuestion{status=AI_PENDING|PENDING|APPROVED}
deactivate QService
QController --> UI : 18. HTTP 201 Created
deactivate QController
UI --> U : 18a. Display submitted question status
deactivate UI

note over QService
  status=PENDING → wait for NS-04 safety pipeline (spec 02)
  before displaying on public feed.
end note

== UC-47 View Community Question Detail ==
U -> UI : 19. Open community question detail
activate UI
UI -> QController : 19a. GET /api/v1/community/questions/{id}
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
QController --> UI : 26. HTTP 200 OK {question, answers[]}
deactivate QController
UI --> U : 26a. Display question and answers
deactivate UI

== UC-50 Post Community Answer ==
U -> UI : 27. Submit answer
activate UI
UI -> AController : 27a. POST /api/v1/community/questions/{questionId}/answers\n{body, personalExperience=true}
activate AController
AController -> AService : 28. post(authorId, questionId, request)
activate AService
AService -> AService : 28a. requirePostingAllowed(authorId) +\nisVerifiedActiveExpert(author)
activate AService
deactivate AService
AService -> ARepo : 29. save(CommunityAnswer{status=AI_PENDING, expertLabeled})
activate ARepo
ARepo -> DB : 30. INSERT INTO community_answers ...
activate DB
DB --> ARepo : 31. saved
deactivate DB
ARepo --> AService : 32. CommunityAnswer
deactivate ARepo
AService -> Audit : 33. log(COMMUNITY_ANSWER_POSTED)
activate Audit
Audit --> AService : 34. void
deactivate Audit
AService --> AController : 35. CommunityAnswer{status=AI_PENDING|PENDING|APPROVED}
deactivate AService
AController --> UI : 36. HTTP 201 Created
deactivate AController
UI --> U : 36a. Display submitted answer status
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Browse Feed → View Detail → Create Question → Post Answer (Main Flow)**


## 4. Business Rules Applied

- BR-PRIVACY — `authorId` luôn được lưu nội bộ để truy vết trách nhiệm ngay cả khi `anonymous=true` hiển thị công khai.
- BR-COMMUNITY-01 — nội dung công khai (câu hỏi/câu trả lời) chịu kiểm duyệt trước và sau khi xuất bản.
- UC-50 — chỉ chuyên gia có `TrustStatus=ACTIVE` và `VerificationStatus=APPROVED` (MF-05) mới được gắn `expertLabeled=true`.
- UC-49/51 — chỉ chủ bài viết mới sửa/xoá được, và không được phép khi bài đang `LOCKED` do kiểm duyệt/điều tra.
