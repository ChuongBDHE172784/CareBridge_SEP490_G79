# Story: Expert Availability Configuration

**Story ID**: STORY-402  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 5  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As an** expert user  
**I want to** set and manage my weekly availability schedule  
**So that** mothers can book consultations during my available time slots

---

## Context

This story allows experts to configure their recurring weekly availability. Experts can set time blocks for each day of the week that they are available for consultations.

---

## Requirements

- **3.2.1.4**: Configure Availability
- Backend: `expert/availability` or `expert/` package
- API endpoints for CRUD operations on availability slots
- Day-of-week (1-7), start/end time, timezone, active flag
- Check for time conflicts when saving
- Only EXPERT role can access own availability

---

## API Design

### POST /api/v1/expert/availability
Create availability slot  
**Request**: `AvailabilitySlotRequest`
```json
{
  "dayOfWeek": 2,
  "startTime": "09:00:00",
  "endTime": "17:00:00",
  "timezone": "Asia/Hanoi",
  "isActive": true
}
```

### GET /api/v1/expert/availability
Get all my availability slots

### PUT /api/v1/expert/availability/{id}
Update specific slot

### DELETE /api/v1/expert/availability/{id}
Remove availability slot

---

## Acceptance Criteria

### Scenario: Expert creates availability slot
Given authenticated expert  
When POST `/api/v1/expert/availability` with valid data  
Then status 201, slot created with correct fields

### Scenario: Time conflict detection
Given expert has slot Mon 9-12  
When creating slot Mon 10-11  
Then response 400 "Time slot conflicts with existing availability"

### Scenario: Only own slots can be modified
Given expert A  
When DELETE `/api/v1/expert/availability/{slot-of-expert-B}`  
Then status 403 Forbidden

---

## Files to Create

**Backend**:
- `expert/service/AvailabilityService.java`
- `expert/controller/AvailabilityController.java`
- `expert/entity/ExpertAvailability.java` (exists, verify)
- `expert/repository/ExpertAvailabilityRepository.java`
- `expert/dto/request/AvailabilitySlotRequest.java`
- `expert/dto/response/AvailabilitySlotResponse.java`
- `expert/mapper/AvailabilityMapper.java`
- `expert/policy/AvailabilityPolicy.java`

**Mobile**: `lib/features/expert/screens/availability_config_screen.dart`  
**Frontend**: `src/features/expertDirectory/components/AvailabilityCalendar.tsx`

---

**Story End**
