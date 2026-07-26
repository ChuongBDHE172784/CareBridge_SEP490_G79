# Lệnh sửa tiếp cho Claude Code 4.8 — Sau review commit `cba39b98`

Bạn đang làm việc tại `D:\Do_aN`, nhánh `LamVH1`. Đọc đầy đủ `AGENTS.md`,
`CLAUDE.md`, các dual-remote Git rules và ba tài liệu sau trước khi sửa:

- `CLAUDE_CODE_4_8_EXPERT_FLOW_TASK.md`
- `CLAUDE_CODE_4_8_EXPERT_FLOW_CONTINUATION.md`
- `CODEX_5_6_CONTINUITY_REPORT.md`

## 1. Kết luận review hiện tại

Không được coi commit `cba39b98` là `COMPLETE`. Báo cáo continuity mâu thuẫn
với code và tự thừa nhận Web/Mobile contribution UI mới là “optional”. Trạng
thái đúng hiện tại là **PARTIAL / NEEDS FIX**.

Các bằng chứng đã xác nhận trực tiếp từ source:

1. Commit chứa **1.049 file, 442.893 dòng thêm**, nhưng chỉ khoảng 14 file
   source/test liên quan. `.codex_tmp`, Python dependencies, PDF/PNG render,
   `android/local.properties` và nhiều tài liệu không liên quan đã bị gom vào
   cùng commit đã push.
2. `ExpertContributionsScreen` trên Mobile vẫn là placeholder. Không tìm thấy
   medical contribution domain/API/Web UI hoàn chỉnh.
3. `uploadWithPurpose()` chỉ có trên `FileServiceImpl`, không được khai báo trong
   `IFileService`, nên domain service inject interface không thể tái sử dụng API
   như báo cáo tuyên bố.
4. Identity và credential vẫn gọi `uploadPrivateFile()`. Ảnh được suy ra purpose
   thành `PUBLIC_CONTENT_IMAGE`, không phải selfie/CCCD/credential tương ứng.
5. `CloudinaryStorageService.generatePresignedUrl()` trả nguyên `key`; không tạo
   signed URL. `generateSignedUrl()` không được gọi và biến `boundedTtl` không
   tham gia URL, nên tuyên bố URL hết hạn sau 15 phút chưa đúng.
6. Cloudinary đang persist `secure_url` thay vì canonical `public_id`; tham số
   storage key bị bỏ qua. Delete/compensation có nguy cơ sai public ID, resource
   type hoặc delivery type đối với authenticated asset.
7. `uploadPublicFile()` đang cố ý cho PDF lên Cloudinary và test hiện còn khóa
   hành vi sai này. Điều đó vi phạm quy tắc DOCUMENT → R2.
8. `uploadWithPurpose()` không kiểm tra `FileKind` khai báo có khớp magic
   bytes/MIME phát hiện hay không. Caller có thể route document như IMAGE.
9. Mọi ZIP có header `PK` đang bị nhận là DOCX; code có comment “would need
   deeper inspection” nhưng báo cáo lại ghi magic-byte validation complete.
10. Với format chưa nhận diện, `detectMimeType()` vẫn tin `Content-Type` từ
    client. HEIC chưa có kiểm tra nội dung thực tế.
11. File được đọc toàn bộ nhiều lần qua `getBytes()` để detect, checksum và
    upload. Điều này không đạt yêu cầu xử lý file nặng/streaming.
12. `CommunityAnswerRepository.findQuestionIdsWithExpertAnswer()` không lọc
    `status = APPROVED`; PENDING/HIDDEN/DELETED vẫn tạo badge expert answer.
13. `ExpertEventHandlerImpl` mô tả cộng điểm khi “posted”, không phải khi
    moderation approve; hiện không tìm thấy call site. `sourceId` còn truyền
    `null`, và bảng contribution point không có unique constraint chống cộng
    trùng.
14. `AnswerStatus` không có `REQUEST_REVISION`; moderation ánh xạ request
    revision về `PENDING`, nên báo cáo không được tuyên bố có lifecycle status
    riêng nếu contract/UI không biểu diễn lý do revision.

## 2. Ưu tiên P0 — Sửa module file trước khi làm UI

### 2.1 Contract dùng chung

- Khai báo API semantic trên `IFileService`; không gọi method riêng của impl.
- Domain phải truyền `FilePurpose` và access mode rõ ràng.
- Dùng một routing policy duy nhất:
  - IMAGE JPEG/PNG/WebP → Cloudinary.
  - PDF/DOC/DOCX → Cloudflare R2.
  - Ảnh selfie/CCCD/credential → Cloudinary authenticated/private.
  - Ảnh contribution draft/pending → Cloudinary authenticated; chỉ chuyển sang
    public delivery sau approval nếu policy cho phép.
- `uploadPublicFile()` phải từ chối DOCUMENT; xóa test đang yêu cầu PDF bị đẩy
  lên Cloudinary và thay bằng test fail-closed.
- Kiểm tra detected kind khớp requested kind/purpose. Không tin enum từ caller.

### 2.2 Cloudinary identity và signed delivery

- Persist canonical Cloudinary metadata: `publicId`, `resourceType`, delivery
  type/access mode; không dùng URL tạm làm storage key.
- Upload options phải thực sự phụ thuộc `FileAccessMode`; hiện mọi upload đều
  hard-code `type=authenticated`.
- `viewFile()` phải gọi đúng signed/authenticated delivery path dựa trên record.
- TTL 15 phút phải có hiệu lực kiểm chứng được. Nếu Cloudinary signed URL thông
  thường không có expiry, dùng cơ chế auth token/private download phù hợp SDK;
  không giữ tham số TTL giả không sử dụng.
- Delete/compensation truyền đúng public ID, resource type và delivery type.
- Không nuốt lỗi cleanup hoàn toàn; log/audit và có durable cleanup record/job
  nếu xóa object thất bại.

### 2.3 Nhận diện và xử lý file an toàn

- DOCX: mở ZIP có giới hạn, xác minh `[Content_Types].xml` và
  `word/document.xml`; từ chối ZIP thường và zip bomb.
- DOC: xác minh OLE signature và giới hạn size.
- Không fallback sang declared MIME cho loại được bảo vệ. Extension, declared
  MIME và detected MIME bất nhất phải trả `FILE-001`.
- Sinh canonical extension từ detected MIME; không nối extension tùy ý từ tên
  người dùng vào object key.
- Reject empty file, filename nguy hiểm và payload vượt quota/size.
- Đọc payload một lần hoặc stream có giới hạn; checksum failure phải fail rõ,
  không lưu `checksum = null` trong khi báo cáo tuyên bố integrity complete.
- Size limit tài liệu phải configurable và thống nhất Backend/Web/Mobile.

### 2.4 Migration

- Không sửa tiếp migration đã được áp dụng. Kiểm tra `flyway_schema_history` ở
  môi trường local/disposable trước khi quyết định.
- Nếu `V20260718000004` đã chạy, tạo migration kế tiếp để sửa schema/backfill.
- Backfill không được gắn mọi Cloudinary legacy file thành
  `PUBLIC_CONTENT_IMAGE`; phải bảo toàn purpose/visibility hoặc đánh dấu
  `LEGACY_UNKNOWN` để review.
- Sau backfill, đặt constraint/nullability/index phù hợp và thêm test migration
  với fixture chứa legacy Cloudinary PDF, R2 image và record thiếu provider.

## 3. Ưu tiên P0 — Community answer lifecycle

- Sửa query `findQuestionIdsWithExpertAnswer` chỉ tính answer
  `expertLabeled=true AND status=APPROVED`.
- Kiểm tra cả search, feed và bookmark dùng cùng semantics.
- Award point/notification chỉ tại transition atomically từ non-approved sang
  `APPROVED`, không lúc post.
- Lưu `sourceId = answerId`; thêm unique constraint/idempotency theo
  `(userId, sourceType, sourceId)` để retry hoặc hai moderator không cộng trùng.
- Lock/version moderation target để hai request approve đồng thời không tăng
  `answerCount` hai lần.
- Khi APPROVED → HIDDEN/PENDING/DELETED, count và badge phải giảm đúng một lần.
- Request revision phải lưu reason/action để Web/Mobile hiển thị; không cần thêm
  enum status riêng nếu thiết kế dùng PENDING, nhưng report phải nói đúng.
- Thêm service/integration tests cho PENDING, approve, duplicate approve,
  concurrent approve, hide, revision, edit approved và delete approved.

## 4. Ưu tiên P1 — Medical contribution end-to-end

Triển khai phần mà continuity report đang gọi nhầm là optional:

- Backend domain/migration/API cho `DRAFT -> SUBMITTED -> APPROVED/REJECTED`,
  optimistic locking, rejection reason và audit.
- Attachment liên kết bằng `fileId`, kind, order, owner, purpose; không lưu URL
  tạm và không chiếm file của user/contribution khác.
- Web có màn danh sách, tạo/sửa draft, hai thanh upload, submit và trạng thái.
- Mobile thay `ExpertPlaceholderScreen` bằng flow thật và service/model/test.
- Admin/Content Admin có review queue/detail/approve/reject.
- Hai thanh bắt buộc:
  - Hình ảnh JPEG/PNG/WebP → Cloudinary.
  - PDF/DOC/DOCX → R2.
- Có progress/retry/remove từng file, reload draft không mất attachment, refresh
  signed/presigned URL khi hết hạn.
- Chỉ expert APPROVED + trust ACTIVE được create/submit.

## 5. Ưu tiên P0 — Dọn phạm vi commit đã push

Không force-push hoặc rewrite history nếu người dùng chưa cho phép. Tạo follow-up
cleanup commit an toàn:

- Bỏ `.codex_tmp/**` khỏi Git tracking và thêm `.codex_tmp/` vào `.gitignore`;
  ưu tiên `git rm --cached` để không xóa bản local của người dùng.
- Bỏ `CareBridgeMobileApp/android/local.properties` khỏi tracking nếu đây là
  machine-local config; thêm ignore theo convention Flutter.
- Liệt kê riêng các PDF/DOCX/report lớn đã bị commit và hỏi người dùng trước khi
  loại chúng nếu chưa rõ chúng là deliverable hay artifact tạm.
- Không dùng `git add -A`. Stage explicit từng nhóm file.
- Chia commit: storage fix, Community lifecycle, contribution Backend, Web,
  Mobile, tests, repository hygiene.

## 6. Test bắt buộc

Backend phải bổ sung và chạy:

```powershell
cd D:\Do_aN\05_Development\CareBridgeAPI
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd "-Dtest=*File*,*CommunityAnswer*,*Moderation*,*Contribution*,*ExpertIdentity*,*ExpertCredential*" test
```

Các case file tối thiểu: ảnh authenticated view, TTL, public image, PDF/DOC/DOCX
R2, ZIP giả DOCX, spoof MIME, mismatched kind, empty/oversize, R2 unavailable,
DB failure compensation và cleanup failure.

Web/Mobile:

```powershell
cd D:\Do_aN\05_Development\CareBridgeWebApp
npm run build
npm test

cd D:\Do_aN\05_Development\CareBridgeMobileApp
flutter analyze
flutter test test/features/expert
flutter test test/features/community
```

Không ghi PASS nếu pattern/path discover 0 test. Báo số test thực tế.

## 7. Runtime acceptance

Chỉ dùng PII giả và prefix test:

1. Selfie/CCCD upload Cloudinary authenticated; DB lưu public ID, admin xem được
   URL có hạn và user khác bị từ chối.
2. PNG contribution upload Cloudinary; draft không public.
3. PDF, DOC, DOCX upload R2; DB/provider đúng và presigned URL refresh được.
4. ZIP giả DOCX, MIME spoof và missing R2 đều fail closed.
5. Expert trả lời question: PENDING không tạo badge/count/point; APPROVED mới tạo
   đúng một lần; approve retry/concurrent không nhân đôi.
6. Draft contribution sống qua reload, reject/resubmit/approve hoàn chỉnh trên
   Backend, Web và Mobile.

## 8. Báo cáo và điều kiện dừng

Sau khi làm xong, tạo:

```text
CODEX_5_6_POST_REVIEW_FIX_REPORT.md
```

Báo cáo phải ghi commit, file source, test discovered/pass/fail, runtime proof đã
redact, cleanup Git và rủi ro còn lại. Chỉ ghi `COMPLETE` khi contribution UI
không còn placeholder và có runtime proof Cloudinary/R2. Nếu chưa có môi trường
runtime hoặc integration tests còn fail, ghi `PARTIAL`, không gọi là non-blocking
một cách mặc định.

Không commit/merge/push cho đến khi người dùng yêu cầu rõ trong phiên mới.
