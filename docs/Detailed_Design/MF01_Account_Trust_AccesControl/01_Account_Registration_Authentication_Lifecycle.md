# MF-01 / Spec 01 — Account Registration & Authentication Lifecycle

| Field | Value |
| --- | --- |
| Feature | MF-01 — Account, Trust & Access Control |
| Use Cases Covered | UC-01 Register Account, UC-02 Verify OTP, UC-03 Log In, UC-04 Log Out, UC-05 Request Password Reset, UC-06 Reset Password, UC-07 Change Password |
| Primary Actor(s) | Guest → User |
| Secondary Actors | OTP / Email Service |
| Platform | Mobile App / Web Portal |
| Main Flow Summary | A Guest registers with email/phone + password, confirms ownership via OTP to activate the account, then logs in and receives a session. Forgot/Reset/Change password are the credential-recovery and credential-rotation branches of the same identity lifecycle. |
| Grounding (source code) | `security/entity/User.java`, `OtpVerification.java`, `RefreshToken.java`, `PasswordResetToken.java`, `security/controller/AuthController.java` (`/api/v1/auth/*`), `security/service/impl/AuthServiceImpl.java`, `ForgotPasswordServiceImpl.java` |

## 1. Tổng quan luồng chính (Main Flow Overview)

Đây là luồng "xương sống" của MF-01: không có tài khoản nào truy cập được dữ liệu
riêng tư (mother/baby/health) nếu chưa đi qua chuỗi **Register → Verify OTP → Login**.
`User.accountStatus` bắt đầu ở `PENDING_ACTIVATION`, chỉ chuyển sang `ACTIVE` sau khi
OTP đúng (BR-ACCOUNT-02). Đăng nhập kiểm tra `accountStatus`, `enabled`, `locked` và
`suspendedUntil` trước khi phát hành `RefreshToken` (BR-SECURITY-01). Logout thu hồi
refresh token hiện tại (BR-SECURITY-02). Forgot/Reset Password dùng một
`PasswordResetToken` một-lần dùng, có hạn (BR-SECURITY-03/04); Change Password yêu cầu
xác nhận mật khẩu hiện tại (BR-SECURITY-05). Các luồng update-profile/session-list/
notification-preferences khác của MF-01 là settings phụ, không lặp lại ở đây.

## 2. Class Diagram

```plantuml
@startuml MF01_01_AccountAuth_ClassDiagram
skinparam classAttributeIconSize 0
skinparam classFontStyle bold
skinparam backgroundColor #FAFAFA
skinparam ArrowColor #555555
skinparam ClassBorderColor #2E75B6
skinparam ClassHeaderBackgroundColor #D5E8F0

' === ENTITY ===
class User {
  + id: UUID
  + email: String
  + phone: String
  + passwordHash: String
  + name: String
  + role: Role
  + accountStatus: String
  + emailVerified: Boolean
  + phoneVerified: Boolean
  + enabled: boolean
  + locked: boolean
  + lockedAt: Instant
  + suspendedUntil: Instant
  + mustChangePassword: boolean
  + lastLoginAt: Instant
}

enum Role {
  MOTHER
  FAMILY
  EXPERT
  MODERATOR
  CONTENT_ADMIN
  SYSTEM_ADMIN
  PARTNER
}

class OtpVerification {
  + id: Long
  + user: User
  + codeHash: String
  + purpose: OtpPurpose
  + expiresAt: Instant
  + usedAt: Instant
  + verified: boolean
  + attempts: int
}

enum OtpPurpose {
  REGISTER
  LOGIN
}

class RefreshToken {
  + id: Long
  + user: User
  + tokenHash: String
  + expiresAt: Instant
  + revoked: boolean
}

class PasswordResetToken {
  + id: UUID
  + user: User
  + tokenHash: String
  + expiresAt: Instant
  + usedAt: Instant
  + attemptCount: int
}

' === DTO ===
class RegisterRequest {
  + email: String
  + phone: String
  + password: String
  + role: Role
}

class LoginRequest {
  + identifier: String
  + password: String
}

' === CONTROLLER / SERVICE ===
class AuthController {
  - authService: AuthService
  + register(RegisterRequest): ResponseEntity
  + verifyOtp(VerifyOtpRequest): ResponseEntity
  + login(LoginRequest): ResponseEntity
  + logout(): ResponseEntity
  + forgotPassword(ForgotPasswordRequest): ResponseEntity
  + resetPassword(ResetPasswordRequest): ResponseEntity
  + changePassword(ChangePasswordRequest): ResponseEntity
}

interface AuthService <<interface>> {
  + register(RegisterRequest): AuthResponse
  + verifyOtp(VerifyOtpRequest): AuthResponse
  + login(LoginRequest): AuthResponse
  + logout(UUID userId): void
  + changePassword(UUID userId, ChangePasswordRequest): void
}

class AuthServiceImpl implements AuthService {
  - userRepository: UserRepository
  - otpService: OtpService
  - passwordEncoder: PasswordEncoder
  - refreshTokenRepository: RefreshTokenRepository
  - auditService: AuditService
}

interface UserRepository <<interface>> {
  + findByEmailOrPhone(id: String): Optional<User>
  + save(user: User): User
}

class ForgotPasswordServiceImpl {
  - passwordResetTokenRepository: PasswordResetTokenRepository
  - userRepository: UserRepository
  + requestReset(identifier: String): void
  + resetPassword(token: String, newPassword: String): void
}

User "1" *-- "0..*" OtpVerification : has
User "1" *-- "0..*" RefreshToken : has
User "1" *-- "0..*" PasswordResetToken : has
User "1" -- "1" Role
OtpVerification --> OtpPurpose
AuthServiceImpl --> UserRepository : uses
AuthServiceImpl --> OtpService : uses
AuthController --> AuthService : uses
ForgotPasswordServiceImpl --> UserRepository : uses

@enduml
```

**Hình 1 — Class Diagram: Account Registration & Authentication Lifecycle**

## 3. Sequence Diagram — Main Flow

```plantuml
@startuml MF01_01_AccountAuth_SequenceDiagram
skinparam sequenceArrowThickness 2
skinparam roundcorner 10
skinparam backgroundColor #FAFAFA

actor "Guest / User" as Client
participant "AuthController" as Controller
participant "AuthServiceImpl" as Service
participant "UserRepository" as Repo
participant "OtpService" as Otp
participant "AuditService" as Audit
database "PostgreSQL" as DB

== UC-01 Register Account ==
Client -> Controller : POST /api/v1/auth/register\n{email|phone, password, role}
Controller -> Service : register(request)
Service -> Repo : existsByEmailOrPhone(id)
Repo -> DB : SELECT ... FROM users
DB --> Repo : not found
Service -> Repo : save(User{accountStatus="PENDING_ACTIVATION"})
Repo -> DB : INSERT INTO users ...
Service -> Otp : generateAndSend(userId, REGISTER)
Otp -> DB : INSERT INTO otp_verifications ...
Service -> Audit : emit(AccountRegistered)
Service --> Controller : AuthResponse{status=PENDING_ACTIVATION}
Controller --> Client : HTTP 201 Created

== UC-02 Verify OTP ==
Client -> Controller : POST /api/v1/auth/verify-otp\n{identifier, code}
Controller -> Service : verifyOtp(request)
Service -> Otp : validate(code, purpose=REGISTER)
Otp -> DB : SELECT ... FROM otp_verifications WHERE ...
DB --> Otp : otpRecord{expiresAt, attempts}
Otp --> Service : verified=true
Service -> Repo : setAccountStatus(user, "ACTIVE")
Repo -> DB : UPDATE users SET account_status='ACTIVE'
Service -> Audit : emit(AccountActivated)
Service --> Controller : AuthResponse{status=ACTIVE}
Controller --> Client : HTTP 200 OK

== UC-03 Log In ==
Client -> Controller : POST /api/v1/auth/login\n{identifier, password}
Controller -> Service : login(request)
Service -> Repo : findByEmailOrPhone(identifier)
Repo -> DB : SELECT ... FROM users
DB --> Repo : user{accountStatus="ACTIVE", locked=false}
Service -> Service : check accountStatus=="ACTIVE" AND !locked AND suspendedUntil==null
Service -> Service : passwordEncoder.matches(password, passwordHash)
Service -> Repo : save(RefreshToken), update lastLoginAt
Repo -> DB : INSERT INTO refresh_tokens ...
Service -> Audit : emit(LoginSucceeded)
Service --> Controller : AuthResponse{accessToken, refreshToken}
Controller --> Client : HTTP 200 OK

== UC-04 Log Out ==
Client -> Controller : POST /api/v1/auth/logout\nAuthorization: Bearer <token>
Controller -> Service : logout(userId, refreshToken)
Service -> Repo : revoke(refreshToken)
Repo -> DB : UPDATE refresh_tokens SET revoked=true
Service --> Controller : void
Controller --> Client : HTTP 204 No Content

@enduml
```

**Hình 2 — Sequence Diagram: Account Registration → OTP → Login → Logout (Main Flow)**

> Ghi chú luồng phụ: `POST /forgot-password` phát hành `PasswordResetToken` một-lần
> dùng qua kênh đã đăng ký (không tiết lộ tài khoản có tồn tại hay không — BR-ACCOUNT-05);
> `POST /reset-password` xác thực token rồi ghi mật khẩu mới và thu hồi toàn bộ session
> đang hoạt động (BR-SECURITY-04); `PUT /change-password` yêu cầu xác nhận mật khẩu hiện
> tại trước khi ghi mật khẩu mới (BR-SECURITY-05). Cả ba đều tái sử dụng
> `AuthServiceImpl`/`ForgotPasswordServiceImpl` ở trên, không tạo entity mới.

## 4. State Machine — `User.accountStatus`

```plantuml
@startuml MF01_01_AccountStatus_StateMachine
skinparam backgroundColor #FAFAFA
skinparam StateBackgroundColor #D5E8F0
skinparam StateBorderColor #2E75B6

[*] --> PENDING_ACTIVATION : POST /register (UC-01)

PENDING_ACTIVATION --> ACTIVE : OTP verified (UC-02)
PENDING_ACTIVATION --> [*] : OTP expired, không kích hoạt\n[dọn dẹp định kỳ]

ACTIVE --> DEACTIVATED : User tự yêu cầu hoặc\nAdmin vô hiệu hóa (UC-14 / UC-17)
DEACTIVATED --> ACTIVE : Admin kích hoạt lại (UC-17)

note right of ACTIVE
  Guard bổ sung khi Login (UC-03), không phải state riêng:
  - locked = true  → chặn đăng nhập (khóa tạm do đăng nhập sai)
  - suspendedUntil != null và còn hạn → chặn đăng nhập
  - mustChangePassword = true → bắt buộc đổi mật khẩu trước khi vào app
end note

note right of PENDING_ACTIVATION
  OTP hết hạn theo cấu hình OtpVerification.expiresAt;
  attempts bị giới hạn (BR-ACCOUNT-03).
end note

@enduml
```

**Hình 3 — State Machine: `User.accountStatus` Lifecycle**

## 5. Business Rules Applied

- BR-RBAC — quyền truy cập giới hạn theo `role` hiệu lực.
- BR-ACCOUNT-01 — định danh liên hệ (email/phone) phải duy nhất.
- BR-ACCOUNT-02 — tài khoản không active cho tới khi verify OTP thành công.
- BR-ACCOUNT-03 — số lần thử OTP bị giới hạn và OTP có hạn dùng.
- BR-ACCOUNT-05 — response của forgot-password trung lập, không tiết lộ tài khoản tồn tại hay không.
- BR-RBAC-01 — routing sau login dựa trên role + trạng thái tài khoản hiệu lực.
- BR-SECURITY-01 — tài khoản `DEACTIVATED`/`locked`/`suspended` không thể tạo session mới.
- BR-SECURITY-02 — refresh token/session hiện tại bị thu hồi khi logout.
- BR-SECURITY-03 — request đặt lại mật khẩu bị giới hạn tần suất (rate-limited).
- BR-SECURITY-04 — mọi session đang hoạt động bị thu hồi sau khi reset password thành công.
- BR-SECURITY-05 — mật khẩu hiện tại phải được xác thực trước khi đổi mật khẩu.
