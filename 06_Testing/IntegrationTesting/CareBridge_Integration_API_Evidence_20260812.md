# CareBridge Integration API Evidence Catalog

> Postman-focused catalog. Each case contains only the request method, executable URL, raw JSON body, and expected response/result. `NEEDS_CONFIRMATION` means the current code does not prove a safe executable value.

**Postman Quick Start**

- **Fastest mode (recommended):** import `CareBridge_Integration_One_Click_20260812.postman_collection.json` and `CareBridge_Local_One_Click_20260812.postman_environment.json` from this folder, then select the environment named **CareBridge Local One Click 20260812**.
- Open the required TC in the imported collection, check its ready-made Method/URL/raw JSON, and click **Send**. Protected requests automatically log in with the correct QA account, obtain a new access token, and attach it as a Bearer Token. Do not manually copy an access token.
- The collection also derives the current `sessionId` from the newly issued JWT, so session detail/revoke requests do not use an expired or foreign session ID.
- Request descriptions state the expected result. IDs that already exist in Supabase were placed in the environment. Items prefixed **[RUNTIME VALUE REQUIRED]** are the unavoidable exceptions: Firebase ID token, email OTP, password-reset/appeal token, or internal service key must come from the real issuing system and cannot be fabricated safely.
- If testing directly from this Markdown instead of the imported collection, set `baseUrl = http://localhost:8080`, `aiTriageBaseUrl = http://localhost:8001`, and `exerciseBaseUrl = http://localhost:8002`, then follow the detailed **Postman Execution** block.
- All QA accounts below use password `Test@1234` and were verified against the running API on 2026-08-12.

| Purpose | Login |
| --- | --- |
| Mother | `mother@carebridge.dev` or phone `0908000007` |
| Family | `family@carebridge.dev` |
| Expert | `expert2@carebridge.dev` |
| Moderator | `moderator@carebridge.dev` |
| Content Admin | `content@carebridge.dev` |
| System Admin | `admin@carebridge.dev` |
| Locked-account negative test | `locked.qa@carebridge.dev` |
| Deactivated-account negative test | `deactivated.qa@carebridge.dev` |
| Pending-verification negative test | `pending.qa@carebridge.dev` |
| Password-reset flow | `reset.qa@carebridge.dev` |
| Change-password flow | `change-password.qa@carebridge.dev` |
- UUIDs and ordinary request values below are concrete values from the Supabase fixture set on 2026-08-12. Tests that delete or change records can invalidate later cases, so run mutation/destructive cases last.
- Values written as `<PASTE_..._HERE>` are intentionally runtime-only OTPs, reset/appeal/continuation tokens, Firebase ID tokens, or device tokens. They cannot be safely hard-coded; obtain them from the immediately preceding API response, email, Firebase login, or client device.
- Every executable request below includes a **Postman Execution** block. Follow its numbered Method, Authorization, runtime-ID, Params/Headers, Body, Send, and evidence instructions in order. The seven client-only/unproven cases instead contain explicit **Manual Execution** or **Execution Status** instructions.

# Authentication_Access_Control

## Register Account

### IT-AUTH-001 - A guest can register a valid account and receive an OTP challenge

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/register (default: `http://localhost:8080/api/v1/auth/register`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA 20260812",
  "email": "carebridge.qa.20260812@example.com",
  "password": "Test@1234",
  "role": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Registration reaches AuthController and AuthService. 2. A pending user and OTP record are persisted in PostgreSQL. 3. OTP delivery is requested. 4. The client receives the OTP verification step only after successful processing.

### IT-AUTH-002 - Registration rejects an already registered email or phone without creating a duplicate account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/register (default: `http://localhost:8080/api/v1/auth/register`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge Existing Account",
  "password": "Test@1234",
  "email": "mother@carebridge.dev"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend detects the existing account through UserRepository. 2. Registration returns a validation or conflict error. 3. No additional user or OTP record is persisted.

### IT-AUTH-003 - Registration rejects a password that fails the configured password-complexity policy

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/register (default: `http://localhost:8080/api/v1/auth/register`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge Weak Password",
  "password": "123",
  "email": "carebridge.qa.weak.20260812@example.com",
  "role": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Password validation fails before account activation. 2. No valid registration is completed. 3. No false success state is shown.

### IT-AUTH-004 - Invalid registration data does not persist a pending account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/register (default: `http://localhost:8080/api/v1/auth/register`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "",
  "password": "Test@1234",
  "email": "invalid-email"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend returns a validation error. 2. No confirmed registration record is created. 3. The user remains on an actionable registration state.

### IT-AUTH-005 - A valid registration OTP activates the account and creates an authenticated session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/verify-otp (default: `http://localhost:8080/api/v1/auth/verify-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_LATEST_OTP_FROM_EMAIL_HERE>` with the latest mock-email/real-email OTP generated immediately before this request. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com",
  "otp": "<PASTE_LATEST_OTP_FROM_EMAIL_HERE>"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. OTP is validated. 2. OTP is marked as used. 3. The user status becomes active. 4. Refresh-token and user-session records are persisted. 5. Authentication tokens are returned. 6. The permitted workspace opens.

### IT-AUTH-006 - An invalid registration OTP is rejected without activating the account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/verify-otp (default: `http://localhost:8080/api/v1/auth/verify-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com",
  "otp": "000000"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. OTP validation fails. 2. The pending user remains unactivated. 3. No authenticated session is created. 4. The UI displays an OTP error or resend action.

### IT-AUTH-007 - An expired OTP cannot activate a pending account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/verify-otp (default: `http://localhost:8080/api/v1/auth/verify-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_EXPIRED_OTP_HERE>` with an OTP whose expiry has elapsed. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com",
  "otp": "<PASTE_EXPIRED_OTP_HERE>"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend rejects the expired OTP. 2. No refresh token or active session is created. 3. Account activation is not completed.

### IT-AUTH-008 - A previously consumed OTP cannot be reused

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/verify-otp (default: `http://localhost:8080/api/v1/auth/verify-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_PREVIOUSLY_USED_OTP_HERE>` with an OTP already consumed by a successful verification. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com",
  "otp": "<PASTE_PREVIOUSLY_USED_OTP_HERE>"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend detects the consumed OTP. 2. Backend rejects the second verification. 3. No duplicate activation, token, or session side effect is created.

### IT-AUTH-009 - Requesting a new OTP creates a usable replacement verification challenge without duplicating account activation

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/resend-otp (default: `http://localhost:8080/api/v1/auth/resend-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. A new verification challenge is generated. 2. A new verification challenge is delivered. 3. Successful verification activates the existing pending user rather than creating another user.

## Log In

### IT-AUTH-010 - An active user can log in with valid email credentials

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "mother@carebridge.dev",
  "password": "Test@1234"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. User lookup and password validation succeed. 2. Login state, refresh token, and current session are persisted. 3. Protected content is accessible with the returned authentication state.

### IT-AUTH-011 - An active user can log in with valid phone credentials when phone login is supported

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "phone": "0908000007",
  "password": "Test@1234"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Backend resolves the account by supported identifier. 2. Backend authenticates it. 3. Backend creates session data. 4. Backend returns the permitted workspace.

### IT-AUTH-012 - Login with an incorrect password is rejected without creating a valid session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "password": "WrongPassword@999",
  "email": "mother@carebridge.dev"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Authentication is rejected. 2. No usable refresh-token or session state is issued. 3. Protected access remains unavailable.

### IT-AUTH-013 - An account failing current account-status or lock policy cannot obtain a normal authenticated session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "locked.qa@carebridge.dev",
  "password": "Test@1234"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend applies account policy after credential lookup. 2. Backend rejects normal login. 3. No valid protected session is established.

### IT-AUTH-014 - A valid Google federated identity can be exchanged for a CareBridge authenticated session

**Required runtime data:** `<PASTE_REAL_FIREBASE_ID_TOKEN_HERE>` must be replaced with a fresh **Firebase ID token** issued by project `project-d04b488f-17fb-4ae5-b64` after Google sign-in. Do not use a CareBridge access token, Google OAuth access token, Google ID token before Firebase exchange, Firebase custom token, or the literal placeholder. To capture it quickly: open `http://127.0.0.1:5000`, open browser DevTools > Network, click **Sign in with Google**, select the `/api/v1/auth/federated` request, and copy `idToken` from its Request Payload. Use it promptly because Firebase ID tokens expire.

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/federated (default: `http://localhost:8080/api/v1/auth/federated`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_REAL_FIREBASE_ID_TOKEN_HERE>` with a fresh Firebase ID token captured after Google sign-in in the CareBridge client. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "idToken": "<PASTE_REAL_FIREBASE_ID_TOKEN_HERE>",
  "deviceInfo": "Postman on Windows 11"
}
```

**Expected Result After Send:** With a fresh Firebase ID token whose `firebase.sign_in_provider` is `google.com`: HTTP `200`, Firebase verification succeeds, the identity is found or created, and CareBridge access/refresh tokens are returned. With the placeholder, an expired/revoked token, or a token from another project: HTTP `401`, error `AUTH-FED-001`.

### IT-AUTH-015 - An invalid Firebase identity token cannot create a federated CareBridge session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/federated (default: `http://localhost:8080/api/v1/auth/federated`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "idToken": "invalid.firebase.id.token",
  "deviceInfo": "Postman on Windows 11"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Firebase verification fails. 2. No authenticated CareBridge session is persisted. 3. Protected access remains unavailable.

### IT-AUTH-016 - Logout revokes the current session and associated refresh token

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/logout (default: `http://localhost:8080/api/v1/auth/logout`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. User-session and refresh-token state are revoked in PostgreSQL. 2. Logout is audited. 3. The client returns to login. 4. The revoked credentials no longer provide protected access.

### IT-AUTH-017 - A revoked session cannot continue to access protected APIs

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/profile (default: `http://localhost:8080/api/v1/auth/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side session or token validation rejects the request. 2. Protected service and repository operations are not executed as an authenticated user.

## Reset Password

### IT-AUTH-018 - A user can initiate the supported forgot-password flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/forgot-password (default: `http://localhost:8080/api/v1/auth/forgot-password`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "contact": "reset.qa@carebridge.dev"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Forgot-password processing creates the supported reset challenge. 2. Forgot-password processing requests delivery through the configured messaging service without changing the password prematurely.

### IT-AUTH-019 - A valid password-reset challenge updates the account password through the complete client-to-database flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/reset-password (default: `http://localhost:8080/api/v1/auth/reset-password`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_RUNTIME_TOKEN_HERE>` with the token returned or delivered by the immediately preceding flow step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "token": "<PASTE_RUNTIME_TOKEN_HERE>",
  "newPassword": "NewTest@5678",
  "confirmPassword": "NewTest@5678"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Reset challenge validation succeeds. 2. The new password state is persisted. 3. Subsequent authentication accepts the new password.

### IT-AUTH-020 - An invalid or expired password-reset challenge cannot update account credentials

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/reset-password (default: `http://localhost:8080/api/v1/auth/reset-password`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "token": "invalid-or-expired-reset-token",
  "newPassword": "NewTest@5678",
  "confirmPassword": "NewTest@5678"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend rejects the reset. 2. Stored credentials remain unchanged. 3. No false reset-success state is displayed.

## Change Password

### IT-AUTH-021 - An authenticated user can change the password using the supported credential flow

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/change-password (default: `http://localhost:8080/api/v1/auth/change-password`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "oldPassword": "Test@1234",
  "newPassword": "NewTest@5678",
  "confirmPassword": "NewTest@5678"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AuthController and AuthService validate the request. 2. AuthController and AuthService persist the changed credential. 3. The new password succeeds on subsequent authentication.

### IT-AUTH-022 - Change Password rejects an incorrect current password

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/change-password (default: `http://localhost:8080/api/v1/auth/change-password`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "oldPassword": "WrongPassword@999",
  "newPassword": "NewTest@5678",
  "confirmPassword": "NewTest@5678"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Password change is rejected. 2. The persisted credential remains unchanged. 3. The original valid password continues to represent the account state.

### IT-AUTH-023 - The previous password no longer authenticates after a successful password change

**Step 1**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/change-password (default: `http://localhost:8080/api/v1/auth/change-password`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "oldPassword": "Test@1234",
  "newPassword": "NewTest@5678",
  "confirmPassword": "NewTest@5678"
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "mother@carebridge.dev",
  "password": "Test@1234"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Old credentials are rejected. 2. New credentials are accepted, confirming that the persisted authentication state was updated.

## Manage Login Sessions

### IT-AUTH-024 - An authenticated user can retrieve only their own active login sessions

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/sessions (default: `http://localhost:8080/api/v1/sessions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. SessionController and SessionService return session records scoped to the authenticated user. 2. Another user's sessions are not exposed.

### IT-AUTH-025 - A user can revoke one of their own login sessions

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/sessions/{sessionId} (default: `http://localhost:8080/api/v1/sessions/{sessionId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The selected session is persisted as revoked. 2. The current authorized session remains usable. 3. The revoked session is denied protected access.

### IT-AUTH-026 - A user can revoke all other sessions while retaining the current session

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/sessions (default: `http://localhost:8080/api/v1/sessions`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Other sessions are revoked in persistent session state. 2. The current session remains authorized according to the requested operation.

### IT-AUTH-027 - A user cannot revoke a session owned by another user

**Purpose:** Log in as two different users. Use User A's Bearer token to attempt to delete a session owned by User B, then confirm User B's session was not revoked.

**Step 1 - Log in as User B (session owner)**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)

**Authorization:** `No Auth`

**Raw JSON Body:**

```json
{
  "email": "family@carebridge.dev",
  "password": "Test@1234"
}
```

**After Send:** Expect HTTP `200`. Copy `data.accessToken` and keep it as **User B token**.

**Step 2 - Get User B's session ID**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/sessions (default: `http://localhost:8080/api/v1/sessions`)

**Authorization:** Select `Bearer Token` and paste the **User B token** from Step 1.

**Raw JSON Body:** No body. In Postman, select **Body > none**.

**After Send:** Expect HTTP `200`. In `data`, select a session with `status: "active"` and copy its `sessionId`. Keep it as **User B sessionId**. Example session verified on 2026-08-12: `c30b0e26-f35b-4355-97bb-bee7cd7ee2ea`. If this ID is no longer returned, use the current ID from this response.

**Step 3 - Log in as User A (attacker/different owner)**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)

**Authorization:** `No Auth`

**Raw JSON Body:**

```json
{
  "email": "mother@carebridge.dev",
  "password": "Test@1234"
}
```

**After Send:** Expect HTTP `200`. Copy `data.accessToken` and keep it as **User A token**.

**Step 4 - Attempt to revoke User B's session using User A's token**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/sessions/{USER_B_SESSION_ID} (example: `http://localhost:8080/api/v1/sessions/c30b0e26-f35b-4355-97bb-bee7cd7ee2ea`)

**Authorization:** Select `Bearer Token` and paste the **User A token** from Step 3. Do not use User B's token here.

**Raw JSON Body:** No body. In Postman, select **Body > none**.

**Actual Result After Send (verified 2026-08-12):** The ownership check rejects the mutation and User B's session is not revoked. The current backend returns HTTP `500`, `error: "INTERNAL_ERROR"`, because `IllegalArgumentException: Cannot revoke another user's session` is not mapped to the intended client error. Expected API semantics should be HTTP `403 Forbidden`; record the current `500` as a backend error-mapping defect, not a Postman configuration error.

**Step 5 - Prove User B's session still exists**

Repeat `GET {{baseUrl}}/api/v1/sessions` using the **User B token** from Step 1 and **Body > none**. Expect HTTP `200`; confirm the copied User B `sessionId` is still present with `status: "active"`. Capture the Step 4 rejection and Step 5 unchanged session as screenshot evidence.

**Expected Result After Send:** Security outcome passes when User A cannot revoke User B's session and Step 5 proves that session remains active. HTTP contract outcome currently has a defect: Step 4 returns `500 INTERNAL_ERROR`; the intended result should be `403 Forbidden`.

## View Profile

### IT-AUTH-028 - An authenticated user can retrieve their account profile from persistent storage

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ProfileController delegates to ProfileService. 2. The profile is loaded for the authenticated user. 3. The profile is returned to the client without exposing another user's private data.

## Update Profile

### IT-AUTH-029 - An authenticated user can update valid profile information and immediately retrieve the persisted result

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ProfileService validates the update. 2. ProfileService persists the update. 3. Reloading returns the updated values from the backend.

### IT-AUTH-030 - An invalid profile date of birth is rejected without overwriting the stored profile

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ProfileService validation rejects the request. 2. Existing persisted profile data remains unchanged.

### IT-AUTH-031 - Profile operations remain scoped to the authenticated user rather than a client-supplied foreign identity

**Meaning:** User A deliberately places User B's `userId` in the JSON body. The backend must ignore that client-supplied identity and update only the profile identified by User A's Bearer token.

**Step 1 - Log in as User B and establish User B's original profile value**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)

**Authorization:** Select **No Auth**.

**Raw JSON Body:**

```json
{
  "email": "profile-b.qa@carebridge.dev",
  "password": "Test@1234"
}
```

**After Send:** Expect HTTP `200`. Copy `data.accessToken` as **User B token** and copy `data.user.id` as **User B ID**. The seeded User B ID is `2aff3c19-1912-5ce9-9ce7-56df37bc3eec`.

Send `PATCH http://localhost:8080/api/v1/profile` using **User B token**, with **Body > raw > JSON**:

```json
{
  "area": "Can Tho - User B"
}
```

Expect HTTP `200` and `data.area: "Can Tho - User B"`.

**Step 2 - Log in as User A**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)

**Authorization:** Select **No Auth**.

**Raw JSON Body:**

```json
{
  "email": "profile-a.qa@carebridge.dev",
  "password": "Test@1234"
}
```

**After Send:** Expect HTTP `200`. Copy `data.accessToken` as **User A token**. The seeded User A ID is `ad76f1fe-d0fb-5b2d-85d5-864bf824ffce`.

**Step 3 - Attempt the foreign-identity profile update**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)

**Authorization:** Select **Bearer Token** and paste **User A token**. Do not use User B token.

**Raw JSON Body:**

```json
{
  "userId": "2aff3c19-1912-5ce9-9ce7-56df37bc3eec",
  "area": "Ho Chi Minh - User A"
}
```

**After Send:** Expect HTTP `200`. Although the body contains User B's ID, the response must contain User A's ID `ad76f1fe-d0fb-5b2d-85d5-864bf824ffce` and `area: "Ho Chi Minh - User A"`. This proves authorization came from the Bearer token rather than the body.

**Step 4 - Prove User B was not modified**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/profile (default: `http://localhost:8080/api/v1/profile`)

**Authorization:** Replace the token with **User B token** from Step 1.

**Raw JSON Body:** No body. Select **Body > none**.

**After Send:** Expect HTTP `200`, `data.userId: "2aff3c19-1912-5ce9-9ce7-56df37bc3eec"`, and `data.area: "Can Tho - User B"`. User B's value must remain unchanged.

**Expected Result After Send:** PASS when Step 3 updates only User A despite the foreign `userId`, and Step 4 proves User B remains unchanged. Capture Step 3 and Step 4 responses as evidence.

## Manage Notifications & Devices

### IT-AUTH-032 - An authenticated user can retrieve their notification records from PostgreSQL

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/me (default: `http://localhost:8080/api/v1/notifications/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `type = ARTICLE` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. NotificationService queries notifications by authenticated user. 2. NotificationService returns only permitted notification records.

### IT-AUTH-033 - Marking one owned notification as read updates its persisted state

**Step 1**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/{notificationId}/read (default: `http://localhost:8080/api/v1/notifications/{notificationId}/read`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{notificationId}` with the real value from the preceding create/list response for `notificationId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/me (default: `http://localhost:8080/api/v1/notifications/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `type = ARTICLE` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The notification owned by the user is updated through NotificationService. 2. The notification owned by the user appears as read after reload.

### IT-AUTH-034 - Mark All as Read updates all unread notifications belonging to the authenticated user

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/read-all (default: `http://localhost:8080/api/v1/notifications/read-all`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. All applicable notification records for the user are updated. 2. Notifications owned by other users remain unchanged.

### IT-AUTH-035 - A user can register the current device token for push notifications

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/device-token (default: `http://localhost:8080/api/v1/notifications/device-token`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `<PASTE_RUNTIME_TOKEN_HERE>` with the token returned or delivered by the immediately preceding flow step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "token": "<PASTE_RUNTIME_TOKEN_HERE>",
  "platform": "ANDROID"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Device token registration is associated with the authenticated user. 2. Notification integration can target the registered device.

### IT-AUTH-036 - Deregistering a device token stops that token from remaining registered for the authenticated user

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/device-token (default: `http://localhost:8080/api/v1/notifications/device-token`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `token = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. NotificationService removes or deactivates the specified token for the user. 2. The client reflects the deregistered state.

### IT-AUTH-037 - A user cannot mark another user's notification as read

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/{notificationId}/read (default: `http://localhost:8080/api/v1/notifications/{notificationId}/read`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{notificationId}` with the real value from the preceding create/list response for `notificationId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository lookup is scoped by notification ID and authenticated user. 2. The operation is rejected. 3. User B's record is unchanged.

## Manage Consent & Privacy Settings

### IT-AUTH-038 - An authenticated user can retrieve their current consent grants

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consent/grants (default: `http://localhost:8080/api/v1/consent/grants`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Consent records are loaded from ConsentGrantRepository by authenticated user. 2. Consent records are displayed without exposing another user's consent data.

### IT-AUTH-039 - Granting supported consent persists the consent scope for the authenticated user

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consent/grants (default: `http://localhost:8080/api/v1/consent/grants`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "dataType": "HEALTH_RECORD",
  "purpose": "CREATE"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ConsentService persists the grant. 2. It appears in the authenticated user's consent list after reload.

### IT-AUTH-040 - An authenticated user can revoke one of their own consent grants

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consent/grants/{consentId} (default: `http://localhost:8080/api/v1/consent/grants/{consentId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{consentId}` with the real value from the consent grant creation/list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consent/grants (default: `http://localhost:8080/api/v1/consent/grants`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Backend finds the consent by grant ID and authenticated user. 2. Backend applies the revoke operation. 3. The active consent state no longer grants that permission.

### IT-AUTH-041 - A user cannot revoke a consent grant owned by another account

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consent/grants/{consentId} (default: `http://localhost:8080/api/v1/consent/grants/{consentId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{consentId}` with the real value from the consent grant creation/list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side ownership scoping rejects the request. 2. User B's consent remains unchanged.

### IT-AUTH-042 - An authenticated user can perform the supported self-deactivation account flow

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/deactivate (default: `http://localhost:8080/api/v1/auth/deactivate`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Account state is persisted according to the implemented deactivation flow. 2. Subsequent account access follows the resulting account-status policy. 3. No false success is displayed before persistence.

### IT-AUTH-043 - A deactivated account cannot continue normal protected activity using a newly attempted login

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/login (default: `http://localhost:8080/api/v1/auth/login`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "deactivated.qa@carebridge.dev",
  "password": "Test@1234"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend applies the updated account state. 2. Backend rejects access that is no longer permitted.

### IT-AUTH-044 - Verify the implemented own-account deletion behavior through the complete protected account lifecycle

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/deactivate (default: `http://localhost:8080/api/v1/auth/deactivate`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. NEEDS_CONFIRMATION: resulting deletion or archival semantics must match the repository implementation. 2. No protected access may remain contrary to the persisted account state.

## Manage Administrative Access Control

### IT-AUTH-045 - A locked user can submit an account-lock appeal for the current lock episode

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/lock-appeals (default: `http://localhost:8080/api/v1/auth/lock-appeals`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_LOCK_APPEAL_TOKEN_HERE>` with the lock notification/appeal flow for the currently locked account. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "appealToken": "<PASTE_LOCK_APPEAL_TOKEN_HERE>",
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. AccountLockAppealService associates the appeal with the user and lock episode. 2. AccountLockAppealService persists it. 3. AccountLockAppealService returns the submitted appeal state.

### IT-AUTH-046 - Duplicate account-lock appeals for the same lock episode are prevented according to persisted appeal state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/lock-appeals (default: `http://localhost:8080/api/v1/auth/lock-appeals`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_LOCK_APPEAL_TOKEN_HERE>` with the lock notification/appeal flow for the currently locked account. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "appealToken": "<PASTE_LOCK_APPEAL_TOKEN_HERE>",
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend uses lock-episode appeal checks to prevent an unsupported duplicate active appeal. 2. No duplicate successful workflow is fabricated.

### IT-AUTH-047 - A System Admin can list account-lock appeals by status

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/account-lock-appeals (default: `http://localhost:8080/api/v1/admin/account-lock-appeals`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = PENDING` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Admin controller and appeal service query persisted appeals by status. 2. Admin controller and appeal service return the matching records to the admin workspace.

### IT-AUTH-048 - A System Admin can approve a valid account-lock appeal and clear the applicable lock

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/account-lock-appeals/{appealId}/review (default: `http://localhost:8080/api/v1/admin/account-lock-appeals/{appealId}/review`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{appealId}` with the real value from the lock-appeal submission/admin list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "APPROVE",
  "reviewNote": "Reviewed and approved in Postman QA."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Appeal review is persisted. 2. The service clears the lock when required by the approved decision. 3. Audit evidence is recorded. 4. Subsequent authentication reflects the updated account state.

### IT-AUTH-049 - A System Admin can reject an account-lock appeal without incorrectly clearing the account lock

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/account-lock-appeals/{appealId}/review (default: `http://localhost:8080/api/v1/admin/account-lock-appeals/{appealId}/review`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{appealId}` with the real value from the lock-appeal submission/admin list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "REJECT",
  "reviewNote": "Reviewed and approved in Postman QA."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Appeal status is persisted as reviewed according to the rejection. 2. The user's lock is not cleared unless explicitly required by implemented logic.

### IT-AUTH-050 - A non-admin user cannot access System Admin account-governance operations

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users (default: `http://localhost:8080/api/v1/admin/users`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `email = CareBridge QA` (optional/filter); query `phone = CareBridge QA` (optional/filter); query `name = CareBridge QA` (optional/filter); query `role = MOTHER` (optional/filter); query `enabled = true` (optional/filter); query `locked = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role enforcement rejects the request. 2. Admin data and mutation services are not exposed to the non-admin actor.

### IT-AUTH-051 - A System Admin can search and retrieve user accounts through the admin user-management flow

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/account-lock-appeals (default: `http://localhost:8080/api/v1/admin/account-lock-appeals`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = PENDING` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users (default: `http://localhost:8080/api/v1/admin/users`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `email = CareBridge QA` (optional/filter); query `phone = CareBridge QA` (optional/filter); query `name = CareBridge QA` (optional/filter); query `role = MOTHER` (optional/filter); query `enabled = true` (optional/filter); query `locked = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION, 200`. After Send: 1. AdminUserController and service return authorized matching user data from persistent storage. 2. Unrelated restricted data is not exposed.

### IT-AUTH-052 - A System Admin can retrieve a selected user's administrative detail view

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId} (default: `http://localhost:8080/api/v1/admin/users/{userId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The admin API loads the selected user through AdminUserService. 2. The admin API returns the permitted account detail required by the admin UI.

### IT-AUTH-053 - A System Admin can update a user's supported account status and the new state affects subsequent access

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/status (default: `http://localhost:8080/api/v1/admin/users/{userId}/status`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "enabled": true
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminUserService persists the new status. 2. Audit evidence is produced where implemented. 3. Subsequent authentication/authorization reflects the new state.

### IT-AUTH-054 - A System Admin can update a user's supported role and the new role is enforced by protected APIs

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/role (default: `http://localhost:8080/api/v1/admin/users/{userId}/role`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "newRole": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminRoleController persists the supported role assignment. 2. Later authorization decisions use the updated role. 3. Later authorization decisions grant or deny access accordingly.

### IT-AUTH-055 - A System Admin can create a valid staff account through the administrative account-provisioning flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/staff-accounts (default: `http://localhost:8080/api/v1/admin/staff-accounts`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "mother@carebridge.dev",
  "name": "CareBridge QA Test",
  "role": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend validates the request. 2. The backend persists the staff account and supported role. 3. The backend makes the new account retrievable through admin user management.

### IT-AUTH-056 - Duplicate staff identity data are rejected without creating a second account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/staff-accounts (default: `http://localhost:8080/api/v1/admin/staff-accounts`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "mother@carebridge.dev",
  "name": "CareBridge QA Test",
  "role": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend uniqueness checks reject the duplicate request. 2. No second staff account is persisted.

### IT-AUTH-057 - A System Admin can review the active login sessions associated with a selected user

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/sessions (default: `http://localhost:8080/api/v1/admin/users/{userId}/sessions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminUserService retrieves the target user's persisted session data. 2. AdminUserService returns it only to the authorized admin actor.

### IT-AUTH-058 - A System Admin can retrieve the supported activity history for a selected user

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/activity (default: `http://localhost:8080/api/v1/admin/users/{userId}/activity`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminUserService returns the persisted activity/audit information supported by the implementation for the selected user.

### IT-AUTH-059 - A System Admin can review security and audit records generated by account operations

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/audit-logs (default: `http://localhost:8080/api/v1/admin/audit-logs`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `userId = copy a real UUID from the preceding list/create response` (optional/filter); query `action = LOGIN` (optional/filter); query `fromDate = 2026-08-12T12:00:00Z` (optional/filter); query `toDate = 2026-08-12T12:00:00Z` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. AuditLogRepository-backed records are retrievable by the authorized admin. 2. AuditLogRepository-backed records correspond to the tested security/account operations.

### IT-AUTH-060 - A System Admin can retrieve current system configuration through the protected configuration service

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. SystemConfigurationService retrieves the current persisted configuration. 2. SystemConfigurationService returns the current persisted configuration to the authorized admin interface.

### IT-AUTH-061 - A System Admin can update a supported system configuration value and retrieve the persisted result

**Step 1**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "aiModerationEnabled": true,
  "maintenanceModeEnabled": true,
  "rowVersion": 1
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. SystemConfigurationService validates the change. 2. SystemConfigurationService persists the change. 3. Reload returns the updated value. 4. The administrative change is auditable where implemented.

### IT-AUTH-062 - A conflicting system-configuration update is handled without silently overwriting a newer persisted value

**Step 1**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "aiModerationEnabled": true,
  "maintenanceModeEnabled": true,
  "rowVersion": 1
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The configuration service applies its implemented conflict handling. 2. Stale conflicting data do not silently replace the confirmed newer state.

### IT-AUTH-063 - A non-admin account cannot read or update protected system configuration

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/system-configuration (default: `http://localhost:8080/api/v1/admin/system-configuration`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "aiModerationEnabled": true,
  "maintenanceModeEnabled": true,
  "rowVersion": 1
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role authorization rejects both operations. 2. Persisted configuration is unchanged.

### IT-AUTH-064 - A non-admin cannot bypass the UI to change another user's status or role through admin APIs

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/status (default: `http://localhost:8080/api/v1/admin/users/{userId}/status`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "enabled": true
}
```

**Step 2**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/users/{userId}/role (default: `http://localhost:8080/api/v1/admin/users/{userId}/role`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{userId}` with the real value from the target account's `userId` from the admin user list/profile response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "newRole": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Protected admin controllers reject the actor. 2. The target user's status and role remain unchanged.

### IT-AUTH-065 - An invalid account-lock appeal review request does not mutate appeal or user-lock state

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/account-lock-appeals/{appealId}/review (default: `http://localhost:8080/api/v1/admin/account-lock-appeals/{appealId}/review`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{appealId}` with the real value from the lock-appeal submission/admin list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "INVALID",
  "reviewNote": "Reviewed and approved in Postman QA."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend validation rejects the review. 2. The appeal remains in its prior state. 3. The associated account lock is not incorrectly changed.

### IT-AUTH-066 - External OTP or notification delivery failure leaves the account workflow in a recoverable state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/send (default: `http://localhost:8080/api/v1/notifications/send`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recipientUserId": "f89629c0-aff1-48d0-aee2-0f3172aa7c87",
  "type": "REMINDER",
  "title": "CareBridge integration test",
  "body": "I need safe guidance for nausea during pregnancy."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend does not report an unconfirmed downstream success as complete. 2. Persisted state remains consistent. 3. The workflow can be safely retried according to implemented logic.

### IT-AUTH-067 - Retrying a confirmed authentication mutation does not create duplicate sessions, OTP transitions, or other duplicated confirmed state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/auth/verify-otp (default: `http://localhost:8080/api/v1/auth/verify-otp`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Set **Authorization > No Auth**. This endpoint is public.
3. **Runtime data:** Before Send, replace `<PASTE_PREVIOUSLY_USED_OTP_HERE>` with an OTP already consumed by a successful verification. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "email": "carebridge.qa.20260812@example.com",
  "otp": "<PASTE_PREVIOUSLY_USED_OTP_HERE>"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Retry behavior follows implemented idempotency/state guards. 2. No duplicate confirmed transition or unintended duplicate persistent records are created.

# Mother_Care_Journey

## Create Maternal Profile

### IT-MOTHER-001 - An authenticated mother can create a valid mother-care journey and retrieve the persisted journey state

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys (default: `http://localhost:8080/api/v1/journeys`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "startDate": "2026-08-12",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. JourneyController delegates to JourneyService. 2. The journey is validated. 3. The journey is persisted for the authenticated owner. 4. The journey is returned on the dashboard after reload.

### IT-MOTHER-002 - A mother can create and use a valid preconception journey state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys (default: `http://localhost:8080/api/v1/journeys`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "startDate": "2026-08-12",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The preconception journey is persisted as the owner's active journey. 2. The dashboard returns state appropriate to the preconception stage.

### IT-MOTHER-003 - A mother can create and use a valid pregnancy journey state

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys (default: `http://localhost:8080/api/v1/journeys`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "startDate": "2026-08-12",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. The pregnancy journey is persisted for the owner. 2. The dashboard returns the current pregnancy state from backend data.

### IT-MOTHER-004 - The backend prevents an unsupported duplicate active mother journey

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys (default: `http://localhost:8080/api/v1/journeys`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "startDate": "2026-08-12",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. JourneyService active-journey checks reject the conflicting creation. 2. The existing active journey remains unchanged. 3. No duplicate active journey is persisted.

## View Maternal Profile

### IT-MOTHER-006 - The Mother Journey dashboard is built from the authenticated user's canonical persisted journey state

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The getDashboard operation returns the authenticated owner's current journey context and related integrated data consistently with persisted state.

### IT-MOTHER-008 - A mother cannot read or update another user's journey

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side owner checks reject unauthorized access. 2. User B's journey data and state remain unchanged.

## Update Maternal Profile

### IT-MOTHER-005 - A mother can update her own journey and retrieve the persisted changes

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId} (default: `http://localhost:8080/api/v1/journeys/{journeyId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. JourneyService validates ownership. 2. JourneyService persists supported changes. 3. Subsequent dashboard data reflects the updated journey.

### IT-MOTHER-007 - Recording a pregnancy outcome updates the mother journey lifecycle consistently

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/pregnancy-outcomes (default: `http://localhost:8080/api/v1/journeys/{journeyId}/pregnancy-outcomes`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "submissionId": "fe9181d4-14e9-5070-bcae-937e4cd64119",
  "expectedJourneyVersion": 1,
  "outcomeType": "ONGOING",
  "source": "SELF_REPORTED",
  "reason": "Verified during CareBridge integration testing.",
  "effectiveAt": "2026-08-12T13:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. RecordPregnancyOutcome persists the pregnancy outcome. 2. The resulting journey transition is persisted. 3. The dashboard reflects the updated lifecycle state without duplicate transitions.

### IT-MOTHER-011 - Retrying a confirmed journey mutation does not create duplicate active journeys or duplicate lifecycle transitions

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId} (default: `http://localhost:8080/api/v1/journeys/{journeyId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "journeyType": "PREGNANCY",
  "changeReason": "Updated for integration testing.",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Implemented state guards prevent duplicate confirmed journey state or duplicate transitions. 2. Persisted lifecycle remains consistent.

## View Personalized Care Plan

### IT-MOTHER-009 - Personalized care recommendations are generated from the authenticated mother's journey context and supporting evidence

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/recommendations/profile (default: `http://localhost:8080/api/v1/recommendations/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. RecommendationService uses the owner's journey/transition context and available evidence/content to return personalized recommendations through the API.

### IT-MOTHER-010 - Failure of the optional external recommendation service does not corrupt the mother journey state

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/recommendations/profile (default: `http://localhost:8080/api/v1/recommendations/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/me/dashboard (default: `http://localhost:8080/api/v1/journeys/me/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. External failure is handled without mutating or corrupting persisted journey data. 2. The client receives the implemented safe error/fallback behavior.

## Record Health Tracking Data

### IT-MOTHER-012 - A mother can add a valid maternal health metric and retrieve it from persistent storage

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthMetricService validates the metric for the authenticated owner. 2. HealthMetricService persists the metric for the authenticated owner. 3. Subsequent API retrieval includes the new observation.

### IT-MOTHER-013 - Valid BMI-related maternal metric data can be persisted and returned through the health-metric flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend validates the metric observation. 2. The backend persists it. 3. The returned metric history/trend reflects the saved BMI-related data.

### IT-MOTHER-014 - A hydration observation submitted from the client is persisted and returned in maternal health history

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Hydration data passes backend validation. 2. Hydration data is persisted for the authenticated owner. 3. Hydration data appears in subsequent health-metric retrieval.

### IT-MOTHER-015 - A maternal mood observation submitted from the client is persisted and retrievable

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The mood observation is validated. 2. The mood observation is persisted for the authenticated mother. 3. The mood observation is returned by the backend on reload.

### IT-MOTHER-016 - A fetal-movement quick health note is stored through the maternal health-metric integration flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The quick note reaches the health-metric service. 2. The quick note is persisted for the authenticated owner. 3. The quick note appears in subsequent retrieval.

## View Health Tracking Data

### IT-MOTHER-017 - Maternal health-metric trend retrieval returns the authenticated mother's persisted observations in the expected time sequence

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthMetricService returns the owner's relevant observations for trend display with values/dates consistent with persisted data.

### IT-MOTHER-018 - A mother can retrieve detail for one of her own maternal health metrics

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-metrics/{metricId} (default: `http://localhost:8080/api/v1/health-metrics/{metricId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{metricId}` with the real value from the response from the metric-creation step or the matching metric list endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. HealthMetricController/Service returns the requested observation only when it belongs to the authenticated owner.

## Manage Health Tracking Data

### IT-MOTHER-019 - A mother can update a valid owned maternal health metric and retrieve the changed value

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics/{metricId} (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics/{metricId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys; replace `{metricId}` with the real value from the response from the metric-creation step or the matching metric list endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "valueNumeric": 65.0,
  "unit": "kg",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthMetricService validates the update. 2. HealthMetricService persists the update. 3. Subsequent retrieval reflects the new value without creating an unintended duplicate observation.

### IT-MOTHER-020 - Deleting an owned maternal health metric removes it from subsequent metric retrieval

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-metrics/{metricId} (default: `http://localhost:8080/api/v1/health-metrics/{metricId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{metricId}` with the real value from the response from the metric-creation step or the matching metric list endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `204`. After Send: 1. HealthMetricService applies the implemented deletion semantics. 2. The removed observation is no longer returned as an active metric.

### IT-MOTHER-021 - An invalid maternal health metric is rejected without persisting corrupt observation data

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics/{metricId} (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics/{metricId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys; replace `{metricId}` with the real value from the response from the metric-creation step or the matching metric list endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "valueNumeric": 65.0,
  "unit": "kg",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. MetricObservationValidator or service validation rejects the request. 2. No invalid observation is persisted.

### IT-MOTHER-022 - A mother cannot read, update, or delete another user's maternal health metric

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics/{metricId} (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics/{metricId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys; replace `{metricId}` with the real value from the response from the metric-creation step or the matching metric list endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "valueNumeric": 65.0,
  "unit": "kg",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side ownership checks deny unauthorized access. 2. User B's metric remains unchanged.

## Record Postpartum Recovery Data

### IT-MOTHER-023 - A mother in an active postpartum journey can add a valid postpartum log

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/postpartum-logs (default: `http://localhost:8080/api/v1/journeys/{journeyId}/postpartum-logs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "submissionId": "c59fe391-5b8a-5bc5-b218-ae59cf890c05",
  "logDate": "2026-08-12"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. PostpartumLogService verifies active postpartum ownership. 2. PostpartumLogService persists the log. 3. PostpartumLogService returns it in the owner's postpartum data.

## View Postpartum Recovery Data

### IT-MOTHER-024 - A mother can list postpartum logs belonging to her active postpartum journey

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/postpartum-logs (default: `http://localhost:8080/api/v1/postpartum-logs`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `journeyId = copy a real UUID from the preceding list/create response` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. PostpartumLogService returns the authenticated owner's applicable postpartum logs. 2. PostpartumLogService excludes records belonging to other users.

### IT-MOTHER-025 - A mother can retrieve detail for one of her own postpartum logs

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/postpartum-logs/{logId} (default: `http://localhost:8080/api/v1/postpartum-logs/{logId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend verifies postpartum read ownership. 2. The backend returns the selected log to the authenticated mother.

## Manage Postpartum Recovery Data

### IT-MOTHER-026 - A mother can update an owned postpartum log and retrieve the persisted changes

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/postpartum-logs/{logId} (default: `http://localhost:8080/api/v1/postpartum-logs/{logId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "logDate": "2026-08-12"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. PostpartumLogService validates ownership/state. 2. PostpartumLogService persists the update. 3. Subsequent retrieval reflects the changed values.

### IT-MOTHER-027 - A mother can delete an owned postpartum log and the record is removed according to implemented semantics

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/postpartum-logs/{logId} (default: `http://localhost:8080/api/v1/postpartum-logs/{logId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The service applies the supported deletion behavior. 2. The removed log is no longer returned as an active postpartum record.

### IT-MOTHER-028 - Postpartum log creation is rejected when the mother is not in an active postpartum state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/postpartum-logs (default: `http://localhost:8080/api/v1/journeys/{journeyId}/postpartum-logs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "submissionId": "8db62d04-a69b-5ac4-b698-2c355efc3d2c",
  "logDate": "2026-08-12"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The requireActivePostpartumOwner check rejects the mutation. 2. No postpartum log is persisted.

### IT-MOTHER-029 - A mother cannot access another user's postpartum log

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/postpartum-logs/{logId} (default: `http://localhost:8080/api/v1/postpartum-logs/{logId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Postpartum ownership checks reject unauthorized operations. 2. User B's log remains unchanged.

## View Maternal Health Records

### IT-MOTHER-032 - The maternal health-record timeline returns only active records authorized for the authenticated mother

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/timeline (default: `http://localhost:8080/api/v1/health-records/timeline`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `filter = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. HealthRecordService returns User A's active timeline records only. 2. User B's records are excluded by owner scoping.

### IT-MOTHER-033 - Maternal health-record detail includes the authorized record and its persisted attachment metadata

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{recordId} (default: `http://localhost:8080/api/v1/health-records/{recordId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend returns the owner-scoped HealthRecord and associated HealthRecordFile/uploaded-file information required by the client.

### IT-MOTHER-039 - Archived maternal health records are excluded from the active timeline query

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/timeline (default: `http://localhost:8080/api/v1/health-records/timeline`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `filter = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Timeline filtering returns active records. 2. Timeline filtering excludes the archived record according to repository/service rules.

## Manage Maternal Health Records

### IT-MOTHER-030 - A mother can create a maternal health record without an attachment and retrieve it on her timeline

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recordType": "ULTRASOUND",
  "title": "CareBridge integration test",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthRecordService persists the owner-scoped record. 2. The timeline returns it as an active maternal health record.

### IT-MOTHER-031 - A mother can create a maternal health record with an attachment and retrieve both record and file reference

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recordType": "ULTRASOUND",
  "title": "CareBridge integration test",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. File storage completes successfully. 2. HealthRecord and related file metadata are persisted for the owner. 3. HealthRecord and related file metadata are returned in record detail.

### IT-MOTHER-034 - A mother can update an owned maternal health record and retrieve the persisted changes

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{id} (default: `http://localhost:8080/api/v1/health-records/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "sourceType": "OTHER",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthRecordService validates ownership. 2. HealthRecordService persists supported changes. 3. Subsequent retrieval reflects the updated record.

### IT-MOTHER-035 - Health-record attachment metadata remains synchronized with the record after a supported record/file update

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{id} (default: `http://localhost:8080/api/v1/health-records/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "sourceType": "OTHER",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/files/health-records (default: `http://localhost:8080/api/v1/files/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Record and file-reference state remain consistent across HealthRecordService, record-file repository, uploaded-file repository, and configured storage.

### IT-MOTHER-036 - Archiving an owned maternal health record removes it from the active timeline while preserving the implemented archived state

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{id}/archive (default: `http://localhost:8080/api/v1/health-records/{id}/archive`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. HealthRecordService persists the archive state. 2. The active timeline no longer includes the archived record.

### IT-MOTHER-037 - External file-storage failure during maternal health-record upload does not leave a false successful attachment state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/files/health-records (default: `http://localhost:8080/api/v1/files/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend handles storage failure safely. 2. The system does not present an unconfirmed attachment as successfully persisted. 3. The workflow remains in a consistent recoverable state.

### IT-MOTHER-038 - A mother cannot retrieve, update, or archive another user's maternal health record

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{recordId} (default: `http://localhost:8080/api/v1/health-records/{recordId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Owner checks reject all unauthorized operations. 2. User B's record remains unchanged.

## Manage Appointments

### IT-MOTHER-040 - A mother can retrieve her appointment list through the protected appointment flow

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments (default: `http://localhost:8080/api/v1/appointments`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. AppointmentController returns appointment data authorized for the authenticated user. 2. Another user's appointments are not exposed.

### IT-MOTHER-041 - A mother can create a valid appointment and retrieve it from the appointment list

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments (default: `http://localhost:8080/api/v1/appointments`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reminderType": "APPOINTMENT",
  "title": "CareBridge integration test",
  "scheduledAt": "2026-08-13T13:00:14Z",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "notificationOffsetsMinutes": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The appointment is validated. 2. The appointment is persisted for the owner. 3. Reloading the list returns the newly created appointment.

### IT-MOTHER-042 - Verify the implemented appointment update flow persists valid changes for the authenticated owner

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments/{appointmentId} (default: `http://localhost:8080/api/v1/appointments/{appointmentId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{appointmentId}` with the real value from the response from POST/GET appointments. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "notificationOffsetsMinutes": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments (default: `http://localhost:8080/api/v1/appointments`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. NEEDS_CONFIRMATION: if appointment update is implemented, valid changes are owner-scoped and persisted. 2. Otherwise the client must not expose an unsupported update action.

### IT-MOTHER-043 - A mother can delete an owned appointment and it is removed according to implemented semantics

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments/{appointmentId} (default: `http://localhost:8080/api/v1/appointments/{appointmentId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{appointmentId}` with the real value from the response from POST/GET appointments. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments (default: `http://localhost:8080/api/v1/appointments`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `204, 201`. After Send: 1. Appointment deletion succeeds only for the owner. 2. The deleted appointment is no longer returned as active.

### IT-MOTHER-044 - Appointment creation integrates with the implemented reminder/notification scheduling behavior

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/appointments (default: `http://localhost:8080/api/v1/appointments`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reminderType": "APPOINTMENT",
  "title": "CareBridge integration test",
  "scheduledAt": "2026-08-13T13:00:14Z",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "notificationOffsetsMinutes": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The appointment flow invokes supported reminder integration. 2. Related persistent reminder/schedule state and notification behavior match the configured implementation without duplicate notifications.

## Manage Reminders & Schedules

### IT-MOTHER-045 - An authenticated mother can retrieve her reminders

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ReminderService returns owner-scoped reminders from persistent storage. 2. Reminders belonging to other users are not returned.

### IT-MOTHER-046 - A mother can create a valid generic reminder and retrieve the persisted result

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reminderType": "APPOINTMENT",
  "title": "CareBridge integration test",
  "scheduledAt": "2026-08-13T13:00:14Z",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "notificationOffsetsMinutes": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ReminderService persists the owner-scoped reminder. 2. The new reminder appears on reload.

### IT-MOTHER-047 - A mother can create a medication reminder through the dedicated reminder flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/medication (default: `http://localhost:8080/api/v1/reminders/medication`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "scheduledAt": "2026-08-13T13:00:14Z",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The createMedicationReminder operation persists the supported medication-reminder data for the owner. 2. The createMedicationReminder operation returns it through the reminder list.

### IT-MOTHER-048 - Verify the implemented reminder update flow persists supported changes without creating a duplicate reminder

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/{reminderId} (default: `http://localhost:8080/api/v1/reminders/{reminderId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reminderId}` with the real value from the response from POST/GET reminders. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "notificationOffsetsMinutes": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. NEEDS_CONFIRMATION: the deployed update path, if exposed, persists changes to the existing reminder rather than creating an unintended duplicate.

### IT-MOTHER-049 - Completing an owned reminder updates its persisted completion state and Today Tasks representation

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/{reminderId}/complete (default: `http://localhost:8080/api/v1/reminders/{reminderId}/complete`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reminderId}` with the real value from the response from POST/GET reminders. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. ReminderController/Service persists completion. 2. Reminder and aggregated Today Tasks state reflect the completed action consistently.

### IT-MOTHER-050 - Enabling or disabling an owned reminder updates its active state

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/{reminderId}/enable (default: `http://localhost:8080/api/v1/reminders/{reminderId}/enable`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reminderId}` with the real value from the response from POST/GET reminders. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ReminderService persists the requested enabled state. 2. Subsequent behavior reflects that state.

### IT-MOTHER-051 - Deleting an owned reminder removes it from active reminder retrieval and prevents unsupported future execution

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/{reminderId}/permanent (default: `http://localhost:8080/api/v1/reminders/{reminderId}/permanent`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reminderId}` with the real value from the response from POST/GET reminders. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders (default: `http://localhost:8080/api/v1/reminders`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `204, 201`. After Send: 1. ReminderService applies deletion for the owner. 2. The reminder is no longer returned as active. 3. The reminder does not continue an invalid scheduled workflow.

### IT-MOTHER-052 - A mother can create a valid reminder schedule and retrieve its persisted schedule state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules (default: `http://localhost:8080/api/v1/reminder-schedules`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "times": [],
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ReminderScheduleService persists the owner-scoped schedule. 2. ReminderScheduleService returns it through the schedule API.

### IT-MOTHER-053 - A mother can list her reminder schedules without seeing schedules owned by another user

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules (default: `http://localhost:8080/api/v1/reminder-schedules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ReminderScheduleService returns only schedules authorized for User A.

### IT-MOTHER-054 - A mother can retrieve detail for an owned reminder schedule

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules/{scheduleId} (default: `http://localhost:8080/api/v1/reminder-schedules/{scheduleId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{scheduleId}` with the real value from the preceding create/list response for `scheduleId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The schedule API returns the requested owner-scoped persisted schedule.

### IT-MOTHER-055 - Updating a reminder schedule changes persistent schedule data and subsequent planner materialization

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules/{scheduleId} (default: `http://localhost:8080/api/v1/reminder-schedules/{scheduleId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{scheduleId}` with the real value from the preceding create/list response for `scheduleId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules (default: `http://localhost:8080/api/v1/reminder-schedules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ReminderScheduleService persists the update. 2. The materializeForPlanner operation uses the updated schedule rather than stale recurrence data.

### IT-MOTHER-056 - Deleting an owned reminder schedule removes it from subsequent schedule retrieval and planner materialization

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules/{scheduleId} (default: `http://localhost:8080/api/v1/reminder-schedules/{scheduleId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{scheduleId}` with the real value from the preceding create/list response for `scheduleId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The deleted schedule is no longer active in schedule retrieval. 2. The deleted schedule does not create unsupported future planner items.

### IT-MOTHER-057 - A recurring reminder schedule materializes the expected planner items without duplicate instances

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminder-schedules/{scheduleId} (default: `http://localhost:8080/api/v1/reminder-schedules/{scheduleId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{scheduleId}` with the real value from the preceding create/list response for `scheduleId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "timeZone": "Asia/Ho_Chi_Minh"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The materializeForPlanner operation produces the expected scheduled instances. 2. Repeated processing does not create unintended duplicate confirmed tasks.

### IT-MOTHER-058 - FCM delivery failure during a reminder notification leaves reminder state consistent and safely retryable

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/notifications/send (default: `http://localhost:8080/api/v1/notifications/send`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recipientUserId": "f89629c0-aff1-48d0-aee2-0f3172aa7c87",
  "type": "REMINDER",
  "title": "CareBridge integration test",
  "body": "I need safe guidance for nausea during pregnancy."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Notification failure does not falsely mark an unconfirmed delivery as successful or corrupt reminder state. 2. Backend behavior remains consistent with implemented retry handling.

## View Today Tasks

### IT-MOTHER-059 - Today Tasks aggregates the authenticated mother's eligible care tasks from integrated providers for the requested date

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/today (default: `http://localhost:8080/api/v1/tasks/today`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. UnifiedTodayTaskService aggregates authorized tasks from configured providers. 2. UnifiedTodayTaskService returns a coherent Today Tasks list for the actor/date/timezone.

### IT-MOTHER-060 - Today Tasks uses the requested timezone when bucketing due items into the selected day

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/today (default: `http://localhost:8080/api/v1/tasks/today`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. UnifiedTodayTaskService buckets items according to actor/date/timezone input so tasks appear on the correct local day.

### IT-MOTHER-061 - Completing a reminder from Today Tasks updates the underlying reminder and aggregated task state consistently

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/{taskKind}/{taskId}/actions (default: `http://localhost:8080/api/v1/tasks/{taskKind}/{taskId}/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{taskKind}` with the real value from the preceding create/list response for `taskKind`; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "action": "COMPLETE",
  "clientRequestId": "136a10bd-0bff-5e83-b58d-13a73a5a8c5f"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/today (default: `http://localhost:8080/api/v1/tasks/today`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. TodayTaskController routes the action to the underlying provider. 2. Completion is persisted once. 3. Both views reflect the same completed state.

### IT-MOTHER-062 - A mother cannot read or mutate another user's reminder or reminder schedule

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/{taskKind}/{taskId}/actions (default: `http://localhost:8080/api/v1/tasks/{taskKind}/{taskId}/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{taskKind}` with the real value from the preceding create/list response for `taskKind`; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "action": "COMPLETE",
  "clientRequestId": "21c5a97d-75ae-52da-9ff5-3f6aa97c1ac1"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side owner scoping rejects unauthorized operations. 2. User B's reminder/schedule state remains unchanged.

### IT-MOTHER-063 - Retrying a confirmed reminder or schedule mutation does not create duplicate persisted reminders or planner items

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/tasks/{taskKind}/{taskId}/actions (default: `http://localhost:8080/api/v1/tasks/{taskKind}/{taskId}/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{taskKind}` with the real value from the preceding create/list response for `taskKind`; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "action": "COMPLETE",
  "clientRequestId": "fa688c73-bfbc-54a0-bef1-00aa738fc6c9"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Implemented state/idempotency guards prevent duplicate confirmed records or duplicate task instances.

## View Pregnancy Exercises

### IT-MOTHER-064 - A mother can browse published pregnancy exercises through the backend-supported exercise catalog

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises (default: `http://localhost:8080/api/v1/exercises`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `trimester = FIRST` (optional/filter); query `difficulty = EASY` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The client retrieves published exercise records from the backend. 2. The client displays only exercises available under the implemented rules.

### IT-MOTHER-065 - Pregnancy-exercise filters return catalog results consistent with the selected supported criteria

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises (default: `http://localhost:8080/api/v1/exercises`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `trimester = FIRST` (optional/filter); query `difficulty = EASY` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Backend/client filtering returns the published exercises matching the selected criteria without changing persistent exercise content.

### IT-MOTHER-066 - A mother can retrieve pregnancy-exercise detail from the protected exercise flow

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId} (default: `http://localhost:8080/api/v1/exercises/{exerciseId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseController returns the selected published exercise detail required by the client for the authenticated user.

### IT-MOTHER-067 - The client can retrieve posture configuration required by the supported camera-guided exercise

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/posture-config (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/posture-config`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExerciseController returns the configured posture parameters required by the supported analysis flow.

## Manage Pregnancy Exercise Sessions

### IT-MOTHER-068 - Submitting a valid pre-exercise safety check persists the clearance result used by session start

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/safety-check (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/safety-check`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "q1NoDizziness": true,
  "q2NoContractions": true,
  "q3NoBleeding": true,
  "q4HydratedAndFed": true,
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The safety-check result is persisted for the authenticated user. 2. Latest-safety retrieval returns the result used by the exercise-session flow.

### IT-MOTHER-069 - An unsafe or non-cleared pre-exercise safety result blocks camera-guided exercise session start

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/safety-check (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/safety-check`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "q1NoDizziness": true,
  "q2NoContractions": true,
  "q3NoBleeding": true,
  "q4HydratedAndFed": true,
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "notes": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Server-side safety rules reject session start. 2. No active exercise session is created. 3. Safety guidance remains visible as implemented.

### IT-MOTHER-070 - A mother with valid safety clearance can start a camera-guided exercise session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/sessions (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "safetyCheckId": "857a4f2e-0b9f-537f-9327-e192905b3d8d",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExerciseSessionService validates the safety/session prerequisites. 2. ExerciseSessionService persists an active session for the authenticated owner. 3. ExerciseSessionService returns it to the client.

### IT-MOTHER-071 - Exercise session start is rejected when required safety clearance is missing

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/sessions (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "safetyCheckId": "fe9ed2ae-3397-5f8e-b0f4-7319349c652e",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend prerequisite checks reject the start request. 2. No active exercise session is persisted.

### IT-MOTHER-072 - Pausing an active owned exercise session persists the paused session state

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/pause (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/pause`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseSessionService updates the owner-scoped active session to the implemented paused state. 2. ExerciseSessionService returns the updated result.

### IT-MOTHER-073 - Resuming a paused owned exercise session restores the supported active state

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/resume (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/resume`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseSessionService validates the session owner/state. 2. ExerciseSessionService persists the supported resumed state.

### IT-MOTHER-074 - Posture-analysis input from a valid exercise session integrates with the MediaPipe sidecar and returns an analysis result

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/posture-events (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/posture-events`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "eventTimeMs": 1,
  "keypointSummaryJson": {}
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{exerciseSidecarBaseUrl}}/v1/inference/landmarks (default: `http://localhost:8002/v1/inference/landmarks`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "schemaVersion": "mediapipe-pose-landmarks-v1",
  "modelVersion": "exercise-correction@202a0a802d8d2e3ea42f00e6a8c47da9cafc09d7",
  "exerciseKey": "squat",
  "sequenceNumber": 1,
  "inferenceStreamId": "postman-stream-20260812",
  "landmarks": {
    "nose": {
      "x": 0.5,
      "y": 0.2,
      "z": 0.0,
      "visibility": 0.99
    },
    "left_shoulder": {
      "x": 0.4,
      "y": 0.4,
      "z": 0.0,
      "visibility": 0.99
    },
    "right_shoulder": {
      "x": 0.6,
      "y": 0.4,
      "z": 0.0,
      "visibility": 0.99
    }
  }
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseSessionController sends supported analysis input to the MediaPipe integration. 2. ExerciseSessionController returns the resulting posture score/feedback without treating it as medical diagnosis.

### IT-MOTHER-075 - Completing an owned exercise session persists the final session result

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/complete (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/complete`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseSessionService transitions the owned session to completed state. 2. ExerciseSessionService persists supported result data. 3. ExerciseSessionService returns it through result/history APIs.

### IT-MOTHER-076 - A mother can retrieve the persisted result of her completed exercise session

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/result (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/result`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExerciseSessionController returns the owner-scoped completed-session result from backend state.

### IT-MOTHER-077 - Exercise history returns the authenticated mother's completed session records

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/history (default: `http://localhost:8080/api/v1/exercises/sessions/history`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `trimesterScope = FIRST` (optional/filter); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend returns the authenticated user's eligible exercise-session history. 2. The backend excludes another user's sessions.

### IT-MOTHER-078 - MediaPipe posture-analysis failure does not corrupt or falsely complete the exercise session

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/posture-events (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/posture-events`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "eventTimeMs": 1,
  "keypointSummaryJson": {}
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{exerciseSidecarBaseUrl}}/v1/inference/landmarks (default: `http://localhost:8002/v1/inference/landmarks`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "schemaVersion": "mediapipe-pose-landmarks-v1",
  "modelVersion": "exercise-correction@202a0a802d8d2e3ea42f00e6a8c47da9cafc09d7",
  "exerciseKey": "squat",
  "sequenceNumber": 1,
  "inferenceStreamId": "postman-stream-20260812",
  "landmarks": {
    "nose": {
      "x": 0.5,
      "y": 0.2,
      "z": 0.0,
      "visibility": 0.99
    },
    "left_shoulder": {
      "x": 0.4,
      "y": 0.4,
      "z": 0.0,
      "visibility": 0.99
    },
    "right_shoulder": {
      "x": 0.6,
      "y": 0.4,
      "z": 0.0,
      "visibility": 0.99
    }
  }
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. External analysis failure is handled safely. 2. The session is not falsely marked complete. 3. Persisted session state remains recoverable.

### IT-MOTHER-079 - A mother cannot control or retrieve another user's exercise session

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/sessions/{sessionId}/complete (default: `http://localhost:8080/api/v1/exercises/sessions/{sessionId}/complete`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side owner checks reject unauthorized operations. 2. User B's session remains unchanged.

### IT-MOTHER-080 - Camera-guided exercise analysis is only used when required camera permission/input is available

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/exercises/{exerciseId}/sessions (default: `http://localhost:8080/api/v1/exercises/{exerciseId}/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "safetyCheckId": "2ee450ac-9b1b-5ab6-b7e2-33f814f34c3e",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Without required permission/input the client does not send unsupported camera analysis. 2. After permission, the supported analysis flow can proceed subject to safety clearance.

## Complete EPDS Screening & View Integrated Care

### IT-MOTHER-081 - A completed EPDS screening stores the calculated score as an owner-scoped health metric

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The client-calculated EPDS score is persisted as an EPDS_SCORE health metric for the authenticated mother. 2. The EPDS score is retrievable through metric history.

### IT-MOTHER-082 - EPDS history returns persisted EPDS scores for the authenticated mother without exposing another user's screening history

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. HealthMetricService returns the owner's EPDS_SCORE observations in history/trend. 2. HealthMetricService excludes other users' scores.

### IT-MOTHER-083 - An EPDS response pattern requiring urgent safety guidance keeps that guidance visible while persisting only supported screening data

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Urgent safety guidance remains visible according to client logic. 2. Supported EPDS score/history is persisted without replacing safety messaging with a diagnosis.

### IT-MOTHER-084 - EPDS screening results are presented as non-diagnostic screening information across the integrated client/backend flow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The integrated flow presents screening score/history without asserting a medical diagnosis. 2. Raw answer payload is not exposed in unsupported family-history output.

### IT-MOTHER-085 - An invalid EPDS_SCORE metric is rejected by backend metric validation

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "metricType": "WEIGHT",
  "valueNumeric": 65.0,
  "unit": "kg",
  "measuredAt": "2026-08-12T13:00:14Z",
  "sourceType": "MANUAL",
  "note": "Recorded during CareBridge integration testing.",
  "context": {},
  "periodStart": "2026-08-13T13:00:14Z",
  "periodEnd": "2026-08-13T14:00:14Z",
  "definitionVersion": 1
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. MetricObservationValidator rejects invalid EPDS_SCORE data. 2. No invalid observation is persisted.

### IT-MOTHER-086 - A user cannot retrieve or mutate another mother's EPDS score history

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Health-metric ownership checks reject unauthorized access. 2. User B's EPDS history remains unchanged.

### IT-MOTHER-087 - Verify the integrated journey-to-health-metric flow uses the authenticated mother's current journey context when recording a maternal metric

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/metrics (default: `http://localhost:8080/api/v1/journeys/{journeyId}/metrics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/recommendations/profile (default: `http://localhost:8080/api/v1/recommendations/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. Journey context and HealthMetricService remain owner-consistent. 2. The persisted observation is retrievable for the same mother without crossing journey ownership.

### IT-MOTHER-088 - Verify the integrated postpartum-journey flow allows postpartum logging only after the mother's lifecycle reaches the required postpartum state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/journeys/{journeyId}/postpartum-logs (default: `http://localhost:8080/api/v1/journeys/{journeyId}/postpartum-logs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{journeyId}` with the real value from GET /api/v1/journeys/me/dashboard or the response from POST /api/v1/journeys. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "submissionId": "d9748180-7b7e-579c-bacd-2098a6c2719d",
  "logDate": "2026-08-12"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The lifecycle transition is persisted first. 2. Postpartum ownership/state validation then permits the log, which is returned in the same mother's postpartum history.

### IT-MOTHER-089 - A confirmed mother-journey transition affects subsequent personalized recommendations without duplicating lifecycle state

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/recommendations/profile (default: `http://localhost:8080/api/v1/recommendations/profile`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. RecommendationService uses the updated persisted journey/transition context. 2. Recommendations reflect the new state as implemented. 3. No duplicate lifecycle transition is created.

# Baby_Growth_Vaccination

## Create Baby Profile

### IT-BABY-001 - An authenticated caregiver can create a valid baby profile and retrieve the persisted profile

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies (default: `http://localhost:8080/api/v1/babies`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "nickname": "Baby An",
  "birthDate": "2026-01-15"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies (default: `http://localhost:8080/api/v1/babies`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 403`. After Send: 1. BabyController delegates to BabyService. 2. The profile is validated. 3. The profile is persisted for the authorized caregiver. 4. The profile is returned by subsequent retrieval.

## Manage Baby Profile

### IT-BABY-002 - A caregiver can list and open only baby profiles they are authorized to access

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies (default: `http://localhost:8080/api/v1/babies`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. BabyService and BabyAccessPolicy return only baby profiles accessible to the caregiver. 2. Detail retrieval is allowed only for babies authorized to the caregiver.

### IT-BABY-003 - An authorized caregiver can update a baby's supported profile fields and retrieve the persisted changes

**Step 1**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId} (default: `http://localhost:8080/api/v1/babies/{babyId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "nickname": "Baby An"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId} (default: `http://localhost:8080/api/v1/babies/{babyId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. BabyAccessPolicy authorizes the request. 2. BabyService persists valid changes. 3. Subsequent retrieval returns the updated profile.

### IT-BABY-004 - A caregiver can switch the active baby and subsequent baby-care views use the newly selected authorized baby

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/active (default: `http://localhost:8080/api/v1/babies/{babyId}/active`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. BabyController switchActive updates the supported active-baby context. 2. Subsequent baby-care requests display data for the selected authorized baby.

### IT-BABY-005 - An authorized caregiver can archive a baby profile and the archived state affects subsequent active-profile retrieval

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/archive (default: `http://localhost:8080/api/v1/babies/{babyId}/archive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. BabyService persists the archive/status change. 2. Subsequent active-profile behavior follows the implemented archived-state rules.

### IT-BABY-006 - A caregiver cannot read, update, switch to, or archive an unauthorized baby profile

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId} (default: `http://localhost:8080/api/v1/babies/{babyId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId} (default: `http://localhost:8080/api/v1/babies/{babyId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "nickname": "Baby An"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. BabyAccessPolicy rejects unauthorized operations. 2. The baby profile remains unchanged. 3. Private data are not disclosed.

## Manage Baby Daily Care Logs

### IT-BABY-007 - An authorized caregiver can add a valid daily-care log for the selected baby

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "logType": "FEEDING",
  "unit": "kg",
  "note": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. BabyDailyLogService validates baby access and active status. 2. BabyDailyLogService persists the log for the selected baby. 3. BabyDailyLogService returns it on reload.

### IT-BABY-008 - A caregiver can list and retrieve detail for daily-care logs belonging to an authorized baby

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs/{logId} (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs/{logId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. BabyDailyLogController/Service returns logs associated with the selected authorized baby. 2. The requested owned log detail is returned for the same authorized baby.

### IT-BABY-009 - An authorized caregiver can update a valid daily-care log and retrieve the persisted changes

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs/{logId} (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs/{logId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "startedAt": "2026-08-13T13:00:14Z",
  "unit": "kg",
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. BabyDailyLogService validates the baby-log relationship. 2. BabyDailyLogService persists changes. 3. Subsequent retrieval reflects the updated log.

### IT-BABY-010 - An authorized caregiver can delete a baby's daily-care log according to implemented deletion semantics

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs/{logId} (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs/{logId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs/{logId} (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs/{logId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. BabyDailyLogService validates access. 2. BabyDailyLogService removes the log according to implemented semantics. 3. It is no longer returned as an active log.

### IT-BABY-011 - A daily-care log cannot be manipulated through a mismatched baby path or another baby's identifier

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/daily-logs/{logId} (default: `http://localhost:8080/api/v1/babies/{babyId}/daily-logs/{logId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{logId}` with the real value from the response from the baby daily-log creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The validateLogBelongsToBaby check rejects the mismatched relationship. 2. The log remains unchanged.

## Manage Baby Health Records

### IT-BABY-012 - An authorized caregiver can create a baby health record without an attachment and retrieve it on the baby's health timeline

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recordType": "ULTRASOUND",
  "title": "CareBridge integration test",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `filter = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. HealthRecordService persists the baby-linked owner-authorized record. 2. The baby's active health timeline returns it.

### IT-BABY-013 - An authorized caregiver can create a baby health record with an attachment and retrieve file metadata

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "recordType": "ULTRASOUND",
  "title": "CareBridge integration test",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{recordId} (default: `http://localhost:8080/api/v1/health-records/{recordId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Configured file storage succeeds. 2. The health record and associated file metadata are persisted. 3. The health record and associated file metadata are returned for the authorized baby.

### IT-BABY-014 - Baby health-record timeline and detail are scoped to the selected authorized baby

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records (default: `http://localhost:8080/api/v1/health-records`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `filter = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{recordId} (default: `http://localhost:8080/api/v1/health-records/{recordId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. Only records authorized for the selected baby are returned. 2. Cross-baby or unauthorized record access is rejected.

### IT-BABY-015 - An authorized caregiver can update a baby's health record and retrieve the persisted changes

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{id} (default: `http://localhost:8080/api/v1/health-records/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "sourceType": "OTHER",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. HealthRecordService validates access. 2. HealthRecordService persists the update. 3. Subsequent retrieval reflects the changed baby health record.

### IT-BABY-016 - Archiving a baby health record removes it from the active health-record timeline

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/health-records/{id}/archive (default: `http://localhost:8080/api/v1/health-records/{id}/archive`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. HealthRecordService persists archive state. 2. The archived record is excluded from the active timeline according to implemented filtering.

### IT-BABY-017 - File-storage failure during baby health-record upload does not create a false successful attachment state

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/files/health-records (default: `http://localhost:8080/api/v1/files/health-records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Storage failure is handled safely. 2. The system does not present an unconfirmed attachment as successfully persisted. 3. The workflow remains in a consistent recoverable state.

## Record Growth Data

### IT-BABY-018 - An authorized caregiver can add a valid growth measurement for a baby

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-measurements (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-measurements`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "measuredDate": "2026-08-12",
  "sourceType": "OTHER",
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. GrowthService applies BabyAccessPolicy. 2. GrowthService persists the valid measurement. 3. GrowthService returns it in the baby's growth history/chart data.

## View Growth Data

### IT-BABY-019 - Baby growth history and chart data are generated from persisted measurements for the selected authorized baby

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-chart (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-chart`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. GrowthService returns only authorized measurements for the selected baby. 2. Dates and values remain consistent across history and chart responses.

## Manage Growth Data

### IT-BABY-020 - An authorized caregiver can update a baby's growth measurement and retrieve the changed chart/history data

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId} (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{growthMeasurementId}` with the real value from the response from POST/GET growth measurements for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "measuredDate": "2026-08-12",
  "sourceType": "OTHER",
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. GrowthService validates access. 2. GrowthService persists the update. 3. Subsequent history/chart data reflect the changed measurement without unintended duplication.

### IT-BABY-021 - An authorized caregiver can delete a baby's growth measurement and the result is removed from subsequent growth data

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId} (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{growthMeasurementId}` with the real value from the response from POST/GET growth measurements for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. GrowthService applies BabyAccessPolicy and deletion semantics. 2. The removed measurement no longer appears in active growth history/chart data.

### IT-BABY-022 - A caregiver cannot read or mutate growth measurements for an unauthorized baby

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-measurements (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-measurements`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId} (default: `http://localhost:8080/api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{growthMeasurementId}` with the real value from the response from POST/GET growth measurements for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "measuredDate": "2026-08-12",
  "sourceType": "OTHER",
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. BabyAccessPolicy rejects unauthorized growth operations. 2. Baby B's measurement data remain unchanged and undisclosed.

## Manage Development Milestones

### IT-BABY-023 - An authorized caregiver can add a valid development milestone observation for a baby

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "milestoneType": "OTHER",
  "achievedDate": "2026-08-12",
  "note": "Recorded during CareBridge integration testing.",
  "sourceType": "OTHER"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. MilestoneService validates baby access/path. 2. MilestoneService persists the milestone observation. 3. It appears in the baby's milestone list.

### IT-BABY-024 - Milestone listing returns development observations for the selected authorized baby

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. MilestoneController/Service returns only authorized milestone observations for the selected baby. 2. The supported achievement state is returned.

### IT-BABY-025 - An authorized caregiver can update a baby's milestone observation and retrieve the persisted changes

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones/{milestoneId} (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones/{milestoneId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{milestoneId}` with the real value from the response from POST/GET milestones for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "achievedDate": "2026-08-12",
  "note": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. MilestoneService validates the path-baby relationship. 2. MilestoneService persists changes. 3. Subsequent listing reflects the updated observation.

### IT-BABY-026 - An authorized caregiver can delete a baby's milestone observation

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones/{milestoneId} (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones/{milestoneId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{milestoneId}` with the real value from the response from POST/GET milestones for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. MilestoneService validates access/path. 2. MilestoneService removes the observation according to implemented semantics. 3. It is no longer returned as active.

### IT-BABY-027 - Milestone operations reject a milestone identifier that does not belong to the baby specified in the request path

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/babies/{babyId}/milestones/{milestoneId} (default: `http://localhost:8080/api/v1/babies/{babyId}/milestones/{milestoneId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{milestoneId}` with the real value from the response from POST/GET milestones for the selected baby. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. MilestoneService path-baby validation rejects the mismatched request. 2. Baby B's milestone remains unchanged.

## Manage Vaccination Schedule & Records

### IT-BABY-028 - An authorized caregiver can retrieve the reference vaccination schedule for the selected baby

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/schedule (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/schedule`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. VaccinationService applies BabyAccessPolicy. 2. VaccinationService combines the supported reference schedule with the baby's vaccination context for client display.

### IT-BABY-029 - An authorized caregiver can list vaccination records belonging to the selected baby

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/records (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/records`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. VaccinationService returns owner-authorized records for the selected baby. 2. VaccinationService excludes records for inaccessible babies.

### IT-BABY-030 - An authorized caregiver can add a valid vaccination record for a baby and retrieve it

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/records (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/records`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "vaccineName": "CareBridge QA value",
  "doseNumber": 1,
  "administeredDate": "2026-08-12"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/schedule (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/schedule`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. VaccinationService validates BabyAccessPolicy. 2. VaccinationService persists the record. 3. Subsequent record listing returns the new vaccination state.

### IT-BABY-031 - An authorized caregiver can update a baby's vaccination record and retrieve the persisted changes

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/records/{recordId} (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/records/{recordId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "vaccineName": "CareBridge QA value"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/schedule (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/schedule`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. VaccinationService validates access. 2. VaccinationService persists the update. 3. Subsequent vaccination views reflect the changed record.

### IT-BABY-032 - An authorized caregiver can delete a baby's vaccination record according to implemented semantics

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/records/{recordId} (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/records/{recordId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step; replace `{recordId}` with the real value from the response from the health-record creation/list step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/schedule (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/schedule`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. VaccinationService validates access. 2. VaccinationService applies deletion. 3. The removed record no longer appears as an active vaccination record.

### IT-BABY-033 - Marking a scheduled vaccination as completed persists the administered/completed state

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/completions (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/completions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "vaccineName": "CareBridge QA value",
  "doseNumber": 1,
  "administeredDate": "2026-08-12"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/schedule (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/schedule`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. VaccinationService persists the supported completion transition. 2. Schedule and vaccination records consistently reflect completed/administered state.

### IT-BABY-034 - Postponing a baby's scheduled vaccination persists the new scheduling state without marking it completed

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/postponements (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/postponements`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "vaccineName": "CareBridge QA value",
  "doseNumber": 1,
  "newScheduledDate": "2026-08-12",
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. VaccinationService persists the postponement state/date. 2. The vaccination remains non-completed. 3. The updated schedule is returned.

### IT-BABY-035 - Creating a vaccination reminder integrates the baby's vaccination data with ReminderService and FCM notification delivery

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reminders/vaccination (default: `http://localhost:8080/api/v1/reminders/vaccination`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "babyId": "28ea0184-37b3-4d4a-a7bf-ae1e72fb3860",
  "title": "CareBridge integration test",
  "scheduledAt": "2026-08-13T13:00:14Z",
  "recurrenceType": "NONE",
  "recurrenceEndDate": "2026-08-13T14:00:14Z",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The createVaccinationReminder operation persists the reminder for the authorized vaccination context. 2. NotificationService/FCM processes the due reminder without unintended duplicate notifications.

### IT-BABY-036 - Baby access control and retry safeguards prevent unauthorized or duplicate vaccination state transitions and notifications

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/vaccination/babies/{babyId}/completions (default: `http://localhost:8080/api/v1/vaccination/babies/{babyId}/completions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{babyId}` with the real value from GET /api/v1/babies or the response from the baby-creation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "vaccineName": "CareBridge QA value",
  "doseNumber": 1,
  "administeredDate": "2026-08-12"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. BabyAccessPolicy rejects unauthorized access. 2. Retry/state guards prevent unintended duplicate vaccination transitions, reminders, or notifications.

# Community_QA_Moderation

## Browse Questions

### IT-COMM-001 - Authenticated users can browse the visible Community Q&A feed

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/feed (default: `http://localhost:8080/api/v1/community/feed`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The client calls the community feed API. 2. The service queries visible questions in PostgreSQL. 3. The service returns only community-visible records.

### IT-COMM-003 - A user can open a visible question and its visible answers

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Question detail is loaded from PostgreSQL. 2. Visible answers for the selected question are returned in the detail view.

## Search Questions

### IT-COMM-002 - Community Q&A feed filters and pagination return the matching visible records

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions (default: `http://localhost:8080/api/v1/community/questions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `urgency = LOW` (optional/filter); query `hasExpertAnswer = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Filtering and paging are applied through the backend service/repository. 2. The client displays only matching visible records without duplication across pages.

## Create Question

### IT-COMM-004 - A Mother or Family user can create a valid community question

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions (default: `http://localhost:8080/api/v1/community/questions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "topicId": "f92bb216-9804-4daf-b1de-6dc1e346502b",
  "title": "CareBridge integration test",
  "body": "I need safe guidance for nausea during pregnancy.",
  "stage": "PREGNANCY",
  "pregnancyWeek": 20,
  "urgency": "NORMAL",
  "isAnonymous": false
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. CommunityQuestionController delegates to CommunityQuestionService. 2. The new question is persisted before success is shown. 3. The new question appears in the user's question list.

### IT-COMM-005 - An unsupported duplicate question submission is handled without creating duplicate confirmed records

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions (default: `http://localhost:8080/api/v1/community/questions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "topicId": "f92bb216-9804-4daf-b1de-6dc1e346502b",
  "title": "CareBridge integration test",
  "body": "I need safe guidance for nausea during pregnancy.",
  "stage": "PREGNANCY",
  "pregnancyWeek": 20,
  "urgency": "NORMAL",
  "isAnonymous": false
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend duplicate/state rules are applied. 2. A retry does not create multiple confirmed question records for the same logical submission.

## Edit Question

### IT-COMM-006 - An author can edit their own community question

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "stage": "PREGNANCY",
  "pregnancyWeek": 20
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Ownership is rechecked server-side. 2. The persisted question is updated. 3. The reloaded detail shows the new values.

### IT-COMM-008 - A user cannot edit another user's community question

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "stage": "PREGNANCY",
  "pregnancyWeek": 20
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side ownership checks reject the mutation. 2. User B's persisted question remains unchanged.

## Delete Question

### IT-COMM-007 - An author can delete their own community question

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `204`. After Send: 1. The backend verifies ownership. 2. The backend persists the delete/removal state. 3. The question is no longer returned as an active user question.

### IT-COMM-009 - A user cannot delete another user's community question

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The protected API rejects the unauthorized delete. 2. The target question remains in its prior persisted state.

## Submit Answer

### IT-COMM-010 - An approved active expert can post an expert-labeled answer

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{questionId}/answers (default: `http://localhost:8080/api/v1/community/questions/{questionId}/answers`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{questionId}` with the real value from the response from creating a question or the community question/feed list. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "body": "I need safe guidance for nausea during pregnancy.",
  "isPersonalExperience": true,
  "experienceTag": "CareBridge QA value"
}
```

**Expected Result After Send:** HTTP status: `403`. After Send: 1. CommunityAnswerService persists the answer. 2. Expert profile resolution succeeds. 3. The returned answer carries the verified expert label.

## View Expert Label

### IT-COMM-011 - An unapproved or inactive expert cannot obtain a verified expert label through answer submission

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend trust checks prevent an unsupported verified contribution. 2. No answer is falsely presented as approved expert content.

## View Answers

### IT-COMM-012 - Visible answers are loaded for the selected community question

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{id} (default: `http://localhost:8080/api/v1/community/questions/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CommunityAnswerRepository returns answers scoped to the question and permitted visibility/status. 2. Hidden or removed answers are not shown as normal visible answers.

## Edit Answer

### IT-COMM-013 - An answer author can edit their own answer

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{questionId}/answers/{id} (default: `http://localhost:8080/api/v1/community/questions/{questionId}/answers/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{questionId}` with the real value from the response from creating a question or the community question/feed list; replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "body": "I need safe guidance for nausea during pregnancy.",
  "isPersonalExperience": true,
  "experienceTag": "CareBridge QA value"
}
```

**Expected Result After Send:** HTTP status: `403`. After Send: 1. The backend verifies author ownership. 2. The backend persists the edit. 3. The backend returns the updated answer in question detail.

### IT-COMM-015 - A user cannot edit another user's answer

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{questionId}/answers/{id} (default: `http://localhost:8080/api/v1/community/questions/{questionId}/answers/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{questionId}` with the real value from the response from creating a question or the community question/feed list; replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "body": "I need safe guidance for nausea during pregnancy.",
  "isPersonalExperience": true,
  "experienceTag": "CareBridge QA value"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side author checks reject the request. 2. The stored answer remains unchanged.

## Delete Answer

### IT-COMM-014 - An answer author can delete their own answer

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/questions/{questionId}/answers/{id} (default: `http://localhost:8080/api/v1/community/questions/{questionId}/answers/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{questionId}` with the real value from the response from creating a question or the community question/feed list; replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend verifies ownership. 2. The backend persists the deletion/removal state. 3. The answer is no longer returned as a normal visible answer.

## Report Content

### IT-COMM-018 - An authenticated user can report a community question

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ReportController validates the target. 2. ReportService persists a ContentReport linked to the question before success is shown.

### IT-COMM-019 - An authenticated user can report a community answer

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The answer target is validated. 2. A persisted report is created. 3. The report becomes available to the moderation workflow.

### IT-COMM-020 - An authenticated user can report a supported account target

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The account target is validated. 2. The report is persisted with target type, target ID, category, source and status.

### IT-COMM-021 - Reporting an invalid or missing target is rejected without creating a report

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ReportService target validation returns an actionable error. 2. No ContentReport is persisted.

### IT-COMM-022 - Duplicate report handling does not create unsupported duplicate active reports for the same target/category context

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Backend duplicate/state handling is applied using report repository checks. 2. Retries do not fabricate multiple confirmed report transitions.

### IT-COMM-046 - Retrying a confirmed report creation does not duplicate the confirmed report workflow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/reports (default: `http://localhost:8080/api/v1/reports`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetType": "QUESTION",
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "category": "INACCURATE_INFORMATION",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Retry handling preserves one logical confirmed report outcome. 2. Retry handling does not duplicate moderation side effects.

## Moderate Content

### IT-COMM-016 - A Moderator can remove a community question through the moderation-authorized delete path

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions (default: `http://localhost:8080/api/v1/admin/moderation/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "targetType": "QUESTION",
  "actionType": "APPROVE"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/moderator/community/dashboard (default: `http://localhost:8080/api/v1/moderator/community/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. Moderator authorization is accepted by the service. 2. The resulting question state is persisted before the UI confirms completion.

### IT-COMM-017 - A Moderator can remove a community answer through the moderation-authorized delete path

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions (default: `http://localhost:8080/api/v1/admin/moderation/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "targetType": "QUESTION",
  "actionType": "APPROVE"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/moderator/community/dashboard (default: `http://localhost:8080/api/v1/moderator/community/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. Moderator authorization is accepted. 2. The resulting answer state is persisted. 3. The community view reflects the moderated state.

### IT-COMM-023 - A Moderator can view dashboard counts grouped by report status

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/moderator/community/dashboard (default: `http://localhost:8080/api/v1/moderator/community/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CommunityDashboardService queries report status aggregates from PostgreSQL. 2. CommunityDashboardService returns counts that match persisted report states.

### IT-COMM-024 - A Moderator can load the pending community content queue

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/pending-content (default: `http://localhost:8080/api/v1/admin/moderation/pending-content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `targetType = QUESTION` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ModerationService applies authorization and status filters. 2. ModerationService returns pending moderation items from persisted data.

### IT-COMM-025 - A Moderator can monitor published community content

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/community-content (default: `http://localhost:8080/api/v1/admin/moderation/community-content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `targetType = QUESTION` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ModerationService returns currently visible community content from repository-backed state for monitoring.

### IT-COMM-026 - A Moderator can retrieve moderation content detail for a supported target

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/content/{targetType}/{targetId} (default: `http://localhost:8080/api/v1/admin/moderation/content/{targetType}/{targetId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{targetType}` with the real value from the preceding create/list response for `targetType`; replace `{targetId}` with the real value from the ID of the question/answer/content selected for this moderation case. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ModerationController resolves the target. 2. ModerationController returns persisted content/report context required for review without exposing unauthorized data.

### IT-COMM-027 - A Moderator can claim an unclaimed community report

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/claim (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/claim`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Claim state is rechecked. 2. Claim state is persisted. 3. The report is shown as claimed by Moderator A.

### IT-COMM-028 - A second Moderator cannot improperly claim a report already claimed by another Moderator

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/claim (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/claim`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Current claim state is enforced. 2. The conflicting claim is rejected. 3. Ownership of the claim remains unchanged.

### IT-COMM-029 - The claiming Moderator can release a claimed report

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/release (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/release`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The claim transition is persisted. 2. The report becomes available for subsequent authorized claiming.

### IT-COMM-030 - An authorized Moderator can resolve a community report

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/resolve (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/resolve`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "outcome": "APPROVE",
  "expiresAt": "2026-08-13T13:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ModerationService validates current state. 2. ModerationService persists the resolution. 3. ModerationService returns the confirmed report status.

### IT-COMM-031 - An unauthorized Moderator cannot resolve a report when current claim/state rules do not permit the action

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/resolve (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/resolve`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "outcome": "APPROVE",
  "expiresAt": "2026-08-13T13:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Claim/state enforcement rejects the action. 2. The persisted resolution state is unchanged.

### IT-COMM-032 - A Moderator can apply a supported moderation action to community content

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions (default: `http://localhost:8080/api/v1/admin/moderation/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "targetType": "QUESTION",
  "actionType": "APPROVE"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/moderator/community/dashboard (default: `http://localhost:8080/api/v1/moderator/community/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. The moderation action and resulting content state are persisted. 2. The moderation action and resulting content state are reflected consistently across moderator and community views.

### IT-COMM-033 - A Moderator can issue a supported warning action to an account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/account-actions (default: `http://localhost:8080/api/v1/admin/moderation/account-actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetUserId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "actionType": "APPROVE",
  "expiresAt": "2026-08-13T13:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ModerationService persists the action and resulting account moderation state with audit evidence.

### IT-COMM-034 - A Moderator can apply a supported suspension action to an account

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/account-actions (default: `http://localhost:8080/api/v1/admin/moderation/account-actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetUserId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "actionType": "APPROVE",
  "expiresAt": "2026-08-13T13:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The moderation action is persisted. 2. Subsequent access follows the resulting account status policy.

### IT-COMM-035 - An eligible moderation action can be undone by an authorized Moderator

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions/{actionId}/undo (default: `http://localhost:8080/api/v1/admin/moderation/actions/{actionId}/undo`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{actionId}` with the real value from the moderation history/action response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Undo eligibility is rechecked. 2. The reversal is persisted. 3. The target state reflects the valid undo transition.

### IT-COMM-036 - An ineligible or already-undone moderation action cannot be undone again

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions/{actionId}/undo (default: `http://localhost:8080/api/v1/admin/moderation/actions/{actionId}/undo`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{actionId}` with the real value from the moderation history/action response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend rejects the invalid transition. 2. No duplicate reversal is persisted.

### IT-COMM-037 - A Moderator can review AI moderation assessment with persisted content context without autonomous enforcement

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/assessment (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/assessment`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AI assessment is presented as review evidence. 2. No unsupported autonomous moderation decision is persisted solely from the assessment.

### IT-COMM-038 - AI moderation service failure leaves moderation content in a safe retryable state

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/reports/{reportId}/assessment (default: `http://localhost:8080/api/v1/admin/moderation/reports/{reportId}/assessment`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{reportId}` with the real value from the report-creation response or the moderator queue. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. External failure does not fabricate moderation completion. 2. Persisted content/report state remains consistent and retryable.

### IT-COMM-044 - A non-Moderator cannot access moderator queues or moderation mutations

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/queue (default: `http://localhost:8080/api/v1/admin/moderation/queue`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `targetType = QUESTION` (optional/filter); query `status = PENDING` (optional/filter); query `source = USER` (optional/filter); query `priority = NORMAL` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role checks reject moderator-only operations. 2. No moderation data or mutation is exposed.

### IT-COMM-047 - Retrying a confirmed moderation transition does not duplicate moderation actions or side effects

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/moderation/actions (default: `http://localhost:8080/api/v1/admin/moderation/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "targetId": "7ad5ddb4-b7ce-429c-bfc8-f83ca7e35f6c",
  "targetType": "QUESTION",
  "actionType": "APPROVE"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend preserves a single valid transition outcome. 2. Duplicate moderation actions or conflicting target state are not created.

## Manage Community Topics

### IT-COMM-039 - An authorized administrator can create a community topic

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test",
  "description": "Realistic integration test data for CareBridge.",
  "type": "TOPIC",
  "sortOrder": 100
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. CommunityTopicController persists the topic through the service/repository. 2. The topic appears in the managed topic list.

### IT-COMM-040 - An authorized administrator can update an existing community topic

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics/{id} (default: `http://localhost:8080/api/v1/community/topics/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test",
  "description": "Realistic integration test data for CareBridge.",
  "sortOrder": 100
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `includeHidden = false` (optional/filter); query `type = TOPIC` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. The existing topic is updated in PostgreSQL. 2. The returned topic reflects the saved values.

### IT-COMM-041 - Duplicate community topic names are rejected

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test",
  "description": "Realistic integration test data for CareBridge.",
  "type": "TOPIC",
  "sortOrder": 100
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository uniqueness checks reject the duplicate. 2. Existing topics remain unchanged.

### IT-COMM-042 - An authorized administrator can delete an unused community topic

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics/{id} (default: `http://localhost:8080/api/v1/community/topics/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `includeHidden = false` (optional/filter); query `type = TOPIC` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. The backend validates topic state. 2. The backend removes the unused topic. 3. It is no longer returned in managed topics.

### IT-COMM-043 - Deletion of a community topic referenced by questions does not leave broken references

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics/{id} (default: `http://localhost:8080/api/v1/community/topics/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"moderator@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MODERATOR`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `includeHidden = false` (optional/filter); query `type = TOPIC` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The operation is rejected or safely handled according to implemented repository constraints. 2. Referenced questions remain consistent.

### IT-COMM-045 - A non-authorized user cannot manage community topics

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics/{id} (default: `http://localhost:8080/api/v1/community/topics/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/community/topics (default: `http://localhost:8080/api/v1/community/topics`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `includeHidden = false` (optional/filter); query `type = TOPIC` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Protected role checks reject the mutation. 2. Topic data remains unchanged.

# Expert_Network_Contribution

## Create Expert Profile

### IT-EXPERT-001 - An Expert Applicant can create and persist a professional expert profile

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles (default: `http://localhost:8080/api/v1/expert/profiles`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "specialtyId": "8e0a5bdc-a825-54ee-8d5e-1b9366e5e006",
  "hospitalId": "1d86935f-ffcf-5ac1-8a7d-9a11095c67ff"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExpertProfileController delegates to ExpertProfileService. 2. The profile is persisted in PostgreSQL. 3. The reloaded profile reflects the submitted values.

## View Expert Profile

### IT-EXPERT-002 - An Expert can retrieve their own professional profile

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/me (default: `http://localhost:8080/api/v1/expert/profiles/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The protected API resolves the current user. 2. The protected API loads the matching ExpertProfile from the repository. 3. The protected API returns only the caller's own professional profile.

### IT-EXPERT-013 - Users can browse the directory of verified public experts

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/directory (default: `http://localhost:8080/api/v1/expert/directory`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `specialty = CareBridge QA` (optional/filter); query `q = pregnancy` (optional/filter); query `page = 0` (optional/filter); query `size = 10` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ExpertProfileService returns only profiles eligible for verified public listing from PostgreSQL.

### IT-EXPERT-014 - A user can open the public profile of a verified expert

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId} (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend returns the permitted public professional profile for the selected expert without exposing private verification data.

### IT-EXPERT-015 - An unapproved or inactive expert is not exposed as a verified public expert

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId} (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Verified-directory queries honor the current verification/trust state. 2. The profile is not presented as a verified active expert when it is unapproved or inactive.

## Update Expert Profile

### IT-EXPERT-003 - An Expert can update supported professional profile fields and retrieve the persisted result

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/me (default: `http://localhost:8080/api/v1/expert/profiles/me`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "specialtyIds": []
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend validates ownership. 2. The backend persists the update. 3. The reloaded profile returns the new values.

## Submit Verification

### IT-EXPERT-004 - Expert credential evidence can be uploaded and associated with the correct expert profile

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/credentials (default: `http://localhost:8080/api/v1/expert/credentials`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Credential metadata is associated with the caller's expert profile. 2. File storage integration completes before a confirmed state is presented.

## View Verification Status

### IT-EXPERT-005 - An Expert can retrieve the current verification status of their application

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/me/verification-status (default: `http://localhost:8080/api/v1/expert/profiles/me/verification-status`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend loads the caller's expert profile/verification data from PostgreSQL. 2. The backend returns the current verification state.

## Review Verification

### IT-EXPERT-006 - A System Admin can list expert applications by verification status

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/review-cases (default: `http://localhost:8080/api/v1/expert/review-cases`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `search = pregnancy` (optional/filter); query `status = ACTIVE` (optional/filter); query `pageable = CareBridge QA` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExpertProfileService queries expert profiles by verification status. 2. The admin view displays matching persisted applications.

### IT-EXPERT-007 - A System Admin can approve an eligible expert application

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId}/approve (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}/approve`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Approval is persisted through ExpertProfileService. 2. The profile becomes approved according to the implemented trust workflow.

### IT-EXPERT-008 - A System Admin can reject an expert application with the supported review action

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId}/reject (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}/reject`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The rejection decision is persisted. 2. The applicant's verification state reflects the reviewed outcome.

### IT-EXPERT-009 - A System Admin can update the trust status of an expert profile

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId}/trust (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}/trust`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `status = ACTIVE` (required). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Trust status is persisted with admin context. 2. Downstream verified-expert behavior reflects the current trust state.

### IT-EXPERT-010 - A non-admin user cannot approve, reject or change trust status for an expert profile

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId}/approve (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}/approve`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/profiles/{expertProfileId}/reject (default: `http://localhost:8080/api/v1/expert/profiles/{expertProfileId}/reject`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{expertProfileId}` with the real value from GET /api/v1/expert/directory or the expert-profile creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role checks reject the requests. 2. The expert verification/trust state remains unchanged.

### IT-EXPERT-011 - Identity or face verification integrates with the configured verification service before a verified state is accepted

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/verify-face (default: `http://localhost:8080/api/v1/expert/verify-face`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/identity/{attemptId}/review (default: `http://localhost:8080/api/v1/expert/identity/{attemptId}/review`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{attemptId}` with the real value from the expert verification submission/status response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reviewStatus": "PENDING_REVIEW"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend receives the verification result. 2. The backend persists the supported verification state. 3. The backend does not mark the profile verified before the integration succeeds.

### IT-EXPERT-012 - File-storage or face-verification failure leaves the expert application in a safe retryable state

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/credentials (default: `http://localhost:8080/api/v1/expert/credentials`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/verify-face (default: `http://localhost:8080/api/v1/expert/verify-face`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. No false verified success is persisted during failure. 2. Application data remains consistent. 3. The operation can be retried safely.

## Manage Expert Availability & Presence

### IT-EXPERT-016 - A verified Expert can create an availability slot

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability (default: `http://localhost:8080/api/v1/expert/availability`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "startAt": "2026-08-13T13:00:14Z",
  "endAt": "2026-08-13T14:00:14Z",
  "channelType": "OTHER"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExpertAvailabilityService resolves the caller's expert profile. 2. ExpertAvailabilityService persists the slot. 3. ExpertAvailabilityService returns it in My Availability.

### IT-EXPERT-017 - An Expert can retrieve only their own availability slots

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Repository lookup is scoped to Expert A's expertProfileId. 2. Expert B's availability slots are not returned.

### IT-EXPERT-018 - An Expert can delete one of their own availability slots

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/{id} (default: `http://localhost:8080/api/v1/expert/availability/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Ownership is rechecked. 2. The slot is removed from persisted availability state.

### IT-EXPERT-019 - An Expert cannot delete an availability slot belonging to another Expert

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/{id} (default: `http://localhost:8080/api/v1/expert/availability/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side expert-profile scoping rejects the mutation. 2. Expert B's slot remains unchanged.

### IT-EXPERT-020 - An Expert can update their online status through the availability service

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/online-status (default: `http://localhost:8080/api/v1/expert/online-status`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "online": true
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. The backend resolves the expert profile. 2. The backend persists the supported online status state returned to the client.

### IT-EXPERT-021 - An Expert can share location through the supported expert availability flow

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/location/share (default: `http://localhost:8080/api/v1/expert/location/share`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "accuracyMeters": 1.0,
  "expiresAt": "2026-08-13T13:00:14Z",
  "consentReference": "3008d70e-e256-552b-9cc2-85ae53cc035f"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. ExpertAvailabilityService persists the location-share state for the caller's expert profile. 2. ExpertAvailabilityService returns the confirmed result.

### IT-EXPERT-022 - An Expert can stop an active location share

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/location/share (default: `http://localhost:8080/api/v1/expert/location/share`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/expert/availability/me (default: `http://localhost:8080/api/v1/expert/availability/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. The backend ends the caller's location-share state. 2. The client no longer treats the share as active.

## Create Consultation Request

### IT-EXPERT-023 - A Mother or Family user can create a free expert conversation request for an eligible expert

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests (default: `http://localhost:8080/api/v1/consultation-requests`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "clientRequestId": "a9326840-d8fb-58b6-94d3-8fdae6ba585e",
  "expertProfileId": "2cbeb01a-9036-42b6-b393-27c5ce8aaedc",
  "topic": "Pregnancy care consultation",
  "description": "Realistic integration test data for CareBridge.",
  "preferredWindowStart": "2026-08-13T13:00:14Z",
  "preferredWindowEnd": "2026-08-13T14:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConsultationRequestService validates expert eligibility. 2. ConsultationRequestService persists the request. 3. ConsultationRequestService returns the confirmed pending request.

### IT-EXPERT-024 - Consultation request retries using the same client request identity do not create duplicate requests

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests (default: `http://localhost:8080/api/v1/consultation-requests`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "clientRequestId": "ff08a554-39c0-5186-9d04-add2f85e77fd",
  "expertProfileId": "2cbeb01a-9036-42b6-b393-27c5ce8aaedc",
  "topic": "Pregnancy care consultation",
  "description": "Realistic integration test data for CareBridge.",
  "preferredWindowStart": "2026-08-13T13:00:14Z",
  "preferredWindowEnd": "2026-08-13T14:00:14Z"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository idempotency lookup by requesterUserId and clientRequestId preserves one logical request. 2. Duplicate confirmed requests are avoided.

## Manage Consultation Request

### IT-EXPERT-025 - A requester can list their own expert conversation requests

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/mine (default: `http://localhost:8080/api/v1/consultation-requests/mine`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = PENDING` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConsultationRequestRepository queries by requesterUserId. 2. ConsultationRequestRepository returns only the caller's request records.

### IT-EXPERT-026 - A requester can open an authorized expert conversation request detail

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id} (default: `http://localhost:8080/api/v1/consultation-requests/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConsultationRequestPolicy authorizes the caller. 2. The backend returns the persisted request detail.

### IT-EXPERT-027 - A requester can cancel their own request when the current state permits cancellation

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/cancel (default: `http://localhost:8080/api/v1/consultation-requests/{id}/cancel`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Policy checks validate ownership/current state. 2. The cancellation is persisted before the UI confirms it.

### IT-EXPERT-028 - A user cannot view or cancel another user's consultation request

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/cancel (default: `http://localhost:8080/api/v1/consultation-requests/{id}/cancel`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConsultationRequestPolicy rejects unauthorized access. 2. The persisted request remains unchanged.

### IT-EXPERT-029 - An Expert can list assigned/pending consultation requests and the pending summary

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/assigned (default: `http://localhost:8080/api/v1/consultation-requests/assigned`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = PENDING` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/pending-summary (default: `http://localhost:8080/api/v1/consultation-requests/pending-summary`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend resolves the Expert profile. 2. The backend queries requests by expertProfileId/status. 3. The backend returns the matching queue and summary.

### IT-EXPERT-030 - An eligible Expert can accept a consultation request and atomically create or link the direct conversation

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/accept (default: `http://localhost:8080/api/v1/consultation-requests/{id}/accept`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Acceptance is persisted. 2. Expert eligibility is rechecked. 3. A direct conversation is created or linked atomically. 4. Requester notification is queued/sent without a duplicate transition.

### IT-EXPERT-031 - An Expert can reject a consultation request through the Backend-supported PATCH contract and notify the requester

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/reject (default: `http://localhost:8080/api/v1/consultation-requests/{id}/reject`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The rejection is persisted. 2. The requester decision notification is produced according to the supported contract.

### IT-EXPERT-032 - Verify the known Expert Web reject-method mismatch is not recorded as an operational success

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/reject (default: `http://localhost:8080/api/v1/consultation-requests/{id}/reject`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. NEEDS_CONFIRMATION: until the Web client is aligned to the Backend PATCH contract, the reject branch must not be marked successful and the request must not be falsely transitioned.

### IT-EXPERT-033 - An ineligible Expert cannot accept a pending consultation request

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{id}/accept (default: `http://localhost:8080/api/v1/consultation-requests/{id}/accept`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConsultationRequestPolicy rejects the transition. 2. No accepted state or direct conversation is created from the invalid acceptance.

## Manage Direct Conversation

### IT-EXPERT-034 - A participant can list their direct expert conversations

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations (default: `http://localhost:8080/api/v1/direct-conversations`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. DirectConversationService queries conversations for the current participant. 2. DirectConversationService returns the authorized conversation summaries ordered by activity.

### IT-EXPERT-035 - Only accepted-conversation participants can open conversation detail and timeline

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/timeline (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/timeline`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `after = CareBridge QA` (optional/filter); query `before = CareBridge QA` (optional/filter); query `limit = 30` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Participants receive the persisted conversation/timeline. 2. Non-participants are rejected by server-side conversation policy.

### IT-EXPERT-036 - Sending a direct message persists one idempotent message and updates the conversation timeline

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/messages (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/messages`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "clientMessageId": "beaea108-e1aa-5e2d-9440-4f89acffc415",
  "messageType": "OTHER",
  "attachmentId": "e0348586-2a7d-5973-987c-e73077eb24f2"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/timeline (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/timeline`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `after = CareBridge QA` (optional/filter); query `before = CareBridge QA` (optional/filter); query `limit = 30` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. DirectMessageService validates participation. 2. DirectMessageService persists one message for the clientMessageId/payload. 3. The timeline shows one logical message without duplication.

### IT-EXPERT-037 - A conversation participant can access a permitted message attachment while non-participants cannot

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response; replace `{messageId}` with the real value from the response from sending/listing messages in the selected conversation. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Attachment ownership/access checks and conversation membership permit authorized viewing. 2. Attachment ownership/access checks and conversation membership reject unauthorized access.

### IT-EXPERT-038 - A message sender can recall their own direct message without allowing another participant to recall it as sender

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response; replace `{messageId}` with the real value from the response from sending/listing messages in the selected conversation. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The valid recall state is persisted for the sender's message. 2. Unauthorized recall attempts are rejected.

## Start Direct Call

### IT-EXPERT-039 - A conversation participant can initiate a voice/video call and obtain join credentials from ZegoCloud integration

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/calls (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/calls`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "callType": "VIDEO"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ConversationCallService verifies participation. 2. ConversationCallService persists the call. 3. ConversationCallService issues supported ZegoCloud room credentials only for the authorized active call.

## Manage Direct Call

### IT-EXPERT-040 - Only conversation participants can answer, decline or end a direct call and that the final call state is persisted

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/calls/{callId}/answer (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/calls/{callId}/answer`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response; replace `{callId}` with the real value from the response from starting/listing a direct call. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/calls/{callId}/decline (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/calls/{callId}/decline`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response; replace `{callId}` with the real value from the response from starting/listing a direct call. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 3**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/direct-conversations/{conversationId}/calls/{callId}/end (default: `http://localhost:8080/api/v1/direct-conversations/{conversationId}/calls/{callId}/end`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"expert2@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `EXPERT`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{conversationId}` with the real value from the accepted consultation/direct-conversation response; replace `{callId}` with the real value from the response from starting/listing a direct call. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Authorized call transitions are persisted. 2. Unauthorized actors are rejected. 3. No conflicting call state is created.

# AI_Nurse_Risk_Triage

## Submit Symptom Intake

### IT-AI-001 - An authorized user can start an AI triage conversation

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/conversation/start (default: `http://localhost:8080/api/v1/triage/intake/conversation/start`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "initialText": "I have a headache and feel dizzy.",
  "stage": "PREGNANCY",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. IntakeController creates or initializes an authorized intake context. 2. IntakeController returns a conversation response without diagnostic claims.

### IT-AI-002 - An authorized user can run a symptom intake and persist the triage session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. TriageService evaluates the request with the approved AI service. 2. TriageService persists the IntakeSession. 3. TriageService returns the supported risk result to the client.

### IT-AI-003 - Retrying an intake with the same client request identity does not create duplicate triage sessions

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository lookup by userId and clientRequestId preserves one logical session. 2. No duplicate confirmed intake session is created.

### IT-AI-004 - An authorized user can continue an existing AI triage conversation

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/conversation/continue (default: `http://localhost:8080/api/v1/triage/intake/conversation/continue`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "intakeSessionId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The continuation is resolved to the caller's intake context. 2. The continuation is processed by the triage service. 3. The persisted conversation/session state remains consistent.

### IT-AI-005 - A continuation token resolves only to the caller's triage session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/continuations/resolve (default: `http://localhost:8080/api/v1/triage/intake/continuations/resolve`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `<PASTE_RUNTIME_TOKEN_HERE>` with the token returned or delivered by the immediately preceding flow step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "token": "<PASTE_RUNTIME_TOKEN_HERE>"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The owner receives the continuation descriptor. 2. Repository scoping by userId and continuationToken prevents another user from resolving it.

### IT-AI-006 - An authorized user can acknowledge a valid triage continuation

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/continuations/acknowledge (default: `http://localhost:8080/api/v1/triage/intake/continuations/acknowledge`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `<PASTE_RUNTIME_TOKEN_HERE>` with the token returned or delivered by the immediately preceding flow step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "token": "<PASTE_RUNTIME_TOKEN_HERE>"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. IntakeController validates the token against the caller. 2. IntakeController returns the supported acknowledgement without creating a duplicate session.

## View Triage History

### IT-AI-007 - A user can retrieve the result of their own triage session

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/{sessionId} (default: `http://localhost:8080/api/v1/triage/intake/{sessionId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend loads the IntakeSession by session ID and authenticated user. 2. The backend returns the persisted triage result.

### IT-AI-008 - A user can view their AI triage history in persisted order

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. IIntakeSessionRepository returns sessions scoped to the current user and ordered by creation time as implemented.

### IT-AI-009 - A user cannot retrieve another user's triage session or result

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/{sessionId} (default: `http://localhost:8080/api/v1/triage/intake/{sessionId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{sessionId}` with the real value from GET /api/v1/sessions using the same account; choose the row required by the case (`current: false` for revocation). Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Owner-scoped repository access rejects the request. 2. No triage data belonging to User B is disclosed.

## Classify Risk Level

### IT-AI-010 - A GREEN triage outcome is presented as non-diagnostic guidance

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The integrated triage result returns GREEN when supported by the rules/model. 2. The client presents the result as non-diagnostic guidance rather than a medical diagnosis.

### IT-AI-011 - A YELLOW triage outcome is persisted and presented with the supported caution guidance

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The YELLOW outcome is returned. 2. The YELLOW outcome is persisted for the session with supported non-diagnostic guidance. 3. History shows the same risk state.

### IT-AI-012 - A RED triage outcome is persisted and exposes the supported escalation options

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The RED outcome is persisted. 2. The client exposes supported emergency/expert handoff actions without claiming diagnosis or guaranteed assistance.

### IT-AI-013 - A matched red-flag rule enforces the configured minimum risk level

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The triage graph/rules apply the red flag. 2. The final risk is not lower than the configured minimum required by the active rule.

### IT-AI-014 - Invalid or unsupported intake input is rejected without a false triage result

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Validation returns an actionable error. 2. No fabricated completed triage result is persisted.

### IT-AI-015 - The Spring triage flow integrates successfully with the approved AI triage service

**Method:** `POST`

**Base URL + Endpoint:** {{aiTriageBaseUrl}}/internal/triage/v2/turn (default: `http://localhost:8001/internal/triage/v2/turn`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "sessionId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591",
  "stateVersion": 0,
  "expectedStateVersion": 0,
  "requestId": "request_1234567890",
  "messageId": "message_1234567890",
  "latestUserMessage": "I have a headache and feel dizzy.",
  "activeProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "selectedTarget": "MOTHER",
  "journeyContext": {},
  "previousState": null,
  "signals": {},
  "measurements": {},
  "answeredQuestionIds": [],
  "submittedOptionCodes": [],
  "expectedRulesetHash": "dd230a924fb51149c5053ade5f1eba1d0837737d572150e396a875dd025adfb3"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Backend sends the approved request to the AI service. 2. Backend validates/maps the integration result. 3. Backend persists the session/result. 4. Backend returns it to the client.

### IT-AI-016 - AI triage service failure leaves the intake in a safe retryable state without false completion

**Method:** `POST`

**Base URL + Endpoint:** {{aiTriageBaseUrl}}/internal/triage/v2/turn (default: `http://localhost:8001/internal/triage/v2/turn`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "sessionId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591",
  "stateVersion": 0,
  "expectedStateVersion": 0,
  "requestId": "request_1234567890",
  "messageId": "message_1234567890",
  "latestUserMessage": "I have a headache and feel dizzy.",
  "activeProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "selectedTarget": "MOTHER",
  "journeyContext": {},
  "previousState": null,
  "signals": {},
  "measurements": {},
  "answeredQuestionIds": [],
  "submittedOptionCodes": [],
  "expectedRulesetHash": "dd230a924fb51149c5053ade5f1eba1d0837737d572150e396a875dd025adfb3"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. External failure does not fabricate GREEN/YELLOW/RED completion. 2. Persisted state remains consistent. 3. The logical intake can be retried safely.

### IT-AI-017 - AI triage output is grounded only by approved evidence sources where evidence grounding is required

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/internal/api/v1/triage/evidence-sources/approved (default: `http://localhost:8080/internal/api/v1/triage/evidence-sources/approved`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `stage = PREGNANCY` (required); header `X-CareBridge-Internal-Key = <PASTE_CONFIGURED_INTERNAL_KEY_HERE>` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The AI flow uses only approved evidence exposed by the internal evidence service. 2. The AI flow does not ground the output on unapproved evidence.

### IT-AI-018 - Unapproved evidence is not accepted as authoritative triage grounding

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/internal/api/v1/triage/evidence-sources/approved (default: `http://localhost:8080/internal/api/v1/triage/evidence-sources/approved`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `stage = PREGNANCY` (required); header `X-CareBridge-Internal-Key = <PASTE_CONFIGURED_INTERNAL_KEY_HERE>` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The triage flow does not present the unapproved source as approved grounding. 2. The triage flow does not fabricate evidence-backed completion.

## Manage Triage Handoffs

### IT-AI-019 - A user can create an emergency-map handoff from their own triage session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/handoff (default: `http://localhost:8080/api/v1/map/emergency/handoff`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triageHandoffId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591",
  "riskLevel": "CareBridge QA value",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. EmergencyMapHandoffService verifies session ownership. 2. EmergencyMapHandoffService validates coordinates. 3. EmergencyMapHandoffService persists the handoff. 4. EmergencyMapHandoffService returns the confirmed handoff to the client.

### IT-AI-020 - A user can list only their own emergency handoffs created from AI triage

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/me (default: `http://localhost:8080/api/v1/map/emergency/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend returns handoffs scoped to User A. 2. User B's handoff records are not exposed.

### IT-AI-021 - A user can retrieve an authorized emergency handoff detail

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/{handoffId} (default: `http://localhost:8080/api/v1/map/emergency/{handoffId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{handoffId}` with the real value from the preceding create/list response for `handoffId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. EmergencyMapHandoffService authorizes the caller. 2. EmergencyMapHandoffService returns the persisted handoff detail linked to the owned triage context.

### IT-AI-022 - Invalid emergency-handoff coordinates are rejected without persisting a false handoff

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/handoff (default: `http://localhost:8080/api/v1/map/emergency/handoff`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triageHandoffId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591",
  "riskLevel": "CareBridge QA value",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Coordinate validation rejects the request. 2. No completed emergency handoff is added to the user's records.

### IT-AI-023 - A user cannot retrieve another user's emergency handoff

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/{handoffId} (default: `http://localhost:8080/api/v1/map/emergency/{handoffId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{handoffId}` with the real value from the preceding create/list response for `handoffId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Owner/role authorization rejects the request. 2. User B's handoff data is not disclosed.

### IT-AI-024 - A user can request expert support from an AI triage result

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/{intakeSessionId}/expert-handoffs (default: `http://localhost:8080/api/v1/triage/intake/{intakeSessionId}/expert-handoffs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{intakeSessionId}` with the real value from the preceding create/list response for `intakeSessionId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "consentAccepted": true,
  "consentPolicyVersion": "v1"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The triage handoff creates a ConsultationRequest through the expert-support service. 2. The request is persisted for the authenticated requester.

### IT-AI-025 - Creating expert support from AI triage triggers the supported expert notification workflow

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/{intakeSessionId}/expert-handoffs (default: `http://localhost:8080/api/v1/triage/intake/{intakeSessionId}/expert-handoffs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{intakeSessionId}` with the real value from the preceding create/list response for `intakeSessionId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "consentAccepted": true,
  "consentPolicyVersion": "v1"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/consultation-requests/{requestId}/triage-context (default: `http://localhost:8080/api/v1/consultation-requests/{requestId}/triage-context`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{requestId}` with the real value from the consultation-request creation/list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The consultation request is persisted. 2. Firebase notification is queued/sent to the expert workflow without changing the triage result into a diagnosis.

### IT-AI-026 - Retrying the same expert handoff from triage does not create duplicate consultation requests

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake/{intakeSessionId}/expert-handoffs (default: `http://localhost:8080/api/v1/triage/intake/{intakeSessionId}/expert-handoffs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{intakeSessionId}` with the real value from the preceding create/list response for `intakeSessionId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "consentAccepted": true,
  "consentPolicyVersion": "v1"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Idempotent request handling preserves one logical consultation request. 2. Duplicate notification side effects are avoided.

## Manage Risk Rules

### IT-AI-027 - A System Admin can list AI red-flag rules using supported filters

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `severity = GREEN` (optional/filter); query `isActive = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. RedFlagRuleService queries PostgreSQL using the supported filters. 2. RedFlagRuleService returns the matching rule records.

### IT-AI-028 - A System Admin can create a valid AI red-flag rule

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding",
  "severity": "GREEN",
  "action": "BLOCK"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The service validates the rule. 2. The service persists it. 3. The service records audit evidence. 4. The service returns it in the rule list.

### IT-AI-029 - Duplicate red-flag keywords are rejected

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding",
  "severity": "GREEN",
  "action": "BLOCK"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository duplicate-keyword validation rejects the new rule. 2. The existing rule remains unchanged.

### IT-AI-030 - A System Admin can update a red-flag rule and persist the new active/severity/action state

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules/{id} (default: `http://localhost:8080/api/v1/admin/red-flag-rules/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/admin/api/v1/evidence-sources (default: `http://localhost:8080/admin/api/v1/evidence-sources`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = ACTIVE` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 403`. After Send: 1. The rule is updated in PostgreSQL. 2. Audit evidence records the administrative change.

### IT-AI-031 - A System Admin can delete a non-default red-flag rule

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules/{id} (default: `http://localhost:8080/api/v1/admin/red-flag-rules/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `severity = GREEN` (optional/filter); query `isActive = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `204, 201`. After Send: 1. The service verifies deletability. 2. The service removes the rule. 3. The service records the supported audit evidence.

### IT-AI-032 - A system-default red-flag rule cannot be deleted

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules/{id} (default: `http://localhost:8080/api/v1/admin/red-flag-rules/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `severity = GREEN` (optional/filter); query `isActive = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend rejects deletion. 2. The system-default rule remains persisted and active/inactive according to its prior state.

### IT-AI-033 - Active red-flag rules can be retrieved by severity for triage execution

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `severity = GREEN` (optional/filter); query `isActive = true` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Only rules satisfying the requested active/severity criteria are returned to the triage execution path.

### IT-AI-034 - A non-admin user cannot create, update or delete AI red-flag rules

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding",
  "severity": "GREEN",
  "action": "BLOCK"
}
```

**Step 2**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules/{id} (default: `http://localhost:8080/api/v1/admin/red-flag-rules/{id}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding"
}
```

**Step 3**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules/{id} (default: `http://localhost:8080/api/v1/admin/red-flag-rules/{id}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role checks reject all administrative mutations. 2. Rule data remains unchanged.

### IT-AI-035 - AI red-flag rule changes retain audit evidence

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/red-flag-rules (default: `http://localhost:8080/api/v1/admin/red-flag-rules`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "keyword": "severe bleeding",
  "severity": "GREEN",
  "action": "BLOCK"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The administrative rule mutation is persisted together with audit details identifying the change type and actor context required by the service.

## Apply Integration Safeguards

### IT-AI-036 - Retrying a confirmed triage-related mutation does not duplicate sessions, handoffs or notifications

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/triage/intake (default: `http://localhost:8080/api/v1/triage/intake`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "symptoms": "Headache and mild dizziness since this morning.",
  "motherProfileId": "74f1d787-e133-499e-9b6d-1cf7bb902e99",
  "stage": "PREGNANCY",
  "gestationalWeeks": 20
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Retry handling preserves one valid logical transition. 2. No duplicate confirmed records are created. 3. No duplicate external side effects are created.

# Emergency_Map_Nearby_Care

## Search Nearby Facilities

### IT-EMER-001 - A user can find nearby active care facilities using backend data and TrackAsia integration

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/nearby-facilities (default: `http://localhost:8080/api/v1/map/nearby-facilities`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `lat = 10.0452` (required); query `lng = 105.7469` (required); query `radiusMeters = 5000` (optional/filter); query `type = hospital` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CareFacilityService validates the location. 2. CareFacilityService queries active facilities. 3. CareFacilityService integrates TrackAsia location/route data when required. 4. CareFacilityService returns nearby care results to the mobile client.

### IT-EMER-002 - Nearby care search respects the supported radius and facility filters

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/nearby-facilities (default: `http://localhost:8080/api/v1/map/nearby-facilities`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `lat = 10.0452` (required); query `lng = 105.7469` (required); query `radiusMeters = 5000` (optional/filter); query `type = hospital` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend applies supported search parameters. 2. The backend returns only matching active facilities for the requested search context.

### IT-EMER-003 - Invalid nearby-search coordinates are rejected without returning misleading facility results

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/nearby-facilities (default: `http://localhost:8080/api/v1/map/nearby-facilities`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `lat = 10.0452` (required); query `lng = 105.7469` (required); query `radiusMeters = 5000` (optional/filter); query `type = hospital` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Input validation returns an actionable error. 2. No false nearby-facility result is presented.

### IT-EMER-004 - TrackAsia unavailability does not fabricate route, distance or ETA data

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/nearby-facilities (default: `http://localhost:8080/api/v1/map/nearby-facilities`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `lat = 10.0452` (required); query `lng = 105.7469` (required); query `radiusMeters = 5000` (optional/filter); query `type = hospital` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The integration failure is surfaced safely. 2. Provider/ETA limitations remain visible. 3. The system does not fabricate navigation estimates.

### IT-EMER-005 - A user can retrieve detail for an active care facility

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/facilities/{id} (default: `http://localhost:8080/api/v1/map/facilities/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CareFacilityController loads the active facility from PostgreSQL. 2. CareFacilityController returns its supported detail to the client.

### IT-EMER-006 - An inactive or missing facility is not returned as a normal active facility detail

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/facilities/{id} (default: `http://localhost:8080/api/v1/map/facilities/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The active-facility repository lookup returns no normal active record. 2. The API responds with the supported not-found/unavailable outcome.

## Navigate to Facility

### IT-EMER-007 - A user can request a route to a selected care facility through TrackAsia

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/route (default: `http://localhost:8080/api/v1/map/route`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "fromLat": 10.7769,
  "fromLng": 106.7009,
  "toLat": 10.7757,
  "toLng": 106.7004
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend/client sends the supported route request to TrackAsia. 2. Route information is returned. 3. Provider-status and ETA limitations remain visible.

### IT-EMER-008 - The mobile client can open device navigation for a selected care facility

**Manual Execution (not a Postman request):** 1. Log in to the CareBridge client as `mother@carebridge.dev` / `Test@1234`. 2. Open nearby facilities and select a facility that has coordinates. 3. Tap the navigation action. 4. Confirm the device navigation application/URL opens with the selected coordinates. 5. Capture the selected facility and the opened navigation destination. Do not invent an HTTP Method or endpoint; current backend evidence does not expose this client action as an API.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

Not applicable — this is a device-navigation action, not an HTTP API request.

**Expected Result After Send:** 1. The facility coordinates/detail are resolved through the backend. 2. The mobile client opens the supported navigation action without claiming guaranteed ETA.

## Contact Support

### IT-EMER-009 - The mobile client can open the device dialer for a care facility's available phone contact

**Manual Execution (not a Postman request):** 1. Log in to the CareBridge client as `mother@carebridge.dev` / `Test@1234`. 2. Open a facility detail that contains a phone number. 3. Tap the call action. 4. Confirm the device dialer opens with that number; placing a real call is not required. 5. Capture the facility phone and dialer screen. Do not invent a Postman endpoint for this client-only action.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

Not applicable — this opens the device dialer and is not an HTTP API request.

**Expected Result After Send:** 1. Facility detail is retrieved from the backend. 2. The mobile client opens the device calling action using the supported facility contact.

## Trigger Emergency Alert

### IT-EMER-010 - Opening the emergency 115 flow persists an emergency session and invokes the device dialer without claiming dispatch

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions (default: `http://localhost:8080/api/v1/emergency/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triggerSource": "POSTMAN_MANUAL",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. EmergencyService persists or opens the user's emergency session. 2. The device dialer is invoked for 115. 3. CareBridge does not claim ambulance dispatch or completed assistance.

### IT-EMER-011 - A user can retrieve their current active emergency session

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions/active (default: `http://localhost:8080/api/v1/emergency/sessions/active`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. EmergencyService loads the active session scoped to the current user. 2. The client restores the persisted emergency state.

### IT-EMER-012 - A user can resolve their own active emergency session

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions/{id}/resolve (default: `http://localhost:8080/api/v1/emergency/sessions/{id}/resolve`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend verifies ownership. 2. The backend persists the resolved status. 3. The session is no longer returned as the user's active emergency session.

### IT-EMER-013 - Escalation from AI triage opens or reuses the correct emergency session without duplicate active sessions

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/map/emergency/handoff (default: `http://localhost:8080/api/v1/map/emergency/handoff`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triageHandoffId": "c08e4eb7-ccd4-4242-9eda-a48f59cbd591",
  "riskLevel": "CareBridge QA value",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions (default: `http://localhost:8080/api/v1/emergency/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triggerSource": "POSTMAN_MANUAL",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. EmergencyService links the triage escalation. 2. EmergencyService opens or reuses the appropriate emergency session. 3. Retry does not create duplicate active emergency sessions.

### IT-EMER-014 - Opening an emergency flow can notify authorized family recipients through Firebase Cloud Messaging

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions (default: `http://localhost:8080/api/v1/emergency/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triggerSource": "POSTMAN_MANUAL",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/location-shares (default: `http://localhost:8080/api/v1/emergency/location-shares`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "latitude": 10.7769,
  "longitude": 106.7009,
  "expiresAt": "2026-08-13T13:00:14Z",
  "consentReference": "28df545e-b45b-5def-abe9-a135b908ae3b"
}
```

**Expected Result After Send:** HTTP status: `201, NEEDS_CONFIRMATION`. After Send: 1. The emergency workflow persists its state. 2. The emergency workflow queues/sends the supported family notification through FCM. 3. The UI does not guarantee delivery or assistance.

### IT-EMER-015 - An authorized Family user can view their family-alert list

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/family-alerts (default: `http://localhost:8080/api/v1/family-alerts`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FamilyAlertService queries notification records for the caller. 2. FamilyAlertService returns the supported family-alert items.

### IT-EMER-016 - Emergency or family-alert detail is accessible only to an authorized caller

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions/{id}/alert (default: `http://localhost:8080/api/v1/emergency/sessions/{id}/alert`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Authorized access returns the persisted alert/emergency detail. 2. Unauthorized access is rejected. 3. Sensitive emergency/location data is not disclosed.

### IT-EMER-017 - Dialer or FCM failure does not create a false claim of emergency dispatch, completed calling or guaranteed family delivery

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions (default: `http://localhost:8080/api/v1/emergency/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triggerSource": "POSTMAN_MANUAL",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Persisted emergency state remains consistent. 2. External failure is represented safely. 3. The system does not claim emergency dispatch, completed assistance, or guaranteed notification delivery.

# Family_Sync_Cooperative_Care

## Manage Care Group

### IT-FAMILY-001 - A Mother can create a care group and persist the owner-linked care context

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "groupName": "CareBridge QA Family Group 20260812",
  "description": "Realistic integration test data for CareBridge."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 403`. After Send: 1. CareGroupService validates the owner and context. 2. CareGroupService persists the CareGroup. 3. CareGroupService returns it in the owner's group list.

### IT-FAMILY-002 - A user can list care groups they own or validly participate in according to the supported flow

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. Backend group/membership queries return only care groups authorized for the current user and current group status.

### IT-FAMILY-003 - Duplicate care-group names for the same owner are rejected

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "groupName": "CareBridge QA Family Group 20260812",
  "description": "Realistic integration test data for CareBridge."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository duplicate-name checks reject the duplicate. 2. No second confirmed group is created.

### IT-FAMILY-004 - A care-group owner can delete their own group and associated membership state safely

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId} (default: `http://localhost:8080/api/v1/care-groups/{groupId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 403`. After Send: 1. Owner authorization is rechecked. 2. Group deletion/status transition and dependent member cleanup are persisted consistently.

### IT-FAMILY-005 - A non-owner cannot delete another user's care group

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId} (default: `http://localhost:8080/api/v1/care-groups/{groupId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups (default: `http://localhost:8080/api/v1/care-groups`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side owner policy rejects the request. 2. The care group remains unchanged.

## Send Family Invitation

### IT-FAMILY-006 - A care-group owner can invite a family member and trigger the supported notification workflow

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/invitations (default: `http://localhost:8080/api/v1/care-groups/{groupId}/invitations`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "channel": "LINK"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. CareGroupService persists the pending invitation/member state. 2. CareGroupService produces the supported invitation notification.

### IT-FAMILY-007 - An invited user can view their pending care-group invitations

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/invitations/me (default: `http://localhost:8080/api/v1/care-groups/invitations/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend returns invitation records associated with the current user and applicable pending status only.

## Respond to Family Invitation

### IT-FAMILY-008 - An invited Family user can explicitly accept a valid care-group invitation

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/invitations/{token}/accept (default: `http://localhost:8080/api/v1/care-groups/invitations/{token}/accept`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{token}` with the real value from the runtime invitation token returned/sent by the preceding invitation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "familyRelationshipRole": "MOTHER"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/invitations/me (default: `http://localhost:8080/api/v1/care-groups/invitations/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. Invitation state changes to accepted. 2. CareGroupMember is persisted as active/accepted membership. 3. The group becomes accessible according to permissions.

### IT-FAMILY-009 - An invited Family user can explicitly decline a valid care-group invitation

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/invitations/decline (default: `http://localhost:8080/api/v1/care-groups/{groupId}/invitations/decline`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/invitations/me (default: `http://localhost:8080/api/v1/care-groups/invitations/me`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200, 201`. After Send: 1. The invitation is persisted as declined according to the supported workflow. 2. No active membership access is granted.

### IT-FAMILY-010 - A Family user can join a care group using a valid supported invitation code

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/join (default: `http://localhost:8080/api/v1/care-groups/join`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "code": "CareBridge QA value",
  "familyRelationshipRole": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CareGroupService validates the code and current invitation/group state. 2. CareGroupService creates or activates the supported membership for the user.

### IT-FAMILY-011 - Retrying an accepted invitation does not create duplicate accepted memberships

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/invitations/{token}/accept (default: `http://localhost:8080/api/v1/care-groups/invitations/{token}/accept`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{token}` with the real value from the runtime invitation token returned/sent by the preceding invitation step. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "familyRelationshipRole": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Membership checks prevent duplicate accepted CareGroupMember records for the same user/group context.

## Manage Care Group Membership

### IT-FAMILY-012 - A non-owner member can leave an active care group

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/leave (default: `http://localhost:8080/api/v1/care-groups/{groupId}/leave`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The membership transition is persisted. 2. Subsequent group/shared-care access reflects that the user is no longer an active member.

### IT-FAMILY-013 - An authorized user can list current members of an active care group

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Membership data is loaded from CareGroupMemberRepository for the selected group according to authorization and invitation status.

### IT-FAMILY-014 - A care-group owner can remove an eligible member

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{targetUserId} (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{targetUserId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{targetUserId}` with the real value from the target user's `userId` from the group member/user response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Owner authorization is rechecked. 2. Membership removal/state change is persisted. 3. Subsequent access for the removed member is denied.

### IT-FAMILY-015 - A non-owner cannot remove another care-group member

**Step 1**

**Method:** `DELETE`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{targetUserId} (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{targetUserId}`)
**Postman Execution:**

1. Set Method to **DELETE** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{targetUserId}` with the real value from the target user's `userId` from the group member/user response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side group authorization rejects the mutation. 2. Membership remains unchanged.

## Manage Permission Control

### IT-FAMILY-016 - A care-group owner can retrieve a member's current family permission settings

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{memberId}/permissions (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{memberId}/permissions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{memberId}` with the real value from GET /api/v1/care-groups/{groupId}/members. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CareGroupService verifies group ownership/context. 2. CareGroupService returns the persisted FamilyPermission settings for the selected member.

### IT-FAMILY-018 - A non-owner cannot modify family permissions

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{memberId}/permissions (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{memberId}/permissions`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{memberId}` with the real value from GET /api/v1/care-groups/{groupId}/members. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "calendar": true
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{memberId}/permissions (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{memberId}/permissions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{memberId}` with the real value from GET /api/v1/care-groups/{groupId}/members. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side owner/authorization checks reject the update. 2. Persisted permissions remain unchanged.

## Grant Shared Access

### IT-FAMILY-017 - A care-group owner can update supported family permission flags for an accepted member

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/members/{memberId}/permissions (default: `http://localhost:8080/api/v1/care-groups/{groupId}/members/{memberId}/permissions`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{memberId}` with the real value from GET /api/v1/care-groups/{groupId}/members. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "calendar": true
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Updated permission state is persisted. 2. Subsequent shared-data access reflects the exact new flags.

## View Shared Access

### IT-FAMILY-019 - An accepted Family member with required permission flags can view shared care data

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/shared-data (default: `http://localhost:8080/api/v1/care-groups/{groupId}/shared-data`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `category = CareBridge QA` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FamilyDashboardService rechecks active membership and exact permission scope. 2. FamilyDashboardService loads permitted care data. 3. FamilyDashboardService returns only the authorized subset.

### IT-FAMILY-020 - Revoking a family permission immediately prevents subsequent access to the corresponding shared data

**Step 1**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/shared-data (default: `http://localhost:8080/api/v1/care-groups/{groupId}/shared-data`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `category = CareBridge QA` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/family/dashboard (default: `http://localhost:8080/api/v1/family/dashboard`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `selectedCareGroupId = copy a real UUID from the preceding list/create response` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Every read rechecks current permissions. 2. The revoked data category is no longer returned after the permission update.

### IT-FAMILY-021 - Inactive or non-accepted membership cannot access shared care data even if old permission values exist

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/shared-data (default: `http://localhost:8080/api/v1/care-groups/{groupId}/shared-data`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `category = CareBridge QA` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Active-membership checks fail before shared data is returned. 2. Stale permission values do not grant access.

### IT-FAMILY-022 - An authorized Family member can view permitted quick-note history for a supported metric type

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{careGroupId}/quick-notes (default: `http://localhost:8080/api/v1/care-groups/{careGroupId}/quick-notes`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{careGroupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FamilyQuickNoteService validates membership, permission and metric type. 2. FamilyQuickNoteService returns read-only permitted observations from PostgreSQL.

### IT-FAMILY-023 - A Family member cannot access quick-note history for a metric type outside their permission scope

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{careGroupId}/quick-notes (default: `http://localhost:8080/api/v1/care-groups/{careGroupId}/quick-notes`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{careGroupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `metricType = WEIGHT` (required); query `from = 2026-08-01T00:00:00Z` (optional/filter); query `to = 2026-08-31T23:59:59Z` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Permission/metric validation rejects the request. 2. Restricted observations are not disclosed.

### IT-FAMILY-024 - EPDS answer payloads are never exposed through family shared-care or quick-note views

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/shared-data (default: `http://localhost:8080/api/v1/care-groups/{groupId}/shared-data`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. **Params/Headers:** In Postman's **Params/Headers** table add query `category = CareBridge QA` (required); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
5. Select **Body > none**. Do not send `{}` or form data.
6. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Family-facing responses do not expose EPDS answer payloads. 2. Only explicitly permitted non-sensitive summary data is returned according to implementation.

## View Shared Schedule

### IT-FAMILY-025 - An authorized Family member can view the permitted shared care calendar

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{careGroupId}/appointments (default: `http://localhost:8080/api/v1/care-groups/{careGroupId}/appointments`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{careGroupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FamilyDashboardService rechecks membership/permission. 2. FamilyDashboardService returns only calendar/task/reminder data within the authorized linked care context.

## View Family Alerts

### IT-FAMILY-026 - A Family user can view family alerts addressed to them

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/family-alerts (default: `http://localhost:8080/api/v1/family-alerts`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FamilyAlertService queries notification records for the caller. 2. FamilyAlertService returns only their supported family-alert items.

## Manage Cooperative Care Tasks

### IT-FAMILY-027 - An authorized care-group actor can assign a cooperative care task and notify the assignee

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/tasks (default: `http://localhost:8080/api/v1/care-groups/{groupId}/tasks`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "assigneeMemberId": "42bffe88-390c-4c97-bc88-6a67dc4d7ae5",
  "title": "CareBridge integration test",
  "description": "Realistic integration test data for CareBridge.",
  "dueAt": "2026-08-13T13:00:14Z",
  "targetSubject": "MOTHER"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. CareTaskService rechecks membership/authorization. 2. CareTaskService persists the task. 3. CareTaskService produces the supported assignment notification.

### IT-FAMILY-028 - Cooperative care tasks are listed by care group and by current assignee as supported

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/tasks (default: `http://localhost:8080/api/v1/care-groups/{groupId}/tasks`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. CareTaskRepository-backed queries return the authorized group's tasks. 2. The current assignee's tasks are returned without exposing unrelated records.

### IT-FAMILY-029 - An authorized task owner/manager can update a cooperative care task

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/tasks/{taskId} (default: `http://localhost:8080/api/v1/care-groups/{groupId}/tasks/{taskId}`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "description": "Realistic integration test data for CareBridge."
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CareTaskService rechecks group membership and actor rights. 2. CareTaskService persists the update. 3. CareTaskService returns the new task state.

### IT-FAMILY-030 - An assigned Family member can update the task status only through allowed assignee transitions

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/tasks/{taskId}/status (default: `http://localhost:8080/api/v1/care-groups/{groupId}/tasks/{taskId}/status`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "status": "ACTIVE"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Allowed assignee status changes are persisted with audit context. 2. Disallowed actor transitions are rejected.

### IT-FAMILY-031 - An authorized owner can cancel a cooperative care task without duplicate transitions or notification side effects on retry

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/care-groups/{groupId}/tasks/{taskId}/cancel (default: `http://localhost:8080/api/v1/care-groups/{groupId}/tasks/{taskId}/cancel`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"family@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `FAMILY`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{groupId}` with the real value from GET /api/v1/care-groups or the care-group creation response; replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Cancellation is persisted once. 2. Repeated commands do not create conflicting task states or duplicate confirmed notification side effects.

# Content_Checklist_Hub

## Browse FAQ

### IT-CONTENT-001 - A Mother or Family user can browse approved visible verified content and FAQs

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content (default: `http://localhost:8080/api/v1/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ContentService queries repository-backed approved visible content. 2. ContentService returns only lifecycle content eligible for normal consumption.

## Search Content

### IT-CONTENT-003 - Verified content search and filters return matching approved records

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content/search (default: `http://localhost:8080/api/v1/content/search`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `keyword = CareBridge QA` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ContentService applies supported search/filter criteria. 2. ContentService returns only matching approved visible records.

## View Verified Content

### IT-CONTENT-002 - A user can open detail for an approved visible content item

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content/{id} (default: `http://localhost:8080/api/v1/content/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ContentController resolves the approved content item from PostgreSQL. 2. ContentController returns the supported detail to the client.

### IT-CONTENT-004 - Draft, rejected, hidden or otherwise unapproved content is not returned as normal verified content

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content (default: `http://localhost:8080/api/v1/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository/status rules exclude non-approved or non-visible lifecycle content from the normal verified-content experience.

### IT-CONTENT-005 - Lifecycle content retrieval respects the user's eligible care stage/context

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content/lifecycle (default: `http://localhost:8080/api/v1/content/lifecycle`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `type = ARTICLE` (optional/filter); query `topicId = copy a real UUID from the preceding list/create response` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The lifecycle content service returns only content matching the supported owner/stage eligibility rules.

### IT-CONTENT-006 - Direct access to a missing or non-visible content item does not expose it as approved content

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/content/{id} (default: `http://localhost:8080/api/v1/content/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The backend returns the supported not-found/unavailable outcome. 2. Restricted draft content is not disclosed as verified material.

## Manage Verified Content

### IT-CONTENT-007 - An authorized Content Admin can view the content administration workspace

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `keyword = CareBridge QA` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminContentService queries staff-visible content from PostgreSQL. 2. AdminContentService returns the administrative list/detail context.

### IT-CONTENT-008 - An authorized Content Admin can open staff detail for a managed content item

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id} (default: `http://localhost:8080/api/v1/admin/content/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminContentController returns the persisted administrative detail, including lifecycle fields required for management.

### IT-CONTENT-009 - A Content Admin can create a verified article draft

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "type": "ARTICLE",
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. AdminContentService persists a new ARTICLE content item with the supported initial lifecycle state and audit context.

### IT-CONTENT-010 - A Content Admin can update an existing article and retrieve the persisted revision

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id} (default: `http://localhost:8080/api/v1/admin/content/{id}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY",
  "status": "DRAFT"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The article update is persisted in PostgreSQL. 2. The administrative detail reflects the revised values/version state.

### IT-CONTENT-011 - A Content Admin can hide an article through the supported content lifecycle action

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/archive (default: `http://localhost:8080/api/v1/admin/content/{id}/archive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The hide state is persisted. 2. The article is no longer returned as normal visible verified content while remaining available to authorized administration as implemented.

### IT-CONTENT-012 - An authorized administrator can unpublish a published content item

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/unpublish (default: `http://localhost:8080/api/v1/admin/content/{id}/unpublish`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `keyword = CareBridge QA` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ContentUnpublishService persists the lifecycle transition. 2. Audit evidence is persisted. 3. The item is no longer returned as published verified content.

### IT-CONTENT-013 - Article media upload integrates with file storage before the content mutation is confirmed

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/files/upload/with-purpose (default: `http://localhost:8080/api/v1/files/upload/with-purpose`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `kind = IMAGE` (required); query `purpose = EXPERT_IDENTITY_SELFIE` (required); query `accessMode = PRIVATE` (required). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "type": "ARTICLE",
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The storage integration completes successfully. 2. The persisted article references the supported stored media. 3. No false success is shown if storage fails.

### IT-CONTENT-014 - A Content Admin can create a verified FAQ draft

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "type": "ARTICLE",
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. A FAQ content item is persisted with the supported lifecycle state. 2. The FAQ becomes available to the review workflow.

### IT-CONTENT-015 - A Content Admin can update an existing FAQ

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id} (default: `http://localhost:8080/api/v1/admin/content/{id}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY",
  "status": "DRAFT"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The FAQ update is persisted. 2. The returned administrative state reflects the new values.

### IT-CONTENT-016 - A Content Admin can hide a FAQ and remove it from normal verified-content visibility

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/archive (default: `http://localhost:8080/api/v1/admin/content/{id}/archive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The hide lifecycle state is persisted. 2. The FAQ is excluded from normal approved visible results.

### IT-CONTENT-017 - Authorized staff can retrieve content topics used by the content administration workflow

**Execution Status:** `NEEDS_CONFIRMATION`. No current backend route was proven for this operation, so it cannot be executed honestly in Postman. Capture the missing-route finding from the current OpenAPI/backend inventory and do not substitute the Community Topics API unless requirements explicitly confirm both resources are the same.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

No body — NEEDS_CONFIRMATION because no current dev endpoint was proven for content-topic retrieval.

**Expected Result After Send:** 1. The backend returns the persisted topic data required by the supported content management flow.

### IT-CONTENT-018 - An authorized administrator can create a content topic used by verified content

**Execution Status:** `NEEDS_CONFIRMATION`. No supported Method, endpoint, or request DTO is present in the current backend evidence. Do not send a guessed mutation to Supabase or reuse another topic API. Record this as a coverage gap requiring backend/requirements confirmation.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

NEEDS_CONFIRMATION — no current dev endpoint or request DTO was proven for content-topic creation.

**Expected Result After Send:** 1. The topic is persisted. 2. The topic becomes available to supported article/FAQ authoring flows.

### IT-CONTENT-019 - An authorized administrator can update a content topic

**Execution Status:** `NEEDS_CONFIRMATION`. This case has no proven update route or DTO. It is not executable in Postman until the backend contract identifies the content-topic resource. Keep the case as a documented API gap rather than fabricating a successful screenshot.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

NEEDS_CONFIRMATION — no current dev endpoint or request DTO was proven for content-topic update.

**Expected Result After Send:** 1. The topic update is persisted. 2. Subsequent content administration uses the updated topic state.

### IT-CONTENT-020 - Invalid or duplicate topic mutations are rejected without corrupting existing topic references

**Execution Status:** `NEEDS_CONFIRMATION`. A negative request cannot be constructed until the valid content-topic mutation endpoint and DTO are known. Evidence should show that the current OpenAPI has no proven contract; do not guess an endpoint or JSON body.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

NEEDS_CONFIRMATION — no current dev endpoint or request DTO was proven for this content-topic validation case.

**Expected Result After Send:** 1. Validation/repository constraints reject the unsupported mutation. 2. Existing topic and content references remain consistent.

### IT-CONTENT-026 - A non-authorized user cannot access content administration mutations

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** Log in intentionally with the insufficient-role account using `POST /api/v1/auth/login` and `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Paste `data.accessToken` into **Authorization > Bearer Token**. The expected evidence is the authorization rejection; do not use an admin token.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "type": "ARTICLE",
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Server-side role checks reject administrative operations. 2. Managed content remains unchanged.

### IT-CONTENT-027 - Authorized staff can view content version history after revisions

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/versions (default: `http://localhost:8080/api/v1/admin/content/{id}/versions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminContentController returns persisted version snapshots/history for the selected content item.

### IT-CONTENT-028 - Retrying a confirmed content lifecycle mutation does not create duplicate versions or notification side effects

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "type": "ARTICLE",
  "title": "CareBridge integration test",
  "summary": "CareBridge integration test summary.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The system preserves one valid logical transition. 2. Duplicate confirmed content records or versions are avoided. 3. Duplicate external side effects are avoided.

## Review Verified Content

### IT-CONTENT-021 - An authorized approver can load the content approval queue

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `keyword = CareBridge QA` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ContentApprovalService/repository returns content in reviewable states permitted to the approver.

### IT-CONTENT-022 - An authorized approver can approve eligible content and make it available according to the publishing lifecycle

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/decision (default: `http://localhost:8080/api/v1/admin/content/{id}/decision`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "APPROVE"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `keyword = CareBridge QA` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The approval decision is persisted. 2. Review feedback is updated as implemented. 3. Audit evidence is recorded. 4. Eligible content becomes approved/visible according to lifecycle rules.

### IT-CONTENT-023 - An authorized approver can reject reviewable content with the supported feedback

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/decision (default: `http://localhost:8080/api/v1/admin/content/{id}/decision`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "APPROVE"
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content (default: `http://localhost:8080/api/v1/admin/content`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"content@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `CONTENT_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `type = ARTICLE` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `keyword = CareBridge QA` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The rejection decision is persisted. 2. The rejection feedback is persisted. 3. The item remains unavailable as normal approved content.

### IT-CONTENT-024 - Content approval and rejection decisions retain audit evidence and notify the review workflow as supported

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/decision (default: `http://localhost:8080/api/v1/admin/content/{id}/decision`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "APPROVE"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The decision is persisted with audit context. 2. The configured review notification service receives the supported event without duplicate side effects.

### IT-CONTENT-025 - Author and approver permissions are separated for content lifecycle decisions

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/content/{id}/decision (default: `http://localhost:8080/api/v1/admin/content/{id}/decision`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "decision": "APPROVE"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Authoring mutations allowed to the author succeed. 2. Final approval is rejected for the unauthorized author. 3. Final approval succeeds only for the permitted approver.

## View Checklist

### IT-CONTENT-029 - A Mother or authorized Family user can retrieve their current personal checklist tasks

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/current/tasks (default: `http://localhost:8080/api/v1/checklists/current/tasks`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CurrentChecklistService obtains the current unified tasks for the actor. 2. CurrentChecklistService returns only active checklist tasks materialized for that recipient.

### IT-CONTENT-031 - Current checklist retrieval respects the requested date and timezone context

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/current/tasks (default: `http://localhost:8080/api/v1/checklists/current/tasks`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CurrentChecklistService passes date/timezone context to unified task aggregation. 2. CurrentChecklistService returns tasks bucketed for the requested local date.

### IT-CONTENT-032 - An authenticated recipient can view supported checklist history without exposing another recipient's checklist instances

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/history (default: `http://localhost:8080/api/v1/checklists/history`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter); query `targetSubject = MOTHER` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Checklist history is scoped to User A's recipient context. 2. User B's checklist instances are not returned.

### IT-CONTENT-033 - Only approved active distributable checklist-template versions materialize for eligible recipients

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/current/tasks (default: `http://localhost:8080/api/v1/checklists/current/tasks`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Only eligible approved active distributable versions create current checklist instances/tasks for the recipient.

### IT-CONTENT-034 - An ineligible recipient does not receive a checklist instance from a restricted template version

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/current/tasks (default: `http://localhost:8080/api/v1/checklists/current/tasks`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `date = 2026-08-12` (optional/filter); header `X-User-Timezone = Asia/Ho_Chi_Minh` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Recipient eligibility checks prevent materialization of the restricted template for the ineligible user.

## Manage Checklist

### IT-CONTENT-030 - A checklist recipient can apply a supported action to an owned current checklist task

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/checklists/tasks/{taskId}/actions (default: `http://localhost:8080/api/v1/checklists/tasks/{taskId}/actions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{taskId}` with the real value from the cooperative/checklist task creation or list response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "action": "COMPLETE",
  "clientRequestId": "f6fc6a05-a1b3-5b48-b6ec-a16a5e78e1ac"
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. CurrentChecklistController delegates through the unified task action flow. 2. The task state is persisted. 3. The refreshed checklist reflects the confirmed action.

### IT-CONTENT-035 - An authorized Content Admin can create a checklist template

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates (default: `http://localhost:8080/api/v1/admin/checklist-templates`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test",
  "description": "Realistic integration test data for CareBridge.",
  "stage": "PREGNANCY"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. AdminChecklistTemplateService persists the template/version structure. 2. AdminChecklistTemplateService returns it in the administrative list.

### IT-CONTENT-036 - An authorized Content Admin can update an editable checklist-template version

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{id} (default: `http://localhost:8080/api/v1/admin/checklist-templates/{id}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "name": "CareBridge QA Test",
  "description": "Realistic integration test data for CareBridge.",
  "stage": "PREGNANCY",
  "status": "DRAFT"
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The template update is persisted with version/lineage integrity. 2. The administrative detail reflects the saved state.

### IT-CONTENT-037 - Authorized staff can retrieve checklist-template detail by ID

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{id} (default: `http://localhost:8080/api/v1/admin/checklist-templates/{id}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. AdminChecklistTemplateController loads the persisted template/version detail including supported recipient/stage/item configuration.

### IT-CONTENT-038 - Checklist templates can be listed by supported lifecycle status

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates (default: `http://localhost:8080/api/v1/admin/checklist-templates`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `stage = PRE_PREGNANCY` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. ChecklistTemplateRepository-backed filtering returns the matching template versions in supported order.

### IT-CONTENT-039 - An authorized administrator can clone a checklist-template version

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{id}/clone (default: `http://localhost:8080/api/v1/admin/checklist-templates/{id}/clone`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. A new persisted template version is created from the source while maintaining supported lineage/version identifiers and source integrity.

### IT-CONTENT-040 - An authorized administrator can clone a version within a specified checklist lineage

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone (default: `http://localhost:8080/api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{lineageId}` with the real value from the checklist template/version response; replace `{versionId}` with the real value from the selected version from the versions endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The new version is persisted under the requested template lineage with correct version relationships.

### IT-CONTENT-041 - Authorized staff can retrieve checklist-template version history

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{id}/versions (default: `http://localhost:8080/api/v1/admin/checklist-templates/{id}/versions`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. The backend returns persisted template version snapshots/history for the selected template lineage/version context.

### IT-CONTENT-042 - An authorized administrator can archive a checklist template and prevent future active distribution as implemented

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/checklist-templates/{id}/archive (default: `http://localhost:8080/api/v1/admin/checklist-templates/{id}/archive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{id}` with the real value from the creation/list response for the resource named immediately before `{id}` in this endpoint. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "reason": "Verified during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Archive state is persisted. 2. The archived template is no longer treated as an active distributable version for new materialization.

### IT-CONTENT-043 - Checklist distribution-key idempotency prevents duplicate checklist instances for the same logical distribution

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/user-checklist-items/from-template (default: `http://localhost:8080/api/v1/user-checklist-items/from-template`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "templateId": "9d247005-d564-4352-b5a8-f3a4117973dd",
  "journeyId": "74f1d787-e133-499e-9b6d-1cf7bb902e99"
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. ChecklistInstanceRepository lookup by distributionKey preserves one logical instance. 2. Duplicate task instances are not created.

## Configure Exercise

### IT-CONTENT-044 - An authorized Content Admin can create pregnancy exercise content

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises (default: `http://localhost:8080/api/v1/admin/exercises`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "description": "Realistic integration test data for CareBridge.",
  "trimesterScope": "FIRST",
  "difficultyLevel": "EASY",
  "durationMinutes": 1,
  "safetyWarning": "Stop immediately if dizziness, pain, or bleeding occurs.",
  "supportsPostureAnalysis": true
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. AdminExerciseService validates the PregnancyExercise. 2. AdminExerciseService persists the PregnancyExercise. 3. Supported media storage completes before confirmed success.

### IT-CONTENT-045 - Pregnancy exercises can be listed and filtered by supported status, trimester and difficulty

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises (default: `http://localhost:8080/api/v1/admin/exercises`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `status = DRAFT` (optional/filter); query `trimester = FIRST` (optional/filter); query `difficulty = EASY` (optional/filter); query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. AdminExerciseService returns repository-backed exercises matching the requested supported filters.

### IT-CONTENT-046 - An authorized Content Admin can update pregnancy exercise content

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises/{exerciseId} (default: `http://localhost:8080/api/v1/admin/exercises/{exerciseId}`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "title": "CareBridge integration test",
  "description": "Realistic integration test data for CareBridge.",
  "trimesterScope": "FIRST",
  "difficultyLevel": "EASY",
  "durationMinutes": 1,
  "supportsPostureAnalysis": true
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The exercise update is persisted. 2. The returned administrative detail reflects the new values.

### IT-CONTENT-047 - An authorized Content Admin can activate an eligible pregnancy exercise

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises/{exerciseId}/activate (default: `http://localhost:8080/api/v1/admin/exercises/{exerciseId}/activate`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises/{exerciseId} (default: `http://localhost:8080/api/v1/admin/exercises/{exerciseId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201, 200`. After Send: 1. Activation is persisted with audit context. 2. The exercise becomes available according to supported active-content rules.

### IT-CONTENT-048 - An authorized Content Admin can disable an active pregnancy exercise

**Step 1**

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises/{exerciseId}/disable (default: `http://localhost:8080/api/v1/admin/exercises/{exerciseId}/disable`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/exercises/{exerciseId} (default: `http://localhost:8080/api/v1/admin/exercises/{exerciseId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. Disable state is persisted. 2. The exercise is no longer treated as active content for normal exercise consumption.

## Configure Posture Settings

### IT-CONTENT-049 - A System Admin can create posture-analysis configuration for an exercise

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/posture-configs (default: `http://localhost:8080/api/v1/admin/posture-configs`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "exerciseId": "143f5dea-a873-588a-bb3d-19cdb2fe748a",
  "analysisMode": "RULE_BASED",
  "confidenceThreshold": 1.0
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. PostureConfigService persists the configuration/version for the exercise with the supported model/rule metadata.

### IT-CONTENT-050 - A System Admin can create a new posture-config version and list versions in effective order

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/posture-configs/{exerciseId}/versions (default: `http://localhost:8080/api/v1/admin/posture-configs/{exerciseId}/versions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "analysisMode": "RULE_BASED",
  "confidenceThreshold": 1.0
}
```

**Step 2**

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/posture-configs/{exerciseId} (default: `http://localhost:8080/api/v1/admin/posture-configs/{exerciseId}`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{exerciseId}` with the real value from GET /api/v1/exercises or the admin exercise-creation response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. A new configuration version is persisted. 2. The listVersions operation returns the exercise's versions ordered by effective date as implemented.

### IT-CONTENT-051 - Activating a posture-config version validates the configured model/version integration before confirmed activation

**Method:** `PATCH`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/admin/posture-configs/{postureConfigId}/activate (default: `http://localhost:8080/api/v1/admin/posture-configs/{postureConfigId}/activate`)
**Postman Execution:**

1. Set Method to **PATCH** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"admin@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `SYSTEM_ADMIN`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{postureConfigId}` with the real value from the posture-config creation/version response. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. Valid model/version integration permits persisted activation. 2. Invalid or unavailable registry validation does not fabricate an active configuration state.

# Activity_Safety_Monitoring

## Monitor Sensor Activity

### IT-SAFETY-001 - Safety monitoring is off by default and the current configuration is loaded for the authenticated Mother

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/config (default: `http://localhost:8080/api/v1/safety/config`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `403`. After Send: 1. SafetyConfigService loads or returns the user's configuration with monitoring disabled by default. 2. No unsupported wearable/medical-device state is returned.

### IT-SAFETY-002 - A Mother can enable supported safety monitoring only after consent and phone sensor permission are available

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/fall-detection/enable (default: `http://localhost:8080/api/v1/safety/fall-detection/enable`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The protected API persists the enabled configuration for the user. 2. The mobile client starts the supported phone-IMU monitoring flow only when required permission is granted.

### IT-SAFETY-003 - Denied sensor permission does not produce a false active monitoring state

**Manual Execution (not a Postman request):** 1. Open the CareBridge client as `mother@carebridge.dev` / `Test@1234`. 2. Deny motion/sensor permission in the operating-system prompt/settings. 3. Return to safety monitoring. 4. Confirm the client does not show monitoring as active and offers an actionable permission state. 5. Capture the denied OS permission and inactive CareBridge monitoring state. No HTTP request is expected before permission is granted.

**Method:** NEEDS_CONFIRMATION

**Base URL + Endpoint:** NEEDS_CONFIRMATION

**Raw JSON Body:**

Not applicable — denied sensor permission is handled on the client before an HTTP request is sent.

**Expected Result After Send:** 1. The system does not represent monitoring as operational contrary to permission state. 2. Persisted/configured state remains consistent with sensorPermissionGranted requirements.

### IT-SAFETY-004 - A Mother can update supported monitoring sensitivity, countdown and emergency-auto-alert settings

**Method:** `PUT`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/config (default: `http://localhost:8080/api/v1/safety/config`)
**Postman Execution:**

1. Set Method to **PUT** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "fallDetectionEnabled": true,
  "sensitivityLevel": "MEDIUM",
  "emergencyAutoAlert": true
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. SafetyConfigService persists the user-scoped configuration. 2. The refreshed settings match the saved supported values.

### IT-SAFETY-005 - A Mother can disable active safety monitoring and stop the supported IMU monitoring session

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/fall-detection/disable (default: `http://localhost:8080/api/v1/safety/fall-detection/disable`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > none**. Do not send `{}` or form data.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The backend/client disable flow ends the active supported monitoring state. 2. Persisted configuration no longer treats fall detection as enabled.

## Detect Fall Event & Start Alert Countdown

### IT-SAFETY-006 - A qualifying phone-IMU signal creates one persisted suspected safety event

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/imu-data (default: `http://localhost:8080/api/v1/safety/imu-data`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "accelerometerX": 1.0,
  "accelerometerY": 1.0,
  "accelerometerZ": 1.0,
  "gyroscopeX": 1.0,
  "gyroscopeY": 1.0,
  "gyroscopeZ": 1.0,
  "timestamp": "2026-08-12T13:00:14Z",
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. FallDetectionService processes the IMU data. 2. FallDetectionService persists a suspected SafetyEvent linked to the user's monitoring session before presenting the safety check.

### IT-SAFETY-007 - Duplicate processing of the same IMU signal key does not create duplicate suspected safety events

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/imu-data (default: `http://localhost:8080/api/v1/safety/imu-data`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "accelerometerX": 1.0,
  "accelerometerY": 1.0,
  "accelerometerZ": 1.0,
  "gyroscopeX": 1.0,
  "gyroscopeY": 1.0,
  "gyroscopeZ": 1.0,
  "timestamp": "2026-08-12T13:00:14Z",
  "latitude": 10.7769,
  "longitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Repository lookup by imuSessionId and signalKey preserves one logical SafetyEvent. 2. No duplicate confirmed event is created.

## Confirm Alert Countdown

### IT-SAFETY-008 - A Mother can confirm that she is safe for an owned suspected safety event

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/confirm (default: `http://localhost:8080/api/v1/safety/events/{eventId}/confirm`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FallDetectionService verifies event ownership/current state. 2. FallDetectionService persists the response. 3. The event history shows the confirmed safety-check outcome.

### IT-SAFETY-009 - The first valid safety-event response wins and a conflicting second response is rejected

**Step 1**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/confirm (default: `http://localhost:8080/api/v1/safety/events/{eventId}/confirm`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "note": "Recorded during CareBridge integration testing."
}
```

**Step 2**

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/false-positive (default: `http://localhost:8080/api/v1/safety/events/{eventId}/false-positive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. The first valid persisted response remains authoritative. 2. The backend rejects the conflicting transition. 3. The backend does not create inconsistent event state.

## Trigger Safety Emergency Alert

### IT-SAFETY-010 - A Mother can send the supported safety emergency alert for an owned event through Firebase Cloud Messaging

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/emergency-alert (default: `http://localhost:8080/api/v1/safety/events/{eventId}/emergency-alert`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `202`. After Send: 1. Event ownership/state is validated. 2. The supported alert state is persisted. 3. FCM notification is queued/sent to configured contacts without claiming guaranteed assistance.

## View Safety History

### IT-SAFETY-011 - A Mother can view only her own safety-event history in detected-time order

**Method:** `GET`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events (default: `http://localhost:8080/api/v1/safety/events`)
**Postman Execution:**

1. Set Method to **GET** and use the URL above. 
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Params/Headers:** In Postman's **Params/Headers** table add query `page = 0` (optional/filter); query `size = 20` (optional/filter). Keep each required row enabled.
4. Select **Body > none**. Do not send `{}` or form data.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

No body. In Postman, leave the Body tab empty.

**Expected Result After Send:** HTTP status: `200`. After Send: 1. ISafetyEventRepository returns User A's events ordered by detectedAt as implemented and does not expose User B's events.

## Manage Safety Events

### IT-SAFETY-012 - A Mother can report an owned safety event as a false positive

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/false-positive (default: `http://localhost:8080/api/v1/safety/events/{eventId}/false-positive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `200`. After Send: 1. FallDetectionService validates ownership/current state. 2. FallDetectionService persists the false-positive response for the event.

### IT-SAFETY-013 - A user cannot view or mutate a safety event belonging to another user

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/safety/events/{eventId}/false-positive (default: `http://localhost:8080/api/v1/safety/events/{eventId}/false-positive`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. **Runtime data:** Before Send, replace `{eventId}` with the real value from the preceding create/list response for `eventId`. Never send braces or `<PASTE_...>` text literally.
4. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
5. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "note": "Recorded during CareBridge integration testing."
}
```

**Expected Result After Send:** HTTP status: `NEEDS_CONFIRMATION`. After Send: 1. Owner-scoped repository access rejects unauthorized operations. 2. User B's persisted safety event remains unchanged.

## Open Emergency Support

### IT-SAFETY-014 - A Mother can open Emergency Support from an owned safety alert without the system claiming certified detection or guaranteed assistance

**Method:** `POST`

**Base URL + Endpoint:** {{baseUrl}}/api/v1/emergency/sessions (default: `http://localhost:8080/api/v1/emergency/sessions`)
**Postman Execution:**

1. Set Method to **POST** and use the URL above. Do not change it to GET.
2. **Authorization:** First call `POST /api/v1/auth/login` with `{"email":"mother@carebridge.dev","password":"Test@1234"}`. Copy `data.accessToken`, then select **Authorization > Bearer Token** and paste it into **Token** (recommended role: `MOTHER`). Do not paste a refresh token, Firebase ID token, quotation marks, or the word `Bearer` into the Token field.
3. Select **Body > raw > JSON**, paste the JSON body below exactly, and keep `Content-Type: application/json` enabled.
4. Click **Send once**, then compare the HTTP status and response fields with **Expected Result After Send**. Capture Method, full URL, Authorization type, status, and the relevant response fields in the screenshot.


**Raw JSON Body:**

```json
{
  "triggerSource": "POSTMAN_MANUAL",
  "userLatitude": 10.7769,
  "userLongitude": 106.7009
}
```

**Expected Result After Send:** HTTP status: `201`. After Send: 1. The safety flow authorizes the event. 2. The safety flow opens the Emergency Support module. 3. The client does not claim certified fall detection, emergency dispatch, or guaranteed assistance.
