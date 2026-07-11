# 4. Functional Requirements - Detailed Scope Baseline (121 Use Cases)

> **Functional baseline:** Release 1 contains **121 detailed functional use cases**. This is deliberately more detailed than the previous 46-UC high-level draft while remaining substantially smaller and more implementable than the 241-UC baseline.

## 4.1 Functional Scope and Prioritisation

| Release Tier | Use Case Range | Coverage |
| --- | --- | --- |
| P0 Core | UC-01 to UC-82 (82 UCs) | Account/trust, mother journey, baby journey, community moderation, expert network/contribution, AI triage and emergency map/nearby support. |
| P1 Supporting | UC-83 to UC-121 (39 UCs) | Health records, reminders, family coordination, verified content, expenses, device integration and smart safety monitoring. |
| Version 2 Deferred | No Release 1 UC IDs | Paid consultation/commission and partner clinic/sponsored-service management. |


## 4.2 Functional Requirement Catalogue

| FR ID | UC | Requirement | Feature | Platform | Priority | Expected Behaviour | Acceptance Tier |
| --- | --- | --- | --- | --- | --- | --- | --- |
| FR-001 | UC-01 | Đăng ký tài khoản (Register Account) | MF-01 | Mobile App / Web Portal | P0 | Creates an account with a supported contact method and selected initial role. The new account remains unverified until OTP confirmation succeeds. | Must be fully testable in Release 1 core scope. |
| FR-002 | UC-02 | Xác thực OTP (Verify OTP) | MF-01 | Mobile App / Web Portal | P0 | Validates the OTP sent for account activation or another sensitive account action. | Must be fully testable in Release 1 core scope. |
| FR-003 | UC-03 | Đăng nhập (Log In) | MF-01 | Mobile App / Web Portal | P0 | Authenticates a verified account and routes the user to the role-appropriate workspace. | Must be fully testable in Release 1 core scope. |
| FR-004 | UC-04 | Đăng xuất (Log Out) | MF-01 | Mobile App / Web Portal | P0 | Ends the current session on the active device without deleting the account. | Must be fully testable in Release 1 core scope. |
| FR-005 | UC-05 | Yêu cầu đặt lại mật khẩu (Request Password Reset) | MF-01 | Mobile App / Web Portal | P0 | Starts password recovery by requesting a time-limited reset proof through the registered channel. | Must be fully testable in Release 1 core scope. |
| FR-006 | UC-06 | Đặt lại mật khẩu (Reset Password) | MF-01 | Mobile App / Web Portal | P0 | Sets a new password only after a valid recovery proof is verified. | Must be fully testable in Release 1 core scope. |
| FR-007 | UC-07 | Đổi mật khẩu (Change Password) | MF-01 | Mobile App / Web Portal | P0 | Changes the signed-in user password after current-password validation. | Must be fully testable in Release 1 core scope. |
| FR-008 | UC-08 | Xem hồ sơ tài khoản (View Account Profile) | MF-01 | Mobile App / Web Portal | P0 | Shows the user private account profile, role, status and permitted account controls. | Must be fully testable in Release 1 core scope. |
| FR-009 | UC-09 | Cập nhật hồ sơ tài khoản (Update Account Profile) | MF-01 | Mobile App / Web Portal | P0 | Updates non-sensitive private profile data used for account operation and contact preferences. | Must be fully testable in Release 1 core scope. |
| FR-010 | UC-10 | Quản lý danh tính cộng đồng (Manage Community Identity) | MF-01 | Mobile App / Web Portal | P0 | Creates or updates a public community identity that is separate from private maternal, baby and family information. | Must be fully testable in Release 1 core scope. |
| FR-011 | UC-11 | Quản lý tùy chọn thông báo (Manage Notification Preferences) | MF-01 | Mobile App / Web Portal | P0 | Lets the user choose supported delivery channels and categories for reminders, community replies, family alerts and account events. | Must be fully testable in Release 1 core scope. |
| FR-012 | UC-12 | Xem và đánh dấu thông báo (View and Mark Notifications) | MF-01 | Mobile App / Web Portal | P0 | Shows authorized notifications and allows the user to mark individual or all eligible notifications as read. | Must be fully testable in Release 1 core scope. |
| FR-013 | UC-13 | Quản lý phiên đăng nhập của tôi (Manage Own Login Sessions) | MF-01 | Mobile App / Web Portal | P0 | Lists active or recent own sessions and allows the user to revoke a selected device session. | Must be fully testable in Release 1 core scope. |
| FR-014 | UC-14 | Vô hiệu hóa hoặc xóa tài khoản cá nhân (Deactivate or Delete Own Account) | MF-01 | Mobile App / Web Portal | P0 | Requests account deactivation or deletion subject to retention, care-group and audit obligations. | Must be fully testable in Release 1 core scope. |
| FR-015 | UC-15 | Cấp quyền chia sẻ dữ liệu (Grant Data Permission) | MF-01 | Mobile App / Web Portal | P0 | Creates a purpose-specific, scope-based and time-limited permission for a family member or verified expert to access selected data. | Must be fully testable in Release 1 core scope. |
| FR-016 | UC-16 | Rà soát và thu hồi quyền chia sẻ dữ liệu (Review and Revoke Data Permission) | MF-01 | Mobile App / Web Portal | P0 | Shows active and past sharing permissions and lets the owner revoke an active grant. | Must be fully testable in Release 1 core scope. |
| FR-017 | UC-17 | Quản trị tài khoản và quyền truy cập (Administer User Accounts and Role Access) | MF-01 | Admin Portal | P0 | Allows authorized administrators to review account status, apply role/access changes and restrict misuse under separation-of-duties controls. | Must be fully testable in Release 1 core scope. |
| FR-018 | UC-18 | Rà soát truy cập nhạy cảm và sự kiện bảo mật (Review Sensitive Access and Security Events) | MF-01 | Admin Portal | P0 | Reviews sensitive-record access, abnormal login, permission change and file-access events to determine follow-up or investigation. | Must be fully testable in Release 1 core scope. |
| FR-019 | UC-19 | Khởi tạo hành trình chăm sóc mẹ (Initialize Mother Care Journey) | MF-02 | Mother Mobile App | P0 | Creates a mother journey for preconception, pregnancy or postpartum recovery using the minimum dates and stage context required for stage-based support. | Must be fully testable in Release 1 core scope. |
| FR-020 | UC-20 | Cập nhật giai đoạn và ngày hành trình mẹ (Update Mother Journey Stage and Dates) | MF-02 | Mother Mobile App | P0 | Updates stage, last menstrual period, expected due date, birth date or other permitted journey dates when circumstances change. | Must be fully testable in Release 1 core scope. |
| FR-021 | UC-21 | Xem bảng điều khiển hành trình mẹ (View Mother Journey Dashboard) | MF-02 | Mother Mobile App | P0 | Shows current stage/week, relevant care priorities, reminders, checklists, reviewed content and safe shortcuts. | Must be fully testable in Release 1 core scope. |
| FR-022 | UC-22 | Thêm chỉ số sức khỏe mẹ (Add Maternal Health Metric) | MF-02 | Mother Mobile App | P0 | Records a user-entered maternal indicator such as weight, blood pressure, glucose, fetal movement note or another supported observation. | Must be fully testable in Release 1 core scope. |
| FR-023 | UC-23 | Cập nhật hoặc xóa chỉ số sức khỏe mẹ (Update or Delete Maternal Health Metric) | MF-02 | Mother Mobile App | P0 | Corrects or removes a user-entered maternal metric when ownership and record state allow. | Must be fully testable in Release 1 core scope. |
| FR-024 | UC-24 | Xem xu hướng chỉ số sức khỏe mẹ (View Maternal Health Trend) | MF-02 | Mother Mobile App | P0 | Displays time-based trends for recorded maternal indicators with source labels and clear non-diagnostic context. | Must be fully testable in Release 1 core scope. |
| FR-025 | UC-25 | Ghi nhật ký phục hồi sau sinh (Add Postpartum Recovery Log) | MF-02 | Mother Mobile App | P0 | Records a postpartum recovery observation such as sleep, mood, milk-feeding context, pain note or recovery symptom note. | Must be fully testable in Release 1 core scope. |
| FR-026 | UC-26 | Cập nhật hoặc xóa nhật ký phục hồi sau sinh (Update or Delete Postpartum Recovery Log) | MF-02 | Mother Mobile App | P0 | Corrects or removes an owner-entered postpartum recovery log. | Must be fully testable in Release 1 core scope. |
| FR-027 | UC-27 | Quản lý checklist chuẩn bị (Manage Preparation Checklist) | MF-02 | Mother Mobile App | P0 | Adds, edits and completes preparation items for pregnancy, hospital bag, postpartum recovery or early baby care. | Must be fully testable in Release 1 core scope. |
| FR-028 | UC-28 | Duyệt thư viện bài tập thai kỳ (Browse Pregnancy Exercise Library) | MF-02 | Mother Mobile App | P0 | Shows reviewed pregnancy exercise content with embedded stage/difficulty/duration filters and exercise-specific safety notes. | Must be fully testable in Release 1 core scope. |
| FR-029 | UC-29 | Hoàn thành kiểm tra an toàn trước tập (Complete Pre-exercise Safety Check) | MF-02 | Mother Mobile App | P0 | Collects required pre-exercise safety answers and stops the session entry when configured warning answers are present. | Must be fully testable in Release 1 core scope. |
| FR-030 | UC-30 | Thực hiện phiên tập thai kỳ có phản hồi tư thế tùy chọn (Conduct Pregnancy Exercise Session with Optional Posture Feedback) | MF-02 | Mother Mobile App | P0 | Runs an approved exercise session with start, pause, resume and complete actions and optional rule/ML-based posture feedback after camera consent. | Must be fully testable in Release 1 core scope. |
| FR-031 | UC-31 | Xem lịch sử và kết quả phiên tập (View Exercise History and Session Result) | MF-02 | Mother Mobile App | P0 | Shows completed exercise sessions, duration, completion state, aggregate posture feedback and safety notices. | Must be fully testable in Release 1 core scope. |
| FR-032 | UC-32 | Tạo hồ sơ bé (Create Baby Profile) | MF-03 | Mother Mobile App | P0 | Creates a baby profile with core identity and birth context needed for journals, growth and vaccination tracking. | Must be fully testable in Release 1 core scope. |
| FR-033 | UC-33 | Cập nhật hoặc lưu trữ hồ sơ bé (Update or Archive Baby Profile) | MF-03 | Mother Mobile App | P0 | Updates permitted baby profile fields or archives a profile that is no longer actively managed without destroying linked history. | Must be fully testable in Release 1 core scope. |
| FR-034 | UC-34 | Chuyển hồ sơ bé đang theo dõi (Switch Active Baby Profile) | MF-03 | Mother Mobile App | P0 | Selects the baby profile used by the current baby dashboard, journals, growth views and relevant reminders. | Must be fully testable in Release 1 core scope. |
| FR-035 | UC-35 | Xem tổng quan chăm sóc bé (View Baby Care Overview) | MF-03 | Mother Mobile App | P0 | Shows a baby overview with recent journals, growth, milestones, vaccination status and current care prompts. | Must be fully testable in Release 1 core scope. |
| FR-036 | UC-36 | Thêm nhật ký chăm sóc bé hằng ngày (Add Baby Daily Log) | MF-03 | Mother Mobile App | P0 | Records feeding, sleep, diaper, symptom note or other supported daily care observation for a selected baby. | Must be fully testable in Release 1 core scope. |
| FR-037 | UC-37 | Cập nhật hoặc xóa nhật ký chăm sóc bé (Update or Delete Baby Daily Log) | MF-03 | Mother Mobile App | P0 | Corrects or removes an owner-entered baby journal entry. | Must be fully testable in Release 1 core scope. |
| FR-038 | UC-38 | Xem tổng hợp nhật ký bé (View Baby Log Summary) | MF-03 | Mother Mobile App | P0 | Shows recent 24-hour or 7-day feeding, sleep, diaper and logged observation patterns for the active baby. | Must be fully testable in Release 1 core scope. |
| FR-039 | UC-39 | Ghi mốc phát triển (Record Development Milestone) | MF-03 | Mother Mobile App | P0 | Records a caregiver-observed development milestone with date and note. | Must be fully testable in Release 1 core scope. |
| FR-040 | UC-40 | Cập nhật hoặc xóa mốc phát triển (Update or Delete Development Milestone) | MF-03 | Mother Mobile App | P0 | Corrects or removes a caregiver-entered development milestone. | Must be fully testable in Release 1 core scope. |
| FR-041 | UC-41 | Thêm số đo tăng trưởng (Add Growth Measurement) | MF-03 | Mother Mobile App | P0 | Records supported growth measurements for a selected baby with measurement date and source. | Must be fully testable in Release 1 core scope. |
| FR-042 | UC-42 | Cập nhật hoặc xóa số đo tăng trưởng (Update or Delete Growth Measurement) | MF-03 | Mother Mobile App | P0 | Corrects or removes a user-entered growth measurement. | Must be fully testable in Release 1 core scope. |
| FR-043 | UC-43 | Xem xu hướng và lịch sử tăng trưởng (View Growth Trend and Measurement History) | MF-03 | Mother Mobile App | P0 | Displays growth measurement history and charts with reference context and prompts to seek professional assessment when appropriate. | Must be fully testable in Release 1 core scope. |
| FR-044 | UC-44 | Quản lý bản ghi tiêm chủng (Manage Vaccination Record) | MF-03 | Mother Mobile App | P0 | Adds, updates or removes a user-entered vaccination record for the selected baby. | Must be fully testable in Release 1 core scope. |
| FR-045 | UC-45 | Xem lịch tiêm tham khảo và trạng thái nhắc lịch (View Vaccination Reference Schedule and Reminder Status) | MF-03 | Mother Mobile App | P0 | Displays a reference vaccination schedule, completed/recorded status and linked reminder state for the selected baby. | Must be fully testable in Release 1 core scope. |
| FR-046 | UC-46 | Duyệt bảng tin và chủ đề cộng đồng (Browse Community Feed and Topics) | MF-04 | Mobile App | P0 | Displays moderated questions and topic collections. Keyword search, topic chips, filters, sort and pagination are embedded controls of this browse outcome. | Must be fully testable in Release 1 core scope. |
| FR-047 | UC-47 | Xem chi tiết câu hỏi cộng đồng (View Community Question Detail) | MF-04 | Mobile App | P0 | Shows a permitted question, its answer thread, source labels, moderation state and allowed interactions. | Must be fully testable in Release 1 core scope. |
| FR-048 | UC-48 | Đăng câu hỏi cộng đồng (Create Community Question) | MF-04 | Mobile App | P0 | Creates a topic-based community question and optionally applies anonymous public display while preserving internal accountability. | Must be fully testable in Release 1 core scope. |
| FR-049 | UC-49 | Chỉnh sửa hoặc xóa bài đăng cộng đồng của tôi (Edit or Delete Own Community Post) | MF-04 | Mobile App | P0 | Edits or removes the user own community post while the post is not locked by moderation or investigation. | Must be fully testable in Release 1 core scope. |
| FR-050 | UC-50 | Trả lời cộng đồng (Post Community Answer) | MF-04 | Mobile App / Expert Portal | P0 | Posts a personal-experience answer or a verified expert answer with the applicable source/role label. | Must be fully testable in Release 1 core scope. |
| FR-051 | UC-51 | Chỉnh sửa hoặc xóa câu trả lời của tôi (Edit or Delete Own Community Answer) | MF-04 | Mobile App / Expert Portal | P0 | Edits or removes the user own answer when no moderation lock or evidence hold applies. | Must be fully testable in Release 1 core scope. |
| FR-052 | UC-52 | Phản ứng với câu trả lời cộng đồng (React to Community Answer) | MF-04 | Mobile App | P0 | Adds or removes a permitted helpfulness reaction to a community answer. | Must be fully testable in Release 1 core scope. |
| FR-053 | UC-53 | Đánh dấu bài đăng cộng đồng (Bookmark Community Post) | MF-04 | Mobile App | P0 | Saves or removes a permitted post from the user private bookmark list. | Must be fully testable in Release 1 core scope. |
| FR-054 | UC-54 | Theo dõi hoặc bỏ theo dõi chủ đề cộng đồng (Follow or Unfollow Community Topic) | MF-04 | Mobile App | P0 | Subscribes or unsubscribes the user from a community topic for feed relevance and optional notifications. | Must be fully testable in Release 1 core scope. |
| FR-055 | UC-55 | Báo cáo nội dung hoặc tài khoản không an toàn (Report Unsafe Community Content or Account) | MF-04 | Mobile App / Web Portal | P0 | Submits a report for misinformation, dangerous advice, hidden advertising, harassment, privacy breach or suspicious account behavior. | Must be fully testable in Release 1 core scope. |
| FR-056 | UC-56 | Xem hàng đợi kiểm duyệt (View Moderation Queue) | MF-04 | Admin Portal | P0 | Shows reported, automatically flagged and pre-publication content awaiting authorized moderator review. | Must be fully testable in Release 1 core scope. |
| FR-057 | UC-57 | Kiểm duyệt nội dung cộng đồng (Moderate Community Content) | MF-04 | Admin Portal | P0 | Approves, hides, locks, labels or requests revision for a community post or answer based on policy and safety rules. | Must be fully testable in Release 1 core scope. |
| FR-058 | UC-58 | Giải quyết báo cáo và áp dụng xử lý vi phạm (Resolve Content or Account Report and Apply Enforcement) | MF-04 | Admin Portal | P0 | Resolves a report and, when authorized, applies warning, posting restriction, suspension escalation or no-action decision. | Must be fully testable in Release 1 core scope. |
| FR-059 | UC-59 | Quản lý chủ đề và trạng thái hiển thị cộng đồng (Manage Community Topics and Visibility) | MF-04 | Admin Portal | P0 | Creates, updates, reorders or disables community topics and their visibility rules. | Must be fully testable in Release 1 core scope. |
| FR-060 | UC-60 | Nộp hồ sơ chuyên gia (Submit Expert Profile) | MF-05 | Expert Portal / Expert App | P0 | Creates an expert application profile with specialty, experience, support scope and public professional information. | Must be fully testable in Release 1 core scope. |
| FR-061 | UC-61 | Cập nhật hồ sơ chuyên gia (Update Expert Profile) | MF-05 | Expert Portal / Expert App | P0 | Updates approved professional profile fields and public service information subject to review rules. | Must be fully testable in Release 1 core scope. |
| FR-062 | UC-62 | Nộp hoặc thay thế giấy tờ xác thực (Submit or Replace Verification Documents) | MF-05 | Expert Portal / Expert App | P0 | Uploads required credentials, certificates and supporting evidence for verification or renewal. | Must be fully testable in Release 1 core scope. |
| FR-063 | UC-63 | Xem trạng thái xác thực và nộp gia hạn (View Verification Status and Renew Submission) | MF-05 | Expert Portal / Expert App | P0 | Shows verification result, required corrections, expiry status and allows a renewal submission when eligible. | Must be fully testable in Release 1 core scope. |
| FR-064 | UC-64 | Cấu hình trạng thái sẵn sàng và phạm vi hỗ trợ chuyên gia (Configure Expert Availability and Service Scope) | MF-05 | Expert Portal / Expert App | P0 | Configures availability, support methods, service area and public readiness state for the expert directory and nearby support eligibility. | Must be fully testable in Release 1 core scope. |
| FR-065 | UC-65 | Duyệt danh mục chuyên gia đã xác thực (Browse Verified Expert Directory) | MF-05 | Mobile App | P0 | Displays verified experts with embedded search and filters by specialty, availability, badge and consented service area. | Must be fully testable in Release 1 core scope. |
| FR-066 | UC-66 | Xem hồ sơ chuyên gia đã xác thực (View Verified Expert Profile) | MF-05 | Mobile App | P0 | Shows a verified expert public professional profile, specialties, supported scope, availability and contribution reputation. | Must be fully testable in Release 1 core scope. |
| FR-067 | UC-67 | Xem hàng đợi câu hỏi chuyên môn (View Expert Question Queue) | MF-05 | Expert Portal / Expert App | P0 | Shows community questions matched to the expert verified specialties and public support scope. | Must be fully testable in Release 1 core scope. |
| FR-068 | UC-68 | Đăng trả lời với nhãn chuyên gia (Post Verified Expert Answer) | MF-05 | Expert Portal / Expert App | P0 | Posts an expert answer with verified badge, source/safety label and contribution attribution. | Must be fully testable in Release 1 core scope. |
| FR-069 | UC-69 | Xem điểm đóng góp và huy hiệu (View Contribution Points and Badges) | MF-05 | Expert Portal / Expert App | P0 | Shows contribution points, badges and qualifying community activities used to recognize responsible participation. | Must be fully testable in Release 1 core scope. |
| FR-070 | UC-70 | Rà soát hồ sơ xác thực chuyên gia (Review Expert Verification Submission) | MF-05 | Admin Portal | P0 | Approves, requests supplementation, rejects or renews an expert verification submission after credential review. | Must be fully testable in Release 1 core scope. |
| FR-071 | UC-71 | Hạn chế, đình chỉ hoặc khôi phục trạng thái tin cậy chuyên gia (Restrict, Suspend or Reinstate Expert Trust Status) | MF-05 | Admin Portal | P0 | Applies, updates or lifts an expert restriction/suspension when credentials expire or policy violations require it. | Must be fully testable in Release 1 core scope. |
| FR-072 | UC-72 | Chạy tiếp nhận triệu chứng bằng AI (Run AI Symptom Intake) | MF-06 | Mother Mobile App | P0 | Collects structured symptom information, timing, severity and permitted context through a guided intake flow. | Must be fully testable in Release 1 core scope. |
| FR-073 | UC-73 | Xem kết quả phân loại rủi ro (View Risk Triage Result) | MF-06 | Mother Mobile App | P0 | Shows non-diagnostic green, yellow or red orientation with safe next-step guidance and clearly labelled uncertainty. | Must be fully testable in Release 1 core scope. |
| FR-074 | UC-74 | Mở hỗ trợ khẩn cấp từ kết quả rủi ro đỏ (Open Emergency Support from a Red Risk Result) | MF-06 | Mother Mobile App | P0 | Transfers a user from a red-risk result to the Emergency Map and Nearby Care Support flow without treating the result as a diagnosis. | Must be fully testable in Release 1 core scope. |
| FR-075 | UC-75 | Quản lý nguồn tri thức AI đã phê duyệt (Manage Approved AI Knowledge Sources) | MF-06 | Admin Portal | P0 | Registers, reviews, versions or disables approved knowledge sources available to AI retrieval and safe answer composition. | Must be fully testable in Release 1 core scope. |
| FR-076 | UC-76 | Cấu hình quy tắc rủi ro và cờ đỏ AI (Configure AI Risk and Red-Flag Rules) | MF-06 | Admin Portal | P0 | Maintains conservative risk thresholds, red-flag patterns, safe fallback actions and rule version history. | Must be fully testable in Release 1 core scope. |
| FR-077 | UC-77 | Mở bản đồ hỗ trợ khẩn cấp (Open Emergency Map) | MF-07 | Mother Mobile App | P0 | Opens the shared emergency map after informing the user about location permission and the non-dispatch scope. | Must be fully testable in Release 1 core scope. |
| FR-078 | UC-78 | Tìm cơ sở chăm sóc gần đây (Find Nearby Care Facilities) | MF-07 | Mother Mobile App | P0 | Finds permitted nearby care facilities based on the user approved location or a selected area. | Must be fully testable in Release 1 core scope. |
| FR-079 | UC-79 | Xem lộ trình, ETA và gọi nhanh hoặc chỉ đường (View Route, ETA and Quick Call or Navigate) | MF-07 | Mother Mobile App | P0 | Shows route/ETA and enables quick call or navigation to a selected care facility through the map/device capability. | Must be fully testable in Release 1 core scope. |
| FR-080 | UC-80 | Chia sẻ vị trí có thời hạn và gửi cảnh báo khẩn cấp cho gia đình (Share Time-Limited Location and Send Family Emergency Alert) | MF-07 | Mother Mobile App | P0 | Shares minimum necessary location/context for a time-limited emergency alert to selected family recipients. | Must be fully testable in Release 1 core scope. |
| FR-081 | UC-81 | Tạo hoặc hủy yêu cầu hỗ trợ gần vị trí (Create or Cancel Nearby Support Request) | MF-07 | Mother Mobile App | P0 | Creates or cancels a consented nearby-support request visible only to eligible verified experts; no expert arrival is guaranteed. | Must be fully testable in Release 1 core scope. |
| FR-082 | UC-82 | Quản lý khả dụng gần vị trí và phản hồi yêu cầu hỗ trợ (Manage Expert Nearby Availability and Respond to Nearby Support Request) | MF-07 | Expert App / Expert Portal | P0 | Lets an eligible verified expert opt in to nearby availability, view minimal request context and accept, decline or stop responding to a request. | Must be fully testable in Release 1 core scope. |
| FR-083 | UC-83 | Thêm hồ sơ sức khỏe cá nhân và tệp đính kèm (Add Personal Health Record and Attachment) | MF-08 | Mother Mobile App | P1 | Creates a maternal or baby health record and attaches permitted documents or images with type, date and source metadata. | Must be independently testable after P0 dependencies are available. |
| FR-084 | UC-84 | Cập nhật siêu dữ liệu hồ sơ sức khỏe (Update Health Record Metadata) | MF-08 | Mother Mobile App | P1 | Updates title, category, date, source label, tags or note of an owner-entered health record without altering the protected file itself unless allowed. | Must be independently testable after P0 dependencies are available. |
| FR-085 | UC-85 | Lưu trữ hoặc xóa hồ sơ sức khỏe do người dùng nhập (Archive or Delete User-entered Health Record) | MF-08 | Mother Mobile App | P1 | Archives or requests deletion of an owner-entered record when retention, sharing and safety-evidence obligations allow. | Must be independently testable after P0 dependencies are available. |
| FR-086 | UC-86 | Xem timeline và chi tiết hồ sơ sức khỏe (View Health Record Timeline and Detail) | MF-08 | Mother Mobile App | P1 | Shows authorized maternal and baby records by time, category and source with embedded filtering inside the timeline. | Must be independently testable after P0 dependencies are available. |
| FR-087 | UC-87 | Tạo bản tóm tắt sức khỏe (Generate Health Summary) | MF-08 | Mother Mobile App | P1 | Creates a selected-period summary from user-chosen maternal or baby records for personal review or permitted sharing. | Must be independently testable after P0 dependencies are available. |
| FR-088 | UC-88 | Chia sẻ bản tóm tắt hoặc hồ sơ đã chọn theo consent (Share Health Summary or Selected Records Under Consent) | MF-08 | Mother Mobile App | P1 | Shares selected records or a summary only through an active MF-01 consent grant with scope and expiry. | Must be independently testable after P0 dependencies are available. |
| FR-089 | UC-89 | Tạo nhắc lịch hẹn (Create Appointment Reminder) | MF-09 | Mother Mobile App | P1 | Creates a reminder for a self-entered appointment, follow-up, examination, test or other care event. | Must be independently testable after P0 dependencies are available. |
| FR-090 | UC-90 | Tạo nhắc thuốc hoặc vitamin (Create Medicine or Vitamin Reminder) | MF-09 | Mother Mobile App | P1 | Creates a user-entered medicine or vitamin reminder based on prior professional advice or personal routine. | Must be independently testable after P0 dependencies are available. |
| FR-091 | UC-91 | Tạo nhắc lịch tiêm (Create Vaccination Reminder) | MF-09 | Mother Mobile App | P1 | Creates or confirms a reminder linked to the selected baby vaccination record or reference schedule. | Must be independently testable after P0 dependencies are available. |
| FR-092 | UC-92 | Cập nhật, hoãn, hoàn thành, bỏ qua hoặc xóa nhắc lịch (Update, Snooze, Complete, Skip or Delete Care Reminder) | MF-09 | Mother Mobile App | P1 | Manages the lifecycle of a user reminder, including time updates, snooze, complete, skip and deletion where permitted. | Must be independently testable after P0 dependencies are available. |
| FR-093 | UC-93 | Xem việc cần làm hôm nay và kế hoạch chăm sóc (View Today Tasks and Care Plan) | MF-09 | Mother Mobile App | P1 | Shows due reminders, checklist items and permitted family tasks in a prioritized today view. | Must be independently testable after P0 dependencies are available. |
| FR-094 | UC-94 | Tạo nhóm chăm sóc (Create Care Group) | MF-10 | Mother Mobile App | P1 | Creates a care group for a mother, baby or household care context with the creator as initial owner. | Must be independently testable after P0 dependencies are available. |
| FR-095 | UC-95 | Mời hoặc thu hồi lời mời người thân (Invite or Revoke Family Member Invitation) | MF-10 | Mother Mobile App | P1 | Sends or revokes a care-group invitation using a permitted contact or invite link. | Must be independently testable after P0 dependencies are available. |
| FR-096 | UC-96 | Chấp nhận hoặc từ chối lời mời nhóm chăm sóc (Accept or Reject Care Group Invitation) | MF-10 | Family Mobile App | P1 | Lets a recipient accept or reject an active care-group invitation after authentication. | Must be independently testable after P0 dependencies are available. |
| FR-097 | UC-97 | Quản lý thành viên nhóm chăm sóc (Manage Care Group Membership) | MF-10 | Mother Mobile App / Family Mobile App | P1 | Allows a group owner to remove a member and an individual member to leave a group where rules permit. | Must be independently testable after P0 dependencies are available. |
| FR-098 | UC-98 | Quản lý phạm vi quyền của người thân (Manage Family Permission Scope) | MF-10 | Mother Mobile App | P1 | Defines and updates family access to selected calendar, tasks, alerts, logs, summaries or records. | Must be independently testable after P0 dependencies are available. |
| FR-099 | UC-99 | Tạo, cập nhật hoặc hủy nhiệm vụ chăm sóc gia đình (Create, Update or Cancel Family Care Task) | MF-10 | Mother Mobile App | P1 | Creates, edits or cancels a care task assigned to an eligible care-group member. | Must be independently testable after P0 dependencies are available. |
| FR-100 | UC-100 | Cập nhật trạng thái nhiệm vụ được giao (Update Assigned Task Status) | MF-10 | Family Mobile App | P1 | Updates the status of a task assigned to the signed-in family member, such as in progress, completed or needs help. | Must be independently testable after P0 dependencies are available. |
| FR-101 | UC-101 | Xem lịch, dữ liệu và cảnh báo chăm sóc được chia sẻ (View Shared Care Calendar, Data and Alerts) | MF-10 | Family Mobile App | P1 | Displays calendar items, shared care data and family alerts only within the member active permission scope. | Must be independently testable after P0 dependencies are available. |
| FR-102 | UC-102 | Duyệt nội dung đã xác thực (Browse Verified Content) | MF-11 | Mobile App | P1 | Displays approved articles, FAQs and checklists with embedded keyword, stage and topic controls. | Must be independently testable after P0 dependencies are available. |
| FR-103 | UC-103 | Xem chi tiết nội dung đã xác thực (View Verified Content Detail) | MF-11 | Mobile App | P1 | Shows the selected article, FAQ or checklist with source, review/version state, updated date and safety notes. | Must be independently testable after P0 dependencies are available. |
| FR-104 | UC-104 | Tạo nội dung đã xác thực (Create Verified Content) | MF-11 | Admin Portal | P1 | Creates a draft article, FAQ or checklist with stage/topic mapping, sources and initial review state. | Must be independently testable after P0 dependencies are available. |
| FR-105 | UC-105 | Cập nhật nội dung đã xác thực và nguồn (Update Verified Content and Sources) | MF-11 | Admin Portal | P1 | Updates an existing content draft or creates a new version when published content needs changes to body, source, tag or safety notes. | Must be independently testable after P0 dependencies are available. |
| FR-106 | UC-106 | Rà soát và xuất bản phiên bản nội dung (Review and Publish Content Version) | MF-11 | Admin Portal | P1 | Reviews a content version and approves, rejects or requests revision before public publication. | Must be independently testable after P0 dependencies are available. |
| FR-107 | UC-107 | Gỡ xuất bản hoặc lưu trữ nội dung (Unpublish or Archive Content) | MF-11 | Admin Portal | P1 | Withdraws or archives content that is outdated, unsafe, incorrect or replaced by a newer version. | Must be independently testable after P0 dependencies are available. |
| FR-108 | UC-108 | Quản lý danh mục và ánh xạ giai đoạn/chủ đề nội dung (Manage Content Categories and Stage/Topic Mapping) | MF-11 | Admin Portal | P1 | Maintains categories, stage mappings, topic tags and display order used by the content hub. | Must be independently testable after P0 dependencies are available. |
| FR-109 | UC-109 | Thêm khoản chi chuẩn bị (Add Expense Entry) | MF-12 | Mother Mobile App | P1 | Records a household preparation expense with category, amount, date and optional experience note. | Must be independently testable after P0 dependencies are available. |
| FR-110 | UC-110 | Cập nhật hoặc xóa khoản chi chuẩn bị (Update or Delete Expense Entry) | MF-12 | Mother Mobile App | P1 | Corrects or deletes an owner-entered preparation expense. | Must be independently testable after P0 dependencies are available. |
| FR-111 | UC-111 | Xem tổng hợp chi phí chuẩn bị (View Expense Summary) | MF-12 | Mother Mobile App | P1 | Shows simple expense totals by category, month or journey/baby period. | Must be independently testable after P0 dependencies are available. |
| FR-112 | UC-112 | Kết nối thiết bị hoặc nền tảng sức khỏe (Connect Health Device or Platform) | MF-13 | Mother Mobile App | P1 | Connects a supported device/platform after the user reviews permissions and selected indicator scope. | Must be independently testable after P0 dependencies are available. |
| FR-113 | UC-113 | Nhập hoặc đồng bộ quan sát từ thiết bị (Import or Synchronize Device Observations) | MF-13 | Mother Mobile App | P1 | Imports or synchronizes selected device observations such as heart rate, sleep, steps, SpO2, temperature or blood pressure where supported. | Must be independently testable after P0 dependencies are available. |
| FR-114 | UC-114 | Xem xu hướng và chất lượng dữ liệu thiết bị (View Device Data Trend and Quality) | MF-13 | Mother Mobile App | P1 | Displays imported data trends, source labels, data gaps and quality cautions for the selected indicator. | Must be independently testable after P0 dependencies are available. |
| FR-115 | UC-115 | Ngắt kết nối thiết bị và xóa dữ liệu đã nhập (Disconnect Device and Delete Imported Data) | MF-13 | Mother Mobile App | P1 | Stops future synchronization and allows the user to request deletion of imported data according to retention rules. | Must be independently testable after P0 dependencies are available. |
| FR-116 | UC-116 | Quản lý liên hệ khẩn cấp (Manage Emergency Contacts) | MF-14 | Mother Mobile App | P1 | Adds, verifies, reprioritizes or removes emergency alert recipients used by safety monitoring and emergency flows. | Must be independently testable after P0 dependencies are available. |
| FR-117 | UC-117 | Cấu hình giám sát hoạt động thông minh (Configure Smart Activity Monitoring) | MF-14 | Mother Mobile App | P1 | Configures sensor consent, alert recipients, countdown, location-sharing option and monitoring conditions. | Must be independently testable after P0 dependencies are available. |
| FR-118 | UC-118 | Bật giám sát hoạt động thông minh (Enable Smart Activity Monitoring) | MF-14 | Mother Mobile App | P1 | Starts IMU-based monitoring after configuration, permissions and consent are valid. | Must be independently testable after P0 dependencies are available. |
| FR-119 | UC-119 | Tắt giám sát hoạt động thông minh (Disable Smart Activity Monitoring) | MF-14 | Mother Mobile App | P1 | Stops IMU monitoring and prevents new candidate safety events from being created after the state change. | Must be independently testable after P0 dependencies are available. |
| FR-120 | UC-120 | Xử lý sự kiện nghi ngờ ngã hoặc va chạm và kiểm tra an toàn (Handle Suspected Fall or Impact and Safety Check) | MF-14 | Mother Mobile App | P1 | Detects a candidate fall/impact, opens a safety countdown and records I am OK, Need Help or emergency-support handoff response. | Must be independently testable after P0 dependencies are available. |
| FR-121 | UC-121 | Xem lịch sử sự kiện an toàn và báo cáo phát hiện nhầm (Review Safety Event History and Report False Positive) | MF-14 | Mother Mobile App | P1 | Shows prior safety events and lets the user mark a false detection with a reason to improve future threshold evaluation. | Must be independently testable after P0 dependencies are available. |


## 4.3 Screen Flow by Role

### 4.3.1 Mother Mobile App

`Welcome / Authentication -> Mother Journey Setup -> Mother Home -> [Mother Journey | Baby Care | Community | AI Triage | Emergency Map | Health Records | Today Tasks | Care Group | Content Hub | Expense Planner | Devices | Smart Safety] -> Account & Notifications`

### 4.3.2 Family Member Mobile App

`Invitation -> Authentication -> Family Home -> [Shared Calendar | Shared Data | Assigned Tasks | Family Alerts] -> Account & Notifications`

### 4.3.3 Expert App / Expert Portal

`Authentication -> Expert Onboarding / Verification -> Expert Home -> [Professional Profile | Documents & Renewal | Availability | Question Queue | Contribution] -> Nearby Support -> Account & Notifications`

### 4.3.4 Moderator / Content Admin / System Admin Portal

`Web Login -> Role Dashboard -> [Moderation Queue | Reports & Enforcement | Topics | Expert Verification | Accounts & Roles | Audit/Security | AI Governance | Content Draft/Review/Publishing]`


## 4.4 Primary Screen / Workspace Catalogue

| Screen / Workspace | Platform | Name | Purpose | Mapped Use Cases |
| --- | --- | --- | --- | --- |
| S-01 | Shared | Authentication & Recovery | Register, OTP verification, login, logout, password recovery/reset/change. | UC-01 to UC-07 |
| S-02 | Shared | Account, Community Identity & Privacy | Private profile, public community identity, notification preferences, sessions, data permissions and account lifecycle. | UC-08 to UC-16 |
| A-01 | Admin Portal | Accounts, Roles & Audit | User status/roles, sensitive access and security event review. | UC-17 to UC-18 |
| M-01 | Mother Mobile | Mother Journey Dashboard | Stage/week overview, maternal metrics, postpartum logs and preparation checklist. | UC-19 to UC-27 |
| M-02 | Mother Mobile | Pregnancy Exercise | Exercise library, safety check, session, optional posture feedback and history. | UC-28 to UC-31 |
| M-03 | Mother Mobile | Baby Care Workspace | Baby profiles, daily logs, milestones, growth and vaccination. | UC-32 to UC-45 |
| M-04 | Mobile App | Community Feed & Question Detail | Feed/topic browsing with embedded search/filter, question/answer interactions, reports and bookmarks. | UC-46 to UC-55 |
| A-02 | Admin Portal | Moderation & Topic Governance | Queue, content decisions, report resolution, enforcement and topic visibility. | UC-56 to UC-59 |
| E-01 | Expert App / Portal | Expert Profile & Verification | Professional profile, documents, verification status, availability and service scope. | UC-60 to UC-64 |
| M-05 | Mobile App | Verified Expert Directory | Embedded search/filter, expert profile and trust/contribution display. | UC-65 to UC-66 |
| E-02 | Expert App / Portal | Expert Contribution | Question queue, verified answers and contribution points/badges. | UC-67 to UC-69 |
| A-03 | Admin Portal | Expert Governance | Verification review and trust restriction/suspension/reinstatement. | UC-70 to UC-71 |
| M-06 | Mother Mobile | AI Symptom Intake & Triage | Structured intake, non-diagnostic result and emergency handoff. | UC-72 to UC-74 |
| A-04 | Admin Portal | AI Governance | Approved knowledge and risk/red-flag rule management. | UC-75 to UC-76 |
| M-07 | Mother Mobile | Emergency Map & Nearby Care | Map, nearby facilities, route/ETA, quick actions, location alert and support request. | UC-77 to UC-81 |
| E-03 | Expert App / Portal | Nearby Support Response | Nearby availability, minimal request context and response decision. | UC-82 |
| M-08 | Mother Mobile | Personal Health Records | Timeline, protected attachment management, summaries and consented sharing. | UC-83 to UC-88 |
| M-09 | Mother Mobile | Reminders & Today Tasks | Appointment, medicine/vitamin, vaccination reminders and daily care plan. | UC-89 to UC-93 |
| M-10 | Mother / Family Mobile | Care Group | Group, invitation, membership, permissions, family tasks, calendar/data/alerts. | UC-94 to UC-101 |
| M-11 | Mobile App | Verified Content Hub | Reviewed content browse/detail with embedded search/filter. | UC-102 to UC-103 |
| A-05 | Admin Portal | Content Lifecycle | Draft, update, review/publish, withdraw/archive and category mapping. | UC-104 to UC-108 |
| M-12 | Mother Mobile | Expense Planner | Expense entry, edit/delete and summary. | UC-109 to UC-111 |
| M-13 | Mother Mobile | Connected Devices | Connection, sync/import, trend/quality and disconnect/deletion. | UC-112 to UC-115 |
| M-14 | Mother Mobile | Smart Safety | Emergency contacts, configuration, enable/disable, safety check, event history and feedback. | UC-116 to UC-121 |


## 4.5 Screen Authorization

| Actor | Allowed Areas | Authorization Limit |
| --- | --- | --- |
| Guest | S-01 | Registration, OTP and password recovery only; no private health/community management access. |
| User | S-01, S-02, M-04, M-05, M-11 | May use shared functions and browse only content allowed by visibility/moderation state. |
| Mother | S-01, S-02, M-01 to M-14 | Owns mother/baby data, health records, reminders, care groups and safety configuration; shares only by explicit consent. |
| Family Member | S-01, S-02, M-04, M-05, M-10, M-11 | May access only active group data/calendar/tasks/alerts within active permission scope. |
| Expert Applicant / Verified Expert | S-01, S-02, E-01 to E-03, M-04 | Applicant has onboarding/document rights; verified expert gains directory, answer and nearby-support rights based on trust status. |
| Moderator | A-02, A-03 (policy-limited) | Can moderate community evidence but cannot browse unrelated private health records. |
| Content Admin | A-04 (approved knowledge as assigned), A-05, A-02 topic controls | Can manage content and categories but not platform account roles unless separately assigned. |
| System Admin | A-01 to A-05 | Manages access, expert trust, audit/security and AI governance under separation-of-duties controls. |


## 4.6 Embedded Interactions That Are Not Standalone Use Cases

| Interaction | Treatment |
| --- | --- |
| Keyword search, filters, sort and pagination | Embedded in the owning browse/list workspace and covered by its use case. |
| Confirmation dialogs, bottom sheets and navigation | UI states inside the owning use case. |
| Attachment preview, upload progress and delete confirmation | Part of health record, credential or content lifecycle. |
| Push/email delivery | Non-screen event handling invoked by an approved use case. |
| Map permission and location expiry | Consent behavior inside emergency map, location alert or expert nearby availability. |
| Camera consent for posture feedback | Optional part of UC-30, not a separate flow. |
| System-only AI retrieval, map query, device sync scheduler and sensor evaluation | Integration/non-screen function unless it produces a direct user/admin outcome. |


## 4.7 Non-screen Functional Requirements

| ID | Function | Expected Behaviour |
| --- | --- | --- |
| NS-01 | Authentication and session protection | Issue, rotate and revoke credentials; rate-limit OTP/recovery; record security events. |
| NS-02 | Consent enforcement | Check recipient, purpose, scope, expiry and revocation before protected data access. |
| NS-03 | Notification delivery | Deliver approved reminder, community, verification, family and safety events without leaking protected data in payloads. |
| NS-04 | Community safety pipeline | Apply pre/post-publication checks, queue flagged content and retain moderation evidence. |
| NS-05 | AI safety pipeline | Normalize intake, retrieve approved knowledge, apply conservative red-flag fallback and log rule/source versions. |
| NS-06 | Map and location processing | Resolve permitted location, nearby results, route/ETA and location-expiry behavior. |
| NS-07 | Protected storage | Store health records and credentials with authorization, audit and source metadata. |
| NS-08 | Reminder scheduler | Evaluate due reminders/tasks and trigger allowed notifications. |
| NS-09 | Device observation import | Retain source/time/quality metadata and enforce selected indicator scope. |
| NS-10 | IMU candidate-event processing | Evaluate configured sensor patterns, create candidate events, open safety confirmation and route alerts according to consent. |


## 4.8 Explicitly Deferred / Removed Areas

- **Deferred to Version 2:** paid private consultation booking, VNPay payment, ZegoCloud chat/voice/video, commission, consultation reviews/disputes/refunds; partner registration, partner dashboards, service listings, referrals and sponsored campaigns.

- **Removed completely:** the former Operation, Impact & Partner Dashboard feature.

- **Excluded throughout:** diagnosis, prescription, dosage advice, official EMR/HIS, emergency dispatch, medical-device certification, hospital resource scheduling, insurance claims and uncontrolled medical advertising.
