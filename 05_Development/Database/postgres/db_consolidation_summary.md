# CareBridge Database Consolidation Summary (70 → 53 Bảng)

> **Trạng thái:** DRAFT/reference-only; chưa được kiểm chứng trên database triển khai, chưa được phê duyệt và không phải kế hoạch migration/Flyway.

Bản tóm tắt này ghi lại đề xuất gộp bảng từ inventory thiết kế legacy được ghi nhận là **70 bảng** sang snapshot SQL hiện có **53 câu lệnh `CREATE TABLE`**. Con số 53 gồm **52 bảng ứng dụng** và **1 bảng metadata `flyway_schema_history`**; chưa có evidence đối chiếu database runtime trong bộ artifact này.

Nguồn triển khai hiện tại là migration Flyway `B20260724111500__canonical_70_table_baseline.sql` cùng ba Release-1 extension đã duyệt: `expert_consultation_requests`, `consultation_context_shares` và `consultation_context_citations`. Baseline có 70 core tables (69 bảng ứng dụng + `flyway_schema_history`); inventory triển khai hiệu lực là 73 bảng (72 ứng dụng + 1 technical). Snapshot 53 bảng là proposed narrowing target/reference-only, không phải runtime source of truth. So với core baseline, snapshot có 49 bảng ứng dụng chung, loại khỏi inventory 20 tên và thêm 3 bảng hợp nhất, nên `69 - 20 + 3 = 52` bảng ứng dụng.

Tên ở cột “Bảng gốc” là tên legacy/logical. Tên vật lý trong snapshot có thể khác hoặc dùng bảng tổng quát: ví dụ các capability community nằm trong `community_content`/`community_interactions`, consultation/chat/call được biểu diễn trong `archived_records`, và các event SOS nằm trong `safety_events`. `carebridge_53.sql` là inventory vật lý của snapshot nháp; migration Flyway canonical mới là nguồn triển khai.

---

## 1. So Sánh Số Lượng Bảng

| Trạng thái | Số lượng bảng | Ghi chú |
| :--- | :---: | :--- |
| **Release 1 (Legacy, theo inventory ghi nhận)** | **70 bảng** | Mốc đầu vào cần đối chiếu lại với schema/migration canonical. |
| **Phase 2 (Inventory trung gian ghi nhận)** | **65 bảng** | Mốc lịch sử chưa được kiểm chứng độc lập trong bộ artifact này. |
| **Phase 2 (Snapshot đề xuất)** | **53 `CREATE TABLE`** | 52 bảng ứng dụng + 1 bảng metadata Flyway; chênh 17 so với inventory legacy được ghi nhận. |

---

## 2. Các Ánh Xạ Hợp Nhất Đề Xuất

### Cụm 1: Tài khoản & Hồ sơ chuyên gia (Account & Expert Profile)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `user_identities` | `users` | Cột JSONB `social_identities` |
| 2 | `professional_profiles` | `users` | Nullable columns: `specialty`, `experience_years`, `workplace`, `verification_status`,... |
| 3 | `professional_specialties` | `users` | Mảng UUID `specialty_ids uuid[]` |

### Cụm 2: Phiên làm việc & Thu hồi Token (Auth Session & Revocation)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `auth_revocations` / `token_blacklist` | `auth_sessions` | Cột `detected_reuse`, `revocation_metadata_jsonb` tích hợp trực tiếp |

### Cụm 3: Nhật ký Sự kiện chung (Audit Events)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `mother_journey_events` | `audit_events` | `event_category = 'MOTHER_JOURNEY_TRANSITION'` + `payload` JSONB |
| 2 | `moderator_events` | `audit_events` | `event_category = 'MODERATION_REVIEW'` + `payload` JSONB |
| 3 | `expert_contribution_events` | `audit_events` | `event_category = 'EXPERT_CONTRIBUTION'` + `payload` JSONB |

### Cụm 4: Chỉ số & Quan sát Sức khỏe (Health Observations)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `maternal_observations` | `health_observations` | Cột `subject_type` (`MOTHER`, `BABY`, `DEPENDENT`) |

### Cụm 5: Tệp đính kèm Hồ sơ Sức khỏe (Attachments)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `health_record_attachments` (bảng trung gian) | `attachments` | FK `health_record_id` trực tiếp trên `attachments` |

### Cụm 6: Công việc & Nhắc nhở Chăm sóc (Care Tasks)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `scheduled_care_items` | `care_tasks` | `task_type = 'SCHEDULED_REMINDER'` |
| 2 | `family_tasks` | `care_tasks` | `task_type = 'MANUAL_TASK'` |

### Cụm 7: Tương trợ Lân cận (Nearby Support Interactions)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `nearby_support_requests` | `nearby_support_interactions` | Self-join `parent_interaction_id` |
| 2 | `nearby_support_responses` | `nearby_support_interactions` | Self-join `parent_interaction_id` |

### Cụm 8: An toàn & Cứu hộ khẩn cấp (Safety Events)

| # | Bảng gốc (đề xuất hợp nhất) | Bảng đích | Cơ chế dự kiến |
|---|---|---|---|
| 1 | `imu_safety_events` | `safety_events` | `record_type = 'IMU_EVENT'` |
| 2 | `emergency_sessions` | `safety_events` | `record_type = 'EMERGENCY_SESSION'` |
| 3 | `safety_event_responses` | `safety_events` | `action_type = 'RESPONSE'`, self-join `parent_event_id` |
| 4 | `emergency_alert_deliveries` | `safety_events` | `action_type = 'DELIVERY'` |
| 5 | `emergency_alert_attempts` | `safety_events` | `action_type = 'ALERT_ATTEMPT'` |
| 6 | `family_alert_log` | `safety_events` | `action_type = 'FAMILY_ALERT'` |
| 7 | `emergency_map_handoffs` | `safety_events` | `action_type = 'MAP_HANDOFF'` |
| 8 | `location_snapshots` | `safety_events` | `action_type = 'LOCATION_SNAPSHOT'` |
| 9 | `imu_monitoring_sessions` | `safety_monitoring_sessions` | Cần thiết kế migration riêng; snapshot này không thực hiện xóa legacy |
| 10 | `safety_monitoring_config` | `safety_configs` | Cần thiết kế migration riêng; snapshot này không thực hiện xóa legacy |

---

## 3. Tổng Kết Tên Legacy Dự Kiến Hợp Nhất

| Cụm | Tên legacy trong đề xuất | Số tên |
|-----|----------------|:---:|
| Tài khoản & Chuyên gia | `user_identities`, `professional_profiles`, `professional_specialties` | 3 |
| Auth Revocation | `token_blacklist` / `auth_revocations` | 1 |
| Audit Events | `mother_journey_events`, `moderator_events`, `expert_contribution_events` | 3 |
| Health Observations | `maternal_observations` | 1 |
| Attachments | `health_record_attachments` | 1 |
| Care Tasks | `scheduled_care_items`, `family_tasks` | 2 |
| Nearby Support | `nearby_support_requests`, `nearby_support_responses` | 2 |
| Safety Events | `imu_safety_events`, `emergency_sessions`, `safety_event_responses`, `emergency_alert_deliveries`, `emergency_alert_attempts`, `family_alert_log`, `emergency_map_handoffs`, `location_snapshots`, `imu_monitoring_sessions`, `safety_monitoring_config` | 10 |
| **Tổng số tên legacy được liệt kê** | | **~23 tên** |

> `~23` là số tên legacy được liệt kê trong các cụm, không phải phép tính giảm bảng ròng. Chênh lệch **70 − 53 = 17** chỉ là so sánh hai inventory có phạm vi khác nhau; cần mapping đầy đủ và evidence runtime để xác lập kết quả migration.

---

## 4. Lợi Ích Kỳ Vọng Của Cấu Trúc Đề Xuất

1. **Giảm số lượng bảng theo inventory thiết kế**: Từ 70 xuống 53 câu lệnh tạo bảng trong snapshot; hiệu quả vận hành chưa được đo.
2. **Có thể giảm một số JOIN**: Cần benchmark và kiểm thử hồi quy để xác nhận tác động hiệu năng.
3. **Cấu trúc dữ liệu linh hoạt**: Dùng JSONB cho dữ liệu động, `record_type`/`action_type` cho phân loại.
4. **Dễ mở rộng**: Self-join pattern cho phép thêm loại hành động mới chỉ bằng cách thêm giá trị enum.
