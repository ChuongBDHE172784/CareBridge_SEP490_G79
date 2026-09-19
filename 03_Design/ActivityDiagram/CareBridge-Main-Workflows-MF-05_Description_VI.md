# MF-05: Quy trình Đăng ký & Xác thực Chuyên gia Y tế

| Mục | Nội dung |
|---|---|
| **Mã quy trình** | `MF-05` (Expert Onboarding & Verification Workflow) |
| **Sơ đồ thiết kế** | Tab 5 (`wf-mf05-verified-expert-network`) trong `CareBridge-Main-Workflows.drawio` |
| **2 bên tham gia (2 làn)** | **1. Bác sĩ / Chuyên gia y tế (Doctor / Expert)**<br>**2. Hệ thống CareBridge & Quản trị viên (System (CareBridge App & Admin))** |
| **Mục tiêu duy nhất** | Quản lý toàn bộ quy trình bác sĩ gia nhập hệ thống: từ lúc nộp hồ sơ, kiểm tra giấy tờ thật - giả, ký hợp đồng điện tử, phân loại nhóm (Cộng đồng hay Hợp tác), đến khi cài lịch rảnh và sẵn sàng làm việc. |

---

## 1. Tóm tắt câu chuyện thực tế (Kể chuyện dễ hiểu cho người ngoài ngành)

Quy trình này giải quyết câu hỏi: **Làm sao để CareBridge tuyển chọn được bác sĩ thật, bằng thật vào ứng dụng và phân định rõ trách nhiệm của từng người?**

1. **Bác sĩ đăng ký**: Bác sĩ tải app, điền hồ sơ chuyên môn (chuyên khoa Sản/Nhi/Dinh dưỡng, bệnh viện công tác) và chọn trước mình muốn làm **Bác sĩ Cộng đồng** (thiện nguyện) hay **Bác sĩ Hợp tác** (khám dịch vụ có cam kết trực ca).
2. **Nộp giấy tờ xác minh**: Bác sĩ chụp 2 mặt Căn cước công dân (CCCD), chụp ảnh chân dung selfie và tải ảnh Chứng chỉ hành nghề (CCHN) y tế.
3. **Kiểm tra thật - giả 3 lớp**:
   - Hệ thống tự so khớp ảnh chụp mặt trực tiếp với ảnh trên CCCD qua AI (chống mạo danh).
   - Kiểm tra số CCHN với dữ liệu quản lý của ngành Y tế.
   - Quản trị viên (Admin) thẩm định lại lần cuối và bấm Duyệt.
   - *Nếu từ chối (ảnh mờ, sai số bằng)*: Bác sĩ nhận lý do và chụp nộp lại.
4. **Phân loại sau khi duyệt**:
   - **Nhánh Bác sĩ Cộng đồng (`COMMUNITY`)**: Cấp tích xanh xác thực. Bác sĩ không cần ký hợp đồng, không bị ép giờ trực. Hồ sơ được kích hoạt và xuất bản ngay lên danh bạ để hỗ trợ người dùng khi rảnh.
   - **Nhánh Bác sĩ Hợp tác (`CONTRACTED`)**: Hệ thống tạo đề nghị hợp tác (thời hạn 12 tháng, cam kết trực tối thiểu 10 ca/tuần) -> Bác sĩ đọc và ký hợp đồng online ngay trên app -> Hệ thống lưu PDF lên kho bảo mật, ghi nhật ký kiểm toán và kích hoạt tích xanh đối tác -> Bác sĩ cài lịch rảnh bắt buộc -> Xuất bản lên danh bạ với ưu tiên điều phối hàng đầu.
5. **Giám sát định kỳ**: Hệ thống tự động rà soát hạn chứng chỉ hành nghề hoặc ghi nhận vi phạm để tạm ngưng tích xanh và khóa quyền nhận ca nếu cần.

---

## 2. Bảng mô tả chi tiết từng bước trên sơ đồ

| Bước | Tên bước trên sơ đồ | Ai làm? | Giải thích đơn giản (Không dùng từ kỹ thuật) | Căn cứ mã nguồn trong dự án |
|:---:|---|:---:|---|---|
| **Start** | Bắt đầu | Bác sĩ | Bác sĩ đăng nhập tài khoản và bắt đầu tiến trình đăng ký hồ sơ y tế. | Trạng thái ban đầu: `DRAFT`. |
| **1** | Đăng ký & Chọn nhóm hoạt động | Bác sĩ | Điền họ tên, chuyên khoa, bệnh viện công tác và chọn nhóm: **Bác sĩ Cộng đồng** hay **Bác sĩ Hợp tác**. | [`UC-EX-01`](file:///d:/Do_aN/04_Implement/UC-EX-01-CreateExpertProfileAndType/CreateExpertProfileAndType_TDS.md)<br>`ExpertProfileServiceImpl.createProfile()` |
| **2** | Gửi ảnh CCCD & Chứng chỉ hành nghề | Bác sĩ | Chụp ảnh CCCD 2 mặt, chụp ảnh selfie khuôn mặt và tải ảnh Chứng chỉ hành nghề y tế. | [`UC-EX-03`](file:///d:/Do_aN/04_Implement/UC-EX-03-VerifyExpertIdentityAndFace/VerifyExpertIdentityAndFace_TDS.md), [`UC-EX-04`](file:///d:/Do_aN/04_Implement/UC-EX-04-SubmitCredentialsAndTrackVerification/SubmitCredentialsAndTrackVerification_TDS.md)<br>`ExpertIdentityVerificationService` |
| **3** | So khớp ảnh, kiểm tra CCHN & Duyệt | Hệ thống & Admin | Máy so ảnh mặt với CCCD xem có đúng người không; tra cứu chứng chỉ với dữ liệu ngành y tế; sau đó Admin xem lại và bấm duyệt. | [`UC-AD-06`](file:///d:/Do_aN/04_Implement/UC-AD-06-VerifyExpertsAndCredentials/VerifyExpertsAndCredentials_TDS.md)<br>`CompreFacePipelineAdapter`, `HcmMedinetRegistrySource` |
| **Dec 1** | Hồ sơ có hợp lệ không? | Hệ thống | Kiểm tra kết quả thẩm định. | **Không hợp lệ**: Sang Bước 4A.<br>**Hợp lệ**: Sang Bước Dec 2. |
| **4A** | Xem lý do từ chối & Nộp lại | Bác sĩ | Xem lý do hồ sơ bị từ chối (ảnh mờ, sai thông tin...), chụp và nộp lại giấy tờ (quay lại Bước 2). | `verificationStatus = REJECTED`<br>Vòng lặp Resubmit quay về bước 2 |
| **Dec 2** | Bác sĩ thuộc nhóm nào? | Hệ thống | Phân nhánh xử lý theo nhóm bác sĩ đã được admin phê duyệt. | `determineNextStep()` |
| **4B** | Cấp tích xanh Bác sĩ Cộng đồng | Hệ thống | **Nhánh Cộng đồng**: Cấp tích xanh `Verified Community Expert`. Bác sĩ hoàn tất onboarding mà không cần ký hợp đồng hay cam kết giờ trực; hồ sơ đi thẳng đến bước xuất bản danh bạ (Bước 7). | `expert_type = COMMUNITY`<br>`determineNextStep() = COMPLETE` |
| **5A** | Tạo hợp đồng dịch vụ (kèm giờ trực) | Hệ thống | **Nhánh Hợp tác**: Hệ thống lấy đúng họ tên trên CCCD và số chứng chỉ đã duyệt điền vào hợp đồng mẫu 11 điều, quy định cam kết trực tối thiểu 10 ca/tuần. | [`UC-EX-02`](file:///d:/Do_aN/04_Implement/UC-EX-02-ReviewAndAcceptExpertContract/ReviewAndAcceptExpertContract_TDS.md)<br>`ExpertContractService.getOffer()` |
| **5B** | Bác sĩ đọc & Ký hợp đồng online | Bác sĩ | Bác sĩ đọc các điều khoản và bấm xác nhận ký ngay trên app điện thoại (chống sửa đổi bằng mã băm SHA-256). | `ExpertContractService.accept()`<br>(Căn cứ Luật Giao dịch điện tử 2023) |
| **5C** | Lưu hợp đồng & Kích hoạt tích xanh | Hệ thống | Lưu PDF bản ký vào kho R2 riêng biệt, ghi nhật ký kiểm toán (`ACCEPTED`) và chuyển trạng thái bác sĩ thành `CONTRACTED`. | `PdfRenderer.render()`<br>`FilePurpose.EXPERT_CONTRACT`<br>`acceptanceRepository.save()` |
| **6** | Cài đặt lịch rảnh & phạm vi tư vấn | Bác sĩ | Cài đặt các khung giờ rảnh trong tuần (tối thiểu 10 ca/tuần) và chọn chuyên khoa tư vấn để hoàn tất điều kiện làm việc. | [`UC-EX-06`](file:///d:/Do_aN/04_Implement/UC-EX-06-ManageAvailabilityCalendar/ManageAvailabilityCalendar_TDS.md)<br>`replaceAvailability()` |
| **7** | Đăng hồ sơ & Giờ rảnh lên ứng dụng | Hệ thống | Đưa thông tin bác sĩ và lịch trực lên danh bạ công khai của ứng dụng để người dùng có thể tìm kiếm và đặt lịch khám. | [`UC-EX-07`](file:///d:/Do_aN/04_Implement/UC-EX-07-BrowseExpertDirectory/BrowseExpertDirectory_TDS.md)<br>`getAllExperts()`, `ExpertMatchingService` |
| **Dec 3** | Hết hạn chứng chỉ hoặc có vi phạm? | Hệ thống | Hệ thống tự động quét ngày hết hạn chứng chỉ hành nghề định kỳ hoặc ghi nhận khiếu nại nghiêm trọng. | Quét định kỳ & Quản lý vi phạm. |
| **8** | Tạm thu hồi tích xanh & Khóa nhận ca | Hệ thống | Nếu chứng chỉ hết hạn hoặc vi phạm quy chế: Tạm ẩn bác sĩ khỏi danh bạ, khóa quyền nhận ca mới và yêu cầu giải trình. | `ExpertProfile.trustStatus = SUSPENDED`<br>`isEligibleForConsultation() = false` |
| **End** | Hoàn tất | Hệ thống | Bác sĩ đã chính thức gia nhập và vận hành an toàn trên hệ thống CareBridge. | Điểm kết thúc tiến trình. |

---

## 3. Ba điểm cốt lõi để trả lời khi bảo vệ đồ án

1. **Tuyển chọn bác sĩ chặt chẽ (Chống mạo danh y tế)**: Không ai có thể tự lập tài khoản mạo nhận làm bác sĩ. Phải có CCCD thật, quét khuôn mặt thật và số Chứng chỉ hành nghề được kiểm tra với cơ quan y tế thì Admin mới duyệt.
2. **Minh bạch pháp lý khi ký hợp đồng**: Hệ thống chỉ sinh hợp đồng điện tử khi đã có đủ dữ liệu thật từ CCCD và CCHN. Bác sĩ ký trực tiếp trên app với cam kết ca trực rõ ràng, lưu trữ an toàn trên kho dữ liệu riêng biệt.
3. **Phân định rõ ràng giữa 2 mô hình**:
   - Bác sĩ Hợp tác (`CONTRACTED`): Có ký hợp đồng, có cam kết ca trực tối thiểu (10 ca/tuần), được ưu tiên điều phối hàng đầu (Tuyến 1).
   - Bác sĩ Cộng đồng (`COMMUNITY`): Hỗ trợ thiện nguyện, không bị áp KPI giờ giấc, vẫn có thể nhận tư vấn khi rảnh rỗi (Tuyến 2).
