# CareBridge Database Consolidation Summary (70 → 53 Bảng)

Bản tóm tắt này thống kê toàn bộ các thay đổi gộp bảng cơ sở dữ liệu từ cấu trúc thiết kế ban đầu (**70 bảng**) xuống phiên bản tối ưu hóa (**53 bảng**) đã được kiểm chứng và phê duyệt.

---

## 1. So Sánh Số Lượng Bảng

| Trạng thái | Số lượng bảng | Ghi chú |
| :--- | :---: | :--- |
| **Release 1 (Legacy)** | **70 bảng** | Cấu trúc phân mảnh, nhiều bảng quan hệ 1-1 và bảng lịch sử sự kiện riêng biệt. |
| **Phase 2 (Ban đầu)** | **65 bảng** | Đã thu gọn sơ bộ một số thực thể. |
| **Phase 2 (Hiện tại — Đã gộp)** | **53 bảng** | **Giảm 17 bảng** so với Release 1. |

---

## 2. Chi Tiết Các Bảng Đã Gộp

### Cụm 1: Tài khoản & Hồ sơ chuyên gia (Account & Expert Profile)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `user_identities` | `users` | Cột JSONB `social_identities` |
| 2 | `professional_profiles` | `users` | Nullable columns: `specialty`, `experience_years`, `workplace`, `verification_status`,... |
| 3 | `professional_specialties` | `users` | Mảng UUID `specialty_ids uuid[]` |

### Cụm 2: Phiên làm việc & Thu hồi Token (Auth Session & Revocation)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `auth_revocations` / `token_blacklist` | `auth_sessions` | Cột `detected_reuse`, `revocation_metadata_jsonb` tích hợp trực tiếp |

### Cụm 3: Nhật ký Sự kiện chung (Audit Events)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `mother_journey_events` | `audit_events` | `event_category = ''MOTHER_JOURNEY_TRANSITION''` + `payload` JSONB |
| 2 | `moderator_events` | `audit_events` | `event_category = ''MODERATION_REVIEW''` + `payload` JSONB |
| 3 | `expert_contribution_events` | `audit_events` | `event_category = ''EXPERT_CONTRIBUTION''` + `payload` JSONB |

### Cụm 4: Chỉ số & Quan sát Sức khỏe (Health Observations)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `maternal_observations` | `health_observations` | Cột `subject_type` (`MOTHER`, `BABY`, `DEPENDENT`) |

### Cụm 5: Tệp đính kèm Hồ sơ Sức khỏe (Attachments)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `health_record_attachments` (bảng trung gian) | `attachments` | FK `health_record_id` trực tiếp trên `attachments` |

### Cụm 6: Công việc & Nhắc nhở Chăm sóc (Care Tasks)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `scheduled_care_items` | `care_tasks` | `task_type = ''SCHEDULED_REMINDER''` |
| 2 | `family_tasks` | `care_tasks` | `task_type = ''MANUAL_TASK''` |

### Cụm 7: Tương trợ Lân cận (Nearby Support Interactions)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `nearby_support_requests` | `nearby_support_interactions` | Self-join `parent_interaction_id` |
| 2 | `nearby_support_responses` | `nearby_support_interactions` | Self-join `parent_interaction_id` |

### Cụm 8: An toàn & Cứu hộ khẩn cấp (Safety Events)

| # | Bảng gốc (bị gộp) | Gộp vào bảng | Cơ chế |
|---|---|---|---|
| 1 | `imu_safety_events` | `safety_events` | `record_type = ''IMU_EVENT''` |
| 2 | `emergency_sessions` | `safety_events` | `record_type = ''EMERGENCY_SESSION''` |
| 3 | `safety_event_responses` | `safety_events` | `action_type = ''RESPONSE''`, self-join `parent_event_id` |
| 4 | `emergency_alert_deliveries` | `safety_events` | `action_type = ''DELIVERY''` |
| 5 | `emergency_alert_attempts` | `safety_events` | `action_type = ''ALERT_ATTEMPT''` |
| 6 | `family_alert_log` | `safety_events` | `action_type = ''FAMILY_ALERT''` |
| 7 | `emergency_map_handoffs` | `safety_events` | `action_type = ''MAP_HANDOFF''` |
| 8 | `location_snapshots` | `safety_events` | `action_type = ''LOCATION_SNAPSHOT''` |
| 9 | `imu_monitoring_sessions` | `safety_monitoring_sessions` | Migrate & drop legacy |
| 10 | `safety_monitoring_config` | `safety_configs` | Migrate & drop legacy |

---

## 3. Tổng Kết Bảng Đã Gộp / Loại Bỏ

| Cụm | Bảng bị loại bỏ | Số bảng giảm |
|-----|----------------|:---:|
| Tài khoản & Chuyên gia | `user_identities`, `professional_profiles`, `professional_specialties` | 3 |
| Auth Revocation | `token_blacklist` / `auth_revocations` | 1 |
| Audit Events | `mother_journey_events`, `moderator_events`, `expert_contribution_events` | 3 |
| Health Observations | `maternal_observations` | 1 |
| Attachments | `health_record_attachments` | 1 |
| Care Tasks | `scheduled_care_items`, `family_tasks` | 2 |
| Nearby Support | `nearby_support_requests`, `nearby_support_responses` | 2 |
| Safety Events | `imu_safety_events`, `emergency_sessions`, `safety_event_responses`, `emergency_alert_deliveries`, `emergency_alert_attempts`, `family_alert_log`, `emergency_map_handoffs`, `location_snapshots`, `imu_monitoring_sessions`, `safety_monitoring_config` | 10 |
| **Tổng** | | **~23 bảng** |

> **70 bảng (gốc) → 53 bảng (hiện tại)** = giảm **17 bảng trực tiếp**.

---

## 4. Lợi Ích Của Cấu Trúc Mới

1. **Giảm số lượng bảng đáng kể**: Từ 70 xuống 53 — cơ sở dữ liệu gọn gàng, dễ bảo trì.
2. **Tăng tốc độ truy vấn**: Loại bỏ nhiều bảng trung gian giúp giảm số JOIN phức tạp.
3. **Cấu trúc dữ liệu linh hoạt**: Dùng JSONB cho dữ liệu động, `record_type`/`action_type` cho phân loại.
4. **Dễ mở rộng**: Self-join pattern cho phép thêm loại hành động mới chỉ bằng cách thêm giá trị enum.
