# MF-04 / Spec 01 — Community Question and Answer Flow

| Field | Value |
| --- | --- |
| Status | Draft |
| Use Cases Covered | UC-11 Browse Community Q&A; UC-12 Answer Community Questions as Verified Expert; UC-40 Manage Community Questions; UC-41 Manage Community Answers |
| Use Case Group | Shared / Common and Mobile App |
| Platform | Mobile App; Expert Web; Backend |
| Primary Actors | Mother / Family / Verified Expert |
| In Scope | Mother and Family may ask; only approved active experts receive the expert label |
| Explicitly Excluded | Bookmarks, likes, follows and community identity |
| Implementation Trace | UI: CommunityFeedScreen, QuestionDetailScreen, CreateQuestionScreen, ExpertQuestionQueuePage; Controller: CommunityFeedController, CommunityQuestionController, CommunityAnswerController; Service: CommunityQuestionServiceImpl, CommunityAnswerServiceImpl; Repository: CommunityQuestionRepository, CommunityAnswerRepository; Entity: CommunityQuestion, CommunityAnswer |

## 1. Tổng quan luồng chính (Main Flow Overview)

Mother and Family may ask; only approved active experts receive the expert label. The flow starts only from the reachable UI or system trigger named in the metadata. The Backend rechecks authentication, role, ownership, membership, consent and current state as applicable; client-side visibility alone never grants access. A confirmed mutation is persisted before the UI displays success. External-service failure returns a safe retry state and must not fabricate completion.

## 2. Class Diagram

```plantuml
@startuml MF04_01_CommunityQuestionandAnswerFlow_ClassDiagram
skinparam classAttributeIconSize 0
hide empty members

class "CommunityFeedScreen" as UI1 <<UI>>
class "QuestionDetailScreen" as UI2 <<UI>>
class "CreateQuestionScreen" as UI3 <<UI>>
class "ExpertQuestionQueuePage" as UI4 <<UI>>
class "CommunityFeedController" as Controller1 <<Controller>> {
  - feedService: CommunityFeedService
}
class "CommunityQuestionController" as Controller2 <<Controller>> {
  - questionService: CommunityQuestionService
  - searchService: CommunityQuestionSearchService
  + createQuestion(request: CreateCommunityQuestionRequest, principal: Principal): ResponseEntity<ApiResponse<CommunityQuestionResponse>>
  + deleteQuestion(id: UUID, principal: Principal): ResponseEntity<Void>
  + editQuestion(id: UUID, request: UpdateCommunityQuestionRequest, principal: Principal): ResponseEntity<ApiResponse<CommunityQuestionResponse>>
  + getQuestionDetail(id: UUID, principal: Principal): ResponseEntity<ApiResponse<CommunityQuestionDetailResponse>>
}
class "CommunityAnswerController" as Controller3 <<Controller>> {
  - answerService: CommunityAnswerService
  - expertProfileRepository: ExpertProfileRepository
  + deleteAnswer(questionId: UUID, id: UUID, principal: Principal): ResponseEntity<Void>
  + editAnswer(questionId: UUID, id: UUID, request: EditAnswerRequest, ...): ResponseEntity<ApiResponse<CommunityAnswerResponse>>
  + postAnswer(questionId: UUID, request: PostCommunityAnswerRequest, principal: Principal): ResponseEntity<ApiResponse<CommunityAnswerResponse>>
}
class "CommunityQuestionServiceImpl" as Service1 <<Service>> {
  - questionRepository: CommunityQuestionRepository
  - topicRepository: CommunityTopicRepository
  - answerRepository: CommunityAnswerRepository
  + createQuestion(authorId: UUID, request: CreateCommunityQuestionRequest): CommunityQuestionResponse
  + deleteQuestion(questionId: UUID, callerId: UUID, isModeratorCaller: boolean): void
  + editQuestion(authorId: UUID, questionId: UUID, request: UpdateCommunityQuestionRequest): CommunityQuestionResponse
  + getMyQuestions(authorId: UUID, page: int, size: int): PaginatedResponse<CommunityQuestionResponse>
  + getQuestionDetail(questionId: UUID, currentUserId: UUID): CommunityQuestionDetailResponse
}
class "CommunityAnswerServiceImpl" as Service2 <<Service>> {
  - answerRepository: CommunityAnswerRepository
  - questionRepository: CommunityQuestionRepository
  - answerMapper: CommunityAnswerMapper
  - auditService: AuditService
  + deleteAnswer(answerId: UUID, callerId: UUID, isModeratorCaller: boolean): void
  + editAnswer(answerId: UUID, callerId: UUID, request: EditAnswerRequest): CommunityAnswerResponse
  + postAnswer(authorId: UUID, questionId: UUID, request: PostCommunityAnswerRequest): CommunityAnswerResponse
  - resolveExpertProfileId(userId: UUID): UUID
}
interface "CommunityQuestionService" as Service1Contract <<Service>>
interface "CommunityAnswerService" as Service2Contract <<Service>>
interface "CommunityQuestionRepository" as Repository1 {
  + existsByTopicId(topicId: UUID): boolean
  + findByIdAndStatus(id: UUID, status: QuestionStatus): Optional<CommunityQuestion>
  + findByAuthorIdAndTitle(authorId: UUID, title: String): Optional<CommunityQuestion>
  + findAllByAuthorIdOrderByCreatedAtDesc(authorId: UUID, pageable: Pageable): Page<CommunityQuestion>
  + findAllByAuthorIdAndStatusNotOrderByCreatedAtDesc(authorId: UUID, status: QuestionStatus, pageable: Pageable): Page<CommunityQuestion>
  + findByStatus(status: QuestionStatus, pageable: Pageable): Page<CommunityQuestion>
}
interface "CommunityAnswerRepository" as Repository2 {
  + findQuestionIdsWithExpertAnswer(questionIds: java.util.Collection<UUID>): Set<UUID>
  + findFirstByQuestionIdAndAuthorIdOrderByCreatedAtAsc(questionId: UUID, authorId: UUID): Optional<CommunityAnswer>
  + findByStatus(status: AnswerStatus, pageable: Pageable): Page<CommunityAnswer>
  + findVisibleToCommunity(pageable: Pageable): Page<CommunityAnswer>
  + findAllByQuestionIdAndStatusOrderByCreatedAtDesc(questionId: UUID, status: AnswerStatus): List<CommunityAnswer>
  + findAllByQuestionId(questionId: UUID): List<CommunityAnswer>
}
class "CommunityQuestion" as Entity1 <<Entity>> {
  - id: UUID
  - topicId: UUID
  - authorId: java.util.UUID
  - title: String
  - body: String
  - imageUrls: List<String>
  - stage: PregnancyStage
}
class "CommunityAnswer" as Entity2 <<Entity>> {
  - id: UUID
  - questionId: UUID
  - authorId: UUID
  - body: String
  - imageUrls: List<String>
  - expertLabeled: boolean
  - personalExperience: boolean
}
interface "JpaRepository<CommunityQuestion, UUID>" as Repository1Base <<Framework>>
interface "JpaRepository<CommunityAnswer, UUID>" as Repository2Base <<Framework>>
class "PostgreSQL" as DB <<Database>>

Service1Contract <|.. Service1 : implements
Service2Contract <|.. Service2 : implements
Repository1Base <|-- Repository1 : extends
Repository2Base <|-- Repository2 : extends
UI1 ..> Controller1 : invokes API
UI2 ..> Controller2 : invokes API
UI2 ..> Controller3 : invokes API
UI3 ..> Controller2 : invokes API
UI4 ..> Controller3 : invokes API
Controller1 --> Service1Contract : delegates
Controller2 --> Service1Contract : delegates
Controller3 --> Service2Contract : delegates
Service1 --> Repository1 : reads / writes
Service2 --> Repository2 : reads / writes
Repository1 ..> Entity1 : maps
Repository1 ..> DB : persists
Repository2 ..> Entity2 : maps
Repository2 ..> DB : persists
Entity1 "1" *-- "0..*" Entity2 : answers
@enduml
```

**Figure 1 — Class Diagram: Community Question and Answer Flow**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF04_01_CommunityQuestionandAnswerFlow_SequenceDiagram
skinparam shadowing false

actor "Mother / Family / Verified Expert" as Actor
boundary ":CommunityFeedScreen" as UI1
boundary ":ExpertQuestionQueuePage" as UI2
boundary ":CreateQuestionScreen" as UI3
boundary ":QuestionDetailScreen" as UI4
control ":CommunityFeedController" as Controller1
control ":CommunityAnswerController" as Controller2
control ":CommunityQuestionController" as Controller3
participant ":CommunityFeedServiceImpl" as Service1 <<service>>
participant ":CommunityAnswerServiceImpl" as Service2 <<service>>
participant ":CommunityQuestionServiceImpl" as Service3 <<service>>
participant ":CommunityQuestionRepository" as Repository1 <<repository>>
participant ":CommunityAnswerRepository" as Repository2 <<repository>>
database "PostgreSQL" as DB

group UC-11 Browse Community Q&A
  Actor -> UI1 : 1. startBrowseCommunityQA()
  activate UI1
  UI1 -> Controller1 : 2. getFeed(filters, pageable)
  activate Controller1
  Controller1 -> Service1 : 3. getFeed(filters, pageable)
  activate Service1
  alt [request is authorized and input is valid]
    Service1 -> Repository1 : 4a. findFeedVisible(pageable)
    activate Repository1
    Repository1 -> DB : 4a-1. SELECT
    activate DB
    DB --> Repository1 : 4a-2. queryResult
    deactivate DB
    Repository1 --> Service1 : 4a-3. domainRecords
    deactivate Repository1
    Service1 --> Controller1 : 4a-4. resultDTO
    deactivate Service1
    Controller1 --> UI1 : 4a-5. 200 OK
    deactivate Controller1
    UI1 --> Actor : 4a-6. displayBrowseCommunityQAResult()
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

group UC-12 Answer Community Questions as Verified Expert
  Actor -> UI2 : 5. startAnswerCommunityQuestionsAsVerifiedExpert()
  activate UI2
  UI2 -> Controller2 : 6. postAnswer(questionId, request)
  activate Controller2
  Controller2 -> Service2 : 7. postAnswer(questionId, request)
  activate Service2
  alt [command is valid and actor is authorized]
    Service2 -> Repository2 : 8a. save(expertLabeledAnswer)
    activate Repository2
    Repository2 -> DB : 8a-1. INSERT / UPDATE
    activate DB
    DB --> Repository2 : 8a-2. persistedState
    deactivate DB
    Repository2 --> Service2 : 8a-3. savedEntity
    deactivate Repository2
    Service2 --> Controller2 : 8a-4. resultDTO
    deactivate Service2
    Controller2 --> UI2 : 8a-5. 200 OK / 201 Created
    deactivate Controller2
    UI2 --> Actor : 8a-6. displayConfirmedState()
    deactivate UI2
  else [validation, authorization or state check fails]
    Service2 --> Controller2 : 8b. domainError
    deactivate Service2
    Controller2 --> UI2 : 8b-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI2 --> Actor : 8b-2. displayActionableError()
    deactivate UI2
  end
end

group UC-40 Manage Community Questions
  Actor -> UI3 : 9. startManageCommunityQuestions()
  activate UI3
  UI3 -> Controller3 : 10. getMyQuestions() / createQuestion() / editQuestion() / deleteQuestion()
  activate Controller3
  Controller3 -> Service3 : 11. getMyQuestions() / createQuestion() / editQuestion() / deleteQuestion()
  activate Service3
  alt [selected action is view or list]
    Service3 -> Repository1 : 12a. findAllByAuthorIdOrderByCreatedAtDesc()
    activate Repository1
    Repository1 -> DB : 12a-1. SELECT
    activate DB
    DB --> Repository1 : 12a-2. queryResult
    deactivate DB
    Repository1 --> Service3 : 12a-3. domainRecords
    deactivate Repository1
    Service3 --> Controller3 : 12a-4. resultDTO
    deactivate Service3
    Controller3 --> UI3 : 12a-5. 200 OK
    deactivate Controller3
    UI3 --> Actor : 12a-6. displayCurrentState()
    deactivate UI3
  else [selected action creates, updates, archives or deletes]
    Service3 -> Repository1 : 12b. findAllByAuthorIdOrderByCreatedAtDesc()
    activate Repository1
    Repository1 -> DB : 12b-1. SELECT
    activate DB
    DB --> Repository1 : 12b-2. currentState
    deactivate DB
    Repository1 --> Service3 : 12b-3. scopedEntity
    deactivate Repository1
    Service3 -> Repository1 : 12b-4. save() / delete()
    activate Repository1
    Repository1 -> DB : 12b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository1 : 12b-6. persistedState
    deactivate DB
    Repository1 --> Service3 : 12b-7. persistedEntity
    deactivate Repository1
    Service3 --> Controller3 : 12b-8. resultDTO
    deactivate Service3
    Controller3 --> UI3 : 12b-9. 200 OK / 201 Created
    deactivate Controller3
    UI3 --> Actor : 12b-10. displayConfirmedState()
    deactivate UI3
  else [request is invalid, forbidden, not found or conflicting]
    Service3 --> Controller3 : 12c. domainError
    deactivate Service3
    Controller3 --> UI3 : 12c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller3
    UI3 --> Actor : 12c-2. displayActionableError()
    deactivate UI3
  end
end

group UC-41 Manage Community Answers
  Actor -> UI4 : 13. startManageCommunityAnswers()
  activate UI4
  UI4 -> Controller2 : 14. postAnswer() / editAnswer() / deleteAnswer()
  activate Controller2
  Controller2 -> Service2 : 15. postAnswer() / editAnswer() / deleteAnswer()
  activate Service2
  alt [selected action is view or list]
    Service2 -> Repository2 : 16a. findAllByQuestionIdAndStatusOrderByCreatedAtDesc()
    activate Repository2
    Repository2 -> DB : 16a-1. SELECT
    activate DB
    DB --> Repository2 : 16a-2. queryResult
    deactivate DB
    Repository2 --> Service2 : 16a-3. domainRecords
    deactivate Repository2
    Service2 --> Controller2 : 16a-4. resultDTO
    deactivate Service2
    Controller2 --> UI4 : 16a-5. 200 OK
    deactivate Controller2
    UI4 --> Actor : 16a-6. displayCurrentState()
    deactivate UI4
  else [selected action creates, updates, archives or deletes]
    Service2 -> Repository2 : 16b. findAllByQuestionIdAndStatusOrderByCreatedAtDesc()
    activate Repository2
    Repository2 -> DB : 16b-1. SELECT
    activate DB
    DB --> Repository2 : 16b-2. currentState
    deactivate DB
    Repository2 --> Service2 : 16b-3. scopedEntity
    deactivate Repository2
    Service2 -> Repository2 : 16b-4. save() / delete()
    activate Repository2
    Repository2 -> DB : 16b-5. INSERT / UPDATE / DELETE
    activate DB
    DB --> Repository2 : 16b-6. persistedState
    deactivate DB
    Repository2 --> Service2 : 16b-7. persistedEntity
    deactivate Repository2
    Service2 --> Controller2 : 16b-8. resultDTO
    deactivate Service2
    Controller2 --> UI4 : 16b-9. 200 OK / 201 Created
    deactivate Controller2
    UI4 --> Actor : 16b-10. displayConfirmedState()
    deactivate UI4
  else [request is invalid, forbidden, not found or conflicting]
    Service2 --> Controller2 : 16c. domainError
    deactivate Service2
    Controller2 --> UI4 : 16c-1. 400 / 401 / 403 / 404 / 409
    deactivate Controller2
    UI4 --> Actor : 16c-2. displayActionableError()
    deactivate UI4
  end
end

@enduml
```

**Figure 2 — Sequence Diagram: Community Question and Answer Flow Main Flow**

**Brief Explanation:**

1. The diagram separates the implemented goals covered by this specification: UC-11 Browse Community Q&A; UC-12 Answer Community Questions as Verified Expert; UC-40 Manage Community Questions; UC-41 Manage Community Answers.
2. Each actor action enters through the reachable mobile or web boundary and invokes the exact controller operation exposed by the current Backend.
3. The controller delegates to the mapped service operation; read branches query scoped records, while mutation branches load the current state before persisting the requested transition.
4. Repository calls and PostgreSQL responses are shown explicitly, including call-stack activation and dashed return messages.
5. Invalid, unauthorized, missing or conflicting requests return an actionable error without displaying a false success state.
6. External systems are invoked only for the mapped use cases; notification dispatches are asynchronous, while integrations that return data remain synchronous.

## 4. Business Rules Applied

- Access is enforced server-side using the current actor, role, ownership, membership and consent scope.
- Mother and Family may ask; only approved active experts receive the expert label.
- The following remains outside this contract: Bookmarks, likes, follows and community identity.
- Retries must not duplicate confirmed records, transitions, notifications or external side effects.
- Sensitive health, identity, moderation, location and safety operations retain the minimum required audit evidence.
