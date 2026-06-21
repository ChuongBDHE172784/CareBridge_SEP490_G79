# Story: Expert Profile Review and Rating (Basic)

**Story ID**: STORY-410  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 3  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 2 - Complete

---

## User Story

**As a** mother user  
**I want to** rate and review experts after consultation  
**So that** other mothers can make informed decisions

---

## Context

Basic rating system - mothers can leave star rating (1-5) and text review after completed consultation. Reviews are displayed on expert profile.

---

## Requirements

- **EXPERT-005** (partial): Community Q&A labeling not covered, but review system needed
- One review per mother per consultation
- Cannot review before consultation completed
- Average rating calculated on `ExpertProfile.avgRating`

---

## API Design

### POST /api/v1/reviews
Create review  
**Auth**: Required (MOTHER role)  
**Request**: `CreateReviewRequest`
```json
{
  "expertId": "uuid",
  "bookingId": "uuid",
  "rating": 5,
  "comment": "Great consultation, very helpful advice"
}
```

### GET /api/v1/experts/{expertId}/reviews
List reviews for expert  
**Response**: `PageResponse<ReviewResponse>`

---

## Files to Create

**Backend**:
- `expert/entity/ExpertReview.java` (exists, verify)
- `expert/repository/ExpertReviewRepository.java`
- `expert/controller/ReviewController.java`
- `expert/service/ReviewService.java`
- `expert/dto/request/CreateReviewRequest.java`
- `expert/dto/response/ReviewResponse.java`
- `expert/mapper/ExpertReviewMapper.java`

---

**Story End**
