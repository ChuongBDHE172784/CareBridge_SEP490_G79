# LỆNH THỰC THI CHO CLAUDE CODE — HOÀN THIỆN LUỒNG CHUYÊN GIA CAREBRIDGE

**Ngày audit:** 20/07/2026  
**Repository:** `D:\Do_aN`  
**Nhánh làm việc chuẩn:** `LamVH1`  
**Báo cáo đầu vào:** `IMPLEMENTATION_REPORT_CONTRIBUTION_FEATURE.md`  
**Phạm vi:** Backend Spring Boot, Web React, Mobile Flutter, PostgreSQL/Flyway, CompreFace, Cloudinary, Cloudflare R2

> Đây là lệnh triển khai, không phải tài liệu tham khảo tùy chọn. Không được tuyên bố hoàn thành nếu chưa chạy thật Backend, mở lần lượt các màn Web/Mobile, thực hiện upload thật và ghi lại bằng chứng. Không in, chụp hoặc commit giá trị bí mật trong `.env`.

## 1. Kết luận audit hiện tại

Trạng thái đúng là **PARTIAL / NOT RUNTIME-VERIFIED**, không phải Complete.

- Backend `mvnw -DskipTests compile`: đã qua ở thời điểm audit.
- Web `npm run build`: đã qua, nhưng build không phát hiện luồng upload giả.
- Flutter analyze phạm vi Expert/Router: còn 11 cảnh báo/lỗi cần xử lý.
- Chưa có bằng chứng chạy migration trên database sạch, upload thật lên Cloudinary/R2, xem ảnh/tài liệu bằng URL thật, hoặc đi hết journey bằng Web và Mobile.
- Báo cáo đầu vào mô tả sai một số endpoint và tuyên bố test chưa có bằng chứng tương ứng trong source tree.

### Các blocker P0 đã xác minh

| ID | Vấn đề | Hậu quả bắt buộc xử lý |
|---|---|---|
| P0-01 | Web `ContributionDraftPage.tsx` đang tạo `mockFileId` thay vì upload | Payload gửi ID không phải UUID; luồng đóng góp không chạy thật |
| P0-02 | Mobile gọi `POST /api/v1/files/upload/with-purpose`, Backend không có endpoint này | Upload Mobile trả 404 |
| P0-03 | `FileController` upload tổng quát chỉ cho `MOTHER` | Không tái sử dụng được cho EXPERT/MEMBER |
| P0-04 | `CloudinaryStorageService` suy access mode từ MIME, có thể bỏ qua policy/purpose; logic URL ký gắn `expires_at` vào public ID | DB và object lệch quyền; ảnh không tải/TTL không đáng tin |
| P0-05 | `MedicalContributionService.getById` không kiểm tra owner/status; mapping attachment gọi `viewFile(..., ownerUserId)` thay vì caller thật | Người đăng nhập có UUID có thể đọc draft/private file của người khác |
| P0-06 | `CommunityAnswerController` cho expert chưa được duyệt trả lời rồi chỉ bỏ nhãn expert | Trái yêu cầu nghiệp vụ: expert chưa được Admin xác minh phải bị 403 |
| P0-07 | Migration contribution dùng `specialty_id UUID`, `hospital_id UUID` trong khi entity/master data dùng String | Nguy cơ Flyway/runtime schema fail |
| P0-08 | Mobile `expert_answer_composer_screen.dart` còn placeholder | Chưa có luồng trả lời câu hỏi cộng đồng |

## 2. Mục tiêu sản phẩm và luật bất biến

Hoàn thiện một journey nhất quán:

1. Người dùng đăng ký/đăng nhập với vai trò chuyên gia.
2. Tạo hồ sơ chuyên môn bằng master data thật (chuyên khoa, bệnh viện), không nhập ID tùy ý.
3. Gửi ba ảnh riêng: selfie, CCCD mặt trước, CCCD mặt sau.
4. Ảnh được kiểm tra loại/kích thước/nội dung, lưu Cloudinary với purpose rõ ràng và quyền `AUTHENTICATED`; CompreFace chỉ đưa ra tín hiệu hỗ trợ.
5. Admin xem được đủ ảnh, kết quả đối sánh, tài liệu chứng chỉ và audit trail; Admin phê duyệt/từ chối có lý do.
6. Chỉ khi hồ sơ cuối cùng `APPROVED` và trust còn hoạt động, expert mới được trả lời câu hỏi, gửi tài liệu y khoa hoặc dùng chức năng chuyên gia bị giới hạn.
7. Ảnh nội dung công khai lưu Cloudinary. PDF/DOC/DOCX nặng lưu Cloudflare R2 và mặc định `PRIVATE`.
8. Mọi upload đi qua một module file dùng chung, có thể tái sử dụng sau này cho MEMBER; UI có hai vùng upload tách biệt.

### Ma trận lưu trữ chuẩn

| Nhóm file | Provider | Access mặc định | Ví dụ purpose |
|---|---|---|---|
| Selfie/CCCD | Cloudinary | AUTHENTICATED | `EXPERT_IDENTITY_SELFIE`, `EXPERT_IDENTITY_FRONT`, `EXPERT_IDENTITY_BACK` |
| Ảnh thumbnail/nội dung đã công khai | Cloudinary | PUBLIC sau khi được phép công khai | `CONTRIBUTION_THUMBNAIL`, `PUBLIC_CONTENT_IMAGE` |
| Chứng chỉ/tài liệu nguồn PDF, DOC, DOCX | Cloudflare R2 | PRIVATE | `EXPERT_CREDENTIAL`, `CONTRIBUTION_SOURCE`, `CONTRIBUTION_REFERENCE` |

Client **không được tự chọn** provider hoặc access mode tùy ý. Client gửi `purpose`; Backend ánh xạ allowlist `purpose → kind/provider/access/size/mime/roles`. Không dùng tên file hay MIME do client khai báo làm bằng chứng duy nhất.

## 3. Thiết kế module file dùng chung

Tạo contract chung có version, ví dụ:

- `POST /api/v1/files` multipart gồm `file` và `purpose`; cho các role được policy cho phép.
- `GET /api/v1/files/{fileId}` trả metadata và URL xem ngắn hạn khi caller có quyền.
- `DELETE /api/v1/files/{fileId}` chỉ owner/admin và chỉ khi file chưa bị khóa bởi bản ghi nghiệp vụ.

Không tạo thêm endpoint lệch tên giữa Web/Mobile. Nếu giữ endpoint khác vì tương thích, phải có adapter/deprecation test và cả hai client dùng một contract canonical.

Backend phải:

- Có `FilePurposePolicyRegistry` hoặc tương đương dùng chung, không rải `if` theo màn hình.
- Sniff magic bytes an toàn. Với DOCX phải xác minh cấu trúc ZIP/OpenXML, không chấp nhận mọi file mở đầu `PK`.
- Giới hạn kích thước theo purpose; chống zip bomb; chuẩn hóa tên; không dùng original filename làm storage key.
- Stream/hash một cách có kiểm soát; tránh nhiều lần `getBytes()` với file nặng.
- Lưu `ownerUserId`, `provider`, `objectKey/publicId`, MIME đã phát hiện, size, SHA-256, purpose, access, trạng thái, thời gian.
- Không nuốt lỗi checksum/delete. Có retry/outbox hoặc trạng thái cleanup rõ ràng.
- Không ghép metadata provider bằng chuỗi phân cách dễ vỡ nếu có thể dùng cột/JSON có schema.
- Không trả direct URL riêng tư dài hạn. URL R2/Cloudinary riêng tư phải hết hạn thật và được kiểm thử trước/sau TTL.
- `viewFile` luôn nhận caller thật từ SecurityContext; tuyệt đối không truyền `ownerUserId` thay caller để vượt quyền.

## 4. Sửa Cloudinary và lỗi ảnh không hiển thị

Đi theo chuỗi bằng chứng, không sửa mò:

1. Upload một ảnh test vô hại qua API thật.
2. Kiểm tra record `uploaded_files`: provider, storage key/public ID, resource type, delivery type, access, purpose.
3. Kiểm tra object trong Cloudinary bằng prefix test, không lộ secret.
4. Gọi endpoint xem file bằng owner và Admin; ghi status/response shape đã che token.
5. Mở URL trong `<img>` Web và Image widget Flutter; ghi network status, content-type và lỗi `onError` nếu có.
6. Sửa `CloudinaryStorageService` để tôn trọng policy đã resolve; dùng cơ chế Cloudinary chính thức cho public/authenticated/private delivery và expiry. Không chèn query vào public ID.
7. Chuẩn hóa API response: client chỉ lấy đúng `data.presignedUrl`/`data.url`; tránh đoán nhiều shape.
8. Thêm loading, fallback, nút thử lại, thông báo URL hết hạn; refresh URL khi 401/403/expired.
9. Test ảnh selfie/CCCD chỉ owner và Admin được xem; thumbnail approved public xem được không cần token nếu policy chọn PUBLIC.

`ExpertIdentityVerificationServiceImpl` phải gọi `uploadWithPurpose` riêng cho cả ba ảnh, không dùng `uploadPrivateFile` khiến purpose bị suy đoán sai.

## 5. Hoàn thiện Backend theo domain

### 5.1 Onboarding và xác minh chuyên gia

- Dùng state machine rõ ràng cho profile, identity, credential và final verification.
- Final approval chỉ thành công khi identity mới nhất APPROVED và credential bắt buộc APPROVED/còn hạn.
- CompreFace là advisory: lưu score/threshold/model outcome; lỗi service phải cho trạng thái `MANUAL_REVIEW_REQUIRED`, không tự duyệt và không làm mất hồ sơ.
- Chống submit kép bằng transaction, optimistic/pessimistic lock phù hợp và idempotency key.
- Admin review phải lưu reviewer, timestamp, reason, previous/new status; reject reason bắt buộc.
- Suspended/expired/rejected expert bị chặn ngay ở service layer, không chỉ guard giao diện.

### 5.2 Community Q&A

- Khi caller có role EXPERT, trước `postAnswer` phải kiểm tra final verification `APPROVED` và trust active. Không đạt: trả 403 với error code ổn định; không âm thầm hạ xuống câu trả lời thường.
- Nếu sản phẩm cho MEMBER trả lời, giữ policy riêng cho MEMBER; không dùng policy đó để bypass EXPERT.
- Chỉ answer `APPROVED` xuất hiện trong truy vấn công khai.
- Điểm đóng góp chỉ cộng đúng một lần khi chuyển sang APPROVED. Unique key phải có `sourceId` non-null và test retry/concurrency.
- Thực hiện composer thật trên Web/Mobile: nội dung, validation, submit, pending moderation, retry; bỏ placeholder.

### 5.3 Medical contribution

- Canonical base path hiện tại phải thống nhất theo code hoặc migration có chủ đích. Báo cáo cũ ghi `/api/expert/...` nhưng code dùng `/api/v1/contributions`; chọn một contract và sửa toàn bộ Backend/Web/Mobile/test/tài liệu.
- GET detail: owner xem draft/rejected của mình; Admin reviewer xem theo quyền; public/member chỉ xem APPROVED. Không dùng `isAuthenticated()` đơn thuần.
- Re-check eligibility khi create, update và submit; không cho expert vừa bị suspended vẫn submit draft cũ.
- Cho REJECTED quay lại DRAFT/resubmit theo version mới, giữ lịch sử review.
- Validate attachment ACTIVE, đúng owner, đúng purpose/provider/access, chưa gắn trái phép; trả lỗi domain thay vì chờ unique constraint.
- Dọn orphan file theo retention job; không xóa object đang được tham chiếu.
- Tránh N+1 khi list và ký URL; không ký URL file riêng tư nếu danh sách chưa cần preview.
- Thêm version/ETag hoặc trường `version` trong command để xử lý hai tab cùng sửa; trả 409 dễ hiểu.
- Sửa migration: type của `specialty_id`/`hospital_id` phải khớp master data/entity; thêm FK/index cần thiết và chạy trên database sạch.

## 6. Hoàn thiện Web React

- Thay hoàn toàn `mockFileId` bằng `fileApi.upload(file, purpose)` dùng chung.
- Hai vùng rõ ràng: **Ảnh (Cloudinary)** và **Tài liệu PDF/DOC/DOCX (Cloudflare R2)**. UI chỉ nói đích lưu để giải thích; quyết định thật vẫn ở Backend.
- Mỗi file có progress, size/type, success/error, retry/remove; chỉ add attachment sau khi upload thành công.
- Master data specialty/hospital là searchable select; loại TODO/free-text ID.
- Onboarding hiển thị stepper và chỉ một CTA chính theo `nextStep`.
- Admin queue tải URL bằng endpoint có quyền, có skeleton/fallback/refresh URL; không suy loại file từ extension của signed URL, dùng metadata MIME.
- Bổ sung error boundary/toast field errors; 401 refresh auth, 403 giải thích chưa đủ quyền, 409 yêu cầu reload, 413 báo quá dung lượng.
- Community answer composer chỉ render enabled khi eligible, nhưng Backend vẫn là nguồn bảo vệ cuối.

### Trật tự nút và cải thiện UI

Trên Expert dashboard/onboarding, sắp theo journey:

1. `Hoàn thiện hồ sơ`.
2. `Xác minh danh tính`.
3. `Gửi chứng chỉ`.
4. `Theo dõi xét duyệt`.
5. Sau APPROVED mới mở `Trả lời cộng đồng`, `Gửi tài liệu y khoa`, `Lịch tư vấn`, `Hồ sơ công khai`.

Mỗi màn chỉ có một primary CTA. `Lưu nháp` là secondary; `Gửi xét duyệt` là primary; `Xóa/Hủy` là destructive và cần confirm. Không xếp các nút nguy hiểm sát nút gửi. Đảm bảo responsive, keyboard focus, label/aria, contrast, empty/loading/error/success states và tiếng Việt nhất quán.

## 7. Hoàn thiện Mobile Flutter

- Đổi service upload sang endpoint canonical thật; dùng multipart streaming/file path, không nạp toàn bộ PDF/DOCX nặng vào RAM.
- Bỏ TODO master data và free text ID; dùng picker/search tương thích Web.
- Hoàn thiện `expert_answer_composer_screen.dart`; xóa/redirect màn contribution placeholder cũ để không có hai entry point mâu thuẫn.
- Xử lý 11 issue từ analyze: BuildContext qua async gap, import thừa, underscore thừa và các lỗi liên quan.
- Preview ảnh/tài liệu phải dùng URL đúng quyền, refresh khi hết hạn, placeholder/error retry.
- UX hai vùng upload, progress, retry/remove, validation và button hierarchy giống Web.
- Chạy trên ít nhất Android emulator hoặc thiết bị thật; nếu dùng Flutter Web chỉ là bằng chứng bổ sung, không thay Android cho file picker.

## 8. Contract và bảo mật bắt buộc

- Ghi một bảng API canonical cho method/path/role/request/response/error code và dùng chung cho ba nền tảng.
- DTO không trả stack trace, storage secret, raw provider response hay PII không cần thiết.
- Rate limit upload/face verification/answer submit; audit action Admin.
- CORS chỉ allow origin cấu hình; không nới `*` với credential.
- Log có correlation ID và file ID, không log secret, ảnh base64, presigned URL đầy đủ hoặc số CCCD.
- `.env` chỉ kiểm tra **tên biến**, không in giá trị. Các tên hiện có cần dùng: Cloudinary, R2 endpoint/access/secret/bucket/region, CompreFace base URL/API key/threshold/enabled. Chuẩn hóa `R2_BUCKET_NAME` và fallback, không tạo hai nguồn mâu thuẫn.
- Không commit `.env`, object test chứa PII, ảnh CCCD thật, keystore, local properties hoặc URL có token.

## 9. Test tự động tối thiểu phải bổ sung

### Backend

- File policy matrix cho EXPERT/MEMBER/ADMIN, purpose và MIME/magic bytes.
- Cloudinary public/authenticated URL; R2 private URL; owner/admin/stranger; expired URL.
- Identity submit ba ảnh; CompreFace success/timeout/down; Admin approve/reject; submit kép.
- Expert unverified/suspended/expired trả lời community phải 403; verified được submit.
- Contribution ownership/status; rejected resubmit; stale version 409; unauthorized UUID cannot read attachment.
- Flyway migration trên PostgreSQL sạch; repository integration; concurrent moderation chỉ cộng điểm một lần.

### Web/Mobile

- Không còn `mockFileId`, TODO hoặc endpoint upload không tồn tại.
- Component/widget test hai vùng upload, progress/error/retry, master data, guard trạng thái.
- E2E/smoke tối thiểu cho onboarding, Admin approval, answer, contribution và xem file.

Không ghi “71 backend tests/209 mobile tests pass” nếu không kèm command, timestamp, exit code và summary thực tế của lần chạy hiện tại.

## 10. Quy trình chạy thật bắt buộc sau khi code

Claude phải chạy từng bước và sửa đến khi sạch. Không được chỉ build.

1. Kiểm tra Docker và health CompreFace đang chạy; không recreate/xóa volume nếu không cần.
2. Dùng database test/dev an toàn. Chạy Flyway trên database sạch và database upgrade snapshot; không đụng production/shared data.
3. Khởi chạy Backend bằng `05_Development/CareBridgeAPI/.\mvnw.cmd spring-boot:run` với env được nạp an toàn. Chờ READY, gọi health endpoint và giữ log phục vụ smoke test.
4. Chạy backend test mục tiêu, rồi suite phù hợp. Nếu có test thất bại sẵn, chứng minh bằng baseline; không gọi chung là “unrelated” khi chưa đối chiếu.
5. Chạy Web `npm run build`, sau đó `npm run dev`; mở trình duyệt và kiểm tra Network/Console.
6. Đi lần lượt bằng tài khoản Expert mới: đăng ký → hồ sơ → selfie/CCCD → trạng thái chờ.
7. Bằng Admin: mở đủ ba ảnh → review identity → review credential → final approve.
8. Quay lại Expert: trả lời câu hỏi → thấy trạng thái moderation; gửi contribution với một ảnh thật và một PDF/DOCX test; save draft → edit → submit.
9. Bằng Admin: xem đúng ảnh/tài liệu, approve/reject có lý do. Bằng người khác: không đọc được draft/private file. Với contribution approved: kiểm tra hiển thị theo policy.
10. Chạy Mobile `flutter analyze`, `flutter test`, sau đó `flutter run` trên Android; đi lại journey tương đương, đặc biệt file picker/upload/preview.
11. Kiểm tra song song: hai tab update cùng draft, hai Admin review cùng item, retry upload, double-click submit, URL hết hạn, CompreFace tắt giữa request.
12. Sau mỗi nhóm thay đổi, restart Backend nếu cần và mở lại đúng các màn bị ảnh hưởng. Không bỏ qua lỗi console/log dù UI có vẻ chạy.

### Checklist màn hình phải mở tuần tự

- Expert registration/login.
- Expert onboarding/status stepper.
- Expert profile/master data.
- Identity upload và ảnh preview.
- Credential upload/list/detail.
- Admin identity queue và image viewer.
- Admin credential/final verification queue.
- Expert dashboard sau approval.
- Community question detail + answer composer.
- Expert contribution list/new/edit/detail.
- Admin contribution review queue/detail.
- Public/member view của nội dung approved.

Với mỗi màn ghi: URL/route, account role, action, API status, kết quả UI, console/backend error và ảnh chụp đã che PII.

## 11. Điều kiện nghiệm thu Given/When/Then

- **Given** expert chưa final-approved, **when** gửi answer/contribution, **then** Backend trả 403 ổn định và không tạo record.
- **Given** expert approved/active, **when** gửi answer, **then** tạo pending moderation; chỉ approved mới public và điểm chỉ cộng một lần.
- **Given** ảnh purpose hợp lệ, **when** upload, **then** object ở Cloudinary, metadata khớp và caller hợp lệ xem được; stranger không xem ảnh identity.
- **Given** PDF/DOC/DOCX hợp lệ, **when** upload, **then** object ở R2, URL ngắn hạn hoạt động rồi hết hạn; file giả mạo bị từ chối.
- **Given** Web/Mobile upload, **when** tạo contribution, **then** attachment UUID thật tồn tại; không có mock ID hoặc 404.
- **Given** caller biết UUID draft người khác, **when** GET detail/file, **then** nhận 403/404 và không có URL rò rỉ.
- **Given** hai request review đồng thời, **when** cùng approve, **then** một transition hợp lệ, không nhân đôi điểm/audit.
- **Given** CompreFace unavailable, **when** submit identity, **then** hồ sơ đi manual review an toàn, không auto-approve và không mất ảnh.
- **Given** migration sạch và upgrade, **when** Backend start, **then** Flyway thành công và type master data nhất quán.

## 12. Git và cách bàn giao

Tuân thủ tuyệt đối:

- Đọc `.claude/rules/git-dual-remote.md`, `.claude/skills/git-dual-remote-handler.md`, `.claude/workflows/start-day.md`, `.claude/workflows/end-day.md` trước mọi thao tác Git.
- Canonical branch là `LamVH1`; không commit feature/fix trực tiếp trên `dev`.
- Không trộn thay đổi generated/unrelated. Trước commit kiểm tra `git config user.email`, status, staged diff và secret scan.
- Commit semantic, nhỏ theo domain. Không push trước khi pull `dev` từ cả `github` và `gitlab`; không dùng `origin`; không force push.
- Không ghi đè thay đổi hiện hữu của người khác. Nếu gặp dirty files ngoài phạm vi, tách/stash theo workflow và ghi rõ.

Tạo báo cáo cuối tại `D:\Do_aN\CLAUDE_EXPERT_FLOW_RUNTIME_COMPLETION_REPORT.md`, gồm:

1. Commit và danh sách file thay đổi theo Backend/Web/Mobile.
2. API contract cuối cùng và migration đã chạy.
3. Bảng test: command, thời gian, exit code, pass/fail/skipped.
4. Bằng chứng upload Cloudinary/R2 đã che secret và cleanup object test.
5. Bảng smoke từng màn Web/Mobile.
6. Bằng chứng RBAC negative tests và concurrency.
7. Lỗi còn lại, mức độ, lý do; không dùng từ Complete nếu còn P0/P1 hoặc chưa có runtime evidence.

## 13. Definition of Done

Chỉ đánh dấu **COMPLETE** khi đồng thời thỏa:

- Không còn P0/P1 ở mục 1.
- Backend chạy thật, Flyway thật, health tốt.
- Web build và journey trình duyệt thật qua đủ màn.
- Flutter analyze sạch ở phạm vi thay đổi, test pass và journey Android thật chạy được.
- Upload ảnh Cloudinary và tài liệu R2 thật hoạt động, preview/view/download đúng quyền.
- Expert chưa duyệt bị chặn trả lời/gửi contribution ở Backend; expert duyệt làm được.
- Không có mock upload, placeholder chức năng, TODO đường chính hoặc contract endpoint lệch nền tảng.
- Có test ownership, expiry, failure, concurrency và báo cáo bằng chứng trung thực.

