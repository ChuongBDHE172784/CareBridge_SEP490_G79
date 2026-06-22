# TV4 Sprint 0 Implementation Plan - Expert Consultation Foundation

**Owner:** TV4 (Expert Consultation Domain)  
**Sprint:** Sprint 0 - Foundation (2 weeks)  
**Epic:** EPIC-006-expert-ecosystem.md  
**Stories:** STORY-401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411  
**Date:** 2026-06-20  
**Status:** DRAFT - AWAITING APPROVAL

---

## 1. Overview

TV4 owns Expert Consultation domain. Sprint 0 goal: create skeleton APIs and UI foundations for expert profile, verification, availability, directory, booking, payment (mock), realtime (mock), commission, and admin verification.

**Key Principle:** Mock-first for external integrations (VNPay, ZegoCloud, Firebase Storage).

---

## 2. Stories to Implement (Priority Order)

| Story | SRS ID | Use Case | Complexity | Dependencies |
|-------|--------|----------|------------|--------------|
| STORY-401 | 3.2.1.1, 3.2.1.3 | Expert Profile & Credentials | Medium | STORY-002 (auth, audit) |
| STORY-402 | 3.2.1.4 | Expert Availability Config | Medium | STORY-401 |
| STORY-403 | 3.3.1.57, 3.3.1.58, 3.3.9.1 | Expert Directory & Search | Medium | STORY-401, 402 |
| STORY-404 | 3.3.1.52 | Consultation Booking Flow | Medium | STORY-401, 402, 403 |
| STORY-405 | 3.3.1.53, 3.1.2.1 | Payment Processing (Mock) | Medium | STORY-404 |
| STORY-406 | 3.1.2.7 | Realtime Session Creation (Mock) | Hard | STORY-404, 405 |
| STORY-407 | 3.1.2.2 | Commission Calculation | Medium | STORY-405 |
| STORY-408 | 3.3.1.54 | Consultation Session Management | Medium | STORY-406 |
| STORY-409 | (admin) | Admin Expert Verification | Medium | STORY-401 |
| STORY-410 | EXPERT-005 | Expert Review & Rating | Low | STORY-408 |
| STORY-411 | dispute | Dispute & Refund Management | Medium | STORY-405, 408 |

**Recommended implementation order:** 401 → 402 → 403 → 404 → 405 → 406 → 407 → 408 → 409 → 410 → 411

---

## 3. Backend Package Structure

```
04_SourceCode/Backend/src/main/java/com/carebridge/backend/
├── expert/                    (STORY-401, 402, 403, 409, 410)
│   ├── controller/
│   │   ├── ExpertProfileController.java
│   │   ├── ExpertPublicController.java
│   │   ├── AvailabilityController.java
│   │   ├── ExpertSearchController.java
│   │   └── AdminVerificationController.java
│   ├── service/
│   │   ├── ExpertService.java, ExpertServiceImpl.java
│   │   ├── AvailabilityService.java, AvailabilityServiceImpl.java
│   │   ├── AdminVerificationService.java
│   │   └── ReviewService.java, ReviewServiceImpl.java
│   ├── repository/
│   │   ├── ExpertProfileRepository.java
│   │   ├── ExpertCredentialRepository.java
│   │   ├── ExpertAvailabilityRepository.java
│   │   ├── ExpertReviewRepository.java
│   │   └── ExpertSearchRepository.java (custom queries)
│   ├── entity/
│   │   ├── ExpertProfile.java          (exists, verify)
│   │   ├── ExpertCredential.java       (exists, verify)
│   │   ├── ExpertAvailability.java     (exists, verify)
│   │   ├── ExpertReview.java           (exists, verify)
│   │   └── ExpertLocationShare.java   (exists, unused for now)
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateExpertProfileRequest.java
│   │   │   ├── UpdateExpertProfileRequest.java
│   │   │   ├── AvailabilitySlotRequest.java
│   │   │   ├── UploadCredentialRequest.java
│   │   │   ├── SearchExpertsRequest.java
│   │   │   ├── CreateReviewRequest.java
│   │   │   └── RejectCredentialRequest.java
│   │   └── response/
│   │       ├── ExpertProfileResponse.java
│   │       ├── ExpertProfilePublicResponse.java
│   │       ├── ExpertSummaryResponse.java
│   │       ├── AvailabilitySlotResponse.java
│   │       ├── ExpertCredentialResponse.java
│   │       ├── ReviewResponse.java
│   │       ├── VerificationRequestResponse.java
│   │       └── PageResponse<...> (generic)
│   ├── mapper/
│   │   ├── ExpertProfileMapper.java
│   │   ├── ExpertCredentialMapper.java
│   │   ├── AvailabilityMapper.java
│   │   └── ExpertReviewMapper.java
│   └── policy/
│       ├── ExpertProfilePolicy.java
│       ├── AvailabilityPolicy.java
│       └── ReviewPolicy.java
├── consultation/                (STORY-404, 408)
│   ├── controller/
│   │   ├── ConsultationBookingController.java
│   │   ├── ConsultationSessionController.java
│   │   └── DisputeController.java
│   ├── service/
│   │   ├── ConsultationBookingService.java, ConsultationBookingServiceImpl.java
│   │   ├── ConsultationSessionService.java, ConsultationSessionServiceImpl.java
│   │   └── DisputeService.java, DisputeServiceImpl.java
│   ├── repository/
│   │   ├── ConsultationBookingRepository.java
│   │   ├── ConsultationSessionRepository.java
│   │   └── ConsultationMessageRepository.java
│   ├── entity/
│   │   ├── ConsultationBooking.java      (exists, verify)
│   │   ├── ConsultationSession.java      (exists, verify)
│   │   └── ConsultationMessage.java      (exists, may adjust)
│   ├── dto/
│   │   ├── request/
│   │   │   ├── BookConsultationRequest.java
│   │   │   ├── CancelBookingRequest.java
│   │   │   ├── ConfirmBookingRequest.java
│   │   │   ├── CompleteConsultationRequest.java
│   │   │   ├── RaiseDisputeRequest.java
│   │   └── response/
│   │       ├── ConsultationBookingResponse.java
│   │       ├── ConsultationDetailResponse.java
│   │       ├── ConsultationSummaryResponse.java
│   │       └── ConsultationSessionResponse.java
│   ├── mapper/
│   │   ├── ConsultationBookingMapper.java
│   │   ├── ConsultationSessionMapper.java
│   │   └── ConsultationMessageMapper.java
│   └── policy/
│       ├── ConsultationBookingPolicy.java
│       ├── ConsultationAccessPolicy.java
│       └── DisputePolicy.java
├── payment/                    (STORY-405, 407, 411)
│   ├── controller/
│   │   ├── PaymentController.java
│   │   ├── CommissionController.java
│   │   └── AdminRefundController.java
│   ├── service/
│   │   ├── PaymentService.java (interface)
│   │   ├── MockPaymentService.java
│   │   ├── CommissionService.java, CommissionServiceImpl.java
│   │   ├── RefundService.java, RefundServiceImpl.java
│   ├── repository/
│   │   ├── PaymentTransactionRepository.java
│   │   ├── CommissionRecordRepository.java
│   │   ├── RefundRecordRepository.java
│   │   └── ConsultationDisputeRepository.java
│   ├── entity/
│   │   ├── PaymentTransaction.java       (exists)
│   │   ├── CommissionRecord.java         (exists)
│   │   ├── RefundRecord.java             (exists)
│   │   ├── ConsultationDispute.java      (exists)
│   │   ├── ConsultationPriceBand.java    (exists, unused for now)
│   │   └── ExpertConsultationPrice.java  (exists, unused for now)
│   ├── dto/
│   │   ├── request/
│   │   │   ├── ProcessPaymentRequest.java
│   │   │   ├── CalculateCommissionRequest.java
│   │   │   ├── RequestRefundRequest.java
│   │   │   └── ResolveDisputeRequest.java
│   │   └── response/
│   │       ├── PaymentResponse.java
│   │       ├── CommissionResponse.java
│   │       ├── RefundResponse.java
│   │       └── DisputeResponse.java
│   ├── mapper/
│   │   ├── PaymentTransactionMapper.java
│   │   ├── CommissionRecordMapper.java
│   │   ├── RefundRecordMapper.java
│   │   └── ConsultationDisputeMapper.java
│   └── policy/
│       ├── PaymentPolicy.java
│       ├── RefundPolicy.java
│       └── DisputePolicy.java
└── realtime/                   (STORY-406, 408)
    ├── controller/
    │   └── RealtimeController.java
    ├── service/
    │   ├── RealtimeService.java (interface)
    │   └── MockRealtimeService.java
    ├── repository/
    │   └── RealtimeSessionRepository.java
    ├── entity/
    │   └── RealtimeSession.java
    │       ├── id (UUID)
    │       ├── bookingId (UUID, unique)
    │       ├── sessionToken (String)
    │       ├── zegoRoomId (String)
    │       ├── startedAt (Instant)
    │       ├── endedAt (Instant)
    │       ├── createdAt (Instant)
    │       └── expiresAt (Instant)
    ├── dto/
    │   ├── request/
    │   │   └── CreateSessionRequest.java
    │   └── response/
    │       └── SessionTokenResponse.java
    └── mapper/
        └── RealtimeSessionMapper.java
```

---

## 4. Database Migrations

**File:** `src/main/resources/db/migration/V20260620_1200__tv4_expert_consultation_tables.sql`

```sql
-- =====================================================
-- TV4: Expert Consultation Domain Tables
-- Created: 2026-06-20
-- =====================================================

-- Expert Profiles
CREATE TABLE IF NOT EXISTS expert_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    expertise_areas TEXT[] NOT NULL,
    years_experience INTEGER CHECK (years_experience >= 0),
    qualifications TEXT,
    hourly_rate DECIMAL(10,2) CHECK (hourly_rate >= 0),
    avg_rating DECIMAL(3,2) DEFAULT 0.0,
    total_reviews INTEGER DEFAULT 0,
    is_verified BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expert_profiles_user_id ON expert_profiles(user_id);
CREATE INDEX idx_expert_profiles_is_verified ON expert_profiles(is_verified);
CREATE INDEX idx_expert_profiles_is_available ON expert_profiles(is_available);

-- Expert Credentials (Verification Documents)
CREATE TABLE IF NOT EXISTS expert_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_profile_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
    credential_type VARCHAR(100) NOT NULL,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    issue_date DATE,
    expiry_date DATE,
    issuing_authority VARCHAR(255),
    verification_status VARCHAR(50) DEFAULT 'PENDING',
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMP,
    rejection_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expert_credentials_profile_id ON expert_credentials(expert_profile_id);
CREATE INDEX idx_expert_credentials_status ON expert_credentials(verification_status);

-- Expert Availability Slots
CREATE TABLE IF NOT EXISTS expert_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_profile_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    timezone VARCHAR(50) DEFAULT 'Asia/Hanoi',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(expert_profile_id, day_of_week, start_time, end_time)
);

CREATE INDEX idx_expert_availability_profile_id ON expert_availability(expert_profile_id);
CREATE INDEX idx_expert_availability_active ON expert_availability(is_active);

-- Consultation Bookings
CREATE TABLE IF NOT EXISTS consultation_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_code VARCHAR(50) UNIQUE NOT NULL,
    mother_id UUID NOT NULL REFERENCES users(id),
    expert_id UUID NOT NULL REFERENCES expert_profiles(id),
    scheduled_at TIMESTAMP NOT NULL,
    duration_minutes INTEGER DEFAULT 30,
    channel VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    payment_status VARCHAR(50) DEFAULT 'UNPAID',
    consultation_fee DECIMAL(10,2) NOT NULL,
    commission_rate DECIMAL(5,2),
    commission_amount DECIMAL(10,2),
    expert_earnings DECIMAL(10,2),
    reason TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_bookings_mother_id ON consultation_bookings(mother_id);
CREATE INDEX idx_consultation_bookings_expert_id ON consultation_bookings(expert_id);
CREATE INDEX idx_consultation_bookings_status ON consultation_bookings(status);
CREATE INDEX idx_consultation_bookings_scheduled_at ON consultation_bookings(scheduled_at);
CREATE INDEX idx_consultation_bookings_payment_status ON consultation_bookings(payment_status);

-- Consultation Sessions
CREATE TABLE IF NOT EXISTS consultation_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID UNIQUE NOT NULL REFERENCES consultation_bookings(id),
    session_token VARCHAR(500) NOT NULL,
    zego_room_id VARCHAR(100),
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_sessions_booking_id ON consultation_sessions(booking_id);

-- Expert Reviews
CREATE TABLE IF NOT EXISTS expert_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expert_id UUID NOT NULL REFERENCES expert_profiles(id),
    mother_id UUID NOT NULL REFERENCES users(id),
    booking_id UUID NOT NULL REFERENCES consultation_bookings(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(expert_id, mother_id, booking_id)
);

CREATE INDEX idx_expert_reviews_expert_id ON expert_reviews(expert_id);
CREATE INDEX idx_expert_reviews_booking_id ON expert_reviews(booking_id);

-- Consultation Disputes
CREATE TABLE IF NOT EXISTS consultation_disputes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL UNIQUE REFERENCES consultation_bookings(id),
    raised_by UUID NOT NULL REFERENCES users(id),
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    resolution TEXT,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultation_disputes_booking_id ON consultation_disputes(booking_id);
CREATE INDEX idx_consultation_disputes_status ON consultation_disputes(status);
```

---

## 5. API Contract Summary

All endpoints under `/api/v1/` prefix.

### Expert Profile APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/expert/profile` | EXPERT | Create expert profile |
| GET | `/expert/profile` | EXPERT | Get own profile |
| PUT | `/expert/profile` | EXPERT | Update own profile |
| GET | `/expert/profile/{expertId}` | Optional | View expert profile (public) |
| POST | `/expert/verification/documents` | EXPERT | Upload credential |
| GET | `/expert/verification/documents` | EXPERT | List my credentials |

### Expert Availability APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/expert/availability` | EXPERT | Get my slots |
| POST | `/expert/availability` | EXPERT | Create slot |
| PUT | `/expert/availability/{id}` | EXPERT | Update slot |
| DELETE | `/expert/availability/{id}` | EXPERT | Delete slot |

### Expert Directory & Search APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/experts` | Optional | List verified experts (with filters) |
| GET | `/experts/search` | Optional | Search by keyword |
| GET | `/experts/{expertId}` | Optional | View expert detail |
| GET | `/experts/{expertId}/reviews` | Optional | View expert reviews |

### Consultation Booking APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/consultations/book` | MOTHER | Book consultation |
| GET | `/consultations/my-bookings` | MOTHER/EXPERT | List my bookings |
| GET | `/consultations/{id}` | MOTHER/EXPERT | Get booking detail |
| PUT | `/consultations/{id}/cancel` | MOTHER | Cancel booking |
| PUT | `/consultations/{id}/confirm` | EXPERT | Confirm booking |

### Payment APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/payments/process` | MOTHER | Process payment |
| POST | `/payments/callback/vnpay` | None | VNPay webhook (mock) |
| GET | `/payments/transactions/{bookingId}` | MOTHER/EXPERT | Get transaction |

### Commission APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/commission/calculate` | SYSTEM | Calculate commission |
| GET | `/commission/earnings` | EXPERT | Get my earnings |

### Realtime APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/realtime/session/create` | MOTHER/EXPERT | Create session token |
| GET | `/realtime/session/{bookingId}` | MOTHER/EXPERT | Get session info |

### Admin APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/admin/verification/pending` | ADMIN | List pending verifications |
| PUT | `/admin/verification/{id}/approve` | ADMIN | Approve credential |
| PUT | `/admin/verification/{id}/reject` | ADMIN | Reject credential |
| GET | `/admin/disputes` | ADMIN | List all disputes |
| PUT | `/admin/disputes/{id}/resolve` | ADMIN | Resolve dispute |

### Review APIs
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/reviews` | MOTHER | Create review (after consultation) |
| GET | `/experts/{expertId}/reviews` | Optional | List expert reviews |

---

## 6. Implementation Rules

### Code Rules
1. **Layered Architecture**: Controller → Service → Repository
2. **DTO Validation**: Use `@Valid`, `@NotNull`, `@Size`, `@Email`, `@Pattern`
3. **RBAC**: `@PreAuthorize("hasRole('EXPERT')")` etc.
4. **Ownership Check**: Verify user can only access own data
5. **Response Format**: Always return `ApiResponse<T>` from `common.response`
6. **Audit**: Call `auditService.logAction()` after state changes
7. **Mock Services**: `@Profile("dev")` for VNPay, ZegoCloud, Firebase

### Git Workflow
```bash
git checkout LamVH
git checkout -b feature/tv4/story-401-expert-profile
# implement, test, commit
git commit -m "feat(tv4): 3.2.1.1 - create expert profile endpoint (STORY-401)"
git push origin feature/tv4/story-401-expert-profile
# Create PR, request review
```

---

## 7. Step-by-Step Implementation Plan

### Phase 1: Backend Foundation (Days 1-5)

**Day 1-2: Database & Core Entities**
- Create/verify Flyway migration `V20260620_1200__tv4_expert_consultation_tables.sql`
- Run migration: `./mvnw flyway:migrate`
- Review existing entities (`expert/`, `consultation/`, `payment/`, `realtime/`) - adjust if needed
- Create missing entities: `RealtimeSession`
- Compile: `./mvnw clean compile`

**Day 3-4: Repositories & Basic Queries**
- Create repository interfaces for all entities
- Add custom query methods for search/filter
- Write repository unit tests (if needed)

**Day 5: DTOs, Mappers, Policies**
- Create all request/response DTOs with validation annotations
- Create MapStruct mappers (or manual mapping)
- Create policy classes for RBAC checks
- Write unit tests for mappers and policies

### Phase 2: Service Layer (Days 6-8)

**Day 6: Expert Services**
- Implement `ExpertServiceImpl` (profile CRUD)
- Implement `AvailabilityServiceImpl` (slot management)
- Unit tests for ExpertService (Mockito)

**Day 7: Consultation & Payment Services**
- Implement `ConsultationBookingServiceImpl` (booking with conflict check)
- Implement `MockPaymentService`
- Unit tests for booking service

**Day 8: Commission, Realtime, Review Services**
- Implement `CommissionServiceImpl`
- Implement `MockRealtimeService`
- Implement `ReviewServiceImpl`
- Unit tests for all

### Phase 3: Controllers & Integration (Days 9-10)

**Day 9: Expert & Consultation Controllers**
- Implement `ExpertProfileController`, `AvailabilityController`, `ExpertPublicController`
- Implement `ConsultationBookingController`, `ConsultationSessionController`
- Annotate with `@PreAuthorize`, `@Valid`
- Test endpoints with Postman

**Day 10: Payment, Commission, Admin Controllers**
- Implement `PaymentController`, `CommissionController`, `AdminVerificationController`, `DisputeController`, `RealtimeController`
- Wire mock services
- End-to-end API testing

### Phase 4: Mobile App (Days 11-12)

**Day 11: Expert Feature Folders**
- Create `lib/features/expert/` structure
- Screens: `expert_profile_screen.dart`, `expert_verification_screen.dart`, `availability_config_screen.dart`
- Services: `expert_api_service.dart` (Dio client)
- Models: Dart data classes matching backend DTOs
- Connect to backend APIs (use real endpoints, not mock)

**Day 12: Directory & Consultation Mobile**
- Screens: `expert_directory_screen.dart`, `expert_detail_screen.dart`, `expert_search_screen.dart`
- Enhance `lib/features/consultation/` with booking and payment screens
- Test on device/emulator

### Phase 5: Web Portal (Days 13-14)

**Day 13: Expert Management Pages**
- Create `src/features/expertDirectory/` (for mother UI)
- Pages: `ExpertDirectoryPage.tsx`, `ExpertProfilePage.tsx`, `ExpertSearchPage.tsx`
- Services: `expert_api_service.ts` (Axios)
- Models: TypeScript interfaces

**Day 14: Admin & Final Integration**
- Enhance existing `expertVerification` feature pages
- Create admin dispute management page
- Connect all mobile/web UI to real backend APIs
- Final smoke test and bug fixes

---

## 8. Testing Requirements

### Unit Tests (JUnit 5 + Mockito)
- `ExpertServiceTests`: create, update, get profile, duplicate check
- `AvailabilityServiceTests`: slot CRUD, conflict detection
- `ConsultationBookingServiceTests`: book consultation, conflict detection, cancellation
- `PaymentServiceTests`: mock payment success, callback handling
- `CommissionServiceTests`: commission calculation formulas
- `RealtimeServiceTests`: session token generation
- `ReviewServiceTests`: create review, rating validation

**Coverage Target:** ≥70% for services and policies

### Integration Tests
- Booking flow: create expert → set availability → mother books → mock payment → create session
- Admin verification: upload doc → admin approves → expert becomes verified

### Manual Smoke Test
- Test all 11 stories with Postman/curl
- Test mobile app flows
- Test web portal flows

---

## 9. Acceptance Checklist

Before marking any story complete:
- [ ] Backend endpoint implemented and tested
- [ ] Unit tests pass, coverage ≥70%
- [ ] API returns correct `ApiResponse<T>` format
- [ ] RBAC enforced with `@PreAuthorize`
- [ ] Audit logs created for sensitive actions
- [ ] Mobile/web UI connects to real backend (not only mock data)
- [ ] Story acceptance criteria fully met
- [ ] Code reviewed by teammate or Claude
- [ ] Documentation updated (API spec, README if needed)

---

## 10. References

- `docs/bmad/architecture.md` - Domain module map, layered architecture
- `docs/bmad/coding-standards.md` - Java, Flutter, React conventions
- `docs/bmad/function-spec-task-allocation.md` - Use case details (TV4 section)
- `docs/stories/EPIC-006-expert-ecosystem.md` - Epic overview
- `docs/stories/STORY-401*` through `STORY-411` - Individual story specs
- `docs/bmad/function-spec-task-allocation.md` - Sprint 0 tasks
- `_bmad-output/planning-artifacts/implementation-readiness-report-2026-06-19.md` - Review findings
- `02_Design/Architecture/project-structure-design.md` - Backend structure
- `STORY-002-backend-shared-domain-scaffold.md` - Shared contracts reference

---

## 11. Approval Required

This implementation plan must be approved by:
1. **Product Owner/PM** - Verify scope aligns with PRD
2. **Architect** - Verify architecture compliance
3. **TV1 (Shared Foundation)** - Verify contracts usage (auth, audit, response format)

**After approval, TV4 can begin implementation following this plan.**

---

**Plan End**
