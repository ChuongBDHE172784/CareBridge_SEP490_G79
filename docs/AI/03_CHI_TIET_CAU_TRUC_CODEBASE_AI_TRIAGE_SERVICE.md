# CHI TIẾT CẤU TRÚC CODEBASE CAREBRIDGE AI TRIAGE SERVICE

> **Dự án:** CareBridge (SEP490 - Capstone Project)  
> **Module:** `05_Development/CareBridgeAITriageService`  
> **Kiến trúc:** Python 3.11+ / FastAPI / Google Gemini / PostgreSQL + pgvector / LangChain Chunking  
> **Mục đích tài liệu:** Giải thích tường tận từng thư mục, từng file mã nguồn, chức năng, input/output và mối liên hệ giữa các tầng trong hệ thống AI RAG.

---

## 1. SƠ ĐỒ CÂY THƯ MỤC TỔNG THỂ (DIRECTORY TREE)

```text
05_Development/CareBridgeAITriageService/
├── app/                                # Mã nguồn chính của ứng dụng FastAPI
│   ├── api/                            # Tầng Router (Giao diện API RESTful)
│   │   ├── v1/
│   │   │   ├── __init__.py
│   │   │   ├── chat.py                 # Endpoint: AI Nurse Assistant RAG Chat (Bước 10)
│   │   │   ├── documents.py            # Endpoint: Upload file & Nạp dữ liệu vào pgvector
│   │   │   └── metrics.py              # Endpoint: Sàng lọc Sinh hiệu Mẹ bầu (Bước 7 ➔ 8 ➔ 9)
│   │   ├── __init__.py
│   │   └── health.py                   # Endpoint: Health check & Kiểm tra Vector DB
│   ├── constants/                      # Tầng Hằng số & Danh mục Lâm sàng (Clean Code & Clinical Thresholds)
│   │   ├── __init__.py
│   │   └── vital_thresholds.py         # Quản lý tập trung const & enum ngưỡng sinh hiệu chuẩn Bộ Y Tế / WHO / ACOG
│   ├── core/                           # Tầng Hạ tầng Cốt lõi (Infrastructure)
│   │   ├── __init__.py
│   │   ├── database.py                 # Quản lý kết nối Async PostgreSQL & SQLAlchemy
│   │   ├── gemini.py                   # Client Gemini Flash & Embedding kèm Auto-Fallback
│   │   └── security.py                 # Xác thực bảo mật Internal API Key
│   ├── models/                         # Tầng Định nghĩa Dữ liệu (Data Models & DTOs)
│   │   ├── __init__.py
│   │   ├── db_models.py                # SQLAlchemy ORM Model (Bảng maternal_knowledge_chunks)
│   │   └── schemas.py                  # Pydantic Schemas (Request/Response DTOs)
│   ├── rag/                            # Tầng Xử lý Tri thức Y khoa & Vector Engine
│   │   ├── __init__.py
│   │   ├── chunker.py                  # Bộ cắt nhỏ tài liệu (PDF, DOCX, Markdown, TXT)
│   │   ├── embedder.py                 # Bộ tạo Vector Embeddings (768 chiều)
│   │   ├── prompts.py                  # System Prompts Y tế & Prompt Template Builder
│   │   └── vector_store.py             # Thao tác tìm kiếm Cosine Similarity trên pgvector
│   ├── services/                       # Tầng Xử lý Nghiệp vụ Logic (Business Services)
│   │   ├── __init__.py
│   │   ├── ingestion_service.py        # Dịch vụ nạp file, cắt chunk và lưu Vector DB
│   │   ├── metrics_screening_service.py# Sàng lọc sinh hiệu, phát hiện Tiền sản giật & Cấp cứu SOS
│   │   └── rag_chat_service.py         # Xử lý hội thoại RAG, trích dẫn nguồn & gợi ý follow-up
│   ├── __init__.py
│   ├── config.py                       # Quản lý cấu hình tập trung từ biến môi trường (.env)
│   └── main.py                         # Application Entrypoint, CORS, Lifespan & Spring Boot Bridge
├── data/
│   └── raw_documents/                  # Nơi lưu trữ các file cẩm nang y tế gốc
│       ├── 01_dau_hieu_nguy_hiem_khi_mang_thai.md
│       ├── 02_cham_soc_dinh_duong_tiem_chung_thai_ky.md
│       ├── 03_theo_doi_chi_so_sinh_hieu_cu_dong_thai.md
│       └── 04_cham_soc_sau_sinh_va_be_so_sinh.md
├── scripts/                            # Các công cụ dòng lệnh (CLI Tools)
│   ├── init_pgvector_db.py             # Script khởi tạo extension vector, bảng và HNSW index
│   └── ingest_documents.py             # Script CLI nạp tri thức từ thư mục vào pgvector
├── tests/                              # Bộ kiểm thử tự động (Unit & Integration Tests)
│   ├── conftest.py                     # Cấu hình môi trường test Pytest
│   ├── test_api_endpoints.py           # Test toàn bộ REST API endpoints
│   ├── test_ingestion_and_chunker.py   # Test bộ cắt chunk và nạp tài liệu
│   ├── test_metrics_screening.py       # Test sàng lọc sinh hiệu (Bình thường, Tiền sản giật, Sốt...)
│   └── test_rag_chat.py                # Test chatbot AI Nurse và nhận diện ý định khẩn cấp
├── .env                                # File biến môi trường cấu hình API Key & Database
├── .env.example                        # File mẫu biến môi trường
├── requirements.txt                    # Danh sách các thư viện Python phụ thuộc
└── README.md                           # Tài liệu hướng dẫn sử dụng nhanh
```

---

## 2. GIẢI THÍCH CHI TIẾT TỪNG FILE & MODULE

### 2.1. Tầng API Router (`app/api/`)
* **`app/api/v1/metrics.py`:**
  - *Chức năng:* Cung cấp API `POST /api/v1/metrics/evaluate` phục vụ **Bước 7 ➔ 8 ➔ 9** trong workflow hệ thống.
  - *Input:* JSON chứa huyết áp ($SBP/DBP$), đường huyết, nhiệt độ, cử động thai, tuần thai, triệu chứng.
  - *Output:* Đánh giá rủi ro (`CRITICAL_EMERGENCY`, `ANOMALY_MONITOR`, `NORMAL`), cờ `emergency_mode` (kích hoạt SOS/115/chỉ đường bệnh viện), nguyên nhân rủi ro và tài liệu cẩm nang y tế đối chiếu.
* **`app/api/v1/chat.py`:**
  - *Chức năng:* Cung cấp API `POST /api/v1/chat/message` phục vụ **Bước 10 (AI Nurse Assistant RAG Chat)**.
  - *Input:* Câu hỏi của mẹ bầu, giai đoạn thai kỳ, tuần thai.
  - *Output:* Lời giải đáp ân cần từ Gemini Flash, trích dẫn nguồn cẩm nang (`sources`), danh sách gợi ý 3 câu hỏi tiếp theo (`suggested_followups`) và lời nhắc Disclaimer pháp lý y tế.
* **`app/api/v1/documents.py`:**
  - *Chức năng:* Cung cấp các API quản lý cơ sở tri thức:
    - `POST /api/v1/documents/upload`: Nhận file upload (`.pdf`, `.docx`, `.md`, `.txt`), tự động cắt khúc và lưu vào pgvector.
    - `POST /api/v1/documents/ingest-text`: Nạp trực tiếp một đoạn văn bản thô vào Vector DB.
    - `POST /api/v1/documents/sync-directory`: Quét thư mục `data/raw_documents/` và đồng bộ vào CSDL.
* **`app/api/health.py`:**
  - *Chức năng:* Cung cấp API `GET /health` giám sát hệ thống (trạng thái model, tình trạng kết nối PostgreSQL và tổng số chunks tri thức đang có).

---

### 2.2. Tầng Hạ tầng Cốt lõi (`app/core/`)
* **`app/core/database.py`:**
  - *Chức năng:* Tạo Async SQLAlchemy Engine kết nối PostgreSQL qua driver `asyncpg`. Cung cấp dependency `get_db()` với cơ chế xử lý lỗi graceful (nếu chưa bật PostgreSQL, tự động fallback sang bộ nhớ đệm RAM mà không gây crash ứng dụng).
* **`app/core/gemini.py`:**
  - *Chức năng:* Đóng gói client Google GenAI SDK:
    - `embed_text()` / `embed_texts()`: Gọi mô hình `gemini-embedding-001` với `output_dimensionality=768` để sinh Neural Vector 768 chiều.
    - `generate_response()`: Gọi LLM `gemini-flash-lite-latest` (hoặc `gemini-3.7-flash`).
    - **Multi-Model Auto Fallback:** Tự động thử lần lượt các model dự phòng (`gemini-flash-lite-latest` $\rightarrow$ `gemini-2.5-flash` $\rightarrow$ `gemini-flash-latest`) nếu có sự cố nghẽn mạng từ Google.
* **`app/core/security.py`:**
  - *Chức năng:* Middleware kiểm tra header bảo mật (`X-Internal-API-Key`, `X-CareBridge-Internal-Key`). Tự động cấp quyền thuận tiện khi nhà phát triển test trực tiếp trên Swagger UI / localhost.

---

### 2.3. Tầng Hằng số & Danh mục Lâm sàng (`app/constants/`)
* **`app/constants/vital_thresholds.py`:**
  - *Chức năng:* Áp dụng nguyên tắc **Clean Code & Type-Safety**, loại bỏ hoàn toàn Hardcoded Magic Numbers khỏi logic xử lý. Toàn bộ hằng số và ngưỡng đều được chú thích rõ nguồn y khoa và đường dẫn tài liệu chính thống:
    - **`Enum` Danh mục:** `VitalSignType`, `GlucoseMeasurementContext` (6 ngữ cảnh đo Dropdown: `FASTING`, `PRE_MEAL`, `POST_MEAL_1H`, `POST_MEAL_2H`, `RANDOM`, `OTHER_APPROVED`), `BloodPressureCategory`, `TemperatureCategory`, `BMICategory`.
    - **`Const` Hằng số Ngưỡng Lâm sàng:**
      - Huyết áp khẩn cấp ($SBP \ge 160, DBP \ge 110$) theo QĐ 1154/QĐ-BYT (2024 - Thư Viện Pháp Luật) & ACOG PB 222 (PubMed: 32443079).
      - Thân nhiệt sốt thai kỳ ($\ge 38.5^\circ C$) / sốt hậu sản ($\ge 38.0^\circ C$) theo WHO Peripartum Infection Guidelines & QĐ 1359/BYT.
      - Đường huyết 6 ngữ cảnh theo QĐ 1470/QĐ-BYT (2024 - BVĐK Bạc Liêu) & ADA Standards of Care (2024).
      - Cử động thai máy ($\ge 4$ lần/2h từ tuần 28) theo RCOG Green-top 57 & ACOG PB 229 (PubMed: 34011892).
      - Phân tầng thể trạng BMI theo 3 giai đoạn (`PRECONCEPTION` theo WHO Asian Lancet 2004 $18.5 - 22.9$, `PREGNANCY` theo IOM 2009, `POSTPARTUM` bảo vệ nguồn sữa mẹ).
      - Nhu cầu nước uống theo 3 giai đoạn (`PRECONCEPTION` 1.5-2L, `PREGNANCY` 2-2.5L, `POSTPARTUM` 2.5-3L).
      - $SpO_2 < 92\%$, Nhịp tim $> 120$ bpm, Cờ đỏ câu 10 thang trầm cảm EPDS...
    - **`SANITY_RANGES`:** Bộ giới hạn dải sinh lý y tế hợp lý để bắt lỗi người dùng gõ nhầm đơn vị hoặc số liệu phi lý (ví dụ: gõ nhầm 300 mmol/L, $SBP \le DBP$).
    - **Danh mục từ khóa báo động (Keywords Catalog):** Tiền sản giật, Nhiễm trùng ối, Vỡ ối, Ra máu tươi... theo chuẩn Bộ Y Tế, WHO và ACOG.
* **`app/constants/__init__.py`:**
  - *Chức năng:* Package export thuận tiện cho toàn bộ service và tests tái sử dụng nhất quán.

---

### 2.4. Tầng Định nghĩa Dữ liệu (`app/models/`)
* **`app/models/db_models.py`:**
  - *Chức năng:* Định nghĩa bảng CSDL `maternal_knowledge_chunks`:
    - `id`: Khóa chính tự tăng.
    - `title`, `stage`, `topic`, `source`, `section`: Siêu dữ liệu phân loại tài liệu y tế.
    - `content`: Nội dung văn bản tiếng Việt của đoạn cẩm nang.
    - `embedding`: Cột kiểu `Vector(768)` lưu trữ vector tọa độ ngữ nghĩa.
* **`app/models/schemas.py`:**
  - *Chức năng:* Định nghĩa các Pydantic DTO (Data Transfer Objects) đảm bảo tính toàn vẹn kiểu dữ liệu:
    - `HealthMetricsLogRequest` (bổ sung `glucose_context`, hỗ trợ nhận diện giai đoạn `MaternalStage`, tuần thai và đầy đủ sinh hiệu), `HealthMetricsEvaluationResponse`
    - `RagChatRequest`, `RagChatResponse`
    - `IngestDocumentRequest`, `IngestDocumentResponse`, `BatchIngestResponse`

---

### 2.5. Tầng Xử lý Tri thức RAG (`app/rag/`)
* **`app/rag/chunker.py`:**
  - *Chức năng:* Đọc file đa định dạng (dùng `pypdf` đọc PDF, `python-docx` đọc Word, `python-frontmatter` đọc Markdown). Áp dụng thuật toán `RecursiveCharacterTextSplitter` cắt nhỏ văn bản với `chunk_size = 900` ký tự và `chunk_overlap = 180` ký tự (20%).
* **`app/rag/embedder.py`:**
  - *Chức năng:* Cầu nối gọi `GeminiClient` để sinh vector cho các chunk tài liệu hoặc câu hỏi người dùng.
* **`app/rag/vector_store.py`:**
  - *Chức năng:* Thực thi câu truy vấn tìm kiếm Vector trên PostgreSQL sử dụng toán tử khoảng cách Cosine (`<=>`):
    ```sql
    SELECT *, (embedding <=> :query_vector) AS distance
    FROM maternal_knowledge_chunks
    WHERE stage IN (:stages)
    ORDER BY distance ASC
    LIMIT :top_k;
    ```
* **`app/rag/prompts.py`:**
  - *Chức năng:* Quản lý tập trung các System Prompt y tế nghiêm ngặt:
    - `NURSE_ASSISTANT_SYSTEM_PROMPT`: Ràng buộc 6 nguyên tắc an toàn (Non-diagnostic, không kê đơn, Strict Grounding cẩm nang, ân cần).
    - `METRICS_REASONING_SYSTEM_PROMPT`: Chuẩn mực phân tích rủi ro sinh hiệu.
    - `build_rag_chat_prompt()`: Ghép nối Context tri thức + Câu hỏi + Thông tin mẹ bầu thành một Prompt hoàn chỉnh.

---

### 2.6. Tầng Xử lý Nghiệp vụ (`app/services/`)
* **`app/services/metrics_screening_service.py`:**
  - *Chức năng:* Trái tim của **Bước 7 ➔ 8 ➔ 9**. Triển khai mô hình **Deterministic Safety Guardrails**: Import và đối chiếu toàn bộ chỉ số đầu vào với các hằng số y khoa từ `app.constants.vital_thresholds` ($< 1\text{ ms}$) để đảm bảo an toàn tuyệt đối, loại bỏ rủi ro ảo giác AI đối với các ca cấp cứu sản khoa nguy kịch; đồng thời kích hoạt tìm kiếm RAG đối chiếu cẩm nang để trích dẫn bằng chứng y khoa (`SourceCitation`).
  - *Hỗ trợ toàn diện:* 6 ngữ cảnh đo đường huyết (`FASTING`, `PRE_MEAL`, `POST_MEAL_1H`, `POST_MEAL_2H`, `RANDOM`, `OTHER_APPROVED`), kiểm soát tuần thai $\ge 28$ tuần cho thai máy, phân tầng BMI & Lượng nước uống theo 3 giai đoạn hành trình (`PRECONCEPTION`, `PREGNANCY`, `POSTPARTUM`), và kiểm tra dải sinh lý hợp lý (Sanity validation).
* **`app/services/rag_chat_service.py`:**
  - *Chức năng:* Điều phối luồng **Bước 10**. Nhận câu hỏi $\rightarrow$ Vector Search $\rightarrow$ Bơm Context vào Prompt $\rightarrow$ Gọi Gemini Flash tạo sinh $\rightarrow$ Trích xuất trích dẫn tài liệu & sinh 3 câu hỏi gợi ý tiếp theo.
* **`app/services/ingestion_service.py`:**
  - *Chức năng:* Quản lý toàn bộ vòng đời nạp tài liệu: Đọc file $\rightarrow$ Cắt Chunks $\rightarrow$ Sinh Embedding $\rightarrow$ Lưu vào pgvector.

---

### 2.7. File Cấu hình & Khởi chạy Ứng dụng
* **`app/config.py`:** Quản lý toàn bộ cấu hình: Model name, Embedding dimension (768), Database URL, API Key, Timeout, Medical Disclaimer.
* **`app/main.py`:** 
  - Tạo ứng dụng FastAPI.
  - Cấu hình Middleware CORS.
  - Vòng đời `lifespan`: Tự động nạp trước các file cẩm nang trong `data/raw_documents/` khi server khởi động.
  - **Spring Boot Bridge (`POST /internal/triage/turn`):** Cung cấp endpoint tương thích ngược cho backend Java `CareBridgeAPI`.

---

## 3. MỐI QUAN HỆ & LUỒNG DỮ LIỆU GIỮA CÁC MODULE (DATA FLOW)

```
[Request từ Client / Swagger / Mobile / Web]
                       │
                       ▼
             [app/core/security.py] (Kiểm tra API Key)
                       │
         ┌─────────────┴─────────────┐
         ▼                           ▼
[POST /api/v1/metrics/eval]  [POST /api/v1/chat/message]
         │                           │
         ▼                           ▼
[metrics_screening_service]  [rag_chat_service]
         │                           │
         ├───────────────────────────┤
         ▼                           ▼
  [app/rag/prompts.py] ◄──► [app/rag/vector_store.py]
                                     │ (Cosine Distance <=>)
                                     ▼
                        [app/core/database.py]
                                     │
                                     ▼
                      [(PostgreSQL + pgvector)]
```

---

## 4. TÓM TẮT GIÁ TRỊ THIẾT KẾ CHO BÁO CÁO & BẢO VỆ ĐỒ ÁN

1. **Tính Mô-đun hóa cao (High Modularity):** Phân tách rõ ràng giữa tầng Routing (API), tầng Nghiệp vụ (Services), tầng Dữ liệu (Models), và tầng Hạ tầng (Core & RAG).
2. **Khả năng Mở rộng (Extensibility):** Dễ dàng bổ sung thêm các loại tài liệu mới hoặc tích hợp thêm các cảm biến IoT theo dõi sinh hiệu mà không làm ảnh hưởng đến cấu trúc hiện tại.
3. **Độ Tin cậy & Khả năng Chống lỗi (Fault-Tolerance):** Tích hợp Multi-Model Fallback và Graceful In-Memory Fallback đảm bảo dịch vụ luôn hoạt động ổn định 24/7.
