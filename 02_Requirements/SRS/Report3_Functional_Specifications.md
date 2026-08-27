## 3. Functional Specifications

> Status: **Draft — code-first baseline (2026-08-23)**. This catalogue contains **88 completed/reachable actor-goal use cases**. The count is derived from current code boundaries and is not adjusted to match the historical 91-UC list.

### 3.1 Catalogue Rules

- One use case has one primary actor goal, trigger/outcome, lifecycle, and authorization boundary.
- Separate screens/endpoints share a UC only when they implement the same actor outcome and state lifecycle.
- Reachable Mobile/Web variants may share a UC; broken, placeholder, or static controls are recorded as gaps.
- Backend capability without a current authorized client/operator is `Partial/API-only` and is excluded from the completed count.
- AI behavior is advisory or deterministic screening as evidenced by code; no accuracy, uptime, latency, or zero-hallucination claim is inferred.
- Full design/test detail is maintained under `04_Implement`; `CommunityTopicManagement` remains the canonical detailed artifact for UC-AD-09.

### 3.2 Coverage Summary

| Domain | Completed UC count |
| --- | ---: |
| Access, Identity, and Trust | 11 |
| Expert and Consultation | 12 |
| Mother Journey and Health | 19 |
| Baby Care | 8 |
| Community and Content Consumption | 6 |
| AI Nurse and Clinical Assistance | 1 |
| Emergency and Safety | 5 |
| Family Cooperative Care | 5 |
| Administration and Operations | 21 |
| **Total** | **88** |

Controller-route reconciliation:

| Parsed Spring/FastAPI handlers | Completed/reachable UC handlers | Partial/API-only/supporting handlers | Unclassified | Evidence |
| ---: | ---: | ---: | ---: | --- |
| 446 | 381 | 65 | 0 | `04_Implement/ROUTE_COVERAGE_AUDIT.md` |

### 3.3 Access, Identity, and Trust

#### UC-AC-01 Register and Verify Account

| UC ID and Name | UC-AC-01 Register and Verify Account |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web Expert Portal / Backend |
| **Primary Actor** | Guest | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/welcome` and nested registration/OTP screens to achieve the stated outcome. |  |  |
| **Description** | Create a supported email, phone, or federated account, verify the one-time proof, and select an allowed initial role before protected navigation. |  |  |
| **Preconditions** | PRE-1. Guest can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/auth/federated: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/phone/register: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/register: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/resend-otp: No @PreAuthorize on handler/class; effective access comes from the security chain; PUT /api/v1/auth/role: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/verify-otp: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Choose a supported registration method and submit identity data. |  |  |
| **Postconditions** | POST-1. Select an allowed initial role and continue to the role-aware entry point. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose a supported registration method and submit identity data. <br> 2. Receive and verify the supported OTP or federated proof. <br> 3. Select an allowed initial role and continue to the role-aware entry point. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Deleted legacy federated-registration pages are not valid Web entry points. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/auth/federated: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/phone/register: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/register: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/resend-otp: No @PreAuthorize on handler/class; effective access comes from the security chain; PUT /api/v1/auth/role: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/verify-otp: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `400, 401, 404, 409, 429`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | OTP cooldown, uniqueness, proof validity, and the role allow-list are server authoritative. General Web registration redirects to login; only expert registration is reachable on Web. |  |  |
| **Implemented Entry Points** | Mobile `/welcome` and nested registration/OTP screens; Mobile `/role-selection`; Web `/expert/register` and `/login/otp` |  |  |
| **Implemented Contracts** | POST `/api/v1/auth/register`; POST `/api/v1/auth/phone/register`; POST `/api/v1/auth/federated`; POST `/api/v1/auth/verify-otp`; POST `/api/v1/auth/resend-otp`; PUT `/api/v1/auth/role` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/register_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/otp_verification_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/VerifyOtpIntegrationTest.java`; `05_Development/CareBridgeWebApp/src/features/auth/pages/ExpertRegisterPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Deleted legacy federated-registration pages are not valid Web entry points. |  |  |

**Table 1: UC-AC-01 Register and Verify Account Use Case Specification**


#### UC-AC-02 Authenticate and End Current Session

| UC ID and Name | UC-AC-02 Authenticate and End Current Session |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Registered User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/login` and role-aware root dispatch to achieve the stated outcome. |  |  |
| **Description** | Authenticate with a supported credential, refresh the authenticated session, and end the current session through logout. |  |  |
| **Preconditions** | PRE-1. Registered User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/auth/federated: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/login: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/logout: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/phone/login: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/refresh: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Submit a supported credential or verified federated identity. |  |  |
| **Postconditions** | POST-1. Refresh an eligible session or explicitly log out the current session. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Submit a supported credential or verified federated identity. <br> 2. Receive the current access/refresh contract and enter the role-aware workspace. <br> 3. Refresh an eligible session or explicitly log out the current session. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/auth/federated: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/login: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/logout: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/phone/login: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/refresh: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `400, 401, 403, 429`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Blocked accounts and unsupported roles are routed to explicit safe screens. Logout, not the revoke-other-session endpoint, ends the current session. |  |  |
| **Implemented Entry Points** | Mobile `/login` and role-aware root dispatch; Web `/login` and `RoleAwareRedirect` |  |  |
| **Implemented Contracts** | POST `/api/v1/auth/login`; POST `/api/v1/auth/phone/login`; POST `/api/v1/auth/federated`; POST `/api/v1/auth/refresh`; POST `/api/v1/auth/logout` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/services/auth_service.dart`; `05_Development/CareBridgeWebApp/src/features/auth/services/authApi.ts` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/LoginIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/LogoutIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/FederatedLoginIntegrationTest.java`; `05_Development/CareBridgeMobileApp/test/features/auth/password_login_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 2: UC-AC-02 Authenticate and End Current Session Use Case Specification**


#### UC-AC-03 View and Revoke Login Sessions

| UC ID and Name | UC-AC-03 View and Revoke Login Sessions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/profile` to nested `LoginSessionsScreen` to achieve the stated outcome. |  |  |
| **Description** | Inspect other active login sessions and revoke an eligible session belonging to the authenticated account. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/sessions: isAuthenticated(); DELETE /api/v1/sessions/{sessionId}: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Load sessions owned by the authenticated user. |  |  |
| **Postconditions** | POST-1. Revoke it and refresh the server-authoritative session list. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load sessions owned by the authenticated user. <br> 2. Select a non-current eligible session. <br> 3. Revoke it and refresh the server-authoritative session list. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused Mobile widget test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/sessions: isAuthenticated(); DELETE /api/v1/sessions/{sessionId}: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | A user cannot enumerate or revoke another account's sessions. The current session must be ended through logout. |  |  |
| **Implemented Entry Points** | Mobile `/profile` to nested `LoginSessionsScreen` |  |  |
| **Implemented Contracts** | GET `/api/v1/sessions`; DELETE `/api/v1/sessions/{sessionId}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java`; `05_Development/CareBridgeMobileApp/lib/features/session/screens/login_sessions_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/service/impl/SessionServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | No focused Mobile widget test was found. |  |  |

**Table 3: UC-AC-03 View and Revoke Login Sessions Use Case Specification**


#### UC-AC-04 Recover Forgotten Password

| UC ID and Name | UC-AC-04 Recover Forgotten Password |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Guest | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile nested forgot/reset screens from `/login` to achieve the stated outcome. |  |  |
| **Description** | Request a time-limited password recovery proof and set a new password after the proof is validated. |  |  |
| **Preconditions** | PRE-1. Guest can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/auth/forgot-password: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/reset-password: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Submit the supported account identifier. |  |  |
| **Postconditions** | POST-1. Submit a policy-compliant new password and return to login. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Submit the supported account identifier. <br> 2. Receive and enter the recovery proof. <br> 3. Submit a policy-compliant new password and return to login. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused backend forgot/reset service test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/auth/forgot-password: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/reset-password: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `400, 429`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Recovery responses must not disclose whether an unrelated account exists. Proof expiry, one-time use, and password complexity are server authoritative. |  |  |
| **Implemented Entry Points** | Mobile nested forgot/reset screens from `/login`; Web `/forgot-password` |  |  |
| **Implemented Contracts** | POST `/api/v1/auth/forgot-password`; POST `/api/v1/auth/reset-password` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/forgot_password_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/reset_password_screen.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/ForgotPasswordPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeWebApp/src/features/auth/pages/PasswordRecoveryPages.test.tsx`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/policy/PasswordComplexityPolicyTest.java` |  |  |
| **Known Gaps / Exclusions** | No focused backend forgot/reset service test was found. |  |  |

**Table 4: UC-AC-04 Recover Forgotten Password Use Case Specification**


#### UC-AC-05 Change Password

| UC ID and Name | UC-AC-05 Change Password |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile `/profile` to nested change-password screen to achieve the stated outcome. |  |  |
| **Description** | Replace the authenticated user's password after validating the current credential and password policy. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: PUT /api/v1/auth/change-password: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Enter the current password and a new password. |  |  |
| **Postconditions** | POST-1. Persist the replacement credential and show the explicit result. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Enter the current password and a new password. <br> 2. Validate the current credential and complexity policy. <br> 3. Persist the replacement credential and show the explicit result. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused end-to-end change-password test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: PUT /api/v1/auth/change-password: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `400, 401`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | The authenticated principal, not a client-supplied user id, owns the change. The new password must satisfy the server password policy. |  |  |
| **Implemented Entry Points** | Mobile `/profile` to nested change-password screen; Web role settings password routes |  |  |
| **Implemented Contracts** | PUT `/api/v1/auth/change-password` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/change_password_screen.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/ChangePasswordPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/policy/PasswordComplexityPolicyTest.java` |  |  |
| **Known Gaps / Exclusions** | No focused end-to-end change-password test was found. |  |  |

**Table 5: UC-AC-05 Change Password Use Case Specification**


#### UC-AC-06 View and Edit Account Profile

| UC ID and Name | UC-AC-06 View and Edit Account Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/profile` and `/profile/edit` to achieve the stated outcome. |  |  |
| **Description** | View and update the supported profile fields of the authenticated account. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/auth/profile: No @PreAuthorize on handler/class; effective access comes from the security chain; PUT /api/v1/auth/profile: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/profile: isAuthenticated(); PATCH /api/v1/profile: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Load the authenticated profile projection. |  |  |
| **Postconditions** | POST-1. Save and reload the server-authoritative profile. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the authenticated profile projection. <br> 2. Edit only supported profile fields. <br> 3. Save and reload the server-authoritative profile. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/auth/profile: No @PreAuthorize on handler/class; effective access comes from the security chain; PUT /api/v1/auth/profile: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/profile: isAuthenticated(); PATCH /api/v1/profile: isAuthenticated(). <br> E3. Explicit handler failures are `400, 401`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Profile ownership comes from the authenticated principal. Role, verification, and protected identity fields cannot be self-escalated through profile edits. |  |  |
| **Implemented Entry Points** | Mobile `/profile` and `/profile/edit`; Web `/account/profile` |  |  |
| **Implemented Contracts** | GET `/api/v1/auth/profile`; PUT `/api/v1/auth/profile`; GET `/api/v1/profile`; PATCH `/api/v1/profile` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/profile/controller/ProfileController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/account_profile_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/edit_profile_screen.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/AccountProfilePage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/profile/ProfileIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/AuthProfileIntegrationTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 6: UC-AC-06 View and Edit Account Profile Use Case Specification**


#### UC-AC-07 Link Google Identity

| UC ID and Name | UC-AC-07 Link Google Identity |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Backend / Firebase |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/profile` to nested Linked Accounts screen to achieve the stated outcome. |  |  |
| **Description** | Link a verified Google identity to the current CareBridge account and display the resulting linked-account state. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/auth/identities/google: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/identities/google: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Load the current Google-link status. |  |  |
| **Postconditions** | POST-1. Link the identity and reload linked-account state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current Google-link status. <br> 2. Obtain a verified Firebase/Google identity proof. <br> 3. Link the identity and reload linked-account state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-07`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No reachable Web linked-account UI exists. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/auth/identities/google: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/auth/identities/google: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | A federated identity cannot be linked to multiple CareBridge accounts. The server verifies provider identity; client display state is not proof of linkage. |  |  |
| **Implemented Entry Points** | Mobile `/profile` to nested Linked Accounts screen |  |  |
| **Implemented Contracts** | GET `/api/v1/auth/identities/google`; POST `/api/v1/auth/identities/google` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/service/FederatedAuthService.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/linked_accounts_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/integration/FederatedIdentityLinkIntegrationTest.java`; `05_Development/CareBridgeMobileApp/test/features/auth/linked_accounts_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | No reachable Web linked-account UI exists. |  |  |

**Table 7: UC-AC-07 Link Google Identity Use Case Specification**


#### UC-AC-08 View and Acknowledge Notifications

| UC ID and Name | UC-AC-08 View and Acknowledge Notifications |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web role portals / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/notifications` to achieve the stated outcome. |  |  |
| **Description** | View account notifications, mark one or all notifications read, and register or deregister the current device token for delivery. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: DELETE /api/v1/notifications/device-token: isAuthenticated(); POST /api/v1/notifications/device-token: isAuthenticated(); GET /api/v1/notifications/me: isAuthenticated(); PUT /api/v1/notifications/read-all: isAuthenticated(); PUT /api/v1/notifications/{notificationId}/read: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Load notifications belonging to the authenticated account. |  |  |
| **Postconditions** | POST-1. Mark one/all items read and reconcile the current device token lifecycle. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load notifications belonging to the authenticated account. <br> 2. Open or acknowledge an eligible notification. <br> 3. Mark one/all items read and reconcile the current device token lifecycle. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-08`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: DELETE /api/v1/notifications/device-token: isAuthenticated(); POST /api/v1/notifications/device-token: isAuthenticated(); GET /api/v1/notifications/me: isAuthenticated(); PUT /api/v1/notifications/read-all: isAuthenticated(); PUT /api/v1/notifications/{notificationId}/read: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Notification ownership and unread counts are server authoritative. Device tokens belong to the authenticated account and must not expose another user's notifications. |  |  |
| **Implemented Entry Points** | Mobile `/notifications`; Web `/admin/notifications`; Web `/content/notifications` |  |  |
| **Implemented Contracts** | GET `/api/v1/notifications/me`; PUT `/api/v1/notifications/{id}/read`; PUT `/api/v1/notifications/read-all`; POST `/api/v1/notifications/device-token`; DELETE `/api/v1/notifications/device-token` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java`; `05_Development/CareBridgeMobileApp/lib/features/notification/screens/notification_center_screen.dart`; `05_Development/CareBridgeWebApp/src/features/notification/pages/NotificationCenterPage.tsx`; `05_Development/CareBridgeMobileApp/lib/core/notifications/fcm_service.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/notification/NotificationControllerContractTest.java`; `05_Development/CareBridgeMobileApp/test/features/notification/emergency_notification_routing_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 8: UC-AC-08 View and Acknowledge Notifications Use Case Specification**


#### UC-AC-09 Manage Privacy Settings and Consent Grants

| UC ID and Name | UC-AC-09 Manage Privacy Settings and Consent Grants |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/profile` to nested Privacy Settings screen to achieve the stated outcome. |  |  |
| **Description** | Read and update supported privacy settings and grant or revoke purpose-bound consent used by downstream CareBridge features. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/consent/grants: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/consent/grants: No @PreAuthorize on handler/class; effective access comes from the security chain; DELETE /api/v1/consent/grants/{consentId}: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/privacy-settings/me: isAuthenticated(); PUT /api/v1/privacy-settings/me: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Load current privacy settings and consent grants. |  |  |
| **Postconditions** | POST-1. Revoke consent and expose the resulting disabled downstream capability. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load current privacy settings and consent grants. <br> 2. Update an allowed setting or grant a declared purpose. <br> 3. Revoke consent and expose the resulting disabled downstream capability. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-09`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Web Account Profile privacy shortcuts are static and are not a functional self-service UI. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/consent/grants: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/consent/grants: No @PreAuthorize on handler/class; effective access comes from the security chain; DELETE /api/v1/consent/grants/{consentId}: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/privacy-settings/me: isAuthenticated(); PUT /api/v1/privacy-settings/me: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Consent is purpose-bound and revocation must affect downstream location, sharing, or personalization checks. The audit trail must not contain raw protected data. |  |  |
| **Implemented Entry Points** | Mobile `/profile` to nested Privacy Settings screen |  |  |
| **Implemented Contracts** | GET/PUT `/api/v1/privacy-settings/me`; GET/POST/DELETE `/api/v1/consent/grants/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/privacy/controller/PrivacySettingsController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consent/controller/ConsentController.java`; `05_Development/CareBridgeMobileApp/lib/features/privacy/screens/privacy_settings_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/privacy/controller/PrivacySettingsControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentServiceImplGrantTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentServiceImplListTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consent/ConsentServiceImplRevokeTest.java` |  |  |
| **Known Gaps / Exclusions** | Web Account Profile privacy shortcuts are static and are not a functional self-service UI. |  |  |

**Table 9: UC-AC-09 Manage Privacy Settings and Consent Grants Use Case Specification**


#### UC-AC-10 Deactivate Own Account

| UC ID and Name | UC-AC-10 Deactivate Own Account |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/profile` to nested Deactivate Account screen to achieve the stated outcome. |  |  |
| **Description** | Deactivate the authenticated account through the supported confirmation flow. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: DELETE /api/v1/auth/deactivate: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Open the deactivation confirmation flow. |  |  |
| **Postconditions** | POST-1. Deactivate the account and terminate access according to the server policy. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open the deactivation confirmation flow. <br> 2. Confirm the authenticated account action. <br> 3. Deactivate the account and terminate access according to the server policy. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-10`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused Mobile widget test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: DELETE /api/v1/auth/deactivate: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `400, 401, 403`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Only the authenticated account may initiate self-deactivation. Deactivation is not a client-only logout and must use the server lifecycle. |  |  |
| **Implemented Entry Points** | Mobile `/profile` to nested Deactivate Account screen |  |  |
| **Implemented Contracts** | DELETE `/api/v1/auth/deactivate` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AuthController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/deactivate_account_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/AuthServiceDeactivateTest.java` |  |  |
| **Known Gaps / Exclusions** | No focused Mobile widget test was found. |  |  |

**Table 10: UC-AC-10 Deactivate Own Account Use Case Specification**


#### UC-AC-11 Submit Account Lock Appeal

| UC ID and Name | UC-AC-11 Submit Account Lock Appeal |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Access, Identity, and Trust | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Blocked User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/blocked` to achieve the stated outcome. |  |  |
| **Description** | View the current account block state and submit one eligible appeal for administrative review. |  |  |
| **Preconditions** | PRE-1. Blocked User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/auth/lock-appeals: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Load the server-provided block reason/state. |  |  |
| **Postconditions** | POST-1. Display the pending/rejected duplicate result returned by the server. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the server-provided block reason/state. <br> 2. Enter and submit the supported appeal statement. <br> 3. Display the pending/rejected duplicate result returned by the server. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AC-11`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/auth/lock-appeals: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Appeal eligibility and duplicate-pending rules are server authoritative. Submitting an appeal does not remove the block before review. |  |  |
| **Implemented Entry Points** | Mobile `/blocked`; Web `/account-blocked` |  |  |
| **Implemented Contracts** | POST `/api/v1/auth/lock-appeals` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/security/controller/AccountLockAppealController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/blocked_account_screen.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/BlockedAccountPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/service/AccountLockAppealServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/auth/blocked_account_screen_test.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/BlockedAccountPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 11: UC-AC-11 Submit Account Lock Appeal Use Case Specification**


### 3.4 Expert and Consultation

#### UC-EX-01 Create Expert Profile and Select Expert Type

| UC ID and Name | UC-EX-01 Create Expert Profile and Select Expert Type |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Expert Applicant | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/expert-onboarding`, `/expert-profile-setup`, `/expert/type` to achieve the stated outcome. |  |  |
| **Description** | Create the expert profile shell, select the supported expert type, load onboarding master data, and persist onboarding progress. |  |  |
| **Preconditions** | PRE-1. Expert Applicant can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/expert/onboarding: hasRole('EXPERT'); POST /api/v1/expert/profiles: hasRole('EXPERT'); PATCH /api/v1/expert/profiles/me/expert-type: hasRole('EXPERT'); GET /api/v1/master-data/districts: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals/search/trackasia: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals/{id}: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/provinces: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/wards: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Create or load the applicant's expert profile. |  |  |
| **Postconditions** | POST-1. Persist onboarding progress and continue to the next required step. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load the applicant's expert profile. <br> 2. Select an allowed expert type and supported location/facility master data. <br> 3. Persist onboarding progress and continue to the next required step. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/expert/onboarding: hasRole('EXPERT'); POST /api/v1/expert/profiles: hasRole('EXPERT'); PATCH /api/v1/expert/profiles/me/expert-type: hasRole('EXPERT'); GET /api/v1/master-data/districts: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals/search/trackasia: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/hospitals/{id}: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/provinces: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/master-data/wards: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Expert type and onboarding state are server authoritative. Protected expert workspaces remain gated until all required verification stages are eligible. |  |  |
| **Implemented Entry Points** | Mobile `/expert-onboarding`, `/expert-profile-setup`, `/expert/type`; Web `/expert/onboarding` |  |  |
| **Implemented Contracts** | POST `/api/v1/expert/profiles`; PATCH `/api/v1/expert/profiles/me/expert-type`; GET `/api/v1/expert/onboarding`; GET `/api/v1/master-data/provinces`; GET `/api/v1/master-data/districts`; GET `/api/v1/master-data/wards`; GET `/api/v1/master-data/hospitals`; GET `/api/v1/master-data/hospitals/search/trackasia`; GET `/api/v1/master-data/hospitals/{id}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_profile_setup_screen.dart`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 12: UC-EX-01 Create Expert Profile and Select Expert Type Use Case Specification**


#### UC-EX-02 Review and Accept Expert Contract

| UC ID and Name | UC-EX-02 Review and Accept Expert Contract |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Expert Applicant | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile `/expert/contract` to achieve the stated outcome. |  |  |
| **Description** | Review the current cooperation contract offer and record the applicant's acceptance. |  |  |
| **Preconditions** | PRE-1. Expert Applicant can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/expert/contract/accept: hasRole('EXPERT'); GET /api/v1/expert/contract/offer: hasRole('EXPERT'); GET /api/v1/expert/contract/{expertProfileId}/preview: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load the current contract offer for the applicant. |  |  |
| **Postconditions** | POST-1. Persist acceptance and advance onboarding. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current contract offer for the applicant. <br> 2. Review required terms and attest acceptance. <br> 3. Persist acceptance and advance onboarding. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused backend ExpertContract test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/expert/contract/accept: hasRole('EXPERT'); GET /api/v1/expert/contract/offer: hasRole('EXPERT'); GET /api/v1/expert/contract/{expertProfileId}/preview: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Acceptance applies to the current version/offer returned by the server. UI progression alone does not prove contract acceptance. |  |  |
| **Implemented Entry Points** | Mobile `/expert/contract`; Web expert onboarding contract step |  |  |
| **Implemented Contracts** | GET `/api/v1/expert/contract/offer`; POST `/api/v1/expert/contract/accept`; GET `/api/v1/expert/contract/{profileId}/preview` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertcontract/controller/ExpertContractController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_contract_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertOnboardingPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | No focused backend ExpertContract test was found. |  |  |

**Table 13: UC-EX-02 Review and Accept Expert Contract Use Case Specification**


#### UC-EX-03 Verify Expert Identity and Face

| UC ID and Name | UC-EX-03 Verify Expert Identity and Face |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend / File Storage |
| **Primary Actor** | Expert Applicant | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/expert/identity` to achieve the stated outcome. |  |  |
| **Description** | Capture purpose-bound identity evidence and complete the implemented face/identity verification workflow. |  |  |
| **Preconditions** | PRE-1. Expert Applicant can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/expert/identity: hasRole('EXPERT'); GET /api/v1/expert/identity/files/{fileId}/url: hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); POST /api/v1/expert/verify-face: hasRole('EXPERT'). <br> PRE-3. The resource is in a state eligible for: Capture the supported identity document and face evidence. |  |  |
| **Postconditions** | POST-1. Receive and display the server verification state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Capture the supported identity document and face evidence. <br> 2. Submit evidence through purpose-bound file access. <br> 3. Receive and display the server verification state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/expert/identity: hasRole('EXPERT'); GET /api/v1/expert/identity/files/{fileId}/url: hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); POST /api/v1/expert/verify-face: hasRole('EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Identity files use purpose-bound authorization and are not public URLs. Duplicate identity/face policy is server authoritative. |  |  |
| **Implemented Entry Points** | Mobile `/expert/identity`; Web expert onboarding identity step |  |  |
| **Implemented Contracts** | POST `/api/v1/expert/identity`; POST `/api/v1/expert/verify-face`; GET `/api/v1/expert/identity/files/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_identity_capture_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/DuplicateIdentityFaceServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 14: UC-EX-03 Verify Expert Identity and Face Use Case Specification**


#### UC-EX-04 Submit Credentials and Track Verification

| UC ID and Name | UC-EX-04 Submit Credentials and Track Verification |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend / File Storage |
| **Primary Actor** | Expert Applicant | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/expert/credentials`, `/expert-verification-status` to achieve the stated outcome. |  |  |
| **Description** | Submit professional credential evidence, preview the current submission, and track or renew verification when supported. |  |  |
| **Preconditions** | PRE-1. Expert Applicant can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/expert/credentials: hasRole('EXPERT'); GET /api/v1/expert/credentials/me: hasRole('EXPERT'); DELETE /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}/file: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}/preview: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/me/renew: hasRole('EXPERT'); GET /api/v1/expert/profiles/me/verification-status: hasRole('EXPERT'). <br> PRE-3. The resource is in a state eligible for: Upload credential metadata and authorized evidence. |  |  |
| **Postconditions** | POST-1. Track review status or submit an eligible renewal. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Upload credential metadata and authorized evidence. <br> 2. Preview the submitted credential set. <br> 3. Track review status or submit an eligible renewal. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/expert/credentials: hasRole('EXPERT'); GET /api/v1/expert/credentials/me: hasRole('EXPERT'); DELETE /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}/file: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}/preview: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/me/renew: hasRole('EXPERT'); GET /api/v1/expert/profiles/me/verification-status: hasRole('EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Credential files are purpose-bound and verification state is server authoritative. Submission does not grant verified-expert access before approval. |  |  |
| **Implemented Entry Points** | Mobile `/expert/credentials`, `/expert-verification-status`; Web `/expert/credentials` |  |  |
| **Implemented Contracts** | POST `/api/v1/expert/credentials`; GET `/api/v1/expert/credentials/me`; GET/DELETE `/api/v1/expert/credentials/{credentialId}`; GET `/api/v1/expert/credentials/{credentialId}/preview`; GET `/api/v1/expert/credentials/{credentialId}/file`; GET `/api/v1/expert/profiles/me/verification-status`; POST `/api/v1/expert/profiles/me/renew` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/verification_documents_page_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 15: UC-EX-04 Submit Credentials and Track Verification Use Case Specification**


#### UC-EX-05 Manage Professional Profile

| UC ID and Name | UC-EX-05 Manage Professional Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Verified Expert | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile `/expert/profile` to achieve the stated outcome. |  |  |
| **Description** | View and update supported professional-profile fields owned by the authenticated expert. |  |  |
| **Preconditions** | PRE-1. Verified Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/expert/profiles/me: hasRole('EXPERT'); PATCH /api/v1/expert/profiles/me: hasRole('EXPERT'). <br> PRE-3. The resource is in a state eligible for: Load the authenticated expert profile. |  |  |
| **Postconditions** | POST-1. Persist and reload the server-authoritative profile. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the authenticated expert profile. <br> 2. Edit supported professional fields. <br> 3. Persist and reload the server-authoritative profile. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Mobile avatar update currently calls nonexistent `PATCH /api/v1/users/me/profile`; avatar editing is Partial. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/expert/profiles/me: hasRole('EXPERT'); PATCH /api/v1/expert/profiles/me: hasRole('EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only the owning expert may update mutable professional fields. Verification/trust fields cannot be self-escalated. |  |  |
| **Implemented Entry Points** | Mobile `/expert/profile`; Web `/expert/profile` |  |  |
| **Implemented Contracts** | GET/PATCH `/api/v1/expert/profiles/me` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_profile_page_screen.dart`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertProfilePage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/controller/ExpertProfileControllerTest.java` |  |  |
| **Known Gaps / Exclusions** | Mobile avatar update currently calls nonexistent `PATCH /api/v1/users/me/profile`; avatar editing is Partial. |  |  |

**Table 16: UC-EX-05 Manage Professional Profile Use Case Specification**


#### UC-EX-06 Manage Availability Calendar

| UC ID and Name | UC-EX-06 Manage Availability Calendar |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Verified Expert | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/expert-calendar` to achieve the stated outcome. |  |  |
| **Description** | Create, batch-create, list, update, and remove consultation availability owned by the expert. |  |  |
| **Preconditions** | PRE-1. Verified Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/expert/availability: hasRole('EXPERT'); PUT /api/v1/expert/availability/batch: hasRole('EXPERT'); GET /api/v1/expert/availability/me: hasRole('EXPERT'); GET /api/v1/expert/availability/{expertProfileId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); DELETE /api/v1/expert/availability/{id}: hasRole('EXPERT'). <br> PRE-3. The resource is in a state eligible for: Load the expert's availability calendar. |  |  |
| **Postconditions** | POST-1. Delete an eligible slot and refresh the calendar. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the expert's availability calendar. <br> 2. Create or update one/batch availability slots. <br> 3. Delete an eligible slot and refresh the calendar. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/expert/availability: hasRole('EXPERT'); PUT /api/v1/expert/availability/batch: hasRole('EXPERT'); GET /api/v1/expert/availability/me: hasRole('EXPERT'); GET /api/v1/expert/availability/{expertProfileId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); DELETE /api/v1/expert/availability/{id}: hasRole('EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Availability ownership and overlap/state rules are server authoritative. Another expert cannot mutate the owner's slots. |  |  |
| **Implemented Entry Points** | Mobile `/expert-calendar`; Web `/expert/calendar` |  |  |
| **Implemented Contracts** | Availability endpoints under `/api/v1/expert/availability/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertavailability/controller/ExpertAvailabilityController.java`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_calendar_screen.dart`; `05_Development/CareBridgeWebApp/src/features/expert/pages/AvailabilityCalendarPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertavailability/service/impl/ExpertAvailabilityServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertavailability/controller/ExpertAvailabilityControllerOwnershipTest.java`; `05_Development/CareBridgeMobileApp/test/features/expert/expert_calendar_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 17: UC-EX-06 Manage Availability Calendar Use Case Specification**


#### UC-EX-07 Browse Expert Directory and Public Profile

| UC ID and Name | UC-EX-07 Browse Expert Directory and Public Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/experts` to achieve the stated outcome. |  |  |
| **Description** | Search directory-eligible experts, inspect a public professional profile and availability, and choose an expert for consultation. |  |  |
| **Preconditions** | PRE-1. Mother / Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/expert/availability/{expertProfileId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/expert/directory: isAuthenticated(); GET /api/v1/expert/profiles/{expertProfileId}: isAuthenticated(); GET /api/v1/master-data/specialties: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Search/filter the eligible expert directory. |  |  |
| **Postconditions** | POST-1. Continue to the supported consultation-request flow. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Search/filter the eligible expert directory. <br> 2. Open a public expert profile and current availability. <br> 3. Continue to the supported consultation-request flow. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-07`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/expert/availability/{expertProfileId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/expert/directory: isAuthenticated(); GET /api/v1/expert/profiles/{expertProfileId}: isAuthenticated(); GET /api/v1/master-data/specialties: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only directory-eligible verified experts are returned. The consumer directory is a Mobile flow; the Web portal is expert/admin oriented. |  |  |
| **Implemented Entry Points** | Mobile `/experts`; Mobile `/expert/public/:expertProfileId` |  |  |
| **Implemented Contracts** | GET `/api/v1/expert/directory`; GET `/api/v1/expert/profiles/{id}`; GET `/api/v1/expert/availability/{id}`; GET `/api/v1/master-data/specialties` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertavailability/controller/ExpertAvailabilityController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/controller/MasterDataController.java`; `05_Development/CareBridgeMobileApp/lib/features/directChat/screens/expert_directory_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/expert/screens/expert_public_profile_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/ExpertDirectoryEligibilityIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expert/service/ExpertProfileServiceImplDirectoryTest.java`; `05_Development/CareBridgeMobileApp/test/features/directChat/expert_directory_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 18: UC-EX-07 Browse Expert Directory and Public Profile Use Case Specification**


#### UC-EX-08 Mother Creates and Manages Consultation Request

| UC ID and Name | UC-EX-08 Mother Creates and Manages Consultation Request |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile consultation form from public expert profile to achieve the stated outcome. |  |  |
| **Description** | Create a consultation request for an eligible expert, view its status/detail, and cancel it while the lifecycle permits. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/consultation-requests: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/mine: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/{id}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/consultation-requests/{id}/cancel: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Choose an eligible expert and enter request details. |  |  |
| **Postconditions** | POST-1. Cancel an owned request only while cancellation is allowed. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose an eligible expert and enter request details. <br> 2. Submit and track the server lifecycle state. <br> 3. Cancel an owned request only while cancellation is allowed. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-08`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/consultation-requests: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/mine: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/{id}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/consultation-requests/{id}/cancel: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Expert eligibility and request ownership are rechecked on mutation. Notifications are side effects, not the source of request state. |  |  |
| **Implemented Entry Points** | Mobile consultation form from public expert profile; Mobile `/consultation-requests`, `/consultation-requests/:requestId` |  |  |
| **Implemented Contracts** | POST `/api/v1/consultation-requests`; GET `/api/v1/consultation-requests/mine`; GET `/api/v1/consultation-requests/{id}`; PATCH `/api/v1/consultation-requests/{id}/cancel` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java`; `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/consultation_request_form_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/consultation/screens/my_consultation_requests_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImplCreateTest.java`; `05_Development/CareBridgeMobileApp/test/features/consultation/consultation_request_mobile_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 19: UC-EX-08 Mother Creates and Manages Consultation Request Use Case Specification**


#### UC-EX-09 Expert Processes Consultation Requests

| UC ID and Name | UC-EX-09 Expert Processes Consultation Requests |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Verified Expert | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile expert-home consultation tab to achieve the stated outcome. |  |  |
| **Description** | View matching/assigned consultation requests and accept or reject an eligible request. |  |  |
| **Preconditions** | PRE-1. Verified Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/consultation-requests/assigned: hasRole('EXPERT'); GET /api/v1/consultation-requests/matching: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/pending-summary: hasRole('EXPERT'); PATCH /api/v1/consultation-requests/{id}/accept: hasRole('EXPERT'); PATCH /api/v1/consultation-requests/{id}/reject: hasRole('EXPERT'). <br> PRE-3. The resource is in a state eligible for: Load requests eligible for the authenticated expert. |  |  |
| **Postconditions** | POST-1. Accept or reject using the guarded lifecycle transition. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load requests eligible for the authenticated expert. <br> 2. Inspect current request detail/state. <br> 3. Accept or reject using the guarded lifecycle transition. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-09`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Web reject currently sends POST while the backend requires PATCH; reject is broken on Web and remains Partial there. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/consultation-requests/assigned: hasRole('EXPERT'); GET /api/v1/consultation-requests/matching: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/consultation-requests/pending-summary: hasRole('EXPERT'); PATCH /api/v1/consultation-requests/{id}/accept: hasRole('EXPERT'); PATCH /api/v1/consultation-requests/{id}/reject: hasRole('EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Eligibility and current state are rechecked for every decision. A client cannot assign an ineligible request by UI state alone. |  |  |
| **Implemented Entry Points** | Mobile expert-home consultation tab; Web `/expert/consultation-requests` |  |  |
| **Implemented Contracts** | GET `/api/v1/consultation-requests/pending-summary`; GET `/api/v1/consultation-requests/matching`; GET `/api/v1/consultation-requests/assigned`; PATCH `/api/v1/consultation-requests/{id}/accept`; PATCH `/api/v1/consultation-requests/{id}/reject` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/controller/ConsultationRequestController.java`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertConsultationRequestsPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/service/impl/ConsultationRequestServiceImplLifecycleTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/consultation/controller/ConsultationRequestControllerSecurityTest.java` |  |  |
| **Known Gaps / Exclusions** | Web reject currently sends POST while the backend requires PATCH; reject is broken on Web and remains Partial there. |  |  |

**Table 20: UC-EX-09 Expert Processes Consultation Requests Use Case Specification**


#### UC-EX-10 Exchange Direct Messages and Attachments

| UC ID and Name | UC-EX-10 Exchange Direct Messages and Attachments |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend / Firebase / File Storage |
| **Primary Actor** | Mother / Expert | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/direct-chats`, `/direct-chat/:conversationId` to achieve the stated outcome. |  |  |
| **Description** | Exchange authorized conversation messages and attachments, obtain the scoped Firebase token used by live synchronization, mark reads, and recall a message only when current policy permits. |  |  |
| **Preconditions** | PRE-1. Mother / Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/direct-conversations: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/unread-summary: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/attachments: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/messages: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/read: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/timeline: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/firebase/custom-token: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Open an authorized direct conversation and load its timeline. |  |  |
| **Postconditions** | POST-1. Mark reads or recall an owned eligible message and refresh the timeline. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open an authorized direct conversation and load its timeline. <br> 2. Send a supported message or attachment using the scoped live-sync integration. <br> 3. Mark reads or recall an owned eligible message and refresh the timeline. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-10`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/direct-conversations: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/unread-summary: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/attachments: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/messages: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/messages/{messageId}/attachment: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/messages/{messageId}/recall: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/read: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/timeline: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/firebase/custom-token: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Conversation membership is server authoritative. Attachment access is purpose-bound; recall follows current ownership/time/state rules. |  |  |
| **Implemented Entry Points** | Mobile `/direct-chats`, `/direct-chat/:conversationId`; Web `/direct-chats*`, `/expert/direct-chats*` |  |  |
| **Implemented Contracts** | Conversation/timeline/message/attachment/read/recall endpoints under `/api/v1/direct-conversations/**`; POST `/api/v1/firebase/custom-token` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectConversationController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/DirectMessageController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/firebase/FirebaseTokenController.java`; `05_Development/CareBridgeMobileApp/lib/features/directChat/screens/direct_chat_screen.dart`; `05_Development/CareBridgeWebApp/src/features/directChat/pages/ConversationRoomPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/integration/DirectChatIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectConversationServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/DirectMessageServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/directChat/direct_chat_screen_test.dart`; `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 21: UC-EX-10 Exchange Direct Messages and Attachments Use Case Specification**


#### UC-EX-11 Make and Receive Voice/Video Calls

| UC ID and Name | UC-EX-11 Make and Receive Voice/Video Calls |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Mobile / Web / Backend / Zego |
| **Primary Actor** | Mother / Expert | **Confidence** | High |
| **Trigger** | The actor enters Call controls inside Mobile/Web direct conversation rooms to achieve the stated outcome. |  |  |
| **Description** | Create, receive, join, end, or decline a direct voice/video call and apply consent-aware recording behavior. |  |  |
| **Preconditions** | PRE-1. Mother / Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/direct-conversations/calls/active: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/calls/{callId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/answer: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/decline: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/end: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/join-credentials: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/recording: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'). <br> PRE-3. The resource is in a state eligible for: Initiate or receive a call inside an authorized conversation. |  |  |
| **Postconditions** | POST-1. End/decline and handle recording only after the implemented consent attestation. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Initiate or receive a call inside an authorized conversation. <br> 2. Obtain provider join credentials and join with granted device permissions. <br> 3. End/decline and handle recording only after the implemented consent attestation. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-11`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/direct-conversations/calls/active: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); GET /api/v1/direct-conversations/{conversationId}/calls/{callId}: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/answer: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/decline: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/end: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/join-credentials: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/direct-conversations/{conversationId}/calls/{callId}/recording: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); PATCH /api/v1/direct-conversations/{conversationId}/calls/{callId}/ringing: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Conversation membership and call state are server authoritative. Provider secrets never belong in UI state; recording requires implemented consent. |  |  |
| **Implemented Entry Points** | Call controls inside Mobile/Web direct conversation rooms |  |  |
| **Implemented Contracts** | Call lifecycle endpoints under `/api/v1/direct-conversations/{conversationId}/calls/**`; GET `/api/v1/direct-conversations/calls/active` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ConversationCallController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/ActiveConversationCallController.java`; `05_Development/CareBridgeMobileApp/lib/features/directChat/calls/direct_call_coordinator.dart`; `05_Development/CareBridgeWebApp/src/features/directChat/calls/DirectCallProvider.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/ConversationCallServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/directChat/calls/direct_call_coordinator_test.dart`; `05_Development/CareBridgeMobileApp/test/features/directChat/calls/rtc_permissions_test.dart`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/zegocloud/ZegoCloudServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 22: UC-EX-11 Make and Receive Voice/Video Calls Use Case Specification**


#### UC-EX-12 Review Shared Maternal Metrics and Checklists

| UC ID and Name | UC-EX-12 Review Shared Maternal Metrics and Checklists |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Expert and Consultation | **Platform** | Web / Backend |
| **Primary Actor** | Authorized Expert | **Confidence** | Medium |
| **Trigger** | The actor enters Web `/expert/shared-records` to achieve the stated outcome. |  |  |
| **Description** | Review maternal metrics and checklist information shared through an authorized consultation relationship. |  |  |
| **Preconditions** | PRE-1. Authorized Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET `/api/v1/direct-conversations`: Bearer-authenticated expert; server conversation membership filters the list; GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50`: Bearer-authenticated conversation member; GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}`: Existing authorized share/conversation context; backend remains the authorization authority; GET `/api/v1/checklists/journeys/{journeyId}/tasks`: Existing authorized share/conversation context; GET `/api/v1/checklists/users/{motherUserId}/tasks`: Fallback only when the shared payload lacks `journeyId` and includes the conversation-derived mother user. <br> PRE-3. The resource is in a state eligible for: Select an authorized consultation/conversation context. |  |  |
| **Postconditions** | POST-1. Review current records without mutating the mother's canonical data. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Select an authorized consultation/conversation context. <br> 2. Load shared metrics and checklist projections. <br> 3. Review current records without mutating the mother's canonical data. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-EX-12`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET `/api/v1/direct-conversations`: Bearer-authenticated expert; server conversation membership filters the list; GET `/api/v1/direct-conversations/{conversationId}/timeline?limit=50`: Bearer-authenticated conversation member; GET `/api/v1/journeys/{journeyId}/metrics?metricType={code}`: Existing authorized share/conversation context; backend remains the authorization authority; GET `/api/v1/checklists/journeys/{journeyId}/tasks`: Existing authorized share/conversation context; GET `/api/v1/checklists/users/{motherUserId}/tasks`: Fallback only when the shared payload lacks `journeyId` and includes the conversation-derived mother user. <br> E3. Explicit handler failures are `composition preserves each owning resource's current error/degraded behavior`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | The active sharing/consultation relationship is rechecked by the backend. Read access must not broaden to unrelated journeys or accounts. |  |  |
| **Implemented Entry Points** | Web `/expert/shared-records`; Shared items in direct-chat context |  |  |
| **Implemented Contracts** | Authorized conversation timeline and live-sync metric/checklist endpoints |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertSharedRecordsPage.tsx`; `05_Development/CareBridgeWebApp/src/features/expert/services/expertSharedRecordsService.ts` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeWebApp/src/features/directChat/services/directChatApi.test.ts` |  |  |
| **Known Gaps / Exclusions** | No dedicated shared-record backend resource exists; this UC composes existing authorized endpoints. |  |  |

**Table 23: UC-EX-12 Review Shared Maternal Metrics and Checklists Use Case Specification**


### 3.5 Mother Journey and Health

#### UC-MH-01 Complete Journey Consent and Stage Onboarding

| UC ID and Name | UC-MH-01 Complete Journey Consent and Stage Onboarding |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/journey-onboarding` to achieve the stated outcome. |  |  |
| **Description** | Complete journey consent and choose the supported maternal stage before entering stage-specific care flows. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/journey-onboarding: hasRole('MOTHER'); GET /api/v1/journey-onboarding/status: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Review and grant the required journey consent. |  |  |
| **Postconditions** | POST-1. Persist onboarding completion and continue to journey setup/home. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Review and grant the required journey consent. <br> 2. Select the supported pregnancy/postpartum stage. <br> 3. Persist onboarding completion and continue to journey setup/home. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/journey-onboarding: hasRole('MOTHER'); GET /api/v1/journey-onboarding/status: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Consent and onboarding eligibility are server authoritative. A stage-specific flow cannot bypass required onboarding state. |  |  |
| **Implemented Entry Points** | Mobile `/journey-onboarding`; Mobile `/mother-stage-selection`; Mobile `/postpartum-recovery-setup` |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/journey-onboarding/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyOnboardingController.java`; `05_Development/CareBridgeMobileApp/lib/features/journey/services/journey_onboarding_service.dart`; `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_stage_selection_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyOnboardingIntegrationTest.java`; `05_Development/CareBridgeMobileApp/test/features/journey/journey_onboarding_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 24: UC-MH-01 Complete Journey Consent and Stage Onboarding Use Case Specification**


#### UC-MH-02 Create or Update Maternal Journey

| UC ID and Name | UC-MH-02 Create or Update Maternal Journey |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/journey-setup` to achieve the stated outcome. |  |  |
| **Description** | Create a stage-specific maternal journey or update supported fields of an owned active journey. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/journeys: isAuthenticated(); PUT /api/v1/journeys/{journeyId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Enter stage-specific journey dates/details. |  |  |
| **Postconditions** | POST-1. Validate and persist supported updates. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Enter stage-specific journey dates/details. <br> 2. Create a new eligible journey or load an owned journey in edit mode. <br> 3. Validate and persist supported updates. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/journeys: isAuthenticated(); PUT /api/v1/journeys/{journeyId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Journey ownership, date compatibility, and active-lifecycle constraints are server authoritative. The `/journey-update` text placeholder is not a valid route. |  |  |
| **Implemented Entry Points** | Mobile `/journey-setup`; Edit via `/journey-setup?mode=edit&journeyId=...` |  |  |
| **Implemented Contracts** | POST `/api/v1/journeys`; PUT `/api/v1/journeys/{journeyId}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyController.java`; `05_Development/CareBridgeMobileApp/lib/features/journey/screens/journey_setup_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyUpdateServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/journey/journey_setup_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 25: UC-MH-02 Create or Update Maternal Journey Use Case Specification**


#### UC-MH-03 Record Pregnancy Outcome and Transition

| UC ID and Name | UC-MH-03 Record Pregnancy Outcome and Transition |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Pregnancy outcome screen nested from the mother journey tab to achieve the stated outcome. |  |  |
| **Description** | Record an outcome for an eligible pregnancy journey and transition to supported postpartum or baby-care state. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/journeys/{journeyId}/pregnancy-outcomes: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Open the outcome flow for an eligible owned pregnancy journey. |  |  |
| **Postconditions** | POST-1. Persist the outcome and expose the resulting postpartum/baby next actions. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open the outcome flow for an eligible owned pregnancy journey. <br> 2. Enter the supported outcome and transition details. <br> 3. Persist the outcome and expose the resulting postpartum/baby next actions. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/journeys/{journeyId}/pregnancy-outcomes: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Outcome submission is guarded by journey ownership and lifecycle state. The transition must not create duplicate baby/postpartum state on retry. |  |  |
| **Implemented Entry Points** | Pregnancy outcome screen nested from the mother journey tab |  |  |
| **Implemented Contracts** | POST `/api/v1/journeys/{journeyId}/pregnancy-outcomes` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyController.java`; `05_Development/CareBridgeMobileApp/lib/features/journey/screens/pregnancy_outcome_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/PregnancyOutcomeIntegrationTest.java`; `05_Development/CareBridgeMobileApp/test/features/journey/pregnancy_outcome_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 26: UC-MH-03 Record Pregnancy Outcome and Transition Use Case Specification**


#### UC-MH-04 View Journey Dashboard, History, and Timeline

| UC ID and Name | UC-MH-04 View Journey Dashboard, History, and Timeline |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile mother home and journey tab to achieve the stated outcome. |  |  |
| **Description** | View the current journey dashboard plus server-projected history and timeline for an owned maternal journey. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/journeys/me/dashboard: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/journeys/{journeyId}/history: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/timeline: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Load the current journey dashboard. |  |  |
| **Postconditions** | POST-1. Review ordered lifecycle/timeline projections and available next actions. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current journey dashboard. <br> 2. Select an owned journey/history period. <br> 3. Review ordered lifecycle/timeline projections and available next actions. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/journeys/me/dashboard: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/journeys/{journeyId}/history: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/timeline: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only compatible active/current lifecycle data is exposed on the dashboard. History and timeline remain scoped to the authenticated mother. |  |  |
| **Implemented Entry Points** | Mobile mother home and journey tab |  |  |
| **Implemented Contracts** | GET `/api/v1/journeys/me/dashboard`; GET `/api/v1/journeys/{id}/history`; GET `/api/v1/journeys/{id}/timeline` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyController.java`; `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_journey_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyDashboardServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/journey/JourneyTimelineServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 27: UC-MH-04 View Journey Dashboard, History, and Timeline Use Case Specification**


#### UC-MH-05 Manage Recommendation Profile

| UC ID and Name | UC-MH-05 Manage Recommendation Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/recommendation-profile` to achieve the stated outcome. |  |  |
| **Description** | Create or update the consent-gated preference/profile data used by personalized recommendations. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/recommendations/profile: hasRole('MOTHER'); PUT /api/v1/recommendations/profile: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Load the current recommendation profile and consent state. |  |  |
| **Postconditions** | POST-1. Validate and persist the profile. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current recommendation profile and consent state. <br> 2. Complete or revise supported preference/risk fields. <br> 3. Validate and persist the profile. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/recommendations/profile: hasRole('MOTHER'); PUT /api/v1/recommendations/profile: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Recommendation profile data belongs to the authenticated mother and is consent gated. Validation and supported enum/range values are server authoritative. |  |  |
| **Implemented Entry Points** | Mobile `/recommendation-profile` |  |  |
| **Implemented Contracts** | GET/PUT `/api/v1/recommendations/profile` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationController.java`; `05_Development/CareBridgeMobileApp/lib/features/recommendation/screens/recommendation_profile_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/recommendation/RecommendationProfileValidatorTest.java`; `05_Development/CareBridgeMobileApp/test/features/recommendation/recommendation_questionnaire_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 28: UC-MH-05 Manage Recommendation Profile Use Case Specification**


#### UC-MH-06 Browse Personalized Recommendations

| UC ID and Name | UC-MH-06 Browse Personalized Recommendations |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Recommendation sections inside Mobile mother home to achieve the stated outcome. |  |  |
| **Description** | Browse stage-aware verified content recommendations derived from the stored recommendation profile. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/recommendations/content: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Load recommendation context for the current journey/profile. |  |  |
| **Postconditions** | POST-1. Open a recommendation through the verified-content flow. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load recommendation context for the current journey/profile. <br> 2. Retrieve ranked eligible verified content. <br> 3. Open a recommendation through the verified-content flow. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/recommendations/content: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `400`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Recommendations are informational and do not replace clinical advice. Consent, stage, and publication eligibility filter the server result. |  |  |
| **Implemented Entry Points** | Recommendation sections inside Mobile mother home |  |  |
| **Implemented Contracts** | GET `/api/v1/recommendations/content` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationController.java`; `05_Development/CareBridgeMobileApp/lib/features/home/screens/mother_home_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/recommendation/RecommendationServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/home/mother_home_recommendation_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 29: UC-MH-06 Browse Personalized Recommendations Use Case Specification**


#### UC-MH-07 Record and Review General Maternal Metrics

| UC ID and Name | UC-MH-07 Record and Review General Maternal Metrics |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/journeys/:journeyId/metrics/add` to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, delete when allowed, and trend supported maternal metric observations for an owned journey. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: DELETE /api/v1/health-metrics/{metricId}: hasRole('MOTHER'); GET /api/v1/health-metrics/{metricId}: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Choose a supported metric type for the owned journey. |  |  |
| **Postconditions** | POST-1. Review detail/trend or perform an allowed edit/delete. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose a supported metric type for the owned journey. <br> 2. Enter a value/unit/time and submit after capability validation. <br> 3. Review detail/trend or perform an allowed edit/delete. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-07`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: DELETE /api/v1/health-metrics/{metricId}: hasRole('MOTHER'); GET /api/v1/health-metrics/{metricId}: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Metric type, unit, ranges, journey ownership, and edit/delete capability are server authoritative. Specialized fetal movement, hydration, and EPDS flows are specified separately. |  |  |
| **Implemented Entry Points** | Mobile `/journeys/:journeyId/metrics/add`; Mobile `/health-metrics/:id` and edit route; Mobile `/journeys/:journeyId/metrics/trend` |  |  |
| **Implemented Contracts** | GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**`; GET/DELETE `/api/v1/health-metrics/{metricId}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthMetricController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/HealthMetricAddServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/HealthMetricUpdateServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/MetricTrendServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 30: UC-MH-07 Record and Review General Maternal Metrics Use Case Specification**


#### UC-MH-08 Track Fetal Movement Sessions

| UC ID and Name | UC-MH-08 Track Fetal Movement Sessions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile metric add route with `metricType=FETAL_MOVEMENT_SESSION` to achieve the stated outcome. |  |  |
| **Description** | Run a timed fetal-movement observation session and store the supported session result as a journey metric. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Start a fetal-movement session for an eligible journey. |  |  |
| **Postconditions** | POST-1. Complete and persist the session observation. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Start a fetal-movement session for an eligible journey. <br> 2. Record movements and the observation period. <br> 3. Complete and persist the session observation. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-08`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Period start/end and movement values follow server validation. This is tracking, not a diagnostic conclusion. |  |  |
| **Implemented Entry Points** | Mobile metric add route with `metricType=FETAL_MOVEMENT_SESSION` |  |  |
| **Implemented Contracts** | GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/fetal_movement_tracker_screen.dart`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/MetricObservationValidatorTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 31: UC-MH-08 Track Fetal Movement Sessions Use Case Specification**


#### UC-MH-09 Track Hydration

| UC ID and Name | UC-MH-09 Track Hydration |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile metric add route with `metricType=HYDRATION` to achieve the stated outcome. |  |  |
| **Description** | Record hydration intake through the dedicated tracker and persist the corresponding journey metric. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Open hydration tracking for an eligible journey. |  |  |
| **Postconditions** | POST-1. Persist and refresh hydration progress. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open hydration tracking for an eligible journey. <br> 2. Add or adjust the supported intake amount. <br> 3. Persist and refresh hydration progress. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-09`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: No focused hydration-screen test was found. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Amount/unit/range validation is server authoritative. Hydration tracking does not diagnose dehydration. |  |  |
| **Implemented Entry Points** | Mobile metric add route with `metricType=HYDRATION` |  |  |
| **Implemented Contracts** | GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/hydration_tracker_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/MetricObservationValidatorTest.java` |  |  |
| **Known Gaps / Exclusions** | No focused hydration-screen test was found. |  |  |

**Table 32: UC-MH-09 Track Hydration Use Case Specification**


#### UC-MH-10 Complete and Review EPDS Screening

| UC ID and Name | UC-MH-10 Complete and Review EPDS Screening |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters EPDS screen nested from maternal metric surfaces to achieve the stated outcome. |  |  |
| **Description** | Answer the EPDS questionnaire, store the computed screening metric, and follow the implemented severity/safety response. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Complete all required EPDS responses. |  |  |
| **Postconditions** | POST-1. Display the server severity guidance and supported next action. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Complete all required EPDS responses. <br> 2. Compute and submit the score plus safety-relevant answer state. <br> 3. Display the server severity guidance and supported next action. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-10`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/journeys/{journeyId}/metrics: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT'); POST /api/v1/journeys/{journeyId}/metrics: hasRole('MOTHER'); GET /api/v1/journeys/{journeyId}/metrics/capabilities: hasRole('MOTHER'); PUT /api/v1/journeys/{journeyId}/metrics/{metricId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | EPDS is screening, not diagnosis. The self-harm item follows the deterministic safety-event floor even if the total score is lower. |  |  |
| **Implemented Entry Points** | EPDS screen nested from maternal metric surfaces |  |  |
| **Implemented Contracts** | GET/POST/PUT `/api/v1/journeys/{journeyId}/metrics/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/JourneyMetricController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/epds_screen.dart`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/policy/EpdsSeverityPolicy.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/HealthMetricServiceEpdsEventTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/policy/EpdsSeverityPolicyTest.java`; `05_Development/CareBridgeMobileApp/test/features/healthRecords/epds_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 33: UC-MH-10 Complete and Review EPDS Screening Use Case Specification**


#### UC-MH-11 Request AI Health Overview Screening

| UC ID and Name | UC-MH-11 Request AI Health Overview Screening |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Python AI Service |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters CTA in Mobile metric trend to achieve the stated outcome. |  |  |
| **Description** | Submit a recent metric or latest multi-metric snapshot to deterministic maternal screening and follow the NORMAL, ANOMALY_MONITOR, or CRITICAL_EMERGENCY result. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/metrics/evaluate: Internal API key via `verify_internal_api_key` dependency. <br> PRE-3. The resource is in a state eligible for: Build the supported single/latest metric payload. |  |  |
| **Postconditions** | POST-1. Display the result and route yellow to prefilled AI chat or red to emergency map. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Build the supported single/latest metric payload. <br> 2. Run deterministic validation/classification and optional RAG context lookup. <br> 3. Display the result and route yellow to prefilled AI chat or red to emergency map. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-11`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/metrics/evaluate: Internal API key via `verify_internal_api_key` dependency. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Deterministic thresholds establish the safety result; retrieval/generation cannot lower it. The feature is screening and guidance, not diagnosis. |  |  |
| **Implemented Entry Points** | CTA in Mobile metric trend; Automatic check after metric save |  |  |
| **Implemented Contracts** | POST Python `/api/v1/metrics/evaluate` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py`; `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py`; `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |  |  |
| **Known Gaps / Exclusions** | Mobile currently embeds an internal key and calls Python directly; replace with a server-side gateway before production. |  |  |

**Table 34: UC-MH-11 Request AI Health Overview Screening Use Case Specification**


#### UC-MH-12 Manage Health Records and Attachments

| UC ID and Name | UC-MH-12 Manage Health Records and Attachments |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend / Object Storage |
| **Primary Actor** | Mother / Authorized Family / Expert | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/health-records*` to achieve the stated outcome. |  |  |
| **Description** | Create, view, edit, archive, and list maternal health records and access purpose-authorized attachments. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family / Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/files: hasRole('MOTHER'); POST /api/v1/files/health-records: hasRole('MOTHER'); POST /api/v1/files/upload/with-purpose: hasAnyRole('EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'MODERATOR', 'CONTENT_ADMIN', 'MOTHER'); DELETE /api/v1/files/{fileId}: hasRole('MOTHER'); GET /api/v1/files/{fileId}: isAuthenticated(); GET /api/v1/health-records: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/health-records: hasRole('MOTHER'); GET /api/v1/health-records/timeline: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/health-records/{id}: hasRole('MOTHER'); PATCH /api/v1/health-records/{id}/archive: hasRole('MOTHER'); GET /api/v1/health-records/{recordId}: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Create or load an owned health record. |  |  |
| **Postconditions** | POST-1. Archive an eligible record and refresh the list. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load an owned health record. <br> 2. Attach/view an authorized file and update supported record fields. <br> 3. Archive an eligible record and refresh the list. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-12`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/files: hasRole('MOTHER'); POST /api/v1/files/health-records: hasRole('MOTHER'); POST /api/v1/files/upload/with-purpose: hasAnyRole('EXPERT', 'ADMIN', 'SYSTEM_ADMIN', 'MODERATOR', 'CONTENT_ADMIN', 'MOTHER'); DELETE /api/v1/files/{fileId}: hasRole('MOTHER'); GET /api/v1/files/{fileId}: isAuthenticated(); GET /api/v1/health-records: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/health-records: hasRole('MOTHER'); GET /api/v1/health-records/timeline: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/health-records/{id}: hasRole('MOTHER'); PATCH /api/v1/health-records/{id}/archive: hasRole('MOTHER'); GET /api/v1/health-records/{recordId}: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | File purpose, owner, share expiry, and consultation context determine access. Archive is the supported record lifecycle; file deletion follows explicit policy. |  |  |
| **Implemented Entry Points** | Mobile `/health-records*`; Mobile `/files/:fileId/view` and shared-view route |  |  |
| **Implemented Contracts** | Health-record endpoints under `/api/v1/health-records/**`; File endpoints under `/api/v1/files/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthRecordController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/controller/FileController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/policy/FileAccessPolicy.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_record_timeline_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/health/HealthRecordServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/FileControllerUploadTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/file/policy/FileAccessPolicyTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 35: UC-MH-12 Manage Health Records and Attachments Use Case Specification**


#### UC-MH-13 Manage Appointments and Calendar

| UC ID and Name | UC-MH-13 Manage Appointments and Calendar |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/appointments/add`, `/appointments/calendar`, appointment detail/edit to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, cancel/delete when allowed, and calendar-browse personal or shared care-group appointments. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/appointments: hasRole('MOTHER'); POST /api/v1/appointments: hasRole('MOTHER'); DELETE /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); GET /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); PATCH /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); GET /api/v1/care-groups/{careGroupId}/appointments: isAuthenticated(); GET /api/v1/care-groups/{careGroupId}/appointments/{appointmentId}: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Create an appointment with supported time/location/reminder details. |  |  |
| **Postconditions** | POST-1. Update or cancel it when ownership/membership/state permits. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create an appointment with supported time/location/reminder details. <br> 2. Browse calendar and open an owned/shared appointment. <br> 3. Update or cancel it when ownership/membership/state permits. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-13`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/appointments: hasRole('MOTHER'); POST /api/v1/appointments: hasRole('MOTHER'); DELETE /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); GET /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); PATCH /api/v1/appointments/{appointmentId}: hasRole('MOTHER'); GET /api/v1/care-groups/{careGroupId}/appointments: isAuthenticated(); GET /api/v1/care-groups/{careGroupId}/appointments/{appointmentId}: isAuthenticated(). <br> E3. Explicit handler failures are `400`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Ownership/care-group access is rechecked after locking for mutations. Notification preferences may be read for delivery, but their settings UI is Partial. |  |  |
| **Implemented Entry Points** | Mobile `/appointments/add`, `/appointments/calendar`, appointment detail/edit; Shared appointment detail route |  |  |
| **Implemented Contracts** | Appointment endpoints under `/api/v1/appointments/**`; Care-group appointment endpoints under `/api/v1/care-groups/{id}/appointments/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/appointment/controller/AppointmentController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/appointment/controller/CareGroupAppointmentController.java`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/appointment_calendar_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/appointment/service/CareGroupAppointmentServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/reminder/appointment_calendar_screen_test.dart`; `05_Development/CareBridgeMobileApp/test/features/reminder/shared_appointment_detail_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 36: UC-MH-13 Manage Appointments and Calendar Use Case Specification**


#### UC-MH-14 Manage General, Medication, and Vaccination Reminders

| UC ID and Name | UC-MH-14 Manage General, Medication, and Vaccination Reminders |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/reminders/all` and reminder add/detail/manage routes to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, snooze, complete, or remove supported reminder types. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/reminders: hasRole('MOTHER'); POST /api/v1/reminders: hasRole('MOTHER'); POST /api/v1/reminders/medication: hasRole('MOTHER'); GET /api/v1/reminders/today: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/reminders/vaccination: hasRole('MOTHER'); GET /api/v1/reminders/vaccination/suggestions: hasRole('MOTHER'); DELETE /api/v1/reminders/{reminderId}: hasRole('MOTHER'); GET /api/v1/reminders/{reminderId}: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/complete: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/enable: hasRole('MOTHER'); DELETE /api/v1/reminders/{reminderId}/permanent: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/snooze: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Choose the reminder type and enter supported trigger/details. |  |  |
| **Postconditions** | POST-1. Snooze, complete, update, or remove it using an allowed action. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose the reminder type and enter supported trigger/details. <br> 2. Persist and load the reminder state. <br> 3. Snooze, complete, update, or remove it using an allowed action. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-14`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/reminders: hasRole('MOTHER'); POST /api/v1/reminders: hasRole('MOTHER'); POST /api/v1/reminders/medication: hasRole('MOTHER'); GET /api/v1/reminders/today: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/reminders/vaccination: hasRole('MOTHER'); GET /api/v1/reminders/vaccination/suggestions: hasRole('MOTHER'); DELETE /api/v1/reminders/{reminderId}: hasRole('MOTHER'); GET /api/v1/reminders/{reminderId}: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/complete: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/enable: hasRole('MOTHER'); DELETE /api/v1/reminders/{reminderId}/permanent: hasRole('MOTHER'); PATCH /api/v1/reminders/{reminderId}/snooze: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Reminder ownership and state transitions are server authoritative. Delivery side effects do not replace canonical reminder state. |  |  |
| **Implemented Entry Points** | Mobile `/reminders/all` and reminder add/detail/manage routes; Medication and vaccination reminder add routes |  |  |
| **Implemented Contracts** | Reminder endpoints under `/api/v1/reminders/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/controller/ReminderController.java`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/all_reminders_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/create_medication_reminder_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/create_vaccination_reminder_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/ReminderServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/MedicationReminderServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/VaccinationReminderServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 37: UC-MH-14 Manage General, Medication, and Vaccination Reminders Use Case Specification**


#### UC-MH-15 Manage Recurring Reminder Schedules

| UC ID and Name | UC-MH-15 Manage Recurring Reminder Schedules |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/reminder-schedules`, `/reminder-schedules/:id` to achieve the stated outcome. |  |  |
| **Description** | Create, inspect, update, pause/resume, and delete recurring reminder schedules. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/reminder-schedules: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/reminder-schedules: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Create a recurrence rule and schedule window. |  |  |
| **Postconditions** | POST-1. Update, pause/resume, or remove the owned schedule. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create a recurrence rule and schedule window. <br> 2. Inspect generated/current schedule state. <br> 3. Update, pause/resume, or remove the owned schedule. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-15`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/reminder-schedules: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/reminder-schedules: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/reminder-schedules/{scheduleId}: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Recurrence parsing, ownership, and schedule state are server authoritative. Retries must not create duplicate occurrences beyond current policy. |  |  |
| **Implemented Entry Points** | Mobile `/reminder-schedules`, `/reminder-schedules/:id` |  |  |
| **Implemented Contracts** | Schedule endpoints under `/api/v1/reminder-schedules/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/schedule/controller/ReminderScheduleController.java`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/reminder_schedules_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/schedule/ReminderScheduleServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/reminder/reminder_schedule_service_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 38: UC-MH-15 Manage Recurring Reminder Schedules Use Case Specification**


#### UC-MH-16 Manage Personal Checklist and Roadmap

| UC ID and Name | UC-MH-16 Manage Personal Checklist and Roadmap |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/checklists/roadmap` and embedded add/import/history surfaces to achieve the stated outcome. |  |  |
| **Description** | View the lifecycle checklist roadmap, import optional templates, create personal items, and manage eligible checklist history/actions. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/checklists/current/tasks: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/checklists/history: hasRole('MOTHER'); GET /api/v1/checklists/journeys/{journeyId}/tasks: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN'); POST /api/v1/checklists/tasks/{taskId}/actions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/checklists/users/{userId}/tasks: hasAnyRole('EXPERT', 'ADMIN'); GET /api/v1/user-checklist-items: hasRole('MOTHER'); POST /api/v1/user-checklist-items: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/user-checklist-items/from-template: hasRole('MOTHER'); POST /api/v1/user-checklist-items/import: hasRole('MOTHER'); DELETE /api/v1/user-checklist-items/{itemId}: hasRole('MOTHER'); PUT /api/v1/user-checklist-items/{itemId}: hasRole('MOTHER'); PATCH /api/v1/user-checklist-items/{itemId}/toggle: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Load the current lifecycle roadmap/checklist. |  |  |
| **Postconditions** | POST-1. Perform an allowed item action and review history. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current lifecycle roadmap/checklist. <br> 2. Import an optional template or create a personal item. <br> 3. Perform an allowed item action and review history. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-16`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/checklists/current/tasks: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/checklists/history: hasRole('MOTHER'); GET /api/v1/checklists/journeys/{journeyId}/tasks: hasAnyRole('MOTHER', 'FAMILY', 'EXPERT', 'ADMIN'); POST /api/v1/checklists/tasks/{taskId}/actions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/checklists/users/{userId}/tasks: hasAnyRole('EXPERT', 'ADMIN'); GET /api/v1/user-checklist-items: hasRole('MOTHER'); POST /api/v1/user-checklist-items: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/user-checklist-items/from-template: hasRole('MOTHER'); POST /api/v1/user-checklist-items/import: hasRole('MOTHER'); DELETE /api/v1/user-checklist-items/{itemId}: hasRole('MOTHER'); PUT /api/v1/user-checklist-items/{itemId}: hasRole('MOTHER'); PATCH /api/v1/user-checklist-items/{itemId}/toggle: hasRole('MOTHER'). <br> E3. Explicit handler failures are `400, 404`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | System-distributed and user-created items have different mutation rules. Checklist operations remain scoped to the active lifecycle/authorized group. |  |  |
| **Implemented Entry Points** | Mobile `/checklists/roadmap` and embedded add/import/history surfaces |  |  |
| **Implemented Contracts** | Endpoints under `/api/v1/user-checklist-items/**`; Current/history endpoints under `/api/v1/checklists/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/controller/UserChecklistItemController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CurrentChecklistController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/history/controller/ChecklistHistoryController.java`; `05_Development/CareBridgeMobileApp/lib/features/checklist/screens/checklist_roadmap_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/UserCreatedChecklistTaskServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/history/ChecklistHistoryServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/checklist/checklist_roadmap_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 39: UC-MH-16 Manage Personal Checklist and Roadmap Use Case Specification**


#### UC-MH-17 View and Act on Unified Today Tasks

| UC ID and Name | UC-MH-17 View and Act on Unified Today Tasks |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Today-task panels on role home screens to achieve the stated outcome. |  |  |
| **Description** | View the unified today projection, execute the supported action for an appointment/reminder/checklist/care task, and advance checklist sequence when the owning workflow requires it. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/checklists/sequences/advance: hasRole('MOTHER'); GET /api/v1/tasks/today: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/tasks/{taskKind}/{taskId}/actions: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Load the unified today projection. |  |  |
| **Postconditions** | POST-1. Submit one action, apply any server-authorized sequence advance, and refresh the projection. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the unified today projection. <br> 2. Open a task and inspect the kind-specific allowed actions. <br> 3. Submit one action, apply any server-authorized sequence advance, and refresh the projection. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-17`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/checklists/sequences/advance: hasRole('MOTHER'); GET /api/v1/tasks/today: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/tasks/{taskKind}/{taskId}/actions: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | The task kind/id pair is resolved server-side and ownership/membership is rechecked. Actions are idempotent only where the owning domain defines that semantic. |  |  |
| **Implemented Entry Points** | Today-task panels on role home screens; Mobile `/checklists/task-detail` |  |  |
| **Implemented Contracts** | GET `/api/v1/tasks/today`; POST `/api/v1/tasks/{taskKind}/{taskId}/actions`; POST `/api/v1/checklists/sequences/advance` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/TodayTaskController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/sequence/ChecklistSequenceController.java`; `05_Development/CareBridgeMobileApp/lib/features/reminder/widgets/today_tasks_panel.dart`; `05_Development/CareBridgeMobileApp/lib/features/reminder/services/today_task_service.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/today/UnifiedTodayTaskServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/reminder/today_tasks_navigation_contract_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 40: UC-MH-17 View and Act on Unified Today Tasks Use Case Specification**


#### UC-MH-18 Browse Exercises and Complete Safety Check

| UC ID and Name | UC-MH-18 Browse Exercises and Complete Safety Check |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/mother-exercise` and nested detail/safety sheet to achieve the stated outcome. |  |  |
| **Description** | Browse pregnancy exercises, review an exercise detail, submit the implemented pre-session safety check, and reload its latest result before camera execution. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/exercises: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/{exerciseId}/safety-check: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}/safety-check/latest: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Browse/filter eligible exercises for the current stage. |  |  |
| **Postconditions** | POST-1. Submit/reload the safety check and continue only if eligible. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Browse/filter eligible exercises for the current stage. <br> 2. Open instructions and contraindication information. <br> 3. Submit/reload the safety check and continue only if eligible. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-18`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/exercises: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/{exerciseId}/safety-check: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}/safety-check/latest: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Stage/publication eligibility and safety-check policy are server authoritative. A failed/blocked safety check cannot be bypassed by client navigation. |  |  |
| **Implemented Entry Points** | Mobile `/mother-exercise` and nested detail/safety sheet |  |  |
| **Implemented Contracts** | GET `/api/v1/exercises`; GET `/api/v1/exercises/{id}`; POST `/api/v1/exercises/{id}/safety-check`; GET `/api/v1/exercises/{id}/safety-check/latest` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/policy/SafetyCheckPolicy.java`; `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/mother_exercise_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseQueryServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSafetyCheckServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 41: UC-MH-18 Browse Exercises and Complete Safety Check Use Case Specification**


#### UC-MH-19 Perform Exercise Session and Review Results

| UC ID and Name | UC-MH-19 Perform Exercise Session and Review Results |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Mother Journey and Health | **Platform** | Mobile / Backend / Camera / Posture Sidecar |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Nested Mobile exercise session, result, and history screens to achieve the stated outcome. |  |  |
| **Description** | Load the active posture configuration, start an eligible exercise session, run posture analysis, complete/abort it, and review the result or history. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/exercises/sessions/history: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/complete: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/pause: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/sessions/{sessionId}/posture-events: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/sessions/{sessionId}/result: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/resume: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}/posture-config: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/{exerciseId}/sessions: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /v1/inference/landmarks: No explicit internal-key dependency on this handler; router/application policy must be checked. <br> PRE-3. The resource is in a state eligible for: Load the active posture configuration and start a session after a passed safety check/camera permission. |  |  |
| **Postconditions** | POST-1. Complete/abort and review the stored result/history. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the active posture configuration and start a session after a passed safety check/camera permission. <br> 2. Process supported posture observations through the configured posture-correction sidecar or implemented fallback. <br> 3. Complete/abort and review the stored result/history. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-MH-19`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/exercises/sessions/history: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/complete: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/pause: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/sessions/{sessionId}/posture-events: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/sessions/{sessionId}/result: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); PATCH /api/v1/exercises/sessions/{sessionId}/resume: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); GET /api/v1/exercises/{exerciseId}/posture-config: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /api/v1/exercises/{exerciseId}/sessions: hasAnyRole('MOTHER', 'SYSTEM_ADMIN'); POST /v1/inference/landmarks: No explicit internal-key dependency on this handler; router/application policy must be checked. <br> E3. Explicit handler failures are `400, 422, 503`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Server session state is canonical; camera/model feedback is advisory. The sidecar inference contract validates sequence and landmark payloads; provider failure follows the implemented backend fallback/degraded path. Late frames and retries must not mutate a completed/aborted session. |  |  |
| **Implemented Entry Points** | Nested Mobile exercise session, result, and history screens |  |  |
| **Implemented Contracts** | GET `/api/v1/exercises/{id}/posture-config`; POST `/api/v1/exercises/{id}/sessions`; Exercise-session endpoints under `/api/v1/exercises/sessions/**`; POST `/v1/inference/landmarks` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/ExerciseSessionController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/service/impl/PostureAnalysisServiceImpl.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapter.java`; `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`; `05_Development/CareBridgeMobileApp/lib/features/exercise/screens/exercise_session_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/ExerciseSessionHistoryEmbeddedPostgresTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureAnalysisServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/inference/ExerciseCorrectionHttpAdapterTest.java`; `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/tests/test_http_contract.py`; `05_Development/CareBridgeMobileApp/test/features/exercise/exercise_session_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 42: UC-MH-19 Perform Exercise Session and Review Results Use Case Specification**


### 3.6 Baby Care

#### UC-BC-01 Manage Baby Profiles

| UC ID and Name | UC-BC-01 Manage Baby Profiles |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/babies`, `/babies/add`, `/babies/detail/:id`, `/babies/:id/edit` to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, activate, and archive an eligible baby profile. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/babies: isAuthenticated(); POST /api/v1/babies: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}: isAuthenticated(); PUT /api/v1/babies/{babyId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/babies/{babyId}/active: hasRole('MOTHER'); POST /api/v1/babies/{babyId}/archive: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Create or select a baby profile. |  |  |
| **Postconditions** | POST-1. Archive an eligible profile and refresh the owned list. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or select a baby profile. <br> 2. View/update supported profile fields or mark it active. <br> 3. Archive an eligible profile and refresh the owned list. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/babies: isAuthenticated(); POST /api/v1/babies: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}: isAuthenticated(); PUT /api/v1/babies/{babyId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/babies/{babyId}/active: hasRole('MOTHER'); POST /api/v1/babies/{babyId}/archive: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Baby access follows mother/care-group authorization. Archive and active-profile state are server authoritative. |  |  |
| **Implemented Entry Points** | Mobile `/babies`, `/babies/add`, `/babies/detail/:id`, `/babies/:id/edit`; Baby selector in Web `/mother/baby-care` |  |  |
| **Implemented Contracts** | Baby profile endpoints under `/api/v1/babies/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/baby/controller/BabyController.java`; `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profiles_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/baby/screens/add_baby_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/BabyServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/baby/BabyServiceArchiveTest.java`; `05_Development/CareBridgeMobileApp/test/features/baby/add_baby_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 43: UC-BC-01 Manage Baby Profiles Use Case Specification**


#### UC-BC-02 View Baby Care Hub and Detail Overview

| UC ID and Name | UC-BC-02 View Baby Care Hub and Detail Overview |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | Medium |
| **Trigger** | The actor enters Mobile baby profile detail to achieve the stated outcome. |  |  |
| **Description** | View the selected baby's care hub assembled from current daily-log, growth, milestone, and vaccination projections. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET `/api/v1/babies`: Bearer-authenticated mother/caregiver; backend filters authorized babies; PATCH `/api/v1/babies/{babyId}/active`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/daily-logs`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/growth-measurements?page=0&size=20`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/milestones`: Authorized baby relationship required; GET `/api/v1/vaccination/babies/{babyId}/records` plus schedule: Authorized baby relationship required. <br> PRE-3. The resource is in a state eligible for: Select an authorized baby. |  |  |
| **Postconditions** | POST-1. Navigate to the corresponding detailed care flow. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Select an authorized baby. <br> 2. Load the current care projections from their owning resources. <br> 3. Navigate to the corresponding detailed care flow. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Web BabyCareHub has no focused test; backend `care-overview`/`care-timeline` routes have no client consumer. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET `/api/v1/babies`: Bearer-authenticated mother/caregiver; backend filters authorized babies; PATCH `/api/v1/babies/{babyId}/active`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/daily-logs`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/growth-measurements?page=0&size=20`: Authorized baby relationship required; GET `/api/v1/babies/{babyId}/milestones`: Authorized baby relationship required; GET `/api/v1/vaccination/babies/{babyId}/records` plus schedule: Authorized baby relationship required. <br> E3. Explicit handler failures are `composition preserves each owning resource's current error/degraded behavior`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | The hub is a composition; it does not create a separate canonical care-overview resource. Baby authorization is rechecked by every underlying endpoint. |  |  |
| **Implemented Entry Points** | Mobile baby profile detail; Web `/mother/baby-care` |  |  |
| **Implemented Contracts** | Read endpoints for daily logs, growth, milestones, and vaccination records |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_profile_detail_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeMobileApp/test/features/baby/baby_care_contract_test.dart` |  |  |
| **Known Gaps / Exclusions** | Web BabyCareHub has no focused test; backend `care-overview`/`care-timeline` routes have no client consumer. |  |  |

**Table 44: UC-BC-02 View Baby Care Hub and Detail Overview Use Case Specification**


#### UC-BC-03 Manage Baby Daily Logs

| UC ID and Name | UC-BC-03 Manage Baby Daily Logs |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*` to achieve the stated outcome. |  |  |
| **Description** | Create, view, edit, and remove eligible baby feeding, sleep, diaper, or other supported daily logs. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/babies/{babyId}/daily-logs: isAuthenticated(); POST /api/v1/babies/{babyId}/daily-logs: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/babies/{babyId}/daily-logs/{logId}: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}/daily-logs/{logId}: isAuthenticated(); PUT /api/v1/babies/{babyId}/daily-logs/{logId}: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Choose a supported log type and enter type-specific data. |  |  |
| **Postconditions** | POST-1. Edit or delete it when ownership/state permits. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose a supported log type and enter type-specific data. <br> 2. Persist and inspect the log detail. <br> 3. Edit or delete it when ownership/state permits. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/babies/{babyId}/daily-logs: isAuthenticated(); POST /api/v1/babies/{babyId}/daily-logs: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/babies/{babyId}/daily-logs/{logId}: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}/daily-logs/{logId}: isAuthenticated(); PUT /api/v1/babies/{babyId}/daily-logs/{logId}: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Type-specific field validation and baby authorization are server authoritative. Log time/order uses server-normalized values. |  |  |
| **Implemented Entry Points** | Baby detail log modal and Mobile `/babies/:babyId/daily-logs/:logId*`; Web baby-care log forms |  |  |
| **Implemented Contracts** | Daily-log endpoints under `/api/v1/babies/{babyId}/daily-logs/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyDailyLogController.java`; `05_Development/CareBridgeMobileApp/lib/features/baby/screens/edit_baby_daily_log_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyDailyLogServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 45: UC-BC-03 Manage Baby Daily Logs Use Case Specification**


#### UC-BC-04 Review 24-Hour Daily Log Summary

| UC ID and Name | UC-BC-04 Review 24-Hour Daily Log Summary |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/babies/:babyId/log-summary` to achieve the stated outcome. |  |  |
| **Description** | View the server-computed 24-hour summary of supported daily-log categories for an authorized baby. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/babies/{babyId}/daily-logs/summary: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Select an authorized baby and summary period. |  |  |
| **Postconditions** | POST-1. Review counts/durations and open detailed logs when supported. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Select an authorized baby and summary period. <br> 2. Load the server aggregation. <br> 3. Review counts/durations and open detailed logs when supported. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/babies/{babyId}/daily-logs/summary: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | The summary is derived from canonical logs and must not expose another baby's data. Empty periods return a stable empty summary rather than fabricated values. |  |  |
| **Implemented Entry Points** | Mobile `/babies/:babyId/log-summary` |  |  |
| **Implemented Contracts** | GET `/api/v1/babies/{babyId}/daily-logs/summary` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyLogSummaryController.java`; `05_Development/CareBridgeMobileApp/lib/features/baby/screens/baby_log_summary_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/BabyLogSummaryServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/baby/baby_log_summary_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 46: UC-BC-04 Review 24-Hour Daily Log Summary Use Case Specification**


#### UC-BC-05 Manage Growth Measurements and Chart

| UC ID and Name | UC-BC-05 Manage Growth Measurements and Chart |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/babies/:babyId/growth` and baby growth tab to achieve the stated outcome. |  |  |
| **Description** | Record and manage growth measurements and view the derived growth chart for an authorized baby. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/babies/{babyId}/growth-chart: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}/growth-measurements: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/babies/{babyId}/growth-measurements: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Enter an age/date-appropriate growth measurement. |  |  |
| **Postconditions** | POST-1. Load and review the chart projection. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Enter an age/date-appropriate growth measurement. <br> 2. Persist/update/remove it when allowed. <br> 3. Load and review the chart projection. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/babies/{babyId}/growth-chart: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/babies/{babyId}/growth-measurements: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/babies/{babyId}/growth-measurements: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/babies/{babyId}/growth-measurements/{growthMeasurementId}: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `400`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Measurement units, ranges, chronology, and baby authorization are server authoritative. Growth charts are informational and not a diagnosis. |  |  |
| **Implemented Entry Points** | Mobile `/babies/:babyId/growth` and baby growth tab; Web baby-care growth panel |  |  |
| **Implemented Contracts** | Growth measurement endpoints under `/api/v1/babies/{babyId}/growth-measurements/**`; GET `/api/v1/babies/{babyId}/growth-chart` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/GrowthMeasurementController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/GrowthChartController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/growth_measurement_history_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/service/GrowthServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/healthRecords/growth_measurement_form_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 47: UC-BC-05 Manage Growth Measurements and Chart Use Case Specification**


#### UC-BC-06 Manage Development Milestones

| UC ID and Name | UC-BC-06 Manage Development Milestones |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Mobile milestone add/detail routes and baby detail milestone tab to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, and remove supported development milestone records for an authorized baby. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/babies/{babyId}/milestones: isAuthenticated(); POST /api/v1/babies/{babyId}/milestones: hasRole('MOTHER'); DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}: hasRole('MOTHER'); PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Choose a supported milestone and observed date/state. |  |  |
| **Postconditions** | POST-1. Edit/delete when authorization and state allow. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose a supported milestone and observed date/state. <br> 2. Persist and view the record. <br> 3. Edit/delete when authorization and state allow. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/babies/{babyId}/milestones: isAuthenticated(); POST /api/v1/babies/{babyId}/milestones: hasRole('MOTHER'); DELETE /api/v1/babies/{babyId}/milestones/{milestoneId}: hasRole('MOTHER'); PATCH /api/v1/babies/{babyId}/milestones/{milestoneId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Milestone ownership and chronology are server authoritative. Milestone status is tracking information, not a diagnosis. |  |  |
| **Implemented Entry Points** | Mobile milestone add/detail routes and baby detail milestone tab; Web baby-care milestone panel |  |  |
| **Implemented Contracts** | Milestone endpoints under `/api/v1/babies/{babyId}/milestones/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/MilestoneController.java`; `05_Development/CareBridgeMobileApp/lib/features/baby/screens/record_milestone_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/carejourney/MilestoneServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/baby/milestone_model_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 48: UC-BC-06 Manage Development Milestones Use Case Specification**


#### UC-BC-07 Manage Vaccination Records

| UC ID and Name | UC-BC-07 Manage Vaccination Records |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Mobile vaccination add/detail/edit routes to achieve the stated outcome. |  |  |
| **Description** | Create, view, update, and remove vaccination completion records for an authorized baby. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/vaccination/babies/{babyId}/records: isAuthenticated(); POST /api/v1/vaccination/babies/{babyId}/records: isAuthenticated(); DELETE /api/v1/vaccination/babies/{babyId}/records/{recordId}: isAuthenticated(); PATCH /api/v1/vaccination/babies/{babyId}/records/{recordId}: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Choose a scheduled or supported vaccine/dose. |  |  |
| **Postconditions** | POST-1. View/update/delete the eligible record. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose a scheduled or supported vaccine/dose. <br> 2. Record completion details and evidence where supported. <br> 3. View/update/delete the eligible record. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-07`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/vaccination/babies/{babyId}/records: isAuthenticated(); POST /api/v1/vaccination/babies/{babyId}/records: isAuthenticated(); DELETE /api/v1/vaccination/babies/{babyId}/records/{recordId}: isAuthenticated(); PATCH /api/v1/vaccination/babies/{babyId}/records/{recordId}: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Dose identity, chronology, duplication, and baby authorization are server authoritative. A record does not change the vaccine catalogue itself. |  |  |
| **Implemented Entry Points** | Mobile vaccination add/detail/edit routes; Web baby-care vaccination panel |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/vaccination/babies/{babyId}/records`; PATCH/DELETE `/api/v1/vaccination/babies/{babyId}/records/{recordId}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/vaccination_record_form_screen.dart`; `05_Development/CareBridgeWebApp/src/features/babyCare/pages/BabyCareHubPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/healthRecords/vaccination_model_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 49: UC-BC-07 Manage Vaccination Records Use Case Specification**


#### UC-BC-08 Review Vaccination Schedule and Create Next-Dose Reminder

| UC ID and Name | UC-BC-08 Review Vaccination Schedule and Create Next-Dose Reminder |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Baby Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Caregiver | **Confidence** | High |
| **Trigger** | The actor enters Baby-detail vaccination tab to achieve the stated outcome. |  |  |
| **Description** | Review the baby's vaccination schedule and create a supported reminder for an upcoming suggested dose. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Caregiver can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/reminders/vaccination: hasRole('MOTHER'); GET /api/v1/reminders/vaccination/suggestions: hasRole('MOTHER'); GET /api/v1/vaccination/babies/{babyId}/schedule: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Load the authorized baby's computed schedule. |  |  |
| **Postconditions** | POST-1. Create and display the linked reminder. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the authorized baby's computed schedule. <br> 2. Select an upcoming eligible dose/suggestion. <br> 3. Create and display the linked reminder. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-BC-08`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/reminders/vaccination: hasRole('MOTHER'); GET /api/v1/reminders/vaccination/suggestions: hasRole('MOTHER'); GET /api/v1/vaccination/babies/{babyId}/schedule: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Schedule/suggestion state comes from canonical vaccination records and catalogue rules. Creating a reminder does not mark a dose completed. |  |  |
| **Implemented Entry Points** | Baby-detail vaccination tab; Mobile `/reminders/vaccination/add` |  |  |
| **Implemented Contracts** | GET `/api/v1/vaccination/babies/{babyId}/schedule`; POST `/api/v1/reminders/vaccination`; GET `/api/v1/reminders/vaccination/suggestions` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/services/vaccination_service.dart`; `05_Development/CareBridgeMobileApp/lib/features/reminder/screens/create_vaccination_reminder_screen.dart`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/reminder/controller/ReminderController.java`; `05_Development/CareBridgeMobileApp/lib/features/reminder/services/reminder_service.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationBookServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/vaccination/VaccinationReminderDispatchTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/reminder/VaccinationReminderServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 50: UC-BC-08 Review Vaccination Schedule and Create Next-Dose Reminder Use Case Specification**


### 3.7 Community and Content Consumption

#### UC-CO-01 Browse and Search Community Q&A

| UC ID and Name | UC-CO-01 Browse and Search Community Q&A |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile Community Feed and Question Detail screens to achieve the stated outcome. |  |  |
| **Description** | Browse the moderated community feed, search eligible questions, inspect a question, and browse the topic directory. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/community/feed: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/community/questions: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/community/questions/{id}: isAuthenticated(); GET /api/v1/community/topics: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Load the eligible moderated feed or topic directory. |  |  |
| **Postconditions** | POST-1. Open a visible question detail and its allowed actions. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the eligible moderated feed or topic directory. <br> 2. Search/filter community questions. <br> 3. Open a visible question detail and its allowed actions. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/community/feed: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/community/questions: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/community/questions/{id}: isAuthenticated(); GET /api/v1/community/topics: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Publication/moderation state controls visibility. Community Q&A is distinct from verified health-content lifecycle. |  |  |
| **Implemented Entry Points** | Mobile Community Feed and Question Detail screens |  |  |
| **Implemented Contracts** | GET `/api/v1/community/feed`; GET `/api/v1/community/questions`; GET `/api/v1/community/questions/{id}`; GET `/api/v1/community/topics` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityFeedController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/community_feed_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityFeedControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityFeedServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionSearchServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/community/community_feed_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 51: UC-CO-01 Browse and Search Community Q&A Use Case Specification**


#### UC-CO-02 Manage Own Community Questions

| UC ID and Name | UC-CO-02 Manage Own Community Questions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Backend / File Storage |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile Create Question, My Questions, and Edit Question screens to achieve the stated outcome. |  |  |
| **Description** | Create, list, edit, and delete the authenticated user's community questions with supported image attachments. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/community/questions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/community/questions/mine: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/community/questions/{id}: isAuthenticated(); PATCH /api/v1/community/questions/{id}: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Choose an eligible topic and create a question. |  |  |
| **Postconditions** | POST-1. Edit/delete an eligible owned question and reconcile attachments. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose an eligible topic and create a question. <br> 2. List/open questions owned by the actor. <br> 3. Edit/delete an eligible owned question and reconcile attachments. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/community/questions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/community/questions/mine: hasAnyRole('MOTHER', 'FAMILY'); DELETE /api/v1/community/questions/{id}: isAuthenticated(); PATCH /api/v1/community/questions/{id}: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Question ownership and moderation lifecycle are server authoritative. Image upload/orphan cleanup follows current file policy. |  |  |
| **Implemented Entry Points** | Mobile Create Question, My Questions, and Edit Question screens |  |  |
| **Implemented Contracts** | POST `/api/v1/community/questions`; GET `/api/v1/community/questions/mine`; PATCH/DELETE `/api/v1/community/questions/{id}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionController.java`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/create_question_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/my_questions_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityQuestionControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityQuestionEditControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityQuestionDeleteControllerTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 52: UC-CO-02 Manage Own Community Questions Use Case Specification**


#### UC-CO-03 Answer Community Questions

| UC ID and Name | UC-CO-03 Answer Community Questions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Web Expert Portal / Backend / File Storage |
| **Primary Actor** | Authenticated User / Eligible Expert | **Confidence** | High |
| **Trigger** | The actor enters Mobile Post Answer screen to achieve the stated outcome. |  |  |
| **Description** | Post, edit, or delete an eligible answer and allow experts to process questions through the expert queue. |  |  |
| **Preconditions** | PRE-1. Authenticated User / Eligible Expert can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/community/questions/{questionId}/answers: isAuthenticated(); DELETE /api/v1/community/questions/{questionId}/answers/{id}: isAuthenticated(); PATCH /api/v1/community/questions/{questionId}/answers/{id}: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Open an answerable visible question. |  |  |
| **Postconditions** | POST-1. Edit/delete an owned eligible answer or process it through expert presentation rules. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open an answerable visible question. <br> 2. Submit a supported answer and optional attachment. <br> 3. Edit/delete an owned eligible answer or process it through expert presentation rules. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/community/questions/{questionId}/answers: isAuthenticated(); DELETE /api/v1/community/questions/{questionId}/answers/{id}: isAuthenticated(); PATCH /api/v1/community/questions/{questionId}/answers/{id}: isAuthenticated(). <br> E3. Explicit handler failures are `403`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Answer ownership and question/moderation state are server authoritative. Answer notification/presentation effects do not bypass canonical state. |  |  |
| **Implemented Entry Points** | Mobile Post Answer screen; Web `/expert/question-queue` |  |  |
| **Implemented Contracts** | Answer endpoints under `/api/v1/community/questions/{questionId}/answers/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerController.java`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/post_answer_screen.dart`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertQuestionQueuePage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/controller/CommunityAnswerControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityAnswerServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/community/post_answer_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 53: UC-CO-03 Answer Community Questions Use Case Specification**


#### UC-CO-04 Like, Bookmark, and Follow Community Content

| UC ID and Name | UC-CO-04 Like, Bookmark, and Follow Community Content |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Question/answer detail to achieve the stated outcome. |  |  |
| **Description** | Toggle question/answer likes, bookmark eligible questions, and follow topics for the authenticated user. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/community/answers/{answerId}/like: isAuthenticated(); GET /api/v1/community/me/bookmarks: isAuthenticated(); POST /api/v1/community/questions/{questionId}/bookmark: isAuthenticated(); POST /api/v1/community/questions/{questionId}/like: isAuthenticated(); POST /api/v1/community/topics/{id}/follow: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Open eligible content or a topic. |  |  |
| **Postconditions** | POST-1. Reload aggregate/owned engagement state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open eligible content or a topic. <br> 2. Toggle the requested engagement for the authenticated actor. <br> 3. Reload aggregate/owned engagement state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/community/answers/{answerId}/like: isAuthenticated(); GET /api/v1/community/me/bookmarks: isAuthenticated(); POST /api/v1/community/questions/{questionId}/bookmark: isAuthenticated(); POST /api/v1/community/questions/{questionId}/like: isAuthenticated(); POST /api/v1/community/topics/{id}/follow: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Each actor-target toggle is unique/idempotent according to its owning service. Engagement cannot make hidden/ineligible content visible. |  |  |
| **Implemented Entry Points** | Question/answer detail; Mobile Bookmarked Questions screen; Topic directory |  |  |
| **Implemented Contracts** | POST `/api/v1/community/questions/{questionId}/like`; POST `/api/v1/community/answers/{answerId}/like`; POST `/api/v1/community/questions/{questionId}/bookmark`; GET `/api/v1/community/me/bookmarks`; POST `/api/v1/community/topics/{id}/follow` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityQuestionLikeController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityAnswerLikeController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityBookmarkController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityQuestionLikeServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityAnswerLikeServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/CommunityBookmarkServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/service/TopicFollowServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 54: UC-CO-04 Like, Bookmark, and Follow Community Content Use Case Specification**


#### UC-CO-05 Browse Verified Health Content

| UC ID and Name | UC-CO-05 Browse Verified Health Content |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/content` to achieve the stated outcome. |  |  |
| **Description** | Browse/search consumer-visible verified content and open lifecycle/checklist content eligible for the current stage. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/content: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/checklists: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/lifecycle: hasRole('MOTHER'); GET /api/v1/content/lifecycle/{id}: hasRole('MOTHER'); GET /api/v1/content/search: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/{id}: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Load/search consumer-visible verified content. |  |  |
| **Postconditions** | POST-1. Open a visible detail/checklist and follow its allowed next action. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load/search consumer-visible verified content. <br> 2. Filter by current lifecycle/stage when supported. <br> 3. Open a visible detail/checklist and follow its allowed next action. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/content: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/checklists: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/lifecycle: hasRole('MOTHER'); GET /api/v1/content/lifecycle/{id}: hasRole('MOTHER'); GET /api/v1/content/search: No @PreAuthorize on handler/class; effective access comes from the security chain; GET /api/v1/content/{id}: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only publication/lifecycle-eligible content is consumer-visible. Verified content is editorial content, not community Q&A. |  |  |
| **Implemented Entry Points** | Mobile `/content`; View Content and Verified Content Detail screens |  |  |
| **Implemented Contracts** | GET `/api/v1/content`; GET `/api/v1/content/{id}`; GET `/api/v1/content/search`; GET `/api/v1/content/lifecycle`; GET `/api/v1/content/lifecycle/{id}`; GET `/api/v1/content/checklists` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/view_content_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/ContentSearchServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/unit/LifecycleContentServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/community/services/content_service_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 55: UC-CO-05 Browse Verified Health Content Use Case Specification**


#### UC-CO-06 Report Community Content or Account

| UC ID and Name | UC-CO-06 Report Community Content or Account |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Community and Content Consumption | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Confidence** | High |
| **Trigger** | The actor enters Report action from Mobile community content/account surfaces to achieve the stated outcome. |  |  |
| **Description** | Submit a supported report against eligible community content or an account for moderator review. |  |  |
| **Preconditions** | PRE-1. Authenticated User can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/reports: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Choose the report target and supported reason. |  |  |
| **Postconditions** | POST-1. Display the created/duplicate/ineligible result. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose the report target and supported reason. <br> 2. Enter optional bounded detail and submit. <br> 3. Display the created/duplicate/ineligible result. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-CO-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/reports: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Target type, reporter identity, duplicate policy, and initial report state are server authoritative. Submitting a report does not directly punish or hide an account/content item. |  |  |
| **Implemented Entry Points** | Report action from Mobile community content/account surfaces |  |  |
| **Implemented Contracts** | POST `/api/v1/reports` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ReportController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/ReportServiceImpl.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/report/ReportControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/report/ReportServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/report/ReportIntegrationTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 56: UC-CO-06 Report Community Content or Account Use Case Specification**


### 3.8 AI Nurse and Clinical Assistance

#### UC-AI-01 Use AI Nurse RAG Chat

| UC ID and Name | UC-AI-01 Use AI Nurse RAG Chat |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | AI Nurse and Clinical Assistance | **Platform** | Mobile / Spring Gateway / Python AI Service |
| **Primary Actor** | Mother / Family where allowed | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/rag/chat` and local chat history/session sheet to achieve the stated outcome. |  |  |
| **Description** | Ask a maternal-care question through the reachable AI Nurse chat, receive a grounded advisory response with citations/disclaimer, and follow deterministic expert/emergency guardrails. |  |  |
| **Preconditions** | PRE-1. Mother / Family where allowed can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/chat/message: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/rag/answer: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> PRE-3. The resource is in a state eligible for: Submit a supported question plus minimal allowed context. |  |  |
| **Postconditions** | POST-1. Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Submit a supported question plus minimal allowed context. <br> 2. Retrieve stage-eligible knowledge and generate through the configured model chain. <br> 3. Apply deterministic safety flags, return sources/disclaimer, and render degraded mode safely if generation fails. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AI-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/chat/message: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/rag/answer: No @PreAuthorize on handler/class; effective access comes from the security chain. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Python `RagChatRequest` carries message, stage, optional mother-only gestational age/survey/recent metrics, role, and conversation history; `RagChatResponse` returns answer, critical/expert flags, follow-ups, citations, disclaimer, and generated time. Mobile currently calls Python directly with literal internal key `carebridge`, then falls back to authenticated Spring `/api/v1/rag/answer`; this compiled-key boundary is not production-safe. Python retrieval defaults stage to `PREGNANCY`, includes stage plus `ALL`, requests top 4, and excludes returned chunks with similarity below `0.35` before prompting. Hybrid retrieval ranks by `0.35 * vector similarity + 0.20 * keyword ratio + title/phrase boosts` and de-duplicates title/section candidates. Retrieval-query expansion uses the latest user/human turn among the last two history messages; the prompt includes at most the last six messages. Family requests exclude gestational age, survey profile, and recent metrics; Mother requests include only supported formatted fields. Citations are created only from valid retrieved chunks, de-duplicated by title/section, and omit a generic root citation when a specific section exists. Generation tries distinct configured/fallback Gemini models and returns a bounded static response when all provider calls fail. Model tags are removed from visible text and mapped to critical/expert flags/follow-ups; deterministic abnormal-metric rules can raise but never lower expert consultation. Every Python success/degraded response includes the configured medical disclaimer; Spring returns its own constant disclaimer and explicit fallback flag. RAG is advisory/non-diagnostic; deterministic safety rules are the floor and citations must come from retrieved knowledge. |  |  |
| **Implemented Entry Points** | Mobile `/rag/chat` and local chat history/session sheet |  |  |
| **Implemented Contracts** | POST Spring `/api/v1/rag/answer`; POST Python `/api/v1/chat/message` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/controller/RagController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/service/RagPolicyServiceImpl.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/dto/RagAnswerRequest.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/integration/gemini/dto/RagAnswerResponse.java`; `05_Development/CareBridgeAITriageService/app/api/v1/chat.py`; `05_Development/CareBridgeAITriageService/app/models/schemas.py`; `05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py`; `05_Development/CareBridgeAITriageService/app/rag/vector_store.py`; `05_Development/CareBridgeAITriageService/app/rag/prompts.py`; `05_Development/CareBridgeAITriageService/app/core/gemini.py`; `05_Development/CareBridgeAITriageService/app/core/security.py`; `05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/gemini/RagControllerTest.java`; `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`; `05_Development/CareBridgeAITriageService/tests/test_rag_chat.py` |  |  |
| **Known Gaps / Exclusions** | Structured triage session/history/handoff backend infrastructure has no reachable intake UI and remains Partial. The literal `carebridge` key is accepted even when another expected production key is configured; record this as a failing production-security expectation. No focused Mobile test currently proves Python failure to Spring fallback and response-shape downgrade. |  |  |

**Table 57: UC-AI-01 Use AI Nurse RAG Chat Use Case Specification**


### 3.9 Emergency and Safety

#### UC-ES-01 Find and Navigate to Care Facility

| UC ID and Name | UC-ES-01 Find and Navigate to Care Facility |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Emergency and Safety | **Platform** | Mobile / Backend / Map Provider |
| **Primary Actor** | Mother / Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/emergency/map` to achieve the stated outcome. |  |  |
| **Description** | Find nearby or listed eligible care facilities, inspect one, calculate a route, and hand off to external navigation when supported. |  |  |
| **Preconditions** | PRE-1. Mother / Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/map/emergency/handoff: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/map/facilities: isAuthenticated(); GET /api/v1/map/facilities/{id}: isAuthenticated(); GET /api/v1/map/nearby-facilities: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/map/route: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Provide/confirm allowed location context. |  |  |
| **Postconditions** | POST-1. Calculate a route or hand off to supported navigation. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Provide/confirm allowed location context. <br> 2. Load nearby/listed facilities and choose one. <br> 3. Calculate a route or hand off to supported navigation. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-ES-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/map/emergency/handoff: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/map/facilities: isAuthenticated(); GET /api/v1/map/facilities/{id}: isAuthenticated(); GET /api/v1/map/nearby-facilities: hasAnyRole('MOTHER', 'FAMILY'); POST /api/v1/map/route: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location use is consent/permission gated. Map/provider failure must not report a false route or facility verification state. |  |  |
| **Implemented Entry Points** | Mobile `/emergency/map` |  |  |
| **Implemented Contracts** | GET `/api/v1/map/facilities`; GET `/api/v1/map/nearby-facilities`; GET `/api/v1/map/facilities/{id}`; POST `/api/v1/map/route`; POST `/api/v1/map/emergency/handoff` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/CareFacilityController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java`; `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/map/CareFacilityServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyMapHandoffServiceImplTest.java`; `05_Development/CareBridgeMobileApp/test/features/emergency/emergency_map_screen_test.dart`; `05_Development/CareBridgeMobileApp/test/features/emergency/trackasia_web_contract_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 58: UC-ES-01 Find and Navigate to Care Facility Use Case Specification**


#### UC-ES-02 Start and Resolve Emergency Session

| UC ID and Name | UC-ES-02 Start and Resolve Emergency Session |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Emergency and Safety | **Platform** | Mobile / Backend / Notification / Location |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/emergency/map` to achieve the stated outcome. |  |  |
| **Description** | Open an emergency session, share permitted location/alert context with family, view the active session, and resolve it when the incident ends. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/emergency/location-shares: hasRole('MOTHER'); POST /api/v1/emergency/sessions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/emergency/sessions/active: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/emergency/sessions/{id}/resolve: hasAnyRole('MOTHER', 'FAMILY'). <br> PRE-3. The resource is in a state eligible for: Confirm/start an emergency session. |  |  |
| **Postconditions** | POST-1. Resolve the owned active session when safe. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Confirm/start an emergency session. <br> 2. Publish permitted alert/location context and show the active session. <br> 3. Resolve the owned active session when safe. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-ES-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/emergency/location-shares: hasRole('MOTHER'); POST /api/v1/emergency/sessions: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/emergency/sessions/active: hasAnyRole('MOTHER', 'FAMILY'); PATCH /api/v1/emergency/sessions/{id}/resolve: hasAnyRole('MOTHER', 'FAMILY'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Emergency session state is canonical; delivery retries cannot create duplicate active incidents. Location sharing is consent gated and is a subflow, not a separate UC. |  |  |
| **Implemented Entry Points** | Mobile `/emergency/map`; Mobile `/emergency/alert/:sessionId` |  |  |
| **Implemented Contracts** | POST `/api/v1/emergency/sessions`; GET `/api/v1/emergency/sessions/active`; PATCH `/api/v1/emergency/sessions/{id}/resolve`; POST `/api/v1/emergency/location-shares` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/FamilyLocationShareController.java`; `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_alert_detail_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencySessionOpenedHandlerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/FamilyLocationShareServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 59: UC-ES-02 Start and Resolve Emergency Session Use Case Specification**


#### UC-ES-03 Respond to Family Emergency Alert

| UC ID and Name | UC-ES-03 Respond to Family Emergency Alert |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Emergency and Safety | **Platform** | Mobile / Backend / Notification / Map Provider |
| **Primary Actor** | Authorized Family Member | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/family-alert/:sessionId` and family alert list to achieve the stated outcome. |  |  |
| **Description** | Open a family alert, acknowledge it, contact the mother, or navigate toward the shared emergency location. |  |  |
| **Preconditions** | PRE-1. Authorized Family Member can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/emergency/sessions/{id}/alert: hasAnyRole('MOTHER', 'FAMILY'); PUT /api/v1/emergency/sessions/{id}/alert/acknowledge: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/family-alerts: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Open an alert belonging to an authorized care-group relationship. |  |  |
| **Postconditions** | POST-1. Call the mother or open directions using current shared location. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open an alert belonging to an authorized care-group relationship. <br> 2. Acknowledge the active alert. <br> 3. Call the mother or open directions using current shared location. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-ES-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/emergency/sessions/{id}/alert: hasAnyRole('MOTHER', 'FAMILY'); PUT /api/v1/emergency/sessions/{id}/alert/acknowledge: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/family-alerts: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Family authorization and active-session state are rechecked. Acknowledgement does not resolve the mother's emergency session. |  |  |
| **Implemented Entry Points** | Mobile `/family-alert/:sessionId` and family alert list |  |  |
| **Implemented Contracts** | GET `/api/v1/family-alerts`; GET `/api/v1/emergency/sessions/{id}/alert`; PUT `/api/v1/emergency/sessions/{id}/alert/acknowledge` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyAlertController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyController.java`; `05_Development/CareBridgeMobileApp/lib/features/emergency/screens/family_alert_detail_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/EmergencyAlertAcknowledgementServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/emergency/FamilyAlertServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/emergency/family_alert_detail_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 60: UC-ES-03 Respond to Family Emergency Alert Use Case Specification**


#### UC-ES-04 Configure and Test Fall Detection

| UC ID and Name | UC-ES-04 Configure and Test Fall Detection |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Emergency and Safety | **Platform** | Mobile / Backend / Device Sensors |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/safety/fall-detection/enable` to achieve the stated outcome. |  |  |
| **Description** | Grant required consent/permissions, enable or disable fall detection, configure current safety settings, and complete sensor self-test. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/safety/config: hasRole('MOTHER'); PUT /api/v1/safety/config: hasRole('MOTHER'); POST /api/v1/safety/events/sensor-self-test: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/sensor-self-test/complete: hasRole('MOTHER'); POST /api/v1/safety/fall-detection/disable: hasRole('MOTHER'); POST /api/v1/safety/fall-detection/enable: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Review consent/device permission prerequisites. |  |  |
| **Postconditions** | POST-1. Run and complete the supported sensor self-test or disable monitoring. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Review consent/device permission prerequisites. <br> 2. Enable/configure fall detection. <br> 3. Run and complete the supported sensor self-test or disable monitoring. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-ES-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/safety/config: hasRole('MOTHER'); PUT /api/v1/safety/config: hasRole('MOTHER'); POST /api/v1/safety/events/sensor-self-test: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/sensor-self-test/complete: hasRole('MOTHER'); POST /api/v1/safety/fall-detection/disable: hasRole('MOTHER'); POST /api/v1/safety/fall-detection/enable: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Consent, device permission, and latest server configuration are required before monitoring. A failed self-test must not be presented as an enabled healthy detector. |  |  |
| **Implemented Entry Points** | Mobile `/safety/fall-detection/enable`; Mobile `/safety` |  |  |
| **Implemented Contracts** | GET/PUT `/api/v1/safety/config`; POST `/api/v1/safety/fall-detection/enable`; POST `/api/v1/safety/fall-detection/disable`; POST `/api/v1/safety/events/sensor-self-test`; POST `/api/v1/safety/events/{eventId}/sensor-self-test/complete` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SafetyConfigController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/SensorSelfTestController.java`; `05_Development/CareBridgeMobileApp/lib/features/safety/screens/enable_fall_detection_screen.dart`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SafetyConfigServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SensorSelfTestServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/safety/safety_contract_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 61: UC-ES-04 Configure and Test Fall Detection Use Case Specification**


#### UC-ES-05 Respond to Detected Fall or Impact

| UC ID and Name | UC-ES-05 Respond to Detected Fall or Impact |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Emergency and Safety | **Platform** | Mobile / Backend / Device Sensors |
| **Primary Actor** | Mother | **Confidence** | High |
| **Trigger** | The actor enters Mobile Safety Monitoring screen and Safety Countdown sheet to achieve the stated outcome. |  |  |
| **Description** | Monitor device signals, display the suspected-fall countdown, and confirm safe/false-positive or escalate to emergency. |  |  |
| **Preconditions** | PRE-1. Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/safety/events: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/confirm: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/emergency-alert: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/false-positive: hasRole('MOTHER'); POST /api/v1/safety/imu-data: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Monitor configured sensor input and create a suspected event when rules trigger. |  |  |
| **Postconditions** | POST-1. Persist false-positive/confirmation or escalate to an emergency session. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Monitor configured sensor input and create a suspected event when rules trigger. <br> 2. Show a bounded countdown and allow the mother to respond. <br> 3. Persist false-positive/confirmation or escalate to an emergency session. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-ES-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/safety/events: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/confirm: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/emergency-alert: hasRole('MOTHER'); POST /api/v1/safety/events/{eventId}/false-positive: hasRole('MOTHER'); POST /api/v1/safety/imu-data: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Server event state and deterministic detector rules are canonical. Late/repeated signals must not reopen a resolved event or duplicate emergency alerts. |  |  |
| **Implemented Entry Points** | Mobile Safety Monitoring screen and Safety Countdown sheet |  |  |
| **Implemented Contracts** | POST `/api/v1/safety/imu-data`; GET `/api/v1/safety/events`; POST `/api/v1/safety/events/{eventId}/confirm`; POST `/api/v1/safety/events/{eventId}/false-positive`; POST `/api/v1/safety/events/{eventId}/emergency-alert` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/controller/FallDetectionController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/safety/service/impl/FallDetectionService.java`; `05_Development/CareBridgeMobileApp/lib/features/safety/screens/safety_monitoring_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/safety/widgets/safety_countdown_sheet.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/FallDetectionControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/FallDetectionServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/safety/SuspectedFallDetectedHandlerTest.java`; `05_Development/CareBridgeMobileApp/test/features/safety/fall_detection_sensor_service_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 62: UC-ES-05 Respond to Detected Fall or Impact Use Case Specification**


### 3.10 Family Cooperative Care

#### UC-FM-01 Manage Care Group Lifecycle

| UC ID and Name | UC-FM-01 Manage Care Group Lifecycle |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Family Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Group Owner / Member | **Confidence** | High |
| **Trigger** | The actor enters Mobile `/care-groups`, `/care-groups/add`, My Care Groups to achieve the stated outcome. |  |  |
| **Description** | Create, list, leave, delete when eligible, and relink the journey associated with a care group. |  |  |
| **Preconditions** | PRE-1. Mother / Group Owner / Member can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/care-groups: isAuthenticated(); POST /api/v1/care-groups: hasRole('MOTHER'); DELETE /api/v1/care-groups/{groupId}: hasRole('MOTHER'); PATCH /api/v1/care-groups/{groupId}/journey: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/leave: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Create or load care groups belonging to the actor. |  |  |
| **Postconditions** | POST-1. Delete a group only when owner/lifecycle rules allow. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load care groups belonging to the actor. <br> 2. Relink an eligible group to a journey or leave as a member. <br> 3. Delete a group only when owner/lifecycle rules allow. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-FM-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/care-groups: isAuthenticated(); POST /api/v1/care-groups: hasRole('MOTHER'); DELETE /api/v1/care-groups/{groupId}: hasRole('MOTHER'); PATCH /api/v1/care-groups/{groupId}/journey: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/leave: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Group ownership/membership and journey compatibility are server authoritative. Delete/leave/relink have distinct guarded effects. |  |  |
| **Implemented Entry Points** | Mobile `/care-groups`, `/care-groups/add`, My Care Groups |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/care-groups`; DELETE `/api/v1/care-groups/{groupId}`; POST `/api/v1/care-groups/{groupId}/leave`; PATCH `/api/v1/care-groups/{groupId}/journey` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_groups_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/my_care_groups_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareGroupJourneyRelinkServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/familySync/mother_care_group_ui_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 63: UC-FM-01 Manage Care Group Lifecycle Use Case Specification**


#### UC-FM-02 Manage Invitations, Join Requests, and Membership

| UC ID and Name | UC-FM-02 Manage Invitations, Join Requests, and Membership |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Family Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Group Owner / Invitee / Applicant | **Confidence** | High |
| **Trigger** | The actor enters Mobile care-group invitation/member/join-request screens to achieve the stated outcome. |  |  |
| **Description** | Invite a member, accept/decline/revoke an invitation, request to join, approve/reject a request, or remove an eligible member. |  |  |
| **Preconditions** | PRE-1. Group Owner / Invitee / Applicant can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/care-groups/invitations/me: isAuthenticated(); POST /api/v1/care-groups/invitations/{token}/accept: isAuthenticated(); POST /api/v1/care-groups/join: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/invitations/accept: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations/decline: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/join-requests: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/join-requests/{memberId}/respond: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/members: isAuthenticated(); DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Create an invitation or join request. |  |  |
| **Postconditions** | POST-1. Refresh canonical membership and invitation state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create an invitation or join request. <br> 2. The receiving/owning actor accepts, declines, revokes, approves, or rejects it. <br> 3. Refresh canonical membership and invitation state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-FM-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/care-groups/invitations/me: isAuthenticated(); POST /api/v1/care-groups/invitations/{token}/accept: isAuthenticated(); POST /api/v1/care-groups/join: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/invitations/accept: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations/decline: isAuthenticated(); POST /api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/join-requests: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/join-requests/{memberId}/respond: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/members: isAuthenticated(); DELETE /api/v1/care-groups/{groupId}/members/{targetUserId}: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Invitation/join-request states, expiry, uniqueness, and actor authority are server authoritative. A token or UI state cannot bypass membership policy. |  |  |
| **Implemented Entry Points** | Mobile care-group invitation/member/join-request screens |  |  |
| **Implemented Contracts** | GET `/api/v1/care-groups/invitations/me`; POST `/api/v1/care-groups/invitations/{token}/accept`; POST `/api/v1/care-groups/join`; POST `/api/v1/care-groups/{groupId}/invitations`; POST `/api/v1/care-groups/{groupId}/invitations/accept`; POST `/api/v1/care-groups/{groupId}/invitations/decline`; POST `/api/v1/care-groups/{groupId}/invitations/{targetUserId}/revoke`; GET `/api/v1/care-groups/{groupId}/join-requests`; POST `/api/v1/care-groups/{groupId}/join-requests/{memberId}/respond`; GET `/api/v1/care-groups/{groupId}/members`; DELETE `/api/v1/care-groups/{groupId}/members/{targetUserId}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_invitation_screen.dart`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/care_group_members_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareGroupInviteIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareGroupServiceImplMembershipLifecycleTest.java`; `05_Development/CareBridgeMobileApp/test/features/familySync/care_group_invitation_screen_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 64: UC-FM-02 Manage Invitations, Join Requests, and Membership Use Case Specification**


#### UC-FM-03 Manage Family Member Permissions

| UC ID and Name | UC-FM-03 Manage Family Member Permissions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Family Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Group Owner | **Confidence** | High |
| **Trigger** | The actor enters Mobile Manage Family Permission screen to achieve the stated outcome. |  |  |
| **Description** | View and update the bounded sharing permissions of an eligible care-group member. |  |  |
| **Preconditions** | PRE-1. Mother / Group Owner can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/care-groups/{groupId}/members/{memberId}/permissions: isAuthenticated(); PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions: hasRole('MOTHER'). <br> PRE-3. The resource is in a state eligible for: Open an eligible member's permission settings. |  |  |
| **Postconditions** | POST-1. Persist and reload the effective permission set. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open an eligible member's permission settings. <br> 2. Change only supported permission flags. <br> 3. Persist and reload the effective permission set. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-FM-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/care-groups/{groupId}/members/{memberId}/permissions: isAuthenticated(); PATCH /api/v1/care-groups/{groupId}/members/{memberId}/permissions: hasRole('MOTHER'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only the authorized owner/mother may change member permissions. Revoked permissions must affect subsequent data reads immediately according to policy. |  |  |
| **Implemented Entry Points** | Mobile Manage Family Permission screen |  |  |
| **Implemented Contracts** | GET/PATCH `/api/v1/care-groups/{groupId}/members/{memberId}/permissions` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/manage_family_permission_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/ManageFamilyPermissionIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/controller/CareGroupControllerPermissionTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareGroupServiceImplPermissionTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 65: UC-FM-03 Manage Family Member Permissions Use Case Specification**


#### UC-FM-04 Assign and Track Family Care Tasks

| UC ID and Name | UC-FM-04 Assign and Track Family Care Tasks |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Family Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Confidence** | High |
| **Trigger** | The actor enters Mobile Assigned Tasks screen to achieve the stated outcome. |  |  |
| **Description** | Assign a care task to an eligible member, view it, update status/details, or cancel it through the task lifecycle. |  |  |
| **Preconditions** | PRE-1. Mother / Authorized Family can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/care-groups/{groupId}/tasks: isAuthenticated(); POST /api/v1/care-groups/{groupId}/tasks: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/tasks/{taskId}: isAuthenticated(); PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel: hasRole('MOTHER'); PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Create a task for an eligible group/member. |  |  |
| **Postconditions** | POST-1. Update status/details or cancel using an allowed transition. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create a task for an eligible group/member. <br> 2. Load assigned/owned tasks and current state. <br> 3. Update status/details or cancel using an allowed transition. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-FM-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/care-groups/{groupId}/tasks: isAuthenticated(); POST /api/v1/care-groups/{groupId}/tasks: hasRole('MOTHER'); GET /api/v1/care-groups/{groupId}/tasks/{taskId}: isAuthenticated(); PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}: hasRole('MOTHER'); POST /api/v1/care-groups/{groupId}/tasks/{taskId}/cancel: hasRole('MOTHER'); PATCH /api/v1/care-groups/{groupId}/tasks/{taskId}/status: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Assignee membership, ownership, and finite-state transitions are server authoritative. Retries cannot create duplicate assignments beyond current policy. |  |  |
| **Implemented Entry Points** | Mobile Assigned Tasks screen |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/care-groups/{groupId}/tasks`; GET/PATCH `/api/v1/care-groups/{groupId}/tasks/{taskId}`; PATCH `/api/v1/care-groups/{groupId}/tasks/{taskId}/status`; POST `/api/v1/care-groups/{groupId}/tasks/{taskId}/cancel` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/CareGroupController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/service/impl/CareTaskServiceImpl.java`; `05_Development/CareBridgeMobileApp/lib/features/familySync/screens/assigned_tasks_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/CareTaskAssignmentIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/CareTaskServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/entity/CareTaskStatusFsmTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 66: UC-FM-04 Assign and Track Family Care Tasks Use Case Specification**


#### UC-FM-05 Monitor Shared Family Care

| UC ID and Name | UC-FM-05 Monitor Shared Family Care |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Family Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Authorized Family Member / Mother | **Confidence** | High |
| **Trigger** | The actor enters Family home, care-group detail, shared-data, and quick-note history screens to achieve the stated outcome. |  |  |
| **Description** | View family dashboard projections, permitted shared maternal/baby data, care-group checklist state/actions, and quick-note history. |  |  |
| **Preconditions** | PRE-1. Authorized Family Member / Mother can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks: hasRole('FAMILY'); GET /api/v1/care-groups/{careGroupId}/checklists/history: hasRole('FAMILY'); POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions: hasRole('FAMILY'); GET /api/v1/care-groups/{careGroupId}/quick-notes: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/care-groups/{groupId}/shared-data: isAuthenticated(); GET /api/v1/family/dashboard: isAuthenticated(). <br> PRE-3. The resource is in a state eligible for: Select an authorized care group. |  |  |
| **Postconditions** | POST-1. Apply an allowed checklist action or review/add a quick note and refresh history. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Select an authorized care group. <br> 2. Load dashboard/shared-data/checklist projections constrained by permissions. <br> 3. Apply an allowed checklist action or review/add a quick note and refresh history. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-FM-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/care-groups/{careGroupId}/checklists/current/tasks: hasRole('FAMILY'); GET /api/v1/care-groups/{careGroupId}/checklists/history: hasRole('FAMILY'); POST /api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions: hasRole('FAMILY'); GET /api/v1/care-groups/{careGroupId}/quick-notes: hasAnyRole('MOTHER', 'FAMILY'); GET /api/v1/care-groups/{groupId}/shared-data: isAuthenticated(); GET /api/v1/family/dashboard: isAuthenticated(). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Membership, permission, and consent are rechecked for every projection. Family emergency alerts are specified under UC-ES-03, not duplicated here. |  |  |
| **Implemented Entry Points** | Family home, care-group detail, shared-data, and quick-note history screens |  |  |
| **Implemented Contracts** | GET `/api/v1/family/dashboard`; GET `/api/v1/care-groups/{groupId}/shared-data`; GET `/api/v1/care-groups/{careGroupId}/checklists/current/tasks`; GET `/api/v1/care-groups/{careGroupId}/checklists/history`; POST `/api/v1/care-groups/{careGroupId}/checklists/tasks/{taskId}/actions`; Quick-note endpoints under `/api/v1/care-groups/{careGroupId}/quick-notes` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyDashboardController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/SharedDataController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/today/controller/CareGroupChecklistController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/family/controller/FamilyQuickNoteController.java`; `05_Development/CareBridgeMobileApp/lib/features/home/screens/family_member_home_screen.dart` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyDashboardServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/SharedDataServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/family/service/FamilyQuickNoteServiceTest.java`; `05_Development/CareBridgeMobileApp/test/features/familySync/family_dashboard_contract_test.dart` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 67: UC-FM-05 Monitor Shared Family Care Use Case Specification**


### 3.11 Administration and Operations

#### UC-AD-01 Manage User Accounts and Roles

| UC ID and Name | UC-AD-01 Manage User Accounts and Roles |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/users*` to achieve the stated outcome. |  |  |
| **Description** | Search and inspect user accounts, sessions/activity, update eligible account status, and assign an allowed role. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/users: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}/activity: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/users/{userId}/role: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}/sessions: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/users/{userId}/status: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Search/list users and open an account detail. |  |  |
| **Postconditions** | POST-1. Apply an allowed status/role mutation and record the result. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Search/list users and open an account detail. <br> 2. Inspect current status, role, sessions, and activity projection. <br> 3. Apply an allowed status/role mutation and record the result. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-01`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/users: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}/activity: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/users/{userId}/role: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/users/{userId}/sessions: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/users/{userId}/status: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | All mutations are System-Admin-only and audited. The role vocabulary comes from current server enums; UI options cannot grant unsupported roles. |  |  |
| **Implemented Entry Points** | Web `/admin/users*` |  |  |
| **Implemented Contracts** | GET/PATCH `/api/v1/admin/users/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminUserController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminRoleController.java`; `05_Development/CareBridgeWebApp/src/features/admin/pages/UserListPage.tsx`; `05_Development/CareBridgeWebApp/src/features/admin/pages/UserDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/controller/AdminUserControllerIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/service/AdminRoleServiceImplTest.java`; `05_Development/CareBridgeWebApp/src/features/admin/pages/UserListPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 68: UC-AD-01 Manage User Accounts and Roles Use Case Specification**


#### UC-AD-02 Provision Staff Accounts

| UC ID and Name | UC-AD-02 Provision Staff Accounts |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/staff-accounts/create` to achieve the stated outcome. |  |  |
| **Description** | Create a supported staff account with an allowed administrative/content/moderation role and bounded initial credential delivery. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/staff-accounts: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Choose an allowed staff role and enter required identity data. |  |  |
| **Postconditions** | POST-1. Display bounded credential/delivery outcome without logging secrets. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Choose an allowed staff role and enter required identity data. <br> 2. Create the staff account through the administrative contract. <br> 3. Display bounded credential/delivery outcome without logging secrets. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-02`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/staff-accounts: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only System Admin may provision staff. Allowed role, uniqueness, temporary credential, and audit behavior are server authoritative. |  |  |
| **Implemented Entry Points** | Web `/admin/staff-accounts/create` |  |  |
| **Implemented Contracts** | POST `/api/v1/admin/staff-accounts` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminStaffController.java`; `05_Development/CareBridgeWebApp/src/features/admin/pages/CreateStaffAccountPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/controller/AdminStaffControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/service/AdminStaffServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 69: UC-AD-02 Provision Staff Accounts Use Case Specification**


#### UC-AD-03 Review Account Lock Appeals

| UC ID and Name | UC-AD-03 Review Account Lock Appeals |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/account-lock-appeals*` to achieve the stated outcome. |  |  |
| **Description** | List and inspect pending account-lock appeals and record an eligible approval/rejection decision. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/account-lock-appeals: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/account-lock-appeals/{appealId}: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/account-lock-appeals/{appealId}/review: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load the appeal queue and open one current case. |  |  |
| **Postconditions** | POST-1. Approve or reject through the guarded review transition. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the appeal queue and open one current case. <br> 2. Review the block/appeal context. <br> 3. Approve or reject through the guarded review transition. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-03`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/account-lock-appeals: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/account-lock-appeals/{appealId}: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/account-lock-appeals/{appealId}/review: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only a pending eligible appeal can be reviewed. The decision is audited and is distinct from general audit-log inspection. |  |  |
| **Implemented Entry Points** | Web `/admin/account-lock-appeals*` |  |  |
| **Implemented Contracts** | GET/PATCH `/api/v1/admin/account-lock-appeals/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/admin/controller/AdminAccountLockAppealController.java`; `05_Development/CareBridgeWebApp/src/features/admin/pages/AccountLockAppealsPage.tsx`; `05_Development/CareBridgeWebApp/src/features/admin/pages/AccountLockAppealDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/identity/admin/service/AccountLockAppealServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 70: UC-AD-03 Review Account Lock Appeals Use Case Specification**


#### UC-AD-04 Inspect Audit Activity

| UC ID and Name | UC-AD-04 Inspect Audit Activity |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web dashboard projection / Backend |
| **Primary Actor** | System Admin / Authorized Operations | **Confidence** | High |
| **Trigger** | The actor enters Recent-audit section on Web `/admin/dashboard` to achieve the stated outcome. |  |  |
| **Description** | Inspect the supported recent audit projection exposed to authorized administrators. |  |  |
| **Preconditions** | PRE-1. System Admin / Authorized Operations can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/audit-logs: hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS'). <br> PRE-3. The resource is in a state eligible for: Open the admin dashboard audit section. |  |  |
| **Postconditions** | POST-1. Inspect sanitized actor/action/resource metadata. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open the admin dashboard audit section. <br> 2. Request permitted audit records with supported filters/page state. <br> 3. Inspect sanitized actor/action/resource metadata. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-04`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/audit-logs: hasAnyRole('SYSTEM_ADMIN', 'OPERATIONS'). <br> E3. Explicit handler failures are `400`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Audit access is restricted and returned payloads must not contain raw secrets or unnecessary health data. There is no separate full audit-log page in the current Web router. |  |  |
| **Implemented Entry Points** | Recent-audit section on Web `/admin/dashboard` |  |  |
| **Implemented Contracts** | GET `/api/v1/admin/audit-logs` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/audit/controller/AuditController.java`; `05_Development/CareBridgeWebApp/src/features/admin/models/auditLog.ts`; `05_Development/CareBridgeWebApp/src/features/admin/pages/AdminDashboardPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/controller/AuditControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/audit/AuditServiceImplTest.java`; `05_Development/CareBridgeWebApp/src/features/admin/models/auditLog.test.ts` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 71: UC-AD-04 Inspect Audit Activity Use Case Specification**


#### UC-AD-05 Configure System and Maintenance Mode

| UC ID and Name | UC-AD-05 Configure System and Maintenance Mode |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/system-configuration` to achieve the stated outcome. |  |  |
| **Description** | Read and update supported system configuration, including maintenance-mode behavior. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/system-configuration: hasRole('SYSTEM_ADMIN'); PUT /api/v1/admin/system-configuration: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load current versioned system configuration. |  |  |
| **Postconditions** | POST-1. Persist and observe maintenance/filter behavior where applicable. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load current versioned system configuration. <br> 2. Edit supported values and confirm the change. <br> 3. Persist and observe maintenance/filter behavior where applicable. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-05`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/system-configuration: hasRole('SYSTEM_ADMIN'); PUT /api/v1/admin/system-configuration: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only System Admin may mutate configuration. Validation, version/concurrency, maintenance allow-list, and audit behavior are server authoritative. |  |  |
| **Implemented Entry Points** | Web `/admin/system-configuration` |  |  |
| **Implemented Contracts** | GET/PUT `/api/v1/admin/system-configuration` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/systemconfiguration/controller/SystemConfigurationController.java`; `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SystemConfigurationPage.tsx`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/systemconfiguration/security/MaintenanceModeFilter.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/systemconfiguration/SystemConfigurationControllerSecurityTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/systemconfiguration/SystemConfigurationServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/systemconfiguration/MaintenanceModeFilterTest.java`; `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SystemConfigurationPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 72: UC-AD-05 Configure System and Maintenance Mode Use Case Specification**


#### UC-AD-06 Verify Experts and Credentials

| UC ID and Name | UC-AD-06 Verify Experts and Credentials |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend / File Storage |
| **Primary Actor** | System Admin / Authorized Reviewer | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/experts*` and `/admin/expert-verification-queue` to achieve the stated outcome. |  |  |
| **Description** | Review expert profiles, identity and credential evidence, record review decisions, and approve/reject/trust eligible experts. |  |  |
| **Preconditions** | PRE-1. System Admin / Authorized Reviewer can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/expert/admin/profiles: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/pending: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}/file: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}/preview: hasRole('SYSTEM_ADMIN'); PUT /api/v1/expert/credentials/{credentialId}/review: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/identity/files/{fileId}/url: hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); GET /api/v1/expert/identity/pending: hasRole('SYSTEM_ADMIN'); PUT /api/v1/expert/identity/{attemptId}/review: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/{expertProfileId}/approve: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/{expertProfileId}/reject: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/expert/profiles/{expertProfileId}/trust: hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN'); GET /api/v1/expert/review-cases: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/review-cases/{expertProfileId}: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load the verification queue and open an applicant case. |  |  |
| **Postconditions** | POST-1. Record the eligible decision and resulting profile/trust state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the verification queue and open an applicant case. <br> 2. Review purpose-authorized identity/credential evidence and registry results. <br> 3. Record the eligible decision and resulting profile/trust state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-06`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/expert/admin/profiles: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/pending: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}: hasRole('EXPERT'); GET /api/v1/expert/credentials/{credentialId}/file: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/credentials/{credentialId}/preview: hasRole('SYSTEM_ADMIN'); PUT /api/v1/expert/credentials/{credentialId}/review: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/identity/files/{fileId}/url: hasAnyRole('EXPERT', 'SYSTEM_ADMIN'); GET /api/v1/expert/identity/pending: hasRole('SYSTEM_ADMIN'); PUT /api/v1/expert/identity/{attemptId}/review: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/{expertProfileId}/approve: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/expert/profiles/{expertProfileId}/expert-type: hasRole('SYSTEM_ADMIN'); POST /api/v1/expert/profiles/{expertProfileId}/reject: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/expert/profiles/{expertProfileId}/trust: hasAnyRole('SYSTEM_ADMIN', 'CONTENT_ADMIN'); GET /api/v1/expert/review-cases: hasRole('SYSTEM_ADMIN'); GET /api/v1/expert/review-cases/{expertProfileId}: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Backend verification state, not UI state, grants expert eligibility. Sensitive evidence access is purpose-bound and review decisions are audited. |  |  |
| **Implemented Entry Points** | Web `/admin/experts*` and `/admin/expert-verification-queue` |  |  |
| **Implemented Contracts** | GET `/api/v1/expert/admin/profiles`; GET `/api/v1/expert/review-cases`; GET `/api/v1/expert/review-cases/{expertProfileId}`; GET `/api/v1/expert/identity/pending`; GET `/api/v1/expert/identity/files/{fileId}/url`; PUT `/api/v1/expert/identity/{attemptId}/review`; GET `/api/v1/expert/credentials/pending`; GET `/api/v1/expert/credentials/{credentialId}`; GET `/api/v1/expert/credentials/{credentialId}/preview`; GET `/api/v1/expert/credentials/{credentialId}/file`; PUT `/api/v1/expert/credentials/{credentialId}/review`; POST `/api/v1/expert/profiles/{expertProfileId}/approve`; POST `/api/v1/expert/profiles/{expertProfileId}/reject`; PATCH `/api/v1/expert/profiles/{expertProfileId}/trust`; PATCH `/api/v1/expert/profiles/{expertProfileId}/expert-type` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertIdentityVerificationController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/ExpertCredentialController.java`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/registry/RegistryMatcherTest.java`; `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 73: UC-AD-06 Verify Experts and Credentials Use Case Specification**


#### UC-AD-07 Oversee Consultation Calls and Recordings

| UC ID and Name | UC-AD-07 Oversee Consultation Calls and Recordings |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend / Object Storage |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/consultation-calls` to achieve the stated outcome. |  |  |
| **Description** | Inspect administrative consultation-call records, obtain an authorized recording URL, and delete a recording when retention policy permits. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/consultation-calls: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); GET /api/v1/admin/consultation-calls/{callId}: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); DELETE /api/v1/admin/consultation-calls/{callId}/recording: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); GET /api/v1/admin/consultation-calls/{callId}/recording-url: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load call oversight records and open a detail. |  |  |
| **Postconditions** | POST-1. Delete an eligible recording and verify metadata/retention result. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load call oversight records and open a detail. <br> 2. Request an authorized recording URL when present. <br> 3. Delete an eligible recording and verify metadata/retention result. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-07`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/consultation-calls: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); GET /api/v1/admin/consultation-calls/{callId}: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); DELETE /api/v1/admin/consultation-calls/{callId}/recording: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'); GET /api/v1/admin/consultation-calls/{callId}/recording-url: hasAnyRole('SYSTEM_ADMIN', 'ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only authorized admins may access recordings. Recording consent, purpose-bound URLs, retention, deletion, and audit are server authoritative. |  |  |
| **Implemented Entry Points** | Web `/admin/consultation-calls` |  |  |
| **Implemented Contracts** | GET/DELETE `/api/v1/admin/consultation-calls/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/directchat/controller/AdminConsultationCallController.java`; `05_Development/CareBridgeWebApp/src/features/consultationManagement/pages/ConsultationCallListPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/controller/AdminConsultationCallControllerSecurityTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/directchat/service/impl/AdminConsultationCallServiceImplTest.java`; `05_Development/CareBridgeWebApp/src/features/consultationManagement/pages/ConsultationCallListPage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 74: UC-AD-07 Oversee Consultation Calls and Recordings Use Case Specification**


#### UC-AD-08 Author and Version Articles and FAQs

| UC ID and Name | UC-AD-08 Author and Version Articles and FAQs |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend / File Storage |
| **Primary Actor** | Content Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/content/articles*`, `/content/faq*`, `/content/list`, `/content/:id*` to achieve the stated outcome. |  |  |
| **Description** | Create, edit, version, preview, list, tag with the current recommendation catalogue, and manage supported draft lifecycle of verified articles and FAQs. |  |  |
| **Preconditions** | PRE-1. Content Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/content: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/content: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/checklists: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/content/import-batch: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/recommendation-tags: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); GET /api/v1/admin/content/{id}: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); PUT /api/v1/admin/content/{id}: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/{id}/versions: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Create or load an article/FAQ draft and current recommendation tags. |  |  |
| **Postconditions** | POST-1. Save/submit/preview according to the authoring lifecycle. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load an article/FAQ draft and current recommendation tags. <br> 2. Edit sanitized rich text, metadata, media, and version state. <br> 3. Save/submit/preview according to the authoring lifecycle. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-08`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/content: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/content: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/checklists: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/content/import-batch: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/recommendation-tags: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); GET /api/v1/admin/content/{id}: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); PUT /api/v1/admin/content/{id}: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/content/{id}/versions: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Draft/version/publication state is server authoritative. Rich text sanitization and image orphan cleanup follow current policies. |  |  |
| **Implemented Entry Points** | Web `/content/articles*`, `/content/faq*`, `/content/list`, `/content/:id*` |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/admin/content`; GET/PUT `/api/v1/admin/content/{id}`; GET `/api/v1/admin/content/{id}/versions`; POST `/api/v1/admin/content/import-batch`; GET `/api/v1/admin/content/checklists`; GET `/api/v1/admin/content/recommendation-tags` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/recommendation/controller/RecommendationAdminController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentListPage.tsx`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditContentPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminContentControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UpdateContentIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/policy/HtmlContentSanitizerTest.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/components/RichTextEditor.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 75: UC-AD-08 Author and Version Articles and FAQs Use Case Specification**


#### UC-AD-09 Manage Community Taxonomy

| UC ID and Name | UC-AD-09 Manage Community Taxonomy |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Content Admin / Authorized Moderator | **Confidence** | High |
| **Trigger** | The actor enters Web `/content/topics` to achieve the stated outcome. |  |  |
| **Description** | Create, edit, organize, hide/show, and delete eligible community categories/topics/tags while preserving hierarchy and slug invariants. |  |  |
| **Preconditions** | PRE-1. Content Admin / Authorized Moderator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/community/topics: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/community/topics: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'); DELETE /api/v1/community/topics/{id}: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'); PATCH /api/v1/community/topics/{id}: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load the current taxonomy tree. |  |  |
| **Postconditions** | POST-1. Delete only an entity without prohibited dependents and refresh the tree. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the current taxonomy tree. <br> 2. Create/update/reparent or change visibility using an eligible type/parent combination. <br> 3. Delete only an entity without prohibited dependents and refresh the tree. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-09`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/community/topics: No @PreAuthorize on handler/class; effective access comes from the security chain; POST /api/v1/community/topics: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'); DELETE /api/v1/community/topics/{id}: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'); PATCH /api/v1/community/topics/{id}: hasAnyRole('MODERATOR', 'CONTENT_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Use the hand-authored `04_Implement/CommunityTopicManagement` TDS/Test-Spec as the canonical detailed baseline for this UC. Hierarchy, type, parent, slug uniqueness, dependent-question/follow, visibility, and RBAC rules are server authoritative. |  |  |
| **Implemented Entry Points** | Web `/content/topics` |  |  |
| **Implemented Contracts** | GET/POST `/api/v1/community/topics`; PATCH/DELETE `/api/v1/community/topics/{id}` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/controller/CommunityTopicController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ManageTopicsPage.tsx`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/service/CommunityTopicServiceImpl.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/CommunityTopicControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/CommunityTopicServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/CommunityTopicIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/community/util/SlugGeneratorTest.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/topicTree.test.ts` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 76: UC-AD-09 Manage Community Taxonomy Use Case Specification**


#### UC-AD-10 Author Checklist Templates

| UC ID and Name | UC-AD-10 Author Checklist Templates |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Content Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/content/checklists*` to achieve the stated outcome. |  |  |
| **Description** | Create, edit, clone, import, version, and archive checklist templates before administrative approval. |  |  |
| **Preconditions** | PRE-1. Content Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/checklist-templates: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/import-batch: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/checklist-templates/{id}: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); PUT /api/v1/admin/checklist-templates/{id}: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/{id}/archive: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/{id}/clone: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/checklist-templates/{id}/versions: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone: hasRole('CONTENT_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Create or load a checklist template/version. |  |  |
| **Postconditions** | POST-1. Save/submit/archive using an allowed author lifecycle action. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load a checklist template/version. <br> 2. Edit tasks/rules or clone/import validated content. <br> 3. Save/submit/archive using an allowed author lifecycle action. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-10`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/checklist-templates: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/import-batch: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/checklist-templates/{id}: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); PUT /api/v1/admin/checklist-templates/{id}: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/{id}/archive: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/checklist-templates/{id}/clone: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/checklist-templates/{id}/versions: hasAnyRole('CONTENT_ADMIN','SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/clone: hasRole('CONTENT_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Content Admin authors; approval/activation is a separate System Admin UC. Import/version/template validation is server authoritative. |  |  |
| **Implemented Entry Points** | Web `/content/checklists*` |  |  |
| **Implemented Contracts** | Checklist-template CRUD/clone/import/archive endpoints under `/api/v1/admin/checklist-templates/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminChecklistTemplateController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistListPage.tsx`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistFormPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/AdminChecklistTemplateServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/ChecklistImportControllerTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 77: UC-AD-10 Author Checklist Templates Use Case Specification**


#### UC-AD-11 Review, Approve, and Activate Checklist Versions

| UC ID and Name | UC-AD-11 Review, Approve, and Activate Checklist Versions |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web checklist review route and approval queue to achieve the stated outcome. |  |  |
| **Description** | Review submitted checklist versions, record an approval decision, and activate an eligible approved version for distribution. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/checklist-templates/{id}/decision: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/activate: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/approve: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/review: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load submitted checklist versions and open one review. |  |  |
| **Postconditions** | POST-1. Activate an eligible approved version and verify distribution/audit state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load submitted checklist versions and open one review. <br> 2. Approve/reject with the supported decision contract. <br> 3. Activate an eligible approved version and verify distribution/audit state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-11`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/checklist-templates/{id}/decision: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/activate: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/approve: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/review: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | System Admin approval is distinct from Content Admin authoring. Version transition and distribution idempotency are server authoritative. |  |  |
| **Implemented Entry Points** | Web checklist review route and approval queue |  |  |
| **Implemented Contracts** | POST `/api/v1/admin/checklist-templates/{id}/decision`; POST `/api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/review`; POST `/api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/approve`; POST `/api/v1/admin/checklist-templates/{lineageId}/versions/{versionId}/activate` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ChecklistTemplateApprovalController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ChecklistDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/ChecklistTemplateApprovalServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/checklist/distribution/ChecklistApprovalDistributionAuditContractTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 78: UC-AD-11 Review, Approve, and Activate Checklist Versions Use Case Specification**


#### UC-AD-12 Manage Exercise Catalogue

| UC ID and Name | UC-AD-12 Manage Exercise Catalogue |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend / File Storage |
| **Primary Actor** | Content Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/content/exercises*` to achieve the stated outcome. |  |  |
| **Description** | Create, edit, preview, publish-state manage, and archive pregnancy exercise catalogue entries. |  |  |
| **Preconditions** | PRE-1. Content Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/exercises: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/exercises: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/exercises/{exerciseId}: hasRole('CONTENT_ADMIN'); PUT /api/v1/admin/exercises/{exerciseId}: hasRole('CONTENT_ADMIN'); PATCH /api/v1/admin/exercises/{exerciseId}/activate: hasRole('CONTENT_ADMIN'); PATCH /api/v1/admin/exercises/{exerciseId}/disable: hasRole('CONTENT_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Create or load an exercise entry. |  |  |
| **Postconditions** | POST-1. Save and apply an eligible catalogue lifecycle action. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Create or load an exercise entry. <br> 2. Edit validated instructions/media/stage metadata. <br> 3. Save and apply an eligible catalogue lifecycle action. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-12`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/exercises: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/exercises: hasRole('CONTENT_ADMIN'); GET /api/v1/admin/exercises/{exerciseId}: hasRole('CONTENT_ADMIN'); PUT /api/v1/admin/exercises/{exerciseId}: hasRole('CONTENT_ADMIN'); PATCH /api/v1/admin/exercises/{exerciseId}/activate: hasRole('CONTENT_ADMIN'); PATCH /api/v1/admin/exercises/{exerciseId}/disable: hasRole('CONTENT_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Content Admin owns catalogue authoring; consumer visibility follows server lifecycle. Posture-analysis configuration is a separate System Admin lifecycle. |  |  |
| **Implemented Entry Points** | Web `/content/exercises*` |  |  |
| **Implemented Contracts** | Admin exercise endpoints under `/api/v1/admin/exercises/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminExerciseController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/PregnancyExerciseListPage.tsx`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditPregnancyExercisePage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/AdminExerciseControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/AdminExerciseServiceTest.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/CreatePregnancyExercisePage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 79: UC-AD-12 Manage Exercise Catalogue Use Case Specification**


#### UC-AD-13 Manage Posture Analysis Configuration

| UC ID and Name | UC-AD-13 Manage Posture Analysis Configuration |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/posture-configs*` to achieve the stated outcome. |  |  |
| **Description** | Create, version, validate, activate, and retire posture-analysis configuration associated with exercises. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/posture-configs: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/posture-configs/{exerciseId}: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/posture-configs/{exerciseId}/versions: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load an exercise's posture configurations. |  |  |
| **Postconditions** | POST-1. Activate or retire an eligible version through the guarded lifecycle. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load an exercise's posture configurations. <br> 2. Create/edit a validated version. <br> 3. Activate or retire an eligible version through the guarded lifecycle. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-13`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/posture-configs: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/posture-configs/{exerciseId}: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/posture-configs/{exerciseId}/versions: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/posture-configs/{postureConfigId}/activate: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only System Admin may mutate posture configuration. Version uniqueness, activation exclusivity, compatibility, and lifecycle state are server authoritative. |  |  |
| **Implemented Entry Points** | Web `/admin/posture-configs*` |  |  |
| **Implemented Contracts** | Admin posture-config endpoints under `/api/v1/admin/posture-configs/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/exercise/controller/AdminPostureConfigController.java`; `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/PostureConfigListPage.tsx`; `05_Development/CareBridgeWebApp/src/features/postureConfiguration/pages/EditPostureConfigPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/AdminPostureConfigControllerSecurityTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigLifecycleIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/exercise/PostureConfigServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 80: UC-AD-13 Manage Posture Analysis Configuration Use Case Specification**


#### UC-AD-14 Approve or Reject Submitted Content

| UC ID and Name | UC-AD-14 Approve or Reject Submitted Content |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/content-approval-queue`, `/admin/content-review/:id` to achieve the stated outcome. |  |  |
| **Description** | Review a submitted content version and record an approval or rejection decision. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/content/{id}/decision: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load the submitted-content queue and open one version. |  |  |
| **Postconditions** | POST-1. Approve or reject with a recorded decision. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the submitted-content queue and open one version. <br> 2. Review the current sanitized content/version metadata. <br> 3. Approve or reject with a recorded decision. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-14`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/content/{id}/decision: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only an eligible submitted version can be decided. Approval is auditable and controls later consumer visibility according to lifecycle. |  |  |
| **Implemented Entry Points** | Web `/admin/content-approval-queue`, `/admin/content-review/:id` |  |  |
| **Implemented Contracts** | POST `/api/v1/admin/content/{id}/decision` and supporting review endpoints |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentApprovalController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentApprovalQueuePage.tsx`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/ContentApprovalIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/security/ContentApprovalControllerSecurityTest.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentApprovalQueuePage.test.tsx` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 81: UC-AD-14 Approve or Reject Submitted Content Use Case Specification**


#### UC-AD-15 Unpublish or Archive Content

| UC ID and Name | UC-AD-15 Unpublish or Archive Content |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Content Admin where allowed | **Confidence** | High |
| **Trigger** | The actor enters Web `/content/:id/unpublish` and content detail actions to achieve the stated outcome. |  |  |
| **Description** | Remove eligible published content from consumer visibility or archive it through the supported remediation lifecycle. |  |  |
| **Preconditions** | PRE-1. Content Admin where allowed can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/content/{id}/archive: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/content/{id}/unpublish: hasRole('CONTENT_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Open an eligible content/version detail. |  |  |
| **Postconditions** | POST-1. Apply the transition and verify consumer visibility/state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Open an eligible content/version detail. <br> 2. Confirm unpublish or archive with required context. <br> 3. Apply the transition and verify consumer visibility/state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-15`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/content/{id}/archive: hasRole('CONTENT_ADMIN'); POST /api/v1/admin/content/{id}/unpublish: hasRole('CONTENT_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Unpublish/archive permissions and state transitions are server authoritative. This remediation action is distinct from System Admin approval/rejection. |  |  |
| **Implemented Entry Points** | Web `/content/:id/unpublish` and content detail actions |  |  |
| **Implemented Contracts** | POST `/api/v1/admin/content/{id}/unpublish`; POST `/api/v1/admin/content/{id}/archive` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentUnpublishController.java`; `05_Development/CareBridgeWebApp/src/features/contentManagement/pages/ContentDetailPage.tsx`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/AdminContentController.java` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/UnpublishContentIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/UnpublishContentControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/content/HideContentServiceImplTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 82: UC-AD-15 Unpublish or Archive Content Use Case Specification**


#### UC-AD-16 Moderate Pending and Visible Community Content

| UC ID and Name | UC-AD-16 Moderate Pending and Visible Community Content |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Moderator | **Confidence** | High |
| **Trigger** | The actor enters Web moderator dashboard, `/moderator/pending-content*`, and `/moderator/community-content` to achieve the stated outcome. |  |  |
| **Description** | Review the community moderation dashboard and pending/visible content, inspect action history, apply an eligible moderation action, and undo it when policy allows. |  |  |
| **Preconditions** | PRE-1. Moderator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/moderation/actions: hasRole('MODERATOR'); POST /api/v1/admin/moderation/actions/{actionId}/undo: hasRole('MODERATOR'); GET /api/v1/admin/moderation/community-content: hasRole('MODERATOR'); GET /api/v1/admin/moderation/content/{targetType}/{targetId}: hasRole('MODERATOR'); GET /api/v1/admin/moderation/history: hasRole('MODERATOR'); GET /api/v1/admin/moderation/pending-content: hasRole('MODERATOR'); GET /api/v1/moderator/community/dashboard: hasRole('MODERATOR'). <br> PRE-3. The resource is in a state eligible for: Load dashboard/pending/visible content and open one item. |  |  |
| **Postconditions** | POST-1. Undo an eligible action and verify restored state/audit history. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load dashboard/pending/visible content and open one item. <br> 2. Review context/history and apply a supported moderation action. <br> 3. Undo an eligible action and verify restored state/audit history. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-16`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/moderation/actions: hasRole('MODERATOR'); POST /api/v1/admin/moderation/actions/{actionId}/undo: hasRole('MODERATOR'); GET /api/v1/admin/moderation/community-content: hasRole('MODERATOR'); GET /api/v1/admin/moderation/content/{targetType}/{targetId}: hasRole('MODERATOR'); GET /api/v1/admin/moderation/history: hasRole('MODERATOR'); GET /api/v1/admin/moderation/pending-content: hasRole('MODERATOR'); GET /api/v1/moderator/community/dashboard: hasRole('MODERATOR'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Backend endpoints accept Moderator according to current security policy; Web route visibility is not authorization. Moderation transitions and undo eligibility are server authoritative. |  |  |
| **Implemented Entry Points** | Web moderator dashboard, `/moderator/pending-content*`, and `/moderator/community-content` |  |  |
| **Implemented Contracts** | GET `/api/v1/moderator/community/dashboard`; GET `/api/v1/admin/moderation/pending-content`; GET `/api/v1/admin/moderation/community-content`; GET `/api/v1/admin/moderation/content/{targetType}/{targetId}`; POST `/api/v1/admin/moderation/actions`; GET `/api/v1/admin/moderation/history`; POST `/api/v1/admin/moderation/actions/{actionId}/undo` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/CommunityDashboardController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/PendingContentQueuePage.tsx`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/CommunityContentMonitorPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ModerationQueueIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ModerationContentDetailIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/UndoModerationActionIntegrationTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 83: UC-AD-16 Moderate Pending and Visible Community Content Use Case Specification**


#### UC-AD-17 Claim and Resolve User Reports

| UC ID and Name | UC-AD-17 Claim and Resolve User Reports |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Moderator | **Confidence** | High |
| **Trigger** | The actor enters Web `/moderator/reports*` to achieve the stated outcome. |  |  |
| **Description** | Claim/release a report case, inspect related context and advisory AI assessment, and resolve it through the report workflow. |  |  |
| **Preconditions** | PRE-1. Moderator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/moderation/assessments/{assessmentId}/feedback: hasRole('MODERATOR'); GET /api/v1/admin/moderation/queue: hasRole('MODERATOR'); GET /api/v1/admin/moderation/reports/{reportId}/assessment: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/claim: hasRole('MODERATOR'); GET /api/v1/admin/moderation/reports/{reportId}/related: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/release: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/resolve: hasRole('MODERATOR'). <br> PRE-3. The resource is in a state eligible for: Load the report queue and claim an eligible case. |  |  |
| **Postconditions** | POST-1. Resolve or release the case through the guarded state transition. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load the report queue and claim an eligible case. <br> 2. Review target/account history, related reports, and advisory assessment. <br> 3. Resolve or release the case through the guarded state transition. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-17`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/moderation/assessments/{assessmentId}/feedback: hasRole('MODERATOR'); GET /api/v1/admin/moderation/queue: hasRole('MODERATOR'); GET /api/v1/admin/moderation/reports/{reportId}/assessment: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/claim: hasRole('MODERATOR'); GET /api/v1/admin/moderation/reports/{reportId}/related: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/release: hasRole('MODERATOR'); POST /api/v1/admin/moderation/reports/{reportId}/resolve: hasRole('MODERATOR'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Claim/state ownership and resolve actions are audited. AI assessment is advisory and never directly punishes an account. |  |  |
| **Implemented Entry Points** | Web `/moderator/reports*` |  |  |
| **Implemented Contracts** | GET `/api/v1/admin/moderation/queue`; POST `/api/v1/admin/moderation/reports/{reportId}/claim`; POST `/api/v1/admin/moderation/reports/{reportId}/release`; GET `/api/v1/admin/moderation/reports/{reportId}/related`; POST `/api/v1/admin/moderation/reports/{reportId}/resolve`; GET `/api/v1/admin/moderation/reports/{reportId}/assessment`; POST `/api/v1/admin/moderation/assessments/{assessmentId}/feedback` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationModeratorController.java`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/ReportsQueuePage.tsx`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/ContentReportDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ClaimReportWorkflowTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ResolveReportControllerTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/ResolveReportServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiAssessmentModeratorServiceTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 84: UC-AD-17 Claim and Resolve User Reports Use Case Specification**


#### UC-AD-18 Manage Account Violations

| UC ID and Name | UC-AD-18 Manage Account Violations |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend |
| **Primary Actor** | Moderator | **Confidence** | High |
| **Trigger** | The actor enters Web `/moderator/violations*` to achieve the stated outcome. |  |  |
| **Description** | Inspect account violation history, apply an eligible warn/suspend action, and undo it while policy permits. |  |  |
| **Preconditions** | PRE-1. Moderator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: POST /api/v1/admin/moderation/account-actions: hasRole('MODERATOR'); GET /api/v1/admin/moderation/account-history: hasRole('MODERATOR'); GET /api/v1/admin/moderation/account-history/{targetUserId}: hasRole('MODERATOR'); POST /api/v1/admin/moderation/actions/{actionId}/undo: hasRole('MODERATOR'). <br> PRE-3. The resource is in a state eligible for: Search/open an account violation case and history. |  |  |
| **Postconditions** | POST-1. Undo an eligible action and verify account/access state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Search/open an account violation case and history. <br> 2. Apply an eligible warning/suspension action. <br> 3. Undo an eligible action and verify account/access state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-18`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: POST /api/v1/admin/moderation/account-actions: hasRole('MODERATOR'); GET /api/v1/admin/moderation/account-history: hasRole('MODERATOR'); GET /api/v1/admin/moderation/account-history/{targetUserId}: hasRole('MODERATOR'); POST /api/v1/admin/moderation/actions/{actionId}/undo: hasRole('MODERATOR'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Role, current account state, action severity, expiry, and undo eligibility are server authoritative. Every enforcement and reversal is auditable. |  |  |
| **Implemented Entry Points** | Web `/moderator/violations*` |  |  |
| **Implemented Contracts** | GET `/api/v1/admin/moderation/account-history`; GET `/api/v1/admin/moderation/account-history/{targetUserId}`; POST `/api/v1/admin/moderation/account-actions`; POST `/api/v1/admin/moderation/actions/{actionId}/undo` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ModerationController.java`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/ViolationHistoryPage.tsx`; `05_Development/CareBridgeWebApp/src/features/moderation/pages/ViolationDetailPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/WarnOrSuspendAccountServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/integration/WarnOrSuspendAccountEnforcementIntegrationTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/moderation/UndoModerationActionIntegrationTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 85: UC-AD-18 Manage Account Violations Use Case Specification**


#### UC-AD-19 Configure AI Moderation Policies

| UC ID and Name | UC-AD-19 Configure AI Moderation Policies |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Web / Backend / Gemini |
| **Primary Actor** | System Admin | **Confidence** | High |
| **Trigger** | The actor enters Web `/admin/safety-rules` to achieve the stated outcome. |  |  |
| **Description** | Manage AI moderation policy versions/status, test policy behavior, and request supported rescans without allowing the model to enforce penalties directly. |  |  |
| **Preconditions** | PRE-1. System Admin can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/admin/ai-moderation/policies: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/ai-moderation/policies: hasRole('SYSTEM_ADMIN'); PUT /api/v1/admin/ai-moderation/policies/{id}: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/ai-moderation/policies/{id}/status: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/ai-moderation/rescan: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/ai-moderation/status: hasAnyRole('SYSTEM_ADMIN', 'MODERATOR'); POST /api/v1/admin/ai-moderation/test: hasRole('SYSTEM_ADMIN'). <br> PRE-3. The resource is in a state eligible for: Load current AI moderation policy/status. |  |  |
| **Postconditions** | POST-1. Activate or request a bounded rescan and inspect the result. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Load current AI moderation policy/status. <br> 2. Create/update/test an eligible policy version. <br> 3. Activate or request a bounded rescan and inspect the result. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-19`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/admin/ai-moderation/policies: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/ai-moderation/policies: hasRole('SYSTEM_ADMIN'); PUT /api/v1/admin/ai-moderation/policies/{id}: hasRole('SYSTEM_ADMIN'); PATCH /api/v1/admin/ai-moderation/policies/{id}/status: hasRole('SYSTEM_ADMIN'); POST /api/v1/admin/ai-moderation/rescan: hasRole('SYSTEM_ADMIN'); GET /api/v1/admin/ai-moderation/status: hasAnyRole('SYSTEM_ADMIN', 'MODERATOR'); POST /api/v1/admin/ai-moderation/test: hasRole('SYSTEM_ADMIN'). <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only System Admin manages policies. Deterministic server policy decides cases; Gemini output is advisory signal and medical symptom text is not automatically a violation. |  |  |
| **Implemented Entry Points** | Web `/admin/safety-rules` |  |  |
| **Implemented Contracts** | Policy/status/test/rescan endpoints under `/api/v1/admin/ai-moderation/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/controller/AiModerationAdminController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/aimoderation/service/AiPolicyServiceImpl.java`; `05_Development/CareBridgeWebApp/src/features/aiRuleManagement/pages/SafetyRuleManagementPage.tsx` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationAdminControllerSecurityTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiPolicyServiceImplTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiModerationDecisionPolicyTest.java`; `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/aimoderation/AiContentScanWorkerTest.java` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 86: UC-AD-19 Configure AI Moderation Policies Use Case Specification**


#### UC-AD-20 Manage AI Knowledge Base

| UC ID and Name | UC-AD-20 Manage AI Knowledge Base |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Python AI Service / FastAPI Swagger / Database |
| **Primary Actor** | Authorized Technical Operator | **Confidence** | High |
| **Trigger** | The actor enters FastAPI Swagger `/docs`; no dedicated Web Admin route to achieve the stated outcome. |  |  |
| **Description** | Inspect knowledge/chunks, upload and ingest supported documents, synchronize/rebuild eligible sources, and delete obsolete knowledge. |  |  |
| **Preconditions** | PRE-1. Authorized Technical Operator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: DELETE /api/v1/documents/by-title: Internal API key via `verify_internal_api_key` dependency; DELETE /api/v1/documents/clear-all: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/files: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/ingest-text: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/list: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/search-vector: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/stats: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/sync-directory: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/upload: Internal API key via `verify_internal_api_key` dependency. <br> PRE-3. The resource is in a state eligible for: Authenticate with the configured internal operator key. |  |  |
| **Postconditions** | POST-1. Delete an eligible source and verify future retrieval state. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Authenticate with the configured internal operator key. <br> 2. Inspect/upload/ingest/synchronize supported knowledge sources. <br> 3. Delete an eligible source and verify future retrieval state. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-20`; it must preserve that handler's exact DTO/status contract. <br> AF3. The documented current limitation remains Partial and must not be presented as success: Current operation is API/Swagger-based rather than a role-authenticated Web administration page. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: DELETE /api/v1/documents/by-title: Internal API key via `verify_internal_api_key` dependency; DELETE /api/v1/documents/clear-all: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/files: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/ingest-text: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/list: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/search-vector: Internal API key via `verify_internal_api_key` dependency; GET /api/v1/documents/stats: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/sync-directory: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/documents/upload: Internal API key via `verify_internal_api_key` dependency. <br> E3. Explicit handler failures are `500`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | All operations require the configured internal API key. File type/size/name validation and curated source metadata are required. Deleting knowledge changes future retrieval but does not prove generated answers are error-free. |  |  |
| **Implemented Entry Points** | FastAPI Swagger `/docs`; no dedicated Web Admin route |  |  |
| **Implemented Contracts** | Python document/knowledge endpoints under `/api/v1/documents/**` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAITriageService/app/api/v1/documents.py`; `05_Development/CareBridgeAITriageService/app/services/ingestion_service.py`; `05_Development/CareBridgeAITriageService/scripts/ingest_documents.py` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAITriageService/tests/test_ingestion_and_chunker.py`; `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py` |  |  |
| **Known Gaps / Exclusions** | Current operation is API/Swagger-based rather than a role-authenticated Web administration page. |  |  |

**Table 87: UC-AD-20 Manage AI Knowledge Base Use Case Specification**


#### UC-AD-21 Run AI Diagnostic and Clinical Simulators

| UC ID and Name | UC-AD-21 Run AI Diagnostic and Clinical Simulators |  |  |
| :--- | :--- | :--- | :--- |
| **Status** | Draft — code-first baseline | **Date Revised** | 2026-08-23 |
| **Feature / Group** | Administration and Operations | **Platform** | Python AI Service / FastAPI Swagger |
| **Primary Actor** | Authorized Technical Operator | **Confidence** | High |
| **Trigger** | The actor enters FastAPI Swagger `/docs`; no dedicated Web Admin route to achieve the stated outcome. |  |  |
| **Description** | Test prompt/model configuration and run deterministic metric simulation to inspect actual versus expected screening behavior. |  |  |
| **Preconditions** | PRE-1. Authorized Technical Operator can reach one implemented entry point. <br> PRE-2. Current handler/composition authorization applies: GET /api/v1/chat/models: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/chat/test-prompt: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/metrics/simulate-batch: Internal API key via `verify_internal_api_key` dependency. <br> PRE-3. The resource is in a state eligible for: Authenticate with the configured internal operator key. |  |  |
| **Postconditions** | POST-1. Review actual versus expected results without promoting them to production guarantees. <br> POST-2. The response and canonical server state/projection match the exact owning contract in the paired TDS. <br> POST-3. No audit, notification, event, file, or provider side effect is claimed unless the paired TDS cites its current publisher/adapter. |  |  |
| **Normal Flow** | 1. Authenticate with the configured internal operator key. <br> 2. Inspect model configuration or submit a bounded diagnostic prompt/simulation batch. <br> 3. Review actual versus expected results without promoting them to production guarantees. |  |  |
| **Alternative Flows** | AF1. The actor uses another listed in-scope entry point; the same owning contract and authorization remain authoritative. <br> AF2. The flow takes a different listed operation/handler path within `UC-AD-21`; it must preserve that handler's exact DTO/status contract. <br> AF3. No additional alternative behavior is claimed beyond the listed entry points, operations, and exact contracts. |  |  |
| **Exceptions** | E1. A request that violates an extracted DTO/Pydantic validator follows the exact validation oracle in the paired Test-Spec and performs no unintended write. <br> E2. A request outside the current authorization boundary fails closed: GET /api/v1/chat/models: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/chat/test-prompt: Internal API key via `verify_internal_api_key` dependency; POST /api/v1/metrics/simulate-batch: Internal API key via `verify_internal_api_key` dependency. <br> E3. Explicit handler failures are `no 4xx/5xx declared in selected handler syntax; paired TDS cites framework/advice boundaries`; service/advice-only codes are not invented and remain governed by the cited implementation oracle. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Diagnostic endpoints are operational tools, not consumer clinical flows. Historical counts, latency, uptime, or accuracy are not current requirements unless rerun and dated. |  |  |
| **Implemented Entry Points** | FastAPI Swagger `/docs`; no dedicated Web Admin route |  |  |
| **Implemented Contracts** | POST `/api/v1/chat/test-prompt`; GET `/api/v1/chat/models`; POST `/api/v1/metrics/simulate-batch` |  |  |
| **Implementation Evidence** | `05_Development/CareBridgeAITriageService/app/api/v1/chat.py`; `05_Development/CareBridgeAITriageService/app/api/v1/metrics.py`; `05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py` |  |  |
| **Existing Test Evidence** | `05_Development/CareBridgeAITriageService/tests/test_api_endpoints.py`; `05_Development/CareBridgeAITriageService/tests/test_metrics_screening.py` |  |  |
| **Known Gaps / Exclusions** | Not applicable — no manifest-level exclusion is recorded; only the entry points and contracts listed in this UC are claimed complete. |  |  |

**Table 88: UC-AD-21 Run AI Diagnostic and Clinical Simulators Use Case Specification**


### 3.12 Partial / API-Only / Unreachable Capabilities

These items are code evidence, not completed actor-goal use cases:

| Item | Current disposition | Evidence |
| --- | --- | --- |
| Notification preferences self-service UI | Partial — backend GET/PUT exists, but Mobile profile navigation is TODO and Web shortcuts are static. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationPreferenceController.java`; `05_Development/CareBridgeMobileApp/lib/features/auth/screens/account_profile_screen.dart`; `05_Development/CareBridgeWebApp/src/features/auth/pages/AccountProfilePage.tsx` |
| Health summary build/share | API-only — `/api/v1/health-summaries/**` has no current Mobile/Web consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/controller/HealthSummaryController.java` |
| Expense tracking | API-only — `/api/v1/expenses/**` has backend/tests but no current client consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/ExpenseController.java` |
| Baby care-overview and care-timeline projections | API-only — both routes have no client consumer; current hubs compose underlying resources. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyCareOverviewController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/BabyCareTimelineController.java` |
| Baby appointment-preparation summary | API-only — no current client consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/carejourney/controller/AppointmentPreparationController.java` |
| Vaccination completion and postponement operations | API/service-only — completion has a Mobile service method but no reachable UI caller, and postponement has no current client caller; neither is promoted into the completed vaccination UCs. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/vaccination/controller/VaccinationController.java`; `05_Development/CareBridgeMobileApp/lib/features/healthRecords/services/vaccination_service.dart` |
| Care-facility verification | API-only/Partial — pending/verify endpoints exist but no dedicated Web route; do not claim a completed portal UC. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/map/controller/AdminCareFacilityController.java` |
| Structured AI triage session/history/handoff | Partial backend infrastructure — no reachable Mobile intake starts or continues triage sessions; client service methods alone do not prove an actor entry flow. The Python `/internal/triage/turn` handler is a compatibility bridge for this incomplete path, not the implemented AI Nurse RAG chat. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/TriageConsentController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/HealthMemoryController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/IntakeController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/TriageSessionController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/consultation/context/controller/TriageExpertHandoffController.java`; `05_Development/CareBridgeAITriageService/app/main.py`; `05_Development/CareBridgeMobileApp/lib/features/aiTriage/services/triage_service.dart` |
| Spring triage evidence-source governance | API/Swagger-only — administrative/internal evidence-source endpoints have no current role portal and are not the Python AI knowledge-base UC. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/EvidenceSourceAdminController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/controller/InternalEvidenceSourceController.java` |
| Expert contribution points | API-only/not reachable from current clients. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ContributionPointController.java` |
| Expert location sharing and online toggle | Service/API capability without a reachable current UI action; imported service methods do not establish an actor goal. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertavailability/controller/ExpertAvailabilityController.java`; `05_Development/CareBridgeWebApp/src/features/expert/services/expertApi.ts`; `05_Development/CareBridgeMobileApp/lib/features/expert/services/expert_home_service.dart` |
| System-admin moderation escalation UI | API-only — backend escalation contract has no current Web consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/SystemAdminEscalationController.java` |
| Bulk/paged login-session administration for self-service | API/service-only — bulk revoke and paged list have no current screen caller; UC-AC-03 covers reachable list/single revoke only. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/identity/controller/SessionController.java`; `05_Development/CareBridgeMobileApp/lib/features/session/services/session_service.dart` |
| Direct notification dispatch endpoint | Supporting/internal API — clients register/deregister tokens and read notifications, but no actor screen directly invokes arbitrary `/notifications/send`. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/notification/controller/NotificationController.java` |
| Emergency location snapshots and handoff history | API-only — snapshot and handoff-history routes have no current Mobile/Web consumer; reachable navigation handoff remains UC-ES-01. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/location/controller/LocationSnapshotController.java`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/emergency/controller/EmergencyMapHandoffController.java` |
| Credential issuer catalogue | API-only — issuer lookup exists without a current client consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/controller/CredentialIssuerController.java` |
| Global cross-domain search | API-only — `/api/v1/search` has no current client consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/search/controller/SearchController.java` |
| Lifecycle checklist content projection | API-only — `/api/v1/content/lifecycle/checklists` is covered by backend tests but has no current Mobile/Web consumer. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/controller/ContentController.java` |
| Legacy verified-expert list projection | API-only — `/api/v1/expert/verified` has no current client consumer; the reachable consumer flow uses the expert directory contract. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/controller/ExpertProfileController.java` |
| Development-only manual data seed | Development supporting endpoint — `/api/manual-seed` is enabled only by the dev profile and explicit property; it is not a production actor-goal use case. | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/ManualSeedController.java` |
| Operational health and checklist E2E attestation | Operational diagnostics only — AI-triage health, posture-sidecar health, and checklist environment attestation are not actor-goal product use cases. | `05_Development/CareBridgeAITriageService/app/api/health.py`; `05_Development/MachineLearning/MediaPipe_Posture/exercise_correction_sidecar/app/main.py`; `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/checklist/operations/ChecklistE2eEnvironmentAttestationController.java` |

### 3.13 Traceability Maintenance

Every completed UC must have a paired full-form TDS and Test-Spec, except UC-AD-09 which uses the existing `04_Implement/CommunityTopicManagement` pair. Changes to routes, authorization, schema, lifecycle, or reachable clients require updating the manifest, SRS, paired specifications, and partial-capability appendix together.
