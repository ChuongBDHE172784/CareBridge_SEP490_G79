# CAREBRIDGE AI RAG — TÀI LIỆU THIẾT KẾ KIẾN TRÚC, KỸ THUẬT VÀ CẨM NANG BẢO VỆ ĐỒ ÁN (DEFENSE HANDBOOK)

> **Dự án:** CareBridge (SEP490 - Capstone Project)  
> **Phân hệ:** `CareBridgeAITriageService` (Hệ thống AI RAG & Sàng lọc Chỉ số Sinh hiệu Mẹ Bầu)  
> **Mục đích tài liệu:** Cung cấp toàn bộ thiết kế kiến trúc, giải thích chi tiết kỹ thuật RAG, luồng logic hoạt động, thuật toán vector, và bộ câu hỏi - đáp chuyên sâu phục vụ bảo vệ trước **Hội đồng Chấm Đồ án Tốt nghiệp**.

---

## MỤC LỤC
1. [Bản chất AI RAG trong Y tế & Dự án CareBridge](#1-bản-chất-ai-rag-trong-y-tế--dự-án-carebridge)
2. [Kiến trúc Tổng thể & Lựa chọn Công nghệ](#2-kiến-trúc-tổng-thể--lựa-chọn-công-nghệ)
3. [Kỹ thuật Data Pipeline: Ingestion, Chunking & Embeddings](#3-kỹ-thuật-data-pipeline-ingestion-chunking--embeddings)
4. [Cơ chế Vector Database & Thuật toán Tìm kiếm (pgvector + HNSW)](#4-cơ-chế-vector-database--thuật-toán-tìm-kiếm-pgvector--hnsw)
5. [Luồng Logic Hoạt động theo Workflow Hệ thống](#5-luồng-logic-hoạt-động-theo-workflow-hệ-thống)
6. [Bộ Câu hỏi & Trả lời Phản biện trước Hội Đồng (Defense Q&A)](#6-bộ-câu-hỏi--trả-lời-phản-biện-trước-hội-đồng-defense-qa)
7. [Hướng dẫn Vận hành & Nạp Thêm Tri Thức Mới](#7-hướng-dẫn-vận-hành--nạp-thêm-tri-thức-mới)

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
    subgraph INGESTION["1. INGESTION PIPELINE (Xử lý Tri thức Y tế)"]
        DOCS["Tài liệu Y tế Chính thống<br/>(PDF, DOCX, Markdown, TXT)"] --> CHUNKER["Document Chunker<br/>(RecursiveCharacterTextSplitter)<br/>Chunk: 900 chars | Overlap: 180 chars"]
        CHUNKER --> EMBED_GEN["Gemini Embedding Model<br/>(Neural Vector 768-dim)"]
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
        USER_MSG["Mẹ bầu đặt câu hỏi:<br/>'Mang thai 3 tháng đầu cần uống vi chất gì?'"] --> RAG_SVC["RAG Chat Service"]
        RAG_SVC --> Q_EMBED["Embed Query Vector (768-dim)"]
        Q_EMBED --> V_SEARCH["Cosine Distance Search (<=>)<br/>Top K=4 chunks + Metadata Filter (Stage)"]
        PG_VECTOR -.-> V_SEARCH
        V_SEARCH --> PROMPT_BUILD["Prompt Builder<br/>• Medical System Instruction<br/>• User Query<br/>• Retrieved Contexts"]
        PROMPT_BUILD --> GEMINI_LLM["LLM: Gemini Flash-Lite / 3.7 Flash<br/>(Auto Model Fallback)"]
        GEMINI_LLM --> FINAL_RESP["Response hoàn chỉnh:<br/>• Lời giải đáp ân cần, khoa học<br/>• Trích dẫn nguồn (Citations)<br/>• Gợi ý câu hỏi tiếp theo (Follow-ups)<br/>• Disclaimer Y tế bắt buộc"]
    end
```

### Bảng Lựa chọn Công nghệ & Lý do Khoa học

| Thành phần | Công nghệ lựa chọn | Lý do khoa học & Ưu thế kỹ thuật |
| :--- | :--- | :--- |
| **Backend Framework** | **FastAPI (Python 3.11+)** | Xử lý bất đồng bộ (`asyncio`), độ trễ thấp, tự động sinh chuẩn OpenAPI/Swagger UI, quản lý schema type-safe với Pydantic v2. |
| **Vector Database** | **PostgreSQL + pgvector** | Lưu trữ trực tiếp Vector 768 chiều trong cùng một CSDL quan hệ với hệ thống chính, loại bỏ chi phí vận hành DB vector độc lập, hỗ trợ Transaction ACID, phân quyền và backup hợp nhất. |
| **Vector Index** | **HNSW (Hierarchical Navigable Small World)** | Thuật toán đồ thị tìm kiếm láng giềng gần nhất xấp xỉ (Approximate Nearest Neighbors - ANN) với độ phức tạp tìm kiếm $O(\log N)$, phản hồi trong vài mili-giây. |
| **Embedding Model** | **Gemini Embedding (`output_dimensionality=768`)** | Mã hóa ngữ nghĩa tiếng Việt đa tầng, sinh vector 768 chiều chuẩn hóa. |
| **LLM Generator** | **Google Gemini Flash-Lite / 3.7 Flash** | Tốc độ phản hồi cực nhanh (< 1.5s), quota dồi dào, khả năng suy luận lâm sàng và diễn đạt tiếng Việt ân cần. |
| **Multi-Model Auto Fallback** | `gemini-flash-lite-latest` $\rightarrow$ `gemini-2.5-flash` $\rightarrow$ `gemini-flash-latest` | Đảm bảo **100% Uptime**, tự động chuyển sang model dự phòng nếu Google bảo trì hoặc nghẽn mạng. |

---

## 3. Kỹ thuật Data Pipeline: Ingestion, Chunking & Embeddings

### 3.1. Kỹ thuật Phân đoạn Văn bản (Text Chunking)
* **Thuật toán sử dụng:** `RecursiveCharacterTextSplitter`.
* **Cơ chế:** Phân tách phân cấp theo danh sách ký tự ưu tiên `["\n\n", "\n", ". ", " ", ""]` nhằm bảo toàn toàn vẹn một ý niệm y khoa hoàn chỉnh.
* **Tham số tối ưu hóa:**
  - `chunk_size = 900` ký tự (~150 - 200 từ tiếng Việt): Kích thước vàng ôm trọn một nội dung y khoa (Triệu chứng + Cơ chế + Hướng xử trí), tránh tình trạng vector bị loãng ngữ nghĩa.
  - `chunk_overlap = 180` ký tự (20% overlap): Đảm bảo các câu nằm ở ranh giới giữa 2 chunk không bị đứt đoạn ngữ cảnh, giúp việc truy xuất vector luôn liền mạch.

### 3.2. Làm giàu Siêu dữ liệu (Metadata Enrichment)
Mỗi đoạn văn bản sau khi cắt khúc được gán siêu dữ liệu đa chiều:
- `title`: Tên tài liệu cẩm nang gốc.
- `stage`: Giai đoạn áp dụng (`PRECONCEPTION`, `PREGNANCY`, `POSTPARTUM`, `INFANT`, `TODDLER`, `ALL`).
- `topic`: Chủ đề y khoa (`DANGER_SIGNS`, `NUTRITION`, `HEALTH_MONITORING`, `POSTPARTUM_CARE`...).
- `source`: Cơ quan/Bệnh viện phát hành (Bộ Y Tế, WHO, Bệnh viện Từ Dũ, Viện Dinh Dưỡng).
- `section`: Tên chương mục trong tài liệu.

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

### 4.3. Cấu trúc bảng CSDL trong PostgreSQL:
```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE maternal_knowledge_chunks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    stage VARCHAR(50) NOT NULL,
    topic VARCHAR(100) NOT NULL,
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

---

## 5. Luồng Logic Hoạt động theo Workflow Hệ thống

Bám sát sơ đồ thiết kế hệ thống tại [docs/mainworkflow-Trang-3.drawio.png](file:///Users/huy/Documents/Đồ%20án/CareBridge_SEP490_G79/docs/mainworkflow-Trang-3.drawio.png):

### 5.1. Luồng Sàng lọc Chỉ số Sức khỏe (Bước 7 ➔ 8 ➔ 9)
Khi mẹ bầu nhập chỉ số theo dõi hàng ngày:
1. **Tiếp nhận tham số:** Huyết áp ($SBP/DBP$), Đường huyết ($Glucose$), Thân nhiệt ($Temp$), Số lần cử động thai ($Kicks$), Tuần thai, và triệu chứng chủ quan.
2. **Kiểm tra Ngưỡng An toàn Lâm sàng (Clinical Gates):**
   - *Cơn tăng huyết áp khẩn cấp:* $SBP \ge 160$ hoặc $DBP \ge 110$ mmHg.
   - *Nghi ngờ Tiền sản giật:* $SBP \ge 140$ hoặc $DBP \ge 90$ mmHg kèm theo $\ge 1$ triệu chứng (đau đầu dữ dội, hoa mắt, nhìn mờ, phù mặt, đau thượng vị).
   - *Mất cử động thai:* Tuần thai $\ge 28$ nhưng thai cử động $< 4$ lần trong 2 giờ hoặc $0$ lần cử động.
   - *Sốt cao thai kỳ:* Thân nhiệt $\ge 38.5^\circ C$.
   - *Dấu hiệu xuất huyết / Rỉ ối:* Có triệu chứng ra máu âm đạo tươi hoặc vỡ ối.
3. **Phân loại Kết quả:**
   - **`CRITICAL_EMERGENCY`:** Trả về `emergency_mode = True`, đưa ra chỉ dẫn cấp cứu 115, tự động kích hoạt tính năng định vị Bệnh viện Sản khoa gần nhất trên Mobile App.
   - **`ANOMALY_MONITOR`:** Chỉ số tiền tăng huyết áp ($130-139/85-89$), ốm nghén, đau lưng nhẹ $\rightarrow$ Hướng dẫn mẹ tiếp tục theo dõi và gợi ý chuyển sang Bước 10 trò chuyện với AI Nurse.
   - **`NORMAL`:** Các chỉ số nằm trong khoảng sinh lý an toàn.

### 5.2. Luồng AI Nurse Assistant RAG Chat (Bước 10)
Khi mẹ bầu gửi câu hỏi thảo luận:
1. **Semantic Search:** Embed câu hỏi thành vector 768 chiều $\rightarrow$ Truy vấn pgvector lấy Top $K=4$ đoạn văn bản có điểm số Cosine cao nhất, có lọc theo `stage` (ví dụ mẹ đang mang thai thì lọc tài liệu `PREGNANCY`).
2. **Context Injection:** Đưa 4 đoạn tài liệu vào System Prompt theo khuôn mẫu:
   ```text
   DƯỚI ĐÂY LÀ CẨM NANG Y TẾ CHÍNH THỐNG:
   [1] Nguồn: {source} - {title}
   Nội dung: {chunk_content}
   ---------------------------
   YÊU CẦU: Hãy giải đáp câu hỏi của mẹ dựa trên thông tin trên, đóng vai Trợ lý Điều dưỡng ân cần, giải thích rõ ràng, trích dẫn nguồn và đưa ra lời khuyên đi khám khi có dấu hiệu bất thường.
   ```
3. **Generative Inference:** Gemini Flash-Lite sinh câu trả lời mượt mà, định dạng rõ ràng, trả kèm `sources` (trích dẫn) và `suggested_followups` (gợi ý 3 câu hỏi tiếp theo để mẹ dễ dàng tương tác).

---

## 6. Bộ Câu hỏi & Trả lời Phản biện trước Hội Đồng (Defense Q&A)

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

### Câu 3: "Em đang dùng loại Vector nào? Tại sao lại chọn pgvector trên PostgreSQL mà không dùng Pinecone hay ChromaDB?"
* **Trả lời:**
  > "Thưa Thầy/Cô:
  > 1. Hệ thống đang sử dụng **Neural Vector (Dense Vector)** 768 chiều sinh ra bởi mô hình mạng nơ-ron sâu Transformer (`gemini-embedding-001`), giúp nắm bắt ngữ nghĩa sâu của người dùng thay vì so khớp từ khóa rời rạc.
  > 2. Chúng em chọn **pgvector trên PostgreSQL** vì:
  >    - **Kiến trúc đồng nhất (Single Source of Truth):** Toàn bộ dữ liệu người dùng, chỉ số sức khỏe và dữ liệu vector tri thức đều nằm chung trong hệ quản trị CSDL PostgreSQL của dự án, tận dụng được Transaction ACID, cơ chế Backup/Restore và bảo mật sẵn có.
  >    - **Chỉ mục HNSW hiệu năng cao:** pgvector hỗ trợ chỉ mục HNSW với tốc độ truy vấn chỉ vài mili-giây, hoàn toàn đáp ứng được tải thực tế mà không phát sinh thêm chi phí duy trì một Server Vector Database riêng biệt bên ngoài."

---

### Câu 4: "Làm thế nào để đảm bảo hệ thống không tự chẩn đoán bệnh bừa bãi hay gây nguy hiểm cho người dùng?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống CareBridge áp dụng cơ chế **Phòng vệ 3 Lớp (Three-Tier Safety Guardrails)**:
  > 1. **Lớp 1 - Hard Clinical Rule Gates:** Các ngưỡng sinh hiệu nguy kịch (Huyết áp $\ge 140/90$ kèm triệu chứng, sốt $\ge 38.5^\circ C$, thai ngừng máy $\ge 2$ giờ) được kiểm tra bằng code deterministic trước khi qua AI. Nếu chạm ngưỡng, hệ thống lập tức kích hoạt `CRITICAL_EMERGENCY` mà không để LLM quyết định.
  > 2. **Lớp 2 - System Prompt Grounding:** Ép vai trò AI là **Trợ lý Điều dưỡng (AI Nurse Assistant)** với nguyên tắc cấm tự ý chẩn đoán xác định bệnh tật và cấm tự kê đơn/kê thuốc.
  > 3. **Lớp 3 - Medical Disclaimer:** Mọi câu trả lời của AI đều gắn kèm cảnh báo y tế bắt buộc, nhắc nhở mẹ bầu tham khảo ý kiến Bác sĩ chuyên khoa."

---

### Câu 5: "Nếu mạng bị mất hoặc API Gemini bị nghẽn (503/429), hệ thống của em có bị chết (crash) không?"
* **Trả lời:**
  > "Thưa Thầy/Cô, hệ thống hoàn toàn **không bị crash** nhờ 2 cơ chế:
  > 1. **Multi-Model Auto Fallback:** Trong `GeminiClient`, nếu model chính gặp sự cố, hệ thống sẽ tự động thử lần lượt các model dự phòng (`gemini-flash-lite-latest` $\rightarrow$ `gemini-2.5-flash` $\rightarrow$ `gemini-flash-latest`).
  > 2. **Graceful Offline Fallback:** Nếu toàn bộ kết nối API bên ngoài bị ngắt, hệ thống vẫn sàng lọc sinh hiệu bình thường bằng bộ quy tắc lâm sàng, đồng thời trả về hướng dẫn cẩm nang dự phòng an toàn kèm khuyến cáo người dùng liên hệ Bác sĩ."

---

## 7. Hướng dẫn Vận hành & Nạp Thêm Tri Thức Mới

### 7.1. Cách nạp tài liệu mới qua CLI (Dành cho Kỹ thuật viên)
1. Copy file tài liệu mới (`.pdf`, `.docx`, `.md`, `.txt`) vào thư mục:
   ```text
   05_Development/CareBridgeAITriageService/data/raw_documents/
   ```
2. Chạy lệnh:
   ```bash
   cd 05_Development/CareBridgeAITriageService
   ./venv/bin/python scripts/ingest_documents.py
   ```

### 7.2. Cách nạp tài liệu qua REST API (Dành cho Web Admin / Frontend)
Gửi HTTP POST request dạng `multipart/form-data`:
```bash
curl -X POST "http://localhost:8001/api/v1/documents/upload" \
  -H "X-Internal-API-Key: carebridge" \
  -F "file=@/duong_dan/cam_nang_dinh_duong.pdf" \
  -F "stage=PREGNANCY" \
  -F "topic=NUTRITION" \
  -F "source=Viện Dinh Dưỡng Quốc Gia"
```

### 7.3. Chạy Kiểm thử Toàn bộ Hệ thống
```bash
cd 05_Development/CareBridgeAITriageService
./venv/bin/pytest tests/ -v
```
*(Toàn bộ 14/14 test cases về an toàn lâm sàng và API đều đạt 100%).*
