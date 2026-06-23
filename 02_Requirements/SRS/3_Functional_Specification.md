## **3\. Functional Specifications** {#3.-functional-specifications}

### **3.1 Shared** {#3.1-shared}

#### ***3.1.1 Authentication & Profile*** {#3.1.1-authentication-&-profile}

##### **3.1.1.1 Register Account** {#3.1.1.1-register-account}

| UC ID and Name | UC-01 Register Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Guest | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Guest selects or initiates Register Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates an account using email or phone, captures the initial role, and starts OTP verification. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is not required to have an authenticated session for this flow. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Guest opens Register Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 20: Register Account Use Cases Specification**

##### **3.1.1.2 Verify OTP** {#3.1.1.2-verify-otp}

| UC ID and Name | UC-02 Verify OTP |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Guest | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Guest selects or initiates Verify OTP from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Verifies the OTP code to activate an account or confirm a sensitive action. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is not required to have an authenticated session for this flow. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Guest opens Verify OTP. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 21: Verify OTP Use Cases Specification**

##### **3.1.1.3 Login** {#3.1.1.3-login}

| UC ID and Name | UC-03 Login |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The User selects or initiates Login from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Authenticates the user and routes them to the correct role dashboard. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Login. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 22: Login Use Cases Specification**

##### **3.1.1.4 Logout** {#3.1.1.4-logout}

| UC ID and Name | UC-04 Logout |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Logout from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Ends the current authenticated session and revokes the active token or session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Logout. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 23: Logout Use Cases Specification**

##### **3.1.1.5 Forgot Password** {#3.1.1.5-forgot-password}

| UC ID and Name | UC-05 Forgot Password |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Guest | **Secondary Actors** | None |
| **Trigger** | The Guest selects or initiates Forgot Password from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Requests a password reset link or code through email or phone. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is not required to have an authenticated session for this flow. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Guest opens Forgot Password. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 24: Forgot Password Use Cases Specification**

##### **3.1.1.6 Reset Password** {#3.1.1.6-reset-password}

| UC ID and Name | UC-06 Reset Password |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Guest | **Secondary Actors** | None |
| **Trigger** | The Guest selects or initiates Reset Password from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a new password after a valid reset code or link is confirmed. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is not required to have an authenticated session for this flow. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Guest opens Reset Password. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 25: Reset Password Use Cases Specification**

##### **3.1.1.7 Change Password** {#3.1.1.7-change-password}

| UC ID and Name | UC-07 Change Password |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Change Password from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Changes the password for a signed-in user after validating the current password. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Change Password. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 26: Change Password Use Cases Specification**

##### **3.1.1.8 View Account Profile** {#3.1.1.8-view-account-profile}

| UC ID and Name | UC-08 View Account Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Account Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays personal information, role, account status, and basic settings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens the View Account Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 27: View Account Profile Use Cases Specification**

##### **3.1.1.9 Update Account Profile** {#3.1.1.9-update-account-profile}

| UC ID and Name | UC-09 Update Account Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Update Account Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates non-sensitive profile information such as name, avatar, phone number, and area. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Update Account Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 28: Update Account Profile Use Cases Specification**

##### **3.1.1.10 Update Notification Preferences** {#3.1.1.10-update-notification-preferences}

| UC ID and Name | UC-10 Update Notification Preferences |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The User selects or initiates Update Notification Preferences from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Configures notification channels and categories according to role and permissions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Update Notification Preferences. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 29: Update Notification Preferences Use Cases Specification**

##### **3.1.1.11 View Notifications** {#3.1.1.11-view-notifications}

| UC ID and Name | UC-11 View Notifications |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The User selects or initiates View Notifications from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays personal notifications by time, type, and read or unread status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Notifications. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 30: View Notifications Use Cases Specification**

##### **3.1.1.12 Mark Notifications as Read** {#3.1.1.12-mark-notifications-as-read}

| UC ID and Name | UC-12 Mark Notifications as Read |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The User selects or initiates Mark Notifications as Read from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Marks one or more notifications as read. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Mark Notifications as Read. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 31: Mark Notifications as Read Use Cases Specification**

##### **3.1.1.13 Search and Filter** {#3.1.1.13-search-and-filter}

| UC ID and Name | UC-13 Search and Filter |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Search and Filter from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Searches questions, articles, experts, profiles, or permitted data with filters. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Search and Filter. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 32: Search and Filter Use Cases Specification**

##### **3.1.1.14 Report Content or Account** {#3.1.1.14-report-content-or-account}

| UC ID and Name | UC-14 Report Content or Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Report Content or Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits a report about inaccurate content, disguised advertising, harassment, or unsafe advice. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Report Content or Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 33: Report Content or Account Use Cases Specification**

##### **3.1.1.15 Deactivate Own Account** {#3.1.1.15-deactivate-own-account}

| UC ID and Name | UC-15 Deactivate Own Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Deactivate Own Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Requests account deactivation according to the data policy. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Deactivate Own Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 34: Deactivate Own Account Use Cases Specification**

##### **3.1.1.16 Manage Own Sessions** {#3.1.1.16-manage-own-sessions}

| UC ID and Name | UC-16 Manage Own Sessions |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Manage Own Sessions from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays and revokes the user's own active sessions on other devices. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Manage Own Sessions. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 35: Manage Own Sessions Use Cases Specification**

##### **3.1.1.17 Grant Data Permission** {#3.1.1.17-grant-data-permission}

| UC ID and Name | UC-17 Grant Data Permission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Grant Data Permission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Grants data access to a recipient with scope, purpose, and expiry controls. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Grant Data Permission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 36: Grant Data Permission Use Cases Specification**

##### **3.1.1.18 Revoke Data Permission** {#3.1.1.18-revoke-data-permission}

| UC ID and Name | UC-18 Revoke Data Permission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Revoke Data Permission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Revokes previously granted access for a family member or expert. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Revoke Data Permission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 37: Revoke Data Permission Use Cases Specification**

##### **3.1.1.19 View Sharing History** {#3.1.1.19-view-sharing-history}

| UC ID and Name | UC-19 View Sharing History |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Sharing History from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays granted permissions, recipients, timestamps, and status history. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Sharing History. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 38: View Sharing History Use Cases Specification**

##### **3.1.1.20 Create Community Profile** {#3.1.1.20-create-community-profile}

| UC ID and Name | UC-20 Create Community Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Create Community Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a public community display name, avatar, interest stage, and visibility options. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Create Community Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 39: Create Community Profile Use Cases Specification**

##### **3.1.1.21 Update Community Profile** {#3.1.1.21-update-community-profile}

| UC ID and Name | UC-21 Update Community Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Update Community Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates or hides public community profile information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Update Community Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 40: Update Community Profile Use Cases Specification**

##### **3.1.1.22 Delete Own Account** {#3.1.1.22-delete-own-account}

| UC ID and Name | UC-156 Delete Own Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Delete Own Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Requests account deletion and processes data according to retention rules, legal obligations, and waiting period. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Delete Own Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Authentication & Profile. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 41: Delete Own Account Use Cases Specification**

#### ***3.1.2 Supporting Services*** {#3.1.2-supporting-services}

##### **3.1.2.1 Process Payment Transaction** {#3.1.2.1-process-payment-transaction}

| UC ID and Name | UC-126 Process Payment Transaction |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | VNPay Payment Gateway | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The VNPay Payment Gateway selects or initiates Process Payment Transaction from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives transaction requests and returns success, failure, or cancellation status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The VNPay Payment Gateway opens Process Payment Transaction. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 42: Process Payment Transaction Use Cases Specification**

##### **3.1.2.2 Calculate Commission** {#3.1.2.2-calculate-commission}

| UC ID and Name | UC-127 Calculate Commission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The System Admin selects or initiates Calculate Commission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Calculates platform fee, expert revenue, and reconciliation status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Calculate Commission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 43: Calculate Commission Use Cases Specification**

##### **3.1.2.3 Send Automated Notification** {#3.1.2.3-send-automated-notification}

| UC ID and Name | UC-128 Send Automated Notification |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Firebase Cloud Messaging | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Firebase Cloud Messaging selects or initiates Send Automated Notification from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Sends push, in-app, or email notifications for reminders, consultations, reports, and emergencies. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Firebase Cloud Messaging opens Send Automated Notification. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 44: Send Automated Notification Use Cases Specification**

##### **3.1.2.4 Sync Health Device Data** {#3.1.2.4-sync-health-device-data}

| UC ID and Name | UC-130 Sync Health Device Data |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Smartwatch / Wearable Device | **Secondary Actors** | None |
| **Trigger** | The Smartwatch / Wearable Device selects or initiates Sync Health Device Data from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Synchronizes or imports user-authorized device data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Smartwatch / Wearable Device opens Sync Health Device Data. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 45: Sync Health Device Data Use Cases Specification**

##### **3.1.2.5 Extract Structured Intake Data** {#3.1.2.5-extract-structured-intake-data}

| UC ID and Name | UC-131 Extract Structured Intake Data |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Gemini AI Service | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Gemini AI Service selects or initiates Extract Structured Intake Data from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Converts free-text answers into structured data for the rule engine. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Gemini AI Service opens Extract Structured Intake Data. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 46: Extract Structured Intake Data Use Cases Specification**

##### **3.1.2.6 Generate RAG Answer** {#3.1.2.6-generate-rag-answer}

| UC ID and Name | UC-132 Generate RAG Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Gemini AI Service | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Gemini AI Service selects or initiates Generate RAG Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Answers FAQs using approved sources and avoids out-of-scope content. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Gemini AI Service opens Generate RAG Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Backend/External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 47: Generate RAG Answer Use Cases Specification**

##### **3.1.2.7 Establish Realtime Communication Session** {#3.1.2.7-establish-realtime-communication-session}

| UC ID and Name | UC-154 Establish Realtime Communication Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | ZegoCloud \+ Firebase Realtime | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The ZegoCloud \+ Firebase Realtime selects or initiates Establish Realtime Communication Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates rooms or tokens and maintains chat, voice, or video status for a valid booking. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The ZegoCloud \+ Firebase Realtime opens Establish Realtime Communication Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Backend / External; Source group: Supporting Services. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 48: Establish Realtime Communication Session Use Cases Specification**

#### ***3.1.3 MF-19 Map & Location*** {#3.1.3-mf-19-map-&-location}

##### **3.1.3.1 Calculate Distance, Route and ETA** {#3.1.3.1-calculate-distance,-route-and-eta}

| UC ID and Name | UC-129 Calculate Distance, Route and ETA |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | TrackAsia Map Service | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The TrackAsia Map Service selects or initiates Calculate Distance, Route and ETA from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Provides shared map and location capability for facilities, experts sharing location, and nearby support requests. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The TrackAsia Map Service opens Calculate Distance, Route and ETA. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Backend / External; Source group: Supporting Services \- MF-19 Map & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 49: Calculate Distance, Route and ETA Use Cases Specification**

#### ***3.1.4 Consent & Privacy*** {#3.1.4-consent-&-privacy}

##### **3.1.4.1 Manage Privacy Settings** {#3.1.4.1-manage-privacy-settings}

| UC ID and Name | UC-157 Manage Privacy Settings |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The User selects or initiates Manage Privacy Settings from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Manages visibility for community profile, health data, online status, location, and data-use preferences. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Manage Privacy Settings. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Consent & Privacy. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 50: Manage Privacy Settings Use Cases Specification**

#### ***3.1.5 Notifications*** {#3.1.5-notifications}

##### **3.1.5.1 Receive Reminder Notification** {#3.1.5.1-receive-reminder-notification}

| UC ID and Name | UC-158 Receive Reminder Notification |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The User selects or initiates Receive Reminder Notification from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives due notifications for appointments, medicine or vitamins, vaccination, checklists, or care tasks. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Receive Reminder Notification. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Notifications. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 51: Receive Reminder Notification Use Cases Specification**

##### **3.1.5.2 Receive Community Reply Notification** {#3.1.5.2-receive-community-reply-notification}

| UC ID and Name | UC-159 Receive Community Reply Notification |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The User selects or initiates Receive Community Reply Notification from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives notifications for replies, mentions, likes, or moderation outcomes on posts and questions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Receive Community Reply Notification. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Notifications. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 52: Receive Community Reply Notification Use Cases Specification**

##### **3.1.5.3 Receive Consultation Notification** {#3.1.5.3-receive-consultation-notification}

| UC ID and Name | UC-160 Receive Consultation Notification |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging, ZegoCloud Realtime Service |
| **Trigger** | The User selects or initiates Receive Consultation Notification from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives notifications for requests, confirmations, schedule changes, incoming calls, and session status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Receive Consultation Notification. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: App/Web; Source group: Shared \- Notifications. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 53: Receive Consultation Notification Use Cases Specification**

##### **3.1.5.4 Receive Emergency Alert** {#3.1.5.4-receive-emergency-alert}

| UC ID and Name | UC-161 Receive Emergency Alert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | Firebase Cloud Messaging, TrackAsia Map Service |
| **Trigger** | The Family Member selects or initiates Receive Emergency Alert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives consent-based safety or minimal location alerts from the Mother. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens Receive Emergency Alert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Shared \- Notifications. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 54: Receive Emergency Alert Use Cases Specification**

### **3.2 Web** {#3.2-web}

#### ***3.2.1 Expert Network & Consultation*** {#3.2.1-expert-network-&-consultation}

##### **3.2.1.1 Create Expert Profile** {#3.2.1.1-create-expert-profile}

| UC ID and Name | UC-87 Create Expert Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Create Expert Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a professional profile with expertise, experience, and support scope. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Create Expert Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 55: Create Expert Profile Use Cases Specification**

##### **3.2.1.2 Update Expert Profile** {#3.2.1.2-update-expert-profile}

| UC ID and Name | UC-88 Update Expert Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Update Expert Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates expert profile and public display information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Update Expert Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 56: Update Expert Profile Use Cases Specification**

##### **3.2.1.3 Upload Verification Documents** {#3.2.1.3-upload-verification-documents}

| UC ID and Name | UC-89 Upload Verification Documents |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Upload Verification Documents from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uploads degrees, certificates, or related documents for admin verification. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Upload Verification Documents. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 57: Upload Verification Documents Use Cases Specification**

##### **3.2.1.4 Configure Availability** {#3.2.1.4-configure-availability}

| UC ID and Name | UC-90 Configure Availability |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Configure Availability from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Configures available slots, online status, and supported consultation modalities. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Configure Availability. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 58: Configure Availability Use Cases Specification**

##### **3.2.1.5 View Expert Question Queue** {#3.2.1.5-view-expert-question-queue}

| UC ID and Name | UC-91 View Expert Question Queue |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates View Expert Question Queue from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays specialty-matched questions that need expert answers. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens View Expert Question Queue. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 59: View Expert Question Queue Use Cases Specification**

##### **3.2.1.6 Post Expert Answer** {#3.2.1.6-post-expert-answer}

| UC ID and Name | UC-92 Post Expert Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Post Expert Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Posts public answers with an expert badge and safe-scope boundaries. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Post Expert Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 60: Post Expert Answer Use Cases Specification**

##### **3.2.1.7 Suggest Private Consultation** {#3.2.1.7-suggest-private-consultation}

| UC ID and Name | UC-93 Suggest Private Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Verified Expert selects or initiates Suggest Private Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Suggests private consultation when deeper discussion is needed, with reason and transparent fee. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Suggest Private Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 61: Suggest Private Consultation Use Cases Specification**

##### **3.2.1.8 View Shared Health Summary** {#3.2.1.8-view-shared-health-summary}

| UC ID and Name | UC-94 View Shared Health Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates View Shared Health Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays shared summaries or records within the user-granted scope and time limit. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens View Shared Health Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 62: View Shared Health Summary Use Cases Specification**

##### **3.2.1.9 Manage Consultation Session** {#3.2.1.9-manage-consultation-session}

| UC ID and Name | UC-95 Manage Consultation Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Verified Expert selects or initiates Manage Consultation Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives bookings, joins consultation sessions, and updates participation status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Manage Consultation Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 63: Manage Consultation Session Use Cases Specification**

##### **3.2.1.10 Write Consultation Summary** {#3.2.1.10-write-consultation-summary}

| UC ID and Name | UC-96 Write Consultation Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Write Consultation Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Writes the consultation summary and safe next steps for the user to review. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Write Consultation Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 64: Write Consultation Summary Use Cases Specification**

##### **3.2.1.11 View Revenue and Commission** {#3.2.1.11-view-revenue-and-commission}

| UC ID and Name | UC-97 View Revenue and Commission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Verified Expert selects or initiates View Revenue and Commission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays completed sessions, revenue, platform commission, and reconciliation status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens View Revenue and Commission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 65: View Revenue and Commission Use Cases Specification**

##### **3.2.1.12 View Contribution Points** {#3.2.1.12-view-contribution-points}

| UC ID and Name | UC-98 View Contribution Points |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates View Contribution Points from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays contribution points and badges from answers and community activity. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens View Contribution Points. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Network & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 66: View Contribution Points Use Cases Specification**

#### ***3.2.2 Moderation, Content, Operation*** {#3.2.2-moderation,-content,-operation}

##### **3.2.2.1 View Moderation Queue** {#3.2.2.1-view-moderation-queue}

| UC ID and Name | UC-99 View Moderation Queue |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Community Moderator | **Secondary Actors** | None |
| **Trigger** | The Community Moderator selects or initiates View Moderation Queue from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays posts, answers, and reports waiting for moderation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Community Moderator opens View Moderation Queue. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 67: View Moderation Queue Use Cases Specification**

##### **3.2.2.2 Moderate Community Content** {#3.2.2.2-moderate-community-content}

| UC ID and Name | UC-100 Moderate Community Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Community Moderator | **Secondary Actors** | None |
| **Trigger** | The Community Moderator selects or initiates Moderate Community Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves, hides, locks comments, or requests edits for community content. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Community Moderator opens Moderate Community Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 68: Moderate Community Content Use Cases Specification**

##### **3.2.2.3 Resolve Report** {#3.2.2.3-resolve-report}

| UC ID and Name | UC-101 Resolve Report |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Community Moderator | **Secondary Actors** | None |
| **Trigger** | The Community Moderator selects or initiates Resolve Report from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Reviews a report and decides whether to keep, label, hide, warn, or suspend. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Community Moderator opens Resolve Report. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 69: Resolve Report Use Cases Specification**

##### **3.2.2.4 Warn or Suspend Account** {#3.2.2.4-warn-or-suspend-account}

| UC ID and Name | UC-102 Warn or Suspend Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Community Moderator | **Secondary Actors** | None |
| **Trigger** | The Community Moderator selects or initiates Warn or Suspend Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Warns, restricts posting, or suspends accounts that violate rules. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Community Moderator opens Warn or Suspend Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 70: Warn or Suspend Account Use Cases Specification**

##### **3.2.2.5 Verify Expert Profile** {#3.2.2.5-verify-expert-profile}

| UC ID and Name | UC-103 Verify Expert Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Verify Expert Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves, rejects, or requests additional expert profile and document information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Verify Expert Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 71: Verify Expert Profile Use Cases Specification**

##### **3.2.2.6 Revoke Expert Badge** {#3.2.2.6-revoke-expert-badge}

| UC ID and Name | UC-104 Revoke Expert Badge |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Revoke Expert Badge from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Temporarily locks or revokes an expert badge when rules are violated or documents expire. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Revoke Expert Badge. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 72: Revoke Expert Badge Use Cases Specification**

##### **3.2.2.7 Create Content/FAQ/Checklist** {#3.2.2.7-create-content/faq/checklist}

| UC ID and Name | UC-105 Create Content/FAQ/Checklist |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Create Content/FAQ/Checklist from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates short content, FAQs, or checklists by stage and topic. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Create Content/FAQ/Checklist. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 73: Create Content/FAQ/Checklist Use Cases Specification**

##### **3.2.2.8 Update Content/FAQ/Checklist** {#3.2.2.8-update-content/faq/checklist}

| UC ID and Name | UC-106 Update Content/FAQ/Checklist |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Update Content/FAQ/Checklist from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates content, tags, source labels, versions, or status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Update Content/FAQ/Checklist. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 74: Update Content/FAQ/Checklist Use Cases Specification**

##### **3.2.2.9 Hide or Delete Content** {#3.2.2.9-hide-or-delete-content}

| UC ID and Name | UC-107 Hide or Delete Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Hide or Delete Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Hides or soft-deletes outdated, incorrect, or reported content. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Hide or Delete Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 75: Hide or Delete Content Use Cases Specification**

##### **3.2.2.10 Approve Content Version** {#3.2.2.10-approve-content-version}

| UC ID and Name | UC-108 Approve Content Version |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Approve Content Version from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves a new content version before it is displayed to users. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Approve Content Version. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 76: Approve Content Version Use Cases Specification**

##### **3.2.2.11 Manage Community Topics** {#3.2.2.11-manage-community-topics}

| UC ID and Name | UC-109 Manage Community Topics |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Community Moderator | **Secondary Actors** | None |
| **Trigger** | The Community Moderator selects or initiates Manage Community Topics from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates, edits, or hides topics, tags, and Q\&A groups. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Community Moderator opens Manage Community Topics. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 77: Manage Community Topics Use Cases Specification**

##### **3.2.2.12 Manage AI and Red-Flag Rules** {#3.2.2.12-manage-ai-and-red-flag-rules}

| UC ID and Name | UC-110 Manage AI and Red-Flag Rules |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Manage AI and Red-Flag Rules from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Configures safety rules, dangerous keywords, green/yellow/red levels, and actions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Manage AI and Red-Flag Rules. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 78: Manage AI and Red-Flag Rules Use Cases Specification**

##### **3.2.2.13 View Community Dashboard** {#3.2.2.13-view-community-dashboard}

| UC ID and Name | UC-111 View Community Dashboard |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates View Community Dashboard from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays user, question, report, handling time, and trending-topic metrics. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens View Community Dashboard. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 79: View Community Dashboard Use Cases Specification**

##### **3.2.2.14 View Expert Dashboard** {#3.2.2.14-view-expert-dashboard}

| UC ID and Name | UC-112 View Expert Dashboard |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates View Expert Dashboard from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays expert count, answers, consultations, reports, and quality metrics. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens View Expert Dashboard. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 80: View Expert Dashboard Use Cases Specification**

##### **3.2.2.15 View Impact Report** {#3.2.2.15-view-impact-report}

| UC ID and Name | UC-113 View Impact Report |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates View Impact Report from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays aggregated and anonymized impact metrics for fundraising, CSR, or partners. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens View Impact Report. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 81: View Impact Report Use Cases Specification**

##### **3.2.2.16 Manage User Accounts** {#3.2.2.16-manage-user-accounts}

| UC ID and Name | UC-114 Manage User Accounts |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Manage User Accounts from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays, searches, filters, and manages user account status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Manage User Accounts. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 82: Manage User Accounts Use Cases Specification**

##### **3.2.2.17 Create Staff Account** {#3.2.2.17-create-staff-account}

| UC ID and Name | UC-115 Create Staff Account |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Create Staff Account from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates moderator, content admin, or admin accounts according to permissions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Create Staff Account. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 83: Create Staff Account Use Cases Specification**

##### **3.2.2.18 Update Role and Permission** {#3.2.2.18-update-role-and-permission}

| UC ID and Name | UC-116 Update Role and Permission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Update Role and Permission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Assigns roles, updates permissions, and locks or unlocks access rights. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Update Role and Permission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 84: Update Role and Permission Use Cases Specification**

##### **3.2.2.19 View Audit Logs** {#3.2.2.19-view-audit-logs}

| UC ID and Name | UC-117 View Audit Logs |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates View Audit Logs from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays audit logs for complaint review, data access review, and moderation traceability. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens View Audit Logs. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Moderation, Content, Operation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 85: View Audit Logs Use Cases Specification**

#### ***3.2.3 Partner & Sponsored Governance*** {#3.2.3-partner-&-sponsored-governance}

##### **3.2.3.1 Create Partner Profile** {#3.2.3.1-create-partner-profile}

| UC ID and Name | UC-118 Create Partner Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Partner Representative | **Secondary Actors** | None |
| **Trigger** | The Partner Representative selects or initiates Create Partner Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits partner clinic, organization, and contact information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Partner Representative opens Create Partner Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 86: Create Partner Profile Use Cases Specification**

##### **3.2.3.2 Update Partner Profile** {#3.2.3.2-update-partner-profile}

| UC ID and Name | UC-119 Update Partner Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Partner Representative | **Secondary Actors** | None |
| **Trigger** | The Partner Representative selects or initiates Update Partner Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates partner information, address, contact details, and service description. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Partner Representative opens Update Partner Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 87: Update Partner Profile Use Cases Specification**

##### **3.2.3.3 Submit Service Listing** {#3.2.3.3-submit-service-listing}

| UC ID and Name | UC-120 Submit Service Listing |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Partner Representative | **Secondary Actors** | None |
| **Trigger** | The Partner Representative selects or initiates Submit Service Listing from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits services or reference schedules for admin approval and display. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Partner Representative opens Submit Service Listing. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 88: Submit Service Listing Use Cases Specification**

##### **3.2.3.4 Submit Sponsored Content** {#3.2.3.4-submit-sponsored-content}

| UC ID and Name | UC-121 Submit Sponsored Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Partner Representative | **Secondary Actors** | None |
| **Trigger** | The Partner Representative selects or initiates Submit Sponsored Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits labeled sponsored articles, banners, or campaigns for review. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Partner Representative opens Submit Sponsored Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 89: Submit Sponsored Content Use Cases Specification**

##### **3.2.3.5 View Partner Performance** {#3.2.3.5-view-partner-performance}

| UC ID and Name | UC-122 View Partner Performance |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Partner Representative | **Secondary Actors** | None |
| **Trigger** | The Partner Representative selects or initiates View Partner Performance from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays aggregated views, referral clicks, and approved campaign performance. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Partner Representative opens View Partner Performance. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 90: View Partner Performance Use Cases Specification**

##### **3.2.3.6 Approve Partner Profile** {#3.2.3.6-approve-partner-profile}

| UC ID and Name | UC-123 Approve Partner Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Approve Partner Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves, rejects, or requests additional partner profile information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Approve Partner Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 91: Approve Partner Profile Use Cases Specification**

##### **3.2.3.7 Approve Sponsored Service/Campaign** {#3.2.3.7-approve-sponsored-service/campaign}

| UC ID and Name | UC-124 Approve Sponsored Service/Campaign |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Approve Sponsored Service/Campaign from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves partner content, applies sponsored or partner labels, and prevents unsafe medical advertising. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Approve Sponsored Service/Campaign. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 92: Approve Sponsored Service/Campaign Use Cases Specification**

##### **3.2.3.8 Remove Partner Content** {#3.2.3.8-remove-partner-content}

| UC ID and Name | UC-125 Remove Partner Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Remove Partner Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Removes service listings or campaigns when reported, expired, or policy-violating. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Remove Partner Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Partner/Admin Portal; Source group: Partner Portal \- Partner & Sponsored Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 93: Remove Partner Content Use Cases Specification**

#### ***3.2.4 Expert Governance*** {#3.2.4-expert-governance}

##### **3.2.4.1 Suspend Expert** {#3.2.4.1-suspend-expert}

| UC ID and Name | UC-172 Suspend Expert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Suspend Expert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Temporarily suspends expert privileges, consultation access, or public visibility after violations, serious complaints, or invalid documents. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Suspend Expert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Expert Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 94: Suspend Expert Use Cases Specification**

##### **3.2.4.2 Renew Expert Verification** {#3.2.4.2-renew-expert-verification}

| UC ID and Name | UC-173 Renew Expert Verification |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Renew Expert Verification from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits updated documents before verification expiry and tracks renewal results. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Renew Expert Verification. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal \- Expert Governance. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 95: Renew Expert Verification Use Cases Specification**

#### ***3.2.5 Audit & Security*** {#3.2.5-audit-&-security}

##### **3.2.5.1 Investigate Security Incident** {#3.2.5.1-investigate-security-incident}

| UC ID and Name | UC-174 Investigate Security Incident |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Investigate Security Incident from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Investigates unauthorized access, data leakage, abnormal behavior, or security complaints using audit logs and evidence. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Investigate Security Incident. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Audit & Security. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 96: Investigate Security Incident Use Cases Specification**

##### **3.2.5.2 Review Security Event** {#3.2.5.2-review-security-event}

| UC ID and Name | UC-175 Review Security Event |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The System Admin selects or initiates Review Security Event from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Reviews login alerts, permission changes, unusual file downloads, sensitive-record access, and decides next actions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Review Security Event. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Audit & Security. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 97: Review Security Event Use Cases Specification**

#### ***3.2.6 Pregnancy Exercise Management*** {#3.2.6-pregnancy-exercise-management}

##### **3.2.6.1 Manage Pregnancy Exercises** {#3.2.6.1-manage-pregnancy-exercises}

| UC ID and Name | UC-185 Manage Pregnancy Exercises |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Manage Pregnancy Exercises from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates, updates, activates, or disables exercises and configures trimester, difficulty, duration, instructions, and warnings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Manage Pregnancy Exercises. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Pregnancy Exercise Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 98: Manage Pregnancy Exercises Use Cases Specification**

##### **3.2.6.2 Manage Posture Analysis Configuration** {#3.2.6.2-manage-posture-analysis-configuration}

| UC ID and Name | UC-186 Manage Posture Analysis Configuration |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | VNPay Payment Gateway, Smartwatch/Wearable Device |
| **Trigger** | The System Admin selects or initiates Manage Posture Analysis Configuration from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Manages analysis mode, rule or model version, confidence thresholds, and feedback levels for each exercise. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Manage Posture Analysis Configuration. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Pregnancy Exercise Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 99: Manage Posture Analysis Configuration Use Cases Specification**

#### ***3.2.7 Consultation Pricing*** {#3.2.7-consultation-pricing}

##### **3.2.7.1 Set Consultation Price** {#3.2.7.1-set-consultation-price}

| UC ID and Name | UC-238 Set Consultation Price |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Verified Expert selects or initiates Set Consultation Price from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates the initial price for a consultation package within the CareBridge allowed price band. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Set Consultation Price. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal / Expert App \- Consultation Pricing. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 100: Set Consultation Price Use Cases Specification**

##### **3.2.7.2 Update Consultation Price** {#3.2.7.2-update-consultation-price}

| UC ID and Name | UC-239 Update Consultation Price |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Verified Expert selects or initiates Update Consultation Price from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a new price version for future bookings without changing locked booking prices. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Update Consultation Price. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert Portal / Expert App; Source group: Expert Portal / Expert App \- Consultation Pricing. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 101: Update Consultation Price Use Cases Specification**

#### ***3.2.8 Consultation Pricing & Commission*** {#3.2.8-consultation-pricing-&-commission}

##### **3.2.8.1 Configure Consultation Price Bands** {#3.2.8.1-configure-consultation-price-bands}

| UC ID and Name | UC-240 Configure Consultation Price Bands |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The System Admin selects or initiates Configure Consultation Price Bands from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Manages platform price limits and commission rates by consultation modality and duration. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Configure Consultation Price Bands. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Admin Web \- Consultation Pricing & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 102: Configure Consultation Price Bands Use Cases Specification**

### **3.3 Mobile** {#3.3-mobile}

#### ***3.3.1 Care Journey, Community, Health & Consultation*** {#3.3.1-care-journey,-community,-health-&-consultation}

##### **3.3.1.1 Create Mother Journey** {#3.3.1.1-create-mother-journey}

| UC ID and Name | UC-22 Create Mother Journey |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Create Mother Journey from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a journey for pre-pregnancy, pregnancy, postpartum, or baby-care tracking. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Mother Journey. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 103: Create Mother Journey Use Cases Specification**

##### **3.3.1.2 Update Mother Journey** {#3.3.1.2-update-mother-journey}

| UC ID and Name | UC-23 Update Mother Journey |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Mother Journey from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates last menstrual period, expected due date, birth date, or journey status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Mother Journey. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 104: Update Mother Journey Use Cases Specification**

##### **3.3.1.3 View Mother Journey Dashboard** {#3.3.1.3-view-mother-journey-dashboard}

| UC ID and Name | UC-24 View Mother Journey Dashboard |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates View Mother Journey Dashboard from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays pregnancy week or postpartum stage, tasks, reminders, and suggested content. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Mother Journey Dashboard. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 105: View Mother Journey Dashboard Use Cases Specification**

##### **3.3.1.4 Add Maternal Health Metric** {#3.3.1.4-add-maternal-health-metric}

| UC ID and Name | UC-25 Add Maternal Health Metric |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Mother selects or initiates Add Maternal Health Metric from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records maternal metrics such as weight, blood pressure, blood glucose, fetal movement, or symptoms. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Maternal Health Metric. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 106: Add Maternal Health Metric Use Cases Specification**

##### **3.3.1.5 Update Maternal Health Metric** {#3.3.1.5-update-maternal-health-metric}

| UC ID and Name | UC-26 Update Maternal Health Metric |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Maternal Health Metric from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Corrects an incorrectly entered maternal health metric record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Maternal Health Metric. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 107: Update Maternal Health Metric Use Cases Specification**

##### **3.3.1.6 View Maternal Health Trend** {#3.3.1.6-view-maternal-health-trend}

| UC ID and Name | UC-27 View Maternal Health Trend |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Maternal Health Trend from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays metric trends over time without producing a medical diagnosis. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Maternal Health Trend. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 108: View Maternal Health Trend Use Cases Specification**

##### **3.3.1.7 Add Postpartum Log** {#3.3.1.7-add-postpartum-log}

| UC ID and Name | UC-28 Add Postpartum Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Mother selects or initiates Add Postpartum Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records postpartum recovery notes such as bleeding, incision pain, milk, sleep, stress, or symptoms. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Postpartum Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 109: Add Postpartum Log Use Cases Specification**

##### **3.3.1.8 Create Baby Profile** {#3.3.1.8-create-baby-profile}

| UC ID and Name | UC-31 Create Baby Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Create Baby Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a baby profile with nickname, birth date, gender, and birth weight or length. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Baby Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 110: Create Baby Profile Use Cases Specification**

##### **3.3.1.9 Update Baby Profile** {#3.3.1.9-update-baby-profile}

| UC ID and Name | UC-32 Update Baby Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Baby Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates baby profile information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Baby Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 111: Update Baby Profile Use Cases Specification**

##### **3.3.1.10 Archive Baby Profile** {#3.3.1.10-archive-baby-profile}

| UC ID and Name | UC-33 Archive Baby Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Archive Baby Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Hides or archives a baby profile that is no longer actively tracked without deleting linked data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Archive Baby Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 112: Archive Baby Profile Use Cases Specification**

##### **3.3.1.11 Add Feeding Sleep Diaper Log** {#3.3.1.11-add-feeding-sleep-diaper-log}

| UC ID and Name | UC-34 Add Feeding Sleep Diaper Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Mother selects or initiates Add Feeding Sleep Diaper Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Quickly records feeding, sleep, diaper, fever, vomiting, or prescribed medicine notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Feeding Sleep Diaper Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 113: Add Feeding Sleep Diaper Log Use Cases Specification**

##### **3.3.1.12 Update Baby Daily Log** {#3.3.1.12-update-baby-daily-log}

| UC ID and Name | UC-35 Update Baby Daily Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Baby Daily Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates or deletes an incorrectly entered baby daily log record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Baby Daily Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 114: Update Baby Daily Log Use Cases Specification**

##### **3.3.1.13 View Baby Log Summary** {#3.3.1.13-view-baby-log-summary}

| UC ID and Name | UC-36 View Baby Log Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Gemini AI Service, VNPay Payment Gateway |
| **Trigger** | The Mother selects or initiates View Baby Log Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays a 24-hour or 7-day summary of feeding, sleep, diaper, and symptom logs. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Baby Log Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 115: View Baby Log Summary Use Cases Specification**

##### **3.3.1.14 Record Development Milestone** {#3.3.1.14-record-development-milestone}

| UC ID and Name | UC-37 Record Development Milestone |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Record Development Milestone from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records milestones such as rolling, crawling, walking, speaking, teething, or weaning with date and notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Record Development Milestone. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 116: Record Development Milestone Use Cases Specification**

##### **3.3.1.15 View Growth Chart** {#3.3.1.15-view-growth-chart}

| UC ID and Name | UC-38 View Growth Chart |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Growth Chart from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays baby weight, height, and head circumference over time with prompts to ask an expert when needed. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Growth Chart. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 117: View Growth Chart Use Cases Specification**

##### **3.3.1.16 Add Health Record** {#3.3.1.16-add-health-record}

| UC ID and Name | UC-39 Add Health Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Add Health Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uploads ultrasound images, lab results, prescriptions, vaccination forms, examination results, or notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Health Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 118: Add Health Record Use Cases Specification**

##### **3.3.1.17 Update Health Record** {#3.3.1.17-update-health-record}

| UC ID and Name | UC-40 Update Health Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Health Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates record title, type, tags, date, related person, or notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Health Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 119: Update Health Record Use Cases Specification**

##### **3.3.1.18 Delete or Archive Health Record** {#3.3.1.18-delete-or-archive-health-record}

| UC ID and Name | UC-41 Delete or Archive Health Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete or Archive Health Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes or archives a user-entered health record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete or Archive Health Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 120: Delete or Archive Health Record Use Cases Specification**

##### **3.3.1.19 View Health Record Timeline** {#3.3.1.19-view-health-record-timeline}

| UC ID and Name | UC-42 View Health Record Timeline |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Health Record Timeline from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays mother and baby records by date, stage, type, and source. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Health Record Timeline. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 121: View Health Record Timeline Use Cases Specification**

##### **3.3.1.20 Generate Health Summary** {#3.3.1.20-generate-health-summary}

| UC ID and Name | UC-43 Generate Health Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Generate Health Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Generates a 24-hour, 7-day, or consultation summary from user-selected data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Generate Health Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 122: Generate Health Summary Use Cases Specification**

##### **3.3.1.21 Share Summary with Expert** {#3.3.1.21-share-summary-with-expert}

| UC ID and Name | UC-44 Share Summary with Expert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Share Summary with Expert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Shares a health summary or record with an expert within the granted permission period. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Share Summary with Expert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 123: Share Summary with Expert Use Cases Specification**

##### **3.3.1.22 Create Appointment Reminder** {#3.3.1.22-create-appointment-reminder}

| UC ID and Name | UC-45 Create Appointment Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Create Appointment Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a reminder for checkups, follow-up visits, ultrasounds, lab tests, or expert questions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Appointment Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 124: Create Appointment Reminder Use Cases Specification**

##### **3.3.1.23 Create Medication Reminder** {#3.3.1.23-create-medication-reminder}

| UC ID and Name | UC-46 Create Medication Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Create Medication Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a medicine or vitamin reminder based on user-entered existing instructions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Medication Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 125: Create Medication Reminder Use Cases Specification**

##### **3.3.1.24 Create Vaccination Reminder** {#3.3.1.24-create-vaccination-reminder}

| UC ID and Name | UC-47 Create Vaccination Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Create Vaccination Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates or confirms a baby vaccination reminder based on reference schedules or birth date. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Vaccination Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 126: Create Vaccination Reminder Use Cases Specification**

##### **3.3.1.25 Update or Snooze Reminder** {#3.3.1.25-update-or-snooze-reminder}

| UC ID and Name | UC-48 Update or Snooze Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Update or Snooze Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates time, snoozes, completes, or skips a reminder. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update or Snooze Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 127: Update or Snooze Reminder Use Cases Specification**

##### **3.3.1.26 View Today Tasks** {#3.3.1.26-view-today-tasks}

| UC ID and Name | UC-49 View Today Tasks |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates View Today Tasks from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays today's reminders, checklists, and prioritized care tasks. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Today Tasks. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 128: View Today Tasks Use Cases Specification**

##### **3.3.1.27 Manage Preparation Checklist** {#3.3.1.27-manage-preparation-checklist}

| UC ID and Name | UC-50 Manage Preparation Checklist |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Manage Preparation Checklist from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Adds, edits, or marks checklist items for delivery preparation, paperwork, and baby care. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Manage Preparation Checklist. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 129: Manage Preparation Checklist Use Cases Specification**

##### **3.3.1.28 Add Expense** {#3.3.1.28-add-expense}

| UC ID and Name | UC-51 Add Expense |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Add Expense from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records expenses for checkups, delivery, diapers, milk, vaccination, or supplies. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Expense. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 130: Add Expense Use Cases Specification**

##### **3.3.1.29 Update Expense** {#3.3.1.29-update-expense}

| UC ID and Name | UC-52 Update Expense |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Expense from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates or deletes a user-entered expense. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Expense. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 131: Update Expense Use Cases Specification**

##### **3.3.1.30 View Expense Summary** {#3.3.1.30-view-expense-summary}

| UC ID and Name | UC-53 View Expense Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Expense Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays total expenses by month, stage, or category. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Expense Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 132: View Expense Summary Use Cases Specification**

##### **3.3.1.31 Create Community Question** {#3.3.1.31-create-community-question}

| UC ID and Name | UC-54 Create Community Question |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Create Community Question from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Posts a community question with topic, stage, pregnancy or baby age, and urgency. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Community Question. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 133: Create Community Question Use Cases Specification**

##### **3.3.1.32 Edit Community Post** {#3.3.1.32-edit-community-post}

| UC ID and Name | UC-55 Edit Community Post |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Edit Community Post from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Edits the user's own community post or question when it is not locked by moderation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Edit Community Post. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 134: Edit Community Post Use Cases Specification**

##### **3.3.1.33 Post Community Answer** {#3.3.1.33-post-community-answer}

| UC ID and Name | UC-56 Post Community Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Post Community Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Shares a labeled personal experience without diagnosis or prescription. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Post Community Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 135: Post Community Answer Use Cases Specification**

##### **3.3.1.34 Use Anonymous Display** {#3.3.1.34-use-anonymous-display}

| UC ID and Name | UC-57 Use Anonymous Display |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Use Anonymous Display from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Posts sensitive questions under an anonymous community label. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Use Anonymous Display. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 136: Use Anonymous Display Use Cases Specification**

##### **3.3.1.35 Bookmark Community Post** {#3.3.1.35-bookmark-community-post}

| UC ID and Name | UC-58 Bookmark Community Post |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Bookmark Community Post from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Saves a community post to the bookmark list for later viewing. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Bookmark Community Post. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 137: Bookmark Community Post Use Cases Specification**

##### **3.3.1.36 Like Answer** {#3.3.1.36-like-answer}

| UC ID and Name | UC-59 Like Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Like Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Likes or unlikes an answer to reflect usefulness and support ranking. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Like Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 138: Like Answer Use Cases Specification**

##### **3.3.1.37 Run AI Symptom Intake** {#3.3.1.37-run-ai-symptom-intake}

| UC ID and Name | UC-60 Run AI Symptom Intake |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Mother selects or initiates Run AI Symptom Intake from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uses AI to ask step-by-step questions and collect structured symptoms and context. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Run AI Symptom Intake. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 139: Run AI Symptom Intake Use Cases Specification**

##### **3.3.1.38 View Risk Triage Result** {#3.3.1.38-view-risk-triage-result}

| UC ID and Name | UC-61 View Risk Triage Result |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Gemini AI Service |
| **Trigger** | The Mother selects or initiates View Risk Triage Result from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays green, yellow, or red risk level and the safe next action. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Risk Triage Result. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 140: View Risk Triage Result Use Cases Specification**

##### **3.3.1.39 Open Emergency Flow** {#3.3.1.39-open-emergency-flow}

| UC ID and Name | UC-62 Open Emergency Flow |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Open Emergency Flow from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Opens emergency support when a red flag is detected or selected by the user. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Open Emergency Flow. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 141: Open Emergency Flow Use Cases Specification**

##### **3.3.1.40 Find Nearby Care Facility** {#3.3.1.40-find-nearby-care-facility}

| UC ID and Name | UC-63 Find Nearby Care Facility |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates Find Nearby Care Facility from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uses current location to find nearby hospitals or clinics. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Find Nearby Care Facility. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 142: Find Nearby Care Facility Use Cases Specification**

##### **3.3.1.41 Quick Call or Navigate** {#3.3.1.41-quick-call-or-navigate}

| UC ID and Name | UC-64 Quick Call or Navigate |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service, ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Quick Call or Navigate from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Calls a hotline or care facility, or opens map navigation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Quick Call or Navigate. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 143: Quick Call or Navigate Use Cases Specification**

##### **3.3.1.42 Send Family Emergency Alert** {#3.3.1.42-send-family-emergency-alert}

| UC ID and Name | UC-65 Send Family Emergency Alert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging, TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates Send Family Emergency Alert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Sends a minimal alert and permitted location to authorized family members. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Send Family Emergency Alert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 144: Send Family Emergency Alert Use Cases Specification**

##### **3.3.1.43 Connect Health Device** {#3.3.1.43-connect-health-device}

| UC ID and Name | UC-66 Connect Health Device |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Connect Health Device from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Connects a wearable or health platform after user consent. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Connect Health Device. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 145: Connect Health Device Use Cases Specification**

##### **3.3.1.44 Import Device Data Manually** {#3.3.1.44-import-device-data-manually}

| UC ID and Name | UC-67 Import Device Data Manually |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Import Device Data Manually from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Imports or mocks heart rate, sleep, steps, SpO2, or blood pressure data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Import Device Data Manually. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 146: Import Device Data Manually Use Cases Specification**

##### **3.3.1.45 Disconnect Health Device** {#3.3.1.45-disconnect-health-device}

| UC ID and Name | UC-68 Disconnect Health Device |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Disconnect Health Device from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Disconnects the device and stops health device synchronization. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Disconnect Health Device. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 147: Disconnect Health Device Use Cases Specification**

##### **3.3.1.46 View Device Data Trend** {#3.3.1.46-view-device-data-trend}

| UC ID and Name | UC-69 View Device Data Trend |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Device Data Trend from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays device data trends with source labels and accuracy warnings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Device Data Trend. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 148: View Device Data Trend Use Cases Specification**

##### **3.3.1.47 Create Care Group** {#3.3.1.47-create-care-group}

| UC ID and Name | UC-70 Create Care Group |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Create Care Group from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates a care group for the mother or baby. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Create Care Group. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 149: Create Care Group Use Cases Specification**

##### **3.3.1.48 Invite Family Member** {#3.3.1.48-invite-family-member}

| UC ID and Name | UC-71 Invite Family Member |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Invite Family Member from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Invites a family member by link, QR code, or phone number. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Invite Family Member. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 150: Invite Family Member Use Cases Specification**

##### **3.3.1.49 Manage Family Permission** {#3.3.1.49-manage-family-permission}

| UC ID and Name | UC-72 Manage Family Permission |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Manage Family Permission from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Grants or updates family access to calendar, logs, alerts, and records. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Manage Family Permission. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 151: Manage Family Permission Use Cases Specification**

##### **3.3.1.50 Assign Family Task** {#3.3.1.50-assign-family-task}

| UC ID and Name | UC-73 Assign Family Task |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Assign Family Task from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Assigns a care task, due date, and reminder to a care group member. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Assign Family Task. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 152: Assign Family Task Use Cases Specification**

##### **3.3.1.51 View Shared Care Calendar** {#3.3.1.51-view-shared-care-calendar}

| UC ID and Name | UC-74 View Shared Care Calendar |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Shared Care Calendar from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays shared calendar items and tasks according to permissions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Shared Care Calendar. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 153: View Shared Care Calendar Use Cases Specification**

##### **3.3.1.52 Book Private Consultation** {#3.3.1.52-book-private-consultation}

| UC ID and Name | UC-75 Book Private Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Book Private Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Selects expert, topic, modality, slot, shared data, and confirms the booking. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Book Private Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 154: Book Private Consultation Use Cases Specification**

##### **3.3.1.53 Pay Consultation Fee** {#3.3.1.53-pay-consultation-fee}

| UC ID and Name | UC-76 Pay Consultation Fee |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Mother selects or initiates Pay Consultation Fee from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Pays the consultation fee through the payment gateway or mock payment. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Pay Consultation Fee. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 155: Pay Consultation Fee Use Cases Specification**

##### **3.3.1.54 Join Consultation Session** {#3.3.1.54-join-consultation-session}

| UC ID and Name | UC-77 Join Consultation Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Join Consultation Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Joins the booked chat, voice, or video consultation session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Join Consultation Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 156: Join Consultation Session Use Cases Specification**

##### **3.3.1.55 Submit Dispute or Refund Request** {#3.3.1.55-submit-dispute-or-refund-request}

| UC ID and Name | UC-78 Submit Dispute or Refund Request |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Mother selects or initiates Submit Dispute or Refund Request from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Submits a dispute when the expert is absent, scope is violated, or a technical issue occurs. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Submit Dispute or Refund Request. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 157: Submit Dispute or Refund Request Use Cases Specification**

##### **3.3.1.56 Review Expert After Consultation** {#3.3.1.56-review-expert-after-consultation}

| UC ID and Name | UC-79 Review Expert After Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Review Expert After Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Reviews the expert after a confirmed consultation session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Review Expert After Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 158: Review Expert After Consultation Use Cases Specification**

##### **3.3.1.57 View Expert Directory** {#3.3.1.57-view-expert-directory}

| UC ID and Name | UC-80 View Expert Directory |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Expert Directory from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays and filters experts by specialty, badge, and availability. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Expert Directory. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 159: View Expert Directory Use Cases Specification**

##### **3.3.1.58 View Expert Profile** {#3.3.1.58-view-expert-profile}

| UC ID and Name | UC-81 View Expert Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Expert Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays expert qualifications, consultation scope, badge, reviews, and availability. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Expert Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 160: View Expert Profile Use Cases Specification**

##### **3.3.1.59 View Content and Checklist** {#3.3.1.59-view-content-and-checklist}

| UC ID and Name | UC-82 View Content and Checklist |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Content and Checklist from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays approved articles, FAQs, and checklists by stage. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Content and Checklist. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Care Journey, Community, Health & Consultation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 161: View Content and Checklist Use Cases Specification**

#### ***3.3.2 Pregnancy Exercise & Posture Support*** {#3.3.2-pregnancy-exercise-&-posture-support}

##### **3.3.2.1 View and Select Pregnancy Exercise** {#3.3.2.1-view-and-select-pregnancy-exercise}

| UC ID and Name | UC-29 View and Select Pregnancy Exercise |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View and Select Pregnancy Exercise from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays suitable exercises and lets the Mother choose one to view or start. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View and Select Pregnancy Exercise. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 162: View and Select Pregnancy Exercise Use Cases Specification**

##### **3.3.2.2 Analyze Exercise Posture** {#3.3.2.2-analyze-exercise-posture}

| UC ID and Name | UC-30 Analyze Exercise Posture |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | VNPay Payment Gateway, Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Analyze Exercise Posture from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Analyzes body landmark data and returns near-real-time posture feedback using rule-based or ML-based logic. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Analyze Exercise Posture. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 163: Analyze Exercise Posture Use Cases Specification**

##### **3.3.2.3 View Pregnancy Exercise Detail** {#3.3.2.3-view-pregnancy-exercise-detail}

| UC ID and Name | UC-177 View Pregnancy Exercise Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Pregnancy Exercise Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays description, instructions, duration, difficulty, suitable trimester, attention points, and safety warnings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Pregnancy Exercise Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 164: View Pregnancy Exercise Detail Use Cases Specification**

##### **3.3.2.4 Complete Pre-exercise Safety Check** {#3.3.2.4-complete-pre-exercise-safety-check}

| UC ID and Name | UC-178 Complete Pre-exercise Safety Check |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Complete Pre-exercise Safety Check from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Answers safety questions before starting and blocks continuation when warning signs are present. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Complete Pre-exercise Safety Check. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 165: Complete Pre-exercise Safety Check Use Cases Specification**

##### **3.3.2.5 Start Exercise Session** {#3.3.2.5-start-exercise-session}

| UC ID and Name | UC-179 Start Exercise Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Start Exercise Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates an exercise session after an exercise is selected and the safety check passes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Start Exercise Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 166: Start Exercise Session Use Cases Specification**

##### **3.3.2.6 Enable Posture Camera** {#3.3.2.6-enable-posture-camera}

| UC ID and Name | UC-180 Enable Posture Camera |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Enable Posture Camera from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Requests permission and enables the camera when posture analysis is supported and chosen by the Mother. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Enable Posture Camera. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 167: Enable Posture Camera Use Cases Specification**

##### **3.3.2.7 Pause or Resume Exercise Session** {#3.3.2.7-pause-or-resume-exercise-session}

| UC ID and Name | UC-181 Pause or Resume Exercise Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | VNPay Payment Gateway, Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Pause or Resume Exercise Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Pauses or resumes timing and posture feedback during an exercise session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Pause or Resume Exercise Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 168: Pause or Resume Exercise Session Use Cases Specification**

##### **3.3.2.8 Complete Exercise Session** {#3.3.2.8-complete-exercise-session}

| UC ID and Name | UC-182 Complete Exercise Session |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Complete Exercise Session from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Ends the session and saves duration, completion status, posture score summary, and common issues. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Complete Exercise Session. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 169: Complete Exercise Session Use Cases Specification**

##### **3.3.2.9 View Exercise Session Result** {#3.3.2.9-view-exercise-session-result}

| UC ID and Name | UC-183 View Exercise Session Result |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates View Exercise Session Result from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays post-session results including duration, completion level, posture score, and warnings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Exercise Session Result. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 170: View Exercise Session Result Use Cases Specification**

##### **3.3.2.10 View Pregnancy Exercise History** {#3.3.2.10-view-pregnancy-exercise-history}

| UC ID and Name | UC-184 View Pregnancy Exercise History |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Pregnancy Exercise History from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays saved pregnancy exercise sessions and their details. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Pregnancy Exercise History. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Pregnancy Exercise & Posture Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 171: View Pregnancy Exercise History Use Cases Specification**

#### ***3.3.3 Family Sync*** {#3.3.3-family-sync}

##### **3.3.3.1 Accept Care Group Invitation** {#3.3.3.1-accept-care-group-invitation}

| UC ID and Name | UC-83 Accept Care Group Invitation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Family Member selects or initiates Accept Care Group Invitation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Lets a family member sign in or register and accept a care group invitation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens Accept Care Group Invitation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 172: Accept Care Group Invitation Use Cases Specification**

##### **3.3.3.2 View Shared Data** {#3.3.3.2-view-shared-data}

| UC ID and Name | UC-84 View Shared Data |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Family Member selects or initiates View Shared Data from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays shared calendar, logs, checklists, or alerts according to granted permissions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens View Shared Data. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 173: View Shared Data Use Cases Specification**

##### **3.3.3.3 Update Assigned Task Status** {#3.3.3.3-update-assigned-task-status}

| UC ID and Name | UC-85 Update Assigned Task Status |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | None |
| **Trigger** | The Family Member selects or initiates Update Assigned Task Status from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Marks an assigned task as in progress, completed, or needing support. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens Update Assigned Task Status. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 174: Update Assigned Task Status Use Cases Specification**

##### **3.3.3.4 View Family Alert** {#3.3.3.4-view-family-alert}

| UC ID and Name | UC-86 View Family Alert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Family Member selects or initiates View Family Alert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Receives and displays important family alerts with minimum consent-based information. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens View Family Alert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 175: View Family Alert Use Cases Specification**

#### ***3.3.4 Smart Activity Monitoring & Safety Support*** {#3.3.4-smart-activity-monitoring-&-safety-support}

##### **3.3.4.1 Configure Safety Monitoring** {#3.3.4.1-configure-safety-monitoring}

| UC ID and Name | UC-133 Configure Safety Monitoring |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging, TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates Configure Safety Monitoring from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Configures consent, alert recipients, countdown, optional location, and active monitoring time. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Configure Safety Monitoring. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 176: Configure Safety Monitoring Use Cases Specification**

##### **3.3.4.2 Enable Fall Detection** {#3.3.4.2-enable-fall-detection}

| UC ID and Name | UC-134 Enable Fall Detection |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Enable Fall Detection from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Starts an IMU monitoring session using the saved configuration. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Enable Fall Detection. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 177: Enable Fall Detection Use Cases Specification**

##### **3.3.4.3 Disable Fall Detection** {#3.3.4.3-disable-fall-detection}

| UC ID and Name | UC-135 Disable Fall Detection |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Smartwatch/Wearable Device |
| **Trigger** | The Mother selects or initiates Disable Fall Detection from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Stops the sensor stream and prevents new safety events from being created. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Disable Fall Detection. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 178: Disable Fall Detection Use Cases Specification**

##### **3.3.4.4 Detect Suspected Fall or Impact** {#3.3.4.4-detect-suspected-fall-or-impact}

| UC ID and Name | UC-136 Detect Suspected Fall or Impact |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Phone Motion Sensors | **Secondary Actors** | None |
| **Trigger** | The Phone Motion Sensors selects or initiates Detect Suspected Fall or Impact from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Detects abnormal acceleration, orientation change, and low-motion states to create a suspected event. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Phone Motion Sensors opens Detect Suspected Fall or Impact. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App / On-device; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 179: Detect Suspected Fall or Impact Use Cases Specification**

##### **3.3.4.5 Confirm Safety Check** {#3.3.4.5-confirm-safety-check}

| UC ID and Name | UC-137 Confirm Safety Check |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging, ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Confirm Safety Check from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Asks the user to confirm I am OK, Need help, or Call emergency before an automatic alert is sent. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens a Confirm Safety Check. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 180: Confirm Safety Check Use Cases Specification**

##### **3.3.4.6 Send Emergency Alert** {#3.3.4.6-send-emergency-alert}

| UC ID and Name | UC-138 Send Emergency Alert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Send Emergency Alert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Sends a minimal alert to configured family members and records delivery status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Send Emergency Alert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App \+ Backend; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has a legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 181: Send Emergency Alert Use Cases Specification**

##### **3.3.4.7 View Safety Event History** {#3.3.4.7-view-safety-event-history}

| UC ID and Name | UC-139 View Safety Event History |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates View Safety Event History from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays event history, confirmation results, alert status, and false-positive labels. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Safety Event History. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 182: View Safety Event History Use Cases Specification**

##### **3.3.4.8 Report False Positive Detection** {#3.3.4.8-report-false-positive-detection}

| UC ID and Name | UC-140 Report False Positive Detection |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Report False Positive Detection from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Labels a false positive and optional reason to improve rules. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Report False Positive Detection. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App \+ Backend; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 183: Report False Positive Detection Use Cases Specification**

##### **3.3.4.9 Open Emergency Support from Safety Alert** {#3.3.4.9-open-emergency-support-from-safety-alert}

| UC ID and Name | UC-141 Open Emergency Support from Safety Alert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging, TrackAsia Map Service, ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Open Emergency Support from Safety Alert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Routes to emergency support for quick call, nearest facility search, or navigation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Open Emergency Support from Safety Alert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 184: Open Emergency Support from Safety Alert Use Cases Specification**

##### **3.3.4.10 Configure Emergency Contact** {#3.3.4.10-configure-emergency-contact}

| UC ID and Name | UC-176 Configure Emergency Contact |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Configure Emergency Contact from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Adds, verifies, prioritizes, or removes alert recipients used by safety and emergency flows. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Configure Emergency Contact. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Critical |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Smart Activity Monitoring & Safety Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 185: Configure Emergency Contact Use Cases Specification**

#### ***3.3.5 Mobile Consultation & Location*** {#3.3.5-mobile-consultation-&-location}

##### **3.3.5.1 Update Consultation Availability Status** {#3.3.5.1-update-consultation-availability-status}

| UC ID and Name | UC-142 Update Consultation Availability Status |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Update Consultation Availability Status from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Lets the expert manage online status and available consultation channels. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Update Consultation Availability Status. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App / Expert Portal; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 186: Update Consultation Availability Status Use Cases Specification**

##### **3.3.5.2 Respond to Consultation Request** {#3.3.5.2-respond-to-consultation-request}

| UC ID and Name | UC-143 Respond to Consultation Request |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Respond to Consultation Request from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Lets the expert accept, reject, or propose changes to a consultation request. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Respond to Consultation Request. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App / Expert Portal; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 187: Respond to Consultation Request Use Cases Specification**

##### **3.3.5.3 Consult via Chat** {#3.3.5.3-consult-via-chat}

| UC ID and Name | UC-144 Consult via Chat |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Verified Expert selects or initiates Consult via Chat from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Exchanges messages inside a valid consultation session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Consult via Chat. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App / Expert Portal; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 188: Consult via Chat Use Cases Specification**

##### **3.3.5.4 Consult via Voice Call** {#3.3.5.4-consult-via-voice-call}

| UC ID and Name | UC-145 Consult via Voice Call |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Verified Expert selects or initiates Consult via Voice Call from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Joins a voice call inside a confirmed consultation session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Consult via Voice Call. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 189: Consult via Voice Call Use Cases Specification**

##### **3.3.5.5 Consult via Video Call** {#3.3.5.5-consult-via-video-call}

| UC ID and Name | UC-146 Consult via Video Call |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Verified Expert selects or initiates Consult via Video Call from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Joins a video call inside a confirmed consultation session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Consult via Video Call. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App / Expert Portal; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 190: Consult via Video Call Use Cases Specification**

##### **3.3.5.6 Contact Nearby User** {#3.3.5.6-contact-nearby-user}

| UC ID and Name | UC-151 Contact Nearby User |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Verified Expert selects or initiates Contact Nearby User from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Contacts the user after accepting a nearby support request. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Contact Nearby User. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- Mobile Consultation & Location. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 191: Contact Nearby User Use Cases Specification**

#### ***3.3.6 MF-19 Location & Nearby Support*** {#3.3.6-mf-19-location-&-nearby-support}

##### **3.3.6.1 Share Expert Location** {#3.3.6.1-share-expert-location}

| UC ID and Name | UC-147 Share Expert Location |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Verified Expert selects or initiates Share Expert Location from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Lets the expert voluntarily share current or approximate location in the TrackAsia map layer for a selected time period. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Share Expert Location. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- MF-19 Location & Nearby Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 192: Share Expert Location Use Cases Specification**

##### **3.3.6.2 Manage Location Visibility** {#3.3.6.2-manage-location-visibility}

| UC ID and Name | UC-148 Manage Location Visibility |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Verified Expert selects or initiates Manage Location Visibility from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Manages location visibility scope, duration, and display conditions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Manage Location Visibility. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- MF-19 Location & Nearby Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 193: Manage Location Visibility Use Cases Specification**

##### **3.3.6.3 View Nearby Support Requests** {#3.3.6.3-view-nearby-support-requests}

| UC ID and Name | UC-150 View Nearby Support Requests |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Verified Expert selects or initiates View Nearby Support Requests from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays nearby support requests as a list or map markers with minimum necessary data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens View Nearby Support Requests. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- MF-19 Location & Nearby Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 194: View Nearby Support Requests Use Cases Specification**

##### **3.3.6.4 Navigate to Support Location** {#3.3.6.4-navigate-to-support-location}

| UC ID and Name | UC-152 Navigate to Support Location |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Verified Expert selects or initiates Navigate to Support Location from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uses TrackAsia to calculate route and ETA to the consented support location. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Navigate to Support Location. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Expert App; Source group: Expert App \- MF-19 Location & Nearby Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 195: Navigate to Support Location Use Cases Specification**

#### ***3.3.7 MF-19 Emergency Map & Nearby Care Support*** {#3.3.7-mf-19-emergency-map-&-nearby-care-support}

##### **3.3.7.1 Find Nearby Available Experts** {#3.3.7.1-find-nearby-available-experts}

| UC ID and Name | UC-149 Find Nearby Available Experts |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates Find Nearby Available Experts from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Finds verified experts who opted in to location sharing and are available for nearby support. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Find Nearby Available Experts. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- MF-19 Emergency Map & Nearby Care Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 196: Find Nearby Available Experts Use Cases Specification**

##### **3.3.7.2 Contact Nearby Expert** {#3.3.7.2-contact-nearby-expert}

| UC ID and Name | UC-153 Contact Nearby Expert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service, ZegoCloud Realtime Service |
| **Trigger** | The Mother selects or initiates Contact Nearby Expert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Sends a chat, voice call, video call, or booking request to a selected nearby expert. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Contact Nearby Expert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- MF-19 Emergency Map & Nearby Care Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 197: Contact Nearby Expert Use Cases Specification**

##### **3.3.7.3 View Nearby Experts on Map** {#3.3.7.3-view-nearby-experts-on-map}

| UC ID and Name | UC-155 View Nearby Experts on Map |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates View Nearby Experts on Map from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays opted-in experts as markers or approximate areas on the map. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Nearby Experts on Map. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- MF-19 Emergency Map & Nearby Care Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 198: View Nearby Experts on Map Use Cases Specification**

##### **3.3.7.4 Search Nearby Support** {#3.3.7.4-search-nearby-support}

| UC ID and Name | UC-166 Search Nearby Support |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | TrackAsia Map Service |
| **Trigger** | The Mother selects or initiates Search Nearby Support from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Finds nearby care facilities or available experts using TrackAsia map and location capability. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Search Nearby Support. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- MF-19 Emergency Map & Nearby Care Support. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 199: Search Nearby Support Use Cases Specification**

#### ***3.3.8 Community Q\&A*** {#3.3.8-community-q&a}

##### **3.3.8.1 Search Community Questions** {#3.3.8.1-search-community-questions}

| UC ID and Name | UC-162 Search Community Questions |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Search Community Questions from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Searches questions by keyword, stage, topic, answered status, and expert label. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Search Community Questions. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 200: Search Community Questions Use Cases Specification**

##### **3.3.8.2 Search Community Topics** {#3.3.8.2-search-community-topics}

| UC ID and Name | UC-163 Search Community Topics |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Search Community Topics from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Searches and browses topics for pregnancy, postpartum, babies, nutrition, mental health, or safety. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Search Community Topics. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 201: Search Community Topics Use Cases Specification**

##### **3.3.8.3 Delete Community Post** {#3.3.8.3-delete-community-post}

| UC ID and Name | UC-170 Delete Community Post |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Delete Community Post from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Deletes or archives the user's own post or question when it is not locked or under investigation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Delete Community Post. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 202: Delete Community Post Use Cases Specification**

##### **3.3.8.4 Follow Topic** {#3.3.8.4-follow-topic}

| UC ID and Name | UC-171 Follow Topic |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | Firebase Cloud Messaging, VNPay Payment Gateway |
| **Trigger** | The User selects or initiates Follow Topic from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Follows or unfollows topics to personalize the feed and receive related notifications. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Follow Topic. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 203: Follow Topic Use Cases Specification**

#### ***3.3.9 Verified Expert Network*** {#3.3.9-verified-expert-network}

##### **3.3.9.1 Search Expert** {#3.3.9.1-search-expert}

| UC ID and Name | UC-164 Search Expert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Search Expert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Searches experts by name, specialty, support scope, verified badge, or keyword. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Search Expert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Verified Expert Network. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 204: Search Expert Use Cases Specification**

##### **3.3.9.2 Filter Expert** {#3.3.9.2-filter-expert}

| UC ID and Name | UC-165 Filter Expert |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The User selects or initiates Filter Expert from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Filters experts by specialty, consultation modality, availability, fee, rating, online status, and distance when consent exists. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Filter Expert. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Verified Expert Network. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 205: Filter Expert Use Cases Specification**

#### ***3.3.10 File Management*** {#3.3.10-file-management}

##### **3.3.10.1 Upload File** {#3.3.10.1-upload-file}

| UC ID and Name | UC-167 Upload File |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Upload File from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Uploads ultrasound images, medical records, vaccination forms, baby images, or related documents with file, size, and ownership checks. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Upload File. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- File Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 206: Upload File Use Cases Specification**

##### **3.3.10.2 View File** {#3.3.10.2-view-file}

| UC ID and Name | UC-168 View File |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View File from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Previews or downloads files within valid access scope and sharing period. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View File. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: App/Web; Source group: Mobile App \- File Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 207: View File Use Cases Specification**

##### **3.3.10.3 Delete File** {#3.3.10.3-delete-file}

| UC ID and Name | UC-169 Delete File |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete File from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes an uploaded file when it is not bound by records, consultations, or retention policy. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete File. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | High |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- File Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 208: Delete File Use Cases Specification**

#### ***3.3.11 Mother Care Journey*** {#3.3.11-mother-care-journey}

##### **3.3.11.1 View Maternal Health Metric Detail** {#3.3.11.1-view-maternal-health-metric-detail}

| UC ID and Name | UC-187 View Maternal Health Metric Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Maternal Health Metric Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays value, timestamp, source, and notes for one maternal health metric. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Maternal Health Metric Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Mother Care Journey. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 209: View Maternal Health Metric Detail Use Cases Specification**

##### **3.3.11.2 Delete Maternal Health Metric** {#3.3.11.2-delete-maternal-health-metric}

| UC ID and Name | UC-188 Delete Maternal Health Metric |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Maternal Health Metric from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes a Mother-entered metric that is no longer needed or was entered incorrectly. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Maternal Health Metric. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Mother Care Journey. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 210: Delete Maternal Health Metric Use Cases Specification**

##### **3.3.11.3 View Postpartum Logs** {#3.3.11.3-view-postpartum-logs}

| UC ID and Name | UC-189 View Postpartum Logs |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Postpartum Logs from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays postpartum recovery logs and details over time. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Postpartum Logs. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Mother Care Journey. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 211: View Postpartum Logs Use Cases Specification**

##### **3.3.11.4 Update Postpartum Log** {#3.3.11.4-update-postpartum-log}

| UC ID and Name | UC-190 Update Postpartum Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Postpartum Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates Mother-entered postpartum log content. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Postpartum Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Mother Care Journey. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 212: Update Postpartum Log Use Cases Specification**

##### **3.3.11.5 Delete Postpartum Log** {#3.3.11.5-delete-postpartum-log}

| UC ID and Name | UC-191 Delete Postpartum Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Postpartum Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes a Mother-entered postpartum log. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Postpartum Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Mother Care Journey. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 213: Delete Postpartum Log Use Cases Specification**

#### ***3.3.12 Baby Care Journey & Growth*** {#3.3.12-baby-care-journey-&-growth}

##### **3.3.12.1 View Baby Profile** {#3.3.12.1-view-baby-profile}

| UC ID and Name | UC-192 View Baby Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Baby Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays basic information and tracking status for one baby profile. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Baby Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 214: View Baby Profile Use Cases Specification**

##### **3.3.12.2 Switch Active Baby Profile** {#3.3.12.2-switch-active-baby-profile}

| UC ID and Name | UC-193 Switch Active Baby Profile |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Switch Active Baby Profile from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Selects the active baby profile when the account manages multiple babies. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Switch Active Baby Profile. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 215: Switch Active Baby Profile Use Cases Specification**

##### **3.3.12.3 View Baby Daily Log Detail** {#3.3.12.3-view-baby-daily-log-detail}

| UC ID and Name | UC-194 View Baby Daily Log Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Baby Daily Log Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays content, timestamp, and type for one baby daily log record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Baby Daily Log Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 216: View Baby Daily Log Detail Use Cases Specification**

##### **3.3.12.4 Delete Baby Daily Log** {#3.3.12.4-delete-baby-daily-log}

| UC ID and Name | UC-195 Delete Baby Daily Log |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Baby Daily Log from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes a Mother-entered baby daily log record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Baby Daily Log. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 217: Delete Baby Daily Log Use Cases Specification**

##### **3.3.12.5 Update Development Milestone** {#3.3.12.5-update-development-milestone}

| UC ID and Name | UC-196 Update Development Milestone |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Development Milestone from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates date, notes, or status for a development milestone. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Development Milestone. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 218: Update Development Milestone Use Cases Specification**

##### **3.3.12.6 Delete Development Milestone** {#3.3.12.6-delete-development-milestone}

| UC ID and Name | UC-197 Delete Development Milestone |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Development Milestone from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes a Mother-recorded development milestone. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Development Milestone. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Baby Care Journey & Growth. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 219: Delete Development Milestone Use Cases Specification**

#### ***3.3.13 Community Q\&A & Moderation*** {#3.3.13-community-q&a-&-moderation}

##### **3.3.13.1 View Community Feed** {#3.3.13.1-view-community-feed}

| UC ID and Name | UC-198 View Community Feed |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The User selects or initiates View Community Feed from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays permitted questions and posts by topic and time. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Community Feed. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A & Moderation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 220: View Community Feed Use Cases Specification**

##### **3.3.13.2 View Community Question Detail** {#3.3.13.2-view-community-question-detail}

| UC ID and Name | UC-199 View Community Question Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Community Question Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays question content, answers, source labels, and moderation status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Community Question Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A & Moderation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 221: View Community Question Detail Use Cases Specification**

##### **3.3.13.3 Edit Own Answer** {#3.3.13.3-edit-own-answer}

| UC ID and Name | UC-200 Edit Own Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Edit Own Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Edits the user's own answer when it is not locked. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Edit Own Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A & Moderation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 222: Edit Own Answer Use Cases Specification**

##### **3.3.13.4 Delete Own Answer** {#3.3.13.4-delete-own-answer}

| UC ID and Name | UC-201 Delete Own Answer |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Delete Own Answer from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes the user's own answer according to community policy. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Delete Own Answer. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Community Q\&A & Moderation. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 223: Delete Own Answer Use Cases Specification**

#### ***3.3.14 Direct Consultation & Commission*** {#3.3.14-direct-consultation-&-commission}

##### **3.3.14.1 View Consultation List** {#3.3.14.1-view-consultation-list}

| UC ID and Name | UC-202 View Consultation List |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Verified Expert | **Secondary Actors** | ZegoCloud Realtime Service |
| **Trigger** | The Mother, Verified Expert selects or initiates View Consultation List from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays upcoming, pending, completed, canceled, or no-show consultation sessions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Verified Expert opens View Consultation List. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: App/Expert; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 224: View Consultation List Use Cases Specification**

##### **3.3.14.2 View Consultation Detail** {#3.3.14.2-view-consultation-detail}

| UC ID and Name | UC-203 View Consultation Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Mother, Verified Expert selects or initiates View Consultation Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays expert or user, time, modality, status, fee, and shared-data scope. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Verified Expert opens View Consultation Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: App/Expert; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 225: View Consultation Detail Use Cases Specification**

##### **3.3.14.3 Reschedule Consultation** {#3.3.14.3-reschedule-consultation}

| UC ID and Name | UC-204 Reschedule Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Mother, Verified Expert selects or initiates Reschedule Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Proposes and confirms a new time slot before the allowed deadline. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Verified Expert opens Reschedule Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: App/Expert; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 226: Reschedule Consultation Use Cases Specification**

##### **3.3.14.4 Cancel Consultation** {#3.3.14.4-cancel-consultation}

| UC ID and Name | UC-205 Cancel Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Verified Expert | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The Mother, Verified Expert selects or initiates Cancel Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Cancels a session according to deadline, fee, and reason policy. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Verified Expert opens Cancel Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: App/Expert; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 227: Cancel Consultation Use Cases Specification**

##### **3.3.14.5 Complete Consultation** {#3.3.14.5-complete-consultation}

| UC ID and Name | UC-206 Complete Consultation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert | **Secondary Actors** | None |
| **Trigger** | The Verified Expert selects or initiates Complete Consultation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Ends the session and changes status for summary, review, and reconciliation. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert opens Complete Consultation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert App/Portal; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 228: Complete Consultation Use Cases Specification**

##### **3.3.14.6 Mark Consultation No-show** {#3.3.14.6-mark-consultation-no-show}

| UC ID and Name | UC-207 Mark Consultation No-show |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Verified Expert, System Admin | **Secondary Actors** | None |
| **Trigger** | The Verified Expert, System Admin selects or initiates Mark Consultation No-show from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records no-show after the waiting period and session-status evidence. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Verified Expert, System Admin opens Mark Consultation No-show. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Expert/Admin; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 229: Mark Consultation No-show Use Cases Specification**

##### **3.3.14.7 View Consultation Summary** {#3.3.14.7-view-consultation-summary}

| UC ID and Name | UC-208 View Consultation Summary |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Consultation Summary from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays the expert-written summary and safe next steps after the session. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Consultation Summary. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 230: View Consultation Summary Use Cases Specification**

##### **3.3.14.8 Resolve Consultation Dispute** {#3.3.14.8-resolve-consultation-dispute}

| UC ID and Name | UC-209 Resolve Consultation Dispute |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | None |
| **Trigger** | The System Admin selects or initiates Resolve Consultation Dispute from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Reviews minimum necessary data, both parties' responses, and decides the dispute outcome. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Resolve Consultation Dispute. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 231: Resolve Consultation Dispute Use Cases Specification**

##### **3.3.14.9 Approve or Reject Refund** {#3.3.14.9-approve-or-reject-refund}

| UC ID and Name | UC-210 Approve or Reject Refund |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | System Admin | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The System Admin selects or initiates Approve or Reject Refund from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Approves or rejects refund according to dispute outcome and transaction status. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The System Admin opens Approve or Reject Refund. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Mobile App / Expert \- Direct Consultation & Commission. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 232: Approve or Reject Refund Use Cases Specification**

#### ***3.3.15 Personal Health Records & File Management*** {#3.3.15-personal-health-records-&-file-management}

##### **3.3.15.1 View Health Record Detail** {#3.3.15.1-view-health-record-detail}

| UC ID and Name | UC-211 View Health Record Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Health Record Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays metadata, source, date, related person, notes, and attached files for a health record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Health Record Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Personal Health Records & File Management. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 233: View Health Record Detail Use Cases Specification**

#### ***3.3.16 Reminders, Tasks & Care Plan*** {#3.3.16-reminders,-tasks-&-care-plan}

##### **3.3.16.1 View Reminder Detail** {#3.3.16.1-view-reminder-detail}

| UC ID and Name | UC-212 View Reminder Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates View Reminder Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays reminder type, recurrence, time, status, and notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Reminder Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Reminders, Tasks & Care Plan. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 234: View Reminder Detail Use Cases Specification**

##### **3.3.16.2 Complete Reminder** {#3.3.16.2-complete-reminder}

| UC ID and Name | UC-213 Complete Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Complete Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Marks a reminder as completed and updates the next recurrence if applicable. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Complete Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Reminders, Tasks & Care Plan. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 235: Complete Reminder Use Cases Specification**

##### **3.3.16.3 Skip Reminder** {#3.3.16.3-skip-reminder}

| UC ID and Name | UC-214 Skip Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Skip Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Skips one reminder occurrence without deleting the recurrence configuration. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Skip Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Reminders, Tasks & Care Plan. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 236: Skip Reminder Use Cases Specification**

##### **3.3.16.4 Delete Reminder** {#3.3.16.4-delete-reminder}

| UC ID and Name | UC-215 Delete Reminder |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Delete Reminder from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Deletes or disables a Mother-created reminder. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Reminder. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Reminders, Tasks & Care Plan. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 237: Delete Reminder Use Cases Specification**

#### ***3.3.17 Family Sync & Cooperative Care*** {#3.3.17-family-sync-&-cooperative-care}

##### **3.3.17.1 View Care Group Members** {#3.3.17.1-view-care-group-members}

| UC ID and Name | UC-216 View Care Group Members |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Family Member | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother, Family Member selects or initiates View Care Group Members from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays members, roles, permissions, and invitation status in the care group. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Family Member opens View Care Group Members. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 238: View Care Group Members Use Cases Specification**

##### **3.3.17.2 Revoke Family Invitation** {#3.3.17.2-revoke-family-invitation}

| UC ID and Name | UC-217 Revoke Family Invitation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Mother selects or initiates Revoke Family Invitation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Cancels a family invitation before it is accepted. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Revoke Family Invitation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 239: Revoke Family Invitation Use Cases Specification**

##### **3.3.17.3 Reject Care Group Invitation** {#3.3.17.3-reject-care-group-invitation}

| UC ID and Name | UC-218 Reject Care Group Invitation |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | Firebase Cloud Messaging |
| **Trigger** | The Family Member selects or initiates Reject Care Group Invitation from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Rejects a care group invitation and does not create data access permission. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens Reject Care Group Invitation. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 240: Reject Care Group Invitation Use Cases Specification**

##### **3.3.17.4 Remove Family Member** {#3.3.17.4-remove-family-member}

| UC ID and Name | UC-219 Remove Family Member |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Remove Family Member from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Removes a member and revokes active permissions. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Remove Family Member. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 241: Remove Family Member Use Cases Specification**

##### **3.3.17.5 Leave Care Group** {#3.3.17.5-leave-care-group}

| UC ID and Name | UC-220 Leave Care Group |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Family Member | **Secondary Actors** | None |
| **Trigger** | The Family Member selects or initiates Leave Care Group from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Leaves the care group and stops access to shared data. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Family Member opens Leave Care Group. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 242: Leave Care Group Use Cases Specification**

##### **3.3.17.6 View Assigned Task Detail** {#3.3.17.6-view-assigned-task-detail}

| UC ID and Name | UC-221 View Assigned Task Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother, Family Member | **Secondary Actors** | None |
| **Trigger** | The Mother, Family Member selects or initiates View Assigned Task Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays task content, due date, assigner, status, and notes. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother, Family Member opens View Assigned Task Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 243: View Assigned Task Detail Use Cases Specification**

##### **3.3.17.7 Update Family Task** {#3.3.17.7-update-family-task}

| UC ID and Name | UC-222 Update Family Task |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Family Task from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates content, due date, or recipient for an incomplete task. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Family Task. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 244: Update Family Task Use Cases Specification**

##### **3.3.17.8 Cancel Family Task** {#3.3.17.8-cancel-family-task}

| UC ID and Name | UC-223 Cancel Family Task |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Cancel Family Task from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Cancels a task and notifies related members. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Cancel Family Task. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Family Sync & Cooperative Care. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 245: Cancel Family Task Use Cases Specification**

#### ***3.3.18 Verified Content & Checklist Hub*** {#3.3.18-verified-content-&-checklist-hub}

##### **3.3.18.1 Search Verified Content** {#3.3.18.1-search-verified-content}

| UC ID and Name | UC-224 Search Verified Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates Search Verified Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Searches approved articles, FAQs, and checklists by keyword, stage, and topic. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens Search Verified Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App / Admin Web \- Verified Content & Checklist Hub. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 246: Search Verified Content Use Cases Specification**

##### **3.3.18.2 View Verified Content Detail** {#3.3.18.2-view-verified-content-detail}

| UC ID and Name | UC-225 View Verified Content Detail |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | None |
| **Trigger** | The User selects or initiates View Verified Content Detail from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays content, source, version, update date, and related warnings. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Verified Content Detail. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App / Admin Web \- Verified Content & Checklist Hub. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 247: View Verified Content Detail Use Cases Specification**

##### **3.3.18.3 Manage Content Categories** {#3.3.18.3-manage-content-categories}

| UC ID and Name | UC-226 Manage Content Categories |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Manage Content Categories from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Creates, updates, sorts, and hides content categories. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Manage Content Categories. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Mobile App / Admin Web \- Verified Content & Checklist Hub. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 248: Manage Content Categories Use Cases Specification**

##### **3.3.18.4 Unpublish Content** {#3.3.18.4-unpublish-content}

| UC ID and Name | UC-227 Unpublish Content |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Content Admin | **Secondary Actors** | None |
| **Trigger** | The Content Admin selects or initiates Unpublish Content from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Stops displaying a content version without deleting its history. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Content Admin opens Unpublish Content. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Admin Portal; Source group: Mobile App / Admin Web \- Verified Content & Checklist Hub. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 249: Unpublish Content Use Cases Specification**

#### ***3.3.19 Vaccination & Growth Tracking*** {#3.3.19-vaccination-&-growth-tracking}

##### **3.3.19.1 View Vaccination Schedule** {#3.3.19.1-view-vaccination-schedule}

| UC ID and Name | UC-228 View Vaccination Schedule |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Vaccination Schedule from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays vaccination doses for a baby profile by status and expected time. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Vaccination Schedule. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 250: View Vaccination Schedule Use Cases Specification**

##### **3.3.19.2 Add Vaccination Record** {#3.3.19.2-add-vaccination-record}

| UC ID and Name | UC-229 Add Vaccination Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Add Vaccination Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records vaccination dose, date, facility, and proof file if available. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Vaccination Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. BR-SAFETY: medical guidance must be non-diagnostic, escalation-aware, and red-flag safe. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 251: Add Vaccination Record Use Cases Specification**

##### **3.3.19.3 Update Vaccination Record** {#3.3.19.3-update-vaccination-record}

| UC ID and Name | UC-230 Update Vaccination Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Vaccination Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates a Mother-entered vaccination record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Vaccination Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 252: Update Vaccination Record Use Cases Specification**

##### **3.3.19.4 Delete Vaccination Record** {#3.3.19.4-delete-vaccination-record}

| UC ID and Name | UC-231 Delete Vaccination Record |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Vaccination Record from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes a Mother-entered vaccination record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Vaccination Record. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 253: Delete Vaccination Record Use Cases Specification**

##### **3.3.19.5 Mark Vaccination Completed** {#3.3.19.5-mark-vaccination-completed}

| UC ID and Name | UC-232 Mark Vaccination Completed |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Mark Vaccination Completed from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Marks a planned vaccination as completed and links it to a vaccination record. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Mark Vaccination Completed. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 254: Mark Vaccination Completed Use Cases Specification**

##### **3.3.19.6 Postpone Vaccination** {#3.3.19.6-postpone-vaccination}

| UC ID and Name | UC-233 Postpone Vaccination |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Postpone Vaccination from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates the new expected date and user-entered reason. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Postpone Vaccination. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 255: Postpone Vaccination Use Cases Specification**

##### **3.3.19.7 Add Growth Measurement** {#3.3.19.7-add-growth-measurement}

| UC ID and Name | UC-234 Add Growth Measurement |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Add Growth Measurement from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Records baby weight, height, or head circumference. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Add Growth Measurement. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 256: Add Growth Measurement Use Cases Specification**

##### **3.3.19.8 Update Growth Measurement** {#3.3.19.8-update-growth-measurement}

| UC ID and Name | UC-235 Update Growth Measurement |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Update Growth Measurement from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Updates a Mother-entered growth measurement. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Update Growth Measurement. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Regular |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-PRIVACY: health and family data must follow consent, purpose, and minimum-necessary access rules. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 257: Update Growth Measurement Use Cases Specification**

##### **3.3.19.9 Delete Growth Measurement** {#3.3.19.9-delete-growth-measurement}

| UC ID and Name | UC-236 Delete Growth Measurement |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates Delete Growth Measurement from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Soft-deletes an incorrectly entered growth measurement. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens Delete Growth Measurement. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Occasional |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 258: Delete Growth Measurement Use Cases Specification**

##### **3.3.19.10 View Growth Measurement History** {#3.3.19.10-view-growth-measurement-history}

| UC ID and Name | UC-237 View Growth Measurement History |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | Mother | **Secondary Actors** | None |
| **Trigger** | The Mother selects or initiates View Growth Measurement History from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays the measurement list used as the source for growth charts. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The Mother opens View Growth Measurement History. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Vaccination & Growth Tracking. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 259: View Growth Measurement History Use Cases Specification**

#### ***3.3.20 Expert Consultation & Pricing*** {#3.3.20-expert-consultation-&-pricing}

##### **3.3.20.1 View Expert Consultation Pricing** {#3.3.20.1-view-expert-consultation-pricing}

| UC ID and Name | UC-241 View Expert Consultation Pricing |  |  |
| ----: | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Created** | 12/06/2026 |
| **Primary Actor** | User | **Secondary Actors** | VNPay Payment Gateway |
| **Trigger** | The User selects or initiates View Expert Consultation Pricing from the relevant CareBridge screen or workflow. |  |  |
| **Description** | Displays the currently effective consultation price list before the user confirms booking. |  |  |
| **Preconditions** | PRE-1. The CareBridge system and required external services are available. PRE-2. The actor is on the relevant app or web portal screen. PRE-3. The actor is authenticated and has the required role or permission. PRE-4. Required reference data exists when the use case views, updates, deletes, or processes an existing record. |  |  |
| **Postconditions** | POST-1. The requested operation is completed or a clear result state is shown. POST-2. Related CareBridge records, statuses, or notifications are updated when applicable. POST-3. Sensitive actions are recorded for audit, safety, or privacy review where required. |  |  |
| **Normal Flow** | Step 1\. The User opens View Expert Consultation Pricing. Step 2\. The system validates access, required context, and available data. Step 3\. The actor enters information, selects an item, or confirms the requested action. Step 4\. The system applies the relevant business rules and processes the request. Step 5\. The system displays the result and updates related screens, records, or notifications. |  |  |
| **Alternative Flows** | AF1. The actor cancels the action; the system returns to the previous safe state without unintended changes. AF2. No matching data is available; the system displays an empty state with the next allowed action. AF3. Optional filters, search terms, attachments, or shared context may be added when the screen supports them. |  |  |
| **Exceptions** | E1. Access is denied when the actor is unauthenticated, unauthorized, or outside the permitted data scope. E2. Invalid, missing, expired, or conflicting data is rejected with a field-level or action-level message. E3. External service, network, or server failure is handled with retry guidance and no duplicate unsafe action. |  |  |
| **Priority** | Medium |  |  |
| **Frequency of Use** | Frequent |  |  |
| **Business Rules** | BR-RBAC: users may access only functions allowed by their role and permission scope. BR-CONSULTATION: booking, payment, dispute, refund, and pricing actions must keep an auditable lifecycle state. |  |  |
| **Other Information** | Platform: Mobile App; Source group: Mobile App \- Expert Consultation & Pricing. |  |  |
| **Assumptions** | The actor has legitimate purpose for accessing the function, and CareBridge retains data according to privacy and audit policies. |  |  |

**Table 260: View Expert Consultation Pricing Use Cases Specification**

