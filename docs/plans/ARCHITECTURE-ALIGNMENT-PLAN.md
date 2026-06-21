# Architecture Alignment Plan
# System Architecture 2 (drawio) ↔ project-structure-design.md

Source: adversarial review dated 2026-06-19
Status: In progress

---

## Completed

- [x] **Xóa MongoDB** khỏi `project-structure-design.md` — thay bằng PostgreSQL JSONB. Ảnh hưởng: Section 2, 3, 12, 16.

---

## Pending Fixes

### FIX-01 — CompreFace FaceID: xác định scope

**Vấn đề:** Drawio có service box "CompreFace FaceID API" (face enrollment / face verification). `project-structure-design.md` không có bất kỳ mention nào.

**Cần quyết định:**
- Nếu FaceID **không phải** feature của CareBridge: xóa CompreFace khỏi drawio (System Architecture 2).
- Nếu FaceID **là** feature: bổ sung vào md — thêm package `integration/compreFace/` trong Section 2 integration list, thêm vào Section 9 Domain Module Map, thêm vào Section 12 Contracts.

**Tác động:** Drawio (xóa 1 service box + 1 edge) hoặc md (thêm ~3 sections).

---

### FIX-02 — SMTP/Email: bổ sung vào md integration list

**Vấn đề:** Drawio có 2 SMTP edges (OTP email + email notification). md không có SMTP trong integration list, không có contract file.

**Việc cần làm trong `project-structure-design.md`:**
- Section 2: thêm `- SMTP email service for OTP and notification delivery.` vào integration list.
- Section 12 Contracts: thêm `integrations/smtp.md`.
- Section 13 Cross-Cutting Policies → Integration reliability: thêm fallback rule cho SMTP failure.

**Tác động:** md only, không cần sửa drawio.

---

### FIX-03 — API Gateway: document hoặc remove

**Vấn đề:** Drawio có component "API Gateway" giữa Nginx và Backend. md không đề cập.

**Cần quyết định:**
- Nếu API Gateway **được dùng** (e.g., Spring Cloud Gateway, Kong): bổ sung vào md — thêm vào Section 2 stack, Section 3 Layered Architecture (gateway layer), Section 5 deployment components, Section 16 Decisions.
- Nếu **không dùng** (traffic đi thẳng Nginx → Backend): xóa khỏi drawio, đổi edge thành Nginx → Backend trực tiếp.

**Tác động:** Decision ảnh hưởng đến cả drawio lẫn md.

---

### FIX-04 — Redis: document trong md

**Vấn đề:** Drawio có "Cache / Session / Queue" (TCP 6379). md chỉ nhắc Redis 1 lần trong reading summary, không có quyết định thiết kế.

**Việc cần làm trong `project-structure-design.md`:**
- Section 2: thêm `- Redis for session cache and short-lived token storage.` vào stack.
- Section 5 Backend Structure: thêm dependency `spring-boot-starter-data-redis` vào current dependencies list.
- Section 13 Cross-Cutting Policies (Security): bổ sung Redis session management.
- Section 16 Decisions: thêm row `| Session cache | Redis | Short-lived session state and OTP token storage; avoids database round-trips for auth flows. |`.

**Tác động:** md only.

---

### FIX-05 — Nginx: document trong md

**Vấn đề:** Drawio có "Nginx Reverse Proxy" (reverse proxy, static frontend routing). md không đề cập.

**Việc cần làm trong `project-structure-design.md`:**
- Section 2: thêm `- Nginx as reverse proxy and static frontend server.` vào stack.
- Section 16 Decisions: thêm row `| Reverse proxy | Nginx | Routes HTTPS traffic to Backend (port 8080) and serves Frontend static assets. |`.

**Tác động:** md only.

---

### FIX-06 — Port range HTTP 8081–8090: sửa trong drawio

**Vấn đề:** Edge label từ API Gateway → Backend đọc là "Internal REST API HTTP 8081–8090". Với monolith một Spring Boot app, chỉ có 1 port (8080).

**Việc cần làm trong drawio (System Architecture 2):**
- Tìm edge từ API Gateway đến Backend Domain Services.
- Đổi label thành `HTTP 8080 / REST`.

**Tác động:** Drawio only — sửa 1 edge label thủ công trong draw.io editor.

---

### FIX-07 — Flutter Mobile App: thêm component box vào drawio

**Vấn đề:** Drawio không có Mobile App component box. Frontend Web Portal có box riêng, nhưng Mobile App (Flutter) — client chính của người dùng — bị invisible.

**Việc cần làm trong drawio (System Architecture 2):**
- Thêm component box "Mobile App (Flutter)" vào Users / Client Layer.
- Thêm edge từ Mobile App → cloudflared Tunnel / API Gateway (HTTPS 443).
- Thêm edge từ Mobile App → ZegoCloud, Firebase Chat, TrackAsia, Firebase Storage (các integration trực tiếp từ mobile).

**Tác động:** Drawio only — thêm ~1 box + ~4–5 edges.

---

### FIX-08 — Family Member scope: quyết định và đồng bộ

**Vấn đề:** Ba nguồn mâu thuẫn:
- Drawio: "Family Member" là user type hiện tại (ngang hàng Mother, Expert, Partner).
- `project-structure-design.md` Section 7: *"family remains a future capability module only"*.
- `project-structure-design.md` Section 11 (Mobile): có `familySync/` feature folder.

**Cần quyết định scope:**
- **Option A — Future only**: Xóa "Family Member" khỏi drawio Users / Client Layer. Xóa `familySync/` khỏi mobile app structure trong md (Section 11).
- **Option B — MVP included**: Xóa câu "family remains a future capability module only" khỏi md Section 7. Thêm `family` hoặc giữ `carecoordination` làm implementation. Giữ drawio và mobile structure như hiện tại.

**Tác động:** Decision ảnh hưởng scope, epics, và sprint plan.

---

### FIX-09 — Emergency domain: thêm vào drawio

**Vấn đề:** `emergency` là domain package quan trọng (EmergencyEvent, LocationSnapshot, healthcare safety routing) nhưng không có representation trong drawio.

**Việc cần làm trong drawio (System Architecture 2):**
- Nếu Backend được vẽ là một monolith box: không cần thêm box — chỉ cần thêm edge label từ Backend → TrackAsia để làm rõ emergency context, hoặc thêm note trên TrackAsia edge: "Map / Emergency routing / Nearby care".
- Cập nhật TrackAsia edge label từ "Map / Geocoding / Nearby / Route / ETA" → "Map / Emergency routing / Geocoding / Nearby / Route / ETA".

**Tác động:** Drawio only — sửa 1 edge label.

---

### FIX-10 — Typo "ace verification": sửa trong drawio

**Vấn đề:** CompreFace edge label đọc "face enrollment / ace verification" (thiếu chữ "f").

**Chú ý:** FIX-10 phụ thuộc vào kết quả FIX-01. Nếu CompreFace bị xóa, FIX-10 tự giải quyết.

**Nếu CompreFace được giữ lại:**
- Sửa edge label trong drawio: "ace verification" → "face verification".

---

## Thứ tự thực hiện

```
FIX-01 (quyết định CompreFace scope)
  ├── Nếu xóa → FIX-10 tự đóng
  └── Nếu giữ → FIX-10 (typo fix)

FIX-08 (quyết định Family scope)
  └── Quyết định trước khi sprint planning

FIX-03 (quyết định API Gateway)
  └── Quyết định trước khi sửa drawio

Sau khi có quyết định:
  FIX-02 (md only) — dễ, làm trước
  FIX-04 (md only) — dễ, làm trước
  FIX-05 (md only) — dễ, làm trước
  FIX-06 (drawio) — 1 edge label
  FIX-07 (drawio) — 1 box + edges
  FIX-09 (drawio) — 1 edge label update
```

---

## Tóm tắt nhanh

| Fix | Loại | Độ phức tạp | Phụ thuộc quyết định |
|-----|------|-------------|----------------------|
| FIX-01 | Decision | - | Cần team quyết định |
| FIX-02 | md edit | Thấp | Không |
| FIX-03 | Decision + edit | Trung bình | Cần team quyết định |
| FIX-04 | md edit | Thấp | Không |
| FIX-05 | md edit | Thấp | Không |
| FIX-06 | drawio edit | Thấp | Không |
| FIX-07 | drawio edit | Trung bình | Không |
| FIX-08 | Decision | - | Cần team quyết định |
| FIX-09 | drawio edit | Thấp | Phụ thuộc FIX-01 |
| FIX-10 | drawio edit | Thấp | Phụ thuộc FIX-01 |
