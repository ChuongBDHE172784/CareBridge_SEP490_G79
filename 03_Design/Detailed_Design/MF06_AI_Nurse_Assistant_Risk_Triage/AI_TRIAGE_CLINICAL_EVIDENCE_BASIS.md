# Căn cứ và giới hạn lâm sàng của AI Triage CareBridge

> Trạng thái tại ngày 14/08/2026: tài liệu giải trình kỹ thuật, **không phải phê duyệt lâm sàng**. Không cơ quan/tổ chức nào dưới đây đã thẩm định hoặc chứng nhận CareBridge.

## 1. CareBridge đang làm gì?

CareBridge là công cụ **định hướng mức độ khẩn cấp ban đầu**, không chẩn đoán, không kê đơn và không thay thế việc khám trực tiếp. Hệ thống dùng Gemini để trích xuất dữ kiện có kiểm tra; quyết định mức nguy cơ do rule deterministic thực hiện. Khi dữ liệu thiếu, mâu thuẫn, rule chưa đủ bao phủ hoặc dịch vụ lỗi, hệ thống phải dừng ở “Cần thêm thông tin”/chuyển nhân viên y tế, không được suy ra Xanh.

Disclaimer bắt buộc:

> Thông tin từ AI chỉ mang tính chất tham khảo, bạn cần tham vấn trực tiếp Bác sĩ/Chuyên gia Y tế khi có triệu chứng bất thường.

## 2. Căn cứ cho ba màu

### Trẻ bệnh: căn cứ Việt Nam trực tiếp

Quyết định **2341/QĐ-BYT ngày 07/08/2024**, “Hướng dẫn Xử trí lồng ghép chăm sóc trẻ bệnh” (IMCI), là căn cứ phù hợp nhất cho ngữ nghĩa màu ở nhánh trẻ em:

| Màu | Ý nghĩa trong IMCI | Diễn giải an toàn trên CareBridge |
|---|---|---|
| Đỏ | Bệnh nặng; điều trị cấp cứu trước chuyển viện và chuyển viện/nhập viện khẩn | Cần được nhân viên y tế đánh giá ngay/cấp cứu |
| Vàng | Cần điều trị đặc hiệu tại cơ sở y tế và theo dõi | Cần được nhân viên y tế đánh giá sớm |
| Xanh | Không cần điều trị đặc hiệu; tư vấn chăm sóc tại nhà và dấu hiệu phải quay lại | Chỉ có thể cân nhắc theo dõi tại nhà sau khi đủ dữ liệu và không có phân loại nặng hơn |

IMCI chọn **phân loại nặng nhất** khi một trẻ đồng thời khớp nhiều ô. Hướng dẫn chia trẻ nhỏ thành nhóm **0–2 tháng** và **2 tháng–5 tuổi**, được thiết kế cho nhân viên y tế có đào tạo tại cơ sở khám chữa bệnh. Vì CareBridge hiện chỉ hỗ trợ 0–23 tháng và chia stage 0–11/12–23 tháng, việc ánh xạ từng rule vẫn phải được bác sĩ nhi thẩm định; không thể lấy tên “IMCI” để mặc nhiên hợp thức hóa mọi ngưỡng.

Văn bản Bộ Y tế số 199/BYT-BMTE năm 2025 xác nhận QĐ 2341 cùng QĐ **2246/QĐ-BYT** (khám định kỳ trẻ dưới 24 tháng) là hướng dẫn chuyên môn dành cho cơ sở khám chữa bệnh và đào tạo nhân viên y tế. Đây là bằng chứng về nguồn và đối tượng áp dụng, không phải giấy phép cho chatbot tự đánh giá.

### Thai kỳ, sau sinh và chuẩn bị mang thai

Không có bằng chứng rằng QĐ 2341 áp dụng cho phụ nữ. Nguồn BYT cần đối chiếu theo từng chủ đề:

- **QĐ 1139/QĐ-BYT ngày 23/04/2026**: Hướng dẫn quốc gia về chăm sóc trước khi có thai và trước khi sinh; áp dụng tại cơ sở khám chữa bệnh toàn quốc, thay phần tương ứng của QĐ 4128/QĐ-BYT năm 2016. Tài liệu hỗ trợ tư vấn/sàng lọc và dấu hiệu cần khám lại, nhưng không định nghĩa một bảng màu chatbot cho toàn bộ mẹ bầu.
- **QĐ 1154/QĐ-BYT ngày 04/05/2024**: sàng lọc, chẩn đoán và xử trí tăng huyết áp thai kỳ, tiền sản giật và sản giật. Đây là nguồn cần dùng để bác sĩ đối chiếu rule đau đầu nặng–thay đổi thị lực–huyết áp; hiện chưa có traceability đã ký cho từng predicate.
- Nhánh sau sinh cần nguồn BYT riêng cho băng huyết, nhiễm trùng và an toàn tâm thần; hiện registry chưa chứng minh đầy đủ các ánh xạ này.
- Chuẩn bị mang thai chủ yếu là dự phòng, tư vấn và phát hiện yếu tố cần khám. Không được tuyên bố ba màu ở nhánh này “theo IMCI”.

### WHO chỉ là nguồn hỗ trợ/đối chiếu

- WHO IMCI dùng Đỏ = chuyển khẩn, Vàng = điều trị tại cơ sở, Xanh = chăm sóc tại nhà; ưu tiên phân loại nặng hơn.
- WHO IITT định nghĩa Đỏ = khám ngay, Vàng = khám sớm, Xanh = có thể chờ, nhưng đây là công cụ **triage tại cơ sở cấp cứu** cho nhân viên y tế, không phải công cụ tự đánh giá tại nhà.
- WHO PCPNC/preconception cung cấp dấu hiệu nguy hiểm và can thiệp theo giai đoạn; không chứng nhận rule CareBridge.

## 3. Trạng thái chứng cứ thật trong repository

`triage_rules_v2.json` hiện có 17 rule: 11 Đỏ, 2 Vàng, 2 Cần thêm thông tin, 1 Ngoài phạm vi và 1 Xanh. **Cả 17 đang `sourceVerificationStatus=PENDING`**. Public GREEN đang bị release gate khóa. Do đó:

- Có nền tảng hợp lý cho semantics Đỏ/Vàng/Xanh ở IMCI trẻ em.
- Chưa đủ cơ sở để nói “các rule đã được BYT phê duyệt” hoặc “AI Triage tuân thủ đầy đủ QĐ 2341”.
- Citation hiển thị cho người dùng chỉ được phát khi tài liệu có URL HTTPS, domain được duyệt, hash cố định và trạng thái `SOURCE_VERIFIED`; hiện manifest tổng thể vẫn `PENDING`.

### Bộ câu hỏi hiện tại có phải “bộ câu hỏi của Bộ Y tế” không?

**Không.** Nội dung câu hỏi và lựa chọn trong `question_catalog_v1.json` do CareBridge biên soạn để thu thập các field/signal mà rule engine cần; đây không phải bản sao nguyên văn, biểu mẫu được Bộ Y tế cấp phép, hay bộ câu hỏi đã được Bộ Y tế thẩm định. Việc API công bố trực tiếp catalog này chỉ giải quyết tính nhất quán và tiếng Việt trên UI, không làm thay đổi trạng thái lâm sàng của catalog.

Muốn gọi đây là bộ câu hỏi “dựa trên BYT” một cách có thể kiểm toán, mỗi câu hỏi phải có traceability **question → field/signal → predicate rule → mục/trang/phụ lục của văn bản BYT**, kèm người có chuyên môn duyệt wording, lựa chọn và ngưỡng. Khi chưa có bảng đó, cách diễn đạt đúng là “bộ câu hỏi nội bộ đang chờ thẩm định, có tham khảo hướng dẫn BYT/WHO”.

## 4. Việc cần bác sĩ/người có thẩm quyền quyết định

1. Lập bảng **rule → câu/ô/phụ lục/trang** từ QĐ 2341 cho từng rule nhi; xử lý sai khác nhóm tuổi 0–2 tháng.
2. Đối chiếu và ký từng predicate thai kỳ với QĐ 1154 và QĐ 1139; không chỉ ký tên nguồn ở mức tài liệu.
3. Chọn và ký nguồn chính thức cho băng huyết/nhiễm trùng/tâm thần sau sinh.
4. Quyết định định nghĩa sản phẩm cho Xanh. Cho tới khi xong, tiếp tục khóa public GREEN là đúng.
5. Thực hiện validation lâm sàng hồi cứu/tiến cứu, đo sensitivity/false-negative theo stage trước khi dùng ngoài môi trường học thuật.

## 5. Chính sách dữ liệu nhập

- Tuổi bé hỗ trợ: `0..23` tháng; số âm hoặc từ 24 tháng trở lên bị từ chối vì ngoài phạm vi sản phẩm, không phải vì “không thể tồn tại”.
- Nhiệt độ cơ thể Celsius được nhận trong `30..45°C`; ngoài khoảng phải yêu cầu kiểm tra số đo/đơn vị, không clamp và không coi là bằng chứng lâm sàng.
- Tuổi thai: `1..45` tuần; số ngày sau sinh không âm.
- UI chặn để người dùng sửa ngay; Java/Python kiểm tra lại để client giả mạo không vượt qua biên tin cậy.

## 6. Nguồn tham khảo

- Bộ Y tế, QĐ 2341/QĐ-BYT (07/08/2024), bản triển khai được hệ thống Bộ Y tế xác nhận: <https://emohbackup.moh.gov.vn/publish/attach/getfile/411552>
- Bản toàn văn QĐ 2341/QĐ-BYT để tra cứu nội dung (nguồn pháp luật thứ cấp, không thay thế bản Bộ Y tế): <https://luatvietnam.vn/y-te/quyet-dinh-2341-qd-byt-2024-tai-lieu-huong-dan-xu-tri-long-ghep-cham-soc-tre-benh-363662-d1.html>
- Bộ Y tế, QĐ 1139/QĐ-BYT (23/04/2026): <https://vnpa.moh.gov.vn/wp-content/uploads/2026/05/QD-1139-Tai-lieu-huong-dan-cham-soc-SKSS.pdf>
- WHO, Interagency Integrated Triage Tool: <https://www.who.int/tools/triage>
- WHO, IMCI Module 2 — The Sick Young Infant: <https://iris.who.int/bitstream/handle/10665/104772/9789241506823_Module-2_eng.pdf>
- WHO, IMCI handbook: <https://iris.who.int/bitstream/handle/10665/42939/9241546441.pdf>
