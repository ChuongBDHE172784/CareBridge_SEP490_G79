# Đánh giá Tổng thể & Lộ trình Hoàn thiện Module AI Triage (Schema Canonical 70 Bảng — Nhánh `dev`)

| Field | Value |
|---|---|
| **Status** | Approved (bản đánh giá — đã xác minh trực tiếp trên code) |
| **Date** | 2026-07-26 |
| **Author** | AI Agent |
| **Baseline** | `B20260724111500__canonical_70_table_baseline.sql` (69 bảng nghiệp vụ + `flyway_schema_history` = 70) |

> **Lưu ý bối cảnh:** Tài liệu này thay thế bản đánh giá cũ (viết theo schema *trước* Phase 2). Đợt hợp nhất Phase 2 (`db/migration/phase2/V20260722231400` → `V20260722231900`) đã **drop toàn bộ bảng legacy** và thay bằng tên canonical. Mọi tên bảng dưới đây là tên **thực tế trong baseline 70 bảng**.

---

## PHẦN I: CÁC BẢNG AI TRIAGE DÙNG (Canonical)

### 1. Bảng cốt lõi

1. **`triage_sessions`** *(hợp nhất `intake_sessions` + `structured_intake_data` cũ thành 1 bảng, 1 dòng)*
   * Phiên triage: `user_id`, `baby_profile_id`, `mother_profile_id`, `stage`, `status` (`PROCESSING`, `COMPLETED`, `NEED_MORE_INFO`, `FAILED`), `client_request_id`, `raw_ai_response`.
   * Dữ liệu cấu trúc (gộp từ `structured_intake_data`): `symptom_list` (jsonb), `duration_days`, `intensity`, `emergency_flag`, `risk_level`, `extracted_at`, `structured_created_by`.
   * Disclaimer: `disclaimer_text` + `disclaimer_version` (không có cột `disclaimer` đơn).
   * Entity: `IntakeSession.java`, `StructuredIntakeData.java` — cả hai map `@Table(name = "triage_sessions")`.
2. **`health_context_memories`** *(thay `health_memory_entries`)*
   * Bộ nhớ ngữ cảnh y tế giữa các phiên. Cột thực tế: `related_stage`, `summary_text`, `memory_payload_jsonb`, `expires_at` (timestamp tuyệt đối — không phải `ttl_seconds`), `deleted_at`.
   * Entity: `HealthMemoryEntry.java`. Expiry được enforce khi đọc (`HealthMemoryEntryRepository`).
3. **`knowledge_sources`** *(thay `evidence_sources)*
   * Allow-list domain y tế tin cậy cho RAG (who.int, moh.gov.vn, cdc.gov…). Entity: `EvidenceSource.java`.
4. **`knowledge_source_reviews`** *(thay `evidence_source_review_log`)*
   * Nhật ký phê duyệt/thu hồi domain — immutable (trigger chặn UPDATE/DELETE). Entity: `EvidenceSourceReviewLog.java`.
*(Legacy đã xóa hẳn: `triage_assessments`, `triage_answers` — drop tại `V20260722020500__remove_legacy_triage_persistence.sql`. Lưu ý `triage_sessions` hiện tại là bảng mới, không liên quan cấu trúc trắc nghiệm cũ.)*

### 2. Bảng khẩn cấp downstream

* **`safety_events`** *(thay `emergency_sessions`)*: phiên SOS, tự kích hoạt khi risk `RED`. Entity: `EmergencySession.java`.
* **`safety_event_actions`** *(thay `family_alert_log`, `emergency_alert_attempts`, `emergency_alert_deliveries`, `emergency_map_handoffs`)*: mọi hành động cảnh báo/chuyển tiếp trong phiên SOS. Entities: `FamilyAlertLog.java`, `EmergencyAlertAttempt.java`, `EmergencyAlertDelivery.java`, `EmergencyMapHandoff.java`.
* **`location_snapshots`**: tọa độ GPS tạm thời trong phiên SOS (còn trong baseline).
* **`triage_emergency_escalation_links`**: liên kết phiên triage RED → hành động khẩn cấp.

### 3. Bảng liên quan các hạng mục hoàn thiện

* **`family_tasks`**, **`scheduled_care_items`**: đích cho follow-up task (bảng `reminders` cũ đã drop).
* **`consultation_context_shares`** (+ `consultation_context_citations`): handoff dữ liệu triage sang chuyên gia, có cột `intake_session_id`.
* **`data_permissions`** *(thay `consent_grants`)*: đích lưu vết đồng ý disclaimer. Entity: `ConsentGrant.java`.

---

## PHẦN II: NHỮNG GÌ ĐÃ LÀM ĐƯỢC (Đã xác minh trên code)

1. **Đàm thoại đa lượt & phân loại 1 lượt** — `IntakeController.java` có 7 endpoint (`runIntake`, `getResult`, `listSessions`, `startConversation`, `continueConversation`, `resolveContinuation`, `acknowledgeContinuation`), tất cả gắn `@PreAuthorize("hasRole('MOTHER')")` từng method. Base path `/api/v1/triage/intake`.
2. **Fallback Java Rule Engine** — `TriageService.java` bắt lỗi Python AI client ở cả 3 luồng (one-shot :442-453, start :309-314, continue :370-375) → fallback `TriageGraphService`. Lỗi phân loại `TIMEOUT` / `NETWORK_ERROR` / `PYTHON_5XX` / `MALFORMED_RESPONSE` / `OTHER`, có metrics (`TriageFallbackMetrics`).
3. **Kiểm duyệt citation RAG** — `TriageService.readValidatedCitations` → `EvidenceSourceServiceImpl.isApprovedDeepLink`: chỉ nhận HTTPS + domain APPROVED trong `knowledge_sources`; claim đối chiếu với `sourceId` còn sống.
4. **Chuẩn hóa triệu chứng tiếng Việt (đã có!)** — `SymptomNormalizer.java`: 17 triệu chứng canonical, strip dấu, synonym dân gian (`sot`, `kho tho`, `co giat`, `li bi`, `bo bu`, `tieu chay`, `tim tai`…), rule số liệu (≥37.5°C → fever, ≥39.0 → high_fever). Bản mirror Python: `app/symptom_normalizer.py`.
5. **Sự kiện hoàn tất & kích hoạt SOS** — `IntakeSessionCompleted` → `StructuredIntakeService` ghi dữ liệu cấu trúc vào `triage_sessions`; risk `RED` → `EmergencyEscalationTriggered` → mở `safety_events` → `EmergencySessionOpened` → `FamilyAlertService` ghi `safety_event_actions`.
6. **Handoff sang chuyên gia cho YELLOW (đã có!)** — `TriageExpertHandoffService` + policy `YELLOW_EXPERT_CONTEXT_V1`: chia sẻ tóm tắt triệu chứng có consent qua `consultation_context_shares.intake_session_id`.

### Điểm sai lệch so với bản đánh giá cũ

* ❌ **Health memory chưa được nối vào triage**: `HealthMemoryService` chỉ được inject vào `HealthMemoryController` (list + delete). `TriageService` không đọc/ghi memory ở bất kỳ đâu — hạ tầng có nhưng là dead code với luồng intake.
* ✅ **Sàn an toàn khẩn cấp độc lập database**: `TriageRedFlagPolicy`, `TriageRedFlagSafetyFilter` và fallback Java giữ bộ tín hiệu bảo thủ trong code; catalog quản trị và bảng dữ liệu đã được retire để UI không tạo cảm giác cấu hình động có thể thay đổi định tuyến khẩn cấp.

---

## PHẦN III: CÁC HẠNG MỤC CẦN CODE ĐỂ HOÀN THIỆN

Thứ tự ưu tiên theo gap thực tế:

### 1. Nối Bộ nhớ Ngữ cảnh vào Luồng Triage (`TriageHealthMemoryContext`)
* **Hiện trạng:** `health_context_memories` + service + repository (có TTL enforce) đã tồn tại nhưng luồng triage không dùng.
* **Việc cần làm:** (a) Sau khi phiên triage COMPLETED, ghi tóm tắt bệnh sử ngắn hạn (summary + payload jsonb, kèm `expires_at`) vào `health_context_memories`; (b) khi bắt đầu phiên mới, đọc các memory còn hạn của đúng đối tượng (mẹ/bé, đúng `related_stage`) và đưa vào context gửi AI + Java fallback.

### 2. Follow-up Task cho Rủi ro VÀNG (`TriageYellowFollowUp`)
* **Hiện trạng:** YELLOW đã có expert handoff + khuyến nghị `CONTACT_HEALTHCARE_PROVIDER`, nhưng không có nhắc hẹn theo dõi.
* **Việc cần làm:** Khi phiên kết thúc YELLOW, tự tạo task theo dõi sau 4–6 giờ (vd: "Kiểm tra lại thân nhiệt của bé") vào **`family_tasks`** / **`scheduled_care_items`** (KHÔNG dùng `reminders` — đã drop), kèm push notification.

### 3. Lưu vết Đồng ý Disclaimer (`TriageDisclaimerConsent`)
* **Hiện trạng:** Disclaimer chỉ lưu trên `triage_sessions.disclaimer_text/disclaimer_version`, chưa có gate xác nhận từ người dùng.
* **Việc cần làm:** Lần đầu sử dụng, yêu cầu xác nhận "Tôi hiểu rằng AI Triage chỉ mang tính chất tham khảo…" và lưu vết vào **`data_permissions`** (KHÔNG dùng `consent_grants` — đã drop); các phiên sau kiểm tra đã có consent còn hiệu lực.

### 4. Mở rộng Từ điển Synonym (việc nhỏ, làm dần)
* `SymptomNormalizer` đã có sẵn — chỉ bổ sung từ dân gian còn thiếu ("thóp phập phồng", "trớ sữa", "sốt sình sịch"…) vào cả bản Java và Python, kèm test.

> **Ghi chú tuân thủ:** Mọi hạng mục trên đi theo quy trình `implement-flow`: TDS + Test-Spec tại `04_Implement/[FeatureName]/` phải được duyệt (`Approved`) trước khi viết code production. AI chỉ đưa khuyến nghị tham khảo — không chẩn đoán, không kê đơn, không trì hoãn định tuyến khẩn cấp (BR-SAFETY).
