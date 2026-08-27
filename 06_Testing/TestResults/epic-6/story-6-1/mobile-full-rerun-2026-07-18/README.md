# MF-01 Story 6.1 — Full Mobile Manual Rerun

## Tổng quan

- Ngày chạy: `2026-07-18`
- Thiết bị: Samsung SM-N986N, Android 13
- Package: `com.carebridge.app`
- Backend: local Spring Boot qua `adb reverse tcp:8080 tcp:8080`
- Database: PostgreSQL disposable `carebridge_story61`
- Kết quả: **9 ĐẠT / 4 MỘT PHẦN / 3 KHÔNG ĐẠT / 0 BỊ CHẶN**
- Quality gate: **KHÔNG ĐẠT**

Không có token, mật khẩu hoặc dữ liệu người dùng thật trong evidence. Các log refresh chỉ giữ trạng thái HTTP và đã loại bỏ nội dung token.

> Historical record: this report preserves the pre-fix result. The seven non-pass cases were fixed and rerun successfully. Final closure evidence is in `../mobile-gap-fix-rerun-2026-07-18/README.md` (`7/7 PASS`, composite `16/16 PASS`).

## Kết quả 16 ca

| ID | Kết quả | Quan sát chính | Evidence tiêu biểu |
| --- | --- | --- | --- |
| MF01-MOB-001 | ĐẠT | Cả Mother mới và Mother đã có role nhưng chưa có journey đều vào màn chọn giai đoạn; chưa chọn thì nút tiếp tục bị khóa. | `001-newa-stage.*`, `001-newa-existing-routing.*`, `001-empty-routing.*` |
| MF01-MOB-002 | KHÔNG ĐẠT | Toàn bộ card chạm được và CTA đổi đúng, nhưng hierarchy vẫn ghi `selected="false"` cho card đã chọn; TalkBack không nhận được selected state. | `002-select-planning.*`, `002-select-pregnancy.*`, `002-select-baby.*`, `002-talkback.*` |
| MF01-MOB-003 | ĐẠT | Bấm tạo PRE liên tiếp vẫn chỉ có một `PRE_PREGNANCY ACTIVE`, version 0 và một CREATED event. | `003-pre-created.*`, `003-pre-db.txt` |
| MF01-MOB-004 | ĐẠT | Tab Hành trình hiển thị rõ `Chuẩn bị mang thai`, CTA chuyển thai kỳ và lịch sử CREATED. | `004-pre-journey.*` |
| MF01-MOB-005 | ĐẠT | Đã chạy đủ LMP, conception, gestational age và clinician EDD; LMP 01/07/2026 → 07/04/2027, conception 01/07/2026 → 24/03/2027; cycle 28/29 ngày và `KHÔNG BIẾT` hoạt động. | `005-*-result.*`, `005-lmp-cycle-28.*`, `005-lmp-cycle-29.*` |
| MF01-MOB-006 | MỘT PHẦN | Tạo được PREGNANCY và provenance clinician đúng, nhưng payload/DB còn giữ `lastMenstrualDate=2026-07-01` từ lượt LMP trước dù lượt tạo cuối dùng clinician EDD. | `006-preg-created.*`, `006-preg-db.txt` |
| MF01-MOB-007 | ĐẠT | Nhánh nuôi bé mở form `Thêm hồ sơ bé`; không tạo `BABY_CARE` canonical và chưa tạo baby profile trước khi lưu. | `007-baby-selected.*`, `007-baby-route.*`, `007-baby-db.txt` |
| MF01-MOB-008 | MỘT PHẦN | PRE được transition tại chỗ thành PREGNANCY, chỉ một current row và hai transition; tuy nhiên màn Hành trình vẫn hiển thị PRE cho đến khi relaunch. | `008-transitioned.*`, `008-relaunch.xml`, `008-transition-db.txt` |
| MF01-MOB-009 | ĐẠT | Fixture PREG vào Home, dashboard tuần/tam cá nguyệt/EDD đúng canonical active. | `009-preg-dashboard.*` |
| MF01-MOB-010 | MỘT PHẦN | DB cập nhật EDD 08/01 → 12/01, version 1 → 2 và thêm đúng DATES_CHANGED; dashboard ngay sau lưu vẫn hiển thị 08/01, relaunch mới hiển thị 12/01. | `010-after-update.*`, `010-after-relaunch.*`, `010-update-db.txt` |
| MF01-MOB-011 | ĐẠT | Có mục `Lịch sử hành trình`, mới nhất trước; hiển thị CREATED/DATES_CHANGED và provenance thân thiện, không lộ raw JSON/ID/token. | `011-history.*`, `010-update-db.txt` |
| MF01-MOB-012 | ĐẠT | Gỡ adb reverse tạo lỗi kết nối rõ ràng, không xóa session; khôi phục và chạm nhanh ba lần vẫn chỉ có một PRE và một CREATED event. | `012-network-error.*`, `012-retry-success.*`, `012-retry-db.txt` |
| MF01-MOB-013 | ĐẠT | Access token bị loại khỏi bản sao encrypted storage nhưng refresh còn hợp lệ: refresh đúng một lần, HTTP 200 và xoay token. Session bị revoke: refresh HTTP 401, app xóa session và về welcome, không chồng dialog. | `013-valid-refresh*`, `013-invalid-refresh*` |
| MF01-MOB-014 | KHÔNG ĐẠT — P0 | Sau logout `MOTHER_PREG` rồi login `MOTHER_OTHER`, Home và Hành trình vẫn hiển thị tuần thai/EDD 12/01/2027 của tài khoản trước. DB của tài khoản sau có 0 journey; relaunch mới xóa cache. | `014-preg-before-switch.*`, `014-other-immediate.*`, `014-other-journey.*`, `014-other-relaunch.*`, `014-other-db.txt` |
| MF01-MOB-015 | KHÔNG ĐẠT | Back icon đã có nhãn và target 48dp, nhưng selected state chưa có semantics. Ở font 150% + landscape không thể cuộn tới đủ phương pháp; nút cố định che nội dung. Ở result portrait, disclaimer nằm dưới nút tạo và không thể cuộn tới. | `015-wizard-step1-landscape*`, `015-result-font150*`, `015-result-landscape.*` |
| MF01-MOB-016 | MỘT PHẦN | Resume 30 giây giữa wizard và result giữ nguyên dữ liệu. Background ngay sau create tạo đúng một PRE trong DB, nhưng khi resume Home vẫn báo chưa setup; force-stop/relaunch mới hiển thị PRE. | `016-wizard-resume.*`, `016-result-resume.*`, `016-pre-resume.*`, `016-pre-after-relaunch.*`, `016-pre-resume-db.txt` |

## Gap tái hiện

| Gap | Mức độ | Ảnh hưởng |
| --- | --- | --- |
| MF01-RERUN-GAP-01 — selected semantics không được cập nhật | P1 | TalkBack không thông báo card đang chọn; trạng thái dựa nhiều vào màu/CTA. |
| MF01-RERUN-GAP-02 — cache journey dùng chéo tài khoản | **P0 / Security** | Người dùng sau nhìn thấy stage, tuần thai và EDD của người dùng trước cho đến khi relaunch. |
| MF01-RERUN-GAP-03 — current journey không refresh sau mutation/resume | P1 | Create, PRE→PREG và update EDD đã thành công ở DB nhưng UI tiếp tục hiển thị dữ liệu cũ. |
| MF01-RERUN-GAP-04 — wizard rò state giữa phương pháp | P1 | Tạo bằng clinician EDD vẫn gửi/lưu LMP của lượt tính trước, làm dữ liệu nguồn không nhất quán. |
| MF01-RERUN-GAP-05 — font 150%/landscape bị che và mất control | P1 | Không truy cập được đủ phương pháp; disclaimer kết quả bị nút cố định che và không cuộn được. |

## Đối soát invariant cuối lượt

`final-db-invariants.txt` xác nhận:

- không có owner nào có hơn một canonical lifecycle `ACTIVE`;
- không có canonical lifecycle loại `BABY_CARE`;
- các tài khoản create/retry chỉ có đúng số transition dự kiến.

Thiết bị đã được khôi phục `font_scale=1.0`, portrait/auto-rotate và giữ `adb reverse tcp:8080`.

`003-pre-relaunch.*` là evidence của lượt thử bị gián đoạn bởi TalkBack tutorial và **không được dùng để kết luận**; kết quả chính thức của MF01-MOB-003 dùng `003-pre-created.*` và `003-pre-db.txt`.
