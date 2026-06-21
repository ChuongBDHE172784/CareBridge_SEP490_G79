---
epicId: EPIC-006
title: Expert Ecosystem - Profile, Verification, Booking, and Consultation
status: draft
priority: P3
estimatedStoryPoints: 55
epicOwner: TV4
relatedFRs:
  - EXPERT-001
  - EXPERT-002
  - EXPERT-003
  - EXPERT-004
  - EXPERT-005
  - 3.2.1.1
  - 3.2.1.3
  - 3.2.1.4
  - 3.3.1.52
  - 3.3.1.53
  - 3.3.1.54
  - 3.3.1.57
  - 3.3.1.58
  - 3.3.9.1
  - 3.1.2.1
  - 3.1.2.2
  - 3.1.2.7
---

# Epic: Expert Ecosystem

## 1. Epic Overview

**Epic ID:** EPIC-006
**Title:** Expert Ecosystem - Profile, Verification, Booking, and Consultation
**Status:** Draft
**Priority:** P3 (Community and Expert Ecosystem)
**Owner:** TV4 (Expert Consultation Domain)
**Sprint Target:** Sprint 0 (Foundation), Sprint 1-2 (Complete)

## 2. Business Context

CareBridge connects mothers and families with verified healthcare experts for private consultations. This epic establishes the complete expert ecosystem: expert profile creation, credential verification, availability management, expert discovery, consultation booking, payment processing, and realtime communication sessions.

The expert ecosystem is a revenue-generating feature where experts can monetize their expertise while providing quality healthcare guidance to the community.

## 3. Scope

### 3.1 In Scope

✅ **Expert Profile Management**
- Expert creates and maintains professional profile
- Bio, expertise areas, qualifications, experience years
- Profile photo and credentials upload

✅ **Expert Verification Workflow**
- Admin verification of expert credentials
- Document upload (licenses, certifications, degrees)
- Approval/rejection with reason
- Verification status display

✅ **Expert Availability Management**
- Expert sets available time slots
- Weekly schedule configuration
- Online/offline toggle

✅ **Expert Directory & Discovery**
- Users can search and filter verified experts
- View expert profiles, ratings, availability
- Filter by specialty, location, rating

✅ **Consultation Booking**
- Book private consultation with expert
- Select date/time, consultation channel (chat/voice/video)
- Payment integration (VNPay mock)
- Booking status management

✅ **Payment Processing**
- Consultation fee payment (mock VNPay)
- Payment status tracking
- Transaction history

✅ **Commission System**
- Automatic commission calculation for experts
- Commission rate configuration
- Earnings tracking

✅ **Realtime Communication**
- Session token creation for ZegoCloud (mock)
- Consultation session management
- Chat/voice/video session initiation

✅ **Consultation Lifecycle**
- Pending → Confirmed → Completed/Cancelled/No-show
- Expert can write consultation summary
- Dispute and refund handling (skeleton)

### 3.2 Out of Scope (Post-MVP)

- Real payment gateway integration (use mock in MVP)
- Real ZegoCloud integration (use mock session tokens)
- Advanced scheduling with calendar sync
- Video/voice call implementation (placeholder only)
- Advanced review and rating system (basic only)
- Expert analytics dashboard (beyond basic earnings)
- Refund dispute resolution workflow (skeleton only)
- Partner expert management (single expert type)

## 4. User Stories

This epic contains **11 user stories** covering Sprint 0 foundation:

### Sprint 0 Stories (Foundation)

1. **STORY-401**: Expert Profile and Credentials Management
2. **STORY-402**: Expert Availability Configuration
3. **STORY-403**: Expert Directory and Search
4. **STORY-404**: Consultation Booking Flow
5. **STORY-405**: Payment Processing (Mock VNPay)
6. **STORY-406**: Realtime Session Creation (Mock ZegoCloud)
7. **STORY-407**: Commission Calculation
8. **STORY-408**: Consultation Session Management
9. **STORY-409**: Admin Expert Verification Workflow
10. **STORY-410**: Expert Profile Review and Rating (Basic)
11. **STORY-411**: Consultation Status and Dispute Management

## 5. Functional Requirements Coverage

| FR ID | Requirement | Story Coverage |
|-------|-------------|----------------|
| EXPERT-001 | Expert profile: bio, credentials, expertise areas, photo | STORY-401 |
| EXPERT-002 | Admin verification workflow: submit → review → approve/reject | STORY-409 |
| EXPERT-003 | Verified badge display on expert posts and profile | STORY-401 |
| EXPERT-004 | Expert availability toggle (online/offline) | STORY-402 |
| EXPERT-005 | Community Q&A: experts can answer any post, label as "verified expert" | STORY-410 (partial) |
| 3.1.2.1 | Process Payment Transaction | STORY-405 |
| 3.1.2.2 | Calculate Commission | STORY-407 |
| 3.1.2.7 | Establish Realtime Communication Session | STORY-406 |
| 3.2.1.1 | Create Expert Profile | STORY-401 |
| 3.2.1.3 | Upload Verification Documents | STORY-401 |
| 3.2.1.4 | Configure Availability | STORY-402 |
| 3.3.1.52 | Book Private Consultation | STORY-404 |
| 3.3.1.53 | Pay Consultation Fee | STORY-405 |
| 3.3.1.54 | Join Consultation Session | STORY-408 |
| 3.3.1.57 | View Expert Directory | STORY-403 |
| 3.3.1.58 | View Expert Profile | STORY-403 |
| 3.3.9.1 | Search Expert | STORY-403 |

## 6. Domain Module Map

According to `docs/bmad/architecture.md`, the following backend packages belong to this epic:

| Package | Responsibility | Stories |
|---------|----------------|---------|
| `expert` | Expert profile, credentials, availability, reviews | 401, 402, 403, 409, 410 |
| `consultation` | Consultation booking, session, messages | 404, 408 |
| `payment` | Payment transactions, commission, disputes | 405, 407, 411 |
| `realtime` | Session token management for ZegoCloud | 406 |

## 7. Data Model Overview

### Core Entities

```java
// Expert domain
ExpertProfile (id, userId, bio, expertiseAreas[], yearsExperience, hourlyRate, avgRating, isVerified, isAvailable)
ExpertCredential (id, expertProfileId, credentialType, fileUrl, verificationStatus, verifiedBy, verifiedAt)
ExpertAvailability (id, expertProfileId, dayOfWeek, startTime, endTime, timezone, isActive)
ExpertReview (id, expertId, motherId, rating, comment, createdAt)
ExpertLocationShare (id, expertId, location, sharedUntil)

// Consultation domain
ConsultationBooking (id, consultationCode, motherId, expertId, scheduledAt, durationMinutes, channel, status, paymentStatus, consultationFee, commissionRate, notes)
ConsultationSession (id, bookingId, sessionToken, zegoRoomId, startedAt, endedAt)
ConsultationMessage (id, bookingId, senderId, messageType, content, createdAt)

// Payment domain
PaymentTransaction (id, bookingId, paymentGateway, gatewayTransactionId, amount, paymentStatus, paymentResponse)
CommissionRecord (id, expertId, bookingId, consultationFee, commissionRate, commissionAmount, settlementStatus)
ConsultationDispute (id, bookingId, raisedBy, reason, status, resolution, resolvedAt)
RefundRecord (id, bookingId, paymentTransactionId, refundAmount, reason, status)
ExpertConsultationPrice (id, expertId, consultationType, price, currency)
ConsultationPriceBand (id, name, priceRange, applicableExpertTiers)

// Realtime domain
RealtimeSession (id, bookingId, sessionToken, zegoRoomId, expiresAt)
```

## 8. External Integrations

| Integration | Purpose | Implementation | Fallback |
|-------------|---------|----------------|----------|
| VNPay | Payment processing | Mock service returning deterministic success | Mock remains for MVP |
| ZegoCloud | Realtime video/voice/chat | Mock service returning session token | Mock remains for MVP |
| Firebase Storage | Expert credential documents | Mock storage (local file path) | Local filesystem |

## 9. Success Criteria

By the end of this epic:

✅ **Expert can:**
- Create and edit professional profile
- Upload verification credentials (license, certification)
- Set weekly availability schedule
- Toggle online/offline status
- View booking requests and consultation history
- Set consultation pricing (basic)
- Receive payment (mock)
- View earnings summary

✅ **Mother/Family can:**
- Search and filter verified experts
- View expert profiles, bio, expertise, ratings
- Check expert availability
- Book consultation (date/time/channel)
- Pay consultation fee (mock)
- Join consultation session (placeholder)
- Rate expert after consultation

✅ **Admin can:**
- Review expert verification documents
- Approve or reject expert verification with reason
- View expert directory with filters
- Manage expert status (active/suspended)
- View commission reports

✅ **System:**
- Generates unique consultation codes
- Calculates commission automatically (configurable rate)
- Creates audit logs for all sensitive actions
- Enforces RBAC (expert can only edit own profile, mother can only book)
- Mock payment returns deterministic response for testing
- Mock realtime session returns valid session token
- All APIs return standardized `ApiResponse<T>` format

## 10. Dependencies

| Dependency | Epic/Story | Impact |
|------------|------------|--------|
| Authentication & Authorization | EPIC-001 (STORY-002, STORY-005) | Required - experts must be authenticated |
| Consent Framework | EPIC-001 (STORY-002) | Required - consultation data sharing needs consent |
| Audit Service | EPIC-001 (STORY-002) | Required - log profile updates, bookings, payments |
| Notification Service | EPIC-001 (STORY-002) | Optional - booking confirmations (can be added later) |
| User Management | EPIC-009 (Admin) | Required - admin verification workflow |
| File Storage | EPIC-011 (Firebase Storage) | Optional - use mock file storage initially |

## 11. Technical Considerations

### 11.1 Database Schema

All tables must be created via Flyway migrations with proper foreign keys and indexes:

```sql
--专家表专家资料表expert_profiles
CREATE TABLE expert_profiles (...);

--专家凭证表expert_credentials
CREATE TABLE expert_credentials (...);

--专家可用时间表expert_availability
CREATE TABLE expert_availability (...);

--咨询预订表consultation_bookings
CREATE TABLE consultation_bookings (...);

--咨询会话表consultation_sessions
CREATE TABLE consultation_sessions (...);

--支付交易表payment_transactions
CREATE TABLE payment_transactions (...);

--佣金记录表commission_records
CREATE TABLE commission_records (...);

--专家咨询价格表expert_consultation_prices
CREATE TABLE expert_consultation_prices (...);

--咨询价格带表consultation_price_bands
CREATE TABLE consultation_price_bands (...);
```

### 11.2 API Versioning

All endpoints must use `/api/v1/` prefix:

```
POST   /api/v1/expert/profile
GET    /api/v1/expert/profile
PUT    /api/v1/expert/profile
POST   /api/v1/expert/verification/documents
GET    /api/v1/expert/verification/documents
POST   /api/v1/expert/availability
GET    /api/v1/expert/availability
GET    /api/v1/experts
GET    /api/v1/experts/search
GET    /api/v1/experts/{id}
POST   /api/v1/consultations/book
GET    /api/v1/consultations/my-bookings
GET    /api/v1/consultations/{id}
PUT    /api/v1/consultations/{id}/cancel
POST   /api/v1/payments/process
POST   /api/v1/realtime/session/create
POST   /api/v1/commission/calculate
GET    /api/v1/commission/earnings
```

### 11.3 Security & RBAC

- `@PreAuthorize("hasRole('EXPERT')")` - expert-only endpoints
- `@PreAuthorize("hasRole('ADMIN')")` - admin verification endpoints
- `@PreAuthorize("hasRole('MOTHER')")` - booking endpoints
- `@PreAuthorize("hasAnyRole('MOTHER','EXPERT')")` - consultation view
- Ownership checks: expert can only update own profile, mother can only cancel own bookings

### 11.4 Mock Services

**MockPaymentService** (profile: dev):
```java
@Service
@Profile("dev")
public class MockPaymentService implements PaymentService {
    // Always returns SUCCESS with mock transaction ID
}
```

**MockRealtimeService** (profile: dev):
```java
@Service
@Profile("dev")
public class MockRealtimeService implements RealtimeService {
    // Returns deterministic session token
}
```

## 12. Testing Strategy

### Unit Tests (≥70% coverage)
- ExpertService: profile CRUD, availability management
- ConsultationBookingService: booking creation, conflict detection, cancellation
- PaymentService: payment processing, callback handling
- CommissionService: commission calculation formulas
- RealtimeService: session token generation

### Integration Tests
- Full booking flow: expert availability check → create booking → process payment → create session
- Admin verification: upload credentials → admin review → status change

### API Contract Tests
- All endpoints return correct `ApiResponse<T>` structure
- Error responses have proper HTTP status codes and error messages
- Validation errors return 400 with field-level error details

## 13. Implementation Phases

### Phase 1: Backend Foundation (Week 1-2)
- Database migrations (Flyway)
- Entity classes (already exist, may need adjustments)
- Repository interfaces
- DTOs (request/response)
- Mappers (MapStruct)
- Core services (ExpertService, ConsultationBookingService)
- Mock services (Payment, Realtime)
- Policies (RBAC checks)
- Controllers with proper `@PreAuthorize`
- Unit tests

### Phase 2: Mobile App (Week 2-3)
- Create `lib/features/expert/` structure
- Screens: profile, verification, availability, directory, detail, search
- API services and repositories
- UI components (ExpertCard, AvailabilitySlot, etc.)
- Connect to backend APIs

### Phase 3: Web Portal (Week 3)
- Create `src/features/expertDirectory/` structure
- Pages: directory, profile detail, search
- Components: ExpertCard, filters, reviews
- Admin: enhance `expertVerification` feature
- Connect to backend APIs

### Phase 4: Integration & Polish (Week 3-4)
- End-to-end testing
- Bug fixes
- Performance optimization
- Documentation

## 14. Definition of Done (DoD)

Each story must meet:

✅ **Code Quality**
- Follows coding standards (`docs/bmad/coding-standards.md`)
- Passes `./mvnw compile` without errors
- No security vulnerabilities (RBAC, consent, input validation)
- Proper exception handling

✅ **Testing**
- Unit tests with ≥70% coverage for services and policies
- All tests pass (`./mvnw test`)
- Integration tests for critical flows
- Manual smoke test documented

✅ **Documentation**
- API documented in OpenAPI spec
- README updated with setup instructions
- Database migration file included
- Story acceptance criteria fully met

✅ **Review**
- Code reviewed by Claude (code review agent)
- Architecture compliance verified
- Peer review from TV1 (for auth/consent integration)
- Approved and merged to `LamVH` branch

✅ **Deployment**
- Works with `docker-compose up` (PostgreSQL)
- Flyway migrations run successfully
- Application starts without errors
- Endpoints testable with curl/Postman

## 15. Risk Mitigation

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Mock payment differs from real VNPay | Medium | Medium | Document differences; plan for integration in Sprint 4 |
| ZegoCloud SDK complexity | High | Medium | Use mock session tokens for MVP; real integration deferred |
| Booking conflict race conditions | Medium | High | Use database locking or optimistic locking |
| Commission calculation errors | Low | Medium | Comprehensive unit tests for all scenarios |
| Expert profile incomplete data | Medium | Low | Validation on backend; profile completeness score |
| Mobile and web UI divergence | Medium | Medium | Share API contracts; coordinate with TV4 frontend/mobile devs |

## 16. Metrics for Success

- **Functional Coverage**: All 17 FRs covered by stories
- **Code Coverage**: ≥70% for services and policies
- **Bug Rate**: < 1 critical bug per story at acceptance
- **Demo Readiness**: All use cases demonstrable by Sprint 0 end
- **API Compliance**: 100% of endpoints return correct `ApiResponse<T>` format

---

**Epic End**
