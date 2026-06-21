# Story: Consultation Session Management

**Story ID**: STORY-408  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 5  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** mother or expert  
**I want to** view consultation details and join the session  
**So that** we can conduct the consultation via chat/voice/video

---

## Context

This story manages consultation session lifecycle - viewing consultation details, starting session, and completing consultation.

---

## Requirements

- **3.3.1.54**: Join Consultation Session
- View consultation details (scheduled time, expert info, channel)
- Get realtime session token
- Mark consultation as completed
- Expert can write summary after completion

---

## API Design

### GET /api/v1/consultations/{id}
Get consultation details  
**Auth**: Required (mother or expert participant)  
**Response**: `ConsultationDetailResponse` (includes expert profile, payment status, session info)

### POST /api/v1/consultations/{id}/start
Mother or expert marks consultation started  
**Auth**: Required (participant)  
**Response**: `ApiResponse<ConsultationSessionResponse>`

### POST /api/v1/consultations/{id}/complete
Mark consultation complete  
**Auth**: Required (participant)  
**Request**: `CompleteConsultationRequest` (optional summary)
```json
{
  "summary": "Discussed feeding schedule..."
}
```

### POST /api/v1/consultations/{id}/no-show
Mark mother no-show (expert only)

---

## Files to Create

**Backend**:
- `consultation/controller/ConsultationSessionController.java`
- `consultation/service/ConsultationSessionService.java`
- `consultation/entity/ConsultationSession.java` (exists)
- `consultation/repository/ConsultationSessionRepository.java`
- `consultation/dto/response/ConsultationDetailResponse.java`
- `consultation/dto/request/CompleteConsultationRequest.java`

**Mobile**: Enhance `lib/features/consultation/screens/consultation_session_screen.dart`  
**Frontend**: `src/features/consultationManagement/pages/ConsultationDetailPage.tsx`

---

**Story End**
