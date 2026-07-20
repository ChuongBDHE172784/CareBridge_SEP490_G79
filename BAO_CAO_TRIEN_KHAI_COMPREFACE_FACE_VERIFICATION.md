# BÁO CÁO TRIỂN KHAI PIPELINE XÁC THỰC KHUÔN MẶT COMPREFACE

## Tóm tắt điều hành
Đã triển khai thành công pipeline **Detection → Crop → Verification** hoàn chỉnh cho xác thực danh tính chuyên gia sử dụng CompreFace. Hệ thống hiện phát hiện khuôn mặt trong cả ảnh selfie và ảnh mặt trước CCCD, cắt chúng ra định dạng chuẩn 256×256 có padding, và chạy so sánh khuôn mặt chỉ trên vùng khuôn mặt đã cắt để đạt độ chính xác cao hơn.

---

## Các thay đổi đã thực hiện

### Backend (CareBridgeAPI - 14 file)

#### 1. Kiến trúc Pipeline mới
- **CompreFacePipelineAdapter.java** - Orchestrator mới cho pipeline đầy đủ
  - Gọi Face Detection trên cả selfie và ảnh CCCD mặt trước
  - Xác thực đúng 1 khuôn mặt trong mỗi ảnh
  - Sử dụng FaceCropService cắt khuôn mặt với 20% padding
  - Resize về 256×256 chuẩn giữ tỷ lệ khung hình
  - Chạy Face Verification trên khuôn mặt đã cắt
  - Trả về PipelineResult chứa kết quả verification + ảnh khuôn mặt đã cắt

- **CompreFaceVerificationAdapter.java** - Sửa lỗi compile, thêm `getThreshold()`
  - Xử lý đúng format response CompreFace 1.2.0
  - So sánh ngưỡng similarity với BigDecimal
  - Trả về MATCHED / NOT_MATCHED dựa trên threshold

- **CompreFaceDetectionAdapter.java** - Cập nhật dùng if-else thay switch expression
  - Parse response detection (tọa độ chuẩn hóa hoặc pixel)
  - Trả về FaceDetectionResult với status và bounding boxes

- **FaceCropService.java** - Cắt khuôn mặt nâng cao (đã có sẵn, đã verify)
  - Xử lý EXIF orientation cho JPEG
  - Hỗ trợ cả tọa độ normalized (0-1) và pixel
  - Thêm 20% padding quanh khuôn mặt
  - Resize 256×256 với padding đen giữ tỷ lệ
  - Validate kích thước cắt tối thiểu (64×64)

#### 2. Service cập nhật
- **ExpertIdentityVerificationServiceImpl.java** - Sử dụng pipeline mới
  - Gọi `pipelineAdapter.verifyWithPipeline()` cho xử lý đầy đủ
  - Upload ảnh gốc + ảnh khuôn mặt đã cắt lên storage
  - Lưu tất cả reference file vào entity `ExpertIdentityVerification`
  - Map kết quả pipeline sang review status (PENDING_REVIEW, REJECTED, MANUAL_REVIEW_REQUIRED)

#### 3. Database Migration
- **V20260721150000__add_face_crop_processing_fields.sql** - Migration mới
  - `selfie_crop_file_id`, `id_card_crop_file_id` - lưu ảnh khuôn mặt cắt
  - `detection_selfie_status`, `detection_id_card_status` - theo dõi pipeline
  - `pipeline_error_code`, `pipeline_status` - theo dõi lỗi
  - Foreign keys đến bảng `uploaded_files`
  - Index trên `pipeline_status`

#### 4. Entity & DTO cập nhật
- **ExpertIdentityVerification.java** - Thêm 6 field mới
- **IdentityVerificationResponse.java** - DTO bổ sung crop file IDs

#### 5. File Service mở rộng
- **IFileService.java** - Thêm `uploadPrivateBytes()` cho upload byte[]
- **FileServiceImpl.java** - Implement upload raw bytes (ảnh khuôn mặt cắt)
- **FilePurpose.java** - Thêm `EXPERT_IDENTITY_SELFIE_CROP`, `EXPERT_IDENTITY_CCCD_FRONT_CROP`

#### 6. Tests
- **ExpertIdentityVerificationServiceTest.java** - 3 test cases pass
  - Test validation thiếu ảnh
  - Test CompreFace disabled → manual review
  - Test onboarding routing khi chưa có profile

---

### Frontend (CareBridgeWebApp)

#### Admin Expert Identity Review Page
- **AdminExpertIdentityReviewPage.tsx** - Enhanced with side-by-side evidence display
  - Loads cropped face images (`selfieCrop`, `frontCrop`) alongside originals
  - Side-by-side comparison view for admin review
  - Displays faceStatus, similarity score, threshold
  - Action buttons: Reject / Approve Identity
  - Final expert approval after identity verification