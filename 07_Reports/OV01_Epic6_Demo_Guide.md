# HƯỚNG DẪN DEMO CÁC CHỨC NĂNG ĐÃ TRIỂN KHAI — OV01 / EPIC 6

**Phạm vi demo:** Story 6.1–6.5
**Đối tượng sử dụng:** Thành viên trình bày tiến độ, người vận hành thiết bị demo
**Ngày cập nhật:** 21/07/2026
**Trạng thái phạm vi:** Story 6.1–6.4 `DONE`; Story 6.5 cũ `SUPERSEDED — feature removed`

## 1. Mục tiêu buổi demo

Kịch bản chứng minh các năng lực đã triển khai:

1. Người dùng vai trò Mẹ phải hoàn tất thông tin nền và đồng thuận.
2. Hệ thống duy trì một hành trình Mẹ canonical và lịch sử chuyển trạng thái.
3. Người dùng có thể thiết lập thai kỳ, cập nhật kết quả thai kỳ và chuyển sang hậu sản.
4. Người dùng có thể bắt đầu phục hồi hậu sản mà không cần tạo hồ sơ em bé.
5. Sau live birth, người dùng có thể để sau hoặc tạo hồ sơ em bé standalone; Add Baby thông thường không có “Để sau”.

Không trình bày Story 6.6–6.10 như chức năng đã hoàn thành.

## 2. Nguyên tắc an toàn khi demo

- Chỉ dùng database local/disposable và tài khoản synthetic.
- Không dùng tài khoản, OTP, token hoặc dữ liệu sức khỏe thật.
- Không chiếu access token, refresh token, biến môi trường hoặc log chứa credential.
- Không chạy migration cleanup dữ liệu legacy trong buổi demo.
- Không sửa trực tiếp database để “làm xanh” một bước đang lỗi.
- Story 6.5 đã `SUPERSEDED — feature removed`; không dùng evidence liên kết cũ để mô tả hành vi hiện tại.

## 3. Chuẩn bị môi trường

### 3.1. Thành phần cần chạy

- PostgreSQL có đầy đủ Flyway migration của Story 6.1–6.5 và migration loại bỏ chức năng liên kết hồ sơ bé.
- CareBridge Backend API.
- Flutter Mobile App từ cùng commit với backend.
- Thiết bị Android hoặc emulator.
- Kết nối mạng ổn định giữa ứng dụng và API.

### 3.2. Bật dữ liệu demo local

Seeder chỉ chạy khi được bật rõ ràng. Thiết lập trong terminal dùng cho backend:

```powershell
$env:SUPABASE_DB_URL = "jdbc:postgresql://<host>:<port>/<database>"
$env:SUPABASE_DB_USERNAME = "<demo-db-user>"
$env:SUPABASE_DB_PASSWORD = "<demo-db-password>"
$env:JWT_ACTIVE_KEY_ID = "<demo-active-kid>"
$env:JWT_PRIVATE_KEY = "<demo-base64-der-pkcs8-private-key>"
$env:JWT_PUBLIC_KEYS = "<demo-active-kid>:<demo-base64-der-spki-public-key>"
$env:SPRING_PROFILES_ACTIVE = "dev"
$env:CAREBRIDGE_DEV_SEED_ENABLED = "true"
```

Trước khi chạy lệnh trên, người vận hành phải nạp
`CAREBRIDGE_DEV_SEED_PASSWORD` bằng một mật khẩu synthetic riêng, không mặc định,
thông qua secret source của terminal local. Hướng dẫn này cố ý không hiển thị, đề
xuất hoặc ghi lại giá trị đó. Seeder chỉ tồn tại dưới profile `dev`, không có profile
`prod`, và chỉ chạy khi property enable được bật rõ ràng. Nếu password bị bỏ trống
hoặc trùng historical default đã retired, backend sẽ fail startup thay vì seed dữ liệu.

`application.yaml` đang đặt Flyway mặc định là `false`. Cách an toàn nhất là dùng database demo đã được migrate và kiểm tra trước. Chỉ khi tạo database disposable hoàn toàn mới và đã được nhóm cho phép, bật Flyway cho đúng lần khởi tạo:

```powershell
$env:SPRING_FLYWAY_ENABLED = "true"
```

Không bật Flyway tùy tiện trên shared/staging trong buổi demo. `spring.jpa.hibernate.ddl-auto=update` không thay thế các constraint, trigger và backfill của Flyway.

Sau đó chạy backend:

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd spring-boot:run
```

> Chỉ người vận hành phiên demo được biết giá trị `CAREBRIDGE_DEV_SEED_PASSWORD` đã
> nạp. Không đọc, in, ghi log hoặc chiếu giá trị này; không dùng lại secret của shared,
> staging hay production.

### 3.3. Chạy ứng dụng trên thiết bị

Với thiết bị Android qua USB:

```powershell
adb devices
adb reverse tcp:8080 tcp:8080
cd 05_Development/CareBridgeMobileApp
flutter pub get
flutter run --dart-define=API_BASE_URL=http://127.0.0.1:8080
```

Với Android emulator, dùng `http://10.0.2.2:8080`. Với thiết bị thật không dùng `adb reverse`, dùng địa chỉ LAN của máy chạy backend và bảo đảm hai thiết bị cùng mạng.

### 3.4. Tài khoản/fixture đề xuất

| Mục đích | Tài khoản synthetic | Trạng thái cần có |
|---|---|---|
| Demo onboarding và tạo hành trình từ đầu | `mebau@carebridge.dev` hoặc tài khoản Mẹ mới trên DB disposable | Chưa có baseline/journey từ lần demo trước |
| Demo dashboard thai kỳ nhanh | `mother3@carebridge.dev` | Có canonical `PREGNANCY` |
| Demo hậu sản có sẵn | `mother4@carebridge.dev` | Có canonical `POSTPARTUM`; baby profile, nếu có, là standalone |
| Demo “Để sau” và tạo baby mới | `mother5@carebridge.dev` | `POSTPARTUM + LIVE_BIRTH`, ban đầu 0 baby |
| Demo nhiều baby độc lập | `mother6@carebridge.dev` | Có Baby A và Baby B standalone, cùng owner, ACTIVE |

Mọi tài khoản fixture dùng chung password synthetic do người vận hành nạp riêng cho
phiên demo; tài liệu này không lưu giá trị đó.

> Seeder không reset toàn bộ nghiệp vụ sau mỗi lần chạy. Trước buổi review, xác nhận fixture vẫn ở trạng thái mong muốn trên database disposable. Nếu đã dùng `mother5` hoặc `mother6`, chuẩn bị lại database/fixture trước khi demo.

## 4. Checklist trước khi trình bày

- [ ] Backend startup không có lỗi Flyway.
- [ ] Database demo đã có migration cần thiết; nếu là DB mới, Flyway chỉ được bật trên DB disposable đã xác minh.
- [ ] Bộ khóa RS256 demo (`JWT_ACTIVE_KEY_ID`, `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEYS`) đã được nạp nhưng không hiển thị trên màn hình chiếu.
- [ ] `CAREBRIDGE_DEV_SEED_PASSWORD` synthetic đã được nạp từ secret source local, không bị in hoặc hiển thị; seeder chỉ chạy với `dev & !prod` và enable gate rõ ràng.
- [ ] API nhận request tại `http://127.0.0.1:8080`.
- [ ] Thiết bị xuất hiện trong `adb devices`.
- [ ] `adb reverse tcp:8080 tcp:8080` thành công.
- [ ] Đăng nhập thử một tài khoản seed thành công.
- [ ] `mebau`/tài khoản mới chưa có lifecycle nếu dùng cho full flow.
- [ ] `mother5` có zero baby.
- [ ] `mother6` có hai profile standalone Baby A/B.
- [ ] Tắt thông báo cá nhân và đóng các cửa sổ có secret.
- [ ] Chuẩn bị sẵn báo cáo tiến độ `OV01_Epic6_Progress_Review.md` làm slide dự phòng.

## 5. Luồng demo chính

Kịch bản phải dùng nhiều fixture vì direct postpartum của Story 6.4 cần một tài khoản chưa có canonical journey:

- **Flow A — Story 6.2 → 6.1 → 6.3:** tài khoản NEW → baseline/consent → PRE/PREGNANCY → pregnancy outcome → POSTPARTUM.
- **Flow B — Story 6.4:** đăng xuất, dùng một tài khoản NEW khác đã qua baseline/consent nhưng chưa có journey → tạo POSTPARTUM trực tiếp → recovery log.
- **Flow C — Story 6.5:** dùng `mother5` cho typed live-birth Add Baby/defer và `mother6` cho nhiều profile standalone.

Không cố chạy Flow B trên tài khoản vừa hoàn tất Flow A, vì tài khoản đó đã có canonical journey.

## 6. Demo Story 6.2 — Baseline Context và Required Consent Gate

### 6.1. Mục tiêu trình bày

- Không thể bỏ qua baseline/consent để vào hành trình.
- Đồng thuận không được chọn sẵn.
- Submit hợp lệ chuyển sang màn hình chọn giai đoạn.

### 6.2. Thao tác

1. Đăng nhập bằng tài khoản Mẹ mới hoặc `mebau@carebridge.dev` trên DB disposable.
2. Nếu hệ thống hỏi vai trò, chọn **Mẹ**.
3. Tại màn hình onboarding, để trống **Mục tiêu hiện tại** và **Bạn muốn nhận hỗ trợ về**.
4. Bấm tiếp tục để cho thấy validation tại chỗ.
5. Chọn một mục tiêu, ví dụ **Chuẩn bị mang thai**.
6. Chọn ít nhất một nhu cầu hỗ trợ.
7. Sang bước đồng thuận và chỉ ra checkbox **Đồng ý xử lý dữ liệu hành trình** chưa được chọn sẵn.
8. Thử tiếp tục khi chưa đồng ý; ứng dụng phải giữ người dùng trong onboarding.
9. Chọn đồng thuận và hoàn tất.

### 6.3. Kết quả cần chỉ ra

- Thiếu trường bắt buộc không tạo lifecycle.
- Consent thiếu/không hợp lệ bị fail-closed.
- Dữ liệu hợp lệ chuyển sang **Chọn giai đoạn**.
- Khi mở lại app, trạng thái được đọc từ server; không bỏ qua consent gate.

### 6.4. Câu thuyết minh gợi ý

> “Baseline được lưu theo revision và consent được kiểm tra lại ở service boundary. UI không phải lớp bảo vệ duy nhất; backend vẫn từ chối tạo/chuyển hành trình nếu consent không hợp lệ.”

## 7. Demo Story 6.1 — Canonical Mother Journey và Transition History

### 7.1. Tạo hành trình đầu tiên

1. Từ màn hình chọn giai đoạn, chọn **Muốn mang thai**.
2. Bấm **Tạo hành trình chuẩn bị**.
3. Ứng dụng mở **Hành trình của Mẹ** ở trạng thái chuẩn bị mang thai.
4. Chuyển sang tab **Hành trình** nếu đang ở Trang chủ.

Kết quả cần chỉ ra:

- Chỉ có một hành trình ACTIVE của tài khoản.
- Dashboard hiển thị đúng stage `PRE_PREGNANCY`.
- Lịch sử có event tạo hành trình.

### 7.2. Chuyển PRE sang PREGNANCY trên cùng journey

1. Tại dashboard PRE, bấm **Bắt đầu hành trình thai kỳ**.
2. Chọn phương pháp nhập ngày phù hợp, ví dụ ngày dự sinh do bác sĩ xác nhận hoặc ngày đầu kỳ kinh cuối.
3. Nhập ngày synthetic hợp lệ và xác nhận.
4. Quan sát dashboard chuyển sang thai kỳ.

Kết quả cần chỉ ra:

- Không tạo journey thứ hai.
- Stage đổi sang `PREGNANCY` trên cùng journey.
- Version tăng và history có transition PRE → PREGNANCY.
- Ngày/EDD và provenance hiển thị lại sau refresh.

### 7.3. Phương án nhanh

Nếu không có tài khoản mới sạch, đăng nhập `mother3@carebridge.dev` và mở tab **Hành trình** để trình bày dashboard PREGNANCY. Dùng ảnh/test evidence thay cho bước tạo mới, nhưng nói rõ đây là fixture đã chuẩn bị trước.

## 8. Demo Story 6.3 — Pregnancy Outcome và Postpartum Transition

### 8.1. Nhánh live birth

1. Đăng nhập tài khoản có canonical PREGNANCY, ví dụ `mother3@carebridge.dev`.
2. Vào tab **Hành trình**.
3. Bấm hành động **Cập nhật tình trạng thai kỳ**.
4. Chọn kết quả **Em bé đã chào đời**/live birth.
5. Chọn ngày synthetic hợp lệ và mức độ chính xác/nguồn ngày nếu màn hình yêu cầu.
6. Kiểm tra màn hình xác nhận, sau đó bấm **Xác nhận**.
7. Quan sát dashboard chuyển sang hậu sản.

Kết quả cần chỉ ra:

- Outcome evidence được ghi append-only.
- Canonical journey chuyển sang POSTPARTUM.
- History vẫn giữ các transition trước đó.
- Hệ thống không tự suy diễn hoặc ghi đè evidence cũ.

### 8.2. Nhánh pregnancy loss — chỉ demo khi có fixture riêng

1. Dùng tài khoản PREGNANCY synthetic khác; không dùng lại tài khoản vừa demo live birth.
2. Chọn kết quả mất thai phù hợp trong màn hình cập nhật.
3. Xác nhận bằng ngôn ngữ trung tính, nhạy cảm.
4. Quan sát trạng thái recovery.

Kết quả cần chỉ ra:

- Có thể vào recovery mà không tạo `baby_profiles`.
- UI không dùng thông điệp chúc mừng live birth cho nhánh mất thai.

> Không chạy hai outcome khác nhau trên cùng fixture trong buổi demo nếu chưa chuẩn bị trước luồng correction.

## 9. Demo Story 6.4 — Direct Postpartum Recovery với Zero Baby

### 9.1. Tạo hành trình hậu sản trực tiếp

1. Dùng tài khoản Mẹ mới đã hoàn tất baseline/consent.
2. Tại màn hình chọn giai đoạn, chọn **Đang hồi phục sau sinh**.
3. Màn hình **Hành trình sau sinh** xuất hiện.
4. Tại **Ngày bắt đầu hồi phục**, bấm **Chọn ngày** và chọn ngày synthetic hợp lệ.
5. Chọn **Chính xác** hoặc **Ước tính**.
6. Bấm **Bắt đầu hành trình**.

Kết quả cần chỉ ra:

- Dashboard POSTPARTUM mở thành công.
- Không bắt buộc nhập dữ liệu thai kỳ.
- Không tự động tạo baby profile.
- Mẹ vẫn dùng được chức năng phục hồi khi số baby bằng 0.

### 9.2. Recovery log CRUD

1. Tại dashboard POSTPARTUM, chọn **Nhật ký hồi phục**.
2. Bấm **Thêm nhật ký**.
3. Chọn **Ngày ghi nhận**, mức độ chảy máu synthetic và các thông tin không nhạy cảm khác.
4. Bấm **Lưu nhật ký**.
5. Mở bản ghi vừa tạo để xem chi tiết.
6. Chọn sửa, thay đổi một giá trị và lưu lại.
7. Nếu cần chứng minh delete, xóa bản ghi demo cuối cùng và xác nhận nó không còn trong danh sách.

Kết quả cần chỉ ra:

- List/detail/create/update/delete gọi dữ liệu thật từ backend.
- Create có submission ID chống ghi trùng khi retry.
- Delete là soft-delete; không tuyên bố dữ liệu lịch sử bị xóa vật lý.
- Nút **Dấu hiệu cần hỗ trợ khẩn cấp** mới là seam hỗ trợ; không trình bày toàn bộ Story 6.6 là đã hoàn tất.

## 10. Demo Story 6.5 — Standalone Add Baby

### 10.1. Typed live-birth entry và “Để sau”

1. Đăng xuất và đăng nhập `mother5@carebridge.dev`.
2. Hoàn tất nhánh `LIVE_BIRTH` và chờ POSTPARTUM reload thành công.
3. Xác nhận Add Baby mở tự động bằng typed transition context.
4. Chọn **Để sau**.
5. Xác nhận app điều hướng đúng một lần tới tab **Bé**.

Kết quả cần chỉ ra:

- POSTPARTUM Journey đã commit vẫn authoritative.
- Không có baby hoặc handoff token được tạo.
- Mở `/babies/add` thông thường không hiển thị **Để sau**.

### 10.2. Tạo baby standalone sau live birth

1. Chạy lại `LIVE_BIRTH` trên fixture disposable hoặc dùng typed test entry đã duyệt.
2. Giữ Add Baby ở transition mode.
3. Nhập nickname và dữ liệu synthetic tối thiểu.
4. Hoàn tất tạo.
5. Quan sát tab **Bé** sau điều hướng.

Kết quả cần chỉ ra:

- Baby mới là profile standalone thuộc đúng owner.
- Response/create payload không chứa quan hệ Mother Journey.
- Journey version/stage của Mẹ không bị thay đổi.
- Danh sách hiển thị đúng một baby, không nhân đôi khi refresh.

### 10.3. Nhiều baby standalone và ownership isolation

1. Đăng xuất và đăng nhập `mother6@carebridge.dev`.
2. Mở tab **Bé**.
3. Chạm lần lượt Baby A/B.
4. Mở profile, growth và vaccination của từng baby.
5. Refresh và xác nhận mỗi profile xuất hiện đúng một lần.

Kết quả cần chỉ ra:

- Danh sách chỉ gồm baby thuộc owner hiện tại.
- Dữ liệu mỗi baby được cô lập và selected state vẫn hợp lệ sau refresh.
- Journey không có baby card hay hành động quan hệ.
- Refresh giữ dữ liệu server-authoritative và selected state hợp lệ.
- Truy cập baby khác owner bị từ chối trung tính.

### 10.4. Negative contract

> “Story 6.5 cũ đã superseded và chức năng liên kết hồ sơ bé bị loại bỏ. Demo hiện tại chỉ chứng minh standalone Add Baby, legacy relationship JSON trả validation `400`, và các URL đã gỡ trả generic `404/405`.”

## 11. Phương án dự phòng khi demo trực tiếp gặp sự cố

| Sự cố | Cách xử lý an toàn |
|---|---|
| Backend không khởi động | Kiểm tra PostgreSQL/Flyway và biến môi trường; không đổi schema thủ công |
| Thiết bị không gọi được API | Chạy lại `adb reverse tcp:8080 tcp:8080`, xác nhận base URL và backend port |
| Tài khoản đã bị thay đổi bởi lần demo trước | Đổi sang fixture dự phòng hoặc khôi phục DB disposable đã chuẩn bị |
| `mother5` đã có baby | Dùng snapshot DB sạch; không xóa trực tiếp trên shared DB |
| `mother6` thiếu Baby A/B | Khôi phục fixture standalone trên DB disposable |
| UI loading/network lỗi | Thử lại một lần, sau đó chuyển sang evidence screenshot/manual summary |
| Demo full flow quá rủi ro | Dùng các fixture `mother3`, `mother5`, `mother6` cho từng lát cắt độc lập |
| Không thể chứng minh DB trên màn hình | Trình bày dataflow/table trong báo cáo tiến độ; không chiếu credential hoặc token |

Evidence dự phòng:

- `_bmad-output/test-artifacts/story-6-2-manual/`
- `_bmad-output/test-artifacts/story-6-3-manual/`
- `_bmad-output/test-artifacts/story-6-4-manual/`
- `_bmad-output/test-artifacts/story-6-5-manual/manual-run-summary.md`
- `06_Testing/TestCases/mobile/OV-01-Mother-Lifecycle-Orchestration-Manual-Test-Guide.md`
- `07_Reports/OV01_Epic6_Progress_Review.md`

## 12. Những nội dung không được tuyên bố là hoàn thành

| Story | Không trình bày như chức năng đã đóng |
|---|---|
| 6.6 | RED escalation idempotent và toàn bộ postpartum safety orchestration |
| 6.7 | Persist safety outcome exactly-once và return-to-origin |
| 6.8 | YELLOW handoff tới verified expert với consented context |
| 6.9 | APPROVED-only checklist/content theo canonical stage |
| 6.10 | Full OV-01 E2E, traceability và release quality gate |

## 13. Checklist kết thúc demo

- [ ] Đăng xuất tài khoản demo.
- [ ] Dừng Flutter/backend local nếu không còn sử dụng.
- [ ] Không lưu screenshot/log chứa password hoặc token.
- [ ] Ghi lại story, fixture và kết quả thực tế nếu có lỗi.
- [ ] Không đẩy file `.env` hoặc credential vào Git.
- [ ] Nếu dùng DB disposable, xử lý theo quy trình của nhóm; không tác động shared/staging.

## 14. Thông điệp kết luận đề xuất

> “Epic 6 đã triển khai nền móng lifecycle từ baseline/consent, canonical journey và pregnancy outcome đến zero-baby postpartum. Story 6.5 cũ đã superseded; baby profile hiện là standalone và liên kết với Mother Journey đã bị loại bỏ. Các nhánh safety, expert/content và E2E closure thuộc Story 6.6–6.10 vẫn là phần việc tiếp theo.”

## 15. File code tham chiếu khi được hỏi kỹ thuật

| Chức năng | Mobile | Backend |
|---|---|---|
| Baseline/consent | `lib/features/journey/screens/journey_onboarding_screen.dart` | `journey/controller/JourneyOnboardingController.java`, `journey/service/impl/JourneyOnboardingServiceImpl.java` |
| Canonical journey/history | `lib/features/journey/screens/mother_journey_screen.dart` | `journey/controller/JourneyController.java`, `journey/service/impl/JourneyTransitionServiceImpl.java` |
| Pregnancy outcome | `lib/features/journey/screens/pregnancy_outcome_screen.dart` | `journey/service/impl/JourneyTransitionServiceImpl.java` |
| Direct postpartum | `lib/features/journey/screens/postpartum_recovery_setup_screen.dart` | `journey/service/impl/JourneyServiceImpl.java` |
| Recovery log | `lib/features/healthRecords/screens/postpartum_log_list_screen.dart`, `postpartum_log_detail_screen.dart`, `postpartum_log_form_screen.dart`; service `lib/features/healthRecords/services/postpartum_log_service.dart` | `health/controller/PostpartumLogController.java`, `health/service/impl/PostpartumLogServiceImpl.java` |
| Standalone Add Baby | `lib/features/baby/screens/add_baby_screen.dart` | `baby/controller/BabyController.java`, `baby/service/impl/BabyServiceImpl.java` |

Đường dẫn gốc:

- Mobile: `05_Development/CareBridgeMobileApp/`
- Backend: `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/`
