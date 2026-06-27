# TV1 (Phương) — Screen Build Tracking: Sprint 0 & Sprint 1

**Cập nhật lần cuối:** 2026-06-27 (CB-001, CB-002, CB-003, CB-004 — Done)  
**Phạm vi:** Sprint 0 + Sprint 1  
**Người phụ trách:** TV1 — Phương  

---

## Trạng thái

| Ký hiệu | Ý nghĩa |
| --- | --- |
| ⬜ Not Started | Chưa bắt đầu |
| 🔄 In Progress | Đang làm |
| ✅ Done | Hoàn thành |
| ⚠️ Blocked | Bị chặn / cần làm rõ |

---

## Mobile App — Sprint 0 (12 màn hình)

| # | Screen ID | Tên màn hình | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **CB-001** | Mobile Welcome | UC-01, UC-03 | 3.1.1.1 / 3.1.1.3 | ✅ Done | `welcome_screen.dart` — cần update `main.dart` để dùng WelcomeScreen thay LoginScreen |
| 2 | **CB-002** | Register Account | UC-01 | 3.1.1.1 | ✅ Done | `register_screen.dart` — name field collects UX only; profile update qua CB-111 |
| 3 | **CB-003** | Verify OTP | UC-02 | 3.1.1.2 | ✅ Done | `otp_verification_screen.dart` — dùng chung cho register & login |
| 4 | **CB-004** | Login | UC-03 | 3.1.1.3 | ✅ Done | `login_screen.dart` |
| 5 | **CB-108** | Notifications | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ⬜ Not Started | Danh sách thông báo; đánh dấu đã đọc |
| 6 | **CB-109** | Notification Detail | UC-11 | 3.1.5.1–3.1.5.4 | ⬜ Not Started | Chi tiết 1 thông báo; CB-109_2 là bản thiết kế thay thế |
| 7 | **CB-114** | Login Sessions | UC-16 | 3.1.1.16 | ⬜ Not Started | Danh sách các phiên đăng nhập đang hoạt động |
| 8 | **CB-115** | Revoke Session Confirmation | UC-16 | 3.1.1.16 | ⬜ Not Started | Xác nhận thu hồi 1 phiên cụ thể |
| 9 | **CB-116** | Logout Confirmation | UC-04 | 3.1.1.4 | ⬜ Not Started | Xác nhận đăng xuất; CB-116_2 đến _6 là các variant thiết kế |
| 10 | **CB-133** | Emergency Alert Detail | UC-161 | 3.1.5.4 | ⬜ Not Started | Chi tiết cảnh báo khẩn cấp nhận được từ notification |
| 11 | **CB-219** | Notification Center | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ⬜ Not Started | Trung tâm thông báo; bản thiết kế mới hơn CB-049 |
| 12 | **CB-220** | Privacy Settings | UC-17, UC-18, UC-19, UC-157 | 3.1.4.1 | ⬜ Not Started | Cài đặt quyền riêng tư; bản thiết kế mới hơn CB-050 |

---

## Mobile App — Sprint 1 (6 màn hình)

| # | Screen ID | Tên màn hình | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 13 | **CB-005** | Forgot Password | UC-05 | 3.1.1.5 | ⬜ Not Started | Nhập email/phone để nhận link đặt lại mật khẩu |
| 14 | **CB-006** | Reset Password | UC-06 | 3.1.1.6 | ⬜ Not Started | Nhập mật khẩu mới bằng reset token |
| 15 | **CB-110** | Account Profile | UC-08, UC-20 | 3.1.1.8 | ⬜ Not Started | Xem thông tin tài khoản hiện tại |
| 16 | **CB-111** | Edit Account Profile | UC-09, UC-21 | 3.1.1.9 | ⬜ Not Started | Chỉnh sửa tên, số điện thoại, avatar |
| 17 | **CB-112** | Change Password | UC-07 | 3.1.1.7 | ⬜ Not Started | Đổi mật khẩu khi đã đăng nhập |
| 18 | **CB-152** ⚠️ | Pre-exercise Safety Check | **UC-177** (+ UC-178, UC-179) | 3.3.2.3 | ⬜ Not Started | Sprint 1: chỉ implement phần UC-177 (xem exercise detail). Phần safety check UC-178, UC-179 để Sprint 2 |

---

## Mobile App — Design Gap ⚠️

| # | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- |
| 19 | **UC-29** | 3.3.2.1 View and Select Pregnancy Exercise | ⚠️ Blocked | **Không có CB screen nào được thiết kế cho mobile.** Cần tạo màn hình exercise catalog tùy chỉnh hoặc yêu cầu designer bổ sung. Tham khảo pattern từ CB-018 (Expert Directory) hoặc CB-180 (Verified Content). API backend `/api/v1/exercises` đã sẵn sàng. |

---

## Web App — Sprint 0 (7 màn hình)

| # | Screen ID | Tên màn hình | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | **CB-142** | Security Events | UC-175 | 3.2.5.2 | ⬜ Not Started | Danh sách security events để admin review |
| 21 | **CB-143** | Security Event Detail | UC-175 | 3.2.5.2 | ⬜ Not Started | Chi tiết 1 security event |
| 22 | **CB-144** | Security Incident Investigation | UC-174 | 3.2.5.1 | ⬜ Not Started | Màn hình điều tra sự cố bảo mật |
| 23 | **CB-145** | Security Incident Resolution | UC-174 | 3.2.5.1 | ⬜ Not Started | Ghi nhận ghi chú và đánh dấu giải quyết sự cố |
| 24 | **CB-151** | Security Incident List | UC-174 | 3.2.5.1 | ⬜ Not Started | Danh sách tất cả sự cố bảo mật |
| 25 | **CB-219** | Notification Center (web) | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ⬜ Not Started | Trung tâm thông báo cho user web (admin/expert) |
| 26 | **CB-220** | Privacy Settings (web) | UC-17, UC-18, UC-19, UC-157 | 3.1.4.1 | ⬜ Not Started | Cài đặt quyền riêng tư cho user web |

---

## Web App — Sprint 1

Không có web screen nào thêm cho TV1 Sprint 1.  
Role MOTHER dùng mobile; exercise admin web (CB-189–CB-193, UC-185) thuộc **Sprint 3** (`3.2.6.1 Manage Pregnancy Exercises`).

---

## Đã hoàn thành trước (không cần build lại)

| Screen ID | Tên màn hình | Platform | Trạng thái |
| --- | --- | --- | --- |
| CB-053 | Web Login | Web | ✅ Done — `LoginPage.tsx` |
| CB-098 | Web Verify OTP | Web | ✅ Done — `OtpPage.tsx` |
| — | Blocked Account | Web | ✅ Done — `BlockedAccountPage.tsx` |
| — | No Web Access | Web | ✅ Done — `NoWebAccessPage.tsx` |
| — | Auth State (core) | Mobile | ✅ Done — `auth_state.dart` |
| — | API Client (core) | Mobile | ✅ Done — `api_client.dart` |
| — | Token Storage (core) | Mobile | ✅ Done — `token_storage.dart` |
| — | Account Block Parser | Mobile | ✅ Done — `account_block_parser.dart` |

---

## Tổng kết số lượng

| Layer | Sprint 0 | Sprint 1 | Tổng cần build |
| --- | --- | --- | --- |
| Mobile | 12 màn hình | 6 màn hình | **18 màn hình** |
| Web | 7 màn hình | 0 màn hình | **7 màn hình** |
| Design gap (custom) | 1 | — | **1 màn hình** |
| **Tổng** | **20** | **6** | **26 màn hình** |

---

## Thứ tự ưu tiên build

### Mobile (ưu tiên cao → thấp)

1. **CB-001, CB-004, CB-003** — Welcome + Login + OTP (luồng cốt lõi, các thành viên TV2–TV5 phụ thuộc)
2. **CB-002** — Register (cần song song với Login)
3. **CB-110, CB-111** — Account Profile + Edit Profile (Sprint 1)
4. **CB-005, CB-006, CB-112** — Forgot/Reset/Change Password (Sprint 1)
5. **CB-219, CB-108, CB-109** — Notification Center + List + Detail (Sprint 0)
6. **CB-220** — Privacy Settings (Sprint 0)
7. **CB-114, CB-115, CB-116** — Session Management + Logout (Sprint 0)
8. **CB-133** — Emergency Alert Detail (Sprint 0)
9. **UC-29 custom** — Exercise Catalog (Sprint 1, design gap)
10. **CB-152 (UC-177 only)** — Exercise Detail (Sprint 1)

### Web (ưu tiên cao → thấp)

1. **CB-151, CB-144, CB-145** — Security Incident List + Investigation + Resolution (Sprint 0)
2. **CB-142, CB-143** — Security Events + Detail (Sprint 0)
3. **CB-219, CB-220** — Notification Center + Privacy Settings (Sprint 0)
