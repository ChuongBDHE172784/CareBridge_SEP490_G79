# 3. Functional Specifications - Detailed Scope Baseline (121 Use Cases)

> **Revision status:** This document supersedes the 46-UC high-level draft. Release 1 is modelled at a **detailed but controlled level** with **121 use cases (UC-01 to UC-121)**. It preserves meaningful user, governance and safety outcomes while removing standalone search/filter functions, confirmation dialogs, duplicated portal/app variants and backend-only technical operations.

## 3.1 Design Level and Scope Rules

| Rule | Decision |
| --- | --- |
| D-01: Detailed outcome boundary | Keep separate use cases when the actor creates, changes, reviews, approves, shares, enforces or completes a distinct domain outcome. |
| D-02: Embedded search/filter | Search, filter, sort, pagination and chips are embedded inside their owning browse/list use case: community feed, expert directory, content hub, records timeline, moderation queue and admin lists. |
| D-03: Keep meaningful views | A browse/detail/dashboard use case remains when consuming a protected domain result is itself a meaningful user outcome, for example journey dashboard, health-record timeline, community question detail or expert profile. |
| D-04: No duplicate platform UCs | The same expert business outcome on Expert App and Expert Portal is one use case with both platforms listed. |
| D-05: No dialog UCs | Confirmation dialogs, modal state, raw upload preview and page-only navigation are screen behaviors inside the owning function. |
| D-06: V2 boundary | Paid consultation/payment/realtime session workflows and partner clinic/sponsored-service workflows remain Version 2. They receive no active Release 1 UC. |
| D-07: Cross-cutting safety | Safety, compliance, privacy, consent enforcement, dangerous-advice blocking and audit logging apply to all relevant use cases as controls, not as artificial standalone user functions. |


## 3.2 Renumbered Major Features and Release Order

| Code | Feature | Priority | Release Scope | Purpose |
| --- | --- | --- | --- | --- |
| MF-01 | Account, Trust & Access Control | P0 - Core | Current Release | Provides account lifecycle, public community identity separated from private care data, consent management, notification preferences, session protection, role-based access, expert trust review and sensitive access audit review. |
| MF-02 | Mother Care Journey | P0 - Core | Current Release | Supports preconception, pregnancy and postpartum recovery with stage tracking, maternal indicators, recovery logs, preparation checklists and reviewed pregnancy exercise support. It does not assess medical fitness or replace professional supervision. |
| MF-03 | Baby Care Journey, Growth & Vaccination | P0 - Core | Current Release | Supports baby profiles, daily care journals, milestones, growth measurements and vaccination records/reference schedules. It supports observation and appointment preparation, not diagnosis. |
| MF-04 | Community Q&A & Moderation | P0 - Core | Current Release | Provides topic-based and optionally anonymous questions, experience sharing, verified expert answers, reports, moderation and topic governance. Search and filters remain embedded in the owning browse screens. |
| MF-05 | Verified Expert Network & Contribution | P0 - Core | Current Release | Manages expert profiles, credential review, specialty/service scope, availability, directory visibility, expert contribution, badges and trust restrictions. Contribution points show participation, not clinical competence. |
| MF-06 | AI Nurse Assistant & Risk Triage | P0 - Core | Current Release | Collects structured symptom context, returns non-diagnostic green/yellow/red risk orientation and safe next-step guidance using approved knowledge and red-flag rules. |
| MF-07 | Emergency Map & Nearby Care Support | P0 - Core | Current Release | Provides emergency map entry, nearby facilities, route/ETA, quick call/navigation, time-limited location alerts and consented nearby-support requests. It does not dispatch ambulances or guarantee expert arrival. |
| MF-08 | Personal Health Records & Source Labeling | P1 - Supporting | Current Release after P0 | Stores user-entered maternal and child records, protected attachments, source labels and summaries. Sharing is always controlled by explicit consent. |
| MF-09 | Reminders, Tasks & Care Plan | P1 - Supporting | Current Release after P0 | Organizes appointment, medicine/vitamin and vaccination reminders plus today tasks. It is a personal organization tool, not a prescription or formal treatment plan. |
| MF-10 | Family Sync & Cooperative Care | P1 - Supporting | Current Release after P0 | Supports care groups, invitations, scoped family access, shared calendar/data/alerts and assigned care tasks. |
| MF-11 | Verified Content & Checklist Hub | P1 - Supporting | Current Release after P0 | Provides reviewed stage-based articles, FAQs and actionable checklists with source labels, review status, versioning and content governance. |
| MF-12 | Expense & Preparation Planner | P1 - Supporting | Current Release after P0 | Helps households record preparation expenses and view simple summaries. It is not accounting, billing, insurance or financial advice. |
| MF-13 | Connected Device & Health Data Integration | P1 - Supporting | Current Release after P0 | Supports opt-in health-platform/device connection, controlled import, source/quality labels, trends and disconnect/deletion management. |
| MF-14 | Smart Activity Monitoring & Safety Support | P1 - Supporting | Current Release after P0 | Uses phone IMU only after consent to detect suspected falls/impacts, request safety confirmation, alert selected contacts and collect false-positive feedback. It is not a certified fall detector or emergency dispatch system. |
| MF-15 | Paid Direct Consultation & Commission | V2 - Deferred | Version 2 only | Future booking, payment, realtime consultation, temporary record sharing, commission, disputes and refunds. No active Release 1 use case, screen or integration is retained. |
| MF-16 | Partner Clinic & Sponsored Service Management | V2 - Deferred | Version 2 only | Future partner verification, approved service/reference listings, referrals and clearly labeled sponsored campaigns. No active Release 1 use case, screen or integration is retained. |


### 3.2.1 Cross-cutting Control

| Code | Control | Treatment |
| --- | --- | --- |
| CC-01 | Safety, Compliance & Abuse Prevention | Cross-cutting control for disclaimers, dangerous-advice blocking, privacy, consent, audit, expert-access restrictions and location/device safety. It is not an independent user use case. |


## 3.3 Use Case Catalogue (UC-01 to UC-121)

| ID | Use Case | Feature | Primary Actor | Platform | Priority | Description |
| --- | --- | --- | --- | --- | --- | --- |
| UC-01 | Đăng ký tài khoản (Register Account) | MF-01 | Guest | Mobile App / Web Portal | P0 | Creates an account with a supported contact method and selected initial role. The new account remains unverified until OTP confirmation succeeds. |
| UC-02 | Xác thực OTP (Verify OTP) | MF-01 | Guest | Mobile App / Web Portal | P0 | Validates the OTP sent for account activation or another sensitive account action. |
| UC-03 | Đăng nhập (Log In) | MF-01 | User | Mobile App / Web Portal | P0 | Authenticates a verified account and routes the user to the role-appropriate workspace. |
| UC-04 | Đăng xuất (Log Out) | MF-01 | User | Mobile App / Web Portal | P0 | Ends the current session on the active device without deleting the account. |
| UC-05 | Yêu cầu đặt lại mật khẩu (Request Password Reset) | MF-01 | Guest | Mobile App / Web Portal | P0 | Starts password recovery by requesting a time-limited reset proof through the registered channel. |
| UC-06 | Đặt lại mật khẩu (Reset Password) | MF-01 | Guest | Mobile App / Web Portal | P0 | Sets a new password only after a valid recovery proof is verified. |
| UC-07 | Đổi mật khẩu (Change Password) | MF-01 | User | Mobile App / Web Portal | P0 | Changes the signed-in user password after current-password validation. |
| UC-08 | Xem hồ sơ tài khoản (View Account Profile) | MF-01 | User | Mobile App / Web Portal | P0 | Shows the user private account profile, role, status and permitted account controls. |
| UC-09 | Cập nhật hồ sơ tài khoản (Update Account Profile) | MF-01 | User | Mobile App / Web Portal | P0 | Updates non-sensitive private profile data used for account operation and contact preferences. |
| UC-10 | Quản lý danh tính cộng đồng (Manage Community Identity) | MF-01 | User | Mobile App / Web Portal | P0 | Creates or updates a public community identity that is separate from private maternal, baby and family information. |
| UC-11 | Quản lý tùy chọn thông báo (Manage Notification Preferences) | MF-01 | User | Mobile App / Web Portal | P0 | Lets the user choose supported delivery channels and categories for reminders, community replies, family alerts and account events. |
| UC-12 | Xem và đánh dấu thông báo (View and Mark Notifications) | MF-01 | User | Mobile App / Web Portal | P0 | Shows authorized notifications and allows the user to mark individual or all eligible notifications as read. |
| UC-13 | Quản lý phiên đăng nhập của tôi (Manage Own Login Sessions) | MF-01 | User | Mobile App / Web Portal | P0 | Lists active or recent own sessions and allows the user to revoke a selected device session. |
| UC-14 | Vô hiệu hóa hoặc xóa tài khoản cá nhân (Deactivate or Delete Own Account) | MF-01 | User | Mobile App / Web Portal | P0 | Requests account deactivation or deletion subject to retention, care-group and audit obligations. |
| UC-15 | Cấp quyền chia sẻ dữ liệu (Grant Data Permission) | MF-01 | User | Mobile App / Web Portal | P0 | Creates a purpose-specific, scope-based and time-limited permission for a family member or verified expert to access selected data. |
| UC-16 | Rà soát và thu hồi quyền chia sẻ dữ liệu (Review and Revoke Data Permission) | MF-01 | User | Mobile App / Web Portal | P0 | Shows active and past sharing permissions and lets the owner revoke an active grant. |
| UC-17 | Quản trị tài khoản và quyền truy cập (Administer User Accounts and Role Access) | MF-01 | System Admin | Admin Portal | P0 | Allows authorized administrators to review account status, apply role/access changes and restrict misuse under separation-of-duties controls. |
| UC-18 | Rà soát truy cập nhạy cảm và sự kiện bảo mật (Review Sensitive Access and Security Events) | MF-01 | System Admin | Admin Portal | P0 | Reviews sensitive-record access, abnormal login, permission change and file-access events to determine follow-up or investigation. |
| UC-19 | Khởi tạo hành trình chăm sóc mẹ (Initialize Mother Care Journey) | MF-02 | Mother | Mother Mobile App | P0 | Creates a mother journey for preconception, pregnancy or postpartum recovery using the minimum dates and stage context required for stage-based support. |
| UC-20 | Cập nhật giai đoạn và ngày hành trình mẹ (Update Mother Journey Stage and Dates) | MF-02 | Mother | Mother Mobile App | P0 | Updates stage, last menstrual period, expected due date, birth date or other permitted journey dates when circumstances change. |
| UC-21 | Xem bảng điều khiển hành trình mẹ (View Mother Journey Dashboard) | MF-02 | Mother | Mother Mobile App | P0 | Shows current stage/week, relevant care priorities, reminders, checklists, reviewed content and safe shortcuts. |
| UC-22 | Thêm chỉ số sức khỏe mẹ (Add Maternal Health Metric) | MF-02 | Mother | Mother Mobile App | P0 | Records a user-entered maternal indicator such as weight, blood pressure, glucose, fetal movement note or another supported observation. |
| UC-23 | Cập nhật hoặc xóa chỉ số sức khỏe mẹ (Update or Delete Maternal Health Metric) | MF-02 | Mother | Mother Mobile App | P0 | Corrects or removes a user-entered maternal metric when ownership and record state allow. |
| UC-24 | Xem xu hướng chỉ số sức khỏe mẹ (View Maternal Health Trend) | MF-02 | Mother | Mother Mobile App | P0 | Displays time-based trends for recorded maternal indicators with source labels and clear non-diagnostic context. |
| UC-25 | Ghi nhật ký phục hồi sau sinh (Add Postpartum Recovery Log) | MF-02 | Mother | Mother Mobile App | P0 | Records a postpartum recovery observation such as sleep, mood, milk-feeding context, pain note or recovery symptom note. |
| UC-26 | Cập nhật hoặc xóa nhật ký phục hồi sau sinh (Update or Delete Postpartum Recovery Log) | MF-02 | Mother | Mother Mobile App | P0 | Corrects or removes an owner-entered postpartum recovery log. |
| UC-27 | Quản lý checklist chuẩn bị (Manage Preparation Checklist) | MF-02 | Mother | Mother Mobile App | P0 | Adds, edits and completes preparation items for pregnancy, hospital bag, postpartum recovery or early baby care. |
| UC-28 | Duyệt thư viện bài tập thai kỳ (Browse Pregnancy Exercise Library) | MF-02 | Mother | Mother Mobile App | P0 | Shows reviewed pregnancy exercise content with embedded stage/difficulty/duration filters and exercise-specific safety notes. |
| UC-29 | Hoàn thành kiểm tra an toàn trước tập (Complete Pre-exercise Safety Check) | MF-02 | Mother | Mother Mobile App | P0 | Collects required pre-exercise safety answers and stops the session entry when configured warning answers are present. |
| UC-30 | Thực hiện phiên tập thai kỳ có phản hồi tư thế tùy chọn (Conduct Pregnancy Exercise Session with Optional Posture Feedback) | MF-02 | Mother | Mother Mobile App | P0 | Runs an approved exercise session with start, pause, resume and complete actions and optional rule/ML-based posture feedback after camera consent. |
| UC-31 | Xem lịch sử và kết quả phiên tập (View Exercise History and Session Result) | MF-02 | Mother | Mother Mobile App | P0 | Shows completed exercise sessions, duration, completion state, aggregate posture feedback and safety notices. |
| UC-32 | Tạo hồ sơ bé (Create Baby Profile) | MF-03 | Mother | Mother Mobile App | P0 | Creates a baby profile with core identity and birth context needed for journals, growth and vaccination tracking. |
| UC-33 | Cập nhật hoặc lưu trữ hồ sơ bé (Update or Archive Baby Profile) | MF-03 | Mother | Mother Mobile App | P0 | Updates permitted baby profile fields or archives a profile that is no longer actively managed without destroying linked history. |
| UC-34 | Chuyển hồ sơ bé đang theo dõi (Switch Active Baby Profile) | MF-03 | Mother | Mother Mobile App | P0 | Selects the baby profile used by the current baby dashboard, journals, growth views and relevant reminders. |
| UC-35 | Xem tổng quan chăm sóc bé (View Baby Care Overview) | MF-03 | Mother | Mother Mobile App | P0 | Shows a baby overview with recent journals, growth, milestones, vaccination status and current care prompts. |
| UC-36 | Thêm nhật ký chăm sóc bé hằng ngày (Add Baby Daily Log) | MF-03 | Mother | Mother Mobile App | P0 | Records feeding, sleep, diaper, symptom note or other supported daily care observation for a selected baby. |
| UC-37 | Cập nhật hoặc xóa nhật ký chăm sóc bé (Update or Delete Baby Daily Log) | MF-03 | Mother | Mother Mobile App | P0 | Corrects or removes an owner-entered baby journal entry. |
| UC-38 | Xem tổng hợp nhật ký bé (View Baby Log Summary) | MF-03 | Mother | Mother Mobile App | P0 | Shows recent 24-hour or 7-day feeding, sleep, diaper and logged observation patterns for the active baby. |
| UC-39 | Ghi mốc phát triển (Record Development Milestone) | MF-03 | Mother | Mother Mobile App | P0 | Records a caregiver-observed development milestone with date and note. |
| UC-40 | Cập nhật hoặc xóa mốc phát triển (Update or Delete Development Milestone) | MF-03 | Mother | Mother Mobile App | P0 | Corrects or removes a caregiver-entered development milestone. |
| UC-41 | Thêm số đo tăng trưởng (Add Growth Measurement) | MF-03 | Mother | Mother Mobile App | P0 | Records supported growth measurements for a selected baby with measurement date and source. |
| UC-42 | Cập nhật hoặc xóa số đo tăng trưởng (Update or Delete Growth Measurement) | MF-03 | Mother | Mother Mobile App | P0 | Corrects or removes a user-entered growth measurement. |
| UC-43 | Xem xu hướng và lịch sử tăng trưởng (View Growth Trend and Measurement History) | MF-03 | Mother | Mother Mobile App | P0 | Displays growth measurement history and charts with reference context and prompts to seek professional assessment when appropriate. |
| UC-44 | Quản lý bản ghi tiêm chủng (Manage Vaccination Record) | MF-03 | Mother | Mother Mobile App | P0 | Adds, updates or removes a user-entered vaccination record for the selected baby. |
| UC-45 | Xem lịch tiêm tham khảo và trạng thái nhắc lịch (View Vaccination Reference Schedule and Reminder Status) | MF-03 | Mother | Mother Mobile App | P0 | Displays a reference vaccination schedule, completed/recorded status and linked reminder state for the selected baby. |
| UC-46 | Duyệt bảng tin và chủ đề cộng đồng (Browse Community Feed and Topics) | MF-04 | User | Mobile App | P0 | Displays moderated questions and topic collections. Keyword search, topic chips, filters, sort and pagination are embedded controls of this browse outcome. |
| UC-47 | Xem chi tiết câu hỏi cộng đồng (View Community Question Detail) | MF-04 | User | Mobile App | P0 | Shows a permitted question, its answer thread, source labels, moderation state and allowed interactions. |
| UC-48 | Đăng câu hỏi cộng đồng (Create Community Question) | MF-04 | User | Mobile App | P0 | Creates a topic-based community question and optionally applies anonymous public display while preserving internal accountability. |
| UC-49 | Chỉnh sửa hoặc xóa bài đăng cộng đồng của tôi (Edit or Delete Own Community Post) | MF-04 | User | Mobile App | P0 | Edits or removes the user own community post while the post is not locked by moderation or investigation. |
| UC-50 | Trả lời cộng đồng (Post Community Answer) | MF-04 | User | Mobile App / Expert Portal | P0 | Posts a personal-experience answer or a verified expert answer with the applicable source/role label. |
| UC-51 | Chỉnh sửa hoặc xóa câu trả lời của tôi (Edit or Delete Own Community Answer) | MF-04 | User | Mobile App / Expert Portal | P0 | Edits or removes the user own answer when no moderation lock or evidence hold applies. |
| UC-52 | Phản ứng với câu trả lời cộng đồng (React to Community Answer) | MF-04 | User | Mobile App | P0 | Adds or removes a permitted helpfulness reaction to a community answer. |
| UC-53 | Đánh dấu bài đăng cộng đồng (Bookmark Community Post) | MF-04 | User | Mobile App | P0 | Saves or removes a permitted post from the user private bookmark list. |
| UC-54 | Theo dõi hoặc bỏ theo dõi chủ đề cộng đồng (Follow or Unfollow Community Topic) | MF-04 | User | Mobile App | P0 | Subscribes or unsubscribes the user from a community topic for feed relevance and optional notifications. |
| UC-55 | Báo cáo nội dung hoặc tài khoản không an toàn (Report Unsafe Community Content or Account) | MF-04 | User | Mobile App / Web Portal | P0 | Submits a report for misinformation, dangerous advice, hidden advertising, harassment, privacy breach or suspicious account behavior. |
| UC-56 | Xem hàng đợi kiểm duyệt (View Moderation Queue) | MF-04 | Moderator | Admin Portal | P0 | Shows reported, automatically flagged and pre-publication content awaiting authorized moderator review. |
| UC-57 | Kiểm duyệt nội dung cộng đồng (Moderate Community Content) | MF-04 | Moderator | Admin Portal | P0 | Approves, hides, locks, labels or requests revision for a community post or answer based on policy and safety rules. |
| UC-58 | Giải quyết báo cáo và áp dụng xử lý vi phạm (Resolve Content or Account Report and Apply Enforcement) | MF-04 | Moderator / System Admin | Admin Portal | P0 | Resolves a report and, when authorized, applies warning, posting restriction, suspension escalation or no-action decision. |
| UC-59 | Quản lý chủ đề và trạng thái hiển thị cộng đồng (Manage Community Topics and Visibility) | MF-04 | Moderator / Content Admin | Admin Portal | P0 | Creates, updates, reorders or disables community topics and their visibility rules. |
| UC-60 | Nộp hồ sơ chuyên gia (Submit Expert Profile) | MF-05 | Expert Applicant | Expert Portal / Expert App | P0 | Creates an expert application profile with specialty, experience, support scope and public professional information. |
| UC-61 | Cập nhật hồ sơ chuyên gia (Update Expert Profile) | MF-05 | Verified Expert | Expert Portal / Expert App | P0 | Updates approved professional profile fields and public service information subject to review rules. |
| UC-62 | Nộp hoặc thay thế giấy tờ xác thực (Submit or Replace Verification Documents) | MF-05 | Expert Applicant / Verified Expert | Expert Portal / Expert App | P0 | Uploads required credentials, certificates and supporting evidence for verification or renewal. |
| UC-63 | Xem trạng thái xác thực và nộp gia hạn (View Verification Status and Renew Submission) | MF-05 | Expert Applicant / Verified Expert | Expert Portal / Expert App | P0 | Shows verification result, required corrections, expiry status and allows a renewal submission when eligible. |
| UC-64 | Cấu hình trạng thái sẵn sàng và phạm vi hỗ trợ chuyên gia (Configure Expert Availability and Service Scope) | MF-05 | Verified Expert | Expert Portal / Expert App | P0 | Configures availability, support methods, service area and public readiness state for the expert directory and nearby support eligibility. |
| UC-65 | Duyệt danh mục chuyên gia đã xác thực (Browse Verified Expert Directory) | MF-05 | User | Mobile App | P0 | Displays verified experts with embedded search and filters by specialty, availability, badge and consented service area. |
| UC-66 | Xem hồ sơ chuyên gia đã xác thực (View Verified Expert Profile) | MF-05 | User | Mobile App | P0 | Shows a verified expert public professional profile, specialties, supported scope, availability and contribution reputation. |
| UC-67 | Xem hàng đợi câu hỏi chuyên môn (View Expert Question Queue) | MF-05 | Verified Expert | Expert Portal / Expert App | P0 | Shows community questions matched to the expert verified specialties and public support scope. |
| UC-68 | Đăng trả lời với nhãn chuyên gia (Post Verified Expert Answer) | MF-05 | Verified Expert | Expert Portal / Expert App | P0 | Posts an expert answer with verified badge, source/safety label and contribution attribution. |
| UC-69 | Xem điểm đóng góp và huy hiệu (View Contribution Points and Badges) | MF-05 | Verified Expert | Expert Portal / Expert App | P0 | Shows contribution points, badges and qualifying community activities used to recognize responsible participation. |
| UC-70 | Rà soát hồ sơ xác thực chuyên gia (Review Expert Verification Submission) | MF-05 | System Admin | Admin Portal | P0 | Approves, requests supplementation, rejects or renews an expert verification submission after credential review. |
| UC-71 | Hạn chế, đình chỉ hoặc khôi phục trạng thái tin cậy chuyên gia (Restrict, Suspend or Reinstate Expert Trust Status) | MF-05 | System Admin | Admin Portal | P0 | Applies, updates or lifts an expert restriction/suspension when credentials expire or policy violations require it. |
| UC-72 | Chạy tiếp nhận triệu chứng bằng AI (Run AI Symptom Intake) | MF-06 | Mother | Mother Mobile App | P0 | Collects structured symptom information, timing, severity and permitted context through a guided intake flow. |
| UC-73 | Xem kết quả phân loại rủi ro (View Risk Triage Result) | MF-06 | Mother | Mother Mobile App | P0 | Shows non-diagnostic green, yellow or red orientation with safe next-step guidance and clearly labelled uncertainty. |
| UC-74 | Mở hỗ trợ khẩn cấp từ kết quả rủi ro đỏ (Open Emergency Support from a Red Risk Result) | MF-06 | Mother | Mother Mobile App | P0 | Transfers a user from a red-risk result to the Emergency Map and Nearby Care Support flow without treating the result as a diagnosis. |
| UC-75 | Quản lý nguồn tri thức AI đã phê duyệt (Manage Approved AI Knowledge Sources) | MF-06 | Content Admin / System Admin | Admin Portal | P0 | Registers, reviews, versions or disables approved knowledge sources available to AI retrieval and safe answer composition. |
| UC-76 | Cấu hình quy tắc rủi ro và cờ đỏ AI (Configure AI Risk and Red-Flag Rules) | MF-06 | System Admin | Admin Portal | P0 | Maintains conservative risk thresholds, red-flag patterns, safe fallback actions and rule version history. |
| UC-77 | Mở bản đồ hỗ trợ khẩn cấp (Open Emergency Map) | MF-07 | Mother | Mother Mobile App | P0 | Opens the shared emergency map after informing the user about location permission and the non-dispatch scope. |
| UC-78 | Tìm cơ sở chăm sóc gần đây (Find Nearby Care Facilities) | MF-07 | Mother | Mother Mobile App | P0 | Finds permitted nearby care facilities based on the user approved location or a selected area. |
| UC-79 | Xem lộ trình, ETA và gọi nhanh hoặc chỉ đường (View Route, ETA and Quick Call or Navigate) | MF-07 | Mother | Mother Mobile App | P0 | Shows route/ETA and enables quick call or navigation to a selected care facility through the map/device capability. |
| UC-80 | Chia sẻ vị trí có thời hạn và gửi cảnh báo khẩn cấp cho gia đình (Share Time-Limited Location and Send Family Emergency Alert) | MF-07 | Mother | Mother Mobile App | P0 | Shares minimum necessary location/context for a time-limited emergency alert to selected family recipients. |
| UC-81 | Tạo hoặc hủy yêu cầu hỗ trợ gần vị trí (Create or Cancel Nearby Support Request) | MF-07 | Mother | Mother Mobile App | P0 | Creates or cancels a consented nearby-support request visible only to eligible verified experts; no expert arrival is guaranteed. |
| UC-82 | Quản lý khả dụng gần vị trí và phản hồi yêu cầu hỗ trợ (Manage Expert Nearby Availability and Respond to Nearby Support Request) | MF-07 | Verified Expert | Expert App / Expert Portal | P0 | Lets an eligible verified expert opt in to nearby availability, view minimal request context and accept, decline or stop responding to a request. |
| UC-83 | Thêm hồ sơ sức khỏe cá nhân và tệp đính kèm (Add Personal Health Record and Attachment) | MF-08 | Mother | Mother Mobile App | P1 | Creates a maternal or baby health record and attaches permitted documents or images with type, date and source metadata. |
| UC-84 | Cập nhật siêu dữ liệu hồ sơ sức khỏe (Update Health Record Metadata) | MF-08 | Mother | Mother Mobile App | P1 | Updates title, category, date, source label, tags or note of an owner-entered health record without altering the protected file itself unless allowed. |
| UC-85 | Lưu trữ hoặc xóa hồ sơ sức khỏe do người dùng nhập (Archive or Delete User-entered Health Record) | MF-08 | Mother | Mother Mobile App | P1 | Archives or requests deletion of an owner-entered record when retention, sharing and safety-evidence obligations allow. |
| UC-86 | Xem timeline và chi tiết hồ sơ sức khỏe (View Health Record Timeline and Detail) | MF-08 | Mother | Mother Mobile App | P1 | Shows authorized maternal and baby records by time, category and source with embedded filtering inside the timeline. |
| UC-87 | Tạo bản tóm tắt sức khỏe (Generate Health Summary) | MF-08 | Mother | Mother Mobile App | P1 | Creates a selected-period summary from user-chosen maternal or baby records for personal review or permitted sharing. |
| UC-88 | Chia sẻ bản tóm tắt hoặc hồ sơ đã chọn theo consent (Share Health Summary or Selected Records Under Consent) | MF-08 | Mother | Mother Mobile App | P1 | Shares selected records or a summary only through an active MF-01 consent grant with scope and expiry. |
| UC-89 | Tạo nhắc lịch hẹn (Create Appointment Reminder) | MF-09 | Mother | Mother Mobile App | P1 | Creates a reminder for a self-entered appointment, follow-up, examination, test or other care event. |
| UC-90 | Tạo nhắc thuốc hoặc vitamin (Create Medicine or Vitamin Reminder) | MF-09 | Mother | Mother Mobile App | P1 | Creates a user-entered medicine or vitamin reminder based on prior professional advice or personal routine. |
| UC-91 | Tạo nhắc lịch tiêm (Create Vaccination Reminder) | MF-09 | Mother | Mother Mobile App | P1 | Creates or confirms a reminder linked to the selected baby vaccination record or reference schedule. |
| UC-92 | Cập nhật, hoãn, hoàn thành, bỏ qua hoặc xóa nhắc lịch (Update, Snooze, Complete, Skip or Delete Care Reminder) | MF-09 | Mother | Mother Mobile App | P1 | Manages the lifecycle of a user reminder, including time updates, snooze, complete, skip and deletion where permitted. |
| UC-93 | Xem việc cần làm hôm nay và kế hoạch chăm sóc (View Today Tasks and Care Plan) | MF-09 | Mother | Mother Mobile App | P1 | Shows due reminders, checklist items and permitted family tasks in a prioritized today view. |
| UC-94 | Tạo nhóm chăm sóc (Create Care Group) | MF-10 | Mother | Mother Mobile App | P1 | Creates a care group for a mother, baby or household care context with the creator as initial owner. |
| UC-95 | Mời hoặc thu hồi lời mời người thân (Invite or Revoke Family Member Invitation) | MF-10 | Mother | Mother Mobile App | P1 | Sends or revokes a care-group invitation using a permitted contact or invite link. |
| UC-96 | Chấp nhận hoặc từ chối lời mời nhóm chăm sóc (Accept or Reject Care Group Invitation) | MF-10 | Family Member | Family Mobile App | P1 | Lets a recipient accept or reject an active care-group invitation after authentication. |
| UC-97 | Quản lý thành viên nhóm chăm sóc (Manage Care Group Membership) | MF-10 | Mother / Family Member | Mother Mobile App / Family Mobile App | P1 | Allows a group owner to remove a member and an individual member to leave a group where rules permit. |
| UC-98 | Quản lý phạm vi quyền của người thân (Manage Family Permission Scope) | MF-10 | Mother | Mother Mobile App | P1 | Defines and updates family access to selected calendar, tasks, alerts, logs, summaries or records. |
| UC-99 | Tạo, cập nhật hoặc hủy nhiệm vụ chăm sóc gia đình (Create, Update or Cancel Family Care Task) | MF-10 | Mother | Mother Mobile App | P1 | Creates, edits or cancels a care task assigned to an eligible care-group member. |
| UC-100 | Cập nhật trạng thái nhiệm vụ được giao (Update Assigned Task Status) | MF-10 | Family Member | Family Mobile App | P1 | Updates the status of a task assigned to the signed-in family member, such as in progress, completed or needs help. |
| UC-101 | Xem lịch, dữ liệu và cảnh báo chăm sóc được chia sẻ (View Shared Care Calendar, Data and Alerts) | MF-10 | Family Member | Family Mobile App | P1 | Displays calendar items, shared care data and family alerts only within the member active permission scope. |
| UC-102 | Duyệt nội dung đã xác thực (Browse Verified Content) | MF-11 | User | Mobile App | P1 | Displays approved articles, FAQs and checklists with embedded keyword, stage and topic controls. |
| UC-103 | Xem chi tiết nội dung đã xác thực (View Verified Content Detail) | MF-11 | User | Mobile App | P1 | Shows the selected article, FAQ or checklist with source, review/version state, updated date and safety notes. |
| UC-104 | Tạo nội dung đã xác thực (Create Verified Content) | MF-11 | Content Admin | Admin Portal | P1 | Creates a draft article, FAQ or checklist with stage/topic mapping, sources and initial review state. |
| UC-105 | Cập nhật nội dung đã xác thực và nguồn (Update Verified Content and Sources) | MF-11 | Content Admin | Admin Portal | P1 | Updates an existing content draft or creates a new version when published content needs changes to body, source, tag or safety notes. |
| UC-106 | Rà soát và xuất bản phiên bản nội dung (Review and Publish Content Version) | MF-11 | System Admin / Content Admin | Admin Portal | P1 | Reviews a content version and approves, rejects or requests revision before public publication. |
| UC-107 | Gỡ xuất bản hoặc lưu trữ nội dung (Unpublish or Archive Content) | MF-11 | Content Admin / System Admin | Admin Portal | P1 | Withdraws or archives content that is outdated, unsafe, incorrect or replaced by a newer version. |
| UC-108 | Quản lý danh mục và ánh xạ giai đoạn/chủ đề nội dung (Manage Content Categories and Stage/Topic Mapping) | MF-11 | Content Admin | Admin Portal | P1 | Maintains categories, stage mappings, topic tags and display order used by the content hub. |
| UC-109 | Thêm khoản chi chuẩn bị (Add Expense Entry) | MF-12 | Mother | Mother Mobile App | P1 | Records a household preparation expense with category, amount, date and optional experience note. |
| UC-110 | Cập nhật hoặc xóa khoản chi chuẩn bị (Update or Delete Expense Entry) | MF-12 | Mother | Mother Mobile App | P1 | Corrects or deletes an owner-entered preparation expense. |
| UC-111 | Xem tổng hợp chi phí chuẩn bị (View Expense Summary) | MF-12 | Mother | Mother Mobile App | P1 | Shows simple expense totals by category, month or journey/baby period. |
| UC-112 | Kết nối thiết bị hoặc nền tảng sức khỏe (Connect Health Device or Platform) | MF-13 | Mother | Mother Mobile App | P1 | Connects a supported device/platform after the user reviews permissions and selected indicator scope. |
| UC-113 | Nhập hoặc đồng bộ quan sát từ thiết bị (Import or Synchronize Device Observations) | MF-13 | Mother | Mother Mobile App | P1 | Imports or synchronizes selected device observations such as heart rate, sleep, steps, SpO2, temperature or blood pressure where supported. |
| UC-114 | Xem xu hướng và chất lượng dữ liệu thiết bị (View Device Data Trend and Quality) | MF-13 | Mother | Mother Mobile App | P1 | Displays imported data trends, source labels, data gaps and quality cautions for the selected indicator. |
| UC-115 | Ngắt kết nối thiết bị và xóa dữ liệu đã nhập (Disconnect Device and Delete Imported Data) | MF-13 | Mother | Mother Mobile App | P1 | Stops future synchronization and allows the user to request deletion of imported data according to retention rules. |
| UC-116 | Quản lý liên hệ khẩn cấp (Manage Emergency Contacts) | MF-14 | Mother | Mother Mobile App | P1 | Adds, verifies, reprioritizes or removes emergency alert recipients used by safety monitoring and emergency flows. |
| UC-117 | Cấu hình giám sát hoạt động thông minh (Configure Smart Activity Monitoring) | MF-14 | Mother | Mother Mobile App | P1 | Configures sensor consent, alert recipients, countdown, location-sharing option and monitoring conditions. |
| UC-118 | Bật giám sát hoạt động thông minh (Enable Smart Activity Monitoring) | MF-14 | Mother | Mother Mobile App | P1 | Starts IMU-based monitoring after configuration, permissions and consent are valid. |
| UC-119 | Tắt giám sát hoạt động thông minh (Disable Smart Activity Monitoring) | MF-14 | Mother | Mother Mobile App | P1 | Stops IMU monitoring and prevents new candidate safety events from being created after the state change. |
| UC-120 | Xử lý sự kiện nghi ngờ ngã hoặc va chạm và kiểm tra an toàn (Handle Suspected Fall or Impact and Safety Check) | MF-14 | Mother / Phone IMU Sensor | Mother Mobile App | P1 | Detects a candidate fall/impact, opens a safety countdown and records I am OK, Need Help or emergency-support handoff response. |
| UC-121 | Xem lịch sử sự kiện an toàn và báo cáo phát hiện nhầm (Review Safety Event History and Report False Positive) | MF-14 | Mother | Mother Mobile App | P1 | Shows prior safety events and lets the user mark a false detection with a reason to improve future threshold evaluation. |


## 3.4 Detailed Functional Specifications

### UC-01 Đăng ký tài khoản (Register Account)

| Field | Specification |
| --- | --- |
| Primary Actor | Guest |
| Secondary Actors / Services | OTP / Email Service |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | Guest selects Register Account. |
| Description | Creates an account with a supported contact method and selected initial role. The new account remains unverified until OTP confirmation succeeds. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested account outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Guest opens the Register Account function.<br>2. The system loads the required authorized context and input fields for the account.<br>3. Guest provides email or phone, password, initial role.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the account, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-ACCOUNT-01: The contact identifier must be unique.<br>BR-ACCOUNT-02: The account is not active until verification succeeds. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-02 Xác thực OTP (Verify OTP)

| Field | Specification |
| --- | --- |
| Primary Actor | Guest |
| Secondary Actors / Services | OTP / Email Service |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | Guest submits an OTP code. |
| Description | Validates the OTP sent for account activation or another sensitive account action. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested OTP proof outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Guest starts Verify OTP.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Guest provides OTP code and target action.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the OTP proof action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-ACCOUNT-03: OTP attempts are rate-limited and expire.<br>BR-ACCOUNT-04: Failed verification must not reveal whether unrelated accounts exist. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-03 Đăng nhập (Log In)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User submits credentials on the login screen. |
| Description | Authenticates a verified account and routes the user to the role-appropriate workspace. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested account session outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Log In.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides identifier and password.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the account session action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-RBAC-01: Routing is based on effective role and account status.<br>BR-SECURITY-01: Suspended or deactivated accounts cannot create a session. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-04 Đăng xuất (Log Out)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User selects Log Out. |
| Description | Ends the current session on the active device without deleting the account. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested current session outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Log Out.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides the required selection, confirmation or input.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the current session action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-SECURITY-02: The current refresh token/session must be revoked. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-05 Yêu cầu đặt lại mật khẩu (Request Password Reset)

| Field | Specification |
| --- | --- |
| Primary Actor | Guest |
| Secondary Actors / Services | OTP / Email Service |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | Guest selects Forgot Password. |
| Description | Starts password recovery by requesting a time-limited reset proof through the registered channel. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested password reset outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Guest starts Request Password Reset.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Guest provides registered email or phone.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the password reset action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-ACCOUNT-05: The system returns a neutral message whether or not the identifier exists.<br>BR-SECURITY-03: Reset requests are rate-limited. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-06 Đặt lại mật khẩu (Reset Password)

| Field | Specification |
| --- | --- |
| Primary Actor | Guest |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | Guest opens a valid recovery link or submits a valid reset code. |
| Description | Sets a new password only after a valid recovery proof is verified. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested password outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Guest opens the selected password.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Guest selects an allowed action and provides reset proof and new password.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the password, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-SECURITY-04: Existing sessions are revoked after a successful password reset. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-07 Đổi mật khẩu (Change Password)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User selects Change Password. |
| Description | Changes the signed-in user password after current-password validation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested password outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected password.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides current password and new password.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the password, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-SECURITY-05: The current password must be validated before the change. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-08 Xem hồ sơ tài khoản (View Account Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User opens Account Profile. |
| Description | Shows the user private account profile, role, status and permitted account controls. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested account profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the View Account Profile function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted account profile data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-PRIVACY-01: Private account data is never displayed in public community contexts. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-09 Cập nhật hồ sơ tài khoản (Update Account Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User saves profile changes. |
| Description | Updates non-sensitive private profile data used for account operation and contact preferences. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested account profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected account profile.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides name, avatar, area, permitted contact fields.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the account profile, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-ACCOUNT-06: Sensitive identity changes may require re-verification. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-10 Quản lý danh tính cộng đồng (Manage Community Identity)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User opens Community Identity settings. |
| Description | Creates or updates a public community identity that is separate from private maternal, baby and family information. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community identity outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected community identity.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides display name, avatar, visibility settings.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the community identity, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-PRIVACY-02: Community identity must not expose private health records by default.<br>BR-COMMUNITY-01: Display names and avatars remain subject to moderation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-11 Quản lý tùy chọn thông báo (Manage Notification Preferences)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User changes notification preferences. |
| Description | Lets the user choose supported delivery channels and categories for reminders, community replies, family alerts and account events. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested notification preference outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected notification preference.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides channels and categories.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the notification preference, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-NOTIFY-01: Safety-critical alerts may remain enabled where required by active monitoring consent. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-12 Xem và đánh dấu thông báo (View and Mark Notifications)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User opens Notification Center. |
| Description | Shows authorized notifications and allows the user to mark individual or all eligible notifications as read. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested notifications outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens View and Mark Notifications.<br>2. The system validates access and loads authorized context.<br>3. User provides read state.<br>4. The system applies business rules and records the result.<br>5. The system shows the outcome and refreshes related data. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-NOTIFY-02: Notification deep links must re-check authorization before opening protected data. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-13 Quản lý phiên đăng nhập của tôi (Manage Own Login Sessions)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User opens Login Sessions. |
| Description | Lists active or recent own sessions and allows the user to revoke a selected device session. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested login sessions outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected login sessions.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides selected session.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the login sessions, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-SECURITY-06: A user may not revoke an unrelated account session. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-14 Vô hiệu hóa hoặc xóa tài khoản cá nhân (Deactivate or Delete Own Account)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User submits an account lifecycle request. |
| Description | Requests account deactivation or deletion subject to retention, care-group and audit obligations. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested own account outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected own account.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides deactivation or deletion request.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the own account, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-PRIVACY-03: Deletion follows retention and legal obligations.<br>BR-FAMILY-01: The system explains ownership impact on active care groups. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-15 Cấp quyền chia sẻ dữ liệu (Grant Data Permission)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User confirms a sharing grant. |
| Description | Creates a purpose-specific, scope-based and time-limited permission for a family member or verified expert to access selected data. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested data permission outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Grant Data Permission.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides recipient, data scope, purpose, expiry.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the data permission action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-CONSENT-01: The grant must include recipient, scope, purpose and expiry.<br>BR-CONSENT-02: Sharing is denied until the recipient identity is valid. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-16 Rà soát và thu hồi quyền chia sẻ dữ liệu (Review and Revoke Data Permission)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User opens Sharing History or selects Revoke. |
| Description | Shows active and past sharing permissions and lets the owner revoke an active grant. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested data permission outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Review and Revoke Data Permission.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides selected permission.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the data permission action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-CONSENT-03: Revocation takes effect before the next protected read.<br>BR-AUDIT-01: Grant and revocation events are auditable. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-17 Quản trị tài khoản và quyền truy cập (Administer User Accounts and Role Access)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Admin opens user administration. |
| Description | Allows authorized administrators to review account status, apply role/access changes and restrict misuse under separation-of-duties controls. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested user account and role access outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin opens Administer User Accounts and Role Access.<br>2. The system validates access and loads authorized context.<br>3. System Admin provides target account, role/access decision, reason.<br>4. The system applies business rules and records the result.<br>5. The system shows the outcome and refreshes related data. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-RBAC-02: Administrators may only change roles within assigned authority.<br>BR-AUDIT-02: Access changes require an auditable reason. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-18 Rà soát truy cập nhạy cảm và sự kiện bảo mật (Review Sensitive Access and Security Events)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-01 - Shared - Account, Trust & Access Control |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Admin opens audit and security review. |
| Description | Reviews sensitive-record access, abnormal login, permission change and file-access events to determine follow-up or investigation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested audit/security event outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin opens the Review Sensitive Access and Security Events function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted audit/security event data using any embedded list controls where applicable.<br>4. System Admin may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RBAC: access is limited by effective role and assigned permission.<br>BR-PRIVACY: private maternal, baby and health data are separated from public community identity.<br>BR-AUDIT-03: Audit records are append-only for normal operators.<br>BR-PRIVACY-04: Audit viewers must not receive more health content than necessary. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-19 Khởi tạo hành trình chăm sóc mẹ (Initialize Mother Care Journey)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother starts Journey Setup. |
| Description | Creates a mother journey for preconception, pregnancy or postpartum recovery using the minimum dates and stage context required for stage-based support. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested mother care journey outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Initialize Mother Care Journey function.<br>2. The system loads the required authorized context and input fields for the mother care journey.<br>3. Mother provides journey stage and key dates.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the mother care journey, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-JOURNEY-01: Estimated dates are supportive planning values, not medical diagnosis. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-20 Cập nhật giai đoạn và ngày hành trình mẹ (Update Mother Journey Stage and Dates)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves journey changes. |
| Description | Updates stage, last menstrual period, expected due date, birth date or other permitted journey dates when circumstances change. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested mother care journey outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected mother care journey.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides stage and date fields.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the mother care journey, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-JOURNEY-02: The system recalculates stage-dependent suggestions after valid changes. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-21 Xem bảng điều khiển hành trình mẹ (View Mother Journey Dashboard)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Mother Journey. |
| Description | Shows current stage/week, relevant care priorities, reminders, checklists, reviewed content and safe shortcuts. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested mother journey dashboard outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Mother Journey Dashboard function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted mother journey dashboard data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-JOURNEY-03: The dashboard is informational and does not make clinical conclusions. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-22 Thêm chỉ số sức khỏe mẹ (Add Maternal Health Metric)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves a new maternal metric. |
| Description | Records a user-entered maternal indicator such as weight, blood pressure, glucose, fetal movement note or another supported observation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested maternal health metric outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Maternal Health Metric function.<br>2. The system loads the required authorized context and input fields for the maternal health metric.<br>3. Mother provides metric type, value, time, source, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the maternal health metric, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-METRIC-01: The source and timestamp are required for trend interpretation.<br>BR-HEALTH-BOUNDARY-01: Values are not used to diagnose disease. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-23 Cập nhật hoặc xóa chỉ số sức khỏe mẹ (Update or Delete Maternal Health Metric)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother edits or removes an existing metric. |
| Description | Corrects or removes a user-entered maternal metric when ownership and record state allow. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested maternal health metric outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected maternal health metric.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected metric and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the maternal health metric, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-METRIC-02: Delete is limited to owner-entered data and retains audit metadata when required. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-24 Xem xu hướng chỉ số sức khỏe mẹ (View Maternal Health Trend)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects a metric trend. |
| Description | Displays time-based trends for recorded maternal indicators with source labels and clear non-diagnostic context. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested maternal health trend outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Maternal Health Trend function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted maternal health trend data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-HEALTH-BOUNDARY-02: Charts must not state or imply a medical diagnosis. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-25 Ghi nhật ký phục hồi sau sinh (Add Postpartum Recovery Log)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves a postpartum log. |
| Description | Records a postpartum recovery observation such as sleep, mood, milk-feeding context, pain note or recovery symptom note. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested postpartum recovery log outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Postpartum Recovery Log function.<br>2. The system loads the required authorized context and input fields for the postpartum recovery log.<br>3. Mother provides log type, time, note, optional severity.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the postpartum recovery log, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-POSTPARTUM-01: Sensitive entries remain private unless actively shared. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-26 Cập nhật hoặc xóa nhật ký phục hồi sau sinh (Update or Delete Postpartum Recovery Log)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother edits or removes a postpartum log. |
| Description | Corrects or removes an owner-entered postpartum recovery log. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested postpartum recovery log outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected postpartum recovery log.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected log and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the postpartum recovery log, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-POSTPARTUM-02: Deleted entries follow retention/audit policy. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-27 Quản lý checklist chuẩn bị (Manage Preparation Checklist)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens a preparation checklist. |
| Description | Adds, edits and completes preparation items for pregnancy, hospital bag, postpartum recovery or early baby care. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested preparation checklist outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected preparation checklist.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides item, status, due context, note.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the preparation checklist, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-CHECKLIST-01: Checklist suggestions are organizational prompts, not clinical orders. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-28 Duyệt thư viện bài tập thai kỳ (Browse Pregnancy Exercise Library)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Exercise Library. |
| Description | Shows reviewed pregnancy exercise content with embedded stage/difficulty/duration filters and exercise-specific safety notes. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested pregnancy exercise library outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Browse Pregnancy Exercise Library function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted pregnancy exercise library data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-EXERCISE-01: Only reviewed and active exercises are visible.<br>BR-EXERCISE-02: Raw camera media is not retained. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-29 Hoàn thành kiểm tra an toàn trước tập (Complete Pre-exercise Safety Check)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects Start Exercise. |
| Description | Collects required pre-exercise safety answers and stops the session entry when configured warning answers are present. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested pre-exercise safety check outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Complete Pre-exercise Safety Check.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides safety responses.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the pre-exercise safety check action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-EXERCISE-03: A configured warning result blocks exercise session start and gives safe next-step guidance.<br>BR-EXERCISE-04: Passing the check does not certify medical fitness. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-30 Thực hiện phiên tập thai kỳ có phản hồi tư thế tùy chọn (Conduct Pregnancy Exercise Session with Optional Posture Feedback)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | MediaPipe / On-device Pose Service |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother starts an eligible exercise session. |
| Description | Runs an approved exercise session with start, pause, resume and complete actions and optional rule/ML-based posture feedback after camera consent. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested pregnancy exercise session outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Conduct Pregnancy Exercise Session with Optional Posture Feedback.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides exercise selection, optional camera consent, session state.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the pregnancy exercise session action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-EXERCISE-05: Posture feedback is supportive and not professional exercise supervision.<br>BR-EXERCISE-06: No raw image/video is retained. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-31 Xem lịch sử và kết quả phiên tập (View Exercise History and Session Result)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-02 - Mother Mobile - Mother Care Journey |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens exercise history or a completed session. |
| Description | Shows completed exercise sessions, duration, completion state, aggregate posture feedback and safety notices. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested exercise session history outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Exercise History and Session Result function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted exercise session history data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: journey and exercise support does not diagnose, prescribe or replace professional assessment.<br>BR-OWNERSHIP: mother journey data is private by default.<br>BR-EXERCISE-07: Results describe session feedback only and must not assess health fitness. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-32 Tạo hồ sơ bé (Create Baby Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects Add Baby. |
| Description | Creates a baby profile with core identity and birth context needed for journals, growth and vaccination tracking. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Create Baby Profile function.<br>2. The system loads the required authorized context and input fields for the baby profile.<br>3. Mother provides nickname, date of birth, sex, birth measurements.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the baby profile, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-01: Only the caregiver owner may create a profile in their care space. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-33 Cập nhật hoặc lưu trữ hồ sơ bé (Update or Archive Baby Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves changes or selects Archive. |
| Description | Updates permitted baby profile fields or archives a profile that is no longer actively managed without destroying linked history. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected baby profile.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected profile and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the baby profile, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-02: Archive preserves linked records and historical audit references. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-34 Chuyển hồ sơ bé đang theo dõi (Switch Active Baby Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects a different baby profile. |
| Description | Selects the baby profile used by the current baby dashboard, journals, growth views and relevant reminders. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested active baby profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Switch Active Baby Profile.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides selected baby.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the active baby profile action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-03: The active selection changes display context only, not ownership or permission. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-35 Xem tổng quan chăm sóc bé (View Baby Care Overview)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Baby Care. |
| Description | Shows a baby overview with recent journals, growth, milestones, vaccination status and current care prompts. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby care overview outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Baby Care Overview function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted baby care overview data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-04: The overview is descriptive and does not diagnose illness or developmental disorder. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-36 Thêm nhật ký chăm sóc bé hằng ngày (Add Baby Daily Log)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves a baby journal entry. |
| Description | Records feeding, sleep, diaper, symptom note or other supported daily care observation for a selected baby. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby daily log outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Baby Daily Log function.<br>2. The system loads the required authorized context and input fields for the baby daily log.<br>3. Mother provides log type, time, value, note, source.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the baby daily log, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-05: Medicine information may be recorded only as user-entered context; the system does not prescribe or set dosage. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-37 Cập nhật hoặc xóa nhật ký chăm sóc bé (Update or Delete Baby Daily Log)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother edits or deletes a baby journal entry. |
| Description | Corrects or removes an owner-entered baby journal entry. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby daily log outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected baby daily log.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected log and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the baby daily log, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-06: Deletion is limited to owner-entered data and follows retention rules. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-38 Xem tổng hợp nhật ký bé (View Baby Log Summary)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Baby Log Summary. |
| Description | Shows recent 24-hour or 7-day feeding, sleep, diaper and logged observation patterns for the active baby. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested baby log summary outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Baby Log Summary function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted baby log summary data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-BABY-07: Summary patterns are observational and not diagnostic. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-39 Ghi mốc phát triển (Record Development Milestone)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves a milestone. |
| Description | Records a caregiver-observed development milestone with date and note. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested development milestone outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Record Development Milestone function.<br>2. The system loads the required authorized context and input fields for the development milestone.<br>3. Mother provides milestone type, observed date, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the development milestone, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-DEVELOPMENT-01: Milestone records support observation only and do not determine developmental disorders. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-40 Cập nhật hoặc xóa mốc phát triển (Update or Delete Development Milestone)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother edits or deletes a milestone. |
| Description | Corrects or removes a caregiver-entered development milestone. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested development milestone outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected development milestone.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected milestone and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the development milestone, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-DEVELOPMENT-02: The record owner may edit only their own entry. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-41 Thêm số đo tăng trưởng (Add Growth Measurement)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves a growth measurement. |
| Description | Records supported growth measurements for a selected baby with measurement date and source. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested growth measurement outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Growth Measurement function.<br>2. The system loads the required authorized context and input fields for the growth measurement.<br>3. Mother provides weight, height/length, head circumference, date, source.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the growth measurement, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-GROWTH-01: Measurement source and time are required to interpret trends. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-42 Cập nhật hoặc xóa số đo tăng trưởng (Update or Delete Growth Measurement)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother edits or deletes a growth measurement. |
| Description | Corrects or removes a user-entered growth measurement. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested growth measurement outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected growth measurement.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected measurement and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the growth measurement, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-GROWTH-02: The system retains audit information where a measurement affected a shared summary. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-43 Xem xu hướng và lịch sử tăng trưởng (View Growth Trend and Measurement History)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Growth Tracking. |
| Description | Displays growth measurement history and charts with reference context and prompts to seek professional assessment when appropriate. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested growth trend outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Growth Trend and Measurement History function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted growth trend data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-GROWTH-03: Charts do not diagnose disease or determine a clinical growth status. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-44 Quản lý bản ghi tiêm chủng (Manage Vaccination Record)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother saves or modifies a vaccination record. |
| Description | Adds, updates or removes a user-entered vaccination record for the selected baby. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested vaccination record outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected vaccination record.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides vaccine, dose, date, provider note, attachment.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the vaccination record, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-VACCINE-01: The digital record is a personal support record and does not replace official vaccination documentation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-45 Xem lịch tiêm tham khảo và trạng thái nhắc lịch (View Vaccination Reference Schedule and Reminder Status)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-03 - Mother Mobile - Baby Care Journey, Growth & Vaccination |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother opens Vaccination Tracking. |
| Description | Displays a reference vaccination schedule, completed/recorded status and linked reminder state for the selected baby. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested vaccination reference schedule outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Vaccination Reference Schedule and Reminder Status function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted vaccination reference schedule data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-HEALTH-BOUNDARY: baby records, trends and milestones support observation rather than diagnosis.<br>BR-OWNERSHIP: baby data is private by default.<br>BR-VACCINE-02: Reference schedules are informational and users should confirm with qualified providers. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-46 Duyệt bảng tin và chủ đề cộng đồng (Browse Community Feed and Topics)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User opens Community. |
| Description | Displays moderated questions and topic collections. Keyword search, topic chips, filters, sort and pagination are embedded controls of this browse outcome. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community feed and topics outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the Browse Community Feed and Topics function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted community feed and topics data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-02: Only content visible under moderation state is returned.<br>BR-COMMUNITY-03: Search/filter is not a separate use case. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-47 Xem chi tiết câu hỏi cộng đồng (View Community Question Detail)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User opens a question from the community feed. |
| Description | Shows a permitted question, its answer thread, source labels, moderation state and allowed interactions. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community question detail outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the View Community Question Detail function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted community question detail data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-04: Hidden or restricted content remains inaccessible to ordinary users. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-48 Đăng câu hỏi cộng đồng (Create Community Question)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User submits a new community question. |
| Description | Creates a topic-based community question and optionally applies anonymous public display while preserving internal accountability. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community question outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the Create Community Question function.<br>2. The system loads the required authorized context and input fields for the community question.<br>3. User provides topic, content, optional anonymous display, stage context.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the community question, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-05: Anonymous display does not remove internal traceability.<br>BR-SAFETY-01: High-risk wording may be routed for review or safe escalation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-49 Chỉnh sửa hoặc xóa bài đăng cộng đồng của tôi (Edit or Delete Own Community Post)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User chooses Edit or Delete on own post. |
| Description | Edits or removes the user own community post while the post is not locked by moderation or investigation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community post outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected community post.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides selected post and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the community post, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-06: Locked, escalated or evidence-preserved posts cannot be silently altered. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-50 Trả lời cộng đồng (Post Community Answer)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App / Expert Portal |
| Priority | P0 |
| Trigger | User submits an answer to a permitted question. |
| Description | Posts a personal-experience answer or a verified expert answer with the applicable source/role label. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community answer outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the Post Community Answer function.<br>2. The system loads the required authorized context and input fields for the community answer.<br>3. User provides answer content and optional source context.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the community answer, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-07: Personal answers must not be presented as diagnosis or prescription.<br>BR-EXPERT-01: Verified labels are applied only to active verified experts. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-51 Chỉnh sửa hoặc xóa câu trả lời của tôi (Edit or Delete Own Community Answer)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App / Expert Portal |
| Priority | P0 |
| Trigger | User chooses Edit or Delete on own answer. |
| Description | Edits or removes the user own answer when no moderation lock or evidence hold applies. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community answer outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the selected community answer.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. User selects an allowed action and provides selected answer and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the community answer, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-08: Changes to expert answers remain attributable and auditable. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-52 Phản ứng với câu trả lời cộng đồng (React to Community Answer)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User taps a reaction control. |
| Description | Adds or removes a permitted helpfulness reaction to a community answer. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community answer outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts React to Community Answer.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides selected answer and reaction state.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the community answer action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-09: Reactions are participation signals, not proof of medical accuracy. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-53 Đánh dấu bài đăng cộng đồng (Bookmark Community Post)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User toggles a bookmark. |
| Description | Saves or removes a permitted post from the user private bookmark list. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community post outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Bookmark Community Post.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides selected post.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the community post action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-PRIVACY-05: Bookmarks are private to the account unless the owner explicitly shares them. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-54 Theo dõi hoặc bỏ theo dõi chủ đề cộng đồng (Follow or Unfollow Community Topic)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User toggles topic follow state. |
| Description | Subscribes or unsubscribes the user from a community topic for feed relevance and optional notifications. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community topic outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Follow or Unfollow Community Topic.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides selected topic.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the community topic action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-10: Topic notifications remain subject to notification preferences. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-55 Báo cáo nội dung hoặc tài khoản không an toàn (Report Unsafe Community Content or Account)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Mobile App / Web Portal |
| Priority | P0 |
| Trigger | User selects Report from a permitted target. |
| Description | Submits a report for misinformation, dangerous advice, hidden advertising, harassment, privacy breach or suspicious account behavior. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested content or account outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User starts Report Unsafe Community Content or Account.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. User provides reason, optional evidence/note.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the content or account action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-MODERATION-01: Reports must preserve the target evidence reference and reporter context.<br>BR-PRIVACY-06: Report visibility is restricted to authorized reviewers. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-56 Xem hàng đợi kiểm duyệt (View Moderation Queue)

| Field | Specification |
| --- | --- |
| Primary Actor | Moderator |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Moderator opens Moderation Queue. |
| Description | Shows reported, automatically flagged and pre-publication content awaiting authorized moderator review. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested moderation queue outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Moderator opens the View Moderation Queue function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted moderation queue data using any embedded list controls where applicable.<br>4. Moderator may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-MODERATION-02: Moderators see only evidence and private context needed to resolve the case. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-57 Kiểm duyệt nội dung cộng đồng (Moderate Community Content)

| Field | Specification |
| --- | --- |
| Primary Actor | Moderator |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Moderator selects a moderation item. |
| Description | Approves, hides, locks, labels or requests revision for a community post or answer based on policy and safety rules. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community content outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Moderator starts Moderate Community Content.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Moderator provides decision, reason, visibility state.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the community content action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-MODERATION-03: Safety-sensitive content may be escalated rather than published.<br>BR-AUDIT-04: Moderation decisions require a reason and audit trail. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-58 Giải quyết báo cáo và áp dụng xử lý vi phạm (Resolve Content or Account Report and Apply Enforcement)

| Field | Specification |
| --- | --- |
| Primary Actor | Moderator / System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Authorized reviewer resolves a report. |
| Description | Resolves a report and, when authorized, applies warning, posting restriction, suspension escalation or no-action decision. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested report and enforcement action outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Moderator / System Admin starts Resolve Content or Account Report and Apply Enforcement.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Moderator / System Admin provides resolution, target state, reason.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the report and enforcement action action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-ENFORCEMENT-01: Enforcement severity follows configured policy and authority.<br>BR-ENFORCEMENT-02: A restriction must identify scope and effective period. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-59 Quản lý chủ đề và trạng thái hiển thị cộng đồng (Manage Community Topics and Visibility)

| Field | Specification |
| --- | --- |
| Primary Actor | Moderator / Content Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-04 - Community & Admin - Community Q&A & Moderation |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Authorized staff saves a topic configuration. |
| Description | Creates, updates, reorders or disables community topics and their visibility rules. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested community topic outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Moderator / Content Admin opens the selected community topic.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Moderator / Content Admin selects an allowed action and provides name, description, order, visibility.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the community topic, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-MODERATION: visible community content is governed by moderation state, report workflow and safety policy.<br>BR-SAFETY: community content may not be used to diagnose, prescribe or promote unsafe treatment.<br>BR-COMMUNITY-11: Topic changes do not alter the authorship of existing posts. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-60 Nộp hồ sơ chuyên gia (Submit Expert Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | Expert Applicant |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert applicant starts expert onboarding. |
| Description | Creates an expert application profile with specialty, experience, support scope and public professional information. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Expert Applicant opens the Submit Expert Profile function.<br>2. The system loads the required authorized context and input fields for the expert profile.<br>3. Expert Applicant provides professional identity, specialty, experience, support scope.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the expert profile, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-02: An applicant is not shown as verified until approval. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-61 Cập nhật hồ sơ chuyên gia (Update Expert Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert saves profile changes. |
| Description | Updates approved professional profile fields and public service information subject to review rules. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert opens the selected expert profile.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Verified Expert selects an allowed action and provides profile fields and public display information.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the expert profile, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-03: Material credential/scope changes may return the profile to review. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-62 Nộp hoặc thay thế giấy tờ xác thực (Submit or Replace Verification Documents)

| Field | Specification |
| --- | --- |
| Primary Actor | Expert Applicant / Verified Expert |
| Secondary Actors / Services | Protected Object Storage |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert submits verification documents. |
| Description | Uploads required credentials, certificates and supporting evidence for verification or renewal. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested verification document set outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Expert Applicant / Verified Expert opens the Submit or Replace Verification Documents function.<br>2. The system loads the required authorized context and input fields for the verification document set.<br>3. Expert Applicant / Verified Expert provides credential files, expiry dates, declarations.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the verification document set, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-04: Documents are protected and visible only to authorized reviewers.<br>BR-EXPERT-05: Expiry dates are retained where applicable. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-63 Xem trạng thái xác thực và nộp gia hạn (View Verification Status and Renew Submission)

| Field | Specification |
| --- | --- |
| Primary Actor | Expert Applicant / Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert opens Verification Status. |
| Description | Shows verification result, required corrections, expiry status and allows a renewal submission when eligible. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested verification status outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Expert Applicant / Verified Expert opens View Verification Status and Renew Submission.<br>2. The system validates access and loads authorized context.<br>3. Expert Applicant / Verified Expert provides selected renewal action.<br>4. The system applies business rules and records the result.<br>5. The system shows the outcome and refreshes related data. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-06: A rejected or expired verification limits verified-only capabilities. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-64 Cấu hình trạng thái sẵn sàng và phạm vi hỗ trợ chuyên gia (Configure Expert Availability and Service Scope)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert saves availability or scope settings. |
| Description | Configures availability, support methods, service area and public readiness state for the expert directory and nearby support eligibility. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert availability and service scope outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert opens the selected expert availability and service scope.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Verified Expert selects an allowed action and provides availability slots, online state, support methods, service area.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the expert availability and service scope, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-07: Location-related settings remain consent-based and time-limited where precise sharing is used. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-65 Duyệt danh mục chuyên gia đã xác thực (Browse Verified Expert Directory)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User opens Expert Directory. |
| Description | Displays verified experts with embedded search and filters by specialty, availability, badge and consented service area. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested verified expert directory outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the Browse Verified Expert Directory function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted verified expert directory data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-08: Only currently verified and permitted public profiles are listed.<br>BR-EXPERT-09: Search/filter is embedded in the directory, not a standalone use case. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-66 Xem hồ sơ chuyên gia đã xác thực (View Verified Expert Profile)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Mobile App |
| Priority | P0 |
| Trigger | User opens an expert profile. |
| Description | Shows a verified expert public professional profile, specialties, supported scope, availability and contribution reputation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested verified expert profile outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the View Verified Expert Profile function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted verified expert profile data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-10: Contribution/review signals must not be represented as medical competence guarantees. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-67 Xem hàng đợi câu hỏi chuyên môn (View Expert Question Queue)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert opens Expert Question Queue. |
| Description | Shows community questions matched to the expert verified specialties and public support scope. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert question queue outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert opens the View Expert Question Queue function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted expert question queue data using any embedded list controls where applicable.<br>4. Verified Expert may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-11: The queue cannot expose private reporter health data beyond publicly visible question context. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-68 Đăng trả lời với nhãn chuyên gia (Post Verified Expert Answer)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert submits an answer. |
| Description | Posts an expert answer with verified badge, source/safety label and contribution attribution. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested verified expert answer outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert opens the Post Verified Expert Answer function.<br>2. The system loads the required authorized context and input fields for the verified expert answer.<br>3. Verified Expert provides answer, source label, safe scope note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the verified expert answer, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-12: The answer must remain non-diagnostic and non-prescriptive.<br>BR-COMMUNITY-12: Promotional misuse is prohibited. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-69 Xem điểm đóng góp và huy hiệu (View Contribution Points and Badges)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Expert Portal / Expert App |
| Priority | P0 |
| Trigger | Expert opens Contribution Activity. |
| Description | Shows contribution points, badges and qualifying community activities used to recognize responsible participation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested contribution points and badges outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert opens the View Contribution Points and Badges function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted contribution points and badges data using any embedded list controls where applicable.<br>4. Verified Expert may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-13: Points and badges do not prove clinical competence or medical accuracy. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-70 Rà soát hồ sơ xác thực chuyên gia (Review Expert Verification Submission)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Admin opens a verification submission. |
| Description | Approves, requests supplementation, rejects or renews an expert verification submission after credential review. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert verification submission outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin opens the Review Expert Verification Submission function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted expert verification submission data using any embedded list controls where applicable.<br>4. System Admin may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-14: Verification decisions require documented evidence and rationale.<br>BR-AUDIT-05: Verification decisions are auditable. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-71 Hạn chế, đình chỉ hoặc khôi phục trạng thái tin cậy chuyên gia (Restrict, Suspend or Reinstate Expert Trust Status)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-05 - Expert & Admin - Verified Expert Network & Contribution |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Admin submits an expert trust-status decision. |
| Description | Applies, updates or lifts an expert restriction/suspension when credentials expire or policy violations require it. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested expert trust status outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin opens the selected expert trust status.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. System Admin selects an allowed action and provides restriction scope, effective period, reason.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the expert trust status, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPERT: verified status is required for verified badge and verified expert response.<br>BR-TRUST: contribution points and badges show participation, not clinical competence.<br>BR-EXPERT-15: Public badge, question response and nearby availability must reflect the active trust state. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-72 Chạy tiếp nhận triệu chứng bằng AI (Run AI Symptom Intake)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Gemini / Rule Engine |
| Feature / Group | MF-06 - Mother & Admin - AI Nurse Assistant & Risk Triage |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother starts AI Symptom Intake. |
| Description | Collects structured symptom information, timing, severity and permitted context through a guided intake flow. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested symptom intake session outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Run AI Symptom Intake.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides symptoms, duration, severity, optional context.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the symptom intake session action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-AI: outputs are non-diagnostic and non-prescriptive.<br>BR-SAFETY: red-flag routing must provide conservative safe next actions.<br>BR-AI-01: Intake must state that the assistant is not diagnosing or prescribing.<br>BR-AI-02: Only necessary consented context may be used. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-73 Xem kết quả phân loại rủi ro (View Risk Triage Result)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Gemini / Rule Engine |
| Feature / Group | MF-06 - Mother & Admin - AI Nurse Assistant & Risk Triage |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | The system completes a symptom intake evaluation. |
| Description | Shows non-diagnostic green, yellow or red orientation with safe next-step guidance and clearly labelled uncertainty. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested risk triage result outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Risk Triage Result function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted risk triage result data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-AI: outputs are non-diagnostic and non-prescriptive.<br>BR-SAFETY: red-flag routing must provide conservative safe next actions.<br>BR-AI-03: Results must not name a disease, prescribe medication or set dosage.<br>BR-AI-04: Red results must prioritize emergency/offline care guidance. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-74 Mở hỗ trợ khẩn cấp từ kết quả rủi ro đỏ (Open Emergency Support from a Red Risk Result)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-06 - Mother & Admin - AI Nurse Assistant & Risk Triage |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects emergency support from a red-risk result. |
| Description | Transfers a user from a red-risk result to the Emergency Map and Nearby Care Support flow without treating the result as a diagnosis. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested red risk result outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Open Emergency Support from a Red Risk Result.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides current result context.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the red risk result action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-AI: outputs are non-diagnostic and non-prescriptive.<br>BR-SAFETY: red-flag routing must provide conservative safe next actions.<br>BR-AI-05: The emergency handoff does not dispatch emergency services. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-75 Quản lý nguồn tri thức AI đã phê duyệt (Manage Approved AI Knowledge Sources)

| Field | Specification |
| --- | --- |
| Primary Actor | Content Admin / System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-06 - Mother & Admin - AI Nurse Assistant & Risk Triage |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Authorized staff saves a knowledge source decision. |
| Description | Registers, reviews, versions or disables approved knowledge sources available to AI retrieval and safe answer composition. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested approved AI knowledge source outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Content Admin / System Admin opens the selected approved AI knowledge source.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Content Admin / System Admin selects an allowed action and provides source, reviewer, version, validity, status.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the approved AI knowledge source, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-AI: outputs are non-diagnostic and non-prescriptive.<br>BR-SAFETY: red-flag routing must provide conservative safe next actions.<br>BR-AI-06: AI retrieval must use approved, traceable source material only. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-76 Cấu hình quy tắc rủi ro và cờ đỏ AI (Configure AI Risk and Red-Flag Rules)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-06 - Mother & Admin - AI Nurse Assistant & Risk Triage |
| Platform | Admin Portal |
| Priority | P0 |
| Trigger | Admin publishes a risk-rule configuration. |
| Description | Maintains conservative risk thresholds, red-flag patterns, safe fallback actions and rule version history. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested AI risk/red-flag rule outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin opens the selected AI risk/red-flag rule.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. System Admin selects an allowed action and provides rule version, threshold, action, effective status.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the AI risk/red-flag rule, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-AI: outputs are non-diagnostic and non-prescriptive.<br>BR-SAFETY: red-flag routing must provide conservative safe next actions.<br>BR-AI-07: Rule changes require versioning and audit history.<br>BR-AI-08: A safer fallback takes precedence when confidence is insufficient. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-77 Mở bản đồ hỗ trợ khẩn cấp (Open Emergency Map)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | TrackAsia Map Service |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects Emergency Support. |
| Description | Opens the shared emergency map after informing the user about location permission and the non-dispatch scope. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested emergency map outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Open Emergency Map.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides location permission choice.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the emergency map action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-MAP-01: Location permission is optional and purpose-specific.<br>BR-EMERGENCY-01: CareBridge does not dispatch ambulances. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-78 Tìm cơ sở chăm sóc gần đây (Find Nearby Care Facilities)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | TrackAsia Map Service |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects Nearby Facilities. |
| Description | Finds permitted nearby care facilities based on the user approved location or a selected area. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested nearby care facilities outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Find Nearby Care Facilities function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted nearby care facilities data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-MAP-02: Results are informational and facility availability is not guaranteed. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-79 Xem lộ trình, ETA và gọi nhanh hoặc chỉ đường (View Route, ETA and Quick Call or Navigate)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | TrackAsia Map Service |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother selects a facility action. |
| Description | Shows route/ETA and enables quick call or navigation to a selected care facility through the map/device capability. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested selected care facility outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts View Route, ETA and Quick Call or Navigate.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides selected facility and destination mode.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the selected care facility action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-MAP-03: Route/ETA is an estimate and does not guarantee arrival time. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-80 Chia sẻ vị trí có thời hạn và gửi cảnh báo khẩn cấp cho gia đình (Share Time-Limited Location and Send Family Emergency Alert)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother confirms Send Family Alert. |
| Description | Shares minimum necessary location/context for a time-limited emergency alert to selected family recipients. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested family emergency alert outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Share Time-Limited Location and Send Family Emergency Alert.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides selected recipients, location precision, expiry.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the family emergency alert action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-EMERGENCY-02: Alerts contain minimum necessary context.<br>BR-CONSENT-04: Location sharing expires automatically or on user stop. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-81 Tạo hoặc hủy yêu cầu hỗ trợ gần vị trí (Create or Cancel Nearby Support Request)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Mother Mobile App |
| Priority | P0 |
| Trigger | Mother submits or cancels a nearby-support request. |
| Description | Creates or cancels a consented nearby-support request visible only to eligible verified experts; no expert arrival is guaranteed. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested nearby support request outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Create or Cancel Nearby Support Request.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides support reason, location scope, expiry.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the nearby support request action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-EMERGENCY-03: The request is not an ambulance dispatch or guaranteed service booking.<br>BR-EMERGENCY-04: Request visibility uses minimum necessary context. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-82 Quản lý khả dụng gần vị trí và phản hồi yêu cầu hỗ trợ (Manage Expert Nearby Availability and Respond to Nearby Support Request)

| Field | Specification |
| --- | --- |
| Primary Actor | Verified Expert |
| Secondary Actors / Services | TrackAsia Map Service |
| Feature / Group | MF-07 - Mother/Expert Mobile - Emergency Map & Nearby Care Support |
| Platform | Expert App / Expert Portal |
| Priority | P0 |
| Trigger | Expert manages nearby availability or opens an eligible request. |
| Description | Lets an eligible verified expert opt in to nearby availability, view minimal request context and accept, decline or stop responding to a request. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; required reference data exists; and role, ownership or consent conditions are met. |
| Postconditions | The requested nearby support request outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Verified Expert starts Manage Expert Nearby Availability and Respond to Nearby Support Request.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Verified Expert provides availability, response decision, location consent.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the nearby support request action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EMERGENCY: the system supports faster action but does not dispatch emergency services or guarantee arrival.<br>BR-LOCATION: location is consented, minimum necessary and time-limited.<br>BR-EMERGENCY-05: Expert location sharing is opt-in, configurable and time-limited.<br>BR-EMERGENCY-06: Acceptance does not create a medical emergency dispatch obligation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-83 Thêm hồ sơ sức khỏe cá nhân và tệp đính kèm (Add Personal Health Record and Attachment)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Storage |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves a health record. |
| Description | Creates a maternal or baby health record and attaches permitted documents or images with type, date and source metadata. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health record outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Personal Health Record and Attachment function.<br>2. The system loads the required authorized context and input fields for the health record.<br>3. Mother provides owner, category, date, source, note, attachment.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the health record, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-RECORD-01: Source label and owner context are required.<br>BR-RECORD-02: File type/size and access policy are validated. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-84 Cập nhật siêu dữ liệu hồ sơ sức khỏe (Update Health Record Metadata)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother edits a health record. |
| Description | Updates title, category, date, source label, tags or note of an owner-entered health record without altering the protected file itself unless allowed. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health record metadata outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected health record metadata.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected record and metadata changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the health record metadata, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-RECORD-03: Edits are limited to records the user owns or is authorized to manage. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-85 Lưu trữ hoặc xóa hồ sơ sức khỏe do người dùng nhập (Archive or Delete User-entered Health Record)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Archive or Delete. |
| Description | Archives or requests deletion of an owner-entered record when retention, sharing and safety-evidence obligations allow. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health record outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected health record.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected record and lifecycle action.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the health record, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-RECORD-04: Evidence/retention locks prevent silent deletion. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-86 Xem timeline và chi tiết hồ sơ sức khỏe (View Health Record Timeline and Detail)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Health Records. |
| Description | Shows authorized maternal and baby records by time, category and source with embedded filtering inside the timeline. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health record timeline outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Health Record Timeline and Detail function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted health record timeline data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-RECORD-05: Timeline queries enforce ownership and active consent at read time. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-87 Tạo bản tóm tắt sức khỏe (Generate Health Summary)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Generate Summary. |
| Description | Creates a selected-period summary from user-chosen maternal or baby records for personal review or permitted sharing. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health summary outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Generate Health Summary.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides record set and time range.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the health summary action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-RECORD-06: Summaries preserve source labels and do not create diagnoses. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-88 Chia sẻ bản tóm tắt hoặc hồ sơ đã chọn theo consent (Share Health Summary or Selected Records Under Consent)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-08 - Mother Mobile - Personal Health Records & Source Labeling |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Share under an active or new grant. |
| Description | Shares selected records or a summary only through an active MF-01 consent grant with scope and expiry. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested health summary/record set outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Share Health Summary or Selected Records Under Consent.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides recipient, selected data, purpose, expiry.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the health summary/record set action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-RECORD: records and attachments carry source labels and are protected by ownership/consent.<br>BR-PRIVACY: sharing is explicit, scoped and time-limited.<br>BR-CONSENT-05: Sharing requires a valid consent scope and re-checks it for every access. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-89 Tạo nhắc lịch hẹn (Create Appointment Reminder)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-09 - Mother Mobile - Reminders, Tasks & Care Plan |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves an appointment reminder. |
| Description | Creates a reminder for a self-entered appointment, follow-up, examination, test or other care event. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested appointment reminder outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Create Appointment Reminder function.<br>2. The system loads the required authorized context and input fields for the appointment reminder.<br>3. Mother provides title, date/time, recurrence, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the appointment reminder, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-REMINDER: reminders organize user-entered/approved schedules and do not create prescriptions or formal treatment plans.<br>BR-REMINDER-01: Reminder content is organizational and not a care order. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-90 Tạo nhắc thuốc hoặc vitamin (Create Medicine or Vitamin Reminder)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-09 - Mother Mobile - Reminders, Tasks & Care Plan |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves a medicine/vitamin reminder. |
| Description | Creates a user-entered medicine or vitamin reminder based on prior professional advice or personal routine. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested medicine/vitamin reminder outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Create Medicine or Vitamin Reminder function.<br>2. The system loads the required authorized context and input fields for the medicine/vitamin reminder.<br>3. Mother provides item name, time, recurrence, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the medicine/vitamin reminder, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-REMINDER: reminders organize user-entered/approved schedules and do not create prescriptions or formal treatment plans.<br>BR-REMINDER-02: The system does not prescribe medicine, determine dosage or alter existing advice. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-91 Tạo nhắc lịch tiêm (Create Vaccination Reminder)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-09 - Mother Mobile - Reminders, Tasks & Care Plan |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves a vaccination reminder. |
| Description | Creates or confirms a reminder linked to the selected baby vaccination record or reference schedule. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested vaccination reminder outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Create Vaccination Reminder function.<br>2. The system loads the required authorized context and input fields for the vaccination reminder.<br>3. Mother provides baby, vaccine/dose label, date/time, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the vaccination reminder, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-REMINDER: reminders organize user-entered/approved schedules and do not create prescriptions or formal treatment plans.<br>BR-REMINDER-03: The reminder does not replace official vaccination advice or documentation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-92 Cập nhật, hoãn, hoàn thành, bỏ qua hoặc xóa nhắc lịch (Update, Snooze, Complete, Skip or Delete Care Reminder)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-09 - Mother Mobile - Reminders, Tasks & Care Plan |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother changes a reminder state. |
| Description | Manages the lifecycle of a user reminder, including time updates, snooze, complete, skip and deletion where permitted. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested care reminder outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected care reminder.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected reminder and lifecycle state.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the care reminder, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-REMINDER: reminders organize user-entered/approved schedules and do not create prescriptions or formal treatment plans.<br>BR-REMINDER-04: Completion or skip records an outcome but does not imply clinical adherence. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-93 Xem việc cần làm hôm nay và kế hoạch chăm sóc (View Today Tasks and Care Plan)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-09 - Mother Mobile - Reminders, Tasks & Care Plan |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Today Tasks. |
| Description | Shows due reminders, checklist items and permitted family tasks in a prioritized today view. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested today tasks and care plan outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Today Tasks and Care Plan function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted today tasks and care plan data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-REMINDER: reminders organize user-entered/approved schedules and do not create prescriptions or formal treatment plans.<br>BR-REMINDER-05: The view aggregates organizational tasks and does not create a formal treatment plan. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-94 Tạo nhóm chăm sóc (Create Care Group)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Create Care Group. |
| Description | Creates a care group for a mother, baby or household care context with the creator as initial owner. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested care group outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Create Care Group function.<br>2. The system loads the required authorized context and input fields for the care group.<br>3. Mother provides group name and care context.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the care group, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-02: The group owner controls membership and default permission scope. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-95 Mời hoặc thu hồi lời mời người thân (Invite or Revoke Family Member Invitation)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother sends or revokes an invitation. |
| Description | Sends or revokes a care-group invitation using a permitted contact or invite link. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested care group invitation outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Invite or Revoke Family Member Invitation function.<br>2. The system loads the required authorized context and input fields for the care group invitation.<br>3. Mother provides recipient contact/link and group.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the care group invitation, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-03: An invitation has an expiry and does not grant access before acceptance. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-96 Chấp nhận hoặc từ chối lời mời nhóm chăm sóc (Accept or Reject Care Group Invitation)

| Field | Specification |
| --- | --- |
| Primary Actor | Family Member |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Family Mobile App |
| Priority | P1 |
| Trigger | Family member opens an invitation. |
| Description | Lets a recipient accept or reject an active care-group invitation after authentication. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested care group invitation outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Family Member starts Accept or Reject Care Group Invitation.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Family Member provides accept/reject decision.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the care group invitation action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-04: Acceptance creates membership only after group and invitation validation. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-97 Quản lý thành viên nhóm chăm sóc (Manage Care Group Membership)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother / Family Member |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Mother Mobile App / Family Mobile App |
| Priority | P1 |
| Trigger | A member or owner selects a membership action. |
| Description | Allows a group owner to remove a member and an individual member to leave a group where rules permit. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested care group membership outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother / Family Member opens the selected care group membership.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother / Family Member selects an allowed action and provides selected member and action.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the care group membership, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-05: Removing a member immediately ends access derived solely from group membership. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-98 Quản lý phạm vi quyền của người thân (Manage Family Permission Scope)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves a permission scope. |
| Description | Defines and updates family access to selected calendar, tasks, alerts, logs, summaries or records. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested family permission scope outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected family permission scope.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides member, data scope, purpose, expiry.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the family permission scope, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-06: Family access follows the same minimum-necessary, scope and expiry principles as consent grants. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-99 Tạo, cập nhật hoặc hủy nhiệm vụ chăm sóc gia đình (Create, Update or Cancel Family Care Task)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves a family care task. |
| Description | Creates, edits or cancels a care task assigned to an eligible care-group member. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested family care task outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected family care task.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides assignee, due date, task details, status.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the family care task, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-07: A task reveals only the context allowed by the assignee permission scope. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-100 Cập nhật trạng thái nhiệm vụ được giao (Update Assigned Task Status)

| Field | Specification |
| --- | --- |
| Primary Actor | Family Member |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Family Mobile App |
| Priority | P1 |
| Trigger | Family member changes assigned task status. |
| Description | Updates the status of a task assigned to the signed-in family member, such as in progress, completed or needs help. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested assigned task status outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Family Member opens the selected assigned task status.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Family Member selects an allowed action and provides selected task and status/note.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the assigned task status, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-08: Members may update only tasks assigned to them unless the group role permits otherwise. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-101 Xem lịch, dữ liệu và cảnh báo chăm sóc được chia sẻ (View Shared Care Calendar, Data and Alerts)

| Field | Specification |
| --- | --- |
| Primary Actor | Family Member |
| Secondary Actors / Services | None |
| Feature / Group | MF-10 - Mother/Family Mobile - Family Sync & Cooperative Care |
| Platform | Family Mobile App |
| Priority | P1 |
| Trigger | Family member opens Shared Care. |
| Description | Displays calendar items, shared care data and family alerts only within the member active permission scope. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested shared care context outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Family Member opens the View Shared Care Calendar, Data and Alerts function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted shared care context data using any embedded list controls where applicable.<br>4. Family Member may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-FAMILY: family access is limited by active membership and permission scope.<br>BR-PRIVACY: shared views show minimum necessary context.<br>BR-FAMILY-09: Every protected read re-checks active membership and scope. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-102 Duyệt nội dung đã xác thực (Browse Verified Content)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Mobile App |
| Priority | P1 |
| Trigger | User opens Verified Content Hub. |
| Description | Displays approved articles, FAQs and checklists with embedded keyword, stage and topic controls. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested verified content hub outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the Browse Verified Content function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted verified content hub data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-01: Only approved, active content is visible to end users.<br>BR-CONTENT-02: Search/filter remains embedded in this browse use case. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-103 Xem chi tiết nội dung đã xác thực (View Verified Content Detail)

| Field | Specification |
| --- | --- |
| Primary Actor | User |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Mobile App |
| Priority | P1 |
| Trigger | User opens a verified content item. |
| Description | Shows the selected article, FAQ or checklist with source, review/version state, updated date and safety notes. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested verified content detail outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. User opens the View Verified Content Detail function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted verified content detail data using any embedded list controls where applicable.<br>4. User may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-03: The content view must display applicable source/review information. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-104 Tạo nội dung đã xác thực (Create Verified Content)

| Field | Specification |
| --- | --- |
| Primary Actor | Content Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Admin Portal |
| Priority | P1 |
| Trigger | Content Admin saves a new content draft. |
| Description | Creates a draft article, FAQ or checklist with stage/topic mapping, sources and initial review state. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested verified content draft outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Content Admin opens the Create Verified Content function.<br>2. The system loads the required authorized context and input fields for the verified content draft.<br>3. Content Admin provides content type, title, body, sources, stage/topic.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the verified content draft, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-04: Drafts are not publicly visible before approval. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-105 Cập nhật nội dung đã xác thực và nguồn (Update Verified Content and Sources)

| Field | Specification |
| --- | --- |
| Primary Actor | Content Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Admin Portal |
| Priority | P1 |
| Trigger | Content Admin saves content changes. |
| Description | Updates an existing content draft or creates a new version when published content needs changes to body, source, tag or safety notes. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested verified content and sources outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Content Admin opens the selected verified content and sources.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Content Admin selects an allowed action and provides content changes, source changes, version note.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the verified content and sources, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-05: Published content changes must preserve version traceability. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-106 Rà soát và xuất bản phiên bản nội dung (Review and Publish Content Version)

| Field | Specification |
| --- | --- |
| Primary Actor | System Admin / Content Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Admin Portal |
| Priority | P1 |
| Trigger | Authorized reviewer submits a review decision. |
| Description | Reviews a content version and approves, rejects or requests revision before public publication. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested content version outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. System Admin / Content Admin opens the Review and Publish Content Version function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted content version data using any embedded list controls where applicable.<br>4. System Admin / Content Admin may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-06: A published version must identify responsible review and effective date. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-107 Gỡ xuất bản hoặc lưu trữ nội dung (Unpublish or Archive Content)

| Field | Specification |
| --- | --- |
| Primary Actor | Content Admin / System Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Admin Portal |
| Priority | P1 |
| Trigger | Authorized staff unpublishes or archives content. |
| Description | Withdraws or archives content that is outdated, unsafe, incorrect or replaced by a newer version. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested verified content outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Content Admin / System Admin opens the selected verified content.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Content Admin / System Admin selects an allowed action and provides selected item, reason, state.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the verified content, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-07: Withdrawal preserves a historical record for audit while removing public visibility. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-108 Quản lý danh mục và ánh xạ giai đoạn/chủ đề nội dung (Manage Content Categories and Stage/Topic Mapping)

| Field | Specification |
| --- | --- |
| Primary Actor | Content Admin |
| Secondary Actors / Services | None |
| Feature / Group | MF-11 - Mobile & Admin - Verified Content & Checklist Hub |
| Platform | Admin Portal |
| Priority | P1 |
| Trigger | Content Admin saves category configuration. |
| Description | Maintains categories, stage mappings, topic tags and display order used by the content hub. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested content category mapping outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Content Admin opens the selected content category mapping.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Content Admin selects an allowed action and provides category, stage, topic, display order, status.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the content category mapping, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-CONTENT: public content must have review state, source labeling and version traceability.<br>BR-CONTENT-08: Category changes do not silently delete content history. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-109 Thêm khoản chi chuẩn bị (Add Expense Entry)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-12 - Mother Mobile - Expense & Preparation Planner |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves an expense entry. |
| Description | Records a household preparation expense with category, amount, date and optional experience note. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested expense entry outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Add Expense Entry function.<br>2. The system loads the required authorized context and input fields for the expense entry.<br>3. Mother provides category, amount, date, note.<br>4. The system validates business rules, ownership/consent and data format.<br>5. The system creates the expense entry, records required audit information and returns the resulting state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPENSE: expense tracking is household preparation only, not formal accounting or billing.<br>BR-EXPENSE-01: Expense data is for household planning only and is not a bill, claim or accounting record. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-110 Cập nhật hoặc xóa khoản chi chuẩn bị (Update or Delete Expense Entry)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-12 - Mother Mobile - Expense & Preparation Planner |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother edits or deletes an expense entry. |
| Description | Corrects or deletes an owner-entered preparation expense. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested expense entry outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected expense entry.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides selected entry and changes.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the expense entry, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPENSE: expense tracking is household preparation only, not formal accounting or billing.<br>BR-EXPENSE-02: Only the owner can modify personal expense entries. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-111 Xem tổng hợp chi phí chuẩn bị (View Expense Summary)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-12 - Mother Mobile - Expense & Preparation Planner |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Expense Summary. |
| Description | Shows simple expense totals by category, month or journey/baby period. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested expense summary outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Expense Summary function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted expense summary data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-EXPENSE: expense tracking is household preparation only, not formal accounting or billing.<br>BR-EXPENSE-03: Summaries are informational and not financial advice. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-112 Kết nối thiết bị hoặc nền tảng sức khỏe (Connect Health Device or Platform)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Health Connect / Gadgetbridge / Controlled Import |
| Feature / Group | MF-13 - Mother Mobile - Connected Device & Health Data Integration |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Connect Device. |
| Description | Connects a supported device/platform after the user reviews permissions and selected indicator scope. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested device/platform connection outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Connect Health Device or Platform.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides provider, selected permissions, indicator scope.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the device/platform connection action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-DEVICE: device observations are source-labeled, quality-qualified and non-clinical.<br>BR-PRIVACY: users can disconnect and request deletion of imported data.<br>BR-DEVICE-01: Connection requires explicit user consent.<br>BR-DEVICE-02: Only supported indicators are requested. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-113 Nhập hoặc đồng bộ quan sát từ thiết bị (Import or Synchronize Device Observations)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Health Connect / Gadgetbridge / Controlled Import |
| Feature / Group | MF-13 - Mother Mobile - Connected Device & Health Data Integration |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother initiates sync/import or authorized background sync runs. |
| Description | Imports or synchronizes selected device observations such as heart rate, sleep, steps, SpO2, temperature or blood pressure where supported. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested device observations outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Import or Synchronize Device Observations.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides selected indicators and source timestamps.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the device observations action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-DEVICE: device observations are source-labeled, quality-qualified and non-clinical.<br>BR-PRIVACY: users can disconnect and request deletion of imported data.<br>BR-DEVICE-03: Every observation retains source, time and quality metadata.<br>BR-DEVICE-04: Device data is not treated as a clinical measurement. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-114 Xem xu hướng và chất lượng dữ liệu thiết bị (View Device Data Trend and Quality)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-13 - Mother Mobile - Connected Device & Health Data Integration |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Device Trends. |
| Description | Displays imported data trends, source labels, data gaps and quality cautions for the selected indicator. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested device data trend and quality outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the View Device Data Trend and Quality function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted device data trend and quality data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-DEVICE: device observations are source-labeled, quality-qualified and non-clinical.<br>BR-PRIVACY: users can disconnect and request deletion of imported data.<br>BR-DEVICE-05: Trend views must not diagnose illness or claim medical accuracy. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-115 Ngắt kết nối thiết bị và xóa dữ liệu đã nhập (Disconnect Device and Delete Imported Data)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-13 - Mother Mobile - Connected Device & Health Data Integration |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Disconnect or Delete Imported Data. |
| Description | Stops future synchronization and allows the user to request deletion of imported data according to retention rules. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested device connection/imported data outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Disconnect Device and Delete Imported Data.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides connection and deletion scope.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the device connection/imported data action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-DEVICE: device observations are source-labeled, quality-qualified and non-clinical.<br>BR-PRIVACY: users can disconnect and request deletion of imported data.<br>BR-DEVICE-06: Disconnect stops future import immediately.<br>BR-DEVICE-07: Deletion respects audit/retention obligations. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-116 Quản lý liên hệ khẩn cấp (Manage Emergency Contacts)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Firebase Cloud Messaging |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother saves emergency-contact settings. |
| Description | Adds, verifies, reprioritizes or removes emergency alert recipients used by safety monitoring and emergency flows. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested emergency contacts outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected emergency contacts.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides contact, priority, verification state.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the emergency contacts, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-01: Contacts must be explicitly selected for safety alert delivery. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-117 Cấu hình giám sát hoạt động thông minh (Configure Smart Activity Monitoring)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Smart Safety settings. |
| Description | Configures sensor consent, alert recipients, countdown, location-sharing option and monitoring conditions. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested smart activity monitoring outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the selected smart activity monitoring.<br>2. The system verifies the actor authorization, ownership and current record state.<br>3. Mother selects an allowed action and provides consent, contacts, countdown, location option.<br>4. The system validates applicable policy, safety and consistency rules.<br>5. The system applies the change to the smart activity monitoring, writes required audit data and refreshes related screens or notifications. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-02: Monitoring is optional and can be configured or disabled by the user.<br>BR-SAFETY-03: The system explains device/OS limitations. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-118 Bật giám sát hoạt động thông minh (Enable Smart Activity Monitoring)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Phone IMU Sensor |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Enable Monitoring. |
| Description | Starts IMU-based monitoring after configuration, permissions and consent are valid. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested smart activity monitoring outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Enable Smart Activity Monitoring.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides active monitoring state.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the smart activity monitoring action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-04: Sensor access is requested only while needed and permitted.<br>BR-SAFETY-05: The system cannot guarantee continuous background monitoring. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-119 Tắt giám sát hoạt động thông minh (Disable Smart Activity Monitoring)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | Phone IMU Sensor |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother selects Disable Monitoring. |
| Description | Stops IMU monitoring and prevents new candidate safety events from being created after the state change. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested smart activity monitoring outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother starts Disable Smart Activity Monitoring.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother provides inactive monitoring state.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the smart activity monitoring action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-06: Disabling monitoring must be immediately reflected in future sensor-event processing. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-120 Xử lý sự kiện nghi ngờ ngã hoặc va chạm và kiểm tra an toàn (Handle Suspected Fall or Impact and Safety Check)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother / Phone IMU Sensor |
| Secondary Actors / Services | Phone IMU Sensor / Firebase Cloud Messaging |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | A configured sensor pattern creates a candidate event. |
| Description | Detects a candidate fall/impact, opens a safety countdown and records I am OK, Need Help or emergency-support handoff response. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested suspected fall/impact event outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother / Phone IMU Sensor starts Handle Suspected Fall or Impact and Safety Check.<br>2. The system verifies the current authorization, required context and applicable consent state.<br>3. Mother / Phone IMU Sensor provides candidate event, safety response, optional location.<br>4. The system applies the relevant domain and safety rules, including external-service checks when needed.<br>5. The system completes the suspected fall/impact event action, records required audit information and presents the safe result state. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-07: A candidate event is not proof of a fall or medical emergency.<br>BR-SAFETY-08: Automatic alerts use configured recipients and minimum necessary context. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



### UC-121 Xem lịch sử sự kiện an toàn và báo cáo phát hiện nhầm (Review Safety Event History and Report False Positive)

| Field | Specification |
| --- | --- |
| Primary Actor | Mother |
| Secondary Actors / Services | None |
| Feature / Group | MF-14 - Mother Mobile - Smart Activity Monitoring & Safety Support |
| Platform | Mother Mobile App |
| Priority | P1 |
| Trigger | Mother opens Safety Event History or marks a false positive. |
| Description | Shows prior safety events and lets the user mark a false detection with a reason to improve future threshold evaluation. |
| Preconditions | The user is on the relevant authorized screen; the CareBridge backend is available; the relevant P0 identity/consent context exists; and required reference data exists. |
| Postconditions | The requested safety event history outcome is completed or a clear safe result state is shown. Related records, visibility, permissions and notifications are updated where applicable. Sensitive actions are auditable. |
| Normal Flow | 1. Mother opens the Review Safety Event History and Report False Positive function.<br>2. The system validates role, ownership, active consent and visibility scope before retrieval.<br>3. The system retrieves the permitted safety event history data using any embedded list controls where applicable.<br>4. Mother may select an authorized item, time range or permitted embedded control.<br>5. The system displays the authorized result with applicable source, safety or status labels. |
| Alternative Flows | The actor may cancel before final submission. If no eligible record or result exists, the system shows an empty state and the next allowed action without exposing unauthorized data. |
| Exceptions | Access is denied for an unauthenticated, unauthorized, expired-consent or out-of-scope request. Invalid, missing, expired or conflicting data is rejected with a clear message. External-service failure must preserve a safe state and provide retry guidance without duplicating the action. |
| Business Rules | BR-SAFETY-MONITORING: monitoring is optional, consent-based and cannot guarantee fall detection or emergency response.<br>BR-SAFETY-09: Feedback improves system quality but does not alter retained audit evidence improperly. |
| Other Information | Embedded search/filter/list controls, if needed, are part of this use case and not separately modelled. |



## 3.5 Deferred Version 2 Boundary

- **MF-15 Paid Direct Consultation & Commission:** booking, payment, VNPay, chat/voice/video, ZegoCloud, commission, consultation dispute/refund and expert revenue workflows are Version 2 only.

- **MF-16 Partner Clinic & Sponsored Service Management:** partner registration, partner dashboard, service listings, referrals, sponsor campaigns and partner performance are Version 2 only.

- The former **MF-16 Operation, Impact & Partner Dashboard** is removed completely; no replacement dashboard use case is retained.


## 3.6 Optimisation Examples from the Previous 241-UC Baseline

| Previous modelling pattern | Detailed-scope treatment |
| --- | --- |
| Standalone community/expert/content search and filter UCs | Absorbed into UC-46 Browse Community Feed and Topics, UC-65 Browse Verified Expert Directory and UC-102 Browse Verified Content. |
| Separate file manager/viewer/upload/delete UCs | Re-modelled as record lifecycle UCs UC-83 to UC-88 under MF-08. |
| Separate mobile and portal expert versions | Combined into single cross-platform expert outcomes UC-60 to UC-71. |
| Paid consultation, payment, realtime session, commission and refund UCs | Deferred to MF-15 Version 2; no active Release 1 use case. |
| Partner onboarding, services, campaigns and partner dashboards | Deferred to MF-16 Version 2; no active Release 1 use case. |
| Backend-only payment/AI/realtime/map service UCs | Moved to non-screen integration requirements unless they expose a direct user or admin outcome. |
| Confirmation dialogs, standalone detail modals and pagination UCs | Defined as screen interaction behavior inside the owning use case. |
