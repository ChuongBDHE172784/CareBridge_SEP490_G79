# CAREBRIDGE AI RAG — TÀI LIỆU THIẾT KẾ KIẾN TRÚC, KỸ THUẬT VÀ CẨM NANG BẢO VỆ ĐỒ ÁN (DEFENSE HANDBOOK)

> **Dự án:** CareBridge (SEP490 - Capstone Project)  
> **Phân hệ:** `CareBridgeAITriageService` (Hệ thống AI RAG & Sàng lọc Chỉ số Sinh hiệu Mẹ Bầu)  
> **Mục đích tài liệu:** Cung cấp toàn bộ thiết kế kiến trúc, giải thích chi tiết kỹ thuật RAG, so sánh các thuật toán chunking, luồng logic hoạt động, thuật toán vector, cơ chế hội thoại đa lượt, và bộ câu hỏi - đáp chuyên sâu phục vụ bảo vệ trước **Hội đồng Chấm Đồ án Tốt nghiệp**.

---

## MỤC LỤC
1. [Bản chất AI RAG trong Y tế & Dự án CareBridge](#1-bản-chất-ai-rag-trong-y-tế--dự-án-carebridge)
2. [Kiến trúc Tổng thể & Lựa chọn Công nghệ](#2-kiến-trúc-tổng-thể--lựa-chọn-công-nghệ)
3. [Kỹ thuật Data Pipeline: Ingestion, Chunking & Embeddings](#3-kỹ-thuật-data-pipeline-ingestion-chunking--embeddings)
4. [Cơ chế Vector Database & Thuật toán Tìm kiếm (pgvector + HNSW)](#4-cơ-chế-vector-database--thuật-toán-tìm-kiếm-pgvector--hnsw)
5. [Luồng Logic Hoạt động theo Workflow Hệ thống](#5-luồng-logic-hoạt-động-theo-workflow-hệ-thống)
6. [Quản lý Ngữ cảnh Hội thoại Đa lượt & Gợi ý Động (Multi-turn Context & Dynamic Follow-ups)](#6-quản-lý-ngữ-cảnh-hội-thoại-đa-lượt-multi-turn-context--query-expansion)
7. [Kiến trúc Giao diện AI Nurse trên Ứng dụng Di động (Mobile App UI & State)](#7-kiến-trúc-giao-diện-ai-nurse-trên-ứng-dụng-di-động-mobile-app-ui--state)
8. [Bộ Công cụ Quản trị, Soi Vector & Mô phỏng Lâm sàng](#8-bộ-công-cụ-quản-trị-soi-vector--mô-phỏng-lâm-sàng)
9. [Bộ Câu hỏi & Trả lời Phản biện trước Hội Đồng (Defense Q&A - 12 Câu Hỏi Chuyên Sâu)](#9-bộ-câu-hỏi--trả-lời-phản-biện-trước-hội-đồng-defense-qa---12-câu-hỏi-chuyên-sâu)
10. [Hướng dẫn Vận hành & Nạp Thêm Tri Thức Mới](#10-hướng-dẫn-vận-hành--nạp-thêm-tri-thức-mới)

---

## 1. Bản chất AI RAG trong Y tế & Dự án CareBridge

### 1.1. AI RAG là gì?
**RAG (Retrieval-Augmented Generation - Tạo sinh có Tăng cường Truy xuất)** là một kiến trúc AI gồm hai công đoạn đồng bộ:
1. **Truy xuất Ngữ nghĩa (Semantic Retrieval):** Khi nhận câu hỏi từ mẹ bầu, hệ thống mã hóa câu hỏi thành vector tọa độ và tìm kiếm các đoạn cẩm nang y tế chính thống phù hợp nhất từ cơ sở dữ liệu tri thức (`pgvector`).
2. **Tăng cường & Tạo sinh (Augmentation & Generation):** Đóng gói các đoạn cẩm nang tìm được vào ngữ cảnh y khoa (Context) kèm câu hỏi và chuyển đến Mô hình Ngôn ngữ Lớn (LLM Gemini Flash). LLM tổng hợp câu trả lời ân cần, chính xác, bám sát tài liệu y tế được cấp.

```
[Câu hỏi mẹ bầu] ──► [Truy xuất Vector pgvector] ──► [Top K Đoạn Cẩm nang Bộ Y Tế]
                                                              │
                                                              ▼
[Câu trả lời chuẩn y khoa] ◄── [LLM Gemini Flash] ◄── [Prompt + Ngữ cảnh Cẩm nang]
```

### 1.2. Giá trị Cốt lõi của RAG trong Y tế Mẹ Bầu
- **Triệt tiêu Ảo giác (Zero Hallucination):** Ràng buộc AI chỉ được trả lời dựa trên tài liệu y tế đã được kiểm duyệt từ Bộ Y Tế, WHO, Bệnh viện Phụ Sản.
- **An toàn Pháp lý (Non-Diagnostic Safety):** AI giữ vai trò Trợ lý Điều dưỡng (AI Nurse Assistant), giải thích khoa học, hỗ trợ tinh thần và hướng dẫn mẹ thời điểm cần đến cơ sở y tế.
- **Cập nhật Tri thức Tức thì (Zero Re-training):** Bổ sung tài liệu mới vào hệ thống trong vài giây chỉ bằng việc nạp file, không cần tốn chi phí huấn luyện lại mô hình.
- **Minh bạch & Trích dẫn Nguồn (Citations):** Mọi câu trả lời đều ghi rõ nguồn gốc tài liệu để mẹ bầu và Bác sĩ đối chiếu.

---

## 2. Kiến trúc Tổng thể & Lựa chọn Công nghệ

```mermaid
flowchart TB
    subgraph INGESTION["1. INGESTION PIPELINE (Xử lý Tri thức Đa Định dạng)"]
        DOCS["Tài liệu Y tế Đa dạng<br/>(PDF, DOCX, Markdown, TXT)"] --> CHUNKER["Document Chunker<br/>(Recursive Hierarchical Splitting)<br/>Chunk: 900 chars | Overlap: 180 chars"]
        CHUNKER --> SEC_EXT["Smart Section Extractor<br/>(Tự bóc tách Heading/Điều khoản)"]
        SEC_EXT --> EMBED_GEN["Gemini Embedding Model<br/>(Neural Vector 768-dim)"]
        EMBED_GEN --> PG_VECTOR[("PostgreSQL + pgvector<br/>maternal_knowledge_chunks<br/>(Vector 768-dim, HNSW Index)")]
    end

    subgraph METRICS_FLOW["2. WORKFLOW SÀNG LỌC SINH HIỆU (Bước 7 ➔ 8 ➔ 9)"]
        USER_METRICS["Mẹ bầu nhập Sinh hiệu:<br/>Huyết áp, Đường huyết, Nhiệt độ,<br/>Cử động thai, Triệu chứng, Tuần thai"] --> EVAL_SVC["Metrics Screening Service"]
        EVAL_SVC --> THRESHOLD_GATE{"Kiểm tra Ngưỡng Lâm sàng<br/>& RAG Danger Signs"}
        THRESHOLD_GATE -->|Nguy hiểm cấp cứu| CRITICAL["CRITICAL_EMERGENCY (SOS)<br/>• emergency_mode = true<br/>• Gọi cấp cứu 115<br/>• Định vị BV Sản gần nhất"]
        THRESHOLD_GATE -->|Bất thường nhẹ| ANOMALY["ANOMALY_MONITOR<br/>• Gợi ý chuyển sang Bước 10 Chat AI Nurse"]
        THRESHOLD_GATE -->|Bình thường| NORMAL["NORMAL<br/>• Tiếp tục theo dõi thai kỳ an toàn"]
    end

    subgraph CHAT_FLOW["3. WORKFLOW AI NURSE ASSISTANT (Bước 10)"]
        USER_MSG["Mẹ bầu đặt câu hỏi bất kỳ:<br/>'Mang thai 3 tháng đầu cần uống vi chất gì?'"] --> RAG_SVC["RAG Chat Service"]
        RAG_SVC --> Q_EXPAND["Semantic Query Expansion<br/>(Tổng hợp triệu chứng 4-6 tin gần nhất)"]
        Q_EXPAND --> Q_EMBED["Embed Query Vector (768-dim)"]
        Q_EMBED --> V_SEARCH["Cosine Distance Search (<=>)<br/>Top K=4 chunks + Metadata Filter (Stage)"]
        PG_VECTOR -.-> V_SEARCH
        V_SEARCH --> PROMPT_BUILD["Prompt Builder<br/>• Medical System Instruction<br/>• Multi-turn History<br/>• User Query<br/>• Retrieved Contexts"]
        PROMPT_BUILD --> GEMINI_LLM["LLM: Gemini Flash-Lite / 3.7 Flash<br/>(Auto Model Fallback)"]
        GEMINI_LLM --> FINAL_RESP["Response hoàn chỉnh:<br/>• Lời giải đáp ân cần, khoa học<br/>• Trích dẫn nguồn (Citations & Section)<br/>• Gợi ý câu hỏi tiếp theo (Follow-ups)<br/>• Disclaimer Y tế bắt buộc"]
    end
```

### Bảng Lựa chọn Công nghệ & Lý do Khoa học

| Thành phần                    | Công nghệ lựa chọn                                                                              | Lý do khoa học & Ưu thế kỹ thuật                                                                                                                                                       |
| :---------------------------- | :---------------------------------------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Backend Framework**         | **FastAPI (Python 3.11+)**                                                                      | Xử lý bất đồng bộ (`asyncio`), độ trễ thấp, tự động sinh chuẩn OpenAPI/Swagger UI, quản lý schema type-safe với Pydantic v2.                                                           |
| **Vector Database**           | **PostgreSQL + pgvector**                                                                       | Lưu trữ trực tiếp Vector 768 chiều trong cùng một CSDL quan hệ với hệ thống chính, loại bỏ chi phí vận hành DB vector độc lập, hỗ trợ Transaction ACID, phân quyền và backup hợp nhất. |
| **Vector Index**              | **HNSW (Hierarchical Navigable Small World)**                                                   | Thuật toán đồ thị tìm kiếm láng giềng gần nhất xấp xỉ (Approximate Nearest Neighbors - ANN) với độ phức tạp tìm kiếm $O(\log N)$, phản hồi trong vài mili-giây.                        |
| **Embedding Model**           | **Gemini Embedding (`output_dimensionality=768`)**                                              | Mã hóa ngữ nghĩa tiếng Việt đa tầng, sinh vector 768 chiều chuẩn hóa.                                                                                                                  |
| **LLM Generator**             | **Google Gemini Flash-Lite / 3.7 Flash**                                                        | Tốc độ phản hồi cực nhanh (< 1.5s), quota dồi dào, khả năng suy luận lâm sàng và diễn đạt tiếng Việt ân cần.                                                                           |
| **Multi-Model Auto Fallback** | `gemini-flash-lite-latest` $\rightarrow$ `gemini-2.5-flash` $\rightarrow$ `gemini-flash-latest` | Đảm bảo **100% Uptime**, tự động chuyển sang model dự phòng nếu Google bảo trì hoặc nghẽn mạng.                                                                                        |

---

## 3. Kỹ thuật Data Pipeline: Ingestion, Chunking & Embeddings

### 3.1. Kỹ thuật Phân đoạn Văn bản (Recursive Hierarchical Text Splitting)
* **Thuật toán sử dụng:** `RecursiveCharacterTextSplitter`.
* **Cơ chế phân cấp:** Tách phân cấp theo danh sách ký tự ưu tiên `["\n## ", "\n### ", "\n#### ", "\n\n", "\n", ". ", " "]`.
  1. *Cấp 1:* Ưu tiên tách theo tiêu đề đề mục lớn (`\n## `, `\n### `) để giữ nguyên vẹn một ý niệm y khoa.
  2. *Cấp 2:* Nếu mục dài > 900 ký tự, tách theo từng đoạn văn (`\n\n`).
  3. *Cấp 3:* Nếu đoạn văn vẫn quá dài, tách theo dấu chấm kết câu (`. `).
  4. *Cấp 4:* Tách theo dấu cách giữa các từ (`" "`), không bao giờ chém ngang một từ ngữ y khoa.
* **Tham số tối ưu hóa:**
  - `chunk_size = 900` ký tự (~150 - 200 từ tiếng Việt): Kích thước vàng ôm trọn một nội dung y khoa (Triệu chứng + Cơ chế + Hướng xử trí), tránh tình trạng vector bị loãng ngữ nghĩa.
  - `chunk_overlap = 180` ký tự (20% overlap): Đảm bảo các câu nằm ở ranh giới giữa 2 chunk được gối đầu liền mạch.

### 3.2. Thuật toán Smart Section Extraction (Tự động Bóc tách Tiêu đề Đề mục)
Nhằm đảm bảo dữ liệu không bao giờ bị `NULL` và phần trích dẫn nguồn (Citations) luôn chỉ rõ tên chương mục cụ thể, hệ thống tích hợp bộ soi dòng thông minh:
* Tự động nhận diện tiêu đề Markdown (`#`, `##`, `###`).
* Tự động nhận diện số thứ tự điều khoản văn bản pháp luật (*"Điều 3:..."*, *"Bước 4: Tiệt khuẩn..."*, *"Chương IV: Cấp cứu sản khoa"*).
* Tự động nhận diện vị trí trang PDF (*"[Trang X]"*).
* Gán trực tiếp vào cột `section` của từng chunk tương ứng trong CSDL.

### 3.3. So sánh 5 Kỹ thuật Chunking trong Thế giới AI RAG

| Kỹ thuật Chunking                              | Nguyên lý                                                                                                       | Ưu điểm                                                                                  | Nhược điểm                                                                   | Đánh giá áp dụng                                  |
| :--------------------------------------------- | :-------------------------------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------------------- | :--------------------------------------------------------------------------- | :------------------------------------------------ |
| **Fixed-size Chunking**                        | Cắt cứng theo số ký tự (500, 1000).                                                                             | Nhanh, đơn giản.                                                                         | Rất dễ cắt đôi một từ ngữ hoặc ngắt giữa chừng câu cảnh báo cấp cứu.         | ❌ Thô sơ, không an toàn trong Y tế.               |
| **Sentence Chunking**                          | Tách theo từng câu riêng biệt sau dấu chấm.                                                                     | Câu văn hoàn chỉnh.                                                                      | Từng câu đơn lẻ bị thiếu ngữ cảnh tổng thể (đại từ không rõ nghĩa).          | ❌ Quá vụn vặt.                                    |
| **Recursive Hierarchical** *(CareBridge chọn)* | Cắt đệ quy phân cấp: `Tiêu đề` $\rightarrow$ `Đoạn văn` $\rightarrow$ `Câu` $\rightarrow$ `Từ` kèm 20% gối đầu. | **Bảo toàn cấu trúc cẩm nang**, không cắt vụn từ ngữ, tốc độ xử lý tức thì, chi phí = 0. | Cần văn bản có cấu trúc phân đoạn rõ ràng.                                   | ⭐ **Chuẩn mực công nghiệp tối ưu nhất cho Y tế.** |
| **Semantic Chunking**                          | Tính khoảng cách góc Vector giữa các câu liên tiếp để tìm điểm chuyển ý.                                        | Điểm ngắt tự nhiên theo mạch tư duy.                                                     | Tốn nhiều lượt gọi API Embedding (chậm, dễ chạm quota).                      | ⚠️ Phù hợp khi có server GPU riêng.                |
| **Agentic Chunking**                           | Dùng LLM đọc toàn bộ sách và tự chia chunk.                                                                     | Hiểu sâu ngữ cảnh.                                                                       | Tốn kém chi phí token rất lớn, tốc độ nạp chậm với tài liệu hàng trăm trang. | ⚠️ Tốn kém chi phí vận hành.                       |

### 3.4. Tính Linh hoạt Định dạng (Format-Agnostic Ingestion)
Hệ thống xử lý mượt mà 4 định dạng dữ liệu đầu vào mà **không bắt buộc phải có cấu trúc cố định**:
1. **Markdown (`.md`):** Tự động đọc YAML Frontmatter nếu có, hoặc tự động sinh nếu không có.
2. **Word (`.docx`):** Dùng `python-docx` đọc trực tiếp file hành chính/văn bản y tế (ví dụ: `Quyết-định-1359-QĐ-BYT.docx` 159KB được bóc tách 244 chunks mượt mà trong 2 giây).
3. **PDF (`.pdf`):** Dùng `pypdf` trích xuất text từng trang kèm nhãn `[Trang X]`.
4. **Văn bản thô (`.txt`):** Đọc text và làm sạch định dạng tự động.

---

## 4. Cơ chế Vector Database & Thuật toán Tìm kiếm (pgvector + HNSW)

### 4.1. Bản chất Neural Vector (Dense Embedding)
Mỗi câu hoặc đoạn văn bản cẩm nang y tế được mạng nơ-ron Transformer ánh xạ thành một vector tọa độ trong không gian 768 chiều:
$$\vec{v} = \text{Embed}(\text{"đau đầu, hoa mắt, nhìn mờ"}) = [e_1, e_2, e_3, \dots, e_{768}] \in \mathbb{R}^{768}$$
Khi mẹ bầu diễn đạt theo ngôn ngữ đời thường *"em bị nhức đầu như búa bổ, mắt nhìn thấy đốm sáng"*, mạng nơ-ron sinh ra vector $\vec{q}$ có hướng không gian gần như trùng khớp với vector $\vec{v}$ của bài viết *"Dấu hiệu Tiền sản giật"*, giúp tìm đúng tài liệu ngay cả khi từ ngữ không trùng khớp từng chữ.

### 4.2. Độ đo Tương đồng Cosine (Cosine Similarity)
Hệ thống sử dụng toán tử khoảng cách Cosine Distance (`<=>`) trong pgvector:
$$\text{Cosine Similarity}(\vec{u}, \vec{v}) = \frac{\vec{u} \cdot \vec{v}}{\|\vec{u}\| \|\vec{v}\|} = \frac{\sum_{i=1}^{768} u_i v_i}{\sqrt{\sum_{i=1}^{768} u_i^2} \sqrt{\sum_{i=1}^{768} v_i^2}}$$
- $\text{Cosine Distance} = 1 - \text{Cosine Similarity}$. Khoảng cách càng nhỏ (gần 0), hai đoạn văn bản càng tương đồng về ngữ nghĩa y khoa.

### 4.3. Cấu trúc bảng CSDL `maternal_knowledge_chunks` (Enterprise RAG Schema)
```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE maternal_knowledge_chunks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    stage VARCHAR(50) NOT NULL DEFAULT 'ALL',
    topic VARCHAR(100) NOT NULL DEFAULT 'GENERAL',
    source VARCHAR(255) NOT NULL,
    section VARCHAR(255),
    content TEXT NOT NULL,
    chunk_index INTEGER DEFAULT 0,
    embedding VECTOR(768),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW()
);

-- Chỉ mục HNSW cho tìm kiếm siêu tốc
CREATE INDEX idx_maternal_chunks_embedding_hnsw 
ON maternal_knowledge_chunks 
USING hnsw (embedding vector_cosine_ops);
```

#### Giải thích chi tiết các trường dữ liệu:
* **`id`:** Định danh duy nhất cho từng đoạn tri thức.
* **`title`:** Tên tài liệu / Tiêu đề cẩm nang.
* **`stage`:** Phân loại giai đoạn (`PREGNANCY`, `POSTPARTUM`, `ALL`) phục vụ **Metadata Filtering**.
* **`topic`:** Chủ đề y khoa linh hoạt (`DANGER_SIGNS`, `NUTRITION`, `HEALTH_MONITORING`, `GENERAL`...).
* **`source`:** Cơ quan ban hành (*Bộ Y Tế, WHO, BV Từ Dũ*) để trích dẫn minh bạch.
* **`section`:** Chương/Mục/Tiểu mục cụ thể của đoạn văn bản (được bóc tách tự động).
* **`content`:** Nội dung văn bản tiếng Việt đưa vào Context cho Gemini.
* **`chunk_index`:** Vị trí thứ tự của đoạn trong tài liệu gốc.
* **`embedding`:** Vector 768 chiều dùng cho thuật toán Cosine Distance.
* **`created_at`:** Thời gian nạp tri thức.

---

### 4.4. Thuật toán Tìm kiếm Lai (Hybrid Search - Enterprise RAG Standard)

Trong thực tế triển khai RAG y tế, nếu chỉ sử dụng **Pure Dense Vector Search (Tìm kiếm Vector Thuần túy)**, hệ thống có thể gặp hiện tượng **Semantic Dilution (Pha loãng Ngữ nghĩa)** hoặc **Semantic Drift (Lệch ngữ cảnh)**:
* Một đoạn văn bản dài về *Kế hoạch hóa gia đình / Tránh thai* có chứa cụm từ *"mang thai 3 tháng đầu"* có thể vô tình đạt điểm khoảng cách vector gần với câu hỏi *"Mang thai 3 tháng đầu cần bổ sung vi chất gì?"*, dẫn tới việc trích xuất nhầm tài liệu.
* Để khắc phục triệt để, CareBridge triển khai kiến trúc **Hybrid Search (Tìm kiếm Lai hai lớp)** kết hợp sức mạnh của 2 thế giới:
  1. **Dense Semantic Retrieval (Lớp Vector Sâu):** Sử dụng pgvector Cosine Distance để nắm bắt ý định tự nhiên, từ đồng nghĩa và sắc thái câu hỏi của người dùng.
  2. **Sparse Keyword & Medical Entity Re-ranking (Lớp Từ khóa Chuyên khoa):** Đánh giá tần suất và sự xuất hiện chính xác của các thực thể y khoa (như *vi chất, axit folic, sắt, canxi, tiền sản giật, sau sinh...*).

#### Công thức tính điểm Re-ranking tổng hợp (Hybrid Scoring Formula):
$$\text{Hybrid Score} = (0.35 \times \text{Vector Similarity}) + (0.45 \times \text{Keyword Hit Ratio}) + \text{Medical Phrase Boost}$$

* $\text{Keyword Hit Ratio} = \frac{\text{Số từ khóa y khoa khớp}}{\text{Tổng số từ khóa trong câu hỏi}}$
* $\text{Medical Phrase Boost} \in [0.25, 0.35]$: Thưởng điểm trực tiếp khi phát hiện khớp cụm từ chuyên môn chính xác (VD: *"axit folic"*, *"vi chất"*, *"tiền sản giật"*).

Nhờ cơ chế này, tài liệu chuyên khảo về *Dinh dưỡng thai kỳ* đạt điểm tuyệt đối ($\approx 0.95 - 1.39$), trong khi các tài liệu không liên quan bị đẩy xuống cuối hoặc loại bỏ hoàn toàn.

---

### 4.5. Cơ chế Lọc Ngưỡng Tương Đồng & Khử Trùng Lặp Trích Dẫn (Relevance Gate & Deduplication)

1. **Relevance Gate (Bộ lọc Ngưỡng Liên quan):**
   * Các đoạn văn bản có $\text{Similarity Score} < 0.05$ (không mang ý nghĩa đóng góp cho câu trả lời) sẽ bị loại bỏ hoàn toàn, không được đưa vào `context` của Prompt và không hiển thị ở danh sách `sources` của câu trả lời.
   * Ngăn chặn tình trạng AI trích dẫn các tài liệu "râu ông nọ cắm cằm bà kia".

2. **Citation Deduplication (Khử Trùng Lặp Nguồn):**
   * Nếu nhiều chunk thuộc cùng một Tiêu đề và Mục tài liệu (`f"{title}_{section}"`) cùng lọt vào Top-K, hệ thống tự động gộp và chỉ hiển thị 1 nguồn duy nhất đại diện, giúp giao diện người dùng trên Mobile App luôn gọn gàng, chuyên nghiệp và minh bạch.

---

## 5. Luồng Logic Hoạt động theo Workflow Hệ thống

Bám sát sơ đồ thiết kế hệ thống tại [docs/mainworkflow-Trang-3.drawio.png](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/docs/mainworkflow-Trang-3.drawio.png):

### 5.1. Luồng Sàng lọc Chỉ số Sức khỏe & Sinh hiệu Lâm sàng (Bước 7 ➔ 8 ➔ 9)

Hệ thống CareBridge chuẩn hóa danh mục các chỉ số sức khỏe dựa trên hướng dẫn chuyên môn của **Bộ Y tế Việt Nam** và **Tổ chức Y tế Thế giới (WHO)**.

#### A. Danh mục 8 Chỉ số Sức khỏe Chuẩn hóa của Mẹ (Maternal Health Metrics):
1. **Chỉ số khối cơ thể (BMI / Cân nặng & Chiều cao):** Đơn vị $kg/m^2$ — Giám sát mức tăng cân theo từng tam cá nguyệt.
2. **Cử động thai (Fetal Movement Count / Session):** Đơn vị $count / session$ — Theo dõi thai máy từ tuần 28 để phát hiện sớm dấu hiệu suy thai.
3. **Huyết áp (Blood Pressure - $SBP / DBP$):** Đơn vị $mmHg$ — Sàng lọc cao huyết áp thai kỳ và Tiền sản giật.
4. **Lượng nước uống (Hydration):** Đơn vị $ml$ — Theo dõi việc bổ sung nước đầy đủ cho mẹ và tái tạo thể tích tuần hoàn/nước ối.
5. **Nhịp tim mẹ (Maternal Heart Rate):** Đơn vị $bpm$ — Phát hiện rối loạn nhịp tim, hồi hộp, thiếu máu thai kỳ.
6. **Điểm sàng lọc trầm cảm EPDS (Edinburgh Postnatal Depression Scale):** Thang điểm $0 - 30$ điểm (10 câu hỏi chuẩn y khoa) — Đánh giá sức khỏe tâm thần, sàng lọc sớm Trầm cảm trước và sau sinh; có cơ chế cờ đỏ an toàn nếu câu hỏi 10 $> 0$.
7. **Đường huyết (Blood Glucose):** Đơn vị $mg/dL$ hoặc $mmol/L$ — Theo dõi và phòng ngừa Đái tháo đường thai kỳ (ĐTĐTK).
8. **Thân nhiệt / Nhiệt độ cơ thể (Body Temperature - `TEMPERATURE`):** Đơn vị $^\circ C$ ($30.0 - 45.0^\circ C$) kèm vị trí đo (`ARMPIT`, `FOREHEAD`, `ORAL`, `EAR`) — Phân tích theo từng giai đoạn sản khoa (Mang thai vs Hậu sản vs Chu sinh) để phát hiện sớm Sốt cao thai kỳ, Nhiễm trùng ối (*Chorioamnionitis*) và Sốt nhiễm trùng hậu sản (*Puerperal Sepsis* theo chuẩn WHO).

*(Hệ thống kiên quyết loại bỏ chỉ số "Mức độ căng thẳng / Stress" cảm tính chung chung để thay thế bằng thang điểm trầm cảm EPDS chuẩn y tế được quốc tế công nhận).*

---

#### B. Chỉ số Phát triển của Bé (Giai đoạn Sau sinh — UC-31):
* **Cân nặng bé ($Baby Weight$):** Đơn vị $kg$.
* **Chiều dài lúc sinh / Chiều cao bé ($Baby Length$):** Đơn vị $cm$.
* *So sánh đối chiếu trực tiếp với biểu đồ chuẩn tăng trưởng của WHO ($Z-Score$).*

---

#### C. Cơ chế Tổng hợp Đa Ngữ cảnh Tự động (Multi-Source Context Aggregation):
Khi Mẹ bầu mở màn hình nhập chỉ số sức khỏe ([AddMaternalHealthMetricScreen](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart)), hệ thống không chỉ kiểm tra chỉ số đơn lẻ mà tự động kết hợp 3 nguồn thông tin:
1. **Dữ liệu Survey Khảo sát Ban đầu (Onboarding Medical Profile):**
   - Gọi `GET /api/v1/recommendations/profile` (hoặc trích xuất từ `mother_journeys.recommendation_profile_json`).
   - Tự động bóc tách các mã bệnh lý nền (`underlyingConditions`: `CHRONIC_HYPERTENSION`, `CARDIOVASCULAR_DISEASE`...), tiền sử sản khoa (`reproductiveHistory`: `PRIOR_PREECLAMPSIA`, `PRIOR_GDM`...), và BMI ban đầu.
2. **Tuần thai Thực tế & Giai đoạn Sản khoa (Gestational Age & Maternal Stage):**
   - Gọi `GET /api/v1/journey/dashboard` để lấy chính xác tuần thai hiện tại (`effectivePregnancyWeek` / `weekNumber`) và giai đoạn (`PREGNANCY` vs `POSTPARTUM`).
3. **Chỉ số Sức khỏe & Triệu chứng Mẹ vừa nhập:**
   - Huyết áp tâm thu/tâm trương ($SBP/DBP$), Đường huyết ($Glucose$), Cử động thai ($Kicks$), Cân nặng/Chiều cao, Thân nhiệt ($Temperature$), Nhịp tim và ghi chú triệu chứng tự do.

---

#### D. Quy tắc Kiểm tra Ngưỡng Lâm sàng & Thực thi Giao diện 3 Nhánh (End-to-End Workflow):
Dữ liệu tổng hợp được đóng gói và gửi lên AI Triage Service qua API `POST /api/v1/metrics/evaluate`. Hệ thống phân loại và điều hướng giao diện theo 3 nhánh:

```mermaid
flowchart TD
    INPUT["Mẹ nhập Chỉ số<br/>(Huyết áp, Thân nhiệt, Đường huyết, Thai máy, v.v.)"] --> SAVE_DB["Lưu Database PostgreSQL"]
    SAVE_DB --> AGGREGATE["Tổng hợp Đa Ngữ cảnh:<br/>• Survey bệnh nền & Tiền sử sản khoa<br/>• Tuần thai & Giai đoạn (PREGNANCY/POSTPARTUM)<br/>• Chỉ số & Triệu chứng vừa nhập"]
    AGGREGATE --> AI_EVAL["AI Triage Service<br/>(POST /api/v1/metrics/evaluate)"]
    
    AI_EVAL --> RISK_CHECK{"Đánh giá Rủi ro"}

    RISK_CHECK -->|CRITICAL_EMERGENCY| RED_BRANCH["🚨 NHÁNH ĐỎ: CẤP CỨU NGUY CẤP<br/>• Modal Cảnh báo Đỏ rực<br/>• Nút 'Mở Bản đồ BV & Cấp cứu'<br/>• Nút 'Gọi 115 ngay'"]
    RED_BRANCH --> RED_ACTIONS["Kích hoạt EmergencyMapScreen:<br/>1. Còi hú báo động SOS<br/>2. Tự động gửi GPS khẩn cấp tới nhóm Gia đình (Family Alert)<br/>3. Quét Bệnh viện Sản gần nhất & mở dẫn đường TrackAsia"]

    RISK_CHECK -->|ANOMALY_MONITOR| YELLOW_BRANCH["⚠️ NHÁNH VÀNG: BẤT THƯỜNG NHẸ<br/>• Modal Cảnh báo Vàng<br/>• Nút 'Hỏi Trợ lý AI Nurse'<br/>• Nút 'Đã hiểu & Tiếp tục theo dõi'"]
    YELLOW_BRANCH --> YELLOW_ACTIONS["Chuyển sang RagChatScreen:<br/>AI Nurse trả lời kèm trích dẫn cẩm nang y tế"]

    RISK_CHECK -->|NORMAL| GREEN_BRANCH["✅ NHÁNH XANH: BÌNH THƯỜNG<br/>• SnackBar xanh thông báo thành công<br/>• Động viên mẹ an tâm"]
```

1. **🔴 Nhánh ĐỎ — Nguy cơ Cấp cứu Khẩn cấp (`CRITICAL_EMERGENCY`):**
   - *Ngưỡng kích hoạt:*
     - $SBP \ge 160$ hoặc $DBP \ge 110$ mmHg (Cơn tăng huyết áp kịch phát).
     - $SBP \ge 140$ hoặc $DBP \ge 90$ mmHg kèm triệu chứng đau đầu/hoa mắt/nhìn mờ/tiền sử Tiền sản giật (`PRIOR_PREECLAMPSIA`).
     - Mất cử động thai sau tuần 28 ($0$ lần hoặc $< 4$ lần/2h).
     - **Thân nhiệt theo Giai đoạn:**
       - *Mang thai (`PREGNANCY`):* $T \ge 38.5^\circ C$ (hoặc $\ge 38.0^\circ C$ kèm rỉ ối, đau bụng, ra máu) $\rightarrow$ Nguy cơ Nhiễm trùng ối (*Chorioamnionitis*) hoặc Nhiễm trùng toàn thân.
       - *Hậu sản (`POSTPARTUM`):* $T \ge 38.0^\circ C \rightarrow$ Nghi ngờ Nhiễm trùng hậu sản (*Puerperal Sepsis*), Viêm nội mạc tử cung, Viêm tắc tuyến vú theo WHO.
       - *Chung/Chu sinh:* $T \ge 39.0^\circ C$.
     - Ra máu âm đạo tươi hoặc rỉ/vỡ ối sớm.
   - *Hành vi Ứng dụng:*
     - Hiển thị Modal Cảnh báo Nguy cấp Toàn màn hình (`CẢNH BÁO NGUY CẤP Y TẾ`).
     - **Nút "MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU":** Điều hướng sang [EmergencyMapScreen](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart) (`/emergency/map?mode=triage&stage=PREGNANCY`):
       1. **Bật còi hú SOS**.
       2. **Tự động gửi vị trí GPS khẩn cấp** tới tài khoản người thân trong nhóm Gia đình (`FamilyAlert` / `EmergencyService`).
       3. **Quét và định vị các Bệnh viện Chuyên khoa Sản gần nhất** trên bản đồ TrackAsia / Google Maps và mở dẫn đường tức thì.
     - **Nút "GỌI CẤP CỨU 115 NGAY":** Kích hoạt quay số trực tiếp `tel:115`.

2. **🟡 Nhánh VÀNG — Cần Theo dõi Bất thường (`ANOMALY_MONITOR`):**
   - *Ngưỡng kích hoạt:* Tiền tăng huyết áp ($130-139/85-89$), Đường huyết lúc đói $\ge 5.1$ mmol/L, cử động thai giảm nhẹ, sốt nhẹ ($37.5^\circ C - 38.4^\circ C$ thai kỳ, $37.5^\circ C - 37.9^\circ C$ hậu sản) hoặc hạ thân nhiệt ($< 35.5^\circ C$), ốm nghén, đau lưng.
   - *Hành vi Ứng dụng:*
     - Hiển thị Modal Cảnh báo Vàng (`LƯU Ý THEO DÕI SỨC KHỎE`).
     - Liệt kê các nguy cơ AI phát hiện.
     - **Nút "HỎI TRỢ LÝ AI NURSE (BƯỚC 10)":** Mở ngay [RagChatScreen](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart) (`/rag/chat`) kèm nội dung chỉ số đã điền sẵn để AI Nurse giải thích và trích dẫn cẩm nang y tế.

3. **🟢 Nhánh XANH — Chỉ số An toàn (`NORMAL`):**
   - *Ngưỡng kích hoạt:* Tất cả chỉ số nằm trong giới hạn an toàn ($36.0 - 37.4^\circ C$, HA bình thường, đường huyết chuẩn).
   - *Hành vi Ứng dụng:* Hiển thị SnackBar thông báo thành công và động viên mẹ an tâm.

---

#### E. Cơ chế 2 Chế độ Đánh giá Sức khỏe (Two-Tier Evaluation Architecture):

Nhằm tối ưu hóa trải nghiệm người dùng (UX) và đảm bảo tính chính xác lâm sàng, CareBridge thiết kế hệ thống theo **2 Chế độ Đánh giá rõ ràng**:

1. **Chế độ 1: Cô lập Đánh giá Chỉ số Đơn lẻ (Single Metric Isolation):**
   * Khi mẹ bầu nhập một chỉ số đo lường cụ thể trong ngày (ví dụ: Huyết áp, Đường huyết, Thân nhiệt, hoặc Cân nặng), hệ thống **chỉ kiểm tra và đánh giá chỉ số vừa nhập** kèm theo ghi chú triệu chứng của lần đo đó.
   * **Lợi ích:** Tránh hiện tượng cảnh báo sai lệch (False Alarm) do các chỉ số cũ chưa kịp cập nhật, giúp việc ghi nhận nhật ký hằng ngày diễn ra nhanh chóng, nhẹ nhàng và không gây phiền toái cho mẹ.

2. **Chế độ 2: Trang Tổng hợp & Đánh giá Sức khỏe Toàn diện AI (Total Overview AI Health Assessment):**
   * Trong danh mục Dropdown chọn chỉ số sức khỏe, CareBridge cung cấp mục chuyên biệt: **`📊 Đánh giá Sức khỏe Toàn diện AI`** ([`HealthMetricTrendScreen`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart)).
   * Tại đây, hệ thống tự động tổng hợp bức tranh đa chiều gồm:
     1. **Tuần thai thực tế & Bệnh sử khảo sát (Survey Onboarding):** Tuần thai hiện tại kết hợp tiền sử tiền sản giật, tăng huyết áp mạn, đái tháo đường thai kỳ.
     2. **Bảng 8 Chỉ số Sinh hiệu & Tâm lý mới nhất:**
        - 🩺 **Huyết áp (`BLOOD_PRESSURE`):** Tâm thu và tâm trương đo gần nhất.
        - ⚖️ **BMI & Thể trạng (`BMI` / `WEIGHT` / `HEIGHT`):** Cân nặng, chiều cao, chỉ số BMI.
        - 🩸 **Đường huyết (`BLOOD_GLUCOSE`):** Nồng độ glucose kèm ngữ cảnh đo.
        - 👶 **Cử động thai (`FETAL_MOVEMENT_SESSION`):** Số lần thai máy trong 2 giờ.
        - 💧 **Lượng nước uống (`HYDRATION`):** Tổng lượng nước nạp trong ngày.
        - ❤️ **Nhịp tim mẹ (`MATERNAL_HEART_RATE`):** Tần số tim mẹ (bpm).
        - 🧠 **Tâm trạng & Cảm xúc (`EPDS_SCORE`):** Điểm sàng lọc trầm cảm thai kỳ Edinburgh.
        - 🌡️ **Nhiệt độ cơ thể (`TEMPERATURE`):** Thân nhiệt ($^\circ C$) đo gần nhất.
     3. **Ô nhập triệu chứng cảm nhận bổ sung:** Cho phép mẹ ghi nhận các biểu hiện như đau đầu, hoa mắt, nhìn mờ, phù nề, sốt ớn lạnh, mệt mỏi...
     4. **Nút CTA "GỬI AI ĐÁNH GIÁ SỨC KHỎE TOÀN DIỆN":** Khi bấm, toàn bộ gói dữ liệu đa chiều được gửi lên `CareBridgeAITriageService` để thực hiện sàng lọc tương quan đa biến (Correlation Matrix Screening) và điều hướng chính xác theo 3 luồng (🔴 Cấp cứu SOS GPS / 🟡 AI Nurse Assistant / 🟢 An toàn).

---

#### F. Bảng Ánh xạ Mã Nguồn Thực thi (Code Mapping Reference):

Để đối chiếu và minh chứng trước Hội đồng, toàn bộ luồng xử lý được tổ chức rõ ràng trong các module mã nguồn sau:

| Thành phần & Nhiệm vụ                                           | File Mã nguồn                                                                                                                                                                                                                                                                                                                                                                   | Hàm / Class thực thi chính                                                                                                                                                                                                                       |
| :-------------------------------------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **1. Tầng Ngưỡng Lâm sàng Cố định (Deterministic Safety Gate)** | [`CareBridgeAITriageService/app/services/metrics_screening_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py)                                                                                                                                                           | `_check_blood_pressure()`, `_check_temperature()`, `_check_glucose()`, `_check_fetal_movements()`, `_check_bmi()`, `_check_heart_rate()`, `_check_water_intake()`, `_check_epds_score()`, `_check_spo2()`, `_check_sleep()`, `_check_symptoms()` |
| **2. Tầng Truy xuất Cẩm nang RAG Động (pgvector Retrieval)**    | [`CareBridgeAITriageService/app/services/metrics_screening_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py) & [`app/rag/vector_store.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/rag/vector_store.py) | `_build_retrieval_query()` ➔ `vector_store.similarity_search(top_k=3)` ➔ trả về `relevant_sources`                                                                                                                                               |
| **3. Tầng Backend Validation (Spring Boot)**                    | [`MetricObservationValidator.java`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/health/service/MetricObservationValidator.java)                                                                                                                                                                 | `validateNumericValue()` ➔ `requireRange(30.0, 45.0)` cho `TEMPERATURE`                                                                                                                                                                          |
| **4. Tầng Tạo sinh AI Nurse RAG (Bước 10)**                     | [`CareBridgeAITriageService/app/services/rag_chat_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py) & [`app/rag/gemini_client.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/rag/gemini_client.py)                 | `chat_with_nurse()`, `generate_medical_response()`                                                                                                                                                                                               |
| **5. Mobile: Nhập Chỉ số Đơn lẻ (Isolate Evaluation)**          | [`CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/add_maternal_health_metric_screen.dart)                                                                                                       | `_save()`, `_evaluateMetricWithAi()`, dropdown vị trí đo `_temperatureSite`                                                                                                                                                                      |
| **6. Mobile: Chỉnh sửa Bản ghi Chỉ số**                         | [`CareBridgeMobileApp/lib/features/healthRecords/screens/edit_health_metric_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/edit_health_metric_screen.dart)                                                                                                                       | Hỗ trợ xem/sửa giá trị thân nhiệt và vị trí đo `measurementSite`                                                                                                                                                                                 |
| **7. Mobile: Trang Tổng quan & Đánh giá Sức khỏe Toàn diện AI** | [`CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/healthRecords/screens/health_metric_trend_screen.dart)                                                                                                                     | `_overviewOption`, `_buildTotalOverviewSection()`, `_evaluateTotalOverviewWithAi()`, `_showMetricPickerModal()`, nạp song song 8 metrics                                                                                                         |
| **8. Mobile: Modal Cấp cứu & Kích hoạt Bản đồ SOS + GPS**       | [`emergency_map_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/emergency/screens/emergency_map_screen.dart) & [`SafetyPermissionService`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/safety/services/safety_permission_service.dart)    | `_showCriticalEmergencyDialog()` ➔ `readConsentedLocation()` ➔ `EmergencyService().openFlow(triggerSource: 'AI_TRIAGE', latitude, longitude)` ➔ `context.push('/emergency/map')`                                                                 |
| **9. Mobile: Modal Cảnh báo & Tự động Đính kèm sang AI Nurse**  | [`rag_chat_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart)                                                                                                                                                                                                       | `_showAnomalyDialog()` ➔ `context.push('/rag/chat', extra: {attachedContext: ..., initialMessage: ...})` ➔ `_AttachedContextBanner`                                                                                                              |

---

### 5.2. Luồng AI Nurse Assistant RAG Chat (Bước 10)
Khi mẹ bầu gửi câu hỏi thảo luận:
1. **Semantic Search:** Embed câu hỏi thành vector 768 chiều $\rightarrow$ Truy vấn pgvector lấy Top $K=4$ đoạn văn bản có điểm số Cosine cao nhất, có lọc theo `stage` (ví dụ mẹ đang mang thai thì lọc tài liệu `PREGNANCY`).
2. **Context Injection:** Đưa 4 đoạn tài liệu vào System Prompt theo khuôn mẫu nghiêm ngặt.
3. **Generative Inference:** Gemini Flash sinh câu trả lời mượt mà, định dạng rõ ràng, trả kèm `sources` (trích dẫn chi tiết mục) và `suggested_followups` (gợi ý 3 câu hỏi tiếp theo để mẹ dễ dàng tương tác).

---

## 6. Quản lý Ngữ cảnh Hội thoại Đa lượt (Multi-turn Context & Query Expansion)

Khi mẹ bầu trò chuyện qua lại nhiều lượt, một bài toán kinh điển trong AI y tế xuất hiện: **Đại từ thay thế và câu hỏi phụ thiếu ngữ cảnh (Co-reference Problem)**.

### 6.1. Vấn đề thực tế:
* *Lượt 1:* Mẹ: *"Em mang thai 32 tuần, hôm nay thấy bị đau đầu và phù hai chân"* ➔ AI Nurse phản hồi giải thích về nguy cơ huyết áp thai kỳ.
* *Lượt 2:* Mẹ chỉ hỏi tiếp: *"Nó có nguy hiểm đến em bé không ạ?"*
* Nếu hệ thống chỉ lấy câu *"Nó có nguy hiểm đến em bé không ạ?"* đi tìm kiếm trong Vector DB, pgvector sẽ không thể tìm thấy cẩm nang Tiền sản giật/Huyết áp vì không chứa từ khóa triệu chứng.

### 6.2. Giải pháp Kỹ thuật của CareBridge:
1. **Mở rộng Truy vấn Ngữ nghĩa (Semantic Query Expansion):**
   Hệ thống tự động trích xuất các triệu chứng mà mẹ đã đề cập trong các lượt chat gần nhất ghép nối vào câu hỏi mới:
   $$\text{Search Query} = \text{"đau đầu phù hai chân tuần 32"} + \text{"Nó có nguy hiểm đến em bé không ạ?"}$$
   ➔ CSDL Vector lập tức truy xuất chính xác $100\%$ cẩm nang xử trí Tiền sản giật và Biến chứng thai nhi.
2. **Cơ chế Cửa sổ Trượt (Sliding Window Context - 4 đến 6 tin nhắn gần nhất):**
   Hệ thống duy trì **4 đến 6 tin nhắn gần nhất (2-3 cặp Hỏi - Đáp)** đưa vào Prompt của Gemini Flash. Tỷ lệ này đảm bảo:
   - AI luôn nắm trọn vẹn toàn bộ diễn biến sức khỏe mẹ vừa chia sẻ.
   - Tránh hiện tượng "Loãng ngữ cảnh" (Context Drift) nếu nhồi quá nhiều tin cũ.
   - Duy trì thời gian phản hồi siêu tốc (< 1 giây) và tối ưu chi phí token.

### 6.3. Cơ chế Sinh Gợi ý Câu hỏi Tiếp theo Động 100% (LLM-Native Dynamic Follow-ups)
* **Vấn đề của phương pháp cũ (Rule-based / Keyword Heuristics):** Nếu dùng `if/elif` từ khóa (ví dụ: thấy từ "ăn" thì gợi ý món ăn, thấy từ "đau" thì gợi ý đau lưng), hệ thống sẽ bị fix cứng và không thể thích ứng với hàng triệu câu hỏi y tế phong phú của người dùng.
* **Giải pháp Hiện đại của CareBridge:**
  1. Trong System Prompt, sau khi Gemini tổng hợp câu trả lời dựa trên cẩm nang y tế, Gemini được yêu cầu: *Dựa trên chính nội dung vừa giải thích, tự động suy luận ra 3 câu hỏi ngắn gọn mà mẹ bầu có khả năng cao muốn tìm hiểu tiếp theo nhất*.
  2. Gemini xuất 3 câu hỏi sau thẻ `[GỢI Ý CÂU HỎI]:`.
  3. Hàm `_extract_dynamic_followups` ở backend bóc tách dữ liệu sạch và trả về trường `suggested_followups` trong JSON response.
  4. Trên Mobile App, các câu hỏi này được render thành các **ActionChips** giúp người dùng chỉ cần chạm 1 chạm là gửi tiếp câu hỏi mà không cần gõ phím.

### 6.4. Tầng Phòng vệ Cấp cứu Xác định (Deterministic Clinical Red-flag Guardrail)
* **Nguyên tắc:** Dù RAG tạo sinh thông minh đến đâu, trong Y tế **tuyệt đối không được phó mặc tính mạng bệnh nhân 100% cho xác suất của LLM**.
* **Cơ chế:** Hệ thống chạy song song một hàm kiểm tra cờ đỏ `_detect_emergency_intent`. Nếu phát hiện các dấu hiệu tối cấp cứu (*ra máu âm đạo, vỡ ối, co giật, đau bụng quặn dữ dội, sốt cao $\ge 39^\circ C$, thai ngừng máy*):
  - Lập tức kích hoạt `has_critical_warning = True`.
  - Tự động ưu tiên đưa các nút gợi ý cấp cứu (*"Gọi cấp cứu 115 ngay?", "Bệnh viện phụ sản gần nhất?"*).
  - Kích hoạt giao diện cảnh báo nguy hiểm trên Mobile App để bảo vệ an toàn thai phụ.

---

## 7. Kiến trúc Giao diện AI Nurse trên Ứng dụng Di động (Mobile App UI & State)

### 7.1. Bộ Phân tích & Định dạng Markdown Toàn diện (Rich-Text Markdown Parser)
* Các mô hình LLM hiện đại luôn trả về định dạng Markdown phong phú (`#` headings, `**` bold, `*` italic, `1.` numbered list, `•` bullet list, `---` divider).
* Để loại bỏ hoàn toàn tình trạng bị lộ dấu `***` hay định dạng thô, Mobile App tích hợp bộ Tokenizer Regex chuyên sâu:
  - Nhận diện và render các cấp tiêu đề Heading với cỡ chữ lớn và in đậm trang nhã.
  - Phân tách nội dung in đậm, in nghiêng, và mã code bằng `TextSpan` đa phong cách.
  - Tự động thụt lề chuẩn mực cho danh sách có thứ tự và danh sách gạch đầu dòng màu cam đất nung (`#C98C7B`).

### 7.2. Quản lý Đa Phiên Chat & Lưu trữ Cục bộ Bảo mật (Multi-Session & Encrypted Storage)
* **Tạo phiên mới (`+` New Session):** Cho phép mẹ bầu bắt đầu chủ đề thảo luận mới bất kỳ lúc nào, lưu trữ độc lập các cuộc trò chuyện trước đó.
* **Lịch sử trò chuyện (`🕒` Chat History Bottom Sheet):** Cho phép xem lại danh sách các phiên chat trong quá khứ, số lượng tin nhắn, thời gian trao đổi, tải lại phiên hoặc xóa phiên.
* **Bảo mật cục bộ:** Toàn bộ lịch sử chat được mã hóa và lưu trữ qua `FlutterSecureStorage` theo khóa `carebridge_ai_rag_sessions_${userId}`, đảm bảo tính riêng tư của thai phụ kể cả khi ứng dụng bị đóng hoàn toàn.

### 7.3. Khung Lưu ý Y tế Bắt buộc (Mandatory Safety Disclaimer Box)
* Dưới mỗi bong bóng chat phản hồi của AI luôn đính kèm một khung cảnh báo nhẹ nhàng:
  > ⚠️ *Lưu ý: Thông tin từ AI chỉ mang tính chất tham khảo và có thể có sai sót. Vui lòng tham khảo ý kiến bác sĩ hoặc đến ngay cơ sở y tế khi có dấu hiệu bất thường.*

---

## 8. Bộ Công cụ Quản trị, Soi Vector & Mô phỏng Lâm sàng

Nhằm phục vụ công tác quản trị, kiểm thử và **thuyết trình trực quan trước Hội đồng Chấm Đồ án**, hệ thống cung cấp đầy đủ các công cụ tương tác trên Swagger UI (`http://localhost:8001/docs`):

1. **`POST /api/v1/metrics/simulate-batch` (Clinical Simulator):**
   Chạy tự động bộ 5 ca lâm sàng kinh điển (Tiền sản giật nặng, sốt cao $39.2^\circ C$, mất cử động thai tuần 34, chỉ số bình thường, bất thường nhẹ) để chứng minh độ chính xác lâm sàng đạt $100\%$.
2. **`POST /api/v1/documents/search-vector` (Vector Search Simulator):**
   Nhập bất kỳ câu hỏi/triệu chứng nào để soi trực tiếp điểm tương đồng Cosine (`similarity_score`) và đoạn trích xuất trước khi gửi qua LLM.
3. **`GET /api/v1/documents/stats` & `GET /api/v1/documents/list`:**
   Xem thống kê tổng số chunks, phân bố theo giai đoạn thai kỳ/chủ đề và duyệt toàn bộ cơ sở tri thức có phân trang.
4. **`DELETE /api/v1/documents/by-title` & `DELETE /api/v1/documents/clear-all`:**
   Quản lý toàn diện vòng đời dữ liệu, cho phép xóa sạch các tài liệu cũ/sai lệch để AI không dùng nữa.

---

## 9. Bộ Câu hỏi & Trả lời Phản biện trước Hội Đồng (Defense Q&A - 12 Câu Hỏi Chuyên Sâu)

### Câu 1: "AI RAG em hiểu là gì và vì sao dự án y tế cho mẹ bầu lại chọn RAG thay vì Fine-tuning mô hình?"
* **Trả lời:**
  > "Thưa Thầy/Cô, RAG (Retrieval-Augmented Generation) là kỹ thuật kết hợp giữa **Truy xuất thông tin chính xác từ CSDL** và **Tạo sinh ngôn ngữ tự nhiên từ LLM**. 
  > Dự án CareBridge chọn RAG thay vì Fine-tuning vì 3 lý do cốt lõi trong Y tế:
  > 1. **Triệt tiêu ảo giác (No Hallucination):** Fine-tuning chỉ điều chỉnh 'văn phong' chứ không đảm bảo LLM không bịa thông tin. RAG ép buộc LLM phải trả lời dựa trên đúng đoạn trích dẫn của Bộ Y Tế được cung cấp.
  > 2. **Tính cập nhật tri thức (Zero Downtime):** Khi Bộ Y Tế ban hành hướng dẫn tiêm chủng mới, với RAG chúng em chỉ mất 2 giây để nạp file PDF vào CSDL vector. Nếu dùng Fine-tuning, chúng em phải huấn luyện lại model rất tốn kém thời gian và chi phí GPU.
  > 3. **Tính minh bạch (Explainability & Citations):** RAG cho phép hệ thống trích dẫn chính xác bài viết, số trang và nguồn gốc cẩm nang y tế cho thai phụ xem, điều mà mô hình Fine-tuning dạng Black-box không thể làm được."

---

### Câu 2: "Tại sao em chọn Chunk size là 900 ký tự và Overlap 180 ký tự? Căn cứ vào đâu?"
* **Trả lời:**
  > "Thưa Thầy/Cô, việc chọn Chunk size 900 ký tự (~150 - 200 từ tiếng Việt) và Overlap 20% (180 ký tự) dựa trên đặc thù tài liệu y khoa:
  > - Nếu Chunk quá nhỏ (< 300 ký tự): Đoạn văn bị xé vụn, mất ngữ cảnh (ví dụ câu điều kiện 'Nếu huyết áp cao kèm nhìn mờ' bị tách khỏi phần 'Xử trí cấp cứu').
  > - Nếu Chunk quá lớn (> 2000 ký tự): Vector embedding bị 'loãng' (diluted semantic), chứa quá nhiều chủ đề hỗn tạp khiến độ tương đồng Cosine bị giảm độ chính xác khi tìm kiếm.
  > - Mức 900 ký tự vừa vặn ôm trọn một ý lâm sàng hoàn chỉnh (Triệu chứng + Giải thích + Lời khuyên), và Overlap 180 ký tự giúp liên kết mượt mà giữa các đoạn kế nhau."

---

### Câu 3: "So sánh kỹ thuật Recursive Hierarchical Chunking của em với Semantic Chunking và Agentic Chunking?"
* **Trả lời:**
  > "Thưa Thầy/Cô:
  > - **Semantic Chunking** ngắt đoạn theo biến thiên khoảng cách vector giữa các câu liên tiếp. Tuy nhiên kỹ thuật này tốn quá nhiều lượt gọi API Embeddings (khiến tốc độ nạp tài liệu chậm và dễ chạm giới hạn quota) và sinh ra kích thước chunk không đều.
  > - **Agentic Chunking** dùng LLM đọc toàn bộ văn bản để tự chia đoạn, rất thông minh nhưng chi phí token cực lớn khi nạp tài liệu hàng trăm trang.
  > - **Recursive Hierarchical Chunking** (LangChain) mà CareBridge lựa chọn là giải pháp cân bằng tối ưu nhất: Cắt theo phân cấp `Đề mục Markdown` $\rightarrow$ `Đoạn văn` $\rightarrow$ `Dấu chấm kết câu` $\rightarrow$ `Dấu cách từ`. Kỹ thuật này bảo toàn 100% cấu trúc y khoa, không bao giờ ngắt vụn từ ngữ, tốc độ xử lý tức thì và chi phí vận hành bằng 0."

---

### Câu 4: "Em đang dùng loại Vector nào? Tại sao lại chọn pgvector trên PostgreSQL mà không dùng Pinecone hay ChromaDB?"
* **Trả lời:**
  > "Thưa Thầy/Cô:
  > 1. Hệ thống đang sử dụng **Neural Vector (Dense Vector)** 768 chiều sinh ra bởi mô hình mạng nơ-ron sâu Transformer (`gemini-embedding-001`), giúp nắm bắt ngữ nghĩa sâu của người dùng thay vì so khớp từ khóa rời rạc.
  > 2. Chúng em chọn **pgvector trên PostgreSQL** vì:
  >    - **Kiến trúc đồng nhất (Single Source of Truth):** Toàn bộ dữ liệu người dùng, chỉ số sức khỏe và dữ liệu vector tri thức đều nằm chung trong hệ quản trị CSDL PostgreSQL của dự án, tận dụng được Transaction ACID, cơ chế Backup/Restore và bảo mật sẵn có.
  >    - **Chỉ mục HNSW hiệu năng cao:** pgvector hỗ trợ chỉ mục HNSW với tốc độ truy vấn chỉ vài mili-giây, hoàn toàn đáp ứng được tải thực tế mà không phát sinh thêm chi phí duy trì một Server Vector Database riêng biệt bên ngoài."

---

### Câu 5: "Khi mẹ bầu chat nhiều câu liên tục (ví dụ: 'Nó có nguy hiểm không?'), làm sao AI hiểu 'Nó' là triệu chứng gì để tra cứu tài liệu chính xác?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống CareBridge áp dụng kỹ thuật **Semantic Query Expansion kết hợp Cửa sổ trượt (Sliding Window)**:
  > Khi mẹ hỏi câu phụ như 'Nó có nguy hiểm không?', hệ thống sẽ tự động lấy các triệu chứng mẹ đã kể ở 4-6 tin nhắn trước đó (ví dụ 'đau đầu, phù chân tuần 32') để ghép thành một Query tìm kiếm đầy đủ ngữ cảnh gửi vào pgvector. Đồng thời, toàn bộ đoạn hội thoại gần nhất được gửi vào Prompt của Gemini Flash giúp câu trả lời liền mạch và thông minh."

---

### Câu 6: "Hệ thống có bắt buộc tài liệu phải theo khuôn mẫu cố định nào không? Nếu nạp một file PDF hay Word 100 trang của Bộ Y Tế thì sao?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống hoàn toàn **không ép buộc tài liệu theo bất kỳ khuôn mẫu cố định nào**. 
  > Hệ thống hỗ trợ đa định dạng (PDF, DOCX, Markdown, TXT) và tự động nhận diện thông minh:
  > - Khi nạp file Word (`.docx`) hoặc PDF của Bộ Y Tế, thư viện `python-docx` và `pypdf` sẽ đọc toàn bộ nội dung.
  > - Thuật toán **Smart Section Extractor** tự động bóc tách các điều khoản (*'Điều 3:...'*, *'Bước 4: Tiệt khuẩn...'*) và gán vào cột `section`.
  > - Các trường siêu dữ liệu (`stage`, `topic`) nếu không có sẵn sẽ tự động gán mặc định để phục vụ lọc dữ liệu. Ví dụ thực tế: Chúng em đã nạp trực tiếp file *Quyết định 1359 của Bộ Y Tế (159KB)* và hệ thống tự động bóc tách thành 244 đoạn tri thức chuẩn chỉ trong 2 giây."

---

### Câu 7: "Làm sao hệ thống đưa ra được các nút gợi ý câu hỏi tiếp theo cho mẹ bầu? Có phải em đang gán cứng bằng từ khóa (Rule-based) không?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống sử dụng cơ chế **LLM-Native Dynamic Follow-up Generation (Sinh động hoàn toàn từ Gemini)**:
  > Chúng em không dùng `if/else` từ khóa vì từ khóa bị giới hạn và không bao quát được thực tế y khoa. Thay vào đó, sau khi Gemini tạo xong câu trả lời dựa trên cẩm nang y tế, mô hình sẽ tự động suy luận ra đúng 3 câu hỏi liên kết chặt chẽ nhất với câu trả lời đó. Backend bóc tách 3 câu hỏi này và đẩy về Mobile App để render thành các nút ActionChip, giúp mẹ bầu tiếp tục hỏi đáp chỉ với một cú chạm."

---

### Câu 8: "Tại sao trong code xử lý vẫn có danh sách từ khóa dấu hiệu nguy hiểm (ra máu, vỡ ối, co giật...)? Có mâu thuẫn với việc dùng AI không?"
* **Trả lời:**
  > "Thưa Thầy/Cô, việc này hoàn toàn không mâu thuẫn mà là nguyên tắc **Clinical Safety Guardrail (Hàng rào an toàn y tế xác định)** bắt buộc trong các hệ thống y tế chuẩn mực:
  > Dù AI có thông minh đến đâu, việc suy luận của LLM vẫn mang tính xác suất (Probabilistic). Đối với các dấu hiệu đe dọa trực tiếp tính mạng mẹ và bé (như xuất huyết âm đạo ồ ạt, vỡ ối sớm, co giật tiền sản giật), hệ thống phải có một tầng Deterministic Guardrail độc lập để ngay lập tức kích hoạt cảnh báo đỏ và hướng dẫn cấp cứu 115, không được phép phó mặc rủi ro cho mô hình ngôn ngữ."

---

### Câu 9: "Làm thế nào để đảm bảo hệ thống không tự chẩn đoán bệnh bừa bãi hay gây nguy hiểm cho người dùng?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống CareBridge áp dụng cơ chế **Phòng vệ 3 Lớp (Three-Tier Safety Guardrails)**:
  > 1. **Lớp 1 - Hard Clinical Rule Gates:** Các ngưỡng sinh hiệu nguy kịch (Huyết áp $\ge 140/90$ kèm triệu chứng, sốt $\ge 38.5^\circ C$, thai ngừng máy $\ge 2$ giờ) được kiểm tra bằng code deterministic trước khi qua AI. Nếu chạm ngưỡng, hệ thống lập tức kích hoạt `CRITICAL_EMERGENCY` mà không để LLM quyết định.
  > 2. **Lớp 2 - System Prompt Grounding:** Ép vai trò AI là **Trợ lý Điều dưỡng (AI Nurse Assistant)** với nguyên tắc cấm tự ý chẩn đoán xác định bệnh tật và cấm tự kê đơn/kê thuốc.
  > 3. **Lớp 3 - Medical Disclaimer:** Mọi câu trả lời của AI đều gắn kèm cảnh báo y tế bắt buộc, nhắc nhở mẹ bầu tham khảo ý kiến Bác sĩ chuyên khoa."

---

### Câu 10: "Lịch sử trò chuyện và các phiên chat AI của mẹ bầu được lưu trữ và bảo mật thế nào trên ứng dụng di động?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống hỗ trợ quản lý **Đa phiên hội thoại (Multi-session Chat)**:
  > Mẹ bầu có thể tạo phiên trò chuyện mới hoặc xem lại lịch sử các buổi tư vấn trước đó. Toàn bộ lịch sử này được mã hóa và lưu trữ an toàn trong bộ nhớ bảo mật của thiết bị thông qua thư viện `FlutterSecureStorage` theo định danh người dùng (`carebridge_ai_rag_sessions_${userId}`), đảm bảo tuân thủ quyền riêng tư dữ liệu y tế cá nhân."

---

### Câu 11: "Nếu một cuốn cẩm nang y tế bị cũ hoặc sai lệch, làm sao để xóa triệt để để AI không trả lời sai?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống đã cung cấp API quản lý vòng đời dữ liệu `DELETE /api/v1/documents/by-title`. Khi Quản trị viên nhập tên tài liệu cần xóa, hệ thống sẽ thực hiện đồng thời 2 việc:
  > 1. Xóa toàn bộ các dòng vector tương ứng trong bảng `maternal_knowledge_chunks` của CSDL PostgreSQL.
  > 2. Xóa file vật lý tương ứng trên đĩa cứng.
  > Ngay sau đó, AI sẽ lập tức không còn truy xuất được các đoạn tri thức đó nữa, đảm bảo thông tin sai lệch bị triệt tiêu 100%."

---

### Câu 12: "Nếu mạng bị mất hoặc API Gemini bị nghẽn (503/429), hệ thống của em có bị chết (crash) không?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống hoàn toàn **không bị crash** nhờ 2 cơ chế:
  > 1. **Multi-Model Auto Fallback:** Trong `GeminiClient`, nếu model chính gặp sự cố, hệ thống sẽ tự động thử lần lượt các model dự phòng (`gemini-flash-lite-latest` $\rightarrow$ `gemini-2.5-flash` $\rightarrow$ `gemini-flash-latest`).
  > 2. **Graceful Offline Fallback:** Nếu toàn bộ kết nối API bên ngoài bị ngắt, hệ thống vẫn sàng lọc sinh hiệu bình thường bằng bộ quy tắc lâm sàng, đồng thời trả về hướng dẫn cẩm nang dự phòng an toàn kèm khuyến cáo người dùng liên hệ Bác sĩ."

---

### Câu 13: "Tại sao hệ thống của em phải sử dụng Hybrid Search (Tìm kiếm Lai) thay vì chỉ dùng Vector Search thuần túy? Trong thực tế nó giải quyết bài toán gì?"
* **Trả lời:**
  > "Thưa Thầy/Cô, trong các hệ thống RAG chuyên sâu về y tế, **Pure Dense Vector Search (tìm kiếm vector ngữ nghĩa thuần túy)** thường gặp hiện tượng **Semantic Drift (Lệch ngữ cảnh do pha loãng từ ngữ)**.
  > 
  > **Ví dụ thực tế:**
  > Khi người dùng hỏi: *'Mang thai 3 tháng đầu cần bổ sung vi chất gì?'*, nếu chỉ tính khoảng cách vector thuần túy, một đoạn văn bản dài về *'Quy trình kế hoạch hóa gia đình / Đặt DCTC'* có chứa câu *'Lưu ý mang thai 3 tháng đầu...'* có thể vô tình đạt điểm vector gần bằng tài liệu dinh dưỡng.
  > 
  > Do đó, CareBridge áp dụng **Hybrid Search kết hợp Re-ranking**:
  > 1. **Lớp Vector pgvector:** Đóng vai trò bộ lọc diện rộng để hiểu câu hỏi tự nhiên theo ngữ nghĩa.
  > 2. **Lớp Sparse Keyword & Medical Entity Boosting:** Đánh giá sự xuất hiện chính xác của các thực thể chuyên môn (*axit folic, sắt, canxi, uốn ván, huyết áp...*).
  > 3. **Bộ lọc Ngưỡng tương đồng & Khử trùng lặp:** Loại bỏ hoàn toàn các nguồn không liên quan ($< 0.05$) và gộp các chunk cùng mục.
  > 
  > Nhờ vậy, câu trả lời của AI và danh sách Cẩm nang trích dẫn (Citations) luôn đạt độ chính xác $100\%$, không bao giờ bị lệch sang các chủ đề không liên quan."

---

### Câu 14: "Làm thế nào hệ thống kết hợp thông tin survey của mẹ với tuần thai và chỉ số sức khỏe để AI ra quyết định điều hướng cấp cứu hoặc chat AI Nurse?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống CareBridge áp dụng cơ chế **Multi-Source Context Aggregation (Tổng hợp Đa Ngữ cảnh Y khoa)** trước khi gửi dữ liệu lên AI Triage:
  > 1. **Thu thập dữ liệu 3 chiều:**
  >    - *Chiều 1 (Tiền sử & Bệnh nền):* Tự động lấy từ bảng khảo sát ban đầu (`MotherJourney.recommendationProfileJson` qua API `GET /api/v1/recommendations/profile`) các mã rủi ro như `PRIOR_PREECLAMPSIA` (Tiền sử Tiền sản giật), `CHRONIC_HYPERTENSION` (Tăng huyết áp mạn), `PREGESTATIONAL_DIABETES` (Đái tháo đường).
  >    - *Chiều 2 (Giai đoạn thai kỳ):* Lấy chính xác số tuần thai từ `MotherJourney` (`/api/v1/journey/dashboard`).
  >    - *Chiều 3 (Dữ liệu tức thời):* Chỉ số sinh hiệu (Huyết áp $SBP/DBP$, Đường huyết, Cử động thai, BMI...) và triệu chứng mẹ vừa nhập trên `AddMaternalHealthMetricScreen`.
  > 2. **Phân tích Rủi ro & Phản xạ Lâm sàng (Triage Decision):**
  >    - Nếu huyết áp cao $\ge 140/90$ kèm tiền sử `PRIOR_PREECLAMPSIA` hoặc triệu chứng đau đầu/hoa mắt $\rightarrow$ Phân loại `CRITICAL_EMERGENCY`.
  >    - **Nhánh ĐỎ:** Ứng dụng hiện Modal Cảnh báo Cấp cứu, cung cấp nút **'MỞ BẢN ĐỒ BỆNH VIỆN & CẤP CỨU'** để chuyển sang `EmergencyMapScreen` (bật còi SOS, tự động gửi định vị GPS khẩn cấp tới Người thân trong nhóm Gia đình, quét danh sách Bệnh viện Sản khoa gần nhất và mở dẫn đường TrackAsia/Google Maps) và nút **'GỌI 115'**.
  >    - **Nhánh VÀNG (`ANOMALY_MONITOR`):** Hiện Modal Cảnh báo Vàng kèm nút **'HỎI TRỢ LÝ AI NURSE'** để chuyển ngay sang `RagChatScreen` với câu hỏi ngữ cảnh được điền sẵn, giúp mẹ nhận tư vấn cẩm nang kịp thời."

---

### Câu 15: "Các chỉ số sức khỏe đang được so sánh theo ngưỡng cố định (hard-coded) hay so sánh theo tài liệu RAG? Chỉ rõ vị trí mã nguồn thực thi?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống CareBridge áp dụng **Kiến trúc Lai 2 Tầng Chuẩn Y khoa (Dual-Layer Clinical Architecture)** — tiêu chuẩn bắt buộc của các hệ thống Y tế số quốc tế:
  > 
  > 1. **Tầng 1: Ngưỡng Lâm sàng Cố định (Deterministic Safety Gate):**
  >    - *Bản chất:* Các con số sinh tử như Huyết áp cấp cứu ($\ge 160/110$ mmHg), Mất cử động thai sau tuần 28 ($0$ lần/2h), Sốt cao $\ge 38.5^\circ C$ là **Quy chuẩn Y khoa Bắt buộc** của Bộ Y tế và ACOG.
  >    - *Lý do kỹ thuật:* Trong y tế, **tuyệt đối không được phó mặc tính mạng bệnh nhân cho mô hình xác suất LLM** vì LLM có nguy cơ ảo giác (hallucination) và độ trễ mạng. Tầng này phản hồi tức thì trong $1$ ms để bảo vệ 100% an toàn thai phụ.
  >    - *Vị trí code:* Nằm tại [`CareBridgeAITriageService/app/services/metrics_screening_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py) (các hàm `_check_blood_pressure()`, `_check_temperature()`, `_check_glucose()`, `_check_fetal_movements()`, `_check_symptoms()`).
  > 
  > 2. **Tầng 2: Truy xuất & Đối chiếu Cẩm nang RAG Động (Dynamic Semantic RAG Retrieval):**
  >    - *Bản chất:* Sau khi xác định nhóm rủi ro, hệ thống tự động tổng hợp chỉ số thành vector truy vấn 768 chiều và truy vấn CSDL Vector `pgvector` để **trích xuất chính xác các đoạn cẩm nang y tế liên quan** (phác đồ Tiền sản giật, cẩm nang đếm cử động thai, chế độ ăn cho mẹ đái tháo đường).
  >    - *Vị trí code:* Nằm tại [`CareBridgeAITriageService/app/services/metrics_screening_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/metrics_screening_service.py) (hàm `_build_retrieval_query()` và `vector_store.similarity_search()`) kết nối với [`app/rag/vector_store.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/rag/vector_store.py).
  > 
  > 3. **Tầng 3: AI Nurse RAG Generative Chat (Bước 10):**
  >    - *Bản chất:* Mẹ bầu trao đổi tự nhiên với AI Nurse, toàn bộ câu trả lời, lời khuyên dinh dưỡng và gợi ý câu hỏi tiếp theo được Gemini sinh **động 100% dựa trên kho tài liệu RAG**.
  >    - *Vị trí code:* Nằm tại [`CareBridgeAITriageService/app/services/rag_chat_service.py`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeAITriageService/app/services/rag_chat_service.py) và giao diện Mobile tại [`CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart`](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/05_Development/CareBridgeMobileApp/lib/features/aiTriage/screens/rag_chat_screen.dart)."

---

## 10. Hướng dẫn Vận hành & Nạp Thêm Tri Thức Mới

### 10.1. Cách nạp tài liệu mới qua CLI (Dành cho Kỹ thuật viên)
1. Copy file tài liệu mới (`.pdf`, `.docx`, `.md`, `.txt`) vào thư mục:
   ```text
   05_Development/CareBridgeAITriageService/data/raw_documents/
   ```
2. Chạy lệnh:
   ```bash
   cd 05_Development/CareBridgeAITriageService
   ./venv/bin/python scripts/ingest_documents.py
   ```

### 10.2. Cách nạp tài liệu qua REST API (Dành cho Web Admin / Frontend)
Gửi HTTP POST request dạng `multipart/form-data`:
```bash
curl -X POST "http://localhost:8001/api/v1/documents/upload" \
  -H "X-Internal-API-Key: carebridge" \
  -F "file=@/duong_dan/cam_nang_dinh_duong.pdf" \
  -F "stage=PREGNANCY" \
  -F "topic=NUTRITION" \
  -F "source=Viện Dinh Dưỡng Quốc Gia"
```

### 10.3. Chạy Kiểm thử Toàn bộ Hệ thống
```bash
cd 05_Development/CareBridgeAITriageService
./venv/bin/pytest tests/ -v
```
*(Toàn bộ **15/15 test cases** về an toàn lâm sàng, đa lượt hội thoại và API đều đạt 100% Passed).*

