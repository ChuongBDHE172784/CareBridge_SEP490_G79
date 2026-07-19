# MF-01 / Story 6.1 — Mobile manual test rerun (local isolated)

## Phạm vi

Lần chạy này xử lý toàn bộ 12 ca từng bị chặn trong bảng tổng hợp của
`MF-01-Story-6.1-Mobile-Manual-Test-Guide.md`.

- Thiết bị: Samsung SM-N986N, Android 13.
- App: Flutter debug APK build từ current worktree.
- API: `http://127.0.0.1:8080` qua `adb reverse tcp:8080 tcp:8080`.
- Database: PostgreSQL 16 disposable local, cổng 5434.
- Migration: 86 Flyway migrations, gồm canonical mother lifecycle history.
- Test data: chỉ dùng tài khoản tổng hợp từ
  `06_Testing/TestData/mobile/MF-01-Story-6.1-Fixtures.sql`.
- Không lưu access token, refresh token hoặc credential thật trong evidence.

## Kết quả 12 ca đã gỡ chặn

| Case | Kết quả | Evidence chính |
| --- | --- | --- |
| MF01-MOB-001 | KHÔNG ĐẠT — MOB-GAP-07 | `03-new-a-role-selection.*`, `05-new-a-stage-selection.*`, `15-existing-empty-routing.*` |
| MF01-MOB-002 | MỘT PHẦN — ĐẠT | `05-new-a-stage-selection.*`, `06-stage-preg-selected.png`, `07-stage-baby-selected.png`, `08-stage-planning-selected.*` |
| MF01-MOB-003 | ĐẠT | `09-new-a-pre-created.*`; DB: 1 PRE_PREGNANCY ACTIVE, version 0, 1 CREATED |
| MF01-MOB-004 | KHÔNG ĐẠT — MOB-GAP-05 | `09-new-a-pre-created.*`, `10-new-a-pre-journey-tab.*` |
| MF01-MOB-006 | KHÔNG ĐẠT — MOB-GAP-01 | `16-new-c-preg-wizard-step1.xml`, `17-new-c-preg-create-result.*`; DB: 0/0 |
| MF01-MOB-007 | KHÔNG ĐẠT — MOB-GAP-03 | `18-new-b-baby-care-result.*`; DB: 0/0 |
| MF01-MOB-008 | KHÔNG ĐẠT | `11-new-a-add-second-start.xml` đến `14-new-a-second-create-result.*`; DB vẫn 1 active/1 transition |
| MF01-MOB-009 | ĐẠT | `19-preg-home.*`, `20-preg-journey-dashboard.*` |
| MF01-MOB-010 | KHÔNG ĐẠT — MOB-GAP-02 | `21-preg-edit-edd-dialog.xml` đến `24-preg-edit-edd-result.*`; DB không đổi |
| MF01-MOB-012 | ĐẠT | `25-new-d-pre-ready.xml`, `26-new-d-network-failure.*`, `27-new-d-network-retry-success.*` |
| MF01-MOB-013 | ĐẠT | `32-token-valid-refresh-result.*`, `33-token-invalid-refresh-result.*` |
| MF01-MOB-014 | ĐẠT | `28-logout-dialog.xml` đến `31-account-other-journey.*` |

MF01-MOB-002 chỉ được đánh giá một phần vì lần rerun xác nhận semantics hierarchy,
touch target và selected state nhưng không bật TalkBack trực tiếp. Phần accessibility
đã có defect riêng tại MF01-MOB-015 trong evidence run trước.

## Đối chiếu dữ liệu

- Double tap tạo PRE_PREGNANCY: đúng 1 lifecycle và 1 CREATED transition.
- Thử tạo lifecycle thứ hai: không phát sinh current/history mới.
- Tạo PREGNANCY thiếu provenance: không phát sinh current/history.
- Nhánh BABY_CARE: không phát sinh canonical lifecycle.
- Update EDD thiếu provenance: EDD/version/history giữ nguyên.
- Retry sau lỗi kết nối: đúng 1 lifecycle và 1 CREATED transition.
- Token TTL 2 giây: refresh hợp lệ giữ phiên; refresh bị revoke xóa phiên và đưa về login.

## Trạng thái môi trường sau test

- `adb reverse tcp:8080 tcp:8080` đã được khôi phục.
- Backend được khôi phục access-token TTL bình thường 15 phút.
- Font scale `1.0`, portrait và auto-rotate giữ trạng thái bình thường.
- App đang ở welcome/login sau ca invalid refresh; không còn phiên test đăng nhập.
