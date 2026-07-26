# Lệnh làm việc cho Claude Code 4.8 — Hoàn thiện luồng Chuyên gia CareBridge

> **Cập nhật sau báo cáo 2026-07-19:** Đọc và thực thi tiếp
> `CLAUDE_CODE_4_8_EXPERT_FLOW_CONTINUATION.md`. File continuation là nguồn
> đúng hơn khi có mâu thuẫn về trạng thái hoàn thành hoặc cách định tuyến
> storage. Quy tắc chốt: **IMAGE → Cloudinary; PDF/DOC/DOCX → Cloudflare R2**.

Bạn đang làm việc trực tiếp trong repository `D:\Do_aN` trên nhánh cá nhân `LamVH1`.

Mục tiêu của bạn là **tiếp tục từ working tree hiện tại**, hoàn thiện end-to-end luồng Chuyên gia trên Backend, Web và Mobile. Không làm lại từ đầu, không xóa hoặc hoàn nguyên các thay đổi đang có. Không commit, merge hoặc push nếu chưa có yêu cầu rõ ràng mới từ người dùng, kể cả khi toàn bộ kiểm chứng đã pass.

## 1. Kết quả cuối cùng bắt buộc

Hoàn thiện một luồng liền mạch:

1. Người dùng đăng ký tài khoản với role `EXPERT`.
2. Người dùng tạo hồ sơ chuyên gia bằng danh mục chuẩn: chuyên khoa và cơ sở y tế dùng ID từ master data, không nhập chuỗi tùy ý.
3. Người dùng chụp/tải đủ selfie, CCCD mặt trước và CCCD mặt sau.
4. Backend gọi CompreFace đang chạy bằng Docker để so khớp selfie với CCCD mặt trước.
5. CompreFace chỉ cung cấp tín hiệu hỗ trợ; không được tự động phê duyệt chuyên gia.
6. Admin xem được đủ ba ảnh bằng URL có thời hạn, xem trạng thái/điểm CompreFace, duyệt hoặc từ chối định danh.
7. Chuyên gia tải chứng chỉ chuyên môn, admin duyệt chứng chỉ, sau đó mới có thể phê duyệt chuyên gia cuối cùng.
8. Sau khi được duyệt, chuyên gia có thể trả lời câu hỏi Community và gửi tài liệu y khoa.
9. Màn gửi tài liệu phải có hai vùng riêng:
   - `Hình ảnh`: xem preview, xóa/thay ảnh, upload lên Cloudinary.
   - `Tệp tài liệu`: PDF/DOC/DOCX, hiển thị tên/dung lượng/trạng thái, upload lên Cloudflare R2.
10. Logic upload phải nằm trong module file dùng chung để sau này Member/Mother/Family có thể tái sử dụng; không viết upload Cloudinary riêng trong controller hoặc từng màn hình.

## 2. Quy tắc an toàn bắt buộc

- Đọc đầy đủ `AGENTS.md`, `CLAUDE.md` và các file sau trước khi thao tác Git:
  - `.claude/rules/git-dual-remote.md`
  - `.claude/skills/git-dual-remote-handler.md`
  - `.claude/workflows/start-day.md`
  - `.claude/workflows/end-day.md`
- Working tree hiện đang bẩn và chứa code đang làm. **Không checkout/reset/stash/clean/revert** nếu chưa xác định chính xác chủ sở hữu thay đổi.
- Đây là phiên tiếp tục một phiên đang làm dở: không chạy start-day sync, không checkout/pull/merge/stash. Chỉ sync Git sau khi người dùng xác nhận cách bảo toàn toàn bộ dirty tree.
- Giữ nhánh làm việc là `LamVH1`. Không tạo feature commit trực tiếp trên `dev`.
- Không dùng `git push origin`; chỉ dùng remote `github` hoặc `gitlab` theo workflow dự án.
- Không đọc hoặc in giá trị secret trong `.env`. Chỉ kiểm tra sự tồn tại của tên biến.
- Bắt đầu review bằng `git diff --name-only` và `git diff --stat`, sau đó xem diff theo từng file. Không in nội dung `.env`, secret-bearing config hoặc giá trị credential; khi rà secret chỉ báo tên file, tên biến và vị trí đã redacted.
- Không đưa API key R2, Cloudinary hoặc CompreFace xuống Web/Mobile.
- Ảnh CCCD, selfie và ảnh chứng chỉ lưu trên Cloudinary ở chế độ private/authenticated, chỉ xem qua signed URL có thời hạn tối đa 15 phút. Chứng chỉ dạng PDF/DOC/DOCX lưu private trên Cloudflare R2 và chỉ xem qua presigned URL tối đa 15 phút.
- Ảnh/tệp của bài đóng góp y khoa có thể lưu Cloudinary, nhưng phải lưu `provider`, `storageKey/publicId`, MIME, size và owner trong database. Attachment ở `DRAFT`, `SUBMITTED` hoặc `REJECTED` không được public; dùng authenticated delivery/access-controlled URL và chỉ phát hành public delivery sau `APPROVED` nếu business rule cho phép.
- Không lưu URL presigned hết hạn vào database.
- Không sửa migration đã được áp dụng. Với các migration untracked hiện tại, kiểm tra kỹ trước khi giữ hoặc thay thế.
- Không thêm microservice hoặc dependency mới nếu kiến trúc hiện tại đã đáp ứng được.

## 3. Context phải đọc trước

- `_bmad-output/implementation-artifacts/spec-expert-registration-identity-verification.md`
- `_bmad-output/implementation-artifacts/investigations/member4-expert-flow-investigation.md`
- `MEMORY.md`
- `MEMORY_expert-registration-flow.md`
- Backend:
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expert/**`
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/**`
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/**`
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/masterdata/**`
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/community/**`
  - `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/content/**`
- Web:
  - `05_Development/CareBridgeWebApp/src/features/expert/**`
  - `05_Development/CareBridgeWebApp/src/features/community/**`
- Mobile:
  - `05_Development/CareBridgeMobileApp/lib/features/expert/**`
  - `05_Development/CareBridgeMobileApp/lib/features/community/**`

## 4. Trạng thái hiện tại — phải bảo toàn

Các thay đổi sau đã được triển khai một phần trong working tree:

- Web onboarding đã sửa lỗi build, dùng dropdown master data và hiển thị ba ảnh cho admin.
- Contract hồ sơ đang chuyển sang `specialtyId` và `hospitalId`, đồng thời giữ tên hiển thị `specialty` và `workplace` để tương thích.
- Mobile profile setup đang chuyển từ input text sang dropdown chuyên khoa/cơ sở y tế.
- `UploadedFile` đang được bổ sung `storageProvider` để mỗi file tự xác định nơi lưu.
- `IFileService` đã có bước tách ban đầu `uploadPublicFile(...)` và
  `uploadPrivateFile(...)`, nhưng abstraction này chưa đúng yêu cầu cuối cùng.
  Phải route theo kind/purpose: mọi IMAGE sang Cloudinary (private/authenticated
  nếu nhạy cảm hoặc chưa duyệt), PDF/DOC/DOCX sang R2.
- Selfie/CCCD hiện đang gọi `uploadPrivateFile(...)` và đi R2; đây là gap cần
  chuyển sang Cloudinary private/authenticated mà không làm lộ PII.
- Cấu hình R2 đã hỗ trợ cả `R2_BUCKET` và tên biến đang có `R2_BUCKET_NAME`.
- Backend đã bỏ đường gọi CompreFace trùng, controller preview đang dùng `FaceVerificationAdapter` chung.
- Review identity/credential đang được thêm pessimistic lock và audit/idempotency.
- Đã thêm migration:
  - `V20260718000002__track_uploaded_file_storage_provider.sql`
  - `V20260718000003__link_expert_profile_master_data.sql`

Không giả định các thay đổi trên đã hoàn hảo. Hãy review diff trước, sửa tiếp trên chính các file hiện tại.

## 5. Vấn đề đã biết cần xử lý trước

### 5.1 Migration master data nguy hiểm

`V20260718000001__full_vietnam_master_data.sql` hiện có thao tác `DELETE` và bộ mã tỉnh không nhất quán/trùng tên. Không được chạy migration này ở trạng thái hiện tại.

Yêu cầu:

- Loại bỏ mọi `DELETE` dữ liệu master production.
- Dùng `INSERT ... ON CONFLICT ... DO UPDATE` với mã ổn định.
- Không tạo hai tỉnh/thành trùng tên chuẩn hóa.
- Bảo đảm hospital/district đang được tham chiếu không bị mất.
- Thêm test hoặc script kiểm tra uniqueness và FK.
- Chỉ kiểm thử migration trên database disposable/local tạo từ fixture hoặc bản sao đã ẩn danh; tuyệt đối không migrate, truncate hoặc sửa production/shared dev DB.

### 5.2 Storage routing chưa được nghiệm thu runtime

- Kiểm tra `FileServiceImpl` sau thay đổi constructor/provider routing.
- Bảo đảm record cũ có `storageProvider = null` vẫn đọc được an toàn hoặc được migration backfill.
- Bảo đảm Cloudinary delete dùng đúng `resource_type` và đúng `public_id`, không dựa vào URL một cách mơ hồ.
- Nếu object upload thành công nhưng DB/audit/presign thất bại, phải compensation delete hoặc đưa vào durable cleanup/outbox.
- Không để R2 thiếu cấu hình rồi âm thầm fallback tài liệu PDF/DOC/DOCX sang Cloudinary.

### 5.3 Mobile tests cần cập nhật

`ExpertOnboardingService.createProfile` đã đổi sang `specialtyId`/`hospitalId`. Cập nhật fake tests và widget tests tương ứng. Lần chạy gần nhất của:

```powershell
flutter test test/features/expert/expert_onboarding_service_test.dart
```

bị treo trên Windows và đã dừng, chưa có kết luận pass/fail.

### 5.4 Community chưa hoàn tất lifecycle

Kiểm tra và sửa tối thiểu:

- EXPERT chưa được xác minh không được đăng câu trả lời dưới danh nghĩa chuyên gia.
- `hasExpertAnswer` chỉ tính answer `APPROVED`, không tính `PENDING/HIDDEN/DELETED`.
- `answerCount` chỉ tăng khi moderator chuyển answer sang `APPROVED`, không tăng ngay lúc submit.
- Contribution point và notification phải được phát đúng thời điểm được duyệt, idempotent, không cộng hai lần khi hai moderator xử lý đồng thời.
- Web và Mobile phải tải đủ nội dung câu hỏi trước khi chuyên gia trả lời và phản ánh đúng trạng thái `PENDING`.
- Nếu triển khai “câu trả lời được chấp nhận/best answer”, phân biệt rõ với moderator approval và thêm migration/API/UI/test đầy đủ; không giả vờ hai trạng thái là một.

### 5.5 Gửi tài liệu y khoa chưa có end-to-end

Tạo workflow contribution có trạng thái tối thiểu `DRAFT`, `SUBMITTED`, `APPROVED`, `REJECTED`:

- Chỉ expert `APPROVED` và trust status hợp lệ được tạo/gửi.
- Metadata bài: title, summary/body, topic/category, clinical disclaimer, createdBy, timestamps.
- Attachment dùng module file chung, có `kind = IMAGE | DOCUMENT`, thứ tự hiển thị và provider; IMAGE → Cloudinary, DOCUMENT → R2.
- IMAGE chấp nhận JPEG/PNG/WebP theo policy dự án.
- DOCUMENT chấp nhận PDF/DOC/DOCX; kiểm tra magic bytes/MIME/size ở Backend, không tin extension từ client.
- Hai vùng chọn file riêng trên Web và Mobile.
- Có progress, retry, remove trước submit, lỗi từng file và không làm mất draft.
- Admin/content-admin có queue review và lý do từ chối.
- Tệp chỉ liên kết với contribution sau khi upload thành công; cleanup object mồ côi khi transaction thất bại.
- Thêm Backend tests, Web tests và Flutter fake-driven tests.

## 6. CompreFace và lỗi “không thấy ảnh”

Không kết luận CompreFace là nguyên nhân ảnh không hiển thị. Tách hai concern:

- CompreFace nhận bytes và trả `status`, `similarity`, `threshold`.
- Admin lấy ảnh qua endpoint authorized URL của file service.

Kiểm tra theo thứ tự:

1. Submission response có đủ ba `fileId`.
2. Database `uploaded_files` có đúng `storage_provider = cloudinary`, private public ID/object key và không lưu URL tạm.
3. Endpoint `/api/v1/expert/identity/files/{fileId}/url` trả signed URL hợp lệ cho owner hoặc `SYSTEM_ADMIN`.
4. Cloudinary authenticated delivery/CORS cho phép browser GET từ Web origin nhưng không làm ảnh thành public.
5. Web unwrap đúng `ApiResponse.data.presignedUrl`.
6. `<img>` hiển thị loading/error riêng cho từng ảnh; presigned URL hết hạn phải có nút tải lại.
7. Admin luôn thấy ba ô riêng: selfie, CCCD trước, CCCD sau.

Khi `COMPREFACE_ENABLED=false` hoặc provider timeout, submission phải đi `MANUAL_REVIEW_REQUIRED`; UI không được khóa nút gửi. Khi `MATCHED`, UI hiển thị tích xanh nhưng vẫn chờ admin duyệt.

## 7. Biến môi trường

Chỉ kiểm tra tên biến, không in giá trị:

```text
CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET
R2_ENDPOINT
R2_ACCESS_KEY_ID
R2_SECRET_ACCESS_KEY
R2_BUCKET_NAME hoặc R2_BUCKET
R2_REGION
COMPREFACE_ENABLED
COMPREFACE_BASE_URL
COMPREFACE_API_KEY
COMPREFACE_SIMILARITY_THRESHOLD
```

Thiết kế kỳ vọng:

- Document provider là R2; image provider là Cloudinary. Access mode private/public
  là concern riêng, không được dùng provider để suy ra quyền truy cập.
- Contribution image gọi luồng Cloudinary với access mode theo lifecycle; contribution document gọi rõ luồng R2, không phụ thuộc provider mặc định mơ hồ.
- Secrets chỉ tồn tại ở Backend/Deployment.

## 8. Thứ tự thực hiện

1. Chạy `git status --short`, đọc diff hiện tại và phân loại file source so với generated/unrelated.
2. Sửa compilation/test failures do contract mới và `IFileService` mới.
3. Làm an toàn migrations master data/storage provider.
4. Chứng minh Cloudinary private upload → DB public ID/key → signed URL → Web admin hiển thị ba ảnh. Đồng thời chứng minh PDF/DOC/DOCX upload R2 → DB key → presigned URL. Runtime smoke test chỉ dùng PII giả, tài khoản test và folder/bucket test có prefix riêng; ghi object ID và cleanup object test sau khi xác minh nếu được phép.
5. Hoàn thiện profile dropdown Web/Mobile và server-side validation master IDs.
6. Hoàn thiện credential submit/review/final expert approval/audit/idempotency.
7. Hoàn thiện Community answer lifecycle.
8. Xây workflow contribution + hai vùng Cloudinary IMAGE và R2 DOCUMENT trên Backend/Web/Mobile.
9. Bổ sung tests và chạy toàn bộ verification hẹp trước, build rộng sau.
10. Chạy adversarial review diff, sửa lỗi Critical/High, rà secret và migration lần cuối.
11. Chỉ commit/push khi người dùng yêu cầu; tuân thủ dual-remote workflow.

## 9. Lệnh kiểm chứng tối thiểu

Backend:

```powershell
cd D:\Do_aN\05_Development\CareBridgeAPI
.\mvnw.cmd clean compile
.\mvnw.cmd "-Dtest=*ExpertIdentity*,*ExpertCredential*,*File*,*CommunityAnswer*,*Contribution*" test
```

Web:

```powershell
cd D:\Do_aN\05_Development\CareBridgeWebApp
npm run build
npm test
```

Mobile:

```powershell
cd D:\Do_aN\05_Development\CareBridgeMobileApp
flutter analyze
flutter test test/features/expert
# Chỉ chạy dòng dưới sau khi suite/path đã tồn tại:
flutter test test/features/community
```

Kiểm tra repository:

```powershell
cd D:\Do_aN
git status --short
rg -n "<<<<<<<|=======|>>>>>>>" 05_Development
```

Không coi `compile` là đủ. Phải chạy tests liên quan và ghi rõ test nào bị treo hoặc không thể chạy.
Trước mỗi suite, kiểm tra path/pattern test có tồn tại và ghi số test được discover. Maven wildcard không match test nào hoặc Flutter test path chưa tồn tại phải được báo là thiếu coverage, không được ghi `PASS`.

## 10. Bằng chứng đã có

Snapshot này được ghi tại HEAD `f5e68c84` trên `LamVH1` với dirty tree. Hai remote thực tế đang dùng HTTPS tới đúng GitHub/GitLab repository; không tự đổi URL hoặc cấu hình xác thực. Các kết quả PASS dưới đây chỉ là lịch sử tham khảo và bắt buộc chạy lại sau khi sửa:

- `CareBridgeAPI`: `.\mvnw.cmd clean compile` — **PASS**, compile 1.275 source files.
- `CareBridgeWebApp`: `npm run build` — **PASS** sau khi sửa contract/import.
- `CareBridgeMobileApp`: `flutter analyze` không thấy error trong module expert; còn lint info cũ ở module khác.
- `flutter test test/features/expert/expert_onboarding_service_test.dart` — **INCONCLUSIVE**, runner treo và bị dừng.
- Live Cloudinary private → browser image, live R2 document và live CompreFace Docker chưa được chứng minh trong phiên này.

## 11. Tiêu chí hoàn thành

Chỉ báo “đã hoàn thành” khi có đủ bằng chứng:

- Backend/Web/Mobile build được.
- Test trọng tâm pass.
- Migration không destructive và chạy được trên DB disposable sạch lẫn fixture có dữ liệu tham chiếu.
- EXPERT mới luôn đi qua onboarding, không vào dashboard trước khi hoàn tất.
- Admin xem được đủ ba ảnh và URL hết hạn được refresh.
- Final approval bị chặn nếu thiếu identity hoặc credential đã duyệt/còn hạn.
- Expert chưa verified không có quyền hành động dành riêng cho verified expert.
- Community count/badge/points/notification đúng sau moderation.
- Contribution có hai vùng ảnh/tệp riêng, file thật sự xuất hiện trên Cloudinary và metadata DB nhất quán.
- Không có secret trong diff/log.
- Báo cáo cuối liệt kê file đổi, commands đã chạy, kết quả, và rủi ro còn lại.

Hãy làm việc chủ động đến khi đạt các tiêu chí trên. Nếu gặp ambiguity ảnh hưởng kiến trúc hoặc dữ liệu production, dừng đúng điểm đó và hỏi người dùng; không tự đoán rồi phá schema hoặc dữ liệu.


- sau khi lam xong phai taoj file nmd bao cao .md cho toi, de codex 5.6 sol kieể tra
