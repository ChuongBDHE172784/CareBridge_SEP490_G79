# Story: Admin Expert Verification Workflow

**Story ID**: STORY-409  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 5  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 1 - End-to-End  
**Depends On**: STORY-401

---

## User Story

**As an** admin user  
**I want to** review and verify expert credentials  
**So that** only qualified experts can offer consultation services

---

## Context

Admin workflow for verifying expert credentials. Admins can view submitted documents, approve or reject with reason, and see verification history.

---

## Requirements

- **3.2.1.3**: Upload Verification Documents (completed by expert)
- **3.2.1.2** (implicit): Admin verification
- View list of pending verifications
- View credential documents (file URLs)
- Approve or reject with rejection reason
- Expert's profile `isVerified` flag updated upon approval

---

## API Design

### GET /api/v1/admin/verification/pending
List pending verification requests  
**Auth**: Required (ADMIN role)  
**Response**: `PageResponse<VerificationRequestResponse>`

### GET /api/v1/admin/verification/{credentialId}
View credential details with document URL  
**Auth**: Required (ADMIN role)

### PUT /api/v1/admin/verification/{credentialId}/approve
Approve credential  
**Auth**: Required (ADMIN role)

### PUT /api/v1/admin/verification/{credentialId}/reject
Reject credential with reason  
**Request**: `RejectCredentialRequest`
```json
{
  "reason": "Document expired"
}
```

---

## Acceptance Criteria

### Scenario: Admin sees pending verifications
Given admin authenticated  
When GET `/api/v1/admin/verification/pending`  
Then returns list with expert name, credential type, submitted date

### Scenario: Admin approves credential
Given pending credential  
When PUT `/api/v1/admin/verification/{id}/approve`  
Then credential.verificationStatus = APPROVED  
And expertProfile.isVerified = true (if all credentials approved)  
And audit log created

### Scenario: Admin rejects with reason
When PUT `/api/v1/admin/verification/{id}/reject` with reason  
Then status = REJECTED  
And rejectionReason saved  
And expert notified (future)

---

## Files to Create

**Backend**:
- `expert/controller/AdminVerificationController.java`
- `expert/service/AdminVerificationService.java`
- `expert/repository/ExpertCredentialRepository.java` (add query methods)

**Frontend**: `src/features/expertVerification/pages/ExpertVerificationPage.tsx` (already exists, enhance)

---

**Story End**
