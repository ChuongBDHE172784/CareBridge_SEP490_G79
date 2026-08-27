# CareBridge AI Maternal RAG & Health Metrics Screening Service

Dịch vụ AI RAG & Sàng lọc Chỉ số Sức khỏe Mẹ Bầu xây dựng bằng **Python 3.11+ / FastAPI**, tích hợp **Google Gemini 3.7 Flash**, **Gemini Embedding (768 chiều)** và cơ sở dữ liệu vector **PostgreSQL + pgvector**.

Hệ thống được thiết kế bám sát 100% theo quy trình hệ thống tại `docs/mainworkflow-Trang-3.drawio.png`.

---

## 1. Kiến trúc & Nguyên tắc Y tế An toàn (Clinical Safety)

```
                                  [ Dữ liệu Cẩm nang Y tế ]
                                  (PDF, DOCX, Markdown, TXT)
                                             │
                                             ▼
                               [ Document Ingestion Pipeline ]
                               • Recursive Character Text Splitter
                               • Gemini Embedding (768 dims)
                                             │
                                             ▼
                              [(PostgreSQL + pgvector Table)]
                                 maternal_knowledge_chunks
                                             │
                                             │ (Vector Search Cosine <=>)
                                             ▼
 ┌─────────────────────────────┐    ┌───────────────────────────────┐
 │   Chỉ số Sức khỏe Mẹ Bầu    │    │     Mẹ Bầu Hỏi đáp / Chat     │
 │   (Bước 7: Health Metrics)  │    │   (Bước 10: AI Nurse Assistant│
 └──────────────┬──────────────┘    └───────────────┬───────────────┘
                │                                   │
                ▼                                   ▼
 ┌─────────────────────────────┐    ┌───────────────────────────────┐
 │  Metrics Screening Service  │    │      RAG Chat Service         │
 │  • Ngưỡng y khoa sinh lý    │    │  • Semantic Vector Retrieval  │
 │  • RAG đối chiếu Danger Sign│    │  • Metadata Filter theo Stage │
 └──────────────┬──────────────┘    │  • Gemini 3.7 Flash Reasoning │
                │                   │  • Guardrail: Non-Diagnostic  │
        ┌───────┴───────┐           └───────────────┬───────────────┘
        ▼               ▼                           ▼
  [CRITICAL_RED]  [ANOMALY / NORMAL]    [Câu trả lời cẩm nang y khoa]
  • Kích hoạt SOS • Chuyển sang Bước 10 • Trích dẫn nguồn (Citations)
  • Gọi 115       • Tiếp tục theo dõi   • Cảnh báo khi nào cần gặp Bác sĩ
```

1. **Non-Diagnostic Principle (Không tự chẩn đoán y khoa):** AI đóng vai trò **Trợ lý Điều dưỡng Y tế (AI Nurse Assistant)**. AI không tự khẳng định bệnh, không tự kê đơn/kê thuốc, giải đáp cặn kẽ dựa trên cẩm nang y tế được trích xuất (Strict Grounding) và luôn kèm Disclaimer pháp lý.
2. **Khớp với Workflow Dự án:**
   - **Bước 7 ➔ 8 ➔ 9:** Nhận chỉ số sinh hiệu (Huyết áp, Thân nhiệt, Đường huyết, Cử động thai, Tuần thai). Tự động kiểm tra ngưỡng nguy hiểm và tra cứu RAG cẩm nang để phân loại `NORMAL`, `ANOMALY_MONITOR` (chuyển sang Chat) hoặc `CRITICAL_EMERGENCY` (kích hoạt SOS, gọi 115, chỉ đường tới bệnh viện gần nhất).
   - **Bước 10 (AI Nurse Assistant RAG Chat):** Trò chuyện, giải đáp thắc mắc, hướng dẫn mẹ bầu chăm sóc thai kỳ dựa trên tài liệu y tế chính thống (Bộ Y Tế, WHO, Vinmec), có trích dẫn nguồn.

---

## 2. Hướng dẫn Cài đặt & Khởi chạy

### Bước 1: Chuẩn bị môi trường & Cài đặt thư viện
```bash
cd 05_Development/CareBridgeAITriageService
python3 -m venv venv
source venv/bin/activate  # Trên Windows: .\venv\Scripts\activate
pip install -r requirements.txt
```

### Bước 2: Cấu hình biến môi trường (`.env`)
Tạo file `.env` từ `.env.example`:
```dotenv
GEMINI_API_KEY=YOUR_GEMINI_API_KEY
GEMINI_MODEL=gemini-3.7-flash
GEMINI_EMBEDDING_MODEL=gemini-embedding-001
GEMINI_ENABLED=true

# Kết nối PostgreSQL pgvector (tương thích Supabase / Docker PostgreSQL)
DATABASE_URL=postgresql+asyncpg://carebridge:carebridge@localhost:5433/carebridge
AI_TRIAGE_INTERNAL_API_KEY=carebridge
```

### Bước 3: Khởi tạo CSDL pgvector & Nạp dữ liệu ban đầu
```bash
# 1. Tạo extension pgvector và bảng maternal_knowledge_chunks kèm chỉ mục HNSW
python scripts/init_pgvector_db.py

# 2. Tự động chunking và nạp toàn bộ cẩm nang y tế trong thư mục data/raw_documents/
python scripts/ingest_documents.py
```

### Bước 4: Chạy server FastAPI
```bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

Swagger UI tài liệu API xem tại: `http://localhost:8001/docs`

---

## 3. Hướng dẫn Đưa Thêm File Tài Liệu để AI Chunk & Học Thêm Tri Thức

Khi bạn muốn bổ sung thêm sách cẩm nang, tài liệu hướng dẫn của Bác sĩ hoặc Bộ Y Tế, bạn có thể thực hiện theo **2 cách cực kỳ đơn giản**:

### Cách 1: Thả file vào thư mục và chạy Script CLI (Khuyên dùng)
1. Thả file tài liệu của bạn (hỗ trợ các định dạng: **`.pdf`**, **`.docx`**, **`.md`**, **`.txt`**) vào thư mục:
   ```text
   05_Development/CareBridgeAITriageService/data/raw_documents/
   ```
2. Chạy lệnh:
   ```bash
   python scripts/ingest_documents.py
   ```
3. Hệ thống sẽ tự động:
   - Đọc văn bản từ file.
   - Chia nhỏ văn bản thông minh bằng `RecursiveCharacterTextSplitter` (chunk size ~900 ký tự, overlap ~180 ký tự để giữ trọn vẹn ngữ cảnh y khoa).
   - Gọi Gemini Embedding để tạo vector 768 chiều.
   - Lưu vào bảng `maternal_knowledge_chunks` trong PostgreSQL.
   - Ngay lập tức AI có thể trả lời các câu hỏi dựa trên tài liệu mới này!

### Cách 2: Tải file trực tiếp qua REST API (Dành cho Web Admin / Frontend)
Gửi request `POST /api/v1/documents/upload` (dạng `multipart/form-data`):
```bash
curl -X POST "http://localhost:8001/api/v1/documents/upload" \
  -H "X-Internal-API-Key: carebridge" \
  -F "file=@/path/to/tai_lieu_tiem_phong.pdf" \
  -F "stage=PREGNANCY" \
  -F "topic=VACCINATION" \
  -F "source=Bộ Y Tế Việt Nam"
```

---

## 4. Danh mục REST API Chi tiết

### 1. Sàng lọc Chỉ số Sức khỏe & Dấu hiệu Nguy hiểm (Bước 7 ➔ 8 ➔ 9)
- **Endpoint:** `POST /api/v1/metrics/evaluate`
- **Headers:** `X-Internal-API-Key: carebridge`
- **Request Body mẫu (Phát hiện Tiền sản giật nặng - CRITICAL):**
```json
{
  "stage": "PREGNANCY",
  "gestational_age_weeks": 32,
  "systolic_bp": 165,
  "diastolic_bp": 110,
  "temperature": 37.0,
  "symptoms": ["Đau đầu dữ dội", "Hoa mắt nhìn mờ", "Phù mặt"]
}
```
- **Response mẫu:**
```json
{
  "status": "CRITICAL_EMERGENCY",
  "emergency_mode": true,
  "headline": "CẢNH BÁO: Phát hiện chỉ số / dấu hiệu nguy hiểm khẩn cấp!",
  "summary": "Chỉ số sinh hiệu hoặc triệu chứng của mẹ đang ở ngưỡng báo động cao...",
  "risk_factors": [
    "Huyết áp rất cao (165/110 mmHg) - Nguy cơ Tiền sản giật nặng / Đột quỵ thai kỳ"
  ],
  "suggested_action": "KÍCH HOẠT CHẾ ĐỘ CẤP CỨU: Gọi 115 hoặc di chuyển ngay đến Bệnh viện chuyên khoa Sản gần nhất.",
  "relevant_sources": [
    {
      "title": "Dấu hiệu Cảnh báo Nguy hiểm trong Thai kỳ và Xử trí Cấp cứu",
      "source": "Hướng dẫn Quốc gia về Chăm sóc Sức khỏe Sinh sản - Bộ Y Tế & WHO",
      "section": "Chương IV - Cảnh báo Cấp cứu Sản khoa"
    }
  ],
  "disclaimer": "Lưu ý: Thông tin do AI cung cấp chỉ mang tính chất tham khảo..."
}
```

---

### 2. AI Nurse Assistant RAG Chat (Bước 10)
- **Endpoint:** `POST /api/v1/chat/message`
- **Headers:** `X-Internal-API-Key: carebridge`
- **Request Body mẫu:**
```json
{
  "message": "Mang thai 3 tháng đầu em nên bổ sung sắt và axit folic như thế nào cho đúng?",
  "stage": "PREGNANCY",
  "gestational_age_weeks": 10
}
```
- **Response mẫu:**
```json
{
  "answer": "Chào mẹ, CareBridge AI Nurse Assistant xin được chia sẻ cùng mẹ:\n\nTrong 3 tháng đầu thai kỳ, việc bổ sung Sắt và Axit Folic là vô cùng quan trọng...\n\n- Axit Folic: 400 - 600 mcg/ngày giúp phòng ngừa dị tật ống thần kinh.\n- Sắt: 30 - 60 mg/ngày phòng chống thiếu máu, nên uống khi bụng đói cùng nước cam...",
  "has_critical_warning": false,
  "suggested_followups": [
    "Mang thai nên kiêng những thực phẩm nào?",
    "Cách bổ sung canxi và sắt đúng cách?",
    "Lịch tiêm phòng uốn ván cho mẹ bầu?"
  ],
  "sources": [
    {
      "title": "Hướng dẫn Dinh dưỡng, Vi chất và Tiêm chủng cho Phụ nữ Mang thai",
      "source": "Viện Dinh Dưỡng Quốc Gia & Cục Y Tế Dự Phòng - Bộ Y Tế"
    }
  ],
  "disclaimer": "Lưu ý: Thông tin do AI cung cấp chỉ mang tính chất tham khảo..."
}
```

---

## 5. Chạy Kiểm thử (Test Suite)

```bash
pytest tests/ -v
```
Toàn bộ 14 test cases kiểm thử các tình huống lâm sàng (Bình thường, Tiền sản giật, Sốt cao, Mất cử động thai, RAG Chat, Ingestion file) đều đã vượt qua kiểm thử thành công.
