# Story: Expert Directory and Search

**Story ID**: STORY-403  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 8  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As a** mother or family member  
**I want to** search and browse verified experts  
**So that** I can find the right expert for my healthcare questions and book consultation

---

## Context

This story implements expert discovery - users can search, filter, and view expert profiles. Only verified experts are shown in the directory.

---

## Requirements

- **3.3.1.57**: View Expert Directory
- **3.3.1.58**: View Expert Profile
- **3.3.9.1**: Search Expert
- Support filters: expertise area, rating, availability, location
- Show expert summary: name, bio snippet, expertise[], avg rating, hourly rate, isAvailable

---

## API Design

### GET /api/v1/experts
List experts with optional filters  
**Query params**:
- `expertiseAreas` (comma-separated)
- `minRating` (decimal)
- `isAvailable` (boolean)
- `location` (string, city/region)
- `page`, `size`, `sort`

**Response**: `PageResponse<ExpertSummaryResponse>`

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": "uuid",
        "userId": "uuid",
        "displayName": "Dr. Nguyen Van A",
        "bio": "Experienced...",
        "expertiseAreas": ["PEDIATRICS"],
        "hourlyRate": 250000.00,
        "avgRating": 4.8,
        "totalReviews": 45,
        "isVerified": true,
        "isAvailable": true
      }
    ],
    "totalElements": 150,
    "totalPages": 15,
    "pageNumber": 0,
    "pageSize": 20
  }
}
```

### GET /api/v1/experts/search
Full-text search by name, bio, expertise  
**Query params**: `q` (keyword), `page`, `size`

### GET /api/v1/experts/{expertId}
View expert profile details  
**Response**: `ExpertProfilePublicResponse` (more detailed)

### GET /api/v1/experts/{expertId}/reviews
View expert reviews (basic stub for Sprint 0)

---

## Acceptance Criteria

### Scenario: Mother views expert directory
Given authenticated user with role "MOTHER"  
When GET `/api/v1/experts`  
Then status 200 with list of verified experts  
And each expert shows id, displayName, expertiseAreas, avgRating, hourlyRate

### Scenario: Filter experts by expertise area
When GET `/api/v1/experts?expertiseAreas=PEDIATRICS`  
Then only experts with PEDIATRICS in expertiseAreas returned

### Scenario: Filter by minimum rating
When GET `/api/v1/experts?minRating=4.5`  
Then only experts with avgRating >= 4.5 returned

### Scenario: Search experts by keyword
When GET `/api/v1/experts/search?q=pediatric`  
Then experts matching keyword in name/bio/expertise returned

### Scenario: View expert profile detail
Given expert ID  
When GET `/api/v1/experts/{expertId}`  
Then status 200 with full profile: bio, qualifications, credentials (public fields), availability status

### Scenario: Pagination works
When GET `/api/v1/experts?page=1&size=10`  
Then returns 10 experts with correct pagination metadata

---

## Files to Create

**Backend**:
- `expert/repository/ExpertRepository.java` (custom search methods)
- `expert/controller/ExpertPublicController.java`
- `expert/controller/ExpertSearchController.java`
- `expert/dto/response/ExpertSummaryResponse.java`
- `expert/dto/response/ExpertDirectoryResponse.java`
- `expert/mapper/ExpertSummaryMapper.java`
- `expert/policy/ExpertProfilePolicy.java` (view policy)

**Existing entities**: `ExpertProfile`, `ExpertCredential`, `ExpertAvailability`

**Mobile**: `lib/features/expert/screens/expert_directory_screen.dart`, `expert_detail_screen.dart`, `expert_search_screen.dart`  
**Frontend**: `src/features/expertDirectory/pages/ExpertDirectoryPage.tsx`, `ExpertProfilePage.tsx`

---

**Story End**
