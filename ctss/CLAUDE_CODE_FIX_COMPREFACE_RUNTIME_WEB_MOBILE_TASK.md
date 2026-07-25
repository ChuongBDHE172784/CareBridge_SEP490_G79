# LỆNH BẮT BUỘC CHO CLAUDE CODE — SỬA COMPREFACE VÀ CHẠY THẬT BACKEND, WEB, MOBILE

**Repository:** `D:\Do_aN`  
**Nhánh chuẩn:** `LamVH1`  
**Báo cáo cần đối chiếu:** `BAO_CAO_TRIEN_KHAI_COMPREFACE_FACE_VERIFICATION.md`  
**Mức độ hiện tại:** `NOT COMPLETE` — không được viết thêm báo cáo “thành công” trước khi sửa và chạy runtime thật.

> Làm trực tiếp đến khi luồng chạy được. Có lỗi compile, migration, runtime, API, UI, Web hoặc Flutter thì tự truy nguyên và sửa luôn trong phạm vi luồng chuyên gia. Không dừng ở việc liệt kê lỗi. Không in hoặc commit secret trong `.env`.

## 1. Kết quả bắt buộc

Hoàn thiện xuyên suốt:

```text
Expert Web/App chụp selfie + CCCD trước/sau
        ↓
Backend lưu ảnh gốc private
        ↓
CompreFace Detection phát hiện đúng một khuôn mặt
        ↓
Backend crop selfie và khuôn mặt CCCD
        ↓
CompreFace Verification so sánh hai face crop
        ↓
Backend lưu score, threshold, trạng thái và crop file IDs
        ↓
Admin nhìn thấy selfie + crop CCCD + dấu xác thực
        ↓
Admin duyệt/từ chối định danh
        ↓
Admin duyệt chứng chỉ và final-approve chuyên gia
```

Backend, Web và Mobile phải dùng cùng contract/state. CompreFace chỉ cung cấp bằng chứng; Admin là người quyết định cuối.

## 2. Sửa ngay các blocker đã audit

### P0-01 — Detection parser sai response

File: `CompreFaceDetectionAdapter.java`.

- Code đang đọc `box.get("prob")`.
- CompreFace 1.2.0 trả `box.probability`.
- Sửa thành typed DTO; không dùng `Map<String,Object>` cast rải rác.
- Parse đúng `result[].box.probability/x_min/y_min/x_max/y_max`.
- Kiểm tra response null, thiếu box, kiểu số sai, box ngoài biên và nhiều khuôn mặt.
- Không suy normalized coordinates nếu API đang trả pixel. DTO phải biểu diễn rõ coordinate space.

### P0-02 — Verification parser sai response

File: `CompreFaceVerificationAdapter.java`.

- Code đang đọc `result[0].subjects` — đây không phải schema Face Verification.
- Response đúng là `result[0].source_image_face` và `result[0].face_matches[]`; similarity nằm trong từng `face_matches[]`.
- Tạo typed DTO và lấy đúng similarity từ `face_matches`.
- Với pipeline đã crop đúng một mặt, yêu cầu cardinality rõ ràng; response thiếu/malformed phải thành `PROVIDER_BAD_RESPONSE`, không âm thầm thành `NOT_MATCHED`.
- Không tìm similarity bằng recursive scan hoặc parser của Recognition API.
- Map HTTP 400/401/403/429/5xx/timeout bằng exception/status typed, không đoán từ `ex.getMessage()`.

Tài liệu chuẩn phải đối chiếu khi code:

- `https://github.com/exadel-inc/CompreFace/blob/master/docs/Rest-API-description.md`

### P0-03 — Hai API key chưa được nối vào cấu hình

Sửa `application.yaml`, `.env.example` phù hợp và configuration properties:

```yaml
carebridge:
  compreface:
    enabled: ${COMPREFACE_ENABLED:false}
    base-url: ${COMPREFACE_BASE_URL:http://localhost:8000}
    detection-api-key: ${COMPREFACE_DETECTION_API_KEY:}
    verification-api-key: ${COMPREFACE_VERIFICATION_API_KEY:}
    similarity-threshold: ${COMPREFACE_SIMILARITY_THRESHOLD:0.75}
    det-prob-threshold: ${COMPREFACE_DET_PROB_THRESHOLD:0.7}
    detection-limit: 2
```

- Không log hai key.
- Không trả key cho Web/Mobile.
- Fail/switch manual review có error code rõ nếu thiếu key.
- Giữ `COMPREFACE_API_KEY` chỉ làm fallback tạm nếu thật sự cần tương thích và ghi deprecation; không tạo hai nguồn cấu hình mâu thuẫn.

### P0-04 — Crop IDs không được trả về API

`IdentityVerificationResponse` có `selfieCropFileId` và `idCardCropFileId`, nhưng `ExpertIdentityVerificationServiceImpl.toResponse()` chưa map chúng.

- Map đủ hai ID và các pipeline fields Admin/Mobile cần.
- Thêm contract test để lỗi này không tái diễn.
- Admin chỉ gọi URL khi ID khác null.

### P0-05 — Crop CCCD bị gắn sai purpose

`FileServiceImpl.uploadPrivateBytes()` hiện tự suy purpose theo `kind`; cả hai crop đều là IMAGE nên đều thành `EXPERT_IDENTITY_SELFIE_CROP`.

- Đổi contract nhận `FilePurpose purpose` tường minh.
- Selfie crop → `EXPERT_IDENTITY_SELFIE_CROP`.
- CCCD crop → `EXPERT_IDENTITY_CCCD_FRONT_CROP`.
- Validate allowlist purpose/kind/access/provider.
- Hai loại crop đều Cloudinary private.
- Cập nhật mọi caller và test.

### P0-06 — Không được tự reject theo AI

Trong `ExpertIdentityVerificationServiceImpl`:

- `MATCHED` → `PENDING_REVIEW`.
- `NOT_MATCHED`, `NO_FACE`, `MULTIPLE_FACES`, `DISABLED`, provider/crop error → `MANUAL_REVIEW_REQUIRED`.
- Không map `NOT_MATCHED` trực tiếp sang human `REJECTED`.
- Admin approve trường hợp không MATCHED phải nhập lý do override bắt buộc và được audit.
- Admin reject luôn cần reason.

### P0-07 — Pipeline status đang sai

Không được luôn ghi `pipelineStatus("COMPLETED")`.

State tối thiểu:

- `PROCESSING`
- `MATCHED`
- `NOT_MATCHED`
- `MANUAL_REVIEW_REQUIRED`
- `PROVIDER_ERROR`
- `CROP_ERROR`

Lưu `pipelineErrorCode`, detection status, processed time. UI không được tô đỏ mọi trạng thái khác MATCHED.

### P0-08 — Báo cáo nói xử lý EXIF nhưng code chưa làm

`FaceCropService.correctExifOrientation()` hiện trả nguyên ảnh.

- Implement EXIF orientation thật bằng thư viện phù hợp hoặc pipeline ảnh đã kiểm thử.
- Loại GPS/EXIF khỏi derived crop.
- Clamp box sau khi rotate.
- Test orientation 1/3/6/8, ảnh portrait/landscape, box sát biên.
- Không ghi “đã xử lý EXIF” nếu implementation vẫn là stub.

### P0-09 — Không giữ transaction DB khi gọi CompreFace

Hiện `submit()` nằm trong service `@Transactional` và thực hiện HTTP Detection/Verification cùng upload storage.

- Tách transaction ngắn để lưu ảnh/attempt.
- Xử lý pipeline qua durable job/outbox hoặc cơ chế retry bền vững.
- Không dùng fire-and-forget chỉ trong RAM.
- Hai worker không được xử lý cùng attempt.
- Provider timeout/down không làm mất ba ảnh gốc hoặc attempt.

Nếu chưa thể hoàn thiện async trong một lượt, phải ít nhất thu hẹp transaction và lưu attempt trước khi gọi provider; ghi rõ giới hạn còn lại là P1, không gọi Complete.

## 3. Backend phải hoàn thiện

- Migration mới phải additive; không sửa migration đã chạy ở môi trường khác.
- Migration chạy được trên PostgreSQL sạch và upgrade DB hiện tại.
- Thêm FK/index/check phù hợp; không để comment kiểu “constraint may fail”.
- Entity/DTO/migration thống nhất tên field và nullability.
- Lưu crop file IDs, similarity, threshold, detection probabilities, bounding boxes, provider error và timestamps.
- Không lưu embedding, API key, base64 hoặc presigned URL.
- Owner/Admin mới xem được selfie, CCCD và crop.
- Signed URL hết hạn phải refresh được.
- Green tick chỉ hợp lệ khi `faceStatus == MATCHED`, score không null và `score >= threshold`.
- Endpoint preview `/api/v1/expert/verify-face` không được tạo nguồn kết quả thứ hai. Xóa auto-preview hoặc chuyển sang preflight explicit, rate-limited, không dùng làm quyết định.
- Error response không được trả raw exception message.

## 4. Web phải hoàn thiện và chạy thật

### Expert onboarding

- Chụp selfie bằng camera trước; CCCD bằng camera sau khi hỗ trợ.
- Đủ ba ảnh mới bật nút gửi.
- Không tự gọi CompreFace khi người dùng vừa chọn ảnh.
- Submit một authoritative attempt rồi hiển thị trạng thái processing/pending/manual.
- Chống double-click, cleanup object URL và dừng polling khi unmount.
- Hiển thị lỗi camera permission, 413, 422, 503 bằng tiếng Việt dễ hiểu.
- Không nói “đã thành chuyên gia” khi mới chỉ face matched.

### Admin identity review

- Hiển thị selfie crop và CCCD face crop cạnh nhau.
- Bên dưới hiển thị ảnh gốc selfie, CCCD trước/sau.
- Badge riêng:
  - Xanh: CompreFace xác nhận trùng khớp.
  - Đỏ: dưới ngưỡng.
  - Vàng: cần kiểm tra thủ công/provider lỗi.
  - Xám: đang xử lý.
- Score 0 phải hiện `0%`, không bị coi là null.
- Có loading, image `onError`, retry/refresh signed URL.
- Approve mismatch/provider error bắt buộc modal + override reason.
- Hai Admin thao tác đồng thời: request stale nhận 409 và UI reload.

## 5. Flutter App phải hoàn thiện và chạy thật

- Kiểm tra `expert_identity_capture_screen.dart`, service, model, router và store.
- Selfie mặc định camera trước; CCCD mặc định camera sau.
- Xử lý permission denied/permanently denied và hướng dẫn mở Settings.
- Correct orientation/compress ảnh an toàn trước upload, không làm mất chi tiết CCCD.
- Hiển thị progress của ba file, retry và lỗi riêng.
- Sau submit không chuyển tiếp như thể identity đã được duyệt. Hiển thị trạng thái:
  - Đang xử lý.
  - CompreFace trùng khớp, chờ Admin.
  - Cần Admin kiểm tra thủ công.
  - Admin từ chối và reason.
- Poll có backoff; cancel timer khi dispose; không setState sau dispose.
- Mobile không gọi trực tiếp CompreFace và không chứa key.
- Journey credential có thể tiếp tục theo nghiệp vụ nhưng final expert features vẫn khóa đến Admin approval.
- Sửa sạch mọi lỗi `flutter analyze` trong phạm vi expert/router/network liên quan.

## 6. Test phải bổ sung trước runtime

### Backend

- Detection response fixture đúng `box.probability`.
- Verification response fixture đúng `source_image_face` + `face_matches`.
- 0/1/2 faces, low probability, malformed JSON, missing box.
- Matched/not matched/empty matches/timeout/401/429/5xx.
- EXIF orientation và crop boundary.
- Hai crop có đúng purpose khác nhau.
- `toResponse()` trả đủ crop file IDs.
- AI mismatch không tự human reject.
- Admin override reason và concurrency 409.
- Migration sạch/upgrade.

### Web/Mobile

- Capture/retake/permission/thiếu ảnh/quá dung lượng.
- Processing → matched/manual/rejected.
- Admin tải crop IDs, image error, refresh URL, override reason.
- Mobile dispose polling an toàn.

## 7. Bắt buộc chạy Docker và Backend thật

Trước hết kiểm tra CompreFace:

```powershell
cd D:\Do_aN\05_Development\Deployment
docker compose -f docker-compose.staging.yml -f docker-compose.compreFace.yml --profile compreface ps
```

Nếu stack chưa chạy, khởi động theo README hiện có. Không xóa volume. Xác nhận account/application có hai service Detection và Verification; không in key.

Sau đó chạy Backend, không chỉ compile:

```powershell
cd D:\Do_aN\05_Development\CareBridgeAPI
.\mvnw.cmd spring-boot:run
```

Linux/macOS tương đương:

```bash
./mvnw spring-boot:run
```

Yêu cầu:

- Giữ process chạy.
- Chờ log application READY/Started.
- Xác nhận Flyway thành công.
- Gọi health endpoint.
- Thực hiện request identity thật qua Backend.
- Nếu start lỗi, đọc root cause và sửa; chạy lại đến khi sạch.
- Không sửa bằng cách disable Flyway/security/CompreFace hoặc nuốt exception.
- Không giết process lạ đang dùng port; chỉ dừng process do chính Claude khởi chạy hoặc báo rõ conflict.

## 8. Bắt buộc chạy Web thật

Mở terminal thứ hai trong khi Backend vẫn chạy:

```powershell
cd D:\Do_aN\05_Development\CareBridgeWebApp
npm run dev
```

Yêu cầu:

- Mở URL Vite được in ra.
- Đăng nhập Expert và đi đủ onboarding/camera/submit/status.
- Đăng nhập Admin và mở queue/ảnh crop/score/badge/approve/reject.
- Theo dõi Browser Console, Network và Backend log.
- Có lỗi runtime, CORS, 401/403/404/409/500, image load hoặc response unwrap thì sửa luôn rồi chạy lại.
- `npm run build` không thay thế `npm run dev` + kiểm tra trình duyệt.

## 9. Bắt buộc chạy Flutter App thật

Mở terminal thứ ba:

```powershell
cd D:\Do_aN\05_Development\CareBridgeMobileApp
flutter pub get
flutter analyze
flutter test
flutter devices
flutter run
```

Yêu cầu:

- Ưu tiên Android emulator/thiết bị thật vì camera/file picker.
- Đi đủ journey Expert capture → submit → trạng thái.
- Kiểm tra Backend nhận đúng multipart names `selfie`, `identityFront`, `identityBack`.
- Có lỗi build, permission manifest, routing, API base URL, cleartext HTTP dev, serialization hoặc UI overflow thì sửa luôn.
- Chạy lại analyze/test/run sau khi sửa.
- Flutter Web không thay thế bằng chứng Android cho camera.

## 10. Runtime matrix bắt buộc

Chạy bằng ảnh test có quyền sử dụng, không commit CCCD thật:

| Case | Kỳ vọng |
|---|---|
| Cùng người | Detection 1+1 mặt, crop hiển thị, score >= threshold, badge xanh, chờ Admin |
| Khác người | Score dưới threshold, manual review, không auto reject |
| Selfie không có mặt | Error/manual state rõ, không mất attempt |
| CCCD có hai mặt | Multiple faces/manual, không chọn similarity tùy ý |
| Ảnh xoay EXIF | Crop đúng hướng |
| CompreFace tắt giữa request | Provider error/retry/manual, ảnh gốc vẫn còn |
| API key sai | Sanitized provider error, không lộ key |
| Hai Admin cùng duyệt | Một thành công, request stale 409 |
| Signed URL hết hạn | Web/Admin refresh URL và ảnh hiển thị lại |

## 11. Quy tắc làm việc và Git

- Đọc `AGENTS.md` và bốn file dual-remote trước thao tác Git.
- Worktree đang có nhiều file modified/untracked; không reset, checkout đè hoặc xóa thay đổi hiện hữu.
- Không trộn contribution/community thay đổi ngoài phạm vi vào commit face verification.
- Kiểm tra `git config user.email`, staged diff và secret scan trước commit.
- Commit semantic, nhỏ theo Backend/Web/Mobile/test.
- Không push trước khi đồng bộ `dev` từ cả `github` và `gitlab` theo workflow dự án.

## 12. Definition of Done

Chỉ báo `COMPLETE` khi đồng thời:

- Detection và Verification parser khớp response CompreFace 1.2.0.
- Hai API key được Backend load an toàn.
- Crop IDs được trả về API và Admin tải được.
- Crop selfie/CCCD có đúng purpose riêng.
- Mismatch không auto reject.
- EXIF implementation thật, không còn stub.
- Backend chạy thật bằng `./mvnw spring-boot:run` hoặc `.\mvnw.cmd spring-boot:run`.
- Web chạy thật bằng `npm run dev` và đã mở journey Expert/Admin.
- Flutter analyze/test/run thành công trên Android và journey identity hoạt động.
- Docker runtime matrix đã chạy, không chỉ unit mock.
- Console/Network/Backend log không còn lỗi liên quan.
- Không lộ secret/PII và không còn TODO/mock/placeholder trong đường chính.

## 13. Báo cáo cuối bắt buộc

Cập nhật hoặc tạo mới:

`D:\Do_aN\BAO_CAO_HOAN_TAT_COMPREFACE_BACKEND_WEB_MOBILE_RUNTIME.md`

Báo cáo phải có:

1. File/commit thay đổi.
2. Root cause và cách sửa từng P0.
3. API/schema/state cuối.
4. Command chính xác đã chạy.
5. Timestamp, exit code và log tóm tắt của Backend/Web/Flutter.
6. URL/route từng màn đã mở và kết quả.
7. Runtime matrix cùng người/khác người/no face/multiple faces/provider down.
8. Ảnh chụp đã che PII và không chứa key.
9. Lỗi còn lại và severity.

Không được ghi “hoàn tất” nếu thiếu bằng chứng của một trong ba lệnh chính:

```text
./mvnw spring-boot:run
npm run dev
flutter run
```

