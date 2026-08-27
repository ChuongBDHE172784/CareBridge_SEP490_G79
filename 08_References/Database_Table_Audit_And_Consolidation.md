# CareBridge — Kiểm toán bảng Database & Phương án gộp / bỏ

**Ngày:** 2026-08-05
**Nhánh:** `LamVH1` @ `f4c0642e`
**Nguồn dữ liệu:** truy vấn **trực tiếp** Supabase production-shared (`wqsunmakzdaxwyknegkq`, pooler `aws-1-ap-northeast-1:5432`) + quét **1.673 file Java**, toàn bộ WebApp / MobileApp / AITriageService / Firebase
**Trạng thái:** không phải phân tích tĩnh — mọi số cột, số dòng, FK, trigger, view dưới đây đọc từ DB thật

> Báo cáo này **thay thế** hai file cũ `Database_Table_Consolidation_Analysis.md` và
> `Partner_Module_Removal_Plan.md` (cả hai là phân tích tĩnh từ migration, có vài số
> liệu lệch so với DB thật — xem mục 8).

---

## 1. Con số tổng quan

| Hạng mục | Số lượng | Ghi chú |
| --- | --- | --- |
| **Bảng trong schema `public`** | **75** | 74 bảng nghiệp vụ + `flyway_schema_history` |
| View tương thích | 4 | `care_logs`, `expert_credentials`, `emergency_contacts`, `nearby_support_interactions` |
| Ràng buộc khóa ngoại | 169 | đếm qua `pg_constraint`, không phải `information_schema` |
| Trigger | 26 | gồm 4 trigger `INSTEAD OF` cho view và 6 trigger bất biến |
| JPA entity `@Table` | 110 | trỏ vào **69 bảng** + 3 view |
| Repository Spring Data | 131 | |
| Migration Flyway đã apply | 25 | version mới nhất `20260804160000` |
| Migration file trong repo | 27 | 25 versioned + 2 finalizer thủ công |

Bảng hệ thống của Supabase (`auth` 23, `storage` 8, `realtime` 3, `vault` 1) **không**
tính vào đây — chúng do nền tảng quản lý, không đụng tới.

### Vì sao 110 entity mà chỉ 69 bảng

Đây là điểm quan trọng nhất của toàn bộ báo cáo: **dự án đã gộp bảng từ trước rồi.**
Nhiều entity Java cùng trỏ vào một bảng vật lý, phân biệt nhau bằng cột phân loại:

| Bảng | Số entity | Các entity |
| --- | --- | --- |
| `safety_events` | 8 | `SafetyEvent`, `EmergencySession`, `EmergencyAlertAttempt`, `EmergencyAlertDelivery`, `EmergencyMapHandoff`, `FamilyAlertLog`, `LocationSnapshot`, `SafetyEventResponseRecord` |
| `audit_events` | 6 | `AuditLog`, `ModerationAction`, `ContributionPoint`, `MotherBaselineContext`, `MotherJourneyTransition`, `PregnancyOutcomeEvidence` |
| `health_observations` | 6 | `HealthObservation`, `MaternalHealthMetric`, `PostpartumLog`, `DeviceMeasurement`, `ExerciseSafetyCheck`, `PostureFeedbackEvent` |
| `users` | 5 | `User`, `Person`, `UserProfile`, `ExpertProfile`, `CommunityProfile` |
| `community_interactions` | 5 | `CommunityAnswerLike`, `CommunityQuestionLike`, `CommunityBookmark`, `UserTopicFollow`, `QuestionNotificationMute` |
| `care_item_templates` | 4 | `ChecklistTemplate`, `ChecklistItem`, `PregnancyExercise`, `PostureAnalysisConfig` |
| `care_tasks` | 3 | `CareTask` ×2 package, `Reminder` |
| `data_permissions` | 3 | `ConsentGrant`, `DataPermission`, `TriageDisclaimerConsent` |
| `auth_challenges` | 2 | `OtpVerification`, `PasswordResetToken` |
| `community_content` | 2 | `CommunityQuestion`, `CommunityAnswer` |
| `health_records` | 2 | `HealthRecord`, `HealthSummary` |
| `triage_sessions` | 2 | `IntakeSession`, `StructuredIntakeData` |

**Hệ quả cho việc gộp tiếp:** dư địa gộp còn lại **hẹp hơn nhiều** so với những gì
danh sách 75 bảng gợi ý. Phần lớn thứ dễ gộp đã gộp xong. Những gì còn lại hoặc là
bảng con thuần túy, hoặc là bảng có lý do pháp lý để đứng riêng.

### Pattern gộp mà dự án đang dùng

Cách làm đã được kiểm chứng, và **mọi đề xuất ở mục 5 phải đi theo đúng pattern này**:

> cột phân loại (`*_type` / `*_category`) + JSONB metadata + **view tương thích có
> trigger `INSTEAD OF`** để code Java cũ vẫn `SELECT`/`INSERT` như bảng chưa bị xóa.

| View | Thực chất đọc từ | Điều kiện | Dòng thật |
| --- | --- | --- | --- |
| `care_logs` | `care_tasks` | `task_type = 'CARE_LOG'` | 31 |
| `expert_credentials` | `attachments` | `attachment_category = 'EXPERT_CREDENTIAL'` | 2 |
| `emergency_contacts` | `care_group_members` ⋈ `care_groups` ⋈ `users` | `is_emergency_contact` | 2 |
| `nearby_support_interactions` | *(không gì cả)* | `WHERE false` | 0 |

---

## 2. Chỗ nào trong code đang connect tới SQL

Đây là phần cần kiểm kỹ nhất trước khi đụng schema. Kết quả quét toàn repo:

### 2.1. Chỉ có **một** service nói chuyện với Postgres

| Module | Kết nối DB? | Bằng chứng |
| --- | --- | --- |
| **`CareBridgeAPI`** (Spring Boot) | ✅ **Có — duy nhất** | `org.postgresql.Driver`, Hikari pool, Flyway, 131 repository |
| `CareBridgeAITriageService` (FastAPI) | ❌ Không | `requirements.txt` chỉ có `fastapi, uvicorn, pydantic, langgraph, langchain-core, google-genai, httpx…` — **không có** `psycopg`/`sqlalchemy`/`asyncpg`/`supabase` |
| `CareBridgeWebApp` (React) | ❌ Không | `package.json` không có `@supabase/supabase-js` hay driver nào. Gọi backend qua `axios`. |
| `CareBridgeMobileApp` (Flutter) | ❌ Không | `pubspec.yaml` không có `supabase_flutter`/`postgres`. Lưu cục bộ bằng `flutter_secure_storage`. |
| `MachineLearning` | ❌ Không | không có driver DB |
| `Firebase` | ⚠️ Datastore **khác** | Firestore, chỉ 1 collection `userConversationEvents/{uid}/events/{eventId}` cho signaling gọi/chat. Không liên quan Postgres. |

> ⚠️ `CareBridgeWebApp/src/vite-env.d.ts` khai báo `VITE_SUPABASE_URL` và
> `VITE_SUPABASE_ANON_KEY`, nhưng **không file nào trong `src/` đọc hai biến này**.
> Đây là khai báo thừa. Không có đường đi trực tiếp từ trình duyệt xuống Postgres —
> tốt cho bảo mật, và có nghĩa là **đổi schema chỉ ảnh hưởng backend**.

### 2.2. Backend nối vào DB bằng ba đường

| Đường | Vị trí | Ghi chú |
| --- | --- | --- |
| **1. JPA/Hibernate** | 110 entity, 131 repository | `ddl-auto: validate` — đổi schema mà không đổi entity sẽ **fail khi khởi động**, không âm thầm hỏng |
| **2. Native SQL** | **221 chỗ** trong **73 file** (`nativeQuery = true`, `createNativeQuery`, `JdbcTemplate`, `@Modifying`) | Đây là rủi ro lớn nhất khi gộp bảng: Hibernate **không** validate được các câu này |
| **3. Datasource thứ hai** | `ChecklistRetentionDataSourceConfiguration` | Pool riêng, credential riêng (`CAREBRIDGE_CHECKLIST_RETENTION_DB_*`), có cả `ChecklistRetentionOwnerIsolationVerifier`. Mặc định tắt. |

Ngoài ra `RuntimeDatasourceEnvironmentPostProcessor` bơm credential runtime, và
Flyway chạy bằng user riêng (`CAREBRIDGE_FLYWAY_DB_USERNAME`) tách khỏi user runtime.

### 2.3. Logic nghiệp vụ nằm trong DB, không phải trong Java

26 trigger — bỏ sót nhóm này khi migrate là hỏng dữ liệu âm thầm:

| Nhóm | Trigger | Ý nghĩa |
| --- | --- | --- |
| **Bất biến** | `audit_events_immutable_trg`, `triage_session_evidence_immutable_trg`, `knowledge_source_reviews_immutable_trg`, `trg_consultation_context_shares_append_only`, `trg_consultation_context_citations_append_only` | Chặn `UPDATE`/`DELETE` ở tầng DB |
| **Guard checklist** | 7 trigger trên `care_item_templates`, 2 trên `checklist_instances`, 2 trên `checklist_task_instances`, 2 trên `checklist_action_commands` | Validate template đã duyệt, retention, target |
| **View ghi được** | `care_logs_view_write_trg`, `expert_credentials_view_write_trg`, `emergency_contacts_view_write_trg` | `INSTEAD OF INSERT/UPDATE/DELETE` |
| **Sinh dữ liệu** | `care_tasks_reminder_occurrence_alias_trg` | `AFTER INSERT/UPDATE` trên `care_tasks` → ghi vào `reminder_occurrence_aliases` |
| **Khóa tính năng** | `nearby_support_interactions_disabled_trg` | Chặn mọi ghi — tính năng đã khai tử |

---

## 3. BỎ — module Partner

Bạn không phát triển Partner nữa. Kiểm chứng trên DB thật:

### 3.1. Bảng phải bỏ

| Bảng | Cột | Dòng thật | FK đi vào |
| --- | --- | --- | --- |
| **`partner_organizations`** | 14 | **2** (đều là seed) | **0 — không bảng nào trỏ vào** |

Hai dòng đang có: *Hội Sản Phụ Khoa Việt Nam*, *Hội Nhi Khoa Cần Thơ* — cả hai từ
`V2__seed_reference_data.sql`. **Không có dữ liệu thật.**

FK duy nhất là đi **ra**: `representative_user_id → users(user_id)`. Xóa bảng không
kéo theo bảng nào.

### 3.2. Ba hiểu lầm cần đính chính

**a) `care_facilities.partner_id` KHÔNG phải khóa ngoại tới Partner.**

```
care_facilities_partner_archive_fk : care_facilities.partner_id → archived_records.archive_id
```

Tên cột nói Partner, ràng buộc trỏ `archived_records`. Đã kiểm tra:
`SELECT count(*) FROM care_facilities WHERE partner_id IS NOT NULL` → **0**.
Xóa `partner_organizations` **không** ảnh hưởng `care_facilities`. Nhưng cột này là
một lỗi đặt sai constraint từ trước → **xử lý ở migration riêng**, đừng trộn vào lần gỡ Partner.

**b) `audit_events` KHÔNG có cột `action`.** Kế hoạch cũ đề xuất chạy
`WHERE action LIKE 'PARTNER_%'` — câu đó lỗi. Bảng dùng `event_category` +
`resource_type`. Đã kiểm tra đúng cột:

```sql
SELECT count(*) FROM audit_events
WHERE event_category ILIKE '%PARTNER%' OR resource_type ILIKE '%PARTNER%';
-- => 0
```

**Không có bản ghi audit Partner nào.** Nên 7 giá trị `PARTNER_*` trong enum
`AuditAction` **xóa được an toàn** — không có dữ liệu cũ nào cần đọc lại. (Kế hoạch
cũ khuyên giữ lại; khuyến nghị đó dựa trên giả định sai.)

**c) Có 1 tài khoản đang mang role `PARTNER`:**

```
50337ac8-7bbd-4b03-a5e9-580a6eed582a — "Partner Test" — role=PARTNER
```

Phân bố role hiện tại: `MOTHER` 20, `FAMILY` 6, `NULL` 6, `EXPERT` 5,
`SYSTEM_ADMIN` 2, `MODERATOR` 1, `CONTENT_ADMIN` 1, **`PARTNER` 1**.
Là tài khoản test, xử lý được.

### 3.3. Khối lượng code phải gỡ

| Tầng | Số file | Cách xử lý |
| --- | --- | --- |
| Backend `partner/` (main) | 20 | Xóa cả package |
| Backend `partner/` (test) | 8 | Xóa cả package |
| Backend main — file khác nhắc PARTNER | 9 | **Sửa**, không xóa |
| Backend test — file khác nhắc PARTNER | 19 | **Sửa** — phần tốn công nhất |
| WebApp `features/partnerGovernance/` | 12 | Xóa cả thư mục |
| WebApp — file khác nhắc partner | 9 | **Sửa** |
| Mobile | 1 | `care_facility_model.dart` chỉ có field `partnerId` — chờ xử lý cột ở 3.2a |

**Tổng: 78 file.** Bảng thì chỉ 1, nhưng công sức nằm hết ở tầng code — đặc biệt 19
file test liệt kê toàn bộ enum `Role`, sẽ không biên dịch được sau khi bỏ giá trị `PARTNER`.

### 3.4. Migration đề xuất

```sql
-- V20260806000000__remove_partner_module.sql
-- Không bảng nào FK vào partner_organizations → DROP trực tiếp là đủ.
DROP TABLE IF EXISTS public.partner_organizations;

-- users.role là varchar(50), không phải enum type → không cần ALTER TYPE.
UPDATE public.users SET role = NULL WHERE role = 'PARTNER';   -- 1 dòng
```

**Không** đụng `audit_events` (0 dòng liên quan, và bảng có trigger chặn `DELETE`).
**Không** đụng `care_facilities.partner_id` trong migration này.

---

## 4. GỘP ĐƯỢC — ưu tiên cao

Xếp theo tỉ lệ lợi ích / rủi ro.

### 4.1. `direct_conversations` — trạng thái đã đọc bị lưu ở hai nơi ⚠️

| Bảng | Cột | Dòng |
| --- | --- | --- |
| `direct_conversations` | 10 (có 4 cột read legacy) | 1 |
| `direct_conversation_read_cursors` | 6 | **0** |

`direct_conversations` giữ `mother_last_read_at`, `mother_last_read_message_id`,
`expert_last_read_at`, `expert_last_read_message_id`. Migration
`V20260801100001` đã tạo bảng cursor và backfill từ chính 4 cột đó.

**Bảng cursor đang có 0 dòng** trong khi 4 cột legacy vẫn được map trong
`DirectConversation.java` → hiện code vẫn đọc/ghi đường cũ, bảng mới nằm không.

**Đề xuất:** bỏ 4 cột legacy, chỉ dùng bảng cursor.
**Vì sao:** 4 cột chỉ mô hình hóa được đúng 2 vai (mother/expert), không mở rộng cho
thành viên gia đình. Bảng cursor là dạng chuẩn, hỗ trợ N người đọc.
**Rủi ro: thấp nhất trong toàn bộ danh sách** — 1 dòng dữ liệu, và đây đang là nguồn
sai lệch tiềm tàng. **Làm trước tiên.**

### 4.2. Hai bảng job gần như trùng khít → `notification_jobs`

| Bảng | Cột | Dòng |
| --- | --- | --- |
| `reminder_schedule_jobs` | 16 | **221** |
| `appointment_notification_jobs` | 17 | 10 |

**11/16 và 11/17 cột giống hệt nhau:**

```
job_id, due_at, status, attempt_count, next_attempt_at, locked_by, locked_at,
notification_record_id, last_error_code, created_at, updated_at
```

Chỉ khác phần nguồn:
- `reminder_schedule_jobs`: `schedule_id, schedule_revision, occurrence_date, local_time, time_zone`
- `appointment_notification_jobs`: `reminder_id, occurrence_id, occurrence_generation, occurrence_scheduled_at, config_revision, offset_minutes`

**Đề xuất:** một bảng `notification_jobs` với `job_type IN ('REMINDER','APPOINTMENT')`,
`source_type` + `source_id` thay 2 FK, phần đặc thù vào JSONB.

**Vì sao gộp:** đây không phải để bớt 1 bảng. Hiện có **hai worker và hai planner
riêng** (`ReminderScheduleWorker`, `AppointmentNotificationWorker`) cài lại **cùng một
logic** retry / lease-locking / stale-detection, cùng hai bộ cờ cấu hình
`REMINDER_SCHEDULE_*` và `APPOINTMENT_NOTIFICATION_*`. Gộp bảng cho phép gộp worker —
đó mới là lợi ích thật.

**Rủi ro: trung bình-cao.** 221 job đang sống, phải chạy khi queue rỗng.

### 4.3. `appointment_notification_configs` + `appointment_notification_rules` → 1 bảng

| Bảng | Cột | Dòng | Nội dung |
| --- | --- | --- | --- |
| `appointment_notification_configs` | **5** | 5 | `reminder_id, time_zone, config_revision, created_at, updated_at` |
| `appointment_notification_rules` | 6 | 20 | `rule_id, reminder_id, offset_minutes, sort_order, created_at, updated_at` |

Config có PK là `reminder_id` → quan hệ **1-1 tuyệt đối** với `care_tasks`. Rules chỉ
là danh sách offset có thứ tự. Cả hai đều 0 FK đi vào.

**Đề xuất:** một bảng, list offset đưa vào `offsets_jsonb`. Triệt để hơn: nhét cả hai
vào `care_tasks.metadata_jsonb` — `care_tasks` vốn đã là đích của pattern gộp.
**Vì sao:** một bảng 5 cột chỉ để giữ 1 timezone và 1 số revision không xứng là bảng riêng.
**Rủi ro: thấp.** Phải giữ `config_revision` vì job đang chạy tham chiếu tới nó.

### 4.4. `reminder_schedule_times` → mảng trên `reminder_schedules`

4 cột (`time_id, schedule_id, local_time, sort_order`), 6 dòng, 0 FK đi vào,
không có vòng đời độc lập.

**Đề xuất:** `local_times TIME[]` hoặc JSONB trên `reminder_schedules`.
**Vì sao:** bảng con thuần túy — mỗi lần đọc lịch nhắc đều phải JOIN thêm một bảng chỉ
để lấy vài mốc giờ.
**Đánh đổi:** mất UNIQUE `(schedule_id, local_time)` ở tầng DB, phải chuyển lên
application. Chấp nhận được — không phải dữ liệu tiền hay pháp lý.

### 4.5. `growth_measurements` → `health_observations`

| Bảng | Cột | Dòng |
| --- | --- | --- |
| `growth_measurements` | 12 | **8** |
| `health_observations` | 25 | 513 |

`health_observations` đã có đủ bộ cột cần thiết: `observation_type`, `value_numeric`,
`value_secondary`, `unit`, `observed_at`, `care_subject_id`, `source_type`,
`raw_payload_jsonb`, `observation_shape`. `growth_measurements` chỉ chứa 3 phép đo
(`weight_kg`, `height_cm`, `head_circumference_cm`) = 3 giá trị `observation_type`.

**Đề xuất:** gộp vào `health_observations`, để lại **view `growth_measurements`** đúng
như `care_logs` đang làm.
**Vì sao:** nhóm đang đi đúng hướng này rồi — `V20260804100000__retire_standalone_weight_metric`
và `V20260804090000__replace_maternal_heart_rate_with_bmi` đều là hợp nhất chỉ số sức
khỏe. Đây là bước tiếp theo tự nhiên. 8 dòng dữ liệu, migrate rất nhẹ.
**Lưu ý:** `growth_measurements` có `deleted_at` (soft delete) mà `health_observations`
không có — phải quyết cách xử lý trước.

### 4.6. `preparation_checklist_items` → `checklist_task_instances`

| Bảng | Cột | Dòng |
| --- | --- | --- |
| `preparation_checklist_items` | 13 | **2** |
| `checklist_task_instances` | 20 | 144 |

So cột:

```
preparation_checklist_items : checklist_item_id, owner_user_id, mother_journey_id,
                              template_entry_id, title, display_order, status,
                              due_at, completed_at, category, baby_id, created_at, updated_at
checklist_task_instances    : ..., title_snapshot, display_order, status, due_at,
                              completed_at, category, target_subject, ...
```

Trùng khái niệm gần như hoàn toàn: *một mục checklist gán cho người dùng, có
title / display_order / status / due_at / completed_at / category*.

**Vì sao gộp:** đây là **tàn dư checklist v1** sống sót qua đợt hợp nhất v2
(`V20260731020000__prepare_checklist_schema_simplification`,
`V20260731030000__retire_checklist_support_tables`). Hệ thống đang có **hai mô hình
checklist song song** với 2 dòng dữ liệu ở mô hình cũ, nhưng vẫn nuôi đủ
controller + service + repository + exception handler riêng.
**Rủi ro: thấp** về dữ liệu (2 dòng), **trung bình** về code — phải bỏ
`UserChecklistItemController` và ghép vào luồng checklist v2.

### 4.7. `consultation_sessions` → `consultation_bookings`

| Bảng | Cột | Dòng | FK đi vào |
| --- | --- | --- | --- |
| `consultation_sessions` | 9 | 1 | 0 |
| `consultation_bookings` | 17 | 1 | — |

Quan hệ 1-1: mỗi booking tối đa một session.
**Đề xuất:** gộp, các cột session để `NULL` khi booking bị hủy trước giờ.
**Đánh đổi:** `consultation_bookings` 17 → 25 cột. Chấp nhận được (`users` đang 52 cột).

### 4.8. `safety_configs` → `users.settings_jsonb`

10 cột, **1 dòng**, khóa theo `user_id`, quan hệ 1-1, 0 FK đi vào.

**Đề xuất:** đưa vào `users.settings_jsonb` — **cột này đã tồn tại sẵn** trong `users`.
**Vì sao:** một bảng riêng cho 1 dòng thiết lập 1-1 là chi phí thuần túy.
**Đánh đổi:** mất kiểu chặt cho `countdown_seconds` (int) và `sensitivity_level`.
Nếu cần validate chặt thì dùng JSONB CHECK constraint.

### 4.9. `reminder_occurrence_aliases` → nên là VIEW, không phải bảng

6 cột, 15 dòng, 0 FK đi vào. Được **sinh tự động** bởi trigger
`care_tasks_reminder_occurrence_alias_trg` (`AFTER INSERT/UPDATE` trên `care_tasks`).

**Đề xuất:** đây là dữ liệu **dẫn xuất 100%** từ `care_tasks`. Thay bảng + trigger bằng
một view (hoặc materialized view nếu cần tốc độ).
**Vì sao:** bảng dẫn xuất luôn có nguy cơ lệch với nguồn khi trigger lỗi hoặc khi ai đó
ghi thẳng vào `care_tasks` bằng `COPY`/migration.
**Rủi ro: thấp** — nhưng phải kiểm tra `ReminderOccurrenceAlias` entity có ghi trực tiếp không.

---

## 5. Gộp được nhưng nên dừng lại cân nhắc

| Bảng | Gộp vào | Lý do nên cân nhắc |
| --- | --- | --- |
| `content_item_topics` (3 cột, 3 dòng) | mảng trên `content_items` | Đang là `@ElementCollection` của `ContentItem` **và** bị 3 câu native SQL trong `ContentRepository` join vào. Chuyển sang mảng là mất FK và index kém đi khi lọc theo topic. |
| `content_item_sources` (8 cột, 24 dòng) | JSONB trên `content_items` | Cũng là `@ElementCollection`. Có FK tới `knowledge_sources` — chuyển sang JSONB là mất liên kết đó. |
| `professional_specialties` (4 cột, 2 dòng) | mảng `specialty_ids` trên `users` | `users` **đã có** cột `specialty_ids`. Tức là dữ liệu này có thể đang lưu 2 nơi → cần kiểm tra trước. Nhưng `is_primary` là thuộc tính của *quan hệ*, không của bên nào. |
| `ai_content_scan_jobs` (14) + `ai_content_assessments` (23) | 1 bảng | Một job có thể sinh nhiều assessment khi rescan → gộp sẽ lặp dữ liệu job. |
| `vaccination_records` + `development_milestones` | `health_observations` | Hai bảng mang ngữ nghĩa **lịch trình và trạng thái** (`scheduled_date`, `postpone_reason`, `milestone_status`), không chỉ là phép đo. Gộp làm `health_observations` mất tính thuần nhất. |
| `vaccination_schedules` (9 cột, 7 dòng) | *giữ nguyên* | Là **catalog có version** (`schedule_version`, `active_from`, `active_to`) — khác bản chất với `vaccination_records` (dữ liệu bệnh nhân). |
| `expense_entries` (11 cột, 2 dòng) | `care_tasks` | Về lý thuyết gộp được qua `item_type`, nhưng chi tiêu là dữ liệu tiền — trộn vào bảng task làm mờ ranh giới nghiệp vụ. Không đáng. |

---

## 6. KHÔNG gộp được

### 6.1. Có trigger bất biến ở tầng DB — gộp là vi phạm ràng buộc

| Bảng | Cột | Dòng | Trigger chặn |
| --- | --- | --- | --- |
| `audit_events` | 34 | **3.750** | `audit_events_immutable_trg` (BEFORE UPDATE, DELETE) |
| `triage_session_evidence` | 12 | 6 | `triage_session_evidence_immutable_trg` |
| `knowledge_source_reviews` | 8 | 1 | `knowledge_source_reviews_immutable_trg` |
| `consultation_context_shares` | 16 | 1 | `trg_..._append_only` |
| `consultation_context_citations` | 9 | 1 | `trg_..._append_only` |

`audit_events` còn có cột **`legal_hold`** — bảng chịu ràng buộc pháp lý. Ngoài ra
6 entity Java đã dùng chung bảng này rồi; đây là **đích gộp**, không phải nguồn.

### 6.2. Ràng buộc pháp lý / truy vết

| Bảng | Lý do |
| --- | --- |
| `triage_sessions` (36 cột, 18 dòng) | Có `content_hash`, `schema_version`, `disclaimer_version` + trigger `triage_completed_snapshot_guard_trg`. Là **bằng chứng phiên bản** của lời khuyên y tế đã hiển thị cho người dùng. Gộp = mất khả năng chứng minh. |
| `data_permissions` (24 cột, 27 dòng) | Bảng đồng ý / phân quyền, phải tách để audit và thu hồi độc lập. Đã là đích gộp của 3 entity. |
| `account_deletion_requests` | Quy trình xóa tài khoản theo quy định, có `scheduled_for`, `processed_by`. |
| `moderation_cases` (21 cột, 10 dòng) | Hồ sơ xử lý vi phạm, cần vòng đời và quyền truy cập riêng. |

### 6.3. Khác vòng đời hoặc khác cấp bảo mật

| Cặp bảng | Vì sao không gộp |
| --- | --- |
| `device_tokens` (7 cột, 25 dòng) vs `device_connections` (11 cột, 1 dòng) | Token FCM xoay vòng liên tục, không nhạy cảm. `device_connections` giữ **OAuth token thiết bị đeo** (`token_reference`, `scopes_jsonb`, `consent_granted_at`) — cấp bảo mật và vòng đời hoàn toàn khác. |
| `auth_sessions` (19 cột, 102 dòng) vs `auth_challenges` (13 cột, 14 dòng) | Session sống nhiều ngày và có `refresh_token_hash`, `detected_reuse`. Challenge sống vài phút rồi hết hạn. Gộp làm bảng phình vô ích và khó dọn rác. |
| `expert_availability` vs `expert_location_shares` | Một bên là khung giờ rảnh; một bên là tọa độ GPS có `expires_at` + `consent_reference`. Không chung ngữ nghĩa, không chung chính sách xóa. |
| `knowledge_sources` (15 cột) vs `knowledge_source_reviews` (8 cột) | Quan hệ 1-N; gộp sẽ lặp toàn bộ metadata nguồn. Thêm nữa review có trigger bất biến. |
| `care_groups` vs `care_group_members` | 1-N kinh điển; `care_group_members` còn là nguồn của view `emergency_contacts`. |

### 6.4. Đã là bảng đích của việc gộp — gộp thêm là quá tải

`users` (52 cột, 5 entity, **71 FK đi vào** — hub lớn nhất), `care_tasks` (31 cột, 3 entity),
`health_observations` (25 cột, 6 entity), `attachments`, `care_subjects` (17 FK đi vào),
`care_item_templates` (56 cột, 4 entity, 7 trigger guard), `community_interactions` (5 entity).

Các bảng này đã hấp thụ bảng khác hoặc là hub được cả hệ thống tham chiếu. Thêm nữa
sẽ vượt ngưỡng dễ bảo trì.

### 6.5. Bảng hệ thống

`flyway_schema_history` — Flyway quản lý, **tuyệt đối không đụng**.

---

## 7. Hai vấn đề ngược lại — nên TÁCH / nên XÓA CODE

### 7.1. `safety_events` — 81 cột, 5.976 dòng, 5,4 MB → nên TÁCH

Bảng lớn nhất hệ thống về mọi mặt. Đang gánh **8 entity** và gom lẫn:

- Dữ liệu cảm biến thô: `peak_acceleration`, `angular_velocity`, `inactivity_seconds`, `magnitude`, `accuracy_meters`
- Vị trí: `latitude`, `longitude`, `user_latitude`, `user_longitude`, `location_snapshot_jsonb`
- Vòng đời cảnh báo: `alert_generation`, `alert_status`, `alert_claim_token`, `alert_lease_expires_at`, `alert_claimed_at`, …
- Gửi thông báo: `fcm_message_id`, `device_token_id`, `delivery_status`, `delivered_at`, `failure_code`
- **Sáu cột phân loại khác nhau**: `event_type`, `response_type`, `record_type`, `action_type`, `actor_type`, `context_type`

Sáu cột `*_type` trong cùng một bảng là dấu hiệu rõ ràng bảng đang gánh nhiều thực thể
khác nhau. Còn có cả cặp trùng lặp `latitude`/`user_latitude`, `started_at`/`completed_at`
song song với `alert_*_at`.

**Đề xuất:** tách phần cảm biến + phần delivery ra bảng riêng (hoặc JSONB), giữ lại
phần sự kiện và phản hồi. **Đây là bảng duy nhất trong hệ thống mà việc tách mang lại
lợi ích rõ hơn việc gộp.**

### 7.2. `nearby_support_interactions` — tính năng chết nhưng code còn sống

View định nghĩa là `SELECT NULL::uuid, … WHERE false` — **luôn trả về 0 dòng**, cộng
trigger `nearby_support_interactions_disabled_trg` chặn mọi ghi.

Nhưng package `nearbycare/` vẫn còn **12 file** đầy đủ: controller, 2 entity, 2 repository,
service impl, mapper, 2 request DTO, 2 response DTO.

**Đề xuất:** xóa cả package `com/carebridge/backend/nearbycare/` và view. Không phải
việc gộp bảng — là dọn code chết. Endpoint hiện tại luôn trả rỗng hoặc ném lỗi khi ghi.

---

## 8. Bảng nghi mồ côi — `archived_records`

| Bảng | Cột | Dòng | Entity | Native SQL |
| --- | --- | --- | --- | --- |
| `archived_records` | 11 | 1 | **0** | **0** |

**Không có một dòng code nào trong `src/main/java` chạm vào bảng này.** Chỉ xuất hiện ở:
- `V1__init_schema.sql`, `V2__seed_reference_data.sql`
- 4 file test (`HealthRecord4{0,1,2}TestFactory`, `HealthRecordServiceArchiveTest`)
- FK từ `care_facilities.partner_id` (0 dòng dùng)

**Chưa xóa.** Cần xác nhận với người viết chức năng archive: đây là bảng dự phòng cho
tính năng chưa làm xong, hay tàn dư? Cấu trúc (`legacy_table`, `legacy_id`,
`payload_jsonb`, `retention_until`, `checksum`) trông như hạ tầng archive được thiết kế
có chủ đích nhưng chưa nối vào đâu.

---

## 9. KẾT LUẬN — Bảng nào BỎ, bảng nào GỘP, và tại sao

### 9.1. BỎ (drop table)

| # | Bảng | Cột | Dòng | Vì sao bỏ |
| --- | --- | --- | --- | --- |
| 1 | **`partner_organizations`** | 14 | 2 | **Không phát triển module Partner nữa.** 0 FK đi vào, 2 dòng đều là seed, 0 bản ghi audit liên quan → xóa an toàn tuyệt đối. |

**Dọn kèm (không phải bảng):**
- `users.role = 'PARTNER'` → `NULL` (1 tài khoản test)
- 7 giá trị `PARTNER_*` trong enum `AuditAction` → xóa được (đã xác nhận 0 dòng audit)
- 78 file code ở backend + webapp + mobile
- **Không** xóa `care_facilities.partner_id` trong đợt này — constraint của nó trỏ
  `archived_records`, là vấn đề độc lập, trộn vào sẽ khó rollback

**Ứng viên bỏ nhưng chưa đủ căn cứ:** `archived_records` (0 code ref) — chờ xác nhận, xem mục 8.

### 9.2. GỘP

| # | Bảng bị gộp | Gộp vào | Δ bảng | Tại sao gộp | Rủi ro |
| --- | --- | --- | --- | --- | --- |
| 1 | 4 cột read của `direct_conversations` | `direct_conversation_read_cursors` | 0 | **Cùng một trạng thái lưu ở hai nơi** sau migration `V20260801100001`. Bảng cursor mở rộng được cho N người đọc, 4 cột kia chỉ 2 vai. Đang là nguồn sai lệch dữ liệu. | Thấp |
| 2 | `appointment_notification_rules` | `appointment_notification_configs` | −1 | Config chỉ **5 cột**, PK là `reminder_id` → 1-1 với `care_tasks`. Rules chỉ là list offset có thứ tự → JSONB. Hai bảng cho một khái niệm cấu hình. | Thấp |
| 3 | `reminder_schedule_times` | `reminder_schedules` | −1 | **Bảng con thuần túy** 4 cột, 0 FK đi vào, không có vòng đời độc lập. Mỗi lần đọc lịch nhắc phải JOIN thêm chỉ để lấy mấy mốc giờ. | Thấp |
| 4 | `consultation_sessions` | `consultation_bookings` | −1 | Quan hệ **1-1 tuyệt đối**, 0 FK đi vào, 1 dòng dữ liệu. Bảng 9 cột cho quan hệ 1-1 là chi phí thuần túy. | Thấp |
| 5 | `safety_configs` | `users.settings_jsonb` | −1 | 1-1 với `users`, **1 dòng**, và `users` **đã có sẵn** cột `settings_jsonb`. | Thấp–TB |
| 6 | `reminder_occurrence_aliases` | → chuyển thành VIEW trên `care_tasks` | −1 | **Dữ liệu dẫn xuất 100%** do trigger sinh ra. Bảng dẫn xuất luôn có nguy cơ lệch với nguồn. | Thấp–TB |
| 7 | `preparation_checklist_items` | `checklist_task_instances` | −1 | **Tàn dư checklist v1** sống sót qua đợt hợp nhất v2. Hệ thống đang nuôi 2 mô hình checklist song song, mô hình cũ chỉ còn **2 dòng**. | TB (code) |
| 8 | `growth_measurements` | `health_observations` + view | −1 | 3 phép đo = 3 giá trị `observation_type`. `health_observations` đã có đủ cột. Nhóm **đang đi đúng hướng này** qua 2 migration gần nhất. | TB |
| 9 | `reminder_schedule_jobs` + `appointment_notification_jobs` | `notification_jobs` | −1 | **11 cột trùng khít.** Lợi ích thật không phải bớt 1 bảng mà là **bỏ được một worker trùng lặp** — hiện 2 worker cài lại cùng logic retry/locking/stale-detection. | TB–Cao |

### 9.3. Kết quả nếu làm hết

```
Hiện tại:  75 bảng  (74 nghiệp vụ + flyway_schema_history)
Bỏ Partner:        −1   →  74
Gộp mục 2–9:       −8   →  66
                          ══
                          66 bảng  (−12%)
```

Cộng thêm: bỏ **1 view chết** (`nearby_support_interactions`) + **12 file code chết**,
và **1 worker trùng lặp**.

### 9.4. Thứ tự thực hiện

| # | Việc | Δ | Rủi ro |
| --- | --- | --- | --- |
| 1 | Chạy các câu kiểm tra trên Supabase (mục 3.2) | — | Không |
| 2 | Bỏ 4 cột read legacy khỏi `direct_conversations` | 0 | Thấp |
| 3 | Xóa package `nearbycare/` + view `nearby_support_interactions` | −1 view | Thấp |
| 4 | Gộp `appointment_notification_rules` vào `configs` | −1 | Thấp |
| 5 | Gộp `reminder_schedule_times` vào `reminder_schedules` | −1 | Thấp |
| 6 | Gộp `consultation_sessions` vào `consultation_bookings` | −1 | Thấp |
| 7 | Gỡ Partner: code trước (78 file), migration sau cùng | −1 | Thấp |
| 8 | `safety_configs` → `users.settings_jsonb` | −1 | TB |
| 9 | `reminder_occurrence_aliases` → view | −1 | TB |
| 10 | `preparation_checklist_items` → `checklist_task_instances` | −1 | TB |
| 11 | `growth_measurements` → `health_observations` + view | −1 | TB |
| 12 | Hai bảng job → `notification_jobs` + gộp worker | −1 | TB–Cao |
| 13 | Tách dữ liệu cảm biến khỏi `safety_events` | +1 | TB |

Nguyên tắc xuyên suốt: **code trước, migration sau.** Nếu bước code phát hiện phụ thuộc
chưa lường trước, vẫn dừng được mà database chưa bị đụng.

---

## 10. Bốn cảnh báo bắt buộc đọc trước khi bắt tay

**1. `ddl-auto: validate` là bạn, không phải kẻ thù.** `application.yaml` đặt
`validate`, nên đổi schema mà quên đổi entity sẽ **fail ngay khi khởi động** thay vì hỏng
âm thầm. Nhưng nó **không** bảo vệ được 221 câu native SQL trong 73 file — đó mới là
chỗ dễ vỡ. Mỗi lần gộp bảng phải grep lại native SQL.

**2. Đừng đặt `SPRING_PROFILES_ACTIVE=local` khi trỏ vào Supabase.** Profile `local`
bật `ddl-auto: update` → Hibernate sẽ tự `ALTER` schema **dùng chung cả nhóm** và
crash-loop backend.

**3. Schema Supabase là DB dùng chung.** Version `20260804160000` trên DB khớp đúng
migration mới nhất trong repo (đã kiểm chứng — nhánh này **không** bị lệch, khác với
cảnh báo trong báo cáo cũ). Nhưng `validate-on-migrate: false` và
`ignore-migration-patterns: "*:missing"` đang che mọi lệch pha. Mọi `DROP TABLE`
ảnh hưởng tất cả người đang chạy backend — **thông báo trước.**

**4. 26 trigger là logic nghiệp vụ nằm trong DB.** Gộp bảng mà quên port trigger sang
là mất ràng buộc bất biến, mất guard checklist, mất khả năng ghi qua view. Danh sách
đầy đủ ở mục 2.3.

---

## Phụ lục — Toàn bộ 75 bảng

`Entity` = số class Java `@Table` trỏ vào bảng đó. `0` nghĩa là truy cập qua
`@ElementCollection`, native SQL, hoặc không ai dùng.

| Bảng | Cột | Dòng | Entity |
| --- | ---: | ---: | ---: |
| `account_deletion_requests` | 11 | 1 | 1 |
| `account_lock_appeals` | 9 | 0 | 1 |
| `administrative_areas` | 8 | 474 | 1 |
| `ai_content_assessments` | 23 | 3 | 1 |
| `ai_content_scan_jobs` | 14 | 3 | 1 |
| `ai_moderation_policies` | 18 | 11 | 1 |
| `appointment_notification_configs` | 5 | 5 | 1 |
| `appointment_notification_jobs` | 17 | 10 | 1 |
| `appointment_notification_rules` | 6 | 20 | 1 |
| `archived_records` | 11 | 1 | **0** |
| `attachments` | 24 | 3 | 1 |
| `audit_events` | 34 | 3750 | 6 |
| `auth_challenges` | 13 | 14 | 2 |
| `auth_sessions` | 19 | 102 | 1 |
| `care_facilities` | 21 | 25 | 1 |
| `care_group_members` | 18 | 9 | 1 |
| `care_groups` | 13 | 5 | 1 |
| `care_item_templates` | 56 | 71 | 4 |
| `care_subjects` | 13 | 38 | 1 |
| `care_tasks` | 31 | 46 | 3 |
| `checklist_action_commands` | 14 | 41 | 1 |
| `checklist_instances` | 26 | 33 | 1 |
| `checklist_task_instances` | 20 | 144 | 1 |
| `community_content` | 21 | 32 | 2 |
| `community_interactions` | 7 | 22 | 5 |
| `community_topics` | 12 | 119 | 1 |
| `consultation_bookings` | 17 | 1 | 1 |
| `consultation_context_citations` | 9 | 1 | 1 |
| `consultation_context_shares` | 16 | 1 | 1 |
| `consultation_sessions` | 9 | 1 | 1 |
| `content_item_sources` | 8 | 24 | **0** |
| `content_item_topics` | 3 | 3 | **0** |
| `content_items` | 22 | 27 | 1 |
| `conversation_calls` | 11 | 1 | 1 |
| `data_permissions` | 24 | 27 | 3 |
| `development_milestones` | 12 | 3 | 1 |
| `device_connections` | 11 | 1 | 1 |
| `device_tokens` | 7 | 25 | 1 |
| `direct_conversation_read_cursors` | 6 | 0 | **0** |
| `direct_conversations` | 10 | 1 | 1 |
| `direct_messages` | 10 | 2 | 1 |
| `expense_entries` | 11 | 2 | 1 |
| `expert_availability` | 9 | 3 | 1 |
| `expert_consultation_requests` | 16 | 1 | 1 |
| `expert_location_shares` | 12 | 1 | 1 |
| `flyway_schema_history` | 10 | 25 | **0** |
| `growth_measurements` | 12 | 8 | 1 |
| `health_context_memories` | 12 | 10 | 1 |
| `health_metric_definitions` | 23 | 12 | 1 |
| `health_observations` | 25 | 513 | 6 |
| `health_records` | 17 | 1 | 2 |
| `knowledge_source_reviews` | 8 | 1 | 1 |
| `knowledge_sources` | 15 | 13 | 1 |
| `maternal_exercise_sessions` | 16 | 21 | 1 |
| `moderation_cases` | 21 | 10 | 1 |
| `mother_journeys` | 30 | 19 | 1 |
| `notification_records` | 21 | 13 | 1 |
| `partner_organizations` | 14 | 2 | 1 |
| `preparation_checklist_items` | 13 | 2 | 1 |
| `professional_specialties` | 4 | 2 | 1 |
| `reminder_occurrence_aliases` | 6 | 15 | 1 |
| `reminder_schedule_jobs` | 16 | 221 | 1 |
| `reminder_schedule_times` | 4 | 6 | 1 |
| `reminder_schedules` | 12 | 6 | 1 |
| `safety_configs` | 10 | 1 | 1 |
| `safety_events` | 81 | 5976 | 8 |
| `safety_monitoring_sessions` | 7 | 1 | 1 |
| `specialties` | 6 | 8 | 1 |
| `system_configurations` | 14 | 1 | 1 |
| `triage_session_evidence` | 12 | 6 | **0** |
| `triage_sessions` | 36 | 18 | 2 |
| `users` | 52 | 42 | 5 |
| `vaccination_records` | 14 | 2 | 1 |
| `vaccination_schedules` | 9 | 7 | 1 |
