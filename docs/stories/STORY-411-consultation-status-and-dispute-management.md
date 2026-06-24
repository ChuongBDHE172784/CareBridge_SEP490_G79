# Story: Consultation Status and Dispute Management

**Story ID**: STORY-411  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 5  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 2 - Complete

---

## User Story

**As a** mother or expert  
**I want to** report issues with consultations and request refunds  
**So that** disputes can be resolved fairly

---

## Context

This story handles consultation disputes and refund requests. Both parties can raise disputes, and admins can resolve them and approve refunds.

---

## Requirements

- **3.3.5.8**: Submit Dispute or Refund Request (from PRD)
- Dispute reasons: quality issue, no-show, technical problem
- Admin can approve/reject refund
- Status tracking

---

## API Design

### POST /api/v1/consultations/{id}/dispute
Raise dispute  
**Auth**: Required (mother or expert participant)  
**Request**: `RaiseDisputeRequest`
```json
{
  "reason": "EXPERT_NO_SHOW",
  "description": "Expert did not join scheduled time"
}
```

### GET /api/v1/admin/disputes
Admin lists all disputes  
**Auth**: Required (ADMIN)

### PUT /api/v1/admin/disputes/{id}/resolve
Admin resolves dispute  
**Request**: `ResolveDisputeRequest`
```json
{
  "resolution": "REFUND_APPROVED",
  "refundAmount": 250000.00,
  "notes": "Verified expert no-show"
}
```

---

## Files to Create

**Backend**:
- `payment/entity/ConsultationDispute.java` (exists)
- `payment/repository/ConsultationDisputeRepository.java`
- `consultation/controller/DisputeController.java`
- `consultation/service/DisputeService.java`
- `payment/controller/AdminRefundController.java`
- `payment/service/RefundService.java`
- `consultation/dto/request/RaiseDisputeRequest.java`
- `payment/dto/request/ResolveDisputeRequest.java`

---

**Story End**
