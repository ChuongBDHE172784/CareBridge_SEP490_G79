# Story: Expert Profile and Credentials Management

**Story ID**: STORY-401  
**Epic**: EPIC-006-expert-ecosystem.md  
**Status**: DRAFT  
**Priority**: P3  
**Story Points**: 8  
**Owner**: TV4 (Expert Consultation)  
**Sprint**: Sprint 0 - Foundation  

---

## User Story

**As an** expert user  
**I want to** create and manage my professional profile including bio, expertise areas, qualifications, and verification credentials  
**So that** I can establish my credibility and be discoverable by mothers seeking consultation

---

## Context

This story establishes the expert profile domain with full CRUD operations. Experts must be able to:
- Create their profile after registration
- Upload verification documents (licenses, certifications)
- Set their expertise areas and hourly rate
- Maintain profile information over time

The backend must provide secure endpoints that enforce ownership (experts can only edit their own profile) and integrate with the admin verification workflow (STORY-409).

---

## Requirements

### Functional Requirements

- **EXPERT-001**: Expert profile: bio, credentials, expertise areas, photo
- **3.2.1.1**: Create Expert Profile
- **3.2.1.3**: Upload Verification Documents

### Technical Requirements

- Follow layered architecture: `controller → service → repository → entity`
- Use DTOs for all request/response payloads with validation
- Apply RBAC: `@PreAuthorize("hasRole('EXPERT')")` on all endpoints
- Ownership check: expert can only access own profile
- Call `AuditService` for profile changes and document uploads
- Return standardized `ApiResponse<T>` format
- Mock file storage for credentials (store file path in database)

---

## Database Design

**Tables to create** (if not exist from STORY-002):
- `expert_profiles` - main profile table
- `expert_credentials` - verification documents

**Flyway migration**: `V20260620_1200__tv4_expert_profile_tables.sql`

### expert_profiles table:
```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
bio TEXT,
expertise_areas TEXT[] NOT NULL,
years_experience INTEGER CHECK (years_experience >= 0),
qualifications TEXT,
hourly_rate DECIMAL(10,2) CHECK (hourly_rate >= 0),
avg_rating DECIMAL(3,2) DEFAULT 0.0,
total_reviews INTEGER DEFAULT 0,
is_verified BOOLEAN DEFAULT FALSE,
is_available BOOLEAN DEFAULT TRUE,
created_at TIMESTAMP NOT NULL DEFAULT NOW(),
updated_at TIMESTAMP NOT NULL DEFAULT NOW()
```

### expert_credentials table:
```sql
id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
expert_profile_id UUID NOT NULL REFERENCES expert_profiles(id) ON DELETE CASCADE,
credential_type VARCHAR(100) NOT NULL, -- LICENSE, CERTIFICATION, DEGREE, IDENTITY
file_url VARCHAR(500) NOT NULL,
file_name VARCHAR(255) NOT NULL,
issue_date DATE,
expiry_date DATE,
issuing_authority VARCHAR(255),
verification_status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
verified_by UUID REFERENCES users(id),
verified_at TIMESTAMP,
rejection_reason TEXT,
created_at TIMESTAMP NOT NULL DEFAULT NOW()
```

---

## API Design

### POST /api/v1/expert/profile
**Description**: Create expert profile for authenticated expert  
**Auth**: Required (EXPERT role)  
**Request**: `CreateExpertProfileRequest`
```json
{
  "bio": "Experienced pediatrician with 10 years in neonatal care",
  "expertiseAreas": ["PEDIATRICS", "NEONATOLOGY", "MATERNAL_HEALTH"],
  "yearsExperience": 10,
  "qualifications": "MD, PhD in Pediatrics - Hanoi Medical University",
  "hourlyRate": 250000.00
}
```
**Response**: `ApiResponse<ExpertProfileResponse>`
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "userId": "uuid",
    "bio": "...",
    "expertiseAreas": [...],
    "yearsExperience": 10,
    "hourlyRate": 250000.00,
    "isVerified": false,
    "isAvailable": true,
    "createdAt": "2026-06-20T..."
  }
}
```
**Errors**:
- `400`: Validation failed, profile already exists
- `401`: Unauthorized
- `403`: Not an expert

### GET /api/v1/expert/profile
**Description**: Get own expert profile  
**Auth**: Required (EXPERT role)  
**Response**: `ApiResponse<ExpertProfileResponse>`

### PUT /api/v1/expert/profile
**Description**: Update own expert profile  
**Auth**: Required (EXPERT role)  
**Request**: `UpdateExpertProfileRequest` (same fields as create, all optional)  
**Response**: `ApiResponse<ExpertProfileResponse>`

### GET /api/v1/expert/profile/{expertId}
**Description**: View expert profile (public - can be viewed by any authenticated user)  
**Auth**: Optional (guests can view limited info)  
**Response**: `ApiResponse<ExpertProfilePublicResponse>`

### POST /api/v1/expert/verification/documents
**Description**: Upload verification credential document  
**Auth**: Required (EXPERT role)  
**Request**: `MultipartFile` + metadata
```json
{
  "credentialType": "LICENSE",
  "issueDate": "2015-06-01",
  "expiryDate": "2027-06-01",
  "issuingAuthority": "Hanoi Medical Association"
}
```
**Response**: `ApiResponse<ExpertCredentialResponse>`

### GET /api/v1/expert/verification/documents
**Description**: List my verification credentials  
**Auth**: Required (EXPERT role)  
**Response**: `ApiResponse<List<ExpertCredentialResponse>>`

---

## Acceptance Criteria

### Scenario 1: Expert creates profile successfully
**Given** an authenticated user with role "EXPERT"  
**And** the user does not have an expert profile yet  
**When** they POST `/api/v1/expert/profile` with valid data  
**Then** response status is `201 Created`  
**And** `response.success` is `true`  
**And** `response.data.id` contains the profile UUID  
**And** `response.data.userId` matches the authenticated user's ID  
**And** `response.data.isVerified` is `false`  
**And** `AuditService.logAction()` was called with action `EXPERT_PROFILE_CREATED`

### Scenario 2: Duplicate profile creation fails
**Given** an authenticated expert who already has a profile  
**When** they POST `/api/v1/expert/profile`  
**Then** response status is `400 Bad Request`  
**And** `response.message` contains "Expert profile already exists"

### Scenario 3: Expert uploads verification document
**Given** an authenticated expert with existing profile  
**When** they POST `/api/v1/expert/verification/documents` with multipart file and metadata  
**Then** response status is `201 Created`  
**And** `response.data.credentialType` matches request  
**And** `response.data.verificationStatus` is `"PENDING"`  
**And** `response.data.fileUrl` contains stored file path  
**And** audit log created with action `EXPERT_CREDENTIAL_UPLOADED`

### Scenario 4: Non-expert cannot access expert endpoints
**Given** an authenticated user with role "MOTHER"  
**When** they POST `/api/v1/expert/profile`  
**Then** response status is `403 Forbidden`

### Scenario 5: Unauthenticated user cannot access expert endpoints
**Given** an unauthenticated user  
**When** they GET `/api/v1/expert/profile`  
**Then** response status is `401 Unauthorized`

### Scenario 6: Expert can only view own profile by ID
**Given** an authenticated expert with ID `expert-1`  
**When** they GET `/api/v1/expert/profile/expert-2` (different expert)  
**Then** response status is `200 OK` (public profile view allowed)  
**But** if they try to PUT `/api/v1/expert/profile/expert-2`  
**Then** response status is `403 Forbidden` (ownership check)

### Scenario 7: Validation errors for invalid request
**Given** an authenticated expert  
**When** they POST `/api/v1/expert/profile` with empty `expertiseAreas`  
**Then** response status is `400 Bad Request`  
**And** `response.errors` contains validation messages for required fields

---

## Files to Create/Modify

### Backend (Java Spring Boot)

**Package: `expert/`**
```
src/main/java/com/carebridge/backend/expert/
├── controller/
│   ├── ExpertProfileController.java
│   ├── ExpertPublicController.java
│   └── VerificationController.java
├── service/
│   ├── ExpertService.java (interface)
│   └── ExpertServiceImpl.java
├── repository/
│   ├── ExpertProfileRepository.java
│   ├── ExpertProfileJpaRepository.java (extends JpaRepository<ExpertProfile, UUID>)
│   └── ExpertCredentialRepository.java
├── entity/
│   ├── ExpertProfile.java (exists - may need adjustments)
│   ├── ExpertCredential.java (exists - may need adjustments)
│   └── ExpertReview.java (exists - for future)
├── dto/
│   ├── request/
│   │   ├── CreateExpertProfileRequest.java
│   │   ├── UpdateExpertProfileRequest.java
│   │   ├── UploadCredentialRequest.java
│   │   └── ExpertProfileForm.java (shared validation)
│   └── response/
│       ├── ExpertProfileResponse.java
│       ├── ExpertProfilePublicResponse.java
│       └── ExpertCredentialResponse.java
├── mapper/
│   ├── ExpertProfileMapper.java
│   └── ExpertCredentialMapper.java
└── policy/
    └── ExpertProfilePolicy.java
```

**Entity Adjustments** (if needed):
- `ExpertProfile.java`: ensure JPA annotations, add `@Builder`, `@Data`
- `ExpertCredential.java`: ensure proper relationships

**Service Interface**:
```java
public interface ExpertService {
    ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request);
    ExpertProfileResponse getOwnProfile(UUID userId);
    ExpertProfileResponse updateProfile(UUID userId, UpdateExpertProfileRequest request);
    ExpertProfilePublicResponse getPublicProfile(UUID expertId);
    ExpertCredentialResponse uploadCredential(UUID userId, UploadCredentialRequest request, MultipartFile file);
    List<ExpertCredentialResponse> getMyCredentials(UUID userId);
}
```

### Database Migration

**File**: `src/main/resources/db/migration/V20260620_1200__tv4_expert_profile_tables.sql`

---

## Dependencies

- **STORY-002**: SecurityConfig, User entity, RBAC roles (EXPERT role must exist)
- **STORY-005**: AuditService for logging
- **Common**: `ApiResponse<T>` wrapper, exception handling

---

## Testing Strategy

### Unit Tests
- `ExpertServiceTests`:
  - `createProfile_success()`
  - `createProfile_duplicate_throws()`
  - `updateProfile_success()`
  - `getProfile_notFound_throws()`
  - `uploadCredential_success()`
  - `getMyCredentials_returnsList()`

### Mock Dependencies
- `ExpertProfileRepository` (Mockito)
- `ExpertCredentialRepository`
- `AuditService`
- `FileStorageService` (mock)

### Coverage Target
- Service: 100% (all branches)
- Policy: 100%
- Controller: 80% (happy path + error paths)

---

## Implementation Order

1. **Day 1**: Review existing entities, create migration SQL if needed
2. **Day 2**: Create DTOs, Mappers, Repositories
3. **Day 3**: Implement ExpertService with business logic, unit tests
4. **Day 4**: Create Controllers with RBAC annotations
5. **Day 5**: Integration testing, Postman collection, documentation

---

## References

- `docs/bmad/architecture.md` - Domain module map, layered architecture
- `docs/bmad/coding-standards.md` - Java coding conventions
- `02_Design/Architecture/project-structure-design.md` - Backend structure
- `function-spec-task-allocation.md` - Use case details
- `_bmad-output/planning-artifacts/epics.md` - Epic breakdown

---

**Story End**
