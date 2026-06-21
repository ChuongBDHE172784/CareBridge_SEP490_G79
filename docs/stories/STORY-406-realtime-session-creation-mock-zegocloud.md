# Story: Realtime Session Creation (Mock ZegoCloud)

**Story ID**: STORY-406  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 5  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** mother or expert  
**I want to** get a session token to join a realtime consultation  
**So that** I can connect with the other party via chat/voice/video

---

## Context

This story creates mock realtime session tokens for consultation sessions. In production, this would integrate with ZegoCloud to create rooms and tokens. For MVP, we return deterministic mock tokens.

---

## Requirements

- **3.1.2.7**: Establish Realtime Communication Session
- **3.3.1.54**: Join Consultation Session
- Mock service returns session token and room ID
- Session token expires after consultation duration + buffer

---

## API Design

### POST /api/v1/realtime/session/create
Create session for confirmed booking  
**Auth**: Required (MOTHER or EXPERT role, must be booking participant)  
**Request**: `CreateSessionRequest`
```json
{
  "bookingId": "uuid",
  "channel": "VIDEO"
}
```
**Response**: `ApiResponse<SessionTokenResponse>`
```json
{
  "success": true,
  "data": {
    "sessionToken": "mock-session-token-abc123",
    "zegoRoomId": "room-booking-uuid",
    "expiresAt": "2026-06-20T15:00:00"
  }
}
```

### GET /api/v1/realtime/session/{bookingId}
Get active session info  
**Auth**: Required (booking participants only)

---

## Files to Create

**Backend**:
- `realtime/entity/RealtimeSession.java`
- `realtime/repository/RealtimeSessionRepository.java`
- `realtime/service/RealtimeService.java` (interface)
- `realtime/service/MockRealtimeService.java`
- `realtime/controller/RealtimeController.java`
- `realtime/dto/request/CreateSessionRequest.java`
- `realtime/dto/response/SessionTokenResponse.java`
- `realtime/mapper/RealtimeSessionMapper.java`

**Mobile**: `lib/features/consultation/screens/consultation_session_screen.dart` (will use token)  
**Frontend**: `src/features/consultationManagement/pages/ConsultationSessionPage.tsx`

---

**Story End**
