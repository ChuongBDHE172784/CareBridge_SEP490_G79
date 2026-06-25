---
title: CareBridge Project Structure Design
project: CareBridge_SEP490_G79
source_documents:
  - 01_Requirements/SRS/Report1_Project Introduction.docx.md
  - 01_Requirements/SRS/Report2_Project Management Plan.docx.md
  - 01_Requirements/SRS/Report3_Software Requirement Specification.docx.md
created_by: bmad-create-architecture
date: 2026-06-17
status: draft
architecture_style: Layered Architecture using Controller-Service-Repository pattern
---
# CareBridge Project Structure Design

## 1. Reading Report

This design is derived from the three supplied project documents:

- Report 1 - Project Introduction: CareBridge is a maternal and early childhood healthcare support platform for pre-pregnancy, pregnancy, postpartum, baby care, family coordination, expert guidance, moderated community, AI-assisted triage, emergency support, and legal-safe healthcare boundaries.
- Report 2 - Project Management Plan: the MVP is Hanoi-first, academic-scope, and uses Flutter, Java Spring Boot, PostgreSQL,  Redis or lightweight caching, Firebase Cloud Messaging, TrackAsia, Firebase Storage, GitLab CI/CD, AWS, and Vercel.
- Report 3 - Software Requirement Specification: the system includes mobile app, web portal, backend services, Gemini AI/RAG, ZegoCloud, Firebase chat/storage/notification, TrackAsia, smartwatch data, phone IMU, MediaPipe posture analysis, VNPay, consent, audit, and security event workflows.

## 2. Architecture Position

CareBridge should use **Layered Architecture using Controller-Service-Repository pattern**.

This style is suitable for the SEP490 MVP because it is familiar to Spring Boot teams, easy to explain in documentation, simple to scaffold, and strong enough for the project scope when combined with clear module boundaries.

Recommended shape:

- Spring Boot modular monolith backend, organized by business domain.
- Each backend domain follows `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, and optional `policy`.
- Flutter mobile app organized by feature and simple UI/API/state layers.
- Web portal organized by feature and simple page/service/API layers.
- PostgreSQL for structured relational data.
- Firebase Storage or compatible object storage for uploaded files.
- Integration services for Gemini, TrackAsia, Firebase, ZegoCloud, VNPay, wearable/smartwatch, and MediaPipe-related data.

## 3. Layered Architecture Rules

| Layer              | Backend package                                   | Responsibility                                                                                        |
| ------------------ | ------------------------------------------------- | ----------------------------------------------------------------------------------------------------- |
| Presentation Layer | `controller`, `dto.request`, `dto.response` | Receive API requests, validate input shape, call services, return responses                           |
| Business Layer     | `service`, `policy`                           | Implement business workflows, healthcare safety rules, consent checks, RBAC decisions, audit triggers |
| Data Access Layer  | `repository`                                    | Query and persist data through Spring Data JPA/, repositories                                        |
| Persistence Model  | `entity`, `document`                          | Represent database tables/collections                                                                 |
| Mapping Layer      | `mapper`                                        | Convert request/response/entity objects                                                               |
| Integration Layer  | `integration`, external service clients         | Wrap Firebase, Gemini, TrackAsia, ZegoCloud, VNPay, storage, wearable APIs                            |
| Shared Layer       | `shared`, `common`                            | Exceptions, constants, response wrappers, validation helpers, utilities                               |

Dependency direction:

```text
Controller -> Service -> Repository -> Database
Service -> Integration Service -> External System
Controller/Service -> Mapper
Service -> Policy
```

Rules:

- Controllers must not contain business logic.
- Services own workflow logic and transaction boundaries.
- Repositories must not contain business decisions.
- Entities should not be returned directly to clients.
- DTOs should be used for API request and response payloads.
- Mappers should handle conversion between DTOs and entities.
- Policies should hold reusable business rules such as consent, red-flag triage, emergency-first routing, moderation enforcement, payment eligibility, and audit eligibility.

## 4. Repository Structure

Preserve the current SDLC folders. Expand implementation detail under `04_SourceCode`.

```text
CareBridge_SEP490_G79/
├── 01_Planning/                        ← Kế hoạch dự án
│   ├── MeetingMinutes/
│   ├── ProgressReports/
│   ├── Risks/
│   └── Schedule/
├── 02_Requirements/                    ← Yêu cầu phần mềm
│   ├── BusinessRules/
│   ├── ContextDiagram/
│   ├── DFD/
│   ├── RequirementTraceabilityMatrix/
│   ├── SRS/
│   └── UseCase/
├── 03_Design/                          ← Thiết kế hệ thống
│   ├── ActivityDiagram/
│   ├── APIDesign/
│   ├── Architecture/
│   ├── ClassDiagram/
├── 04_Implement/                       ← Kế hoạch triển khai
│   └── implement_artifacts/
├── 05_Development/                     ← Source code đầy đủ
│   ├── CareBridgeAPI/                     (Backend — Spring Boot)
│   ├── CareBridgeMobileApp/               (Mobile — Flutter)
│   ├── CareBridgeWebApp/                  (Web Portal — React + Vite)
│   ├── Contracts/
│   ├── Database/
│   ├── Deployment/
│   └── DevTools/
├── 06_Testing/                         ← Kiểm thử
│   ├── AISafety/
│   ├── Automation/
│   ├── BugReports/
│   ├── SecurityPrivacy/
│   ├── TestCases/
│   ├── TestReports/
│   └── UAT/
├── 07_Reports/                         ← Báo cáo nộp trường
└── 08_References/                      ← Tài liệu tham khảo
    └── Template/
```

## 5. Backend Structure

Use one Spring Boot application. Organize packages by domain first, then by layer inside each domain. This is easier to maintain than putting all controllers in one global folder and all services in another global folder.

The backend project has already been initialized directly under `04_SourceCode/Backend` with Maven wrapper, `pom.xml`, `compose.yaml`, `src/main/java/com/carebridge/backend/BackendApplication.java`, `src/main/resources/application.yaml`, and `src/test/java/com/carebridge/backend/BackendApplicationTests.java`. The domain packages shown below are the recommended next folders to add under the existing package root `com.carebridge.backend`.

Current backend baseline:

- Spring Boot application root: `04_SourceCode/Backend`
- Java package root: `com.carebridge.backend`
- Main class: `BackendApplication`
- Build tool: Maven
- Current dependencies: Spring Web MVC, Spring Data JPA, Spring Security, PostgreSQL driver, Lombok, Spring Boot Docker Compose, test starters
- Current local infrastructure: `compose.yaml` with PostgreSQL

```text
04_SourceCode/Backend/
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
│   ├── reminder/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   └── mapper/
│   ├── family/
│   │   ├── controller/
│   │   ├── service/
│   │   ├── repository/
│   │   ├── entity/
│   │   ├── dto/
│   │   │   ├── request/
│   │   │   └── response/
│   │   ├── mapper/
│   │   └── policy/
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
│   ├── application-local.yaml
│   ├── application-dev.yaml
│   ├── application-demo.yaml
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

## 7. Example: Consultation Module

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

## 8. Backend Domain Module Map

| SRS domain                     | Backend package                                      | Key responsibilities                                                    |
| ------------------------------ | ---------------------------------------------------- | ----------------------------------------------------------------------- |
| Authentication, profile, role  | `security`, `identity`                           | Login, OTP, JWT/session, profile, role, account lifecycle               |
| Privacy and consent            | `consent`, `audit`                               | Permission scope, sharing grant, expiry, revoke, sharing history        |
| Mother and baby journey        | `carejourney`, `healthrecord`, `reminder`      | Mother journey, baby profile, health metrics, health records, reminders |
| Community Q&A                  | `community`                                        | Posts, answers, anonymous display, reports, moderation queue            |
| Expert ecosystem               | `expert`, `consultation`                         | Expert profile, verification, availability, expert answer, consultation |
| AI triage and RAG              | `triage`, `content`, `integration.gemini`      | Intake, RAG answer, red-flag rules, safe recommendation, AI logs        |
| Emergency map and nearby care  | `emergency`, `integration.trackasia`             | Emergency flow, nearby care search, route, location snapshot            |
| Family sync                    | `family`, `consent`, `reminder`                | Care group, invitation, shared permissions, shared tasks                |
| Safety monitoring              | `safety`, `emergency`, `integration.firebase`  | Safety settings, safety events, alerts, false-positive feedback         |
| Pregnancy exercise and posture | `exercise`                                         | Exercise library, safety checks, sessions, posture config/feedback      |
| Partner and sponsored content  | `partner`, `content`                             | Partner profile, service listing, sponsored content governance          |
| Payment, refund, commission    | `payment`, `consultation`, `integration.vnpay` | Transactions, refunds, settlement, commission                           |
| Audit and security incident    | `audit`, `security`                              | Audit log, security event, incident investigation, access review        |

## 9. Web Portal Structure

Use one Vite React TypeScript application initialized directly under `04_SourceCode/Frontend`. Keep Vite project files at the frontend root, then organize application code with feature-based folders inside `src/`.

Current frontend baseline:

- React application root: `04_SourceCode/Frontend`
- Build tool: Vite
- Language: TypeScript
- Entry point: `src/main.tsx`
- Root component: `src/App.tsx`
- Package/dependency files: `package.json`, `package-lock.json`
- Generated/local folders such as `node_modules/` and `dist/` are excluded by `.gitignore`.

Primary frontend dependencies:

- `react`, `react-dom`
- `react-router-dom`
- `@tanstack/react-query`
- `axios`
- `zustand`
- `react-hook-form`, `@hookform/resolvers`, `zod`
- `dayjs`
- `lucide-react`

Recommended structure:

```text
04_SourceCode/Frontend/
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

- `/admin`: user management, role/permission, expert/partner approval, dashboards, audit/security events.
- `/moderator`: moderation queue, reports, warnings, suspensions, topic governance.
- `/content`: articles, FAQ, checklist, pregnancy exercises, content versions, RAG index status.
- `/expert`: profile, credential upload, availability, question queue, consultations, summaries, earnings.
- `/partner`: partner profile, service listing, sponsored content, performance.

## 10. Mobile App Structure

Use feature-based Flutter structure with simple UI, service, model, and repository folders.

```text
04_SourceCode/MobileApp/
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

## 11. Database and Contracts

```text
04_SourceCode/Database/
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

04_SourceCode/Contracts/
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

- PostgreSQL: identity, RBAC, consent, care journey, baby profile, health metadata, family group, reminders, consultation lifecycle, payments, partner profile, moderation state, audit indexes.
- Object storage: health record files, credential documents, content media, exercise media.

## 12. Cross-Cutting Policies

Security:

- Use RBAC for user, mother, family member, verified expert, moderator, content admin, system admin, and partner representative.
- Use method-level authorization for sensitive flows.
- Keep access tokens short-lived and support refresh/session controls.

Consent:

- Model consent by data type, purpose, recipient, expiry, and revocation status.
- Enforce consent before returning health records, baby data, family data, location snapshots, safety alerts with location, expert-shared data, and RAG context using selected user data.
- Implement consent checks in `ConsentService` and reusable consent policies.

Healthcare safety:

- AI output is support only, not diagnosis or prescription.
- Red-flag routing is owned by `TriageService` and `RedFlagPolicy`, not delegated fully to Gemini.
- Emergency flow must be reachable from triage, safety alert, and dashboard.
- Payment or paid consultation suggestion must not block emergency-first actions.

Audit:

- Audit logs should be append-only from the application perspective.
- Prioritize audit for RBAC changes, consent, health record access, expert verification, moderation, AI triage, emergency/safety events, payment/refund, and security incident review.
- Trigger audit records from services after sensitive actions.

Integration reliability:

- Each external system should have a dedicated integration service.
- Gemini unavailable: show conservative fallback guidance.
- TrackAsia unavailable: show manual emergency guidance and quick-call options.
- Firebase notification failure: keep event state and retry.
- VNPay failure: booking remains unpaid/unconfirmed.
- ZegoCloud unavailable: fallback to chat or reschedule.

## 13. Testing Structure

```text
05_Testing/
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

- Unit tests for services and policies.
- Repository tests for important database queries.
- Integration tests for RBAC, consent, audit, payment callback, notification callback, and AI fallback.
- AI safety tests for red flags, medication advice, emergency symptoms, postpartum mental health, child fever, and unsupported diagnosis requests.
- E2E tests for critical mother, family, expert, moderator, admin, and partner flows.

## 14. Implementation Order

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

## 15. Decisions

| Decision                 | Recommendation                                                   | Reason                                                                 |
| ------------------------ | ---------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Repository shape         | Preserve SDLC folders and expand`04_SourceCode`                | Matches current repo and academic deliverable structure.               |
| Backend architecture     | Layered Architecture using Controller-Service-Repository pattern | Familiar, simple, and suitable for Spring Boot MVP delivery.           |
| Backend package style    | Package by domain, layer inside each domain                      | Keeps related code together while preserving layered responsibilities. |
| Backend module structure | `controller/service/repository/entity/dto/mapper/policy`       | Directly matches common Spring Boot practice.                          |
| Frontend architecture    | Feature-based pages/components/services/models                   | Practical for admin/expert/partner portal development.                 |
| Mobile architecture      | Feature-based screens/widgets/services/repositories/models       | Practical for Flutter team implementation.                             |
| Data architecture        | PostgreSQL plus object storage                                  | Matches structured workflows, flexible content/logs, and file uploads. |
| Integration strategy     | Dedicated integration services with fallback behavior            | Reduces schedule risk and supports demo mode.                          |
| Safety strategy          | Centralized consent, audit, RBAC, and triage policies            | Required by healthcare-safe positioning and sensitive data handling.   |
