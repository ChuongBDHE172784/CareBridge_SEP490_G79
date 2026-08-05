# MF-05 / Spec 02 — Expert Directory, Availability & Community Contribution

| Field | Value |
| --- | --- |
| Feature | MF-05 — Verified Expert Network & Contribution |
| Flows Covered | Manage availability and online visibility; browse verified expert directory; view public expert profile; answer a selected community question |
| Primary Actor(s) | Verified Expert, Mother, Family Member |
| Platform | Mobile App; Expert Web Portal; CareBridge API |
| Main Flow Summary | An approved expert manages availability and directory visibility. A Mother or Family Member browses the verified directory and opens a public professional profile. The expert contributes by answering a community question through the shared MF-04 answer flow. |
| Explicitly Excluded | Contribution badges, badge levels, leaderboard, competence score, automatic point-award flow, consultation fee/rating, consultation requests, direct chat, voice/video calls |
| Grounding (active UI/API) | Mobile `expert_calendar_screen.dart`, `expert_directory_screen.dart`, `expert_public_profile_screen.dart`, `question_detail_screen.dart`, `post_answer_screen.dart`; Web `ExpertQuestionQueuePage.tsx`; API `ExpertAvailabilityController`, `ExpertProfileController`, `CommunityAnswerController` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Spec này chỉ mô tả phần MF-05 đang có luồng sử dụng thực tế và còn nằm trong SRS mới.
Expert phải hoàn tất Spec 01 và đang có trạng thái xác minh/tin cậy hợp lệ trước khi bật
hiển thị trong directory. Expert có thể quản lý các khoảng thời gian sẵn sàng và trạng
thái online. Mother hoặc Family Member tìm kiếm directory theo thông tin chuyên môn,
sau đó xem public profile ở mức dữ liệu nghề nghiệp được phép công khai.

Đóng góp của expert dùng chính luồng câu trả lời của MF-04. Câu trả lời có thể hiển thị
nhãn xác minh nghề nghiệp theo SRS; đây không phải huy hiệu thành tích. Không có màn hình
điểm, huy hiệu, cấp bậc hay leaderboard đang được route trên Web/Mobile, vì vậy các API
điểm còn sót lại không được đưa vào hợp đồng này. Các nút chat, yêu cầu tư vấn, phí và
rating còn xuất hiện trong code Mobile/Web là drift của MF-12 deferred và bị loại.

## 2. Class Diagram

```plantuml
@startuml MF05_02_ExpertDirectoryContribution_ClassDiagram
skinparam classAttributeIconSize 0

class ExpertProfile {
  + id: UUID
  + userId: UUID
  + professionalTitle: String
  + specialty: String
  + serviceScope: String
  + verificationStatus: VerificationStatus
  + trustStatus: TrustStatus
  + directoryVisible: Boolean
}

class ExpertAvailability {
  + id: UUID
  + expertProfileId: UUID
  + startAt: Instant
  + endAt: Instant
  + status: AvailabilityStatus
}

class CommunityQuestion {
  + id: UUID
  + topicId: UUID
  + title: String
  + status: QuestionStatus
}

class CommunityAnswer {
  + id: UUID
  + questionId: UUID
  + authorId: UUID
  + body: String
  + expertLabeled: Boolean
  + status: AnswerStatus
}

class ExpertAvailabilityController
class ExpertProfileController
class CommunityAnswerController
interface IExpertAvailabilityService
interface ExpertProfileService
interface CommunityAnswerService
interface ExpertAvailabilityRepository
interface ExpertProfileRepository
interface CommunityAnswerRepository

ExpertProfile "1" *-- "0..*" ExpertAvailability
ExpertProfile "1" --> "0..*" CommunityAnswer : contributes
CommunityQuestion "1" *-- "0..*" CommunityAnswer
ExpertAvailabilityController --> IExpertAvailabilityService
ExpertProfileController --> ExpertProfileService
CommunityAnswerController --> CommunityAnswerService
IExpertAvailabilityService --> ExpertAvailabilityRepository
ExpertProfileService --> ExpertProfileRepository
CommunityAnswerService --> CommunityAnswerRepository
CommunityAnswerService ..> ExpertProfileRepository : rechecks trust
@enduml
```

**Hình 1 — Class Diagram: Expert Directory, Availability & Community Contribution**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF05_02_ExpertDirectoryContribution_SequenceDiagram
actor "Verified Expert" as Expert
actor "Mother / Family Member" as User
participant "Mobile / Web UI" as UI
participant "ExpertAvailabilityController" as AvailabilityController
participant "ExpertProfileController" as ProfileController
participant "CommunityAnswerController" as AnswerController
participant "ExpertAvailabilityServiceImpl" as AvailabilityService
participant "ExpertProfileServiceImpl" as ProfileService
participant "CommunityAnswerServiceImpl" as AnswerService
participant "ExpertAvailabilityRepository" as AvailabilityRepo
participant "ExpertProfileRepository" as ProfileRepo
participant "CommunityAnswerRepository" as AnswerRepo
database "PostgreSQL" as DB

Expert -> UI : 1. Save an availability slot
activate UI
UI -> AvailabilityController : 1a. POST /api/v1/expert/availability
activate AvailabilityController
AvailabilityController -> AvailabilityService : 2. createAvailability(expertId, request)
activate AvailabilityService
AvailabilityService -> ProfileRepo : 3. findApprovedTrustedExpert(expertId)
activate ProfileRepo
ProfileRepo -> DB : 4. SELECT expert profile and trust status
activate DB
DB --> ProfileRepo : 5. expert profile / empty
deactivate DB
ProfileRepo --> AvailabilityService : 6. expert profile / empty
deactivate ProfileRepo
alt [expert is approved and trusted]
  AvailabilityService -> AvailabilityRepo : 7a. save(availability)
  activate AvailabilityRepo
  AvailabilityRepo -> DB : 7a-1. INSERT expert_availability
  activate DB
  DB --> AvailabilityRepo : 7a-2. saved row
  deactivate DB
  AvailabilityRepo --> AvailabilityService : 7a-3. availability
  deactivate AvailabilityRepo
  AvailabilityService --> AvailabilityController : 7a-4. availability
  deactivate AvailabilityService
  AvailabilityController --> UI : 7a-5. 201 Created
  deactivate AvailabilityController
  UI --> Expert : 7a-6. Display saved slot
  deactivate UI
else [verification or trust is invalid]
  AvailabilityService --> AvailabilityController : 7b. access rejected
  deactivate AvailabilityService
  AvailabilityController --> UI : 7b-1. 403 Forbidden
  deactivate AvailabilityController
  UI --> Expert : 7b-2. Display verification requirement
  deactivate UI
end

User -> UI : 8. Browse and open a verified expert
activate UI
UI -> ProfileController : 8a. GET /api/v1/expert/directory?specialty=&page=
activate ProfileController
ProfileController -> ProfileService : 9. listDirectory(filter)
activate ProfileService
ProfileService -> ProfileRepo : 10. findDirectoryVisibleApprovedExperts(filter)
activate ProfileRepo
ProfileRepo -> DB : 11. SELECT public expert fields
activate DB
DB --> ProfileRepo : 12. experts[]
deactivate DB
ProfileRepo --> ProfileService : 13. experts[]
deactivate ProfileRepo
ProfileService --> ProfileController : 14. public profiles[]
deactivate ProfileService
ProfileController --> UI : 15. 200 OK
deactivate ProfileController
UI --> User : 16. Display directory and public profile
deactivate UI

Expert -> UI : 17. Submit answer to selected question
activate UI
UI -> AnswerController : 17a. POST /api/v1/community/questions/{id}/answers
activate AnswerController
AnswerController -> AnswerService : 18. createAnswer(actorId, questionId, body)
activate AnswerService
AnswerService -> ProfileRepo : 19. findByUserId(actorId)
activate ProfileRepo
ProfileRepo -> DB : 20. SELECT verification and trust status
activate DB
DB --> ProfileRepo : 21. expert profile
deactivate DB
ProfileRepo --> AnswerService : 22. expert profile
deactivate ProfileRepo
AnswerService -> AnswerRepo : 23. save(answer with verification label metadata)
activate AnswerRepo
AnswerRepo -> DB : 24. INSERT community_answer
activate DB
DB --> AnswerRepo : 25. saved answer
deactivate DB
AnswerRepo --> AnswerService : 26. answer
deactivate AnswerRepo
AnswerService --> AnswerController : 27. answer
deactivate AnswerService
AnswerController --> UI : 28. 201 Created
deactivate AnswerController
UI --> Expert : 29. Display submitted/pending answer
deactivate UI
@enduml
```

**Hình 2 — Sequence Diagram: Configure Availability, Browse Directory & Submit Expert Answer**

## 4. Business Rules Applied

- Directory chỉ công khai các trường nghề nghiệp được phép và chỉ với expert đã được xác minh, còn trust hợp lệ và bật visibility.
- Availability/online status không cam kết expert sẽ phản hồi hoặc cung cấp dịch vụ tại một thời điểm cụ thể.
- Nhãn verified expert trên answer chỉ phản ánh trạng thái xác minh nền tảng; không phải huy hiệu, xếp hạng hay bảo đảm nội dung đúng.
- Không tự động cộng điểm từ sequence đăng answer vì không có caller và UI end-to-end đang hoạt động cho luồng đó.
- Phí, rating, consultation request, direct chat và voice/video call thuộc MF-12 deferred, không thuộc Spec này.
