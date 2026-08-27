# CareBridge — Đặc tả thay đổi Database sau kiểm toán và chốt phạm vi

**Ngày:** 2026-08-05  
**Trạng thái:** Đã chốt định hướng; chỉ được triển khai khi vượt qua các gate trong tài liệu  
**Nguồn:** `Database_Table_Audit_And_Consolidation V2.md`, schema canonical hiện tại và quyết định cuối của Product Owner

---

## 1. Mục đích và phạm vi

V3 là bản tổng hợp có tính thực thi cho các thay đổi database. V2 vẫn là nguồn audit chi tiết
về số dòng, code caller và hiện trạng Supabase; V3 quyết định đề xuất nào được giữ, sửa, hoãn
hoặc loại bỏ.

Tài liệu này chỉ chốt **database target và trình tự migration**. Tên migration Flyway, SQL đầy
đủ và thay đổi application code phải được thiết kế/review riêng trước khi chạy trên Supabase.

Các nguyên tắc bắt buộc:

1. Không dùng `DROP ... CASCADE`.
2. Không drop object khi code đang deploy vẫn map hoặc query object đó.
3. Mọi thay đổi phá vỡ contract đi theo `expand → backfill → verify → switch code → contract`.
4. Số dòng trong V2 chỉ là snapshot ngày 2026-08-05; migration phải truy vấn lại DB thật.
5. Mỗi Flyway migration/object có transaction và rollback boundary riêng; release wave không
   phải một transaction atomic và không được gom toàn bộ thay đổi vào một file.

---

## 2. Kết luận điều hành

### 2.1. Kết quả dự kiến

| Mốc | Số bảng `public` | Ghi chú |
| --- | ---: | --- |
| Hiện tại theo V2 | 75 | 74 bảng nghiệp vụ + `flyway_schema_history` |
| Sau các thay đổi được duyệt trong phạm vi hiện tại | **64** | Giảm ròng 11 bảng |
| Sau khi gộp growth ở wave riêng | **63** | Chưa tính việc tách `safety_events` |
| Sau khi tách `safety_events` | Chưa chốt | Sẽ tăng ít nhất 1 bảng; cần thiết kế riêng |

Ngoài thay đổi số bảng:

- Drop 1 view chết: `nearby_support_interactions`.
- Drop 4 cột read legacy khỏi `direct_conversations`.
- Giữ `users.settings_jsonb` trong giai đoạn hiện tại.
- Giữ bảng vật lý `reminder_occurrence_aliases`.

Giảm ròng 11 bảng trong phạm vi hiện tại được tính như sau:

| Object bị loại/gộp | Δ bảng |
| --- | ---: |
| `account_deletion_requests`, `account_lock_appeals`, `archived_records` | −3 |
| `device_connections` | −1 |
| `partner_organizations` | −1 |
| `reminder_schedule_times` | −1 |
| `appointment_notification_rules` | −1 |
| Hai bảng job → một `notification_jobs` | −1 |
| `safety_configs` | −1 |
| `preparation_checklist_items` | −1 |
| `consultation_sessions` | −1 |
| **Tổng** | **−11** |

### 2.2. Decision register

| # | Thay đổi | Quyết định cuối | Rủi ro |
| --- | --- | --- | --- |
| 1 | Drop `account_deletion_requests` | Duyệt có điều kiện; thay bằng deactivation trực tiếp trên `users` | Trung bình |
| 2 | Drop `account_lock_appeals` | Duyệt có điều kiện; audit/ticket CSKH phải thay thế lịch sử appeal | Trung bình |
| 3 | Drop `archived_records` | Duyệt sau khi gỡ inbound FK và cột legacy của `care_facilities` | Cao nếu drop trực tiếp |
| 4 | Drop `device_connections` | Duyệt vì bỏ IoT; phải xử lý provenance trong `health_observations` | Cao nếu drop trực tiếp |
| 5 | Drop `partner_organizations` | Duyệt | Thấp–trung bình |
| 6 | Drop 4 cột read legacy | Duyệt; code contract phải đi trước | Thấp |
| 7 | Drop view `nearby_support_interactions` | Duyệt; gỡ mapping code trước | Thấp |
| 8 | Drop `settings_jsonb` | **Không triển khai hiện tại** | Critical nếu drop ngay |
| 9 | Appointment rules → JSONB trong config | Duyệt với validator DB và `config_revision` | Trung bình |
| 10 | Hai bảng notification job → một bảng polymorphic | Duyệt theo thiết kế typed-polymorphic, không dùng FK giả `source_type/source_id` | Cao |
| 11 | `preparation_checklist_items` → `checklist_task_instances` | Duyệt; phải tạo/xác định parent instance | Trung bình |
| 12 | `consultation_sessions` → `consultation_bookings` | Duyệt sau kiểm tra cardinality | Trung bình–cao |
| 13 | `growth_measurements` → `health_observations` | Duyệt định hướng, triển khai khi đủ prerequisite | Cao |
| 14 | `reminder_schedule_times` → mảng trên `reminder_schedules` | Duyệt từ V2 | Thấp |
| 15 | `safety_configs` → cột typed trên `users` | Duyệt từ V2 | Trung bình |
| 16 | `reminder_occurrence_aliases` → view | **Không duyệt** sau review canonical migration | Cao về mất lịch sử |
| 17 | Tách `safety_events` | Duyệt về hướng, bắt buộc có design doc riêng | Cao |

---

## 3. Thay đổi chi tiết

### 3.1. Vòng đời tài khoản

#### 3.1.1. Drop `account_deletion_requests`

Quy trình mới là **deactivation ngay lập tức**, không còn hàng đợi xóa 30 ngày.

Target trên `users`:

- `enabled = false`
- `account_status = 'DEACTIVATED'`
- `deactivated_at = now()`
- `deactivation_reason` được ghi nếu người dùng cung cấp
- `deactivated_by = user_id` cho self-deactivation, hoặc actor CSKH/Admin nếu thao tác hộ

Trong cùng transaction nghiệp vụ phải thu hồi refresh session và device token. Request tiếp
theo bị chặn bởi `users.enabled = false`.

Gate trước migration:

```sql
SELECT status, count(*)
FROM public.account_deletion_requests
GROUP BY status;
```

Mọi request `PENDING` phải được giải quyết bằng một trong ba cách có biên bản: chuyển thành
deactivation, hủy, hoặc giữ tạm cho đến khi hết grace period. Không được drop làm mất request
đang chờ mà không có quyết định.

Nên bổ sung constraint sau khi code mới đã hoạt động:

```sql
CHECK (
  account_status IS DISTINCT FROM 'DEACTIVATED'
  OR (enabled = false AND deactivated_at IS NOT NULL)
)
```

Tên chức năng/API phải phản ánh đây là logical deactivation. Nếu sau này cần xóa hoặc ẩn danh
PII, phải thiết kế purge workflow riêng; việc drop bảng request không đáp ứng yêu cầu đó.

#### 3.1.2. Drop `account_lock_appeals`

Admin khóa/mở khóa trực tiếp trên `users`. Trước khi drop:

- Không còn appeal `PENDING`.
- CSKH có ticket ID hoặc `audit_events` ghi actor, user, lock episode, lý do và thời điểm.
- Code không còn query trạng thái appeal trong authentication policy.

Không cần thêm bảng thay thế nếu hệ thống ticket CSKH là nguồn lịch sử chính.

#### 3.1.3. Drop `archived_records`

Không thể drop trực tiếp vì hiện có:

```text
care_facilities.partner_id → archived_records.archive_id
```

Thực hiện trong migration riêng:

1. Kiểm tra `care_facilities.partner_id IS NOT NULL`.
2. Xác nhận không có dependency runtime khác qua `pg_depend`/`pg_constraint`.
3. Drop `care_facilities_partner_archive_fk`.
4. Drop index `idx_care_facilities_partner_id`.
5. Drop cột `care_facilities.partner_id` vì cả Partner lẫn archive đều bị loại.
6. Drop `archived_records` sau khi lưu snapshot/backup cần thiết.

---

### 3.2. Loại bỏ tích hợp thiết bị sức khỏe

Schema canonical hiện dùng tên `device_connections`; `health_device_connections` là tên
legacy còn xuất hiện trong tài liệu cũ, không phải bảng thứ hai trong inventory V2.

Phụ thuộc phải xử lý:

- `health_observations.device_connection_id`
- `health_observations_device_time_ix`
- `health_observations_device_connection_id_fkey`
- `health_observations_device_fk`
- FK từ `device_connections.user_id` về `users`

Gate dữ liệu:

```sql
SELECT count(*) AS linked_observations
FROM public.health_observations
WHERE device_connection_id IS NOT NULL;
```

Nếu có dữ liệu thật, trước khi drop phải snapshot `provider_name`, `device_name` và connection
ID cần truy vết vào `health_observations.raw_payload_jsonb`. Nếu chỉ là seed/test và Product
Owner chấp nhận loại bỏ, có thể xóa/null liên kết có kiểm soát. Sau đó drop hai FK bằng tên
explicit với `IF EXISTS`, drop index, drop cột và cuối cùng drop bảng.

JSON provenance dùng namespace ổn định, ví dụ:

```json
{
  "deviceProvenance": {
    "connectionId": "uuid",
    "providerName": "string",
    "deviceName": "string-or-null",
    "capturedAt": "ISO-8601"
  }
}
```

Không copy `token_reference`, OAuth secret hoặc credential vào observation payload.

Không drop `device_tokens`; đây là token push notification, khác thiết bị IoT.

---

### 3.3. Loại bỏ Partner

Thứ tự database:

1. Kiểm tra user role `PARTNER` và dữ liệu không còn là dữ liệu thật.
2. Chuyển role theo quyết định nghiệp vụ, không mặc định gán `NULL` nếu user thật tồn tại.
3. Tạo lại `users_role_check` không còn `PARTNER`.
4. Drop seed và bảng `partner_organizations`.

`care_facilities.partner_id` không FK tới `partner_organizations`; cột này được xử lý cùng
`archived_records` ở mục 3.1.3.

---

### 3.4. Dọn contract chat và nearby support

#### Direct conversation

Drop các cột:

- `mother_last_read_at`
- `mother_last_read_message_id`
- `expert_last_read_at`
- `expert_last_read_message_id`

Gate:

- Bốn cột không còn dữ liệu cần backfill.
- Entity JPA không còn map bốn cột.
- Repository không còn native SQL ghi `mother_last_read_at`/`expert_last_read_at`.
- Read path duy nhất dùng `direct_conversation_read_cursors`.

#### Nearby support

Sau khi bỏ mapping/repository code:

1. Drop trigger `nearby_support_interactions_disabled_trg`.
2. Drop view `nearby_support_interactions`.
3. Drop function reject-write nếu không còn dependency.

---

### 3.5. Giữ `users.settings_jsonb`

Không drop trong chương trình consolidation này. Cột đang lưu dữ liệu nghiệp vụ:

- notification preferences và appointment reminder defaults;
- privacy settings;
- `activeBabyId`;
- một số key security/compatibility như `suspendedUntil`, `lockedAt`, `mustChangePassword`.

Việc không phát triển theme/language không làm các key trên trở thành dư thừa. Có thể mở một
wave chuẩn hóa riêng để chuyển từng key sang typed column hoặc bảng domain, nhưng chỉ được
drop `settings_jsonb` sau khi không còn reader/writer và đã đối soát toàn bộ key thực tế.

---

### 3.6. `reminder_schedule_times` → `reminder_schedules.local_times`

Thêm `local_times time[]`, backfill theo `sort_order, local_time`, sau đó chuyển code và drop
`reminder_schedule_times`.

Validator phải bảo đảm:

- mảng không rỗng nếu schedule active yêu cầu ít nhất một giờ;
- không có phần tử `NULL`;
- không có giờ trùng;
- thứ tự mảng là thứ tự hiển thị/thực thi.

Lưu ý kỹ thuật: PostgreSQL không cho subquery trực tiếp trong `CHECK`. Ví dụ `CHECK` dùng
`SELECT ... FROM unnest(...)` trong V2 phải được hiện thực bằng immutable validator function
hoặc constraint trigger, rồi `CHECK (validate_reminder_local_times(local_times))`.

---

### 3.7. Appointment notification rules → JSONB

Giữ bảng `appointment_notification_configs`, thêm `rules_jsonb jsonb NOT NULL DEFAULT '[]'`.
Mỗi phần tử tối thiểu có `offsetMinutes`; thứ tự phần tử thay `sort_order`.

Schema logical v1:

```json
[
  { "offsetMinutes": -1440 },
  { "offsetMinutes": -60 }
]
```

DB validator phải bảo đảm:

- root là array;
- mỗi offset là integer trong `[-43200, 10080]`;
- không có offset trùng;
- cấu trúc không chứa key không được hỗ trợ nếu chọn schema đóng.

Việc backfill và tăng `config_revision` phải nằm trong cùng transaction. Job đã tạo vẫn giữ
snapshot `config_revision` và `offset_minutes`; không đọc lại rules hiện tại để diễn giải job
lịch sử.

Sau khi code đọc/ghi JSONB và đối soát 20 rule hiện tại, drop
`appointment_notification_rules`.

---

### 3.8. Hai bảng notification job → `notification_jobs`

Không dùng thiết kế generic `source_type + source_id` vì PostgreSQL không thể tạo FK động.
Target là **typed-polymorphic**:

- Cột chung: `job_id`, `job_type`, `due_at`, `status`, retry/lock fields,
  `notification_record_id`, error và audit timestamps.
- Nhánh schedule: `schedule_id`, `schedule_revision`, `occurrence_date`, `local_time`,
  `time_zone`.
- Nhánh appointment: `reminder_id`, `occurrence_id`, `occurrence_generation`,
  `occurrence_scheduled_at`, `config_revision`, `offset_minutes`.

Ràng buộc bắt buộc:

- `job_type IN ('REMINDER_SCHEDULE','APPOINTMENT')`.
- CHECK theo discriminator: đúng bộ cột của nhánh phải non-null, bộ còn lại phải null.
- `schedule_id` giữ FK tới `reminder_schedules(schedule_id) ON DELETE CASCADE`.
- `reminder_id` giữ FK hiện tại tới `care_tasks(task_id) ON DELETE CASCADE`.
- Giữ FK `notification_record_id`.
- `notification_record_id` tiếp tục `ON DELETE SET NULL`.
- Partial unique index cho schedule:
  `(schedule_id, schedule_revision, occurrence_date, local_time)
  WHERE job_type = 'REMINDER_SCHEDULE'`.
- Partial unique index cho appointment:
  `(reminder_id, occurrence_id, config_revision, offset_minutes)
  WHERE job_type = 'APPOINTMENT'`.
- Giữ status, attempt, lock-shape, generation, revision và offset CHECK hiện có.

Mapping nguồn → đích:

| Nguồn | `job_type` | Identity phải giữ |
| --- | --- | --- |
| `reminder_schedule_jobs` | `REMINDER_SCHEDULE` | `schedule_id, schedule_revision, occurrence_date, local_time` |
| `appointment_notification_jobs` | `APPOINTMENT` | `reminder_id, occurrence_id, config_revision, offset_minutes` |

`occurrence_generation` được giữ như snapshot và có CHECK `>= 0`, nhưng không thêm vào unique
identity vì `occurrence_id` được sinh bởi occurrence-ID v2 và đã phân biệt generation. Nếu code
tạo occurrence ID không còn bảo đảm điều này tại thời điểm triển khai, phải dừng và thêm
`occurrence_generation` vào identity thay vì âm thầm đổi semantics.

Trước backfill phải chứng minh hai nguồn không trùng `job_id`:

```sql
SELECT job_id FROM reminder_schedule_jobs
INTERSECT
SELECT job_id FROM appointment_notification_jobs;
-- expected: 0 rows
```

Nếu có va chạm, migration phải tạo mapping ID cũ → ID mới và cập nhật mọi reference; không
được dựa vào `ON CONFLICT DO NOTHING` vì sẽ làm mất job.

Cutover:

1. Dừng planner tạo job mới.
2. Chờ không còn job `PROCESSING`; requeue lock stale.
3. Chạy final-delta backfill vào `notification_jobs` đã tạo ở Wave 1A, giữ nguyên `job_id`
   và trạng thái.
4. Đối soát count theo loại/trạng thái và unique identity.
5. Deploy worker/repository mới.
6. Quan sát ít nhất một chu kỳ planner + worker thành công.
7. Drop hai bảng cũ ở migration contract riêng.

---

### 3.9. `safety_configs` → typed columns trên `users`

Không đưa safety config vào `settings_jsonb`. Thêm các cột có prefix rõ ràng trên `users`:

- `fall_detection_enabled boolean NOT NULL DEFAULT false`
- `fall_detection_sensitivity_level varchar(10) NOT NULL DEFAULT 'MEDIUM'`
- `emergency_auto_alert boolean NOT NULL DEFAULT true`
- `emergency_countdown_seconds integer NOT NULL DEFAULT 30`
- `sensor_permission_granted boolean NOT NULL DEFAULT false`
- `sensor_permission_recorded_at timestamptz NULL`
- `safety_config_updated_at timestamptz NOT NULL DEFAULT now()`
- `safety_config_updated_by uuid NULL REFERENCES users(user_id)`

Port đầy đủ FK `updated_by` và hai CHECK hiện tại, đặc biệt permission timestamp và tập giá
trị countdown. Backfill 1 dòng hiện có, chuyển repository/hot path, đối soát rồi mới drop
`safety_configs`.

---

### 3.10. `preparation_checklist_items` → checklist v2

Không copy trực tiếp sang `checklist_task_instances`, vì bảng đích bắt buộc:

- `checklist_instance_id`;
- deterministic `task_key`;
- `target_subject`;
- template pair hợp lệ theo origin của parent;
- terminal timestamp phù hợp status.

Với từng dòng legacy, migration phải tìm parent instance tương ứng hoặc tạo parent
`USER_CREATED` có care-group/context hợp lệ. Nếu có `template_entry_id`, chỉ map sang
`SYSTEM_TEMPLATE` khi resolve được đúng template version/item version; không được tạo FK giả.

Nếu row resolve thành system template nhưng chưa có parent phù hợp, migration được phép tạo
parent `SYSTEM_TEMPLATE` đúng lineage/version/recipient/context và phải vượt toàn bộ FK/trigger
guard hiện tại. Nếu không thể tạo parent hợp lệ, row là unresolved và migration phải fail; không
được thay bằng parent `USER_CREATED`.

Row có `template_entry_id` nhưng không resolve được phải được ghi vào migration reconciliation
report và làm migration fail. Không tự động hạ cấp thành `USER_CREATED`, vì thao tác đó làm mất
lineage. Contract migration chỉ được chạy khi unresolved count bằng 0.

`task_key` phải dùng cùng thuật toán `checklist_v1_key` của canonical migration:

- system template: `checklist_v1_key(parent_instance_id, template_item_version_id)`;
- user-created/legacy: `checklist_v1_key(parent_instance_id, 'USER_CREATED', checklist_item_id)`.

Không tạo thuật toán hash thứ hai chỉ cho đợt consolidation này.

Gate hoàn thành: số dòng nguồn bằng số mapping thành công, không trùng `task_key`, GET v2 trả
đủ dữ liệu tương đương, sau đó mới drop bảng legacy.

---

### 3.11. `consultation_sessions` → `consultation_bookings`

Product xác nhận từ thời điểm chuyển đổi, consultation booking chỉ còn luồng miễn phí và mỗi
booking gắn với tối đa một buổi tư vấn logic. Đây phải là invariant toàn cục của bảng sau
migration, không chỉ là nhận xét về dữ liệu seed hiện tại. Nếu vẫn còn booking trả phí/legacy
cho phép nhiều session, **không được drop `consultation_sessions`**.

Gate:

```sql
SELECT booking_id, count(*)
FROM public.consultation_sessions
GROUP BY booking_id
HAVING booking_id IS NULL OR count(*) > 1;
```

Mọi ngoại lệ phải được reconcile, sau đó chạy lại query và nhận **0 dòng**. Biên bản giải thích
không thay thế điều kiện zero-row trước contract migration.

Gate loại bỏ paid/legacy flow:

```sql
SELECT count(*)
FROM consultation_bookings
WHERE expert_price_id IS NOT NULL
   OR price_band_id IS NOT NULL
   OR price_snapshot_amount IS NOT NULL
   OR commission_rate_snapshot IS NOT NULL
   OR price_locked_at IS NOT NULL;
-- expected trước contract: 0
```

Nếu ban đầu khác 0, phải chuyển đổi từng booking rồi chạy lại query. Không drop
`consultation_sessions` chỉ dựa trên biên bản chấp nhận ngoại lệ.

Thêm vào `consultation_bookings`:

- `communication_room_id`
- `session_started_at`, `session_ended_at`, `session_status`
- `expert_summary`
- `technical_log_json`
- `session_created_at`
- `legacy_session_id` để đối soát và truy vết trong giai đoạn chuyển đổi

Backfill theo `booking_id`, xác minh 1–1, chuyển code rồi drop `consultation_sessions`.
`legacy_session_id` chỉ được drop ở wave sau nếu xác nhận không có external/audit reference.
Vì session fields nằm trực tiếp trên một booking row, schema đích tự giới hạn tối đa một logical
session/booking. Bổ sung CHECK shape cho `session_status`, `session_started_at` và
`session_ended_at` để chặn trạng thái hoàn tất không có timestamps hoặc `ended_at < started_at`.

---

### 3.12. `growth_measurements` → `health_observations` — wave sau

Chưa triển khai cho đến khi hoàn tất:

1. Thêm `measurement_group_id uuid` để một lần đo gồm weight/height/head circumference vẫn là
   một aggregate logic.
2. Bổ sung soft-delete hoặc trạng thái tương đương vì `growth_measurements.deleted_at` chưa
   có đối tác trong `health_observations`.
3. Mở `health_metric_definitions.subject_type` cho `BABY`; hiện constraint chỉ cho `MOTHER`.
4. Chốt mapping unit, source, note, timestamps và update/delete theo group.
5. Chuẩn bị compatibility view hoặc chuyển toàn bộ 4 service và frontend trong cùng release.

Sau backfill, một dòng growth có thể sinh tối đa ba observation nhưng phải giữ liên kết group
và legacy identity để migration idempotent. Dùng `care_subject_id` hiện có với
`subject_type = 'BABY'`; trước khi insert phải xác minh care subject thật có type `BABY`.

Identity đề xuất tận dụng unique `(legacy_source, legacy_id)` hiện có trên
`health_observations`:

- `legacy_source = 'growth_measurements'`
- `legacy_id = '<growth_measurement_id>:WEIGHT'`, `:HEIGHT` hoặc `:HEAD_CIRCUMFERENCE`

Nhờ đó rerun backfill không nhân bản observation. `measurement_group_id` dùng trực tiếp
`growth_measurement_id`; cả hai đều là UUID. Ba observation phát sinh từ cùng source row phải
có cùng giá trị này.

---

### 3.13. Giữ `reminder_occurrence_aliases` — sửa kết luận V2

Không chuyển thành view. Canonical migration mô tả rõ đây là **durable reminder occurrence
identity**, độc lập với dữ liệu command retention. Bảng giữ alias lịch sử khi schedule thay đổi
và phân biệt `occurrence_generation`; view trên trạng thái hiện tại của `care_tasks` không thể
tái tạo đầy đủ các generation cũ.

Giữ bảng, unique constraint, indexes, ID functions và trigger capture hiện tại. Việc bảng không
có FK tới `care_tasks` là có chủ đích: alias phải tồn tại độc lập khi task hiện tại thay đổi hoặc
bị purge theo retention khác. Test bắt buộc phải chứng minh generation cũ vẫn truy vấn được sau
khi update schedule hoặc xóa nguồn theo policy được phép.

---

### 3.14. Tách `safety_events` — chương trình riêng

Định hướng tách được duyệt nhưng chưa đủ chi tiết để viết migration. Design doc riêng phải chốt:

- bảng lõi event/response nào được giữ;
- sensor sample/snapshot nào tách riêng;
- alert attempt/delivery nào tách riêng;
- PK/FK, retention, immutability và quyền truy cập;
- cách migrate 5.976 dòng và 8 entity hiện tại;
- target table count và chiến lược compatibility.

Không đưa thay đổi này vào cùng release với notification jobs hoặc growth consolidation.

---

## 4. Những đề xuất V2 không triển khai

Ngoài `reminder_occurrence_aliases`, giữ nguyên các bảng sau theo kết luận “không gộp” của V2:

- `direct_conversation_read_cursors`
- `audit_events`, `triage_session_evidence`, `knowledge_source_reviews`
- `triage_sessions`, `data_permissions`, `moderation_cases`
- `device_tokens`, `auth_sessions`, `auth_challenges`
- `content_item_topics`, `content_item_sources`, `professional_specialties`
- `vaccination_records`, `vaccination_schedules`, `development_milestones`
- `care_groups`, `care_group_members`
- `flyway_schema_history`

Không gộp thêm các bảng trên chỉ để đạt chỉ tiêu số lượng.

---

## 5. Migration và release waves đề xuất

| Wave | Loại | Nội dung | Exit gate |
| --- | --- | --- | --- |
| 0 | Chuẩn bị | Snapshot schema/data, catalog dependency, restore point, rehearsal trên clone | Query/evidence được lưu trong change ticket |
| 1A | **Expand DB** | Thêm `local_times`, `rules_jsonb`, safety columns, booking session columns, `notification_jobs`, validators/indexes; chưa drop object | DDL additive chạy được với code cũ |
| 1B | **Compatibility code** | Code dual-read/write hoặc tiếp tục source-write trong khi chuẩn bị target; bỏ caller Partner/nearby/appeal/deletion queue và legacy chat | Cả code cũ/new contract tương thích schema expand |
| 2 | **Backfill/reconcile** | Times, rules, safety, checklist, consultation; account pending workflow; device provenance; Partner roles. Riêng job: freeze planner/source writes, chờ hết `PROCESSING`, backfill + final delta trong transaction | Expected count đạt, unresolved = 0; job source quiesced |
| 3 | **Code cutover** | Chỉ đọc target; dừng source writes; job planner/worker mới nhận quyền xử lý | Không còn query/write source trong logs và static scan |
| 4 | **Observation** | Chạy ít nhất một release ổn định; job chạy qua một planner + retry cycle; kiểm tra checklist/consultation | Không có mismatch/error; rollback vẫn dùng source được |
| 5 | **Low-risk contract** | Drop chat columns, nearby view, Partner table, times, appointment rules, safety config, checklist legacy, consultation sessions | Từng object vượt dependency/preflight gate |
| 6 | **Account/device/archive contract** | Drop account workflow tables, device integration, `care_facilities.partner_id`, archive table | Không pending workflow; provenance và backup được duyệt |
| 7 | **Queue contract** | Drop hai job source tables | Queue target ổn định và source không nhận write mới |
| 8 | Chương trình riêng | Growth: lặp lại đầy đủ expand → backfill → cutover → observe → contract | Tất cả prerequisite mục 3.12 đạt |
| 9 | Chương trình riêng | Tách `safety_events` | Design doc và migration matrix riêng được duyệt |

Không đặt expand và contract của cùng object trong một deployment. Với queue, checklist và
consultation, source table phải tồn tại suốt observation window. Nếu không thể dual-write, phải
freeze write trong lúc backfill/cutover và ghi rõ downtime window.

Không được bắt đầu target-only write nếu chưa có reverse-sync đã kiểm chứng. Lựa chọn đơn giản
hơn là giữ source-write/dual-write đến đúng cutover, sau đó freeze ngắn, chạy final delta backfill
và chuyển reader/writer atomically ở release boundary.

Mỗi object trước khi triển khai cần một migration matrix con gồm: source/target DDL, mapping,
expected counts, constraints/index/RLS/grant, preflight expected result, deployment boundary,
rollback action và contract gate. V3 chốt chương trình tổng thể, không thay thế matrix/SQL chi
tiết của từng Flyway migration.

---

## 6. Preflight tối thiểu trên Supabase

Trước mỗi contract migration, lưu kết quả các nhóm query sau vào change ticket:

```sql
-- Dependency thật của object sắp drop
SELECT conname, conrelid::regclass, confrelid::regclass,
       pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass,
        'public.account_lock_appeals'::regclass,
        'public.archived_records'::regclass,
        'public.device_connections'::regclass
      ])
   OR confrelid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass,
        'public.account_lock_appeals'::regclass,
        'public.archived_records'::regclass,
        'public.device_connections'::regclass
      ]);

-- Các blocker dữ liệu chính
SELECT count(*) FROM account_deletion_requests WHERE status = 'PENDING';
SELECT count(*) FROM account_lock_appeals WHERE status = 'PENDING';
SELECT count(*) FROM care_facilities WHERE partner_id IS NOT NULL;
SELECT count(*) FROM health_observations WHERE device_connection_id IS NOT NULL;
SELECT count(*) FROM users WHERE role = 'PARTNER';

SELECT booking_id, count(*)
FROM consultation_sessions
GROUP BY booking_id
HAVING booking_id IS NULL OR count(*) > 1;
```

Ngoài ra phải kiểm tra view, trigger, function, policy/RLS, grant và native SQL liên quan; FK
không phải dependency duy nhất của một object PostgreSQL.

Catalog queries tối thiểu cho object sắp drop:

```sql
-- Dependency tổng quát, bao gồm dependency không phải FK
SELECT pg_describe_object(d.classid, d.objid, d.objsubid) AS dependent,
       pg_describe_object(d.refclassid, d.refobjid, d.refobjsubid) AS referenced,
       d.deptype
FROM pg_depend d
WHERE d.refobjid = ANY (ARRAY[
  'public.account_deletion_requests'::regclass::oid,
  'public.account_lock_appeals'::regclass::oid,
  'public.archived_records'::regclass::oid,
  'public.device_connections'::regclass::oid
]);

-- View/materialized-view rewrite phụ thuộc target object
SELECT dependent_view.oid::regclass AS dependent_view,
       pg_get_viewdef(dependent_view.oid, true) AS definition
FROM pg_rewrite rewrite
JOIN pg_class dependent_view ON dependent_view.oid = rewrite.ev_class
WHERE dependent_view.relkind IN ('v', 'm')
  AND EXISTS (
    SELECT 1
    FROM pg_depend d
    WHERE d.classid = 'pg_rewrite'::regclass
      AND d.objid = rewrite.oid
      AND d.refobjid = ANY (ARRAY[
        'public.account_deletion_requests'::regclass::oid,
        'public.account_lock_appeals'::regclass::oid,
        'public.archived_records'::regclass::oid,
        'public.device_connections'::regclass::oid
      ])
  );

-- Trigger không-internal
SELECT tgrelid::regclass AS relation, tgname,
       pg_get_triggerdef(oid)
FROM pg_trigger
WHERE NOT tgisinternal
  AND tgrelid = ANY (ARRAY[
    'public.account_deletion_requests'::regclass::oid,
    'public.account_lock_appeals'::regclass::oid,
    'public.archived_records'::regclass::oid,
    'public.device_connections'::regclass::oid
  ]);

-- RLS policies
SELECT schemaname, tablename, policyname, roles, cmd, qual, with_check
FROM pg_policies
WHERE schemaname = 'public'
  AND tablename IN (
    'account_deletion_requests', 'account_lock_appeals',
    'archived_records', 'device_connections'
  );

-- Grants
SELECT table_schema, table_name, grantee, privilege_type
FROM information_schema.table_privileges
WHERE table_schema = 'public'
  AND table_name IN (
    'account_deletion_requests', 'account_lock_appeals',
    'archived_records', 'device_connections'
  );
```

Các object khác phải thay danh sách target tương ứng; không giới hạn preflight ở bốn bảng ví dụ.

---

## 7. Validation và go/no-go

Một wave chỉ được coi là hoàn tất khi:

1. Flyway chạy thành công trên clone schema/data gần production.
2. Row-count và business-key count trước/sau khớp với mapping đã duyệt.
3. Không có orphan FK, duplicate identity hoặc invalid JSON/array.
4. Backend khởi động với `ddl-auto: validate` và không còn native SQL gọi object cũ.
5. Job/checklist/consultation flow liên quan được chạy end-to-end.
6. Log không có `relation does not exist`, `column does not exist`, constraint hoặc cast error.
7. Có backup/snapshot và forward-fix plan trước khi drop source object.

No-go nếu còn bất kỳ điều kiện nào:

- pending account workflow chưa reconcile;
- `settings_jsonb` còn reader/writer nhưng migration định drop;
- device observation chưa có quyết định provenance;
- consultation có null/duplicate booking chưa xử lý;
- notification job còn `PROCESSING` khi cutover;
- checklist row không resolve được parent/context/template;
- migration sử dụng `CASCADE` để vượt dependency không hiểu rõ.

### 7.1. Rollback boundary

- Expand và backfill phải idempotent/reversible trước code cutover.
- Source table không bị drop trong cùng deployment với cutover; đây là rollback path chính.
- Trong observation window, rollback code phải đọc lại source. Nếu target đã nhận write mới,
  phải có reverse-sync SQL hoặc dual-write evidence trước khi rollback.
- Trước contract migration phải có logical dump hoặc restore point/PITR đã test; ghi rõ thời
  điểm và người xác nhận trong change ticket.
- Sau khi contract `DROP` đã commit, rollback không còn là `undo` đơn giản. Chỉ được forward-fix
  hoặc restore từ restore point, vì vậy contract gate cần phê duyệt riêng của migration owner và
  Product/Tech Lead cho mọi dữ liệu được loại bỏ.

---

## 8. Nguồn sự thật

- Audit hiện trạng và số liệu: `08_References/Database_Table_Audit_And_Consolidation V2.md`
- Baseline canonical: `05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql`
- Checklist, occurrence alias và appointment scheduling:
  `V20260731070000__canonical_post_20260719180000_schema.sql`
- Reminder schedules/jobs: `V20260803110000__separate_reminder_schedules.sql`

Nếu DB thật khác các file trên, catalog Supabase tại thời điểm migration là nguồn quyết định;
phải dừng và cập nhật tài liệu/migration thay vì cố chạy tiếp.
