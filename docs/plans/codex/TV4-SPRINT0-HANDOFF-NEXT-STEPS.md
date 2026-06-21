# TV4 Sprint 0 - Handoff & Next Steps

**Prepared for:** TV4 (Expert Consultation Domain)  
**Date:** 2026-06-20  
**Status:** READY FOR IMPLEMENTATION  
**Next Session:** Tomorrow (2026-06-21)

---

## 🎯 TÓM TẮT CÔNG VIỆC ĐÃ LÀM

### ✅ Đã hoàn thành (hôm nay 2026-06-20):

1. **Phân tích source code hiện tại** - Xác nhận:
   - Backend: packages `expert/`, `consultation/`, `payment/` đã có entities nhưng thiếu layers
   - Mobile: có `features/consultation/` nhưng thiếu `features/expert/`
   - Frontend: có `features/consultationManagement/`, `expertVerification/` nhưng thiếu `expertDirectory/`

2. **Tạo Epic mới**:
   - ✅ `docs/stories/EPIC-006-expert-ecosystem.md`

3. **Tạo 11 Story files** cho Sprint 0:
   - ✅ STORY-401: Expert Profile & Credentials Management
   - ✅ STORY-402: Expert Availability Configuration
   - ✅ STORY-403: Expert Directory & Search
   - ✅ STORY-404: Consultation Booking Flow
   - ✅ STORY-405: Payment Processing (Mock VNPay)
   - ✅ STORY-406: Realtime Session Creation (Mock ZegoCloud)
   - ✅ STORY-407: Commission Calculation
   - ✅ STORY-408: Consultation Session Management
   - ✅ STORY-409: Admin Expert Verification Workflow
   - ✅ STORY-410: Expert Profile Review & Rating
   - ✅ STORY-411: Consultation Status & Dispute Management

4. **Tạo Implementation Plan chi tiết**:
   - ✅ `docs/plans/codex/IMPLEMENTATION-PLAN-TV4-SPRINT0-EXPERT-CONSULTATION.md`

---

## 📋 CÁC FILE CẦN ĐỌC NGÀY MAI TRƯỚC KHI CODE

**Đọc theo thứ tự này:**

### 1. Hiểu tổng thể (Bắt buộc)
```
📄 docs/plans/codex/IMPLEMENTATION-PLAN-TV4-SPRINT0-EXPERT-CONSULTATION.md
   ├─ Section 1-3: Overview, Stories list, Package structure
   ├─ Section 4: Database Migrations (copy SQL)
   ├─ Section 5: API Contract Summary
   ├─ Section 6: Implementation Rules
   ├─ Section 7: Step-by-Step Plan (Day 1-14)
   └─ Section 8-11: Testing, Acceptance, References
```

**Lưu ý:** File này là **bản kế hoạch chi tiết** để codex (AI) và bạn tuân theo.

---

### 2. Đọc Story đầu tiên (Bắt buộc)
```
📄 docs/stories/STORY-401-expert-profile-and-credentials-management.md
   ├─ User Story, Context, Requirements
   ├─ Database Design (tables: expert_profiles, expert_credentials)
   ├─ API Design (POST/GET/PUT endpoints)
   ├─ Acceptance Criteria (7 scenarios)
   ├─ Files to Create (backend, mobile, frontend)
   └─ Dependencies (STORY-002)
```

**Lưu ý:** Đây là story đầu tiên cần implement. Đọc kỹ acceptance criteria.

---

### 3. Tham khảo Epic (Tùy chọn)
```
📄 docs/stories/EPIC-006-expert-ecosystem.md
   ├─ Epic Overview, Scope, User Stories list
   ├─ FR Coverage table (17 FRs)
   ├─ Domain Module Map
   ├─ Success Criteria
   └─ Dependencies
```

---

### 4. Kiểm tra backend hiện tại (Bắt buộc trước khi code)
```bash
# Mở terminal, vào thư mục backend:
cd D:\CareBridge-SEP490-G79\04_SourceCode\CamBridgeAPI

# Kiểm tra entities hiện có:
find src/main/java/com/carebridge/backend/expert/entity -name "*.java"
find src/main/java/com/carebridge/backend/consultation/entity -name "*.java"
find src/main/java/com/carebridge/backend/payment/entity -name "*.java"

# Kiểm tra xem database migrations hiện có:
ls src/main/resources/db/migration/
```

**Ghi chú:** Bạn sẽ thấy:
- `expert/entity/` có: ExpertProfile.java, ExpertCredential.java, ExpertAvailability.java, ExpertReview.java, ExpertLocationShare.java
- `consultation/entity/` có: ConsultationBooking.java, ConsultationMessage.java, ConsultationSession.java
- `payment/entity/` có: 7 entity files
- Migrations: V1-V5 từ STORY-002 đã có

---

## 🚀 CÔNG VIỆC CẦN LÀM NGÀY MAI (2026-06-21)

### **PHASE 1: BACKEND - STORY-401 (Expert Profile & Credentials)**

#### **Buổi sáng (9:00 - 12:00): Database & Entities**

**Bước 1: Tạo/Verify Flyway Migration**
```bash
# File: 04_SourceCode/CamBridgeAPI/src/main/resources/db/migration/V20260621_1200__tv4_expert_profile_tables.sql

# Copy SQL từ Implementation Plan Section 4
# (Đã có sẵn trong file plan, chỉ cần tạo file mới)
```

**Bước 2: Review và cập nhật Entities (nếu cần)**
```bash
# Mở các file:
04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ExpertProfile.java
04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ExpertCredential.java
04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/expert/entity/ExpertAvailability.java

# Kiểm tra:
# - Có @Entity, @Table annotation không?
# - Có @Id, @GeneratedValue?
# - Các field có đúng với migration SQL không?
# - Có Lombok annotations (@Data, @Builder)?
```

**Bước 3: Chạy migration để tạo bảng**
```bash
cd 04_SourceCode/CamBridgeAPI
./mvnw flyway:migrate
# Lưu ý: Cần database đang chạy (docker-compose up -d postgres)
```

#### **Buổi chiều (14:00 - 18:00): Repositories & DTOs**

**Bước 4: Tạo Repositories**
```bash
# Files to create:
expert/repository/ExpertProfileRepository.java
expert/repository/ExpertCredentialRepository.java
expert/repository/ExpertAvailabilityRepository.java
expert/repository/ExpertSearchRepository.java  (custom queries)

# Pattern:
@Repository
public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {
    Optional<ExpertProfile> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}
```

**Bước 5: Tạo DTOs**
```bash
# request/
expert/dto/request/CreateExpertProfileRequest.java
expert/dto/request/UpdateExpertProfileRequest.java
expert/dto/request/UploadCredentialRequest.java

# response/
expert/dto/response/ExpertProfileResponse.java
expert/dto/response/ExpertProfilePublicResponse.java
expert/dto/response/ExpertCredentialResponse.java

# Mỗi DTO có:
# - Lombok @Data, @Builder
# - Validation annotations (@NotNull, @Size, etc.)
# - Jackson annotations (if needed)
```

**Bước 6: Tạo Mappers**
```bash
# Option A: MapStruct (preferred)
# Create mapper interface:
@Mapper(componentModel = "spring")
public interface ExpertProfileMapper {
    ExpertProfileResponse toResponse(ExpertProfile entity);
    ExpertProfile toEntity(CreateExpertProfileRequest request);
}

# Option B: Manual mapping in service
```

**Bước 7: Tạo Policy**
```bash
expert/policy/ExpertProfilePolicy.java

# Content:
@Component
public class ExpertProfilePolicy {
    public void checkCanViewProfile(UUID viewerId, UUID targetExpertId) {
        // Anyone can view public profile (no check)
        // But private fields require ownership or admin
    }

    public void checkCanEditProfile(UUID userId, UUID expertProfileId) {
        if (!userId.equals(expertProfileId)) {
            throw new AccessDeniedException("Cannot edit others' profile");
        }
    }
}
```

---

### **PHASE 2: SERVICE LAYER (Ngày 22/06)**

#### **Buổi sáng: ExpertService**
```bash
# Create:
expert/service/ExpertService.java (interface)
expert/service/impl/ExpertServiceImpl.java

# Methods to implement:
- ExpertProfileResponse createProfile(UUID userId, CreateExpertProfileRequest request)
- ExpertProfileResponse getOwnProfile(UUID userId)
- ExpertProfileResponse updateProfile(UUID userId, UpdateExpertProfileRequest request)
- ExpertProfilePublicResponse getPublicProfile(UUID expertId)
- ExpertCredentialResponse uploadCredential(UUID userId, MultipartFile file, UploadCredentialRequest request)
- List<ExpertCredentialResponse> getMyCredentials(UUID userId)

# Each method:
# - Check ownership via Policy
# - Call repository
# - Audit log via AuditService
# - Return mapped DTO
```

**Unit Tests (JUnit 5 + Mockito):**
```bash
# Create: expert/service/ExpertServiceTests.java

@Test
createProfile_success() { ... }
@Test
createProfile_duplicate_throws() { ... }
@Test
updateProfile_notOwner_throws() { ... }
@Test
getProfile_notFound_throws() { ... }
```

---

## 📞 PROMPT CHO CODEX/CLAUDE KHI CODE

Khi bạn muốn AI (codex/claude) giúp code, dùng prompts sau:

### **Prompt Template cho từng Story:**

```
Tôi đang làm STORY-401 (Expert Profile & Credentials Management).

Đọc các file:
1. docs/plans/codex/IMPLEMENTATION-PLAN-TV4-SPRINT0-EXPERT-CONSULTATION.md (Section 4, 5, 6, 7)
2. docs/stories/STORY-401-expert-profile-and-credentials-management.md
3. docs/bmad/architecture.md (Section 3, 9)
4. docs/bmad/coding-standards.md (Java section)

Yêu cầu: Tạo [TÊN FILE CẦN TẠO] theo đúng:
- Layered architecture (controller → service → repository)
- DTO validation với @Valid, @NotNull, @Size
- RBAC với @PreAuthorize("hasRole('EXPERT')")
- Response format ApiResponse<T>
- Audit log cho sensitive actions
- Unit tests cho service (Mockito)

File cần tạo: [specify file path]
Nội dung cần: [describe what the class should do]
```

---

### **Ví dụ prompt cụ thể:**

```
Tôi cần tạo ExpertProfileController.java cho STORY-401.

Đọc:
- Implementation Plan Section 5 (API Design)
- Story STORY-401 (API Design section)
- Architecture doc (layered architecture rules)

Yêu cầu:
1. File: 04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java
2. Controllers có:
   - @RestController
   - @RequestMapping("/api/v1/expert")
   - @PreAuthorize("hasRole('EXPERT')") trên các endpoint private
   - @Valid trên request body
3. Endpoints cần implement:
   - POST /profile → createProfile()
   - GET /profile → getOwnProfile()
   - PUT /profile → updateProfile()
   - GET /profile/{expertId} → getPublicProfile() (không cần auth)
4. Gọi ExpertService, trả về ApiResponse<T>
5. Xử lý exceptions với @ControllerAdvice (GlobalExceptionHandler từ common)
6. Audit log cho mỗi operation

Hãy viết code đầy đủ cho file này.
```

---

## 🔄 WORKFLOW CODE VỚI AGENT

### **Option 1: Dùng bmad-agent-dev (Recommended)**

```bash
# Trong terminal (Claude Code):
/bmad-agent-dev

# Khi agent hỏi "What would you like me to work on?", trả lời:
"Tôi cần implement STORY-401: Expert Profile & Credentials Management.

Context:
- Epic: EPIC-006-expert-ecosystem.md
- Story: STORY-401-expert-profile-and-credentials-management.md
- Plan: docs/plans/codex/IMPLEMENTATION-PLAN-TV4-SPRINT0-EXPERT-CONSULTATION.md

Order:
1. Tạo Flyway migration (verify với SQL trong plan)
2. Tạo ExpertProfileRepository.java
3. Tạo DTOs: CreateExpertProfileRequest, UpdateExpertProfileRequest, ExpertProfileResponse
4. Tạo ExpertProfileMapper.java
5. Tạo ExpertProfilePolicy.java
6. Tạo ExpertService.java + ExpertServiceImpl.java với business logic
7. Tạo ExpertProfileController.java với endpoints
8. Tạo unit tests cho ExpertService

Lưu ý:
- Tuân thủ layered architecture
- Dùng @PreAuthorize cho RBAC
- Call AuditService.logAction() sau khi tạo/sửa profile
- Response format: ApiResponse<T>
- Mock file storage cho credentials (trả về fake URL)

Sau khi xong, tôi sẽ chạy test và review."
```

---

### **Option 2: Dùng Claude Code trực tiếp**

```bash
# Trong terminal, dùng /prompt hoặc chat:
"Implement ExpertProfileRepository.java theo plan STORY-401.

Requirements from plan:
- Extends JpaRepository<ExpertProfile, UUID>
- findByUserId(UUID userId): Optional<ExpertProfile>
- existsByUserId(UUID userId): boolean

Place: 04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/expert/repository/ExpertProfileRepository.java

Use Spring Data JPA, no custom implementation needed."
```

---

## 📤 CREATING PULL REQUEST / MERGE

**Khi đã code xong 1 story:**

### **1. Commit với proper message:**
```bash
git checkout LamVH
git add .
git commit -m "feat(tv4): 3.2.1.1 - create expert profile endpoint (STORY-401)

- Add ExpertProfileRepository, ExpertService, ExpertProfileController
- Implement create/update/get profile APIs
- Add DTOs with validation
- Add unit tests for ExpertService (85% coverage)
- Create Flyway migration for expert_profiles table

Story: docs/stories/STORY-401-expert-profile-and-credentials-management.md"
```

### **2. Push và tạo PR:**
```bash
git push origin LamVH
# Sau đó lên GitLab/GitHub tạo PR từ LamVH → main (hoặc develop)
```

### **3. PR Description template:**
```markdown
## [TV4] STORY-401: Expert Profile & Credentials Management

### Changes
- Backend: Expert profile CRUD APIs
- Database: Migration V20260621_1200__tv4_expert_profile_tables.sql
- Tests: ExpertServiceTests (85% coverage)

### SRS IDs
- 3.2.1.1 Create Expert Profile
- 3.2.1.3 Upload Verification Documents

### How to Test
1. Start backend: `./mvnw spring-boot:run`
2. Run migrations: `./mvnw flyway:migrate`
3. Test with Postman collection (attached or in docs/plans/codex/)
4. Run tests: `./mvnw test`

### Acceptance Criteria
- [x] Expert can create profile
- [x] Expert can update profile
- [x] Expert can upload credentials
- [x] Duplicate profile creation fails
- [x] Ownership enforced (can't edit others)
- [x] Audit logs created

### Dependencies
- STORY-002: auth, audit service ✅
- Database: PostgreSQL ✅

### Screenshots (if UI affected)
[None for backend-only story]
```

---

### **4. Request Review**
- Tag: `@TV1` (for auth/consent check)
- Tag: `@Architect` (for architecture review)
- Tag: `@QA` (for testing review)

---

## ✅ CHECKLIST TRƯỚC KHI KẾT THÚC NGÀY

**Sau khi code xong STORY-401, check:**

- [ ] Backend compiles: `./mvnw clean compile` ✅
- [ ] Tests pass: `./mvnw test` ✅
- [ ] Coverage ≥70% for services ✅
- [ ] Database migrated successfully ✅
- [ ] All endpoints return `ApiResponse<T>` format ✅
- [ ] RBAC annotations present ✅
- [ ] Audit logs called ✅
- [ ] API documented in OpenAPI (swagger) ✅
- [ ] Code follows coding standards ✅
- [ ] PR created with proper description ✅
- [ ] PR linked to SRS IDs ✅

---

## 📚 REFERENCE FILES (ĐỌC KHI CẦN)

| File | Mục đích | Khi nào đọc |
|------|----------|-------------|
| `IMPLEMENTATION-PLAN-TV4-SPRINT0.md` | Kế hoạch chi tiết | Trước khi code mỗi story |
| `STORY-40X-*.md` | Story spec | Trước khi code story đó |
| `EPIC-006-expert-ecosystem.md` | Epic overview | Hiểu tổng thể |
| `docs/bmad/architecture.md` | Architecture rules | Khi câu hỏi về design |
| `docs/bmad/coding-standards.md` | Coding conventions | Khi không biết format |
| `function-spec-task-allocation.md` | Use case details | Xem business rules |
| `STORY-002-*.md` | Shared contracts | Reference auth/audit APIs |
| `02_Design/Architecture/project-structure-design.md` | Package structure | Hiểu domain boundaries |

---

## 🆘 KHI BẊO ĐỨNG (BLOCKED)

### **Vấn đề 1: Không biết dùng TV1 service nào**
```
Đọc:
- docs/stories/STORY-002-backend-shared-domain-scaffold.md
- 04_SourceCode/CamBridgeAPI/src/main/java/com/carebridge/backend/common/

Tìm:
- AuditService (audit.logAction())
- SecurityConfig (roles)
- ApiResponse (common.response)
```

### **Vấn đề 2: Entity không khớp với DB**
```
Chạy:
./mvnw flyway:info
./mvnw flyway:migrate

Xem migration error, sửa entity cho khớp.
```

### **Vấn đề 3: Không biết viết test**
```
Đọc file mẫu:
04_SourceCode/CamBridgeAPI/src/test/java/com/carebridge/backend/audit/service/AuditServiceTests.java

Copy pattern, thay đổi cho service của bạn.
```

---

## 📅 LỊCH TRÌNH NGÀY MAI (GỢI Ý)

| Thời gian | Công việc | Agent/Tool |
|-----------|-----------|-----------|
| 9:00 - 9:30 | Đọc handoff file này, rà soát plan | - |
| 9:30 - 10:00 | Đọc STORY-401 và Implementation Plan | - |
| 10:00 - 12:00 | Tạo migration + entities | Claude Code / bmad-agent-dev |
| 14:00 - 16:00 | Tạo repositories + DTOs + mappers | Claude Code |
| 16:00 - 18:00 | Tạo service + unit tests | Claude Code |
| 18:00 - 18:30 | Compile, test, commit | Terminal |

---

## 🎯 MỤC TIÊU NGÀY MAI

**Hoàn thành STORY-401:**
- ✅ Database migration executed
- ✅ ExpertProfile, ExpertCredential entities valid
- ✅ Repositories: ExpertProfileRepository, ExpertCredentialRepository
- ✅ DTOs: Create/Update/Response + UploadCredentialRequest
- ✅ Mappers: ExpertProfileMapper, ExpertCredentialMapper
- ✅ Policy: ExpertProfilePolicy
- ✅ Service: ExpertService + Impl với 100% business logic
- ✅ Controller: ExpertProfileController với 3 endpoints
- ✅ Unit tests: ExpertServiceTests ≥70% coverage
- ✅ PR created và merged (hoặc draft)

---

## 📞 LIÊN HỆ / HỎI ĐÁP

**Khi cần hỏi:**
1. Đọc kỹ Implementation Plan trước
2. Tìm trong `docs/bmad/` cho coding standards
3. Xem existing code trong `expert/`, `consultation/` làm mẫu
4. Nếu vẫn không rõ, hỏi trong team chat hoặc dùng agent

---

**CHÚC BẠN NGỦ NGHỈ TỐT! 🌙**

**Ngày mai bắt đầu từ STORY-401, đọc kỹ acceptance criteria và làm theo Implementation Plan.**

---

**Handoff Prepared By:** Claude (BMAD Business Analyst)  
**Date:** 2026-06-20  
**Next Review:** 2026-06-21
