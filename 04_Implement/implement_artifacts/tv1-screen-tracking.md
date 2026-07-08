# TV1 (Phương) — Screen Build Tracking: Sprint 0 & Sprint 1

**Cập nhật lần cuối:** 2026-06-27 (All Sprint 0 + Sprint 1 screens — Done)  
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
| 5 | **CB-108** | Notifications | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ✅ Done | `notification/screens/notifications_screen.dart` — filter chips, pull-to-refresh, mark-all-as-read (TODO: backend chưa có endpoint) |
| 6 | **CB-109** | Notification Detail | UC-11 | 3.1.5.1–3.1.5.4 | ✅ Done | `notification/screens/notification_detail_screen.dart` — hiển thị chi tiết + CTA theo type; mark-as-read (TODO: backend chưa có endpoint) |
| 7 | **CB-114** | Login Sessions | UC-16 | 3.1.1.16 | ✅ Done | `session/screens/login_sessions_screen.dart` — current session highlighted, other sessions with revoke button |
| 8 | **CB-115** | Revoke Session Confirmation | UC-16 | 3.1.1.16 | ✅ Done | `session/screens/revoke_session_sheet.dart` — bottom sheet xác nhận, gọi `DELETE /api/v1/sessions/{id}` |
| 9 | **CB-116** | Logout Confirmation | UC-04 | 3.1.1.4 | ✅ Done | `CareBridgeMobileApp/lib/features/auth/screens/logout_confirmation_screen.dart`; xác nhận đăng xuất; repo hiện chỉ có variant `_3` khả dụng |
| 10 | **CB-133** | Emergency Alert Detail | UC-161 | 3.1.5.4 | ✅ Done | `emergency/screens/emergency_alert_detail_screen.dart` — MOCK data (backend chưa có endpoint); call + directions via url_launcher |
| 11 | **CB-219** | Notification Center | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ✅ Done | Mobile: `notification/screens/notification_center_screen.dart`; Web: `notification/pages/NotificationCenterPage.tsx` |
| 12 | **CB-220** | Privacy Settings | UC-17, UC-18, UC-19, UC-157 | 3.1.4.1 | ✅ Done | Mobile: `privacy/screens/privacy_settings_screen.dart`; Web: `settings/pages/PrivacySettingsPage.tsx` — wired to `/api/v1/privacy-settings` + `/api/v1/consent/grants` |

---

## Mobile App — Sprint 1 (6 màn hình)

| # | Screen ID | Tên màn hình | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 13 | **CB-005** | Forgot Password | UC-05 | 3.1.1.5 | ✅ Done | `auth/screens/forgot_password_screen.dart` — gọi `POST /api/v1/auth/forgot-password` |
| 14 | **CB-006** | Reset Password | UC-06 | 3.1.1.6 | ✅ Done | `auth/screens/reset_password_screen.dart` — gọi `POST /api/v1/auth/reset-password`, password requirements checklist |
| 15 | **CB-110** | Account Profile | UC-08, UC-20 | 3.1.1.8 | ✅ Done | `auth/screens/account_profile_screen.dart` — avatar, menu cards, navigates to edit/password/sessions/privacy/logout |
| 16 | **CB-111** | Edit Account Profile | UC-09, UC-21 | 3.1.1.9 | ✅ Done | `auth/screens/edit_profile_screen.dart` — form name/phone/region/bio, gọi `PUT /api/v1/auth/profile` |
| 17 | **CB-112** | Change Password | UC-07 | 3.1.1.7 | ✅ Done | `auth/screens/change_password_screen.dart` — current+new+confirm, gọi `PUT /api/v1/auth/change-password` |
| 18 | **CB-152** ⚠️ | Pre-exercise Safety Check | **UC-177** (+ UC-178, UC-179) | 3.3.2.3 | ✅ Done | `exercise/screens/pre_exercise_safety_check_screen.dart` — health checklist, notes, start button |

---

## Mobile App — Design Gap ⚠️

| # | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- |
| 19 | **UC-29** | 3.3.2.1 View and Select Pregnancy Exercise | ⚠️ Blocked | **Không có CB screen nào được thiết kế cho mobile.** Cần tạo màn hình exercise catalog tùy chỉnh hoặc yêu cầu designer bổ sung. Tham khảo pattern từ CB-018 (Expert Directory) hoặc CB-180 (Verified Content). API backend `/api/v1/exercises` đã sẵn sàng. |

---

## Web App — Sprint 0 (7 màn hình)

| # | Screen ID | Tên màn hình | UC | Function Spec | Trạng thái | Ghi chú |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | **CB-142** | Security Events | UC-175 | 3.2.5.2 | ✅ Done | `security/pages/SecurityEventsPage.tsx` — stats cards, severity filter chips, event log table |
| 21 | **CB-143** | Security Event Detail | UC-175 | 3.2.5.2 | ✅ Done | `security/pages/SecurityEventDetailPage.tsx` — severity badge, impact/risk, evidence timeline, attack flow |
| 22 | **CB-144** | Security Incident Investigation | UC-174 | 3.2.5.1 | ✅ Done | `security/pages/SecurityIncidentInvestigationPage.tsx` — evidence timeline, containment, notes, affected accounts |
| 23 | **CB-145** | Security Incident Resolution | UC-174 | 3.2.5.1 | ✅ Done | `security/pages/SecurityIncidentResolutionPage.tsx` — root cause, summary, remediation checklist, activity log |
| 24 | **CB-151** | Security Incident List | UC-174 | 3.2.5.1 | ✅ Done | `security/pages/SecurityIncidentListPage.tsx` — search, filters, table with SLA, pagination |
| 25 | **CB-219** | Notification Center (web) | UC-11, UC-12 | 3.1.5.1–3.1.5.4 | ✅ Done | `notification/pages/NotificationCenterPage.tsx` — table layout, filters, pagination |
| 26 | **CB-220** | Privacy Settings (web) | UC-17, UC-18, UC-19, UC-157 | 3.1.4.1 | ✅ Done | `settings/pages/PrivacySettingsPage.tsx` — split layout, consent table, save bar |

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
