# Story: Consultation Booking Flow

**Story ID**: STORY-404  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 8  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** mother user  
**I want to** book a private consultation with an expert  
**So that** I can get personalized healthcare guidance for my concerns

---

## Context

This story implements the consultation booking flow. Mothers can select an expert, choose an available time slot, specify consultation channel (chat/voice/video), and create a booking. The system must:
- Validate expert exists and is verified
- Check time slot is available
- Create consultation booking with unique code
- Trigger payment requirement (but payment happens separately)

---

## Requirements

- **3.3.1.52**: Book Private Consultation
- **3.2.1.4**: Configure Availability (dependency - need expert availability check)
- Booking includes: expertId, scheduledAt, durationMinutes, channel (CHAT/VOICE/VIDEO), notes/reason

---

## API Design

### POST /api/v1/consultations/book
Create consultation booking  
**Auth**: Required (MOTHER role)  
**Request**: `BookConsultationRequest`
```json
{
  "expertId": "uuid",
  "scheduledAt": "2026-06-25T14:00:00",
  "durationMinutes": 30,
  "channel": "VIDEO",
  "notes": "I have questions about my baby's feeding schedule"
}
```
**Response**: `ApiResponse<ConsultationBookingResponse>`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "consultationCode": "CB-EXP-20260620-0001",
    "motherId": "uuid",
    "expertId": "uuid",
    "scheduledAt": "2026-06-25T14:00:00",
    "durationMinutes": 30,
    "channel": "VIDEO",
    "status": "PENDING",
    "paymentStatus": "UNPAID",
    "consultationFee": 250000.00,
    "notes": "...",
    "createdAt": "2026-06-20T..."
  }
}
```

### GET /api/v1/consultations/my-bookings
Get current user's bookings (mother's own bookings or expert's incoming bookings)  
**Auth**: Required (MOTHER or EXPERT)  
**Query**: `?status=PENDING&status=CONFIRMED`

### GET /api/v1/consultations/{id}
Get booking details  
**Auth**: Required (mother who booked OR expert OR admin)  
**Ownership check required**

### PUT /api/v1/consultations/{id}/cancel
Cancel booking  
**Auth**: Required (mother who booked OR admin)  
**Request**: `CancelBookingRequest` with reason

### PUT /api/v1/consultations/{id}/confirm
Expert confirms booking  
**Auth**: Required (EXPERT role, must be this expert's booking)

---

## Business Rules

1. **Booking Conflict Detection**: Mother cannot book same expert with overlapping time
2. **Advance Booking**: Must book at least 2 hours in advance, at most 30 days ahead
3. **Duration**: Standard durations: 15, 30, 60 minutes (based on expert's pricing)
4. **Status Flow**:
   - PENDING → CONFIRMED (expert confirms)
   - PENDING → CANCELLED (mother cancels or expert rejects)
   - CONFIRMED → COMPLETED (after consultation)
   - CONFIRMED → NO_SHOW (if mother doesn't join within 15 min)
5. **Payment**: Booking creates with `paymentStatus = UNPAID`. Mother must pay before consultation (handled in STORY-405)

---

## Acceptance Criteria

### Scenario: Mother books consultation successfully
Given authenticated mother user  
And expert with ID "expert-1" has availability for today 14:00-14:30  
When POST `/api/v1/consultations/book` with valid data  
Then status 201  
And response.data.consultationCode format matches "CB-EXP-YYYYMMDD-NNNN"  
And status = "PENDING"  
And paymentStatus = "UNPAID"  
And consultationFee > 0 (from expert's pricing)

### Scenario: Booking with overlapping time fails
Given mother has existing booking with expert-1 at 14:00-14:30  
When booking same expert at 14:15-14:45  
Then status 400  
And message contains "time slot is no longer available"

### Scenario: Cannot book unverified expert
Given expert with isVerified = false  
When mother attempts to book  
Then status 400 "Expert is not yet verified"

### Scenario: Only mother can book (not expert booking themselves)
Given authenticated expert user  
When POST `/api/v1/consultations/book`  
Then status 403

### Scenario: Cancellation within 2 hours requires admin approval
Given booking scheduled in 1 hour from now  
When mother cancels  
Then status 200 but cancellationStatus = "PENDING_ADMIN_APPROVAL"  
(Advanced rule - can be simplified for MVP)

---

## Files to Create

**Backend**:
- `consultation/controller/ConsultationBookingController.java`
- `consultation/service/ConsultationBookingService.java`
- `consultation/repository/ConsultationBookingRepository.java`
- `consultation/entity/ConsultationBooking.java` (exists, may need adjustments)
- `consultation/dto/request/BookConsultationRequest.java`
- `consultation/dto/response/ConsultationBookingResponse.java`
- `consultation/dto/response/ConsultationSummaryResponse.java`
- `consultation/mapper/ConsultationBookingMapper.java`
- `consultation/policy/ConsultationBookingPolicy.java`
- `expert/repository/ExpertProfileRepository.java` (add availability check method)

**Mobile**: `lib/features/consultation/screens/booking_form_screen.dart`  
**Frontend**: `src/features/consultationManagement/pages/BookingFormPage.tsx` (enhance)

---

**Story End**
