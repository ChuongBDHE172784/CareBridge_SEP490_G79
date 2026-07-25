---
name: expert-registration-flow
description: Luồng đăng ký chuyên gia cần hoàn thiện — dropdown chuẩn Bộ Y tế, OTP auto-fill, CompreFace, admin review 3 ảnh
metadata: type: project
---

Luồng đăng ký chuyên gia cần hoàn thiện cho cả Web + Mobile + Backend.

## Đã có sẵn (không cần tạo mới)
- Backend CompreFace integration (`expertverification/adapter/CompreFaceVerificationAdapter.java`)
- Backend identity verification service (`expertverification/service/impl/ExpertIdentityVerificationServiceImpl.java`)
- Backend OTP system (`security/service/OtpService.java`, `AuthController.java`)
- Web `ExpertRegisterPage.tsx` + `ExpertOnboardingPage.tsx` + `AdminExpertIdentityReviewPage.tsx`
- Database: `expert_identity_verifications` table (migration V20260717120000)

## Cần làm
1. Backend: API master data (specialties, hospitals, provinces, districts)
2. Backend: Cập nhật `CreateExpertProfileRequest` DTO nhận ID thay vì string tự do
3. Frontend (Web): Thay input text → dropdown/searchable select cho chuyên khoa, nơi công tác
4. Frontend (Web): Hiển thị "tích xanh" khi CompreFace trả về MATCHED
5. Mobile (Flutter): Triển khai luồng tương tự Web
6. Backend: Kiểm tra và sửa `expertverification` controller/endpoint nếu cần

## Why:
User yêu cầu hoàn thiện luồng đăng ký chuyên gia với dropdown chuẩn Bộ Y tế, không cần nhập tay.
CompreFace so khớp selfie vs CCCD, admin thấy 3 ảnh.
