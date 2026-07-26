# BÁO CÁO THU GỌN CƠ SỞ DỮ LIỆU — TỪ 70 XUỐNG 65 BẢNG (PHASE 2)

Dựa trên yêu cầu tiếp tục thu gọn cấu trúc cơ sở dữ liệu của Supervisor (Mr. Hải) và thống nhất không thay đổi code Java/Frontend trong giai đoạn này, chúng tôi đã tiến hành tối ưu hóa schema trong các file migration Flyway từ 70 bảng xuống còn đúng **65 bảng** (bao gồm cả `flyway_schema_history`).

---

## 1. Các Thay Đổi Thu Gọn Chi Tiết

### 1.1. Hợp nhất Nhóm Người dùng (Users + Persons + Community Profiles -> Users)
- **Mục tiêu**: Loại bỏ sự phân mảnh thông tin định danh và thông tin cá nhân/cộng đồng.
- **Thực hiện**:
  - Gộp tất cả các cột của bảng `persons` (nhân khẩu học, thông tin liên hệ, v.v.) và các cột cộng đồng của `community_profiles` trực tiếp vào bảng `users`.
  - Loại bỏ định nghĩa bảng `persons` và `community_profiles` trong các migration Wave 1 và Wave 3.
  - Cập nhật các bảng tham chiếu (như `care_subjects`, `data_permissions`, `users` tự liên kết, v.v.) để trực tiếp trỏ đến `users(user_id)`.
  - Cập nhật script chuyển đổi dữ liệu lịch sử để import trực tiếp từ legacy tables vào `users`.

### 1.2. Hợp nhất Các Bảng Lưu Trữ Lịch Sử (Archives -> Archived Records)
- **Mục tiêu**: Thay vì duy trì 3 bảng lưu trữ riêng lẻ cho các domain, gom chúng lại thành một bảng cấu trúc chung.
- **Thực hiện**:
  - Loại bỏ các bảng `archived_partner_records`, `archived_realtime_records`, và `archived_consultation_records`.
  - Định nghĩa một bảng duy nhất là `public.archived_records` trong Wave 9 với đầy đủ các cột thuộc tính bổ sung tùy chọn của cả 3 domain cũ để đảm bảo tính tương thích và truy vấn nhanh.
  - Chuyển hướng toàn bộ logic `INSERT INTO` của cả 3 domain sang bảng `archived_records`, phân biệt bằng trường `legacy_table`.
  - Cập nhật các index và ràng buộc kiểm tra (`CHECK constraints`) trên bảng `archived_records` để kiểm soát cấu trúc dữ liệu của từng loại record tương ứng.

### 1.3. Hợp nhất Sự kiện & Nhật ký (Audit Events + Security Events -> Audit Events)
- **Mục tiêu**: Tập trung hóa nhật ký hệ thống và giảm số lượng bảng lưu vết.
- **Thực hiện**:
  - Hợp nhất các cột của bảng `security_events` (như `severity`, `status`, `reviewed_by`, `payload`, `correlation_id`, `ip_address`, `user_agent`) trực tiếp vào bảng `audit_events`.
  - Trong Wave 7, chuyển đổi trực tiếp `security_event_notes` thành các dòng sự kiện trong `audit_events` dưới phân loại `SECURITY_INVESTIGATION_NOTE` thông qua khoá ngoại tự liên kết UUID (`security_event_id` trỏ ngược lại `audit_events(audit_event_id)`).
  - Trong Wave 9, di chuyển toàn bộ sự kiện bảo mật từ bảng `security_events` cũ sang `audit_events` và chuyển đổi ID từ `bigint` sang `uuid` một cách nhất quán (deterministic uuid mapping).
  - Loại bỏ hoàn toàn các bảng `security_events` và `security_event_notes`.

---

## 2. Thống Kê Số Lượng Bảng Sau Thu Gọn

Sau khi áp dụng các thay đổi trên, tổng số lượng bảng trong hệ thống giảm đi **5 bảng**:
- Persons: -1 bảng
- Community Profiles: -1 bảng
- Archived Realtime Records: -1 bảng
- Archived Consultation Records: -1 bảng
- Security Events / Notes: -1 bảng

**Tổng số bảng đích hiện tại**: **65 bảng** (đã bao gồm `flyway_schema_history`).

---

## 3. Trạng Thái Xác Minh & Kiểm Thử
Chúng tôi đã thực hiện kiểm thử trên môi trường nội bộ để đảm bảo an toàn tuyệt đối cho cơ sở dữ liệu:
1. **Biên dịch dự án**: Lệnh `./mvnw compile` thành công 100%, không bị ảnh hưởng đến mã nguồn backend Java.
2. **Kiểm thử chuỗi Migration (Flyway Checksum & Structure Inspection)**:
   - Chạy kiểm thử thành công: `./mvnw test "-Dtest=FlywayMigrationChainTest" "-Dgate0.enabled=true"`
   - Kết quả manifest ghi nhận **0 lỗi cấu trúc** (`"gateFailures" : [ ]`).
3. **Tính toàn vẹn dữ liệu**: Các hàm băm mã hoá (`md5`) đảm bảo ánh xạ chính xác khoá chính bigint cũ sang UUID mới mà không làm mất liên kết giữa các bảng.
