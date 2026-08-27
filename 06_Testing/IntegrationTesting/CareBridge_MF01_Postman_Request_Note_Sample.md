1_MF01-ACTION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "mother@carebridge.dev",
  "password": "Test@1234"
}
```


2_MF01-ACTION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/refresh](http://localhost:8080/api/v1/auth/refresh)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "refreshToken": "THAY_REFRESH_TOKEN"
}
```


3_MF01-ACTION-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/resend-otp](http://localhost:8080/api/v1/auth/resend-otp)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "email":  "THAY_EMAIL_TAI_KHOAN_DANG_CHO_XAC_MINH"
}
```


4_MF01-ACTION-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/verify-otp](http://localhost:8080/api/v1/auth/verify-otp)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "email":  "THAY_EMAIL_TAI_KHOAN_DANG_CHO_XAC_MINH",
    "otp":  "THAY_OTP_6_CHU_SO"
}
```


6_MF01-CREATE-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/staff-accounts](http://localhost:8080/api/v1/admin/staff-accounts)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "email":  "qa.moderator.20260809.001@carebridge.dev",
    "name":  "QA Moderator MF01",
    "role":  "MODERATOR"
}
```


7_MF01-CREATE-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/federated](http://localhost:8080/api/v1/auth/federated)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "idToken":  "THAY_FIREBASE_GOOGLE_ID_TOKEN",
    "deviceInfo":  "Postman MF01"
}
```


8_MF01-CREATE-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/forgot-password](http://localhost:8080/api/v1/auth/forgot-password)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "contact": "mother@carebridge.dev"
}
```


9_MF01-CREATE-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/identities/google](http://localhost:8080/api/v1/auth/identities/google)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "idToken":  "THAY_FIREBASE_GOOGLE_ID_TOKEN"
}
```


10_MF01-CREATE-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/logout](http://localhost:8080/api/v1/auth/logout)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "refreshToken": "THAY_REFRESH_TOKEN"
}
```


11_MF01-CREATE-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/reset-password](http://localhost:8080/api/v1/auth/reset-password)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "token": "THAY_RESET_TOKEN",
  "newPassword": "NewTest@1234",
  "confirmPassword": "NewTest@1234"
}
```


12_MF01-CREATE-008

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/consent/grants](http://localhost:8080/api/v1/consent/grants)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "dataType":  "HEALTH_RECORD",
    "purpose":  "VIEW",
    "recipient":  "CareBridge Family",
    "scope":  "Pregnancy health record",
    "expiryDays":  30
}
```


13_MF01-CREATE-009

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/firebase/custom-token](http://localhost:8080/api/v1/firebase/custom-token)

Body: Không có body.


14_MF01-CREATE-010

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/device-token](http://localhost:8080/api/v1/notifications/device-token)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "token":  "carebridge-mf01-device-token",
    "platform":  "ANDROID"
}
```


15_MF01-DELETE-001

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/deactivate](http://localhost:8080/api/v1/auth/deactivate)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "confirmPassword": "Test@1234",
  "reason": "qa_reason_01"
}
```


16_MF01-DELETE-002

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/consent/grants/THAY_CONSENT_ID](http://localhost:8080/api/v1/consent/grants/THAY_CONSENT_ID)

Body: Không có body.


17_MF01-DELETE-003

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/device-token](http://localhost:8080/api/v1/notifications/device-token)

Body: Không có body.


18_MF01-DELETE-004

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/sessions](http://localhost:8080/api/v1/sessions)

Body: Không có body.


19_MF01-DELETE-005

Method:DELETE

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/sessions/THAY_SESSION_ID](http://localhost:8080/api/v1/sessions/THAY_SESSION_ID)

Body: Không có body.


20_MF01-LOGIN-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "admin@carebridge.dev",
  "password": "Test@1234"
}
```


21_MF01-LOGIN-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "expert@carebridge.dev",
  "password": "Test@1234"
}
```


22_MF01-LOGIN-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "family@carebridge.dev",
  "password": "Test@1234"
}
```


23_MF01-LOGIN-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "mother@carebridge.dev",
  "password": "Test@1234"
}
```


24_MF01-PROFILE-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile/me](http://localhost:8080/api/v1/profile/me)

Body: Không có body.


25_MF01-PROFILE-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile/me](http://localhost:8080/api/v1/profile/me)

Body: Không có body.


26_MF01-PROFILE-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile/me](http://localhost:8080/api/v1/profile/me)

Body: Không có body.


27_MF01-PROFILE-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile/me](http://localhost:8080/api/v1/profile/me)

Body: Không có body.


28_MF01-RBAC-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


29_MF01-RBAC-002

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


30_MF01-RBAC-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


31_MF01-RBAC-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


32_MF01-SEARCH-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


34_MF01-SEARCH-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users](http://localhost:8080/api/v1/admin/users)

Body: Không có body.


35_MF01-SEARCH-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/consent/grants](http://localhost:8080/api/v1/consent/grants)

Body: Không có body.


36_MF01-UNAUTHORIZED-001

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/audit-logs](http://localhost:8080/api/v1/admin/audit-logs)

Body: Không có body.


37_MF01-UPDATE-001

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users/THAY_USER_ID/role](http://localhost:8080/api/v1/admin/users/THAY_USER_ID/role)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "newRole":  "MODERATOR",
    "lockAccessRights":  false,
    "reason":  "MF01 role update evidence"
}
```


38_MF01-UPDATE-002

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users/THAY_USER_ID/status](http://localhost:8080/api/v1/admin/users/THAY_USER_ID/status)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "enabled":  true,
    "locked":  false,
    "reason":  "MF01 status update evidence",
    "cskhTicketId":  "MF01-TICKET-001"
}
```


39_MF01-UPDATE-003

Method:PATCH

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile](http://localhost:8080/api/v1/profile)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "displayName":  "Mother Test",
    "avatarUrl":  "https://example.com/avatar.png",
    "dateOfBirth":  "1995-10-07",
    "area":  "Ha Noi"
}
```


41_MF01-UPDATE-005

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/system-configuration](http://localhost:8080/api/v1/admin/system-configuration)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "aiModerationEnabled":  true,
    "maintenanceModeEnabled":  false,
    "rowVersion":  1
}
```


42_MF01-UPDATE-006

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/change-password](http://localhost:8080/api/v1/auth/change-password)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "oldPassword": "Test@1234",
  "newPassword": "NewTest@1234",
  "confirmPassword": "NewTest@1234"
}
```


43_MF01-UPDATE-007

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/profile](http://localhost:8080/api/v1/auth/profile)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "name": "qa_name_01",
  "avatarUrl": "https://example.invalid/qa-resource"
}
```


44_MF01-UPDATE-008

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/role](http://localhost:8080/api/v1/auth/role)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "role": "MOTHER"
}
```


45_MF01-UPDATE-009

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/privacy-settings/me](http://localhost:8080/api/v1/privacy-settings/me)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "profileVisibility": "PUBLIC",
  "locationSharingEnabled": true,
  "analyticsConsent": true,
  "dataExportOptOut": true
}
```


46_MF01-UPDATE-010

Method:PUT

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/users/me/notification-preferences](http://localhost:8080/api/v1/users/me/notification-preferences)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
    "preferences":  [
                        {
                            "notificationType":  "REMINDER",
                            "pushEnabled":  true,
                            "emailEnabled":  true,
                            "inAppEnabled":  true
                        }
                    ],
    "appointmentReminderDefaults":  [
                                        -1440,
                                        -30,
                                        0,
                                        15
                                    ]
}
```


47_MF01-VALIDATION-001

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "mother@carebridge.dev",
  "phone": "0901234567",
  "password": "Test@1234"
}
```


48_MF01-VALIDATION-002

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "",
  "password": ""
}
```


49_MF01-VALIDATION-003

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "",
  "password": "Test@1234"
}
```


50_MF01-VALIDATION-004

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "mother@carebridge.dev",
  "password": ""
}
```


51_MF01-VALIDATION-005

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "qa_missing_account@example.invalid",
  "password": "Test@1234"
}
```


52_MF01-VALIDATION-006

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "mother@carebridge.dev",
  "password": "Wrong@1234"
}
```


53_MF01-VALIDATION-007

Method:POST

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/login](http://localhost:8080/api/v1/auth/login)

Body: chọn `Body` → `raw` → `JSON`, sau đó nhập chính xác JSON sau:

```json
{
  "email": "qa_invalid_email",
  "password": "Test@1234"
}
```


56_MF01-VIEW-003

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/system-configuration](http://localhost:8080/api/v1/admin/system-configuration)

Body: Không có body.


57_MF01-VIEW-004

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users/THAY_USER_ID](http://localhost:8080/api/v1/admin/users/THAY_USER_ID)

Body: Không có body.


58_MF01-VIEW-005

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users/THAY_USER_ID/activity](http://localhost:8080/api/v1/admin/users/THAY_USER_ID/activity)

Body: Không có body.


59_MF01-VIEW-006

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/admin/users/THAY_USER_ID/sessions](http://localhost:8080/api/v1/admin/users/THAY_USER_ID/sessions)

Body: Không có body.


60_MF01-VIEW-007

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/identities/google](http://localhost:8080/api/v1/auth/identities/google)

Body: Không có body.


61_MF01-VIEW-008

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/auth/profile](http://localhost:8080/api/v1/auth/profile)

Body: Không có body.


62_MF01-VIEW-009

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/notifications/me](http://localhost:8080/api/v1/notifications/me)

Body: Không có body.


63_MF01-VIEW-010

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/privacy-settings/me](http://localhost:8080/api/v1/privacy-settings/me)

Body: Không có body.


64_MF01-VIEW-011

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/profile](http://localhost:8080/api/v1/profile)

Body: Không có body.


65_MF01-VIEW-012

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/sessions](http://localhost:8080/api/v1/sessions)

Body: Không có body.


66_MF01-VIEW-013

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/sessions/paged](http://localhost:8080/api/v1/sessions/paged)

Body: Không có body.


67_MF01-VIEW-014

Method:GET

Khi dùng cấu hình local hiện tại, Postman sẽ resolve URL thành: [http://localhost:8080/api/v1/users/me/notification-preferences](http://localhost:8080/api/v1/users/me/notification-preferences)

Body: Không có body.
