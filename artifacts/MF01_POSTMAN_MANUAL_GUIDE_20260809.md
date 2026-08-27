# MF-01 — Hướng dẫn nhập request trực tiếp trong Postman

Base URL đang sử dụng: `http://localhost:8080`

## Cách nhập chung

1. Mở Postman và chọn **New → HTTP Request**.
2. Ở ô bên trái URL, chọn đúng phương thức được ghi trong từng Test Case: `GET`, `POST`, `PUT`, `PATCH` hoặc `DELETE`.
3. Dán dòng **URL nhập vào để Send** vào ô URL.
4. Nếu có body: chọn **Body → raw → JSON**, rồi dán JSON được cung cấp.
5. Nếu cần đăng nhập: chọn **Authorization → Bearer Token** và nhập giá trị của `{{accessToken}}`, hoặc giữ header Authorization đã ghi.
6. Nhấn **Send** và đối chiếu status thực tế với **Status mong đợi**.

## Các biến cần chuẩn bị

- `{{phone}}`: số điện thoại Việt Nam chưa đăng ký, ví dụ `0912345678`.
- `{{email}}`: email test chưa đăng ký.
- `{{password}}`, `{{newPassword}}`: mật khẩu thỏa policy.
- `{{otp}}`: lấy từ dòng `[MOCK SMS]` trong backend log.
- `{{accessToken}}`, `{{refreshToken}}`: lấy từ response Login/Verify OTP/Refresh.
- `{{consentId}}`: lấy từ trường `data.id` của response Grant Consent.
- Các ID khác như `{{sessionId}}`, `{{userId}}`, `{{eventId}}`: lấy từ response của request View/List tương ứng.

## 01 - Register

### MF01-ADD-001 - Đăng ký tài khoản mới bằng email hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Nguyen An",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `201`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-002 - Đăng ký tài khoản mới bằng số điện thoại Việt Nam hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Tran Binh",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

**Status mong đợi:** `201`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-003 - Đăng ký tài khoản không chọn role để nhận trạng thái UNASSIGNED.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Le Chi",
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `201`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-004 - Tạo CareBridge account mới qua Firebase federated authentication.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/federated`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "idToken": "{{firebaseIdToken}}",
  "deviceInfo": "CareBridge Flutter test"
}
```

**Status mong đợi:** `201`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-005 - Liên kết Google identity vào tài khoản đang đăng nhập.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/identities/google`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "idToken": "{{firebaseIdToken}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-001 - Từ chối đăng ký khi thiếu cả email và phone.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Missing Contact",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-002 - Từ chối đăng ký với password yếu.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Weak Password",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-003 - Từ chối đăng ký với email sai format.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Invalid Email",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-004 - Từ chối đăng ký với phone Việt Nam sai format.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Invalid Phone",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-005 - Từ chối đăng ký trùng email.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Duplicate",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `409`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-006 - Từ chối self-registration bằng role quản trị.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Invalid Role",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "SYSTEM_ADMIN"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-001 - Rollback đăng ký khi SMTP gửi OTP lỗi.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "SMTP Failure",
  "email": "{{email}}",
  "password": "{{password}}",
  "role": "MOTHER"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-002 - Rollback đăng ký khi SMS gửi OTP lỗi.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/register`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "SMS Failure",
  "phone": "{{phone}}",
  "password": "{{password}}",
  "role": "FAMILY"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-003 - Firebase verifier timeout trả lỗi an toàn.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/federated`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "idToken": "{{firebaseIdToken}}",
  "deviceInfo": "Web test"
}
```

**Status mong đợi:** `503`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 02 - Login

### MF01-VALID-011 - Từ chối login khi gửi cả phone và email.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "phone": "{{phone}}",
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-001 - Login bằng email và password hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-002 - Login bằng phone và password hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "phone": "{{phone}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-003 - Login sai password trả thông báo trung lập.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-004 - Login bằng account chưa verify bị từ chối.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-005 - Login bằng account bị admin lock bị từ chối.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-006 - Login bằng account đang suspended bị từ chối.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-007 - Khóa tạm account sau giới hạn login sai.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-008 - Login thành công sau khi temporary lock hết hạn.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-013 - CORS chỉ cho phép exact configured Web origin.

**Sử dụng phương thức nào:** `OPTIONS`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-008 - Refresh token không lưu raw value trong auth_sessions.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/login`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "password": "{{password}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 03 - OTP

### MF01-UPDATE-001 - Xác thực OTP hợp lệ để kích hoạt tài khoản.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/verify-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-002 - Resend OTP hợp lệ và vô hiệu mã OTP trước đó.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/resend-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-007 - Từ chối OTP không đủ 6 chữ số.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/verify-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-008 - Từ chối OTP sai và giảm số lần thử.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/verify-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-009 - Từ chối OTP đã hết hạn.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/verify-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-010 - Từ chối resend OTP khi gửi đồng thời phone và email.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/resend-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "phone": "{{phone}}",
  "email": "{{email}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-004 - Hai request verify cùng OTP chỉ có một request thắng.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/verify-otp`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "otp": "{{otp}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 04 - Forgot Password

### MF01-UPDATE-016 - Đổi mật khẩu hợp lệ cho user đang đăng nhập.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/change-password`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "oldPassword": "{{password}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-017 - Đặt lại mật khẩu bằng recovery token hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/reset-password`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "token": "{{resetToken}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-012 - Từ chối forgot password với contact rỗng.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/forgot-password`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "contact": ""
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-013 - Từ chối reset password khi confirm không khớp.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/reset-password`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "token": "{{resetToken}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-014 - Từ chối change password khi current password sai.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/change-password`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "oldPassword": "{{password}}",
  "newPassword": "{{newPassword}}",
  "confirmPassword": "{{newPassword}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-001 - Forgot password không tiết lộ account có tồn tại.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/forgot-password`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "contact": "unknown@example.test"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 05 - Refresh Token

### MF01-AUTH-009 - Refresh token hợp lệ được rotate.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/refresh`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-010 - Replay refresh token cũ sau rotation bị từ chối.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/refresh`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-011 - Refresh token hết hạn bị từ chối.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/refresh`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-005 - Hai request refresh đồng thời không tạo hai refresh chain hợp lệ.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/refresh`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 06 - Logout

### MF01-REVOKE-001 - Logout bằng refresh token của session hiện tại.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/logout`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "refreshToken": "{{refreshToken}}"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-002 - Logout không gửi refresh token, dùng JWT sid hiện tại.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/logout`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-007 - Vô hiệu hóa tài khoản cá nhân bằng password đúng.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/deactivate`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "User requested deactivation"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-012 - System Admin không thể tự deactivate.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/deactivate`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "self deactivate"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-011 - Deactivation thu hồi đồng thời session, refresh token và FCM token.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/deactivate`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "confirmPassword": "{{newPassword}}",
  "reason": "Integration revocation check"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 07 - Profile

### MF01-VIEW-004 - Xem hồ sơ tài khoản riêng tư bằng access token hợp lệ.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-005 - Xem hồ sơ mở rộng của chính user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-006 - Xem privacy settings dùng cho khả năng hiển thị hồ sơ cộng đồng.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/privacy-settings/me`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-003 - Cập nhật name và avatarUrl qua auth profile endpoint.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "name": "Updated Name",
  "avatarUrl": "https://example.test/avatar.png"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-004 - Cập nhật một trường area qua extended profile endpoint.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "area": "Ho Chi Minh City"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-005 - Cập nhật nhiều trường hồ sơ mở rộng hợp lệ.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "displayName": "Mai Anh",
  "avatarUrl": "https://example.test/mai.png",
  "phoneNumber": "0912345678",
  "dateOfBirth": "1995-05-20",
  "area": "Da Nang"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-006 - Cập nhật privacy settings hợp lệ.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/privacy-settings/me`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "profileVisibility": "PRIVATE",
  "locationSharingEnabled": false,
  "analyticsConsent": false,
  "dataExportOptOut": true
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-015 - Từ chối extended profile có dateOfBirth trong tương lai.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "dateOfBirth": "2099-01-01"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-016 - Từ chối displayName chứa ký tự HTML nguy hiểm.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "displayName": "{{script}}alert(1){{script}}"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-AUTH-012 - Protected endpoint từ chối access token của session đã revoke.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-001 - Protected profile endpoint từ chối khi không có token.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/profile`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-002 - Profile response không lộ password hash, OTP hoặc token.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/auth/profile`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 08 - Notification Preference

### MF01-VIEW-007 - Xem notification preferences của chính user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/users/me/notification-preferences`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-008 - Xem danh sách notification có dữ liệu của chính user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/me?page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-009 - Xem empty state khi user chưa có notification.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/me?page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-007 - Đăng ký FCM device token cho chính user.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/device-token`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "token": "{{fcmDeviceTokenFromTestFixture}}",
  "platform": "ANDROID"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-010 - System Admin gửi notification hợp lệ đến user có device token.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/send`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "recipientUserId": "{{userId}}",
  "type": "COMMUNITY_REPLY",
  "title": "New reply",
  "body": "A new reply is available.",
  "referenceId": "{{referenceId}}",
  "referenceType": "COMMUNITY_QUESTION"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-007 - Cập nhật notification preference cho một loại notification.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/users/me/notification-preferences`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

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

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-008 - Đánh dấu một notification chưa đọc là đã đọc.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/{{notificationId}}/read`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-009 - Đánh dấu lại notification đã đọc theo tính idempotent.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/{{alreadyReadNotificationId}}/read`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-010 - Đánh dấu tất cả notification chưa đọc của chính user.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/read-all`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-006 - Hủy đăng ký FCM device token của chính user.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/device-token?token={{fcmDeviceToken}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-FILTER-001 - Lọc notification theo type hợp lệ.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/me?type=COMMUNITY_REPLY&page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-SORT-001 - Notification list sắp xếp mới nhất trước.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/me?page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-001 - Chuyển trang notification sau khi lọc type.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/me?type=REMINDER&page=1&size=5`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-018 - Từ chối notification preference có enum không tồn tại.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/users/me/notification-preferences`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

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

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-006 - User không thể mark notification của tài khoản khác.

**Sử dụng phương thức nào:** `PUT`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/{{otherUsersNotificationId}}/read`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{}
```

**Status mong đợi:** `404`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-008 - FCM lỗi vẫn lưu notification FAILED và audit tương ứng.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/notifications/send`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "recipientUserId": "{{userId}}",
  "type": "REMINDER",
  "title": "Reminder",
  "body": "Test reminder"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 09 - Session

### MF01-VIEW-010 - Xem danh sách các phiên đăng nhập đang hoạt động của chính user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-012 - System Admin xem session history của một tài khoản.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{userId}}/sessions?page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-003 - Thu hồi một session khác của chính user.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions/{{otherSessionId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-004 - Thu hồi tất cả session khác, giữ current session.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-010 - Từ chối revoke chính current session qua session endpoint.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions/{{currentSessionId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-SORT-002 - Paged session list sắp xếp lastActivityAt giảm dần.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions/paged?page=0&size=10`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-002 - Lấy trang giữa của session list.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions/paged?page=1&size=2`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-004 - Phân trang session history của target user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{userId}}/sessions?page=1&size=2`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-002 - Session list từ chối anonymous caller.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-004 - User không thể revoke session của tài khoản khác.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/sessions/{{otherUsersSessionId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 10 - Consent

### MF01-ADD-006 - Cấp consent hợp lệ có dataType, purpose, recipient, scope và expiry.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "pregnancy-summary",
  "expiryDays": 30
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-005 - Thu hồi consent đang active của chính user.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants/{{consentId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-REVOKE-009 - Từ chối thu hồi consent đã revoke lần hai.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants/{{revokedConsentId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-017 - Từ chối consent có expiryDays bằng 0.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "SHARE",
  "recipient": "family@example.test",
  "scope": "summary",
  "expiryDays": 0
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-003 - Consent list từ chối anonymous caller.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `401`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-005 - User không thể revoke consent của tài khoản khác.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants/{{otherUsersConsentId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `404`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-009 - Consent revocation chặn protected read kế tiếp.

**Sử dụng phương thức nào:** `DELETE`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants/{{activeConsentId}}`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-004 - Consent grant mặc định expiry hữu hạn khi expiryDays bỏ trống.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "dataType": "SENSITIVE_DATA",
  "purpose": "VIEW",
  "recipient": "expert@example.test",
  "scope": "selected-records"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-005 - Consent list chỉ trả grant của chính user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/consent/grants`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 11 - RBAC

### MF01-VIEW-011 - System Admin xem chi tiết một tài khoản.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{userId}}`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VIEW-013 - System Admin xem activity history của một tài khoản.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{userId}}/activity?page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ADD-008 - System Admin tạo staff account MODERATOR.

**Sử dụng phương thức nào:** `POST`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/staff-accounts`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "email": "{{email}}",
  "phone": "{{phone}}",
  "name": "Moderator QA",
  "role": "MODERATOR"
}
```

**Status mong đợi:** `201`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-011 - System Admin khóa tài khoản khác với reason hợp lệ.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "locked": true,
  "reason": "Confirmed abuse during QA test"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-012 - System Admin mở khóa tài khoản khác với CSKH ticket.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "locked": false,
  "reason": "Issue resolved",
  "cskhTicketId": "CSKH-TEST-001"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-013 - System Admin disable tài khoản khác.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "enabled": false,
  "reason": "Administrative disable"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-UPDATE-014 - System Admin đổi role giữa các staff governance role.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{staffUserId}}/role`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "newRole": "CONTENT_ADMIN",
  "lockAccessRights": false,
  "reason": "Approved staff reassignment"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-SEARCH-001 - Tìm user theo email chính xác trong admin list.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?email=target%40example.test&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-SEARCH-002 - Tìm user theo một phần tên.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?name=Anh&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-SEARCH-003 - Tìm user không có kết quả.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?email=no-match%40example.test&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-FILTER-002 - Lọc admin users theo role và trạng thái enabled.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?role=MOTHER&enabled=true&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-FILTER-003 - Lọc admin users theo locked=true.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?locked=true&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-003 - Phân trang admin user list với size vượt MAX_PAGE_SIZE.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?page=0&size=9999`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-005 - Phân trang activity history của target user.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{userId}}/activity?page=1&size=2`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-019 - Từ chối status update khi không có enabled/locked.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "reason": "No state selected"
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-VALID-020 - Từ chối lock account khi thiếu reason.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "locked": true
}
```

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-007 - Mother bị từ chối truy cập admin user list.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-010 - System Admin không thể đổi trạng thái chính mình.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{callerUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "locked": true,
  "reason": "self target"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-011 - System Admin không thể đổi role chính mình.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{callerUserId}}/role`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "newRole": "CONTENT_ADMIN",
  "reason": "self target"
}
```

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-ACCESS-013 - JWT role cũ không giữ quyền sau khi database role thay đổi.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users?page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-010 - Lỗi audit bắt buộc rollback admin status mutation.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/users/{{targetUserId}}/status`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "locked": true,
  "reason": "Audit failure injection"
}
```

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-INT-012 - Readiness phản ánh PostgreSQL outage mà không lộ endpoint actuator khác.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/actuator/health/readiness`

**Authorization:** `Không cần Authorization`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## 12 - Audit and Privacy

### MF01-VIEW-014 - System Admin/Operations xem audit log được phép.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

---

---

### MF01-SEARCH-004 - Tìm audit log theo userId và action.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?userId={{userId}}&action=LOGIN&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

### MF01-FILTER-004 - Lọc audit log theo khoảng thời gian.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?fromDate=2026-08-01T00:00:00Z&toDate=2026-08-09T23:59:59Z&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

---

### MF01-SORT-003 - Audit log sắp xếp createdAt giảm dần.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PAGE-006 - Từ chối pagination audit log không hợp lệ.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?page=-1&size=0`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `400`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

### MF01-ACCESS-008 - Moderator bị từ chối truy cập audit logs.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?page=0&size=20`

**Authorization:** `Bearer {{moderatorToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `403`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

### MF01-PRIVACY-003 - Public community display không lộ private maternal/child data.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/community/feed?page=0&size=20`

**Authorization:** `Bearer {{accessToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

### MF01-PRIVACY-006 - Audit query không trả health payload vượt nhu cầu điều tra.

**Sử dụng phương thức nào:** `GET`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs?action=VIEW_HEALTH_RECORD&page=0&size=20`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** Không có body.

**Status mong đợi:** `200`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

---

### MF01-PRIVACY-009 - Audit append-only không cho operator sửa/xóa lịch sử qua API.

**Sử dụng phương thức nào:** `PATCH`

**URL nhập vào để Send:**

`http://localhost:8080/api/v1/admin/audit-logs/{{auditId}}`

**Authorization:** `Bearer {{systemAdminToken}}`

**Body nhập trong Postman:** chọn `Body → raw → JSON`, sau đó dán:

```json
{
  "action": "LOGIN"
}
```

**Status mong đợi:** `405`

**Cách Send:** kiểm tra Method + URL + Authorization + Body ở trên, sau đó nhấn nút **Send**.

---

## Tổng kết

Hướng dẫn này chứa 134 HTTP request thuộc MF-01. Các trường hợp UI-only hoặc chức năng chỉ có trong Report 3 nhưng chưa có endpoint không được tạo request giả.
