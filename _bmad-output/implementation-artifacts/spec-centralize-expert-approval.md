---
title: 'Tập trung xét duyệt chuyên gia và đọc tài liệu chứng chỉ'
type: 'bugfix'
created: '2026-07-28'
status: 'done'
baseline_commit: 'a44acf832c73d94622744179d0a6c388ac5d75a7'
context:
  - 'CLAUDE.md'
  - '_bmad-output/implementation-artifacts/spec-fix-expert-canonical-flow.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Quản trị viên phải đi qua ba màn tách rời để duyệt định danh, chứng chỉ, quyết định cuối và trạng thái tin cậy; màn chứng chỉ còn suy đoán loại file từ URL ký tạm nên không đọc ổn định PDF, DOC và DOCX.

**Approach:** Cung cấp một trung tâm xét duyệt theo từng hồ sơ chuyên gia, gom thông tin cá nhân, định danh, toàn bộ chứng chỉ, quyết định cuối và quản lý tin cậy trong cùng route. Dùng metadata của bảng `attachments` hiện có và endpoint xem trước được bảo vệ để đọc tài liệu, tuyệt đối không tạo bảng mới.

## Boundaries & Constraints

**Always:** Giữ `SYSTEM_ADMIN` RBAC, URL/tệp riêng tư, audit và điều kiện phê duyệt cuối hiện có; quyết định từ chối phải có lý do; nhóm dữ liệu theo `expertProfileId`; giới hạn kích thước/nội dung xem trước; chỉ trả văn bản đã vô hiệu hóa markup từ PDF/DOC/DOCX; giữ route cũ bằng redirect về màn tập trung.

**Ask First:** Thay đổi quy tắc eligibility/trust, gửi tài liệu riêng tư sang dịch vụ xem file bên thứ ba, hoặc thêm thư viện khác ngoài Apache Tika phục vụ trích xuất tài liệu.

**Never:** Tạo bảng/migration mới; đưa URL định danh/chứng chỉ thành public; nhúng Office/Google viewer bên ngoài; tự động phê duyệt từ kết quả khuôn mặt; commit file graph, `.cursor`, script thử nghiệm hoặc thay đổi ngoài phạm vi.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Hồ sơ chờ duyệt | Profile có định danh/chứng chỉ ở các trạng thái khác nhau | Một item hồ sơ hiển thị đủ bước, bằng chứng và hành động tại cùng màn | Một phần API lỗi phải báo rõ và không đưa ra trạng thái hoàn tất giả |
| Xem tài liệu | Attachment là ảnh, PDF, DOC hoặc DOCX hợp lệ | Ảnh/PDF hiển thị trực tiếp; PDF/DOC/DOCX có nội dung đọc được và vẫn có nút tải bản gốc | MIME không hỗ trợ, file hỏng/quá lớn trả lỗi an toàn, không làm mất hồ sơ đang chọn |
| Ra quyết định | Admin duyệt/từ chối từng bằng chứng rồi duyệt cuối | UI cập nhật đúng trạng thái hồ sơ mà không chuyển route | Reject thiếu lý do bị chặn; quyết định stale/final hiển thị conflict |
| Hồ sơ đã duyệt | Profile APPROVED hoặc trust bị hạn chế | Có thể lọc và quản lý trust ngay trong trung tâm | Không cho thao tác trust trái enum/permission |

</frozen-after-approval>

## Code Map

- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/` -- identity, credential, metadata attachment và quyết định review canonical.
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/` -- authorize, tải byte riêng tư và metadata `originalName`/`mimeType`.
- `05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx` -- route canonical sẽ trở thành trung tâm theo hồ sơ.
- `05_Development/CareBridgeWebApp/src/app/{router/index.tsx,layouts/AdminLayout.tsx}` -- một menu; route cũ redirect.

## Tasks & Acceptance

**Execution:**
- [x] `CareBridgeAPI/pom.xml`, `expertverification/{dto,service,controller}/**`, `file/{service,storage}/**` -- trả review-case aggregate và preview text PDF/DOC/DOCX bằng Apache Tika, dùng attachment hiện có, giới hạn byte/output và giữ authorization.
- [x] `DocumentReviewResponse`, credential mapper/service -- trả `fileName`, `mimeType`, `fileSizeBytes` từ `ViewFileResponse` thay vì đoán từ presigned URL.
- [x] `ExpertVerificationQueuePage.tsx`, `expertApi.ts` -- hiển thị một danh sách theo chuyên gia, các bước định danh/chứng chỉ/final/trust, viewer theo MIME và trạng thái cập nhật tại chỗ.
- [x] `AdminLayout.tsx`, `router/index.tsx` -- chỉ còn một mục “Xét duyệt chuyên gia”; redirect các URL identity/trust cũ về route canonical.
- [x] Focused Backend/Web tests -- bao phủ aggregate theo hồ sơ, preview R2 có giới hạn, viewer private và các vùng quyết định tập trung.

**Acceptance Criteria:**
- Given một chuyên gia có định danh và nhiều chứng chỉ, when admin mở `/admin/expert-verification-queue`, then mọi bằng chứng và hành động phê duyệt xuất hiện trong cùng một hồ sơ, không cần sang màn khác.
- Given attachment PDF/DOC/DOCX có URL ký không mang phần mở rộng, when admin mở tài liệu, then viewer dùng MIME/name từ backend và hiển thị nội dung đọc được.
- Given admin hoàn tất mọi gate, when bấm phê duyệt cuối, then profile chuyển APPROVED và trạng thái mới hiển thị tại chỗ.
- Given route identity/trust cũ, when truy cập, then được redirect về màn xét duyệt tập trung.

## Design Notes

Review-case là DTO tổng hợp đọc từ `professional_profiles`, `expert_credentials`, `attachments` và user hiện có; không persistence mới. Preview tài liệu chạy server-side, không gửi URL riêng tư cho dịch vụ bên thứ ba, chỉ trả plain text có giới hạn; bản gốc vẫn đi qua URL ngắn hạn đã authorize.

## Verification

**Commands:**
- `.\mvnw.cmd -Dtest=*Expert*Review*,*Credential* test` -- aggregate, quyết định và preview backend pass.
- `npm test -- --run src/features/expert` -- UI/API tests pass.
- `npm run build` -- production TypeScript build pass.

## Suggested Review Order

**Luồng tập trung**

- Entry point gom định danh, chứng chỉ, quyết định cuối và trust theo một hồ sơ.
  [`ExpertVerificationQueuePage.tsx:145`](../../05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx#L145)

- Backend tổng hợp trạng thái và giữ gate cơ sở y tế đã xác minh.
  [`ExpertIdentityVerificationServiceImpl.java:305`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/service/impl/ExpertIdentityVerificationServiceImpl.java#L305)

- Một menu canonical thay thế các điểm xét duyệt rời rạc.
  [`AdminLayout.tsx:25`](../../05_Development/CareBridgeWebApp/src/app/layouts/AdminLayout.tsx#L25)

- Route cũ redirect về trung tâm để giữ tương thích bookmark.
  [`index.tsx:162`](../../05_Development/CareBridgeWebApp/src/app/router/index.tsx#L162)

**Tài liệu riêng tư trên R2**

- Preview xác thực MIME, giới hạn byte/text và trích xuất PDF/DOC/DOCX bằng Tika.
  [`ExpertCredentialServiceImpl.java:222`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/expertverification/service/impl/ExpertCredentialServiceImpl.java#L222)

- R2 kiểm tra object size và dùng ranged read trước khi nạp vào heap.
  [`R2StorageService.java:66`](../../05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/file/service/impl/R2StorageService.java#L66)

- Viewer lấy URL ký mới chỉ khi admin thực sự mở tài liệu.
  [`ExpertVerificationQueuePage.tsx:57`](../../05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.tsx#L57)

**Kiểm chứng**

- Backend test aggregation theo hồ sơ và trạng thái gate.
  [`ExpertIdentityVerificationServiceTest.java:148`](../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertIdentityVerificationServiceTest.java#L148)

- Backend test đọc DOCX private từ R2 qua parser.
  [`ExpertCredentialPreviewServiceTest.java:38`](../../05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/expertverification/ExpertCredentialPreviewServiceTest.java#L38)

- Web test toàn bộ vùng xét duyệt và viewer URL ký mới.
  [`ExpertVerificationQueuePage.test.tsx:78`](../../05_Development/CareBridgeWebApp/src/features/expert/pages/ExpertVerificationQueuePage.test.tsx#L78)
