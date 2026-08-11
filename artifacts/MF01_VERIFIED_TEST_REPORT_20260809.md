# CareBridge MF-01 — Source-Verified Integration Test Report

Generated: 2026-08-09 (Asia/Saigon)  
Database target: **Supabase PostgreSQL**  
Scope: MF-01 only. Source code and Google Sheet were not modified.

## PHẦN A — STARTUP CHECKLIST

- Backend: `cd 05_Development/CareBridgeAPI; .\mvnw.cmd spring-boot:run`
- Backend URL: `http://localhost:8080`; readiness: `GET /actuator/health/readiness`.
- Web: `cd 05_Development/CareBridgeWebApp; npm install; npm run dev -- --host 127.0.0.1 --port 5173`; expected URL `http://localhost:5173`.
- Mobile: `cd 05_Development/CareBridgeMobileApp; flutter pub get; flutter run --dart-define=API_BASE_URL=http://<host>:8080`.
- Database: Supabase is remote; no local DB start command. TCP connectivity to the configured pooler succeeded.
- SMTP: Gmail SMTP 587/STARTTLS is configured. MailHog is not configured. Phone OTP/reset uses `MockSmsService`, so the safe test path reads the backend log.
- Required MF-01 environment names: `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, `SUPABASE_DB_PASSWORD`, `JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEYS`, `MAIL_USERNAME`, `MAIL_PASSWORD`; optional integrations include Firebase/FCM and CORS variables. Secret values are intentionally omitted.
- Test account: create a unique Vietnamese phone account with role `MOTHER`; prepare separate `SYSTEM_ADMIN`, `OPERATIONS`, and `MODERATOR` accounts for privileged paths. No verified seed password was used.
- OTP: search backend output for `[MOCK SMS] To: +84..., OTP: ...`; the service canonicalizes `09...` to `+849...`.
- Actual startup: backend started with profile `supabase`; readiness returned HTTP 200 with DB=UP. Flyway/dev seed/FCM/Gemini/Firestore were disabled only for the test process to avoid unrelated mutations/integrations.
- Prior startup errors: using local profile against Supabase caused a closed-connection Hibernate metadata error; a discarded local PostgreSQL attempt failed a Flyway checklist migration. Neither is the final Supabase configuration.

- Additional actual frontend check: Vite listened on `127.0.0.1:5173` and returned HTTP 200. Mobile was not launched because no emulator/device was confirmed.

## PHẦN B — SOURCE TRACEABILITY

| STT | Feature/Group | Source File | Class/Method | Endpoint | Status |
|---:|---|---|---|---|---|
| 1 | Register / OTP | security/controller/AuthController.java; security/service/impl/AuthServiceImpl.java | register; verifyOtp; resendOtp | POST /api/v1/auth/register; /verify-otp; /resend-otp | Implemented in source + Report 3 |
| 2 | Login | security/controller/AuthController.java; security/service/impl/AuthServiceImpl.java | login | POST /api/v1/auth/login | Implemented in source + Report 3 |
| 3 | JWT / Refresh | security/jwt/JwtTokenProvider.java; identity/service/impl/SessionServiceImpl.java | generate/validate; refresh | POST /api/v1/auth/refresh | Implemented in source + Report 3 |
| 4 | Logout | security/controller/AuthController.java; identity/service/impl/SessionServiceImpl.java | logout | POST /api/v1/auth/logout | Implemented in source + Report 3 |
| 5 | Forgot / Reset / Change password | security/controller/AuthController.java; security/service/impl/ForgotPasswordServiceImpl.java | forgotPassword; resetPassword; changePassword | POST /forgot-password; POST /reset-password; PUT /change-password | Implemented in source + Report 3 |
| 6 | Private profile | security/controller/AuthController.java; profile/controller/ProfileController.java | get/update profile | GET/PUT /api/v1/auth/profile; GET/PATCH /api/v1/profile | Implemented in source + Report 3 |
| 7 | Privacy settings | privacy/controller/PrivacySettingsController.java | getMySettings; updateMySettings | GET/PUT /api/v1/privacy-settings/me | Implemented in source; partial UC-10 |
| 8 | Community identity | Report 3 UC-10 | N/A | N/A | Exists only in Report 3; independent identity endpoint not found |
| 9 | Notification preference | notification/controller/NotificationPreferenceController.java | get/update | GET/PUT /api/v1/users/me/notification-preferences | Implemented in source + Report 3 |
| 10 | Session management | identity/controller/SessionController.java | list/paged/revoke | GET/DELETE /api/v1/sessions | Implemented in source + Report 3 |
| 11 | Consent | consent/controller/ConsentController.java | grant/list/revoke | POST/GET /api/v1/consent/grants; DELETE /{consentId} | Implemented in source + Report 3 |
| 12 | RBAC / Admin | identity/admin/controller/*.java; security/config/SecurityConfig.java | search/view/status/role/staff | /api/v1/admin/users; /staff-accounts | Implemented in source + Report 3 |
| 13 | Audit / Security events | audit/controller/AuditController.java; SecurityIncidentController.java | search/filter/review/notes | /api/v1/admin/audit-logs; /security-events | Implemented in source + Report 3 |
| 14 | Own-account deletion request | Report 3 UC-14 | N/A | N/A | Exists only in Report 3; deactivate exists, retention deletion request endpoint not found |
| 15 | Expert trust review | expert/controller/* | expert onboarding/review | /api/v1/expert/... | Implemented in source, but Report 3 classifies it under MF-05; excluded from MF-01 collection |

All source paths above are relative to `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend`.

## PHẦN C — PHÂN NHÓM MF-01

Total designed cases: **141**. Operation coverage: VIEW=15, ADD=11, UPDATE=17, REVOKE=10, SEARCH=5, FILTER=6, SORT=3, PAGE=6, VALID=21, AUTH=12, ACCESS=13, INT=13, PRIVACY=9.

| Feature group | Cases |
|---|---:|
| Register | 16 |
| Login | 12 |
| OTP | 8 |
| Forgot Password | 6 |
| Refresh Token | 4 |
| Logout / Account lifecycle | 6 |
| Profile | 12 |
| Notification Preference / Notification | 16 |
| Session | 11 |
| Consent | 9 |
| RBAC / Admin | 22 |
| Audit / Privacy / Identity | 19 |

Independent community identity and retention-based delete request are kept as Report-3-only cases with no invented endpoint. Expert trust review is excluded because Report 3 assigns it to MF-05.

## PHẦN D — CHI TIẾT POSTMAN REQUEST

---

Test Case ID:
MF01-VIEW-001

Test Group:
Register

Requirement/Function:
UC-01/UC-03

Source:
05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Thiết bị/emulator có mạng; backend URL test được cấu hình; chưa có token.

Test Procedure:
1. Cài bản build Mobile App kết nối môi trường test.
2. Mở ứng dụng khi chưa đăng nhập.
3. Mở Login, sau đó chuyển sang Register.
4. Kiểm tra các trường và nút hiển thị.
5. Không gửi dữ liệu.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Login hiển thị đúng trường email/phone và password; Register hiển thị name, email/phone, password và role hợp lệ; không hiển thị trường không tồn tại trong request model.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-VIEW-001-request-response.png; MF01-VIEW-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01/UC-03; Source: 05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VIEW-002

Test Group:
Login

Requirement/Function:
UC-01/UC-03

Source:
05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Web dev server và API test hoạt động; trình duyệt chưa đăng nhập.

Test Procedure:
1. Mở Web Portal ở origin được CORS cho phép.
2. Truy cập Login.
3. Mở Expert Register.
4. Kiểm tra trường, nhãn lỗi và nút gửi.
5. Không gửi dữ liệu.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Trang hiển thị đúng contract của LoginRequest/RegisterRequest; role đăng ký chuyên gia được gửi là EXPERT; không lộ token hay dữ liệu người dùng khác.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-VIEW-002-request-response.png; MF01-VIEW-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01/UC-03; Source: 05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VIEW-003

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Có phản hồi đăng ký 201 và OTP challenge chưa dùng.

Test Procedure:
1. Đăng ký một tài khoản mới hợp lệ.
2. Điều hướng đến OTP screen/page.
3. Kiểm tra identifier đã được truyền đúng.
4. Kiểm tra ô OTP 6 chữ số và Resend OTP.
5. Không nhập OTP.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- OTP UI hiển thị đúng identifier đã đăng ký, chỉ nhận mã 6 chữ số và có thao tác resend; chưa tạo session trước khi OTP hợp lệ.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-VIEW-003-request-response.png; MF01-VIEW-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VIEW-004

Test Group:
Profile / Privacy

Requirement/Function:
UC-08

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/mapper/UserMapper.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/auth/profile

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có session hợp lệ và access token; PostgreSQL có user tương ứng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/auth/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả id, name, email/phone, role và trạng thái được phép; không trả passwordHash, refresh token hoặc OTP; tạo audit PROFILE_VIEWED.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-004-request-response.png; MF01-VIEW-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-08; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/mapper/UserMapper.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-VIEW-005

Test Group:
Profile / Privacy

Requirement/Function:
UC-08

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java. Source extension mapped to UC-08

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/profile

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có UserProfile và token hợp lệ.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ trả profile gắn với userId trong JWT, gồm displayName, avatarUrl, phoneNumber, dateOfBirth, area; không nhận userId từ client.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-005-request-response.png; MF01-VIEW-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-08; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java. Source extension mapped to UC-08.

---

Test Case ID:
MF01-VIEW-006

Test Group:
Profile / Privacy

Requirement/Function:
UC-10

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/controller/PrivacySettingsController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/service/impl/PrivacySettingsServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/dto/UpdatePrivacySettingsRequest.java. Partial implementation of community identity/privacy settings

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/privacy-settings/me

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có token hợp lệ; privacy settings có thể chưa tồn tại để kiểm tra default creation.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/privacy-settings/me.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả profileVisibility, locationSharingEnabled, analyticsConsent, dataExportOptOut của chính user; ghi audit PRIVACY_SETTINGS_ACCESSED.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-006-request-response.png; MF01-VIEW-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-10; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/controller/PrivacySettingsController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/service/impl/PrivacySettingsServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/dto/UpdatePrivacySettingsRequest.java. Partial implementation of community identity/privacy settings.

---

Test Case ID:
MF01-VIEW-007

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/users/me/notification-preferences

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có token hợp lệ; có hoặc chưa có preference rows.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/users/me/notification-preferences.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả danh sách preference theo NotificationType và appointmentReminderDefaults; userId lấy từ JWT; audit NOTIFICATION_PREFERENCES_VIEWED được ghi.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-007-request-response.png; MF01-VIEW-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java.

---

Test Case ID:
MF01-VIEW-008

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/notifications/me?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có token; có ít nhất 2 notification_records thuộc user và 1 record thuộc user khác.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/notifications/me?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ trả notification visible của user trong JWT, mới nhất trước; response có read state và metadata đúng contract.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-008-request-response.png; MF01-VIEW-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-VIEW-009

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeMobileApp/lib/features/notification/services/notification_service.dart; 05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/notifications/me?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có token và không có notification_records visible.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/notifications/me?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; content rỗng, totalElements=0; UI hiển thị empty state, không hiển thị dữ liệu user khác.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-009-request-response.png; MF01-VIEW-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeMobileApp/lib/features/notification/services/notification_service.dart; 05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart.

---

Test Case ID:
MF01-VIEW-010

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/sessions

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có ít nhất hai auth_sessions và token thuộc một session.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/sessions.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả sessionId, deviceName, browser, ipAddress, lastActivityAt, status; đánh dấu đúng session hiện tại theo JWT sid; không trả refreshTokenHash.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-010-request-response.png; MF01-VIEW-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-VIEW-011

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users/{{user_id_from_precondition}}

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- USER_ID_FROM_PRECONDITION

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token hợp lệ; target user tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users/<USER_ID_FROM_PRECONDITION>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả summary của đúng user; không lộ passwordHash hoặc raw token.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-011-request-response.png; MF01-VIEW-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-VIEW-012

Test Group:
Session Management

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users/{{user_id_from_precondition}}/sessions?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
- USER_ID_FROM_PRECONDITION

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; target user có nhiều sessions.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users/<USER_ID_FROM_PRECONDITION>/sessions?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ trả sessions của target user theo trang, không trả refresh token/hash.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-012-request-response.png; MF01-VIEW-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-VIEW-013

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users/{{user_id_from_precondition}}/activity?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
- USER_ID_FROM_PRECONDITION

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; target user có audit_events.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users/<USER_ID_FROM_PRECONDITION>/activity?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ trả audit activity của target user theo trang và thời gian.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-013-request-response.png; MF01-VIEW-013-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-VIEW-014

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
Token role SYSTEM_ADMIN hoặc OPERATIONS; có audit_events.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; audit rows sắp xếp giảm dần theo createdAt; mỗi lần xem tạo meta-audit VIEW_AUDIT_LOG mà không chép toàn bộ PII result vào payload.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-014-request-response.png; MF01-VIEW-014-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-VIEW-015

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; có security_events nhiều severity/status.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả security events được phân trang; query được meta-audit; không trả thêm health content ngoài trường cần thiết.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VIEW-015-request-response.png; MF01-VIEW-015-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java.

---

Test Case ID:
MF01-ADD-001

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Nguyen An",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
Email chưa tồn tại; PostgreSQL sạch cho identifier; SMTP stub hoạt động.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Nguyen An",
  "email": "mf01.email@example.test",
  "password": "SafePass1!",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
201

Expected Response:
- HTTP 201; users có một row PENDING_ACTIVATION/enabled=false; OTP hash và expiry được lưu; email OTP được gọi một lần; không tạo session/token trước verify.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-001-request-response.png; MF01-ADD-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql.

---

Test Case ID:
MF01-ADD-002

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Tran Binh",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

Pre-condition:
Phone chưa tồn tại; SMS stub hoạt động.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Tran Binh",
  "phone": "0912345678",
  "password": "SafePass1!",
  "role": "FAMILY"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
201

Expected Response:
- HTTP 201; phone được canonicalize; user chưa active; OTP hash được lưu và SMS adapter nhận đúng phone; chưa tạo token/session.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-002-request-response.png; MF01-ADD-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java.

---

Test Case ID:
MF01-ADD-003

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Le Chi",
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
Email mới; SMTP stub hoạt động.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Le Chi",
  "email": "mf01.norole@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
201

Expected Response:
- HTTP 201; user.role=null; audit OTP_SENT ghi role=UNASSIGNED; user có thể chọn self-service role sau khi xác thực.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-003-request-response.png; MF01-ADD-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-ADD-004

Test Group:
Register / Identity

Requirement/Function:
UC-01/UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/FederatedAuthRequest.java. Source-only federated extension not explicitly separated in Report 3

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/federated

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "idToken": "{{firebaseIdToken}}",
  "deviceInfo": "CareBridge Flutter test"
}
```

Pre-condition:
Firebase emulator có identity chưa liên kết; DB chưa có provider subject/contact.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/federated.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "idToken": "<VALID_FIREBASE_ID_TOKEN>",
  "deviceInfo": "CareBridge Flutter test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
201

Expected Response:
- HTTP 201; tạo đúng một user_identity và CareBridge user/session; trả access/refresh token; không lưu raw Firebase ID token.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-004-request-response.png; MF01-ADD-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01/UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/FederatedAuthRequest.java. Source-only federated extension not explicitly separated in Report 3.

---

Test Case ID:
MF01-ADD-005

Test Group:
Register / Identity

Requirement/Function:
UC-01/UC-08

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/LinkGoogleIdentityRequest.java. Source-only identity-link extension

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/identities/google

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "idToken": "{{firebaseIdToken}}"
}
```

Pre-condition:
CareBridge user ACTIVE có token; Google provider subject chưa liên kết tài khoản khác.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/identities/google.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "idToken": "<FRESH_GOOGLE_FIREBASE_ID_TOKEN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; tạo một user_identity liên kết đúng user; không cấp session token mới; audit FEDERATED_IDENTITY_LINKED được ghi.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-005-request-response.png; MF01-ADD-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01/UC-08; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/LinkGoogleIdentityRequest.java. Source-only identity-link extension.

---

Test Case ID:
MF01-ADD-006

Test Group:
Consent

Requirement/Function:
UC-15

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/consent/grants

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "pregnancy-summary",
  "expiryDays": 30
}
```

Pre-condition:
User ACTIVE có token; recipient test hợp lệ theo luồng tích hợp.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/consent/grants.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "pregnancy-summary",
  "expiryDays": 30
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; tạo consent grant thuộc user trong JWT, expiryAt=now+30d, status hợp lệ; audit CONSENT_GRANTED được ghi.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-006-request-response.png; MF01-ADD-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-15; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-ADD-007

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11/UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/RegisterDeviceTokenRequest.java. Source extension supporting UC-11/UC-12

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/notifications/device-token

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "token": "{{resetToken}}",
  "platform": "ANDROID"
}
```

Pre-condition:
User ACTIVE có token; Firebase/FCM stub; fixture device token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/notifications/device-token.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "token": "<FCM_DEVICE_TOKEN_FROM_TEST_FIXTURE>",
  "platform": "ANDROID"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; device token active thuộc user JWT; đăng ký lặp không tạo duplicate; token cũ thuộc user khác bị deactivate trước khi gán lại.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-007-request-response.png; MF01-ADD-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11/UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/RegisterDeviceTokenRequest.java. Source extension supporting UC-11/UC-12.

---

Test Case ID:
MF01-ADD-008

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminStaffController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminStaffServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/CreateStaffAccountRequest.java. Source extension within UC-17

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/admin/staff-accounts

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "phone": "{{phone}}",
  "name": "Moderator QA",
  "role": "MODERATOR"
}
```

Pre-condition:
SYSTEM_ADMIN token; email/phone mới; SMTP stub.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/admin/staff-accounts.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "moderator.mf01@example.test",
  "phone": "0912345679",
  "name": "Moderator QA",
  "role": "MODERATOR"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
201

Expected Response:
- HTTP 201; staff account tạo một lần với temp credential server-generated, không nhận password từ admin; email gửi một lần; audit STAFF_ACCOUNT_CREATED được ghi.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-008-request-response.png; MF01-ADD-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminStaffController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminStaffServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/CreateStaffAccountRequest.java. Source extension within UC-17.

---

Test Case ID:
MF01-ADD-009

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/AddSecurityNoteRequest.java. Source extension within UC-18 investigation

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/admin/security-events/{{event_id_from_precondition}}/notes

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- EVENT_ID_FROM_PRECONDITION

Request Body:
```json
{
  "noteText": "Reviewed against authentication logs; follow-up required."
}
```

Pre-condition:
SYSTEM_ADMIN token; security event tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/admin/security-events/<EVENT_ID_FROM_PRECONDITION>/notes.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "noteText": "Reviewed against authentication logs; follow-up required."
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; note được trim và gắn đúng event/author; audit SECURITY_NOTE_ADDED được ghi; event gốc không bị sửa ngoài ý muốn.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-009-request-response.png; MF01-ADD-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/AddSecurityNoteRequest.java. Source extension within UC-18 investigation.

---

Test Case ID:
MF01-ADD-010

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/SendNotificationRequest.java. Source integration supporting UC-12

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/notifications/send

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "recipientUserId": "{{userId}}",
  "type": "COMMUNITY_REPLY",
  "title": "New reply",
  "body": "A new reply is available.",
  "referenceId": "{{reference_id_from_precondition}}",
  "referenceType": "COMMUNITY_QUESTION"
}
```

Pre-condition:
SYSTEM_ADMIN token; recipient có active device token; FCM stub success.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/notifications/send.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "recipientUserId": "<USER_ID_FROM_PRECONDITION>",
  "type": "COMMUNITY_REPLY",
  "title": "New reply",
  "body": "A new reply is available.",
  "referenceId": "<REFERENCE_ID_FROM_PRECONDITION>",
  "referenceType": "COMMUNITY_QUESTION"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; FCM được gọi; notification_record trạng thái SENT/attemptCount=1; audit NOTIFICATION_SENT được ghi.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ADD-010-request-response.png; MF01-ADD-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/SendNotificationRequest.java. Source integration supporting UC-12.

---

Test Case ID:
MF01-ADD-011

Test Group:
Public/Private Identity

Requirement/Function:
UC-10

Source:
Report 3 only; implementation not found.

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE; môi trường test; chưa có community identity riêng.

Test Procedure:
1. Mở Community Identity settings theo Report 3.
2. Nhập display name, avatar và visibility.
3. Lưu.
4. Kiểm tra public community projection và private profile storage.
5. Kiểm tra audit.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Tạo public community identity riêng, không dùng hoặc lộ maternal/baby/family private fields; có audit.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-ADD-011-request-response.png; MF01-ADD-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-10; Requirement exists in Report 3, but implementation was not found in current source.

---

Test Case ID:
MF01-UPDATE-001

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/verify-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

Pre-condition:
User pending activation và OTP REGISTER chưa hết hạn.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/verify-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "mf01.email@example.test",
  "otp": "<VALID_6_DIGIT_OTP_FROM_STUB>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; OTP usedAt/verified cập nhật một lần; user ACTIVE/enabled=true; tạo đúng một session và token pair; audit OTP_VERIFIED và USER_REGISTRATION_COMPLETED.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-001-request-response.png; MF01-UPDATE-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java.

---

Test Case ID:
MF01-UPDATE-002

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/resend-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}"
}
```

Pre-condition:
User pending có một OTP chưa dùng; cooldown chưa bị consume.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/resend-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "mf01.pending@example.test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; OTP cũ usedAt được set; OTP mới giữ đúng purpose, expiry và 5 attempts; SMTP gọi một lần; audit OTP_RESENT.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-002-request-response.png; MF01-UPDATE-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-UPDATE-003

Test Group:
Profile / Privacy

Requirement/Function:
UC-09

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/UpdateProfileRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/auth/profile

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Updated Name",
  "avatarUrl": "https://example.test/avatar.png"
}
```

Pre-condition:
User ACTIVE có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/auth/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Updated Name",
  "avatarUrl": "https://example.test/avatar.png"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; users.name/avatar_url cập nhật cho user JWT; response mới đúng; audit PROFILE_UPDATED; không thay email/phone/role.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-003-request-response.png; MF01-UPDATE-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-09; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/UpdateProfileRequest.java.

---

Test Case ID:
MF01-UPDATE-004

Test Group:
Profile / Privacy

Requirement/Function:
UC-09

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java. Source extension mapped to UC-09

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/profile

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "area": "Ho Chi Minh City"
}
```

Pre-condition:
User ACTIVE có UserProfile và token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "area": "Ho Chi Minh City"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ area thay đổi; displayName/avatar/phone/dateOfBirth giữ nguyên; audit PROFILE_UPDATED trong cùng transaction.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-004-request-response.png; MF01-UPDATE-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-09; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java. Source extension mapped to UC-09.

---

Test Case ID:
MF01-UPDATE-005

Test Group:
Profile / Privacy

Requirement/Function:
UC-09

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/profile

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "displayName": "Mai Anh",
  "avatarUrl": "https://example.test/mai.png",
  "phoneNumber": "0912345678",
  "dateOfBirth": "1995-05-20",
  "area": "Da Nang"
}
```

Pre-condition:
User ACTIVE có token và profile.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "displayName": "Mai Anh",
  "avatarUrl": "https://example.test/mai.png",
  "phoneNumber": "0912345678",
  "dateOfBirth": "1995-05-20",
  "area": "Da Nang"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; các field hợp lệ cập nhật đúng; user display name đồng bộ; không sửa userId; audit một lần.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-005-request-response.png; MF01-UPDATE-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-09; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java.

---

Test Case ID:
MF01-UPDATE-006

Test Group:
Profile / Privacy

Requirement/Function:
UC-10

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/controller/PrivacySettingsController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/service/impl/PrivacySettingsServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/dto/UpdatePrivacySettingsRequest.java. Partial implementation of UC-10 privacy/visibility

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/privacy-settings/me

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "profileVisibility": "PRIVATE",
  "locationSharingEnabled": false,
  "analyticsConsent": false,
  "dataExportOptOut": true
}
```

Pre-condition:
User ACTIVE có token; analyticsConsent ban đầu true.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/privacy-settings/me.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "profileVisibility": "PRIVATE",
  "locationSharingEnabled": false,
  "analyticsConsent": false,
  "dataExportOptOut": true
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; settings của user JWT cập nhật; withdrawal analytics được audit; không cập nhật settings user khác.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-006-request-response.png; MF01-UPDATE-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-10; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/controller/PrivacySettingsController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/service/impl/PrivacySettingsServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/dto/UpdatePrivacySettingsRequest.java. Partial implementation of UC-10 privacy/visibility.

---

Test Case ID:
MF01-UPDATE-007

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/users/me/notification-preferences

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "preferences": [
    {
      "notificationType": "COMMUNITY_REPLY",
      "pushEnabled": false,
      "emailEnabled": true,
      "inAppEnabled": true
    }
  ],
  "appointmentReminderDefaults": [
    -1440,
    -60,
    0
  ]
}
```

Pre-condition:
User ACTIVE có token; preference row có hoặc chưa tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/users/me/notification-preferences.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "preferences": [
    {
      "notificationType": "COMMUNITY_REPLY",
      "pushEnabled": false,
      "emailEnabled": true,
      "inAppEnabled": true
    }
  ],
  "appointmentReminderDefaults": [
    -1440,
    -60,
    0
  ]
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; upsert không duplicate theo user+type; reminder defaults được normalize; audit NOTIFICATION_PREFERENCES_UPDATED.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-007-request-response.png; MF01-UPDATE-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java.

---

Test Case ID:
MF01-UPDATE-008

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/notifications/{{notification_id_from_precondition}}/read

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- NOTIFICATION_ID_FROM_PRECONDITION

Request Body:
```json
{}
```

Pre-condition:
User token sở hữu notification đang unread.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/notifications/<NOTIFICATION_ID_FROM_PRECONDITION>/read.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; is_read=true và read_at được cập nhật atomically; audit NOTIFICATIONS_READ count=1 được ghi.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-008-request-response.png; MF01-UPDATE-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-UPDATE-009

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/notifications/{{already_read_notification_id}}/read

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- ALREADY_READ_NOTIFICATION_ID

Request Body:
```json
{}
```

Pre-condition:
User token sở hữu notification đã read.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/notifications/<ALREADY_READ_NOTIFICATION_ID>/read.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; read_at không bị tạo audit mới; không thay đổi dữ liệu khác.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-009-request-response.png; MF01-UPDATE-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-UPDATE-010

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/notifications/read-all

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{}
```

Pre-condition:
User có nhiều unread notifications và user khác cũng có unread records.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/notifications/read-all.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; markedCount bằng số row thực sự đổi; chỉ notifications của user JWT được cập nhật; audit count chính xác.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-010-request-response.png; MF01-UPDATE-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-UPDATE-011

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserStatusRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "locked": true,
  "reason": "Confirmed abuse during QA test"
}
```

Pre-condition:
SYSTEM_ADMIN token; target khác caller, ACTIVE và unlocked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "locked": true,
  "reason": "Confirmed abuse during QA test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; target locked=true, ADMIN lock metadata và episode id được tạo; toàn bộ target sessions revoked; audit USER_ACCOUNT_STATUS_CHANGED.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-011-request-response.png; MF01-UPDATE-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserStatusRequest.java.

---

Test Case ID:
MF01-UPDATE-012

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserStatusRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "locked": false,
  "reason": "Issue resolved",
  "cskhTicketId": "CSKH-TEST-001"
}
```

Pre-condition:
SYSTEM_ADMIN token; target đang ADMIN locked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "locked": false,
  "reason": "Issue resolved",
  "cskhTicketId": "CSKH-TEST-001"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; lock metadata được clear; audit giữ lock episode reference/ticket; không tự khôi phục session đã revoke.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-012-request-response.png; MF01-UPDATE-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserStatusRequest.java.

---

Test Case ID:
MF01-UPDATE-013

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "enabled": false,
  "reason": "Administrative disable"
}
```

Pre-condition:
SYSTEM_ADMIN token; target khác caller và enabled.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "enabled": false,
  "reason": "Administrative disable"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; enabled=false; toàn bộ sessions của target revoked; token cũ bị từ chối ở protected endpoint; audit status change.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-013-request-response.png; MF01-UPDATE-013-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-UPDATE-014

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminRoleController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminRoleServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserRoleRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{staff_user_id}}/role

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- STAFF_USER_ID

Request Body:
```json
{
  "newRole": "CONTENT_ADMIN",
  "lockAccessRights": false,
  "reason": "Approved staff reassignment"
}
```

Pre-condition:
SYSTEM_ADMIN token; target là staff governance role và khác caller.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<STAFF_USER_ID>/role.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "newRole": "CONTENT_ADMIN",
  "lockAccessRights": false,
  "reason": "Approved staff reassignment"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; role đổi từ MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN sang role hợp lệ; audit ROLE_PERMISSION_UPDATED có previous/new role.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-014-request-response.png; MF01-UPDATE-014-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminRoleController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminRoleServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/dto/request/UpdateUserRoleRequest.java.

---

Test Case ID:
MF01-UPDATE-015

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/ReviewSecurityEventRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/admin/security-events/{{event_id_from_precondition}}/review

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- EVENT_ID_FROM_PRECONDITION

Request Body:
```json
{
  "status": "UNDER_REVIEW"
}
```

Pre-condition:
SYSTEM_ADMIN token; security event tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/admin/security-events/<EVENT_ID_FROM_PRECONDITION>/review.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "status": "UNDER_REVIEW"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; status/reviewer/reviewedAt cập nhật; audit SECURITY_EVENT_REVIEWED được ghi.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-015-request-response.png; MF01-UPDATE-015-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/ReviewSecurityEventRequest.java.

---

Test Case ID:
MF01-UPDATE-016

Test Group:
Forgot / Password

Requirement/Function:
UC-07

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/auth/change-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "oldPassword": "{{password}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

Pre-condition:
User ACTIVE có token và password OldPass1!; có refresh token đang active.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/auth/change-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "oldPassword": "OldPass1!",
  "newPassword": "NewPass2@",
  "confirmPassword": "NewPass2@"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; password hash thay đổi; refresh tokens hiện hành bị revoke; audit PASSWORD_CHANGED; đăng nhập bằng password cũ thất bại.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-016-request-response.png; MF01-UPDATE-016-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-07; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java.

---

Test Case ID:
MF01-UPDATE-017

Test Group:
Forgot / Password

Requirement/Function:
UC-06

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ResetPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResetPasswordRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/reset-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "token": "{{resetToken}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

Pre-condition:
User ACTIVE; reset token single-use chưa hết hạn; SMTP/SMS reset flow đã tạo token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/reset-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "token": "<PASSWORD_RESET_TOKEN_FROM_STUB>",
  "newPassword": "ResetPass2@",
  "confirmPassword": "ResetPass2@"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; password hash cập nhật; reset token used; refresh tokens revoke; audit PASSWORD_RESET_COMPLETED; token không dùng lại được.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-UPDATE-017-request-response.png; MF01-UPDATE-017-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-06; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ResetPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResetPasswordRequest.java.

---

Test Case ID:
MF01-REVOKE-001

Test Group:
Logout / Account Lifecycle

Requirement/Function:
UC-04

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/logout

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Pre-condition:
User ACTIVE đã login; access/refresh token hợp lệ.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/logout.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "refreshToken": "<REFRESH_TOKEN_FROM_LOGIN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; auth_session và refresh token bị revoke; token hash vào blacklist; audit LOGOUT; refresh sau đó bị từ chối.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-001-request-response.png; MF01-REVOKE-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-04; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-REVOKE-002

Test Group:
Logout / Account Lifecycle

Requirement/Function:
UC-04

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/logout

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{}
```

Pre-condition:
User có ít nhất hai session; request dùng access token của một session.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/logout.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; đúng session theo sid bị revoke và blacklist; các session khác vẫn active; security context được clear.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-002-request-response.png; MF01-REVOKE-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-04; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-REVOKE-003

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/sessions/{{other_session_id_from_precondition}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- OTHER_SESSION_ID_FROM_PRECONDITION

Request Body:
None

Pre-condition:
User có current session và một other session active.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/sessions/<OTHER_SESSION_ID_FROM_PRECONDITION>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ session được chọn bị revoke; refresh token hash bị blacklist; audit SESSION_REVOKED; current session vẫn active.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-003-request-response.png; MF01-REVOKE-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-REVOKE-004

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/sessions

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User có current session và ít nhất hai other sessions.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/sessions.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; tất cả session khác revoked atomically; current sid vẫn active; message count chính xác; audit khi count>0.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-004-request-response.png; MF01-REVOKE-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-REVOKE-005

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/consent/grants/{{consent_id_from_precondition}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- CONSENT_ID_FROM_PRECONDITION

Request Body:
None

Pre-condition:
User token sở hữu active consent có permissionId.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/consent/grants/<CONSENT_ID_FROM_PRECONDITION>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; consent status REVOKED/revokedAt/revokedBy cập nhật; dependent location/recommendation shares cleanup; audit CONSENT_REVOKED; lần protected read tiếp theo bị chặn.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-005-request-response.png; MF01-REVOKE-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-REVOKE-006

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11/UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java. Source integration supporting UC-11/UC-12

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/notifications/device-token?token={{fcm_device_token}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- token=<FCM_DEVICE_TOKEN>

Path Parameters:
- FCM_DEVICE_TOKEN

Request Body:
None

Pre-condition:
User token; active device token thuộc user.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/notifications/device-token?token=<FCM_DEVICE_TOKEN>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; token của đúng user chuyển inactive; cùng token của user khác không bị tác động.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-006-request-response.png; MF01-REVOKE-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11/UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java. Source integration supporting UC-11/UC-12.

---

Test Case ID:
MF01-REVOKE-007

Test Group:
Logout / Account Lifecycle

Requirement/Function:
UC-14

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/DeactivateRequest.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/auth/deactivate

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "User requested deactivation"
}
```

Pre-condition:
User ACTIVE không phải SYSTEM_ADMIN, có token, sessions và device token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/auth/deactivate.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "confirmPassword": "SafePass1!",
  "reason": "User requested deactivation"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; account_status=DEACTIVATED/enabled=false; sessions, refresh tokens và device tokens bị revoke trong cùng transaction; audit SECURITY_EVENT; dữ liệu được giữ theo retention.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-007-request-response.png; MF01-REVOKE-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-14; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/DeactivateRequest.java.

---

Test Case ID:
MF01-REVOKE-008

Test Group:
Account Lifecycle

Requirement/Function:
UC-14

Source:
Report 3 only; implementation not found.

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE không phải SYSTEM_ADMIN; dữ liệu care-group/audit fixture có sẵn.

Test Procedure:
1. Mở account lifecycle.
2. Chọn Delete Account.
3. Xác nhận password và retention notice.
4. Gửi yêu cầu.
5. Kiểm tra trạng thái deletion request, sessions và audit.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Yêu cầu xóa được ghi nhận theo retention/care-group/audit obligations; session bị bảo vệ phù hợp; không xóa dữ liệu trái chính sách.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-REVOKE-008-request-response.png; MF01-REVOKE-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-14; Requirement exists in Report 3, but implementation was not found in current source.

---

Test Case ID:
MF01-REVOKE-009

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/consent/grants/{{revoked_consent_id}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- REVOKED_CONSENT_ID

Request Body:
None

Pre-condition:
User token sở hữu consent đã revoke.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/consent/grants/<REVOKED_CONSENT_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 với CONSENT-012; revokedAt giữ nguyên; không tạo audit revocation thứ hai.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-009-request-response.png; MF01-REVOKE-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-REVOKE-010

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/sessions/{{current_session_id}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- CURRENT_SESSION_ID

Request Body:
None

Pre-condition:
User có access token chứa sid trùng path sessionId.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/sessions/<CURRENT_SESSION_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 với hướng dẫn dùng Logout; session hiện tại không bị revoke.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-REVOKE-010-request-response.png; MF01-REVOKE-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-SEARCH-001

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?email=target%40example.test&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- email=target%40example.test
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; có target và non-target users.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?email=target%40example.test&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ kết quả phù hợp email filter, không trả user không liên quan.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SEARCH-001-request-response.png; MF01-SEARCH-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-SEARCH-002

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?name=Anh&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- name=Anh
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; fixture nhiều tên.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?name=Anh&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả các user phù hợp query name theo repository contract; không vượt page size.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SEARCH-002-request-response.png; MF01-SEARCH-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-SEARCH-003

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?email=no-match%40example.test&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- email=no-match%40example.test
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; email query không tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?email=no-match%40example.test&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; content rỗng và totalElements=0; UI thể hiện no result.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SEARCH-003-request-response.png; MF01-SEARCH-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-SEARCH-004

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?userId={{user_id}}&action=LOGIN&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- userId=<USER_ID>
- action=LOGIN
- page=0
- size=20

Path Parameters:
- USER_ID

Request Body:
None

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token; có nhiều audit action/user.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?userId=<USER_ID>&action=LOGIN&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; mọi row khớp userId và LOGIN; filter snapshot được meta-audit.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SEARCH-004-request-response.png; MF01-SEARCH-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-SEARCH-005

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events/timeline?correlationId={{correlation_id}}

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- correlationId=<CORRELATION_ID>

Path Parameters:
- CORRELATION_ID

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; có ít nhất ba correlated events.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events/timeline?correlationId=<CORRELATION_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả đúng chuỗi event cùng correlationId theo occurredAt tăng dần.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SEARCH-005-request-response.png; MF01-SEARCH-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java.

---

Test Case ID:
MF01-FILTER-001

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/notifications/me?type=COMMUNITY_REPLY&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- type=COMMUNITY_REPLY
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
User token; có notifications nhiều loại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/notifications/me?type=COMMUNITY_REPLY&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; tất cả row trả về có type COMMUNITY_REPLY và thuộc user JWT.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-001-request-response.png; MF01-FILTER-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-FILTER-002

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?role=MOTHER&enabled=true&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- role=MOTHER
- enabled=true
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; fixture nhiều role/status.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?role=MOTHER&enabled=true&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; mọi kết quả role=MOTHER và enabled=true.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-002-request-response.png; MF01-FILTER-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-FILTER-003

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?locked=true&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- locked=true
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; có locked và unlocked users.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?locked=true&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; chỉ trả tài khoản locked.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-003-request-response.png; MF01-FILTER-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-FILTER-004

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?fromDate=2026-08-01T00:00:00Z&toDate=2026-08-09T23:59:59Z&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- fromDate=2026-08-01T00:00:00Z
- toDate=2026-08-09T23:59:59Z
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token; có audit trước/trong/sau khoảng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?fromDate=2026-08-01T00:00:00Z&toDate=2026-08-09T23:59:59Z&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; mọi audit row nằm trong khoảng thời gian; không bỏ qua boundary hợp lệ.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-004-request-response.png; MF01-FILTER-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-FILTER-005

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events?severity=HIGH&status=OPEN&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- severity=HIGH
- status=OPEN
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; fixture nhiều severity/status.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events?severity=HIGH&status=OPEN&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; mọi event khớp HIGH và OPEN.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-005-request-response.png; MF01-FILTER-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java.

---

Test Case ID:
MF01-FILTER-006

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeWebApp/src/features/admin

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; trước đó đã áp dụng filter trên UI/API.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; kết quả không còn bị giới hạn bởi filter trước đó; UI reset controls phản ánh trạng thái rỗng.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-FILTER-006-request-response.png; MF01-FILTER-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeWebApp/src/features/admin.

---

Test Case ID:
MF01-SORT-001

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/notifications/me?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
User token; ít nhất ba notification có createdAt khác nhau.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/notifications/me?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; createdAt không tăng dần giữa các row; pagination ổn định khi dữ liệu không đổi.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SORT-001-request-response.png; MF01-SORT-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-SORT-002

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/sessions/paged?page=0&size=10

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=0
- size=10

Path Parameters:
None

Request Body:
None

Pre-condition:
User token; nhiều sessions có lastActivityAt khác nhau.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/sessions/paged?page=0&size=10.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; session rows theo lastActivityAt DESC; current flag vẫn đúng sau sort.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SORT-002-request-response.png; MF01-SORT-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-SORT-003

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token; có nhiều audit rows.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; audit rows newest-first theo controller PageRequest.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-SORT-003-request-response.png; MF01-SORT-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-PAGE-001

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/notifications/me?type=REMINDER&page=1&size=5

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- type=REMINDER
- page=1
- size=5

Path Parameters:
None

Request Body:
None

Pre-condition:
User token; có hơn 5 REMINDER notifications.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/notifications/me?type=REMINDER&page=1&size=5.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; trả trang thứ hai đúng size, filter REMINDER được giữ, không duplicate row từ page 0.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-001-request-response.png; MF01-PAGE-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-PAGE-002

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/sessions/paged?page=1&size=2

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=1
- size=2

Path Parameters:
None

Request Body:
None

Pre-condition:
User token; có ít nhất 5 sessions không revoked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/sessions/paged?page=1&size=2.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; page metadata đúng; không trùng sessionId với page 0.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-002-request-response.png; MF01-PAGE-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-PAGE-003

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?page=0&size=9999

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=9999

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; DB có nhiều users.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?page=0&size=9999.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; effective page size bị clamp theo AppConstants.MAX_PAGE_SIZE; không gây tải toàn bảng.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-003-request-response.png; MF01-PAGE-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-PAGE-004

Test Group:
Session Management

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users/{{user_id}}/sessions?page=1&size=2

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=1
- size=2

Path Parameters:
- USER_ID

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; target có ít nhất 5 sessions.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users/<USER_ID>/sessions?page=1&size=2.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; đúng target user và page metadata; không trùng page trước.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-004-request-response.png; MF01-PAGE-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-PAGE-005

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users/{{user_id}}/activity?page=1&size=2

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=1
- size=2

Path Parameters:
- USER_ID

Request Body:
None

Pre-condition:
SYSTEM_ADMIN token; target có ít nhất 5 audit rows.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users/<USER_ID>/activity?page=1&size=2.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; đúng target user, trang và thứ tự; không trùng row page trước.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-005-request-response.png; MF01-PAGE-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-PAGE-006

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?page=-1&size=0

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=-1
- size=0

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?page=-1&size=0.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 với PAGINATION_INVALID; không query full table; không trả audit rows.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PAGE-006-request-response.png; MF01-PAGE-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-VALID-001

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Missing Contact",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
DB không có fixture tương ứng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Missing Contact",
  "password": "SafePass1!",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo user/OTP/audit và không gọi SMTP/SMS.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-001-request-response.png; MF01-VALID-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java.

---

Test Case ID:
MF01-VALID-002

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Weak Password",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
Email mới.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Weak Password",
  "email": "weak@example.test",
  "password": "password",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 với password complexity message; không tạo user/OTP.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-002-request-response.png; MF01-VALID-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-VALID-003

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Invalid Email",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
DB sạch cho dữ liệu.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Invalid Email",
  "email": "not-an-email",
  "password": "SafePass1!",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo user/OTP và không gọi email service.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-003-request-response.png; MF01-VALID-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java.

---

Test Case ID:
MF01-VALID-004

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Invalid Phone",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

Pre-condition:
DB sạch cho dữ liệu.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Invalid Phone",
  "phone": "12345",
  "password": "SafePass1!",
  "role": "FAMILY"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo user/OTP và không gọi SMS.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-004-request-response.png; MF01-VALID-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/RegisterRequest.java.

---

Test Case ID:
MF01-VALID-005

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Duplicate",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
Email đã tồn tại trong users.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Duplicate",
  "email": "existing@example.test",
  "password": "SafePass1!",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
409

Expected Response:
- HTTP 409; giữ nguyên user hiện có; không tạo OTP mới hoặc gửi OTP.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-005-request-response.png; MF01-VALID-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VALID-006

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "Invalid Role",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "SYSTEM_ADMIN"
}
```

Pre-condition:
Email mới.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "Invalid Role",
  "email": "admin-self@example.test",
  "password": "SafePass1!",
  "role": "SYSTEM_ADMIN"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo account; chỉ MOTHER/FAMILY/EXPERT được self-register.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-006-request-response.png; MF01-VALID-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-VALID-007

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/verify-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

Pre-condition:
User pending có OTP hợp lệ khác.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/verify-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "pending@example.test",
  "otp": "12345"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 từ validation; không giảm attempt, không activate user, không tạo session.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-007-request-response.png; MF01-VALID-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/VerifyOtpRequest.java.

---

Test Case ID:
MF01-VALID-008

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/verify-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

Pre-condition:
User pending; OTP REGISTER chưa hết hạn và attempts>1.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/verify-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "pending@example.test",
  "otp": "999999"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 Invalid OTP; attempts giảm đúng một; user vẫn disabled; không tạo token/session.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-008-request-response.png; MF01-VALID-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VALID-009

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/verify-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

Pre-condition:
OTP expiryAt trước now.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/verify-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "expired-otp@example.test",
  "otp": "123456"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 Invalid or expired OTP; user không active; không tạo session.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-009-request-response.png; MF01-VALID-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-VALID-010

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/resend-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "phone": "{{phone}}",
  "email": "{{email}}"
}
```

Pre-condition:
Pending account có cả phone/email.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/resend-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "phone": "0912345678",
  "email": "pending@example.test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; OTP cũ không bị invalidated; không tạo OTP mới, không gọi external service.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-010-request-response.png; MF01-VALID-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResendOtpRequest.java.

---

Test Case ID:
MF01-VALID-011

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "phone": "{{phone}}",
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
Account tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "phone": "0912345678",
  "email": "user@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo session và không cập nhật lastLoginAt.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-011-request-response.png; MF01-VALID-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/LoginRequest.java.

---

Test Case ID:
MF01-VALID-012

Test Group:
Forgot / Password

Requirement/Function:
UC-05

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ForgotPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ForgotPasswordRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/forgot-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "contact": "{{email}}"
}
```

Pre-condition:
Không cần token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/forgot-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "contact": ""
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo reset token, không gọi SMTP/SMS.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-012-request-response.png; MF01-VALID-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-05; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ForgotPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ForgotPasswordRequest.java.

---

Test Case ID:
MF01-VALID-013

Test Group:
Forgot / Password

Requirement/Function:
UC-06

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ResetPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResetPasswordRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/reset-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "token": "{{resetToken}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

Pre-condition:
Reset token hợp lệ chưa dùng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/reset-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "token": "<VALID_RESET_TOKEN>",
  "newPassword": "ResetPass2@",
  "confirmPassword": "Different3#"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; password và reset token giữ nguyên; refresh tokens không bị revoke.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-013-request-response.png; MF01-VALID-013-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-06; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ResetPasswordServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ResetPasswordRequest.java.

---

Test Case ID:
MF01-VALID-014

Test Group:
Forgot / Password

Requirement/Function:
UC-07

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/auth/change-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "oldPassword": "{{password}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

Pre-condition:
User ACTIVE có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/auth/change-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "oldPassword": "WrongPass1!",
  "newPassword": "NewPass2@",
  "confirmPassword": "NewPass2@"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 AUTH-071; password hash/session/refresh token không thay đổi; không audit PASSWORD_CHANGED.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-014-request-response.png; MF01-VALID-014-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-07; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/dto/request/ChangePasswordRequest.java.

---

Test Case ID:
MF01-VALID-015

Test Group:
Profile / Privacy

Requirement/Function:
UC-09

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/profile

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "dateOfBirth": "2099-01-01"
}
```

Pre-condition:
User ACTIVE có token/profile.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "dateOfBirth": "2099-01-01"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 PRF-002; profile và audit không thay đổi.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-015-request-response.png; MF01-VALID-015-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-09; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java.

---

Test Case ID:
MF01-VALID-016

Test Group:
Profile / Privacy

Requirement/Function:
UC-09

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/profile

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "displayName": "{{script}}alert(1){{/script}}"
}
```

Pre-condition:
User ACTIVE có token/profile.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "displayName": "<script>alert(1)</script>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 do DTO pattern; script không được lưu hoặc phản chiếu.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-016-request-response.png; MF01-VALID-016-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-09; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/service/impl/ProfileServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/dto/UpdateProfileRequest.java.

---

Test Case ID:
MF01-VALID-017

Test Group:
Consent

Requirement/Function:
UC-15

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/consent/grants

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "summary",
  "expiryDays": 0
}
```

Pre-condition:
User ACTIVE có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/consent/grants.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "summary",
  "expiryDays": 0
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không tạo grant/audit.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-017-request-response.png; MF01-VALID-017-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-15; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-VALID-018

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/users/me/notification-preferences

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "preferences": [
    {
      "notificationType": "UNKNOWN_TYPE",
      "pushEnabled": true,
      "emailEnabled": true,
      "inAppEnabled": true
    }
  ]
}
```

Pre-condition:
User ACTIVE có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/users/me/notification-preferences.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "preferences": [
    {
      "notificationType": "UNKNOWN_TYPE",
      "pushEnabled": true,
      "emailEnabled": true,
      "inAppEnabled": true
    }
  ]
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; không upsert preference và không ghi audit update.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-018-request-response.png; MF01-VALID-018-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationPreferenceServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/dto/UpdateNotificationPreferencesRequest.java.

---

Test Case ID:
MF01-VALID-019

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "reason": "No state selected"
}
```

Pre-condition:
SYSTEM_ADMIN token; target khác caller.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "reason": "No state selected"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 IAM-114-002; target không đổi; không audit.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-019-request-response.png; MF01-VALID-019-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-VALID-020

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "locked": true
}
```

Pre-condition:
SYSTEM_ADMIN token; target khác caller và unlocked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "locked": true
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400 IAM-114-005; target không bị lock; sessions không bị revoke.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-020-request-response.png; MF01-VALID-020-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-VALID-021

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/ReviewSecurityEventRequest.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/admin/security-events/{{event_id}}/review

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- EVENT_ID

Request Body:
```json
{
  "status": "DELETED"
}
```

Pre-condition:
SYSTEM_ADMIN token; event tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/admin/security-events/<EVENT_ID>/review.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "status": "DELETED"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400; event/reviewer/audit không thay đổi.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-VALID-021-request-response.png; MF01-VALID-021-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/dto/request/ReviewSecurityEventRequest.java.

---

Test Case ID:
MF01-AUTH-001

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User ACTIVE/enabled, unlocked, password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "active@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; tạo session ACTIVE, refresh token và access token có sid; lastLoginAt cập nhật; audit LOGIN; raw refresh token chỉ trả client.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-001-request-response.png; MF01-AUTH-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-AUTH-002

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "phone": "{{phone}}",
  "password": "{{password}}"
}
```

Pre-condition:
User ACTIVE có phone tương ứng và password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "phone": "0912345678",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; phone canonicalize; tạo đúng một session và token pair.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-002-request-response.png; MF01-AUTH-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-AUTH-003

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User ACTIVE tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "active@example.test",
  "password": "WrongPass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401/400 theo global error mapping với Invalid credentials; không lộ trạng thái account; không tạo session/token.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-003-request-response.png; MF01-AUTH-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-004

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User PENDING_ACTIVATION/enabled=false, password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "pending@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 account disabled; không tạo session/token.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-004-request-response.png; MF01-AUTH-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-005

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User enabled, ADMIN locked, password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "adminlocked@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 account admin locked; không xóa lock reason/metadata; không tạo session.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-005-request-response.png; MF01-AUTH-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-006

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User enabled/unlocked, suspendedUntil tương lai.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "suspended@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 ACCOUNT_SUSPENDED; không tạo session; suspendedUntil giữ nguyên.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-006-request-response.png; MF01-AUTH-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-007

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User ACTIVE; rate-limit store sạch; lặp đúng số request trong window 15 phút.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "ratelimit@example.test",
  "password": "WrongPass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Sau số lần theo RateLimitPolicy, request bị từ chối; user locked=true, lockType=TEMPORARY; không tạo session.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-007-request-response.png; MF01-AUTH-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-008

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User TEMPORARY lockedAt quá 15 phút, enabled, password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "expiredlock@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; temporary lock được clear, session/token tạo; admin lock không được áp dụng logic này.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-008-request-response.png; MF01-AUTH-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/AuthenticationPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/PasswordComplexityPolicy.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java.

---

Test Case ID:
MF01-AUTH-009

Test Group:
Refresh Token

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/refresh

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Pre-condition:
User ACTIVE; session ACTIVE; refresh token chưa revoke/chưa hết hạn.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/refresh.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "refreshToken": "<VALID_REFRESH_TOKEN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; token cũ revoked; session hash/expiry cập nhật; trả access và refresh token mới; sessionId giữ nguyên.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-009-request-response.png; MF01-AUTH-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-AUTH-010

Test Group:
Refresh Token

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/refresh

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Pre-condition:
Refresh token cũ đã rotate/revoke; session dùng hash mới.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/refresh.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "refreshToken": "<ROTATED_OLD_REFRESH_TOKEN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401/400 invalid refresh token; không tạo token/session mới; token hiện hành vẫn nhất quán.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-010-request-response.png; MF01-AUTH-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-AUTH-011

Test Group:
Refresh Token

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/refresh

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Pre-condition:
Refresh token/session expiry trước now.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/refresh.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "refreshToken": "<EXPIRED_REFRESH_TOKEN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401/400; không rotate token; session không được phục hồi.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-011-request-response.png; MF01-AUTH-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-AUTH-012

Test Group:
Profile / Privacy

Requirement/Function:
UC-04

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/auth/profile

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
JWT chữ ký/thời hạn hợp lệ nhưng sid trỏ session revoked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/auth/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401 SESSION_REVOKED; không trả profile.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-AUTH-012-request-response.png; MF01-AUTH-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-04; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-ACCESS-001

Test Group:
Profile / Privacy

Requirement/Function:
UC-08

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/auth/profile

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Không có Authorization header.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/auth/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401; không trả dữ liệu và không tạo PROFILE_VIEWED audit.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-001-request-response.png; MF01-ACCESS-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-08; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-002

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/sessions

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Không có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/sessions.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401; không trả session metadata.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-002-request-response.png; MF01-ACCESS-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-003

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/consent/grants

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Không có token.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/consent/grants.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401; không trả consent.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-003-request-response.png; MF01-ACCESS-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-ACCESS-004

Test Group:
Session Management

Requirement/Function:
UC-13

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/sessions/{{other_users_session_id}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- OTHER_USERS_SESSION_ID

Request Body:
None

Pre-condition:
User A token; path session thuộc user B.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/sessions/<OTHER_USERS_SESSION_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
400

Expected Response:
- HTTP 400/403; session người khác không đổi; không ghi SESSION_REVOKED audit.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-004-request-response.png; MF01-ACCESS-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-13; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-005

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/consent/grants/{{other_users_consent_id}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- OTHER_USERS_CONSENT_ID

Request Body:
None

Pre-condition:
User A token; consent thuộc user B.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/consent/grants/<OTHER_USERS_CONSENT_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
404

Expected Response:
- HTTP 404; không tiết lộ grant tồn tại; consent người khác không đổi.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-005-request-response.png; MF01-ACCESS-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-ACCESS-006

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
PUT

URL:
{{baseUrl}}/api/v1/notifications/{{other_users_notification_id}}/read

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- OTHER_USERS_NOTIFICATION_ID

Request Body:
```json
{}
```

Pre-condition:
User A token; notification thuộc user B.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PUT /api/v1/notifications/<OTHER_USERS_NOTIFICATION_ID>/read.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
404

Expected Response:
- HTTP 404; notification user khác không đổi; không audit read.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-006-request-response.png; MF01-ACCESS-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-007

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
Token role MOTHER.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403; không trả user list.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-007-request-response.png; MF01-ACCESS-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-ACCESS-008

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{moderatorToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
Token role MODERATOR.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403; không trả audit rows và không meta-audit.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-008-request-response.png; MF01-ACCESS-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-ACCESS-009

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{operationsToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
Token role OPERATIONS.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403; security events không trả; quyền GET audit-logs riêng không mở rộng sang incident endpoint.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-009-request-response.png; MF01-ACCESS-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-ACCESS-010

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{caller_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- CALLER_USER_ID

Request Body:
```json
{
  "locked": true,
  "reason": "self target"
}
```

Pre-condition:
SYSTEM_ADMIN token; path userId bằng JWT subject.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<CALLER_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "locked": true,
  "reason": "self target"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 IAM-114-004; caller không bị thay đổi; session không revoke; không audit mutation.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-010-request-response.png; MF01-ACCESS-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-011

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminRoleController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminRoleServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{caller_user_id}}/role

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- CALLER_USER_ID

Request Body:
```json
{
  "newRole": "CONTENT_ADMIN",
  "reason": "self target"
}
```

Pre-condition:
SYSTEM_ADMIN token; path userId bằng JWT subject.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<CALLER_USER_ID>/role.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "newRole": "CONTENT_ADMIN",
  "reason": "self target"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 IAM-116-004; role caller không đổi; không audit role update.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-011-request-response.png; MF01-ACCESS-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminRoleController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminRoleServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-012

Test Group:
Logout / Account Lifecycle

Requirement/Function:
UC-14

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/auth/deactivate

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "self deactivate"
}
```

Pre-condition:
SYSTEM_ADMIN token và password đúng.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/auth/deactivate.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "confirmPassword": "AdminPass1!",
  "reason": "self deactivate"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403; admin account/session không đổi.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-012-request-response.png; MF01-ACCESS-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-14; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-ACCESS-013

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/users?page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
JWT phát hành khi user là SYSTEM_ADMIN; DB role đã đổi sang MOTHER trước request.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/users?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
403

Expected Response:
- HTTP 403 nếu database role không còn SYSTEM_ADMIN; JwtAuthenticationFilter dùng current database authority, không dùng stale token role.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-ACCESS-013-request-response.png; MF01-ACCESS-013-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-INT-001

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/GmailEmailService.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "SMTP Failure",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

Pre-condition:
SMTP stub ném RuntimeException; email mới; PostgreSQL test.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "SMTP Failure",
  "email": "smtp-fail@example.test",
  "password": "SafePass1!",
  "role": "MOTHER"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Request trả lỗi external service; transaction không để user/OTP/audit partial; retry không gặp duplicate do lần lỗi.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-001-request-response.png; MF01-INT-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/GmailEmailService.java.

---

Test Case ID:
MF01-INT-002

Test Group:
Register / Identity

Requirement/Function:
UC-01

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/MockSmsService.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/register

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "name": "SMS Failure",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

Pre-condition:
SMS stub ném RuntimeException; phone mới.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/register.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "name": "SMS Failure",
  "phone": "0912345680",
  "password": "SafePass1!",
  "role": "FAMILY"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Request trả lỗi external service; không còn user/OTP/audit partial; retry an toàn.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-002-request-response.png; MF01-INT-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/MockSmsService.java.

---

Test Case ID:
MF01-INT-003

Test Group:
Register / Identity

Requirement/Function:
UC-01/UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/federation/FirebaseAdminTokenVerifier.java. Source-only federated extension

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/federated

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "idToken": "{{firebaseIdToken}}",
  "deviceInfo": "Web test"
}
```

Pre-condition:
Firebase gateway stub timeout; DB sạch provider subject.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/federated.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "idToken": "<TOKEN_CAUSING_FIREBASE_TIMEOUT>",
  "deviceInfo": "Web test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
503

Expected Response:
- HTTP 503; không tạo/link user identity, user hoặc session; response không lộ provider internals.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-003-request-response.png; MF01-INT-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-01/UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/FederatedAuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/federation/FirebaseAdminTokenVerifier.java. Source-only federated extension.

---

Test Case ID:
MF01-INT-004

Test Group:
OTP Verification

Requirement/Function:
UC-02

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/verify-otp

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

Pre-condition:
Một OTP REGISTER hợp lệ; gửi hai request đồng thời; PostgreSQL hỗ trợ locking/unique constraints.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/verify-otp.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "race@example.test",
  "otp": "<VALID_OTP>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Một request thành công; OTP consumed một lần; user active một lần; không duplicate session/audit completion.

Database Verification:
- Verify `users`, `otp_verifications`, `audit_events`; no raw OTP is stored.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-004-request-response.png; MF01-INT-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-02; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java.

---

Test Case ID:
MF01-INT-005

Test Group:
Refresh Token

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/repository/RefreshTokenRepository.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/refresh

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "refreshToken": "{{refreshToken}}"
}
```

Pre-condition:
Một session/token ACTIVE; gửi hai refresh đồng thời.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/refresh.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "refreshToken": "<ONE_VALID_REFRESH_TOKEN>"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Tối đa một request thành công; token cũ revoke; session trỏ duy nhất token hash mới; request thua bị từ chối.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-005-request-response.png; MF01-INT-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/repository/RefreshTokenRepository.java.

---

Test Case ID:
MF01-INT-006

Test Group:
Login

Requirement/Function:
UC-03/UC-04

Source:
05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Mobile app trỏ API test; account ACTIVE; FCM stub; không dùng token thật trong sheet.

Test Procedure:
1. Mở Mobile Login.
2. Nhập email/password hợp lệ và nhấn Login.
3. Kiểm tra AuthState nhận access/refresh/userId/role và FCM registration được kích hoạt.
4. Chọn Logout, xác nhận.
5. Kiểm tra API logout và local auth state.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Login response đầy đủ được lưu atomically; logout backend revoke session; local token/state bị xóa; user trở về auth landing.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-INT-006-request-response.png; MF01-INT-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03/UC-04; Source: 05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/login_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart; 05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java.

---

Test Case ID:
MF01-INT-007

Test Group:
Login

Requirement/Function:
UC-03/UC-08

Source:
05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeWebApp/src/shared/auth/roleRoutes.ts; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java

HTTP Method:
N/A — no implemented API request

URL:
N/A

Headers:
Accept: application/json
N/A

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Web origin allowlisted; account ACTIVE; API test.

Test Procedure:
1. Mở Web Login.
2. Login bằng account role test.
3. Kiểm tra auth store nhận token/user.
4. Kiểm tra role route và GET profile.
5. Refresh browser và kiểm tra session handling.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Role route đúng current role; profile thuộc đúng user; không điều hướng vào workspace trái role; lỗi backend được hiển thị an toàn.

Database Verification:
- N/A for UI-only observation; verify no unexpected database mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- frontend/mobile screen
- browser/device console if an error occurs
- Suggested: MF01-INT-007-request-response.png; MF01-INT-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03/UC-08; Source: 05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts; 05_Development/CareBridgeWebApp/src/features/auth/pages/LoginPage.tsx; 05_Development/CareBridgeWebApp/src/features/auth/pages/OtpPage.tsx; 05_Development/CareBridgeWebApp/src/shared/auth/roleRoutes.ts; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java.

---

Test Case ID:
MF01-INT-008

Test Group:
Notification Preference / Notification

Requirement/Function:
UC-11/UC-12

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/notifications/send

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "recipientUserId": "{{userId}}",
  "type": "REMINDER",
  "title": "Reminder",
  "body": "Test reminder"
}
```

Pre-condition:
SYSTEM_ADMIN token; recipient có active token; FCM stub ném exception.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/notifications/send.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "recipientUserId": "<USER_ID>",
  "type": "REMINDER",
  "title": "Reminder",
  "body": "Test reminder"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200 theo service contract; notification_record FAILED/failedAt/attemptCount đúng; audit NOTIFICATION_FAILED; không báo delivered giả.

Database Verification:
- Verify caller-owned preference/notification/device-token rows and audit event where applicable.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-008-request-response.png; MF01-INT-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-11/UC-12; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java.

---

Test Case ID:
MF01-INT-009

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/policy/ConsentCheckPolicy.java

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/consent/grants/{{active_consent_id}}

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
- ACTIVE_CONSENT_ID

Request Body:
None

Pre-condition:
Owner token; active consent HEALTH_RECORD/VIEW; có protected record và recipient read fixture.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/consent/grants/<ACTIVE_CONSENT_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Sau commit revocation, protected data read tiếp theo bị consent policy từ chối; dữ liệu owner còn nguyên; audit và dependent cleanup nhất quán.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-009-request-response.png; MF01-INT-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/policy/ConsentCheckPolicy.java.

---

Test Case ID:
MF01-INT-010

Test Group:
RBAC / Admin

Requirement/Function:
UC-17

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/users/{{target_user_id}}/status

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- TARGET_USER_ID

Request Body:
```json
{
  "locked": true,
  "reason": "Audit failure injection"
}
```

Pre-condition:
SYSTEM_ADMIN token; audit repository fault-injected trong transaction; target unlocked.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/users/<TARGET_USER_ID>/status.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "locked": true,
  "reason": "Audit failure injection"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Request lỗi; target enabled/locked/session state quay về trước mutation; không có partial audit.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-010-request-response.png; MF01-INT-010-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-17; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/service/impl/AdminUserServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-INT-011

Test Group:
Logout / Account Lifecycle

Requirement/Function:
UC-14

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql

HTTP Method:
DELETE

URL:
{{baseUrl}}/api/v1/auth/deactivate

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "Integration revocation check"
}
```

Pre-condition:
User không phải SYSTEM_ADMIN; có nhiều sessions/refresh tokens/device tokens.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send DELETE /api/v1/auth/deactivate.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "confirmPassword": "SafePass1!",
  "reason": "Integration revocation check"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Sau commit, access token cũ bị 403/401, refresh bị từ chối, device tokens inactive; account data vẫn giữ cho retention; audit có timestamp/reason.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- Inspect Firebase/FCM adapter log and persisted delivery status; distinguish disabled/stub/failure behavior.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-011-request-response.png; MF01-INT-011-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-14; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/service/impl/NotificationServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql.

---

Test Case ID:
MF01-INT-012

Test Group:
Audit / Privacy

Requirement/Function:
UC-03/UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql. Source-only operational integration supporting account availability

HTTP Method:
GET

URL:
{{baseUrl}}/actuator/health/readiness

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Có thể điều khiển PostgreSQL; ứng dụng chạy với health readiness enabled.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /actuator/health/readiness.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Khi DB down trả service unavailable/readiness DOWN; khi DB up trả ready; các actuator khác vẫn denied; không lộ credential/stack trace.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-012-request-response.png; MF01-INT-012-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03/UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql. Source-only operational integration supporting account availability.

---

Test Case ID:
MF01-INT-013

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java. Source-only Web integration

HTTP Method:
OPTIONS

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
Chạy API với carebridge.cors.allowed-origins test; gửi preflight từ allowed và disallowed origins.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send OPTIONS /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Allowlisted origin nhận đúng CORS headers/credentials; wildcard hoặc origin có path/query không được cấu hình; authorization không bị nới rộng.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-INT-013-request-response.png; MF01-INT-013-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java. Source-only Web integration.

---

Test Case ID:
MF01-PRIVACY-001

Test Group:
Forgot / Password

Requirement/Function:
UC-05

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ForgotPasswordServiceImpl.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/forgot-password

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "contact": "{{email}}"
}
```

Pre-condition:
Contact không tồn tại; rate limit chưa vượt.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/forgot-password.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "contact": "unknown@example.test"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200 với cùng neutral message/shape như contact tồn tại; không tạo reset token hoặc audit cho unknown account.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-001-request-response.png; MF01-PRIVACY-001-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-05; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/ForgotPasswordServiceImpl.java.

---

Test Case ID:
MF01-PRIVACY-002

Test Group:
Profile / Privacy

Requirement/Function:
UC-08

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/mapper/UserMapper.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/auth/profile

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User ACTIVE có token; DB chứa password/session/OTP fixtures để kiểm tra không leak.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/auth/profile.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; response JSON không có passwordHash, refreshToken, refreshTokenHash, OTP/codeHash hoặc security lock internals không được phép.

Database Verification:
- Verify only the authenticated user profile/settings changed; verify privacy/audit event.

External Integration Verification:
- Phone flow: inspect `[MOCK SMS]` backend log. Email flow: inspect configured SMTP mailbox; do not expose raw OTP/token in evidence.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-002-request-response.png; MF01-PRIVACY-002-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-08; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/mapper/UserMapper.java.

---

Test Case ID:
MF01-PRIVACY-003

Test Group:
Audit / Privacy

Requirement/Function:
UC-10

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/service/CommunityAuthorDisplayResolver.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/mapper/CommunityFeedMapper.java. Source-based privacy enforcement mapped to UC-10

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/community/feed?page=0&size=20

Headers:
Accept: application/json
Authorization: None

Query Parameters:
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
Authenticated user; có post non-anonymous của user có private data fixtures.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/community/feed?page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
N/A (UI/integration observation)

Expected Response:
- Community author projection chỉ dùng displayName/anonymous label; không trả phone, email, maternal records, baby records hoặc family identifiers.

Database Verification:
- Verify expected rows only; no cross-user mutation.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-003-request-response.png; MF01-PRIVACY-003-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-10; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/service/CommunityAuthorDisplayResolver.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/mapper/CommunityFeedMapper.java. Source-based privacy enforcement mapped to UC-10.

---

Test Case ID:
MF01-PRIVACY-004

Test Group:
Consent

Requirement/Function:
UC-15

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/constants/ConsentConstants.java

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/consent/grants

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "dataType": "SENSITIVE_DATA",
  "purpose": "VIEW",
  "recipient": "expert@example.test",
  "scope": "selected-records"
}
```

Pre-condition:
User ACTIVE có token; recipient fixture.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/consent/grants.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "dataType": "SENSITIVE_DATA",
  "purpose": "VIEW",
  "recipient": "expert@example.test",
  "scope": "selected-records"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; expiryAt dùng DEFAULT_EXPIRY_DAYS, không tạo consent vô hạn; owner là JWT user; audit không chứa raw health data.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-004-request-response.png; MF01-PRIVACY-004-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-15; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/common/constants/ConsentConstants.java.

---

Test Case ID:
MF01-PRIVACY-005

Test Group:
Consent

Requirement/Function:
UC-16

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/consent/grants

Headers:
Accept: application/json
Authorization: Bearer {{accessToken}}

Query Parameters:
None

Path Parameters:
None

Request Body:
None

Pre-condition:
User A token; DB có grants của user A và B.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/consent/grants.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; mọi grant có owner user JWT; active và past grants đúng; không trả grant user khác.

Database Verification:
- Verify the caller-owned consent row, status/timestamps, and consent audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-005-request-response.png; MF01-PRIVACY-005-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-16; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/service/impl/ConsentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/dto/request/GrantConsentRequest.java.

---

Test Case ID:
MF01-PRIVACY-006

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/audit-logs?action=VIEW_HEALTH_RECORD&page=0&size=20

Headers:
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
- action=VIEW_HEALTH_RECORD
- page=0
- size=20

Path Parameters:
None

Request Body:
None

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token; có VIEW_HEALTH_RECORD audit fixtures.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/audit-logs?action=VIEW_HEALTH_RECORD&page=0&size=20.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; audit metadata tối thiểu theo schema; không trả raw file content/clinical record body; meta-audit chỉ lưu filter snapshot.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-006-request-response.png; MF01-PRIVACY-006-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java.

---

Test Case ID:
MF01-PRIVACY-007

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java

HTTP Method:
GET

URL:
{{baseUrl}}/api/v1/admin/security-events/{{event_id}}/notes

Headers:
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
- EVENT_ID

Request Body:
None

Pre-condition:
Không có token; event/notes tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send GET /api/v1/admin/security-events/<EVENT_ID>/notes.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
No request body.
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
401

Expected Response:
- HTTP 401; không trả noteText hoặc authorId.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-007-request-response.png; MF01-PRIVACY-007-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/SecurityIncidentController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/SecurityIncidentServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/config/SecurityConfig.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtAuthenticationFilter.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/jwt/JwtTokenProvider.java.

---

Test Case ID:
MF01-PRIVACY-008

Test Group:
Login

Requirement/Function:
UC-03

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql

HTTP Method:
POST

URL:
{{baseUrl}}/api/v1/auth/login

Headers:
Content-Type: application/json
Accept: application/json
Authorization: None

Query Parameters:
None

Path Parameters:
None

Request Body:
```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

Pre-condition:
User ACTIVE; PostgreSQL query access cho QA.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send POST /api/v1/auth/login.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "email": "hashcheck@example.test",
  "password": "SafePass1!"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
200

Expected Response:
- HTTP 200; client nhận raw refresh token; auth_sessions chỉ có SHA-256 hash; log/audit không chứa raw access/refresh token.

Database Verification:
- Verify `auth_sessions`, refresh-token hash/rotation/revocation, and related audit event.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-008-request-response.png; MF01-PRIVACY-008-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-03; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/service/impl/SessionServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql.

---

Test Case ID:
MF01-PRIVACY-009

Test Group:
Audit / Privacy

Requirement/Function:
UC-18

Source:
05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql

HTTP Method:
PATCH

URL:
{{baseUrl}}/api/v1/admin/audit-logs/{{audit_id}}

Headers:
Content-Type: application/json
Accept: application/json
Authorization: Bearer {{systemAdminToken}}

Query Parameters:
None

Path Parameters:
- AUDIT_ID

Request Body:
```json
{
  "action": "LOGIN"
}
```

Pre-condition:
SYSTEM_ADMIN/OPERATIONS token; audit row tồn tại.

Test Procedure:
1. Start the CareBridge API and Supabase test environment.
2. Prepare the actor and data stated in Pre-conditions.
3. Send PATCH /api/v1/admin/audit-logs/<AUDIT_ID>.
Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.
Request body: {
  "action": "LOGIN"
}
4. Capture the HTTP status, response body and error code/message.
5. Verify the affected database, session, audit or external-service state stated in Expected Results.

Expected HTTP Status:
405

Expected Response:
- HTTP 405/404; không có mutation endpoint; audit row giữ nguyên; normal operator không thể sửa/xóa.

Database Verification:
- Verify target state and append-only audit/security-event records; forbidden calls must not mutate data.

External Integration Verification:
- None, unless the pre-condition explicitly configures an external stub/failure.

Evidence cần chụp:
- request URL/method/headers/body
- response status/body
- relevant Supabase row or audit log
- Suggested: MF01-PRIVACY-009-request-response.png; MF01-PRIVACY-009-database.png

Round 1:
Pending

Round 2:
Pending

Round 3:
Pending

Note:
UC-18; Source: 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java; 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/service/impl/AuditServiceImpl.java; 05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql.

## PHẦN E — JSON BODY

DTOs read: `RegisterRequest`, `LoginRequest`, `VerifyOtpRequest`, `ResendOtpRequest`, `RefreshTokenRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `ChangePasswordRequest`, `DeactivateAccountRequest`, auth/profile update DTOs, `GrantConsentRequest`, notification preference/device/send DTOs, admin status/role/staff DTOs, security-event review/note DTOs. Bodies appear in each case above and contain only DTO fields.

Valid enums read from source:

- Role: `MOTHER`, `FAMILY`, `EXPERT`, `MODERATOR`, `CONTENT_ADMIN`, `SYSTEM_ADMIN`, `OPERATIONS`.
- ConsentDataType: `HEALTH_RECORD`, `LOCATION`, `FAMILY_DATA`, `COMMUNITY_POST`, `SENSITIVE_DATA`, `RAG_CONTEXT`, `EXPERT_SHARED_DATA`, `MOTHER_BASELINE`, `SENSOR_DATA`.
- ConsentPurpose: `VIEW`, `CREATE`, `UPDATE`, `SHARE`, `DELETE`, `PERSONALIZE`.
- ProfileVisibility: `PUBLIC`, `FRIENDS_ONLY`, `PRIVATE`.
- NotificationType: `REMINDER`, `COMMUNITY_REPLY`, `CONSULTATION`, `EMERGENCY`, `MESSAGE`, `GROUP_INVITE`, `CONTENT_REVIEW`.
- DevicePlatform: `ANDROID`, `IOS`, `WEB`.

## PHẦN F — POSTMAN COLLECTION

Collection: `artifacts/CareBridge_MF01_Integration_Tests_verified_20260809.postman_collection.json`. It contains 12 source-backed folders and 134 HTTP requests; seven UI/Report-only observations are documented above but deliberately have no invented request.

## PHẦN G — POSTMAN VARIABLE SCRIPT

```javascript
const body = pm.response.json();
const data = body.data || body;
if (data.accessToken) pm.environment.set("accessToken", data.accessToken);
if (data.refreshToken) pm.environment.set("refreshToken", data.refreshToken);
if (data.user && data.user.id) pm.environment.set("userId", data.user.id);
if (data.id && pm.request.url.toString().includes("/consent/grants")) pm.environment.set("consentId", data.id);
```

Register returns an OTP challenge, not tokens. Phone OTP/reset values must be copied from the mock SMS log into `{{otp}}`/`{{resetToken}}`.

## PHẦN H — TEST EXECUTION

Actual calls: **46 HTTP calls / 43 distinct scenarios**. These are smoke/integration observations, not Round 1/2/3 evidence sign-off.

| Scenario | Actual status | Actual response summary |
|---|---:|---|
| Backend readiness | 200 | Supabase DB and readinessState UP |
| Register phone | 201 | Pending account created; mock SMS invoked |
| Duplicate register | 409 | Account already exists |
| Login before verification | 403 | Account is disabled |
| Malformed OTP | 400 | DTO validation rejected |
| Wrong OTP | 400 | Invalid OTP |
| Verify OTP | 200 | Tokens and user returned |
| Anonymous profile | 401 | Rejected |
| Auth profile GET/PUT | 200 | Read and updated |
| Extended profile GET/PATCH | 200 | Read and updated |
| Privacy GET/PUT | 200 | Defaults read and PRIVATE saved |
| Notification preference GET/PUT | 200 | Read and updated |
| Notification empty list | 200 | Paged empty response |
| Session list/paged | 200 | Caller sessions only |
| Consent grant/list/revoke | 200 | Grant ID returned and revoked |
| MOTHER -> admin users | 403 | Insufficient permissions |
| MOTHER -> audit logs | 403 | Forbidden |
| MOTHER -> security events | 403 | Insufficient permissions |
| Refresh rotation | 200 | New token pair returned |
| Replay old refresh | 401 | No active session for token |
| Logout | 200 | Session/token revoked |
| Refresh after logout | 401 | Token blacklisted |
| Valid login | 200 | Token pair returned |
| Wrong password | 401 | Neutral invalid credentials |
| Forgot known/unknown | 200 | Same neutral shape |
| Reset mismatch | 400 | Passwords do not match |
| Reset valid | 200 | Password reset |
| Old password after reset | 401 | Rejected |
| New password login | 200 | Accepted |
| Change wrong current password | 400 | Rejected |
| Change password | 200 | Requires login again |
| Deactivate wrong password | 401 | Rejected |
| Deactivate valid | 200 | Account deactivated |
| Login deactivated | 403 | Account disabled |

Observed issues: phone canonicalization changed `09...` to `+849...` in the mock log; the initial OTP harness did not account for that. A source-level magic OTP value `111111` is accepted by registration completion and should be treated as a security defect candidate; the actual happy-path run used the generated OTP instead. Gmail was not invoked because the test used phone delivery.

## PHẦN I — EVIDENCE CHECKLIST

For every executed case capture Postman method/URL, relevant headers with tokens masked, DTO body, status, and response. For mutations also capture the caller-owned Supabase record and corresponding audit row. Capture the mock SMS log for OTP/reset with the secret redacted. Capture 401/403 bodies for authorization cases. Suggested names follow `<CASE-ID>-request-response.png`, `<CASE-ID>-database.png`, `<CASE-ID>-audit.png`, and `<CASE-ID>-sms-log.png`. No screenshot was fabricated.

## PHẦN J — KẾT QUẢ CUỐI CÙNG

1. Total: **141 designed cases**; **134 Postman requests**; **7 UI/Report-only observations**.
2. Counts by operation and feature are in Part C.
3. Verified endpoints: auth register/login/verify/resend/refresh/logout/profile/password/deactivate; extended profile; privacy; notification preferences/list; sessions; consent; admin RBAC denial; audit/security denial; readiness.
4. DTO and enum inventory is in Part E.
5. JSON bodies are embedded per test case and in the Postman collection.
6. Preconditions are embedded per case.
7. Order: readiness → register → OTP → login → refresh → profile/privacy/preferences/session → consent → RBAC/audit → logout → recovery/password lifecycle → deactivate.
8. Startup: `.\mvnw.cmd spring-boot:run` with the existing Supabase environment.
9. Base URL: `http://localhost:8080`.
10. Test data: unique phone role MOTHER; test phone masked as `092****914`; account was deactivated at cleanup. Secrets are omitted.
11. Not implemented: standalone community identity management endpoint; retention-based account delete-request workflow.
12. Report-3-only: UC-10 independent community identity; UC-14 deletion request.
13. Errors: startup profile mismatch noted in Part A; API/harness observations in Part H.
14. Evidence checklist: Part I and every detailed case.
15. Collection: `artifacts/CareBridge_MF01_Integration_Tests_verified_20260809.postman_collection.json`.

MF-02 through MF-10 were not processed.
