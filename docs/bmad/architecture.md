# CareBridge Architecture Standards

**Status**: Draft  
**Version**: 1.0  
**Date**: 2026-06-17  
**Architect**: Claude Sonnet 4.6 (Anthropic)  
**Source**: Derived from `02_Design/Architecture/project-structure-design.md` and SRS documents

---

## 1. Architectural Vision

CareBridge uses a **layered modular monolith** architecture for the backend, with clear domain boundaries and dependency rules. The system consists of:

- **Backend**: Java Spring Boot application following Controller-Service-Repository pattern
- **Frontend**: React + Vite + TypeScript admin and partner portals
- **Mobile**: Flutter mobile application for mothers, families, and experts
- **Data**: PostgreSQL for relational data, object storage for uploaded files, and Firebase Realtime Database only for approved realtime use cases.
- **Integrations**: External services wrapped in dedicated integration services

This architecture balances simplicity for a student team with scalability sufficient for the Hanoi pilot.

---

## 2. Backend Layered Architecture

### 2.1 Layer Responsibilities
| Layer | Package Pattern | Responsibility | Allowed Dependencies |
|---|---|---|---|
| **Presentation** | `controller`, `dto.request`, `dto.response` | HTTP request handling, input validation, response formatting | Service, DTO |
| **Business** | `service`, `policy` | Business workflows, safety rules, consent checks, RBAC, audit triggers | Repository, Integration, Policy, Mapper |
| **Data Access** | `repository` | CRUD and query operations using Spring Data JPA | Entity |
| **Persistence Model** | `entity` | Database table mappings using JPA entities | None |
| **Mapping** | `mapper` | Conversion between DTOs and entities | Entity, DTO |
| **Integration** | `integration` | External service clients: Gemini, Firebase, TrackAsia, ZegoCloud, VNPay, Gmail SMTP, CompreFace | External SDKs/APIs |
| **Shared** | `common` | Cross-cutting concerns: responses, exceptions, constants, validation, pagination, utilities | None or minimal |

### 2.2 Dependency Rules

**Direction**: Upper layers may depend on lower layers, never vice versa.

```
Controller -> Service -> Repository -> Database
Controller -> DTO
Service -> Mapper
Service -> Policy
Service -> Integration
Repository -> Entity
Mapper -> DTO + Entity
```

**Strict Prohibitions**:
- ❌ Controllers must not contain business logic
- ❌ Services must not directly access HTTP request/response objects
- ❌ Repositories must not contain business decisions
- ❌ Entities must not be returned directly from controllers (use DTOs)
- ❌ Services must not call other services' repositories directly
- ❌ Integration services must not contain business logic (only protocol handling)

---

## 3. Backend Domain Structure

Package by domain first, then by layer inside each domain.

### 3.1 Domain Module Pattern

```
com.carebridge.backend.
├── {domain}/
│   ├── controller/      (optional, if domain has API endpoints)
│   ├── service/         (interface + impl)
│   ├── repository/      (Spring Data interface)
│   ├── entity/          (JPA @Entity classes)
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── mapper/          (MapStruct or manual)
│   └── policy/          (business rules: consent, red-flag, audit eligibility)
```

### 3.2 Domain Catalog

| Domain | Purpose | Key Entities | Key Policies |
|--------|---------|--------------|--------------|
| `security` | Authentication, sessions, JWT, OTP | User, Session, OTPVerification | AuthenticationPolicy, SessionPolicy |
| `identity` | Profile management, role assignment | Profile, Role, Permission | ProfilePolicy, RoleAssignmentPolicy |
| `consent` | Consent grants, revocation, scope | ConsentGrant, ConsentScope | ConsentCheckPolicy, ConsentValidityPolicy |
| `audit` | Audit logging, security events | AuditLog, SecurityEvent | AuditEligibilityPolicy, RetentionPolicy |
| `carejourney` | Mother journey stages, milestones | JourneyStage, Milestone, StageLog | JourneyTransitionPolicy, StageAccessPolicy |
| `healthrecord` | Health measurements, files | HealthRecord, Measurement, FileAttachment | RecordAccessPolicy, FileUploadPolicy |
| `reminder` | Reminders and notifications | Reminder, NotificationTemplate | ReminderEligibilityPolicy |
| `family` | Family groups, invitations, sharing | FamilyGroup, FamilyMember, SharingGrant | InvitationPolicy, SharingScopePolicy |
| `community` | Posts, comments, categories, reports | Post, Comment, Category, Report | PostingPolicy, ReportingPolicy |
| `expert` | Expert profiles, credentials, verification | ExpertProfile, Credential, Verification | VerificationPolicy, ExpertAccessPolicy |
| `consultation` | Booking, sessions, summaries | Consultation, ConsultationSession, Summary | BookingPolicy, PaymentCheckPolicy |
| `content` | Articles, FAQ, exercises | Article, FAQ, Exercise, ContentVersion | ContentPublishPolicy, VersioningPolicy |
| `triage` | Symptom intake, risk classification | TriageSession, SymptomLog, RiskLevel | TriagePolicy, RedFlagPolicy |
| `emergency` | Emergency flows, contacts, location | EmergencyEvent, Contact, LocationSnapshot | EmergencyAccessPolicy, LocationConsentPolicy |
| `safety` | Safety monitoring, IMU events | SafetyEvent, SafetySettings, FalsePositive | MonitoringPolicy, AlertPolicy |
| `exercise` | Exercise library, posture feedback | Exercise, ExerciseSession, PostureFeedback | ExerciseSafetyPolicy |
| `partner` | Partner profiles, sponsored content | Partner, SponsoredContent, Performance | PartnerApprovalPolicy, ContentGovernancePolicy |
| `payment` | Transactions, refunds, commissions | Payment, Refund, Commission | PaymentPolicy, RefundPolicy |

---

## 4. Cross-Cutting Concerns

### 4.1 Authentication and Authorization

- Use **Spring Security** with method-level security (`@PreAuthorize`)
- **JWT-based authentication is used for both mobile API and web portal. Session-based authentication must not be introduced unless explicitly approved.**
- OTP verification for registration and sensitive operations
- RBAC roles: `ROLE_MOTHER`, `ROLE_FAMILY`, `ROLE_EXPERT`, `ROLE_MODERATOR`, `ROLE_CONTENT_ADMIN`, `ROLE_SYSTEM_ADMIN`, `ROLE_PARTNER`

**Authentication Endpoints**:
- `POST /api/v1/auth/register` — sends OTP for new users
- `POST /api/v1/auth/login` — sends OTP for existing users
- `POST /api/v1/auth/verify-otp` — verifies OTP for REGISTER or LOGIN; returns `accessToken`, `refreshToken`, and `user`
- `POST /api/v1/auth/refresh` — rotates refresh token
- `POST /api/v1/auth/logout` — revokes refresh token

**Security Configuration** (`SecurityConfig.java`):

Configure Spring Security with the following endpoint rules:

| Endpoint Pattern | HTTP Methods | Access Control |
|------------------|--------------|----------------|
| `/api/v1/auth/register` | POST | Public (no authentication required) |
| `/api/v1/auth/login` | POST | Public |
| `/api/v1/auth/verify-otp` | POST | Public |
| `/api/v1/auth/refresh` | POST | Public |
| `/api/v1/auth/profile` | GET, PUT | Authenticated (JWT required) |
| `/api/v1/auth/logout` | POST | Authenticated |
| `/api/v1/consent/grants` | GET, POST | Authenticated |
| `/api/v1/consent/grants/*` | DELETE | Authenticated |
| All other `/api/v1/**` | All | Authenticated + Role-based authorization (use `@PreAuthorize`) |

- Stateless JWT authentication (no sessions)
- CORS configured for Vite dev server (`http://localhost:5173`)
- Method-level security with `@PreAuthorize` for role checks

**Example**:
```java
@PreAuthorize("hasRole('EXPERT') or hasRole('MODERATOR')")
@GetMapping("/posts/{id}/answers")
public List<AnswerResponse> getAnswers(@PathVariable Long id) {
    // ...
}
```

### 4.2 Consent Management

- `ConsentService` centralizes consent checks
- Policies (`ConsentCheckPolicy`) enforce before data access
- Consent model: `dataType`, `purpose`, `recipient`, `scope`, `expiry`, `revoked`
- Store consent grants in `consent_grants` table
- Audit every consent check and data access

**Example flow**:
```java
@Service
public class HealthRecordService {
    public HealthRecordResponse getRecord(Long recordId, User user) {
        ConsentCheckPolicy.ensureConsent(user, recordId, DataType.HEALTH_RECORD, Purpose.VIEW);
        AuditService.log(Action.VIEW_HEALTH_RECORD, user, recordId);
        return mapper.toResponse(repository.findById(recordId));
    }
}
```

### 4.3 Audit Logging

- Immutable `audit_logs` table (no UPDATE/DELETE, only INSERT)
- Log fields: `id`, `timestamp`, `userId`, `action`, `resourceType`, `resourceId`, `details` (JSON), `ipAddress`, `userAgent`
- Trigger from services after sensitive actions
- Separate `security_events` table for security incidents (failed login, permission denied, tampering)
- Admin portal provides audit log viewer with filters

### 4.4 Healthcare Safety Guardrails

- **AI Output**: Always display disclaimer; never allow AI to suggest diagnosis, prescription, or treatment
- **Red-Flag Detection**: `TriageService` and `RedFlagPolicy` must route to emergency first
- **Emergency Priority**: Emergency flow must be accessible within 2 taps from any screen
- **No Commerce Blocking**: Payment steps cannot block emergency access
- **Content Labeling**: Expert answers labeled "verified expert", community answers labeled "community member"

---

## 5. Data Architecture

### 5.1 Primary Database

CareBridge uses PostgreSQL as the primary database for structured application data.

Use cases:
- users
- roles
- profiles
- consent grants
- health records metadata
- family groups
- reminders
- community posts
- comments
- expert profiles
- consultations
- payments
- audit logs
- moderation state

### 5.2 Realtime Data

Firebase Realtime Database is used only for approved realtime use cases, such as:
- realtime status
- lightweight realtime synchronization
- notification state if needed

Firebase Realtime Database must not replace PostgreSQL as the main system database.

### 5.3 Object Storage

Uploaded files should be stored in object storage, not directly in PostgreSQL.

Examples:
- health record attachments
- expert credential documents
- content images
- exercise media

Metadata stays in PostgreSQL.
File binary data stays in object storage.

## 6. Integration Architecture

### 6.1 Integration Service Pattern

Each external system gets a dedicated service in `integration.{name}` package.

**Structure**:
```
integration/
├── firebase/
│   ├── FirebaseMessagingService.java
│   └── FirebaseStorageService.java
│   └── FirebaseRealtimeDatabaseService.java
├── gmail/
│   └── GmailSmtpService.java
├── compreface/
│   └── CompreFaceClient.java
├── gemini/
│   └── GeminiClient.java
├── trackasia/
│   └── TrackAsiaService.java
├── zegocloud/
│   └── ZegoCloudService.java
├── vnpay/
│   └── VNPayService.java
└── wearable/
    └── WearableIntegrationService.java
```

### 6.2 Fallback and Error Handling

| Integration | Failure Mode | Fallback Behavior |
|-------------|--------------|-------------------|
| Gemini | API timeout, quota exceeded, error response | Show conservative rule-based triage: red → emergency, yellow → consult expert, green → self-monitor |
| Firebase Messaging | Push notification send fails | Log error, retry with exponential backoff; if persistent, continue without push |
| Firebase Realtime Database | Realtime sync unavailable | Fall back to polling PostgreSQL; log warning; UI shows cached data with stale indicator |
| TrackAsia | Map service unavailable | Show static list of nearby hospitals with phone numbers; disable map view |
| VNPay | Payment gateway error | Booking remains in "unpaid" state; show user "payment failed, try again later" |
| ZegoCloud | Video call setup fails | Fallback to text chat in consultation; allow reschedule |
| Firebase Storage | File upload fails | Show error, allow retry; if critical (e.g., credential upload), allow manual review later |
| Gmail SMTP | Email send fails | Log error, retry with exponential backoff; queue email for retry; alert admin if persistent |
| CompreFace | Face recognition unavailable | Skip face verification step; fall back to manual admin verification |
| Wearables | Device data sync fails | Show last known data with timestamp; indicate sync unavailable; retry on next connection |

### 6.3 API Contract Discipline

- Define OpenAPI specs for all public APIs (mobile → backend, admin → backend)
- Version APIs: `/api/v1/...`
- Use DTOs for all request/response bodies
- Include pagination for list endpoints
- Standard error response format:
```json
{
  "timestamp": "2025-06-17T10:30:00Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Invalid input",
  "details": [...]
}
```

---

## 7. Mobile App Architecture (Flutter)

### 7.1 Structure

Feature-based organization:

```
lib/
├── main.dart
├── app/              (routing, theme, localization)
├── core/             (network, storage, auth, permissions, errors, utils)
├── shared/           (common widgets, models, services)
├── features/         (one folder per feature)
│   ├── auth/
│   ├── onboarding/
│   ├── motherJourney/
│   ├── babyCare/
│   ├── healthRecords/
│   ├── familySync/
│   ├── community/
│   ├── consultation/
│   ├── aiTriage/
│   ├── emergencyMap/
│   ├── safetyMonitoring/
│   └── pregnancyExercise/
└── integrations/     (firebase, trackasia, zego, mediapipe, imu)
```

### 7.2 State Management

- Use **Riverpod** or **Provider** for simple global state (auth, user profile)
- Use **local state** (`setState`) for UI-only state
- Keep business logic in services, not widgets

### 7.3 API Layer

```
Service → Repository → API Client (Dio or http) → Backend
```

- Repository abstracts data source (network vs. local cache)
- Service contains business logic and orchestrates multiple repositories
- Use interceptors for auth token injection and error handling

---

## 8. Frontend Architecture (React + Vite)

### 8.1 Structure

Feature-based with clear separation:

```
src/
├── main.tsx
├── App.tsx
├── app/
│   ├── router/       (route definitions, guards)
│   ├── providers/    (context providers: auth, theme, i18n)
│   ├── layouts/      (AdminLayout, AuthLayout)
│   └── guards/       (ProtectedRoute, RoleBasedRoute)
├── shared/
│   ├── api/          (axios instance, API helpers)
│   ├── auth/         (auth context, hooks)
│   ├── components/   (reusable UI: Button, Modal, Table, Form)
│   ├── forms/        (form helpers, validation schemas)
│   ├── tables/       (table components with pagination)
│   ├── charts/       (chart components)
│   ├── hooks/        (custom hooks)
│   ├── utils/        (formatting, validation helpers)
│   └── constants/    (route paths, role constants)
└── features/
    ├── userManagement/
    ├── expertVerification/
    ├── moderation/
    ├── contentManagement/
    ├── consultationManagement/
    ├── paymentRefunds/
    ├── auditSecurity/
    ├── aiRuleManagement/
    └── postureConfiguration/
```

### 8.2 State and Data Fetching

- **React Query** for server state (API data fetching, caching, mutations)
- **Zustand** for global UI state (theme, sidebar, notifications)
- **React Hook Form + Zod** for form state and validation

### 8.3 Routing

- **React Router v6** with route guards
- Admin routes: `/admin/*`
- Moderator routes: `/moderator/*`
- Content routes: `/content/*`
- Expert routes: `/expert/*`
- Partner routes: `/partner/*`
- Auth routes: `/login`, `/logout`

---

## 9. Coding Standards Summary

See `docs/bmad/coding-standards.md` for detailed conventions.

**Key Principles**:

1. **Naming**: camelCase for variables/methods, PascalCase for classes/interfaces, UPPER_SNAKE_CASE for constants
2. **Java Packages**: all lowercase, dot-separated, start with `com.carebridge.backend`
3. **File Names**: Match class name exactly (e.g., `UserService.java`)
4. **Error Handling**: Throw domain-specific exceptions; use `@ControllerAdvice` for global exception handling; return structured error JSON
5. **Logging**: Use SLF4J with Logback; log at appropriate level (DEBUG for dev, INFO for business events, WARN for recoverable issues, ERROR for failures)
6. **Transactions**: `@Transactional` on service methods that modify data
7. **Validation**: Bean Validation (`@NotNull`, `@Size`, etc.) on DTOs; custom validators for business rules
8. **Tests**: JUnit 5 + Mockito for unit tests; `@SpringBootTest` for integration tests; test names in `given_when_then` format
9. **Documentation**: Javadoc on public APIs; inline comments only for non-obvious logic
10. **Security**: Never log passwords, tokens, or sensitive PII; sanitize user input; use prepared statements (JPA handles this)

---

## 10. Deployment and Infrastructure

### 10.1 Development

- **Backend**: Spring Boot devtools with hot reload; PostgreSQL via Docker Compose
- **Frontend**: Vite dev server with HMR
- **Mobile**: Flutter hot reload
- **Environment**: Local `.env` files or `application-local.yaml` for dev config

### 10.2 Staging/Production (Pilot)

- Docker containers for backend and databases
- Reverse proxy (Nginx) for API routing
- Firebase for notifications and storage
- Cloud VM (AWS EC2 or equivalent) for backend and admin portal
- Mobile app distributed via TestFlight (iOS) and internal testing (Android)

### 10.3 CI/CD

- GitLab CI/CD pipeline (as mentioned in Report 2)
- Stages: `build`, `test`, `dockerize`, `deploy-staging`, `deploy-prod` (manual)
- Automated tests: unit, integration, security scans
- Manual approval before production deploy

---

## 11. Quality Gates

Before accepting any story as complete:

1. ✅ All acceptance criteria met
2. ✅ Unit tests written and passing (≥ 70% coverage for new code)
3. ✅ Integration tests for cross-cutting concerns (consent, audit, RBAC)
4. ✅ Manual smoke test completed
5. ✅ Code reviewed (by peer or QA agent)
6. ✅ Documentation updated (API spec if needed)
7. ✅ No security vulnerabilities (SQL injection, XSS, insecure direct object references)
8. ✅ Healthcare safety boundaries respected (no diagnosis, no prescription, disclaimers present)

---

**Document End**
