# CareBridge — Quy trình Kiểm thử Thủ công (Manual Test Flow)

> **Historical/superseded reference — do not execute as a current runbook.** This
> snapshot is retained as input/evidence for the 2026-06-28 code state. Its seed
> credentials and direct-login instructions are retired. Current operators must
> follow `README.md`, `05_Development/STARTUP.md`, and the applicable canonical
> guide under `06_Testing/TestCases/`; no password value in this historical file
> may be reused.

> **Ngày**: 2026-06-28
> **Nhánh**: PhuongNT (sau khi merge từ dev)
> **Backend**: `http://localhost:8080`
> **Mobile Web**: `flutter run -d chrome`

---

## Tài khoản Kiểm thử (Test Accounts)

| Email                        | Vai trò (Role)                      | Mật khẩu (Password) |
| ---------------------------- | ------------------------------------ | --------------------- |
| `mother@carebridge.dev`    | MOTHER (Mẹ)                         | `Test@1234`         |
| `family@carebridge.dev`    | FAMILY (Gia đình)                  | `Test@1234`         |
| `admin@carebridge.dev`     | SYSTEM_ADMIN (Quản trị hệ thống) | `Test@1234`         |
| `moderator@carebridge.dev` | MODERATOR (Điều phối viên)       | `Test@1234`         |
| `expert@carebridge.dev`    | EXPERT (Chuyên gia)                 | `Test@1234`         |
| `partner@carebridge.dev`   | PARTNER (Đối tác)                 | `Test@1234`         |
| `content@carebridge.dev`   | CONTENT_ADMIN (Quản trị nội dung) | `Test@1234`         |

> Các tài khoản seed được tự động tạo khi thiết lập `CAREBRIDGE_DEV_SEED_ENABLED=true` trong file `.env`.

---

## QUY TRÌNH 1: Xác thực (Authentication)

| #   | Bước thực hiện                                                                              | Kết quả mong đợi                                                                       | Đạt? |
| --- | ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------ | ------ |
| 1.1 | Mở ứng dụng → xuất hiện**WelcomeScreen**                                            | Màn hình chào mừng có nút Đăng nhập / Đăng ký                                  | ☑     |
| 1.2 | Nhấn**Đăng ký**                                                                       | Chuyển đến màn hình**RegisterScreen** có các trường email, tên, mật khẩu | ☑     |
| 1.3 | Nhấn**Đăng nhập** → nhập `mother@carebridge.dev` + `Test@1234` → Gửi (Submit) | Đăng nhập thành công, chuyển đến màn hình**Trang chủ** (Home)             | ☑     |
| 1.4 | Nhấn**Đăng nhập** → nhập sai mật khẩu                                             | Báo lỗi: "Email/số điện thoại hoặc mật khẩu không đúng"                        | ☑     |
| 1.5 | Nhấn**Đăng nhập** → để trống các trường → Gửi                                | Báo lỗi: "Vui lòng nhập đầy đủ thông tin"                                         | ☑     |
| 1.6 | Nhấn**Quên mật khẩu** → nhập email                                                  | Chuyển đến màn hình**ForgotPasswordScreen**, gửi mã OTP                       | ☑     |
| 1.7 | Đăng nhập → tab**Hồ sơ** → Chọn **Đăng xuất**                            | Xuất hiện hộp thoại xác nhận đăng xuất → xác nhận → quay lại WelcomeScreen   | ☑     |

---

## QUY TRÌNH 2: Bảng điều khiển Trang chủ (Tab 0 — Trang chủ)

| #   | Bước thực hiện                                    | Kết quả mong đợi                                                                             | Đạt? |
| --- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------ | ------ |
| 2.1 | Đăng nhập → vào**Trang chủ** (tab 0)      | Hiển thị thẻ trạng thái hành trình, thao tác nhanh, việc cần làm hôm nay, bài viết | ☑     |
| 2.2 | Cuộn xuống phần**Việc hôm nay**            | Danh sách các công việc/nhắc nhở của ngày hôm nay                                       | ☑     |
| 2.3 | Nhấp vào một công việc → bật/tắt hoàn thành | Công việc chuyển sang trạng thái đã hoàn thành (tự động cập nhật giao diện)       | ☑     |
| 2.4 | Kéo để làm mới (Pull-to-refresh)                 | Dữ liệu được tải lại từ API                                                              | ☑     |
| 2.5 | Nhấn**Xem tất cả** ở phần công việc      | Chuyển đến màn hình**TodayTasksScreen**                                               | ☑     |
| 2.6 | Nhấp vào một thẻ thao tác nhanh                  | Chuyển đến màn hình tính năng tương ứng                                                | ☑     |

---

## QUY TRÌNH 3: Hành trình của Mẹ (Tab 1 — Hành trình)

| #   | Bước thực hiện                                                    | Kết quả mong đợi                                                               | Đạt? |
| --- | --------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ------ |
| 3.1 | Nhấp vào tab**Hành trình** (tab 1)                          | Hiển thị màn hình**MotherJourneyScreen** với thông tin thai kỳ        | ☑     |
| 3.2 | Xem bảng điều khiển: tuần thai, cân nặng, các chỉ số        | Dữ liệu được tải từ`GET /api/v1/journeys/me/dashboard`                    | ☑     |
| 3.3 | Nhấn**Hồ sơ sức khỏe** trong phần thao tác nhanh         | Chuyển hướng đến`/health-records`                                           | ☑     |
| 3.4 | Nhấn**Thiết lập hành trình** (nếu chưa có hành trình) | Chuyển hướng đến`/journey-setup` — Màn hình **JourneySetupScreen** | ☑     |
| 3.5 | Xác minh đường dẫn cập nhật hành trình tồn tại             | `/journey-update` tải thành công (chấp nhận màn hình chờ/placeholder)    | ☑     |

---

## QUY TRÌNH 4: Hồ sơ sức khỏe (Health Records)

| #   | Bước thực hiện                                                               | Kết quả mong đợi                                                                           | Đạt? |
| --- | -------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- | ------ |
| 4.1 | Truy cập đường dẫn`/health-records`                                       | Hiển thị màn hình**HealthRecordTimelineScreen** — hồ sơ được nhóm theo tháng | ☑     |
| 4.2 | Nhấn vào các nhãn lọc (Tất cả / Khám / Xét nghiệm / Siêu âm / Tiêm) | Danh sách được lọc theo loại hồ sơ tương ứng                                        | ☑     |
| 4.3 | Nhấp vào một hồ sơ trong danh sách                                         | Mở chi tiết hồ sơ (hoặc màn hình chờ)                                                  | ☑     |
| 4.4 | Nhấn nút FAB (+)                                                               | Mở biểu mẫu thêm hồ sơ mới (hoặc màn hình chờ)                                      | ☑     |
| 4.5 | Xác minh trạng thái trống (empty state)                                      | Hiển thị "Chưa có hồ sơ nào." khi không có dữ liệu                                  | ☑     |

---

## QUY TRÌNH 5: Chỉ số sức khỏe mẹ (Maternal Health Metrics)

| #   | Bước thực hiện                                                 | Kết quả mong đợi                                                                            | Đạt? |
| --- | ------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------- | ------ |
| 5.1 | Từ Hành trình, nhấn vào một chỉ số (cân nặng/huyết áp) | Chuyển hướng đến`/health-metrics/:id` — Màn hình **MaternalHealthMetricScreen** | ☑     |
| 5.2 | Xem biểu đồ và lịch sử đo lường                           | Dữ liệu được hiển thị trực quan theo thời gian                                         | ☑     |

---

## QUY TRÌNH 6: Tiêm chủng (Vaccination)

| #   | Bước thực hiện                                      | Kết quả mong đợi                                                            | Đạt? |
| --- | ------------------------------------------------------- | ------------------------------------------------------------------------------- | ------ |
| 6.1 | Truy cập đường dẫn`/vaccination/:id`             | Hiển thị màn hình**VaccinationDetailScreen** với thông tin vắc-xin | ☑     |
| 6.2 | Xem chi tiết: tên vắc-xin, ngày tiêm, trạng thái | Các trường thông tin hiển thị chính xác                                 | ☑     |
| 6.3 | Nhấn**Dời lịch**                               | Hộp thoại chọn ngày (date picker) xuất hiện                               | ☑     |
| 6.4 | Nhấn**Xóa hồ sơ**                             | Hộp thoại xác nhận: "Xóa hồ sơ?" → xác nhận/hủy                      | ☑     |

---

## QUY TRÌNH 7: Hồ sơ bé (Baby Profiles)

| #   | Bước thực hiện                             | Kết quả mong đợi                                                                    | Đạt? |
| --- | ---------------------------------------------- | --------------------------------------------------------------------------------------- | ------ |
| 7.1 | Truy cập đường dẫn`/babies`             | Hiển thị màn hình**BabyProfilesScreen** — danh sách hồ sơ của bé        | ☑     |
| 7.2 | Nhấn**Thêm hồ sơ bé**               | Chuyển hướng đến`/babies/add` — Màn hình **AddBabyScreen**              | ☑     |
| 7.3 | Điền tên + ngày sinh → Lưu               | Tạo hồ sơ thành công → quay lại danh sách                                       | ☑     |
| 7.4 | Nhấp vào một hồ sơ bé hiện có          | Chuyển hướng đến`/babies/detail/:id` — màn hình chi tiết                     | ☑     |
| 7.5 | Nhấn giữ → Chọn**Xóa hồ sơ**      | Xuất hiện hộp thoại xác nhận → xóa hồ sơ                                      | ☐     |
| 7.6 | Xác minh trạng thái trống                  | Hiển thị "Chưa có hồ sơ bé" kèm nút kêu gọi hành động (CTA) thêm hồ sơ | ☐     |
| 7.7 | Xác minh xử lý lỗi 403 (không có quyền) | Hiển thị thông báo "Bạn không có quyền xem hồ sơ này."                       | ☐     |

---

## QUY TRÌNH 8: Nhóm chăm sóc (Care Groups)

| #   | Bước thực hiện                      | Kết quả mong đợi                                                                | Đạt? |
| --- | --------------------------------------- | ----------------------------------------------------------------------------------- | ------ |
| 8.1 | Truy cập đường dẫn`/care-groups` | Hiển thị màn hình**CareGroupsScreen** — danh sách các nhóm chăm sóc | ☑     |
| 8.2 | Nhấp vào một nhóm                   | Chuyển hướng đến`/care-groups/members/:id` — xem thành viên trong nhóm   | ☑     |
| 8.3 | Nhấn**Tạo nhóm mới**          | Chuyển hướng đến biểu mẫu tạo nhóm (hoặc màn hình chờ)                 | ☐     |

---

## QUY TRÌNH 9: Nhắc nhở (Reminders)

| #   | Bước thực hiện                                               | Kết quả mong đợi                                                                        | Đạt? |
| --- | ---------------------------------------------------------------- | ------------------------------------------------------------------------------------------- | ------ |
| 9.1 | Từ Trang chủ, nhấn**Xem tất cả** phần việc hôm nay | Chuyển hướng đến màn hình**TodayTasksScreen**                                  | ☑     |
| 9.2 | Nhấp vào một mục nhắc nhở                                  | Chuyển hướng đến`/reminders/detail/:id` — Màn hình **ReminderDetailScreen** | ☑     |
| 9.3 | Bật/tắt trạng thái hoàn thành                              | Trạng thái thay đổi trực quan trên giao diện                                         | ☑     |

---

## QUY TRÌNH 10: Hồ sơ tài khoản (Tab 4 — Hồ sơ)

| #     | Bước thực hiện                                             | Kết quả mong đợi                                                                                                 | Đạt? |
| ----- | -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- | ------ |
| 10.1  | Nhấp vào tab**Hồ sơ** (tab 4)                        | Hiển thị màn hình**AccountProfileScreen** — ảnh đại diện, tên, danh hiệu VIP, danh sách tùy chọn | ☑     |
| 10.2  | Xác minh không có nút quay lại (vì đây là tab chính) | Không hiển thị biểu tượng quay lại (arrow_back) trên thanh ứng dụng (app bar)                              | ☑     |
| 10.3  | Nhấn**Chỉnh sửa hồ sơ**                             | Chuyển hướng đến màn hình**EditProfileScreen** — biểu mẫu thay đổi tên, ảnh đại diện          | ☑     |
| 10.4  | Thay đổi tên → Lưu                                        | Cập nhật thành công, quay về màn hình hồ sơ với dữ liệu mới                                             | ☑     |
| 10.5  | Nhấn**Đổi mật khẩu**                                | Chuyển hướng đến màn hình**ChangePasswordScreen**                                                       | ☑     |
| 10.6  | Nhập mật khẩu cũ + mới → Lưu                            | Đổi mật khẩu thành công (xác minh bằng cách đăng nhập lại bằng mật khẩu mới)                        | ☑     |
| 10.7  | Nhấn**Phiên đăng nhập**                             | Chuyển hướng đến màn hình**LoginSessionsScreen** — danh sách các phiên đăng nhập hoạt động    | ☑     |
| 10.8  | Vuốt/nhấp để thu hồi một phiên                          | Xuất hiện hộp xác nhận → thu hồi → phiên đăng nhập biến mất khỏi danh sách                           | ☑     |
| 10.9  | Nhấn**Quyền riêng tư**                               | Chuyển hướng đến màn hình**PrivacySettingsScreen** — các tùy chọn cài đặt                        | ☑     |
| 10.10 | Bật/tắt "Hiển thị hồ sơ trong Cộng đồng"              | Cài đặt được lưu lại sau khi tải lại trang                                                                 | ☑     |

---

## QUY TRÌNH 11: Thông báo (Notifications)

| #    | Bước thực hiện                                                        | Kết quả mong đợi                                               | Đạt? |
| ---- | ------------------------------------------------------------------------- | ------------------------------------------------------------------ | ------ |
| 11.1 | Nhấn vào biểu tượng chuông (🔔) trên thanh ứng dụng của Hồ sơ | Chuyển hướng đến màn hình**NotificationCenterScreen** | ☑     |
| 11.2 | Xem danh sách thông báo                                                | Hiển thị danh sách thông báo lấy từ API                     | ☑     |
| 11.3 | Nhấp vào một thông báo                                               | Chuyển hướng đến màn hình**NotificationDetailScreen** | ☑     |

---

## QUY TRÌNH 12: Quản lý file (File Manager)

| #    | Bước thực hiện                      | Kết quả mong đợi                                       | Đạt? |
| ---- | --------------------------------------- | ---------------------------------------------------------- | ------ |
| 12.1 | Truy cập đường dẫn`/upload-file` | Hiển thị màn hình**UploadFileScreen**            | ☐     |
| 12.2 | Chọn file → Tải lên (Upload)        | File được tải lên kho lưu trữ Supabase thành công | ☐     |

---

## QUY TRÌNH 13: Thanh điều hướng dưới (Bottom Navigation Bar)

| #    | Bước thực hiện                                                                             | Kết quả mong đợi                                                                         | Đạt? |
| ---- | ---------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- | ------ |
| 13.1 | Nhấn vào từng tab: Trang chủ → Hành trình → Cộng đồng → Việc cần làm → Hồ sơ | Mỗi tab hiển thị đúng màn hình tương ứng, biểu tượng tab được làm nổi bật | ☑     |
| 13.2 | Chuyển đổi qua lại nhanh giữa các tab                                                    | `IndexedStack` giữ nguyên vị trí cuộn trang ở từng tab                              | ☑     |
| 13.3 | Nhấp tab**Cộng đồng** (tab 2)                                                        | Hiển thị màn hình chờ "Cộng đồng (đang xây dựng)"                                 | ☑     |
| 13.4 | Nhấp tab**Việc cần làm** (tab 3)                                                     | Hiển thị màn hình chờ "Việc cần làm (đang xây dựng)"                              | ☑     |

---

## QUY TRÌNH 14: Kiểm thử nhanh Backend API (Smoke Test)

> Sử dụng trình duyệt, Postman hoặc lệnh `curl`. Tất cả các endpoint đều yêu cầu token Bearer ngoại trừ các endpoint xác thực.
>
> **Lấy token**: `POST http://localhost:8080/api/v1/auth/login-direct`
>
> ```json
> {"email": "mother@carebridge.dev", "password": "Test@1234"}
> ```

| #     | Phương thức | Endpoint                                         | Kết quả mong đợi                                               | Đạt? |
| ----- | -------------- | ------------------------------------------------ | ------------------------------------------------------------------ | ------ |
| 14.1  | POST           | `/api/v1/auth/login-direct`                    | Trả về mã 200 + JWT access/refresh token                        | ☐     |
| 14.2  | GET            | `/api/v1/auth/profile`                         | Trả về mã 200 + thông tin hồ sơ người dùng                | ☐     |
| 14.3  | GET            | `/api/v1/journeys/me/dashboard`                | Trả về mã 200 + dữ liệu bảng điều khiển hành trình      | ☐     |
| 14.4  | GET            | `/api/v1/health-records`                       | Trả về mã 200 + danh sách hồ sơ sức khỏe                   | ☐     |
| 14.5  | GET            | `/api/v1/health-metrics`                       | Trả về mã 200 + danh sách chỉ số sức khỏe                  | ☐     |
| 14.6  | GET            | `/api/v1/babies`                               | Trả về mã 200 + danh sách hồ sơ bé                          | ☐     |
| 14.7  | GET            | `/api/v1/care-groups`                          | Trả về mã 200 + danh sách nhóm chăm sóc                     | ☐     |
| 14.8  | GET            | `/api/v1/sessions/paged`                       | Trả về mã 200 + danh sách phân trang các phiên đăng nhập | ☐     |
| 14.9  | GET            | `/api/v1/privacy-settings/me`                  | Trả về mã 200 + đối tượng cài đặt quyền riêng tư      | ☐     |
| 14.10 | GET            | `/api/v1/notifications/me`                     | Trả về mã 200 + danh sách thông báo                          | ☐     |
| 14.11 | GET            | `/api/v1/reminders`                            | Trả về mã 200 + danh sách nhắc nhở                           | ☐     |
| 14.12 | GET            | `/api/v1/vaccination/babies/{babyId}/schedule` | Trả về mã 200 + lịch tiêm chủng của bé                     | ☐     |
| 14.13 | PUT            | `/api/v1/auth/change-password`                 | Trả về mã 200 sau khi cung cấp mật khẩu cũ + mới           | ☐     |
| 14.14 | PUT            | `/api/v1/privacy-settings/me`                  | Trả về mã 200 sau khi thay đổi một cài đặt                | ☐     |

---

## QUY TRÌNH 15: Xử lý lỗi & Các trường hợp đặc biệt (Edge Cases)

| #    | Bước thực hiện                                     | Kết quả mong đợi                                                   | Đạt? |
| ---- | ------------------------------------------------------ | ---------------------------------------------------------------------- | ------ |
| 15.1 | Dừng chạy backend → sử dụng ứng dụng            | Hiển thị thông báo lỗi mạng (không bị crash/văng app)         | ☐     |
| 15.2 | Đăng nhập bằng tài khoản bị khóa               | Hiển thị "Tài khoản bị khóa. Liên hệ hỗ trợ để mở khóa." | ☐     |
| 15.3 | Truy cập đường dẫn`/blocked`                    | Hiển thị màn hình**BlockedAccountScreen**                    | ☐     |
| 15.4 | Truy cập trang cần xác thực khi chưa đăng nhập | Chuyển hướng về trang`/welcome`                                  | ☐     |
| 15.5 | Truy cập trang`/welcome` khi đã đăng nhập      | Chuyển hướng về trang`/` (Trang chủ)                            | ☐     |
| 15.6 | Điều hướng tới đường dẫn không tồn tại     | Ứng dụng xử lý an toàn (không bị crash)                         | ☐     |

---

## Ghi chú (Notes)

- Các đường dẫn đang hiển thị văn bản chờ ("đang xây dựng" hoặc phần `Text(...)` tĩnh) là **chưa được cài đặt** — vui lòng bỏ qua các bước kiểm thử này.
- Sau khi kiểm thử Quy trình 10.6 (đổi mật khẩu), hãy nhớ đổi lại mật khẩu cũ hoặc sử dụng tài khoản seed khác.
- Kéo để làm mới (Pull-to-refresh) hoạt động trên các màn hình có sử dụng `RefreshIndicator`.
- Cơ chế dev-seed của Backend tự động đặt lại (reset) mật khẩu mặc định mỗi khi khởi động lại nếu `CAREBRIDGE_DEV_SEED_ENABLED=true`.
