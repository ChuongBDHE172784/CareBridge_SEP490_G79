# Lệnh tiếp tục cho Claude Code 4.8 — Expert Community & Medical Contribution

Bạn đang làm việc trong repository `D:\Do_aN`, nhánh `LamVH1`. Đây là phiên
tiếp nối sau `EXPERT_REGISTRATION_FLOW_COMPLETION_REPORT.md`. Không coi chữ
`COMPLETE` trong báo cáo là bằng chứng cuối cùng: phải kiểm tra code và chạy lại
test. Báo cáo hiện mâu thuẫn với yêu cầu storage, và màn Mobile contribution
vẫn chỉ là placeholder.

## 1. Kết quả phải đạt

Hoàn thiện end-to-end ba đoạn nối tiếp nhau:

1. Người dùng đăng ký, hoàn thành hồ sơ/định danh/chứng chỉ và được admin duyệt
   thành chuyên gia.
2. Chuyên gia đã duyệt đọc câu hỏi cộng đồng, gửi câu trả lời, theo dõi trạng
   thái moderation; badge/count/point/notification chỉ thay đổi sau khi answer
   được `APPROVED`.
3. Chuyên gia tạo draft và gửi tài liệu y khoa gồm hai vùng upload độc lập:
   **Hình ảnh** lưu Cloudinary và **Tệp tài liệu PDF/DOC/DOCX** lưu Cloudflare
   R2. Web, Mobile và Backend phải dùng cùng contract và module file dùng chung.

## 2. Quy tắc storage không được diễn giải khác

| Loại binary | Provider bắt buộc | Quyền truy cập |
|---|---|---|
| JPEG/PNG/WebP của bài Community/medical contribution | Cloudinary | Private/authenticated khi draft hoặc pending; chỉ public delivery sau approval nếu policy cho phép |
| Selfie, CCCD trước/sau hoặc ảnh chứng chỉ | Cloudinary | Private/authenticated, URL ký có hạn; tuyệt đối không public |
| PDF | Cloudflare R2 | Private, presigned GET tối đa 15 phút |
| DOC | Cloudflare R2 | Private, presigned GET tối đa 15 phút |
| DOCX | Cloudflare R2 | Private, presigned GET tối đa 15 phút |

Không route theo tên màn hình. Route theo `FileKind`, MIME/magic bytes và
`FilePurpose`. Không fallback tài liệu từ R2 sang Cloudinary khi R2 thiếu cấu
hình. Không lưu presigned URL trong database. Database chỉ lưu `fileId`, owner,
provider, storage key/public ID, purpose, access mode, MIME chuẩn hóa, size,
original name, checksum và status.

Tên đúng là **Cloudflare R2** và **Cloudinary**. Không ghi `Cloudfare`,
`Cloudanry` trong code/API/config mới.

## 3. Hiện trạng đã xác nhận từ code — không được bỏ qua

- `FileServiceImpl.ALLOWED_MIME` chưa có WebP, DOC và DOCX.
- `detectMimeType()` mới nhận diện JPEG/PDF/PNG/GIF; đang có khả năng tin
  `Content-Type` client cho định dạng chưa nhận diện.
- `uploadPublicFile()` luôn đưa sang Cloudinary và `uploadPrivateFile()` luôn
  đưa sang R2, chưa thể biểu diễn ảnh private trên Cloudinary.
- `CloudinaryStorageService` đang giả định public HTTPS URL; cần hỗ trợ
  authenticated/signed delivery cho ảnh nhạy cảm và draft.
- `UploadedFile` chưa có `kind`, `purpose`, `accessMode`, checksum hoặc lifecycle
  liên kết attachment.
- `ExpertContributionsScreen` trên Mobile vẫn là
  `ExpertPlaceholderScreen`; báo cáo “end-to-end complete” là chưa đủ bằng chứng.
- Phải rà lại Web contribution/review pages và Backend contribution domain;
  nếu chưa tồn tại thì triển khai theo kiến trúc modular monolith hiện có.

## 4. Work package A — Module file dùng chung

Thiết kế API dùng lại được cho Expert, Mother, Family và Member. Có thể điều
chỉnh tên theo convention dự án, nhưng phải có một entry point rõ nghĩa, ví dụ:

```java
UploadFileResponse upload(
    MultipartFile file,
    UUID ownerId,
    FilePurpose purpose,
    FileAccessMode accessMode
);
```

Backend tự suy ra `FileKind.IMAGE` hoặc `FileKind.DOCUMENT` từ nội dung file:

- IMAGE: JPEG, PNG, WebP; lưu Cloudinary.
- DOCUMENT: PDF, DOC, DOCX; lưu R2.
- DOC phải kiểm tra OLE Compound File magic bytes.
- DOCX phải là ZIP hợp lệ và có cấu trúc Office/Word cần thiết; không chấp nhận
  một ZIP bất kỳ chỉ vì extension là `.docx`.
- Không tin extension hay `MultipartFile.getContentType()` một mình.
- Giới hạn size/quota phải configurable; trả error code ổn định để Web/Mobile
  hiển thị. File lớn phải stream, không gọi `getBytes()` nhiều lần.
- Nếu upload object thành công nhưng ghi DB/audit/link attachment thất bại, phải
  compensation delete hoặc ghi durable cleanup job; test failure path.
- Delete Cloudinary dùng đúng `public_id`, `resource_type` và access mode.
- Record cũ thiếu provider/kind phải được backfill hoặc có compatibility rule
  an toàn, không đoán tài liệu là Cloudinary.

Các controller/domain khác chỉ gọi module file; không inject SDK Cloudinary/S3
và không tự xây URL.

## 5. Work package B — Hai thanh upload trên Web và Mobile

Màn đóng góp tài liệu y khoa phải có hai khu vực nhìn tách biệt:

### Thanh Hình ảnh

- Chỉ chọn JPEG/PNG/WebP, hỗ trợ nhiều ảnh theo giới hạn Backend.
- Thumbnail preview, đổi/xóa ảnh, thứ tự hiển thị, progress và retry từng ảnh.
- Hiển thị lỗi riêng từng item; một ảnh lỗi không làm mất draft hoặc ảnh đã tải.
- Upload thành công trả `fileId`; client không lưu Cloudinary key/secret.

### Thanh Tệp tài liệu

- Chỉ chọn PDF/DOC/DOCX.
- Hiển thị icon loại file, tên, dung lượng, progress, retry và remove.
- Không render DOC/DOCX bằng `<img>`. Preview PDF chỉ qua URL được cấp quyền;
  DOC/DOCX dùng download/open action phù hợp.
- Tệp thực tế phải xuất hiện trong bucket R2 test và DB ghi
  `storage_provider = r2`.

Web và Mobile dùng cùng response model. Khi presigned URL hết hạn, gọi lại API
view để lấy URL mới. Không giữ URL tạm trong local persistence.

## 6. Work package C — Medical contribution lifecycle

Tạo/hoàn thiện domain contribution với tối thiểu:

```text
DRAFT -> SUBMITTED -> APPROVED
                   -> REJECTED -> DRAFT (sửa và gửi lại)
```

Yêu cầu:

- Chỉ expert có `verificationStatus = APPROVED` và trust status hợp lệ được tạo
  hoặc submit.
- Metadata: title, summary/body, topic/category, clinical disclaimer, author,
  version, timestamps và rejection reason.
- Attachment liên kết bằng `fileId`, `kind`, display order; kiểm tra owner và
  purpose trước khi link. Không cho một file thuộc contribution khác bị chiếm.
- Save draft idempotent; submit dùng optimistic locking/idempotency key.
- Admin/Content Admin có queue, detail, approve/reject và audit trail.
- Draft/submitted/rejected không bị truy cập công khai. Approved contribution
  chỉ public đúng phần được policy cho phép; tài liệu R2 vẫn phát URL có hạn.
- Cleanup attachment mồ côi theo retention policy, không hard-delete tức thì khi
  người dùng chỉ remove khỏi form.

Hoàn thiện API, migration mới, Backend service/tests, Web screens/services/tests
và Flutter screens/services/fake-driven tests. Không sửa migration đã áp dụng.

## 7. Work package D — Trả lời câu hỏi Community

Không chỉ kiểm tra rằng endpoint tồn tại. Chứng minh toàn lifecycle:

- Expert chưa được duyệt không thể post answer gắn nhãn chuyên gia.
- Composer Web/Mobile tải đủ question detail, xử lý locked/deleted question và
  hiển thị disclaimer y khoa.
- Submit tạo answer `PENDING`; UI hiển thị đang chờ duyệt.
- `answerCount` và `hasExpertAnswer` chỉ tính `APPROVED`.
- Contribution point và notification phát khi transition sang `APPROVED`, đúng
  một lần kể cả hai moderator xử lý đồng thời hoặc request bị retry.
- `HIDDEN`, `DELETED`, `REQUEST_REVISION` phải cập nhật count/badge nhất quán.
- Moderator approval khác hoàn toàn “accepted/best answer”; không nhập hai khái
  niệm thành một nếu chưa có schema/API riêng.

Thêm concurrency/idempotency tests cho approve và test authorization cho expert
chưa verified.

## 8. Runtime proof bắt buộc

Dùng account và dữ liệu giả, prefix object test riêng. Không dùng PII thật.

1. Upload một PNG ở vùng Hình ảnh; xác nhận provider Cloudinary, DB metadata,
   signed/authenticated view và preview Web/Mobile.
2. Upload lần lượt PDF, DOC, DOCX ở vùng Tệp; xác nhận cả ba nằm trên R2, DB ghi
   `r2`, tải được qua presigned URL rồi URL hết hạn/refresh đúng.
3. Thử file giả extension, ZIP giả DOCX, MIME spoof, quá size, thiếu R2 config và
   caller không phải owner/admin; tất cả phải fail closed đúng error code.
4. Tạo draft, reload không mất attachment, submit, admin reject, sửa/resubmit,
   approve và kiểm tra quyền xem ở từng trạng thái.
5. Expert approved trả lời Community; moderation approve hai lần/retry và chứng
   minh count/point/notification không bị cộng trùng.
6. Cleanup toàn bộ object test sau khi ghi bằng chứng nếu môi trường cho phép.

## 9. Kiểm chứng tối thiểu

```powershell
cd D:\Do_aN\05_Development\CareBridgeAPI
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd "-Dtest=*File*,*Contribution*,*CommunityAnswer*,*Expert*" test

cd D:\Do_aN\05_Development\CareBridgeWebApp
npm run build
npm test

cd D:\Do_aN\05_Development\CareBridgeMobileApp
flutter analyze
flutter test test/features/expert
flutter test test/features/community
```

Trước khi ghi PASS, xác nhận pattern/path thực sự discover test. Nếu suite không
tồn tại, báo thiếu coverage và tạo test; không biến “0 tests” thành PASS.

## 10. Git và bảo toàn working tree

- Đọc `AGENTS.md` và toàn bộ dual-remote Git rules trước thao tác Git.
- Working tree đang có thay đổi của người dùng và file generated. Không
  reset/clean/revert/stash/checkout tùy tiện.
- Không đọc/in secret `.env`; chỉ xác minh tên biến tồn tại.
- Không commit, merge hoặc push nếu người dùng chưa yêu cầu rõ trong phiên mới.
- Khi được yêu cầu commit: chia commit theo backend storage/domain, Web, Mobile
  và tests; không gom tài liệu/generated/unrelated vào commit source.

## 11. Báo cáo bàn giao bắt buộc

Sau khi hoàn thành, tạo file:

```text
EXPERT_COMMUNITY_MEDICAL_CONTRIBUTION_COMPLETION_REPORT.md
```

Báo cáo phải ghi bằng chứng, không chỉ đánh dấu checklist:

- commit/branch và danh sách file source thay đổi;
- storage matrix thực tế và sample metadata đã redacted;
- commands, số test discovered/pass/fail;
- runtime object proof cho Cloudinary và R2;
- API/UI flow đã chạy trên Backend/Web/Mobile;
- lỗi/rủi ro còn lại, đặc biệt test bị treo hoặc môi trường ngoài chưa kiểm tra;
- xác nhận không lộ secret và không dùng PII thật.

Chỉ ghi `COMPLETE` khi toàn bộ acceptance ở trên có bằng chứng runtime. Nếu mới
compile/unit test hoặc UI vẫn placeholder, ghi `PARTIAL` và liệt kê chính xác
phần còn thiếu để Codex 5.6-Sol kiểm tra tiếp.
