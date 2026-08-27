# TỔNG HỢP TOÀN BỘ TÍNH NĂNG, MỤC ĐÍCH VÀ HƯỚNG DẪN SỬ DỤNG PHÂN HỆ AI RAG & SÀNG LỌC SINH HIỆU CAREBRIDGE

> **Dự án:** CareBridge (SEP490 - Capstone Project)  
> **Dịch vụ:** `CareBridgeAITriageService` (FastAPI / Gemini / pgvector)  
> **Giao diện thao tác trực tiếp:** `http://localhost:8001/docs` (Swagger UI)

---

## 📑 MỤC LỤC TÍNH NĂNG

| Nhóm Tính năng | Danh sách API Endpoint | Mục đích chính |
| :--- | :--- | :--- |
| **1. Sàng lọc Sinh hiệu & Lâm sàng** | `POST /api/v1/metrics/evaluate`<br/>`POST /api/v1/metrics/simulate-batch` | Sàng lọc chỉ số sức khỏe mẹ bầu (Bước 7 ➔ 8 ➔ 9), phát hiện Tiền sản giật & Cấp cứu SOS 115; Mô phỏng 5 ca lâm sàng mẫu demo Hội đồng. |
| **2. AI Nurse RAG Chat & Prompt** | `POST /api/v1/chat/message`<br/>`POST /api/v1/chat/test-prompt`<br/>`GET /api/v1/chat/models` | Trợ lý Điều dưỡng giải đáp cẩm nang y tế (Bước 10); Thử nghiệm prompt tùy biến (Prompt Playground); Quản lý model AI. |
| **3. Quản trị Tri thức & Soi Vector** | `POST /api/v1/documents/upload`<br/>`POST /api/v1/documents/ingest-text`<br/>`POST /api/v1/documents/sync-directory`<br/>`GET /api/v1/documents/stats`<br/>`GET /api/v1/documents/list`<br/>`POST /api/v1/documents/search-vector`<br/>`GET /api/v1/documents/files`<br/>`DELETE /api/v1/documents/by-title`<br/>`DELETE /api/v1/documents/clear-all` | Nạp file cẩm nang mới (PDF/Word/MD/TXT); Thống kê tổng số chunks; Soi điểm tương đồng Vector (Simulator); Xóa tri thức cũ/sai lệch. |
| **4. Giám sát & Tương thích Ngược** | `GET /health`<br/>`POST /internal/triage/turn` | Giám sát trạng thái CSDL pgvector & Gemini; Tương thích ngược với backend Java Spring Boot `CareBridgeAPI`. |

---

## 🩺 NHÓM 1: SÀNG LỌC SINH HIỆU & MÔ PHỎNG LÂM SÀNG

### 1. `POST /api/v1/metrics/evaluate` — Đánh giá Chỉ số Sức khỏe & Phát hiện Nguy hiểm
* **Mục đích:** Thực hiện luồng **Bước 7 ➔ 8 ➔ 9** trong workflow hệ thống. Tiếp nhận các chỉ số sinh hiệu đo tại nhà của thai phụ, tự động đối chiếu ngưỡng nguy hiểm lâm sàng và tra cứu cẩm nang y tế để phân loại rủi ro.
* **Các mức rủi ro phân loại:**
  - **`CRITICAL_EMERGENCY` (Báo động đỏ):** Huyết áp $\ge 140/90$ kèm đau đầu/nhìn mờ (Tiền sản giật nặng), sốt cao $\ge 38.5^\circ C$, thai ngừng máy $\ge 2$ giờ ở tuần $\ge 28$, ra máu tươi... ➔ Trả về `emergency_mode: true` để Mobile App bật còi báo động, gọi cấp cứu 115 và chỉ đường tới Bệnh viện Phụ Sản gần nhất.
  - **`ANOMALY_MONITOR` (Bất thường nhẹ / cần theo dõi):** Tiền tăng huyết áp ($130-139/85-89$), ốm nghén nhiều, mệt mỏi, phù nhẹ hai chân cuối ngày... ➔ Khuyên mẹ nghỉ ngơi và gợi ý chuyển sang Bước 10 để trao đổi với AI Nurse.
  - **`NORMAL` (Bình thường):** Mọi chỉ số nằm trong khoảng sinh lý an toàn.
* **Cách sử dụng trên Swagger UI:**
  1. Mở Swagger: `http://localhost:8001/docs` ➔ Chọn `POST /api/v1/metrics/evaluate`.
  2. Bấm **Try it out** và dán dữ liệu mẫu:
     ```json
     {
       "stage": "PREGNANCY",
       "gestational_age_weeks": 32,
       "systolic_bp": 165,
       "diastolic_bp": 110,
       "temperature": 37.0,
       "symptoms": ["Đau đầu dữ dội", "Hoa mắt nhìn mờ"]
     }
     ```
  3. Bấm **Execute** để xem kết quả đánh giá lâm sàng.

---

### 2. `POST /api/v1/metrics/simulate-batch` — Mô phỏng 5 Ca Lâm sàng Mẫu Demo Hội Đồng
* **Mục đích:** Phục vụ **thuyết trình và bảo vệ đồ án**. Tự động chạy hàng loạt 5 ca bệnh kinh điển có sẵn trong y văn để chứng minh độ chính xác 100% của hệ thống:
  - *Ca 1:* Tiền sản giật nặng ($SBP = 165, DBP = 110$, đau đầu, nhìn mờ).
  - *Ca 2:* Sốt cao thai kỳ ($Temp = 39.2^\circ C$).
  - *Ca 3:* Mất cử động thai ở tuần 34 ($0$ lần đạp trong 2 giờ).
  - *Ca 4:* Chỉ số hoàn toàn bình thường ($SBP = 118, DBP = 76, Temp = 36.6$).
  - *Ca 5:* Bất thường nhẹ cần theo dõi ($SBP = 135, DBP = 88$).
* **Cách sử dụng trên Swagger UI:**
  - Chọn `POST /api/v1/metrics/simulate-batch` ➔ Bấm **Try it out** ➔ Bấm **Execute**.
  - Hệ thống trả về bảng kết quả `passed: true` cho toàn bộ 5 ca.

---

## 💬 NHÓM 2: AI NURSE ASSISTANT RAG CHAT & PROMPT PLAYGROUND

### 3. `POST /api/v1/chat/message` — Hỏi đáp Trợ lý Điều dưỡng Y tế (Bước 10)
* **Mục đích:** Thực hiện luồng **Bước 10**. Giải đáp mọi thắc mắc của mẹ bầu về dinh dưỡng, vi chất, tiêm chủng, tư thế nằm, dấu hiệu sinh lý dựa trên cẩm nang y tế chính thống đã được đánh vector.
* **Nguyên tắc an toàn (Guardrails):**
  - Không tự chẩn đoán bệnh ("Mẹ bị bệnh X").
  - Không kê đơn/kê thuốc điều trị.
  - Luôn trích dẫn nguồn cẩm nang (`sources`) và kèm gợi ý 3 câu hỏi tiếp theo (`suggested_followups`).
* **Cách sử dụng trên Swagger UI:**
  - Chọn `POST /api/v1/chat/message` ➔ Bấm **Try it out** ➔ Dán câu hỏi:
    ```json
    {
      "message": "Mang thai 3 tháng đầu em nên bổ sung sắt và axit folic như thế nào?",
      "stage": "PREGNANCY",
      "gestational_age_weeks": 10
    }
    ```
  - Bấm **Execute** để nhận câu trả lời ân cần, khoa học từ Gemini Flash.

---

### 4. `POST /api/v1/chat/test-prompt` — Prompt Playground (Thử nghiệm Prompt Tùy chỉnh)
* **Mục đích:** Dành cho quản trị viên/nhà phát triển muốn thử nghiệm các câu System Prompt khác nhau, tinh chỉnh độ sáng tạo (`temperature` từ `0.0` đến `1.0`) hoặc thử nghiệm các model Gemini khác nhau xem phản hồi thay đổi ra sao.
* **Cách sử dụng trên Swagger UI:**
  - Chọn `POST /api/v1/chat/test-prompt` ➔ Bấm **Try it out** ➔ Nhập `user_message`, `system_instruction` tùy chỉnh ➔ Bấm **Execute**.

---

### 5. `GET /api/v1/chat/models` — Quản lý Danh sách Model AI
* **Mục đích:** Xem model chính đang hoạt động (`gemini-flash-lite-latest`), model embedding (`gemini-embedding-001`), số chiều vector (`768`) và chuỗi model tự động dự phòng (`gemini-2.5-flash`...).
* **Cách sử dụng:** Bấm **Try it out** ➔ **Execute**.

---

## 📚 NHÓM 3: QUẢN TRỊ TRI THỨC Y TẾ & SOI VECTOR DATABASE

### 6. `POST /api/v1/documents/upload` — Nạp File Cẩm nang Y tế Mới
* **Mục đích:** Tải lên tài liệu mới định dạng **PDF, Word (.docx), Markdown (.md), Text (.txt)**. Hệ thống sẽ tự động cắt nhỏ đoạn (chunk size 900 ký tự, overlap 180 ký tự), gọi embedding và lưu vĩnh viễn vào PostgreSQL `pgvector`.
* **Cách sử dụng:**
  - Chọn `POST /api/v1/documents/upload` ➔ Bấm **Try it out**.
  - Bấm **Choose File** để chọn file trên máy tính ➔ Chọn `stage` (ví dụ `PREGNANCY`) ➔ Bấm **Execute**.

---

### 7. `POST /api/v1/documents/search-vector` — Vector Search Simulator (Soi Điểm Tương đồng)
* **Mục đích:** Giúp bạn nhìn thấy trực quan cách CSDL Vector hoạt động: Khi người dùng gõ một câu, thuật toán Cosine (`<=>`) sẽ tìm ra những đoạn cẩm nang nào và chấm điểm tương đồng (`similarity_score`) là bao nhiêu trước khi gửi cho LLM.
* **Cách sử dụng:**
  - Chọn `POST /api/v1/documents/search-vector` ➔ Bấm **Try it out** ➔ Nhập:
    ```json
    {
      "query": "mẹ bị phù chân và nhìn mờ",
      "stage": "PREGNANCY",
      "top_k": 4
    }
    ```
  - Bấm **Execute** để xem Top 4 đoạn cẩm nang trích xuất kèm điểm số tương đồng (ví dụ: `0.83` = 83%).

---

### 8. `GET /api/v1/documents/stats` — Thống kê Tổng quan Cơ sở Tri thức
* **Mục đích:** Xem bức tranh tổng thể: Tổng số chunks tri thức đang có trong CSDL, phân bố theo giai đoạn (`PREGNANCY`, `POSTPARTUM`...), phân bố theo chủ đề (`DANGER_SIGNS`, `NUTRITION`...) và danh sách file gốc.
* **Cách sử dụng:** Bấm **Try it out** ➔ **Execute**.

---

### 9. `GET /api/v1/documents/list` — Tra cứu & Phân trang Chunks Tri thức
* **Mục đích:** Duyệt danh sách các đoạn cẩm nang trong CSDL, có hỗ trợ phân trang (`page`, `page_size`), lọc theo `stage`, `topic` hoặc tìm kiếm từ khóa cụ thể.
* **Cách sử dụng:** Bấm **Try it out** ➔ Có thể nhập từ khóa cần tìm vào ô `keyword` ➔ Bấm **Execute**.

---

### 10. `GET /api/v1/documents/files` — Quản lý File Vật lý trên Đĩa Cứng
* **Mục đích:** Xem danh sách các file tài liệu gốc đang lưu trong thư mục `data/raw_documents/` kèm dung lượng KB và định dạng.
* **Cách sử dụng:** Bấm **Try it out** ➔ **Execute**.

---

### 11. `DELETE /api/v1/documents/by-title` — Xóa Tri thức của 1 Tài liệu Cũ / Sai Lệch
* **Mục đích:** Khi một tài liệu y tế bị cũ hoặc thông tin không còn chính xác, bạn chỉ cần nhập tên tài liệu. Hệ thống sẽ tự động xóa sạch toàn bộ các chunks của tài liệu đó trong PostgreSQL `pgvector` và xóa luôn file vật lý trên đĩa cứng để AI không dùng nữa.
* **Cách sử dụng:**
  - Chọn `DELETE /api/v1/documents/by-title` ➔ Bấm **Try it out** ➔ Nhập tên file hoặc tiêu đề cần xóa (Ví dụ: `01_dau_hieu_nguy_hiem` hoặc `cam_nang_cu.pdf`) ➔ Bấm **Execute**.

---

### 12. `DELETE /api/v1/documents/clear-all` — Dọn dẹp Sạch Toàn bộ CSDL Vector
* **Mục đích:** Xóa trắng toàn bộ tri thức trong Vector Database để chuẩn bị nạp lại một bộ tài liệu hoàn toàn mới từ đầu.
* **Cách sử dụng:** Chọn `DELETE /api/v1/documents/clear-all` ➔ Bấm **Execute**.

---

## ⚙️ NHÓM 4: GIÁM SÁT HỆ THỐNG & TƯƠNG THÍCH NGƯỢC

### 13. `GET /health` — Kiểm tra Trạng thái Hệ thống
* **Mục đích:** Trả về trạng thái hoạt động của Service (`UP`), tên model đang dùng, trạng thái kết nối tới PostgreSQL `pgvector` và số lượng chunks tri thức hiện có.

---

### 14. `POST /internal/triage/turn` — Cầu nối Tương thích Ngược cho Java Spring Boot
* **Mục đích:** Tiếp nhận request từ backend Java `CareBridgeAPI` (thông qua class `HttpTriageWorkflowClient.java`), tự động kích hoạt bộ RAG & Sàng lọc sinh hiệu mới và trả về kết quả định dạng chuẩn cho Spring Boot mà không làm đứt gãy kết nối cũ.
