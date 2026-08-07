# CareBridge — Database Consolidation Source Code Refactor Plan

**Ngày:** 2026-08-06  
**Trạng thái:** Draft for implementation review  
**Database specification:** `Database_Table_Audit_And_Consolidation V3.md`

---

## 1. Mục đích

Tài liệu này chuyển các quyết định database trong
`Database_Table_Audit_And_Consolidation V3.md` thành kế hoạch refactor source code có thể chia
thành PR và release độc lập.

Đối tượng sử dụng chính:

- Backend developer viết entity/repository/service và Flyway compatibility code.
- Web/Mobile developer xử lý breaking API, role, route và model.
- Reviewer/QA kiểm tra code đã ngừng truy cập object cũ trước DB contract migration.
- Release owner điều phối expand, cutover, observation và contract.

V3 là nguồn sự thật cho **target database**. Tài liệu này là nguồn sự thật cho **source-code
impact và deployment order**. Nó không chứa SQL migration đầy đủ và không thay thế change
ticket/migration matrix của từng object.

Impact inventory được lập bằng code-review graph rồi đối chiếu bằng targeted `rg`. Graph tại
thời điểm audit chậm hơn `HEAD` một commit, vì vậy mỗi PR phải chạy lại static scan trước khi
chốt file list.

## 2. Nguyên tắc và ranh giới refactor

1. **Code trước DB contract.** Entity, repository và native SQL phải ngừng dùng object cũ trước
   khi `DROP TABLE/COLUMN/VIEW`.
2. **DB expand trước code target.** Code không được map cột/bảng target khi additive migration
   chưa tồn tại trên mọi môi trường được deploy.
3. **Không mega-PR.** Mỗi domain có PR expand-compatible, cutover và cleanup riêng nếu cần.
4. **Giữ API khi persistence-only.** Reminder, appointment timing, safety, checklist,
   consultation và growth nên giữ request/response contract để tránh refactor client không cần thiết.
5. **Breaking API phải client-first hoặc có deprecation window.** Áp dụng cho deletion request,
   lock appeal, Partner, device và nearby support.
6. **Không sửa migration lịch sử đã apply.** Chỉ thêm Flyway migration mới; test legacy migration
   được giữ nếu còn giá trị regression.
7. **Không target-only write khi rollback còn đọc source.** Dùng dual-write, source-write + replay,
   hoặc freeze/final-delta trước cutover.
8. **Không xóa code retained object.** `settings_jsonb`, `reminder_occurrence_aliases`,
   `direct_conversation_read_cursors` và `device_tokens` tiếp tục là contract sống.
9. **Deferred nghĩa là chưa refactor persistence.** Growth và tách `safety_events` không được
   trộn vào các PR immediate.
10. **Mỗi PR phải có static exit gate.** Không chỉ dựa vào Hibernate validation; 221 native SQL
    locations của backend phải được scan lại.

Quy ước trong tài liệu:

- **SỬA:** refactor nhưng giữ file/feature.
- **XÓA:** xóa sau khi caller và deployment compatibility đã được xử lý.
- **GIỮ:** không refactor persistence hoặc phải giữ contract.
- **TẠO:** source mới cho target schema.
- **DEFER:** chỉ chuẩn bị interface/test boundary, chưa đổi persistence.

## 3. Tổng quan phạm vi source code

| Workstream | Backend | Web | Mobile | API contract | Trạng thái |
| --- | --- | --- | --- | --- | --- |
| Account deletion request | Xóa package queue; hoàn thiện direct deactivation | Không thấy caller chính | Bỏ legacy request sheet/API | **Breaking**: bỏ request/cancel queue | Immediate |
| Account lock appeal | Xóa controller/service/repo/token logic | Xóa appeal pages/API/model | Bỏ appeal form/state | **Breaking**: bỏ appeal endpoints | Immediate |
| Archive/facility partner link | Bỏ `partnerId` mapping | Không đáng kể | Bỏ/optional field facility | Facility response mất field legacy | Immediate |
| Device integration | Xóa `health/device` và wearable package; sửa observation | Không có UI | Không có connect/sync UI | **Breaking**: bỏ device APIs | Immediate |
| Partner | Xóa backend package/role/seed/security | Xóa portal/routes/role | Không có portal chính | **Breaking**: bỏ role/endpoints | Immediate |
| Direct-chat legacy columns | Bỏ 4 fields + native methods | Không đổi | Không đổi | Giữ nguyên | Immediate |
| Nearby support | Xóa backend feature map vào dead view | Không tìm thấy UI | Xóa expert screen/route/home call | **Breaking**: bỏ nearby-support APIs | Immediate |
| Reminder schedule times | Map array, bỏ child repo/entity | Không đổi | Giữ list-times DTO | Giữ nguyên | Immediate |
| Appointment rules | JSONB aggregate, bỏ rule repo/entity | Không đổi | Giữ timing DTO/editor | Giữ nguyên | Immediate |
| Notification jobs | Tạo entity/repo chung; gộp planner/worker | Không đổi | Không đổi | Internal only | Immediate, rủi ro cao |
| Safety config | Chuyển persistence sang `users` | Không đổi | Giữ DTO/UI | Giữ nguyên | Immediate |
| Checklist legacy | Bỏ compatibility read từ legacy sau cutover | Không đổi | Giữ task DTO/identity | Giữ nguyên | Immediate |
| Consultation session | Chuyển read model vào booking | Không đổi | Giữ consultation request UI | Giữ nguyên | Immediate có gate |
| Growth | Chuẩn bị grouped observations | BabyCare page giữ contract | Giữ logical growth model | Giữ nguyên | **Deferred** |
| Reminder alias | Không đổi | Không đổi | Không đổi | Internal identity | **Retained** |
| Safety events split | Không đổi event persistence | Không đổi | Không đổi | Chưa chốt | **Deferred design** |

## 4. Workstream chi tiết

### 4.1. Account lifecycle và lock appeal

#### A. Account deletion request → direct deactivation

**Xóa workflow 30 ngày:**

- `CareBridgeAPI/.../account/controller/AccountDeletionController.java`
- `.../account/service/AccountDeletionService.java`
- `.../account/service/impl/AccountDeletionServiceImpl.java`
- `.../account/entity/AccountDeletionRequest.java` và `DeletionStatus.java`
- `.../account/repository/AccountDeletionRequestRepository.java`
- Hai DTO request/response trong `.../account/dto/`
- `AccountDeletionServiceTest.java` sau khi thay bằng direct-deactivation tests

**Giữ và sửa luồng đúng:**

- `security/controller/AuthController.java`: giữ `DELETE /api/v1/auth/deactivate`.
- `security/service/impl/AuthServiceImpl.java`: trong cùng transaction phải ghi
  `enabled=false`, `accountStatus=DEACTIVATED`, `deactivatedAt`, `deactivationReason`,
  `deactivatedBy`; revoke refresh sessions, deactivate device tokens và audit.
- `security/dto/request/DeactivateRequest.java`: giữ xác nhận mật khẩu.

**Mobile:**

- Xóa `features/auth/screens/delete_account_sheet.dart`.
- Bỏ `requestAccountDeletion()` và `/api/v1/account/deletion-request` khỏi
  `features/auth/services/auth_service.dart`.
- Giữ `deactivateAccount()`, `deactivate_account_screen.dart` và entry trên
  `account_profile_screen.dart`.

**Tests bắt buộc:** wrong password, SYSTEM_ADMIN guard, already deactivated, metadata đầy đủ,
session/refresh-token/device-token bị revoke, access request tiếp theo bị chặn và login lại thất bại.

**Precondition xóa queue code:** reconcile mọi request `PENDING` thành đúng một outcome
`deactivate`, `cancel` hoặc `temporarily retain`; R1b chỉ được chạy khi query `PENDING = 0`.

#### B. Account lock appeal → CSKH unlock

**Xóa:**

- Public/admin controllers `AccountLockAppealController` và `AdminAccountLockAppealController`.
- Entity/status/repository/service/DTO appeal trong `identity/admin`.
- Appeal-token methods và `AppealTokenClaims` trong `JwtTokenProvider`.
- Whitelist `/api/v1/auth/lock-appeals` trong `SecurityConfig`.
- Appeal pages/API/models trên Web và appeal request trên Mobile.

**Sửa:**

- `AuthenticationPolicy`: bỏ repository/status/token appeal nhưng vẫn trả trạng thái account locked.
- `AccountLockedException`: bỏ appeal-specific fields/constructors.
- `JwtAuthenticationFilter`: bỏ appeal-specific response fields nếu có.
- `AdminUserServiceImpl`: bỏ cancel appeal; unlock phải audit actor, user, lock episode, reason,
  CSKH ticket ID và timestamp.
- Web `BlockedAccountPage` và Mobile `blocked_account_screen.dart`: chỉ hướng dẫn liên hệ CSKH.
- Web `AdminDashboardPage`, router và admin API/model: bỏ queue/card/routes appeal.

**Exit gate:** không còn `AccountLockAppeal`, `appealToken`, `appealPending`,
`/auth/lock-appeals` hoặc `/admin/account-lock-appeals` trong production source.

**Precondition R2b:** pending appeal = 0, mọi case chuyển CSKH đã có ticket/audit evidence, và
authentication policy đã chạy độc lập với appeal repository.

### 4.2. Archived records và care facility legacy partner link

Không có production entity/repository cho `archived_records`; refactor tập trung vào cột legacy
`care_facilities.partner_id`.

**Sửa:**

- `map/entity/CareFacility.java`: bỏ `partnerId`.
- `map/dto/response/FacilityResponse.java`: bỏ field legacy.
- `map/mapper/CareFacilityMapper.java` và `map/service/impl/CareFacilityServiceImpl.java`: bỏ mapping.
- Mobile `features/emergency/models/care_facility_model.dart`: bỏ field hoặc cho phép payload
  không có field trong deprecation window.

Không xóa test “archive health record” chỉ vì tên chứa archive. Chỉ sửa fixture/assertion thực sự
đọc bảng `archived_records`.

**Compatibility:** JSON client thường chịu được server bỏ field không sử dụng, nhưng Mobile model
phải được deploy trước nếu constructor/parser hiện yêu cầu `partnerId`.

### 4.3. Device integration removal

**Xóa toàn bộ module IoT backend:**

- Package `health/device`: controllers, services/interfaces, entity, repositories, mapper, DTO,
  exception và device events.
- Package `integration/wearable`: `WearableProviderClient`, `MockWearableProviderClient`,
  `RawMeasurement` và wiring liên quan.
- Unit/integration tests dưới `src/test/.../health/device`.

**Sửa:**

- `health/entity/HealthObservation.java`: bỏ mapping `device_connection_id`; giữ
  `raw_payload_jsonb` và source/provenance fields.
- Seed/dev fixtures và canonical schema tests không còn mong đợi connection table/API.

**Giữ:**

- `device_tokens` cùng notification push code.
- Mobile `SourceType.device` trong `health_metric_model.dart` nếu dữ liệu lịch sử vẫn hiển thị
  provenance thiết bị.

Không tìm thấy Web/Mobile UI connect/disconnect/sync hiện hành. Static scan vẫn phải chạy lại
trước xóa endpoints. Các tài liệu UC66–69 và UC130 được đánh dấu obsolete hoặc out-of-scope.

**Exit gate:** không còn bean/controller/repository device; application context khởi động khi
`device_connections` và `health_observations.device_connection_id` không tồn tại. Ngoài ra phải
có `linked_observations = 0` hoặc provenance backfill đã đối soát 100%; payload đúng JSON shape
trong V3 và scan chứng minh không copy token/OAuth secret/credential.

### 4.4. Partner module removal

**Backend xóa:** toàn bộ package `partner`, Partner exception handler, seed
`partner@carebridge.dev` và upload/authorization rule chỉ dành cho Partner. Historical
`AuditAction` constants được giữ deprecated như quy định bên dưới.

**Backend sửa:**

- `security/rbac/Role.java`: bỏ `PARTNER` sau khi data remap hoàn tất.
- `security/config/SecurityConfig.java`: bỏ matcher/role hierarchy Partner.
- `file/controller/FileController.java`: bỏ Partner upload permission.
- `common/dev/DevDataSeeder.java`: bỏ Partner seed.
- `common/exception/GlobalExceptionHandler.java`: bỏ Partner exception mapping.
- `audit/entity/AuditAction.java`: **giữ deprecated Partner constants trong program này** để
  historical audit values luôn deserialize/report được. Chỉ xóa ở migration riêng nếu distinct
  audit values bằng 0 hoặc dữ liệu đã được migrate bằng quy trình được duyệt.

**Web xóa:** toàn bộ `src/features/partnerGovernance`.

**Web sửa:**

- `app/router/index.tsx`, `app/layouts/AdminLayout.tsx`.
- `shared/auth/authStore.ts`, `shared/auth/roleRoutes.ts`.
- `features/auth/models/user.ts`.
- Admin user list/detail, OTP/notification UI còn hiển thị hoặc filter `PARTNER`.

**Tests:** xóa Partner package/integration/security tests; sửa
`CanonicalRoleSchemaIntegrationTest` để chứng minh `PARTNER` bị reject; chạy lại mọi test enum
Role vì nhiều fixture liệt kê toàn bộ roles.

**Data-first gate:** remap/deactivate mọi user Partner và vô hiệu token/session cũ trước deploy
code bỏ enum; nếu không JPA/JWT deserialization có thể fail.

### 4.5. Direct chat read cursor cleanup

**Sửa:**

- `directchat/entity/DirectConversation.java`: bỏ 4 fields `mother/expert_last_read_*`.
- `directchat/repository/DirectConversationRepository.java`: xóa `markMotherRead`,
  `markExpertRead` và native SQL legacy.

**Giữ:** `ConversationSummaryAggregateRepositoryImpl`, `direct_conversation_read_cursors`, API
mark-read và unread response.

**Tests:** cập nhật read/race integration tests để chứng minh cursor path hoạt động khi bốn cột
không tồn tại. Không sửa migration lịch sử `V20260801100001`; thêm contract migration mới.

### 4.6. Nearby support removal

V2 xác nhận view luôn rỗng và write bị trigger chặn; Product Owner sau đó cho phép coi các đề xuất
V2 hợp lý là đã duyệt. Vì vậy plan này ghi nhận quyết định retire **toàn bộ nearby support request
feature**, không phải chỉ xóa view rồi giữ endpoint hỏng. Nếu quyết định sản phẩm này bị rút lại,
workstream phải dừng để chọn target persistence/API mới; không được drop view độc lập.

**Backend xóa:** package `nearbycare` gồm controller, two entities/repositories, service, mapper,
DTO và exception handling liên quan.

**Mobile xóa/sửa:**

- Xóa `expert/screens/expert_nearby_support_screen.dart`.
- Bỏ route/import `/expert/nearby-support` khỏi `core/routes/app_router.dart`.
- Bỏ request `/api/v1/nearbycare/support-requests/open` và support-request card khỏi
  `expert/services/expert_home_service.dart`/expert home UI.
- Sửa/xóa fixtures trong `expert_app_home_screen_test.dart` và nearby support contract tests.

Không xóa tính năng bản đồ/cơ sở y tế gần đây dùng `care_facilities`; đó là bounded context khác.

**Exit gate:** không còn `/api/v1/nearbycare`, `NearbySupport*`, `support-requests/open` hoặc route
expert nearby support trong production clients trước khi backend endpoint bị gỡ.

### 4.7. Reminder schedule times → array

**Tạo/sửa target mapping:**

- `ReminderSchedule.java`: map `local_times time[]` thành ordered collection.
- `ReminderScheduleServiceImpl`: create/update/toResponse đọc và ghi array; bỏ delete-all/save-all
  qua child table.
- `ReminderScheduleProcessingService`: planner duyệt snapshot array, không query child rows.

**Xóa sau cutover:** `ReminderScheduleTime.java`, `ReminderScheduleTimeRepository.java` và test
chỉ kiểm tra persistence child-table.

**Giữ API:** Mobile `reminder_schedule_model.dart`, service và screens tiếp tục nhận/gửi danh
sách giờ. Thứ tự array là display/execution order.

**Compatibility:** source-write hoặc dual-write trong expand; target-read chỉ sau backfill. Tests
phải bao phủ duplicate, null, empty-active schedule, ordering và timezone behavior.

### 4.8. Appointment notification rules → JSONB

**Sửa:**

- `AppointmentNotificationConfig.java`: map `rules_jsonb` thành value object/list và giữ
  `config_revision`.
- `AppointmentNotificationConfigRepository`: aggregate config + rules trong một row.
- `AppointmentNotificationRuleValidator`: validate shape, integer, range, uniqueness và order.
- `AppointmentNotificationScheduleService`: bỏ rule repository; tạo job từ immutable snapshot
  của JSONB/config revision.
- `CareGroupAppointmentNotificationService`: giữ API nhưng không truy cập entity rule cũ.

**Xóa sau cutover:** `AppointmentNotificationRule.java` và
`AppointmentNotificationRuleRepository.java`.

**Giữ client contract:** Mobile timing editor/model tiếp tục dùng danh sách offset; không expose
raw JSONB ra client.

**Tests:** chuyển validator/service tests sang aggregate JSON; migration contract test kiểm tra
backfill 20 rows, revision bump và invalid JSON bị DB/application cùng reject.

### 4.9. Notification jobs consolidation

Đây là workstream rủi ro cao nhất và phải tách khỏi rule/config refactor.

**Tạo:**

- Entity `NotificationJob` map bảng chung.
- Enum `NotificationJobType { REMINDER_SCHEDULE, APPOINTMENT }`.
- Repository chung với claim/requeue/transition/cancel queries có discriminator.
- Có thể giữ status enum hiện tại dưới tên chung nếu state machine không đổi.

**Sửa:**

- `AppointmentNotificationHorizonPlanner` và appointment worker/processing service.
- Reminder schedule planner/processing service và worker tương ứng.
- Configuration wiring để hai job type có thể dùng batch size/delay riêng nhưng cùng repository.

**Xóa sau observation:** hai entity/repository source và worker/planner trùng lặp không còn dùng.

**Invariants source code phải giữ:**

- Không claim nhầm `job_type`.
- Giữ typed FK fields và hai identity khác nhau.
- Giữ `job_id`, status, attempts, lock owner/time, retry time, notification record và error code.
- `occurrence_id` tiếp tục bao hàm generation theo occurrence-ID v2.
- Không có hai worker xử lý cùng logical job trong cutover.

**Cutover runbook:** disable planners → chờ/requeue `PROCESSING` → final-delta backfill → deploy
repository/workers mới → enable planners → quan sát planner, success, retry, stale-lock cycle →
xóa code source.

“Disable planners” bao gồm disable **cả source planners lẫn source workers/claim queries**. Sau
đó mới chờ `PROCESSING=0`, freeze source writes, chạy final delta trong transaction và đối soát.
Không để worker cũ claim/update source queue trong lúc final delta.

Trước cutover phải test trực tiếp `ReminderOccurrenceIdFactory`/occurrence-ID v2 để chứng minh
`occurrence_id` khác nhau giữa generations và không có ambiguous IDs. Nếu không chứng minh được,
schema/identity target phải thêm `occurrence_generation` theo fallback của V3.

**Tests:** repository Postgres tests cho partial identities/claim concurrency; service tests cho
hai discriminator; worker tests cho retry/lock; migration contract test cho count/status/job-ID
collision và final delta.

### 4.10. Safety config → `users`

**Sửa persistence:**

- `security/entity/User.java`: map các typed safety columns.
- `safety/entity/SafetyMonitoringConfig.java`: xóa sau cutover hoặc chuyển thành domain value
  object không còn `@Entity`.
- `safety/repository/ISafetyConfigRepository.java`: bỏ sau cutover; tạm dùng compatibility adapter.
- `SafetyConfigService`, `FallDetectionService`, `SensorSelfTestService`: đọc/ghi qua user aggregate
  hoặc dedicated adapter thay vì query `safety_configs`.
- `SafetyConfigMapper` và `SafetyConfigChanged` handler: giữ DTO/event semantics.

**Giữ API/Mobile:** `SafetyConfigRequest/Response`, Mobile `safety_config_model.dart`, safety
service/screens và foreground detection không đổi field names.

**Compatibility:** typed-column-first + fallback source, write-through trong observation. Tests
phải chứng minh default cho user chưa có config, permission timestamp CHECK, countdown values,
concurrent update và hot path không query bảng cũ.

### 4.11. Legacy preparation checklist → checklist v2

**Xóa legacy read layer sau backfill:**

- `UserChecklistItem` entity, repository, controller/service code còn trộn rows từ
  `preparation_checklist_items`.
- Legacy exception handler/DTO chỉ phục vụ retired mutations nếu không còn consumer.

**Giữ và sửa target path:**

- `ChecklistTaskInstance` và `ChecklistInstance` entities.
- `ChecklistDistributionService`.
- `ChecklistV2CompatibilityMutationService` trong giai đoạn compatibility.
- `UserCreatedChecklistTaskService`.
- `ChecklistTaskActionHandler`, `ChecklistTodayTaskProvider`, `CareGroupChecklistService`,
  `ChecklistHistoryService`.

Mọi source row phải resolve parent/context/template và dùng canonical `checklist_v1_key`.
Unresolved template làm migration fail; không tự hạ thành user-created.

**Giữ Mobile contract:** toàn bộ `features/checklist` có thể giữ nếu task ID, status, title,
category, due/completed timestamps và ordering không đổi. Backend không được bắt client biết
`checklist_instance_id` hoặc tự reconcile legacy/v2.

**Tests:** giữ migration/rehearsal/collision tests; sửa API/service tests để source table vắng mặt;
thêm equality test giữa legacy response snapshot và target response.

### 4.12. Consultation session → booking

**Sửa:**

- `health/entity/ConsultationBooking.java`: map room, session status/timestamps, summary,
  technical log, session-created timestamp và legacy session ID.
- Repository/service/reporting code đang dùng `ConsultationSessionRepository` phải chuyển sang
  booking fields.
- Mapper/DTO chỉ thay đổi nội bộ nếu API hiện không expose session entity trực tiếp.

**Xóa sau cutover:** `consultation/entity/ConsultationSession.java` và
`ConsultationSessionRepository.java`.

**Giữ Mobile contract:** module hiện chủ yếu dùng `expert_consultation_requests`, không nên đổi
DTO chỉ vì persistence booking/session gộp.

**No-go:** session không có booking, nhiều session/booking hoặc bất kỳ booking nào có một trong
các field `expert_price_id`, `price_band_id`, `price_snapshot_amount`,
`commission_rate_snapshot`, `price_locked_at`. Cardinality query và paid-field query trong V3
phải cùng trả 0 trước contract. Sau reconcile, tests chạy trên schema không có
`consultation_sessions` và chứng minh logical session lifecycle/timestamp shape.

```sql
SELECT booking_id, count(*)
FROM consultation_sessions
GROUP BY booking_id
HAVING booking_id IS NULL OR count(*) > 1;

SELECT count(*)
FROM consultation_bookings
WHERE expert_price_id IS NOT NULL
   OR price_band_id IS NOT NULL
   OR price_snapshot_amount IS NOT NULL
   OR commission_rate_snapshot IS NOT NULL
   OR price_locked_at IS NOT NULL;
-- cả hai expected trước contract: 0
```

### 4.13. Growth measurements → health observations — deferred

Không thay persistence trong immediate program.

**Giữ hiện tại:** `GrowthMeasurement`, repository, controller, `IGrowthService`,
`GrowthServiceImpl` và Mobile/Web growth UI.

**Wave sau mới sửa:**

- `HealthObservation` thêm `measurement_group_id` và BABY legacy identity support.
- `GrowthServiceImpl`, `BabyCareOverviewServiceImpl`, `BabyCareTimelineServiceImpl`,
  `AppointmentPreparationServiceImpl` đọc/ghi grouped observations.
- Health metric definitions/service chấp nhận `BABY` và đúng units.
- Mobile/Web vẫn nhận một logical growth measurement; backend ghép tối đa ba observations.

**Compatibility requirement:** ID logical ổn định, grouped atomic update/delete, soft-delete
không làm hiện lại record, ordering/date/note/source giữ nguyên. Source code chỉ được xóa sau
compatibility view/service vượt full regression.

### 4.14. Objects được giữ và chương trình tách riêng

**Giữ `settings_jsonb`:** không xóa `User.settings`, notification/privacy repositories,
`activeBabyId` hoặc security compatibility readers trong program này.

**Giữ reminder occurrence alias:** entity, repository, ID factory, trigger contract và migration
tests. Job refactor phải dùng cùng occurrence identity/generation.

**Giữ `direct_conversation_read_cursors`:** đây là target read state, không phải legacy table.

**Tách `safety_events`:** chỉ lập design doc riêng. Immediate workstream safety chỉ chuyển config;
không đổi `SafetyEvent`, response records, alert attempts/delivery hoặc sensor persistence.

**Retained negative scope từ V3 §4:** không drop/gộp `audit_events`,
`triage_session_evidence`, `knowledge_source_reviews`, `triage_sessions`, `data_permissions`,
`moderation_cases`, `auth_sessions`, `auth_challenges`, `content_item_topics`,
`content_item_sources`, `professional_specialties`, `vaccination_records`,
`vaccination_schedules`, `development_milestones`, `care_groups`, `care_group_members` hoặc
`flyway_schema_history`. Mỗi migration matrix phải có negative-impact check cho nhóm này.

## 5. API, DTO và compatibility policy

### 5.1. Breaking endpoints được retire

- `POST/DELETE /api/v1/account/deletion-request`.
- Public/admin account-lock appeal endpoints.
- Partner profile, registration, approval và governance endpoints.
- Device connect/disconnect/import/sync/trend endpoints.
- Nearby support request create/list/cancel/respond/open endpoints.
- Facility response bỏ `partnerId` legacy.
- Locked-account response bỏ appeal token/status/pending fields.

| API/shape | Backend owner | Client caller chính | Removal release |
| --- | --- | --- | --- |
| `POST/DELETE /api/v1/account/deletion-request` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/account/controller/AccountDeletionController.java` | Mobile `features/auth/services/auth_service.dart`, delete sheet | R1b sau R1a/R0b |
| `POST /api/v1/auth/lock-appeals` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AccountLockAppealController.java` | Web `accountLockAppealApi.ts`; Mobile `auth_service.dart` | R2b sau R2a/R0b |
| `GET/PATCH /api/v1/admin/account-lock-appeals/**` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminAccountLockAppealController.java` | Web `adminUserApi.ts`, appeal pages/dashboard | R2b |
| `/api/v1/partner/profile` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/partner/controller/PartnerProfileController.java` | Web `features/partnerGovernance/services/partnerApi.ts` | R3c sau R3a/R3b |
| `/api/v1/admin/partners/**` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/partner/controller/PartnerApprovalController.java` | Web Partner verification queue/API | R3c |
| `/api/v1/health/devices/connections/**` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/device/controller/DeviceConnectionController.java`, `DeviceSyncController.java` | Không tìm thấy Web/Mobile caller | R4c |
| `/api/v1/health/metrics/device-import`, `/trend` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/device/controller/DeviceMetricController.java` | Không tìm thấy Web/Mobile caller | R4c |
| `/api/v1/nearbycare/**` | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/nearbycare/controller/NearbySupportController.java` | Mobile expert home/nearby route | R4b sau R4a |
| Facility response `partnerId` | map facility DTO/controller path | Mobile `care_facility_model.dart` | R5b sau R5a |
| Locked-account appeal fields | security exception/filter response | Web/Mobile blocked-account state | R2b sau R2a |

Nếu Web/Mobile đã phát hành cho người dùng thật, client removal phải được deploy trước backend
endpoint removal hoặc backend phải duy trì deprecation window. Không để client cũ nhận 404 không
được xử lý; trong window có thể trả deprecation response rõ ràng, nhưng không ghi vào bảng sắp drop.

Deprecation exit gate không dựa vào thời gian tùy ý: contract chỉ được remove khi minimum
supported Web/Mobile version đã chứa client cleanup và telemetry/access log không còn caller cũ
trong ít nhất một observation window. Nếu ứng dụng chưa từng phát hành ra ngoài môi trường nhóm,
release owner có thể ghi nhận “no external clients” và bỏ deprecation window.

### 5.2. Stable API contracts

- `DELETE /api/v1/auth/deactivate` và request xác nhận mật khẩu.
- Admin user lock/unlock/status API.
- Direct-chat mark-read và unread count.
- Reminder schedules trả ordered list of local times.
- Appointment notification timing trả ordered offsets, không raw JSONB.
- Safety config request/response fields.
- Checklist task/list/history/action contracts và stable task identity.
- Consultation request/handoff contracts.
- Growth API trả một logical measurement, kể cả khi persistence sau này là ba observations.

### 5.3. DTO policy

- Persistence field/table name không được lộ ra API mới.
- Field bị loại khỏi response (`partnerId` của facility) phải optional trên client trước server removal.
- Account locked response bỏ appeal-specific fields theo một version boundary; UI vẫn phải hiển thị
  reason/contact-CSKH path.
- Notification job/entity không có public DTO.
- `legacy_session_id`, `measurement_group_id` và DB provenance là internal fields trừ khi có use
  case audit rõ ràng.

### 5.4. Invariants không được tự diễn giải lại

- Reminder `local_times`: không `NULL`, không phần tử `NULL`, không duplicate; active schedule
  phải có ít nhất một giờ; array order là execution/display order; timezone field giữ nguyên.
- Appointment `rules_jsonb`: array của object `{offsetMinutes}`; integer trong
  `[-43200, 10080]`, không duplicate, array order thay `sort_order`; mọi thay đổi tăng
  `config_revision` trong cùng transaction.
- Safety defaults: fall detection `false`, sensitivity `MEDIUM`, emergency auto-alert `true`,
  countdown `30`, sensor permission `false`; permission `true` bắt buộc có recorded timestamp.
- Notification jobs: state machine `PENDING/PROCESSING/SENT/FAILED/SUPPRESSED/CANCELLED`; typed
  discriminator/FK; schedule identity và appointment identity đúng V3; source `job_id` collision
  phải bằng 0 hoặc có explicit mapping.
- Checklist: dùng canonical `checklist_v1_key`; collision/unresolved template làm reconciliation
  fail và phải về 0 trước contract.
- Consultation: cardinality query và paid/legacy query đều phải về 0 trước drop; biên bản chấp
  nhận ngoại lệ không thay thế zero-row gate.

## 6. PR và release sequence

Không triển khai theo một PR khổng lồ. Sequence đề xuất:

| PR/Release | Phạm vi code | DB dependency | Exit gate |
| --- | --- | --- | --- |
| R0a | Feature inventory, flags/deprecation response, data-gate scripts | V3 Wave 0 | File list/static scans được lưu |
| R0b | Reconcile pending deletion/appeal; tạo CSKH/audit evidence | Trước backend workflow removal | Deletion `PENDING=0`, appeal `PENDING=0` |
| R1a | Mobile bỏ legacy deletion request, giữ direct deactivation | Users fields đã có | Mobile auth tests pass |
| R1b | Backend hoàn thiện deactivation và xóa deletion-queue API/module | R0b + client gate/deprecation đạt | Deactivation/session/token tests pass |
| R2a | Web/Mobile bỏ appeal form/API/state; hiển thị CSKH | Chưa drop appeal table | Client tests/build pass |
| R2b | Backend bỏ appeal endpoints/token/repo; giữ admin unlock + audit | R0b + R2a đạt | Không còn appeal symbols/routes |
| R3a | Web bỏ Partner portal/role/routes | Chưa drop Partner table | Web test/lint/build pass |
| R3b | Data release: remap/deactivate Partner users và revoke session/token | Trước code bỏ enum | Partner user count = 0 |
| R3c | Backend bỏ Partner package/role/seed/security | R3b đạt | Backend tests/startup pass |
| R4a | Mobile bỏ nearby support route/home call | Chưa drop view/API | Client regression pass |
| R4b | Backend bỏ nearby support module | R4a/deprecation đạt | Không còn NearbySupport/API caller |
| R4c | Backend bỏ device/wearable module; giữ provenance | Provenance backfill ready | Context starts without device module |
| R5a | Mobile facility model chấp nhận thiếu `partnerId` | Cột vẫn tồn tại | Parser regression pass |
| R5b | Backend bỏ facility `partnerId` mapping/response | R5a/deprecation đạt | Facility smoke pass |
| R5c | Backend bỏ chat legacy fields/native methods | Columns vẫn tồn tại | Cursor/race tests pass |
| R6a/R6b | Reminder array compatibility rồi cutover | Reminder DB expand/backfill | No time-table reads/writes |
| R6c | Reminder observation release; giữ fallback/source artifacts | R6b | Observation gate §7.5 đạt |
| R7a/R7b | Appointment JSONB compatibility rồi cutover | Rules DB expand/backfill | Revision/JSON tests pass |
| R7c | Appointment observation release; giữ fallback/source artifacts | R7b | Observation gate §7.5 đạt |
| R8a/R8b | Safety typed-column compatibility rồi cutover | Users DB expand/backfill | Defaults/concurrency tests pass |
| R8c | Safety observation release; giữ fallback/source artifacts | R8b | Observation gate §7.5 đạt |
| R9a/R9b | Checklist compatibility rồi cutover | V2 rows reconcile = 0 unresolved | Response equality/source-independent tests |
| R9c | Checklist observation release; giữ legacy source artifacts | R9b | Observation gate §7.5 đạt |
| R10a/R10b | Consultation compatibility rồi cutover | Cardinality/paid gates = 0 | Booking-only lifecycle tests |
| R10c | Consultation observation release; giữ session source artifacts | R10b | Observation gate §7.5 đạt |
| R11a | Tạo common notification job entity/repository; tests | `notification_jobs` đã expand | Common repository tests pass |
| R11b | Freeze/final delta và switch planners/workers | Queue quiesced | Full job lifecycle observed |
| R11c | Job observation release; source queue read-only/frozen | R11b | Job-specific Gate §7.5 đạt |
| R12 | Xóa source entities/repositories còn lại sau mọi observation | Observation gates đạt | Static scan = 0 prohibited source references |
| R13 | DB contract cho **immediate objects only** | Tất cả code gates đạt | `ddl-auto: validate`, smoke/log pass |

Ký hiệu `Ra/Rb` là tối thiểu hai PR/release boundary, không phải một PR có hai commit. Partner,
notification jobs, checklist và consultation không nên cùng một release; growth không thuộc bảng
immediate này.

Growth dùng sequence độc lập `G0 prerequisite design → G1 DB expand → G2 compatibility code →
G3 backfill/cutover → G4 observation → G5 source cleanup/static/startup verification → G6 DB
contract`. R13 tuyệt đối không drop `growth_measurements` hoặc sửa persistence growth.

### 6.1. Hai deployment pattern

**Feature retirement:** client removal → backend endpoint/module removal → observation → DB drop.

**Persistence consolidation:** DB additive expand → compatibility code → backfill/reconcile →
target-only cutover → observation → xóa source mapping/query/entity → static/startup verification →
DB contract. Cleanup sau DB contract chỉ được là housekeeping không còn map/query object đã drop.

## 7. Test và verification plan

### 7.1. Backend

Mỗi PR chạy targeted tests của module; trước release chạy:

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd test
.\mvnw.cmd clean package
```

Test categories bắt buộc:

- Account: deactivation state/session/token/audit/login block.
- Security: locked response, admin unlock audit, role schema không có Partner.
- Direct chat: mark-read race/cursor path khi legacy columns vắng mặt.
- Reminder/appointment: array/JSON validation, revision, timezone/order.
- Jobs: Postgres claim concurrency, discriminator isolation, retry/stale lock, final-delta migration.
- Safety: typed defaults, fallback/cutover, permission/timestamp constraints, hot path.
- Checklist: parent/template/context, deterministic key, unresolved=0, response equality.
- Consultation: free-only/cardinality, lifecycle timestamp shape, source-table absent.
- Growth deferred: grouped identity, soft-delete, units và logical DTO stability.

Growth targeted tests chỉ bắt buộc trong sequence G0–G5; immediate releases vẫn có thể chạy full
regression nhưng không được mở rộng persistence scope để “làm test pass”.

`Postgresql18CanonicalSchemaIntegrationTest` và migration contract tests phải được cập nhật để
không tiếp tục yêu cầu object đã contract-drop, nhưng migration lịch sử đã apply không bị sửa.

### 7.2. WebApp

```powershell
cd 05_Development/CareBridgeWebApp
npm run test
npm run lint
npm run build
```

Targeted UI tests: blocked-account contact-CSKH, admin lock/unlock, routes without Partner/appeal,
auth redirect với mọi role còn hợp lệ.

### 7.3. MobileApp

```powershell
cd 05_Development/CareBridgeMobileApp
flutter analyze
flutter test
```

Targeted suites: auth/blocked account, expert home/router after nearby removal, reminder, safety,
checklist, consultation và healthRecords/growth.

### 7.4. Static và runtime gates

Sau cutover, scan production source cho:

- old table/column names;
- deleted entity/repository/controller class names;
- retired endpoints/routes;
- `PARTNER` enum/role;
- direct `settings_jsonb` removal attempts;
- accidental removal of reminder alias/cursor/device token code.

Runtime evidence:

- Backend starts với `ddl-auto: validate` trên target schema clone.
- Không có `relation/column does not exist`, invalid JSON/array cast hoặc constraint error.
- Query logs không còn source-table access trong observation window.
- Client smoke: login, deactivate, locked account, admin unlock, chat read, reminder scheduling,
  safety config, checklist action và consultation flow.

### 7.5. Gate định lượng mặc định

Change ticket có thể đặt gate chặt hơn; nếu muốn nới lỏng phải có Tech Lead/Release Owner duyệt.

- Backfill reconciliation: **0** missing, duplicate hoặc unresolved business identity.
- Observation gate: **0 active runtime source reads/writes** trong observation window. Source
  entity/repository/fallback artifacts có thể còn trong code nhưng không được gọi; chúng là rollback
  path và chỉ bị xóa tại R12.
- Contract-readiness gate sau R12: **0 prohibited source mapping/query/route references** và
  **0 runtime source queries**. Retained historical constants/tests phải nằm trong explicit allowlist.
- Schema errors: **0** `relation/column does not exist`, JSON/array cast hoặc new-constraint error.
- Low-risk observation: tối thiểu 24 giờ và một normal deployment cycle.
- Notification jobs: tối thiểu hai planner cycles, một retry path và một stale-lock/requeue path;
  không duplicate notification/job identity.
- Queue quiesced: source planners disabled, `PROCESSING = 0`, source counts không đổi ở hai lần
  kiểm tra cách nhau tối thiểu 5 phút trước final delta.
- Breaking client removal: minimum supported client version đã chứa cleanup, hoặc change ticket
  xác nhận không có external client.
- Target-schema rehearsal: clone từ logical dump/snapshot gần production, apply toàn bộ Flyway từ
  đầu và chạy startup/full tests trước release.

Rollback ngay khi có bất kỳ data mismatch, duplicate job/notification, auth deactivation/lock
regression, checklist identity drift hoặc consultation cardinality violation.

## 8. Definition of Done theo PR

Một PR chỉ hoàn tất khi:

1. Scope liên kết rõ tới V3 section và release wave.
2. File inventory được chạy lại trên current `HEAD`.
3. API change được đánh dấu breaking/stable và client dependency được xử lý.
4. Code chạy được trên schema tương ứng của wave; không giả định contract-drop sớm.
5. Source/target write strategy và rollback path có test.
6. Sau target-only cutover, source code path không còn được gọi; source artifacts chỉ giữ làm
   rollback trong observation và phải được xóa tại R12 trước DB contract.
7. Targeted tests và stack validation commands pass.
8. Logs/static scan evidence được đính kèm PR.
9. Không sửa migration history, không `DROP ... CASCADE`, không xóa retained object.
10. Contract migration vẫn nằm ở PR/release riêng và có approval của DB/release owner.

DoD bổ sung cho breaking feature removal:

- Client min-version/deprecation decision đã chốt.
- Navigation, deep link, background call và cached auth state không còn gọi endpoint cũ.
- Docs/use cases được cập nhật hoặc đánh dấu obsolete.

## 9. File-impact inventory

Inventory dưới đây là điểm bắt đầu, không phải danh sách đóng. Dấu `/**` nghĩa là rà toàn package.

| Action | Path/pattern chính |
| --- | --- |
| Xóa | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/account/**` |
| Xóa | Appeal controllers/service/entity/repository/DTO trong `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/{security,identity/admin}` |
| Sửa | Các class security/admin dưới `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/**` |
| Xóa | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/device/**` |
| Xóa | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/wearable/**` |
| Sửa immediate | Backend `health/entity/HealthObservation.java`: bỏ riêng mapping `device_connection_id`, giữ provenance |
| Xóa | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/partner/**` |
| Xóa | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/nearbycare/**` |
| Sửa | Backend `.../map/` facility entity/DTO/mapper/service; Mobile `05_Development/CareBridgeMobileApp/lib/features/emergency/models/care_facility_model.dart` |
| Sửa | Backend `.../directchat/entity/DirectConversation.java`, `DirectConversationRepository.java` |
| Sửa/Xóa | Backend `.../reminder/schedule/` time entity/repo/service/processing |
| Sửa/Xóa | Backend `.../reminder/notification/` config/rule/job entity/repo/service/job packages |
| Sửa/Xóa | Backend `.../safety/` config entity/repo/service/mapper; giữ event persistence |
| Sửa/Xóa | Backend `.../checklist/` legacy entity/repo/controller/service; giữ v2 distribution/today/history |
| Sửa/Xóa | Backend `ConsultationBooking`, `ConsultationSession` và repository/report caller |
| Deferred | Backend `.../carejourney/` growth persistence; chỉ phần `measurement_group_id`/BABY support của `HealthObservation` |
| Xóa Web | `05_Development/CareBridgeWebApp/src/features/partnerGovernance/**` và account-appeal pages/API |
| Sửa Web | Router, auth role store/routes, admin layout/dashboard/user pages, blocked account page |
| Xóa Mobile | `05_Development/CareBridgeMobileApp/lib/` legacy delete sheet; expert nearby screen/route/home call |
| Sửa Mobile | Auth service/blocked state/screen; facility model |
| Giữ clients | Reminder, safety, checklist, consultation và growth models/UI nếu API contract ổn định |

Static scans tối thiểu trước mỗi implementation PR:

```powershell
rg -n "AccountDeletionRequest|account_deletion_requests|AccountLockAppeal|account_lock_appeals" 05_Development
rg -n "PartnerOrganization|PARTNER|partner_organizations" 05_Development
rg -n "device_connections|HealthDeviceConnection|nearby_support_interactions|NearbySupport" 05_Development
rg -n "mother_last_read_at|expert_last_read_at|ReminderScheduleTime|AppointmentNotificationRule" 05_Development
rg -n "ReminderScheduleJob|AppointmentNotificationJob|SafetyMonitoringConfig|UserChecklistItem|ConsultationSession|GrowthMeasurement" 05_Development
```

Kết quả scan phải được **phân loại**, không áp dụng zero tuyệt đối cho mọi chuỗi:

- Zero bắt buộc: `Role.PARTNER`, Partner runtime package/route/security rule/seed; retired endpoint
  callers; source entity/repository/table/column query sau R12.
- Allowlist: deprecated Partner `AuditAction` constants phục vụ historical audit deserialization;
  migration history, V2/V3/plan và tests chứng minh old value bị reject/retained đúng mục đích.
- Mọi allowlist entry phải ghi file, symbol và lý do; chuỗi mới ngoài allowlist làm gate fail.

Docs phải cập nhật/obsolete: UC66–69, UC130, UC156, UC114 và Partner UC118–125.

## 10. Deferred work và non-goals

- Không viết/chạy production migration trong tài liệu này.
- Không drop hoặc chuẩn hóa toàn bộ `settings_jsonb`.
- Không chuyển `reminder_occurrence_aliases` thành view.
- Không gộp `direct_conversation_read_cursors`.
- Không xóa `device_tokens` hay push notification support.
- Không xóa Partner `AuditAction` historical constants trong immediate program.
- Không tách `safety_events` trong immediate program.
- Không chuyển growth persistence trước khi Wave 8 prerequisites đạt.
- Không thay đổi public DTO cho persistence-only refactor nếu không có lý do sản phẩm.
- Không xóa care-facility nearby map/search khi retire nearby support requests.
- Không sửa Flyway migration history đã apply; chỉ thêm migration mới và cập nhật expectations
  của tests phù hợp target schema.

---

## 11. Nguồn sự thật và handoff

- Database decisions/gates: `08_References/Database_Table_Audit_And_Consolidation V3.md`.
- Audit detail và code-call counts: `08_References/Database_Table_Audit_And_Consolidation V2.md`.
- Repository standards/build commands: root `AGENTS.md`.
- Current source and live Supabase catalog tại thời điểm implementation luôn thắng stale inventory.

Trước khi bắt đầu từng workstream, tạo migration matrix/change ticket riêng và trích đúng V3
section, source-code section, expected tests, rollback path và contract gate.
