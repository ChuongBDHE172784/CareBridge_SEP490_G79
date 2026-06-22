# TV4 Sprint 0 Development Plan - Expert Consultation Domain

**Owner:** TV4 (Expert Consultation Domain)
**Sprint:** Sprint 0 - Foundation And Module Skeletons
**Duration:** 2 weeks
**Source:** `docs/bmad/function-spec-task-allocation.md`

---

## 1. Domain Overview

TV4 owns the **Expert Consultation** domain, which includes:

- **Expert Profile Management** - Expert registration, verification, profile editing, availability
- **Expert Discovery** - Directory, search, filtering for mothers to find experts
- **Consultation Booking** - Scheduling, payment integration, session lifecycle
- **Realtime Communication** - Chat, voice, video consultation sessions
- **Payment & Commission** - Transaction processing, commission calculation, pricing
- **Revenue Tracking** - Expert earnings and platform commission views

**External Integrations:**
- Firebase Storage (verification documents, profile images)
- VNPay (payment gateway - mock first)
- ZegoCloud (realtime audio/video - mock first)

**Primary Actors:**
- Verified Expert (mobile + web portal)
- Mother (mobile app - booking consumer)
- System Admin (expert verification, pricing management)
- Content Admin (exercise content - related)

---

## 2. Sprint 0 Scope - 12 Use Cases

### 2.1 Use Case List

| SRS ID | Use Case | Primary Actor | Complexity | External Dependencies |
|--------|----------|---------------|------------|----------------------|
| 3.2.1.1 | Create Expert Profile | Verified Expert | Medium | Firebase Storage |
| 3.2.1.3 | Upload Verification Documents | Verified Expert | Medium | Firebase Storage |
| 3.2.1.4 | Configure Availability | Verified Expert | Medium | None |
| 3.3.1.52 | Book Private Consultation | Mother | Medium | None (mock payment) |
| 3.3.1.53 | Pay Consultation Fee | Mother | Medium | VNPay (mock) |
| 3.3.1.54 | Join Consultation Session | Mother / Expert | Hard | ZegoCloud (mock) |
| 3.3.1.57 | View Expert Directory | Mother | Medium | None |
| 3.3.1.58 | View Expert Profile | Mother | Medium | None |
| 3.3.9.1 | Search Expert | Mother | Medium | None |
| 3.1.2.1 | Process Payment Transaction | System | Hard | VNPay (mock) |
| 3.1.2.2 | Calculate Commission | System | Medium | None |
| 3.1.2.7 | Establish Realtime Communication Session | System | Hard | ZegoCloud (mock) |

**Total:** 12 Use Cases (11 unique + 1 shared supporting service)

### 2.2 Feature Modules to Create

**Backend Packages:**
```
com.carebridge.backend.
├── expert/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/request/
│   ├── dto/response/
│   ├── mapper/
│   └── policy/
├── verification/
├── availability/
├── consultation/
├── booking/
├── payment/
├── commission/
├── pricing/
├── realtime/
├── refund/ (skeleton)
├── dispute/ (skeleton)
└── review/ (skeleton)
```

**Mobile Feature Folders:**
```
lib/features/
├── expert/
│   ├── profile/
│   ├── verification/
│   ├── availability/
│   ├── directory/
│   └── consultation/
└── consultation/
    ├── booking/
    ├── payment/
    └── session/
```

**Web Portal (Admin + Expert Portal):**
```
src/features/
├── expertManagement/
│   ├── expertProfile/
│   ├── expertVerification/
│   └── expertDashboard/
├── consultationManagement/
│   ├── booking/
│   ├── session/
│   └── summary/
└── paymentManagement/
    ├── transactions/
    └── commission/
```

---

## 3. BMAD Workflow for Sprint 0

### PHASE 1: ANALYZE & RESEARCH (Days 1-2)

**Goal:** Understand requirements, dependencies, and existing architecture

#### Tasks:

1. **Read and Analyze Use Cases**
   - Read all 12 Use Case specifications from Report 3
   - Extract: preconditions, postconditions, normal flow, alternative flows, exceptions
   - Identify business rules: BR-RBAC, BR-CONSULTATION, BR-SAFETY, BR-PRIVACY
   - Note frequency of use and priority levels

2. **Study Existing Domain Model**
   - Review PRD (`docs/bmad/prd.md`) - Expert Consultation section
   - Review Architecture (`docs/bmad/architecture.md`) - Integration patterns, database structure
   - Check ERD (`02_Design/Database/CareBridge_ERD.drawio`) for relevant tables
   - Identify entities needed: `expert_profiles`, `verification_documents`, `availability_slots`, `consultations`, `payments`, `commissions`, etc.

3. **Dependencies Analysis**
   - **TV1 Contracts Required:**
     * Authentication (JWT token, user/role info)
     * Authorization (RBAC roles: EXPERT, MOTHER, ADMIN)
     * Consent management (for health data sharing in consultation)
     * Audit event contract (log consultation actions)
     * Notification event contract (consultation invites, reminders)
     * API response/error format
   - **Shared Infrastructure:**
     * Database tables (may need migrations)
     * File storage (Firebase Storage integration)
     * JWT security configuration

4. **API Contract Planning**
   - Draft OpenAPI/Swagger endpoints for expert domain
   - Coordinate with TV1 to ensure auth guard integration
   - Define request/response DTO structures
   - Document error codes and messages

**Deliverables:**
- [ ] Use Case analysis notes (markdown file)
- [ ] Domain entity relationship diagram (drawio or text)
- [ ] API endpoint list (OpenAPI YAML or markdown table)
- [ ] Dependencies checklist (what needs from TV1)
- [ ] Technical question log (to ask in standup/chat)

---

### PHASE 2: DESIGN (Days 2-4)

**Goal:** Design database schema, API contracts, and integration interfaces

#### Tasks:

1. **Database Schema Design**
   - Create Flyway migration files for TV4 domain tables:
     * `expert_profiles`
     * `verification_documents`
     * `availability_slots`
     * `consultations`
     * `consultation_sessions`
     * `consultation_summaries`
     * `pricing_tiers`
     * `payments`
     * `commissions`
     * `reviews`
   - Define foreign keys to shared tables (`users`, `roles`, `baby_profiles`, `mother_journeys`)
   - Add indexes for performance (expert_id, user_id, status, dates)
   - Document each table with comments

2. **Entity Class Design**
   - Create JPA entities with Lombok annotations
   - Define relationships: `@OneToMany`, `@ManyToOne`, `@JoinColumn`
   - Add validation constraints: `@NotNull`, `@Size`, `@DecimalMin/Max`
   - Use `@CreationTimestamp`, `@UpdateTimestamp`
   - Example:
   ```java
   @Entity
   @Table(name = "expert_profiles")
   @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
   public class ExpertProfile {
       @Id @GeneratedValue(strategy = GenerationType.UUID)
       private UUID expertProfileId;
       
       @OneToOne @JoinColumn(name = "user_id", unique = true)
       private User user;
       
       @Column(name = "specialties_json")
       private String specialtiesJson; // JSON array
       
       @Column(name = "years_experience")
       private Integer yearsExperience;
       
       @Column(name = "bio", length = 1000)
       private String bio;
       
       @Column(name = "hourly_rate")
       private BigDecimal hourlyRate;
       
       @Enumerated(EnumType.STRING)
       private VerificationStatus verificationStatus;
       
       @CreationTimestamp
       private Instant createdAt;
       
       @UpdateTimestamp
       private Instant updatedAt;
   }
   ```

3. **API Design**
   - Define REST endpoints following CRUD pattern:
     * `GET /api/v1/experts` - directory with filters
     * `GET /api/v1/experts/{id}` - expert profile detail
     * `POST /api/v1/experts` - create expert profile (during onboarding)
     * `PUT /api/v1/experts/{id}` - update profile
     * `POST /api/v1/experts/{id}/verification-documents` - upload credentials
     * `GET /api/v1/experts/{id}/availability` - view availability
     * `PUT /api/v1/experts/availability` - configure availability
     * `POST /api/v1/consultations/book` - book consultation
     * `GET /api/v1/consultations/my` - list user's consultations
     * `POST /api/v1/consultations/{id}/pay` - process payment
     * `POST /api/v1/consultations/{id}/join` - join session
     * `POST /api/v1/consultations/{id}/complete` - complete consultation
     * `POST /api/v1/consultations/{id}/summary` - write summary
   - Use `@PreAuthorize` for role checks
   - Define response wrapper: `ApiResponse<T>`
   - Define pagination: `Pageable`, `PageResponse<T>`

4. **Integration Service Design**
   - **FirebaseStorageService**: upload/download verification documents, profile images
     * Method: `String uploadFile(MultipartFile file, String subPath)`
     * Return: public URL
   - **VNPayService** (mock):
     * Method: `PaymentResult processPayment(PaymentRequest request)`
     * Return: `PaymentResult` with status, transactionId, redirectUrl
     * Mock: return SUCCESS with dummy transactionId
   - **ZegoCloudService** (mock):
     * Method: `RealtimeSession createSession(Long consultationId, List<String> participantUserIds)`
     * Return: `RealtimeSession` with sessionId, token, server
     * Mock: return dummy session token

5. **Business Logic Design**
   - **ConsultationBookingPolicy**:
     * Check expert availability (no double booking)
     * Check consultation lead time (min 1 hour, max 30 days)
     * Validate pricing tier matches selected service
   - **PaymentPolicy**:
     * Verify consultation is in BOOKED status
     * Ensure amount matches expected price
     * Create Payment record with PENDING status
     * Call VNPay integration (mock returns success)
     * Update consultation to PAID or PAYMENT_FAILED
   - **CommissionPolicy**:
     * Calculate commission: `amount * commissionRate`
     * Commission rate based on expert tier or admin config
   - **ConsultationSessionPolicy**:
     * Only participants can join
     * Session must be in SCHEDULED/STARTED status
     * Generate realtime session credentials via ZegoCloud (mock)

**Deliverables:**
- [ ] Database migration SQL file: `V20260619_1200__expert_consultation_tables.sql`
- [ ] Entity classes (at least skeleton): `ExpertProfile.java`, `Consultation.java`, `Payment.java`, etc.
- [ ] OpenAPI spec draft: `expert-consultation-api.yaml`
- [ ] Integration service interfaces: `FirebaseStorageService.java`, `VNPayService.java`, `ZegoCloudService.java`
- [ ] Policy classes (interfaces): `ConsultationBookingPolicy.java`, `PaymentPolicy.java`
- [ ] DTO classes: `CreateExpertProfileRequest.java`, `BookConsultationRequest.java`, `ConsultationResponse.java`

---

### PHASE 3: IMPLEMENT (Days 4-10)

**Goal:** Build skeleton CRUD APIs with mock integrations

#### Week 1 (Days 4-7): Backend Core

**Day 4: Database & Entities**
- Create Flyway migration
- Run migration on local dev DB
- Create entity classes
- Create repository interfaces extending `JpaRepository`

**Day 5: Controllers & DTOs**
- Create controller classes with endpoint mappings
- Implement DTOs (request/response)
- Add validation annotations (`@Valid`, `@NotNull`, etc.)
- Configure Jackson serialization (exclude lazy fields, format dates)

**Day 6: Services & Business Logic**
- Implement service interfaces and skeletons
- Wire up `@Service` classes with `@Transactional`
- Implement basic CRUD operations
- Add policy checks (stub or basic validation)

**Day 7: Mock Integrations**
- Implement `VNPayService` mock: return SUCCESS always
- Implement `ZegoCloudService` mock: return dummy session token
- Implement `FirebaseStorageService` mock: return fake URL or local file path
- Write unit tests for services (mock repositories)

#### Week 2 (Days 8-10): UI Skeleton + Integration

**Day 8: Mobile Feature Folders (Flutter)**
- Create folder structure in `04_SourceCode/MobileApp/lib/features/expert/`
- Create dummy screens (placeholder widgets):
  * `ExpertProfileScreen.dart`
  * `ExpertVerificationScreen.dart`
  * `ExpertDirectoryScreen.dart`
  * `BookConsultationScreen.dart`
  * `ConsultationListScreen.dart`
- Add routing to `app/router.dart`
- Use mock data services (return hardcoded JSON)

**Day 9: Web Portal (React + Vite)**
- Create feature folders in `04_SourceCode/Frontend/src/features/expertManagement/`
- Create dummy pages (functional components with placeholder UI):
  * `ExpertProfilePage.tsx`
  * `ExpertVerificationQueuePage.tsx`
  * `ConsultationManagementPage.tsx`
  * `PaymentTransactionsPage.tsx`
- Add routes to `AppRouter.tsx`
- Use mock API services (return sample data from JSON files)

**Day 10: End-to-End Wiring**
- Connect mobile/web UI to backend APIs using HTTP client (Dio for Flutter, Axios for React)
- Implement error handling (show snackbar/toast on error)
- Add auth token injection (use TV1 shared auth client)
- Test basic flow: create expert → view directory → book consultation → mock payment

**Deliverables:**
- [ ] Backend: All 12 endpoints return 200/4xx/5xx with proper JSON
- [ ] Database: Tables created, can insert sample data
- [ ] Mobile: At least 3 dummy screens navigate correctly and call APIs
- [ ] Web: At least 3 dummy pages render and fetch data
- [ ] Integration mocks working without real credentials
- [ ] Unit tests: Service layer tests (≥ 50% coverage for new code)
- [ ] API tested with Postman/curl collection

---

### PHASE 4: TEST & VERIFY (Days 11-12)

**Goal:** Ensure skeleton is functional and demo-able

#### Tasks:

1. **Manual Smoke Test**
   - Run backend: `./mvnw spring-boot:run`
   - Run frontend: `npm run dev`
   - Run mobile: `flutter run`
   - Test each endpoint with Postman
   - Test UI navigation flows

2. **Integration Points Check**
   - Confirm TV1 auth guard works (unauthenticated → 401)
   - Confirm role-based access (MOTHER can book, EXPERT can view own profile)
   - Confirm mock integrations return expected format
   - Confirm audit events are logged (check `audit_logs` table)

3. **Cross-Domain Dependencies**
   - Check with TV2: Consultation references baby/mother profiles → ensure foreign keys work
   - Check with TV1: Notification event shape matches contract
   - Check with TV3: Community Q&A may reference expert → ensure expert profile readable

4. **Security Quick Check**
   - Ensure no passwords/tokens logged
   - Ensure endpoints validate `@PreAuthorize`
   - Ensure file upload paths are sanitized (mock still)

**Deliverables:**
- [ ] Smoke test checklist completed
- [ ] Bug list (if any) with priorities
- [ ] Fix critical blockers before Sprint Review

---

### PHASE 5: DOCUMENT & REVIEW (Days 13-14)

**Goal:** Prepare documentation and Sprint 0 review

#### Tasks:

1. **API Documentation**
   - Update OpenAPI spec with final endpoint definitions
   - Add examples for request/response
   - Document error codes
   - Export to HTML or host on internal server

2. **Code Documentation**
   - Add Javadoc to all public classes and methods
   - Document integration service configuration (where to put credentials)
   - Document database schema (ERD update if needed)
   - Create `README.md` in `backend/src/main/java/com/carebridge/backend/expert/` explaining package structure

3. **Deployment Documentation**
   - Update `docs/bmad/architecture.md` if needed (domain boundaries)
   - Add environment variables needed:
     * `VNPAY_TXN_URL`, `VNPAY_SECURE_SECRET` (mock: any value)
     * `ZEGOCLOUD_APP_ID`, `ZEGOCLOUD_SERVER_SECRET` (mock: any value)
     * `FIREBASE_STORAGE_BUCKET`
   - Document Flyway migration order

4. **Sprint 0 Review Preparation**
   - Prepare 5-minute demo showing:
     * Create expert profile (backend + mobile)
     * Upload verification document (mock Firebase)
     * Configure availability
     * View expert directory (mother app)
     * Book consultation and mock payment
     * Join consultation session (mock ZegoCloud)
   - Record video demo (optional)
   - Prepare slides or demo script

5. **Handoff to Sprint 1**
   - List completed tasks vs. Sprint 0 plan
   - List incomplete tasks and reasons
   - List known issues/technical debt
   - List dependencies for TV1-TV5 to continue

**Deliverables:**
- [ ] `API_SPEC.md` in `docs/bmad/task-allocation/`
- [ ] `TV4_DOMAIN_README.md` in backend expert package
- [ ] Environment variable checklist
- [ ] Sprint Review demo (live or recorded)
- [ ] Retrospective notes (what went well, blockers, improvements)

---

## 4. Dependencies and Coordination

### 4.1 Must-Have from TV1 (Before Starting)

| Need | Description | Impact if Missing |
|------|-------------|-------------------|
| Authentication API | `/api/v1/auth/verify-otp`, `/api/v1/auth/refresh` | Cannot secure endpoints or identify users |
| JWT Token Injection | Shared auth client/library | Cannot include auth header in API calls |
| Role Constants | `ROLE_EXPERT`, `ROLE_MOTHER`, `ROLE_ADMIN` | Cannot implement `@PreAuthorize` |
| API Response Format | `ApiResponse<T>` class and error format | Cannot conform to response standards |
| Audit Event Interface | `AuditService.log(...)` method | Cannot audit consultation actions |

### 4.2 Nice-to-Have from TV1

- Notification event publisher (`NotificationService.publish(...)`)
- Consent check policy (`ConsentCheckPolicy.ensureConsent(...)`)
- Shared exception types (`ResourceNotFoundException`, `AccessDeniedException`)

### 4.3 Coordination with Other TVs

- **TV2**: Consultation references mother journey and baby profiles → need entity IDs from TV2
- **TV3**: Community Q&A may show "verified expert" badge → need expert profile fields (name, specialty) readable by TV3
- **TV5**: AI triage may escalate to consultation → need booking API ready for TV5 to call

---

## 5. Acceptance Criteria for Sprint 0

### Backend

- [ ] All 12 endpoints implemented and return valid JSON
- [ ] Database tables created via Flyway migration
- [ ] Authentication required on all endpoints (except public directory/expert profile GET)
- [ ] Role-based authorization works (EXPERT can only access own consultation, ADMIN can view all)
- [ ] Mock integrations compile and return deterministic data
- [ ] Unit tests exist for service layer business logic (≥ 50% coverage)
- [ ] Integration tests for at least 3 critical flows:
  * Create expert profile → upload verification → configure availability
  * Mother books consultation → mock payment succeeds
  * Expert and mother join consultation session

### Mobile App

- [ ] Expert can:
  * Create/update profile (forms call backend)
  * Upload verification document (file picker → upload)
  * Set availability status
  * View consultation list and join session screen
- [ ] Mother can:
  * Search/browse expert directory
  * View expert profile detail
  * Book consultation (select time, confirm)
  * See consultation list with status

### Web Portal (Admin + Expert Portal)

- [ ] Admin can:
  * View expert verification queue
  * Approve/reject expert verification
  * View consultation list (all or filtered)
  * View payment transactions
- [ ] Expert can:
  * View expert dashboard (profile, stats)
  * Manage availability (web UI)

### Demo Readiness

- [ ] System starts without errors (backend, frontend, mobile)
- [ ] Can demonstrate end-to-end flow from create expert to join consultation
- [ ] Mock data is realistic (not all "test test test")
- [ ] API responses follow agreed format
- [ ] Error scenarios handled gracefully (e.g., book non-existent expert → 404)

---

## 6. BMAD Development Checklist

### Before Coding

- [ ] Read all 12 Use Cases from Report 3
- [ ] Review ERD and identify needed tables/columns
- [ ] Draft API endpoint list and share with TV1 for review
- [ ] Create Flyway migration file with table definitions
- [ ] Set up local dev environment (DB, IDE, etc.)

### During Development

- [ ] Write one feature at a time (small PRs)
- [ ] Write unit tests alongside service code
- [ ] Run `mvn test` frequently
- [ ] Update API spec after each endpoint change
- [ ] Document decisions in `DECISIONS.md` if diverging from architecture

### Before Sprint End

- [ ] All endpoints tested with Postman
- [ ] Mobile/web UI calls real backend (not only mock data)
- [ ] Demo script prepared and practiced
- [ ] Code reviewed by teammate or QA agent
- [ ] Merge to `develop` branch with clear PR description referencing SRS IDs

---

## 7. Task Breakdown by Day (Optional)

| Day | Backend | Mobile | Web | Tests/Docs |
|-----|---------|--------|-----|------------|
| 1-2 | Analyze, design DB schema | - | - | Analysis docs |
| 3 | Entities, repositories | - | - | Entity tests |
| 4 | Controllers, DTOs, services | Scaffold folders | Scaffold folders | API spec draft |
| 5 | Mock integrations | Profile screens | Expert profile pages | Service unit tests |
| 6 | Business logic | Verification flow | Verification queue | Integration tests |
| 7 | Availability, directory | Directory screen | Dashboard | - |
| 8 | Booking, payment | Booking flow | Booking management | - |
| 9 | Session, summary | Consultation list | Consultation list | - |
| 10 | Final wiring | Session join | Payment view | End-to-end test |
| 11-12 | - | UI polish | UI polish | Smoke test |
| 13-14 | - | - | - | Docs, review, demo |

---

## 8. Notes

1. **Mock First**: Implement all external integrations as mocks. Do NOT spend time on real VNPay/ZegoCloud integration in Sprint 0.
2. **Skeleton Over Polish**: UI can be simple (basic widgets, no fancy styling). Focus on API → UI wiring.
3. **Follow Coding Standards**: See `docs/bmad/coding-standards.md`
4. **Use TV1 Contracts**: Do NOT create your own auth/audit/notification mechanisms. Use TV1's services.
5. **PR Discipline**: Each PR should be small, with clear SRS ID reference. Example: `feat(expert): add create expert profile endpoint (3.2.1.1)`
6. **Ask for Help**: If blocked by TV1 dependencies, ask in team chat immediately.

---

**Good luck! Build a solid foundation for Expert Consultation domain.**
