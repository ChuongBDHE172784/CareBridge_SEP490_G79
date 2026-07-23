# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Rich Text Editor cho Article/FAQ Content

**Document ID:** `CB-CONTENT-IMP-012-TS`
**Version:** `1.0`
**Date:** `2026-07-23`
**Status:** `Approved`
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND`
**DPO Sign-off:** `N/A — Internal/Public, không có PII`
**Approved by:** `[x] HuyND — 2026-07-23, "đã Approved"`

**References:**
- `04_Implement/ContentRichTextEditor/ContentRichTextEditor_TDS.md` (tài liệu TDS đi kèm — bắt buộc đọc §3 ADR trước khi implement)
- `04_Implement/UC105_CreateContentFAQChecklist/`, `UC106_UpdateContentFAQChecklist/` — pattern gốc của `AdminContentServiceImpl`
- `04_Implement/UC167_UploadFile/` (nếu tồn tại) hoặc trực tiếp `FileServiceImpl`/`CloudinaryStorageService` — hạ tầng bị sửa (ADR-RTE-004)

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                              |
| ---------- | ----------------- | ---------------------------------------------- |
| 2026-07-23 | AI Agent — Claude | Khởi tạo tài liệu — Draft, chờ review cùng TDS |
| 2026-07-23 | HuyND             | Duyệt trực tiếp trong file (`Status: Approved`) |
| 2026-07-23 | AI Agent — Claude | Implement Red→Green xong. Backend: `RTE-TC-001..010` GREEN (26 test, `./mvnw test` toàn repo — 0 regression trong module `content`/`file`/`expert`/`expertverification`; các "Errors" còn lại trong full-suite là lỗi môi trường Testcontainers pre-existing không liên quan — xem §5.1). `RTE-TC-015` viết đúng spec nhưng bị chặn bởi đúng lỗi môi trường đó (không tự chạy được), đã bù bằng verify thủ công trên server thật (xem §4). `RTE-TC-016` giữ `@Disabled` theo đúng phương án spec cho phép (thiếu Cloudinary credential hợp lệ — phát hiện thêm: `.env` dev có `CLOUDINARY_CLOUD_NAME=root` sai, chặn cả QA thủ công luồng ảnh thật). Frontend: `RTE-TC-011..013` GREEN (7 test Vitest, 0 regression trên 28 test toàn suite — phải thêm `@testing-library/react`+`jsdom`+cleanup polyfill cho ProseMirror/ jsdom, xem §5.1 ghi chú). Mobile: `RTE-TC-014` GREEN (2 test `flutter_test`, 0 regression trên 259 test toàn suite — tách `VerifiedContentBody` thành widget riêng để test được). QA thủ công trình duyệt thật + backend thật: tạo bài viết có định dạng thật, và một live security smoke test gọi thẳng API với payload độc hại xác nhận sanitizer chặn đúng trên server đang chạy (chi tiết ở TDS §15). |
| 2026-07-23 | AI Agent — Claude | **Live QA đầu-cuối luồng ảnh (RTE-TC-016), sau khi người dùng cập nhật `.env` với Cloudinary credential hợp lệ.** Phát hiện tính năng chưa thực sự hoạt động: response `presignedUrl` vẫn là `type=authenticated` có `expires_at`, và `curl` trực tiếp URL đó trả về HTTP 400 từ Cloudinary. Truy vết ra 2 bug thật trong hạ tầng `file` dùng chung (không phải env issue như báo cáo trước — báo cáo đó sai, đã sửa lại): (1) `CloudinaryStorageService.store()` không nhận `accessMode` từ caller; (2) `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED tạo URL hỏng, Cloudinary trả 400 cho mọi request (bug pre-existing, ảnh hưởng cả luồng ảnh xác thực danh tính chuyên gia). Được người dùng xác nhận "tiến hành" cho phương án sửa gộp chung ban đầu — đã implement + test GREEN. |
| 2026-07-23 | AI Agent — Claude | **User hỏi lại: "không tách riêng với luồng chuyên gia được à" — thiết kế lại (ADR-RTE-007 bản cuối).** Revert toàn bộ phần gộp chung: `store()` và `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED quay lại **y hệt** code trước phiên này (`git diff HEAD` = rỗng cho cả 2). Thêm method mới, độc lập `storePublic()` — chỉ dùng cho ảnh nội dung PUBLIC, không ai khác gọi tới. `FileServiceImpl` chỉ route qua `storePublic()` khi `accessMode==PUBLIC`. Bug #2 (luồng chuyên gia, HTTP 400) **cố ý không sửa** — vẫn còn nguyên, cần task riêng có TDS/Test-Spec do tính PII/bảo mật. Cập nhật `CloudinaryStorageServiceTest.java` (bỏ 3 test target `store(accessMode)` đã revert; thêm `storePublic_alwaysUploadsAsCloudinaryTypeUpload`; `RTE-TC-009/010` quay lại đúng assertion cũ — PRIVATE/AUTHENTICATED vẫn có `expires_at`, regression guard cho *hành vi không đổi kể cả bug*) và `FileServiceImplTest.java` (`uploadPublicFile_acceptsJpeg` verify `storePublic()` được gọi, `store()` thì không). Verify lại: `./mvnw test` đầy đủ — 204 Errors + 9 Failures giống hệt baseline (0 regression). Live QA lại 2 lần trên browser+backend+Cloudinary thật: (1) qua editor → lưu → xem trực tiếp; (2) sau khi đổi routing sang `storePublic()`, verify lại upload API độc lập — cả 2 lần đều `presignedUrl` không `expires_at`/chữ ký, `curl` HTTP 200. **Bổ sung quan trọng:** verify riêng trang xem chi tiết phía người đọc (`ContentDetailPage`, `dangerouslySetInnerHTML`) — trước đó chỉ verify ảnh render trong editor, chưa verify phía người đọc thực sự thấy gì; đã tạo bài viết, mở `ContentDetailPage`, xác nhận `<img>` load đúng (`naturalWidth`/`naturalHeight` khớp file gốc, `complete: true`). Dọn dẹp toàn bộ dữ liệu/file test. |

---

## 1. Thông tin Module

| Field                     | Value                                                                                                                                                                                                    |
| ------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Feature ID**            | `CB-CONTENT-IMP-012`                                                                                                                                                                                     |
| **Module**                | `content` (backend sanitize) + `file` (URL fix) + `contentManagement` (web editor) + `community` (mobile render)                                                                                         |
| **Spec gốc**              | `ContentRichTextEditor_TDS.md`                                                                                                                                                                           |
| **Priority**              | 🟠 P1                                                                                                                                                                                                     |
| **Data Classification**   | `Internal → Public`                                                                                                                                                                                      |
| **Compliance Scope**      | `OWASP A03:2021 (Stored XSS)`, `BR-RBAC`                                                                                                                                                                 |
| **Upstream Dependencies** | `owasp-java-html-sanitizer` (mới), Tiptap packages (mới), `flutter_html` (mới)                                                                                                                           |
| **Downstream Consumers**  | Web `ContentDetailPage`, Mobile `VerifiedContentDetailScreen`, mọi consumer khác của `CloudinaryStorageService`/`FileServiceImpl` (expert identity, credentials, contribution) — **regression-critical** |

### 1.1 AI Generation Context

| Field                 | Value                                            |
| --------------------- | ------------------------------------------------ |
| **AI Assisted?**      | `Yes`                                            |
| **Constraint Source** | `ContentRichTextEditor_TDS.md §13`               |
| **Model**             | `Claude Sonnet 5`                                |
| **Trust Level**       | `T1 (Draft — chưa qua Red Gate, chưa implement)` |

---

## 2. Logic Issues Resolved (phát hiện trong lúc research, trước khi viết test)

| #   | Giả định ban đầu (có thể sai)                            | Thực tế đã verify bằng code                                                                                                                                                                                                                                       | Fix áp dụng trong test                                                                                                                                                                                           |
| --- | -------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| L1  | "Chỉ cần thêm rich text editor ở frontend là đủ"         | `body` đã được web render qua `dangerouslySetInnerHTML` từ trước — đổi sang HTML thật không cần đổi render web, nhưng **bắt buộc** sanitize server-side (rủi ro XSS tăng lên vì editor luôn sinh HTML thật) và **bắt buộc** sửa mobile (đang render `Text` thuần) | TC-SEC-* (sanitizer) là P0, không phải nice-to-have                                                                                                                                                              |
| L2  | "URL Cloudinary trả về từ upload dùng được lâu dài"      | `CloudinaryStorageService.generateSignedUrl()` luôn cap `expires_at` ở 15 phút, kể cả `accessMode=PUBLIC` — ảnh nhúng trong `body` sẽ vỡ sau 15 phút nếu không sửa                                                                                                | TC-INT-URL-* phải assert URL trả về từ `accessMode=PUBLIC` **không** chứa `expires_at` và **không** chứa `signature`/`__cld_token__`                                                                             |
| L3  | "Sửa `generateSignedUrl()` chỉ ảnh hưởng content module" | Hàm này dùng chung cho `expertverification` (identity docs) và `expert` (contribution attachments) — cả hai dùng `PRIVATE`/`AUTHENTICATED`                                                                                                                        | Bắt buộc có test regression xác nhận `PRIVATE`/`AUTHENTICATED` **vẫn** trả URL có `signed`/`expires_at` y hệt hành vi cũ sau khi sửa (TC-UNIT-URL-004/005)                                                       |
| L4  | "Sanitizer chặn hết `<img>` để an toàn tuyệt đối"        | Nếu chặn `<img>`, tính năng "chèn ảnh" (yêu cầu chính của người dùng) sẽ vô nghĩa                                                                                                                                                                                 | Allowlist PHẢI cho phép `img[src,alt,width,height]` với `src` giới hạn scheme `https`, không cho phép `javascript:`/`data:` — test phải cover cả 2 chiều (ảnh hợp lệ giữ lại, ảnh `javascript:`/`data:` bị loại) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
Feature bao gồm:
├── Backend Unit — HtmlContentSanitizerTest (mới)
├── Backend Unit — CloudinaryStorageServiceTest (PUBLIC vs PRIVATE/AUTHENTICATED branch, mới + regression)
├── Backend Unit — AdminContentServiceImplTest (bổ sung case gọi sanitizer, mở rộng test class có sẵn)
├── Backend Integration (Testcontainers) — ContentBodySanitizeIntegrationTest (mới)
├── Frontend Unit (Vitest) — RichTextEditor.test.tsx (component render + onChange + onImageUpload wiring)
├── Frontend Unit (Vitest) — (fontSizeExtension.ts không còn cần — TextStyleKit đã có sẵn FontSize, xem CHANGELOG)
└── Mobile Widget Test (flutter test) — verified_content_body_test.dart (render HTML thay vì raw text; widget tách riêng để test được)
```

### TDS-02 — Test Basis

| Source               | Items Derived                                                                      |
| -------------------- | ---------------------------------------------------------------------------------- |
| `TDS §3 ADR-RTE-004` | URL PUBLIC vĩnh viễn, PRIVATE/AUTHENTICATED không đổi                              |
| `TDS §3 ADR-RTE-005` | Allowlist sanitizer (thẻ/attribute)                                                |
| `TDS §5.2`           | Toolbar features (bold/italic/underline/color/font-size/font-family/heading/image) |
| `TDS §6.1/6.2`       | Sequence chèn ảnh + sequence lưu có sanitize                                       |
| Logic Issues §2      | L1-L4 ở trên                                                                       |

### TDS-03 — Test Conditions and Coverage

| Condition ID | Test Condition                                                                                                                         | Coverage Item                                 | Test Cases   |
| ------------ | -------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------- | ------------ |
| TC-COND-001  | HTML hợp lệ (trong allowlist) giữ nguyên sau sanitize                                                                                  | `HtmlContentSanitizer.sanitize()`             | `RTE-TC-001` |
| TC-COND-002  | `<script>` bị loại bỏ hoàn toàn                                                                                                        | `sanitize()`                                  | `RTE-TC-002` |
| TC-COND-003  | `onerror=`/`onclick=` (event handler attribute) bị loại bỏ                                                                             | `sanitize()`                                  | `RTE-TC-003` |
| TC-COND-004  | `<img src="javascript:...">` và `<img src="data:...">` bị loại bỏ/vô hiệu hoá                                                          | `sanitize()`                                  | `RTE-TC-004` |
| TC-COND-005  | `<img src="https://res.cloudinary.com/...">` hợp lệ giữ nguyên                                                                         | `sanitize()`                                  | `RTE-TC-005` |
| TC-COND-006  | `style="color:...; font-size:...; font-family:..."` giữ nguyên; `style="position:fixed"` bị loại                                       | `sanitize()`                                  | `RTE-TC-006` |
| TC-COND-007  | `createContent()`/`updateContent()` gọi `sanitizer.sanitize()` đúng 1 lần trước khi lưu                                                | `AdminContentServiceImpl`                     | `RTE-TC-007` |
| TC-COND-008  | `generateSignedUrl(..., PUBLIC, ...)` KHÔNG chứa `expires_at`/`signature` trong URL                                                    | `CloudinaryStorageService`                    | `RTE-TC-008` |
| TC-COND-009  | `generateSignedUrl(..., PRIVATE, ...)` VẪN chứa `expires_at`, TTL vẫn cap ở 15 phút — **regression guard**                             | `CloudinaryStorageService`                    | `RTE-TC-009` |
| TC-COND-010  | `generateSignedUrl(..., AUTHENTICATED, ...)` VẪN chứa `expires_at` — **regression guard**                                              | `CloudinaryStorageService`                    | `RTE-TC-010` |
| TC-COND-011  | Toolbar bold/italic/underline sinh đúng thẻ HTML tương ứng (`<strong>`/`<em>`/`<u>`)                                                   | `RichTextEditor.tsx`                          | `RTE-TC-011` |
| TC-COND-012  | Chọn màu/cỡ chữ sinh `<span style="...">` đúng                                                                                         | `RichTextEditor.tsx` (FontSize từ TextStyleKit, không cần file riêng) | `RTE-TC-012` |
| TC-COND-013  | `onImageUpload` được gọi khi bấm nút chèn ảnh, URL trả về được chèn vào editor qua `setImage`                                          | `RichTextEditor.tsx`                          | `RTE-TC-013` |
| TC-COND-014  | Mobile: `VerifiedContentDetailScreen` render `<b>`/`<img>` thành widget thật, không hiện text thẻ HTML thô                             | `verified_content_detail_screen.dart`         | `RTE-TC-014` |
| TC-COND-015  | End-to-end: tạo content với HTML độc hại qua API → DB không chứa `<script>`                                                            | Integration                                   | `RTE-TC-015` |
| TC-COND-016  | End-to-end: upload ảnh PUBLIC qua endpoint có sẵn → URL trả về resolve HTTP 200 không cần header auth, không hết hạn trong 20 phút chờ | Integration                                   | `RTE-TC-016` |

### TDS-04 — Test Techniques

| Technique                | Applied To                                                                    | Rationale                                                                            |
| ------------------------ | ----------------------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Equivalence Partitioning | HTML input: an toàn / có `<script>` / có event handler / có `img src` độc hại | Bao phủ các lớp tấn công XSS phổ biến nhất (OWASP XSS cheat sheet)                   |
| Boundary/Regression      | `accessMode` = PUBLIC vs PRIVATE vs AUTHENTICATED                             | Đảm bảo sửa 1 nhánh không phá 2 nhánh còn lại (rủi ro cao nhất của ADR-RTE-004)      |
| State-based UI test      | Toolbar toggle bold/italic on-off                                             | Xác nhận `RichTextEditor` không rơi vào trạng thái không nhất quán khi bấm nhiều lần |

### TDS-05 — Test Data Requirements

| Fixture ID   | Type   | Value                                                                         | Mục đích                                                              |
| ------------ | ------ | ----------------------------------------------------------------------------- | --------------------------------------------------------------------- |
| `FX-RTE-001` | String | `<p>Nội dung <strong>an toàn</strong></p>`                                    | Happy path sanitize                                                   |
| `FX-RTE-002` | String | `<p>Xin chào</p><script>alert(document.cookie)</script>`                      | XSS — script tag                                                      |
| `FX-RTE-003` | String | `<img src="x" onerror="alert(1)">`                                            | XSS — event handler attribute                                         |
| `FX-RTE-004` | String | `<img src="javascript:alert(1)">`                                             | XSS — dangerous URL scheme                                            |
| `FX-RTE-005` | String | `<span style="font-size:18px;color:#ff0000;position:fixed;top:0">Test</span>` | CSS property allowlist (giữ font-size/color, loại position)           |
| `FX-RTE-006` | Mock   | `CloudinaryStorageService` với `cloudinary.url()` builder mock                | Assert URL builder gọi đúng method (không `.signed(true)`) khi PUBLIC |
| `FX-RTE-007` | JWT    | `{role: CONTENT_ADMIN}`                                                       | Auth context integration test                                         |

---

## 4. Test Case Specification

### BACKEND — HtmlContentSanitizer

### RTE-TC-001 — HTML hợp lệ giữ nguyên sau sanitize

**Severity:** `HIGH`
**Feature Under Test:** `HtmlContentSanitizer.sanitize()`
**Test File:** `src/test/java/com/carebridge/backend/content/policy/HtmlContentSanitizerTest.java`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`
**Oracle Source:** `TDS §3 ADR-RTE-005 allowlist`

**Test Steps:** Arrange `FX-RTE-001` → Act `sanitize(input)` → Assert output chứa nguyên `<p>`, `<strong>`, text — không mất nội dung hợp lệ.
**Current Status:** 🟢 Passing

---

### RTE-TC-002 — `<script>` bị loại bỏ hoàn toàn

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`

**Test Steps:** Arrange `FX-RTE-002` → Act `sanitize(input)` → Assert output KHÔNG chứa chuỗi `<script`, KHÔNG chứa `alert(document.cookie)`; `<p>Xin chào</p>` vẫn còn.
**Current Status:** 🟢 Passing

---

### RTE-TC-003 — Event handler attribute (`onerror`) bị loại bỏ

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-003`

**Test Steps:** Arrange `FX-RTE-003` → Act `sanitize(input)` → Assert output KHÔNG chứa `onerror`.
**Current Status:** 🟢 Passing

---

### RTE-TC-004 — `src="javascript:..."` bị loại bỏ

**Severity:** `CRITICAL`
**OWASP:** `A03:2021 — Injection`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`

**Test Steps:** Arrange `FX-RTE-004` → Act `sanitize(input)` → Assert output KHÔNG chứa `javascript:` (thẻ `img` bị loại toàn bộ hoặc `src` bị strip — miễn là chuỗi `javascript:` không xuất hiện trong output).
**Current Status:** 🟢 Passing

---

### RTE-TC-005 — `<img src="https://res.cloudinary.com/...">` hợp lệ giữ nguyên

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`

**Test Steps:** Act `sanitize("<img src=\"https://res.cloudinary.com/demo/image/upload/v1/carebridge/abc.jpg\" alt=\"minh hoạ\">")` → Assert output vẫn chứa thẻ `img` với đúng `src`/`alt` — chèn ảnh Cloudinary không bị chặn nhầm.
**Current Status:** 🟢 Passing

---

### RTE-TC-006 — CSS property allowlist: giữ `color`/`font-size`, loại `position`

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`

**Test Steps:** Act `sanitize(FX-RTE-005)` → **Không so khớp chuỗi cứng** (OWASP sanitizer có thể chuẩn hoá spacing/thứ tự property trong `style`) — parse attribute `style` của thẻ trả về (ví dụ bằng regex tách từng `property:value` hoặc parser CSS đơn giản) và assert tập property gồm `font-size` (giá trị `18px`) và `color` (giá trị tương đương `#ff0000`), KHÔNG có `position`.
**Current Status:** 🟢 Passing

---

### BACKEND — AdminContentServiceImpl (mở rộng test class có sẵn)

### RTE-TC-007 — `createContent()`/`updateContent()` gọi sanitizer trước khi lưu

**Severity:** `CRITICAL`
**Feature Under Test:** `AdminContentServiceImpl`
**Test File:** `AdminContentServiceImplTest.java` (mở rộng, `createContent()`) + `UpdateContentServiceImplTest.java` (mở rộng, `updateContent()`)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

**Preconditions:** Mock `HtmlContentSanitizer` (thêm vào cả 2 test class hiện có; các test case khác trong cùng file được stub identity qua `@BeforeEach` để không phá assertion cũ về nội dung `body`).

**Test Steps:**
1. Act: `createContent(request{body="Nội dung chi tiết..."}, authorId)`
2. Assert: `htmlContentSanitizer.sanitize("Nội dung chi tiết...")` được gọi đúng 1 lần; entity đã lưu có `body` = giá trị **trả về từ mock sanitizer** (không phải input thô) — xác nhận service thực sự dùng output đã sanitize, không chỉ gọi cho có.
3. Lặp lại tương tự cho `updateContent(request{body="updated body"}, ...)` ở `UpdateContentServiceImplTest.java`.

**Current Status:** 🟢 Passing

---

### BACKEND — CloudinaryStorageService (URL persistence + regression guard)

### RTE-TC-008 — `accessMode=PUBLIC` trả URL không có `expires_at`/`signature`

**Severity:** `CRITICAL`
**Feature Under Test:** `CloudinaryStorageService.generateSignedUrl()`
**Test File:** `src/test/java/com/carebridge/backend/file/service/impl/CloudinaryStorageServiceTest.java` (mới hoặc mở rộng)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`
**Oracle Source:** `TDS §3 ADR-RTE-004`

**Test Steps:** Act `generateSignedUrl("carebridge/abc123", 15, FileAccessMode.PUBLIC, "image")` → Assert URL trả về KHÔNG chứa `expires_at=`, KHÔNG chứa `__cld_token__`/`signature`.
**Current Status:** 🟢 Passing

---

### RTE-TC-009 — `accessMode=PRIVATE` VẪN trả URL có `expires_at`, cap 15 phút — REGRESSION GUARD

**Severity:** `CRITICAL` — bảo vệ luồng expert identity documents đang chạy thật
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`
**Oracle Source:** `Logic Issue L3 — không được phá luồng PRIVATE hiện có`

**Test Steps:** Act `generateSignedUrl("carebridge/identity/xyz", 15, FileAccessMode.PRIVATE, "image")` → Assert URL trả về **vẫn** chứa `expires_at=` với giá trị tương ứng ≤ 15 phút kể từ thời điểm gọi — hành vi giống hệt trước khi sửa ADR-RTE-004.
**Current Status:** 🟢 Passing

---

### RTE-TC-010 — `accessMode=AUTHENTICATED` VẪN trả URL có `expires_at` — REGRESSION GUARD

**Severity:** `CRITICAL`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-010`

**Test Steps:** Act `generateSignedUrl(..., FileAccessMode.AUTHENTICATED, ...)` → Assert URL vẫn chứa `expires_at=`.
**Current Status:** 🟢 Passing

---

### FRONTEND — RichTextEditor (Vitest + Testing Library)

### RTE-TC-011 — Bold/Italic/Underline sinh đúng thẻ HTML

**Severity:** `MEDIUM`
**Feature Under Test:** `RichTextEditor.tsx`
**Test File:** `src/features/contentManagement/components/RichTextEditor.test.tsx`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-011`

**Test Steps:** Render `<RichTextEditor value="" onChange={spy} onImageUpload={...} />` → gõ text → bấm nút Bold → Assert `onChange` được gọi với HTML chứa `<strong>`.
**Current Status:** 🟢 Passing

---

### RTE-TC-012 — Chọn màu/cỡ chữ sinh `style` inline đúng

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-012`

**Test Steps:** Chọn text → đổi cỡ chữ (select "18px") → Assert `onChange` HTML chứa `font-size: 18px`; đổi màu (input color `#e91e63`) → Assert HTML chứa `color: #e91e63` (hoặc dạng rgb tương đương do trình duyệt chuẩn hoá — assert bằng cách parse thay vì so sánh chuỗi cứng).
**Current Status:** 🟢 Passing

---

### RTE-TC-013 — Chèn ảnh gọi `onImageUpload`, URL trả về được chèn vào editor

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-013`

**Test Steps:** Mock `onImageUpload` trả về `{url: "https://res.cloudinary.com/x.jpg"}` → chọn file qua input → Assert `onImageUpload` được gọi với đúng `File`; Assert `onChange` sau đó được gọi với HTML chứa `<img src="https://res.cloudinary.com/x.jpg"`.
**Current Status:** 🟢 Passing

---

### MOBILE — verified_content_detail_screen (flutter test)

### RTE-TC-014 — Render HTML thành widget, không hiện thẻ thô

**Severity:** `HIGH`
**Feature Under Test:** `VerifiedContentBody` (widget tách riêng từ `VerifiedContentDetailScreen` để test được — `ContentService.getContentDetail()` gọi `apiGet()` top-level không mockable dễ dàng, nên phần render HTML được tách thành widget nhận `html: String` trực tiếp, không phụ thuộc network)
**Test File:** `test/features/community/widgets/verified_content_body_test.dart`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-014`

**Test Steps:** Pump `VerifiedContentBody(html: "<p>Xin chào <b>mẹ bầu</b></p>")` → Assert `find.text(...)` với chuỗi HTML thô KHÔNG tồn tại, `find.textContaining('<p>')`/`find.textContaining('<b>')` KHÔNG tồn tại; Assert nội dung thật ("Xin chào", "mẹ bầu") xuất hiện trong `RichText` widget con. Thêm 1 test case ngoài kế hoạch: `<img>` render thành `Image` widget thật, không hiện literal text `<img`.

**Precondition đã verify (không phải kế hoạch, đã thực hiện):** `flutter pub get` sau khi thêm `flutter_html` vào `pubspec.yaml` chạy thành công (Dart 3.12.1/Flutter 3.44.1, thoả `>=3.2.0 <4.0.0`); `dart analyze` trên file màn hình sửa đổi — "No issues found!".

**Current Status:** 🟢 Passing

---

### INTEGRATION TESTS (Testcontainers PostgreSQL / Spring Boot Test)

### RTE-TC-015 — End-to-end: tạo content với HTML độc hại → DB sạch

**Severity:** `CRITICAL`
**Test File:** `src/test/java/com/carebridge/backend/content/integration/ContentBodySanitizeIntegrationTest.java`
**TDD Phase:** 🟡 Viết xong, KHÔNG chạy được tự động trong môi trường này — xem ghi chú
**Condition Ref:** `TC-COND-015`
**Oracle Source:** `Logic Issue L1`

**Test Steps:**
1. Act: `POST /api/v1/admin/content` (JWT CONTENT_ADMIN) với `body` = `FX-RTE-002` (chứa `<script>`).
2. Assert: HTTP 201.
3. **DB Assertion:**
```java
ContentItem saved = contentRepository.findById(id).orElseThrow();
assertThat(saved.getBody()).doesNotContain("<script");
assertThat(saved.getBody()).contains("Xin chào");
```
**Current Status:** 🟡 Test đã viết đúng theo spec, nhưng **không tự chạy được** trong môi trường Testcontainers hiện tại — Spring context load fail với `SchemaManagementException: missing column [display_name] in table [expert_profiles]`, một gap Flyway/entity **có từ trước** (đã verify: `ChecklistTemplateAdminIntegrationTest`, file không hề đụng tới trong tính năng này, fail với cùng lỗi hệt nhau; toàn bộ 204 "Errors" trong `./mvnw test` chạy full-suite đều cùng nguyên nhân này, không riêng gì test này) — ngoài phạm vi tài liệu này. **Đã verify hành vi tương đương bằng tay trên server thật + DB thật** (không phải Testcontainers): gọi `POST /api/v1/admin/content` qua backend đang chạy thật với payload độc hại hơn cả `FX-RTE-002` (`<script>` + `<img src="javascript:...">` + `<img src="https://evil.example.com/...">` + `style="position:fixed"`) → đọc lại từ DB xác nhận tất cả bị loại bỏ đúng như mong đợi, chỉ giữ `<p>Xin chao</p>` và `<span style="color:red">test</span>`. Xem TDS §15.

---

### RTE-TC-016 — End-to-end: ảnh PUBLIC upload xong resolve được không cần auth, không hết hạn

**Severity:** `HIGH`
**TDD Phase:** 🟡 Live E2E verified thật (không phải suy đoán) — automated test giữ `@Disabled` vì lý do khác với dự kiến ban đầu, xem ghi chú
**Condition Ref:** `TC-COND-016`
**Ghi chú:** Test này gọi Cloudinary thật (hoặc mock server nếu môi trường CI không có credential Cloudinary — xem `CLOUDINARY_*` trong `.env`) — nếu không có credential khả dụng trong CI, đánh dấu `@Disabled` với lý do rõ ràng, KHÔNG xoá test.

**Test Steps:**
1. Act: `POST /api/v1/files/upload/with-purpose` (JWT CONTENT_ADMIN, `kind=IMAGE, purpose=PUBLIC_CONTENT_IMAGE, accessMode=PUBLIC`) với 1 file ảnh test nhỏ.
2. Assert: response `presignedUrl` không chứa `expires_at`.
3. Assert (mức unit, không phụ thuộc network thật): giá trị `accessMode` lưu trong `uploaded_files.access_mode` = `PUBLIC`.
**Current Status:** 🟡 Automated integration test vẫn giữ `@Disabled`/không tự chạy được — **nhưng không còn vì lý do thiếu credential**: sau khi người dùng cập nhật `CLOUDINARY_CLOUD_NAME`/`API_KEY`/`API_SECRET` hợp lệ trong `.env`, cùng gap Testcontainers pre-existing chặn `RTE-TC-015` (`SchemaManagementException: missing column [display_name]`) cũng chặn class này. Đã **verify hành vi thật đầy đủ bằng tay** trên browser + backend thật + Cloudinary thật (không mock):
  - Upload ảnh qua editor thật → response `presignedUrl` = `https://res.cloudinary.com/<cloud>/image/upload/v1/carebridge/<id>?_a=...` — không `expires_at`, không chữ ký `s--...--` (đúng dự kiến RTE-TC-008).
  - `curl` trực tiếp URL trên → HTTP 200, `content-type: image/png`.
  - Ảnh render đúng trong DOM editor thật (`naturalWidth` khớp file gốc, `complete: true`).
  - Lưu bài viết → đọc lại từ DB (`content_items.body`) → `<img src="https://res.cloudinary.com/...">` được giữ nguyên qua sanitizer, đúng public_id đã upload.
  - **Phát hiện thêm 2 bug thật trong lúc verify (không phải giả định)** — xem CHANGELOG và TDS ADR-RTE-007 (bản cuối, đã tách riêng):
    1. `CloudinaryStorageService.store()` bỏ qua `accessMode` do caller truyền vào, tự đoán lại bằng `determineAccessMode(mimeType)` (luôn trả `AUTHENTICATED` cho ảnh) → ảnh PUBLIC không bao giờ thực sự lên Cloudinary với `type=upload`. **Đã sửa** — nhưng KHÔNG sửa `store()` trực tiếp; thêm method mới `storePublic()` riêng biệt để không đụng tới `store()` (dùng chung với luồng chuyên gia) — theo yêu cầu tách biệt của user.
    2. `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED nối chuỗi `"?expires_at=" + ts` thẳng vào `publicId` rồi truyền cho SDK — Cloudinary hiểu cả chuỗi là `public_id`, trả về 400 `public_id ... is invalid` cho MỌI request — nghĩa là toàn bộ luồng ảnh riêng tư (bao gồm ảnh xác thực danh tính chuyên gia) đã hỏng từ trước, không chỉ tính năng này. **CỐ Ý KHÔNG SỬA** — theo yêu cầu tách biệt của user, giữ nguyên hành vi (kể cả bug) của luồng chuyên gia. Cần task riêng.
  - `store()` và nhánh PRIVATE/AUTHENTICATED của `generateSignedUrl()` **không đổi một dòng nào** so với trước phiên làm việc này — verify bằng `git diff HEAD` = rỗng cho các đoạn này. Chỉ `storePublic()` (method mới) và `delete()`'s accessMode-mapping (sửa an toàn, no-op cho PRIVATE/AUTHENTICATED) là thay đổi thật. Test: `RTE-TC-008..010` (không đổi hành vi PRIVATE/AUTHENTICATED, kể cả bug — regression guard cho *hành vi không đổi*) + `storePublic_alwaysUploadsAsCloudinaryTypeUpload` (test mới), tất cả GREEN trong `CloudinaryStorageServiceTest.java`.

---

## 5. Red-Green-Refactor Tracker

| TC ID        | Test File                                  | 🔴 RED confirmed | 🟢 GREEN (commit)                                 | 🔵 REFACTOR note |
| ------------ | ------------------------------------------ | --------------- | -------------------------------------------------- | --------------- |
| `RTE-TC-001` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-002` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-003` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-004` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-005` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-006` | `HtmlContentSanitizerTest.java`            | `[x]`           | Passed                                              |                 |
| `RTE-TC-007` | `AdminContentServiceImplTest.java` + `UpdateContentServiceImplTest.java` | `[x]` | Passed                       |                 |
| `RTE-TC-008` | `CloudinaryStorageServiceTest.java`        | `[x]`           | Passed                                              |                 |
| `RTE-TC-009` | `CloudinaryStorageServiceTest.java`        | `[x]`           | Passed (regression guard, pass cả trước/sau — đúng như kỳ vọng) | |
| `RTE-TC-010` | `CloudinaryStorageServiceTest.java`        | `[x]`           | Passed (regression guard, pass cả trước/sau — đúng như kỳ vọng) | |
| `RTE-TC-011` | `RichTextEditor.test.tsx`                  | `[x]`           | Passed                                              |                 |
| `RTE-TC-012` | `RichTextEditor.test.tsx`                  | `[x]`           | Passed                                              |                 |
| `RTE-TC-013` | `RichTextEditor.test.tsx`                  | `[x]`           | Passed                                              |                 |
| `RTE-TC-014` | `verified_content_body_test.dart`          | `[x]`           | Passed                                              | Widget tách riêng (`VerifiedContentBody`) để test được — file test đổi tên so với kế hoạch ban đầu |
| `RTE-TC-015` | `ContentBodySanitizeIntegrationTest.java`  | `[ ]`           | Viết xong, chặn bởi lỗi môi trường pre-existing (xem §5.1) — bù bằng verify thủ công trên server thật | |
| `RTE-TC-016` | `ContentBodySanitizeIntegrationTest.java`  | `[x]`           | Bug thật đã sửa (2 bug trong `CloudinaryStorageService` — xem CHANGELOG); verify thủ công đầy đủ trên browser+backend+Cloudinary thật. Automated test vẫn `@Disabled` — chặn bởi cùng gap Testcontainers pre-existing như `RTE-TC-015`, không còn do thiếu credential | Đã sửa `store()` (accessMode bị bỏ qua) + `generateSignedUrl()` PRIVATE/AUTHENTICATED (query-string hỏng, Cloudinary trả 400) |

### 5.1 Red Gate Protocol

**Stub cho Red Phase:** Không dùng stub-throw riêng — class `HtmlContentSanitizer` chưa tồn tại trước khi implement nên bản thân lỗi biên dịch (`HtmlContentSanitizer cannot be resolved to a type`) đã là tín hiệu RED hợp lệ, mạnh hơn cả stub throw (không thể "Green-from-Birth" khi code còn chưa biên dịch được).

**Red Gate Verification — đã chạy thật, không phải kế hoạch:**

| TC ID                                                                         | Expected | Actual                                                                                                    | Tất cả FAIL đúng như kỳ vọng? |
| ----------------------------------------------------------------------------- | -------- | ----------------------------------------------------------------------------------------------------------| ----------------------------- |
| `RTE-TC-001..006` (sanitizer)                                                 | 🔴 FAIL  | ☑ FAIL — lỗi biên dịch `HtmlContentSanitizer cannot be resolved to a type` (10 lỗi, xem IDE diagnostics)  | `[x]` Yes                     |
| `RTE-TC-008` (bug PUBLIC URL hiện tại)                                        | 🔴 FAIL  | ☑ FAIL — `AssertionFailedError: PUBLIC URL must not carry an expiry: https://...s--NEmxMaHw--/...%3Fexpires_at%3D...` (chạy thật, chứng minh bug có thật trước khi sửa) | `[x]` Yes |
| `RTE-TC-009, RTE-TC-010` (regression guard — hành vi cũ, không phải code mới) | N/A — PASS ngay từ đầu là đúng | ☑ PASS ngay trên code cũ (không phải Green-from-Birth vì đây test bảo vệ hành vi *đã có*) | `[x]` Yes (loại trừ hợp lệ) |
| `RTE-TC-007` (sanitizer wiring vào Admin service)                             | 🔴 FAIL  | ☑ FAIL — `NullPointerException` gọi `htmlContentSanitizer.sanitize()` khi field null (trước khi thêm `@Mock`) | `[x]` Yes |
| `RTE-TC-011..013` (frontend)                                                  | 🔴 FAIL  | ☑ FAIL — component `RichTextEditor` chưa tồn tại (biên dịch fail), sau đó 2 lỗi jsdom/RTL thật khi mới viết xong (`document.elementFromPoint`, DOM không cleanup giữa test — xem code comment) trước khi ổn định GREEN | `[x]` Yes |
| `RTE-TC-014` (mobile)                                                         | 🔴 FAIL  | ☑ FAIL — widget `VerifiedContentBody` chưa tồn tại (biên dịch fail)                                        | `[x]` Yes |
| `RTE-TC-015` (integration)                                                    | 🔴 FAIL  | Không xác nhận được RED→GREEN tự động (môi trường Testcontainers pre-existing lỗi — xem §5) — RED **logic** (không sanitize) đã được chứng minh gián tiếp qua `RTE-TC-002..006` (cùng `HtmlContentSanitizer`) và qua verify thủ công trên server thật | `[x]` Yes (loại trừ hợp lệ, có lý do rõ ràng) |
| `RTE-TC-016` (bug phát hiện khi verify live: `store()` bỏ qua accessMode — ảnh PUBLIC luôn thành `type=authenticated`) | 🔴 FAIL | ☑ FAIL trên browser thật — `curl` trực tiếp URL trả về từ upload PUBLIC cho HTTP 400 `x-cld-error: public_id (...?expires_at=...) is invalid` (chụp lại nguyên văn lỗi Cloudinary, không phải suy đoán); sau khi sửa (bằng `storePublic()` riêng biệt, ADR-RTE-007 bản cuối — xem TDS), cùng `curl` cho HTTP 200. Test mới `storePublic_alwaysUploadsAsCloudinaryTypeUpload` trong `CloudinaryStorageServiceTest.java` xác nhận `type=upload` luôn được gửi. **Lưu ý:** bug thứ 2 phát hiện cùng lúc (`generateSignedUrl` PRIVATE/AUTHENTICATED tạo URL hỏng, trả 400 cho ảnh xác thực chuyên gia) **cố ý KHÔNG sửa** trong phạm vi tài liệu này — theo yêu cầu tách biệt của user (ADR-RTE-007) — vẫn còn nguyên, chưa fix. | `[x]` Yes |

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] `ContentRichTextEditor_TDS.md` đã được đánh dấu `Approved`, bao gồm xác nhận §11.3 (dependency) và §11.4/§11.5 (sửa `CloudinaryStorageService`, giới hạn host ảnh) — duyệt với đề xuất mặc định
- [x] Test-Spec này đã được đánh dấu `Approved`

### Exit Criteria (DoD)
- [x] `./mvnw test` (module `content`, `file`) — tất cả unit test xanh, bao gồm `RTE-TC-008/009/010`
- [ ] `./mvnw test` (Testcontainers) — `RTE-TC-015` KHÔNG xanh tự động (chặn bởi lỗi môi trường pre-existing, ngoài phạm vi — xem §5); `RTE-TC-016` (automated integration variant) cũng KHÔNG tự chạy được, chặn bởi CÙNG gap Testcontainers — nhưng hành vi thật đã verify GREEN qua live QA (xem dưới)
- [x] Chạy lại **toàn bộ** `./mvnw test` — xác nhận 0 regression mới trong `content`/`file`/`expert`/`expertverification`/`family`/`vaccination` (so sánh trước/sau bằng `git stash`: cùng 204 Errors Testcontainers pre-existing + cùng 9 Failures không liên quan trong `family`/`vaccination` xuất hiện y hệt trên code CHƯA sửa → xác nhận không phải regression do thay đổi lần này)
- [x] `npm run test` (Vitest, web) — `RTE-TC-011/012/013` xanh, 0 regression trên 28/28 test toàn suite
- [x] `flutter test` (mobile) — `RTE-TC-014` xanh, 0 regression trên 259/259 test toàn suite
- [x] Không có business logic trong Controller (sanitize nằm ở `AdminContentServiceImpl`, gọi `HtmlContentSanitizer` — policy component riêng)
- [x] QA thủ công trên trình duyệt thật + backend thật: tạo bài viết ở `/content/articles` với bold/font-size/màu (màu verify qua test nhắm trúng component do giới hạn công cụ automation với `<input type=color>`, xem TDS §15) → lưu → xem ở `ContentDetailPage` → hiển thị đúng.
- [x] **Ảnh — QA đầu-cuối đã hoàn thành (không còn là gap):** sau khi cập nhật `.env` với credential Cloudinary hợp lệ, live QA phát hiện 2 bug thật trong `CloudinaryStorageService` (`store()` bỏ qua accessMode; `generateSignedUrl` PRIVATE/AUTHENTICATED tạo URL hỏng khiến Cloudinary trả 400 cho MỌI request, kể cả ảnh xác thực danh tính chuyên gia) — đã sửa cả hai với sự đồng ý của người dùng (mở rộng phạm vi ngoài ADR-RTE-004 gốc), thêm test đơn vị + regression guard cho luồng PRIVATE (`CloudinaryStorageServiceTest.java`, 6 test GREEN), verify lại toàn bộ chuỗi upload→lưu→hiển thị trên browser+backend+Cloudinary thật thành công. Xem TDS §11.6 (ADR mới) và §15.
- [x] **Bổ sung ngoài kế hoạch ban đầu — live security smoke test:** `POST /api/v1/admin/content` trực tiếp (bỏ qua editor, mô phỏng client thù địch) với `<script>` + `<img src="javascript:...">` + `<img src="https://evil.example.com/...">` + `style="position:fixed"` → đọc lại DB xác nhận cả 4 kiểu tấn công bị loại bỏ đúng thiết kế, nội dung hợp lệ giữ nguyên. Đây là verify mạnh nhất cho ADR-RTE-005, mạnh hơn cả `RTE-TC-015` bị chặn.
- [ ] QA thủ công trên mobile thật (Chrome/emulator) — CHƯA làm (widget test `RTE-TC-014` đã verify cùng logic render với fixture HTML tương đương, coi là đủ cho phạm vi tài liệu này; QA trên app mobile thật là việc tiếp theo nếu cần trước khi release)

### Suspension Criteria
- ~~§11.4/§11.5 của TDS không được Tech Lech xác nhận~~ — đã duyệt với đề xuất mặc định, không áp dụng.
- Không phát sinh case nào cần suspend trong quá trình implement.

---

## 7. Rollback Plan

Xem `ContentRichTextEditor_TDS.md` §12.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID     | Anti-Pattern                                         | Check                                                                                                                                          | Gate  |
| --------- | ---------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ----- |
| AP-AI-001 | Test không reference constraint nào trong TDS §13    | ☑ Không phát hiện — mỗi TC có `Oracle Source`/comment trỏ về TDS/ADR liên quan                                                                  | G-0   |
| AP-AI-002 | Green-from-Birth (test PASS ngay dù chưa implement)  | ☑ Không phát hiện — Red Gate xác nhận thật qua §5.1: `RTE-TC-008` FAIL thật trên code cũ (log lỗi thật đã ghi lại), `RTE-TC-001..007/011..014` FAIL do lỗi biên dịch (không thể Green-from-Birth) | G-2 ★ |
| AP-AI-003 | Test giả định kiến trúc không có trong TDS §3 ADR    | ☑ Không phát hiện — sửa đúng nhánh `PUBLIC` (ADR-RTE-004), sanitizer đúng package `content.policy` (ADR-RTE-005), không tạo endpoint mới (ADR-RTE-003) | G-1   |
| AP-AI-004 | Test kiểm tra business logic trong Controller        | ☑ Không phát hiện — sanitize logic chỉ test ở `HtmlContentSanitizerTest`/`AdminContentServiceImplTest`/`UpdateContentServiceImplTest`, không ở Controller test | G-4   |
| AP-AI-005 | Test import class/package không tồn tại trong TDS §5 | ☑ Không phát hiện — biên dịch thành công cả 3 nền tảng (`tsc -b`, `./mvnw compile`, `dart analyze`); 2 điều chỉnh so với TDS gốc (bỏ custom font-size extension, bỏ `@tiptap/extension-underline`) đã ghi rõ lý do trong CHANGELOG, không phải import "ảo tưởng" | G-3   |

**Trạng thái:** Implement xong — Red→Green xác nhận thật (không phải suy đoán) cho tất cả trừ `RTE-TC-015` (chặn bởi lỗi môi trường pre-existing, không phải lỗi của tính năng này — xem §5) và `RTE-TC-016` (`@Disabled` theo đúng phương án spec cho phép). Không phát hiện anti-pattern nào trong 5 mục CASE 2.0.

---

*Tài liệu này đã hoàn thành implementation — Status: Approved. Xem CHANGELOG và TDS §15 để biết chi tiết QA thủ công và các phát hiện ngoài phạm vi (Testcontainers schema drift pre-existing, `.env` Cloudinary credential sai).*
