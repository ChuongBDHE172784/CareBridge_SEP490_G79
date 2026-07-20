# LỆNH TIẾP TỤC CHO CLAUDE CODE — LUỒNG ĐĂNG KÝ VÀ XÁC MINH CHUYÊN GIA BẰNG COMPREFACE

**Ngày audit:** 21/07/2026  
**Repository:** `D:\Do_aN`  
**Nhánh làm việc chuẩn:** `LamVH1`  
**Báo cáo đầu vào:** `IMPLEMENTATION_REPORT_CONTRIBUTION_FEATURE_FINAL.md`  
**Mục tiêu lần này:** hoàn thiện journey đăng ký thành chuyên gia trên Backend, Web và Mobile; dùng Docker CompreFace để phát hiện/cắt khuôn mặt trên CCCD, so sánh với selfie và đưa bằng chứng có dấu xác thực lên màn Admin.

> Không được tuyên bố Complete chỉ vì compile/test unit qua. Phải chạy Docker CompreFace, Backend, Web và Mobile; chụp/gửi ảnh thật không chứa PII thật; xem kết quả trên màn Admin. Không in, chụp màn hình, log hoặc commit mật khẩu/API key trong `.env`.

## 1. Kết luận audit code hiện tại

Báo cáo FINAL mới xử lý một phần contribution/file/community. Luồng xác minh chuyên gia đã có bộ khung nhưng **chưa đáp ứng pipeline người dùng yêu cầu**.

### Phần đã có thể tái sử dụng

- Web có `ExpertOnboardingPage.tsx`, ba ô selfie/CCCD trước/CCCD sau và camera dialog.
- Mobile có `expert_identity_capture_screen.dart`, camera/gallery cho ba ảnh.
- Backend có `POST /api/v1/expert/identity`, lưu ba file riêng tư và entity `ExpertIdentityVerification`.
- Có adapter `CompreFaceVerificationAdapter` và cấu hình Docker CompreFace 1.2.0.
- Admin có `AdminExpertIdentityReviewPage.tsx`, tải được ba ảnh và hiển thị score/status.
- Final expert approval đã yêu cầu identity và credential được Admin phê duyệt.

### Các khoảng trống P0/P1 phải sửa

| ID | Khoảng trống đã xác minh | Yêu cầu sửa |
|---|---|---|
| FACE-P0-01 | Adapter đang gửi **nguyên ảnh CCCD mặt trước** thẳng vào `/verification/verify` | Thêm bước Detection → xác định đúng một mặt → crop có padding → normalize → mới Verification |
| FACE-P0-02 | `findHighestSimilarity()` duyệt đệ quy và lấy số similarity lớn nhất ở bất kỳ vị trí nào | Dùng DTO typed theo response chính thức, kiểm tra cardinality và lấy đúng `result/source_image_face/face_matches` |
| FACE-P0-03 | Không lưu ảnh khuôn mặt đã tách từ CCCD | Lưu derived crop private trên Cloudinary với purpose riêng và FK trong attempt để Admin xem |
| FACE-P0-04 | Web tự gọi `/verify-face` khi đổi ảnh rồi submit lại gọi CompreFace lần nữa | Chỉ có một authoritative verification pipeline; bỏ auto-call hoặc biến thành explicit preflight không quyết định/lưu kết quả |
| FACE-P0-05 | `NOT_MATCHED` hiện tự chuyển identity sang `REJECTED` trước quyết định Admin | AI chỉ là bằng chứng. Đưa vào hàng đợi `PENDING_REVIEW`/`MANUAL_REVIEW_REQUIRED`; Admin quyết định cuối |
| FACE-P0-06 | CompreFace timeout/lỗi đang dựa vào text exception để đoán NO_FACE/MULTIPLE_FACES | Map HTTP/status/body/error code typed; retry có giới hạn; lỗi provider → manual review, không mất hồ sơ |
| FACE-P0-07 | Chưa có API key riêng cho Detection và Verification | Tạo một CompreFace application với hai service và hai backend-only keys |
| FACE-P1-01 | Mobile không hiển thị trạng thái xử lý/matched/manual review sau submit | Thêm polling/state UI và badge đúng nghĩa |
| FACE-P1-02 | Admin chỉ đổi màu MATCHED/khác MATCHED; DISABLED/ERROR bị tô đỏ như mismatch | Thiết kế badge 4 trạng thái và hiển thị selfie + face crop cạnh nhau |
| FACE-P1-03 | Chỉ có ba unit test identity, không test adapter response, crop, concurrency hoặc journey thật | Bổ sung unit/integration/E2E và runtime evidence |
| FACE-P1-04 | Endpoint preview bắt mọi exception rồi trả 500 kèm `e.getMessage()` | Không rò provider/internal details; dùng error contract và status đúng 400/422/503 |

## 2. Journey chuẩn phải hoàn thiện

1. User đăng ký/đăng nhập với role `EXPERT`.
2. Tạo hồ sơ chuyên môn từ master data thật.
3. Ở bước định danh, Web/App yêu cầu:
   - Chụp selfie trực diện bằng camera trước.
   - Chụp CCCD mặt trước bằng camera sau.
   - Chụp CCCD mặt sau bằng camera sau.
4. Client kiểm tra kích thước/định dạng và hiển thị preview, nhưng Backend vẫn validate lại toàn bộ.
5. Backend lưu ba ảnh gốc private, tạo một verification attempt bền vững.
6. Worker/backend gửi selfie và CCCD mặt trước qua pipeline Detection/Crop/Verification.
7. Khuôn mặt đã crop từ CCCD được lưu private để Admin đối chiếu.
8. Backend lưu kết quả CompreFace: status, similarity, threshold, detection probabilities, bounding box, provider version/error và thời điểm xử lý.
9. Admin mở queue và thấy:
   - Selfie lớn.
   - Khuôn mặt crop từ CCCD cạnh selfie.
   - CCCD trước/sau bản gốc bên dưới.
   - Dấu xanh “CompreFace xác nhận trùng khớp” khi đúng điều kiện.
   - Score, threshold, thời gian và trạng thái manual/error rõ ràng.
10. Admin vẫn là người duyệt/từ chối định danh. Sau đó Admin duyệt chứng chỉ và final approval.
11. Chỉ expert final-approved và trust active mới được trả lời cộng đồng/gửi contribution.

## 3. Thiết lập tài khoản và service CompreFace

CompreFace đã chạy Docker nhưng cần cấu hình đúng service. Không tạo tài khoản CompreFace riêng cho từng người dùng CareBridge.

### Thiết lập một lần bởi operator

1. Kiểm tra stack theo `05_Development/Deployment/docker-compose.compreFace.yml`; không xóa volume hiện có.
2. Mở `http://localhost:8000/login`.
3. Nếu chưa có, tạo **một tài khoản quản trị CompreFace nội bộ** bằng credential do người vận hành nhập trực tiếp. Claude không được tự đặt/hardcode mật khẩu trong source hoặc report.
4. Tạo application tên gợi ý `carebridge-expert-identity`.
5. Trong application, tạo hai service:
   - `carebridge-id-face-detection`, type `DETECTION`.
   - `carebridge-id-face-verification`, type `VERIFICATION`.
6. Copy hai API key vào `.env` Backend, không đưa sang Web/Mobile:
   - `COMPREFACE_DETECTION_API_KEY`
   - `COMPREFACE_VERIFICATION_API_KEY`
7. Giữ `COMPREFACE_API_KEY` làm fallback có cảnh báo deprecation trong một phiên nếu cần; sau đó loại bỏ.
8. Thêm các tên biến vào `.env.example` bằng placeholder, không ghi giá trị thật.
9. Cập nhật `README-compreface.md` với health check, cách rotate/revoke key và test curl đã dùng biến môi trường.

Theo tài liệu chính thức, Face Detection và Face Verification là hai loại service; các endpoint dùng header `x-api-key`. Verification nhận multipart `source_image` và `target_image`, còn Detection nhận `file`; mỗi ảnh tối đa 5 MB.

## 4. Kiến trúc Backend bắt buộc

### 4.1 Pipeline Detection → Crop → Verification

Tách adapter thành các boundary rõ:

- `FaceDetectionAdapter.detect(imageBytes, mime)` trả typed result: số mặt, probability, bounding box, landmarks nếu cần.
- `FaceCropService.crop(image, boundingBox, paddingRatio)` xử lý EXIF orientation trước, clamp tọa độ, thêm padding 15–25%, normalize JPEG/PNG và giới hạn dimension.
- `FaceVerificationAdapter.verify(selfieFace, cccdFace)` trả typed match result.
- `CompreFaceDetectionAdapter` gọi `POST /api/v1/detection/detect?limit=2&det_prob_threshold=...` với detection key.
- `CompreFaceVerificationAdapter` gọi `POST /api/v1/verification/verify?limit=1&prediction_count=1&det_prob_threshold=...` với verification key.

Quy tắc:

- Selfie phải phát hiện đúng 1 khuôn mặt.
- CCCD mặt trước phải phát hiện đúng 1 khuôn mặt trong vùng hợp lệ; 0 hoặc >1 → manual/retry theo error code rõ.
- CCCD mặt sau không gửi sang CompreFace.
- Crop cả selfie và ảnh CCCD trước khi verify để tránh nền/biên thẻ; tối thiểu phải crop CCCD như yêu cầu sản phẩm.
- Không lưu face embedding hoặc raw provider response nếu không thật sự cần. Không log ảnh/base64/biometric template.
- Không suy NO_FACE/MULTIPLE_FACES từ chuỗi exception.
- Không lấy similarity bằng recursive search. Parse đúng typed schema và fail closed khi response sai cấu trúc.

Tài liệu REST chính thức: `https://github.com/exadel-inc/CompreFace/blob/master/docs/Rest-API-description.md`.

### 4.2 Xử lý bền vững và đa luồng

Không giữ database transaction trong lúc chờ HTTP CompreFace lâu.

Flow đề xuất:

1. Trong transaction ngắn: validate, upload ba ảnh private, tạo attempt `PROCESSING`, ghi outbox/job.
2. Commit.
3. Worker nhận job theo attempt ID, lock/idempotency, chạy detect/crop/verify.
4. Upload `CCCD_FACE_CROP` private, cập nhật kết quả trong transaction ngắn.
5. Retry timeout/5xx tối đa theo cấu hình với exponential backoff; sau giới hạn → `MANUAL_REVIEW_REQUIRED`.
6. Hai worker không được xử lý/cập nhật cùng attempt; dùng version/lock và idempotency key.
7. Resubmit sau REJECTED tạo attempt mới; giữ lịch sử cũ và không ghi đè audit.

Nếu chưa có hạ tầng worker, có thể dùng Spring async + durable DB job/outbox; không dùng fire-and-forget chỉ nằm trong RAM.

### 4.3 Entity và migration

Thêm migration mới, không sửa migration đã chạy ở môi trường chia sẻ. Bổ sung tối thiểu:

- `identity_face_crop_file_id UUID NULL` FK `uploaded_files`.
- `processing_status` hoặc review status hỗ trợ `PROCESSING`.
- `selfie_detection_probability`, `identity_detection_probability`.
- Bounding box của face CCCD bằng bốn cột hoặc JSONB có schema.
- `face_processed_at`, `provider_request_id` nếu provider có.
- `provider_version/model` nếu lấy được từ response `status=true`.
- `attempt_version`/optimistic lock.

Không lưu API key, raw image bytes, embedding hoặc presigned URL vào DB.

Thêm purpose dùng chung:

- `EXPERT_IDENTITY_CCCD_FACE_CROP` → `IMAGE` → Cloudinary → `PRIVATE`.

Derived crop phải owner/Admin mới xem được và phải được cleanup khi attempt/file bị purge theo retention.

### 4.4 State machine

Phân biệt rõ AI state và Human review state:

- AI: `PROCESSING`, `MATCHED`, `NOT_MATCHED`, `NO_FACE`, `MULTIPLE_FACES`, `PROVIDER_ERROR`, `DISABLED`.
- Human: `PENDING_REVIEW`, `MANUAL_REVIEW_REQUIRED`, `APPROVED`, `REJECTED`.

Mapping:

- AI `MATCHED` → Human `PENDING_REVIEW`.
- AI `NOT_MATCHED`, `NO_FACE`, `MULTIPLE_FACES`, `PROVIDER_ERROR`, `DISABLED` → Human `MANUAL_REVIEW_REQUIRED`.
- **Không tự đặt Human `REJECTED` từ kết quả AI.**
- Admin approve trên trường hợp AI không MATCHED phải nhập lý do override bắt buộc và ghi audit.
- Green verified tick chỉ xuất hiện khi AI `MATCHED`, score `>= threshold`, response hợp lệ và processing hoàn tất; không dùng tick này thay final expert approval.

### 4.5 API contract

Giữ contract nhất quán:

- `POST /api/v1/expert/identity` → `202 Accepted` hoặc response có attempt `PROCESSING`.
- `GET /api/v1/expert/onboarding` → latest attempt + AI/human states.
- Có thể thêm `GET /api/v1/expert/identity/{attemptId}` cho polling, chỉ owner/Admin.
- Admin queue trả metadata, không nhúng permanent URL.
- `GET /api/v1/expert/identity/files/{fileId}/url` vẫn kiểm tra owner/Admin và trả signed URL ngắn hạn.

Error codes ổn định, ví dụ:

- `EXPIDENT-NO-FACE`
- `EXPIDENT-MULTIPLE-FACES`
- `EXPIDENT-LOW-QUALITY`
- `EXPIDENT-PROVIDER-UNAVAILABLE`
- `EXPIDENT-PROVIDER-BAD-RESPONSE`
- `EXPIDENT-ALREADY-PROCESSING`

Không trả `e.getMessage()` của provider cho client.

## 5. Web — chụp ảnh và trạng thái xác minh

### Expert onboarding

- Selfie dùng camera trước; CCCD dùng camera sau khi thiết bị hỗ trợ.
- Selfie ưu tiên camera-only. Nếu cần cho phép upload fallback trên desktop không camera, gắn cờ `captureSource=UPLOAD_FALLBACK` để Admin biết rủi ro.
- Preview selfie có thể mirror cho UX nhưng bytes gửi Backend không được mirror sai orientation.
- Hiển thị khung căn mặt/ánh sáng, yêu cầu chỉ một mặt, không khẩu trang, không dùng ảnh chụp lại từ màn hình.
- CCCD yêu cầu đủ bốn góc, không lóa, rõ ảnh mặt trước.
- Không tự gọi CompreFace mỗi khi người dùng đổi ảnh. Nút chính là `Gửi xác minh`; sau submit hiển thị `Đang xử lý`, poll có backoff và dừng khi unmount.
- Khi MATCHED: hiển thị “Đối chiếu khuôn mặt thành công — đang chờ Admin duyệt”, không nói tài khoản đã được duyệt.
- Khi manual: giải thích Admin sẽ kiểm tra, không lộ lỗi kỹ thuật provider.
- Có retry/resubmit đúng state, không double submit.

Xóa hoặc tái thiết kế `POST /api/v1/expert/verify-face`. Không giữ hai nguồn kết quả khác nhau giữa preview và attempt đã lưu.

## 6. Mobile Flutter — parity với Web

- Camera trước cho selfie, camera sau cho CCCD; xử lý permission denied/permanently denied và deep link Settings.
- Không nạp/chuyển ảnh chất lượng quá lớn; sửa EXIF orientation, resize/compress an toàn trước upload nhưng vẫn đủ chi tiết CCCD.
- Sau submit không chuyển thẳng sang credential như thể định danh đã xong. Hiển thị màn trạng thái `PROCESSING/PENDING_REVIEW/MANUAL_REVIEW_REQUIRED` và cho tiếp tục gửi credential song song nếu nghiệp vụ cho phép.
- Poll attempt với backoff; cancel timer khi dispose; chống cập nhật state sau unmount.
- Hiển thị badge AI giống Web, nhưng không gọi trực tiếp CompreFace và không chứa API key.
- Thêm retry upload từng file, progress và error cụ thể 400/413/422/503.
- Chạy `flutter analyze` cho toàn bộ feature expert và sửa sạch issue trong phạm vi.

## 7. Admin — màn chấp nhận chuyên gia

Thiết kế lại panel bằng chứng:

### Hàng chính so sánh khuôn mặt

- Cột trái: `Selfie người đăng ký`.
- Ở giữa: icon/đường nối và score.
- Cột phải: `Khuôn mặt tách từ CCCD`.
- Badge:
  - Xanh + check: `CompreFace xác nhận trùng khớp`.
  - Đỏ: `Không đạt ngưỡng tương đồng`.
  - Vàng: `Cần kiểm tra thủ công`.
  - Xám: `Đang xử lý/Chưa có kết quả`.

### Bằng chứng bổ sung

- CCCD mặt trước và mặt sau bản gốc, zoom/fullscreen.
- Similarity và threshold hiển thị riêng; dùng `value != null`, không biến score 0 thành “N/A”.
- Detection probability, thời gian xử lý, provider/model version và provider error đã sanitize.
- Ảnh có loading skeleton, `onError`, nút refresh signed URL khi hết hạn.
- Không suy MIME từ phần mở rộng signed URL; dùng metadata file.

### Quyết định Admin

- `Duyệt định danh`: luôn là quyết định human riêng.
- Nếu AI không MATCHED hoặc provider lỗi, approve phải mở modal xác nhận và bắt buộc lý do override.
- `Từ chối`: bắt buộc reason, có gợi ý chuẩn như ảnh mờ/nhiều mặt/không khớp.
- Sau approve identity, không tự final-approve expert nếu credential chưa approved.
- Mọi hành động lưu reviewer/time/previous state/new state/reason/correlation ID.
- Hai Admin review cùng attempt: một thành công, request stale nhận 409 và UI reload.

## 8. Bảo mật và quyền riêng tư

- Selfie, CCCD, face crop là dữ liệu định danh/biometric nhạy cảm; luôn private.
- API keys chỉ ở Backend secret/env; không nằm trong bundle Web/Flutter, response, log hay screenshot.
- Rate-limit submit/retry/poll; giới hạn 5 MB và sniff magic bytes JPEG/PNG.
- Chống decompression bomb, EXIF bất thường, dimension cực lớn và malformed image.
- Xóa EXIF/GPS khỏi derived crop.
- Cấu hình retention/xóa theo policy; xóa cả original/derived đúng audit và không để orphan.
- Không dùng kết quả face match để xác nhận CCCD thật. Admin vẫn kiểm tra tài liệu và chứng chỉ.
- Không gọi đây là liveness detection. CompreFace verification không chứng minh ảnh selfie được chụp trực tiếp tại thời điểm đó.

## 9. Test bắt buộc

### Backend unit/contract

- Detection: 0, 1, 2 mặt; probability dưới ngưỡng; box ngoài biên; response thiếu field.
- Crop: EXIF rotate, box sát mép, padding/clamp, ảnh quá lớn/malformed.
- Verification: MATCHED đúng field, NOT_MATCHED, empty face_matches, multiple matches, timeout, 4xx, 5xx, invalid JSON.
- Không có recursive highest similarity.
- Mapping AI → human state không tự reject.
- Override approve bắt buộc reason.
- Owner/Admin/stranger với bốn file: selfie/front/back/crop.
- Job idempotency, hai worker, hai Admin và resubmit.

### Integration

- WireMock/Testcontainers contract cho Detection và Verification endpoint, header `x-api-key`, multipart field names.
- Migration trên PostgreSQL sạch và upgrade từ schema hiện tại.
- File crop được lưu Cloudinary private và view bằng signed URL đúng quyền.

### Web/Mobile

- Camera permission, capture/retake, thiếu một trong ba ảnh, quá 5 MB.
- Processing → matched/manual state.
- Poll cancel/backoff, expired URL refresh và image error.
- Admin badge/side-by-side evidence/override reason/409 concurrency.

### Runtime thật với Docker

Dùng bộ ảnh test có quyền sử dụng, không dùng CCCD thật trong repo:

1. Cùng người: kỳ vọng score đạt ngưỡng test.
2. Khác người: kỳ vọng score dưới ngưỡng.
3. CCCD/front không có mặt.
4. Ảnh có hai mặt.
5. Ảnh xoay EXIF, mờ hoặc lóa.
6. Tắt CompreFace giữa request → attempt không mất, chuyển manual/retry.

Threshold không được chọn tùy ý từ một mẫu. Giữ cấu hình, lưu threshold theo từng attempt và ghi rõ dữ liệu/evaluation dùng để hiệu chỉnh.

## 10. Trình tự chạy thật bắt buộc

1. Đọc quy tắc Git trong `AGENTS.md` và bốn file dual-remote trước thao tác Git.
2. Giữ/preserve toàn bộ dirty work hiện hữu; không reset hoặc ghi đè contribution work.
3. Khởi động/kiểm tra Docker CompreFace, `docker compose ps`, health và log không có OOM.
4. Operator tạo account/application/two services nếu chưa có; Claude chỉ xác minh tên biến/API key đã cấu hình, không in giá trị.
5. Chạy migration trên DB dev/test an toàn.
6. Start Backend thật bằng `mvnw spring-boot:run`; chờ READY/health.
7. Gọi Detection và Verification qua Backend với ảnh test; không gọi từ client.
8. Start Web `npm run dev`, mở từng màn onboarding/camera/status/Admin queue và kiểm tra Console/Network.
9. Chạy Mobile `flutter analyze`, `flutter test`, `flutter run` trên Android; đi đủ capture/submit/status.
10. Chạy hai Admin/tab song song và retry worker để kiểm tra idempotency/409.
11. Xóa object test và ghi bằng chứng đã che PII/secret.

## 11. Definition of Done

Chỉ ghi **COMPLETE** khi:

- Có account/application CompreFace nội bộ và hai service Detection/Verification hoạt động bằng backend-only keys.
- Backend thực sự detect rồi crop face CCCD trước verification.
- Parser typed, cardinality đúng, không recursive highest similarity.
- Derived CCCD face crop private được lưu và Admin xem được.
- Admin thấy selfie + crop cạnh nhau và dấu xanh chỉ khi MATCHED >= threshold.
- AI không tự duyệt/từ chối human review; Admin quyết định và override được audit.
- Web và Mobile đều capture ba ảnh, submit một authoritative attempt và hiển thị processing/result.
- CompreFace down không làm mất hồ sơ; retry/manual review hoạt động.
- Backend/Web/Mobile chạy thật, không chỉ compile; có runtime evidence cho cùng người/khác người/no face/multiple faces.
- Không lộ secret/PII và không còn TODO/placeholder/mock ở đường chính.

## 12. Báo cáo Claude phải tạo

Tạo `D:\Do_aN\IMPLEMENTATION_REPORT_EXPERT_FACE_VERIFICATION_FINAL.md` gồm:

1. Commit SHA và file thay đổi theo Backend/Web/Mobile/migration/deployment.
2. Sơ đồ pipeline Detection → Crop → Verification → Admin review.
3. API contract và state mapping cuối.
4. Tên CompreFace application/service đã tạo; **không ghi account password hoặc API key**.
5. Test commands, timestamp, exit code, pass/fail/skipped.
6. Runtime matrix cùng người/khác người/no face/multiple face/provider down.
7. Ảnh chụp Web/Mobile/Admin đã che PII.
8. Bằng chứng Cloudinary private crop và cleanup test object.
9. Lỗi còn lại và severity. Không dùng từ Complete nếu thiếu Docker runtime, Web hoặc Android evidence.

