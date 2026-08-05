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
participant "Web / Mobile UI" as UI
participant "AuthController" as Controller
participant "AuthServiceImpl" as Service
participant "OtpService" as Otp
participant "AuditService" as Audit
participant "UserRepository" as Repo
participant "OtpVerificationRepository" as OtpRepo
participant "RefreshTokenRepository" as RefreshRepo
database "PostgreSQL" as DB

== UC-01 Register Account ==
Client -> UI : 1. Submit request
activate UI
UI -> Controller : 1a. POST /api/v1/auth/register\n{email|phone, password, role}
activate Controller
Controller -> Service : 2. register(request)
activate Service
Service -> Repo : 3. existsByEmailOrPhone(id)
activate Repo
Repo -> DB : 4. SELECT ... FROM users
activate DB
DB --> Repo : 5. not found
deactivate DB
Repo --> Service : 6. false
deactivate Repo
Service -> Repo : 7. save(User{accountStatus="PENDING_ACTIVATION"})
activate Repo
Repo -> DB : 8. INSERT INTO users ...
activate DB
DB --> Repo : 9. savedUser
deactivate DB
Repo --> Service : 10. savedUser
deactivate Repo
Service -> Otp : 11. generateAndSend(userId, REGISTER)
activate Otp
Otp -> OtpRepo : 12. save(OtpVerification{codeHash, expiresAt})
activate OtpRepo
OtpRepo -> DB : 13. INSERT INTO otp_verifications ...
activate DB
DB --> OtpRepo : 14. saved
deactivate DB
OtpRepo --> Otp : 15. void
deactivate OtpRepo
Otp --> Service : 16. void
deactivate Otp
Service -> Audit : 17. log(OTP_SENT)
activate Audit
Audit --> Service : 18. void
deactivate Audit
Service --> Controller : 19. AuthResponse{status=PENDING_ACTIVATION}
deactivate Service
Controller --> UI : 20. HTTP 201 Created
deactivate Controller
UI --> Client : 20a. Display HTTP 201 Created
deactivate UI

== UC-02 Verify OTP ==
Client -> UI : 21. Submit request
activate UI
UI -> Controller : 21a. POST /api/v1/auth/verify-otp\n{identifier, code}
activate Controller
Controller -> Service : 22. verifyOtp(request)
activate Service
Service -> Otp : 23. validate(code, purpose=REGISTER)
activate Otp
Otp -> OtpRepo : 24. findLatestByUserAndPurpose(userId, REGISTER)
activate OtpRepo
OtpRepo -> DB : 25. SELECT ... FROM otp_verifications WHERE ...
activate DB
DB --> OtpRepo : 26. otpRecord{expiresAt, attempts}
deactivate DB
OtpRepo --> Otp : 27. otpRecord
deactivate OtpRepo
Otp --> Service : 28. verified=true
deactivate Otp
Service -> Repo : 29. setAccountStatus(user, "ACTIVE")
activate Repo
Repo -> DB : 30. UPDATE users SET account_status='ACTIVE'
activate DB
DB --> Repo : 31. updated
deactivate DB
Repo --> Service : 32. void
deactivate Repo
Service -> Audit : 33. log(OTP_VERIFIED)
activate Audit
Audit --> Service : 34. void
deactivate Audit
Service -> Audit : 35. log(USER_REGISTRATION_COMPLETED)
activate Audit
Audit --> Service : 36. void
deactivate Audit
Service --> Controller : 37. AuthResponse{status=ACTIVE}
deactivate Service
Controller --> UI : 38. HTTP 200 OK
deactivate Controller
UI --> Client : 38a. Display HTTP 200 OK
deactivate UI

== UC-03 Log In ==
Client -> UI : 39. Submit request
activate UI
UI -> Controller : 39a. POST /api/v1/auth/login\n{identifier, password}
activate Controller
Controller -> Service : 40. login(request)
activate Service
Service -> Repo : 41. findByEmailOrPhone(identifier)
activate Repo
Repo -> DB : 42. SELECT ... FROM users
activate DB
DB --> Repo : 43. user{accountStatus="ACTIVE", locked=false}
deactivate DB
Repo --> Service : 44. user
deactivate Repo
Service -> Service : 45. check accountStatus=="ACTIVE" AND !locked AND suspendedUntil==null
Service -> Service : 46. passwordEncoder.matches(password, passwordHash)
Service -> RefreshRepo : 47. save(RefreshToken), update lastLoginAt
activate RefreshRepo
RefreshRepo -> DB : 48. INSERT INTO refresh_tokens ...
activate DB
DB --> RefreshRepo : 49. saved
deactivate DB
RefreshRepo --> Service : 50. void
deactivate RefreshRepo
Service -> Audit : 51. log(LOGIN)
activate Audit
Audit --> Service : 52. void
deactivate Audit
Service --> Controller : 53. AuthResponse{accessToken, refreshToken}
deactivate Service
Controller --> UI : 54. HTTP 200 OK
deactivate Controller
UI --> Client : 54a. Display HTTP 200 OK
deactivate UI

== UC-04 Log Out ==
Client -> UI : 55. Submit request
activate UI
UI -> Controller : 55a. POST /api/v1/auth/logout\nAuthorization: Bearer <token>
activate Controller
Controller -> Service : 56. logout(userId, refreshToken)
activate Service
Service -> RefreshRepo : 57. revoke(refreshToken)
activate RefreshRepo
RefreshRepo -> DB : 58. UPDATE refresh_tokens SET revoked=true
activate DB
DB --> RefreshRepo : 59. updated
deactivate DB
RefreshRepo --> Service : 60. void
deactivate RefreshRepo
Service -> Audit : 61. log(LOGOUT)
activate Audit
Audit --> Service : 62. void
deactivate Audit
Service --> Controller : 63. void
deactivate Service
Controller --> UI : 64. HTTP 204 No Content
deactivate Controller
UI --> Client : 64a. Display HTTP 204 No Content
deactivate UI

@enduml
```

**Hình 2 — Sequence Diagram: Account Registration → OTP → Login → Logout (Main Flow)**

> Ghi chú luồng phụ: `POST /forgot-password` phát hành `PasswordResetToken` một-lần
> dùng qua kênh đã đăng ký (không tiết lộ tài khoản có tồn tại hay không — BR-ACCOUNT-05);
> `POST /reset-password` xác thực token rồi ghi mật khẩu mới và thu hồi toàn bộ session
> đang hoạt động (BR-SECURITY-04); `PUT /change-password` yêu cầu xác nhận mật khẩu hiện
> tại trước khi ghi mật khẩu mới (BR-SECURITY-05). Cả ba đều tái sử dụng
> `AuthServiceImpl`/`ForgotPasswordServiceImpl` ở trên, không tạo entity mới.


## 4. Business Rules Applied

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
