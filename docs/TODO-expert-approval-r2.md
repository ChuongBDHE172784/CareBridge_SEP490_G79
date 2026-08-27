# Công việc tiếp theo — Xét duyệt chuyên gia và tài liệu Cloudflare R2

Ngày cập nhật: 2026-07-28  
Trạng thái nền: luồng xét duyệt tập trung đã được triển khai tại commit `59a19745`.

## Mục tiêu

Hoàn thiện trung tâm xét duyệt chuyên gia để:

- quản trị viên xử lý định danh, chứng chỉ, quyết định cuối và trust tại một màn hình;
- đọc an toàn tài liệu PDF, DOC và DOCX lưu riêng tư trên Cloudflare R2;
- hoạt động ổn định khi số lượng chuyên gia và tài liệu tăng;
- không tạo bảng mới; mọi thay đổi schema khác, nếu cần, phải được phê duyệt riêng.

## Nguyên tắc bắt buộc

- [ ] Giữ quyền truy cập `SYSTEM_ADMIN` cho toàn bộ API quản trị.
- [ ] Không trả bucket/object key trong API danh sách, detail, audit hoặc log. Presigned URL trực tiếp chỉ được lộ trong thời gian ngắn cho `SYSTEM_ADMIN`; nếu object key tuyệt đối không được lộ thì phải dùng backend proxy.
- [ ] Chỉ tạo presigned URL khi quản trị viên thực sự mở tài liệu.
- [ ] Không gửi tài liệu riêng tư sang Google Docs, Office Online hoặc viewer bên thứ ba.
- [ ] Không tự động duyệt chuyên gia từ kết quả nhận diện khuôn mặt.
- [ ] Không tạo bảng mới; ưu tiên DTO, query projection và các bảng hiện có. Migration không tạo bảng phải được review riêng.
- [ ] Từ chối định danh, chứng chỉ hoặc hồ sơ phải có lý do.

## P0 — Nghiệm thu staging với Cloudflare R2 thật

- [ ] Chuẩn bị một tài khoản `SYSTEM_ADMIN` và ít nhất hai hồ sơ chuyên gia thử nghiệm.
- [ ] Upload lên R2 một file hợp lệ cho từng loại:
  - [ ] PDF;
  - [ ] DOC;
  - [ ] DOCX;
  - [ ] ảnh JPG hoặc PNG.
- [ ] Dùng `mimeType` và `originalName` làm hint; xác minh loại thực bằng magic bytes/content detection của Apache Tika, từ chối khi loại khai báo không khớp nội dung.
- [ ] Xác nhận PDF/ảnh hiển thị được và DOC/DOCX có nội dung text preview.
- [ ] Xác nhận nút tải bản gốc tạo URL ký mới, TTL tối đa 15 phút và không dùng được sau khi hết hạn (sai số cho phép ±60 giây).
- [ ] Kiểm tra file hỏng, MIME giả, file trống và file trên 10 MiB trả lỗi an toàn trước khi nạp toàn bộ object.
- [ ] Kiểm tra đổi nhanh giữa hai hồ sơ không hiển thị ảnh định danh của hồ sơ trước.
- [ ] Kiểm tra một attachment lỗi không làm hỏng toàn bộ danh sách xét duyệt.

### Tiêu chí hoàn thành P0

- Có bằng chứng test cho đủ PDF, DOC, DOCX và ảnh.
- API queue, preview và tạo URL ký trả 401 khi thiếu token, 403 khi không có role `SYSTEM_ADMIN`; object không truy cập được bằng URL không ký và URL ký không dùng được sau khi hết hạn.
- Không có URL riêng tư nào được lưu lâu dài ở frontend.

## P0 — Kiểm thử toàn bộ quyết định xét duyệt

- [ ] Duyệt và từ chối định danh; xác nhận UI cập nhật tại chỗ.
- [ ] Duyệt và từ chối từng chứng chỉ; xác nhận lý do đúng với từng loại quyết định.
- [ ] Reject identity, credential hoặc profile với lý do trống/chỉ có whitespace phải trả 400 từ backend.
- [ ] Chốt giới hạn độ dài lý do từ chối, sau đó enforce giống nhau ở backend và frontend.
- [ ] Xác nhận không thể duyệt cuối khi thiếu một trong các gate:
  - [ ] định danh chưa được duyệt;
  - [ ] chưa có chứng chỉ chuyên môn hợp lệ;
  - [ ] chứng chỉ đã hết hạn;
  - [ ] cơ sở y tế được chọn chưa ở trạng thái `VERIFIED`.
- [ ] Duyệt cuối và xác nhận hồ sơ chuyển `APPROVED` mà không đổi route.
- [ ] Kiểm tra `ACTIVE`, `SUSPENDED`, `REVOKED` và hộp xác nhận khi hạn chế trust.
- [ ] Định nghĩa và test ma trận transition trust; transition không hợp lệ phải bị từ chối.
- [ ] Kiểm tra route cũ tự chuyển về `/admin/expert-verification-queue`.
- [ ] Hai `SYSTEM_ADMIN` xử lý cùng hồ sơ: thao tác stale trả 409, không ghi audit thành công và UI reload snapshot mới.
- [ ] Retry cùng quyết định không tạo trạng thái hoặc audit trùng ngoài chủ đích.

### Tiêu chí hoàn thành quyết định

- Trạng thái trên màn hình luôn khớp trạng thái backend sau mỗi thao tác.
- Conflict hoặc dữ liệu stale hiển thị thông báo rõ, không báo duyệt thành công giả.
- Audit ghi đúng quản trị viên, đối tượng, quyết định và lý do.
- Identity, credential, final approval và trust đều thao tác tại `/admin/expert-verification-queue`; sidebar chỉ có một entry và mọi route quản trị cũ redirect về route này.

## P1 — Tối ưu API danh sách xét duyệt

Hiện tại aggregate queue còn dùng `findAll()` và nhiều truy vấn theo từng hồ sơ.

- [ ] Thiết kế phân trang cho endpoint review-case:
  - [ ] `page`;
  - [ ] `size` có giới hạn tối đa;
  - [ ] bộ lọc trạng thái;
  - [ ] tìm kiếm tên/chuyên môn;
  - [ ] sắp xếp theo thời điểm chờ duyệt.
- [ ] Thay truy vấn N+1 bằng query projection hoặc batch query trên các bảng hiện có.
- [ ] Tính `identityStatus`, `credentialStatus` và `readyForFinalApproval` từ cùng một snapshot dữ liệu.
- [ ] Không tạo presigned URL trong API danh sách.
- [ ] Cô lập lỗi theo attachment/hồ sơ: review-case vẫn trả 200, attachment lỗi có `previewStatus`/`errorCode` đã sanitize, các hồ sơ và attachment khác vẫn đầy đủ.
- [ ] Bổ sung test số lượng query hoặc integration test với nhiều hồ sơ.

### Tiêu chí hoàn thành P1

- Endpoint không tải toàn bộ chuyên gia vào bộ nhớ.
- Số truy vấn không tăng tuyến tính theo số hồ sơ trong một trang.
- Với 10.000 hồ sơ seed trên môi trường staging, trang 50 hồ sơ đạt p95 ≤ 2 giây và tổng query ≤ 10.

## P1 — Cô lập Apache Tika

Backend hiện đã giới hạn byte đọc từ R2 và số ký tự đầu ra. Cần thêm lớp bảo vệ cho tài liệu Office được tạo có chủ đích xấu.

- [ ] Chạy parser trong worker/process có giới hạn tài nguyên.
- [ ] Đặt hard timeout 30 giây cho mỗi lần parse.
- [ ] Giới hạn worker ở 512 MiB, expanded content 100 MiB và tối đa 1.000 embedded entries.
- [ ] Hủy parser khi client ngắt kết nối hoặc request hết hạn.
- [ ] Không ghi nội dung tài liệu riêng tư vào application log.
- [ ] Ghi metric theo loại kết quả: thành công, MIME sai, quá lớn, timeout, parser lỗi.
- [ ] Viết test cho ZIP/DOCX có tỷ lệ nén bất thường và parser timeout.

### Tiêu chí hoàn thành parser

- Tài liệu xấu không thể giữ worker vô thời hạn hoặc làm tăng heap không kiểm soát.
- Lỗi parser không làm ảnh hưởng các request xét duyệt khác.

## P1 — Bổ sung automated tests chạy lặp lại trong CI

### Backend

- [ ] Test preview PDF thật.
- [ ] Test preview DOC thật.
- [ ] Test DOCX thật.
- [ ] Test MIME không hỗ trợ và MIME không khớp nội dung.
- [ ] Test file lớn bị chặn trước khi tải toàn bộ object.
- [ ] Test file R2 không tồn tại hoặc đã bị xóa.
- [ ] Test RBAC: `SYSTEM_ADMIN` được phép, role khác bị từ chối.
- [ ] Test ma trận RBAC cho queue/detail, preview, tạo URL tải, duyệt/từ chối identity, credential, profile và đổi trust: `SYSTEM_ADMIN` thành công; thiếu token trả 401; role khác trả 403; response lỗi không chứa URL ký.
- [ ] Test attachment thuộc hồ sơ/chứng chỉ khác trả 403/404 và không tạo presigned URL.
- [ ] Test response preview/download có `Cache-Control` phù hợp; log không chứa query string của presigned URL, object key hoặc nội dung tài liệu.
- [ ] Test aggregate nhiều chứng chỉ thuộc cùng một `expertProfileId`.
- [ ] Test gate cơ sở y tế `UNVERIFIED`.
- [ ] Test attachment lỗi vẫn trả review-case 200 với lỗi đã sanitize, không làm mất hồ sơ khác.

### Frontend

- [ ] Test ảnh định danh không bị stale khi đổi hồ sơ nhanh.
- [ ] Test một ảnh lỗi nhưng các ảnh còn lại vẫn hiển thị.
- [ ] Test lý do từ chối riêng cho identity, credential và profile.
- [ ] Test approve/reject refresh trạng thái tại chỗ.
- [ ] Test trust confirmation.
- [ ] Test filter làm thay đổi hồ sơ đang chọn.
- [ ] Test PDF, DOC, DOCX, ảnh và lỗi preview.
- [ ] Test redirect hai route quản trị cũ.
- [ ] Test sidebar chỉ có một mục “Xét duyệt chuyên gia”.

## P2 — Dữ liệu và vận hành R2

- [ ] Kiểm kê attachment tài liệu cũ để xác nhận object thực sự tồn tại trên R2.
- [ ] Xác định chiến lược cho tài liệu legacy lưu ở provider khác mà không tạo bảng mới.
- [ ] Thiết lập lifecycle rule cho object bị xóa mềm hoặc upload thất bại.
- [ ] Xác minh CORS chỉ cho phép các origin cần thiết.
- [ ] Xác minh bucket không public và access key chỉ có quyền tối thiểu.
- [ ] Thiết lập cảnh báo cho lỗi R2, latency cao và tỷ lệ preview thất bại.
- [ ] Viết runbook xử lý object thiếu, URL ký lỗi và credential trỏ tới attachment không tồn tại.

## Lệnh kiểm tra

### Backend

```powershell
cd 05_Development/CareBridgeAPI
.\mvnw.cmd -DskipTests compile
.\mvnw.cmd '-Dtest=ExpertCredentialPreviewServiceTest,ExpertIdentityVerificationServiceTest' test
```

### Frontend

```powershell
cd 05_Development/CareBridgeWebApp
npm test -- --run src/features/expert/pages/ExpertVerificationQueuePage.test.tsx src/features/expert/services/expertApi.test.ts
npm run build
```

## Definition of Done

- [ ] Tất cả checklist P0 hoàn thành.
- [ ] Phân trang/N+1 và parser isolation đã được triển khai, có automated test và đạt tiêu chí P1.
- [ ] Test tự động bao phủ các nhánh lỗi quan trọng.
- [ ] Toàn bộ test backend/frontend chạy ổn định trong CI với fixture PDF/DOC/DOCX thật, không phụ thuộc object R2 dùng chung.
- [ ] Không tạo bảng mới; mọi migration khác đã được review và phê duyệt riêng.
- [ ] Không có secret, file graph, `.cursor` hoặc script thử nghiệm trong commit.
- [ ] Code review xác nhận tài liệu vẫn private và mọi thao tác quản trị giữ `SYSTEM_ADMIN`.
- [ ] Có commit riêng, semantic và không trộn thay đổi ngoài phạm vi.
