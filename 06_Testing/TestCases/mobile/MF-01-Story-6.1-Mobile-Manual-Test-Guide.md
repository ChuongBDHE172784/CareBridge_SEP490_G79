# MF-01 / Story 6.1 — Hướng dẫn kiểm thử thủ công trên Mobile App

## Canonical Mother Lifecycle and Transition History

| Trường | Giá trị |
| --- | --- |
| Epic / Story | Epic 6 — Story 6.1 |
| Phạm vi | Tích hợp Flutter với canonical Mother lifecycle |
| Nền tảng | Android emulator hoặc thiết bị Android thật |
| Dữ liệu | Chỉ dùng dữ liệu tổng hợp |
| Phiên bản hướng dẫn | 1.2 |
| Cập nhật lần cuối | 2026-07-18 |
| Trạng thái | Story `done`; mobile manual trên thiết bị thật `16/16 ĐẠT` |

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

Đây là hướng dẫn kiểm thử tích hợp có thể tái sử dụng. Backend và mobile đã tích hợp contract Story 6.1. Mục 3 và Mục 7 ghi kết quả closure cuối; Mục 8 là checklist để chạy lại về sau.

## 2. Trạng thái các gap mobile

Các gap phát hiện trước đây đã được sửa và đóng. Nếu hành vi xuất hiện lại trong lần chạy mới, ghi `KHÔNG ĐẠT — REGRESSION`; không waive bằng nhãn “gap đã biết”.

| Gap | Trạng thái | Regression cần phát hiện |
| --- | --- | --- |
| `MOB-GAP-01` | `ĐÃ ĐÓNG` | Create PREGNANCY thiếu provenance hoặc giữ LMP của phương pháp trước |
| `MOB-GAP-02` | `ĐÃ ĐÓNG` | Update EDD thiếu provenance, không refresh tức thời hoặc giữ LMP stale |
| `MOB-GAP-03` | `ĐÃ ĐÓNG` | Nhánh nuôi bé tạo `BABY_CARE` canonical hoặc báo lỗi sai |
| `MOB-GAP-04` | `ĐÃ ĐÓNG` | History thiếu trang, bị xóa khi lỗi hoặc không có retry |
| `MOB-GAP-05` | `ĐÃ ĐÓNG` | PRE/POSTPARTUM bị coi là chưa có maternal lifecycle hoặc bị cache PREGNANCY che |
| `MOB-GAP-06` | `ĐÃ ĐÓNG` | Model/cache làm mất `version`, `dateSource`, `dateConfidence` |
| `MOB-GAP-07` | `ĐÃ ĐÓNG` | `NO_JOURNEY` không vào stage selection hoặc lỗi transport bị điều hướng nhầm sang onboarding |

## 3. Biên bản closure cuối

| Trường | Kết quả cuối |
| --- | --- |
| Người test | Codex qua ADB, có người dùng phê duyệt thao tác trực tiếp |
| Ngày test | 2026-07-18 |
| Bản dựng | APK debug từ Story 6.1 worktree, `API_BASE_URL=http://127.0.0.1:8080` |
| Môi trường API | Spring Boot local + PostgreSQL disposable với dữ liệu synthetic |
| Thiết bị | Samsung SM-N986N, Android 13 |
| Kết nối | `adb reverse tcp:8080 tcp:8080` |
| Kết quả manual | `16/16 ĐẠT`, `0 KHÔNG ĐẠT`, `0 BỊ CHẶN` |
| Quality gate | `ĐẠT` |
| Defect | `MOB-GAP-01..07` và lỗi accessibility đã đóng |

Không đưa mật khẩu, access token, refresh token, OTP thật, email/số điện thoại thật hoặc dữ liệu sức khỏe thật vào tài liệu và ảnh chụp.

### 3.1 Bằng chứng xác nhận

- Full device rerun trước gap-fix: `06_Testing/TestResults/epic-6/story-6-1/mobile-full-rerun-2026-07-18/README.md`.
- Gap-fix closure: `7/7 ĐẠT`; kết hợp 9 ca không đổi thành composite `16/16 ĐẠT` tại `06_Testing/TestResults/epic-6/story-6-1/mobile-gap-fix-rerun-2026-07-18/README.md`.
- Final code-review verification: Story behavior `15/15 PASS`, full Flutter regression `187/187 PASS`, targeted analyzer không có issue tại `06_Testing/TestResults/epic-6/story-6-1/code-review-2026-07-18.md`.
- Các số `18/18` và `190/190` trong closure report là kết quả trước lần tái cấu trúc test cuối; kết quả code-review ở trên là trạng thái cuối cùng.

## 4. Điều kiện tiên quyết

1. Backend đang chạy và truy cập được từ thiết bị.
2. PostgreSQL test database đã áp dụng đủ hai migration:

   - `V20260718090000__canonical_mother_lifecycle_history.sql`
   - `V20260718091000__enforce_mother_journey_transition_immutability.sql`
3. Mobile app được build từ cùng commit với backend cần test.
4. Chuẩn bị các tài khoản tổng hợp:

   | Bí danh                  | Dữ liệu yêu cầu                                                                              |
   | ------------------------- | ------------------------------------------------------------------------------------------------ |
   | `MOTHER_NEW_A`          | Tài khoản mới chưa chọn role; dùng tạo PRE_PREGNANCY                                      |
   | `MOTHER_NEW_B`          | Tài khoản mới chưa chọn role; dùng test nhánh nuôi bé                                   |
   | `MOTHER_NEW_C`          | Tài khoản mới chưa chọn role; dùng test tạo PREGNANCY                                     |
   | `MOTHER_NEW_D`          | Tài khoản mới chưa chọn role; dùng test mất mạng/retry                                   |
   | `MOTHER_NEW_E`          | Tài khoản mới chưa chọn role; dùng test background/resume khi create                       |
   | `MOTHER_EMPTY_EXISTING` | Đã có role Mẹ nhưng chưa có canonical lifecycle; dùng kiểm tra routing sau đăng nhập |
   | `MOTHER_PREG` | Có sẵn một `PREGNANCY ACTIVE` hợp lệ với provenance; dùng test dashboard/edit |
   | `MOTHER_POSTPARTUM` | Có `POSTPARTUM ACTIVE` và history; dùng kiểm tra maternal lifecycle sau sinh |
   | `MOTHER_HISTORY` | Có số transition lớn hơn page size; dùng kiểm tra pagination/error retry |
   | `MOTHER_OTHER`          | Mẹ khác để kiểm tra cache và cô lập dữ liệu                                            |
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
- **KHÔNG ĐẠT — REGRESSION:** Một gap đã đóng ở Mục 2 xuất hiện lại; phải tạo defect/evidence mới.

Mọi trường hợp tạo hai canonical lifecycle ACTIVE, hiển thị journey của tài khoản khác hoặc làm mất history đều là lỗi chặn phát hành.

Sau mỗi ca:

- bật lại mạng nếu đã tắt;
- đưa font scale về mặc định;
- tắt TalkBack nếu ca tiếp theo không kiểm tra accessibility;
- đưa thiết bị về portrait;
- đăng xuất khi đổi bí danh tài khoản;
- không xóa dữ liệu trực tiếp để “làm xanh” kết quả.

## 7. Bảng tổng hợp

Kết quả cuối trên thiết bị thật ngày 2026-07-18:

| ID | Kịch bản | Ưu tiên | Kết quả cuối | Bằng chứng chính |
| --- | --- | --- | --- | --- |
| MF01-MOB-001 | Mother chưa có journey vào chọn giai đoạn | P0 | `ĐẠT` | Full rerun: `001-newa-stage.*`, `001-empty-routing.*` |
| MF01-MOB-002 | Card selection và accessibility semantics | P1 | `ĐẠT` | Closure: `002-stage-initial.*`, `002-planning-selected.*` |
| MF01-MOB-003 | Tạo `PRE_PREGNANCY` không trùng | P0 | `ĐẠT` | Full rerun: `003-pre-created.*`, `003-pre-db.txt` |
| MF01-MOB-004 | Hiển thị PRE sau khi tạo | P0 | `ĐẠT` | Full rerun: `004-pre-journey.*` |
| MF01-MOB-005 | Bốn phương pháp tính thai kỳ | P1 | `ĐẠT` | Full rerun: `005-*-result.*`, cycle 28/29/unknown |
| MF01-MOB-006 | Tạo PREGNANCY với provenance | P0 | `ĐẠT` | Closure: `006-created-immediate.*` |
| MF01-MOB-007 | Nhánh nuôi bé không tạo BABY_CARE canonical | P0 | `ĐẠT` | Full rerun: `007-baby-route.*`, `007-baby-db.txt` |
| MF01-MOB-008 | PRE chuyển PREG tại chỗ | P0 | `ĐẠT` | Closure: `008-pre.*`, `008-preg-immediate.*` |
| MF01-MOB-009 | Dashboard PREGNANCY | P0 | `ĐẠT` | Full rerun: `009-preg-dashboard.*` |
| MF01-MOB-010 | Cập nhật EDD và refresh tức thời | P0 | `ĐẠT` | Closure: `010-update-immediate-fixed.*` |
| MF01-MOB-011 | Lịch sử chuyển đổi | P1 | `ĐẠT` | Full rerun: `011-history.*`; automated pagination/retry regression |
| MF01-MOB-012 | Lỗi mạng/retry không tạo trùng | P0 | `ĐẠT` | Full rerun: `012-network-error.*`, `012-retry-success.*` |
| MF01-MOB-013 | Refresh/revoke token an toàn | P1 | `ĐẠT` | Full rerun: `013-valid-refresh*`, `013-invalid-refresh*` |
| MF01-MOB-014 | Đổi tài khoản không rò cache | P0 | `ĐẠT` | Closure: `014-account-a-preg.*`, `014-account-b-isolated.*` |
| MF01-MOB-015 | Font scale, landscape, touch target | P1 | `ĐẠT` | Closure: `015-wizard-landscape-*`, `015-result-landscape-*` |
| MF01-MOB-016 | Background/resume | P2 | `ĐẠT` | Closure: `016-selected.*`, `016-resumed.*` |

### 7.1 Vị trí evidence

- 9 ca không đổi đã đạt: `06_Testing/TestResults/epic-6/story-6-1/mobile-full-rerun-2026-07-18/`.
- 7 ca gap-fix closure: `06_Testing/TestResults/epic-6/story-6-1/mobile-gap-fix-rerun-2026-07-18/`.
- Nhật ký lần chạy đầu trước fix chỉ là historical evidence tại `06_Testing/TestResults/epic-6/story-6-1/mobile-manual-2026-07-18/`; không dùng để kết luận trạng thái hiện tại.

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

**Regression trọng tâm:** Sau khi tài khoản trước có journey, response `NO_JOURNEY` có thẩm quyền phải xóa cache không-pending cũ và đưa đúng tài khoản hiện tại tới stage selection. Lỗi transport phải hiển thị retry, không giả làm `NO_JOURNEY`.

**Closure 2026-07-18:** `ĐẠT` — `001-newa-stage.*`, `001-empty-routing.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — selected semantics, mutual exclusion và CTA đã được xác nhận bằng `002-stage-initial.*`, `002-planning-selected.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — đúng một PRE active và một CREATED tại `003-pre-created.*`, `003-pre-db.txt`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Regression trọng tâm:** PRE_PREGNANCY là canonical maternal lifecycle hợp lệ và không được hiển thị như trạng thái chưa thiết lập.

**Closure 2026-07-18:** `ĐẠT` — `004-pre-journey.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — đã chạy đủ LMP, conception, gestational age, clinician EDD và cycle 28/29/unknown; xem `005-*-result.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

### MF01-MOB-006 — Tạo PREGNANCY từ mobile

**Tài khoản:** `MOTHER_NEW_C`.

**Bước thực hiện:**

1. Tính thử bằng LMP để wizard có dữ liệu LMP.
2. Chọn `TÍNH LẠI`, đổi sang clinician EDD và chọn ngày dễ nhận biết.
3. Chạm `Tạo hành trình`.
4. Quan sát loading và điều hướng.
5. Đối chiếu payload/current row để chắc chắn không giữ LMP của phương pháp trước.

**Kết quả mong đợi của sản phẩm:**

- Journey `PREGNANCY ACTIVE` được tạo.
- Request chứa provenance tương ứng phương pháp:

  - nguồn ngày;
  - độ tin cậy;
  - lý do/effective time nếu contract yêu cầu.
- App về Trang chủ và dashboard hiển thị tuần thai/ngày dự sinh.
- Clinician EDD tạo journey với LMP `null`; `dateSource`/`dateConfidence` phản ánh nguồn clinician-confirmed.

**Closure 2026-07-18:** `ĐẠT` — `006-created-immediate.*`; DB không giữ LMP stale.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Regression trọng tâm:** Nhánh này phải đi tới luồng hồ sơ bé và tuyệt đối không gọi create canonical `BABY_CARE`.

**Closure 2026-07-18:** `ĐẠT` — `007-baby-route.*`, `007-baby-db.txt`.

**Lần chạy mới:** `[điền kết quả và evidence]`

### MF01-MOB-008 — Không tạo lifecycle thứ hai

**Tài khoản:** `MOTHER_NEW_A` đã có PRE_PREGNANCY.

**Bước thực hiện:**

1. Mở tab `Hành trình`.
2. Chọn CTA chuyển sang thai kỳ.
3. Hoàn thành pregnancy wizard và xác nhận.
4. Refresh Hành trình ngay, không relaunch app.

**Kết quả mong đợi:**

- Không tạo current row thứ hai.
- Mobile chuyển `PRE_PREGNANCY → PREGNANCY` bằng update/transition.
- UI refresh tức thời, giữ cùng journey ID, tăng version một lần và thêm `STAGE_CHANGED`.
- Cache giữ `version`, `dateSource`, `dateConfidence`.

**Closure 2026-07-18:** `ĐẠT` — `008-pre.*`, `008-preg-immediate.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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
6. Chuyển sang `Bé`, sau đó quay lại `Mẹ`.
7. Lặp lại với `MOTHER_POSTPARTUM` sau khi tài khoản trước đã cache PREGNANCY.

**Kết quả mong đợi:**

- Dashboard lấy canonical active `PREGNANCY`.
- Không chọn nhầm BABY_CARE hoặc dữ liệu tài khoản khác.
- Tuần thai/EDD khớp fixture hoặc phép tính từ LMP.
- Chuyển tab không làm mất dashboard.
- Không hiển thị dữ liệu optimistic cache cũ thay cho response mới.
- POSTPARTUM vẫn được nhận diện là maternal lifecycle, hiển thị history và không bị cache PREGNANCY cũ che.

**Closure 2026-07-18:** `ĐẠT` — dashboard PREGNANCY tại `009-preg-dashboard.*`; POSTPARTUM/cache behavior được xác nhận trong final Story regression.

**Lần chạy mới:** `[điền kết quả và evidence]`

### MF01-MOB-010 — Chỉnh ngày dự sinh từ mobile

**Tài khoản:** `MOTHER_PREG`

**Bước thực hiện:**

1. Trong tab `Hành trình → Mang thai`, chạm icon chỉnh sửa tại thẻ ngày dự sinh.
2. Chọn phương pháp EDD-only và một ngày mới dễ nhận biết.
3. Hoàn thành wizard và lưu.
4. Quan sát lỗi hoặc dashboard được refresh.
5. Đóng/mở lại app và đối chiếu ngày.

**Kết quả mong đợi của sản phẩm:**

- Request update có date provenance.
- Current journey tăng version đúng một lần.
- History thêm `DATES_CHANGED` với previous/new values.
- Dashboard hiển thị EDD mới ngay sau lưu, không cần relaunch.
- EDD-only update xóa LMP cũ khỏi response/cache.

**Closure 2026-07-18:** `ĐẠT` — `010-update-immediate-fixed.*`; DB version `2`, ba transition.

**Lần chạy mới:** `[điền kết quả và evidence]`

### MF01-MOB-011 — Lịch sử chuyển đổi trên mobile

**Tài khoản:** `MOTHER_HISTORY`, có nhiều transition hơn page size và đã từng tải history thành công.

**Bước thực hiện:**

1. Mở Trang chủ, tab Hành trình và Hồ sơ.
2. Tìm mục `Lịch sử hành trình`, `Lịch sử chuyển đổi` hoặc tương đương.
3. Mở history và cuộn đến cuối để tải đủ các trang.
4. Đối chiếu bản ghi đầu/cuối và tổng số event với API.
5. Ngắt backend, refresh history, sau đó khôi phục backend và chạm `Thử lại`.

**Kết quả mong đợi của sản phẩm:**

- Người dùng có điểm truy cập lịch sử của journey thuộc sở hữu.
- History mới nhất hiển thị trước.
- Tải đủ mọi trang, không trùng/mất event.
- Khi refresh lỗi, history cũ vẫn còn và UI có error/retry rõ ràng.
- Chỉ hiển thị các field tối thiểu, không lộ token, contact data, actor ID hoặc raw JSON.

**Closure 2026-07-18:** `ĐẠT` — UI history tại `011-history.*`; pagination và retry được xác nhận trong final Story behavior regression.

**Lần chạy mới:** `[điền kết quả và evidence]`

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
9. Ở lượt độc lập, gây lỗi transport khi dashboard đang được xác định ngay sau đăng nhập.

**Kết quả mong đợi:**

- Khi mất mạng, app hiển thị lỗi kết nối và dừng loading.
- Người dùng có thể retry.
- Phiên đăng nhập không bị xóa vì lỗi mạng tạm thời.
- Sau khôi phục, chỉ một canonical current row và một CREATED history tồn tại.
- Không có snackbar/banner che nút hoặc bị cắt bởi bàn phím/system bar.
- Dashboard transport failure giữ người dùng ở error card có `Thử lại`, không điều hướng sang stage selection.

**Closure 2026-07-18:** `ĐẠT` — `012-network-error.*`, `012-retry-success.*`, `012-retry-db.txt`; transport routing có automated regression.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — `013-valid-refresh*`, `013-invalid-refresh*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

### MF01-MOB-014 — Đổi tài khoản không rò rỉ journey/cache

**Bước thực hiện:**

1. Đăng nhập `MOTHER_PREG`; mở tab Hành trình và ghi lại stage/EDD bằng dữ liệu tổng hợp.
2. Đăng xuất đúng luồng.
3. Đăng nhập `MOTHER_OTHER`.
4. Mở Trang chủ và Hành trình ngay sau đăng nhập.
5. Trong lượt có network delay, để request tài khoản A hoàn tất sau khi đã login tài khoản B.
6. Đóng/mở lại app.

**Kết quả mong đợi:**

- Không có khoảnh khắc hiển thị stage, EDD hoặc journey ID của `MOTHER_PREG`.
- Optimistic dashboard cache được ràng buộc theo user ID.
- `MOTHER_OTHER` chỉ thấy dữ liệu của chính mình.
- Không có request update nào dùng journey ID của tài khoản cũ.
- Async response/write của tài khoản A bị từ chối sau account switch.

Bất kỳ dữ liệu chéo tài khoản nào cũng là lỗi P0/security.

**Closure 2026-07-18:** `ĐẠT` — `014-account-a-preg.*`, `014-account-b-isolated.*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — `015-wizard-landscape-*`, `015-method-selected.xml`, `015-result-landscape-*`.

**Lần chạy mới:** `[điền kết quả và evidence]`

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

**Closure 2026-07-18:** `ĐẠT` — `016-selected.*`, `016-resumed.*`; create hoàn tất khi background và UI refresh đúng khi resume.

**Lần chạy mới:** `[điền kết quả và evidence]`

## 9. Đối chiếu backend tối thiểu

Mobile đã hiển thị history và giữ version/provenance trong model/cache. Vẫn dùng backend manual guide hoặc truy vấn read-only để đối soát các invariant không thể chứng minh hoàn toàn bằng UI:

- mỗi Mother có tối đa một canonical lifecycle ACTIVE;
- create thành công có đúng một CREATED transition;
- request bị từ chối không tạo current/history;
- update thành công tăng version và thêm đúng một transition;
- không có `BABY_CARE` mới trong canonical lifecycle.

Không chạy câu lệnh DELETE/UPDATE trực tiếp trên shared/staging database.

### 9.1 Mapping sang backend manual guide

| Mobile case | Bằng chứng backend cần dùng |
| --- | --- |
| MF01-MOB-003 | Backend guide `MF01-6.1-MAN-004` và Mục 9: một PRE active, một CREATED |
| MF01-MOB-006 | HTTP `201`; current/history có provenance đúng và clinician EDD không giữ LMP stale |
| MF01-MOB-007 | Backend guide `MF01-6.1-MAN-007`: không tạo BABY_CARE canonical |
| MF01-MOB-008 | Backend guide `MF01-6.1-MAN-005..007`: cùng journey ID, tăng version, thêm STAGE_CHANGED |
| MF01-MOB-010 | Backend guide `MF01-6.1-MAN-006`: current/version/history và nullable LMP |
| MF01-MOB-011 | API history `page`/`size`: đủ trang, đúng thứ tự, không trùng event |

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
    last_menstrual_date,
    estimated_due_date,
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

Trạng thái closure ngày 2026-07-18: `ĐẠT`.

- [x] Tạo và hiển thị PRE_PREGNANCY đúng.
- [x] Tạo PREGNANCY với provenance đầy đủ; không giữ state phương pháp cũ.
- [x] PRE chuyển sang PREGNANCY tại chỗ, không tạo lifecycle thứ hai.
- [x] Nhánh nuôi bé không tạo BABY_CARE canonical.
- [x] Update EDD tăng version, ghi history và refresh tức thời.
- [x] POSTPARTUM được nhận diện là maternal lifecycle.
- [x] History hiển thị tối thiểu, tải đủ pagination và giữ dữ liệu khi retry.
- [x] Không rò cache/dữ liệu khi đổi tài khoản hoặc request cũ hoàn tất muộn.
- [x] Lỗi mạng, token expiry và double tap không tạo trùng.
- [x] Accessibility, font scale 150% và landscape đạt.
- [x] Composite device manual `16/16 ĐẠT`.
- [x] Final Story behavior `15/15 PASS`; full Flutter regression `187/187 PASS`.

`16/16` là số ca manual trên thiết bị; `15/15` là số automated Story behavior tests sau lần tái cấu trúc test cuối. Với lần rerun mới, dùng checklist Mục 8 và không sửa kết quả closure lịch sử ở Mục 3/Mục 7.

## 12. Tài liệu liên quan

- `06_Testing/TestCases/backend/MF-01-Story-6.1-Manual-Test-Guide.md`
- `02_Requirements/SRS/Report3_Functional_Specifications.md` — current consolidated UC catalogue
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_stage_selection_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/journey_setup_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_journey_screen.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/services/journey_service.dart`
- `05_Development/CareBridgeMobileApp/lib/features/journey/models/journey_model.dart`
- `06_Testing/TestResults/epic-6/story-6-1/mobile-full-rerun-2026-07-18/README.md` — kết quả full rerun trước gap-fix, lưu làm lịch sử
- `06_Testing/TestResults/epic-6/story-6-1/mobile-gap-fix-rerun-2026-07-18/README.md` — closure 7/7 và composite 16/16
- `06_Testing/TestResults/epic-6/story-6-1/code-review-2026-07-18.md` — verification cuối sau code review
