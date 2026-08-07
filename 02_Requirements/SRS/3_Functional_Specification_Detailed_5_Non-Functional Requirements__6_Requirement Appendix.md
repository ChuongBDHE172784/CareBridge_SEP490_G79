# CareBridge Release 1 — Functional Specifications, Non-Functional Requirements and Requirement Appendix

| Field | Value |
| --- | --- |
| Document Status | Draft |
| Release Scope | MF-01 through MF-10 |
| Requirement Baseline | `Report1_Project Introduction_new.md`, refined by the user-approved current feature inventory dated 06/08/2026 |
| Validation Basis | User-approved feature inventory and behavior confirmed in the current Mobile, Web and Backend implementation |
| Use Case Baseline | 91 business-goal use cases; CRUD operations and technical variants remain steps inside their owning use case |
| Conflict Resolution | The user-approved inventory overrides stale identity/gamification wording. Free expert request, direct chat, attachment and voice/video flows remain active; paid commerce remains excluded. |

**Table 1: Document Control**

## **3. Functional Specifications** {#3-functional-specifications}

### **3.1 Scope and Numbering Rules** {#3.1-scope-and-numbering-rules}

| Rule | Decision |
| --- | --- |
| FS-01 | One UC represents a meaningful actor goal and observable business outcome, not one screen button, endpoint or HTTP method. |
| FS-02 | Register, Login, Reset Password and Change Password remain separate goals. Email/phone/Google login, OTP verification and OTP resend are embedded flows, not separate UCs. |
| FS-03 | Create, view, update, delete, archive, search, filter, pagination and confirmation for the same managed domain object are normally consolidated into one Manage/Browse UC. |
| FS-04 | A separate UC is retained when the actor, authorization boundary, safety outcome, approval decision or cross-system result is materially different. |
| FS-05 | A source file or API endpoint alone is not an active UC; the function must be reachable through the current Mobile/Web flow. |
| FS-06 | Placeholder routes, orphan screens, paid consultation commerce, partner marketplace, nearby-expert request, expert gamification and connected health devices remain excluded. |
| FS-07 | Free expert requests, accepted direct chat, image/document messages and voice/video calls are active MF-05 scope. Expert Web rejection currently has a POST/PATCH contract gap recorded as an implementation issue. |

**Table 2: Functional Specification Scope Rules**

### **3.2 Use Case Grouping Model** {#3.2-use-case-grouping-model}

| Group | Scope | UC Count |
| --- | --- | ---: |
| Shared / Common Use Cases | Business outcomes implemented across both Mobile and Web clients or shared across their role workspaces. | 18 |
| Mobile App Use Cases | Mother, Family and mobile Expert outcomes primarily initiated from the CareBridge Mobile App. | 50 |
| Web App Use Cases | System Admin, Moderator, Content Admin and Expert Portal outcomes primarily initiated from the CareBridge Web App. | 23 |

Each use case belongs to exactly one application group and retains its owning MF code. A use case implemented by both clients is specified once in the Shared/Common group rather than duplicated for each client.

**Table 3: Use Case Grouping Model**

### **3.3 Release 1 Use Case Catalogue** {#3.3-release-1-use-case-catalogue}

#### **3.3.1 Shared / Common Use Cases** {#3.3.1-shared-common-use-cases}

| UC ID | Major Feature | Use Case Name | Primary Actor | Platform |
| --- | --- | --- | --- | --- |
| UC-01 | MF-01 | Register Account | Guest | Mobile / Web / Backend |
| UC-02 | MF-01 | Login | Guest | Mobile / Web / Firebase / Backend |
| UC-03 | MF-01 | Logout | Authenticated User | Mobile / Web / Backend |
| UC-04 | MF-01 | Reset Password | Guest | Mobile / Web / Backend |
| UC-05 | MF-01 | Change Password | Authenticated User | Mobile / Web / Backend |
| UC-06 | MF-01 | Manage Account Profile | Authenticated User | Mobile / Web / Backend |
| UC-07 | MF-01 | Manage Notifications | Authenticated User | Mobile / Web / Backend |
| UC-08 | MF-01 | Manage Privacy and Data Permissions | Authenticated User | Mobile / Web / Backend |
| UC-09 | MF-01 | Deactivate or Delete Own Account | Authenticated User | Mobile / Web / Backend |
| UC-10 | MF-01 | Submit Account Lock Appeal | Locked User | Mobile / Web / Backend |
| UC-11 | MF-04 | Browse Community Q&A | Mother / Family / Expert | Mobile / Expert Web / Backend |
| UC-12 | MF-04 | Answer Community Questions as Verified Expert | Verified Expert | Expert Mobile / Expert Web / Backend |
| UC-13 | MF-05 | Register and Submit Expert Application | Expert Applicant | Expert Mobile / Expert Web / Backend |
| UC-14 | MF-05 | Manage Expert Professional Profile | Expert | Expert Mobile / Expert Web / Backend |
| UC-15 | MF-05 | Manage Expert Availability | Expert | Expert Mobile / Expert Web / Backend |
| UC-16 | MF-05 | Process Expert Conversation Requests | Expert | Expert Mobile / Expert Web / Backend |
| UC-17 | MF-05 | Use Direct Expert Chat | Mother / Family / Expert | Mobile / Expert Web / File Storage / Backend |
| UC-18 | MF-05 | Conduct Direct Voice or Video Call | Mother / Family / Expert | Mobile / Expert Web / ZegoCloud / Backend |

**Table 4: Shared / Common Use Cases Catalogue**

#### **3.3.2 Mobile App Use Cases** {#3.3.2-mobile-app-use-cases}

| UC ID | Major Feature | Use Case Name | Primary Actor | Platform |
| --- | --- | --- | --- | --- |
| UC-19 | MF-01 | Manage Own Login Sessions | Authenticated User | Mobile / Backend |
| UC-20 | MF-02 | Manage Mother Journey | Mother | Mother Mobile App / Backend |
| UC-21 | MF-02 | Manage Maternal Health Metrics | Mother | Mother Mobile App / Backend |
| UC-22 | MF-02 | Manage Postpartum Logs | Mother | Mother Mobile App / Backend |
| UC-23 | MF-02 | Manage Maternal Health Records | Mother | Mother Mobile App / File Storage / Backend |
| UC-24 | MF-02 | Manage Appointments | Mother | Mother Mobile App / Backend |
| UC-25 | MF-02 | Manage Reminders and Schedules | Mother | Mother Mobile App / Backend |
| UC-26 | MF-02 | View Today Care Tasks | Mother | Mother Mobile App / Backend |
| UC-27 | MF-02 | Browse Pregnancy Exercises | Mother | Mother Mobile App / Backend |
| UC-28 | MF-02 | Complete Pre-exercise Safety Check | Mother | Mother Mobile App / Backend |
| UC-29 | MF-02 | Perform Camera-guided Exercise Session | Mother | Mother Mobile App / Camera / Backend |
| UC-30 | MF-02 | View Exercise History and Results | Mother | Mother Mobile App / Backend |
| UC-31 | MF-02 | Record Quick Health Notes | Mother | Mother Mobile App / Backend |
| UC-32 | MF-02 | Manage EPDS Screening | Mother | Mother Mobile App / Backend |
| UC-33 | MF-02 | View Personalized Care Recommendations | Mother | Mother Mobile App / Backend |
| UC-34 | MF-03 | Manage Baby Profiles | Mother | Mother Mobile App / Backend |
| UC-35 | MF-03 | Manage Baby Daily Logs | Mother | Mother Mobile App / Backend |
| UC-36 | MF-03 | Manage Development Milestones | Mother | Mother Mobile App / Backend |
| UC-37 | MF-03 | Manage Baby Growth | Mother | Mother Mobile App / Backend |
| UC-38 | MF-03 | Manage Vaccination Journey | Mother | Mother Mobile App / Backend |
| UC-39 | MF-03 | Manage Baby Health Records | Mother | Mother Mobile App / Backend |
| UC-40 | MF-04 | Manage Community Questions | Mother / Family | Mobile / Backend |
| UC-41 | MF-04 | Manage Community Answers | Mother / Family | Mobile / Backend |
| UC-42 | MF-04 | Report Community Content or Account | Authenticated User | Mobile / Backend |
| UC-43 | MF-05 | Browse Expert Directory | Mother / Family | Mobile / Backend |
| UC-44 | MF-05 | Manage Own Expert Conversation Requests | Mother / Family | Mobile / Backend |
| UC-45 | MF-06 | Use AI Nurse Symptom Triage | Mother | Mobile / AI Service / Backend |
| UC-46 | MF-06 | View AI Triage History | Mother / Authorized Family | Mobile / Backend |
| UC-47 | MF-06 | Escalate AI Triage to Emergency Support | Mother | Mobile / Backend |
| UC-48 | MF-06 | Request Expert Support from AI Triage | Mother / Family | Mobile / Backend |
| UC-49 | MF-07 | Find Nearby Care Facility | Mother / Family | Mobile / TrackAsia / Backend |
| UC-50 | MF-07 | Call or Navigate to Care Facility | Mother / Family | Mobile / TrackAsia / Device Services |
| UC-51 | MF-07 | Call Emergency Number 115 | Mother / Family | Mobile / Device Dialer |
| UC-52 | MF-07 | Alert Family During Emergency | Mother | Mobile / Backend |
| UC-53 | MF-07 | View Emergency or Family Alert | Mother / Family | Mobile / Backend |
| UC-54 | MF-08 | Manage Care Groups | Mother | Mobile / Backend |
| UC-55 | MF-08 | Manage Care Group Invitations | Mother / Family | Mobile / Backend |
| UC-56 | MF-08 | Manage Family Permissions | Mother | Mobile / Backend |
| UC-57 | MF-08 | View Shared Care Data | Authorized Family | Mobile / Backend |
| UC-58 | MF-08 | View Shared Care Calendar | Mother / Authorized Family | Mobile / Backend |
| UC-59 | MF-08 | Manage Cooperative Care Tasks | Mother / Family | Mobile / Backend |
| UC-60 | MF-08 | Manage Care Group Membership | Mother / Family | Mobile / Backend |
| UC-61 | MF-08 | View Family Alerts | Authorized Family | Mobile / Backend |
| UC-62 | MF-09 | Browse Verified Content and FAQ | Mother / Family | Mobile / Backend |
| UC-63 | MF-09 | Manage Personal Care Checklist | Mother / Authorized Family | Mobile / Backend |
| UC-64 | MF-10 | Manage Safety Monitoring Settings | Mother | Mother Mobile App / Backend / Phone IMU |
| UC-65 | MF-10 | Respond to Suspected Fall or Impact | Mother / Phone Motion Sensors | Mother Mobile App / Backend / Phone IMU |
| UC-66 | MF-10 | Send Safety Emergency Alert | Mother | Mother Mobile App / Backend / Phone IMU |
| UC-67 | MF-10 | Review Safety Events and Report False Positive | Mother | Mother Mobile App / Backend / Phone IMU |
| UC-68 | MF-10 | Open Emergency Support from Safety Alert | Mother | Mother Mobile App / Backend |

**Table 5: Mobile App Use Cases Catalogue**

#### **3.3.3 Web App Use Cases** {#3.3.3-web-app-use-cases}

| UC ID | Major Feature | Use Case Name | Primary Actor | Platform |
| --- | --- | --- | --- | --- |
| UC-69 | MF-01 | View Admin Dashboard | System Admin | Admin Web / Backend |
| UC-70 | MF-01 | Manage User Accounts and Roles | System Admin | Admin Web / Backend |
| UC-71 | MF-01 | Create Staff Account | System Admin | Admin Web / Backend |
| UC-72 | MF-01 | Review Account Lock Appeals | System Admin | Admin Web / Backend |
| UC-73 | MF-01 | Review Audit and Security Operations | System Admin | Admin Web / Backend |
| UC-74 | MF-01 | Manage System Configuration | System Admin | Admin Web / Backend |
| UC-75 | MF-04 | View Moderator Dashboard | Moderator | Moderator Web / Backend |
| UC-76 | MF-04 | Review Pending Community Content | Moderator | Moderator Web / Backend |
| UC-77 | MF-04 | Monitor Published Community Content | Moderator | Moderator Web / Backend |
| UC-78 | MF-04 | Manage Community Reports | Moderator | Moderator Web / Backend |
| UC-79 | MF-04 | Apply and Review Moderation Actions | Moderator | Moderator Web / Backend |
| UC-80 | MF-04 | Review AI Moderation Assessment | Moderator | Moderator Web / Backend |
| UC-81 | MF-04 | Manage Community Topics | Moderator | Moderator Web / Backend |
| UC-82 | MF-05 | Review Expert Applications and Trust | System Admin | Admin Web / Backend |
| UC-83 | MF-06 | Manage AI Red-Flag Rules | System Admin | Admin Web / Backend |
| UC-84 | MF-09 | View Content Administration Workspace | Content Admin | Content Web / Backend |
| UC-85 | MF-09 | Manage Verified Articles | Content Admin | Content Web / Backend |
| UC-86 | MF-09 | Manage Verified FAQs | Content Admin | Content Web / Backend |
| UC-87 | MF-09 | Manage Content Topics | Content Admin | Content Web / Backend |
| UC-88 | MF-09 | Review and Approve Content | System Admin | Admin Web / Backend |
| UC-89 | MF-09 | Manage Checklist Templates | Content Admin / System Admin | Content Web / Admin Web / Backend |
| UC-90 | MF-09 | Manage Pregnancy Exercise Content | Content Admin | Content Web / Backend |
| UC-91 | MF-09 | Manage Exercise Posture Configuration | System Admin | Admin Web / Backend |

**Table 6: Web App Use Cases Catalogue**

### **3.4 Detailed Functional Specifications** {#3.4-detailed-functional-specifications}

#### **3.4.1 Shared / Common Use Cases** {#3.4.1-shared-common-use-cases}

##### **3.4.1.1 UC-01 Register Account** {#3.4.1.1-uc-01-register-account}

| UC ID and Name | UC-01 Register Account |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Register Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates a Mother, Family or Expert account, captures the initial role, verifies the supported contact channel through OTP and allows OTP resend within the same registration flow. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. Any required registration, recovery, appeal or verification context is valid. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Register Account. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Register Account and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 7: UC-01 Register Account Use Case Specification**

##### **3.4.1.2 UC-02 Login** {#3.4.1.2-uc-02-login}

| UC ID and Name | UC-02 Login |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Firebase / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Login from a reachable CareBridge screen or system event. |  |  |
| **Description** | Authenticates a supported account by email, phone or Google and routes the user to the role-appropriate workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. Any required registration, recovery, appeal or verification context is valid. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Login. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Login and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 8: UC-02 Login Use Case Specification**

##### **3.4.1.3 UC-03 Logout** {#3.4.1.3-uc-03-logout}

| UC ID and Name | UC-03 Logout |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Logout from a reachable CareBridge screen or system event. |  |  |
| **Description** | Ends the current authenticated session and returns the user to a public or login screen. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Logout. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Logout and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 9: UC-03 Logout Use Case Specification**

##### **3.4.1.4 UC-04 Reset Password** {#3.4.1.4-uc-04-reset-password}

| UC ID and Name | UC-04 Reset Password |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Guest | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Reset Password from a reachable CareBridge screen or system event. |  |  |
| **Description** | Requests and completes password recovery using the registered contact channel and a valid time-limited verification proof. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. Any required registration, recovery, appeal or verification context is valid. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Reset Password. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Reset Password and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 10: UC-04 Reset Password Use Case Specification**

##### **3.4.1.5 UC-05 Change Password** {#3.4.1.5-uc-05-change-password}

| UC ID and Name | UC-05 Change Password |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Change Password from a reachable CareBridge screen or system event. |  |  |
| **Description** | Changes the signed-in user's password after validating the current password and password policy. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Change Password. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Change Password and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 11: UC-05 Change Password Use Case Specification**

##### **3.4.1.6 UC-06 Manage Account Profile** {#3.4.1.6-uc-06-manage-account-profile}

| UC ID and Name | UC-06 Manage Account Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Account Profile from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the user's supported personal profile and contact information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Account Profile. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Account Profile and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 12: UC-06 Manage Account Profile Use Case Specification**

##### **3.4.1.7 UC-07 Manage Notifications** {#3.4.1.7-uc-07-manage-notifications}

| UC ID and Name | UC-07 Manage Notifications |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Notifications from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists notification items, opens notification details and marks individual or eligible notification items as read. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Notifications. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Notifications and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 13: UC-07 Manage Notifications Use Case Specification**

##### **3.4.1.8 UC-08 Manage Privacy and Data Permissions** {#3.4.1.8-uc-08-manage-privacy-and-data-permissions}

| UC ID and Name | UC-08 Manage Privacy and Data Permissions |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Privacy and Data Permissions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews privacy state and grants, reviews or revokes scoped data-sharing permissions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Privacy and Data Permissions. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Privacy and Data Permissions and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 14: UC-08 Manage Privacy and Data Permissions Use Case Specification**

##### **3.4.1.9 UC-09 Deactivate or Delete Own Account** {#3.4.1.9-uc-09-deactivate-or-delete-own-account}

| UC ID and Name | UC-09 Deactivate or Delete Own Account |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Deactivate or Delete Own Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Requests account deactivation or deletion subject to current retention and audit rules. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Deactivate or Delete Own Account. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Deactivate or Delete Own Account and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 15: UC-09 Deactivate or Delete Own Account Use Case Specification**

##### **3.4.1.10 UC-10 Submit Account Lock Appeal** {#3.4.1.10-uc-10-submit-account-lock-appeal}

| UC ID and Name | UC-10 Submit Account Lock Appeal |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Shared / Common Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Web / Backend |
| **Primary Actor** | Locked User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Submit Account Lock Appeal from a reachable CareBridge screen or system event. |  |  |
| **Description** | Submits an appeal for the current account-lock episode using the supported appeal proof. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. Any required registration, recovery, appeal or verification context is valid. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Submit Account Lock Appeal. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Submit Account Lock Appeal and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 16: UC-10 Submit Account Lock Appeal Use Case Specification**

##### **3.4.1.11 UC-11 Browse Community Q&A** {#3.4.1.11-uc-11-browse-community-qanda}

| UC ID and Name | UC-11 Browse Community Q&A |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Shared / Common Use Cases — Community Q&A & Moderation | **Platform** | Mobile / Expert Web / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Community Q&A from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses the approved community feed, opens question details and, for Mother or Family, reviews their own questions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Community Q&A. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Browse Community Q&A and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 17: UC-11 Browse Community Q&A Use Case Specification**

##### **3.4.1.12 UC-12 Answer Community Questions as Verified Expert** {#3.4.1.12-uc-12-answer-community-questions-as-verified-expert}

| UC ID and Name | UC-12 Answer Community Questions as Verified Expert |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Shared / Common Use Cases — Community Q&A & Moderation | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Verified Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Answer Community Questions as Verified Expert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews the expert question queue and creates, updates or removes the expert's answer with the current verified-expert label. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Answer Community Questions as Verified Expert. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Answer Community Questions as Verified Expert and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 18: UC-12 Answer Community Questions as Verified Expert Use Case Specification**

##### **3.4.1.13 UC-13 Register and Submit Expert Application** {#3.4.1.13-uc-13-register-and-submit-expert-application}

| UC ID and Name | UC-13 Register and Submit Expert Application |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert Applicant | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Register and Submit Expert Application from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes expert onboarding, creates the professional application and submits required identity and verification evidence. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. Any required registration, recovery, appeal or verification context is valid. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Register and Submit Expert Application. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Register and Submit Expert Application and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 19: UC-13 Register and Submit Expert Application Use Case Specification**

##### **3.4.1.14 UC-14 Manage Expert Professional Profile** {#3.4.1.14-uc-14-manage-expert-professional-profile}

| UC ID and Name | UC-14 Manage Expert Professional Profile |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Expert Professional Profile from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the professional profile, manages credentials and documents and reviews or renews verification status. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Expert Professional Profile. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Expert Professional Profile and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 20: UC-14 Manage Expert Professional Profile Use Case Specification**

##### **3.4.1.15 UC-15 Manage Expert Availability** {#3.4.1.15-uc-15-manage-expert-availability}

| UC ID and Name | UC-15 Manage Expert Availability |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Expert Availability from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the calendar, creates or removes availability slots and changes supported online availability state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Expert Availability. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Expert Availability and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 21: UC-15 Manage Expert Availability Use Case Specification**

##### **3.4.1.16 UC-16 Process Expert Conversation Requests** {#3.4.1.16-uc-16-process-expert-conversation-requests}

| UC ID and Name | UC-16 Process Expert Conversation Requests |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Expert Mobile / Expert Web / Backend |
| **Primary Actor** | Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Process Expert Conversation Requests from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens assigned requests and accepts or rejects a request; acceptance creates or links the direct conversation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Process Expert Conversation Requests. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Process Expert Conversation Requests and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 22: UC-16 Process Expert Conversation Requests Use Case Specification**

##### **3.4.1.17 UC-17 Use Direct Expert Chat** {#3.4.1.17-uc-17-use-direct-expert-chat}

| UC ID and Name | UC-17 Use Direct Expert Chat |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Expert Web / File Storage / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Use Direct Expert Chat from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens direct conversations and exchanges text, image or document messages, including attachment viewing and eligible recall. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Use Direct Expert Chat. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Use Direct Expert Chat and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 23: UC-17 Use Direct Expert Chat Use Case Specification**

##### **3.4.1.18 UC-18 Conduct Direct Voice or Video Call** {#3.4.1.18-uc-18-conduct-direct-voice-or-video-call}

| UC ID and Name | UC-18 Conduct Direct Voice or Video Call |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Shared / Common Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Expert Web / ZegoCloud / Backend |
| **Primary Actor** | Mother / Family / Expert | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Conduct Direct Voice or Video Call from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts, answers, declines and ends an authorized voice or video call inside an accepted direct conversation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Conduct Direct Voice or Video Call. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Conduct Direct Voice or Video Call and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | The business outcome is specified once across supported clients; client-specific presentation does not create another use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 24: UC-18 Conduct Direct Voice or Video Call Use Case Specification**

#### **3.4.2 Mobile App Use Cases** {#3.4.2-mobile-app-use-cases}

##### **3.4.2.1 UC-19 Manage Own Login Sessions** {#3.4.2.1-uc-19-manage-own-login-sessions}

| UC ID and Name | UC-19 Manage Own Login Sessions |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Mobile App Use Cases — Account, Trust & Access Control | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Own Login Sessions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the user's login sessions and revokes a selected session when supported. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Own Login Sessions. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Own Login Sessions and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 25: UC-19 Manage Own Login Sessions Use Case Specification**

##### **3.4.2.2 UC-20 Manage Mother Journey** {#3.4.2.2-uc-20-manage-mother-journey}

| UC ID and Name | UC-20 Manage Mother Journey |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Mother Journey from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates and updates a preconception, pregnancy or postpartum journey, records stage transitions or pregnancy outcome and views the journey dashboard. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Mother Journey. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Mother Journey and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 26: UC-20 Manage Mother Journey Use Case Specification**

##### **3.4.2.3 UC-21 Manage Maternal Health Metrics** {#3.4.2.3-uc-21-manage-maternal-health-metrics}

| UC ID and Name | UC-21 Manage Maternal Health Metrics |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Maternal Health Metrics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Adds, views, updates and removes supported maternal measurements and reviews their detail and trend. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Maternal Health Metrics. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Maternal Health Metrics and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 27: UC-21 Manage Maternal Health Metrics Use Case Specification**

##### **3.4.2.4 UC-22 Manage Postpartum Logs** {#3.4.2.4-uc-22-manage-postpartum-logs}

| UC ID and Name | UC-22 Manage Postpartum Logs |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Postpartum Logs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Adds, views, updates and deletes postpartum recovery observations. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Postpartum Logs. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Postpartum Logs and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 28: UC-22 Manage Postpartum Logs Use Case Specification**

##### **3.4.2.5 UC-23 Manage Maternal Health Records** {#3.4.2.5-uc-23-manage-maternal-health-records}

| UC ID and Name | UC-23 Manage Maternal Health Records |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / File Storage / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Maternal Health Records from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and archives maternal health records and manages their protected attachments. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Maternal Health Records. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Maternal Health Records and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 29: UC-23 Manage Maternal Health Records Use Case Specification**

##### **3.4.2.6 UC-24 Manage Appointments** {#3.4.2.6-uc-24-manage-appointments}

| UC ID and Name | UC-24 Manage Appointments |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Appointments from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes appointments through the appointment calendar and detail screens. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Appointments. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Appointments and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 30: UC-24 Manage Appointments Use Case Specification**

##### **3.4.2.7 UC-25 Manage Reminders and Schedules** {#3.4.2.7-uc-25-manage-reminders-and-schedules}

| UC ID and Name | UC-25 Manage Reminders and Schedules |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Reminders and Schedules from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates medication and other reminders, manages recurrence schedules and performs snooze, complete, skip, enable or delete actions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Reminders and Schedules. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Reminders and Schedules and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 31: UC-25 Manage Reminders and Schedules Use Case Specification**

##### **3.4.2.8 UC-26 View Today Care Tasks** {#3.4.2.8-uc-26-view-today-care-tasks}

| UC ID and Name | UC-26 View Today Care Tasks |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Today Care Tasks from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays today's reminders, appointments and current care tasks from the implemented home/task view. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Today Care Tasks. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Today Care Tasks and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 32: UC-26 View Today Care Tasks Use Case Specification**

##### **3.4.2.9 UC-27 Browse Pregnancy Exercises** {#3.4.2.9-uc-27-browse-pregnancy-exercises}

| UC ID and Name | UC-27 Browse Pregnancy Exercises |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Pregnancy Exercises from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses reviewed pregnancy exercises and opens exercise instructions and safety information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Pregnancy Exercises. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Browse Pregnancy Exercises and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 33: UC-27 Browse Pregnancy Exercises Use Case Specification**

##### **3.4.2.10 UC-28 Complete Pre-exercise Safety Check** {#3.4.2.10-uc-28-complete-pre-exercise-safety-check}

| UC ID and Name | UC-28 Complete Pre-exercise Safety Check |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Complete Pre-exercise Safety Check from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes the required safety questions before an exercise session and stops entry when a configured warning condition applies. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Complete Pre-exercise Safety Check. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Complete Pre-exercise Safety Check and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 34: UC-28 Complete Pre-exercise Safety Check Use Case Specification**

##### **3.4.2.11 UC-29 Perform Camera-guided Exercise Session** {#3.4.2.11-uc-29-perform-camera-guided-exercise-session}

| UC ID and Name | UC-29 Perform Camera-guided Exercise Session |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Camera / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Perform Camera-guided Exercise Session from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts, pauses, resumes and completes an exercise session with optional camera-based real-time posture analysis after permission. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Perform Camera-guided Exercise Session. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Perform Camera-guided Exercise Session and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 35: UC-29 Perform Camera-guided Exercise Session Use Case Specification**

##### **3.4.2.12 UC-30 View Exercise History and Results** {#3.4.2.12-uc-30-view-exercise-history-and-results}

| UC ID and Name | UC-30 View Exercise History and Results |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Exercise History and Results from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views completed exercise sessions, posture feedback and the recorded session result. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Exercise History and Results. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Exercise History and Results and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 36: UC-30 View Exercise History and Results Use Case Specification**

##### **3.4.2.13 UC-31 Record Quick Health Notes** {#3.4.2.13-uc-31-record-quick-health-notes}

| UC ID and Name | UC-31 Record Quick Health Notes |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Record Quick Health Notes from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records supported BMI, hydration, mood and fetal-movement quick notes. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Record Quick Health Notes. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Record Quick Health Notes and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 37: UC-31 Record Quick Health Notes Use Case Specification**

##### **3.4.2.14 UC-32 Manage EPDS Screening** {#3.4.2.14-uc-32-manage-epds-screening}

| UC ID and Name | UC-32 Manage EPDS Screening |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage EPDS Screening from a reachable CareBridge screen or system event. |  |  |
| **Description** | Completes EPDS screening, receives the supported safety response and reviews previous screening results. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage EPDS Screening. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage EPDS Screening and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 38: UC-32 Manage EPDS Screening Use Case Specification**

##### **3.4.2.15 UC-33 View Personalized Care Recommendations** {#3.4.2.15-uc-33-view-personalized-care-recommendations}

| UC ID and Name | UC-33 View Personalized Care Recommendations |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-02 — Mobile App Use Cases — Mother Care Journey | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Personalized Care Recommendations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays current stage-based care recommendations produced by the implemented recommendation flow. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Personalized Care Recommendations. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Personalized Care Recommendations and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Maternal data is owner-scoped and non-diagnostic; safety and permission gates apply to exercise and screening. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 39: UC-33 View Personalized Care Recommendations Use Case Specification**

##### **3.4.2.16 UC-34 Manage Baby Profiles** {#3.4.2.16-uc-34-manage-baby-profiles}

| UC ID and Name | UC-34 Manage Baby Profiles |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Profiles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, switches and archives baby profiles while preserving linked care history. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Profiles. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Baby Profiles and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 40: UC-34 Manage Baby Profiles Use Case Specification**

##### **3.4.2.17 UC-35 Manage Baby Daily Logs** {#3.4.2.17-uc-35-manage-baby-daily-logs}

| UC ID and Name | UC-35 Manage Baby Daily Logs |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Daily Logs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes feeding, sleep and diaper observations and reviews their summary. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Daily Logs. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Baby Daily Logs and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 41: UC-35 Manage Baby Daily Logs Use Case Specification**

##### **3.4.2.18 UC-36 Manage Development Milestones** {#3.4.2.18-uc-36-manage-development-milestones}

| UC ID and Name | UC-36 Manage Development Milestones |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Development Milestones from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records, reviews, updates and deletes caregiver-observed development milestones. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Development Milestones. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Development Milestones and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 42: UC-36 Manage Development Milestones Use Case Specification**

##### **3.4.2.19 UC-37 Manage Baby Growth** {#3.4.2.19-uc-37-manage-baby-growth}

| UC ID and Name | UC-37 Manage Baby Growth |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Growth from a reachable CareBridge screen or system event. |  |  |
| **Description** | Records, updates and deletes growth measurements and reviews measurement history and growth charts. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Growth. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Baby Growth and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 43: UC-37 Manage Baby Growth Use Case Specification**

##### **3.4.2.20 UC-38 Manage Vaccination Journey** {#3.4.2.20-uc-38-manage-vaccination-journey}

| UC ID and Name | UC-38 Manage Vaccination Journey |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Vaccination Journey from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views the vaccination schedule, manages vaccination records, completes or postpones a dose and creates related reminders. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Vaccination Journey. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Vaccination Journey and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 44: UC-38 Manage Vaccination Journey Use Case Specification**

##### **3.4.2.21 UC-39 Manage Baby Health Records** {#3.4.2.21-uc-39-manage-baby-health-records}

| UC ID and Name | UC-39 Manage Baby Health Records |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-03 — Mobile App Use Cases — Baby Care Journey, Growth & Vaccination | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Baby Health Records from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and archives health records scoped to the selected baby. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Baby Health Records. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Baby Health Records and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Every operation is scoped to an authorized baby profile and does not constitute pediatric diagnosis. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 45: UC-39 Manage Baby Health Records Use Case Specification**

##### **3.4.2.22 UC-40 Manage Community Questions** {#3.4.2.22-uc-40-manage-community-questions}

| UC ID and Name | UC-40 Manage Community Questions |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Questions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, edits and deletes the actor's own community questions under moderation rules. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Questions. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Community Questions and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 46: UC-40 Manage Community Questions Use Case Specification**

##### **3.4.2.23 UC-41 Manage Community Answers** {#3.4.2.23-uc-41-manage-community-answers}

| UC ID and Name | UC-41 Manage Community Answers |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Answers from a reachable CareBridge screen or system event. |  |  |
| **Description** | Posts, edits and deletes the actor's own community answers as personal community contributions. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Answers. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Community Answers and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 47: UC-41 Manage Community Answers Use Case Specification**

##### **3.4.2.24 UC-42 Report Community Content or Account** {#3.4.2.24-uc-42-report-community-content-or-account}

| UC ID and Name | UC-42 Report Community Content or Account |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Mobile App Use Cases — Community Q&A & Moderation | **Platform** | Mobile / Backend |
| **Primary Actor** | Authenticated User | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Report Community Content or Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Submits a report against supported community content or an account with a reason and evidence allowed by the form. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Report Community Content or Account. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Report Community Content or Account and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 48: UC-42 Report Community Content or Account Use Case Specification**

##### **3.4.2.25 UC-43 Browse Expert Directory** {#3.4.2.25-uc-43-browse-expert-directory}

| UC ID and Name | UC-43 Browse Expert Directory |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Mobile App Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Expert Directory from a reachable CareBridge screen or system event. |  |  |
| **Description** | Searches the verified expert directory and opens an expert's public professional profile. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Expert Directory. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Browse Expert Directory and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 49: UC-43 Browse Expert Directory Use Case Specification**

##### **3.4.2.26 UC-44 Manage Own Expert Conversation Requests** {#3.4.2.26-uc-44-manage-own-expert-conversation-requests}

| UC ID and Name | UC-44 Manage Own Expert Conversation Requests |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Mobile App Use Cases — Verified Expert Network & Contribution | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Own Expert Conversation Requests from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views and cancels the actor's free conversation requests to an expert. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Own Expert Conversation Requests. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Own Expert Conversation Requests and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 50: UC-44 Manage Own Expert Conversation Requests Use Case Specification**

##### **3.4.2.27 UC-45 Use AI Nurse Symptom Triage** {#3.4.2.27-uc-45-use-ai-nurse-symptom-triage}

| UC ID and Name | UC-45 Use AI Nurse Symptom Triage |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / AI Service / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Use AI Nurse Symptom Triage from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts structured symptom intake, supplies follow-up information and receives a non-diagnostic risk result and next-step guidance. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Use AI Nurse Symptom Triage. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Use AI Nurse Symptom Triage and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 51: UC-45 Use AI Nurse Symptom Triage Use Case Specification**

##### **3.4.2.28 UC-46 View AI Triage History** {#3.4.2.28-uc-46-view-ai-triage-history}

| UC ID and Name | UC-46 View AI Triage History |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View AI Triage History from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews previous AI triage sessions within the current permission scope. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View AI Triage History. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View AI Triage History and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 52: UC-46 View AI Triage History Use Case Specification**

##### **3.4.2.29 UC-47 Escalate AI Triage to Emergency Support** {#3.4.2.29-uc-47-escalate-ai-triage-to-emergency-support}

| UC ID and Name | UC-47 Escalate AI Triage to Emergency Support |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Escalate AI Triage to Emergency Support from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the emergency support flow when the triage result or red-flag handling requires urgent action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Escalate AI Triage to Emergency Support. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Escalate AI Triage to Emergency Support and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 53: UC-47 Escalate AI Triage to Emergency Support Use Case Specification**

##### **3.4.2.30 UC-48 Request Expert Support from AI Triage** {#3.4.2.30-uc-48-request-expert-support-from-ai-triage}

| UC ID and Name | UC-48 Request Expert Support from AI Triage |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-06 — Mobile App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Request Expert Support from AI Triage from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates a free expert conversation request using the supported context from an AI triage session. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Request Expert Support from AI Triage. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Request Expert Support from AI Triage and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 54: UC-48 Request Expert Support from AI Triage Use Case Specification**

##### **3.4.2.31 UC-49 Find Nearby Care Facility** {#3.4.2.31-uc-49-find-nearby-care-facility}

| UC ID and Name | UC-49 Find Nearby Care Facility |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / TrackAsia / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Find Nearby Care Facility from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the emergency map, searches nearby facilities and reviews facility details, distance, route and ETA. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Find Nearby Care Facility. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Find Nearby Care Facility and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 55: UC-49 Find Nearby Care Facility Use Case Specification**

##### **3.4.2.32 UC-50 Call or Navigate to Care Facility** {#3.4.2.32-uc-50-call-or-navigate-to-care-facility}

| UC ID and Name | UC-50 Call or Navigate to Care Facility |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / TrackAsia / Device Services |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Call or Navigate to Care Facility from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts a device call to a selected facility or opens navigation for its location. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Call or Navigate to Care Facility. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Call or Navigate to Care Facility and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 56: UC-50 Call or Navigate to Care Facility Use Case Specification**

##### **3.4.2.33 UC-51 Call Emergency Number 115** {#3.4.2.33-uc-51-call-emergency-number-115}

| UC ID and Name | UC-51 Call Emergency Number 115 |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Device Dialer |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Call Emergency Number 115 from a reachable CareBridge screen or system event. |  |  |
| **Description** | Starts a device call to emergency number 115 after the actor confirms the action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Call Emergency Number 115. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Call Emergency Number 115 and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 57: UC-51 Call Emergency Number 115 Use Case Specification**

##### **3.4.2.34 UC-52 Alert Family During Emergency** {#3.4.2.34-uc-52-alert-family-during-emergency}

| UC ID and Name | UC-52 Alert Family During Emergency |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Alert Family During Emergency from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends the implemented family emergency alert with only the authorized context and location state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Alert Family During Emergency. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Alert Family During Emergency and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 58: UC-52 Alert Family During Emergency Use Case Specification**

##### **3.4.2.35 UC-53 View Emergency or Family Alert** {#3.4.2.35-uc-53-view-emergency-or-family-alert}

| UC ID and Name | UC-53 View Emergency or Family Alert |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-07 — Mobile App Use Cases — Emergency Map & Nearby Care Support | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Emergency or Family Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens the detail of an emergency or family alert available to the current actor. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Emergency or Family Alert. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Emergency or Family Alert and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Location permission and provider limitations apply; CareBridge does not dispatch or guarantee assistance. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 59: UC-53 View Emergency or Family Alert Use Case Specification**

##### **3.4.2.36 UC-54 Manage Care Groups** {#3.4.2.36-uc-54-manage-care-groups}

| UC ID and Name | UC-54 Manage Care Groups |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Groups from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates and deletes the Mother's care groups; member invitation, permission and membership actions remain in their dedicated cooperative-care use cases. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Groups. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Care Groups and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 60: UC-54 Manage Care Groups Use Case Specification**

##### **3.4.2.37 UC-55 Manage Care Group Invitations** {#3.4.2.37-uc-55-manage-care-group-invitations}

| UC ID and Name | UC-55 Manage Care Group Invitations |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Group Invitations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends, lists and revokes invitations and allows the invited Family member to accept or reject them. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Group Invitations. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Care Group Invitations and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 61: UC-55 Manage Care Group Invitations Use Case Specification**

##### **3.4.2.38 UC-56 Manage Family Permissions** {#3.4.2.38-uc-56-manage-family-permissions}

| UC ID and Name | UC-56 Manage Family Permissions |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Family Permissions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Grants and changes the exact care-data categories a Family member may access. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Family Permissions. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Family Permissions and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 62: UC-56 Manage Family Permissions Use Case Specification**

##### **3.4.2.39 UC-57 View Shared Care Data** {#3.4.2.39-uc-57-view-shared-care-data}

| UC ID and Name | UC-57 View Shared Care Data |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Shared Care Data from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views only the maternal data, quick-note history and other care information explicitly shared with the Family member. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Shared Care Data. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Shared Care Data and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 63: UC-57 View Shared Care Data Use Case Specification**

##### **3.4.2.40 UC-58 View Shared Care Calendar** {#3.4.2.40-uc-58-view-shared-care-calendar}

| UC ID and Name | UC-58 View Shared Care Calendar |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Shared Care Calendar from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays appointments and supported care events shared within an active care group. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Shared Care Calendar. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Shared Care Calendar and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 64: UC-58 View Shared Care Calendar Use Case Specification**

##### **3.4.2.41 UC-59 Manage Cooperative Care Tasks** {#3.4.2.41-uc-59-manage-cooperative-care-tasks}

| UC ID and Name | UC-59 Manage Cooperative Care Tasks |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Cooperative Care Tasks from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, cancels and completes assigned care tasks according to care-group role. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Cooperative Care Tasks. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Cooperative Care Tasks and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 65: UC-59 Manage Cooperative Care Tasks Use Case Specification**

##### **3.4.2.42 UC-60 Manage Care Group Membership** {#3.4.2.42-uc-60-manage-care-group-membership}

| UC ID and Name | UC-60 Manage Care Group Membership |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Care Group Membership from a reachable CareBridge screen or system event. |  |  |
| **Description** | Allows the Mother to remove a member and an active Family member to leave a care group. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Care Group Membership. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Care Group Membership and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 66: UC-60 Manage Care Group Membership Use Case Specification**

##### **3.4.2.43 UC-61 View Family Alerts** {#3.4.2.43-uc-61-view-family-alerts}

| UC ID and Name | UC-61 View Family Alerts |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-08 — Mobile App Use Cases — Family Sync & Cooperative Care | **Platform** | Mobile / Backend |
| **Primary Actor** | Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Family Alerts from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens safety or emergency alerts shared with the Family member. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Family Alerts. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Family Alerts and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Active membership and exact least-privilege family permission are rechecked for every shared-care read or change. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 67: UC-61 View Family Alerts Use Case Specification**

##### **3.4.2.44 UC-62 Browse Verified Content and FAQ** {#3.4.2.44-uc-62-browse-verified-content-and-faq}

| UC ID and Name | UC-62 Browse Verified Content and FAQ |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Mobile App Use Cases — Verified Content & Checklist Hub | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Browse Verified Content and FAQ from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses, searches and opens approved stage- and topic-based articles and FAQs. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Browse Verified Content and FAQ. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Browse Verified Content and FAQ and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 68: UC-62 Browse Verified Content and FAQ Use Case Specification**

##### **3.4.2.45 UC-63 Manage Personal Care Checklist** {#3.4.2.45-uc-63-manage-personal-care-checklist}

| UC ID and Name | UC-63 Manage Personal Care Checklist |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Mobile App Use Cases — Verified Content & Checklist Hub | **Platform** | Mobile / Backend |
| **Primary Actor** | Mother / Authorized Family | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Personal Care Checklist from a reachable CareBridge screen or system event. |  |  |
| **Description** | Imports a template and creates, views, updates, deletes, completes or reopens personal checklist tasks and reviews history. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Personal Care Checklist. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Personal Care Checklist and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 69: UC-63 Manage Personal Care Checklist Use Case Specification**

##### **3.4.2.46 UC-64 Manage Safety Monitoring Settings** {#3.4.2.46-uc-64-manage-safety-monitoring-settings}

| UC ID and Name | UC-64 Manage Safety Monitoring Settings |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Safety Monitoring Settings from a reachable CareBridge screen or system event. |  |  |
| **Description** | Configures, enables and disables phone-IMU safety monitoring and its authorized alert behavior. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Safety Monitoring Settings. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Safety Monitoring Settings and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 70: UC-64 Manage Safety Monitoring Settings Use Case Specification**

##### **3.4.2.47 UC-65 Respond to Suspected Fall or Impact** {#3.4.2.47-uc-65-respond-to-suspected-fall-or-impact}

| UC ID and Name | UC-65 Respond to Suspected Fall or Impact |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother / Phone Motion Sensors | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Respond to Suspected Fall or Impact from a reachable CareBridge screen or system event. |  |  |
| **Description** | Detects a suspected fall or impact and lets the Mother confirm safety or request help within the response window. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Respond to Suspected Fall or Impact. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Respond to Suspected Fall or Impact and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 71: UC-65 Respond to Suspected Fall or Impact Use Case Specification**

##### **3.4.2.48 UC-66 Send Safety Emergency Alert** {#3.4.2.48-uc-66-send-safety-emergency-alert}

| UC ID and Name | UC-66 Send Safety Emergency Alert |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Send Safety Emergency Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Sends an emergency alert to configured authorized contacts when the safety flow requires escalation. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Send Safety Emergency Alert. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Send Safety Emergency Alert and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 72: UC-66 Send Safety Emergency Alert Use Case Specification**

##### **3.4.2.49 UC-67 Review Safety Events and Report False Positive** {#3.4.2.49-uc-67-review-safety-events-and-report-false-positive}

| UC ID and Name | UC-67 Review Safety Events and Report False Positive |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend / Phone IMU |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Safety Events and Report False Positive from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews safety-event history and marks an eligible detection as a false positive for monitoring feedback. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Safety Events and Report False Positive. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review Safety Events and Report False Positive and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 73: UC-67 Review Safety Events and Report False Positive Use Case Specification**

##### **3.4.2.50 UC-68 Open Emergency Support from Safety Alert** {#3.4.2.50-uc-68-open-emergency-support-from-safety-alert}

| UC ID and Name | UC-68 Open Emergency Support from Safety Alert |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-10 — Mobile App Use Cases — Smart Activity Monitoring & Safety Support | **Platform** | Mother Mobile App / Backend |
| **Primary Actor** | Mother | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Open Emergency Support from Safety Alert from a reachable CareBridge screen or system event. |  |  |
| **Description** | Opens emergency map, call and family-support actions from an active safety alert. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Open Emergency Support from Safety Alert. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Open Emergency Support from Safety Alert and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Monitoring is opt-in; detections are suspected events and alerts do not guarantee emergency response. |  |  |
| **Other Information** | Mobile navigation, permission prompts and device-specific interactions are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 74: UC-68 Open Emergency Support from Safety Alert Use Case Specification**

#### **3.4.3 Web App Use Cases** {#3.4.3-web-app-use-cases}

##### **3.4.3.1 UC-69 View Admin Dashboard** {#3.4.3.1-uc-69-view-admin-dashboard}

| UC ID and Name | UC-69 View Admin Dashboard |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Admin Dashboard from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays current administrative account, expert, content and security summary information. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Admin Dashboard. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Admin Dashboard and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 75: UC-69 View Admin Dashboard Use Case Specification**

##### **3.4.3.2 UC-70 Manage User Accounts and Roles** {#3.4.3.2-uc-70-manage-user-accounts-and-roles}

| UC ID and Name | UC-70 Manage User Accounts and Roles |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage User Accounts and Roles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists, searches and opens users, reviews activity and changes supported account status, role or access state. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage User Accounts and Roles. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage User Accounts and Roles and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 76: UC-70 Manage User Accounts and Roles Use Case Specification**

##### **3.4.3.3 UC-71 Create Staff Account** {#3.4.3.3-uc-71-create-staff-account}

| UC ID and Name | UC-71 Create Staff Account |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Create Staff Account from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates an authorized Moderator, Content Admin or other supported staff account. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Create Staff Account. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Create Staff Account and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 77: UC-71 Create Staff Account Use Case Specification**

##### **3.4.3.4 UC-72 Review Account Lock Appeals** {#3.4.3.4-uc-72-review-account-lock-appeals}

| UC ID and Name | UC-72 Review Account Lock Appeals |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Account Lock Appeals from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens lock appeals and records an approve or reject decision. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Account Lock Appeals. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review Account Lock Appeals and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 78: UC-72 Review Account Lock Appeals Use Case Specification**

##### **3.4.3.5 UC-73 Review Audit and Security Operations** {#3.4.3.5-uc-73-review-audit-and-security-operations}

| UC ID and Name | UC-73 Review Audit and Security Operations |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Audit and Security Operations from a reachable CareBridge screen or system event. |  |  |
| **Description** | Reviews audit logs, security events and incident timelines, adds investigation notes and resolves supported incidents. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Audit and Security Operations. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review Audit and Security Operations and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 79: UC-73 Review Audit and Security Operations Use Case Specification**

##### **3.4.3.6 UC-74 Manage System Configuration** {#3.4.3.6-uc-74-manage-system-configuration}

| UC ID and Name | UC-74 Manage System Configuration |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-01 — Web App Use Cases — Account, Trust & Access Control | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage System Configuration from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views and updates the supported runtime configuration exposed by the System Configuration workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage System Configuration. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage System Configuration and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Authentication, account status and server-side role checks apply; sensitive administration is audited. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 80: UC-74 Manage System Configuration Use Case Specification**

##### **3.4.3.7 UC-75 View Moderator Dashboard** {#3.4.3.7-uc-75-view-moderator-dashboard}

| UC ID and Name | UC-75 View Moderator Dashboard |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Moderator Dashboard from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays moderation workload, community and handling summaries. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Moderator Dashboard. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Moderator Dashboard and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 81: UC-75 View Moderator Dashboard Use Case Specification**

##### **3.4.3.8 UC-76 Review Pending Community Content** {#3.4.3.8-uc-76-review-pending-community-content}

| UC ID and Name | UC-76 Review Pending Community Content |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Pending Community Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists and opens pending community questions or answers for moderation review. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Pending Community Content. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review Pending Community Content and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 82: UC-76 Review Pending Community Content Use Case Specification**

##### **3.4.3.9 UC-77 Monitor Published Community Content** {#3.4.3.9-uc-77-monitor-published-community-content}

| UC ID and Name | UC-77 Monitor Published Community Content |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Monitor Published Community Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Browses visible community content and opens a selected item for moderation inspection. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Monitor Published Community Content. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Monitor Published Community Content and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 83: UC-77 Monitor Published Community Content Use Case Specification**

##### **3.4.3.10 UC-78 Manage Community Reports** {#3.4.3.10-uc-78-manage-community-reports}

| UC ID and Name | UC-78 Manage Community Reports |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Reports from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists, claims, releases and opens content/account reports, reviews related evidence and resolves the report. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Reports. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Community Reports and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 84: UC-78 Manage Community Reports Use Case Specification**

##### **3.4.3.11 UC-79 Apply and Review Moderation Actions** {#3.4.3.11-uc-79-apply-and-review-moderation-actions}

| UC ID and Name | UC-79 Apply and Review Moderation Actions |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Apply and Review Moderation Actions from a reachable CareBridge screen or system event. |  |  |
| **Description** | Applies supported content or account actions, reviews violation history and undoes an eligible moderation action. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Apply and Review Moderation Actions. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Apply and Review Moderation Actions and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 85: UC-79 Apply and Review Moderation Actions Use Case Specification**

##### **3.4.3.12 UC-80 Review AI Moderation Assessment** {#3.4.3.12-uc-80-review-ai-moderation-assessment}

| UC ID and Name | UC-80 Review AI Moderation Assessment |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review AI Moderation Assessment from a reachable CareBridge screen or system event. |  |  |
| **Description** | Views AI moderation assessment details and submits moderator feedback where implemented. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review AI Moderation Assessment. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review AI Moderation Assessment and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 86: UC-80 Review AI Moderation Assessment Use Case Specification**

##### **3.4.3.13 UC-81 Manage Community Topics** {#3.4.3.13-uc-81-manage-community-topics}

| UC ID and Name | UC-81 Manage Community Topics |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-04 — Web App Use Cases — Community Q&A & Moderation | **Platform** | Moderator Web / Backend |
| **Primary Actor** | Moderator | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Community Topics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, updates, orders and hides community topics exposed by the moderation workspace. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Community Topics. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Community Topics and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Community ownership, expert verification and moderation state are enforced server-side. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 87: UC-81 Manage Community Topics Use Case Specification**

##### **3.4.3.14 UC-82 Review Expert Applications and Trust** {#3.4.3.14-uc-82-review-expert-applications-and-trust}

| UC ID and Name | UC-82 Review Expert Applications and Trust |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-05 — Web App Use Cases — Verified Expert Network & Contribution | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review Expert Applications and Trust from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists expert cases, opens submitted evidence, approves or rejects verification and changes supported expert trust status. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review Expert Applications and Trust. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review Expert Applications and Trust and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | Expert verification/trust and accepted-conversation membership are rechecked before protected actions. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 88: UC-82 Review Expert Applications and Trust Use Case Specification**

##### **3.4.3.15 UC-83 Manage AI Red-Flag Rules** {#3.4.3.15-uc-83-manage-ai-red-flag-rules}

| UC ID and Name | UC-83 Manage AI Red-Flag Rules |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-06 — Web App Use Cases — AI Nurse Assistant & Risk Triage | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage AI Red-Flag Rules from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists, creates, updates and deletes permitted red-flag rules used by the structured triage flow. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage AI Red-Flag Rules. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage AI Red-Flag Rules and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | High | **Frequency of Use** | Regular |
| **Business Rules** | AI output is non-diagnostic; approved evidence and red-flag rules establish the safe risk floor. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 89: UC-83 Manage AI Red-Flag Rules Use Case Specification**

##### **3.4.3.16 UC-84 View Content Administration Workspace** {#3.4.3.16-uc-84-view-content-administration-workspace}

| UC ID and Name | UC-84 View Content Administration Workspace |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts View Content Administration Workspace from a reachable CareBridge screen or system event. |  |  |
| **Description** | Displays the Content Admin dashboard and browses the content library and its status filters. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers View Content Administration Workspace. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for View Content Administration Workspace and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 90: UC-84 View Content Administration Workspace Use Case Specification**

##### **3.4.3.17 UC-85 Manage Verified Articles** {#3.4.3.17-uc-85-manage-verified-articles}

| UC ID and Name | UC-85 Manage Verified Articles |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Verified Articles from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, versions, archives or unpublishes verified articles through the implemented lifecycle. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Verified Articles. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Verified Articles and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 91: UC-85 Manage Verified Articles Use Case Specification**

##### **3.4.3.18 UC-86 Manage Verified FAQs** {#3.4.3.18-uc-86-manage-verified-faqs}

| UC ID and Name | UC-86 Manage Verified FAQs |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Verified FAQs from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, versions, archives or unpublishes verified FAQ entries through the implemented lifecycle. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Verified FAQs. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Verified FAQs and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 92: UC-86 Manage Verified FAQs Use Case Specification**

##### **3.4.3.19 UC-87 Manage Content Topics** {#3.4.3.19-uc-87-manage-content-topics}

| UC ID and Name | UC-87 Manage Content Topics |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Content Topics from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, updates, orders and changes visibility of article and FAQ topics. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Content Topics. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Content Topics and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 93: UC-87 Manage Content Topics Use Case Specification**

##### **3.4.3.20 UC-88 Review and Approve Content** {#3.4.3.20-uc-88-review-and-approve-content}

| UC ID and Name | UC-88 Review and Approve Content |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Review and Approve Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Lists pending content versions, opens the submitted version and records the supported approval decision. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Review and Approve Content. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Review and Approve Content and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 94: UC-88 Review and Approve Content Use Case Specification**

##### **3.4.3.21 UC-89 Manage Checklist Templates** {#3.4.3.21-uc-89-manage-checklist-templates}

| UC ID and Name | UC-89 Manage Checklist Templates |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Admin Web / Backend |
| **Primary Actor** | Content Admin / System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Checklist Templates from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, updates, clones, reviews, approves and archives checklist template versions according to role. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Checklist Templates. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Checklist Templates and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 95: UC-89 Manage Checklist Templates Use Case Specification**

##### **3.4.3.22 UC-90 Manage Pregnancy Exercise Content** {#3.4.3.22-uc-90-manage-pregnancy-exercise-content}

| UC ID and Name | UC-90 Manage Pregnancy Exercise Content |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Content Web / Backend |
| **Primary Actor** | Content Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Pregnancy Exercise Content from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views, previews, updates and changes supported publication state for pregnancy exercise content. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Pregnancy Exercise Content. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Pregnancy Exercise Content and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 96: UC-90 Manage Pregnancy Exercise Content Use Case Specification**

##### **3.4.3.23 UC-91 Manage Exercise Posture Configuration** {#3.4.3.23-uc-91-manage-exercise-posture-configuration}

| UC ID and Name | UC-91 Manage Exercise Posture Configuration |  |  |
| :--- | :--- | :--- | :--- |
| **Created By** | CareBridge Team | **Date Revised** | 06/08/2026 |
| **Feature / Group** | MF-09 — Web App Use Cases — Verified Content & Checklist Hub | **Platform** | Admin Web / Backend |
| **Primary Actor** | System Admin | **Secondary Actors** | Supporting platform services where applicable |
| **Trigger** | The actor starts Manage Exercise Posture Configuration from a reachable CareBridge screen or system event. |  |  |
| **Description** | Creates, views and updates posture-analysis configuration associated with supported exercises. |  |  |
| **Preconditions** | PRE-1. Required CareBridge and external services are available. PRE-2. The actor can reach the relevant screen or trigger. PRE-3. The actor is authenticated and has the required role, ownership, membership or permission. PRE-4. Referenced records exist when the flow manages existing data. |  |  |
| **Postconditions** | POST-1. The requested business outcome and its final status are displayed. POST-2. Valid confirmed changes are persisted and related views or notifications are refreshed. POST-3. Sensitive actions retain required audit evidence. |  |  |
| **Normal Flow** | Step 1\. The actor opens or triggers Manage Exercise Posture Configuration. Step 2\. The system loads the permitted context and validates access. Step 3\. The actor views data or provides and confirms the supported input. Step 4\. The Backend applies the business rules for Manage Exercise Posture Configuration and performs the requested operation. Step 5\. The system displays the resulting state and updates related data, history or notifications where applicable. |  |  |
| **Alternative Flows** | AF1. Supported method variants, list/detail views, search/filter controls and CRUD actions remain subflows only when relevant to this actor goal. AF2. The actor cancels before confirmation and no unintended change occurs. AF3. An empty result displays the next permitted action. |  |  |
| **Exceptions** | E1. Unauthorized, out-of-scope or ownership-invalid access is denied without exposing protected data. E2. Invalid, expired, conflicting or missing input is rejected with actionable feedback. E3. External, network or server failure returns a safe retry state without false success or duplicate effects. |  |  |
| **Priority** | Medium | **Frequency of Use** | Regular |
| **Business Rules** | Only approved visible content reaches consumers; versions and decisions remain auditable. |  |  |
| **Other Information** | Portal list, detail, search, filter and confirmation screens are subflows of this use case. |  |  |
| **Assumptions** | Current code and the user-approved feature inventory define Release 1 behavior; unsupported legacy APIs do not create additional UCs. |  |  |

**Table 97: UC-91 Manage Exercise Posture Configuration Use Case Specification**

## **5. Non-Functional Requirements** {#5-non-functional-requirements}

This section defines cross-cutting quality and interface requirements for the ten Release 1 MFs. Values not established by approved configuration, tests or deployment policy are marked `Open` rather than invented.

### **5.1 External Interfaces** {#5.1-external-interfaces}

#### **5.1.1 User Interfaces** {#5.1.1-user-interfaces}

| ID | Requirement | Verification / Current Boundary |
| --- | --- | --- |
| NFR-UI-01 | Mobile supports authenticated Mother, Family and Expert role routing with onboarding gates and role-appropriate navigation, including active expert-request and direct-conversation screens. | Flutter router, role-shell, widget tests and API-contract review. |
| NFR-UI-02 | Web supports the System Admin, Moderator, Content Admin and Expert actor goals assigned to MF-01, MF-04, MF-05 and MF-09. | React route guards, page tests and production build; FAMILY has no Web workspace. |
| NFR-UI-03 | Sensitive forms show field validation and preserve recoverable user input where supported. | Component/widget tests and screen review. |
| NFR-UI-04 | Safety-critical triage, emergency and suspected-fall states are visually distinguishable and keep urgent actions visible. | Mobile widget/golden/accessibility review. |
| NFR-UI-05 | Empty, loading, retry, permission-denied and not-found states do not expose unauthorized data or raw exceptions. | Web/Mobile component and widget tests. |

**Table 98: User Interface Requirements**

#### **5.1.2 Software and Service Interfaces** {#5.1.2-software-service-interfaces}

| ID | Interface | Requirement |
| --- | --- | --- |
| NFR-SI-01 | CareBridge API | Protected endpoints require bearer authentication, server-side role/ownership/consent validation and request validation. |
| NFR-SI-02 | Firebase Auth/FCM | Token acquisition/registration and supported account, reminder, expert-request, direct-conversation and emergency notifications fail safely and avoid duplicate side effects. |
| NFR-SI-03 | TrackAsia | Nearby search and route/ETA use timeout/fallback behavior and retain provider/source/status labels; no route or verification state is fabricated. |
| NFR-SI-04 | AI Triage Service | Structured triage uses approved evidence/red-flag controls, validates response shape/citations and preserves non-diagnostic wording. |
| NFR-SI-05 | File Storage | Upload/download requires purpose, owner and authorization checks; a raw storage URL never substitutes for protected access. |
| NFR-SI-06 | Audit/Security | Sensitive actions record minimum actor/action/target/time/result/reason without secrets or unnecessary health content. |

**Table 99: Software and Service Interface Requirements**

#### **5.1.3 Hardware, Device and Sensor Interfaces** {#5.1.3-hardware-device-sensor-interfaces}

| ID | Requirement | Scope |
| --- | --- | --- |
| NFR-DI-01 | Phone accelerometer/gyroscope access is explicit, configurable and disableable; monitoring is off by default. | MF-10 only |
| NFR-DI-02 | Camera access is requested only for the optional pregnancy-exercise posture session after consent. | MF-02 only |
| NFR-DI-03 | Location access is requested only for declared map/safety purposes; denial preserves manual emergency guidance. | MF-07/MF-10 |
| NFR-DI-04 | OS background restrictions, battery state, sensor support and permission changes reconcile monitoring to a safe stopped/limited state. | MF-10 |
| NFR-DI-05 | Wearables, health platforms and connected medical devices are not Release 1 interfaces. | Explicit exclusion |

**Table 100: Hardware, Device and Sensor Interface Requirements**

#### **5.1.4 Communication Interfaces** {#5.1.4-communication-interfaces}

| ID | Requirement | Verification |
| --- | --- | --- |
| NFR-CI-01 | Protected client-server and service-service traffic uses HTTPS/TLS in deployed environments. | Deployment/configuration review |
| NFR-CI-02 | The internal AI evidence endpoint uses a service credential distinct from user/admin authorization. | Integration/security test |
| NFR-CI-03 | External calls have timeouts and safe degradation; retry is allowed only where idempotency prevents duplicate effects. | Integration/failure-path tests |
| NFR-CI-04 | Correlation identifiers and sanitized errors support diagnosis without exposing tokens, OTPs, credentials or sensitive payloads. | Log/security review |
| NFR-CI-05 | Direct participant chat plus voice/video calling through authorized conversation state and ZegoCloud are Release 1 interfaces; paid session/payment processing is excluded. | Scope review |

**Table 101: Communication Interface Requirements**

### **5.2 Quality Attributes** {#5.2-quality-attributes}

#### **5.2.1 Usability and Accessibility** {#5.2.1-usability-accessibility}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-US-01 | Forms identify required/optional fields and display actionable validation near the failing control. | Component/widget tests |
| NFR-US-02 | Supported destructive or sensitive actions require explicit confirmation. | UI tests |
| NFR-US-03 | End-user wording is Vietnamese; safety meaning and stable message codes remain consistent with this appendix. | Content review |
| NFR-US-04 | Critical actions/status do not rely on color alone and remain readable on supported sizes. | Accessibility review; quantified contrast target is Open. |
| NFR-US-05 | Recoverable external failure provides retry or a safe next action without clearing valid user context. | Failure-path tests |

**Table 102: Usability and Accessibility Requirements**

#### **5.2.2 Reliability and Availability** {#5.2.2-reliability-availability}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-RL-01 | A successful persisted response does not lose the confirmed domain record on refresh. | Integration/E2E tests |
| NFR-RL-02 | Notification, checklist action, vaccination completion, safety signal/response and alert retry do not create duplicate effects. | Idempotency/concurrency tests |
| NFR-RL-03 | AI/map/notification/storage/sensor failure returns a safe state and preserves consistency. | Fault-injection/integration tests |
| NFR-RL-04 | Release candidates have no unresolved defect that exposes data, fabricates medical/emergency state or loses confirmed safety evidence. | Release gate |
| NFR-RL-05 | Availability, MTTR and maintenance-notice targets are Open; no unsupported SLA is asserted. | Deployment owner decision |

**Table 103: Reliability and Availability Requirements**

#### **5.2.3 Performance** {#5.2.3-performance}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-PF-01 | List endpoints/clients use bounded pagination or lazy loading where implemented. | API/client tests |
| NFR-PF-02 | Long AI/map operations show progress and timeout/retry guidance instead of freezing. | UI and integration tests |
| NFR-PF-03 | Phone IMU processing does not continuously upload/store raw streams when sampled processing satisfies the feature. | Mobile/backend review |
| NFR-PF-04 | Percentile latency, concurrency and capacity targets are Open until an approved load-test baseline exists. | Performance plan |

**Table 104: Performance Requirements**

#### **5.2.4 Security and Privacy** {#5.2.4-security-privacy}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-SP-01 | Every sensitive read/mutation enforces authentication plus server-side role, ownership, membership and consent checks. | Security/integration tests |
| NFR-SP-02 | Passwords are securely hashed; access/refresh/OTP/reset proofs expire and support revocation/rate limits. | Security tests |
| NFR-SP-03 | Protected data uses encrypted transport and deployment-appropriate encryption at rest. | Deployment review |
| NFR-SP-04 | Community questions, answers and optional anonymous display never expose private maternal, baby or family fields by default. | API privacy tests |
| NFR-SP-05 | Location, camera and motion sensors require declared-purpose permission; revocation prevents future collection. | Mobile/API tests |
| NFR-SP-06 | Logs/audit exclude raw secrets and minimize health/location content. | Log review |
| NFR-SP-07 | Deletion honors retention and preserves required append-only audit/moderation/safety evidence. | Data-policy tests |

**Table 105: Security and Privacy Requirements**

#### **5.2.5 Safety and Compliance** {#5.2.5-safety-compliance}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-SF-01 | CareBridge is a support platform, not a hospital, official EMR, diagnosis, prescription or dispatch service. | Copy/spec review |
| NFR-SF-02 | AI, community/expert answers, exercise/posture, growth and vaccination outputs retain their non-clinical limitations. | Safety/content tests |
| NFR-SF-03 | RED triage and suspected-fall escalation keep call/map/need-help actions visible without claiming dispatch or arrival. | Mobile/integration tests |
| NFR-SF-04 | Moderation can hide/lock unsafe content and apply authorized account action while preserving evidence. | Moderator tests |
| NFR-SF-05 | Missing or uncertain information defaults to safer restrictive behavior and never fabricates external results. | Negative tests |

**Table 106: Safety and Compliance Requirements**

#### **5.2.6 Maintainability and Supportability** {#5.2.6-maintainability-supportability}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-MT-01 | Modules/docs use MF-01 through MF-10 names; legacy feature comments do not define scope. | Structural review |
| NFR-MT-02 | External services use current client boundaries so failure can be mocked and tested. | Architecture review |
| NFR-MT-03 | Business/safety/content configuration changes are auditable and follow active runtime/admin paths. | Code/spec trace |
| NFR-MT-04 | Requirements, the MF-01 through MF-10 Detailed Design Spec files, reachable routes/screens, APIs and tests maintain bidirectional traceability. | Traceability audit |
| NFR-MT-05 | Web build, Mobile analyze/tests and Backend tests are quality gates; known failures are reported. | CI/local verification |

**Table 107: Maintainability and Supportability Requirements**

#### **5.2.7 Scalability and Capacity** {#5.2.7-scalability-capacity}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-SC-01 | Community, content, notification, checklist, audit and admin lists use indexed bounded queries/pagination. | Query/API review |
| NFR-SC-02 | Files and high-growth logs use storage/retention boundaries without changing authorization semantics. | Architecture/data review |
| NFR-SC-03 | Registered-user, DAU and concurrency capacity targets are Open until a deployment/load-test oracle is approved. | Capacity plan |

**Table 108: Scalability and Capacity Requirements**

#### **5.2.8 Compatibility and Portability** {#5.2.8-compatibility-portability}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-CP-01 | Mobile degrades safely when sensor/camera/location/background capability is unavailable. | Platform/widget tests |
| NFR-CP-02 | Web supports browser/build targets declared by current React/Vite configuration. | Build/browser matrix; exact floor is Open if absent. |
| NFR-CP-03 | Demo environments use explicit mock/sandbox/fallback and never display fabricated success. | Integration/demo review |
| NFR-CP-04 | API clients tolerate documented nullable fields and reject incompatible shapes safely. | Contract tests |

**Table 109: Compatibility and Portability Requirements**

#### **5.2.9 Data Quality, Integrity and Auditability** {#5.2.9-data-quality-integrity-auditability}

| ID | Requirement | Acceptance Evidence |
| --- | --- | --- |
| NFR-DQ-01 | Required fields, type/range, source/time, ownership and valid transition are checked before persistence. | Unit/integration tests |
| NFR-DQ-02 | Maternal/baby records, growth, vaccination and AI evidence distinguish source types where applicable. | API/data tests |
| NFR-DQ-03 | Sensitive changes retain actor, time, result/reason and append-only evidence appropriate to the domain. | Audit tests |
| NFR-DQ-04 | Read models never merge data across owner, journey, baby or care-group scope. | Isolation tests |
| NFR-DQ-05 | Dashboard counts use the same states as detail/list APIs and do not imply unsupported completion/verification. | Contract tests |

**Table 110: Data Quality, Integrity and Auditability Requirements**

## **6. Requirement Appendix** {#6-requirement-appendix}

### **6.1 Business Rules** {#6.1-business-rules}

#### **6.1.1 Business Rules by Major Feature** {#6.1.1-business-rules-by-mf}

| Rule ID | Major Feature | Rule Definition |
| --- | --- | --- |
| BR-01 | MF-01 | Account, maternal, baby and family data remain private by default; community display and anonymous posting expose only fields explicitly permitted by the active community contract. |
| BR-02 | MF-01 | Protected access requires effective session plus server-side role/ownership/consent checks. |
| BR-03 | MF-01 | Consent is explicit, purpose/scope/recipient/expiry bound, revocable and rechecked. |
| BR-04 | MF-02 | Mother Journey includes preconception, pregnancy and postpartum; baby care is MF-03. |
| BR-05 | MF-02 | Maternal records/metrics/logs are support data, not official EMR or clinical evidence. |
| BR-06 | MF-02 | Exercise uses reviewed content and safety gating; posture feedback cannot determine medical fitness. |
| BR-07 | MF-02 | Reminders organize care but do not prescribe or prove treatment completion. |
| BR-08 | MF-03 | Baby operations are scoped to an authorized baby and preserve linked history when archived. |
| BR-09 | MF-03 | Logs, milestones, growth and vaccination are observational/reference tools, not diagnosis. |
| BR-10 | MF-04 | Anonymous display preserves internal accountability and moderation visibility. |
| BR-11 | MF-04 | Community content is support/personal experience and remains moderated. |
| BR-12 | MF-04 | Verified-expert answer label reflects current verification; it is not a badge or ranking. |
| BR-13 | MF-05 | Expert verification reflects platform-defined identity/credential checks and is rechecked. |
| BR-14 | MF-05 | Availability/directory visibility does not guarantee response. Gamification, paid booking, payment and commission are excluded; accepted conversation requests, direct chat and calls are allowed. |
| BR-15 | MF-06 | AI may orient GREEN/YELLOW/RED risk only; it cannot diagnose, prescribe or decide clinically. |
| BR-16 | MF-06 | Red-flag rules establish a risk floor; only approved applicable evidence grounds output. |
| BR-17 | MF-06 | Emergency handoff is visible and user initiated; AI does not dispatch services. |
| BR-18 | MF-07 | Map/location requires permission or chosen area and retains provider/source/route limitations. |
| BR-19 | MF-07 | CareBridge does not guarantee facility suitability, availability, ETA or assistance. |
| BR-20 | MF-08 | Shared-care access rechecks active membership and exact permission category. |
| BR-21 | MF-08 | Family access is least privilege and never implies full record access. |
| BR-22 | MF-09 | Only approved visible content and approved/active/distributable checklist versions reach consumers. |
| BR-23 | MF-09 | Content/checklist versions, sources, decisions and task transitions remain auditable. |
| BR-24 | MF-10 | Phone-IMU monitoring is off by default and requires consent, permission and active configuration. |
| BR-25 | MF-10 | Detection creates only a suspected event; first valid response wins and duplicates are prevented. |
| BR-26 | MF-10 | Alerts use selected authorized contacts; location is consent-bound and assistance is not guaranteed. |

**Table 111: Business Rules by Major Feature**

### **6.2 Common Requirements** {#6.2-common-requirements}

| ID | Common Requirement | Rule Definition |
| --- | --- | --- |
| CR-01 | Authentication | Every protected UI/API/background action verifies current session/account state. |
| CR-02 | Authorization | Role, ownership, membership, consent and feature permission are enforced server-side. |
| CR-03 | Validation | Inputs, IDs, files, external responses and transitions are validated before use. |
| CR-04 | Safe Error Handling | Errors are understandable and expose no stack traces, secrets or unauthorized data. |
| CR-05 | Audit | Sensitive actions create minimum actor/action/target/time/result/reason evidence. |
| CR-06 | Consent | Health sharing, location, camera and sensor use pass declared-purpose checks. |
| CR-07 | Notification | Noncritical notifications respect available preferences; safety delivery is not guaranteed. |
| CR-08 | External Failure | AI/map/notification/storage/sensor failure degrades safely without fabricated success. |
| CR-09 | Data Minimization | Only data required for the current outcome and scope is processed. |
| CR-10 | Source/Timestamp | Care data retains source and timestamp where applicable. |
| CR-11 | Idempotency | Retry cannot duplicate task, completion, event or alert side effects. |
| CR-12 | Retention | Deletion preserves required audit, moderation and safety evidence. |
| CR-13 | Localization | Vietnamese wording preserves stable code, severity and safety meaning. |
| CR-14 | Safe Default | Unverifiable permission/service/risk state defaults to restrictive/safe behavior. |
| CR-15 | Traceability | Each UC traces to one MF, one or more grouped Specs, reachable runtime and supporting evidence. |

**Table 112: Common Requirements**

### **6.3 Application Messages List** {#6.3-application-messages-list}

#### **6.3.1 Message Presentation Convention** {#6.3.1-message-presentation-convention}

| Type | Usage |
| --- | --- |
| Inline validation | Field/action validation near the relevant control. |
| Toast/snackbar | Temporary success, information or recoverable failure. |
| Confirmation dialog | Confirmation before a supported destructive/sensitive action. |
| Modal | Blocking consent, identity, safety or conflict state. |
| Banner/safety notice | Persistent caution/urgent limitation and next action. |
| Verified label | Professional/content verification label; never an achievement badge or score. |

**Table 113: Message Presentation Convention**

#### **6.3.2 Reusable Application Messages** {#6.3.2-reusable-application-messages}

| Message Code | Type | Context | Content |
| --- | --- | --- | --- |
| MSG-COM-01 | Toast/Error | Session | Your session has expired. Please log in again. |
| MSG-COM-02 | Inline/Error | Authorization | You do not have permission to view this data or perform this action. |
| MSG-COM-03 | Modal | Identity | Please verify your identity before continuing. |
| MSG-COM-04 | Inline/Error | Account | This account is currently blocked, restricted, suspended or inactive. |
| MSG-PRI-01 | Inline/Error | Consent | Select a valid scope, purpose, recipient and expiry before sharing. |
| MSG-PRI-02 | Toast/Success | Consent | Sharing permission has been revoked. Future access is blocked. |
| MSG-REC-01 | Inline/Error | Protected record/file | This record or attachment is available only to currently authorized users. |
| MSG-COMM-01 | Banner/Safety | Community | This content may describe an urgent health concern. Review emergency guidance. |
| MSG-COMM-02 | Toast/Info | Moderation | Your content is waiting for moderation review. |
| MSG-COMM-03 | Inline/Error | Moderation | This content cannot be published under current community/safety rules. |
| MSG-COMM-04 | Verified label | Expert answer | Verified expert. This answer does not replace direct professional examination. |
| MSG-EXP-01 | Inline/Error | Expert trust | This expert profile is not eligible for verified directory/contribution actions. |
| MSG-AI-01 | Banner/Safety | AI triage | This result is a risk orientation, not a diagnosis or prescription. |
| MSG-AI-02 | Banner/Urgent | RED triage | Urgent warning signs may be present. Use the visible emergency call/map guidance now. |
| MSG-MAP-01 | Banner/Safety | Map | Facility data, route and ETA may be incomplete. CareBridge does not dispatch help. |
| MSG-FAM-01 | Inline/Error | Family permission | Your current care-group permission does not include this data or action. |
| MSG-CONT-01 | Inline/Error | Content | This content version is not approved for consumer display. |
| MSG-CHK-01 | Toast/Info | Checklist | This checklist task changed on another device. Refresh to continue safely. |
| MSG-SAFE-01 | Modal/Urgent | Suspected event | Possible fall or impact detected. Are you safe? |
| MSG-SAFE-02 | Banner/Safety | Monitoring | Detection may miss real events or create false alerts and is not an emergency service. |

**Table 114: Reusable Application Messages**

### **6.4 Other Requirements and Explicit Exclusions** {#6.4-other-requirements-exclusions}

| ID | Requirement / Exclusion | Description |
| --- | --- | --- |
| OR-01 | Healthcare Support Boundary | CareBridge is not a hospital, official EMR, diagnosis, treatment, prescription or emergency-dispatch platform. |
| OR-02 | No Paid Consultation Commerce | Paid booking, consultation pricing, payment, commission, dispute and refund are excluded. Free expert conversation requests, accepted direct chat, private attachments and voice/video calls are active MF-05 scope. |
| OR-03 | No Partner Marketplace | Partner registration/verification, sponsored content and marketplace promotion are outside the ten MFs. |
| OR-04 | No Connected Health Device | Wearable/platform sync and device-data flows are deferred; MF-10 phone IMU is distinct. |
| OR-05 | No Expert Gamification | Contribution badges, badge levels, leaderboard and competence/reputation scores are excluded; verified-answer label remains. |
| OR-06 | No Nearby Expert Request | Nearby expert request/accept/decline is excluded; MF-07 covers facilities, route/ETA, call/navigation. |
| OR-07 | No Health Summary Sharing | Orphan summary generation/sharing and temporary expert file sharing are excluded. |
| OR-08 | No Standalone RAG Chat | AI Nurse is the routed structured intake/triage/history flow. The orphan RagChatScreen is excluded until its route and request contract are made operational. |
| OR-09 | No Expense Planner | Expense/preparation planner is not one of the ten Release 1 MFs. |
| OR-10 | Audit Immutability | Normal operators cannot rewrite/delete audit evidence; corrections are additional records/actions. |
| OR-11 | Raw IMU Minimization | Continuous raw phone IMU streams are not retained long term. |
| OR-12 | Documentation Consistency | Section 3 UCs, Section 5 NFRs, Section 6 and Detailed Design use the same ten-MF boundary. |

**Table 115: Other Requirements and Explicit Exclusions**
