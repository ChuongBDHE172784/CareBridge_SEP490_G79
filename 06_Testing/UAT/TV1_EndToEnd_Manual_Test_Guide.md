# Hướng Dẫn Test Tay End-to-End TV1

**Phạm vi:** Kiểm tra cuối Sprint 5 cho TV1, bao gồm đăng ký/đăng nhập, tài khoản, phiên đăng nhập, quyền riêng tư, thông báo, audit/admin, danh mục bài tập, phiên tập, camera posture, cấu hình posture và các luồng nhạy cảm về an toàn.

**Ngày mục tiêu:** 2026-07-08  
**Tester:** `[tên của bạn]`  
**Môi trường:** Local dev hoặc staging

---

## 1. Điều Kiện Trước Khi Test

### 1.1 Ứng dụng cần chạy

- Backend API: `05_Development/CareBridgeAPI`
- Web portal: `05_Development/CareBridgeWebApp`
- Mobile app: `05_Development/CareBridgeMobileApp`
- PostgreSQL/Supabase database phải truy cập được từ backend
- Tùy chọn nếu muốn chạy full integration test: Docker Desktop đang chạy cho Testcontainers
- Tùy chọn nếu muốn test push notification thật: đã cấu hình Firebase FCM credentials

### 1.2 Biến môi trường cần kiểm tra

Backend `.env` nên dựa trên file mẫu:

```text
05_Development/CareBridgeAPI/.env.example
```

Kiểm tra tối thiểu các biến sau:

```text
SUPABASE_DB_URL=
SUPABASE_DB_USERNAME=
SUPABASE_DB_PASSWORD=
JWT_SECRET=
CAREBRIDGE_FCM_ENABLED=false
FIREBASE_CREDENTIALS_BASE64=
```

Web sử dụng:

```text
VITE_API_URL=http://localhost:8080
```

Nếu `VITE_API_URL` chưa được set, web app sẽ mặc định gọi `http://localhost:8080`.

### 1.3 Khởi động dịch vụ

Backend:

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd spring-boot:run
```

Web:

```powershell
cd 05_Development/CareBridgeWebApp
npm run dev
```

Mobile:

```powershell
cd 05_Development/CareBridgeMobileApp
flutter run
```

### 1.4 Tài khoản test cần có

Chuẩn bị hoặc tạo các tài khoản sau:

| Role | Mục đích | Ví dụ |
| --- | --- | --- |
| `MOTHER` | Test mobile auth và luồng bài tập | `mother.tv1@test.local` |
| `SYSTEM_ADMIN` | Test admin user, audit, posture config | `admin.tv1@test.local` |
| `CONTENT_ADMIN` | Test quản trị bài tập thai kỳ | `content.tv1@test.local` |

Ghi credentials thật vào ghi chú riêng tư, không ghi vào file này.

---

## 2. Smoke Check Trước Khi Test E2E

Chạy nhanh các lệnh sau trước khi test tay:

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd test "-Dtest=ExerciseControllerDetailSecurityTest,ExerciseDetailIntegrationTest,ExerciseSessionServiceTest,ExerciseSafetyCheckServiceTest,PostureAnalysisServiceTest,PostureConfigServiceTest"
```

```powershell
cd 05_Development/CareBridgeWebApp
npm run build
```

```powershell
cd 05_Development/CareBridgeMobileApp
flutter test
```

Kết quả mong đợi:

- Backend targeted tests pass.
- Web production build pass.
- Mobile tests pass.

Lưu ý các caveat hiện tại nếu vẫn gặp:

- Full backend `mvnw test` có thể fail nếu Docker/Testcontainers chưa sẵn sàng.
- Web `npm run lint` hiện vẫn có lint debt cũ.
- Mobile `flutter analyze` hiện vẫn có warning/info cũ.

---

## 3. E2E-01: Đăng Ký, Xác Thực OTP, Đăng Nhập

**Mục tiêu:** Kiểm tra nền tảng truy cập tài khoản.

### Các bước test

1. Mở mobile app.
2. Vào màn hình Đăng ký.
3. Đăng ký một tài khoản mới với role `MOTHER`.
4. Gửi OTP để xác thực tài khoản.
5. Nếu app tự động đăng nhập, hãy đăng xuất.
6. Đăng nhập lại bằng tài khoản vừa tạo.

### Kết quả mong đợi

- Đăng ký thành công.
- Xác thực OTP kích hoạt tài khoản.
- Đăng nhập trả về access token và refresh token.
- User vào được home shell sau đăng nhập.
- Không crash, không bị redirect sai sang màn hình account blocked.

### Backend endpoint liên quan

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/login`

### Bằng chứng cần lưu

- Screenshot trang đăng ký thành công hoặc OTP thành công.
- Screenshot home screen sau khi đăng nhập.
- Đoạn backend log cho thấy auth request thành công.

---

## 4. E2E-02: Profile, Đổi Mật Khẩu, Reset Mật Khẩu

**Mục tiêu:** Kiểm tra các luồng tài khoản nhạy cảm về bảo mật.

### Các bước test

1. Đăng nhập bằng tài khoản `MOTHER`.
2. Mở Account Profile.
3. Sửa các trường profile như display name, phone, region hoặc bio.
4. Lưu và reload lại profile.
5. Đổi mật khẩu bằng mật khẩu hiện tại và mật khẩu mới.
6. Đăng xuất.
7. Đăng nhập bằng mật khẩu mới.
8. Chạy luồng Forgot Password.
9. Hoàn tất Reset Password bằng OTP/token test có sẵn trong môi trường.

### Kết quả mong đợi

- Thay đổi profile được lưu sau khi reload.
- Đổi mật khẩu thành công và mật khẩu cũ không còn dùng được.
- Reset password thành công, không lộ thông tin token nhạy cảm.
- Lỗi hiển thị an toàn cho user, không lộ stack trace.

### Backend endpoint liên quan

- `GET /api/v1/auth/profile`
- `PUT /api/v1/auth/profile`
- `PUT /api/v1/auth/change-password`
- `POST /api/v1/auth/forgot-password`
- `POST /api/v1/auth/reset-password`

### Bằng chứng cần lưu

- Screenshot trước và sau khi update profile.
- Screenshot đổi/reset mật khẩu thành công.
- Ghi chú session có bị vô hiệu hóa đúng kỳ vọng hay không.

---

## 5. E2E-03: Quản Lý Phiên Đăng Nhập

**Mục tiêu:** Kiểm tra danh sách session và thu hồi session.

### Các bước test

1. Đăng nhập trên mobile.
2. Đăng nhập tiếp bằng emulator/browser/device khác nếu có.
3. Mở màn hình Login Sessions.
4. Kiểm tra current session có được đánh dấu rõ không.
5. Revoke một session khác.
6. Thử sử dụng session/device vừa bị revoke.

### Kết quả mong đợi

- Danh sách session load thành công.
- Current session phân biệt được với các session khác.
- Session bị revoke không còn gọi được API authenticated.
- Current session vẫn hoạt động.

### Endpoint mobile dự kiến gọi

- `GET /api/v1/sessions`
- `DELETE /api/v1/sessions/{sessionId}`
- `DELETE /api/v1/sessions`

### Bằng chứng cần lưu

- Screenshot danh sách session.
- Screenshot hoặc ghi chú xác nhận hành vi của session bị revoke.

### Rủi ro hiện tại cần xác minh

Mobile đang gọi trực tiếp các session endpoint. Cần xác nhận backend route tồn tại trong môi trường đang chạy. Nếu gặp 404, đánh dấu flow này là blocked do thiếu route session controller.

---

## 6. E2E-04: Cài Đặt Riêng Tư Và Thu Hồi Đồng Ý

**Mục tiêu:** Kiểm tra privacy settings và consent management.

### Các bước test

1. Đăng nhập bằng `MOTHER`.
2. Mở Privacy Settings trên mobile hoặc web.
3. Bật/tắt các setting như profile visibility, location sharing, analytics consent hoặc notification visibility.
4. Lưu thay đổi.
5. Reload lại page/screen.
6. Mở danh sách consent grant.
7. Revoke một consent đang active nếu có test data.

### Kết quả mong đợi

- Privacy settings load từ backend.
- Cập nhật được lưu sau khi reload.
- Active consents hiển thị đúng.
- Consent bị revoke biến mất khỏi active list hoặc hiển thị trạng thái revoked.

### Backend endpoint liên quan

- `GET /api/v1/privacy-settings/me`
- `PUT /api/v1/privacy-settings/me`
- `GET /api/v1/consent/grants`
- `DELETE /api/v1/consent/grants/{consentId}`

### Bằng chứng cần lưu

- Screenshot trước và sau khi save.
- Screenshot sau khi revoke consent.

---

## 7. E2E-05: Notification Center Và FCM Fallback

**Mục tiêu:** Kiểm tra danh sách thông báo, trạng thái đã đọc, đăng ký device token và hành vi fallback khi FCM tắt.

### Các bước test

1. Đăng nhập bằng `MOTHER`.
2. Mở Notification Center.
3. Đăng ký hoặc refresh device token nếu app có hành động này.
4. Trigger một notification từ admin/system account hoặc API call.
5. Refresh Notification Center.
6. Mở chi tiết notification.
7. Mark read một notification.
8. Mark all as read nếu UI có nút này.

### Kết quả mong đợi

- Danh sách notification load thành công.
- Mở được notification detail.
- Mark-read cập nhật trạng thái.
- Nếu `CAREBRIDGE_FCM_ENABLED=false`, backend dùng logging/stub fallback và không crash.
- Nếu FCM bật, backend có attempt gửi qua Firebase thật.

### Backend endpoint liên quan

- `POST /api/v1/notifications/device-token`
- `DELETE /api/v1/notifications/device-token`
- `GET /api/v1/notifications/me`
- `PUT /api/v1/notifications/{notificationId}/read`
- `PUT /api/v1/notifications/read-all`
- `POST /api/v1/notifications/send`

### Bằng chứng cần lưu

- Screenshot notification list/detail.
- Backend log cho thấy FCM delivery hoặc fallback.

### Rủi ro hiện tại cần xác minh

Web `NotificationCenterPage` hiện có TODO liên quan đến mark-all-read wiring. Nếu nút không hoạt động hoặc trạng thái cũ, đánh dấu partial.

---

## 8. E2E-06: Exercise Demo Path

**Mục tiêu:** Kiểm tra demo path Sprint 5 TV1:

```text
login -> select exercise -> complete pre-exercise safety check -> start session -> enable posture camera -> view exercise result
```

### 8.1 Seed hoặc xác nhận exercise data

Trên web portal bằng `CONTENT_ADMIN`:

1. Mở `/content/exercises`.
2. Tạo hoặc xác nhận có một pregnancy exercise đã published.
3. Kiểm tra exercise có trimester, difficulty, instruction, duration và safety warning.
4. Activate/publish nếu cần.

Backend endpoint:

- `GET /api/v1/admin/exercises`
- `POST /api/v1/admin/exercises`
- `PUT /api/v1/admin/exercises/{exerciseId}`
- `PATCH /api/v1/admin/exercises/{exerciseId}/activate`
- `PATCH /api/v1/admin/exercises/{exerciseId}/disable`

Kết quả mong đợi:

- Admin thấy được exercise.
- Exercise activate được.

### 8.2 Mobile exercise catalog/detail

1. Đăng nhập bằng `MOTHER`.
2. Đi tới exercise selection/catalog.
3. Chọn một published exercise.
4. Mở exercise detail.

Backend endpoint:

- `GET /api/v1/exercises`
- `GET /api/v1/exercises/{exerciseId}`

Kết quả mong đợi:

- Chỉ published exercises được hiển thị.
- Draft/disabled exercises không bị lộ ra user.
- Detail screen hiển thị instruction và safety warning.

Rủi ro hiện tại cần xác minh:

- Exercise screens đang có trong mobile `ScreensExplorer`, nhưng có thể chưa được wire thành app route chính thức. Nếu tester phải dùng Screens Explorer hoặc direct navigation, đánh dấu route wiring là partial.

### 8.3 Hoàn tất pre-exercise safety check

1. Mở pre-exercise safety check cho exercise đã chọn.
2. Trả lời tất cả safety question theo hướng safe.
3. Submit.
4. Ghi lại `safetyCheckId` nếu UI/log có hiển thị.

Backend endpoint:

- `POST /api/v1/exercises/{exerciseId}/safety-check`
- `GET /api/v1/exercises/{exerciseId}/safety-check/latest`

Kết quả mong đợi:

- Câu trả lời safe tạo ra cleared safety check.
- Câu trả lời unsafe chặn start session và hiển thị cảnh báo an toàn.
- Không hiển thị ngôn ngữ chẩn đoán/clinical diagnosis.

Rủi ro hiện tại cần xác minh:

- Mobile screen hiện có TODO cho việc navigate sang exercise session sau safety check. Nếu submit thành công nhưng không tự chuyển màn, đánh dấu bước này partial.

### 8.4 Bắt đầu exercise session

1. Start session từ cleared safety check.
2. Kiểm tra timer/state của session xuất hiện.
3. Pause session.
4. Resume session.

Backend endpoint:

- `POST /api/v1/exercises/{exerciseId}/sessions`
- `PATCH /api/v1/exercises/sessions/{sessionId}/pause`
- `PATCH /api/v1/exercises/sessions/{sessionId}/resume`

Kết quả mong đợi:

- Session chỉ start khi có cleared safety check hợp lệ.
- Pause/resume cập nhật state đúng.
- Duplicate active session bị reject hoặc xử lý an toàn.

### 8.5 Bật posture camera/config

1. Từ exercise session, mở posture camera/enable posture camera UI.
2. Xác nhận app lấy active posture config.
3. Nếu có, submit posture feedback events trong lúc tập.

Backend endpoint:

- `GET /api/v1/exercises/{exerciseId}/posture-config`
- `POST /api/v1/exercises/sessions/{sessionId}/posture-events`

Kết quả mong đợi:

- Active posture config load được theo exercise.
- Nếu chưa có config, UI hiển thị trạng thái unavailable có thể recover.
- Posture feedback dùng ngôn ngữ coaching, không phải diagnosis.

### 8.6 Hoàn tất session và xem kết quả

1. Complete exercise session.
2. Mở session result.
3. Mở exercise history.

Backend endpoint:

- `PATCH /api/v1/exercises/sessions/{sessionId}/complete`
- `GET /api/v1/exercises/sessions/{sessionId}/result`
- `GET /api/v1/exercises/sessions/history`

Kết quả mong đợi:

- Completed session tạo được result.
- Result có completion status, duration và posture score/feedback nếu có.
- History có session vừa complete.

### Bằng chứng cần lưu

- Screenshot mỗi bước: catalog, detail, safety check, session, posture camera, result.
- Ghi lại `exerciseId`, `safetyCheckId`, `sessionId`.
- Backend log cho các transition fail, nếu có.

---

## 9. E2E-07: Admin Posture Configuration

**Mục tiêu:** Kiểm tra vòng đời cấu hình posture phía admin.

### Các bước test

1. Đăng nhập web bằng `SYSTEM_ADMIN`.
2. Mở `/posture-configs`.
3. Tạo posture config cho một published exercise.
4. Tạo version mới.
5. Activate config/version.
6. Quay lại mobile exercise flow và xác nhận `GET /api/v1/exercises/{exerciseId}/posture-config` trả về active config.

### Kết quả mong đợi

- Tạo config thành công.
- Chỉ một config/version active được dùng cho exercise.
- Mobile consume được active config.

### Backend endpoint liên quan

- `POST /api/v1/admin/posture-configs`
- `POST /api/v1/admin/posture-configs/{exerciseId}/versions`
- `PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate`
- `GET /api/v1/admin/posture-configs/{exerciseId}`

### Rủi ro hiện tại cần xác minh

Web posture config pages có mock fallback data. Cần xác nhận dữ liệu đang hiển thị đến từ backend hay fallback mock.

---

## 10. E2E-08: Admin Audit, Security Event, User Role

**Mục tiêu:** Kiểm tra các thao tác governance/admin dùng chung.

### Các bước test

1. Đăng nhập web bằng `SYSTEM_ADMIN`.
2. Mở admin user list.
3. Search/filter một user.
4. Đổi status của một test account an toàn.
5. Đổi role của một test account an toàn.
6. Mở audit logs.
7. Mở security events.
8. Review một security event và thêm note.

### Kết quả mong đợi

- Trang admin-only chặn user không phải admin.
- Thay đổi user status/role được lưu.
- Audit logs ghi lại hành động nhạy cảm.
- Review/note của security event được lưu.

### Backend endpoint liên quan

- `GET /api/v1/admin/users`
- `PATCH /api/v1/admin/users/{userId}/status`
- `PATCH /api/v1/admin/users/{userId}/role`
- `GET /api/v1/admin/audit-logs`
- `GET /api/v1/admin/security-events`
- `PUT /api/v1/admin/security-events/{eventId}/review`
- `POST /api/v1/admin/security-events/{eventId}/notes`
- `GET /api/v1/admin/security-events/{eventId}/notes`

### Bằng chứng cần lưu

- Screenshot admin user search.
- Screenshot audit log sau khi đổi role/status.
- Screenshot security event note.

---

## 11. E2E-09: Hành Vi Account Blocking

**Mục tiêu:** Đảm bảo disabled/locked account được xử lý an toàn và không nhầm plain role mismatch thành blocked account.

### Các bước test

1. Dùng admin để disable hoặc lock một test account.
2. Thử đăng nhập bằng account đó.
3. Tạo một tình huống 403 role mismatch thông thường bằng account không có role cần thiết.

### Kết quả mong đợi

- Disabled account đi tới blocked screen/message.
- Locked account đi tới blocked screen/message.
- Plain `403 ACCESS_DENIED` không clear auth như một account-blocked event.

### Bằng chứng cần lưu

- Screenshot blocked account screen.
- API response body có `ACCOUNT_DISABLED` hoặc `ACCOUNT_LOCKED`.
- Ghi chú hành vi của role mismatch.

---

## 12. Mẫu Tổng Hợp Pass/Fail

| Flow | Trạng thái | Bằng chứng | Ghi chú |
| --- | --- | --- | --- |
| E2E-01 Register/OTP/Login | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-02 Profile/Password | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-03 Sessions | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-04 Privacy/Consent | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-05 Notifications/FCM | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-06 Exercise Demo Path | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-07 Posture Config | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-08 Admin/Audit/Role | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |
| E2E-09 Account Blocking | `[ ] PASS [ ] FAIL [ ] PARTIAL` |  |  |

Kết quả tổng:

```text
[ ] Sẵn sàng demo Sprint 5
[ ] Sẵn sàng demo với caveat đã biết
[ ] Chưa sẵn sàng
```

---

## 13. Mẫu Ghi Defect

| ID | Flow | Severity | Actual | Expected | Evidence | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| TV1-E2E-BUG-001 |  | Critical/High/Medium/Low |  |  |  |  |

Hướng dẫn severity:

- Critical: auth bypass, lộ privacy, safety flow không an toàn, app crash làm chặn demo.
- High: core demo path không thể tiếp tục.
- Medium: có workaround nhưng chưa đạt chất lượng demo.
- Low: lỗi copy/layout hoặc polish không chặn luồng chính.

---

## 14. Rủi Ro Hiện Tại Cần Recheck

- Full backend regression cần Docker/Testcontainers; nếu không có Docker, integration reports sẽ fail.
- Web lint chưa clean.
- Mobile analyzer chưa clean.
- Mobile exercise flow có thể phải đi qua Screens Explorer hoặc manual navigation thay vì route polished.
- Web exercise/posture admin pages có thể hiển thị mock fallback khi backend data rỗng hoặc backend unavailable.
- Notification mark-all-read cần được xác nhận với backend endpoint.
- Session management mobile gọi `/api/v1/sessions`; cần xác nhận backend route có trong môi trường đang chạy.
