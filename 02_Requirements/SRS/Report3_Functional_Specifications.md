## **3\. Functional Specifications**

### **3.1 Shared / Common Use Cases**

#### ***3.1.1 UC-01 Register Account***

| UC ID and Name | UC-01 Register Account |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 03/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Register Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates a Mother, Family or Expert account, captures the initial role, verifies the supported contact channel through OTP and allows OTP resend within the same registration flow. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. Any required registration, recovery, appeal or verification context is valid.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Register Account.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Register Account and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 7: UC-01 Register Account Use Case Specification**

#### ***3.1.2 UC-02 Login***

| UC ID and Name | UC-02 Login |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 03/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Firebase / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Login from a reachable CareBridge screen or system event. |  |  |
| **Description** | Authenticates a supported account by email, phone or Google and routes the user to the role-appropriate workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. Any required registration, recovery, appeal or verification context is valid.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Login.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Login and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 8: UC-02 Login Use Case Specification**

#### ***3.1.3 UC-03 Logout***

| UC ID and Name | UC-03 Logout |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 04/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Logout from a reachable CareBridge screen or system event. |  |  |
| **Description** | Ends the current authenticated session and returns the user to a public or login screen. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Logout.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Logout and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 9: UC-03 Logout Use Case Specification**

#### ***3.1.4 UC-04 Reset Password***

| UC ID and Name | UC-04 Reset Password |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 04/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Reset Password from a reachable CareBridge screen or system event. |  |  |
| **Description** | Requests and completes password recovery using the registered contact channel and a valid time-limited verification proof. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. Any required registration, recovery, appeal or verification context is valid.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Reset Password.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Reset Password and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 10: UC-04 Reset Password Use Case Specification**

#### ***3.1.5 UC-05 Change Password***

| UC ID and Name | UC-05 Change Password |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 05/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Change Password from a reachable CareBridge screen or system event. |  |  |
| **Description** | Changes the signed-in user’s password after validating the current password and password policy. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Change Password.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Change Password and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 11: UC-05 Change Password Use Case Specification**

#### ***3.1.6 UC-06 Manage Account Profile***

| UC ID and Name | UC-06 Manage Account Profile |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 05/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Account Profile from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the user’s supported personal profile and contact information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Account Profile.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Account Profile and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 12: UC-06 Manage Account Profile Use Case Specification**

#### ***3.1.7 UC-07 Manage Notifications***

| UC ID and Name | UC-07 Manage Notifications |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 05/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Notifications from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists notification items, opens notification details and marks individual or eligible notification items as read. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Notifications.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Notifications and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 13: UC-07 Manage Notifications Use Case Specification**

#### ***3.1.8 UC-08 Manage Privacy and Data Permissions***

| UC ID and Name | UC-08 Manage Privacy and Data Permissions |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 06/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Privacy and Data Permissions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews privacy state and grants, reviews or revokes scoped data-sharing permissions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Privacy and Data Permissions.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Privacy and Data Permissions and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 14: UC-08 Manage Privacy and Data Permissions Use Case Specification**

#### ***3.1.9 UC-09 Deactivate or Delete Own Account***

| UC ID and Name | UC-09 Deactivate or Delete Own Account |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 06/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Deactivate or Delete Own Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Requests account deactivation or deletion subject to current retention and audit rules. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Deactivate or Delete Own Account.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Deactivate or Delete Own Account and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 15: UC-09 Deactivate or Delete Own Account Use Case Specification**

#### ***3.1.10 UC-10 Submit Account Lock Appeal***

| UC ID and Name | UC-10 Submit Account Lock Appeal |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 06/07/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Locked User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Submit Account Lock Appeal from a reachable CareBridge screen or system event. |  |  |
| **Description** | Submits an appeal for the current account-lock episode using the supported appeal proof. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. Any required registration, recovery, appeal or verification context is valid.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Submit Account Lock Appeal.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Submit Account Lock Appeal and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 16: UC-10 Submit Account Lock Appeal Use Case Specification**

#### ***3.1.11 UC-11 Browse Community Q\&A***

| UC ID and Name | UC-11 Browse Community Q\&A |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 07/07/2026 |
| **Feature / Group** | MF-04 — Shared / Common Use Cases — Community Q\&A & Moderation | **Platform** | Mobile / Expert Web / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Community Q\&A from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses the approved community feed, opens question details and, for Mother or Family, reviews their own questions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Community Q\&A.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Browse Community Q\&A and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 17: UC-11 Browse Community Q\&A Use Case Specification**

#### ***3.1.12 UC-12 Answer Community Questions as Verified Expert***

| UC ID and Name | UC-12 Answer Community Questions as Verified Expert |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 07/07/2026 |
| **Feature / Group** | MF-04 — Shared / Common Use Cases — Community Q\&A & Moderation | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Verified Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Answer Community Questions as Verified Expert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews the expert question queue and creates, updates or removes the expert’s answer with the current verified-expert label. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Answer Community Questions as Verified Expert.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Answer Community Questions as Verified Expert and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 18: UC-12 Answer Community Questions as Verified Expert Use Case Specification**

#### ***3.1.13 UC-13 Register and Submit Expert Application***

| UC ID and Name | UC-13 Register and Submit Expert Application |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 08/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert Applicant | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Register and Submit Expert Application from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes expert onboarding, creates the professional application and submits required identity and verification evidence. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. Any required registration, recovery, appeal or verification context is valid.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Register and Submit Expert Application.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Register and Submit Expert Application and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 19: UC-13 Register and Submit Expert Application Use Case Specification**

#### ***3.1.14 UC-14 Manage Expert Professional Profile***

| UC ID and Name | UC-14 Manage Expert Professional Profile |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 08/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Expert Professional Profile from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the professional profile, manages credentials and documents and reviews or renews verification status. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Expert Professional Profile.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Expert Professional Profile and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 20: UC-14 Manage Expert Professional Profile Use Case Specification**

#### ***3.1.15 UC-15 Manage Expert Availability***

| UC ID and Name | UC-15 Manage Expert Availability |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 08/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Expert Availability from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the calendar, creates or removes availability slots and changes supported online availability state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Expert Availability.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Expert Availability and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 21: UC-15 Manage Expert Availability Use Case Specification**

#### ***3.1.16 UC-16 Process Expert Conversation Requests***

| UC ID and Name | UC-16 Process Expert Conversation Requests |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 09/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Process Expert Conversation Requests from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens assigned requests and accepts or rejects a request; acceptance creates or links the direct conversation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Process Expert Conversation Requests.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Process Expert Conversation Requests and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 22: UC-16 Process Expert Conversation Requests Use Case Specification**

#### ***3.1.17 UC-17 Use Direct Expert Chat***

| UC ID and Name | UC-17 Use Direct Expert Chat |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 09/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Expert Web / File Storage / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Use Direct Expert Chat from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens direct conversations and exchanges text, image or document messages, including attachment viewing and eligible recall. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Use Direct Expert Chat.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Use Direct Expert Chat and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 23: UC-17 Use Direct Expert Chat Use Case Specification**

#### ***3.1.18 UC-18 Conduct Direct Voice or Video Call***

| UC ID and Name | UC-18 Conduct Direct Voice or Video Call |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 09/07/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Expert Web / ZegoCloud / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Conduct Direct Voice or Video Call from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts, answers, declines and ends an authorized voice or video call inside an accepted direct conversation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Conduct Direct Voice or Video Call.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Conduct Direct Voice or Video Call and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 24: UC-18 Conduct Direct Voice or Video Call Use Case Specification**

### **3.2 Mobile App Use Cases**

#### ***3.2.1 UC-19 Manage Own Login Sessions***

| UC ID and Name | UC-19 Manage Own Login Sessions |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 10/07/2026 |
| **Feature / Group** | MF-01 — Mobile App Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Own Login Sessions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the user’s login sessions and revokes a selected session when supported. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Own Login Sessions.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Own Login Sessions and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 25: UC-19 Manage Own Login Sessions Use Case Specification**

#### ***3.2.2 UC-20 Manage Mother Journey***

| UC ID and Name | UC-20 Manage Mother Journey |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 10/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Mother Journey from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates and updates a preconception, pregnancy or postpartum journey, records stage transitions or pregnancy outcome and views the journey dashboard. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Mother Journey.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Mother Journey and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 26: UC-20 Manage Mother Journey Use Case Specification**

#### ***3.2.3 UC-21 Manage Maternal Health Metrics***

| UC ID and Name | UC-21 Manage Maternal Health Metrics |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 11/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Maternal Health Metrics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Adds, views, updates and removes supported maternal measurements and reviews their detail and trend. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Maternal Health Metrics.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Maternal Health Metrics and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 27: UC-21 Manage Maternal Health Metrics Use Case Specification**

#### ***3.2.4 UC-22 Manage Postpartum Logs***

| UC ID and Name | UC-22 Manage Postpartum Logs |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 11/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Postpartum Logs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Adds, views, updates and deletes postpartum recovery observations. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Postpartum Logs.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Postpartum Logs and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 28: UC-22 Manage Postpartum Logs Use Case Specification**

#### ***3.2.5 UC-23 Manage Maternal Health Records***

| UC ID and Name | UC-23 Manage Maternal Health Records |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 11/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / File Storage / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Maternal Health Records from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and archives maternal health records and manages their protected attachments. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Maternal Health Records.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Maternal Health Records and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 29: UC-23 Manage Maternal Health Records Use Case Specification**

#### ***3.2.6 UC-24 Manage Appointments***

| UC ID and Name | UC-24 Manage Appointments |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 12/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Appointments from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes appointments through the appointment calendar and detail screens. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Appointments.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Appointments and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 30: UC-24 Manage Appointments Use Case Specification**

#### ***3.2.7 UC-25 Manage Reminders and Schedules***

| UC ID and Name | UC-25 Manage Reminders and Schedules |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 12/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Reminders and Schedules from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates medication and other reminders, manages recurrence schedules and performs snooze, complete, skip, enable or delete actions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Reminders and Schedules.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Reminders and Schedules and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 31: UC-25 Manage Reminders and Schedules Use Case Specification**

#### ***3.2.8 UC-26 View Today Care Tasks***

| UC ID and Name | UC-26 View Today Care Tasks |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 12/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Today Care Tasks from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays today’s reminders, appointments and current care tasks from the implemented home/task view. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Today Care Tasks.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Today Care Tasks and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 32: UC-26 View Today Care Tasks Use Case Specification**

#### ***3.2.9 UC-27 Browse Pregnancy Exercises***

| UC ID and Name | UC-27 Browse Pregnancy Exercises |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 13/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Pregnancy Exercises from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses reviewed pregnancy exercises and opens exercise instructions and safety information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Pregnancy Exercises.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Browse Pregnancy Exercises and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 33: UC-27 Browse Pregnancy Exercises Use Case Specification**

#### ***3.2.10 UC-28 Complete Pre-exercise Safety Check***

| UC ID and Name | UC-28 Complete Pre-exercise Safety Check |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 13/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Complete Pre-exercise Safety Check from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes the required safety questions before an exercise session and stops entry when a configured warning condition applies. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Complete Pre-exercise Safety Check.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Complete Pre-exercise Safety Check and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 34: UC-28 Complete Pre-exercise Safety Check Use Case Specification**

#### ***3.2.11 UC-29 Perform Camera-guided Exercise Session***

| UC ID and Name | UC-29 Perform Camera-guided Exercise Session |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 14/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Camera / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Perform Camera-guided Exercise Session from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts, pauses, resumes and completes an exercise session with optional camera-based real-time posture analysis after permission. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Perform Camera-guided Exercise Session.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Perform Camera-guided Exercise Session and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 35: UC-29 Perform Camera-guided Exercise Session Use Case Specification**

#### ***3.2.12 UC-30 View Exercise History and Results***

| UC ID and Name | UC-30 View Exercise History and Results |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 14/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Exercise History and Results from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views completed exercise sessions, posture feedback and the recorded session result. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Exercise History and Results.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Exercise History and Results and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 36: UC-30 View Exercise History and Results Use Case Specification**

#### ***3.2.13 UC-31 Record Quick Health Notes***

| UC ID and Name | UC-31 Record Quick Health Notes |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 14/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Record Quick Health Notes from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records supported BMI, hydration, mood and fetal-movement quick notes. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Record Quick Health Notes.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Record Quick Health Notes and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 37: UC-31 Record Quick Health Notes Use Case Specification**

#### ***3.2.14 UC-32 Manage EPDS Screening***

| UC ID and Name | UC-32 Manage EPDS Screening |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 15/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage EPDS Screening from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes EPDS screening, receives the supported safety response and reviews previous screening results. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage EPDS Screening.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage EPDS Screening and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 38: UC-32 Manage EPDS Screening Use Case Specification**

#### ***3.2.15 UC-33 View Personalized Care Recommendations***

| UC ID and Name | UC-33 View Personalized Care Recommendations |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 15/07/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Personalized Care Recommendations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays current stage-based care recommendations produced by the implemented recommendation flow. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Personalized Care Recommendations.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Personalized Care Recommendations and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 39: UC-33 View Personalized Care Recommendations Use Case Specification**

#### ***3.2.16 UC-34 Manage Baby Profiles***

| UC ID and Name | UC-34 Manage Baby Profiles |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 15/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Profiles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, switches and archives baby profiles while preserving linked care history. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Profiles.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Baby Profiles and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 40: UC-34 Manage Baby Profiles Use Case Specification**

#### ***3.2.17 UC-35 Manage Baby Daily Logs***

| UC ID and Name | UC-35 Manage Baby Daily Logs |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 16/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Daily Logs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes feeding, sleep and diaper observations and reviews their summary. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Daily Logs.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Baby Daily Logs and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 41: UC-35 Manage Baby Daily Logs Use Case Specification**

#### ***3.2.18 UC-36 Manage Development Milestones***

| UC ID and Name | UC-36 Manage Development Milestones |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 16/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Development Milestones from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records, reviews, updates and deletes caregiver-observed development milestones. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Development Milestones.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Development Milestones and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 42: UC-36 Manage Development Milestones Use Case Specification**

#### ***3.2.19 UC-37 Manage Baby Growth***

| UC ID and Name | UC-37 Manage Baby Growth |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 17/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Growth from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records, updates and deletes growth measurements and reviews measurement history and growth charts. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Growth.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Baby Growth and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 43: UC-37 Manage Baby Growth Use Case Specification**

#### ***3.2.20 UC-38 Manage Vaccination Journey***

| UC ID and Name | UC-38 Manage Vaccination Journey |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 17/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Vaccination Journey from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the vaccination schedule, manages vaccination records, completes or postpones a dose and creates related reminders. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Vaccination Journey.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Vaccination Journey and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 44: UC-38 Manage Vaccination Journey Use Case Specification**

#### ***3.2.21 UC-39 Manage Baby Health Records***

| UC ID and Name | UC-39 Manage Baby Health Records |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 17/07/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Health Records from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and archives health records scoped to the selected baby. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Health Records.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Baby Health Records and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 45: UC-39 Manage Baby Health Records Use Case Specification**

#### ***3.2.22 UC-40 Manage Community Questions***

| UC ID and Name | UC-40 Manage Community Questions |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 18/07/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q\&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Questions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, edits and deletes the actor’s own community questions under moderation rules. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Questions.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Community Questions and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 46: UC-40 Manage Community Questions Use Case Specification**

#### ***3.2.23 UC-41 Manage Community Answers***

| UC ID and Name | UC-41 Manage Community Answers |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 18/07/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q\&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Answers from a reachable CareBridge screen or system event. |  |  |
| **Description** | Posts, edits and deletes the actor’s own community answers as personal community contributions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Answers.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Community Answers and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 47: UC-41 Manage Community Answers Use Case Specification**

#### ***3.2.24 UC-42 Report Community Content or Account***

| UC ID and Name | UC-42 Report Community Content or Account |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 18/07/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q\&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Report Community Content or Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Submits a report against supported community content or an account with a reason and evidence allowed by the form. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Report Community Content or Account.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Report Community Content or Account and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 48: UC-42 Report Community Content or Account Use Case Specification**

#### ***3.2.25 UC-43 Browse Expert Directory***

| UC ID and Name | UC-43 Browse Expert Directory |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 19/07/2026 |
| **Feature / Group** | MF-05 — Mobile App Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Expert Directory from a reachable CareBridge screen or system event. |  |  |
| **Description** | Searches the verified expert directory and opens an expert’s public professional profile. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Expert Directory.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Browse Expert Directory and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 49: UC-43 Browse Expert Directory Use Case Specification**

#### ***3.2.26 UC-44 Manage Own Expert Conversation Requests***

| UC ID and Name | UC-44 Manage Own Expert Conversation Requests |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 19/07/2026 |
| **Feature / Group** | MF-05 — Mobile App Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Own Expert Conversation Requests from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views and cancels the actor’s free conversation requests to an expert. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Own Expert Conversation Requests.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Own Expert Conversation Requests and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 50: UC-44 Manage Own Expert Conversation Requests Use Case Specification**

#### ***3.2.27 UC-45 Use AI Nurse Symptom Triage***

| UC ID and Name | UC-45 Use AI Nurse Symptom Triage |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 20/07/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / AI Service / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Use AI Nurse Symptom Triage from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts structured symptom intake, supplies follow-up information and receives a non-diagnostic risk result and next-step guidance. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Use AI Nurse Symptom Triage.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Use AI Nurse Symptom Triage and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 51: UC-45 Use AI Nurse Symptom Triage Use Case Specification**

#### ***3.2.28 UC-46 View AI Triage History***

| UC ID and Name | UC-46 View AI Triage History |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 20/07/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View AI Triage History from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews previous AI triage sessions within the current permission scope. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View AI Triage History.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View AI Triage History and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 52: UC-46 View AI Triage History Use Case Specification**

#### ***3.2.29 UC-47 Escalate AI Triage to Emergency Support***

| UC ID and Name | UC-47 Escalate AI Triage to Emergency Support |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 20/07/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Escalate AI Triage to Emergency Support from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the emergency support flow when the triage result or red-flag handling requires urgent action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Escalate AI Triage to Emergency Support.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Escalate AI Triage to Emergency Support and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 53: UC-47 Escalate AI Triage to Emergency Support Use Case Specification**

#### ***3.2.30 UC-48 Request Expert Support from AI Triage***

| UC ID and Name | UC-48 Request Expert Support from AI Triage |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 21/07/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Request Expert Support from AI Triage from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates a free expert conversation request using the supported context from an AI triage session. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Request Expert Support from AI Triage.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Request Expert Support from AI Triage and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 54: UC-48 Request Expert Support from AI Triage Use Case Specification**

#### ***3.2.31 UC-49 Find Nearby Care Facility***

| UC ID and Name | UC-49 Find Nearby Care Facility |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 21/07/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / TrackAsia / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Find Nearby Care Facility from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the emergency map, searches nearby facilities and reviews facility details, distance, route and ETA. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Find Nearby Care Facility.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Find Nearby Care Facility and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 55: UC-49 Find Nearby Care Facility Use Case Specification**

#### ***3.2.32 UC-50 Call or Navigate to Care Facility***

| UC ID and Name | UC-50 Call or Navigate to Care Facility |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 22/07/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / TrackAsia / Device Services |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Call or Navigate to Care Facility from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts a device call to a selected facility or opens navigation for its location. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Call or Navigate to Care Facility.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Call or Navigate to Care Facility and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 56: UC-50 Call or Navigate to Care Facility Use Case Specification**

#### ***3.2.33 UC-51 Call Emergency Number 115***

| UC ID and Name | UC-51 Call Emergency Number 115 |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 22/07/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Device Dialer |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Call Emergency Number 115 from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts a device call to emergency number 115 after the actor confirms the action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Call Emergency Number 115\.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Call Emergency Number 115 and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 57: UC-51 Call Emergency Number 115 Use Case Specification**

#### ***3.2.34 UC-52 Alert Family During Emergency***

| UC ID and Name | UC-52 Alert Family During Emergency |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 22/07/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Alert Family During Emergency from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends the implemented family emergency alert with only the authorized context and location state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Alert Family During Emergency.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Alert Family During Emergency and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 58: UC-52 Alert Family During Emergency Use Case Specification**

#### ***3.2.35 UC-53 View Emergency or Family Alert***

| UC ID and Name | UC-53 View Emergency or Family Alert |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 23/07/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Emergency or Family Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the detail of an emergency or family alert available to the current actor. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Emergency or Family Alert.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Emergency or Family Alert and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 59: UC-53 View Emergency or Family Alert Use Case Specification**

#### ***3.2.36 UC-54 Manage Care Groups***

| UC ID and Name | UC-54 Manage Care Groups |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 23/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Groups from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes the Mother’s care groups; member invitation, permission and membership actions remain in their dedicated cooperative-care use cases. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Groups.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Care Groups and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 60: UC-54 Manage Care Groups Use Case Specification**

#### ***3.2.37 UC-55 Manage Care Group Invitations***

| UC ID and Name | UC-55 Manage Care Group Invitations |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 23/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Group Invitations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends, lists and revokes invitations and allows the invited Family member to accept or reject them. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Group Invitations.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Care Group Invitations and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 61: UC-55 Manage Care Group Invitations Use Case Specification**

#### ***3.2.38 UC-56 Manage Family Permissions***

| UC ID and Name | UC-56 Manage Family Permissions |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 24/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Family Permissions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Grants and changes the exact care-data categories a Family member may access. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Family Permissions.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Family Permissions and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 62: UC-56 Manage Family Permissions Use Case Specification**

#### ***3.2.39 UC-57 View Shared Care Data***

| UC ID and Name | UC-57 View Shared Care Data |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 24/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Shared Care Data from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views only the maternal data, quick-note history and other care information explicitly shared with the Family member. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Shared Care Data.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Shared Care Data and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 63: UC-57 View Shared Care Data Use Case Specification**

#### ***3.2.40 UC-58 View Shared Care Calendar***

| UC ID and Name | UC-58 View Shared Care Calendar |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 25/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Shared Care Calendar from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays appointments and supported care events shared within an active care group. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Shared Care Calendar.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Shared Care Calendar and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 64: UC-58 View Shared Care Calendar Use Case Specification**

#### ***3.2.41 UC-59 Manage Cooperative Care Tasks***

| UC ID and Name | UC-59 Manage Cooperative Care Tasks |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 25/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Cooperative Care Tasks from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, cancels and completes assigned care tasks according to care-group role. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Cooperative Care Tasks.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Cooperative Care Tasks and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 65: UC-59 Manage Cooperative Care Tasks Use Case Specification**

#### ***3.2.42 UC-60 Manage Care Group Membership***

| UC ID and Name | UC-60 Manage Care Group Membership |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 25/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Group Membership from a reachable CareBridge screen or system event. |  |  |
| **Description** | Allows the Mother to remove a member and an active Family member to leave a care group. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Group Membership.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Care Group Membership and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 66: UC-60 Manage Care Group Membership Use Case Specification**

#### ***3.2.43 UC-61 View Family Alerts***

| UC ID and Name | UC-61 View Family Alerts |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 26/07/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Family Alerts from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens safety or emergency alerts shared with the Family member. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Family Alerts.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Family Alerts and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 67: UC-61 View Family Alerts Use Case Specification**

#### ***3.2.44 UC-62 Browse Verified Content and FAQ***

| UC ID and Name | UC-62 Browse Verified Content and FAQ |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 26/07/2026 |
| **Feature / Group** | MF-09 — Mobile App Use Cases — Verified Content & Checklist Hub | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Verified Content and FAQ from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses, searches and opens approved stage- and topic-based articles and FAQs. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Verified Content and FAQ.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Browse Verified Content and FAQ and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 68: UC-62 Browse Verified Content and FAQ Use Case Specification**

#### ***3.2.45 UC-63 Manage Personal Care Checklist***

| UC ID and Name | UC-63 Manage Personal Care Checklist |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 26/07/2026 |
| **Feature / Group** | MF-09 — Mobile App Use Cases — Verified Content & Checklist Hub | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Personal Care Checklist from a reachable CareBridge screen or system event. |  |  |
| **Description** | Imports a template and creates, views, updates, deletes, completes or reopens personal checklist tasks and reviews history. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Personal Care Checklist.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Personal Care Checklist and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 69: UC-63 Manage Personal Care Checklist Use Case Specification**

#### ***3.2.46 UC-64 Manage Safety Monitoring Settings***

| UC ID and Name | UC-64 Manage Safety Monitoring Settings |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 27/07/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Safety Monitoring Settings from a reachable CareBridge screen or system event. |  |  |
| **Description** | Configures, enables and disables phone-IMU safety monitoring and its authorized alert behavior. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Safety Monitoring Settings.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Safety Monitoring Settings and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 70: UC-64 Manage Safety Monitoring Settings Use Case Specification**

#### ***3.2.47 UC-65 Respond to Suspected Fall or Impact***

| UC ID and Name | UC-65 Respond to Suspected Fall or Impact |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 27/07/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother / Phone Motion Sensors | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Respond to Suspected Fall or Impact from a reachable CareBridge screen or system event. |  |  |
| **Description** | Detects a suspected fall or impact and lets the Mother confirm safety or request help within the response window. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Respond to Suspected Fall or Impact.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Respond to Suspected Fall or Impact and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 71: UC-65 Respond to Suspected Fall or Impact Use Case Specification**

#### ***3.2.48 UC-66 Send Safety Emergency Alert***

| UC ID and Name | UC-66 Send Safety Emergency Alert |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 28/07/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Send Safety Emergency Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends an emergency alert to configured authorized contacts when the safety flow requires escalation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Send Safety Emergency Alert.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Send Safety Emergency Alert and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 72: UC-66 Send Safety Emergency Alert Use Case Specification**

#### ***3.2.49 UC-67 Review Safety Events and Report False Positive***

| UC ID and Name | UC-67 Review Safety Events and Report False Positive |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 28/07/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Safety Events and Report False Positive from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews safety-event history and marks an eligible detection as a false positive for monitoring feedback. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Safety Events and Report False Positive.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review Safety Events and Report False Positive and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 73: UC-67 Review Safety Events and Report False Positive Use Case Specification**

#### ***3.2.50 UC-68 Open Emergency Support from Safety Alert***

| UC ID and Name | UC-68 Open Emergency Support from Safety Alert |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 28/07/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Open Emergency Support from Safety Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens emergency map, call and family-support actions from an active safety alert. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Open Emergency Support from Safety Alert.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Open Emergency Support from Safety Alert and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 74: UC-68 Open Emergency Support from Safety Alert Use Case Specification**

### **3.3 Web App Use Cases**

#### ***3.3.1 UC-69 View Admin Dashboard***

| UC ID and Name | UC-69 View Admin Dashboard |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 29/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Admin Dashboard from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays current administrative account, expert, content and security summary information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Admin Dashboard.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Admin Dashboard and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 75: UC-69 View Admin Dashboard Use Case Specification**

#### ***3.3.2 UC-70 Manage User Accounts and Roles***

| UC ID and Name | UC-70 Manage User Accounts and Roles |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 29/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage User Accounts and Roles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists, searches and opens users, reviews activity and changes supported account status, role or access state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage User Accounts and Roles.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage User Accounts and Roles and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 76: UC-70 Manage User Accounts and Roles Use Case Specification**

#### ***3.3.3 UC-71 Create Staff Account***

| UC ID and Name | UC-71 Create Staff Account |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 29/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Create Staff Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates an authorized Moderator, Content Admin or other supported staff account. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Create Staff Account.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Create Staff Account and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 77: UC-71 Create Staff Account Use Case Specification**

#### ***3.3.4 UC-72 Review Account Lock Appeals***

| UC ID and Name | UC-72 Review Account Lock Appeals |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 30/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Account Lock Appeals from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens lock appeals and records an approve or reject decision. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Account Lock Appeals.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review Account Lock Appeals and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 78: UC-72 Review Account Lock Appeals Use Case Specification**

#### ***3.3.5 UC-73 Review Audit and Security Operations***

| UC ID and Name | UC-73 Review Audit and Security Operations |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 30/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Audit and Security Operations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews audit logs, security events and incident timelines, adds investigation notes and resolves supported incidents. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Audit and Security Operations.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review Audit and Security Operations and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 79: UC-73 Review Audit and Security Operations Use Case Specification**

#### ***3.3.6 UC-74 Manage System Configuration***

| UC ID and Name | UC-74 Manage CareBridge System Configuration |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 31/07/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage CareBridge System Configuration from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the supported runtime configuration exposed by the CareBridge System Configuration workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage CareBridge System Configuration.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage CareBridge System Configuration and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 80: UC-74 Manage System Configuration Use Case Specification**

#### ***3.3.7 UC-75 View Moderator Dashboard***

| UC ID and Name | UC-75 View Moderator Dashboard |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 31/07/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Moderator Dashboard from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays moderation workload, community and handling summaries. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Moderator Dashboard.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Moderator Dashboard and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 81: UC-75 View Moderator Dashboard Use Case Specification**

#### ***3.3.8 UC-76 Review Pending Community Content***

| UC ID and Name | UC-76 Review Pending Community Content |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 31/07/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Pending Community Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens pending community questions or answers for moderation review. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Pending Community Content.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review Pending Community Content and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 82: UC-76 Review Pending Community Content Use Case Specification**

#### ***3.3.9 UC-77 Monitor Published Community Content***

| UC ID and Name | UC-77 Monitor Published Community Content |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 01/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Monitor Published Community Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses visible community content and opens a selected item for moderation inspection. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Monitor Published Community Content.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Monitor Published Community Content and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 83: UC-77 Monitor Published Community Content Use Case Specification**

#### ***3.3.10 UC-78 Manage Community Reports***

| UC ID and Name | UC-78 Manage Community Reports |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 01/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Reports from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists, claims, releases and opens content/account reports, reviews related evidence and resolves the report. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Reports.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Community Reports and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 84: UC-78 Manage Community Reports Use Case Specification**

#### ***3.3.11 UC-79 Apply and Review Moderation Actions***

| UC ID and Name | UC-79 Apply and Review Moderation Actions |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 01/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Apply and Review Moderation Actions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Applies supported content or account actions, reviews violation history and undoes an eligible moderation action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Apply and Review Moderation Actions.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Apply and Review Moderation Actions and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 85: UC-79 Apply and Review Moderation Actions Use Case Specification**

#### ***3.3.12 UC-80 Review AI Moderation Assessment***

| UC ID and Name | UC-80 Review AI Moderation Assessment |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 02/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review AI Moderation Assessment from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views AI moderation assessment details and submits moderator feedback where implemented. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review AI Moderation Assessment.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review AI Moderation Assessment and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 86: UC-80 Review AI Moderation Assessment Use Case Specification**

#### ***3.3.13 UC-81 Manage Community Topics***

| UC ID and Name | UC-81 Manage Community Topics |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 02/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q\&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Topics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, updates, orders and hides community topics exposed by the moderation workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Topics.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Community Topics and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 87: UC-81 Manage Community Topics Use Case Specification**

#### ***3.3.14 UC-82 Review Expert Applications and Trust***

| UC ID and Name | UC-82 Review Expert Applications and Trust |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 03/08/2026 |
| **Feature / Group** | MF-05 — Web App Use Cases — Verified Expert Network & Contribution | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Expert Applications and Trust from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists expert cases, opens submitted evidence, approves or rejects verification and changes supported expert trust status. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Expert Applications and Trust.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review Expert Applications and Trust and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High |  |  |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 88: UC-82 Review Expert Applications and Trust Use Case Specification**

#### ***3.3.15 Reserved***

The use-case identifier and table number are intentionally reserved after retirement of an unsupported administration capability.

**Table 89: Reserved Use Case Identifier**

#### ***3.3.16 UC-84 View Content Administration Workspace***

| UC ID and Name | UC-84 View Content Administration Workspace |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 03/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Content Administration Workspace from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays the Content Admin dashboard and browses the content library and its status filters. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Content Administration Workspace.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for View Content Administration Workspace and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 90: UC-84 View Content Administration Workspace Use Case Specification**

#### ***3.3.17 UC-85 Manage Verified Articles***

| UC ID and Name | UC-85 Manage Verified Articles |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | BachNQ | **Date Revised** | 04/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Verified Articles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, versions, archives or unpublishes verified articles through the implemented lifecycle. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Verified Articles.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Verified Articles and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 91: UC-85 Manage Verified Articles Use Case Specification**

#### ***3.3.18 UC-86 Manage Verified FAQs***

| UC ID and Name | UC-86 Manage Verified FAQs |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 04/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Verified FAQs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, versions, archives or unpublishes verified FAQ entries through the implemented lifecycle. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Verified FAQs.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Verified FAQs and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 92: UC-86 Manage Verified FAQs Use Case Specification**

#### ***3.3.19 UC-87 Manage Content Topics***

| UC ID and Name | UC-87 Manage Content Topics |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | HuyND | **Date Revised** | 04/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Content Topics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, updates, orders and changes visibility of article and FAQ topics. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Content Topics.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Content Topics and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 93: UC-87 Manage Content Topics Use Case Specification**

#### ***3.3.20 UC-88 Review and Approve Content***

| UC ID and Name | UC-88 Review and Approve Content |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 05/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review and Approve Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists pending content versions, opens the submitted version and records the supported approval decision. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review and Approve Content.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Review and Approve Content and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 94: UC-88 Review and Approve Content Use Case Specification**

#### ***3.3.21 UC-89 Manage Checklist Templates***

| UC ID and Name | UC-89 Manage Checklist Templates |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | ChuongBD | **Date Revised** | 05/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Admin Web / Backend |
| **Primary Actor** | Content Admin / CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Checklist Templates from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, clones, reviews, approves and archives checklist template versions according to role. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Checklist Templates.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Checklist Templates and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 95: UC-89 Manage Checklist Templates Use Case Specification**

#### ***3.3.22 UC-90 Manage Pregnancy Exercise Content***

| UC ID and Name | UC-90 Manage Pregnancy Exercise Content |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | PhuongNT | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Pregnancy Exercise Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, previews, updates and changes supported publication state for pregnancy exercise content. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Pregnancy Exercise Content.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Pregnancy Exercise Content and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 96: UC-90 Manage Pregnancy Exercise Content Use Case Specification**

#### ***3.3.23 UC-91 Manage Exercise Posture Configuration***

| UC ID and Name | UC-91 Manage Exercise Posture Configuration |  |  |
| :---- | :---- | :---- | :---- |
| **Created By** | LamVH | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Admin Web / Backend |
| **Primary Actor** | CareBridge System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Exercise Posture Configuration from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views and updates posture-analysis configuration associated with supported exercises. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available.PRE-2. The actor can reach the relevant screen or trigger.PRE-3. The actor is authenticated and has the required role, ownership, membership or permission.PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed.POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed.POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Exercise Posture Configuration.Step 2\. The system loads the permitted context and validates access.Step 3\. The actor views data or provides and confirms the supported input.Step 4\. The Backend applies the business rules for Manage Exercise Posture Configuration and performs the requested operation.Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal.AF2. The actor cancels before confirmation and no unintended change occurs.AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data.E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback.E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium |  |  |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 97: UC-91 Manage Exercise Posture Configuration Use Case Specification**
