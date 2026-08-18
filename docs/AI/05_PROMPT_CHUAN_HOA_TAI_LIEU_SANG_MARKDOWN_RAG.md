# PROMPT MASTER TOÀN NĂNG: CHUẨN HÓA MỌI THỂ LOẠI TÀI LIỆU Y TẾ THÔ SANG MARKDOWN TỐI ƯU CHO AI RAG

> **Mục đích:** Sử dụng câu Prompt dưới đây để yêu cầu AI (ChatGPT, Claude, Gemini...) tiếp nhận **BẤT KỲ THỂ LOẠI TÀI LIỆU Y KHOA NÀO** (PDF scan, Word, Sách cẩm nang, Hướng dẫn Bộ Y Tế, Phác đồ điều trị, Bài báo nghiên cứu khoa học, Bộ câu hỏi FAQ, Infographic/Checklist, Tờ rơi hướng dẫn dùng thuốc) và chuyển đổi sang định dạng Markdown (`.md`) chuẩn cấu trúc tối ưu $100\%$ cho hệ thống CareBridge AI RAG.

---

## 📋 CÂU PROMPT MASTER (COPY TOÀN BỘ KHUNG DƯỚI ĐÂY)

```text
Bạn là Chuyên gia Cao cấp về Kỹ thuật Tri thức Y tế & Tiền xử lý Dữ liệu RAG (Lead Medical Knowledge Engineer).
Nhiệm vụ của bạn là đọc toàn bộ nội dung tài liệu y khoa thô được cung cấp dưới đây, phân tích thể loại tài liệu, làm sạch tạp âm và chuyển đổi thành một file Markdown (.md) chuẩn mực nhất, bảo toàn 100% giá trị tri thức gốc và tối ưu hóa tuyệt đối cho hệ thống AI RAG (Retrieval-Augmented Generation) CareBridge.

======================================================================
HƯỚNG DẪN XỬ LÝ THEO TỪNG THỂ LOẠI TÀI LIỆU VÀ CẤU TRÚC ĐẶC THÙ:
======================================================================

1. NẾU LÀ BẢNG BIỂU PHỨC TẠP (TABLES / MERGED CELLS):
   - Không giữ nguyên bảng kẻ ô Markdown (| col1 | col2 |) nếu bảng có nhiều cột hoặc nội dung dài, vì khi thuật toán Chunking cắt theo độ dài ký tự sẽ làm gãy các hàng và mất tiêu đề cột.
   - Hãy LÀM PHẲNG BẢNG (Table Flattening) thành dạng danh sách có cấu trúc (Structured Key-Value Bullets). Mỗi dòng phải có đầy đủ chủ ngữ, đối tượng, tiêu chí và giá trị.
   - Ví dụ bảng liều lượng:
     * **Phụ nữ chuẩn bị mang thai:** Axit Folic 400 mcg/ngày, bắt đầu uống trước khi thụ thai tối thiểu 1 - 3 tháng.
     * **Phụ nữ mang thai:** Sắt nguyên tố 30 - 60 mg/ngày + Axit Folic 400 - 600 mcg/ngày trong suốt thai kỳ.

2. NẾU LÀ BÀI BÁO NGHIÊN CỨU KHOA HỌC (RESEARCH PAPERS / JOURNAL ARTICLES):
   - Loại bỏ phần phương pháp nghiên cứu thuần thống kê quá sâu (như cỡ mẫu n, phần mềm SPSS, phương sai p-value, t-test).
   - Tập trung chuyển hóa phần [Kết quả nghiên cứu chính (Results)] và [Khuyến nghị lâm sàng / Bàn luận (Discussion & Conclusion)] thành các luận điểm y khoa thực tiễn, dễ hiểu, có giá trị hướng dẫn người bệnh.

3. NẾU LÀ SƠ ĐỒ THUẬT TOÁN / CÂY QUYẾT ĐỊNH (FLOWCHARTS / DECISION TREES):
   - Chuyển sơ đồ rẽ nhánh thành các bước điều kiện logic dạng văn bản (If-Then Logic):
     * **Bước 1 (Đánh giá ban đầu):** Kiểm tra dấu hiệu sốt và đau bụng.
     * **Nếu có sốt cao >= 38.5°C kèm đau bụng quặn:** Hướng dẫn đến ngay cơ sở y tế gần nhất.
     * **Nếu sốt nhẹ < 38°C không đau bụng:** Hướng dẫn lau mát bằng nước ấm, uống nhiều nước và theo dõi tại nhà.

4. NẾU LÀ BỘ CÂU HỎI THƯỜNG GẶP (FAQ / Q&A):
   - Mỗi câu hỏi đặt làm một tiêu đề cấp 3 (### [Câu hỏi rõ ràng?]).
   - Phần trả lời ngay bên dưới súc tích, giải thích nguyên nhân và hướng xử trí cụ thể.

5. NẾU LÀ CHECKLIST / LỊCH KHÁM THEO MỐC TUẦN THAI / THÁNG TUỔI:
   - Gom nhóm theo từng mốc thời gian cụ thể (Ví dụ: `### Mốc 11 - 13 tuần 6 ngày`, `### Mốc 20 - 24 tuần`).
   - Liệt kê các xét nghiệm bắt buộc, siêu âm dị tật, vắc-xin cần tiêm và lời khuyên theo dõi tương ứng.

6. NẾU LÀ THÔNG TIN THUỐC, DƯỢC PHẨM & VI CHẤT (DRUGS & SUPPLEMENTS):
   - Giữ nguyên tên hoạt chất, biệt dược phổ biến, phân loại an toàn cho phụ nữ mang thai & cho con bú (nhóm A, B, C, D, X).
   - Trình bày rõ ràng: Chỉ định, Liều lượng khuyến cáo, Chống chỉ định, Tác dụng phụ thường gặp, và Lưu ý tương tác thuốc.

7. NẾU TÀI LIỆU CÓ TỪ VIẾT TẮT & THUẬT NGỮ CHUYÊN NGÀNH:
   - Giữ nguyên từ viết tắt chuẩn quốc tế kèm chú thích tiếng Việt đầy đủ trong ngoặc đơn (Ví dụ: *OGTT (Oral Glucose Tolerance Test - Nghiệm pháp dung nạp đường huyết đường uống)*, *Preeclampsia (Tiền sản giật)*, *PROM (Premature Rupture of Membranes - Vỡ ối non)*) để cả tìm kiếm ngữ nghĩa lẫn tìm kiếm từ khóa đều bắt trúng 100%.

======================================================================
5 QUY TẮC BẮT BUỘC VỀ ĐỊNH DẠNG ĐẦU RA (MARKDOWN OUTPUT):
======================================================================

QUY TẮC 1: ĐẦU FILE BẮT BUỘC CÓ YAML FRONTMATTER:
---
title: [Tiêu đề tài liệu rõ ràng, tiếng Việt có dấu]
stage: [Chọn 1 trong: PRECONCEPTION | PREGNANCY | POSTPARTUM | BABY_CARE | ALL]
topic: [Chọn 1 trong: NUTRITION | HEALTH_MONITORING | DANGER_SIGNS | POSTPARTUM_CARE | BABY_CARE | MEDICATION | FAQ | GENERAL]
document_type: [Chọn 1 trong: GUIDELINE | RESEARCH | HANDBOOK | FAQ | CHECKLIST | DRUG_INFO]
source: [Tên nguồn/tác giả/tài liệu gốc, ví dụ: Hướng dẫn Quốc gia Bộ Y Tế / Tài liệu CDC / Bệnh viện Từ Dũ]
organization: [Tên tổ chức phát hành, ví dụ: Bộ Y Tế Việt Nam / WHO / ACOG / UNICEF]
section: [Tên chương hoặc phần lớn của tài liệu]
---

QUY TẮC 2: PHÂN CẤP TIÊU ĐỀ CHUẨN SEMANTIC CHUNKING:
- # [Tiêu đề tài liệu]
- ## [Chương / Phần / Chủ đề lớn]
- ### [Tiểu mục chi tiết / Từng vấn đề cụ thể]
- Độ dài lý tưởng cho mỗi tiểu mục (###) là từ 400 đến 900 ký tự (khoảng 1 - 3 đoạn văn). Nếu một mục trong tài liệu gốc quá dài, hãy chủ động tách thành các ### phụ để Vector Chunker không cắt gãy đoạn giữa chừng.

QUY TẮC 3: BẢO TOÀN TRỌN VẸN 100% TRI THỨC VÀ SỐ LIỆU:
- Giữ nguyên vẹn mọi con số, ngưỡng nguy hiểm, đơn vị đo (mmHg, mg/dL, mmol/L, °C, count/session, mcg, IU...).
- Giữ trọn vẹn các cảnh báo an toàn y tế và dấu hiệu bắt buộc phải đến Bệnh viện.
- Tuyệt đối KHÔNG tự ý tóm tắt quá ngắn làm mất thông tin quan trọng.
- Tuyệt đối KHÔNG tự ý bịa đặt nội dung không có trong tài liệu gốc.

QUY TẮC 4: LỌC BỎ HOÀN TOÀN TẠP ÂM VĂN BẢN (NOISE REMOVAL):
- Xóa số trang, header, footer lặp lại trên các trang scan/PDF.
- Xóa các căn cứ hành chính thủ tục (Căn cứ luật số..., Nơi nhận:...).
- Xóa danh mục tài liệu tham khảo đánh số [1], [2], [3] ở cuối bài.
- Sửa lỗi chính tả, dính chữ do phần mềm quét OCR.

QUY TẮC 5: NGÔN NGỮ & VĂN PHONG:
- Tiếng Việt chuẩn mực, trong sáng, diễn đạt chuẩn y khoa nhưng ân cần, dễ tiếp thu cho mẹ bầu và gia đình.

----------------------------------------------------------------------
DƯỚI ĐÂY LÀ NỘI DUNG TÀI LIỆU Y KHOA THÔ CẦN CHUẨN HÓA:
[DÁN NỘI DUNG FILE TÀI LIỆU THÔ HOẶC ĐÍNH KÈM FILE TẠI ĐÂY]
```

---

## 🎯 VÍ DỤ MINH HỌA XỬ LÝ CÁC TÀI LIỆU PHỨC TẠP THỰC TẾ

### 1. Ví dụ Xử lý Bảng biểu Dinh dưỡng & Mốc Tiêm chủng (Làm phẳng bảng):
```markdown
---
title: Lịch Tiêm chủng và Bổ sung Vi chất Thiết yếu cho Phụ nữ Mang thai
stage: PREGNANCY
topic: NUTRITION
document_type: GUIDELINE
source: Hướng dẫn Tiêm chủng Quốc gia - Bộ Y Tế
organization: Bộ Y Tế Việt Nam
section: Dự phòng Nhiễm trùng và Dinh dưỡng Tiền sản
---

# Lịch Tiêm chủng và Bổ sung Vi chất Thiết yếu cho Phụ nữ Mang thai

## 1. Lịch Tiêm Vắc-xin Uốn ván (VAT)

### Hướng dẫn các mũi tiêm uốn ván cho thai phụ:
- **Mang thai lần đầu (Chưa từng tiêm VAT):** Tiêm đủ 2 mũi. Mũi 1 tiêm vào tam cá nguyệt thứ 2 (từ tuần 20 trở đi); Mũi 2 tiêm sau mũi 1 tối thiểu 30 ngày và phải trước ngày dự sinh ít nhất 30 ngày.
- **Mang thai lần 2 (Đã tiêm đủ 2 mũi ở lần trước trong vòng 5 năm):** Tiêm nhắc lại 1 mũi vào tam cá nguyệt thứ 2, trước ngày dự sinh ít nhất 30 ngày.
- **Trường hợp đã tiêm đủ 5 mũi trước đây:** Không cần tiêm thêm nếu mũi cuối cùng cách dưới 10 năm; nếu trên 10 năm tiêm nhắc lại 1 mũi.
```

### 2. Ví dụ Xử lý Bộ Câu hỏi FAQ & Dấu hiệu Khẩn cấp:
```markdown
---
title: Giải đáp Thắc mắc về Theo dõi Cử động Thai và Dấu hiệu Bất thường
stage: PREGNANCY
topic: DANGER_SIGNS
document_type: FAQ
source: Cẩm nang Sức khỏe Sinh sản - Bệnh viện Từ Dũ
organization: Bệnh viện Từ Dũ
section: Theo dõi Thai máy và An toàn Thai kỳ
---

# Giải đáp Thắc mắc về Theo dõi Cử động Thai và Dấu hiệu Bất thường

## 1. Hướng dẫn Đếm Cử động Thai tại Nhà

### Thai máy như thế nào là bình thường?
- Từ tuần thứ 28 của thai kỳ, mẹ nên đếm cử động thai mỗi ngày sau các bữa ăn chính (sáng, trưa, tối).
- Mẹ nằm nghiêng sang trái, thư giãn và đếm số lần bé đạp hoặc xoay trở trong vòng 1 đến 2 giờ.
- **Tiêu chuẩn an toàn:** Thai nhi khỏe mạnh thường có ít nhất **4 lần cử động trong 1 giờ** hoặc **ít nhất 10 lần cử động trong 2 giờ**.

### Khi nào thai máy bất thường cần đi cấp cứu ngay?
- Trong 2 giờ liên tục thai cử động dưới 4 lần dù mẹ đã ăn nhẹ, uống nước ngọt hoặc đổi tư thế.
- Thai nhi đột ngột ngưng máy hoàn toàn hoặc đạp yếu ớt bất thường so với mọi ngày.
- Mẹ cần đến ngay phòng cấp cứu sản khoa để được đo tim thai (Monitor Sản khoa / NST) và siêu âm Doppler mạch máu thai nhi kịp thời.
```

---

## 🛠️ QUY TRÌNH 4 BƯỚC NẠP DỮ LIỆU VÀO CAREBRIDGE:

1. **Bước 1:** Copy toàn bộ **CÂU PROMPT MASTER** ở trên.
2. **Bước 2:** Mở ChatGPT / Claude / Gemini ➔ Dán prompt kèm nội dung file PDF/Word/Nghiên cứu.
3. **Bước 3:** Lưu kết quả Markdown trả về thành file `.md` trong thư mục:
   `05_Development/CareBridgeAITriageService/data/raw_docs/`
4. **Bước 4:** Nạp vào PostgreSQL pgvector bằng lệnh:
   ```bash
   cd 05_Development/CareBridgeAITriageService
   ./venv/bin/python scripts/ingest_documents.py
   ```


