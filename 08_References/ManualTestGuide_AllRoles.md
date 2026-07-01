# CareBridge — Hướng dẫn Test tay Toàn diện theo Role (Web + Mobile)

> Ngày tạo: 2026-07-01 (cập nhật: đã fix bug #1, #3, #4 ở mục 4 — xem ghi chú "ĐÃ FIX" tại từng mục)
> Nguồn đối chiếu: `02_Requirements/SRS/3_Functional_Specification.md` (241 UC), `4_Functional_Requirements.md` (ma trận phân quyền 4.1.3), code thực tế `05_Development/CareBridgeAPI`, `CareBridgeWebApp`, `CareBridgeMobileApp` tại thời điểm nhánh `dev` (commit `2dc8ab8d`).
> Tài liệu này **bổ sung** cho `08_References/ManualTestFlow.md` (vốn chỉ test Mobile/role MOTHER) — mở rộng ra toàn bộ 7 role và cả Web App.

---

## 1. Vì sao tài liệu này khác các UC trong SRS

SRS mô tả 241 use case như hệ thống *nên* làm gì. Sau khi đối chiếu với code thật, nhiều tính năng **có UI nhưng chưa có backend**, hoặc **có backend nhưng chưa có UI**, hoặc **có cả hai nhưng bị chặn bởi lỗi phân quyền**. Test tay theo đúng UC sẽ liên tục gặp lỗi 403/404 không phải do bug tính năng mà do tính năng chưa xong. Vì vậy mỗi mục bên dưới có **verdict khả năng test**:

| Verdict                         | Ý nghĩa                                                                                         |
| ------------------------------- | ------------------------------------------------------------------------------------------------- |
| 🟢**Test được**        | Có UI thật + API thật hoạt động, kỳ vọng end-to-end                                       |
| 🟡**UI-only (mock data)** | Màn hình hoàn chỉnh nhưng dữ liệu giả lập/chưa nối API — chỉ test được giao diện |
| 🟠**Backend-only**        | API hoạt động (test bằng Postman/curl) nhưng chưa có màn hình để bấm tới             |
| 🔴**Bị chặn (bug)**     | Có cả UI lẫn API nhưng gãy luồng do lỗi code (ví dụ role không khớp)                   |
| ⚫**Chưa triển khai**   | Chỉ có thư mục rỗng / bảng DB, chưa có code — đừng test                                |

---

## 2. Chuẩn bị môi trường

### 2.1 Khởi chạy

```bash
# Backend — http://localhost:8080
cd 05_Development/CareBridgeAPI
set -a && source .env && set +a && ./mvnw spring-boot:run

# Web — http://localhost:5173
cd 05_Development/CareBridgeWebApp
npm run dev

# Mobile (Chrome, dùng để test nhanh không cần emulator)
cd 05_Development/CareBridgeMobileApp
flutter run -d chrome
```

### 2.2 ⚠️ Việc cần kiểm tra TRƯỚC khi test (blocker thật sự gặp phải)

1. **Tài khoản seed có tồn tại không?** 7 tài khoản trong bảng dưới chỉ được tạo tự động bởi `DevDataSeeder` khi biến môi trường `CAREBRIDGE_DEV_SEED_ENABLED=true` có trong `.env`. Đã kiểm tra `.env` hiện tại của máy này: **biến này chưa được set** (mặc định `false`). → Cần thêm dòng `CAREBRIDGE_DEV_SEED_ENABLED=true` vào `.env` trước khi khởi động backend lần đầu, nếu không cả 7 tài khoản sẽ không tồn tại và toàn bộ hướng dẫn dưới đây không đăng nhập được.
2. **`SPRING_PROFILES_ACTIVE`**: `.env` hiện đang set `supabase` — profile này khớp với `MockEmailService` (mail giả lập ghi log console thay vì gửi Gmail thật), nên OTP sẽ **hiện trong log console backend**, không cần hộp thư thật.
3. **Cách vượt qua OTP khác nhau giữa Web và Mobile**:
   - **Mobile**: `LoginScreen` gọi thẳng `POST /api/v1/auth/login-direct` — **không có bước OTP khi đăng nhập** (chỉ có OTP khi đăng ký tài khoản mới). Đăng nhập xong vào thẳng Trang chủ.
   - **Web**: `LoginPage` gọi `POST /api/v1/auth/login` (gửi OTP thật), sau đó chuyển sang `/login/otp` để nhập mã. Phải mở terminal đang chạy backend, tìm dòng log dạng:
     `[MOCK EMAIL] To: admin@carebridge.dev, OTP: 123456, Expires in: 5 minutes`
   - Muốn test API thuần (Postman/curl) mà không qua OTP: dùng `POST /api/v1/auth/login-direct` với body `{"email":"...","password":"Test@1234"}`.

### 2.3 Tài khoản test (password chung: `Test@1234`)

| Email                        | Role          | Nền tảng dùng được |
| ---------------------------- | ------------- | ------------------------ |
| `mother@carebridge.dev`    | MOTHER        | Mobile                   |
| `family@carebridge.dev`    | FAMILY        | Mobile                   |
| `expert@carebridge.dev`    | EXPERT        | Mobile + Web             |
| `moderator@carebridge.dev` | MODERATOR     | Web                      |
| `content@carebridge.dev`   | CONTENT_ADMIN | Web                      |
| `partner@carebridge.dev`   | PARTNER       | Web                      |
| `admin@carebridge.dev`     | SYSTEM_ADMIN  | Web                      |

**Phát hiện quan trọng**: Web App chặn cứng MOTHER/FAMILY — đăng nhập web bằng 2 role này sẽ bị đưa tới `/no-web-access` ngay lập tức (theo thiết kế, 2 role này chỉ dùng mobile). Ngược lại, Mobile App **không phân biệt giao diện theo role** — EXPERT và FAMILY đăng nhập mobile sẽ thấy y hệt giao diện MOTHER (5 tab: Trang chủ/Hành trình/Cộng đồng/Việc cần làm/Hồ sơ), chỉ khác 1 mục menu duy nhất ("Hàng đợi câu hỏi") xuất hiện thêm trong tab Hồ sơ nếu role là EXPERT.

---

## 3. Bảng tổng hợp "code đã làm đến đâu" theo module SRS (MF-01→21)

| Module SRS                                                     | Backend API                                                                                                               | Web UI                                                                                                                                                    | Mobile UI                                                                                                                                                      | Verdict                                                                                            |
| -------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| MF-01 Auth/Account/Consent                                     | Đầy đủ                                                                                                                | Đầy đủ (trang admin)                                                                                                                                  | Đầy đủ                                                                                                                                                     | 🟢 Test được                                                                                    |
| MF-02 Mother Journey                                           | Đầy đủ (`/journeys`)                                                                                                | — (MOTHER không có web)                                                                                                                                | Đầy đủ                                                                                                                                                     | 🟢 Test được (mobile)                                                                           |
| MF-02 Pregnancy Exercise                                       | Đầy đủ (`/exercises`)                                                                                               | —                                                                                                                                                        | UI đầy đủ nhưng**không có điểm vào** (thư viện bài tập chưa code)                                                                         | 🟠 Backend-only — test bằng Postman, không bấm được từ app                                 |
| MF-03 Baby Care & Growth                                       | Có, nhưng role check sai chính tả (`FAMILY_MEMBER` thay vì `FAMILY`)                                             | —                                                                                                                                                        | Có (mock data 1 phần)                                                                                                                                        | 🔴 FAMILY không tạo được hồ sơ bé dù dự định được phép                             |
| MF-04 Community Q&A                                            | Đầy đủ (6 controller), thiếu API "report"                                                                            | Chưa có trang`/community` nào trên web                                                                                                              | Có (CB-014), tab Cộng đồng hoạt động                                                                                                                    | 🟢 Test được (mobile); thiếu chức năng report                                                |
| MF-05/06/07 Expert Marketplace, tư vấn có phí, thanh toán | **0% code** — chỉ có bảng DB (`expert_profiles`, `consultation_bookings`, `payment_transactions`...)      | Dashboard Expert chỉ placeholder                                                                                                                         | Không có UI                                                                                                                                                  | ⚫ Chưa triển khai — bỏ qua toàn bộ khi test                                                 |
| MF-08 AI Triage                                                | Đầy đủ (`/triage/intake`)                                                                                           | —                                                                                                                                                        | UI orphan (`SymptomIntakeScreen` không ai push tới), nhưng `RagChatScreen` (`/rag-chat`) hoạt động qua nút "Hỏi AI" trong câu hỏi cộng đồng | 🟡 Chỉ test được qua RAG chat, không test được luồng triage đầy đủ trên UI           |
| MF-09 Personal Health Records                                  | Đầy đủ                                                                                                                | —                                                                                                                                                        | Có, phần lớn mock data                                                                                                                                      | 🟡 UI-only phần lớn                                                                              |
| MF-10 Reminders                                                | Đầy đủ                                                                                                                | —                                                                                                                                                        | Có                                                                                                                                                            | 🟢 Test được (mobile)                                                                           |
| MF-11 Family Sync                                              | Chỉ tạo nhóm + xem thành viên (thiếu mời/xoá thành viên dù có UI)                                             | —                                                                                                                                                        | Có,`MyCareGroupsScreen` (góc nhìn Family) là **orphan** không ai vào được                                                                     | 🟡 Test được phần tạo nhóm/xem member; mời thành viên không test được (chưa có API) |
| MF-12 Verified Content Hub                                     | Đầy đủ (CRUD, versioning)                                                                                             | Đầy đủ (Content Admin CMS) —**nhưng chỉ có API tạo (POST), không có API sửa (PUT/PATCH)** dù trang `ContentDetailPage` có form sửa | Có (`ViewContentScreen`)                                                                                                                                    | 🔴 Web "sửa nội dung" sẽ lỗi vì backend chưa có endpoint update                             |
| MF-13 Vaccination & Growth                                     | Đầy đủ (đọc lịch tiêm)                                                                                            | —                                                                                                                                                        | Có                                                                                                                                                            | 🟢 Test được (mobile, chỉ xem)                                                                 |
| MF-16 Dashboard/Impact Report                                  | Không thấy controller riêng                                                                                            | Dashboard admin/content/expert/partner đa số**placeholder**                                                                                       | —                                                                                                                                                             | ⚫/🟡 Đa số placeholder                                                                          |
| MF-17 Safety/Audit/Security                                    | Audit logs OK; Security Incident**đã fix RBAC** (`hasRole('ADMIN')` → `hasRole('SYSTEM_ADMIN')`, 2026-07-01) | Trang Security Events/Incidents đã code xong, gọi API                                                                                                  | —                                                                                                                                                             | 🟢 Test được — SYSTEM_ADMIN nay vượt qua được check quyền (đã fix bug#1)               |
| MF-17 Moderation                                               | Chỉ có`GET /queue`, thiếu API duyệt/gỡ/report                                                                      | Trang Moderator xem được queue/case                                                                                                                    | —                                                                                                                                                             | 🟡 Chỉ xem được, không thao tác duyệt được (API thao tác chưa có)                     |
| MF-18 Partner                                                  | Chỉ có`POST` tạo hồ sơ đối tác, không có GET/list dịch vụ                                                   | Landing + đăng ký công khai hoạt động; dashboard placeholder                                                                                       | —                                                                                                                                                             | 🟡 Chỉ test được đăng ký, chưa test được vận hành dịch vụ đối tác                |
| MF-19 Emergency Map & MF-21 Fall Detection                     | API tồn tại nhưng**adapter là stub** (không gửi FCM thật, consent vị trí luôn `false`)                  | —                                                                                                                                                        | UI orphan (không có điểm vào từ navigation)                                                                                                              | 🔴/⚫ API trả 200 nhưng không có hiệu ứng thật; không bấm được từ UI                  |
| MF-20 Connected Device                                         | Không thấy                                                                                                              | —                                                                                                                                                        | —                                                                                                                                                             | ⚫ Chưa triển khai                                                                               |

**Kết luận nhanh cho câu hỏi "code đã làm được đến đâu":** Nền tảng auth/account/consent, mother journey, community Q&A, reminders, exercise (backend), AI triage (backend), health record, content CMS (đọc), audit log đã chạy được thật. Toàn bộ trụ cột thương mại của sản phẩm — **đặt lịch tư vấn chuyên gia, thanh toán VNPay, video call ZegoCloud, bản đồ khẩn cấp TrackAsia** — chưa có dòng code backend nào (chỉ có thiết kế bảng DB), nên **không thể test tay** ở giai đoạn hiện tại dù SRS mô tả rất chi tiết (UC-75 đến UC-97, UC-133 đến UC-155...).

---

## 4. Danh sách lỗi/blocker nên báo cho team dev trước khi QA diện rộng

| # | Vấn đề                                                                                                                                                                                                      | File                                                           | Ảnh hưởng                                                                                                                                                                            |
| - | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 | ✅**ĐÃ FIX (2026-07-01)** — `SecurityIncidentController` dùng `@PreAuthorize("hasRole('ADMIN')")` nhưng enum `Role` chỉ có `SYSTEM_ADMIN` → đã sửa thành `hasRole('SYSTEM_ADMIN')` | `audit/controller/SecurityIncidentController.java`           | Trước fix: toàn bộ`/api/v1/admin/security-events/**` trả 403 với mọi tài khoản thật. Sau fix: SYSTEM_ADMIN truy cập được — cần build lại backend và test xác nhận |
| 2 | ✅**ĐÃ FIX (2026-07-01)** — `BabyController POST` dùng `hasAnyRole('MOTHER','FAMILY_MEMBER')` → đã sửa thành `hasAnyRole('MOTHER','FAMILY')`                                              | `baby/controller/BabyController.java`                        | FAMILY nay tạo được hồ sơ bé — cần build lại backend và test lại để xác nhận trả 201 thay vì 403                                                                      |
| 3 | ✅**ĐÃ FIX (2026-07-01)** — `ExerciseController`/`ExerciseSessionController` dùng `hasAnyRole('MOTHER','ADMIN','SYSTEM')` → `ADMIN` đã sửa thành `SYSTEM_ADMIN`                       | `exercise/controller/*.java`                                 | Nhánh`SYSTEM` vẫn còn là role ảo (vô hại, chỉ là OR-condition chết) — không thuộc phạm vi fix lần này                                                                 |
| 4 | ✅**ĐÃ FIX (2026-07-01)** — `NotificationController POST /send` dùng `hasAnyRole('ADMIN','SYSTEM')` → `ADMIN` đã sửa thành `SYSTEM_ADMIN`                                               | `notification/controller/NotificationController.java`        | SYSTEM_ADMIN nay gọi được endpoint gửi thông báo chủ động; nhánh`SYSTEM` vẫn còn dead code (vô hại)                                                                    |
| 5 | `AdminContentController` chỉ có `POST`, không có `PUT/PATCH`                                                                                                                                         | `content/controller/AdminContentController.java`             | Web`ContentDetailPage` có form "sửa nội dung" nhưng sẽ lỗi khi submit vì thiếu API                                                                                            |
| 6 | `ModerationController` chỉ có `GET /queue`                                                                                                                                                               | `content/controller/ModerationController.java`               | Moderator xem được hàng đợi nhưng không duyệt/gỡ được; cũng không có API để user tạo report                                                                          |
| 7 | Emergency/Safety adapter (`FamilyMemberPortAdapter`, `FcmNotificationPortAdapter`, `*LocationConsentPortAdapter`) là stub trả về rỗng/`false`                                                      | `emergency/adapter/*.java`, `safety/adapter/*Adapter.java` | API trả 200 nhưng không gửi push thật, luôn coi như chưa có consent vị trí                                                                                                   |
| 8 | Web sidebar có link chết`/admin/topics` (route không tồn tại)                                                                                                                                           | `AdminLayout.tsx` (web)                                      | Bấm vào từ menu sẽ rơi về`/login`                                                                                                                                               |
| 9 | `AccountProfilePage` (web) đã code xong nhưng không được đăng ký route nào                                                                                                                        | `features/auth/pages/AccountProfilePage.tsx`                 | Không có URL nào truy cập được trang hồ sơ tài khoản trên web                                                                                                               |

---

## 5. Hướng dẫn test theo role

### 5.1 MOTHER — Mobile (`flutter run -d chrome` hoặc emulator)

Dùng trực tiếp `08_References/ManualTestFlow.md` (Quy trình 1–15) làm kịch bản chính — tài liệu đó đã liệt kê từng bước chi tiết cho MOTHER và đã được QA khác chạy một phần (cột "Đạt?"). Bổ sung/lưu ý khi chạy lại:

- **Tab Cộng đồng (tab 2) đã hoàn thiện** (`CommunityFeedScreen`, CB-014) — xác nhận trực tiếp trong `home_shell.dart`: tab 2 gắn với `CommunityFeedScreen()`, chỉ **tab 3 "Việc cần làm"** gắn với `_PlaceholderTab`. Mục 13.3 trong `ManualTestFlow.md` (ghi ngày 2026-06-28, nói tab Cộng đồng là "đang xây dựng") **đã lỗi thời** — Community đã được implement ở các commit sau đó (`07274c23`, `35274d47` seed community topics). Khi chạy lại Quy trình 13 trong ManualTestFlow.md, bỏ qua kết quả mong đợi cũ ở bước 13.3 và thay bằng: tab Cộng đồng phải hiển thị `CommunityFeedScreen` với danh sách câu hỏi thật.
- Các màn hình sau **không thể bấm tới được từ luồng thao tác tay bình thường** dù đã có code UI — bỏ qua, đừng report là bug thiếu màn hình: `ExerciseSessionScreen` và toàn bộ luồng bài tập thai kỳ, `EmergencyAlertDetailScreen`, `SymptomIntakeScreen`, `NotificationsScreen` (bản trùng cũ), `NotificationPreferencesScreen`, `DeactivateAccountScreen`, `DeleteAccountSheet`.
- Route dạng `/journey-update`, `/health-records/add`, `/health-records/detail/:id`, `/care-groups/add`, `/reminders/add` chỉ hiện chữ tĩnh — xác nhận đang chờ implement, không phải lỗi.
- 🔴 Test riêng: mời thành viên vào Care Group — nút "Tạo nhóm mới"/mời có UI nhưng backend không có endpoint invite, kỳ vọng sẽ lỗi hoặc không có phản hồi thật.

### 5.2 FAMILY — Mobile

Đăng nhập bằng `family@carebridge.dev`. Giao diện **giống hệt MOTHER** (không có Family Home riêng dù thiết kế UI/UX có mô tả CB-025).

Test trọng tâm (khác MOTHER):

1. 🟢 Vào tab Hồ sơ bé → nhấn "Thêm hồ sơ bé" → bug #2 **đã fix** (2026-07-01) — kỳ vọng tạo thành công (201), không còn 403. Cần build lại backend trước khi test lại.
2. 🟡 Vào `/care-groups` (qua đường dẫn được share từ Mother, hoặc kiểm tra nếu Family tự thấy nhóm mình tham gia) — xác minh Family chỉ thấy dữ liệu nhóm mà Mother đã mời (không tự tạo nhóm được, đúng thiết kế — chỉ MOTHER có `POST /care-groups`).
3. 🟢 Test chung các luồng account/auth/community/notification giống MOTHER (không phân role ở tầng này).

### 5.3 EXPERT — Mobile + Web

**Mobile** (`expert@carebridge.dev`): đăng nhập → giao diện giống MOTHER, riêng tab Hồ sơ có thêm mục **"Hàng đợi câu hỏi (Expert)"** → mở `ExpertQuestionQueueScreen`. Test: xem danh sách câu hỏi cần chuyên gia trả lời (đọc từ `GET` community endpoint có filter chuyên khoa), thử trả lời 1 câu hỏi (`POST /api/v1/community/questions/{id}/answers`) — kỳ vọng câu trả lời hiện badge Expert khi Mother xem lại.

**Web** (`http://localhost:5173/login` → sau login vào `/expert/dashboard`):

- 🟡 `/expert/dashboard` — **placeholder** ("🚧 Chưa được implement... CB-054"), chỉ xác nhận trang load không crash, không có nội dung để test sâu.
- Không có trang Web nào khác dành riêng cho Expert (marketplace/lịch tư vấn/thanh toán chưa code — mục 3).

### 5.4 MODERATOR — Web (`moderator@carebridge.dev`)

Sau đăng nhập (qua `/login` → nhập OTP đọc từ console log) → vào thẳng `/moderator/safety-cases`.

| Bước | Thao tác                                                                                                   | Kết quả mong đợi                                                               | Verdict                                                                                                                                                     |
| ------ | ----------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1      | Vào`/moderator/dashboard` hoặc `/moderator/safety-cases`                                              | Danh sách case kiểm duyệt bị escalate (`GET /api/v1/admin/moderation/queue`) | 🟢                                                                                                                                                          |
| 2      | Bấm vào 1 case →`/moderator/safety-cases/:caseId`                                                      | Xem chi tiết case                                                                 | 🟡 Trang có UI xử lý resolution nhưng backend cho chi tiết/resolution chưa có — chỉ xem được danh sách chung, thao tác resolve dự kiến lỗi |
| 3      | Kiểm tra menu sidebar mục "Topics"                                                                        | Bấm vào`/admin/topics`                                                         | 🔴 Route chết → bị đá về`/login` (bug #8)                                                                                                           |
| 4      | Xác nhận KHÔNG vào được`/content/*`, `/admin/*` (trừ moderation), `/expert/*`, `/partner/*` | Bị redirect`/forbidden`                                                         | 🟢 Test RBAC — đúng thiết kế                                                                                                                           |

### 5.5 CONTENT_ADMIN — Web (`content@carebridge.dev`)

Sau đăng nhập → vào `/content/dashboard`.

| Bước | Thao tác                                      | Kết quả mong đợi                      | Verdict                                                                                                                                            |
| ------ | ---------------------------------------------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1      | `/content/dashboard`                         | Trang tổng quan CMS hiển thị dữ liệu | 🟢                                                                                                                                                 |
| 2      | `/content/list`                              | Danh sách nội dung, thử search/filter  | 🟢                                                                                                                                                 |
| 3      | `/content/faq`, `/content/checklists`      | Quản lý FAQ/checklist                   | 🟢                                                                                                                                                 |
| 4      | `/content/topics`                            | CRUD chủ đề — test tạo mới          | 🟢 (một số phần: cây cha-con, đếm content chưa nối, coi là giới hạn biết trước)                                                      |
| 5      | Vào`/content/:id` → sửa nội dung → Lưu | Kỳ vọng theo UI sẽ cập nhật          | 🔴**Sẽ lỗi** — backend chỉ có `POST` tạo mới, không có API sửa (bug #5). Đây là bug cần báo, không phải do thao tác sai. |
| 6      | Tạo nội dung mới từ`/content/list`       | `POST /api/v1/admin/content`            | 🟢                                                                                                                                                 |

### 5.6 PARTNER — Web

Phần đăng ký **không cần đăng nhập**: `/partner` (landing) → `/partner/register` (form đăng ký). Test luồng công khai này trước bằng trình duyệt ẩn danh.

Sau khi có tài khoản `partner@carebridge.dev` và đăng nhập → vào `/partner/dashboard`:

- 🟡 Đây là **placeholder dùng tạm component của Admin** (chưa có UI riêng cho Partner) — chỉ xác nhận trang load, không có nội dung nghiệp vụ để test.
- Backend chỉ có `POST /api/v1/partner/profile` (tạo hồ sơ) — không có GET để xem lại, không có API submit dịch vụ/sponsored content dù SRS mô tả (UC-118–125). Không test được các UC này.

### 5.7 SYSTEM_ADMIN — Web (`admin@carebridge.dev`)

Sau đăng nhập → vào `/admin/dashboard`.

| Bước | Thao tác                                                                                                                                             | Kết quả mong đợi                                       | Verdict                                                                                                                                                                                                                                |
| ------ | ----------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1      | `/admin/dashboard`                                                                                                                                  | Trang chủ admin                                           | 🟡 Placeholder ("CB-083"), chỉ xác nhận không crash                                                                                                                                                                                |
| 2      | `/security/events`, `/security/incidents`, `/security/events/:id`, `/security/incidents/:id/investigate`, `/security/incidents/:id/resolve` | Xem/điều tra sự cố bảo mật                           | 🟢 Bug#1 **đã fix** (2026-07-01) — cần build lại backend rồi test lại để xác nhận trả 200 thay vì 403                                                                                                               |
| 3      | `/notifications`                                                                                                                                    | Trung tâm thông báo admin                               | 🟢 (trừ nút "đánh dấu tất cả đã đọc" chưa nối API)                                                                                                                                                                        |
| 4      | `/settings/privacy`                                                                                                                                 | Cấu hình quyền riêng tư + xem/thu hồi consent grants | 🟢                                                                                                                                                                                                                                     |
| 5      | Thử vào`/expert/dashboard` hoặc `/partner/dashboard` từ URL trực tiếp dù là SYSTEM_ADMIN                                                  | Kỳ vọng bị`/forbidden`                                | 🟢 Test RBAC — đây là**ngoại lệ có chủ đích**: SYSTEM_ADMIN được vào `/content/*` và `/moderator/*` cùng role sở hữu, nhưng KHÔNG được vào `/expert/*` và `/partner/*` (khác các nhóm khác) |
| 6      | Audit log (nếu có UI) hoặc test qua API`GET /api/v1/admin/audit-logs` bằng token SYSTEM_ADMIN                                                   | Trả 200 + danh sách log                                  | 🟢 Backend hoạt động đúng (role check ở đây là`SYSTEM_ADMIN` chính xác, khác bug #1)                                                                                                                                     |

---

## 6. Test API thuần (không qua UI) cho các domain chỉ có backend

Dùng Postman/curl, lấy token qua `login-direct` trước:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login-direct \
  -H "Content-Type: application/json" \
  -d '{"email":"mother@carebridge.dev","password":"Test@1234"}'
```

Copy `accessToken`, gắn header `Authorization: Bearer <token>` cho các endpoint sau (chưa có UI để bấm nhưng backend chạy được):

- `POST /api/v1/exercises/{exerciseId}/safety-check`, `POST /api/v1/exercises/{exerciseId}/sessions` — luồng bài tập thai kỳ.
- `POST /api/v1/triage/intake` — AI risk triage.
- `POST /api/v1/safety/fall-detection/enable`, `POST /api/v1/safety/imu-data` — phát hiện té ngã (nhớ: hiệu ứng gửi cảnh báo thật không hoạt động, chỉ kiểm tra API trả 200 + dữ liệu lưu DB).
- `POST /api/v1/emergency/sessions` — mở phiên khẩn cấp (tương tự, không có push FCM thật).

---

## 7. Ghi chú chung

- Sau khi test đổi mật khẩu (mục 10.6 trong ManualTestFlow.md), nhớ đổi lại `Test@1234` hoặc dùng tài khoản seed khác — vì `DevDataSeeder` chỉ tự reset mật khẩu khi backend **khởi động lại**, không reset theo thời gian thực.
- Tài liệu này phản ánh trạng thái code tại thời điểm khảo sát (nhánh `dev`, commit `2dc8ab8d`) — nếu team dev đã fix các bug ở mục 4 hoặc thêm route/API mới, cần cập nhật lại verdict tương ứng.
