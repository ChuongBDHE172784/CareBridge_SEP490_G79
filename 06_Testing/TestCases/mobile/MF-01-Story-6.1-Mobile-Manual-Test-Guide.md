# MF-01 / Story 6.1 — Hướng dẫn kiểm thử thủ công trên Mobile App

## Canonical Mother Lifecycle and Transition History

| Trường | Giá trị |
| --- | --- |
| Epic / Story | Epic 6 — Story 6.1 |
| Phạm vi | Tích hợp Flutter với canonical Mother lifecycle |
| Nền tảng | Android emulator hoặc thiết bị Android thật |
| Dữ liệu | Chỉ dùng dữ liệu tổng hợp |
| Phiên bản hướng dẫn | 1.0 |
| Ngày | 2026-07-18 |
| Trạng thái | Đã thực thi 16/16 ca; đợt closure rerun 7/7 ca từng không đạt/một phần đều PASS |

## 1. Mục tiêu

Tài liệu này hướng dẫn kiểm thử chức năng Story 6.1 trực tiếp trên CareBridge Mobile App:

`Đăng nhập → Chọn vai trò Mẹ → Chọn giai đoạn → Thiết lập hành trình → Trang chủ / Hành trình`

Các tiêu chí chính:

- chỉ tồn tại một canonical lifecycle đang hoạt động cho mỗi Mẹ;
- tạo hành trình chuẩn bị mang thai;
- thiết lập thai kỳ và ngày dự sinh;
- xử lý nhánh đang nuôi bé;
- hiển thị canonical lifecycle trên tab `Hành trình`;
- chỉnh sửa mốc thai kỳ;
- bảo vệ dữ liệu khi đổi tài khoản;
- loading, lỗi mạng, retry và accessibility;
- khả năng xem lịch sử chuyển đổi trên mobile.

Đây là hướng dẫn kiểm thử tích hợp. Backend Story 6.1 đã có API và automated tests, nhưng mobile hiện chưa tích hợp đầy đủ contract mới.

## 2. Các gap mobile đã biết trước khi test

Không đánh dấu các mục dưới đây là `ĐẠT` chỉ vì ứng dụng hiển thị đúng hành vi hiện tại. Nếu hành vi mục tiêu không đạt, ghi nhận `KHÔNG ĐẠT — GAP ĐÃ BIẾT`.

| Gap | Bằng chứng trong mobile hiện tại | Hệ quả dự kiến |
| --- | --- | --- |
| `MOB-GAP-01` | `CreateJourneyRequest` không gửi `dateSource` và `dateConfidence` | Tạo `PREGNANCY` có ngày thai kỳ bị backend từ chối bằng `JOURNEY-018` |
| `MOB-GAP-02` | `UpdateJourneyRequest` không gửi provenance | Chỉnh sửa ngày dự sinh bị backend từ chối bằng `JOURNEY-018` |
| `MOB-GAP-03` | Nhánh `Đang nuôi bé` gửi `journeyType: BABY_CARE` | Backend Story 6.1 trả `JOURNEY-016`; UI hiện diễn giải mọi lỗi 409 thành đã có hành trình |
| `MOB-GAP-04` | `JourneyService` không gọi `GET /api/v1/journeys/{id}/history` | Chưa có màn hình lịch sử chuyển đổi trên mobile |
| `MOB-GAP-05` | Tab Hành trình chỉ xem `PREGNANCY` là pregnancy dashboard | `PRE_PREGNANCY` đã tạo có thể bị hiển thị như chưa có hành trình mang thai |
| `MOB-GAP-06` | Mobile response models chưa chứa `version`, `dateSource`, `dateConfidence` | Không thể kiểm tra version/provenance đầy đủ chỉ từ UI |
| `MOB-GAP-07` | Router chuyển assigned Mother từ auth route thẳng về `/` trước khi `AuthLandingScreen` kiểm tra dashboard | Mẹ đã có role nhưng chưa có journey có thể không được đưa đến màn hình chọn giai đoạn sau đăng nhập |

## 3. Ghi nhận lần kiểm thử

| Trường | Người test điền |
| --- | --- |
| Người test | Codex qua ADB, có người dùng phê duyệt thao tác trực tiếp |
| Ngày test | 2026-07-18 |
| Commit / bản dựng | Repository `e07bc25f`; APK debug được build lại từ current worktree với `API_BASE_URL=http://127.0.0.1:8080` |
| Môi trường API | Spring Boot local cổng 8080 + PostgreSQL 16 disposable local cổng 5434; 86 Flyway migrations; seed tổng hợp riêng |
| Thiết bị | Samsung SM-N986N — thiết bị thật, serial đã lược bỏ khỏi tài liệu |
| Android version | 13 |
| Kích thước màn hình | Physical `1440x3088`; override `1080x2316`; density `420 dpi` |
| API base URL | `http://127.0.0.1:8080` qua `adb reverse tcp:8080 tcp:8080` |
| Kết quả tổng thể | `KHÔNG ĐẠT` — 5 đạt, 3 một phần, 8 không đạt, 0 bị chặn |
| Defect links | Chưa tạo ticket; đã tái hiện `MOB-GAP-01..05`, `MOB-GAP-07` và lỗi accessibility MF01-MOB-015 |

Không đưa mật khẩu, access token, refresh token, OTP thật, email/số điện thoại thật hoặc dữ liệu sức khỏe thật vào tài liệu và ảnh chụp.

### 3.1. Fix verification — 2026-07-18

Kết quả ở mục 3 là biên bản lịch sử của lần chạy trước fix và không bị ghi đè. Đợt triển khai này đã sửa các ca `MF01-MOB-001`, `004`, `006`, `007`, `008`, `010`, `011`, `015`.

| Phạm vi | Trạng thái sau fix | Bằng chứng |
| --- | --- | --- |
| Contract create/update, provenance, PRE transition, BABY_CARE boundary, history, routing, accessibility/font 150% | `PASS — automated` | `test/features/journey/story_6_1_mobile_gap_test.dart`: 10/10 |
| Toàn bộ Flutter test suite | `PASS — regression` | 182/182 |
| Static analysis phần thay đổi | `PASS` | `flutter analyze lib/core/routes/app_router.dart lib/features/journey test/features/journey/story_6_1_mobile_gap_test.dart` |
| APK debug kết nối local API | `PASS` | Build với `API_BASE_URL=http://127.0.0.1:8080`, cài thành công lên Samsung SM-N986N |
| `MF01-MOB-001` — Mother đã có role nhưng chưa có journey | `PASS — device smoke` | `mobile-fix-verification-2026-07-18/01-empty-routing-fixed.*` |
| `MF01-MOB-011` — lịch sử transition và provenance | `PASS — device smoke` | `mobile-fix-verification-2026-07-18/02-preg-history-fixed.*` |
| `MF01-MOB-015` — nhãn accessibility nút quay lại | `PASS — device smoke`; font 150% `PASS — widget` | `mobile-fix-verification-2026-07-18/03-back-accessibility-fixed.*` |

Các ca còn lại đã có regression contract xanh nhưng vẫn phải chạy lại toàn bộ bước manual trong tài liệu này trước khi đổi biên bản tổng thể sang `ĐẠT`. Story tiếp tục ở trạng thái `review`.

### 3.2. Full manual rerun trên thiết bị thật — 2026-07-18

Đã chạy lại đầy đủ 16/16 ca trên Samsung SM-N986N, Android 13, với backend local và PostgreSQL disposable.

| Kết quả | Số ca |
| --- | ---: |
| `ĐẠT` | 9 |
| `MỘT PHẦN` | 4 |
| `KHÔNG ĐẠT` | 3 |
| `BỊ CHẶN` | 0 |

Quality gate vẫn là `KHÔNG ĐẠT`. Lỗi nghiêm trọng nhất là MF01-MOB-014: sau khi đăng xuất `MOTHER_PREG` và đăng nhập `MOTHER_OTHER`, Home/Hành trình hiển thị tuần thai và EDD của tài khoản trước cho đến khi relaunch, dù DB của tài khoản sau có 0 journey.

Các gap còn lại gồm selected semantics cho TalkBack, current journey không refresh sau create/update/transition hoặc resume, state của phương pháp tính thai bị giữ chéo, và nội dung/control bị che ở font 150% + landscape.

Biên bản chi tiết và toàn bộ evidence: `06_Testing/TestResults/epic-6/story-6-1/mobile-full-rerun-2026-07-18/README.md`.

### 3.3. Gap-fix closure rerun trên thiết bị thật — 2026-07-18

Sau khi sửa optimistic cache theo owner, refresh/revision signal, stale-load race, wizard state, selected semantics và responsive layout, đã chạy lại đúng 7 ca chưa đạt hoàn toàn trong biên bản 3.2.

| ID | Trạng thái cuối | Bằng chứng chính |
| --- | --- | --- |
| `MF01-MOB-002` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/002-stage-initial.*`, `002-planning-selected.*` |
| `MF01-MOB-006` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/006-created-immediate.*` |
| `MF01-MOB-008` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/008-pre.*`, `008-preg-immediate.*` |
| `MF01-MOB-010` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/010-update-immediate-fixed.*` |
| `MF01-MOB-014` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/014-account-a-preg.*`, `014-account-b-isolated.*` |
| `MF01-MOB-015` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/015-wizard-landscape-*`, `015-result-landscape-*` |
| `MF01-MOB-016` | `ĐẠT` | `mobile-gap-fix-rerun-2026-07-18/016-selected.*`, `016-resumed.*` |

Kết quả closure: `7/7 ĐẠT`, gồm đủ 4 ca `MỘT PHẦN` và 3 ca `KHÔNG ĐẠT` trước đó. Kết hợp với 9 ca không đổi đã đạt ở lần full rerun, quality gate mobile tổng hợp là `16/16 ĐẠT`.

Automated verification sau bản sửa cuối:

- Story 6.1 mobile gap regression: `18/18 PASS`;
- full Flutter regression: `190/190 PASS`;
- targeted Dart analysis: không có lỗi;
- format và diff whitespace check: PASS.

Biên bản closure và toàn bộ evidence: `06_Testing/TestResults/epic-6/story-6-1/mobile-gap-fix-rerun-2026-07-18/README.md`.

## 4. Điều kiện tiên quyết

1. Backend đang chạy và truy cập được từ thiết bị.
2. PostgreSQL test database đã áp dụng migration:

   `V20260718090000__canonical_mother_lifecycle_history.sql`

3. Mobile app được build từ cùng commit với backend cần test.
4. Chuẩn bị các tài khoản tổng hợp:

   | Bí danh | Dữ liệu yêu cầu |
   | --- | --- |
   | `MOTHER_NEW_A` | Tài khoản mới chưa chọn role; dùng tạo PRE_PREGNANCY |
   | `MOTHER_NEW_B` | Tài khoản mới chưa chọn role; dùng test nhánh nuôi bé |
   | `MOTHER_NEW_C` | Tài khoản mới chưa chọn role; dùng test tạo PREGNANCY |
   | `MOTHER_NEW_D` | Tài khoản mới chưa chọn role; dùng test mất mạng/retry |
   | `MOTHER_NEW_E` | Tài khoản mới chưa chọn role; dùng test background/resume khi create |
   | `MOTHER_EMPTY_EXISTING` | Đã có role Mẹ nhưng chưa có canonical lifecycle; dùng kiểm tra routing sau đăng nhập |
   | `MOTHER_PREG` | Có sẵn một `PREGNANCY ACTIVE` hợp lệ với provenance; dùng test dashboard/edit |
   | `MOTHER_OTHER` | Mẹ khác để kiểm tra cache và cô lập dữ liệu |

5. Không dùng `mother3@carebridge.dev` hoặc `mother4@carebridge.dev` cho happy path tạo mới vì dev seeder đã tạo journey cho các tài khoản này.
6. `MOTHER_PREG` nên được chuẩn bị qua API/Postman với:

   - `journeyType: PREGNANCY`
   - ngày thai kỳ hợp lệ;
   - `dateSource`;
   - `dateConfidence`.

7. Không xóa hoặc sửa trực tiếp dữ liệu trên shared/staging database để tái sử dụng tài khoản.
8. Mỗi test case làm thay đổi lifecycle phải dùng đúng tài khoản riêng ở bảng trên. Nếu không đủ tài khoản, chỉ reset trong disposable local database theo quy trình đã được phê duyệt.

## 5. Chạy ứng dụng

### 5.1 Android emulator

Mobile app mặc định gọi backend qua `http://10.0.2.2:8080`.

```powershell
cd 05_Development/CareBridgeMobileApp
flutter pub get
flutter run
```

### 5.2 Thiết bị Android thật qua USB

Ứng dụng phải gọi `127.0.0.1` khi dùng `adb reverse`:

```powershell
adb reverse tcp:8080 tcp:8080
cd 05_Development/CareBridgeMobileApp
flutter run --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

### 5.3 Thiết bị Android thật qua mạng LAN

```powershell
flutter run --dart-define=API_BASE_URL=http://<IP-MAY-TINH>:8080
```

Điện thoại và máy tính phải cùng mạng; firewall phải cho phép cổng 8080.

### 5.4 Kiểm tra kết nối trước khi test

- Mở app và đăng nhập thành công.
- Nếu app báo không kết nối được máy chủ, chưa bắt đầu test nghiệp vụ.
- Ghi `BỊ CHẶN — API không truy cập được` thay vì đánh dấu test case thất bại.

## 6. Quy tắc đánh giá

- **ĐẠT:** Toàn bộ kết quả mong đợi của sản phẩm được quan sát và có bằng chứng.
- **KHÔNG ĐẠT:** Hành vi khác kết quả mong đợi, có side effect sai hoặc thông báo gây hiểu nhầm.
- **BỊ CHẶN:** Không thể chạy do môi trường, tài khoản hoặc fixture.
- **KHÔNG ĐẠT — GAP ĐÃ BIẾT:** Lỗi khớp với bảng gap ở mục 2. Vẫn phải ghi defect/evidence; không chuyển thành `ĐẠT`.

Mọi trường hợp tạo hai canonical lifecycle ACTIVE, hiển thị journey của tài khoản khác hoặc làm mất history đều là lỗi chặn phát hành.

Sau mỗi ca:

- bật lại mạng nếu đã tắt;
- đưa font scale về mặc định;
- tắt TalkBack nếu ca tiếp theo không kiểm tra accessibility;
- đưa thiết bị về portrait;
- đăng xuất khi đổi bí danh tài khoản;
- không xóa dữ liệu trực tiếp để “làm xanh” kết quả.

## 7. Bảng tổng hợp

| ID | Kịch bản | Ưu tiên | Kết quả | Bằng chứng / lỗi |
| --- | --- | --- | --- | --- |
| MF01-MOB-001 | Mẹ chưa có journey được chuyển đến chọn giai đoạn | P0 | `KHÔNG ĐẠT — MOB-GAP-07` | Tài khoản mới đi đúng role → stage selection; Mother đã có role nhưng chưa có journey bị đưa thẳng về Home. Evidence local `03..05`, `15` |
| MF01-MOB-002 | Chọn card, trạng thái nút và khả năng đọc màn hình | P1 | `MỘT PHẦN — ĐẠT` | Ba card chọn loại trừ nhau, toàn card có thể chạm, CTA đổi đúng; chưa chạy TalkBack trực tiếp. Evidence local `05..08` |
| MF01-MOB-003 | Tạo hành trình `PRE_PREGNANCY` | P0 | `ĐẠT` | Double tap chỉ tạo 1 `PRE_PREGNANCY ACTIVE`, version 0 và 1 CREATED. Evidence local `09` + DB |
| MF01-MOB-004 | Hiển thị PRE_PREGNANCY sau khi tạo | P0 | `KHÔNG ĐẠT — MOB-GAP-05` | Home hiển thị `Chuẩn bị mang thai`, nhưng tab Hành trình vẫn báo chưa có hành trình mang thai. Evidence local `09`, `10` |
| MF01-MOB-005 | Wizard tính thai kỳ theo bốn phương pháp | P1 | `MỘT PHẦN` | Đã chạy phương pháp tuổi thai hiện tại: 4 tuần 0 ngày → EDD 27/03/2027; chưa chạy đủ bốn phương pháp |
| MF01-MOB-006 | Tạo PREGNANCY từ mobile | P0 | `KHÔNG ĐẠT — MOB-GAP-01` | UI báo không thể tạo; DB giữ 0 journey/0 transition cho tài khoản mới. Evidence local `16`, `17` |
| MF01-MOB-007 | Nhánh Đang nuôi bé không tạo BABY_CARE canonical | P0 | `KHÔNG ĐẠT — MOB-GAP-03` | DB đúng là không tạo row, nhưng UI báo sai rằng đã có hành trình hoạt động. Evidence local `18` |
| MF01-MOB-008 | Không tạo lifecycle thứ hai | P0 | `KHÔNG ĐẠT` | Invariant DB được giữ ở 1 active + 1 transition, nhưng mobile gọi create mới thay vì transition và chỉ báo lỗi chung. Evidence local `11..14` |
| MF01-MOB-009 | Dashboard của fixture PREGNANCY | P0 | `ĐẠT` | Dashboard hiển thị tuần 15, tam cá nguyệt 2, LMP 01/04/2026, EDD 08/01/2027. Evidence local `19`, `20` |
| MF01-MOB-010 | Chỉnh ngày dự sinh từ mobile | P0 | `KHÔNG ĐẠT — MOB-GAP-02` | Chọn EDD 09/01/2027 nhưng update bị từ chối; DB vẫn EDD 08/01/2027, version 1, 2 transitions. Evidence local `21..24` |
| MF01-MOB-011 | Lịch sử chuyển đổi trên mobile | P1 | `KHÔNG ĐẠT — MOB-GAP-04` | Không có điểm truy cập history ở Trang chủ, Hành trình hoặc toàn bộ Hồ sơ |
| MF01-MOB-012 | Lỗi mạng và retry không tạo bản ghi trùng | P0 | `ĐẠT` | Gỡ `adb reverse` cho lỗi mạng, khôi phục và retry một lần; DB có đúng 1 PRE_PREGNANCY + 1 CREATED. Evidence local `25..27` |
| MF01-MOB-013 | Access token hết hạn được refresh an toàn | P1 | `ĐẠT` | TTL test 2 giây: refresh hợp lệ giữ dashboard và xoay token; revoke refresh rồi relaunch đưa app về welcome/login. Evidence local `32`, `33` |
| MF01-MOB-014 | Đổi tài khoản không rò rỉ journey/cache | P0 | `ĐẠT` | Đăng xuất đúng luồng rồi đăng nhập Mother khác; Home/Journey không hiển thị PRE/EDD của tài khoản trước. Evidence local `28..31` |
| MF01-MOB-015 | Text scale, xoay màn hình và touch target | P1 | `KHÔNG ĐẠT` | 150% làm tiêu đề `Kết quả` bị cắt/che; nút back có `NAF=true` và không có nhãn accessibility |
| MF01-MOB-016 | Chạy nền/khôi phục trong lúc thiết lập | P2 | `MỘT PHẦN — ĐẠT` | Resume sau 30 giây giữ bước 2 và màn kết quả; phần resume ngay sau create bị chặn |

### 7.1 Nhật ký thực thi ADB

- Thiết bị kết nối ổn định; package `com.carebridge.app`, activity `com.carebridge.app.MainActivity`.
- Phiên ban đầu vào thẳng Trang chủ, hiển thị `Chào Mẹ`, thẻ `Nuôi con`; Hồ sơ hiển thị `Người dùng` và `Chưa có email`, nên không thể ánh xạ phiên này với fixture tổng hợp nào.
- Tab `Hành trình → Mang thai` hiển thị `Chưa có hành trình mang thai` và nút `Thêm hành trình`.
- Wizard mở được đủ bốn lựa chọn. Nút `Tiếp theo` vô hiệu khi chưa chọn phương pháp.
- Đã chọn phương pháp `Tôi đã biết thời gian sản khoa của mình`, giữ giá trị mặc định `4 tuần 0 ngày`; kết quả tính EDD là `27 tháng 3, 2027`.
- Đưa app xuống background 30 giây ở bước 2 và ở màn kết quả: cả hai lượt đều resume đúng màn, giữ nguyên dữ liệu, không crash.
- Ở font scale `1.5`, tiêu đề `Kết quả` bị cắt/che phía dưới progress bar. Font scale đã được khôi phục về `1.0`.
- Đã xoay landscape và khôi phục `accelerometer_rotation=1`, `user_rotation=0`.
- Accessibility hierarchy của nút back ghi `NAF=true`, `content-desc=""`; chưa bật TalkBack để tránh thay đổi sâu thiết bị khi không có fixture.
- Không có `adb reverse`; máy tính không listen cổng 8080. Do API endpoint và tài khoản hiện tại không xác định, không bấm các nút tạo/cập nhật lifecycle.
- Bằng chứng nằm tại `06_Testing/TestResults/epic-6/story-6-1/mobile-manual-2026-07-18/`.

## 8. Chi tiết ca kiểm thử

### MF01-MOB-001 — Mẹ chưa có journey được chuyển đến chọn giai đoạn

**Tài khoản:** `MOTHER_NEW_A`, sau đó lặp lại với `MOTHER_EMPTY_EXISTING`.

**Bước thực hiện:**

1. Đăng xuất khỏi tài khoản đang dùng.
2. Đăng nhập bằng `MOTHER_NEW_A`.
3. Chọn vai trò `Mẹ bầu`.
4. Chờ điều hướng hoàn tất.
5. Giữ nguyên màn hình này để chạy MF01-MOB-002 và MF01-MOB-003.

Chạy phần routing cho existing Mother ở một lượt độc lập sau đó:

6. Đăng xuất.
7. Đăng nhập bằng `MOTHER_EMPTY_EXISTING` đã có role Mẹ nhưng chưa có journey.
8. Kiểm tra app có mở màn hình chọn giai đoạn hay không.

**Kết quả mong đợi:**

- App mở màn hình `Bạn đang ở giai đoạn nào?`.
- Có ba lựa chọn:

  - `Muốn mang thai`
  - `Đang mang thai`
  - `Đang nuôi bé`

- Không tự chọn sẵn một giai đoạn.
- Nút `Tiếp tục` bị vô hiệu khi chưa chọn.
- Không có dữ liệu của tài khoản đã đăng nhập trước đó.
- Cả Mẹ vừa chọn role và Mẹ đã có role nhưng chưa có journey đều được đưa tới màn hình này.

**Dự kiến theo router hiện tại:**

- `MOTHER_NEW_A` đi đúng sau khi chọn role.
- `MOTHER_EMPTY_EXISTING` có thể bị đưa thẳng về Trang chủ trước khi dashboard được kiểm tra.

Nếu `MOTHER_EMPTY_EXISTING` không đến màn hình chọn giai đoạn, ghi `KHÔNG ĐẠT — MOB-GAP-07`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-002 — Trạng thái chọn và khả năng đọc màn hình

**Bước thực hiện:**

1. Chạm lần lượt từng card.
2. Quan sát viền, nền, icon check và nhãn nút phía dưới.
3. Chạm vùng chữ, icon và khoảng trống bên trong card.
4. Bật TalkBack và đọc lại ba lựa chọn.
5. Tăng font size của Android lên mức lớn.
6. Sau khi ghi nhận, tắt TalkBack và đưa font size về mặc định.

**Kết quả mong đợi:**

- Toàn bộ card là vùng chạm, không chỉ phần chữ.
- Chỉ một card được chọn tại một thời điểm.
- Nhãn nút đổi tương ứng:

  - `Tạo hành trình chuẩn bị`
  - `Tiếp tục tính thai kỳ`
  - `Thiết lập hồ sơ bé`

- Nút/card có vùng chạm tối thiểu khoảng 48dp.
- Nội dung không bị cắt, chồng hoặc tràn khỏi màn hình.
- Focus và trạng thái chọn có thể nhận biết bằng nhiều dấu hiệu, không chỉ màu sắc.

**Lưu ý accessibility:** Nếu TalkBack chỉ đọc text rời rạc mà không thông báo card đang được chọn, ghi `KHÔNG ĐẠT`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-003 — Tạo hành trình PRE_PREGNANCY

**Tài khoản:** `MOTHER_NEW_A`

**Bước thực hiện:**

1. Chọn `Muốn mang thai`.
2. Chạm `Tạo hành trình chuẩn bị`.
3. Trong lúc loading, chạm nút thêm nhiều lần.
4. Chờ app điều hướng về Trang chủ.
5. Đóng hoàn toàn app rồi mở lại.
6. Đăng nhập lại nếu cần.

**Kết quả mong đợi:**

- Chỉ gửi một yêu cầu tạo journey.
- Loading indicator xuất hiện và nút không thể bấm lặp.
- Không hiển thị lỗi.
- App chuyển về Trang chủ.
- Khi mở lại, app không yêu cầu chọn giai đoạn lần nữa.
- Backend có đúng một `PRE_PREGNANCY ACTIVE` cho `MOTHER_NEW_A`.
- Có một history event `CREATED`, version `0` — xác nhận bằng backend guide hoặc API vì mobile chưa có history UI.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-004 — Hiển thị PRE_PREGNANCY sau khi tạo

**Tài khoản:** `MOTHER_NEW_A` sau MF01-MOB-003.

**Bước thực hiện:**

1. Từ Trang chủ, chọn tab `Hành trình`.
2. Quan sát tiêu đề, tab `Mang thai` và nội dung journey.
3. Kéo để refresh nếu màn hình hỗ trợ.
4. Quay về Trang chủ rồi mở lại tab `Hành trình`.

**Kết quả mong đợi của sản phẩm:**

- App thể hiện rõ Mẹ đang ở giai đoạn `Chuẩn bị mang thai`.
- Không hiển thị lời kêu gọi tạo một canonical journey mới.
- Không làm người dùng hiểu rằng journey vừa tạo đã mất.

**Dự kiến theo code hiện tại:**

- Có thể hiển thị `Chưa có hành trình mang thai` vì màn hình chỉ render pregnancy dashboard cho `PREGNANCY`.

Nếu quan sát đúng dự kiến hiện tại, ghi `KHÔNG ĐẠT — MOB-GAP-05`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-005 — Wizard tính thai kỳ theo bốn phương pháp

**Tài khoản:** `MOTHER_NEW_C`.

**Bước thực hiện:**

1. Đăng nhập `MOTHER_NEW_C`, chọn vai trò `Mẹ bầu`.
2. Chọn `Đang mang thai`.
3. Xác nhận app mở wizard `journey-setup`.
4. Chạy từng phương pháp, quay lại và chọn `TÍNH LẠI` giữa các lượt:

   - ngày đầu kỳ kinh cuối;
   - ngày thụ thai;
   - tuổi thai hiện tại;
   - ngày sinh dự kiến.

5. Với phương pháp kỳ kinh cuối:

   - chọn một ngày dễ nhận biết;
   - thử chu kỳ 28 ngày;
   - thử một độ dài chu kỳ khác;
   - thử `KHÔNG BIẾT`.

6. Kiểm tra màn hình kết quả.

**Kết quả mong đợi:**

- Progress thể hiện đúng số bước.
- Nút `Tiếp theo` bị vô hiệu khi chưa nhập dữ liệu bắt buộc.
- Có thể quay lại mà dữ liệu đã nhập không biến mất ngoài ý muốn.
- EDD theo kỳ kinh cuối bằng LMP + 280 ngày khi chu kỳ 28 ngày.
- EDD theo ngày thụ thai bằng ngày thụ thai + 266 ngày.
- Tuổi thai/tuổi thai nhi không âm và không vượt giới hạn màn hình cho phép.
- Màn hình kết quả hiển thị tuần/ngày và ngày dự sinh dễ đọc.
- `TÍNH LẠI` quay về bước chọn phương pháp.

Chưa bấm `Tạo hành trình` cho đến MF01-MOB-006.

**Kết quả thực tế / bằng chứng:** `MỘT PHẦN` — Đã xác nhận bốn phương pháp xuất hiện, nút `Tiếp theo` vô hiệu trước khi chọn và chạy phương pháp tuổi thai hiện tại với `4 tuần 0 ngày`. Kết quả hiển thị EDD `27 tháng 3, 2027`; `TÍNH LẠI` xuất hiện. Chưa chạy ba phương pháp còn lại vì phiên hiện tại không phải `MOTHER_NEW_C`. Evidence: `04-setup-step1.png`, `05-setup-step2.png`, `07-setup-result.png`.

### MF01-MOB-006 — Tạo PREGNANCY từ mobile

**Tài khoản:** `MOTHER_NEW_C`.

**Bước thực hiện:**

1. Hoàn thành wizard bằng một trong bốn phương pháp.
2. Tại màn hình kết quả, chạm `Tạo hành trình`.
3. Quan sát loading, thông báo lỗi và điều hướng.
4. Kiểm tra backend không tạo current/history dang dở khi request thất bại.

**Kết quả mong đợi của sản phẩm:**

- Journey `PREGNANCY ACTIVE` được tạo.
- Request chứa provenance tương ứng phương pháp:

  - nguồn ngày;
  - độ tin cậy;
  - lý do/effective time nếu contract yêu cầu.

- App về Trang chủ và dashboard hiển thị tuần thai/ngày dự sinh.

**Dự kiến theo code hiện tại:**

- Mobile gửi ngày nhưng không gửi `dateSource`/`dateConfidence`.
- Backend trả HTTP 400 `JOURNEY-018`.
- UI hiển thị thông báo chung `Không thể tạo hành trình. Vui lòng thử lại.`

Nếu quan sát đúng dự kiến hiện tại, ghi `KHÔNG ĐẠT — MOB-GAP-01`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-007 — Nhánh Đang nuôi bé không tạo BABY_CARE canonical

**Tài khoản:** `MOTHER_NEW_B`

**Bước thực hiện:**

1. Đăng nhập `MOTHER_NEW_B`.
2. Chọn vai trò `Mẹ bầu` để mở màn hình chọn giai đoạn.
3. Chọn `Đang nuôi bé`.
4. Chạm `Thiết lập hồ sơ bé`.
5. Quan sát loading, thông báo và điều hướng.

**Kết quả mong đợi của sản phẩm:**

- Mobile không gọi canonical lifecycle create với `BABY_CARE`.
- Không tạo bản ghi `BABY_CARE` trong canonical maternal lifecycle.
- App chuyển sang luồng hồ sơ bé tương thích hoặc giải thích rõ bước tiếp theo.
- Không hiển thị thông báo sai rằng người dùng đã có active journey nếu thực tế chưa có.

**Dự kiến theo code hiện tại:**

- Mobile gửi `BABY_CARE`.
- Backend trả HTTP 409 `JOURNEY-016`.
- UI có thể hiển thị `Bạn đã có hành trình đang hoạt động cho lựa chọn này.`

Nếu quan sát đúng dự kiến hiện tại, ghi `KHÔNG ĐẠT — MOB-GAP-03`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-008 — Không tạo lifecycle thứ hai

**Tài khoản:** `MOTHER_NEW_A` đã có PRE_PREGNANCY.

**Bước thực hiện:**

1. Mở tab `Hành trình`.
2. Nếu màn hình hiển thị `Thêm hành trình`, chạm nút đó.
3. Hoàn thành pregnancy wizard và chạm `Tạo hành trình`.
4. Thử lại một lần sau khi lỗi xuất hiện.

**Kết quả mong đợi:**

- Không tạo current row thứ hai.
- Mobile nên chuyển `PRE_PREGNANCY → PREGNANCY` bằng update/transition, không gọi create mới.
- Nếu không hỗ trợ transition, phải hiển thị lỗi rõ ràng và giữ nguyên current/history.
- Không có loading vô hạn hoặc tạo trùng khi retry.

**Dự kiến theo code hiện tại:**

- Wizard gọi create mới.
- Vì request thiếu provenance, backend có thể trả `JOURNEY-018` trước khi kiểm tra duplicate.
- UI hiển thị lỗi chung, không hướng dẫn người dùng chuyển giai đoạn.

Data integrity vẫn phải giữ đúng một active canonical row; UX/transition vẫn là `KHÔNG ĐẠT` nếu không thể tiếp tục.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-009 — Dashboard của fixture PREGNANCY

**Tài khoản:** `MOTHER_PREG`

**Bước thực hiện:**

1. Đăng nhập bằng `MOTHER_PREG`.
2. Xác nhận app vào Trang chủ thay vì màn hình chọn giai đoạn.
3. Mở tab `Hành trình`.
4. Chọn phần `Mang thai`.
5. Đối chiếu với fixture:

   - tuần thai;
   - tam cá nguyệt;
   - ngày dự sinh;
   - số ngày còn lại;
   - journey ID khi mở các chức năng liên quan.

6. Chuyển sang `Nuôi con`, sau đó quay lại `Mang thai`.

**Kết quả mong đợi:**

- Dashboard lấy canonical active `PREGNANCY`.
- Không chọn nhầm BABY_CARE hoặc dữ liệu tài khoản khác.
- Tuần thai/EDD khớp fixture hoặc phép tính từ LMP.
- Chuyển tab không làm mất dashboard.
- Không hiển thị dữ liệu optimistic cache cũ thay cho response mới.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-010 — Chỉnh ngày dự sinh từ mobile

**Tài khoản:** `MOTHER_PREG`

**Bước thực hiện:**

1. Trong tab `Hành trình → Mang thai`, chạm icon chỉnh sửa tại thẻ ngày dự sinh.
2. Chọn một phương pháp/mốc ngày mới có thể nhận biết.
3. Hoàn thành wizard và lưu.
4. Quan sát lỗi hoặc dashboard được refresh.
5. Đóng/mở lại app và đối chiếu ngày.

**Kết quả mong đợi của sản phẩm:**

- Request update có date provenance.
- Current journey tăng version đúng một lần.
- History thêm `DATES_CHANGED` với previous/new values.
- Dashboard hiển thị dữ liệu mới sau refresh và relaunch.

**Dự kiến theo code hiện tại:**

- Mobile update gửi ngày nhưng không gửi provenance.
- Backend trả HTTP 400 `JOURNEY-018`.
- UI hiển thị `Không thể cập nhật hành trình. Vui lòng thử lại.`

Nếu quan sát đúng dự kiến hiện tại, ghi `KHÔNG ĐẠT — MOB-GAP-02`.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-011 — Lịch sử chuyển đổi trên mobile

**Tài khoản:** Một Mother có ít nhất hai transition.

**Bước thực hiện:**

1. Mở Trang chủ, tab Hành trình và Hồ sơ.
2. Tìm mục `Lịch sử hành trình`, `Lịch sử chuyển đổi` hoặc tương đương.
3. Tìm khả năng mở CREATED/STAGE_CHANGED/DATES_CHANGED.

**Kết quả mong đợi của sản phẩm:**

- Người dùng có điểm truy cập lịch sử của journey thuộc sở hữu.
- History mới nhất hiển thị trước.
- Chỉ hiển thị các field tối thiểu, không lộ token, contact data, actor ID hoặc raw JSON.

**Dự kiến theo code hiện tại:**

- Không có API call/history screen trên mobile.

Nếu không tìm thấy lịch sử, ghi `KHÔNG ĐẠT — MOB-GAP-04`, không ghi `BỊ CHẶN`.

**Kết quả thực tế / bằng chứng:** `KHÔNG ĐẠT — MOB-GAP-04` — Không có mục lịch sử ở Trang chủ, cả hai phần của tab Hành trình hoặc toàn bộ Hồ sơ sau khi cuộn đến cuối. Evidence: `01-initial-screen.png`, `02-journey-screen.png`, `03-pregnancy-tab.png`, `11-profile.png` đến `13-profile-end.png`.

### MF01-MOB-012 — Lỗi mạng và retry không tạo bản ghi trùng

**Tài khoản:** `MOTHER_NEW_D`.

**Bước thực hiện:**

1. Đăng nhập `MOTHER_NEW_D`, chọn vai trò `Mẹ bầu`.
2. Chọn `Muốn mang thai`.
3. Tắt Wi-Fi/mobile data trên thiết bị. Chỉ được dừng backend khi đang dùng local/isolated environment và không ảnh hưởng người test khác.
4. Chạm nút tạo.
5. Chờ thông báo lỗi.
6. Bật lại kết nối/backend.
7. Chạm lại đúng một lần.
8. Mở lại app.

**Kết quả mong đợi:**

- Khi mất mạng, app hiển thị lỗi kết nối và dừng loading.
- Người dùng có thể retry.
- Phiên đăng nhập không bị xóa vì lỗi mạng tạm thời.
- Sau khôi phục, chỉ một canonical current row và một CREATED history tồn tại.
- Không có snackbar/banner che nút hoặc bị cắt bởi bàn phím/system bar.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-013 — Access token hết hạn được refresh an toàn

**Điều kiện:** Môi trường test có hai fixture được phê duyệt:

- access token hết hạn nhưng refresh token còn hợp lệ;
- access token và refresh token đều không hợp lệ.

**Bước thực hiện:**

Lượt A — refresh token hợp lệ:

1. Đăng nhập và để access token hết hạn hoặc dùng fixture được phê duyệt.
2. Mở tab `Hành trình`.
3. Chờ request dashboard và refresh-token flow hoàn tất.

Lượt B — refresh token không hợp lệ:

4. Dùng fixture có cả access token và refresh token không hợp lệ.
5. Mở tab `Hành trình`.
6. Quan sát session và điều hướng.

**Kết quả mong đợi:**

- App refresh token một lần và retry request.
- Không hiển thị journey của phiên trước.
- Không bật nhiều dialog/login screen chồng nhau.
- Nếu refresh token cũng không hợp lệ, app xóa session và trở về đăng nhập.
- Không ghi token vào log/screenshot.

Nếu không có fixture token, ghi `BỊ CHẶN — thiếu fixture`, không tự sửa storage production.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-014 — Đổi tài khoản không rò rỉ journey/cache

**Bước thực hiện:**

1. Đăng nhập `MOTHER_PREG`; mở tab Hành trình và ghi lại stage/EDD bằng dữ liệu tổng hợp.
2. Đăng xuất đúng luồng.
3. Đăng nhập `MOTHER_OTHER`.
4. Mở Trang chủ và Hành trình ngay sau đăng nhập.
5. Đóng/mở lại app.

**Kết quả mong đợi:**

- Không có khoảnh khắc hiển thị stage, EDD hoặc journey ID của `MOTHER_PREG`.
- Optimistic dashboard cache được ràng buộc theo user ID.
- `MOTHER_OTHER` chỉ thấy dữ liệu của chính mình.
- Không có request update nào dùng journey ID của tài khoản cũ.

Bất kỳ dữ liệu chéo tài khoản nào cũng là lỗi P0/security.

**Kết quả thực tế / bằng chứng:** `[điền]`

### MF01-MOB-015 — Text scale, xoay màn hình và touch target

**Bước thực hiện:**

1. Tăng font size Android lên 150–200%.
2. Kiểm tra:

   - màn hình chọn giai đoạn;
   - từng bước pregnancy wizard;
   - màn hình kết quả;
   - tab Hành trình và thẻ EDD.

3. Xoay portrait ↔ landscape tại mỗi nhóm màn hình.
4. Kiểm tra bằng TalkBack.
5. Sau khi ghi nhận, tắt TalkBack, đưa font size về mặc định và xoay lại portrait.

**Kết quả mong đợi:**

- Không overflow, chữ chồng, nút bị che hoặc mất nội dung.
- Có thể cuộn đến mọi control.
- Nút chính và card có touch target tối thiểu 48dp.
- Text quan trọng có độ tương phản rõ trên nền beige/white.
- Selected state, error và loading không chỉ truyền đạt bằng màu.
- Focus order đi từ tiêu đề → lựa chọn → nút hành động.
- TalkBack đọc được nhãn có ý nghĩa cho icon back/edit.

**Kết quả thực tế / bằng chứng:** `KHÔNG ĐẠT` — Ở font scale `1.5`, tiêu đề `Kết quả` bị cắt/che phía dưới progress bar. UI hierarchy của nút back ghi `NAF=true`, `content-desc=""`, nên không có nhãn accessibility có ý nghĩa. Đã kiểm tra landscape và khôi phục font/orientation về trạng thái ban đầu. Evidence: `09-result-font150.png`, `09-result-font150-window.xml`, `10-result-landscape.png`.

### MF01-MOB-016 — Chạy nền/khôi phục trong lúc thiết lập

**Bước thực hiện:**

1. Đi đến giữa pregnancy wizard.
2. Đưa app xuống background 30 giây rồi mở lại.
3. Lặp lại tại màn hình kết quả.
4. Đăng nhập `MOTHER_NEW_E`, chọn Mẹ → `Muốn mang thai`, sau đó đưa app xuống background ngay sau khi bấm tạo PRE_PREGNANCY.
5. Mở lại sau khi request hoàn tất.

**Kết quả mong đợi:**

- App không crash hoặc quay về sai tài khoản.
- Dữ liệu wizard không đổi ngoài ý muốn khi resume ngắn.
- Không gửi lại create chỉ vì lifecycle resume.
- Sau create thành công, app hiển thị trạng thái nhất quán với backend.
- Back navigation không thoát app bất ngờ hoặc bỏ lại loading overlay.

**Kết quả thực tế / bằng chứng:** `MỘT PHẦN — ĐẠT` — App resume đúng bước 2 sau 30 giây và giữ `4 tuần 0 ngày`; lần thứ hai resume đúng màn kết quả và giữ EDD `27 tháng 3, 2027`. Không crash, không đổi tài khoản. Phần background ngay sau create bị chặn vì không có `MOTHER_NEW_E` và backend isolated. Evidence: `06-wizard-resume.png`, `08-result-resume.png`.

## 9. Đối chiếu backend tối thiểu

Mobile hiện chưa hiển thị history/version/provenance, vì vậy dùng backend manual guide hoặc truy vấn read-only để xác nhận các invariant sau:

- mỗi Mother có tối đa một canonical lifecycle ACTIVE;
- create thành công có đúng một CREATED transition;
- request bị từ chối không tạo current/history;
- update thành công tăng version và thêm đúng một transition;
- không có `BABY_CARE` mới trong canonical lifecycle.

Không chạy câu lệnh DELETE/UPDATE trực tiếp trên shared/staging database.

### 9.1 Mapping sang backend manual guide

| Mobile case | Bằng chứng backend cần dùng |
| --- | --- |
| MF01-MOB-003 | Backend guide `MF01-6.1-MAN-004` và mục 9 |
| MF01-MOB-006 | Backend guide `MF01-6.1-MAN-003`/mục 9 để xác nhận `JOURNEY-018` và không có side effect |
| MF01-MOB-007 | Backend guide `MF01-6.1-MAN-007`/mục 9 để xác nhận không có BABY_CARE canonical |
| MF01-MOB-008 | Backend guide `MF01-6.1-MAN-005..007` để xác nhận đúng một active canonical row |
| MF01-MOB-010 | Backend guide `MF01-6.1-MAN-006`/mục 9 để đối chiếu current/version/history |

### 9.2 Kiểm tra request từ Flutter

Trong local/isolated environment, có thể dùng Flutter DevTools → Network:

1. lọc request chứa `/api/v1/journeys`;
2. ghi HTTP method, status và error code;
3. với dữ liệu tổng hợp, kiểm tra payload có/không có `dateSource` và `dateConfidence`;
4. không chụp hoặc xuất `Authorization` header;
5. không dùng proxy bắt gói trên staging nếu chưa được phê duyệt.

### 9.3 Truy vấn read-only

Nhờ người có quyền database chạy với user ID tổng hợp:

```sql
SELECT
    journey_id,
    journey_type,
    status,
    version,
    date_source,
    date_confidence
FROM public.mother_journeys
WHERE owner_user_id = '<SYNTHETIC_MOTHER_USER_ID>'
ORDER BY created_at;
```

```sql
SELECT
    event_type,
    from_stage,
    to_stage,
    source,
    confidence,
    journey_version,
    recorded_at
FROM public.mother_journey_transitions
WHERE journey_id = '<SYNTHETIC_JOURNEY_ID>'
ORDER BY recorded_at;
```

Ghi lại số dòng và field cần thiết; không export toàn bộ bảng hoặc dữ liệu của tài khoản khác.

## 10. Mẫu báo lỗi

```text
Tiêu đề: [Story 6.1][Mobile][Case ID] Mô tả ngắn

Build / commit:
Thiết bị / Android:
API environment:
Case ID:
Vai trò tài khoản:
Lifecycle fixture:

Điều kiện:
Các bước:
Kết quả mong đợi:
Kết quả thực tế:
Thông báo trên UI:
HTTP status / error code (nếu lấy được từ log backend):
Side effect current/history:
Khả năng tái hiện:

Evidence:
Token/credentials đã che: Có/Không
Dữ liệu thật xuất hiện: Có/Không
Known gap liên quan:
```

## 11. Completion Gate

Mobile integration chỉ đạt Story 6.1 khi:

- tạo PRE_PREGNANCY hoạt động và được hiển thị đúng;
- tạo PREGNANCY gửi đầy đủ provenance và thành công;
- PRE_PREGNANCY có thể chuyển sang PREGNANCY thay vì create row thứ hai;
- nhánh nuôi bé không tạo BABY_CARE canonical;
- chỉnh ngày gửi provenance, tăng version và ghi history;
- người dùng xem được transition history tối thiểu trên mobile;
- không có cache/data leakage khi đổi tài khoản;
- lỗi mạng, token expiry và double tap không tạo trùng;
- các kiểm tra accessibility P1 đạt;
- mọi P0 đạt và không còn `MOB-GAP-01..07` chưa xử lý.

## 12. Tài liệu liên quan

- `06_Testing/TestCases/backend/MF-01-Story-6.1-Manual-Test-Guide.md`
- `04_Implement/UC22 - Canonical Mother Lifecycle and Transition History/UC22 - Canonical Mother Lifecycle and Transition History_TDS.md`
- `04_Implement/UC22 - Canonical Mother Lifecycle and Transition History/UC22 - Canonical Mother Lifecycle and Transition History_Test-Spec.md`
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_stage_selection_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/journey_setup_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_journey_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/services/journey_service.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/models/journey_model.dart`
