# Detailed Design — CareBridge Release 1 (P0-Core + P1-Supporting)

## 1. Mục đích và nguồn dữ liệu (Purpose & Sources)

Tài liệu này chọn lọc từ **121 Use Case** đã được thu hẹp scope trong
[`02_Requirements/SRS/3_Functional_Specification_Detailed_Scope_121UC.md`](../../02_Requirements/SRS/3_Functional_Specification_Detailed_Scope_121UC.md)
và [`4_Functional_Requirements_Detailed_Scope_121UC.md`](../../02_Requirements/SRS/4_Functional_Requirements_Detailed_Scope_121UC.md),
kết hợp với các TDS đã có trong `04_Implement/` và các entity/enum thực tế trong
`05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/` (theo CLAUDE.md,
code hiện tại luôn override tài liệu thiết kế cũ).

Mục tiêu: với mỗi **tính năng cốt lõi (Major Feature)**, xác định **use case chính**
đại diện cho **luồng chính (main flow) đầu-cuối** của tính năng đó, rồi vẽ lại thành
một spec tổng quan (overview) gồm đúng 3 loại giản đồ: **Class Diagram**, **Sequence
Diagram** và **State Machine** — không lặp lại các luồng phụ mang tính CRUD đơn thuần
(search/filter/sort đã được embedded theo quy tắc D-02; create/update/delete/view đơn lẻ
không tạo thành spec riêng trừ khi bản thân nó là một outcome nghiệp vụ khác biệt).

## 2. Phạm vi được chọn (Selected Scope)

Bản thân tài liệu 121-UC đã tự phân tầng ưu tiên rõ ràng ở mục 3.2 và 4.1:

| Tier | Feature Range | Release Scope | Trong `Detailed_Design/`? |
| --- | --- | --- | --- |
| **P0 — Core** | MF-01 → MF-07 (UC-01 → UC-82) | Current Release | ✅ Có |
| **P1 — Supporting** | MF-08 → MF-14 (UC-83 → UC-121) | Current Release, sau P0 | ✅ Có |
| V2 — Deferred | MF-15, MF-16 | Version 2 — không có UC Release 1 | ❌ Loại hẳn (không có TDS/spec chi tiết cho MF-15/16) |

Toàn bộ 14 Major Feature thuộc Release 1 (cả P0-Core lẫn P1-Supporting) đều được vẽ lại
theo cùng phương pháp và mức chi tiết ở đây — P0/P1 chỉ khác thứ tự ưu tiên triển khai
(P0 làm trước, P1 làm sau khi P0 sẵn sàng), không khác nhau về việc có cần Detailed
Design hay không. MF-15/MF-16 (Paid Consultation, Partner/Sponsored) bị loại hoàn toàn
vì SRS xác nhận **không có UC Release 1** nào cho hai feature này (V2-only).

## 3. Cách chọn "use case chính" trong mỗi Major Feature

Trong mỗi MF, các UC được nhóm lại thành 2–3 spec theo nguyên tắc:

1. Giữ lại UC tạo ra **outcome nghiệp vụ khác biệt** (tạo/duyệt/thực thi/khẩn cấp...),
   loại các UC chỉ là biến thể update/delete/view/search của cùng một entity — các thao
   tác đó được **gộp** vào class diagram / state machine của spec chính thay vì tách
   thành spec riêng.
2. Nhóm các UC có **quan hệ nhân-quả trực tiếp trên cùng một luồng** (ví dụ: Register →
   Verify OTP → Login là một luồng định danh duy nhất) thành một spec.
3. Mỗi spec có tối thiểu một **entity trạng thái (stateful entity)** làm trục cho State
   Machine (ví dụ: `AccountStatus`, `ConsentGrant`, `QuestionStatus`, `VerificationStatus`,
   `HandoffStatus`...) lấy trực tiếp từ enum thật trong backend. Khi enum thật chỉ có 2
   trạng thái (ví dụ `BabyProfile.status = ACTIVE/ARCHIVED`), State Machine được vẽ đúng
   với 2 trạng thái đó — không thêm trạng thái suy diễn để "trông đầy đủ hơn".

## 4. Bản đồ Feature → Spec → Use Case

| Feature Folder | Spec File | UC Covered | Stateful Entity (State Machine) |
| --- | --- | --- | --- |
| `MF01_Account_Trust_AccesControl` | 01_Account_Registration_Authentication_Lifecycle | UC-01, UC-02, UC-03, UC-04, UC-05, UC-06, UC-07 | `User.accountStatus` |
| | 02_Community_Identity_Privacy_Consent_Sharing | UC-10, UC-15, UC-16 | `ConsentGrant` lifecycle |
| | 03_Admin_Account_Governance_Security_Audit | UC-14, UC-17, UC-18 | `SecurityEvent.status` / `AccountDeletionRequest.status` |
| `MF02_Mother_Care_Journey` | 01_Mother_Journey_Lifecycle_Dashboard | UC-19, UC-20, UC-21 | `MotherJourney.status` |
| | 02_Maternal_Health_Postpartum_Tracking | UC-22, UC-24, UC-25 | `MaternalHealthMetric.status` |
| | 03_Pregnancy_Exercise_Session_Safety | UC-28, UC-29, UC-30, UC-31 | `ExerciseSession.sessionStatus` |
| `MF03_Baby_Care_Growth_Vaccination` | 01_Baby_Profile_Daily_Care_Overview | UC-32, UC-33, UC-34, UC-35, UC-36, UC-38 | `BabyProfile.status` |
| | 02_Growth_Development_Tracking | UC-39, UC-41, UC-43 | `DevelopmentMilestone` record status |
| | 03_Vaccination_Record_Reminder_Management | UC-44, UC-45 | `VaccinationRecord.status` |
| `MF04_Community_QA_Moderation` | 01_Community_Question_Answer_Flow | UC-46, UC-47, UC-48, UC-50 | `CommunityQuestion.status` / `CommunityAnswer.status` |
| | 02_Content_Moderation_Enforcement_Pipeline | UC-55, UC-56, UC-57, UC-58 | `ContentReport.status` |
| `MF05_Verified_Expert_Network_Contribution` | 01_Expert_Verification_Trust_Lifecycle | UC-60, UC-62, UC-63, UC-70, UC-71 | `VerificationStatus` / `TrustStatus` |
| | 02_Expert_Directory_Contribution_Recognition | UC-64, UC-65, UC-66, UC-67, UC-68, UC-69 | `AvailabilityStatus` |
| `MF06_AI_Nurse_Assistant_Risk_Triage` | 01_AI_Symptom_Intake_Risk_Triage_Emergency_Handoff | UC-72, UC-73, UC-74 | `IntakeStatus` / `RiskLevel` |
| | 02_AI_Knowledge_RedFlag_Governance | UC-75, UC-76 | `EvidenceSource.status` / `RedFlagRule.active` |
| `MF07_Emergency_Map_Nearby_Care_Support` | 01_Emergency_Map_Facility_Route_Navigation | UC-77, UC-78, UC-79 | `EmergencyMapHandoff.status` |
| | 02_Family_Alert_Nearby_Support_Request | UC-80, UC-81, UC-82 | `NearbySupportRequest.status` |
| `MF08_Personal_Health_Records` | 01_Health_Record_Lifecycle_Timeline | UC-83, UC-84, UC-85, UC-86 | `HealthRecord.status` |
| | 02_Health_Summary_Generation_Consent_Sharing | UC-87, UC-88 | `DataPermission.status` |
| `MF09_Reminders_Tasks_Care_Plan` | 01_Reminder_Lifecycle_Today_Tasks | UC-89, UC-90, UC-91, UC-92, UC-93 | `Reminder.status` |
| `MF10_Family_Sync_Cooperative_Care` | 01_Care_Group_Invitation_Lifecycle | UC-94, UC-95, UC-96, UC-97 | `CareGroupMember.inviteStatus` |
| | 02_Family_Permission_Shared_Visibility | UC-98, UC-101 | `PermissionFlag` scope (derived) |
| | 03_Family_Care_Task_Assignment | UC-99, UC-100 | `CareTask.status` (family package FSM) |
| `MF11_Verified_Content_Checklist_Hub` | 01_Content_Browse_Consumption | UC-102, UC-103 | `ContentItem.status` (góc nhìn visibility-gate cho User, đối xứng với spec 02) |
| | 02_Content_Authoring_Review_Publishing_Lifecycle | UC-104, UC-105, UC-106, UC-107, UC-108 | `ContentItem.status` (góc nhìn full authoring FSM cho Content/System Admin) |
| `MF12_Expense_Preparation_Planner` | 01_Expense_Entry_Summary | UC-109, UC-110, UC-111 | Expense record lifecycle tối giản (không có cột status, hard-delete — xem ghi chú trong spec) |
| `MF13_Connected_Device_Health_Data_Integration` | 01_Device_Connection_Sync_Trend_Lifecycle | UC-112, UC-113, UC-114, UC-115 | `HealthDeviceConnection.status` |
| `MF14_Smart_Activity_Monitoring_Safety_Support` | 01_Monitoring_Configuration_Enable_Disable | UC-116, UC-117, UC-118, UC-119 | `ImuMonitoringSession.status` |
| | 02_Fall_Detection_Safety_Check_False_Positive_Feedback | UC-120, UC-121 | `SafetyEvent.status` |

## 5. Quy ước giản đồ (Diagram Convention)

- Toàn bộ giản đồ dùng **PlantUML** (đồng bộ với `docs/spec-templates/CAREBRIDGE_TDS_TEMPLATE.md`
  và các TDS trong `04_Implement/`), đặt trong code fence ```plantuml```.
- Mỗi spec chỉ có đúng 3 giản đồ: **Class Diagram**, **Sequence Diagram (luồng chính)**,
  **State Machine**. Không vẽ Use Case Diagram / Activity Diagram / ERD riêng.
- Ngay dưới mỗi giản đồ là một dòng chú thích in đậm nêu rõ tên giản đồ, ví dụ:
  `**Hình 1 — Class Diagram: Account Authentication Lifecycle**`.
- **Entity, field và enum** trong Class Diagram / State Machine được lấy trực tiếp từ mã
  nguồn backend thật (không suy đoán) — tham chiếu package tương ứng được ghi trong mục
  "Grounding" ở đầu mỗi spec. Tên method service/repository/controller trong Class Diagram
  là **thiết kế minh họa ở mức overview** (đặt tên theo quy ước của codebase), không phải
  chữ ký API đã verify từng dòng — muốn xem chữ ký thật, xem TDS/API spec tương ứng trong
  `04_Implement/`.
- Sequence Diagram chỉ vẽ **happy path của luồng chính**; các nhánh lỗi/luồng phụ được
  tóm tắt bằng 1–2 dòng note, không vẽ đầy đủ.

## 6. Danh sách thư mục

```
Detailed_Design/
├── README.md                                        (tài liệu này)
├── MF01_Account_Trust_AccesControl/                 (P0 — 3 spec)
├── MF02_Mother_Care_Journey/                        (P0 — 3 spec)
├── MF03_Baby_Care_Growth_Vaccination/               (P0 — 3 spec)
├── MF04_Community_QA_Moderation/                    (P0 — 2 spec)
├── MF05_Verified_Expert_Network_Contribution/       (P0 — 2 spec)
├── MF06_AI_Nurse_Assistant_Risk_Triage/              (P0 — 2 spec)
├── MF07_Emergency_Map_Nearby_Care_Support/          (P0 — 2 spec)
├── MF08_Personal_Health_Records/                    (P1 — 2 spec)
├── MF09_Reminders_Tasks_Care_Plan/                  (P1 — 1 spec)
├── MF10_Family_Sync_Cooperative_Care/               (P1 — 3 spec)
├── MF11_Verified_Content_Checklist_Hub/             (P1 — 2 spec)
├── MF12_Expense_Preparation_Planner/                (P1 — 1 spec)
├── MF13_Connected_Device_Health_Data_Integration/   (P1 — 1 spec)
└── MF14_Smart_Activity_Monitoring_Safety_Support/   (P1 — 2 spec)
```

Tổng cộng: 14 Major Feature, 29 spec chi tiết (17 P0-Core + 12 P1-Supporting).
