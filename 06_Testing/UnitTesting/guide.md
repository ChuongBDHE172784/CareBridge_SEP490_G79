# Hướng dẫn Unit Test — CareBridge (SEP490_G79)

Tài liệu này trả lời ba câu hỏi: **chạy test thế nào**, **kết quả đọc ra sao**, và **đưa kết quả vào
`Report5_Unit Test_CareBridge.xlsx` bằng cách nào**.

Tài liệu liên quan:

| File | Nội dung |
|---|---|
| [linkFileTest.md](linkFileTest.md) | Bản đồ tính năng ↔ file test (tính năng nào được test ở file nào) |
| `Report5_Unit Test_CareBridge.xlsx` | Báo cáo nộp — 83 sheet |
| `Report5.1_Unit Test_reference1.xlsx`, `Report5.1_UnitTest_reference2.xlsx` | Hai báo cáo mẫu của dự án khác, dùng để đối chiếu định dạng |
| [report-tools/](report-tools/) | Script sinh báo cáo (`collect.py`, `usecases.py`, `gen_report.py`) |

---

## 1. Chuẩn bị môi trường

| Tầng | Cần có | Kiểm tra |
|---|---|---|
| Backend | JDK 21 | `java -version` |
| Mobile | Flutter SDK | `flutter --version` |
| Web | Node.js ≥ 20 | `node -v` |
| Sinh báo cáo | Python 3 + openpyxl | `python3 -c "import openpyxl"` |

Nếu thiếu openpyxl: `pip3 install openpyxl`.

**Unit test không cần database, không cần `.env`, không cần Docker.**
Backend chạy trên H2 ở chế độ `MODE=PostgreSQL`, Flyway tắt, schema tạo bằng `ddl-auto=create-drop`.
Chỉ *integration test* mới cần Docker — và chúng **không** nằm trong báo cáo này (xem mục 4.6).

---

## 2. Chạy test

### 2.1 Backend (JUnit 5 + Mockito)

```bash
cd 05_Development/CareBridgeAPI

./mvnw test                                    # toàn bộ
./mvnw test -Dtest=AuthServiceLoginTest        # một class
./mvnw test -Dtest='AuthServiceLoginTest#login_wrongPassword_throws'   # một method
./mvnw test -Dtest='*SecurityTest'             # theo mẫu tên
```

Kết quả chi tiết nằm ở `target/surefire-reports/TEST-<FQCN>.xml` — mỗi class một file XML.
Đây chính là nguồn mà script sinh báo cáo đọc, **không cần làm gì thêm**.

> Chạy toàn bộ mất khoảng 10–15 phút. Dòng tổng kết cuối cùng có dạng
> `Tests run: 4031, Failures: 0, Errors: 142, Skipped: 70`.
> **142 `Errors` là bình thường nếu Docker đang tắt** — đó là integration test không khởi động được
> Testcontainers, lỗi môi trường chứ không phải lỗi code. Xem mục 8.

### 2.2 Mobile (flutter_test)

```bash
cd 05_Development/CareBridgeMobileApp

flutter test                                                    # toàn bộ
flutter test test/features/auth/blocked_account_screen_test.dart  # một file
flutter test --plain-name "edit preserves a null note"          # một test theo tên
```

Để lấy kết quả máy đọc được (dùng cho báo cáo):

```bash
flutter test --machine > ../../06_Testing/UnitTesting/report-tools/mobile-test.json
```

`--machine` in ra **JSON theo dòng** (mỗi dòng một sự kiện `suite` / `group` / `testStart` /
`testDone`), không phải một object JSON duy nhất — đừng dùng `json.load` cho cả file.

### 2.3 Web (Vitest + React Testing Library)

```bash
cd 05_Development/CareBridgeWebApp

npm test                                          # toàn bộ (= vitest run)
npx vitest run src/shared/api/apiClient.test.ts    # một file
npx vitest run -t "redirects normal API calls"     # một test theo tên
```

Để lấy kết quả máy đọc được:

```bash
npx vitest run --reporter=json \
  --outputFile=../../06_Testing/UnitTesting/report-tools/web-test.json
```

### 2.4 Demo nhanh cho người review

Muốn cho người khác xem "test chạy thật" mà không phải đợi 15 phút, chạy vài class tiêu biểu:

```bash
# Mobile — chứng minh lỗi ghi đè ghi chú đã được sửa (11 TC)
cd 05_Development/CareBridgeMobileApp && flutter test test/features/healthRecords/growth_measurement_form_test.dart

# Web — chứng minh điều hướng khi hệ thống bảo trì (5 TC)
cd 05_Development/CareBridgeWebApp && npx vitest run src/shared/api/apiClient.test.ts
```

Hai lệnh này tương ứng với các defect còn được theo dõi ở sheet `Defects` — xem mục 4.5.

---

## 3. Đọc kết quả

| Tầng | Ý nghĩa dòng tổng kết |
|---|---|
| Backend | `Tests run` = tổng; `Failures` = assertion sai; `Errors` = exception/không khởi động được; `Skipped` = `@Disabled` hoặc `@EnabledIf` không thoả |
| Mobile | `All tests passed!` hoặc `NN tests passed, MM failed` |
| Web | `Test Files N passed`, `Tests N passed` |

**Failures vs Errors**: `Failures` gần như luôn là lỗi code thật. `Errors` có thể là lỗi code, cũng
có thể là lỗi môi trường (thiếu Docker, thiếu biến môi trường) — phải đọc thông điệp mới kết luận
được.

---

## 4. Cấu trúc báo cáo `Report5_Unit Test_CareBridge.xlsx`

83 sheet = **5 sheet hệ thống** + **78 sheet use case**.

### 4.1 Năm sheet hệ thống

| Sheet | Nội dung |
|---|---|
| `Guideline` | Hướng dẫn đọc, đặt ngay trong workbook cho người chấm |
| `Cover` | Thông tin tài liệu + bảng *Record of change* |
| `MethodList` | 78 use case, kèm module, tên sheet (bấm được), và **file test nguồn** |
| `Statistics` | Bảng tổng hợp Passed/Failed/Untested và N/A/B từng use case |
| `Defects` | Phần A: lỗi đã sửa. Phần B: lỗi còn mở |

### 4.2 Một sheet use case đọc thế nào

Mỗi **cột** `UTCID01`, `UTCID02`, … là **một test case** (một method JUnit / một `test()` của
flutter_test hoặc Vitest). Mỗi **hàng** ở cột D là một điều kiện hoặc một kỳ vọng. Dấu **`O`** ở ô
giao nhau nghĩa là: *test case ở cột này có điều kiện/kỳ vọng ở hàng này*.

Bố cục cố định theo dòng:

| Dòng | Nội dung |
|---|---|
| 1 | `Code Module` (nhóm chức năng) — `Use Case` (tên use case) |
| 2 | `Created By` — `Executed By` |
| 3 | `Test requirement` — **phát biểu yêu cầu**: use case này phải làm đúng điều gì |
| 4–5 | Passed / Failed / Untested / N / A / B / Total Test Cases |
| 7 | Dãy `UTCIDxx` (xoay dọc) |
| 8+ | Khối **Condition** |
| … | Khối **Confirm** |
| cuối | Khối **Result** |

**Khối Condition** gồm ba nhóm ở cột B:

- `Precondition` — tiền đề chung: dependency được mock, chạy trong bộ nhớ, không chạm DB/mạng thật.
- `Test target` — class hoặc file test chứa test case đó.
- `Input condition` — điều kiện đầu vào của test case.

**Khối Confirm** gồm hai nhóm:

- `Expected result` — kết quả kỳ vọng.
- `Actual` — kết quả thực tế: `Đúng kỳ vọng` / `Sai kỳ vọng — xem Defect ID` / `Không thực thi (skipped)`.

**Khối Result**:

- `Type` — N / A / B (xem 4.3).
- `Passed/Failed` — `P` (xanh) / `F` (đỏ) / `U` (vàng, skipped).
- `Executed Date` — ngày chạy.
- `Defect ID` — mã defect nếu test case đó **từng phát hiện lỗi**, kể cả lỗi nay đã sửa xong.

### 4.3 Phân loại N / A / B

| Loại | Nghĩa | Ví dụ trong dự án |
|---|---|---|
| **N** — Normal | Luồng hợp lệ, dữ liệu trong miền bình thường | `list_asSystemAdmin_shouldReturn200` |
| **A** — Abnormal | Sai dữ liệu, thiếu quyền, không tìm thấy, xung đột, ném exception | `allEndpoints_asNonSystemAdmin_shouldReturn403` |
| **B** — Boundary | Giá trị **biên** | `getDirectory_qExactly100Chars_accepted`, `searchContent_withKeyword101Chars_shouldReturn400` |

> Cột này được suy ra **tự động từ tên test** bằng biểu thức chính quy trong
> `report-tools/gen_report.py` (`BOUNDARY`, `ABNORMAL`, `LAYER_BOUNDARY`). Đây là suy đoán, không phải khai
> báo. Hai bẫy đã xử lý, cần giữ khi sửa regex:
> - `serviceBoundary`, `requestBoundary`, `jsonBoundary` là **ranh giới kiến trúc**, không phải biên
>   giá trị → `LAYER_BOUNDARY` loại chúng ra trước khi so khớp.
> - `auditLoggedExactlyOnce`, `hasExactlyFiveValues` là **bất biến đếm**, không phải biên → chữ
>   `exactly` chỉ tính là biên khi đứng cạnh số hoặc `now` (`exactly100Chars`, `dueAtExactlyNow`).
>
> Nếu thấy một test bị xếp sai loại, sửa regex chứ đừng sửa tay file Excel — lần sinh lại sẽ mất.

### 4.4 Ý nghĩa "Test requirement" (dòng 3)

Đây là **phát biểu yêu cầu**, viết tay, nằm trong `REQUIREMENTS` ở `report-tools/usecases.py`.
Không phải danh sách file test (danh sách đó ở `MethodList` cột E và trong `linkFileTest.md`).

⚠️ Một số phát biểu khẳng định về **phân quyền** (`chỉ SYSTEM_ADMIN…`, `chỉ tác giả…`). Khi sửa,
phải đối chiếu với `@PreAuthorize` thật và với chính các test trong sheet đó — viết sai ở đây là
ghi một khẳng định bảo mật sai vào tài liệu nộp.

### 4.5 Sheet `Defects`

**Phần A — đã sửa trong đợt này.** Các defect do chính unit test phát hiện:

| Mã | Lỗi | Test bắt được |
|---|---|---|
| `DEF-RBAC-002` | `AiModerationAdminController` cho non-admin đọc AI policy | `AiModerationAdminControllerSecurityTest#listPolicies_asNonSystemAdmin_returns403` |
| `DEF-DATA-003` | `GrowthMeasurementFormScreen` gửi `note: ""` đè lên `null` | `growth_measurement_form_test.dart :: edit preserves a null note` |

Mã defect xuất hiện đồng thời ở ô `Defect ID` của đúng những UTCID đã phát hiện ra chúng.
**Revert bản sửa thì các test case đó đỏ lại** — đó là bằng chứng defect có thật.

**Phần B — còn mở.** Sinh tự động từ các test đang `failed`. Hiện đang trống.

### 4.6 Cái gì KHÔNG vào báo cáo

- Class `*IntegrationTest` / `*PostgresTest` / `*EmbeddedPostgresTest` / `*SmokeTest` — cần Docker
  hoặc PostgreSQL nhúng, thuộc báo cáo Integration/E2E.
- Class nào lỗi vì Testcontainers dù tên không nói vậy — `collect.py` nhận diện qua thông điệp lỗi.
- Năm nhóm hạ tầng xuyên suốt không ứng với use case nào (khai báo ở `EXCLUDED_USE_CASES`):
  `Platform & Configuration` (nạp `.env`, exception handler, migration contract),
  `Audit & Security Incident`, `Firebase Realtime Bridge`, `Search`, `Checklist Retention Ops`.

Con số: chạy ra 5.102 test → 4.831 là unit test → 4.568 vào báo cáo. Chi tiết ở mục 9.

---

## 5. Cập nhật báo cáo sau khi chạy lại test

Bốn bước, chạy từ thư mục `06_Testing/UnitTesting/report-tools/`:

```bash
cd "$(git rev-parse --show-toplevel)"

# 1. Backend — surefire tự ghi XML vào target/surefire-reports/, không cần chuyển hướng
cd 05_Development/CareBridgeAPI && ./mvnw test; cd -

# 2. Mobile
cd 05_Development/CareBridgeMobileApp \
  && flutter test --machine > ../../06_Testing/UnitTesting/report-tools/mobile-test.json; cd -

# 3. Web
cd 05_Development/CareBridgeWebApp \
  && npx vitest run --reporter=json --outputFile=../../06_Testing/UnitTesting/report-tools/web-test.json; cd -

# 4. Gộp kết quả rồi sinh workbook
cd 06_Testing/UnitTesting/report-tools
python3 collect.py      # -> all-tests.json
python3 gen_report.py   # -> ../Report5_Unit Test_CareBridge.xlsx
```

Bước 2 và 3 **vẫn phải chạy dù test đỏ** — `flutter test` / `vitest` trả exit code khác 0 khi có
test hỏng, nhưng file JSON vẫn được ghi đầy đủ. Đừng để `set -e` cắt ngang.

`collect.py` in ra bảng kiểm nhanh:

```
rows: 5102
by layer: Counter({'backend': 4031, 'mobile': 934, 'web': 137})
unit only: Counter({'passed': 4828, 'untested': 3})
integration: Counter({'failed': 142, 'untested': 67, 'passed': 62})
```

Dòng `integration` là các class cần Docker — `failed: 142` khi Docker tắt là bình thường và
chúng không vào báo cáo.

`gen_report.py` in ra:

```
sheets=78 open_defects=0 fixed_defects=3
grand: {'passed': 4567, 'failed': 0, 'untested': 1, 'N': 2442, 'A': 1921, 'B': 205, 'total': 4568}
```

> Ba file kết quả (`all-tests.json`, `mobile-test.json`, `web-test.json`) được giữ lại trong
> `report-tools/` như một **ảnh chụp của lần chạy gần nhất**. Nhờ đó chỉ cần `python3 gen_report.py` là
> sinh lại workbook trong vài giây, không phải chạy lại cả ba bộ test.
>
> Ngược lại, `collect.py` đọc `mobile-test.json` và `web-test.json` **từ chính thư mục `report-tools/`**.
> Nếu hai file đó thiếu, script vẫn chạy nhưng chỉ gộp phần backend — báo cáo sẽ thiếu mobile và web
> mà không báo lỗi. Luôn đối chiếu dòng `by layer:` trước khi sinh workbook.

### Nếu script báo lỗi

| Thông báo | Nguyên nhân | Xử lý |
|---|---|---|
| `unmapped test container: com.carebridge...` | Có class test mới **không khớp regex nào** | Thêm regex vào `USE_CASES` — mục 6 |
| `FIXED_DEFECTS not matched to any test case: {...}` | Tên test trong `FIXED_DEFECTS` không còn khớp (đã đổi tên test) | Cập nhật khoá `match` |
| `AssertionError` ở `stats[...]` | Một test có trạng thái ngoài passed/failed/untested | Kiểm tra lại file JSON đầu vào |
| `ModuleNotFoundError: openpyxl` | Thiếu thư viện | `pip3 install openpyxl` |

---

## 6. Thêm test mới và đưa vào báo cáo

**Bước 1 — viết test** theo quy ước đặt tên của tầng tương ứng, vì báo cáo diễn giải cột
`Input condition` / `Expected result` **từ tên test**. Tên đặt cẩu thả thì báo cáo đọc vô nghĩa.

Backend — ba đoạn ngăn bằng `_`:

```java
// method_điềukiện_kỳvọng          (phổ biến nhất)
void login_wrongPassword_throwsInvalidCredentials()

// method_kỳvọng_whenĐiềukiện      (script tự nhận ra và đảo lại cho đúng cột)
void isComplexEnough_shouldReturnFalse_whenMissingDigit()
```

Mobile / Web — `group()` mô tả điều kiện, `test()`/`it()` mô tả kỳ vọng:

```dart
group('edit an existing measurement', () {
  testWidgets('preserves a null note when it is unchanged', (tester) async { ... });
});
```

**Bước 2 — gán use case.** Mở `report-tools/usecases.py`, tìm hoặc thêm mục trong `USE_CASES`:

```python
("Tên sheet", "Tên module", r"regex khớp với container"),
```

`container` là **FQCN** với backend (`com.carebridge.backend.security.service.AuthServiceLoginTest`)
và **đường dẫn tương đối repo** với mobile/web (`05_Development/CareBridgeMobileApp/test/...`).

⚠️ **Mục nào khớp trước thì thắng** — đặt regex hẹp lên trên regex rộng.

Script chỉ dừng và báo `unmapped test container` khi class mới **không khớp regex nào**. Nếu tên
class lỡ khớp một regex rộng nằm phía trên, nó sẽ **im lặng bị hút** vào use case đó — không có
cảnh báo. Sau khi thêm test, mở sheet `MethodList` cột *Source test files* và kiểm class mới có nằm
đúng use case không.

**Bước 3 — viết phát biểu yêu cầu** cho use case mới vào `REQUIREMENTS` (cùng file). Thiếu sẽ báo
`KeyError` ngay khi sinh.

**Bước 4** — chạy lại mục 5.

**Bước 5 — cập nhật `linkFileTest.md`** để bản đồ tính năng ↔ file test không bị lệch.

### Ghi nhận một defect mới

Khi test bắt được lỗi thật và bạn sửa code ứng dụng, thêm một mục vào `FIXED_DEFECTS` trong
`report-tools/gen_report.py`:

```python
{
    "id": "DEF-XXX-00N",
    "match": ("TênClassTest", "tiền_tố_tên_test"),   # test sẽ đỏ lại nếu revert bản sửa
    "severity": "Cao — RBAC",
    "component": "đường/dẫn/tới/File.java",
    "symptom": "Người dùng thấy gì khi lỗi xảy ra",
    "cause": "Nguyên nhân gốc, kèm commit nếu truy được",
    "fix": "Đã sửa thế nào",
}
```

Nếu test **vẫn đang đỏ** (chưa sửa được, hoặc cố ý giữ đỏ chờ xác nhận), đừng dùng `FIXED_DEFECTS` —
nó sẽ tự lên phần B của sheet `Defects`. Muốn ghi chú nguyên nhân thì thêm vào `KNOWN_DEFECT_NOTES`.

---

## 8. Bẫy hay gặp

**Backend**

- `Errors` cao bất thường khi chạy `./mvnw test` → Docker đang tắt. Đúng như thiết kế; các class đó
  không tính vào báo cáo. Bật Docker Desktop nếu muốn chạy cả integration test.
- Dùng `@MockitoBean`, **không** phải `@MockBean` (đã bỏ ở Spring Boot 4).
- `@WebMvcTest` nhập từ `org.springframework.boot.webmvc.test.autoconfigure`.
- Test bảo mật kỳ vọng **403** phải gửi **body hợp lệ**: `@Valid` chạy *trước* method security, body
  sai sẽ ra 400 và che mất phép kiểm tra phân quyền.
- `UnnecessaryStubbingException` sau khi sửa code → stub trỏ tới method đã đổi tên; sửa test theo
  code chứ đừng tắt strict stubbing.

**Mobile**

- Viewport mặc định của widget test là 800×600, widget rơi ngoài vùng render nên `tap()` trượt và
  `find` không thấy gì. Đặt `tester.view.physicalSize = const Size(800, 1600)` trong `setUp`.
- `flutter test` **sinh lại** `GeneratedPluginRegistrant` mỗi lần chạy. Các file này đã được
  `git rm --cached` và cho vào `.gitignore`, nên `git status` sẽ không còn bẩn.

**Web**

- Node ≥ 22 có biến toàn cục `localStorage` thử nghiệm che mất `localStorage` của jsdom và **thiếu
  `setItem`**, làm hỏng `persist` của zustand. `vitest.setup.ts` gắn lại đúng đối tượng Storage —
  đừng gỡ dòng `setupFiles` trong `vite.config.ts`.

---

## 9. Tóm tắt số liệu hiện tại

| | Backend | Mobile | Web | Tổng |
|---|---:|---:|---:|---:|
| Test chạy | 4.031 | 934 | 137 | 5.102 |
| Là unit test | 3.760 | 934 | 137 | 4.831 |
| Vào báo cáo | 3.527 | 915 | 126 | **4.568** |
| Passed | | | | **4.567** |
| Failed | | | | **0** |
| Skipped | | | | **1** |
| N / A / B | | | | 2.442 / 1.921 / 205 |

Chênh lệch **5.102 → 4.831**: loại integration test (cần Docker/PostgreSQL nhúng).
Chênh lệch **4.831 → 4.568**: loại 263 TC thuộc 5 nhóm hạ tầng xuyên suốt ở mục 4.6 —
`Platform & Configuration` 78, `Audit & Security Incident` 76, `Firebase Realtime Bridge` 47,
`Search` 38, `Checklist Retention Ops` 24.

78 use case · 3 defect đã phát hiện và sửa · 0 defect còn mở.
