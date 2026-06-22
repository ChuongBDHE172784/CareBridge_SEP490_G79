# Story: Commission Calculation

**Story ID**: STORY-407  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 3  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** system  
**I want to** automatically calculate expert commission when payment is received  
**So that** expert earnings are tracked correctly for settlement

---

## Context

When a consultation payment is successfully processed, the system calculates the expert's commission based on configured rate and creates a commission record.

---

## Requirements

- **3.1.2.2**: Calculate Commission
- Triggered after payment success
- Formula: `commissionAmount = consultationFee * commissionRate / 100`
- Default commission rate configurable (e.g., 70% to expert, 30% to platform)
- Create `commission_records` entry

---

## API Design

### POST /api/v1/commission/calculate
Calculate commission for a completed payment  
**Auth**: Required (system/admin)  
**Request**: `CalculateCommissionRequest`
```json
{
  "bookingId": "uuid"
}
```
**Response**: `ApiResponse<CommissionResponse>`

### GET /api/v1/commission/earnings
Get expert's earnings summary  
**Auth**: Required (EXPERT, sees own only)  
**Query**: `?startDate=&endDate=`

---

## Business Rules

1. Commission rate defaults to 70% (configurable per expert tier)
2. Calculated on consultation fee (before platform deduction)
3. Status: PENDING → SETTLED → PAID_OUT
4. Settlement happens monthly (manual trigger for MVP)

---

## Acceptance Criteria

### Scenario: Commission calculated after payment
Given booking with consultationFee = 250000, commissionRate = 70%  
When payment marked as PAID  
Then commissionAmount = 175000  
And commission record created with status PENDING

### Scenario: Expert views own earnings
Given authenticated expert  
When GET `/api/v1/commission/earnings`  
Then returns sum of PENDING + SETTLED commissions

---

## Files to Create

**Backend**:
- `payment/service/CommissionService.java`
- `payment/repository/CommissionRecordRepository.java`
- `payment/entity/CommissionRecord.java` (exists)
- `payment/dto/response/CommissionResponse.java`
- `payment/mapper/CommissionRecordMapper.java`

**Integration**: `ConsultationBookingService` calls `CommissionService` after payment success

---

**Story End**
