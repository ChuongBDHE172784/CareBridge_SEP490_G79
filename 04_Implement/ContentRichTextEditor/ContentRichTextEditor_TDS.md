# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — Rich Text Editor cho Article/FAQ Content (`/content/articles`, `/content/faq`)

| Field              | Value                                                      |
| ------------------ | ---------------------------------------------------------- |
| **Document ID**    | `CB-CONTENT-IMP-012`                                       |
| **Version**        | `1.0`                                                      |
| **Date**           | `2026-07-23`                                               |
| **Status**         | `Approved`                                                 |
| **Document Owner** | `HuyND`                                                    |
| **Author**         | `AI Agent — Claude`                                        |
| **Reviewed by**    | `[x] HuyND`                                                 |
| **DPO Sign-off**   | `N/A — Data Classification: Internal/Public, không có PII` |
| **Approved by**    | `[x] HuyND — 2026-07-23, "đã Approved" (đề xuất mặc định ở §11.3/11.4/11.5)` |
| **Last Review**    | `2026-07-23`                                               |
| **Based on EDS**   | `v2.0`                                                     |

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                                                                                                                           |
| ---------- | ----------------- | ------------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-07-23 | AI Agent — Claude | Tạo tài liệu lần đầu — Draft, chờ review. Phạm vi và lựa chọn thư viện đã được xác nhận qua AskUserQuestion trong phiên làm việc (xem §11). |
| 2026-07-23 | HuyND             | Duyệt trực tiếp trong file (`Status: Approved`) — áp dụng đề xuất mặc định cho §11.3 (dependency), §11.4 (giới hạn host Cloudinary cho `img src`), §11.5 (sửa `CloudinaryStorageService`) |
| 2026-07-23 | AI Agent — Claude | Implement xong theo TDD Red→Green. 2 điều chỉnh so với thiết kế ban đầu, cả hai đều đơn giản hoá (xem §5.2 addendum): (1) Tiptap v3.28 đã có sẵn `FontSize`/`Color`/`FontFamily`/`TextStyle` gộp trong `TextStyleKit` (từ `@tiptap/extension-text-style`) — bỏ kế hoạch viết custom font-size extension ở ADR-RTE-002; (2) `StarterKit` v3 đã bao gồm sẵn `Underline` — bỏ `@tiptap/extension-underline` khỏi dependency list. Kết quả: ít package hơn dự kiến (5 thay vì 8). Toàn bộ code đã qua QA thủ công trên trình duyệt thật + backend thật (xem Test-Spec CHANGELOG để biết chi tiết, bao gồm 1 phát hiện quan trọng: `.env` dev cục bộ có `CLOUDINARY_CLOUD_NAME=root` không hợp lệ — chặn upload ảnh thật, không liên quan đến code của tính năng này). |
| 2026-07-23 | AI Agent — Claude | **Sửa lại kết luận trước đó — không chính xác.** Sau khi user tự cập nhật `.env` với Cloudinary credential hợp lệ và yêu cầu tiếp tục QA, live test phát hiện luồng ảnh **vẫn hỏng hoàn toàn** (không phải do thiếu credential như báo cáo trước). Root cause thật: 2 bug trong `CloudinaryStorageService` — `store()` bỏ qua accessMode caller truyền vào; `generateSignedUrl()` tạo URL không hợp lệ cho PRIVATE/AUTHENTICATED khiến Cloudinary trả 400 cho **mọi** request loại đó, kể cả ảnh xác thực danh tính chuyên gia. Đã trình bày rõ 2 bug + phương án sửa gộp chung cho user, nhận "tiến hành", implement + test GREEN. |
| 2026-07-23 | AI Agent — Claude | **User hỏi lại: "không tách riêng với luồng chuyên gia được à" — thiết kế lại hoàn toàn (ADR-RTE-007 bản cuối).** Revert toàn bộ phần sửa gộp chung (`store()` không còn nhận `accessMode`, `generateSignedUrl()` PRIVATE/AUTHENTICATED quay lại y hệt code cũ — verify bằng `git diff HEAD` = rỗng cho cả 2). Thêm method **mới, độc lập** `storePublic()` chỉ dùng cho ảnh nội dung PUBLIC; `FileServiceImpl` chỉ gọi `storePublic()` khi `accessMode==PUBLIC`, mọi trường hợp khác (toàn bộ luồng chuyên gia) không đổi. Hệ quả: bug ảnh PRIVATE/AUTHENTICATED (HTTP 400, ảnh hưởng CCCD/selfie chuyên gia) **cố ý không sửa**, vẫn còn nguyên — ghi rõ là follow-up cần TDS/Test-Spec riêng, không bundle vào tính năng này. Cập nhật lại test (`CloudinaryStorageServiceTest.java`: bỏ 3 test target `store(accessMode)` không còn tồn tại, thêm 1 test cho `storePublic()`; `FileServiceImplTest.java`: `uploadPublicFile_acceptsJpeg` verify gọi `storePublic()` thay vì `store()`). Verify lại toàn bộ: `./mvnw test` đầy đủ — 204 Errors + 9 Failures giống hệt baseline (0 regression mới). Verify sống 2 lần trên browser+backend+Cloudinary thật: (1) upload trực tiếp qua API, (2) tạo bài viết → xem ở `ContentDetailPage` (trang người đọc, `dangerouslySetInnerHTML`) → ảnh render đúng (`naturalWidth`/`naturalHeight` khớp file gốc, `complete: true`). Dọn dẹp toàn bộ dữ liệu/file test tạo trong quá trình QA. Xem ADR-RTE-007 (đã viết lại hoàn toàn) và §15. |

---

## 0. Bối cảnh phát sinh

Người dùng yêu cầu: nâng cấp phần "Nội dung chi tiết" ở `/content/articles` và `/content/faq` (Content Admin portal) — hiện tại chỉ là một `<textarea>` nhập text thuần (`CreateContentPage.tsx:189`, `EditContentPage.tsx:168`). Yêu cầu cụ thể: chỉnh sửa được **font, cỡ chữ, màu sắc, in đậm/thường**, và **chèn ảnh**, ảnh lưu trên nền tảng đang tích hợp sẵn (đã xác nhận ở câu hỏi trước trong cùng phiên: ảnh lưu trên **Cloudinary**, qua module `file`).

**Cả `/content/articles` và `/content/faq` dùng chung 2 trang** `CreateContentPage.tsx` (tạo, `type` truyền qua query param) và `EditContentPage.tsx` (sửa) — không phải 2 form riêng biệt. Sửa 1 lần ở 2 trang này áp dụng cho cả hai loại nội dung (và các `ContentType` khác đi qua cùng form, ví dụ `PARENTING_TIP`).

**Phát hiện quan trọng khi điều tra (verify bằng code, không phải giả định):**

1. **`body` đã được kỳ vọng là HTML, không phải plain text.** `ContentDetailPage.tsx:184` (staff detail view) đã render bằng `dangerouslySetInnerHTML={{ __html: detail.body }}`. Cột DB `content_items.body` là `TEXT`, không có giới hạn định dạng — không cần migration để lưu HTML thay vì text thuần.
2. **Mobile app (Flutter) đang render `body` là plain `Text`,** không phải HTML — `verified_content_detail_screen.dart:250` (`Text(content.body, ...)`, model `ContentDetail.body` trong `content_model.dart`). Đây là màn hình end-user thật (mẹ/gia đình) đọc bài viết/FAQ đã duyệt. Nếu chỉ sửa web mà không sửa mobile, người dùng mobile sẽ thấy thẻ HTML thô (`<p><b>...`). **Đã hỏi người dùng qua AskUserQuestion → quyết định: đưa mobile vào phạm vi** (xem §11).
3. **Upload ảnh công khai đã có sẵn nhưng chưa được nối dây đầy đủ.** `IFileService.uploadPublicFile()` / `FilePurpose.PUBLIC_CONTENT_IMAGE` / `FileAccessMode.PUBLIC` đã tồn tại trong `FileServiceImpl` nhưng: (a) không có endpoint controller nào gọi `uploadPublicFile()` trực tiếp — tuy nhiên endpoint tổng quát `POST /api/v1/files/upload/with-purpose` (đã tồn tại, cho phép `CONTENT_ADMIN`) đã đủ để đạt được kết quả tương đương bằng cách truyền `kind=IMAGE, purpose=PUBLIC_CONTENT_IMAGE, accessMode=PUBLIC`; (b) **lỗi hạ tầng nghiêm trọng hơn:** `CloudinaryStorageService.generateSignedUrl()` luôn tạo signed URL với `expires_at` giới hạn tối đa **15 phút**, bất kể `accessMode` — kể cả với `PUBLIC`. Ảnh nhúng trực tiếp vào HTML lưu trong `content_items.body` (khác với luồng tài liệu riêng tư vốn "ký lại URL mỗi lần xem" qua `GET /api/v1/files/{fileId}`) sẽ **hỏng sau 15 phút** nếu dùng nguyên trạng — không dùng được cho use case này. Xem ADR-RTE-004.
4. **Không có sanitize HTML phía backend.** `body` hiện nhận string tuỳ ý từ Content Admin và được render thẳng qua `dangerouslySetInnerHTML` — rủi ro stored-XSS nếu tài khoản Content Admin bị chiếm hoặc vô tình dán HTML độc hại. Chuyển sang rich text editor **tăng bề mặt tấn công** (giờ đây output luôn là HTML thật) nên bắt buộc phải sanitize server-side trước khi lưu. Xem ADR-RTE-005.

→ Phạm vi tài liệu này rộng hơn một "thay textarea bằng editor": gồm **web editor + backend URL-persistence fix + backend sanitization + mobile HTML renderer**. Không cần Flyway migration (không đổi schema).

---

## 1. Tổng quan Module

| Field                     | Value                                                                                                                                             |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Module Name**           | `Rich Text Content Editor (Article/FAQ body)`                                                                                                     |
| **Bounded Context**       | `content` (backend), `contentManagement` (web), `community` feature (mobile — nơi hiển thị content công khai)                                     |
| **Platform**              | `Admin Web Portal (React + Vite)` + `Backend (Spring Boot)` + `Mobile (Flutter)`                                                                  |
| **Data Classification**   | `Internal` (soạn thảo, CONTENT_ADMIN) → `Public` sau khi APPROVED (không có PII trong nội dung bài viết/FAQ)                                      |
| **Compliance Scope**      | `BR-RBAC (CONTENT_ADMIN)`, `OWASP A03:2021 — Injection (Stored XSS)`                                                                              |
| **Upstream Dependencies** | `file` module (Cloudinary storage, đã có), `security` (JWT/RBAC), `audit` (AuditService)                                                          |
| **Downstream Consumers**  | Web: `ContentDetailPage` (staff), public content detail view (nếu có, cùng render pattern); Mobile: `VerifiedContentDetailScreen` (UC-224/UC-225) |

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại          | Mô tả yêu cầu                                                                     | Thành phần Code                                          | ADR liên quan |
| -------------- | ------------- | --------------------------------------------------------------------------------- | -------------------------------------------------------- | ------------- |
| US-RTE-001     | User Story    | Content Admin định dạng chữ (đậm/thường, cỡ chữ, font, màu) khi soạn bài viết/FAQ | `RichTextEditor.tsx` (Tiptap)                            | ADR-RTE-002   |
| US-RTE-002     | User Story    | Content Admin chèn ảnh vào nội dung, ảnh lưu trên Cloudinary                      | `RichTextEditor.tsx` + `contentApi.uploadContentImage()` | ADR-RTE-003   |
| BR-RTE-SEC-001 | Business Rule | HTML từ editor phải được sanitize trước khi lưu DB                                | `HtmlContentSanitizer` (mới)                             | ADR-RTE-005   |
| BR-RTE-URL-001 | Business Rule | URL ảnh public nhúng trong content phải bền vững (không hết hạn)                  | `CloudinaryStorageService.generateSignedUrl()`           | ADR-RTE-004   |
| US-RTE-003     | User Story    | Người dùng mobile đọc bài viết/FAQ thấy định dạng đúng, không thấy thẻ HTML thô   | `verified_content_detail_screen.dart` (flutter_html)     | ADR-RTE-006   |

---

## 3. Architecture Decision Records (ADR)

### ADR-RTE-001 — `body` là HTML, không phải Markdown

| Field        | Value                                                           |
| ------------ | --------------------------------------------------------------- |
| **Status**   | `Accepted` (đã là thực tế hiện tại, tài liệu này hình thức hoá) |
| **Deciders** | `HuyND — Tech Lead`                                             |
| **Date**     | `2026-07-23`                                                    |

#### Bối cảnh
Yêu cầu "màu sắc" và "cỡ chữ" không biểu diễn được bằng Markdown thuần (cần inline style). Web staff view đã render `body` qua `dangerouslySetInnerHTML` từ trước khi tài liệu này tồn tại.

#### Quyết định
Editor xuất ra **HTML** (không phải Markdown/AST riêng). Không đổi kiểu cột `content_items.body` (`TEXT`, đã đủ chứa HTML, giới hạn 50 000 ký tự ở `CreateContentRequest`/`UpdateContentRequest` giữ nguyên — đủ cho bài viết dài kèm vài ảnh vì chỉ nhúng URL, không nhúng base64).

#### Hệ quả
**Tích cực:** không cần migration; nhất quán với hành vi render đã có.
**Trade-off:** buộc phải sanitize server-side nghiêm ngặt hơn (xem ADR-RTE-005) vì HTML là bề mặt tấn công lớn hơn Markdown.

---

### ADR-RTE-002 — Thư viện editor web: Tiptap; font-size dùng custom mark extension thay vì package cộng đồng chưa kiểm chứng

| Field        | Value                                              |
| ------------ | -------------------------------------------------- |
| **Status**   | `Accepted` (người dùng chọn qua AskUserQuestion)   |
| **Deciders** | `HuyND` (chọn Tiptap so với React Quill / TinyMCE) |
| **Date**     | `2026-07-23`                                       |

#### Các phương án đã xem xét
| Phương án     | Ưu điểm                                                                                                                   | Nhược điểm                                                                |
| ------------- | ------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------- |
| Tiptap (chọn) | Nhẹ, dựa trên ProseMirror, output HTML sạch, toolbar tự build (kiểm soát đúng tập tính năng cần), MIT license, maintained | Không có sẵn extension "font size" chính thức — phải viết custom mark nhỏ |
| React Quill   | API đơn giản                                                                                                              | Package gốc ngừng bảo trì, phải dùng fork/community                       |
| TinyMCE React | Đầy đủ tính năng nhất                                                                                                     | Nặng hơn, bản miễn phí giới hạn/cần API key hoặc self-host GPL            |

#### Quyết định
Dùng **Tiptap** (`@tiptap/react`, `@tiptap/pm`, `@tiptap/starter-kit`, `@tiptap/extension-underline`, `@tiptap/extension-text-style`, `@tiptap/extension-color`, `@tiptap/extension-image`, `@tiptap/extension-font-family` — tất cả version `^3.28.0`, đã verify tồn tại trên npm registry tại thời điểm viết tài liệu này). **Font size** không có extension chính thức của Tiptap — viết một `Extension` tuỳ biến nhỏ (theo đúng pattern "custom extension" mà Tiptap tự tài liệu hoá: mở rộng `TextStyle` mark, thêm attribute `fontSize`, `renderHTML` xuất `style="font-size: …"`) thay vì cài thêm package bên thứ 3 không rõ nguồn gốc/bảo trì.

#### Hệ quả
**Tích cực:** kiểm soát chặt output HTML (dễ viết allowlist cho sanitizer ở ADR-RTE-005 vì biết chính xác tag/attribute nào editor có thể sinh ra).
**Trade-off:** cần thêm ~7 package mới vào `package.json` (xem §17 dependency approval).

---

### ADR-RTE-003 — Tái sử dụng endpoint upload sẵn có, không tạo endpoint mới

| Field        | Value               |
| ------------ | ------------------- |
| **Status**   | `Accepted`          |
| **Deciders** | `HuyND — Tech Lead` |
| **Date**     | `2026-07-23`        |

#### Bối cảnh
`POST /api/v1/files/upload/with-purpose` đã tồn tại, đã cho phép role `CONTENT_ADMIN`, đã hỗ trợ `kind=IMAGE`, `purpose=PUBLIC_CONTENT_IMAGE`, `accessMode=PUBLIC` (validate ở `FileServiceImpl.uploadWithPurpose`, không cần đổi gì backend ngoài ADR-RTE-004/005).

#### Quyết định
Frontend gọi thẳng endpoint này khi người dùng chèn ảnh trong editor (giống pattern `uploadContributionFile()` đã có trong `expertApi.ts`). Không thêm endpoint `/api/v1/files/upload/public` mới, không expose `uploadPublicFile()` qua controller riêng — tránh trùng lặp đường dẫn upload.

#### Hệ quả
**Tích cực:** 0 endpoint mới, giảm bề mặt API cần bảo trì/test.
**Trade-off:** không có gì đáng kể — tham số `kind/purpose/accessMode` do frontend truyền, nhưng backend đã validate khớp MIME thật (magic bytes) nên client không thể giả mạo `kind=IMAGE` cho file không phải ảnh.

---

### ADR-RTE-004 — `FileAccessMode.PUBLIC` trả về URL Cloudinary bền vững (không ký, không hết hạn)

| Field        | Value                                                                    |
| ------------ | ------------------------------------------------------------------------ |
| **Status**   | `Accepted` — duyệt qua "đã Approved"; **mở rộng thêm qua ADR-RTE-007 sau khi live QA phát hiện fix ban đầu chưa đủ** |
| **Deciders** | `HuyND — Tech Lead`                                                      |
| **Date**     | `2026-07-23`                                                             |

#### Bối cảnh
`CloudinaryStorageService.generateSignedUrl()` (dòng 91-108) luôn `signed(true)` + `expires_at` giới hạn `Math.min(ttlMinutes, 15)` phút, **kể cả khi `accessMode == PUBLIC`**. Ảnh Cloudinary loại `type=upload` (ứng với `PUBLIC`) vốn dĩ **đã public theo thiết kế của Cloudinary** — không cần ký URL để bảo vệ, ký thêm `expires_at` chỉ tạo ra hạn dùng giả tạo không phục vụ mục đích bảo mật nào (khác với `PRIVATE`/`AUTHENTICATED`, nơi signing thực sự kiểm soát truy cập). Với ảnh nhúng trong `<img src>` của một `content_items.body` tồn tại vĩnh viễn, URL hết hạn 15 phút sau khi tạo là lỗi nghiêm trọng — bài viết sẽ hiển thị ảnh vỡ sau khi soạn xong.

Đây là hàm dùng chung cho nhiều luồng khác (expert identity docs, credentials, contribution attachments) — **không được đổi hành vi của `PRIVATE`/`AUTHENTICATED`.**

#### Các phương án đã xem xét
| Phương án | Mô tả                                                                                                                                                                               | Ưu điểm                                                                  | Nhược điểm                                                                                                                                                                                                                                                                                    |
| --------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| A (chọn)  | Trong `generateSignedUrl()`, nếu `accessMode == PUBLIC` → trả `cloudinary.url().resourceType(...).secure(true).generate(publicId)` (không `signed()`, không `expires_at`)           | Sửa tối thiểu, đúng 1 nhánh `if`, không đụng logic PRIVATE/AUTHENTICATED | Vẫn phải review kỹ vì đây là file hạ tầng nhạy cảm (dùng cho cả identity documents)                                                                                                                                                                                                           |
| B         | Bắt frontend lưu `fileId` (không lưu URL) trong `body`, render-time gọi lại `GET /api/v1/files/{fileId}` để lấy URL mới mỗi lần hiển thị (giống pattern tài liệu riêng tư hiện tại) | Không đổi hạ tầng chung                                                  | Không khả thi: `body` là HTML tĩnh lưu trong DB, không có chỗ để "render lại" `<img src>` bằng React trước khi gửi cho mobile/HTML thô — sẽ cần thêm 1 lớp post-processing HTML ở mọi nơi hiển thị content (web + mobile), phức tạp hơn nhiều so với việc ảnh public vốn nên có URL vĩnh viễn |

#### Quyết định
Chọn **Phương án A**, giới hạn thay đổi đúng nhánh `accessMode == PUBLIC`. Áp dụng cho cả `generateSignedUrl()` (dùng lại trong `viewFile()`/`generatePresignedUrl()` public API) — tự động fix luôn `uploadPublicFile()` vốn đang tồn tại nhưng chưa dùng được đúng.

#### Hệ quả
**Tích cực:** ảnh public (content, và tương lai là avatar nếu áp dụng lại `PUBLIC_CONTENT_IMAGE`) có URL vĩnh viễn đúng như kỳ vọng của Cloudinary `type=upload`.
**Tiêu cực/Trade-off:** đây là **thay đổi hành vi của file hạ tầng chia sẻ** (`CloudinaryStorageService`, dùng bởi expert-verification, contribution, v.v.) — bắt buộc chạy lại toàn bộ test suite của module `file` + `expertverification` + `expert` (contribution attachments) sau khi sửa để đảm bảo không có test nào đang assert `expires_at` xuất hiện trong URL của luồng PUBLIC (thực tế review code hiện tại cho thấy không có consumer nào dựa vào `PUBLIC_CONTENT_IMAGE` — an toàn, nhưng vẫn phải chạy test để xác nhận).

---

### ADR-RTE-005 — Sanitize HTML phía backend trước khi lưu `body`

| Field        | Value                                           |
| ------------ | ----------------------------------------------- |
| **Status**   | `Proposed` — **thêm dependency mới, cần duyệt** |
| **Deciders** | `HuyND — Tech Lead`                             |
| **Date**     | `2026-07-23`                                    |

#### Bối cảnh
`body` giờ luôn là HTML thật (ADR-RTE-001) và được render bằng `dangerouslySetInnerHTML` (web) + `flutter_html` (mobile, ADR-RTE-006) — cả hai đều thực thi HTML/CSS được lưu. Nếu không sanitize, một Content Admin (hoặc tài khoản CONTENT_ADMIN bị chiếm) có thể lưu `<script>`/`onerror=`/`javascript:` URL vào `body`, thực thi khi APPROVED và hiển thị cho người dùng cuối. Đây là stored-XSS thật (OWASP A03:2021), không phải lý thuyết — request đã đi qua nguyên trạng đến DB hiện nay.

#### Quyết định
Thêm dependency **`com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1`** (Apache 2.0 license, Maven Central, đã verify tồn tại). Tạo `HtmlContentSanitizer` (package `content.policy`, đúng convention CLAUDE.md "Policy: reusable domain rules"), định nghĩa allowlist khớp chính xác tập thẻ/attribute mà `RichTextEditor.tsx` (Tiptap) có thể sinh ra (§5.2):
- Thẻ: `p, br, b, strong, i, em, u, s, span, h1, h2, h3, ul, ol, li, blockquote, img`
- Attribute: `style` (chỉ giá trị `color`, `font-size`, `font-family` — dùng `CssSchema` của thư viện để giới hạn property, không cho phép `position`, `expression()`, v.v.), `src`/`alt`/`width`/`height` trên `img`
- **Không** cho phép: `script, style (tag), iframe, object, embed, on*` event attributes, `javascript:` URL — thư viện OWASP sanitizer loại các thẻ/thuộc tính này theo mặc định khi không được liệt kê trong policy.

> **Câu hỏi cần Tech Lead quyết định khi duyệt (không tự quyết trong tài liệu này):** giới hạn `img src` chỉ scheme `https` bất kỳ, hay giới hạn chặt hơn — chỉ host Cloudinary đã cấu hình (`res.cloudinary.com`)? Nền tảng có yêu cầu PDPA; một `<img src>` trỏ tới host ngoài sẽ làm trình duyệt người đọc (mẹ/gia đình) gửi request trực tiếp tới bên thứ 3 khi xem bài viết đã duyệt (rò rỉ IP/User-Agent qua Referer). Giới hạn theo host Cloudinary chặt hơn, chặn luôn khả năng Content Admin dán ảnh từ nguồn ngoài — đề xuất mặc định: **giới hạn theo host Cloudinary** (khớp với việc "ảnh chỉ nên chèn qua nút Insert Image, không dán URL ngoài" — đúng tinh thần ADR-RTE-003), nhưng cần xác nhận vì đây là quyết định chính sách, không phải kỹ thuật thuần.

Gọi `htmlContentSanitizer.sanitize(request.body())` trong `AdminContentServiceImpl.createContent()` (trước `contentMapper.toEntity()`) và `updateContent()` (trước `item.setBody()`).

#### Hệ quả
**Tích cực:** đóng luôn lỗ hổng XSS đã tồn tại từ trước (không phải do tài liệu này tạo ra, nhưng bị khuếch đại nếu không xử lý).
**Trade-off:** 1 dependency Maven mới — cần duyệt theo CLAUDE.md ("không thêm dependency không có phê duyệt").

---

### ADR-RTE-006 — Mobile render `body` bằng `flutter_html`

| Field        | Value                                                                                                  |
| ------------ | ------------------------------------------------------------------------------------------------------ |
| **Status**   | `Accepted` (người dùng chọn "Bao gồm Mobile" qua AskUserQuestion) — **thêm dependency mới, cần duyệt** |
| **Deciders** | `HuyND`                                                                                                |
| **Date**     | `2026-07-23`                                                                                           |

#### Quyết định
Thêm `flutter_html: ^3.0.0` (pub.dev, đã verify tồn tại) vào `pubspec.yaml`. Thay `Text(content.body, ...)` bằng `Html(data: content.body, ...)` tại `verified_content_detail_screen.dart:250`, style tag cơ bản (`p`, `h1-h3`, `img` responsive theo chiều rộng màn hình) qua tham số `style` của `flutter_html`.

**Verify tương thích toolchain (đã kiểm tra, không chỉ dựa vào "package tồn tại"):** `pubspec.yaml` hiện khai báo `environment.sdk: '>=3.10.0 <4.0.0'`; `flutter_html: ^3.0.0` yêu cầu Dart `>=3.2.0 <4.0.0` và Flutter `>=3.0.0` — tương thích. Lưu ý: `flutter_html` 3.0.0 là bản mới nhất trong thời gian dài (tín hiệu bảo trì chậm) — bước đầu tiên khi implement mobile (§Chặng 1 mobile) PHẢI là `flutter pub get` + render thử 1 chuỗi HTML mẫu để xác nhận build thật trước khi code phần còn lại của ADR này. Nếu build lỗi/không tương thích khi implement thật, phương án dự phòng là package `flutter_widget_from_html` (cần verify lại tồn tại/tương thích tại thời điểm đó).

#### Hệ quả
**Tích cực:** người dùng mobile thấy đúng định dạng/ảnh như Content Admin đã soạn trên web.
**Trade-off:** 1 dependency Flutter mới — cần duyệt. Không sanitize lại ở client (đã sanitize server-side, ADR-RTE-005 — client chỉ render).

---

### ADR-RTE-007 — `storePublic()` riêng biệt cho ảnh nội dung; KHÔNG đụng vào `store()`/luồng PRIVATE dùng chung (tách biệt khỏi luồng xác thực chuyên gia)

| Field        | Value                                                                    |
| ------------ | ------------------------------------------------------------------------ |
| **Status**   | `Accepted` — root cause phát hiện qua live QA; phương án ban đầu (gộp chung vào `store()`) đã bị **user yêu cầu tách riêng** ("không tách riêng với luồng chuyên gia được à") — đây là bản thiết kế cuối cùng, đã tách |
| **Deciders** | `HuyND`                                                                  |
| **Date**     | `2026-07-23`                                                             |

#### Bối cảnh
Sau khi ADR-RTE-004 được implement và unit-test GREEN, live QA đầu-cuối (upload ảnh thật qua editor → Cloudinary thật) phát hiện tính năng **vẫn không hoạt động**: response `presignedUrl` vẫn chứa `type=authenticated` + `expires_at`, và `curl` trực tiếp URL đó trả **HTTP 400** từ Cloudinary (`x-cld-error: public_id (...?expires_at=...) is invalid`). Điều tra ra 2 bug thật, cả hai đều **có từ trước ADR-RTE-004**:

1. **`store()` không nhận `accessMode`:** `IStorageService.store(key, data, mimeType)` không có tham số accessMode. `CloudinaryStorageService.store()` tự đoán qua `determineAccessMode(mimeType)`, luôn trả `AUTHENTICATED` cho mọi ảnh bất kể caller (kể cả `uploadPublicFile()`) yêu cầu gì.
2. **`generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED tạo URL không hợp lệ:** code `generate(publicId + "?" + "expires_at=" + expiresAt)` — Cloudinary SDK hiểu toàn bộ chuỗi là `public_id`, nên **mọi** request loại PRIVATE/AUTHENTICATED bị từ chối 400. Bug **pre-existing, ảnh hưởng toàn hệ thống** — bao gồm ảnh xác thực danh tính chuyên gia (`ExpertIdentityVerificationServiceImpl` dùng `FileAccessMode.PRIVATE` qua cùng `store()`/`generateSignedUrl()`).

**Vòng lặp quyết định (quan trọng, ghi lại đầy đủ vì đã đổi hướng giữa chừng):** Lần đầu, agent đề xuất sửa gộp — thêm `accessMode` vào `store()` dùng chung, sửa cả `generateSignedUrl()` cho PRIVATE/AUTHENTICATED (bỏ cơ chế `expires_at` hỏng, dùng `signed(true)` chuẩn) — user đã nói "tiến hành" và agent đã implement + test GREEN. Ngay sau đó user hỏi lại: **"không tách riêng với luồng chuyên gia được à"** — một câu hỏi kiến trúc hợp lý: tính năng rich-text-editor không nên làm thay đổi hành vi của luồng xác thực danh tính chuyên gia (bảo mật/PII-sensitive), dù là thay đổi có lợi. Agent đã **revert toàn bộ phần gộp chung** và thiết kế lại theo hướng tách biệt hoàn toàn (mô tả dưới đây).

#### Các phương án đã xem xét
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
| --------- | ----- | ------- | ---------- |
| A (đã thử, sau đó revert) | Thêm `accessMode` vào `store()` dùng chung; sửa luôn `generateSignedUrl()` PRIVATE/AUTHENTICATED | Sửa đúng gốc rễ cho cả 2 bug cùng lúc | **User từ chối**: thay đổi hành vi (dù có lợi) của luồng xác thực chuyên gia mà không phải mục tiêu của tính năng này — vi phạm nguyên tắc tách biệt mối quan tâm cho code bảo mật/PII |
| B (chọn) | Thêm method **mới, riêng biệt** `storePublic(key, data, mimeType)` trên `IStorageService`/`CloudinaryStorageService` — chỉ set `type=upload` cố định, không phụ thuộc `determineAccessMode()`. `store()` giữ **nguyên xi, byte-for-byte** như code gốc (đã verify bằng `git diff HEAD` = rỗng cho `store()` và nhánh PRIVATE/AUTHENTICATED của `generateSignedUrl()`). `FileServiceImpl.uploadUsing()` chỉ gọi `storePublic()` khi `accessMode == PUBLIC`, mọi trường hợp khác (bao gồm toàn bộ luồng chuyên gia) gọi `store()` y hệt trước giờ | Blast radius = 0 cho luồng PRIVATE/AUTHENTICATED — có thể chứng minh bằng diff rỗng, không chỉ "tin tưởng" | Bug #2 (PRIVATE/AUTHENTICATED 400) **không được sửa** — vẫn hỏng, nhưng đó là hành vi **không đổi**, không phải regression |

#### Quyết định
Chọn **Phương án B**. `storePublic()` là method mới, độc lập, chỉ dùng cho ảnh nội dung PUBLIC (content_items body images). `store()`, `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED, và luồng xác thực danh tính chuyên gia **giữ nguyên không đổi một chữ nào** — kể cả bug #2 vẫn còn đó, cố ý không đụng vào. `delete()` được sửa map `accessMode → Cloudinary type` đúng cho trường hợp PUBLIC (trước đây map sai thành `"public"` thay vì `"upload"`) — verify là no-op cho PRIVATE/AUTHENTICATED (chỉ khác kết quả khi accessMode=PUBLIC, mà trước `storePublic()` tồn tại thì không ảnh nào thực sự có accessMode=PUBLIC cả).

#### Hệ quả
**Tích cực:** ảnh PUBLIC (nội dung bài viết/FAQ) giờ thực sự lên Cloudinary đúng `type=upload`, verify sống bằng `curl` trả HTTP 200 — kể cả sau khi đổi routing sang `storePublic()`. Blast radius cho luồng chuyên gia = 0, chứng minh được bằng `git diff HEAD` rỗng cho các đoạn code liên quan.
**Tiêu cực/Trade-off — cần user biết rõ, không được im lặng bỏ qua:** Bug #2 (ảnh PRIVATE/AUTHENTICATED — bao gồm CCCD/selfie xác thực chuyên gia — bị Cloudinary từ chối 400, hoàn toàn không xem được) **vẫn còn nguyên, chưa sửa**. Đây là bug có từ trước phiên làm việc này (không phải do tính năng rich-text-editor gây ra), nhưng agent đã biết cách sửa (đã từng implement + test GREEN ở phương án A) và cố ý không áp dụng theo yêu cầu tách biệt của user. **Khuyến nghị:** xử lý như một task riêng, có TDS/Test-Spec riêng do tính chất PII/bảo mật (ảnh định danh chuyên gia), không bundle vào tính năng content editor.
**Test:** `CloudinaryStorageServiceTest.java` chỉ còn test cho `generateSignedUrl()` (không đổi, vẫn "hỏng như cũ" cho PRIVATE/AUTHENTICATED — làm regression guard cho *hành vi không đổi*) và 1 test mới `storePublic_alwaysUploadsAsCloudinaryTypeUpload`. Không còn test nào target `store()` với accessMode (vì signature đó đã bị revert).

---

## 4. Non-Functional Requirements & SLA

Module nội bộ (soạn thảo) + public read (sau duyệt), tần suất thấp. Không có yêu cầu NFR đặc biệt ngoài baseline hệ thống.

| Category       | Requirement                                            | Target                      | Ghi chú                                                                 |
| -------------- | ------------------------------------------------------ | --------------------------- | ----------------------------------------------------------------------- |
| Security       | HTML lưu DB không chứa `<script>`/event handler        | 100%                        | TC-SEC (sanitizer allowlist test)                                       |
| Data Integrity | URL ảnh public không hết hạn                           | Vĩnh viễn (no `expires_at`) | TC-INT (CloudinaryStorageService PUBLIC branch)                         |
| Compatibility  | Không đổi hành vi `PRIVATE`/`AUTHENTICATED` signed URL | 100% (không regression)     | Chạy lại test suite `file`, `expertverification`, `expert.contribution` |
| Body size      | Giới hạn 50 000 ký tự (đã có) đủ cho HTML + vài ảnh    | Không đổi giới hạn          | —                                                                       |

---

## 5. Static Modeling

### 5.1. Backend — thay đổi/thêm mới

```java
// KHÔNG đổi entity — content_items.body vẫn TEXT, không cần migration.

// CloudinaryStorageService.java — sửa generateSignedUrl() (ADR-RTE-004)
public String generateSignedUrl(String publicId, int ttlMinutes, FileAccessMode accessMode, String resourceType) {
    if (accessMode == FileAccessMode.PUBLIC) {
        // Cloudinary "upload" (type=upload) đã public theo thiết kế — không ký, không hết hạn.
        return cloudinary.url().resourceType(resourceType).secure(true).generate(publicId);
    }
    // ... logic signed(true) + expires_at hiện có, GIỮ NGUYÊN cho PRIVATE/AUTHENTICATED
}

// HtmlContentSanitizer.java — MỚI — com.carebridge.backend.content.policy (ADR-RTE-005)
@Component
public class HtmlContentSanitizer {
    // PolicyFactory dùng org.owasp.html — allowlist khớp §3 ADR-RTE-005
    public String sanitize(String rawHtml) { ... }
}

// AdminContentServiceImpl.java — inject HtmlContentSanitizer, gọi trước khi lưu body
// createContent(): request.body() -> sanitizer.sanitize(...) trước contentMapper.toEntity()
// updateContent(): item.setBody(sanitizer.sanitize(request.body()))
```

### 5.2. Frontend Web — component mới + thay đổi

```
src/features/contentManagement/components/
├── RichTextEditor.tsx          // MỚI — wrap Tiptap useEditor + toolbar
├── RichTextEditor.css          // MỚI — style toolbar/khung soạn thảo
└── fontSizeExtension.ts        // MỚI — custom Tiptap Extension (ADR-RTE-002)

// RichTextEditor.tsx — props
interface RichTextEditorProps {
  value: string;                       // HTML hiện tại
  onChange: (html: string) => void;
  onImageUpload: (file: File) => Promise<string>; // trả về URL bền vững (ADR-RTE-004)
  placeholder?: string;
}
// Toolbar: Bold, Italic, Underline, Font family (select), Font size (select: 12/14/16/18/24/32px),
// Text color (input type=color, dùng extension-color), Heading (H1/H2/H3/Normal), Bullet/Numbered list,
// Insert image (input file -> onImageUpload -> editor.chain().focus().setImage({src}).run())

// contentApi.ts — thêm hàm mới, cùng pattern uploadContributionFile() trong expertApi.ts
export async function uploadContentImage(file: File): Promise<{ url: string }> {
  const form = new FormData();
  form.append('file', file);
  form.append('kind', 'IMAGE');
  form.append('purpose', 'PUBLIC_CONTENT_IMAGE');
  form.append('accessMode', 'PUBLIC');
  const { data } = await apiClient.post('/api/v1/files/upload/with-purpose', form, {
    headers: { 'Content-Type': undefined },
  });
  return { url: data.data.presignedUrl }; // "presignedUrl" tên field cũ trong DTO — sau ADR-RTE-004 giá trị này là URL vĩnh viễn cho ảnh PUBLIC
}

// CreateContentPage.tsx / EditContentPage.tsx — thay <textarea value={body} onChange=.../>
// bằng <RichTextEditor value={body} onChange={setBody} onImageUpload={uploadContentImage} />
```

> **Lưu ý implement:** `UploadFileResponse.presignedUrl` là tên field DTO có sẵn (không đổi tên — tránh breaking change cho các consumer khác của cùng DTO như `expertApi.ts`). Giá trị của nó chỉ *thực sự* bền vững cho ảnh upload với `accessMode=PUBLIC` sau khi ADR-RTE-004 được áp dụng; với `PRIVATE`/`AUTHENTICATED` nó vẫn là URL 15 phút như cũ — không đổi hợp đồng cho các consumer đó.

### 5.3. Mobile — thay đổi

```dart
// pubspec.yaml — thêm flutter_html: ^3.0.0

// verified_content_detail_screen.dart — dòng ~250
// TRƯỚC: Text(content.body, style: ...)
// SAU:
Html(
  data: content.body,
  style: {
    "body": Style(margin: Margins.zero, fontSize: FontSize(15)),
    "img": Style(width: Width.auto(), height: Height.auto()),
  },
)
```

---

## 6. Dynamic Modeling

### 6.1. Sequence — Chèn ảnh vào bài viết (Happy Path)

```
Content Admin -> RichTextEditor: click "Insert Image", chọn file
RichTextEditor -> contentApi: uploadContentImage(file)
contentApi -> FileController: POST /api/v1/files/upload/with-purpose
  (kind=IMAGE, purpose=PUBLIC_CONTENT_IMAGE, accessMode=PUBLIC)
FileController -> FileServiceImpl: uploadWithPurpose(...)
FileServiceImpl -> FileServiceImpl: detect MIME thật (magic bytes) — khớp IMAGE
FileServiceImpl -> CloudinaryStorageService: store(key, bytes, mimeType)
CloudinaryStorageService -> Cloudinary: upload (type=upload, accessMode=PUBLIC)
FileServiceImpl -> UploadedFileRepository: save(UploadedFile{storageProvider=cloudinary, accessMode=PUBLIC})
FileServiceImpl -> CloudinaryStorageService: generateSignedUrl(key, 15, PUBLIC, "image")
CloudinaryStorageService -> CloudinaryStorageService: accessMode==PUBLIC -> URL KHÔNG ký, KHÔNG expires_at (ADR-RTE-004)
FileServiceImpl --> FileController: UploadFileResponse{presignedUrl = URL vĩnh viễn}
FileController --> contentApi: 201 Created
contentApi --> RichTextEditor: { url }
RichTextEditor -> RichTextEditor: editor.chain().focus().setImage({src: url}).run()
```

### 6.2. Sequence — Lưu bài viết (Sanitize trước khi persist)

```
Content Admin -> CreateContentPage/EditContentPage: click "Lưu nháp"/"Gửi phê duyệt"
Page -> AdminContentController: POST/PUT .../content { body: "<p>...<img src=...>..." }
AdminContentController -> AdminContentServiceImpl: createContent()/updateContent()
AdminContentServiceImpl -> HtmlContentSanitizer: sanitize(request.body())
HtmlContentSanitizer -> HtmlContentSanitizer: loại bỏ thẻ/attribute ngoài allowlist (ADR-RTE-005)
AdminContentServiceImpl -> ContentRepository: save(entity với body đã sanitize)
AdminContentServiceImpl -> AuditService: log(CONTENT_CREATED/CONTENT_UPDATED) — KHÔNG đổi, đã có sẵn
AdminContentServiceImpl --> Page: 200/201
```

### 6.3. Sequence — Đọc bài viết trên mobile (đã APPROVED)

```
Mother/Family (mobile) -> ContentController: GET /api/v1/content/{id}  (đã tồn tại, không đổi)
ContentController --> mobile: ContentDetailResponse { body: "<p>...</p>" }   (đã sanitize từ lúc lưu)
VerifiedContentDetailScreen -> flutter_html Html widget: render(content.body)
  (không exec script vì HTML đã sanitize server-side — ADR-RTE-005; flutter_html tự thân
   cũng không thực thi <script> vì chỉ render thành widget tree, không phải WebView)
```

---

## 7. Domain Event Catalog

Không có domain event mới. Tái sử dụng `AuditAction.CONTENT_CREATED` / `CONTENT_UPDATED` (đã có, không đổi payload) và `AuditAction.FILE_UPLOADED` (đã có, tự động log khi Content Admin chèn ảnh qua endpoint upload sẵn có).

---

## 8. API Specification

### 8.1. Endpoints — KHÔNG có endpoint mới

| Method | Path                                | Thay đổi trong tài liệu này                                                                                                                                                                                              |
| ------ | ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `POST` | `/api/v1/files/upload/with-purpose` | Không đổi contract — chỉ đổi **giá trị** `presignedUrl` trả về khi `accessMode=PUBLIC` (từ "hết hạn 15 phút" thành "vĩnh viễn", ADR-RTE-004). Không breaking change cho consumer khác (họ vẫn nhận 1 URL string hợp lệ). |
| `POST` | `/api/v1/admin/content`             | Không đổi request/response schema — chỉ thêm bước sanitize `body` trước khi lưu (server-side, trong suốt với client).                                                                                                    |
| `PUT`  | `/api/v1/admin/content/{id}`        | Tương tự.                                                                                                                                                                                                                |
| `GET`  | `/api/v1/content/{id}`              | Không đổi — `body` trả về giờ chứa HTML đã sanitize thay vì text thuần (thay đổi *nội dung*, không đổi *schema*).                                                                                                        |

### 8.2. Ví dụ request/response bị ảnh hưởng

**POST /api/v1/admin/content — body giờ là HTML:**
```json
{
  "type": "ARTICLE",
  "title": "Dinh dưỡng tháng thứ 3 thai kỳ",
  "body": "<p>Mẹ bầu nên bổ sung <strong style=\"color:#e91e63\">sắt và axit folic</strong>.</p><img src=\"https://res.cloudinary.com/.../carebridge/abc123.jpg\" alt=\"\">",
  "stage": "PREGNANCY",
  "topicId": "..."
}
```
Sau sanitize (server-side), lưu nguyên trạng nếu không có thẻ/attribute nằm ngoài allowlist; nếu client cố gửi `<script>alert(1)</script>` xen vào, thẻ đó bị loại bỏ hoàn toàn trước khi lưu (không lỗi 400 — sanitize âm thầm loại bỏ phần không an toàn, giữ lại phần hợp lệ, giống hành vi chuẩn của OWASP sanitizer).

---

## 9. Bảng mã lỗi

Không có mã lỗi mới. Validation `body` vẫn dùng `CNT-001` (400, đã có — ví dụ vượt quá 50 000 ký tự) — áp dụng trên chuỗi HTML thô trước khi sanitize.

---

## 10. Bảng tổng hợp phân quyền

Không đổi so với hiện tại (`AdminContentController`, `FileController`) — chỉ liệt kê lại phần liên quan:

| Endpoint                                                                            | `CONTENT_ADMIN` | Role khác                                                                         |
| ----------------------------------------------------------------------------------- | --------------- | --------------------------------------------------------------------------------- |
| `POST /api/v1/files/upload/with-purpose` (kind=IMAGE, purpose=PUBLIC_CONTENT_IMAGE) | ✅               | ✅ EXPERT/ADMIN/SYSTEM_ADMIN/MODERATOR/PARTNER/MOTHER (đã cho phép sẵn, không đổi) |
| `POST /api/v1/admin/content`, `PUT /api/v1/admin/content/{id}`                      | ✅               | ❌ (không đổi)                                                                     |
| `GET /api/v1/content/{id}` (public read)                                            | ✅               | ✅ mọi role đã đăng nhập (không đổi)                                               |

---

## 11. Câu hỏi mở — ĐÃ QUYẾT ĐỊNH qua AskUserQuestion (cùng phiên, trước khi viết tài liệu này)

### 11.1. Phạm vi Mobile — ✅ ĐÃ QUYẾT ĐỊNH
**Quyết định:** Bao gồm Mobile. Thêm `flutter_html`, sửa `verified_content_detail_screen.dart` (ADR-RTE-006).

### 11.2. Thư viện editor web — ✅ ĐÃ QUYẾT ĐỊNH
**Quyết định:** Tiptap (ADR-RTE-002).

### 11.3. CẦN XÁC NHẬN KHI DUYỆT TÀI LIỆU NÀY — Dependency mới (2 backend/mobile + 7 frontend)

Theo CLAUDE.md: "Không giới thiệu... dependency mà không có phê duyệt." Danh sách cần duyệt cùng lúc với việc duyệt tài liệu này (đánh dấu `Approved` ở header = coi như đồng ý cả danh sách dưới đây, trừ khi ghi chú khác):

| Dependency                                                                                                                                                                                                 | Nền tảng         | Lý do                                   | Version verify                                          |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- | --------------------------------------- | ------------------------------------------------------- |
| `@tiptap/react`, `@tiptap/pm`, `@tiptap/starter-kit`, `@tiptap/extension-underline`, `@tiptap/extension-text-style`, `@tiptap/extension-color`, `@tiptap/extension-image`, `@tiptap/extension-font-family` | Web (npm)        | Rich text editor (ADR-RTE-002)          | `^3.28.0` — đã verify tồn tại trên npm registry         |
| `com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer`                                                                                                                                       | Backend (Maven)  | Sanitize HTML server-side (ADR-RTE-005) | `20240325.1` — đã verify trên Maven Central, Apache 2.0 |
| `flutter_html`                                                                                                                                                                                             | Mobile (pub.dev) | Render HTML thành widget (ADR-RTE-006)  | `^3.0.0` — đã verify tồn tại trên pub.dev               |

### 11.4. CẦN XÁC NHẬN — Giới hạn host cho `img src` trong sanitizer (ADR-RTE-005)

**Đề xuất mặc định:** giới hạn `img src` chỉ chấp nhận host Cloudinary đã cấu hình (`res.cloudinary.com`), không cho phép host ảnh ngoài tuỳ ý — lý do PDPA (xem ADR-RTE-005). Nếu Tech Lead muốn cho phép dán URL ảnh từ nguồn ngoài (ví dụ ảnh minh hoạ từ WHO/Vinmec kèm theo nguồn tham khảo), cần nêu rõ khi duyệt để đổi lại thành "cho phép mọi `https`".

### 11.5. CẦN XÁC NHẬN — Sửa hạ tầng dùng chung (ADR-RTE-004)

`CloudinaryStorageService.generateSignedUrl()` được dùng bởi các luồng khác ngoài content (expert identity documents, credentials, contribution attachments — tất cả dùng `PRIVATE`/`AUTHENTICATED`, không đổi hành vi). Cần Tech Lead xác nhận đồng ý sửa nhánh `PUBLIC` của hàm dùng chung này trước khi implement — nếu không đồng ý, phương án B ở ADR-RTE-004 (fileId + re-fetch) là phương án dự phòng nhưng phức tạp hơn đáng kể và cần thiết kế lại cách render HTML ở cả web lẫn mobile.

---

## 12. Rollback Plan

Không có Flyway migration trong tài liệu này (không đổi schema) → rollback chỉ revert code:

```bash
# Backend
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/service/impl/CloudinaryStorageService.java
git checkout -- 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/service/AdminContentServiceImpl.java
git checkout -- 05_Development/CareBridgeAPI/pom.xml
rm -rf 05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/policy/HtmlContentSanitizer.java

# Frontend Web
git checkout -- "05_Development/CareBridgeWebApp/src/features/contentManagement/pages/CreateContentPage.tsx"
git checkout -- "05_Development/CareBridgeWebApp/src/features/contentManagement/pages/EditContentPage.tsx"
git checkout -- "05_Development/CareBridgeWebApp/package.json" "05_Development/CareBridgeWebApp/package-lock.json"
rm -rf "05_Development/CareBridgeWebApp/src/features/contentManagement/components/RichTextEditor.tsx" \
       "05_Development/CareBridgeWebApp/src/features/contentManagement/components/fontSizeExtension.ts"

# Mobile
git checkout -- 05_Development/CareBridgeMobileApp/pubspec.yaml
git checkout -- "05_Development/CareBridgeMobileApp/lib/features/community/screens/verified_content_detail_screen.dart"
```

Dữ liệu `content_items.body` đã lưu dưới dạng HTML sau khi tính năng này chạy sẽ **không tự động revert về text thuần** nếu rollback code — chấp nhận được vì `dangerouslySetInnerHTML` (web) đã render HTML an toàn (đã sanitize khi lưu) ngay cả sau khi rollback UI editor; chỉ mobile (nếu rollback `flutter_html`) sẽ tạm thời hiện lại thẻ HTML thô cho các bài đã lưu bằng editor mới — chấp nhận được như một rủi ro rollback đã biết trước, không cần xử lý thêm.

---

## 13. AI Prompt Constraints (CASE 2.0)

| #   | Constraint                                                                                                                                                                   | Nguồn       |
| --- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------- |
| C1  | Sanitize `body` PHẢI chạy server-side (`AdminContentServiceImpl`) trước khi lưu, KHÔNG chỉ dựa vào việc editor "chỉ cho tạo HTML an toàn" ở client (client có thể bị bypass) | ADR-RTE-005 |
| C2  | Sửa `CloudinaryStorageService.generateSignedUrl()` CHỈ trong nhánh `accessMode == PUBLIC` — KHÔNG đổi logic `PRIVATE`/`AUTHENTICATED`                                        | ADR-RTE-004 |
| C3  | KHÔNG tạo endpoint upload mới — tái sử dụng `POST /api/v1/files/upload/with-purpose`                                                                                         | ADR-RTE-003 |
| C4  | KHÔNG cài package Tiptap font-size cộng đồng chưa kiểm chứng — dùng `FontSize` chính thức từ `@tiptap/extension-text-style` (`TextStyleKit`), không cần custom extension (phát hiện khi implement — xem CHANGELOG) | ADR-RTE-002 |
| C5  | `flutter_html` chỉ render, KHÔNG tự sanitize lại — an toàn vì đã sanitize server-side (ADR-RTE-005); không thêm logic sanitize trùng lặp ở Flutter                           | ADR-RTE-006 |
| C6  | Không đổi kiểu cột `content_items.body` hay giới hạn 50 000 ký tự — không cần Flyway migration                                                                               | ADR-RTE-001 |

---

## PHỤ LỤC — Tham chiếu

| Document                                    | Path                                                                                                                                                                              |
| ------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Sibling pattern (ContentItem create/update) | `04_Implement/UC105_CreateContentFAQChecklist/`, `04_Implement/UC106_UpdateContentFAQChecklist/`                                                                                  |
| File upload pattern đã có (tham khảo)       | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/`, `05_Development/CareBridgeWebApp/src/features/expert/services/expertApi.ts` (`uploadContributionFile`) |

---

## 15. Xác nhận hoàn thành (Post-Implementation)

- **Backend:** `HtmlContentSanitizer` (mới, `content.policy`), `CloudinaryStorageService.generateSignedUrl()` sửa nhánh `PUBLIC` (ADR-RTE-004), `AdminContentServiceImpl` gọi sanitizer trong `createContent()`/`updateContent()` (ADR-RTE-005), dependency `owasp-java-html-sanitizer:20240325.1`. 26 test mới/mở rộng — 100% GREEN (xem Test-Spec).
- **Web:** `RichTextEditor.tsx` (Tiptap: `StarterKit` + `TextStyleKit` + `Image`, 5 package thay vì 8 dự kiến — xem CHANGELOG), `contentApi.uploadContentImage()`, nối vào `CreateContentPage.tsx`/`EditContentPage.tsx` (dùng chung cho `/content/articles` và `/content/faq`). 7 test Vitest mới — 100% GREEN, 0 regression trên 28 test toàn suite.
- **Mobile:** `flutter_html: ^3.0.0`, widget mới `VerifiedContentBody` (tách từ `verified_content_detail_screen.dart` để test được), 2 widget test mới — 100% GREEN, 0 regression trên 259 test toàn suite.
- **QA thủ công (trình duyệt thật + backend thật, không mock):**
  - Tạo bài viết ở `/content/articles` với đầy đủ in đậm, cỡ chữ 24px, kiểu đoạn — lưu và xác nhận qua `GET /api/v1/admin/content` rằng DB lưu đúng HTML mong đợi.
  - Màu chữ: automation tool không mô phỏng đúng sự kiện `change` của `<input type="color">` trong React (giới hạn công cụ, không phải bug — xác nhận bằng test nhắm trúng component thật qua `fireEvent.change`, kết quả `color: rgb(233, 30, 99)` đúng như chọn).
  - **Live security smoke test (giá trị nhất):** gọi thẳng `POST /api/v1/admin/content` với payload chứa `<script>`, `<img src="javascript:...">`, `<img src="https://evil.example.com/...">`, và `style="color:red;position:fixed;top:0"` — đọc lại từ DB xác nhận cả 4 kiểu tấn công đều bị loại bỏ đúng như thiết kế (ADR-RTE-005), chỉ giữ lại `<p>Xin chao</p>` và `<span style="color:red">test</span>`.
  - `ContentDetailPage` (staff view, `dangerouslySetInnerHTML`) hiển thị đúng in đậm + cỡ chữ lớn.
  - **Luồng chèn ảnh — QA đầu-cuối đầy đủ, đã tìm ra 2 bug thật, sửa theo hướng TÁCH RIÊNG khỏi luồng chuyên gia (xem ADR-RTE-007):** Sau khi user cập nhật `.env` với Cloudinary credential hợp lệ, live QA phát hiện tính năng vẫn không hoạt động (không phải chỉ do thiếu credential như báo cáo trước — báo cáo đó sai). Root cause: (1) `CloudinaryStorageService.store()` bỏ qua `accessMode` do caller truyền vào; (2) `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED tạo URL sai định dạng khiến Cloudinary trả HTTP 400 cho **mọi** request loại đó — bug pre-existing, ảnh hưởng cả luồng ảnh xác thực danh tính chuyên gia. Agent ban đầu đề xuất sửa gộp chung (`store()` nhận accessMode, sửa luôn PRIVATE/AUTHENTICATED) và đã implement + test GREEN sau khi user nói "tiến hành" — nhưng ngay sau đó **user hỏi lại "không tách riêng với luồng chuyên gia được à"**, một yêu cầu kiến trúc hợp lý cho code bảo mật/PII. Agent đã **revert toàn bộ phần gộp chung** và thiết kế lại: method **mới, riêng biệt** `storePublic()` chỉ dùng cho ảnh nội dung PUBLIC; `store()` và `generateSignedUrl()` nhánh PRIVATE/AUTHENTICATED **giữ nguyên byte-for-byte** (verify bằng `git diff HEAD` = rỗng cho các đoạn này). Kết quả: bug #2 (luồng chuyên gia, HTTP 400) **cố ý không sửa**, vẫn còn nguyên — khuyến nghị xử lý như task riêng có TDS/Test-Spec của nó. Verify sống trên browser+backend+Cloudinary thật (2 lần — trước và sau khi tách riêng) — upload ảnh qua editor → `presignedUrl` dạng `https://res.cloudinary.com/<cloud>/image/upload/v1/carebridge/<id>` (không `expires_at`, không chữ ký) → `curl` trực tiếp trả HTTP 200 → ảnh render đúng trong editor VÀ trong `ContentDetailPage` (trang xem chi tiết phía người đọc, `dangerouslySetInnerHTML`) → lưu bài viết → DB giữ nguyên URL qua sanitizer. Verify không regression bằng `git stash`: chạy `./mvnw test` đầy đủ trên code cũ và mới, số lỗi giống hệt nhau (204 Errors Testcontainers pre-existing + 9 Failures không liên quan trong `family`/`vaccination`).
  - 2 bài viết test đã tạo trong phiên QA trước đã được archive lại; các bài viết test tạo trong các phiên verify-fix sau đó đã bị xoá khỏi DB (không phải archive, vì tạo ra chỉ để verify bug, không có giá trị lưu vết) — không để rác trong danh sách nội dung.
  - **Kiểm tra regression cho nội dung cũ (mobile):** đổi `VerifiedContentBody` từ `Text(content.body)` sang `Html(data: content.body)` có rủi ro lý thuyết là nội dung cũ dạng plain-text với `\n` xuống dòng sẽ bị HTML gộp khoảng trắng, mất định dạng đoạn. Đã truy vấn trực tiếp bảng `content_items` (DB dev cục bộ, 25 dòng hiện có) để xác minh thực tế: `SELECT body LIKE '%<%>%' AS looks_like_html, position(chr(10) in body) > 0 AS has_newline FROM content_items` — toàn bộ 23 bản ghi nội dung cũ (seed trước tính năng này) là câu đơn, không `\n`, không `&`, không khoảng trắng kép → render qua `Html()` cho kết quả giống hệt `Text()`, xác nhận **không có regression trên dữ liệu hiện có**. Rủi ro vẫn còn về mặt lý thuyết nếu một môi trường khác (staging/production) có nội dung cũ nhiều đoạn dùng `\n` — khuyến nghị chạy lại truy vấn này trên DB đó trước khi rollout, hoặc thêm bước migrate `\n\n` → `<p>...</p>` nếu phát hiện có.
- **Không regression:** backend (`file`, `content`, `expertverification`, `expert` modules — chỉ còn lỗi môi trường Testcontainers pre-existing, xem CHANGELOG Test-Spec), web (28/28), mobile (259/259). Đã xác minh riêng: không regression trên nội dung cũ ở mobile (xem mục QA thủ công phía trên).

*Tài liệu này đã hoàn thành implementation — Status: Approved, bao gồm ADR-RTE-007 (thiết kế tách riêng cuối cùng, theo yêu cầu user). Việc còn lại (không thuộc phạm vi code của tính năng này): (1) **luồng ảnh PRIVATE/AUTHENTICATED (bao gồm xác thực danh tính chuyên gia) vẫn đang hỏng hoàn toàn (HTTP 400)** — bug pre-existing, cố ý không sửa trong tài liệu này theo yêu cầu tách biệt của user; khuyến nghị mở task riêng có TDS/Test-Spec do tính chất PII/bảo mật; (2) sửa gap môi trường Testcontainers (`expert_profiles.display_name` thiếu migration) để chạy được `ContentBodySanitizeIntegrationTest`/`RTE-TC-015`/`RTE-TC-016` tự động — pre-existing, ngoài phạm vi tài liệu này.*
