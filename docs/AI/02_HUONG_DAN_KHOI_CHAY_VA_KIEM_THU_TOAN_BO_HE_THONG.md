# HƯỚNG DẪN KHỞI CHẠY & KIỂM THỬ TOÀN BỘ HỆ THỐNG CAREBRIDGE

> **Dự án:** CareBridge (SEP490 - Capstone Project)  
> **Phân hệ bao gồm:** PostgreSQL Database, AI RAG Service (FastAPI), Main Backend (Spring Boot), Web App (React Vite), Mobile App (Flutter).

---

## 🗺️ TỔNG QUAN KIẾN TRÚC VẬN HÀNH

```
                     ┌─────────────────────────────────────────┐
                     │          PostgreSQL Database            │
                     │          (Port 5433 hoặc 5432)          │
                     └────────────┬───────────────┬────────────┘
                                  │               │
                 ┌────────────────┴────┐     ┌────┴────────────────┐
                 │ CareBridgeAITriage  │     │   CareBridgeAPI     │
                 │ (Python AI Service) │◄───►│ (Java Spring Boot)  │
                 │     (Port 8001)     │     │     (Port 8080)     │
                 └─────────┬───────────┘     └────┬───────────┬────┘
                           │                      │           │
                           │                      │           │
                     [Swagger Docs]               ▼           ▼
                   (http://localhost:8001)   [CareBridgeWeb] [CareBridgeMobile]
                                              (React Vite)      (Flutter)
                                              (Port 5173)
```

---

## 📌 BƯỚC 1: Khởi động Cơ sở Dữ liệu PostgreSQL

Mở **Terminal 1** và chạy Docker Compose để bật PostgreSQL (có hỗ trợ extension `pgvector`):
```bash
cd "05_Development/CareBridgeAPI"
docker compose up -d
```
*(Nếu bạn dùng PostgreSQL cài trực tiếp trên máy macOS/Windows hoặc Supabase thì chỉ cần đảm bảo dịch vụ CSDL đang bật).*

---

## 📌 BƯỚC 2: Thêm File Dữ liệu & Khởi chạy Python AI RAG Service

### 2.1. Thêm tài liệu mới (Nếu muốn)
Thả các file cẩm nang (`.pdf`, `.docx`, `.md`, `.txt`) vào thư mục:
```text
05_Development/CareBridgeAITriageService/data/raw_documents/
```

### 2.2. Khởi tạo CSDL Vector & Nạp tri thức
Mở **Terminal 2**:
```bash
cd "05_Development/CareBridgeAITriageService"

# 1. Kích hoạt extension pgvector và tạo bảng maternal_knowledge_chunks kèm chỉ mục HNSW
./venv/bin/python scripts/init_pgvector_db.py

# 2. Tự động cắt đoạn (Chunking), tạo Vector Embedding 768 chiều và lưu vào CSDL
./venv/bin/python scripts/ingest_documents.py

# 3. Chạy Server FastAPI AI Service
./venv/bin/uvicorn app.main:app --reload --port 8001
```
* **Server AI hoạt động tại:** `http://localhost:8001`
* **Giao diện Swagger UI tương tác trực tiếp:** `http://localhost:8001/docs`

---

## 📌 BƯỚC 3: Khởi chạy Backend Java Spring Boot

Mở **Terminal 3**:
```bash
cd "05_Development/CareBridgeAPI"

# Khởi chạy Spring Boot
./mvnw spring-boot:run
```
* **Backend Spring Boot hoạt động tại:** `http://localhost:8080`
* **Swagger UI Spring Boot:** `http://localhost:8080/swagger-ui.html`

---

## 📌 BƯỚC 4: Khởi chạy Web App (React + TypeScript + Vite)

Mở **Terminal 4**:
```bash
cd "05_Development/CareBridgeWebApp"

# Cài đặt thư viện (chỉ cần chạy lần đầu)
npm install

# Khởi chạy dev server
npm run dev
```
* **Truy cập Web App tại:** `http://localhost:5173`

---

## 📌 BƯỚC 5: Khởi chạy Mobile App (Flutter)

Mở **Terminal 5** (Đảm bảo đã kết nối thiết bị thật hoặc mở máy ảo Android/iOS):
```bash
cd "05_Development/CareBridgeMobileApp"

# Cập nhật dependencies
flutter pub get

# Khởi chạy ứng dụng
flutter run
```

---

## 🧪 BƯỚC 6: KỊCH BẢN KIỂM THỬ TỪNG TÍNH NĂNG (TESTING WORKFLOW)

### Kịch bản 1: Sàng lọc Sinh hiệu Mẹ Bầu & Báo động Cấp cứu (Bước 7 ➔ 8 ➔ 9)
1. Mở trình duyệt truy cập: `http://localhost:8001/docs`
2. Chọn `POST /api/v1/metrics/evaluate` ➔ Bấm **Try it out**.
3. Dán JSON thử nghiệm ca **Tiền sản giật khẩn cấp (Huyết áp 165/110 + đau đầu, nhìn mờ)**:
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
4. Bấm **Execute**:
   * **Kết quả trả về:**
     - `status`: `"CRITICAL_EMERGENCY"`
     - `emergency_mode`: `true` *(kích hoạt gọi 115 và định vị BV Sản gần nhất)*
     - `relevant_sources`: Trích dẫn cẩm nang y tế chương Dấu hiệu nguy hiểm.

---

### Kịch bản 2: Hỏi đáp Trợ lý Điều dưỡng AI Nurse RAG Chat (Bước 10)
1. Chọn `POST /api/v1/chat/message` ➔ Bấm **Try it out**.
2. Dán câu hỏi về chăm sóc thai kỳ:
   ```json
   {
     "message": "Em mang thai 3 tháng đầu thì cần lưu ý bổ sung những vi chất gì quan trọng nhất?",
     "stage": "PREGNANCY",
     "gestational_age_weeks": 10
   }
   ```
3. Bấm **Execute**:
   * **Kết quả trả về:**
     - Gemini Flash giải đáp chi tiết về Axit Folic, Sắt, Canxi theo cẩm nang Bộ Y Tế.
     - Kèm trích dẫn nguồn (`sources`) từ Viện Dinh Dưỡng Quốc Gia.
     - Kèm 3 câu hỏi gợi ý tiếp theo (`suggested_followups`).

---

### Kịch bản 3: Nạp trực tiếp file PDF/Word Cẩm nang mới qua Web Admin
1. Chọn `POST /api/v1/documents/upload` ➔ Bấm **Try it out**.
2. Chọn file `.pdf` hoặc `.docx` từ máy tính ➔ Bấm **Execute**.
3. Hệ thống sẽ tự động cắt khúc văn bản, tạo vector và lưu vào pgvector.

---

### Kịch bản 4: Chạy Toàn bộ Unit & Integration Test Tự động
Mở terminal và chạy lệnh test độc lập cho AI Service:
```bash
cd "05_Development/CareBridgeAITriageService"
./venv/bin/pytest tests/ -v
```
*(Toàn bộ **14/14 test cases** lâm sàng và API đều đạt kết quả 100% Passed).*
