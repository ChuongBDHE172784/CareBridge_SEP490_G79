# PROMPT MẪU LÝ TƯỞNG: CHUẨN HÓA TÀI LIỆU Y TẾ SANG MARKDOWN DÀNH CHO CAREBRIDGE AI RAG

> **Mục đích:** Sử dụng câu Prompt dưới đây để yêu cầu bất kỳ AI nào (ChatGPT, Claude, Gemini...) chuyển đổi tài liệu y khoa thô (PDF, Word, Sách y tế, Quyết định Bộ Y Tế, bài báo khoa học) sang định dạng Markdown (`.md`) chuẩn cấu trúc tối ưu 100% cho thuật toán **Chunking (900 chars / 180 overlap)** và **Vector Embedding (768 chiều)** của CareBridge.

---

## 📋 CÂU PROMPT MASTER (COPY TOÀN BỘ PHẦN DƯỚI ĐÂY)

```text
Bạn là Chuyên gia Kỹ thuật Tri thức Y tế & AI RAG (Medical Knowledge Engineer).
Nhiệm vụ của bạn là đọc toàn bộ nội dung tài liệu thô được cung cấp dưới đây và biên tập, chuẩn hóa lại thành một file Markdown (.md) chất lượng cao nhất, tối ưu 100% cho hệ thống AI RAG (Retrieval-Augmented Generation) chăm sóc sức khỏe mẹ bầu CareBridge.

HÃY TUÂN THỦ NGHIÊM NGẶT CÁC QUY TẮC SAU:

1. ĐẦU FILE BẮT BUỘC CÓ YAML FRONTMATTER:
---
title: [Tiêu đề cẩm nang rõ ràng, tiếng Việt có dấu]
stage: [Chọn 1 trong các giá trị: PRECONCEPTION | PREGNANCY | POSTPARTUM | ALL]
topic: [Chọn 1 trong các giá trị: DANGER_SIGNS | NUTRITION | HEALTH_MONITORING | POSTPARTUM_CARE | GENERAL]
source: [Tên cơ quan y tế, tác giả, nhà xuất bản, ví dụ: Bộ Y Tế Việt Nam / WHO / BV Từ Dũ / Viện Dinh Dưỡng]
organization: [Tên tổ chức y tế]
section: [Tên chương / mục lớn]
---

2. NGUYÊN TẮC CẤU TRÚC ĐỂ THUẬT TOÁN CHUNKING HOẠT ĐỘNG HOÀN HẢO:
- Sử dụng tiêu đề phân cấp chuẩn: # cho tên tài liệu, ## cho từng chủ đề lớn, ### cho các tiểu mục cụ thể.
- Thuật toán Text Splitter được cấu hình cắt theo kích thước 900 ký tự. Do đó, MỖI TIỂU MỤC (###) NÊN CÓ ĐỘ DÀI TỪ 400 ĐẾN 800 KÝ TỰ (khoảng 1 - 3 đoạn văn súc tích).
- Không viết một đoạn văn liền tù tì quá 1500 ký tự mà không ngắt đoạn.
- Trình bày dạng danh sách gạch đầu dòng (-) rõ ràng cho các triệu chứng, số đo sinh hiệu và các bước xử trí.

3. NGUYÊN TẮC BẢO TOÀN DỮ LIỆU ĐỊNH LƯỢNG LÂM SÀNG:
- Giữ nguyên vẹn 100% các con số định lượng y khoa, ngưỡng nguy hiểm, đơn vị đo:
  + Huyết áp: mmHg (ví dụ: >= 140/90 mmHg, >= 160/110 mmHg).
  + Thân nhiệt: °C (ví dụ: >= 38.5°C).
  + Đường huyết: mmol/L hoặc mg/dL (ví dụ: lúc đói < 5.1 mmol/L).
  + Cử động thai: số lần / số giờ (ví dụ: >= 4 lần trong 2 giờ).
  + Vi chất dinh dưỡng: mcg, mg, IU (ví dụ: Axit Folic 400-600 mcg/ngày, Sắt 30-60 mg/ngày).

4. CẤU TRÚC 3 THÀNH PHẦN BẮT BUỘC TRONG TỪNG CHỦ ĐỀ BỆNH LÝ / TRIỆU CHỨNG:
Trong mỗi mục bệnh lý hoặc triệu chứng cảnh báo, phải trình bày đủ 3 ý:
- [Dấu hiệu & Triệu chứng nhận biết]: Các biểu hiện lâm sàng cụ thể.
- [Nguyên nhân & Nguy cơ sức khỏe]: Giải thích ngắn gọn cơ chế hoặc nguy cơ cho mẹ/bé.
- [Hướng xử trí & Cảnh báo đi viện]: Lời khuyên tự chăm sóc tại nhà và dấu hiệu bắt buộc phải đến cấp cứu tại Bệnh viện ngay lập tức.

5. LÀM SẠCH DỮ LIỆU (DATA CLEANING):
- Loại bỏ toàn bộ số trang thừa, header/footer của tài liệu gốc, các lỗi dính chữ do scan OCR, các điều khoản thủ tục hành chính rườm rà không liên quan đến chuyên môn y tế.
- Giọng văn khoa học, chuẩn xác, tiếng Việt trong sáng, ân cần.

DƯỚI ĐÂY LÀ NỘI DUNG TÀI LIỆU THÔ CẦN CHUẨN HÓA:
[DÁN NỘI DUNG FILE TÀI LIỆU THÔ HOẶC ĐÍNH KÈM FILE TẠI ĐÂY]
```

---

## 🎯 VÍ DỤ MINH HỌA MỘT FILE `.md` SAU KHI ĐƯỢC CHUẨN HÓA HOÀN HẢO:

```markdown
---
title: Hướng dẫn Dự phòng và Xử trí Đái tháo đường Thai kỳ
stage: PREGNANCY
topic: HEALTH_MONITORING
source: Hướng dẫn Quốc gia về Chăm sóc Sức khỏe Sinh sản - Bộ Y Tế
organization: Bộ Y Tế Việt Nam
section: Chương VI - Rối loạn Chuyển hóa và Bệnh lý Nội khoa Thai kỳ
---

# Hướng dẫn Dự phòng và Xử trí Đái tháo đường Thai kỳ

## 1. Tiêu chuẩn Chẩn đoán và Theo dõi Đường huyết

### Chỉ số đường huyết mục tiêu ở thai phụ:
- **Đường huyết lúc đói:** Duy trì từ 3.9 - 5.0 mmol/L (70 - 90 mg/dL).
- **Đường huyết 1 giờ sau ăn:** Dưới 7.8 mmol/L (140 mg/dL).
- **Đường huyết 2 giờ sau ăn:** Dưới 6.7 mmol/L (120 mg/dL).
- **HbA1c:** Duy trì dưới 6.0% trong suốt thai kỳ.

### Các dấu hiệu cảnh báo bất thường:
- Thường xuyên khát nước, uống nhiều nước và đi tiểu nhiều lần cả ngày lẫn đêm.
- Sụt cân không rõ nguyên nhân dù ăn uống bình thường hoặc ăn nhiều hơn.
- Vết thương ngoài da lâu lành, dễ bị viêm nhiễm nấm ngứa âm đạo tái diễn.
- Thai nhi phát triển to nhanh bất thường trên kết quả siêu âm định kỳ.

---

## 2. Chế độ Dinh dưỡng và Chăm sóc tại Nhà

### Nguyên tắc ăn uống khoa học:
- **Chia nhỏ bữa ăn:** Ăn từ 5 - 6 bữa/ngày (3 bữa chính và 2 - 3 bữa phụ) để tránh đường huyết tăng vọt sau ăn hoặc tụt đường huyết xa bữa ăn.
- **Lựa chọn tinh bột phức:** Thay cơm trắng, bánh mì trắng bằng gạo lứt, yến mạch, khoai lang có chỉ số đường huyết (GI) thấp.
- **Bổ sung chất xơ:** Tăng cường rau củ màu xanh đậm (bông cải, rau bina, mồng tơi) trong mỗi bữa ăn.
- **Hạn chế tuyệt đối:** Đường tinh luyện, nước ngọt có ga, bánh kẹo ngọt, trái cây sấy khô và trái cây quá ngọt (sầu riêng, mít, vải).

---

## 3. Khi nào Mẹ bầu cần Đến Bệnh viện Ngay?

### Các dấu hiệu nguy hiểm cần can thiệp y tế khẩn cấp:
- Đường huyết đo lúc đói liên tục $\ge 7.0$ mmol/L hoặc sau ăn 2 giờ $\ge 11.1$ mmol/L.
- Xuất hiện triệu chứng tụt đường huyết nặng: Vã mồ hôi lạnh, run tay chân, tim đập nhanh, hoa mắt chóng mặt, lú lẫn hoặc ngất xỉu.
- Có dấu hiệu nhiễm toan ceton: Buồn nôn, nôn mửa liên tục, hơi thở có mùi hoa quả chín/mùi táo thối, thở nhanh sâu.
- Mẹ bầu cần đến ngay Bệnh viện có khoa Nội tiết - Sản khoa để Bác sĩ chỉ định phác đồ điều trị và dùng Insulin kịp thời.
```

---

## 🛠️ HƯỚNG DẪN 3 BƯỚC THỰC HIỆN:

1. **Bước 1:** Copy toàn bộ **CÂU PROMPT MASTER** ở trên.
2. **Bước 2:** Mở ChatGPT / Claude / Gemini ➔ Dán câu prompt kèm theo nội dung file tài liệu y tế bạn muốn chuyển đổi.
3. **Bước 3:** Lấy kết quả Markdown AI trả về ➔ Lưu thành file `.md` (ví dụ: `05_dai_thao_duong_thai_ky.md`) ➔ Thả vào thư mục:
   `05_Development/CareBridgeAITriageService/data/raw_documents/`
4. **Bước 4:** Bấm nút `POST /api/v1/documents/sync-directory` trên Swagger UI **http://localhost:8001/docs** để tự động nạp tri thức mới vào hệ thống!
