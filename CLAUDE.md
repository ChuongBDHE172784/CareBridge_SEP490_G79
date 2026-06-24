
title: CareBridge Project Structure Design
project: CareBridge_SEP490_G79
source_documents:
  - 02_Requirements/SRS/Report1_Project Introduction.docx.md
  - 02_Requirements/SRS/Report2_Project Management Plan.docx.md
  - 02_Requirements/SRS/3_Functional_Specification.md
  - 02_Requirements/SRS/4_Functional_Requirements.md
  
created_by: bmad-create-architecture
date: 2026-06-17
status: draft
architecture_style: Layered Architecture using Controller-Service-Repository pattern
---

# CareBridge Project Structure Design

## 1. Reading Report

This design is derived from the supplied project documents:

- Report 1 - Project Introduction: CareBridge is a maternal and early childhood healthcare support platform for pre-pregnancy, pregnancy, postpartum, baby care, family coordination, expert guidance, moderated community, AI-assisted triage, emergency support, and legal-safe healthcare boundaries.
- Report 2 - Project Management Plan: the MVP is Hanoi-first, academic-scope, and uses Flutter, Java Spring Boot, PostgreSQL, MongoDB, Redis or lightweight caching, Firebase Cloud Messaging, TrackAsia, Firebase Storage, GitLab CI/CD, AWS, and Vercel.
- Functional Specification (`3_Functional_Specification.md`) & Functional Requirements (`4_Functional_Requirements.md`): the system includes mobile app, web portal, backend services, Gemini AI/RAG, ZegoCloud, Firebase chat/storage/notification, TrackAsia, smartwatch data, phone IMU, MediaPipe posture analysis, VNPay, consent, audit, and security event workflows.

## 2. Architecture Position

CareBridge should use **Layered Architecture using Controller-Service-Repository pattern**.

This style is suitable for the SEP490 MVP because it is familiar to Spring Boot teams, easy to explain in documentation, simple to scaffold, and strong enough for the project scope when combined with clear module boundaries.

Recommended shape:

- Spring Boot modular monolith backend, organized by business domain.
- Each backend domain follows `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, and optional `policy`.
- Flutter mobile app organized by feature and simple UI/API/state layers.
- Web portal organized by feature and simple page/service/API layers.
- PostgreSQL for structured relational data.
- MongoDB for flexible content, logs, RAG chunks, and metadata-heavy records.
- Firebase Storage or compatible object storage for uploaded files.
- Integration services for Gemini, TrackAsia, Firebase, ZegoCloud, VNPay, wearable/smartwatch, and MediaPipe-related data.

## 3. Layered Architecture Rules

| Layer              | Backend package                             | Responsibility                                                                                        |
| ------------------ | ------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| Presentation Layer | `controller`, `dto.request`, `dto.response` | Receive API requests, validate input shape, call services, return responses                           |
| Business Layer     | `service`, `policy`                         | Implement business workflows, healthcare safety rules, consent checks, RBAC decisions, audit triggers |
| Data Access Layer  | `repository`                                | Query and persist data through Spring Data JPA/Mongo repositories                                     |
| Persistence Model  | `entity`, `document`                        | Represent database tables/collections                                                                 |
| Mapping Layer      | `mapper`                                    | Convert request/response/entity objects                                                               |
| Integration Layer  | `integration`, external service clients     | Wrap Firebase, Gemini, TrackAsia, ZegoCloud, VNPay, storage, wearable APIs                            |
| Shared Layer       | `shared`, `common`                          | Exceptions, constants, response wrappers, validation helpers, utilities                               |

Dependency direction:

```text
Controller -> Service -> Repository -> Database
Service -> Integration Service -> External System
Controller/Service -> Mapper
Service -> Policy

```

Rules:

* Controllers must not contain business logic.
* Services own workflow logic and transaction boundaries.
* Repositories must not contain business decisions.
* Entities should not be returned directly to clients.
* DTOs should be used for API request and response payloads.
* Mappers should handle conversion between DTOs and entities.
* Policies should hold reusable business rules such as consent, red-flag triage, emergency-first routing, moderation enforcement, payment eligibility, and audit eligibility.

## 4. Repository Structure

This repository follows an SDLC folder layout reorganized to separate concerns cleanly.

```text
CareBridge_SEP490_G79/
├── 01_Planning/
│   ├── MeetingMinutes/
│   ├── ProgressReports/
│   ├── Risks/
│   └── Schedule/
├── 02_Requirements/
│   ├── BusinessRules/
│   ├── ContextDiagram/
│   ├── DFD/
│   ├── RequirementTraceabilityMatrix/
│   ├── SRS/
│   └── UseCase/
├── 03_Design/
│   ├── ActivityDiagram/
│   ├── APIDesign/
│   ├── Architecture/
│   │   ├── project-structure-design.md
│   │   ├── system-architecture.md
│   │   ├── integration-architecture.md
│   │   └── security-consent-audit-architecture.md
│   ├── ClassDiagram/
│   ├── SequenceDiagram/
│   ├── TechnicalDesign/
│   └── UI_UX/
├── 04_Implement/
│   └── implement_artifacts/
├── 05_Development/
│   ├── CareBridgeAPI/
│   ├── CareBridgeMobileApp/
│   ├── CareBridgeWebApp/
│   ├── Contracts/
│   ├── Database/
│   ├── Deployment/
│   ├── DevTools/
│   └── MachineLearning/
├── 06_Testing/
│   ├── AISafety/
│   ├── Automation/
│   ├── BugReports/
│   ├── SecurityPrivacy/
│   ├── TestCases/
│   ├── TestReports/
│   └── UAT/
├── 07_Reports/
└── 08_References/
    └── Template/

```

## 5. Backend Structure

Use one Spring Boot application. Organize packages by domain first, then by layer inside each domain. This is easier to maintain than putting all controllers in one global folder and all services in another global folder.

The backend project has already been initialized directly under `05_Development/CareBridgeAPI` with Maven wrapper, `pom.xml`, `compose.yaml`, `src/main/java/com/carebridge/backend/BackendApplication.java`, `src/main/resources/application.yaml`, and `src/test/java/com/carebridge/backend/BackendApplicationTests.java`. The domain packages shown below reflect the current package ownership under the existing package root `com.carebridge.backend`.

Current backend baseline:

* Spring Boot application root: `05_Development/CareBridgeAPI`
* Java package root: `com.carebridge.backend`
* Main class: `BackendApplication`
* Build tool: Maven
* Current dependencies: Spring Web MVC, Spring Data JPA, Spring Security, PostgreSQL driver, Lombok, Spring Boot Docker Compose, test starters
* Current local infrastructure: `compose.yaml` with PostgreSQL
* Current Supabase support: `application.yaml` includes a `supabase` profile that reads database and Supabase values from environment variables.

Current entity ownership rule:

* Business capability owns its `entity` package.
* Existing behavior modules win over broad generated packages when they are clearer bounded contexts.
* `users` stays in `security.entity` because authentication owns account behavior.
* `audit_logs` stays in `audit.entity` because audit owns sensitive action history.
* ERD entity packages must not use a catch-all package such as `database.entity`.

```text
05_Development/CareBridgeAPI/
├── .mvn/
│   └── wrapper/
├── .gitattributes
├── .gitignore
├── compose.yaml
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
├── src/main/java/com/carebridge/backend/
│   ├── BackendApplication.java
│   ├── common/
│   │   ├── config/
│   │   ├── constants/
│   │   ├── exception/
│   │   ├── response/
│   │   ├── pagination/
│   │   ├── validation/
│   │   └── util/
│   ├── security/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   ├── jwt/
│   │   ├── rbac/
│   │   ├── otp/
│   │   └── session/
│   ├── integration/
│   │   ├── firebase/
│   │   ├── firebaseStorage/
│   │   ├── gemini/
│   │   ├── trackasia/
│   │   ├── zegocloud/
│   │   ├── vnpay/
│   │   └── wearable/
│   ├── identity/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── consent/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── carejourney/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── babycare/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── healthrecord/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── carecoordination/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── reminder/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   └── mapper/
│   ├── community/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── expert/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── consultation/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── content/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── triage/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── emergency/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── safety/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── exercise/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── device/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── partner/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   ├── payment/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
│   └── audit/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       ├── dto/
│       │   ├── request/
│       │   └── response/
│       ├── mapper/
│       └── policy/
├── src/main/resources/
│   ├── application.yaml
│   ├── db/migration/
│   ├── messages/
│   ├── static/
│   └── templates/
└── src/test/java/com/carebridge/backend/
    ├── BackendApplicationTests.java
    ├── unit/
    ├── integration/
    ├── security/
    ├── consent/
    ├── triage/
    └── moderation/

```

## 6. Backend Module Convention

Each domain module should follow this pattern:

```text
{domain}/
├── controller/
│   └── {Domain}Controller.java
├── service/
│   ├── {Domain}Service.java
│   └── {Domain}ServiceImpl.java
├── repository/
│   └── {Domain}Repository.java
├── entity/
│   └── {Domain}.java
├── dto/
│   ├── request/
│   │   ├── Create{Domain}Request.java
│   │   └── Update{Domain}Request.java
│   └── response/
│       ├── {Domain}Response.java
│       └── {Domain}DetailResponse.java
├── mapper/
│   └── {Domain}Mapper.java
└── policy/
    └── {Domain}Policy.java

```

For large modules, split by sub-feature:

```text
consultation/
├── controller/
│   ├── ConsultationBookingController.java
│   ├── ConsultationSessionController.java
│   └── ConsultationDisputeController.java
├── service/
│   ├── ConsultationBookingService.java
│   ├── ConsultationSessionService.java
│   └── ConsultationDisputeService.java
├── repository/
│   ├── ConsultationBookingRepository.java
│   ├── ConsultationSessionRepository.java
│   └── ConsultationDisputeRepository.java
├── entity/
│   ├── ConsultationBooking.java
│   ├── ConsultationSession.java
│   └── ConsultationDispute.java
├── dto/
│   ├── request/
│   └── response/
├── mapper/
└── policy/

```

## 7. Current Backend Entity Ownership

The current JPA entity packages are aligned with the updated ERD data dictionary and the backend capability modules.

| Backend package           | Entity ownership                                                                                                                                        |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `security.entity`         | `User`, `RefreshToken`, `OtpVerification`                                                                                                               |
| `audit.entity`            | `AuditLog`, `SecurityEvent`, audit enums                                                                                                                |
| `consent.entity`          | `ConsentGrant`, consent enums                                                                                                                           |
| `identity.entity`         | `Role`, `UserRole`, `UserSession`, `CommunityProfile`, `NotificationPreference`, `Notification`, `DataPermission`                                       |
| `carejourney.entity`      | `MotherJourney`, `MaternalHealthMetric`, `PostpartumLog`                                                                                                |
| `babycare.entity`         | `BabyProfile`, `BabyDailyLog`, `DevelopmentMilestone`, `GrowthMeasurement`, `VaccinationRecord`                                                         |
| `healthrecord.entity`     | `HealthRecord`, `HealthSummary`                                                                                                                         |
| `carecoordination.entity` | `CareGroup`, `CareGroupMember`, `CareTask`, `Expense`                                                                                                   |
| `reminder.entity`         | `Reminder`                                                                                                                                              |
| `community.entity`        | `CommunityTopic`, `CommunityQuestion`, `CommunityAnswer`, `ContributionPoint`                                                                           |
| `content.entity`          | `ContentItem`, `ContentReport`, `ModerationAction`, `ChecklistTemplate`, `ChecklistItem`                                                                |
| `expert.entity`           | `ExpertProfile`, `ExpertCredential`, `ExpertAvailability`, `ExpertLocationShare`, `ExpertReview`                                                        |
| `consultation.entity`     | `ConsultationBooking`, `ConsultationSession`, `ConsultationMessage`                                                                                     |
| `payment.entity`          | `PaymentTransaction`, `CommissionRecord`, `ConsultationPriceBand`, `ExpertConsultationPrice`, `ConsultationDispute`, `RefundRecord`, `SettlementRecord` |
| `partner.entity`          | `PartnerOrganization`, `PartnerExpertLink`, `PartnerService`, `SponsoredCampaign`, `CareFacility`                                                       |
| `emergency.entity`        | `EmergencyEvent`, `LocationSnapshot`                                                                                                                    |
| `device.entity`           | `HealthDeviceConnection`, `DeviceMeasurement`                                                                                                           |
| `safety.entity`           | `SafetyMonitoringSetting`, `SafetyEvent`, `SafetyAlert`                                                                                                 |
| `exercise.entity`         | `PregnancyExercise`, `ExerciseSafetyCheck`, `ExerciseSession`, `PostureAnalysisConfig`, `PostureFeedbackEvent`                                          |
| `triage.entity`           | `TriageAssessment`, `TriageAnswer`                                                                                                                      |

Notes:

* `family` remains a future capability module only. The current ERD does not define `families`, `family_members`, or similar family-owned tables. Family coordination data is currently represented through `carecoordination.entity`.
* `ai` is not an entity-owning module. AI triage persistence belongs to `triage.entity`; AI providers and RAG clients belong under `integration.gemini` or future AI integration packages.
* `database.entity` must remain empty or absent. It is too broad and hides business ownership.

## 8. Example: Consultation Module

```text
consultation/
├── controller/
│   ├── ConsultationBookingController.java
│   └── ConsultationSessionController.java
├── service/
│   ├── ConsultationBookingService.java
│   ├── ConsultationBookingServiceImpl.java
│   ├── ConsultationSessionService.java
│   └── ConsultationSessionServiceImpl.java
├── repository/
│   ├── ConsultationBookingRepository.java
│   └── ConsultationSessionRepository.java
├── entity/
│   ├── ConsultationBooking.java
│   ├── ConsultationSession.java
│   └── ConsultationSummary.java
├── dto/
│   ├── request/
│   │   ├── CreateConsultationBookingRequest.java
│   │   ├── UpdateConsultationStatusRequest.java
│   │   └── WriteConsultationSummaryRequest.java
│   └── response/
│       ├── ConsultationBookingResponse.java
│       ├── ConsultationSessionResponse.java
│       └── ConsultationSummaryResponse.java
├── mapper/
│   ├── ConsultationBookingMapper.java
│   └── ConsultationSessionMapper.java
└── policy/
    ├── ConsultationPaymentPolicy.java
    ├── ConsultationConsentPolicy.java
    └── ConsultationSafetyPolicy.java

```

Request flow:

```text
ConsultationBookingController
  -> ConsultationBookingService
  -> ConsultationConsentPolicy / ConsultationPaymentPolicy
  -> ConsultationBookingRepository
  -> Database

```

Integration flow:

```text
ConsultationBookingService
  -> PaymentService / VNPayIntegrationService
  -> VNPay

```

Audit flow:

```text
ConsultationBookingService
  -> AuditService
  -> AuditLogRepository

```

## 9. Backend Domain Module Map

| Functional domain                 | Backend package                                         | Key responsibilities                                                                  |
| --------------------------------- | ------------------------------------------------------- | ------------------------------------------------------------------------------------- |
| Authentication, profile, role     | `security`, `identity`                                  | Login, OTP, JWT/session, profile, role, account lifecycle                             |
| Privacy and consent               | `consent`, `audit`                                      | Permission scope, sharing grant, expiry, revoke, sharing history                      |
| Mother, baby, and care journey    | `carejourney`, `babycare`, `healthrecord`, `reminder`   | Mother journey, baby profile, health metrics, health records, reminders               |
| Care coordination and family sync | `carecoordination`, `consent`, `reminder`               | Care group, invitation, shared permissions, shared tasks, shared expenses             |
| Community Q&A                     | `community`                                             | Topics, questions, answers, anonymous display, contribution points                    |
| Content and moderation            | `content`                                               | Articles/content items, reports, moderation actions, checklist templates/items        |
| Expert ecosystem                  | `expert`, `consultation`                                | Expert profile, verification, credentials, availability, expert reviews, consultation |
| AI triage and RAG                 | `triage`, `integration.gemini`                          | Intake, triage answers, red-flag rules, safe recommendation, AI provider integration  |
| Emergency map and nearby care     | `emergency`, `partner`, `integration.trackasia`         | Emergency flow, nearby care search, route, location snapshot, care facilities         |
| Safety monitoring                 | `safety`, `device`, `emergency`, `integration.firebase` | Device measurements, safety settings, safety events, alerts, false-positive feedback  |
| Pregnancy exercise and posture    | `exercise`                                              | Exercise library, safety checks, sessions, posture config/feedback                    |
| Partner and sponsored content     | `partner`, `content`                                    | Partner profile, service listing, sponsored content governance                        |
| Payment, refund, commission       | `payment`, `consultation`, `integration.vnpay`          | Transactions, refunds, settlement, commission                                         |
| Audit and security incident       | `audit`, `security`                                     | Audit log, security event, incident investigation, access review                      |

## 10. Web Portal Structure

Use one Vite React TypeScript application initialized directly under `05_Development/CareBridgeWebApp`. Keep Vite project files at the frontend root, then organize application code with feature-based folders inside `src/`.

Current frontend baseline:

* React application root: `05_Development/CareBridgeWebApp`
* Build tool: Vite
* Language: TypeScript
* Entry point: `src/main.tsx`
* Root component: `src/App.tsx`
* Package/dependency files: `package.json`, `package-lock.json`
* Generated/local folders such as `node_modules/` and `dist/` are excluded by `.gitignore`.

Primary frontend dependencies:

* `react`, `react-dom`
* `@supabase/supabase-js`
* `react-router-dom`
* `@tanstack/react-query`
* `axios`
* `zustand`
* `react-hook-form`, `@hookform/resolvers`, `zod`
* `dayjs`
* `lucide-react`

Recommended structure:

```text
05_Development/CareBridgeWebApp/
├── .gitignore
├── README.md
├── eslint.config.js
├── index.html
├── package.json
├── package-lock.json
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── vite.config.ts
├── public/
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── App.css
    ├── index.css
    ├── vite-env.d.ts
    ├── assets/
    ├── app/
    │   ├── router/
    │   ├── providers/
    │   ├── layouts/
    │   └── guards/
    ├── shared/
    │   ├── api/
    │   ├── auth/
    │   ├── components/
    │   ├── forms/
    │   ├── tables/
    │   ├── charts/
    │   ├── hooks/
    │   ├── utils/
    │   └── constants/
    └── features/
        ├── auth/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── dashboard/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── expertVerification/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── moderation/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── contentManagement/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── partnerGovernance/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── consultationManagement/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── paymentRefunds/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── auditSecurity/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        ├── aiRuleManagement/
        │   ├── pages/
        │   ├── components/
        │   ├── services/
        │   └── models/
        └── postureConfiguration/
            ├── pages/
            ├── components/
            ├── services/
            └── models/

```

Web route groups:

* `/admin`: user management, role/permission, expert/partner approval, dashboards, audit/security events.
* `/moderator`: moderation queue, reports, warnings, suspensions, topic governance.
* `/content`: articles, FAQ, checklist, pregnancy exercises, content versions, RAG index status.
* `/expert`: profile, credential upload, availability, question queue, consultations, summaries, earnings.
* `/partner`: partner profile, service listing, sponsored content, performance.

## 11. Mobile App Structure

Use feature-based Flutter structure with simple UI, service, model, and repository folders.

```text
05_Development/CareBridgeMobileApp/
├── pubspec.yaml
├── android/
├── ios/
├── web/
├── test/
└── lib/
    ├── main.dart
    ├── app/
    │   ├── router/
    │   ├── theme/
    │   ├── localization/
    │   └── environment/
    ├── core/
    │   ├── network/
    │   ├── storage/
    │   ├── auth/
    │   ├── permissions/
    │   ├── errors/
    │   ├── widgets/
    │   ├── utils/
    │   └── constants/
    ├── shared/
    │   ├── components/
    │   ├── models/
    │   └── services/
    ├── features/
    │   ├── onboarding/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── auth/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── motherJourney/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── babyCare/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── healthRecords/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── familySync/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── community/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── consultation/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── aiTriage/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── emergencyMap/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   ├── safetyMonitoring/
    │   │   ├── screens/
    │   │   ├── widgets/
    │   │   ├── services/
    │   │   ├── repositories/
    │   │   └── models/
    │   └── pregnancyExercise/
    │       ├── screens/
    │       ├── widgets/
    │       ├── services/
    │       ├── repositories/
    │       └── models/
    ├── integrations/
    │   ├── firebaseMessaging/
    │   ├── firebaseStorage/
    │   ├── trackasia/
    │   ├── zegocloud/
    │   ├── mediapipe/
    │   ├── imuSensor/
    │   └── wearable/
    └── l10n/

```

Flutter rule:

```text
Screen/Widget -> Service -> Repository -> API/Local Storage/Device SDK

```

## 12. Database and Contracts

```text
05_Development/Database/
├── postgres/
│   ├── migrations/
│   ├── seeds/
│   ├── views/
│   ├── functions/
│   └── indexes/
├── mongodb/
│   ├── collections/
│   ├── indexes/
│   └── seeds/
├── sample-data/
└── docs/

05_Development/Contracts/
├── openapi/
│   └── carebridge-api.yaml
├── events/
│   ├── notification-events.schema.json
│   ├── audit-events.schema.json
│   └── safety-events.schema.json
├── permissions/
│   ├── rbac-matrix.md
│   └── consent-scope-catalog.md
├── messages/
│   ├── application-message-catalog.md
│   └── error-code-catalog.md
└── integrations/
    ├── firebase.md
    ├── gemini.md
    ├── trackasia.md
    ├── zegocloud.md
    └── vnpay.md

```

Recommended data split:

* PostgreSQL: identity, RBAC, consent, care journey, baby profile, health metadata, care coordination groups, reminders, consultation lifecycle, payments, partner profile, moderation state, audit indexes.
* MongoDB: community post bodies, content versions, FAQ/checklist rich content, AI prompt/response metadata, flexible safety logs, RAG document chunks, device raw metadata.
* Object storage: health record files, credential documents, content media, exercise media.

## 13. Cross-Cutting Policies

Security:

* Use RBAC for user, mother, family member, verified expert, moderator, content admin, system admin, and partner representative.
* Use method-level authorization for sensitive flows.
* Keep access tokens short-lived and support refresh/session controls.

Consent:

* Model consent by data type, purpose, recipient, expiry, and revocation status.
* Enforce consent before returning health records, baby data, family data, location snapshots, safety alerts with location, expert-shared data, and RAG context using selected user data.
* Implement consent checks in `ConsentService` and reusable consent policies.

Healthcare safety:

* AI output is support only, not diagnosis or prescription.
* Red-flag routing is owned by `TriageService` and `RedFlagPolicy`, not delegated fully to Gemini.
* Emergency flow must be reachable from triage, safety alert, and dashboard.
* Payment or paid consultation suggestion must not block emergency-first actions.

Audit:

* Audit logs should be append-only from the application perspective.
* Prioritize audit for RBAC changes, consent, health record access, expert verification, moderation, AI triage, emergency/safety events, payment/refund, and security incident review.
* Trigger audit records from services after sensitive actions.

Integration reliability:

* Each external system should have a dedicated integration service.
* Gemini unavailable: show conservative fallback guidance.
* TrackAsia unavailable: show manual emergency guidance and quick-call options.
* Firebase notification failure: keep event state and retry.
* VNPay failure: booking remains unpaid/unconfirmed.
* ZegoCloud unavailable: fallback to chat or reschedule.

## 14. Testing Structure

```text
06_Testing/
├── TestCases/
│   ├── backend/
│   ├── mobile/
│   ├── web/
│   └── integration/
├── Automation/
│   ├── backend-api/
│   ├── mobile-e2e/
│   └── web-e2e/
├── SecurityPrivacy/
│   ├── rbac-tests.md
│   ├── consent-tests.md
│   └── sensitive-data-access-tests.md
├── AISafety/
│   ├── red-flag-scenarios.md
│   ├── medication-boundary-tests.md
│   ├── emergency-routing-tests.md
│   ├── postpartum-mental-health-tests.md
│   └── child-fever-tests.md
└── UAT/
    ├── mother-scenarios.md
    ├── family-scenarios.md
    ├── expert-scenarios.md
    └── moderator-admin-scenarios.md

```

Minimum gates:

* Unit tests for services and policies.
* Repository tests for important database queries.
* Integration tests for RBAC, consent, audit, payment callback, notification callback, and AI fallback.
* AI safety tests for red flags, medication advice, emergency symptoms, postpartum mental health, child fever, and unsupported diagnosis requests.
* E2E tests for critical mother, family, expert, moderator, admin, and partner flows.

## 15. Implementation Order

1. Foundation: backend app, Flutter app, web portal, database migration baseline, OpenAPI contracts, CI pipeline.
2. Security core: auth, OTP, sessions, RBAC, profile, privacy settings.
3. Consent and audit core: consent grants, sharing scopes, immutable audit events, sensitive access policies.
4. Care MVP: mother journey, baby profile, health records, reminders, family sync.
5. Community and governance: community feed, reporting, moderation, expert answer labels.
6. Expert and consultation: expert verification, availability, booking lifecycle, summaries, payment sandbox.
7. AI and safety: Gemini intake/RAG wrapper, red-flag rules, conservative fallback, AI logs.
8. Emergency and location: TrackAsia integration, nearby care search, quick call, location consent.
9. Safety monitoring and exercise: IMU detection demo, family alert, pregnancy exercise, MediaPipe posture support.
10. Partner, dashboard, hardening: partner governance, impact dashboards, security review, UAT, release package.

## 16. Decisions

| Decision                 | Recommendation                                                          | Reason                                                                 |
| ------------------------ | ----------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Repository shape         | Use the reorganized 8-folder SDLC structure and expand `05_Development` | Matches current repo and academic deliverable structure.               |
| Backend architecture     | Layered Architecture using Controller-Service-Repository pattern        | Familiar, simple, and suitable for Spring Boot MVP delivery.           |
| Backend package style    | Package by domain, layer inside each domain                             | Keeps related code together while preserving layered responsibilities. |
| Backend module structure | `controller/service/repository/entity/dto/mapper/policy`                | Directly matches common Spring Boot practice.                          |
| Frontend architecture    | Feature-based pages/components/services/models                          | Practical for admin/expert/partner portal development.                 |
| Mobile architecture      | Feature-based screens/widgets/services/repositories/models              | Practical for Flutter team implementation.                             |
| Data architecture        | PostgreSQL plus MongoDB plus object storage                             | Matches structured workflows, flexible content/logs, and file uploads. |
| Integration strategy     | Dedicated integration services with fallback behavior                   | Reduces schedule risk and supports demo mode.                          |
| Safety strategy          | Centralized consent, audit, RBAC, and triage policies                   | Required by healthcare-safe positioning and sensitive data handling.   |

# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.
