# CareBridge — Kiểm toán Database: bảng nào BỎ, bảng nào GỘP

**Ngày:** 2026-08-05 · **Nhánh:** `LamVH1` @ `f4c0642e`
**Nguồn dữ liệu:** truy vấn **trực tiếp** Supabase (`wqsunmakzdaxwyknegkq`, pooler
`aws-1-ap-northeast-1:5432`) + quét **1.673 file Java** và toàn bộ WebApp / MobileApp /
AITriageService / Firebase.

Mọi số cột, số dòng, khóa ngoại, ràng buộc, trigger, view trong báo cáo này **đọc từ DB
thật**, không suy ra từ file migration.

> Thay thế hai file cũ `Database_Table_Consolidation_Analysis.md` và
> `Partner_Module_Removal_Plan.md` — cả hai là phân tích tĩnh, có số liệu và kết luận sai
> (chi tiết ở mục 4.2).

---

## Mục lục

1. [Tổng quan schema](#1-tổng-quan-schema)
2. [Chỗ nào trong code đang connect tới SQL](#2-chỗ-nào-trong-code-đang-connect-tới-sql)
3. [Bảng audit — từng bảng ai đang gọi](#3-bảng-audit--từng-bảng-ai-đang-gọi)
4. [BỎ — module Partner](#4-bỏ--module-partner)
5. [GỘP ĐƯỢC](#5-gộp-được)
6. [Cách hiện thực: thêm cột / mảng / JSONB](#6-cách-hiện-thực-thêm-cột--mảng--jsonb)
7. [KHÔNG gộp được](#7-không-gộp-được)
8. [Nên TÁCH / nên xóa code chết](#8-nên-tách--nên-xóa-code-chết)
9. [Chờ quyết định sản phẩm](#9-chờ-quyết-định-sản-phẩm)
10. [KẾT LUẬN](#10-kết-luận--bỏ-gì-gộp-gì-tại-sao)
11. [Cảnh báo trước khi bắt tay](#11-bốn-cảnh-báo-trước-khi-bắt-tay)
12. [Phụ lục — toàn bộ 75 bảng](#phụ-lục--toàn-bộ-75-bảng)

---

## 1. Tổng quan schema

| Hạng mục | Số lượng | Ghi chú |
| --- | --- | --- |
| **Bảng trong schema `public`** | **75** | 74 bảng nghiệp vụ + `flyway_schema_history` |
| View tương thích | 4 | `care_logs`, `expert_credentials`, `emergency_contacts`, `nearby_support_interactions` |
| Khóa ngoại | 169 | đếm qua `pg_constraint` |
| Trigger | 26 | gồm 4 `INSTEAD OF` cho view và 5 trigger bất biến |
| JPA entity `@Table` | 110 | trỏ vào **69 bảng** + 3 view |
| Repository Spring Data | 131 | |
| Migration Flyway đã apply | 25 | mới nhất `20260804160000` — **khớp đúng** repo |
| File migration trong repo | 27 | 25 versioned + 2 finalizer thủ công |

Bảng hệ thống Supabase (`auth` 23, `storage` 8, `realtime` 3, `vault` 1) không tính —
nền tảng quản lý, không đụng tới.

### 1.1. Vì sao 110 entity mà chỉ 69 bảng

Đây là bối cảnh quan trọng nhất của cả báo cáo: **dự án đã gộp bảng từ trước rồi.** Nhiều
entity Java cùng trỏ vào một bảng vật lý, phân biệt bằng cột phân loại:

| Bảng | Entity | Danh sách |
| --- | ---: | --- |
| `safety_events` | 8 | `SafetyEvent`, `EmergencySession`, `EmergencyAlertAttempt`, `EmergencyAlertDelivery`, `EmergencyMapHandoff`, `FamilyAlertLog`, `LocationSnapshot`, `SafetyEventResponseRecord` |
| `audit_events` | 6 | `AuditLog`, `ModerationAction`, `ContributionPoint`, `MotherBaselineContext`, `MotherJourneyTransition`, `PregnancyOutcomeEvidence` |
| `health_observations` | 6 | `HealthObservation`, `MaternalHealthMetric`, `PostpartumLog`, `DeviceMeasurement`, `ExerciseSafetyCheck`, `PostureFeedbackEvent` |
| `users` | 5 | `User`, `Person`, `UserProfile`, `ExpertProfile`, `CommunityProfile` |
| `community_interactions` | 5 | `CommunityAnswerLike`, `CommunityQuestionLike`, `CommunityBookmark`, `UserTopicFollow`, `QuestionNotificationMute` |
| `care_item_templates` | 4 | `ChecklistTemplate`, `ChecklistItem`, `PregnancyExercise`, `PostureAnalysisConfig` |
| `care_tasks` | 3 | `CareTask` (×2 package), `Reminder` |
| `data_permissions` | 3 | `ConsentGrant`, `DataPermission`, `TriageDisclaimerConsent` |
| `auth_challenges` / `community_content` / `health_records` / `triage_sessions` | 2 mỗi bảng | |

**Hệ quả:** dư địa gộp còn lại **hẹp hơn nhiều** so với con số 75 gợi ý. Phần dễ gộp đã
gộp xong. Cái còn lại hoặc là bảng con thuần túy, hoặc có lý do pháp lý để đứng riêng.

### 1.2. Pattern gộp mà dự án đang dùng

Cách làm đã kiểm chứng, **mọi đề xuất ở mục 5 nên đi theo đúng pattern này**:

> cột phân loại (`*_type` / `*_category`) + JSONB metadata + **view tương thích có trigger
> `INSTEAD OF`**, để code Java cũ vẫn `SELECT`/`INSERT` như bảng chưa bị xóa.

| View | Thực chất đọc từ | Điều kiện | Dòng thật |
| --- | --- | --- | ---: |
| `care_logs` | `care_tasks` | `task_type = 'CARE_LOG'` | 31 |
| `expert_credentials` | `attachments` | `attachment_category = 'EXPERT_CREDENTIAL'` | 2 |
| `emergency_contacts` | `care_group_members` ⋈ `care_groups` ⋈ `users` | `is_emergency_contact` | 2 |
| `nearby_support_interactions` | *(không gì cả)* | `WHERE false` | 0 |

---

## 2. Chỗ nào trong code đang connect tới SQL

### 2.1. Chỉ có **một** service nói chuyện với Postgres

| Module | Kết nối DB? | Bằng chứng |
| --- | --- | --- |
| **`CareBridgeAPI`** (Spring Boot) | ✅ **duy nhất** | `org.postgresql.Driver`, Hikari, Flyway, 131 repository |
| `CareBridgeAITriageService` (FastAPI) | ❌ | `requirements.txt` chỉ có `fastapi, uvicorn, pydantic, langgraph, langchain-core, google-genai, httpx…` — **không** `psycopg`/`sqlalchemy`/`asyncpg`/`supabase` |
| `CareBridgeWebApp` (React) | ❌ | `package.json` không có driver nào. Gọi backend qua `axios`. |
| `CareBridgeMobileApp` (Flutter) | ❌ | `pubspec.yaml` không có `supabase_flutter`/`postgres`. Lưu cục bộ bằng `flutter_secure_storage`. |
| `MachineLearning` | ❌ | không có driver DB |
| `Firebase` | ⚠️ datastore **khác** | Firestore, 1 collection `userConversationEvents/{uid}/events/{eventId}` cho signaling gọi/chat |

> `CareBridgeWebApp/src/vite-env.d.ts` khai báo `VITE_SUPABASE_URL` và
> `VITE_SUPABASE_ANON_KEY`, nhưng **không file nào trong `src/` đọc chúng**. Khai báo thừa.
> Không có đường đi trực tiếp từ trình duyệt xuống Postgres — tốt cho bảo mật, và nghĩa là
> **đổi schema chỉ ảnh hưởng backend**.

### 2.2. Backend nối vào DB bằng ba đường

| Đường | Vị trí | Ghi chú |
| --- | --- | --- |
| **1. JPA/Hibernate** | 110 entity, 131 repository | `ddl-auto: validate` — đổi schema mà quên đổi entity sẽ **fail lúc khởi động**, không hỏng âm thầm |
| **2. Native SQL** | **221 chỗ** / **73 file** (`nativeQuery = true`, `createNativeQuery`, `JdbcTemplate`, `@Modifying`) | **Rủi ro lớn nhất** khi gộp bảng — Hibernate không validate được |
| **3. Datasource thứ hai** | `ChecklistRetentionDataSourceConfiguration` | Pool + credential riêng (`CAREBRIDGE_CHECKLIST_RETENTION_DB_*`), có `ChecklistRetentionOwnerIsolationVerifier`. Mặc định tắt. |

`RuntimeDatasourceEnvironmentPostProcessor` bơm credential runtime; Flyway chạy bằng user
riêng (`CAREBRIDGE_FLYWAY_DB_USERNAME`) tách khỏi user runtime.

### 2.3. Logic nghiệp vụ nằm trong DB, không phải trong Java

26 trigger — bỏ sót nhóm này khi migrate là hỏng dữ liệu âm thầm:

| Nhóm | Trigger | Ý nghĩa |
| --- | --- | --- |
| **Bất biến** | `audit_events_immutable_trg`, `triage_session_evidence_immutable_trg`, `knowledge_source_reviews_immutable_trg`, `trg_consultation_context_shares_append_only`, `trg_consultation_context_citations_append_only` | Chặn `UPDATE`/`DELETE` ở tầng DB |
| **Guard checklist** | 7 trên `care_item_templates`, 2 trên `checklist_instances`, 2 trên `checklist_task_instances`, 2 trên `checklist_action_commands` | Validate template đã duyệt, retention, target |
| **View ghi được** | `care_logs_view_write_trg`, `expert_credentials_view_write_trg`, `emergency_contacts_view_write_trg` | `INSTEAD OF INSERT/UPDATE/DELETE` |
| **Sinh dữ liệu** | `care_tasks_reminder_occurrence_alias_trg` | `AFTER INSERT/UPDATE` trên `care_tasks` → ghi `reminder_occurrence_aliases` |
| **Khóa tính năng** | `nearby_support_interactions_disabled_trg` | Chặn mọi ghi — tính năng đã khai tử |

---

## 3. Bảng audit — từng bảng ai đang gọi

Truy vết bảng → entity → repository → service → controller → endpoint → màn hình, cho
mọi bảng nằm trong danh sách bỏ/gộp.

| Bảng | Repository & cách truy cập | Service | Endpoint | Frontend |
| --- | --- | ---: | --- | --- |
| `partner_organizations` | `PartnerOrganizationRepository` | 2 | `/api/v1/partner/profile`, `/api/v1/admin/partners/*/decision` | WebApp `features/partnerGovernance/` (12 file) |
| `reminder_schedule_times` | 2 method: `findByScheduleId…`, `deleteByScheduleId` | **1** | `/api/v1/reminder-schedules` (`MOTHER`,`FAMILY`) | Mobile 1 file |
| `appointment_notification_configs` | **0 method tùy biến** — JpaRepository trần | 2 | `/api/v1/appointments` (`MOTHER`) | Mobile 1 file |
| `appointment_notification_rules` | 2 method: `findByReminderId…`, `deleteByReminderId` | **1** | ↑ | ↑ |
| `reminder_schedule_jobs` | 6 method, **5 native `@Modifying`** | 2 | *(chạy nền)* | — |
| `appointment_notification_jobs` | 8 method, **6 native `@Modifying`** | 3 | *(chạy nền)* | — |
| `reminder_occurrence_aliases` | 2 method **read-only** | **1 call site** | qua Today-task | Mobile home |
| `safety_configs` | chỉ `findByUserId` + `save` | 3 | `/api/v1/safety/config` (`MOTHER`) | Mobile 3 file |
| `preparation_checklist_items` | **`Repository`, không phải `JpaRepository`** — 3 method đọc | **1** | `/api/v1/user-checklist-items` — phần lớn write đã `throw retiredMutation()` | Mobile 1 file |
| `growth_measurements` | 4 `@Query`, đều lọc `careSubjectId` + `deletedAt is null` | **4** | qua baby-care | Mobile 6 file + **WebApp 1 trang** |
| `consultation_sessions` | `ConsultationSessionRepository` | **0** ⚠️ | **0** | — |
| `archived_records` | *(không có entity)* | **0** ⚠️ | **0** | — |

**Kết luận:** ngoài `growth_measurements` (4 service + 2 frontend) và `safety_configs`
(3 service), mọi bảng còn lại chỉ có **1 điểm truy cập duy nhất**. Bề mặt tác động nhỏ
hơn nhiều so với lo ngại ban đầu.

---

## 4. BỎ — module Partner

### 4.1. Bảng phải bỏ

| Bảng | Cột | Dòng thật | FK đi vào |
| --- | ---: | ---: | --- |
| **`partner_organizations`** | 14 | **2** (đều là seed) | **0 — không bảng nào trỏ vào** |

Hai dòng đang có: *Hội Sản Phụ Khoa Việt Nam*, *Hội Nhi Khoa Cần Thơ* — từ
`V2__seed_reference_data.sql`. **Không có dữ liệu thật.** FK duy nhất đi **ra**
(`representative_user_id → users`), nên xóa bảng không kéo theo bảng nào.

### 4.2. Ba đính chính so với kế hoạch cũ

**a) `care_facilities.partner_id` KHÔNG phải khóa ngoại tới Partner.**

```
care_facilities_partner_archive_fk : care_facilities.partner_id → archived_records.archive_id
```

Tên cột nói Partner, ràng buộc trỏ `archived_records`. Đã kiểm tra:
`SELECT count(*) FROM care_facilities WHERE partner_id IS NOT NULL` → **0**.
Xóa `partner_organizations` không ảnh hưởng `care_facilities`. Nhưng cột này là lỗi đặt
sai constraint từ trước → **xử lý ở migration riêng**, đừng trộn vào lần gỡ Partner.

**b) `audit_events` KHÔNG có cột `action`.** Kế hoạch cũ đề xuất chạy
`WHERE action LIKE 'PARTNER_%'` — câu đó lỗi cú pháp. Bảng dùng `event_category` +
`resource_type`. Kiểm tra đúng cột:

```sql
SELECT count(*) FROM audit_events
WHERE event_category ILIKE '%PARTNER%' OR resource_type ILIKE '%PARTNER%';
-- => 0
```

**Không có bản ghi audit Partner nào**, nên 7 giá trị `PARTNER_*` trong enum `AuditAction`
**xóa được an toàn**. (Kế hoạch cũ khuyên giữ lại — dựa trên giả định sai.)

**c) Có 1 tài khoản mang role `PARTNER`:** `50337ac8-…-580a6eed582a` — *"Partner Test"*.
Phân bố role: `MOTHER` 20, `FAMILY` 6, `NULL` 6, `EXPERT` 5, `SYSTEM_ADMIN` 2,
`MODERATOR` 1, `CONTENT_ADMIN` 1, **`PARTNER` 1**. Là tài khoản test.

### 4.3. Khối lượng code phải gỡ — 78 file

| Tầng | File | Cách xử lý |
| --- | ---: | --- |
| Backend `partner/` (main) | 20 | Xóa cả package |
| Backend `partner/` (test) | 8 | Xóa cả package |
| Backend main — file khác nhắc PARTNER | 9 | **Sửa** |
| Backend test — file khác nhắc PARTNER | 19 | **Sửa** — tốn công nhất |
| WebApp `features/partnerGovernance/` | 12 | Xóa cả thư mục |
| WebApp — file khác | 9 | **Sửa** |
| Mobile | 1 | `care_facility_model.dart` chỉ có field `partnerId` — chờ xử lý 4.2a |

Bảng thì 1, nhưng công sức nằm hết ở tầng code — đặc biệt 19 file test liệt kê toàn bộ
enum `Role`, sẽ không biên dịch được sau khi bỏ giá trị `PARTNER`.

### 4.4. Migration đề xuất

```sql
-- V20260806000000__remove_partner_module.sql
-- Không bảng nào FK vào partner_organizations → DROP trực tiếp là đủ.
DROP TABLE IF EXISTS public.partner_organizations;

-- users.role là varchar(50), không phải enum type → không cần ALTER TYPE.
UPDATE public.users SET role = NULL WHERE role = 'PARTNER';   -- 1 dòng
```

**Không** đụng `audit_events` (0 dòng liên quan, và có trigger chặn `DELETE`).
**Không** đụng `care_facilities.partner_id` trong migration này.

---

## 5. GỘP ĐƯỢC

Xếp theo tỉ lệ lợi ích / rủi ro. Riêng mục 5.0 không làm giảm số bảng nhưng nên làm trước.

### 5.0. `direct_conversations` — bỏ 4 cột chết (KHÔNG phải gộp bảng)

| Bảng | Cột | Dòng |
| --- | ---: | ---: |
| `direct_conversations` | 10 (có 4 cột read legacy) | 1 |
| `direct_conversation_read_cursors` | 6 | 0 |

Migration `V20260801100001` đã tạo bảng cursor và backfill từ 4 cột
`mother_last_read_*` / `expert_last_read_*`. **4 cột đó hiện là code chết hoàn toàn:**

| Đường | Trạng thái |
| --- | --- |
| `advanceReadCursor()` → upsert vào `direct_conversation_read_cursors` | ✅ đường **duy nhất** đang chạy (`ConversationSummaryAggregateRepositoryImpl:77`) |
| `fetchUnreadCounts()` → `LEFT JOIN direct_conversation_read_cursors` | ✅ đếm chưa đọc chỉ dựa vào bảng cursor |
| `markMotherRead()` / `markExpertRead()` (`DirectConversationRepository:42,51`) | ❌ **0 caller trong `src/main`** |
| 4 cột trong `DirectConversation.java:47–56` | ❌ không getter nào được gọi, không lộ ra DTO |

Tham chiếu duy nhất tới 2 method cũ là `verify(…, never())` trong
`DirectConversationServiceImplReadTest` — test **cố ý khẳng định đường cũ đã ngừng dùng**.

Bảng cursor trống 0 dòng **không phải** vì code chưa dùng, mà vì DB chỉ có dữ liệu seed
(1 hội thoại, 2 tin nhắn ngày 2026-07-24) và chưa ai gọi `PATCH /read` lần nào.

**Việc cần làm:** `DROP` 4 cột khỏi bảng + entity, xóa 2 method repository chết.
**Δ bảng: 0.** Hai bảng này **không gộp vào nhau được** — lý do ở mục 7.5.
**Rủi ro: thấp nhất** — không có dữ liệu để migrate, không có caller để sửa.

### 5.1. `reminder_schedule_times` → mảng trên `reminder_schedules`

4 cột (`time_id, schedule_id, local_time, sort_order`), 6 dòng, 0 FK đi vào.

**Bằng chứng code — đang bị dùng như *giá trị*, không phải *thực thể*.** Repository chỉ có
đúng 2 method (`findByScheduleIdOrderBySortOrderAscLocalTimeAsc`, `deleteByScheduleId`),
và `ReminderScheduleServiceImpl:177-183` làm:

```java
timeRepository.deleteByScheduleId(scheduleId);   // xóa sạch
… rows.add(ReminderScheduleTime.builder()…)      // dựng lại từ đầu
timeRepository.saveAll(rows);                    // chèn lại cả bộ
```

Rồi dòng 187-188 đọc ra **map thẳng thành `List<LocalTime>`** — vứt bỏ `time_id` và
`sort_order`. Code đã coi nó là danh sách giờ; chỉ schema là chưa.

**Rủi ro: thấp nhất trong nhóm gộp thật.** 1 service, 6 dòng dữ liệu.
Cách hiện thực và ràng buộc giữ lại được: xem mục 6.3.

### 5.2. `appointment_notification_rules` → `appointment_notification_configs`

| Bảng | Cột | Dòng | Nội dung |
| --- | ---: | ---: | --- |
| `appointment_notification_configs` | **5** | 5 | `reminder_id, time_zone, config_revision, created_at, updated_at` |
| `appointment_notification_rules` | 6 | 20 | `rule_id, reminder_id, offset_minutes, sort_order, created_at, updated_at` |

Config có PK là `reminder_id` → 1-1 tuyệt đối với `care_tasks`.

**Bằng chứng code — cùng dấu hiệu mục 5.1:**
- `AppointmentNotificationConfigRepository` **không có một method tùy biến nào**. Truy cập
  luôn bằng `configRepository.findById(reminder.getId())` — tra theo khóa của bảng cha.
- `AppointmentNotificationRuleRepository` chỉ có `findByReminderIdOrderByOffsetMinutesAsc`
  + `deleteByReminderId`. `AppointmentNotificationScheduleService:171-182` cũng
  **xóa sạch rồi chèn lại cả bộ**. `rule_id` chưa bao giờ dùng để tra cứu.

**Rủi ro: thấp.** Chỉ **1 service** chạm vào. Phải giữ `config_revision` vì job đang chạy
tham chiếu tới nó.

### 5.3. `reminder_occurrence_aliases` → chuyển thành VIEW

6 cột, 15 dòng, 0 FK đi vào. Được **sinh tự động** bởi trigger
`care_tasks_reminder_occurrence_alias_trg` (`AFTER INSERT/UPDATE` trên `care_tasks`).

**Java KHÔNG ghi vào bảng này.** Toàn bộ `src/main` có đúng một call site:

```
checklist/today/provider/ReminderTaskActionHandler.java:182
    return occurrenceAliasRepository.findByOccurrenceId(taskId);
```

Repository chỉ khai báo 2 method đọc, không nơi nào gọi `save()`/`delete()`. Đường ghi duy
nhất là trigger DB. Chuyển thành view **không phá vỡ dòng code nào** — `findByOccurrenceId`
chạy y nguyên trên view.

**Vì sao:** bảng dẫn xuất luôn có nguy cơ lệch với nguồn khi trigger lỗi, hoặc khi ai đó
ghi thẳng vào `care_tasks` bằng `COPY`/migration. **Rủi ro: thấp.**

### 5.4. `safety_configs` → 8 cột typed trên `users`

10 cột, **1 dòng**, `UNIQUE (user_id)` → quan hệ 1-1 **được DB bảo đảm**, 0 FK đi vào.

**Bằng chứng code:** `ISafetyConfigRepository` chỉ có **`findByUserId`** (+ `save` kế thừa).
PK `safety_config_id` **chưa bao giờ dùng để tra cứu** — dấu hiệu rõ ràng đây thực chất là
một hàng thuộc tính của `users`. Ba nơi gọi:

| Nơi gọi | Mục đích |
| --- | --- |
| `SafetyConfigService` | `GET`/`PUT /api/v1/safety/config` (`MOTHER`) |
| `SensorSelfTestService:128` | đọc khi chạy self-test cảm biến |
| `FallDetectionService:223, 305` | ⚠️ đọc trong **luồng phát hiện ngã** |

**Dùng cột typed, KHÔNG dùng `settings_jsonb`** — lý do đầy đủ ở mục 6.2.

**Rủi ro: thấp–trung bình.** Đây cũng là **mục đáng cắt nhất** nếu cần thu hẹp phạm vi:
bảng đang có `UNIQUE` + 2 CHECK + chỉ 10 cột, tức là đang làm đúng việc của mình. Lợi ích
−1 bảng, chi phí +8 cột trên bảng hub và sửa 3 service.

### 5.5. `preparation_checklist_items` → `checklist_task_instances`

| Bảng | Cột | Dòng |
| --- | ---: | ---: |
| `preparation_checklist_items` | 13 | **2** |
| `checklist_task_instances` | 20 | 144 |

Trùng khái niệm gần như hoàn toàn: *một mục checklist gán cho người dùng, có
title / display_order / status / due_at / completed_at / category*.

**Bảng này ĐÃ ĐƯỢC KHAI TỬ CHÍNH THỨC.** Javadoc trên `UserChecklistItemRepository`:

> `/** Read-only compatibility repository. Legacy checklist writes were retired by CHK-025. */`

Bằng chứng đi kèm:
- Repository extends **`Repository`, không phải `JpaRepository`** — cố ý không lộ
  `save()`/`delete()`. Chỉ 3 method đọc.
- `UserChecklistItemController`: `POST /import`, `PATCH /{id}/toggle`, `PUT /{id}` đều
  **`throw retiredMutation()`**. Write mới đi qua `userCreatedTaskService` (v2).
- `GET` còn sống nhưng chỉ để **trộn 2 dòng legacy vào danh sách v2** rồi `putIfAbsent`.

Bảng đã đóng băng: sẽ không bao giờ có dòng thứ 3. Đây không phải gộp một tính năng đang
chạy, mà là **dọn nốt một đợt migration đã làm 90%**.

**Rủi ro: thấp** về dữ liệu (2 dòng, read-only), **trung bình** về code — phải chuyển 2
dòng sang `checklist_task_instances`, bỏ controller, service (158 dòng), repository,
entity và `ChecklistControllerExceptionHandler`.

### 5.6. `growth_measurements` → `health_observations` + view

| Bảng | Cột | Dòng |
| --- | ---: | ---: |
| `growth_measurements` | 12 | **8** |
| `health_observations` | 25 | 513 |

`health_observations` đã có đủ bộ cột: `observation_type`, `value_numeric`,
`value_secondary`, `unit`, `observed_at`, `care_subject_id`, `source_type`,
`raw_payload_jsonb`, `observation_shape`. `growth_measurements` chỉ chứa 3 phép đo
(`weight_kg`, `height_cm`, `head_circumference_cm`) = 3 giá trị `observation_type`.

Nhóm **đang đi đúng hướng này**: `V20260804100000__retire_standalone_weight_metric` và
`V20260804090000__replace_maternal_heart_rate_with_bmi` đều là hợp nhất chỉ số sức khỏe.

**⚠️ Bề mặt tác động LỚN NHẤT trong nhóm gộp:**

| Tầng | Nơi dùng |
| --- | --- |
| Service | `GrowthServiceImpl`, `BabyCareOverviewServiceImpl`, `BabyCareTimelineServiceImpl`, `AppointmentPreparationServiceImpl` — **4** |
| Mobile | `growth_measurement_model.dart`, `who_growth_standard.dart`, `features/baby/` (3 màn) + router — **6 file** |
| WebApp | `features/babyCare/pages/BabyCareHubPage.tsx` — **bảng duy nhất trong nhóm gộp có mặt trên WebApp** |

Cả 4 `@Query` đều lọc `careSubjectId` **và** `deletedAt is null`. View tương thích bắt
buộc phải tái hiện đúng cả hai, nếu không danh sách tăng trưởng sẽ hiện lại bản ghi đã
xóa mềm.

**Phát hiện thêm:** bảng có **hai cột trùng nội dung** — `baby_id` và `care_subject_id`,
cả hai `NOT NULL`, đồng bộ thủ công bởi `GrowthMeasurement.alignCanonicalCareSubject()`
(dòng 70-74). Mọi truy vấn chỉ dùng `care_subject_id`. **`baby_id` là cột thừa — nên bỏ,
kể cả khi không gộp bảng.**

### 5.7. Hai bảng job → `notification_jobs`

| Bảng | Cột | Dòng |
| --- | ---: | ---: |
| `reminder_schedule_jobs` | 16 | **221** |
| `appointment_notification_jobs` | 17 | 10 |

11 cột giống hệt nhau:

```
job_id, due_at, status, attempt_count, next_attempt_at, locked_by, locked_at,
notification_record_id, last_error_code, created_at, updated_at
```

Chỉ khác phần nguồn:
- `reminder_schedule_jobs`: `schedule_id, schedule_revision, occurrence_date, local_time, time_zone`
- `appointment_notification_jobs`: `reminder_id, occurrence_id, occurrence_generation, occurrence_scheduled_at, config_revision, offset_minutes`

**Bốn bằng chứng cho thấy đây là copy-paste, không phải hai thiết kế độc lập:**

**a) Hai entity đã dùng chung một enum trạng thái.** `ReminderScheduleJob.java:3` import
`AppointmentNotificationJobStatus` từ **package của bảng kia**. Không tồn tại
`ReminderScheduleJobStatus`. Cả hai chạy trên `PENDING / PROCESSING / SENT / FAILED /
SUPPRESSED / CANCELLED`. Về máy trạng thái, chúng **đã là một**.

**b) Hai repository trùng tên method 6/6:**

| `ReminderScheduleJobRepository` | `AppointmentNotificationJobRepository` |
| --- | --- |
| `findClaimableIds` | `findClaimableIds` |
| `claim` | `claim` |
| `requeueStale` | `requeueStale` |
| `transitionAfterProcessing` | `transitionAfterProcessing` |
| `cancelObsoleteRevisions` | `cancelObsoleteRevisions` |
| `cancelActiveByScheduleId` | `cancelActiveByReminderId` |
| — | `findByIdForUpdate`, `cancelActiveByOccurrenceId` |

**c) Hai worker giống nhau từng dòng.** `ReminderScheduleWorker` (48 dòng) và
`AppointmentNotificationWorker` (50 dòng) khác nhau **duy nhất ở tên biến và tiền tố
property**: cùng `enabled` → `fcmService.isReady()` → `claimDueJobs(workerId, batchSize)`
→ `processAsync`.

**d) Tám cờ cấu hình song song, mặc định giống hệt:**

```
carebridge.notification.reminder-schedule.{enabled, batch-size:25, worker-delay-ms:15000, planner-cron: 0 20 2 * * *}
carebridge.notification.appointment.     {enabled, batch-size:25, worker-delay-ms:15000, planner-cron: 0 15 2 * * *}
```

**Đề xuất:** một bảng `notification_jobs` với `job_type IN ('REMINDER','APPOINTMENT')`,
`source_type` + `source_id` thay 2 FK, phần đặc thù vào JSONB. Gộp bảng cho phép gộp luôn
**2 worker + 2 planner + 8 cờ cấu hình** — đó mới là lợi ích thật, không phải bớt 1 bảng.

**Rủi ro: trung bình–cao.** 221 job đang sống, và **11/14 method của hai repository là
native `@Modifying`** — Hibernate không validate được. Phải viết lại từng câu và chạy khi
queue rỗng.

---

## 6. Cách hiện thực: thêm cột / mảng / JSONB

Mỗi mục gộp có 3 cách hiện thực. **Ràng buộc DB thật quyết định cách nào đúng**, không
phải sở thích.

### 6.1. Nguyên tắc chọn

> `ALTER TABLE ADD COLUMN` là công cụ **đúng cho quan hệ 1-1**, và là **anti-pattern cho
> 1-N**. Kiểm tra bằng đúng một câu: **cột FK trỏ về cha có `UNIQUE` không?**

| Mục | FK về cha có UNIQUE? | Quan hệ thật | Thêm cột? |
| --- | --- | --- | --- |
| `safety_configs` → `users` | ✅ **`UNIQUE (user_id)`** | 1-1 (DB bảo đảm) | ✅ **Nên** |
| `consultation_sessions` → `consultation_bookings` | ❌ chỉ FK, **không UNIQUE** | 1-N | ❌ |
| `reminder_schedule_times` → `reminder_schedules` | ❌ `UNIQUE(schedule_id, local_time)` | 1-N | ❌ |
| `appointment_notification_rules` → `configs` | ❌ `UNIQUE(reminder_id, offset_minutes)` | 1-N | ❌ |
| `direct_conversation_read_cursors` → `direct_conversations` | ❌ PK `(conversation_id, reader_user_id)` | 1-N | ❌ |

> **Lưu ý về số liệu:** đo thực tế cho `reminder_schedule_times` là 1 con/cha,
> `appointment_notification_rules` là 4 con/cha — nhưng **toàn bộ là dữ liệu seed**, không
> chứng minh gì về lực lượng quan hệ. Bằng chứng phải lấy từ **ràng buộc schema**.

### 6.2. `safety_configs` — thêm cột tốt hơn JSONB

Bảng đang có 2 CHECK constraint:

```sql
CHECK (countdown_seconds = ANY (ARRAY[15, 30, 60]))
CHECK (sensor_permission_granted = false OR sensor_permission_recorded_at IS NOT NULL)
```

| | `users.settings_jsonb` | Thêm 8 cột typed vào `users` |
| --- | --- | --- |
| 2 CHECK trên | **Mất** — phải viết lại trên jsonb path (chậm, khó đọc) hoặc đẩy lên application | **Giữ nguyên**, chỉ đổi tên constraint |
| Hot path `FallDetectionService` | Giải nén JSONB **mỗi sự kiện IMU** | Đọc cột `int`/`boolean` trực tiếp |
| Kích thước `users` | 52 cột | 60 cột |

8 cột `NULL` trên 41/42 user gần như miễn phí — Postgres dùng NULL bitmap. 60 cột vẫn dưới
`care_item_templates` (56) và xa `safety_events` (81).

### 6.3. Bảng con 1-N — mảng, và ràng buộc giữ lại được nhiều hơn tưởng

`time_1, time_2, time_3…` là **repeating group** kinh điển: số lượng không cố định, thêm
mốc thứ 6 phải `ALTER TABLE`, câu "lịch nào có mốc 07:00" phải `OR` qua N cột.

Nhưng mảng Postgres **không** làm mất ràng buộc như thường nghĩ:

```sql
-- reminder_schedules: thay UNIQUE(schedule_id, local_time)
local_times time[] NOT NULL
  CHECK (cardinality(local_times) = (SELECT count(DISTINCT t) FROM unnest(local_times) t))

-- appointment_notification_configs: thay UNIQUE + CHECK offset range
offsets int[] NOT NULL
  CHECK (cardinality(offsets) = (SELECT count(DISTINCT o) FROM unnest(offsets) o))
  CHECK (NOT EXISTS (SELECT 1 FROM unnest(offsets) o WHERE o < -43200 OR o > 10080))
```

Và **thứ tự mảng thay luôn `sort_order`** — bỏ được cả cột `sort_order`, cả
`CHECK (sort_order >= 0)`, cả `UNIQUE(schedule_id, sort_order)`. Về mặt ràng buộc, mảng ở
đây **không thua** bảng con, chỉ khác cách diễn đạt.

---

## 7. KHÔNG gộp được

### 7.1. Có trigger bất biến ở tầng DB

| Bảng | Cột | Dòng | Trigger chặn |
| --- | ---: | ---: | --- |
| `audit_events` | 34 | **3.750** | `audit_events_immutable_trg` (BEFORE UPDATE, DELETE) |
| `triage_session_evidence` | 12 | 6 | `triage_session_evidence_immutable_trg` |
| `knowledge_source_reviews` | 8 | 1 | `knowledge_source_reviews_immutable_trg` |
| `consultation_context_shares` | 16 | 1 | `trg_…_append_only` |
| `consultation_context_citations` | 9 | 1 | `trg_…_append_only` |

`audit_events` còn có cột **`legal_hold`** — chịu ràng buộc pháp lý. Và đã là **đích gộp**
của 6 entity, không phải nguồn.

### 7.2. Ràng buộc pháp lý / truy vết

| Bảng | Lý do |
| --- | --- |
| `triage_sessions` (36 cột, 18 dòng) | Có `content_hash`, `schema_version`, `disclaimer_version` + trigger `triage_completed_snapshot_guard_trg`. Là **bằng chứng phiên bản** của lời khuyên y tế đã hiển thị. Gộp = mất khả năng chứng minh. |
| `data_permissions` (24 cột, 27 dòng) | Bảng đồng ý / phân quyền, phải tách để audit và thu hồi độc lập. Đã là đích gộp của 3 entity. |
| `account_deletion_requests` | Quy trình xóa tài khoản theo quy định (`scheduled_for`, `processed_by`). |
| `moderation_cases` (21 cột, 10 dòng) | Hồ sơ xử lý vi phạm, cần vòng đời và quyền truy cập riêng. |

### 7.3. Khác vòng đời hoặc khác cấp bảo mật

| Cặp bảng | Vì sao không gộp |
| --- | --- |
| `device_tokens` (7 cột, 25 dòng) vs `device_connections` (11 cột, 1 dòng) | Token FCM xoay vòng liên tục, không nhạy cảm. `device_connections` giữ **OAuth token thiết bị đeo** (`token_reference`, `scopes_jsonb`, `consent_granted_at`) — cấp bảo mật và vòng đời khác hẳn. |
| `auth_sessions` (19 cột, 102 dòng) vs `auth_challenges` (13 cột, 14 dòng) | Session sống nhiều ngày, có `refresh_token_hash`, `detected_reuse`. Challenge sống vài phút. Gộp làm bảng phình vô ích và khó dọn rác. |
| `expert_availability` vs `expert_location_shares` | Một bên là khung giờ rảnh; một bên là tọa độ GPS có `expires_at` + `consent_reference`. Khác chính sách xóa. |
| `knowledge_sources` vs `knowledge_source_reviews` | 1-N; gộp sẽ lặp toàn bộ metadata nguồn. Review còn có trigger bất biến. |
| `care_groups` vs `care_group_members` | 1-N kinh điển; `care_group_members` còn là nguồn của view `emergency_contacts`. |

### 7.4. Đã là đích gộp — thêm nữa là quá tải

`users` (52 cột, 5 entity, **71 FK đi vào** — hub lớn nhất), `care_tasks` (31 cột, 3 entity),
`health_observations` (25 cột, 6 entity), `attachments`, `care_subjects` (17 FK đi vào),
`care_item_templates` (56 cột, 4 entity, 7 trigger guard), `community_interactions` (5 entity).

### 7.5. `direct_conversation_read_cursors` không gộp vào `direct_conversations`

Cần nói rõ vì mục 5.0 dễ gây hiểu nhầm. Hai bảng khác **lực lượng quan hệ**:

```
direct_conversations              PK (conversation_id)                    → 1 dòng / hội thoại
direct_conversation_read_cursors  PK (conversation_id, reader_user_id)    → N dòng / hội thoại
```

Quan hệ **1-N**. Nhét N dòng con vào 1 dòng cha chỉ có hai cách, cả hai đều tệ hơn hiện tại:

**Cách 1 — quay lại cột cố định theo vai** (`mother_last_read_*`, `expert_last_read_*`):
đúng là thiết kế cũ đang bị khai tử. Nó chết vì chỉ mô hình hóa được 2 vai, trong khi
`FAMILY` đã có quyền vào cả 4 endpoint chat. Thêm vai thứ ba là phải `ALTER TABLE`.

**Cách 2 — JSONB `read_cursors_jsonb`:** mất bốn thứ đang có —

| Mất gì | Hiện đang có |
| --- | --- |
| FK `last_read_message_id → direct_messages` | Con trỏ không thể trỏ vào tin nhắn không tồn tại |
| PK `(conversation_id, reader_user_id)` | Mỗi người đọc đúng 1 dòng, DB tự chặn trùng |
| **Upsert nguyên tử** | `INSERT … ON CONFLICT … DO UPDATE … RETURNING` chạy **một câu lệnh**. JSONB phải read-modify-write → mother và expert cùng mark-read là **mất cập nhật** |
| Index đếm chưa đọc | `LEFT JOIN … ON rc.reader_user_id = :currentUserId` dùng index PK; JSONB path là mất index |

**Giữ nguyên 2 bảng.** Việc duy nhất là `DROP COLUMN` 4 cột chết.

### 7.6. Bảng hệ thống

`flyway_schema_history` — Flyway quản lý, **tuyệt đối không đụng**.

### 7.7. Gộp được nhưng nên dừng lại cân nhắc

| Bảng | Gộp vào | Lý do nên dừng |
| --- | --- | --- |
| `content_item_topics` (3 cột, 3 dòng) | mảng trên `content_items` | Đang là `@ElementCollection` của `ContentItem` **và** bị 3 câu native SQL trong `ContentRepository` join vào. Mất FK, index kém khi lọc theo topic. |
| `content_item_sources` (8 cột, 24 dòng) | JSONB trên `content_items` | Cũng là `@ElementCollection`. Có FK tới `knowledge_sources` — chuyển sang JSONB là mất liên kết đó. |
| `professional_specialties` (4 cột, 2 dòng) | mảng `specialty_ids` trên `users` | `users` **đã có** cột `specialty_ids` → dữ liệu có thể đang lưu 2 nơi, cần kiểm tra trước. `is_primary` là thuộc tính của *quan hệ*. |
| `ai_content_scan_jobs` (14) + `ai_content_assessments` (23) | 1 bảng | Một job sinh nhiều assessment khi rescan → gộp sẽ lặp dữ liệu job. |
| `vaccination_records` + `development_milestones` | `health_observations` | Mang ngữ nghĩa **lịch trình và trạng thái** (`scheduled_date`, `postpone_reason`, `milestone_status`), không chỉ là phép đo. |
| `vaccination_schedules` (9 cột, 7 dòng) | *giữ nguyên* | **Catalog có version** (`schedule_version`, `active_from`, `active_to`) — khác bản chất với dữ liệu bệnh nhân. |
| `expense_entries` (11 cột, 2 dòng) | `care_tasks` | Về lý thuyết gộp được qua `item_type`, nhưng chi tiêu là dữ liệu tiền — trộn vào bảng task làm mờ ranh giới nghiệp vụ. |

---

## 8. Nên TÁCH / nên xóa code chết

### 8.1. `safety_events` — 81 cột, 5.976 dòng, 5,4 MB → nên TÁCH

Bảng lớn nhất hệ thống về mọi mặt, gánh **8 entity** và gom lẫn:

- Cảm biến thô: `peak_acceleration`, `angular_velocity`, `inactivity_seconds`, `magnitude`, `accuracy_meters`
- Vị trí: `latitude`, `longitude`, `user_latitude`, `user_longitude`, `location_snapshot_jsonb`
- Vòng đời cảnh báo: `alert_generation`, `alert_status`, `alert_claim_token`, `alert_lease_expires_at`, …
- Gửi thông báo: `fcm_message_id`, `device_token_id`, `delivery_status`, `delivered_at`, `failure_code`
- **Sáu cột phân loại**: `event_type`, `response_type`, `record_type`, `action_type`, `actor_type`, `context_type`

Sáu cột `*_type` trong một bảng là dấu hiệu rõ ràng bảng đang gánh nhiều thực thể. Còn có
cặp trùng lặp `latitude`/`user_latitude`, `started_at`/`completed_at` song song với `alert_*_at`.

**Đề xuất:** tách phần cảm biến + phần delivery ra bảng riêng (hoặc JSONB), giữ lại phần
sự kiện và phản hồi. **Bảng duy nhất trong hệ thống mà tách có lợi hơn gộp.**

### 8.2. `nearby_support_interactions` — tính năng chết, code còn sống

View là `SELECT NULL::uuid, … WHERE false` — **luôn trả 0 dòng**, cộng trigger
`nearby_support_interactions_disabled_trg` chặn mọi ghi.

Nhưng package `nearbycare/` vẫn còn **12 file** đầy đủ: controller, 2 entity, 2 repository,
service impl, mapper, 2 request DTO, 2 response DTO.

**Đề xuất:** xóa cả package `com/carebridge/backend/nearbycare/` và view. Không phải gộp
bảng — là dọn code chết. Endpoint hiện tại luôn trả rỗng hoặc ném lỗi khi ghi.

---

## 9. Chờ quyết định sản phẩm

Ba hạng mục **không tự quyết được** — cần chủ sản phẩm trả lời trước.

### 9.1. `consultation_sessions` — giàn giáo cho tính năng chưa xây

9 cột mô tả **một buổi tư vấn trả phí đã thực sự diễn ra**:

```
session_id, booking_id → consultation_bookings,
communication_room_id   -- phòng gọi (Zego)
started_at, ended_at, session_status
expert_summary          -- chuyên gia ghi kết luận sau buổi tư vấn
technical_log_json      -- log kỹ thuật cuộc gọi
```

Là bước 2 của luồng **booking trả phí**: `expert_availability` → `consultation_bookings`
(đặt lịch, giá, hoa hồng, chính sách hủy) → `consultation_sessions` → `expert_summary`.

**Luồng đó khác hẳn luồng đang chạy.** Cái đang sống là `expert_consultation_requests` →
direct chat (`/api/v1/consultation-requests`). Luồng booking trả phí **chưa có controller
nào**; `consultation_bookings` cũng chỉ được một service đọc (`ShareSummaryServiceImpl`).

**Vì sao entity trông như code chết.** Javadoc trên entity:

> `// Minimal, read-only mapping (UC-113) — only the columns this reporting endpoint needs.`

Entity chỉ map 3/9 cột để phục vụ **một con số đếm** cho UC-113. Repository cũng chỉ có
`countByEndedAtIsNotNull()`.

**Và UC-113 cũng chưa được xây.** Không có `ImpactReportController`/`Service`. Bốn method
chuẩn bị sẵn cho nó đều **0 caller**:

| Repository | Method | Chỉ số |
| --- | --- | --- |
| `ConsultationSessionRepository` | `countByEndedAtIsNotNull()` | phiên tư vấn hoàn tất |
| `UserRepository:58` | `countByRole(Role)` | số bà mẹ được phục vụ |
| `PartnerOrganizationRepository:22` | `countByStatus(...)` | **tổ chức đối tác đang hoạt động** |
| `ContentRepository:150` | *(đếm nội dung đã xuất bản)* | độ phủ nội dung |

SRS mô tả UC-113 *View Impact Report* (System Admin, Priority High):
**"Displays aggregated and anonymized impact metrics for fundraising, CSR, or partners."**

**Mấu chốt: UC-113 gắn với Partner** — thứ đang bỏ. Một trong bốn chỉ số lấy thẳng từ
`partner_organizations`.

| Nếu | Thì |
| --- | --- |
| Luồng tư vấn trả phí **vẫn trong scope** | **GIỮ** `consultation_sessions`. Và **không gộp** vào `consultation_bookings`: `booking_id` **không có UNIQUE** nên schema cho phép nhiều session/booking (gọi rớt → kết nối lại, dời lịch). Thêm nữa `expert_summary` là nội dung y tế, hồ sơ lưu trữ khác bản ghi thương mại. |
| Bỏ luôn UC-113 **và** luồng booking (đi cùng Partner) | **XÓA CẢ CỤM** `consultation_sessions` + `consultation_bookings`, không xóa lẻ. |

**Không xóa lẻ `consultation_sessions`** — xóa mình nó mà giữ `consultation_bookings` là
để lại luồng cụt: đặt lịch được nhưng không ghi được buổi tư vấn nào.

### 9.2. `archived_records` — 0 tham chiếu trong code sản phẩm

| Bảng | Cột | Dòng | Entity | Native SQL |
| --- | ---: | ---: | ---: | ---: |
| `archived_records` | 11 | 1 | **0** | **0** |

Không một dòng code nào trong `src/main/java` chạm vào. Chỉ xuất hiện ở
`V1__init_schema.sql`, `V2__seed_reference_data.sql`, 4 file test
(`HealthRecord4{0,1,2}TestFactory`, `HealthRecordServiceArchiveTest`), và FK từ
`care_facilities.partner_id` (0 dòng dùng).

Cấu trúc (`legacy_table`, `legacy_id`, `payload_jsonb`, `retention_until`, `checksum`)
trông như hạ tầng archive thiết kế có chủ đích nhưng chưa nối vào đâu.
**Cần xác nhận:** dự phòng cho tính năng chưa xong, hay tàn dư?

### 9.3. UC-113 View Impact Report

Nếu bỏ Partner thì báo cáo impact "for fundraising, CSR, or partners" còn ý nghĩa không?
Nếu bỏ, dọn luôn 4 method repository chết ở mục 9.1.

---

## 10. KẾT LUẬN — bỏ gì, gộp gì, tại sao

### 10.1. BỎ

| # | Bảng | Cột | Dòng | Vì sao bỏ |
| --- | --- | ---: | ---: | --- |
| 1 | **`partner_organizations`** | 14 | 2 | **Không phát triển module Partner nữa.** 0 FK đi vào, 2 dòng đều là seed, 0 bản ghi audit liên quan → xóa an toàn tuyệt đối. |

**Dọn kèm (không phải bảng):**
- `users.role = 'PARTNER'` → `NULL` (1 tài khoản test)
- 7 giá trị `PARTNER_*` trong enum `AuditAction` → xóa được (đã xác nhận 0 dòng audit)
- 78 file code ở backend + WebApp + Mobile
- **Không** xóa `care_facilities.partner_id` trong đợt này — constraint trỏ
  `archived_records`, là vấn đề độc lập, trộn vào sẽ khó rollback

**Chờ quyết định (mục 9):** `consultation_sessions`, `consultation_bookings`,
`archived_records`.

### 10.2. GỘP

| # | Bảng bị gộp | Gộp vào | Δ | Tại sao gộp | Rủi ro |
| --- | --- | --- | ---: | --- | --- |
| 0 | *(không gộp bảng)* `DROP` 4 cột read của `direct_conversations` | — | **0** | Quan hệ 1-N nên **không gộp được** (7.5). Đây là xóa cột chết: `markMotherRead`/`markExpertRead` **0 caller** (test còn `verify(never())`), 4 cột đều `NULL`. | Thấp |
| 1 | `reminder_schedule_times` | `reminder_schedules` (mảng) | −1 | Repository chỉ `findByScheduleId…` + `deleteByScheduleId`; service **xóa sạch rồi chèn lại cả bộ**, map thẳng thành `List<LocalTime>`, vứt bỏ `time_id`/`sort_order`. **Code đã coi nó là mảng.** 1 service. | Thấp |
| 2 | `appointment_notification_rules` | `appointment_notification_configs` | −1 | Config **5 cột**, repository **0 method tùy biến**, luôn tra bằng `findById(reminderId)`. Rules cũng xóa-rồi-chèn-lại. 1 service. | Thấp |
| 3 | `reminder_occurrence_aliases` | → **VIEW** trên `care_tasks` | −1 | **Dẫn xuất 100%** do trigger sinh. Java **không hề ghi** — 1 call site đọc. Chuyển thành view không phá dòng code nào. | Thấp |
| 4 | `safety_configs` | **8 cột typed** trên `users` | −1 | 1-1 **được DB bảo đảm** bằng `UNIQUE(user_id)`; PK chưa bao giờ dùng tra cứu. Cột typed giữ được 2 CHECK và giữ hot path `FallDetectionService` nhanh — JSONB mất cả hai. **Đáng cắt nhất nếu cần thu hẹp phạm vi.** | Thấp–TB |
| 5 | `preparation_checklist_items` | `checklist_task_instances` | −1 | **Đã khai tử chính thức** — Javadoc ghi *"writes were retired by CHK-025"*, repository là `Repository` read-only, 3 endpoint write đều `throw retiredMutation()`. Đóng băng ở 2 dòng. | TB (code) |
| 6 | `growth_measurements` | `health_observations` + view | −1 | 3 phép đo = 3 `observation_type`; đích đã đủ cột; nhóm đang đi hướng này qua 2 migration gần nhất. ⚠️ **Bề mặt lớn nhất**: 4 service + 6 file Mobile + 1 trang WebApp. Bỏ luôn cột thừa `baby_id`. | TB |
| 7 | `reminder_schedule_jobs` + `appointment_notification_jobs` | `notification_jobs` | −1 | Không chỉ 11 cột trùng: **2 entity dùng chung enum `AppointmentNotificationJobStatus`**, repository trùng tên method 6/6, **2 worker giống nhau từng dòng**, 8 cờ cấu hình song song cùng mặc định. Lợi ích thật là **gộp worker**. | TB–Cao |

### 10.3. Kết quả

```
Hiện tại:        75 bảng  (74 nghiệp vụ + flyway_schema_history)
Bỏ Partner:              −1   →  74
Gộp mục 1–7:             −7   →  67
                                 ══
                                 67 bảng  (−11%)
```

Cộng thêm: bỏ **1 view chết** (`nearby_support_interactions`), **12 file code chết**
(`nearbycare/`), và **1 worker + 1 planner trùng lặp**.

Nếu ba hạng mục ở mục 9 cũng được duyệt bỏ → **64 bảng**.

### 10.4. Thứ tự thực hiện

| # | Việc | Δ | Rủi ro |
| --- | --- | ---: | --- |
| 1 | Chạy các câu kiểm tra trên Supabase (mục 4.2) | — | Không |
| 2 | Bỏ 4 cột read legacy khỏi `direct_conversations` | 0 | Thấp |
| 3 | Xóa package `nearbycare/` + view `nearby_support_interactions` | −1 view | Thấp |
| 3b | **Hỏi chủ sản phẩm** 3 câu ở mục 9 | — | Không |
| 4 | `reminder_schedule_times` → mảng trên `reminder_schedules` | −1 | Thấp |
| 5 | `appointment_notification_rules` → `configs` | −1 | Thấp |
| 6 | `reminder_occurrence_aliases` → view | −1 | Thấp |
| 7 | Gỡ Partner: **code trước** (78 file), migration sau cùng | −1 | Thấp |
| 8 | `safety_configs` → 8 cột trên `users` | −1 | TB |
| 9 | `preparation_checklist_items` → `checklist_task_instances` | −1 | TB |
| 10 | `growth_measurements` → `health_observations` + view (+ bỏ `baby_id`) | −1 | TB |
| 11 | Hai bảng job → `notification_jobs` + gộp worker/planner | −1 | TB–Cao |
| 12 | Tách dữ liệu cảm biến khỏi `safety_events` | +1 | TB |

Ba việc đầu là **xóa code chết đã xác minh** — không caller, không dữ liệu sống. Làm trước
để thu hẹp bề mặt trước khi đụng phần có người dùng thật.

Nguyên tắc xuyên suốt: **code trước, migration sau.** Nếu bước code phát hiện phụ thuộc
chưa lường trước, vẫn dừng được mà database chưa bị đụng.

---

## 11. Bốn cảnh báo trước khi bắt tay

**1. `ddl-auto: validate` là bạn, không phải kẻ thù.** Đổi schema mà quên đổi entity sẽ
**fail ngay khi khởi động** thay vì hỏng âm thầm. Nhưng nó **không** bảo vệ được **221 câu
native SQL trong 73 file** — đó mới là chỗ dễ vỡ. Mỗi lần gộp bảng phải grep lại native SQL.

**2. Đừng đặt `SPRING_PROFILES_ACTIVE=local` khi trỏ vào Supabase.** Profile `local` bật
`ddl-auto: update` → Hibernate sẽ tự `ALTER` schema **dùng chung cả nhóm** và crash-loop
backend.

**3. DB dùng chung cả nhóm.** Version `20260804160000` trên DB **khớp đúng** migration mới
nhất trong repo — nhánh này không bị lệch. Nhưng `validate-on-migrate: false` và
`ignore-migration-patterns: "*:missing"` đang che mọi lệch pha. Mọi `DROP TABLE` ảnh hưởng
tất cả người đang chạy backend — **thông báo trước.**

**4. 26 trigger là logic nghiệp vụ nằm trong DB.** Gộp bảng mà quên port trigger là mất
ràng buộc bất biến, mất guard checklist, mất khả năng ghi qua view. Danh sách đầy đủ ở 2.3.

---

## Phụ lục — toàn bộ 75 bảng

`Entity` = số class Java `@Table` trỏ vào bảng. `0` nghĩa là truy cập qua
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
