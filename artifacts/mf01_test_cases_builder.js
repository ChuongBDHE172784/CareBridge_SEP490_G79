const fs = require('fs');

const ROOT = '05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend';
const WEB = '05_Development/CareBridgeWebApp/src';
const MOBILE = '05_Development/CareBridgeMobileApp/lib';

const S = {
  auth: `${ROOT}/security/controller/AuthController.java; ${ROOT}/security/service/impl/AuthServiceImpl.java`,
  authPolicy: `${ROOT}/security/policy/AuthenticationPolicy.java; ${ROOT}/security/policy/PasswordComplexityPolicy.java; ${ROOT}/security/policy/RateLimitPolicy.java`,
  jwt: `${ROOT}/security/config/SecurityConfig.java; ${ROOT}/security/jwt/JwtAuthenticationFilter.java; ${ROOT}/security/jwt/JwtTokenProvider.java`,
  profile: `${ROOT}/profile/controller/ProfileController.java; ${ROOT}/profile/service/impl/ProfileServiceImpl.java; ${ROOT}/profile/dto/UpdateProfileRequest.java`,
  privacy: `${ROOT}/privacy/controller/PrivacySettingsController.java; ${ROOT}/privacy/service/impl/PrivacySettingsServiceImpl.java; ${ROOT}/privacy/dto/UpdatePrivacySettingsRequest.java`,
  consent: `${ROOT}/consent/controller/ConsentController.java; ${ROOT}/consent/service/impl/ConsentServiceImpl.java; ${ROOT}/consent/dto/request/GrantConsentRequest.java`,
  session: `${ROOT}/identity/controller/SessionController.java; ${ROOT}/identity/service/impl/SessionServiceImpl.java`,
  notify: `${ROOT}/notification/controller/NotificationController.java; ${ROOT}/notification/service/impl/NotificationServiceImpl.java`,
  notifyPref: `${ROOT}/notification/controller/NotificationPreferenceController.java; ${ROOT}/notification/service/impl/NotificationPreferenceServiceImpl.java; ${ROOT}/notification/dto/UpdateNotificationPreferencesRequest.java`,
  admin: `${ROOT}/identity/admin/controller/AdminUserController.java; ${ROOT}/identity/admin/service/impl/AdminUserServiceImpl.java`,
  role: `${ROOT}/identity/admin/controller/AdminRoleController.java; ${ROOT}/identity/admin/service/impl/AdminRoleServiceImpl.java`,
  staff: `${ROOT}/identity/admin/controller/AdminStaffController.java; ${ROOT}/identity/admin/service/impl/AdminStaffServiceImpl.java`,
  audit: `${ROOT}/audit/controller/AuditController.java; ${ROOT}/audit/service/impl/AuditServiceImpl.java`,
  db: '05_Development/CareBridgeAPI/src/main/resources/db/migration/V1__init_schema.sql',
  webAuth: `${WEB}/features/auth/services/authApi.ts; ${WEB}/features/auth/pages/LoginPage.tsx; ${WEB}/features/auth/pages/OtpPage.tsx`,
  mobileAuth: `${MOBILE}/features/auth/services/auth_service.dart; ${MOBILE}/features/auth/screens/login_screen.dart; ${MOBILE}/features/auth/screens/register_screen.dart; ${MOBILE}/features/auth/screens/otp_verification_screen.dart`,
  mobileSession: `${MOBILE}/features/session/services/session_service.dart; ${MOBILE}/features/session/screens/login_sessions_screen.dart`,
  mobileNotify: `${MOBILE}/features/notification/services/notification_service.dart; ${MOBILE}/features/notification/screens/notification_center_screen.dart`,
};

const cases = [];
const seq = {};
function id(group) {
  seq[group] = (seq[group] || 0) + 1;
  return `MF01-${group}-${String(seq[group]).padStart(3, '0')}`;
}
function note(uc, source, extra) {
  return `${uc}; Source: ${source}.${extra ? ` ${extra}` : ''}`;
}
function api(group, uc, desc, method, path, body, expected, pre, source, extraNote = '') {
  const request = body === undefined ? 'No request body.' : `Request body: ${JSON.stringify(body, null, 2)}`;
  const procedure = [
    `1. Start the CareBridge API and PostgreSQL test environment.`,
    `2. Prepare the actor and data stated in Pre-conditions.`,
    `3. Send ${method} ${path}.`,
    `Headers: Authorization: Bearer <ACCESS_TOKEN_FROM_PRECONDITION> when required; Content-Type: application/json.`,
    request,
    `4. Capture the HTTP status, response body and error code/message.`,
    `5. Verify the affected database, session, audit or external-service state stated in Expected Results.`,
  ].join('\n');
  cases.push({ id: id(group), group, description: desc, procedure, expected, preconditions: pre, note: note(uc, source, extraNote) });
}
function ui(group, uc, desc, procedure, expected, pre, source, extraNote = '') {
  cases.push({ id: id(group), group, description: desc, procedure, expected, preconditions: pre, note: note(uc, source, extraNote) });
}
function requirementOnly(group, uc, desc, procedure, expected, pre) {
  cases.push({
    id: id(group), group, description: desc, procedure, expected, preconditions: pre,
    note: `${uc}; Requirement exists in Report 3, but implementation was not found in current source.`,
  });
}

// VIEW
ui('VIEW', 'UC-01/UC-03', 'Hiển thị màn hình đăng nhập và đăng ký trên Mobile App.',
  '1. Cài bản build Mobile App kết nối môi trường test.\n2. Mở ứng dụng khi chưa đăng nhập.\n3. Mở Login, sau đó chuyển sang Register.\n4. Kiểm tra các trường và nút hiển thị.\n5. Không gửi dữ liệu.',
  'Login hiển thị đúng trường email/phone và password; Register hiển thị name, email/phone, password và role hợp lệ; không hiển thị trường không tồn tại trong request model.',
  'Thiết bị/emulator có mạng; backend URL test được cấu hình; chưa có token.', `${S.mobileAuth}; ${S.auth}`);
ui('VIEW', 'UC-01/UC-03', 'Hiển thị màn hình đăng nhập/đăng ký chuyên gia trên Web Portal.',
  '1. Mở Web Portal ở origin được CORS cho phép.\n2. Truy cập Login.\n3. Mở Expert Register.\n4. Kiểm tra trường, nhãn lỗi và nút gửi.\n5. Không gửi dữ liệu.',
  'Trang hiển thị đúng contract của LoginRequest/RegisterRequest; role đăng ký chuyên gia được gửi là EXPERT; không lộ token hay dữ liệu người dùng khác.',
  'Web dev server và API test hoạt động; trình duyệt chưa đăng nhập.', `${S.webAuth}; ${S.auth}`);
ui('VIEW', 'UC-02', 'Hiển thị màn hình nhập OTP sau khi đăng ký thành công.',
  '1. Đăng ký một tài khoản mới hợp lệ.\n2. Điều hướng đến OTP screen/page.\n3. Kiểm tra identifier đã được truyền đúng.\n4. Kiểm tra ô OTP 6 chữ số và Resend OTP.\n5. Không nhập OTP.',
  'OTP UI hiển thị đúng identifier đã đăng ký, chỉ nhận mã 6 chữ số và có thao tác resend; chưa tạo session trước khi OTP hợp lệ.',
  'Có phản hồi đăng ký 201 và OTP challenge chưa dùng.', `${S.mobileAuth}; ${S.webAuth}; ${S.auth}`);
api('VIEW', 'UC-08', 'Xem hồ sơ tài khoản riêng tư bằng access token hợp lệ.', 'GET', '/api/v1/auth/profile', undefined,
  'HTTP 200; trả id, name, email/phone, role và trạng thái được phép; không trả passwordHash, refresh token hoặc OTP; tạo audit PROFILE_VIEWED.',
  'User ACTIVE có session hợp lệ và access token; PostgreSQL có user tương ứng.', `${S.auth}; ${ROOT}/security/mapper/UserMapper.java; ${S.audit}`);
api('VIEW', 'UC-08', 'Xem hồ sơ mở rộng của chính user.', 'GET', '/api/v1/profile', undefined,
  'HTTP 200; chỉ trả profile gắn với userId trong JWT, gồm displayName, avatarUrl, phoneNumber, dateOfBirth, area; không nhận userId từ client.',
  'User ACTIVE có UserProfile và token hợp lệ.', S.profile, 'Source extension mapped to UC-08.');
api('VIEW', 'UC-10', 'Xem privacy settings dùng cho khả năng hiển thị hồ sơ cộng đồng.', 'GET', '/api/v1/privacy-settings/me', undefined,
  'HTTP 200; trả profileVisibility, locationSharingEnabled, analyticsConsent, dataExportOptOut của chính user; ghi audit PRIVACY_SETTINGS_ACCESSED.',
  'User ACTIVE có token hợp lệ; privacy settings có thể chưa tồn tại để kiểm tra default creation.', S.privacy, 'Partial implementation of community identity/privacy settings.');
api('VIEW', 'UC-11', 'Xem notification preferences của chính user.', 'GET', '/api/v1/users/me/notification-preferences', undefined,
  'HTTP 200; trả danh sách preference theo NotificationType và appointmentReminderDefaults; userId lấy từ JWT; audit NOTIFICATION_PREFERENCES_VIEWED được ghi.',
  'User ACTIVE có token hợp lệ; có hoặc chưa có preference rows.', S.notifyPref);
api('VIEW', 'UC-12', 'Xem danh sách notification có dữ liệu của chính user.', 'GET', '/api/v1/notifications/me?page=0&size=20', undefined,
  'HTTP 200; chỉ trả notification visible của user trong JWT, mới nhất trước; response có read state và metadata đúng contract.',
  'User ACTIVE có token; có ít nhất 2 notification_records thuộc user và 1 record thuộc user khác.', S.notify);
api('VIEW', 'UC-12', 'Xem empty state khi user chưa có notification.', 'GET', '/api/v1/notifications/me?page=0&size=20', undefined,
  'HTTP 200; content rỗng, totalElements=0; UI hiển thị empty state, không hiển thị dữ liệu user khác.',
  'User ACTIVE có token và không có notification_records visible.', `${S.notify}; ${S.mobileNotify}`);
api('VIEW', 'UC-13', 'Xem danh sách các phiên đăng nhập đang hoạt động của chính user.', 'GET', '/api/v1/sessions', undefined,
  'HTTP 200; trả sessionId, deviceName, browser, ipAddress, lastActivityAt, status; đánh dấu đúng session hiện tại theo JWT sid; không trả refreshTokenHash.',
  'User ACTIVE có ít nhất hai auth_sessions và token thuộc một session.', S.session);
api('VIEW', 'UC-17', 'System Admin xem chi tiết một tài khoản.', 'GET', '/api/v1/admin/users/<USER_ID_FROM_PRECONDITION>', undefined,
  'HTTP 200; trả summary của đúng user; không lộ passwordHash hoặc raw token.',
  'SYSTEM_ADMIN token hợp lệ; target user tồn tại.', S.admin);
api('VIEW', 'UC-17', 'System Admin xem session history của một tài khoản.', 'GET', '/api/v1/admin/users/<USER_ID_FROM_PRECONDITION>/sessions?page=0&size=20', undefined,
  'HTTP 200; chỉ trả sessions của target user theo trang, không trả refresh token/hash.',
  'SYSTEM_ADMIN token; target user có nhiều sessions.', S.admin);
api('VIEW', 'UC-17', 'System Admin xem activity history của một tài khoản.', 'GET', '/api/v1/admin/users/<USER_ID_FROM_PRECONDITION>/activity?page=0&size=20', undefined,
  'HTTP 200; chỉ trả audit activity của target user theo trang và thời gian.',
  'SYSTEM_ADMIN token; target user có audit_events.', S.admin);
api('VIEW', 'UC-18', 'System Admin/Operations xem audit log được phép.', 'GET', '/api/v1/admin/audit-logs?page=0&size=20', undefined,
  'HTTP 200; audit rows sắp xếp giảm dần theo createdAt; mỗi lần xem tạo meta-audit VIEW_AUDIT_LOG mà không chép toàn bộ PII result vào payload.',
  'Token role SYSTEM_ADMIN hoặc OPERATIONS; có audit_events.', S.audit);

// ADD / CREATE
api('ADD', 'UC-01', 'Đăng ký tài khoản mới bằng email hợp lệ.', 'POST', '/api/v1/auth/register',
  { name: 'Nguyen An', email: 'mf01.email@example.test', password: 'SafePass1!', role: 'MOTHER' },
  'HTTP 201; users có một row PENDING_ACTIVATION/enabled=false; OTP hash và expiry được lưu; email OTP được gọi một lần; không tạo session/token trước verify.',
  'Email chưa tồn tại; PostgreSQL sạch cho identifier; SMTP stub hoạt động.', `${S.auth}; ${ROOT}/security/dto/request/RegisterRequest.java; ${S.db}`);
api('ADD', 'UC-01', 'Đăng ký tài khoản mới bằng số điện thoại Việt Nam hợp lệ.', 'POST', '/api/v1/auth/register',
  { name: 'Tran Binh', phone: '0912345678', password: 'SafePass1!', role: 'FAMILY' },
  'HTTP 201; phone được canonicalize; user chưa active; OTP hash được lưu và SMS adapter nhận đúng phone; chưa tạo token/session.',
  'Phone chưa tồn tại; SMS stub hoạt động.', `${S.auth}; ${ROOT}/security/dto/request/RegisterRequest.java`);
api('ADD', 'UC-01', 'Đăng ký tài khoản không chọn role để nhận trạng thái UNASSIGNED.', 'POST', '/api/v1/auth/register',
  { name: 'Le Chi', email: 'mf01.norole@example.test', password: 'SafePass1!' },
  'HTTP 201; user.role=null; audit OTP_SENT ghi role=UNASSIGNED; user có thể chọn self-service role sau khi xác thực.',
  'Email mới; SMTP stub hoạt động.', `${S.auth}; ${S.authPolicy}`);
api('ADD', 'UC-01/UC-03', 'Tạo CareBridge account mới qua Firebase federated authentication.', 'POST', '/api/v1/auth/federated',
  { idToken: '<VALID_FIREBASE_ID_TOKEN>', deviceInfo: 'CareBridge Flutter test' },
  'HTTP 201; tạo đúng một user_identity và CareBridge user/session; trả access/refresh token; không lưu raw Firebase ID token.',
  'Firebase emulator có identity chưa liên kết; DB chưa có provider subject/contact.', `${ROOT}/security/controller/AuthController.java; ${ROOT}/security/service/impl/FederatedAuthServiceImpl.java; ${ROOT}/security/dto/request/FederatedAuthRequest.java`, 'Source-only federated extension not explicitly separated in Report 3.');
api('ADD', 'UC-01/UC-08', 'Liên kết Google identity vào tài khoản đang đăng nhập.', 'POST', '/api/v1/auth/identities/google',
  { idToken: '<FRESH_GOOGLE_FIREBASE_ID_TOKEN>' },
  'HTTP 200; tạo một user_identity liên kết đúng user; không cấp session token mới; audit FEDERATED_IDENTITY_LINKED được ghi.',
  'CareBridge user ACTIVE có token; Google provider subject chưa liên kết tài khoản khác.', `${ROOT}/security/controller/AuthController.java; ${ROOT}/security/service/impl/FederatedAuthServiceImpl.java; ${ROOT}/security/dto/request/LinkGoogleIdentityRequest.java`, 'Source-only identity-link extension.');
api('ADD', 'UC-15', 'Cấp consent hợp lệ có dataType, purpose, recipient, scope và expiry.', 'POST', '/api/v1/consent/grants',
  { dataType: 'HEALTH_RECORD', purpose: 'SHARE', recipient: 'family@example.test', scope: 'pregnancy-summary', expiryDays: 30 },
  'HTTP 200; tạo consent grant thuộc user trong JWT, expiryAt=now+30d, status hợp lệ; audit CONSENT_GRANTED được ghi.',
  'User ACTIVE có token; recipient test hợp lệ theo luồng tích hợp.', S.consent);
api('ADD', 'UC-11/UC-12', 'Đăng ký FCM device token cho chính user.', 'POST', '/api/v1/notifications/device-token',
  { token: '<FCM_DEVICE_TOKEN_FROM_TEST_FIXTURE>', platform: 'ANDROID' },
  'HTTP 200; device token active thuộc user JWT; đăng ký lặp không tạo duplicate; token cũ thuộc user khác bị deactivate trước khi gán lại.',
  'User ACTIVE có token; Firebase/FCM stub; fixture device token.', `${S.notify}; ${ROOT}/notification/dto/RegisterDeviceTokenRequest.java`, 'Source extension supporting UC-11/UC-12.');
api('ADD', 'UC-17', 'System Admin tạo staff account MODERATOR.', 'POST', '/api/v1/admin/staff-accounts',
  { email: 'moderator.mf01@example.test', phone: '0912345679', name: 'Moderator QA', role: 'MODERATOR' },
  'HTTP 201; staff account tạo một lần với temp credential server-generated, không nhận password từ admin; email gửi một lần; audit STAFF_ACCOUNT_CREATED được ghi.',
  'SYSTEM_ADMIN token; email/phone mới; SMTP stub.', `${S.staff}; ${ROOT}/identity/admin/dto/request/CreateStaffAccountRequest.java`, 'Source extension within UC-17.');
api('ADD', 'UC-12', 'System Admin gửi notification hợp lệ đến user có device token.', 'POST', '/api/v1/notifications/send',
  { recipientUserId: '<USER_ID_FROM_PRECONDITION>', type: 'COMMUNITY_REPLY', title: 'New reply', body: 'A new reply is available.', referenceId: '<REFERENCE_ID_FROM_PRECONDITION>', referenceType: 'COMMUNITY_QUESTION' },
  'HTTP 200; FCM được gọi; notification_record trạng thái SENT/attemptCount=1; audit NOTIFICATION_SENT được ghi.',
  'SYSTEM_ADMIN token; recipient có active device token; FCM stub success.', `${S.notify}; ${ROOT}/notification/dto/SendNotificationRequest.java`, 'Source integration supporting UC-12.');
requirementOnly('ADD', 'UC-10', 'Tạo community identity tách biệt hoàn toàn với private care profile.',
  '1. Mở Community Identity settings theo Report 3.\n2. Nhập display name, avatar và visibility.\n3. Lưu.\n4. Kiểm tra public community projection và private profile storage.\n5. Kiểm tra audit.',
  'Tạo public community identity riêng, không dùng hoặc lộ maternal/baby/family private fields; có audit.',
  'User ACTIVE; môi trường test; chưa có community identity riêng.');

// UPDATE
api('UPDATE', 'UC-02', 'Xác thực OTP hợp lệ để kích hoạt tài khoản.', 'POST', '/api/v1/auth/verify-otp',
  { email: 'mf01.email@example.test', otp: '<VALID_6_DIGIT_OTP_FROM_STUB>' },
  'HTTP 200; OTP usedAt/verified cập nhật một lần; user ACTIVE/enabled=true; tạo đúng một session và token pair; audit OTP_VERIFIED và USER_REGISTRATION_COMPLETED.',
  'User pending activation và OTP REGISTER chưa hết hạn.', `${S.auth}; ${ROOT}/security/dto/request/VerifyOtpRequest.java`);
api('UPDATE', 'UC-02', 'Resend OTP hợp lệ và vô hiệu mã OTP trước đó.', 'POST', '/api/v1/auth/resend-otp',
  { email: 'mf01.pending@example.test' },
  'HTTP 200; OTP cũ usedAt được set; OTP mới giữ đúng purpose, expiry và 5 attempts; SMTP gọi một lần; audit OTP_RESENT.',
  'User pending có một OTP chưa dùng; cooldown chưa bị consume.', `${S.auth}; ${ROOT}/security/dto/request/ResendOtpRequest.java; ${S.authPolicy}`);
api('UPDATE', 'UC-09', 'Cập nhật name và avatarUrl qua auth profile endpoint.', 'PUT', '/api/v1/auth/profile',
  { name: 'Updated Name', avatarUrl: 'https://example.test/avatar.png' },
  'HTTP 200; users.name/avatar_url cập nhật cho user JWT; response mới đúng; audit PROFILE_UPDATED; không thay email/phone/role.',
  'User ACTIVE có token.', `${S.auth}; ${ROOT}/security/dto/request/UpdateProfileRequest.java`);
api('UPDATE', 'UC-09', 'Cập nhật một trường area qua extended profile endpoint.', 'PATCH', '/api/v1/profile',
  { area: 'Ho Chi Minh City' },
  'HTTP 200; chỉ area thay đổi; displayName/avatar/phone/dateOfBirth giữ nguyên; audit PROFILE_UPDATED trong cùng transaction.',
  'User ACTIVE có UserProfile và token.', S.profile, 'Source extension mapped to UC-09.');
api('UPDATE', 'UC-09', 'Cập nhật nhiều trường hồ sơ mở rộng hợp lệ.', 'PATCH', '/api/v1/profile',
  { displayName: 'Mai Anh', avatarUrl: 'https://example.test/mai.png', phoneNumber: '0912345678', dateOfBirth: '1995-05-20', area: 'Da Nang' },
  'HTTP 200; các field hợp lệ cập nhật đúng; user display name đồng bộ; không sửa userId; audit một lần.',
  'User ACTIVE có token và profile.', S.profile);
api('UPDATE', 'UC-10', 'Cập nhật privacy settings hợp lệ.', 'PUT', '/api/v1/privacy-settings/me',
  { profileVisibility: 'PRIVATE', locationSharingEnabled: false, analyticsConsent: false, dataExportOptOut: true },
  'HTTP 200; settings của user JWT cập nhật; withdrawal analytics được audit; không cập nhật settings user khác.',
  'User ACTIVE có token; analyticsConsent ban đầu true.', S.privacy, 'Partial implementation of UC-10 privacy/visibility.');
api('UPDATE', 'UC-11', 'Cập nhật notification preference cho một loại notification.', 'PUT', '/api/v1/users/me/notification-preferences',
  { preferences: [{ notificationType: 'COMMUNITY_REPLY', pushEnabled: false, emailEnabled: true, inAppEnabled: true }], appointmentReminderDefaults: [-1440, -60, 0] },
  'HTTP 200; upsert không duplicate theo user+type; reminder defaults được normalize; audit NOTIFICATION_PREFERENCES_UPDATED.',
  'User ACTIVE có token; preference row có hoặc chưa tồn tại.', S.notifyPref);
api('UPDATE', 'UC-12', 'Đánh dấu một notification chưa đọc là đã đọc.', 'PUT', '/api/v1/notifications/<NOTIFICATION_ID_FROM_PRECONDITION>/read', {},
  'HTTP 200; is_read=true và read_at được cập nhật atomically; audit NOTIFICATIONS_READ count=1 được ghi.',
  'User token sở hữu notification đang unread.', S.notify);
api('UPDATE', 'UC-12', 'Đánh dấu lại notification đã đọc theo tính idempotent.', 'PUT', '/api/v1/notifications/<ALREADY_READ_NOTIFICATION_ID>/read', {},
  'HTTP 200; read_at không bị tạo audit mới; không thay đổi dữ liệu khác.',
  'User token sở hữu notification đã read.', S.notify);
api('UPDATE', 'UC-12', 'Đánh dấu tất cả notification chưa đọc của chính user.', 'PUT', '/api/v1/notifications/read-all', {},
  'HTTP 200; markedCount bằng số row thực sự đổi; chỉ notifications của user JWT được cập nhật; audit count chính xác.',
  'User có nhiều unread notifications và user khác cũng có unread records.', S.notify);
api('UPDATE', 'UC-17', 'System Admin khóa tài khoản khác với reason hợp lệ.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { locked: true, reason: 'Confirmed abuse during QA test' },
  'HTTP 200; target locked=true, ADMIN lock metadata và episode id được tạo; toàn bộ target sessions revoked; audit USER_ACCOUNT_STATUS_CHANGED.',
  'SYSTEM_ADMIN token; target khác caller, ACTIVE và unlocked.', `${S.admin}; ${ROOT}/identity/admin/dto/request/UpdateUserStatusRequest.java`);
api('UPDATE', 'UC-17', 'System Admin mở khóa tài khoản khác với CSKH ticket.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { locked: false, reason: 'Issue resolved', cskhTicketId: 'CSKH-TEST-001' },
  'HTTP 200; lock metadata được clear; audit giữ lock episode reference/ticket; không tự khôi phục session đã revoke.',
  'SYSTEM_ADMIN token; target đang ADMIN locked.', `${S.admin}; ${ROOT}/identity/admin/dto/request/UpdateUserStatusRequest.java`);
api('UPDATE', 'UC-17', 'System Admin disable tài khoản khác.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { enabled: false, reason: 'Administrative disable' },
  'HTTP 200; enabled=false; toàn bộ sessions của target revoked; token cũ bị từ chối ở protected endpoint; audit status change.',
  'SYSTEM_ADMIN token; target khác caller và enabled.', S.admin);
api('UPDATE', 'UC-17', 'System Admin đổi role giữa các staff governance role.', 'PATCH', '/api/v1/admin/users/<STAFF_USER_ID>/role',
  { newRole: 'CONTENT_ADMIN', lockAccessRights: false, reason: 'Approved staff reassignment' },
  'HTTP 200; role đổi từ MODERATOR/CONTENT_ADMIN/SYSTEM_ADMIN sang role hợp lệ; audit ROLE_PERMISSION_UPDATED có previous/new role.',
  'SYSTEM_ADMIN token; target là staff governance role và khác caller.', `${S.role}; ${ROOT}/identity/admin/dto/request/UpdateUserRoleRequest.java`);
api('UPDATE', 'UC-07', 'Đổi mật khẩu hợp lệ cho user đang đăng nhập.', 'PUT', '/api/v1/auth/change-password',
  { oldPassword: 'OldPass1!', newPassword: 'NewPass2@', confirmPassword: 'NewPass2@' },
  'HTTP 200; password hash thay đổi; refresh tokens hiện hành bị revoke; audit PASSWORD_CHANGED; đăng nhập bằng password cũ thất bại.',
  'User ACTIVE có token và password OldPass1!; có refresh token đang active.', `${S.auth}; ${ROOT}/security/dto/request/ChangePasswordRequest.java`);
api('UPDATE', 'UC-06', 'Đặt lại mật khẩu bằng recovery token hợp lệ.', 'POST', '/api/v1/auth/reset-password',
  { token: '<PASSWORD_RESET_TOKEN_FROM_STUB>', newPassword: 'ResetPass2@', confirmPassword: 'ResetPass2@' },
  'HTTP 200; password hash cập nhật; reset token used; refresh tokens revoke; audit PASSWORD_RESET_COMPLETED; token không dùng lại được.',
  'User ACTIVE; reset token single-use chưa hết hạn; SMTP/SMS reset flow đã tạo token.', `${ROOT}/security/controller/AuthController.java; ${ROOT}/security/service/impl/ResetPasswordServiceImpl.java; ${ROOT}/security/dto/request/ResetPasswordRequest.java`);

// DELETE / REVOKE
api('REVOKE', 'UC-04', 'Logout bằng refresh token của session hiện tại.', 'POST', '/api/v1/auth/logout',
  { refreshToken: '<REFRESH_TOKEN_FROM_LOGIN>' },
  'HTTP 200; auth_session và refresh token bị revoke; token hash vào blacklist; audit LOGOUT; refresh sau đó bị từ chối.',
  'User ACTIVE đã login; access/refresh token hợp lệ.', `${S.auth}; ${S.session}`);
api('REVOKE', 'UC-04', 'Logout không gửi refresh token, dùng JWT sid hiện tại.', 'POST', '/api/v1/auth/logout', {},
  'HTTP 200; đúng session theo sid bị revoke và blacklist; các session khác vẫn active; security context được clear.',
  'User có ít nhất hai session; request dùng access token của một session.', S.session);
api('REVOKE', 'UC-13', 'Thu hồi một session khác của chính user.', 'DELETE', '/api/v1/sessions/<OTHER_SESSION_ID_FROM_PRECONDITION>', undefined,
  'HTTP 200; chỉ session được chọn bị revoke; refresh token hash bị blacklist; audit SESSION_REVOKED; current session vẫn active.',
  'User có current session và một other session active.', S.session);
api('REVOKE', 'UC-13', 'Thu hồi tất cả session khác, giữ current session.', 'DELETE', '/api/v1/sessions', undefined,
  'HTTP 200; tất cả session khác revoked atomically; current sid vẫn active; message count chính xác; audit khi count>0.',
  'User có current session và ít nhất hai other sessions.', S.session);
api('REVOKE', 'UC-16', 'Thu hồi consent đang active của chính user.', 'DELETE', '/api/v1/consent/grants/<CONSENT_ID_FROM_PRECONDITION>', undefined,
  'HTTP 200; consent status REVOKED/revokedAt/revokedBy cập nhật; dependent location/recommendation shares cleanup; audit CONSENT_REVOKED; lần protected read tiếp theo bị chặn.',
  'User token sở hữu active consent có permissionId.', S.consent);
api('REVOKE', 'UC-11/UC-12', 'Hủy đăng ký FCM device token của chính user.', 'DELETE', '/api/v1/notifications/device-token?token=<FCM_DEVICE_TOKEN>', undefined,
  'HTTP 200; token của đúng user chuyển inactive; cùng token của user khác không bị tác động.',
  'User token; active device token thuộc user.', S.notify, 'Source integration supporting UC-11/UC-12.');
api('REVOKE', 'UC-14', 'Vô hiệu hóa tài khoản cá nhân bằng password đúng.', 'DELETE', '/api/v1/auth/deactivate',
  { confirmPassword: 'SafePass1!', reason: 'User requested deactivation' },
  'HTTP 200; account_status=DEACTIVATED/enabled=false; sessions, refresh tokens và device tokens bị revoke trong cùng transaction; audit SECURITY_EVENT; dữ liệu được giữ theo retention.',
  'User ACTIVE không phải SYSTEM_ADMIN, có token, sessions và device token.', `${S.auth}; ${ROOT}/security/dto/request/DeactivateRequest.java`);
requirementOnly('REVOKE', 'UC-14', 'Gửi yêu cầu xóa tài khoản cá nhân theo retention workflow.',
  '1. Mở account lifecycle.\n2. Chọn Delete Account.\n3. Xác nhận password và retention notice.\n4. Gửi yêu cầu.\n5. Kiểm tra trạng thái deletion request, sessions và audit.',
  'Yêu cầu xóa được ghi nhận theo retention/care-group/audit obligations; session bị bảo vệ phù hợp; không xóa dữ liệu trái chính sách.',
  'User ACTIVE không phải SYSTEM_ADMIN; dữ liệu care-group/audit fixture có sẵn.');
api('REVOKE', 'UC-16', 'Từ chối thu hồi consent đã revoke lần hai.', 'DELETE', '/api/v1/consent/grants/<REVOKED_CONSENT_ID>', undefined,
  'HTTP 400 với CONSENT-012; revokedAt giữ nguyên; không tạo audit revocation thứ hai.',
  'User token sở hữu consent đã revoke.', S.consent);
api('REVOKE', 'UC-13', 'Từ chối revoke chính current session qua session endpoint.', 'DELETE', '/api/v1/sessions/<CURRENT_SESSION_ID>', undefined,
  'HTTP 400 với hướng dẫn dùng Logout; session hiện tại không bị revoke.',
  'User có access token chứa sid trùng path sessionId.', S.session);

// SEARCH
api('SEARCH', 'UC-17', 'Tìm user theo email chính xác trong admin list.', 'GET', '/api/v1/admin/users?email=target%40example.test&page=0&size=20', undefined,
  'HTTP 200; chỉ kết quả phù hợp email filter, không trả user không liên quan.',
  'SYSTEM_ADMIN token; có target và non-target users.', S.admin);
api('SEARCH', 'UC-17', 'Tìm user theo một phần tên.', 'GET', '/api/v1/admin/users?name=Anh&page=0&size=20', undefined,
  'HTTP 200; trả các user phù hợp query name theo repository contract; không vượt page size.',
  'SYSTEM_ADMIN token; fixture nhiều tên.', S.admin);
api('SEARCH', 'UC-17', 'Tìm user không có kết quả.', 'GET', '/api/v1/admin/users?email=no-match%40example.test&page=0&size=20', undefined,
  'HTTP 200; content rỗng và totalElements=0; UI thể hiện no result.',
  'SYSTEM_ADMIN token; email query không tồn tại.', S.admin);
api('SEARCH', 'UC-18', 'Tìm audit log theo userId và action.', 'GET', '/api/v1/admin/audit-logs?userId=<USER_ID>&action=LOGIN&page=0&size=20', undefined,
  'HTTP 200; mọi row khớp userId và LOGIN; filter snapshot được meta-audit.',
  'SYSTEM_ADMIN/OPERATIONS token; có nhiều audit action/user.', S.audit);

// FILTER
api('FILTER', 'UC-12', 'Lọc notification theo type hợp lệ.', 'GET', '/api/v1/notifications/me?type=COMMUNITY_REPLY&page=0&size=20', undefined,
  'HTTP 200; tất cả row trả về có type COMMUNITY_REPLY và thuộc user JWT.',
  'User token; có notifications nhiều loại.', S.notify);
api('FILTER', 'UC-17', 'Lọc admin users theo role và trạng thái enabled.', 'GET', '/api/v1/admin/users?role=MOTHER&enabled=true&page=0&size=20', undefined,
  'HTTP 200; mọi kết quả role=MOTHER và enabled=true.',
  'SYSTEM_ADMIN token; fixture nhiều role/status.', S.admin);
api('FILTER', 'UC-17', 'Lọc admin users theo locked=true.', 'GET', '/api/v1/admin/users?locked=true&page=0&size=20', undefined,
  'HTTP 200; chỉ trả tài khoản locked.',
  'SYSTEM_ADMIN token; có locked và unlocked users.', S.admin);
api('FILTER', 'UC-18', 'Lọc audit log theo khoảng thời gian.', 'GET', '/api/v1/admin/audit-logs?fromDate=2026-08-01T00:00:00Z&toDate=2026-08-09T23:59:59Z&page=0&size=20', undefined,
  'HTTP 200; mọi audit row nằm trong khoảng thời gian; không bỏ qua boundary hợp lệ.',
  'SYSTEM_ADMIN/OPERATIONS token; có audit trước/trong/sau khoảng.', S.audit);

// SORT
api('SORT', 'UC-12', 'Notification list sắp xếp mới nhất trước.', 'GET', '/api/v1/notifications/me?page=0&size=20', undefined,
  'HTTP 200; createdAt không tăng dần giữa các row; pagination ổn định khi dữ liệu không đổi.',
  'User token; ít nhất ba notification có createdAt khác nhau.', S.notify);
api('SORT', 'UC-13', 'Paged session list sắp xếp lastActivityAt giảm dần.', 'GET', '/api/v1/sessions/paged?page=0&size=10', undefined,
  'HTTP 200; session rows theo lastActivityAt DESC; current flag vẫn đúng sau sort.',
  'User token; nhiều sessions có lastActivityAt khác nhau.', S.session);
api('SORT', 'UC-18', 'Audit log sắp xếp createdAt giảm dần.', 'GET', '/api/v1/admin/audit-logs?page=0&size=20', undefined,
  'HTTP 200; audit rows newest-first theo controller PageRequest.',
  'SYSTEM_ADMIN/OPERATIONS token; có nhiều audit rows.', S.audit);

// PAGINATION
api('PAGE', 'UC-12', 'Chuyển trang notification sau khi lọc type.', 'GET', '/api/v1/notifications/me?type=REMINDER&page=1&size=5', undefined,
  'HTTP 200; trả trang thứ hai đúng size, filter REMINDER được giữ, không duplicate row từ page 0.',
  'User token; có hơn 5 REMINDER notifications.', S.notify);
api('PAGE', 'UC-13', 'Lấy trang giữa của session list.', 'GET', '/api/v1/sessions/paged?page=1&size=2', undefined,
  'HTTP 200; page metadata đúng; không trùng sessionId với page 0.',
  'User token; có ít nhất 5 sessions không revoked.', S.session);
api('PAGE', 'UC-17', 'Phân trang admin user list với size vượt MAX_PAGE_SIZE.', 'GET', '/api/v1/admin/users?page=0&size=9999', undefined,
  'HTTP 200; effective page size bị clamp theo AppConstants.MAX_PAGE_SIZE; không gây tải toàn bảng.',
  'SYSTEM_ADMIN token; DB có nhiều users.', S.admin);
api('PAGE', 'UC-17', 'Phân trang session history của target user.', 'GET', '/api/v1/admin/users/<USER_ID>/sessions?page=1&size=2', undefined,
  'HTTP 200; đúng target user và page metadata; không trùng page trước.',
  'SYSTEM_ADMIN token; target có ít nhất 5 sessions.', S.admin);
api('PAGE', 'UC-17', 'Phân trang activity history của target user.', 'GET', '/api/v1/admin/users/<USER_ID>/activity?page=1&size=2', undefined,
  'HTTP 200; đúng target user, trang và thứ tự; không trùng row page trước.',
  'SYSTEM_ADMIN token; target có ít nhất 5 audit rows.', S.admin);
api('PAGE', 'UC-18', 'Từ chối pagination audit log không hợp lệ.', 'GET', '/api/v1/admin/audit-logs?page=-1&size=0', undefined,
  'HTTP 400 với PAGINATION_INVALID; không query full table; không trả audit rows.',
  'SYSTEM_ADMIN/OPERATIONS token.', S.audit);

// VALIDATION
api('VALID', 'UC-01', 'Từ chối đăng ký khi thiếu cả email và phone.', 'POST', '/api/v1/auth/register',
  { name: 'Missing Contact', password: 'SafePass1!', role: 'MOTHER' },
  'HTTP 400; không tạo user/OTP/audit và không gọi SMTP/SMS.',
  'DB không có fixture tương ứng.', `${S.auth}; ${ROOT}/security/dto/request/RegisterRequest.java`);
api('VALID', 'UC-01', 'Từ chối đăng ký với password yếu.', 'POST', '/api/v1/auth/register',
  { name: 'Weak Password', email: 'weak@example.test', password: 'password', role: 'MOTHER' },
  'HTTP 400 với password complexity message; không tạo user/OTP.',
  'Email mới.', `${S.auth}; ${S.authPolicy}`);
api('VALID', 'UC-01', 'Từ chối đăng ký với email sai format.', 'POST', '/api/v1/auth/register',
  { name: 'Invalid Email', email: 'not-an-email', password: 'SafePass1!', role: 'MOTHER' },
  'HTTP 400; không tạo user/OTP và không gọi email service.',
  'DB sạch cho dữ liệu.', `${S.auth}; ${ROOT}/security/dto/request/RegisterRequest.java`);
api('VALID', 'UC-01', 'Từ chối đăng ký với phone Việt Nam sai format.', 'POST', '/api/v1/auth/register',
  { name: 'Invalid Phone', phone: '12345', password: 'SafePass1!', role: 'FAMILY' },
  'HTTP 400; không tạo user/OTP và không gọi SMS.',
  'DB sạch cho dữ liệu.', `${S.auth}; ${ROOT}/security/dto/request/RegisterRequest.java`);
api('VALID', 'UC-01', 'Từ chối đăng ký trùng email.', 'POST', '/api/v1/auth/register',
  { name: 'Duplicate', email: 'existing@example.test', password: 'SafePass1!', role: 'MOTHER' },
  'HTTP 409; giữ nguyên user hiện có; không tạo OTP mới hoặc gửi OTP.',
  'Email đã tồn tại trong users.', S.auth);
api('VALID', 'UC-01', 'Từ chối self-registration bằng role quản trị.', 'POST', '/api/v1/auth/register',
  { name: 'Invalid Role', email: 'admin-self@example.test', password: 'SafePass1!', role: 'SYSTEM_ADMIN' },
  'HTTP 400; không tạo account; chỉ MOTHER/FAMILY/EXPERT được self-register.',
  'Email mới.', `${S.auth}; ${S.authPolicy}`);
api('VALID', 'UC-02', 'Từ chối OTP không đủ 6 chữ số.', 'POST', '/api/v1/auth/verify-otp',
  { email: 'pending@example.test', otp: '12345' },
  'HTTP 400 từ validation; không giảm attempt, không activate user, không tạo session.',
  'User pending có OTP hợp lệ khác.', `${S.auth}; ${ROOT}/security/dto/request/VerifyOtpRequest.java`);
api('VALID', 'UC-02', 'Từ chối OTP sai và giảm số lần thử.', 'POST', '/api/v1/auth/verify-otp',
  { email: 'pending@example.test', otp: '999999' },
  'HTTP 400 Invalid OTP; attempts giảm đúng một; user vẫn disabled; không tạo token/session.',
  'User pending; OTP REGISTER chưa hết hạn và attempts>1.', S.auth);
api('VALID', 'UC-02', 'Từ chối OTP đã hết hạn.', 'POST', '/api/v1/auth/verify-otp',
  { email: 'expired-otp@example.test', otp: '123456' },
  'HTTP 400 Invalid or expired OTP; user không active; không tạo session.',
  'OTP expiryAt trước now.', S.auth);
api('VALID', 'UC-02', 'Từ chối resend OTP khi gửi đồng thời phone và email.', 'POST', '/api/v1/auth/resend-otp',
  { phone: '0912345678', email: 'pending@example.test' },
  'HTTP 400; OTP cũ không bị invalidated; không tạo OTP mới, không gọi external service.',
  'Pending account có cả phone/email.', `${S.auth}; ${ROOT}/security/dto/request/ResendOtpRequest.java`);
api('VALID', 'UC-03', 'Từ chối login khi gửi cả phone và email.', 'POST', '/api/v1/auth/login',
  { phone: '0912345678', email: 'user@example.test', password: 'SafePass1!' },
  'HTTP 400; không tạo session và không cập nhật lastLoginAt.',
  'Account tồn tại.', `${S.auth}; ${ROOT}/security/dto/request/LoginRequest.java`);
api('VALID', 'UC-05', 'Từ chối forgot password với contact rỗng.', 'POST', '/api/v1/auth/forgot-password',
  { contact: '' },
  'HTTP 400; không tạo reset token, không gọi SMTP/SMS.',
  'Không cần token.', `${ROOT}/security/service/impl/ForgotPasswordServiceImpl.java; ${ROOT}/security/dto/request/ForgotPasswordRequest.java`);
api('VALID', 'UC-06', 'Từ chối reset password khi confirm không khớp.', 'POST', '/api/v1/auth/reset-password',
  { token: '<VALID_RESET_TOKEN>', newPassword: 'ResetPass2@', confirmPassword: 'Different3#' },
  'HTTP 400; password và reset token giữ nguyên; refresh tokens không bị revoke.',
  'Reset token hợp lệ chưa dùng.', `${ROOT}/security/service/impl/ResetPasswordServiceImpl.java; ${ROOT}/security/dto/request/ResetPasswordRequest.java`);
api('VALID', 'UC-07', 'Từ chối change password khi current password sai.', 'PUT', '/api/v1/auth/change-password',
  { oldPassword: 'WrongPass1!', newPassword: 'NewPass2@', confirmPassword: 'NewPass2@' },
  'HTTP 400 AUTH-071; password hash/session/refresh token không thay đổi; không audit PASSWORD_CHANGED.',
  'User ACTIVE có token.', `${S.auth}; ${ROOT}/security/dto/request/ChangePasswordRequest.java`);
api('VALID', 'UC-09', 'Từ chối extended profile có dateOfBirth trong tương lai.', 'PATCH', '/api/v1/profile',
  { dateOfBirth: '2099-01-01' },
  'HTTP 400 PRF-002; profile và audit không thay đổi.',
  'User ACTIVE có token/profile.', S.profile);
api('VALID', 'UC-09', 'Từ chối displayName chứa ký tự HTML nguy hiểm.', 'PATCH', '/api/v1/profile',
  { displayName: '<script>alert(1)</script>' },
  'HTTP 400 do DTO pattern; script không được lưu hoặc phản chiếu.',
  'User ACTIVE có token/profile.', S.profile);
api('VALID', 'UC-15', 'Từ chối consent có expiryDays bằng 0.', 'POST', '/api/v1/consent/grants',
  { dataType: 'HEALTH_RECORD', purpose: 'SHARE', recipient: 'family@example.test', scope: 'summary', expiryDays: 0 },
  'HTTP 400; không tạo grant/audit.',
  'User ACTIVE có token.', S.consent);
api('VALID', 'UC-11', 'Từ chối notification preference có enum không tồn tại.', 'PUT', '/api/v1/users/me/notification-preferences',
  { preferences: [{ notificationType: 'UNKNOWN_TYPE', pushEnabled: true, emailEnabled: true, inAppEnabled: true }] },
  'HTTP 400; không upsert preference và không ghi audit update.',
  'User ACTIVE có token.', S.notifyPref);
api('VALID', 'UC-17', 'Từ chối status update khi không có enabled/locked.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { reason: 'No state selected' },
  'HTTP 400 IAM-114-002; target không đổi; không audit.',
  'SYSTEM_ADMIN token; target khác caller.', S.admin);
api('VALID', 'UC-17', 'Từ chối lock account khi thiếu reason.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { locked: true },
  'HTTP 400 IAM-114-005; target không bị lock; sessions không bị revoke.',
  'SYSTEM_ADMIN token; target khác caller và unlocked.', S.admin);

// AUTHENTICATION
api('AUTH', 'UC-03', 'Login bằng email và password hợp lệ.', 'POST', '/api/v1/auth/login',
  { email: 'active@example.test', password: 'SafePass1!' },
  'HTTP 200; tạo session ACTIVE, refresh token và access token có sid; lastLoginAt cập nhật; audit LOGIN; raw refresh token chỉ trả client.',
  'User ACTIVE/enabled, unlocked, password đúng.', S.auth);
api('AUTH', 'UC-03', 'Login bằng phone và password hợp lệ.', 'POST', '/api/v1/auth/login',
  { phone: '0912345678', password: 'SafePass1!' },
  'HTTP 200; phone canonicalize; tạo đúng một session và token pair.',
  'User ACTIVE có phone tương ứng và password đúng.', S.auth);
api('AUTH', 'UC-03', 'Login sai password trả thông báo trung lập.', 'POST', '/api/v1/auth/login',
  { email: 'active@example.test', password: 'WrongPass1!' },
  'HTTP 401/400 theo global error mapping với Invalid credentials; không lộ trạng thái account; không tạo session/token.',
  'User ACTIVE tồn tại.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Login bằng account chưa verify bị từ chối.', 'POST', '/api/v1/auth/login',
  { email: 'pending@example.test', password: 'SafePass1!' },
  'HTTP 403 account disabled; không tạo session/token.',
  'User PENDING_ACTIVATION/enabled=false, password đúng.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Login bằng account bị admin lock bị từ chối.', 'POST', '/api/v1/auth/login',
  { email: 'adminlocked@example.test', password: 'SafePass1!' },
  'HTTP 403 account admin locked; không xóa lock reason/metadata; không tạo session.',
  'User enabled, ADMIN locked, password đúng.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Login bằng account đang suspended bị từ chối.', 'POST', '/api/v1/auth/login',
  { email: 'suspended@example.test', password: 'SafePass1!' },
  'HTTP 403 ACCOUNT_SUSPENDED; không tạo session; suspendedUntil giữ nguyên.',
  'User enabled/unlocked, suspendedUntil tương lai.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Khóa tạm account sau giới hạn login sai.', 'POST', '/api/v1/auth/login',
  { email: 'ratelimit@example.test', password: 'WrongPass1!' },
  'Sau số lần theo RateLimitPolicy, request bị từ chối; user locked=true, lockType=TEMPORARY; không tạo session.',
  'User ACTIVE; rate-limit store sạch; lặp đúng số request trong window 15 phút.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Login thành công sau khi temporary lock hết hạn.', 'POST', '/api/v1/auth/login',
  { email: 'expiredlock@example.test', password: 'SafePass1!' },
  'HTTP 200; temporary lock được clear, session/token tạo; admin lock không được áp dụng logic này.',
  'User TEMPORARY lockedAt quá 15 phút, enabled, password đúng.', `${S.auth}; ${S.authPolicy}`);
api('AUTH', 'UC-03', 'Refresh token hợp lệ được rotate.', 'POST', '/api/v1/auth/refresh',
  { refreshToken: '<VALID_REFRESH_TOKEN>' },
  'HTTP 200; token cũ revoked; session hash/expiry cập nhật; trả access và refresh token mới; sessionId giữ nguyên.',
  'User ACTIVE; session ACTIVE; refresh token chưa revoke/chưa hết hạn.', S.auth);
api('AUTH', 'UC-03', 'Replay refresh token cũ sau rotation bị từ chối.', 'POST', '/api/v1/auth/refresh',
  { refreshToken: '<ROTATED_OLD_REFRESH_TOKEN>' },
  'HTTP 401/400 invalid refresh token; không tạo token/session mới; token hiện hành vẫn nhất quán.',
  'Refresh token cũ đã rotate/revoke; session dùng hash mới.', S.auth);
api('AUTH', 'UC-03', 'Refresh token hết hạn bị từ chối.', 'POST', '/api/v1/auth/refresh',
  { refreshToken: '<EXPIRED_REFRESH_TOKEN>' },
  'HTTP 401/400; không rotate token; session không được phục hồi.',
  'Refresh token/session expiry trước now.', S.auth);
api('AUTH', 'UC-04', 'Protected endpoint từ chối access token của session đã revoke.', 'GET', '/api/v1/auth/profile', undefined,
  'HTTP 401 SESSION_REVOKED; không trả profile.',
  'JWT chữ ký/thời hạn hợp lệ nhưng sid trỏ session revoked.', S.jwt);

// AUTHORIZATION / RBAC
api('ACCESS', 'UC-08', 'Protected profile endpoint từ chối khi không có token.', 'GET', '/api/v1/auth/profile', undefined,
  'HTTP 401; không trả dữ liệu và không tạo PROFILE_VIEWED audit.',
  'Không có Authorization header.', `${S.jwt}; ${S.auth}`);
api('ACCESS', 'UC-13', 'Session list từ chối anonymous caller.', 'GET', '/api/v1/sessions', undefined,
  'HTTP 401; không trả session metadata.',
  'Không có token.', `${S.jwt}; ${S.session}`);
api('ACCESS', 'UC-16', 'Consent list từ chối anonymous caller.', 'GET', '/api/v1/consent/grants', undefined,
  'HTTP 401; không trả consent.',
  'Không có token.', `${S.jwt}; ${S.consent}`);
api('ACCESS', 'UC-13', 'User không thể revoke session của tài khoản khác.', 'DELETE', '/api/v1/sessions/<OTHER_USERS_SESSION_ID>', undefined,
  'HTTP 400/403; session người khác không đổi; không ghi SESSION_REVOKED audit.',
  'User A token; path session thuộc user B.', S.session);
api('ACCESS', 'UC-16', 'User không thể revoke consent của tài khoản khác.', 'DELETE', '/api/v1/consent/grants/<OTHER_USERS_CONSENT_ID>', undefined,
  'HTTP 404; không tiết lộ grant tồn tại; consent người khác không đổi.',
  'User A token; consent thuộc user B.', S.consent);
api('ACCESS', 'UC-12', 'User không thể mark notification của tài khoản khác.', 'PUT', '/api/v1/notifications/<OTHER_USERS_NOTIFICATION_ID>/read', {},
  'HTTP 404; notification user khác không đổi; không audit read.',
  'User A token; notification thuộc user B.', S.notify);
api('ACCESS', 'UC-17', 'Mother bị từ chối truy cập admin user list.', 'GET', '/api/v1/admin/users?page=0&size=20', undefined,
  'HTTP 403; không trả user list.',
  'Token role MOTHER.', `${S.admin}; ${S.jwt}`);
api('ACCESS', 'UC-18', 'Moderator bị từ chối truy cập audit logs.', 'GET', '/api/v1/admin/audit-logs?page=0&size=20', undefined,
  'HTTP 403; không trả audit rows và không meta-audit.',
  'Token role MODERATOR.', `${S.audit}; ${S.jwt}`);
api('ACCESS', 'UC-17', 'System Admin không thể đổi trạng thái chính mình.', 'PATCH', '/api/v1/admin/users/<CALLER_USER_ID>/status',
  { locked: true, reason: 'self target' },
  'HTTP 403 IAM-114-004; caller không bị thay đổi; session không revoke; không audit mutation.',
  'SYSTEM_ADMIN token; path userId bằng JWT subject.', S.admin);
api('ACCESS', 'UC-17', 'System Admin không thể đổi role chính mình.', 'PATCH', '/api/v1/admin/users/<CALLER_USER_ID>/role',
  { newRole: 'CONTENT_ADMIN', reason: 'self target' },
  'HTTP 403 IAM-116-004; role caller không đổi; không audit role update.',
  'SYSTEM_ADMIN token; path userId bằng JWT subject.', S.role);
api('ACCESS', 'UC-14', 'System Admin không thể tự deactivate.', 'DELETE', '/api/v1/auth/deactivate',
  { confirmPassword: 'AdminPass1!', reason: 'self deactivate' },
  'HTTP 403; admin account/session không đổi.',
  'SYSTEM_ADMIN token và password đúng.', S.auth);
api('ACCESS', 'UC-17', 'JWT role cũ không giữ quyền sau khi database role thay đổi.', 'GET', '/api/v1/admin/users?page=0&size=20', undefined,
  'HTTP 403 nếu database role không còn SYSTEM_ADMIN; JwtAuthenticationFilter dùng current database authority, không dùng stale token role.',
  'JWT phát hành khi user là SYSTEM_ADMIN; DB role đã đổi sang MOTHER trước request.', S.jwt);

// INTEGRATION
api('INT', 'UC-01', 'Rollback đăng ký khi SMTP gửi OTP lỗi.', 'POST', '/api/v1/auth/register',
  { name: 'SMTP Failure', email: 'smtp-fail@example.test', password: 'SafePass1!', role: 'MOTHER' },
  'Request trả lỗi external service; transaction không để user/OTP/audit partial; retry không gặp duplicate do lần lỗi.',
  'SMTP stub ném RuntimeException; email mới; PostgreSQL test.', `${S.auth}; ${ROOT}/security/service/impl/GmailEmailService.java`);
api('INT', 'UC-01', 'Rollback đăng ký khi SMS gửi OTP lỗi.', 'POST', '/api/v1/auth/register',
  { name: 'SMS Failure', phone: '0912345680', password: 'SafePass1!', role: 'FAMILY' },
  'Request trả lỗi external service; không còn user/OTP/audit partial; retry an toàn.',
  'SMS stub ném RuntimeException; phone mới.', `${S.auth}; ${ROOT}/security/service/impl/MockSmsService.java`);
api('INT', 'UC-01/UC-03', 'Firebase verifier timeout trả lỗi an toàn.', 'POST', '/api/v1/auth/federated',
  { idToken: '<TOKEN_CAUSING_FIREBASE_TIMEOUT>', deviceInfo: 'Web test' },
  'HTTP 503; không tạo/link user identity, user hoặc session; response không lộ provider internals.',
  'Firebase gateway stub timeout; DB sạch provider subject.', `${ROOT}/security/service/impl/FederatedAuthServiceImpl.java; ${ROOT}/security/federation/FirebaseAdminTokenVerifier.java`, 'Source-only federated extension.');
api('INT', 'UC-02', 'Hai request verify cùng OTP chỉ có một request thắng.', 'POST', '/api/v1/auth/verify-otp',
  { email: 'race@example.test', otp: '<VALID_OTP>' },
  'Một request thành công; OTP consumed một lần; user active một lần; không duplicate session/audit completion.',
  'Một OTP REGISTER hợp lệ; gửi hai request đồng thời; PostgreSQL hỗ trợ locking/unique constraints.', `${S.auth}; ${ROOT}/security/repository/OtpVerificationRepository.java`);
api('INT', 'UC-03', 'Hai request refresh đồng thời không tạo hai refresh chain hợp lệ.', 'POST', '/api/v1/auth/refresh',
  { refreshToken: '<ONE_VALID_REFRESH_TOKEN>' },
  'Tối đa một request thành công; token cũ revoke; session trỏ duy nhất token hash mới; request thua bị từ chối.',
  'Một session/token ACTIVE; gửi hai refresh đồng thời.', `${S.auth}; ${ROOT}/security/repository/RefreshTokenRepository.java`);
ui('INT', 'UC-03/UC-04', 'Mobile App lưu token sau login và xóa trạng thái sau logout.',
  '1. Mở Mobile Login.\n2. Nhập email/password hợp lệ và nhấn Login.\n3. Kiểm tra AuthState nhận access/refresh/userId/role và FCM registration được kích hoạt.\n4. Chọn Logout, xác nhận.\n5. Kiểm tra API logout và local auth state.',
  'Login response đầy đủ được lưu atomically; logout backend revoke session; local token/state bị xóa; user trở về auth landing.',
  'Mobile app trỏ API test; account ACTIVE; FCM stub; không dùng token thật trong sheet.', `${S.mobileAuth}; ${S.auth}; ${S.session}`);
ui('INT', 'UC-03/UC-08', 'Web Portal route user theo role sau login và tải profile.',
  '1. Mở Web Login.\n2. Login bằng account role test.\n3. Kiểm tra auth store nhận token/user.\n4. Kiểm tra role route và GET profile.\n5. Refresh browser và kiểm tra session handling.',
  'Role route đúng current role; profile thuộc đúng user; không điều hướng vào workspace trái role; lỗi backend được hiển thị an toàn.',
  'Web origin allowlisted; account ACTIVE; API test.', `${S.webAuth}; ${WEB}/shared/auth/roleRoutes.ts; ${S.auth}`);
api('INT', 'UC-11/UC-12', 'FCM lỗi vẫn lưu notification FAILED và audit tương ứng.', 'POST', '/api/v1/notifications/send',
  { recipientUserId: '<USER_ID>', type: 'REMINDER', title: 'Reminder', body: 'Test reminder' },
  'HTTP 200 theo service contract; notification_record FAILED/failedAt/attemptCount đúng; audit NOTIFICATION_FAILED; không báo delivered giả.',
  'SYSTEM_ADMIN token; recipient có active token; FCM stub ném exception.', S.notify);
api('INT', 'UC-16', 'Consent revocation chặn protected read kế tiếp.', 'DELETE', '/api/v1/consent/grants/<ACTIVE_CONSENT_ID>', undefined,
  'Sau commit revocation, protected data read tiếp theo bị consent policy từ chối; dữ liệu owner còn nguyên; audit và dependent cleanup nhất quán.',
  'Owner token; active consent HEALTH_RECORD/VIEW; có protected record và recipient read fixture.', `${S.consent}; ${ROOT}/consent/policy/ConsentCheckPolicy.java`);
api('INT', 'UC-17', 'Lỗi audit bắt buộc rollback admin status mutation.', 'PATCH', '/api/v1/admin/users/<TARGET_USER_ID>/status',
  { locked: true, reason: 'Audit failure injection' },
  'Request lỗi; target enabled/locked/session state quay về trước mutation; không có partial audit.',
  'SYSTEM_ADMIN token; audit repository fault-injected trong transaction; target unlocked.', `${S.admin}; ${S.audit}`);
api('INT', 'UC-14', 'Deactivation thu hồi đồng thời session, refresh token và FCM token.', 'DELETE', '/api/v1/auth/deactivate',
  { confirmPassword: 'SafePass1!', reason: 'Integration revocation check' },
  'Sau commit, access token cũ bị 403/401, refresh bị từ chối, device tokens inactive; account data vẫn giữ cho retention; audit có timestamp/reason.',
  'User không phải SYSTEM_ADMIN; có nhiều sessions/refresh tokens/device tokens.', `${S.auth}; ${S.session}; ${S.notify}; ${S.db}`);
api('INT', 'UC-03/UC-18', 'Readiness phản ánh PostgreSQL outage mà không lộ endpoint actuator khác.', 'GET', '/actuator/health/readiness', undefined,
  'Khi DB down trả service unavailable/readiness DOWN; khi DB up trả ready; các actuator khác vẫn denied; không lộ credential/stack trace.',
  'Có thể điều khiển PostgreSQL; ứng dụng chạy với health readiness enabled.', `${S.jwt}; ${S.db}`, 'Source-only operational integration supporting account availability.');
api('INT', 'UC-03', 'CORS chỉ cho phép exact configured Web origin.', 'OPTIONS', '/api/v1/auth/login', undefined,
  'Allowlisted origin nhận đúng CORS headers/credentials; wildcard hoặc origin có path/query không được cấu hình; authorization không bị nới rộng.',
  'Chạy API với carebridge.cors.allowed-origins test; gửi preflight từ allowed và disallowed origins.', S.jwt, 'Source-only Web integration.');

// PRIVACY / SAFETY
api('PRIVACY', 'UC-05', 'Forgot password không tiết lộ account có tồn tại.', 'POST', '/api/v1/auth/forgot-password',
  { contact: 'unknown@example.test' },
  'HTTP 200 với cùng neutral message/shape như contact tồn tại; không tạo reset token hoặc audit cho unknown account.',
  'Contact không tồn tại; rate limit chưa vượt.', `${ROOT}/security/service/impl/ForgotPasswordServiceImpl.java`);
api('PRIVACY', 'UC-08', 'Profile response không lộ password hash, OTP hoặc token.', 'GET', '/api/v1/auth/profile', undefined,
  'HTTP 200; response JSON không có passwordHash, refreshToken, refreshTokenHash, OTP/codeHash hoặc security lock internals không được phép.',
  'User ACTIVE có token; DB chứa password/session/OTP fixtures để kiểm tra không leak.', `${S.auth}; ${ROOT}/security/mapper/UserMapper.java`);
api('PRIVACY', 'UC-10', 'Public community display không lộ private maternal/child data.', 'GET', '/api/v1/community/feed?page=0&size=20', undefined,
  'Community author projection chỉ dùng displayName/anonymous label; không trả phone, email, maternal records, baby records hoặc family identifiers.',
  'Authenticated user; có post non-anonymous của user có private data fixtures.', `${ROOT}/community/service/CommunityAuthorDisplayResolver.java; ${ROOT}/community/mapper/CommunityFeedMapper.java`, 'Source-based privacy enforcement mapped to UC-10.');
api('PRIVACY', 'UC-15', 'Consent grant mặc định expiry hữu hạn khi expiryDays bỏ trống.', 'POST', '/api/v1/consent/grants',
  { dataType: 'SENSITIVE_DATA', purpose: 'VIEW', recipient: 'expert@example.test', scope: 'selected-records' },
  'HTTP 200; expiryAt dùng DEFAULT_EXPIRY_DAYS, không tạo consent vô hạn; owner là JWT user; audit không chứa raw health data.',
  'User ACTIVE có token; recipient fixture.', `${S.consent}; ${ROOT}/common/constants/ConsentConstants.java`);
api('PRIVACY', 'UC-16', 'Consent list chỉ trả grant của chính user.', 'GET', '/api/v1/consent/grants', undefined,
  'HTTP 200; mọi grant có owner user JWT; active và past grants đúng; không trả grant user khác.',
  'User A token; DB có grants của user A và B.', S.consent);
api('PRIVACY', 'UC-18', 'Audit query không trả health payload vượt nhu cầu điều tra.', 'GET', '/api/v1/admin/audit-logs?action=VIEW_HEALTH_RECORD&page=0&size=20', undefined,
  'HTTP 200; audit metadata tối thiểu theo schema; không trả raw file content/clinical record body; meta-audit chỉ lưu filter snapshot.',
  'SYSTEM_ADMIN/OPERATIONS token; có VIEW_HEALTH_RECORD audit fixtures.', S.audit);
api('PRIVACY', 'UC-03', 'Refresh token không lưu raw value trong auth_sessions.', 'POST', '/api/v1/auth/login',
  { email: 'hashcheck@example.test', password: 'SafePass1!' },
  'HTTP 200; client nhận raw refresh token; auth_sessions chỉ có SHA-256 hash; log/audit không chứa raw access/refresh token.',
  'User ACTIVE; PostgreSQL query access cho QA.', `${S.auth}; ${S.session}; ${S.db}`);
api('PRIVACY', 'UC-18', 'Audit append-only không cho operator sửa/xóa lịch sử qua API.', 'PATCH', '/api/v1/admin/audit-logs/<AUDIT_ID>',
  { action: 'LOGIN' },
  'HTTP 405/404; không có mutation endpoint; audit row giữ nguyên; normal operator không thể sửa/xóa.',
  'SYSTEM_ADMIN/OPERATIONS token; audit row tồn tại.', `${S.audit}; ${S.db}`);

const counts = cases.reduce((acc, tc) => {
  acc[tc.group] = (acc[tc.group] || 0) + 1;
  return acc;
}, {});

const rows = cases.map(tc => [
  tc.id,
  tc.description,
  tc.procedure,
  tc.expected,
  tc.preconditions,
  '',
  'Pending', '', '',
  'Pending', '', '',
  'Pending', '', '',
  tc.note,
]);

const output = { feature: 'MF-01 — Account, Trust & Access Control', counts, total: cases.length, cases, rows };
fs.writeFileSync('artifacts/mf01_test_cases.json', JSON.stringify(output, null, 2), 'utf8');
console.log(JSON.stringify({ total: output.total, counts: output.counts }, null, 2));
