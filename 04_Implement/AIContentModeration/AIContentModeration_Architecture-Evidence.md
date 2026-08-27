> [!IMPORTANT]
> Historical implementation evidence for `UC-AD-17` and `UC-AD-19`; this is not a canonical current TDS. Current code and the canonical code-first specifications override conflicts.

# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — AI-assisted Content Moderation (Gemini) end-to-end

| Field              | Value                                    |
| ------------------ | ---------------------------------------- |
| **Document ID**    | `CB-MOD-IMP-016`                         |
| **Version**        | `1.0`                                    |
| **Date**           | `2026-07-26`                             |
| **Status**         | `Approved`                               |
| **Document Owner** | `HuyND`                                  |
| **Author**         | `AI Agent — Claude (Fable)`              |
| **Reviewed by**    | `[x] HuyND — 2026-07-26 (uỷ quyền trước trong đề bài)` |
| **DPO Sign-off**   | `N/A` — không xuất PII; assessment chỉ lưu trích đoạn giới hạn từ nội dung công khai |
| **Approved by**    | `[x] HuyND — 2026-07-26 (đề bài yêu cầu "PHÂN TÍCH, THIẾT KẾ VÀ TRIỂN KHAI HOÀN CHỈNH … không dừng lại để chờ xác nhận")` |
| **Last Review**    | `2026-07-26`                             |
| **Based on EDS**   | `v2.0`                                   |

---

## CHANGELOG

| Ngày       | Người thực hiện     | Nội dung thay đổi |
| ---------- | ------------------- | ----------------- |
| 2026-07-26 | AI Agent — Claude   | Tạo tài liệu lần đầu sau graph-based current-state analysis. Vì đề bài uỷ quyền triển khai không chờ xác nhận, Status đặt `Approved` ngay khi tạo (ghi rõ nguồn uỷ quyền ở header). |
| 2026-07-26 | AI Agent — Claude (Dev Agent) | Hiệu chỉnh khi triển khai: (1) Backend thực tế là **Spring Boot 4.1.0** (không phải 3.5.x như CLAUDE.md) — dùng `spring-boot-starter-webmvc`, `@MockitoBean`, `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`; không có WebClient/spring-retry/WireMock → RestClient + backoff tự viết + JDK `com.sun.net.httpserver.HttpServer` cho HTTP-level test. (2) **ADR-015**: scheduler mặc định 1 thread dùng chung với `SafetyCountdownJob` (poll 1s) → worker chỉ claim trên scheduler thread, xử lý Gemini qua `@Async` (`AiScanProcessingService.processJobAsync`); Gemini call nằm NGOÀI transaction DB (Hikari pool = 5), persistence tách vào `AiScanResultRecorder` (@Transactional). (3) Seed **11** system-default policies (10 + PROMPT_INJECTION). (4) Error codes claim workflow: MOD-036 (claim conflict), MOD-037 (release denied), MOD-038 (claimed by another) — MOD-034/035 đã bị dùng bởi CB-MOD-IMP-015. (5) `AuditEligibilityPolicy` là allowlist — 11 AuditAction mới được đăng ký kèm theo. (6) Migration validate thật trên Postgres 18 scratch DB: apply sạch + idempotent (ON CONFLICT/IF NOT EXISTS), 11 policies, 2 cột mới trên moderation_cases. (7) `AiClaimAtomicityIntegrationTest` dùng H2 DB riêng (`aimoderation_claim_testdb`) vì shared `testdb` bị drop khi Spring test-context cache evict một context khác (create-drop). |

---

## CB-MOD-IMP-017 — Domain-leak removal + persistence consolidation (2026-07-26, cùng ngày)

Hai thay đổi scoped sau CB-MOD-IMP-016:

1. **Gỡ domain leak**: `CommunitySafetyPolicy.autoReportIfRedFlag()` bị XÓA cùng 4 call sites (question create/edit, answer post/edit) — medical red-flag keyword ("chảy máu nhiều"…) là tín hiệu cấp cứu, không phải bằng chứng vi phạm; người mô tả triệu chứng không bị tạo hồ sơ AUTOMATED nữa. `TriageRedFlagPolicy` (floor keywords + emergency guidance + RAG/triage routing) không bị đụng — graph affected-flows xác nhận 0 flow triage bị ảnh hưởng. Lời khuyên y khoa NGUY HIỂM vẫn được AI classifier (policy `DANGEROUS_MEDICAL_ADVICE`, phân biệt symptom-vs-advice trong prompt) đưa vào priority review — đường automated duy nhất còn lại. Regression pin: `CommunitySafetyPolicyTest` (reflection — không method *report*, không phụ thuộc ContentReportRepository/TriageRedFlagPolicy).
2. **Consolidate persistence** (`V20260726150000__consolidate_ai_moderation_persistence.sql`):
   - `ai_assessment_matches` → `ai_content_assessments.matches_jsonb` (JSONB NOT NULL DEFAULT '[]', CHECK jsonb_typeof='array'; snapshot bất biến {policyId, policyCode, policyVersion, category, severity, confidence, evidence[], explanation} — policy đổi version sau này không đổi assessment cũ). Ghi atomic 1 INSERT; đọc qua `AiModerationMapper.parseMatches()` → API vẫn trả `AiAssessmentMatchResponse[]` typed (+2 field mới policyId/policyVersion). KHÔNG GIN index (chưa có consumer JSONB filter — ghi chú trong migration).
   - `ai_assessment_feedback` → 5 cột `ai_feedback_{decision,reason,by,at,assessment_id}` trên `moderation_cases` (feedback hiện hành; đọc detail không JOIN) + toàn bộ lịch sử là event append-only `moderation_events.action_type='AI_FEEDBACK_SUBMITTED'` với `event_payload_jsonb` {decision, assessmentId, reason≤500, previousDecision?, submittedAt} — mapping `event_payload_jsonb` được thêm vào entity `ModerationAction` (cột đã tồn tại NOT NULL DEFAULT '{}').
   - Migration: backfill 2 chiều + 3 reconciliation gates (match-count; event-per-feedback theo deterministic UUID md5(feedback_id) → idempotent khi retry; latest-per-case đúng decision/by/at/assessment) — mismatch → RAISE → rollback toàn phần, chỉ khi mọi gate pass mới DROP 2 bảng. Đã chứng minh trên Postgres 18 scratch: upgrade-with-data ✓, forced-mismatch rollback ✓ (sabotage trigger), retry-idempotent ✓ (pre-inserted event không nhân đôi), fresh-chain ✓.
   - **Guard exclusions bắt buộc** (risk HIGH đã báo trước khi sửa): event feedback chia sẻ target với content action nên 3 query đổi sang biến thể loại trừ `AI_FEEDBACK_SUBMITTED`: history feed, most-recent guard của undo/revert, linked-action của revert — nếu không, feedback event sẽ chặn undo/revert (MOD-029/033) và lọt vào history UI.
   - Feedback service: chỉ nhận assessment đã attach case (AIM-013), từ chối khi case IN_REVIEW bởi moderator khác (MOD-038), latest-wins trên case + append event + audit cùng transaction; `AiFeedbackResponse.feedbackId` = id của event lịch sử. API contract (URI + shape) giữ nguyên → web chỉ thêm 2 field optional vào type `AiAssessmentMatch`.
   - Đã xóa: `AiAssessmentMatch`, `AiAssessmentMatchRepository`, `AiAssessmentFeedback`, `AiAssessmentFeedbackRepository`; không còn runtime reference (tên bảng cũ chỉ còn trong 2 migration + comment tài liệu).

---

## 1. TỔNG QUAN

Xây dựng luồng AI-assisted Content Moderation hoàn chỉnh:

1. Nội dung cộng đồng công khai (QUESTION/ANSWER, CONTENT đã publish) được **enqueue scan job bền vững trong PostgreSQL** ngay trong transaction tạo/cập nhật nội dung.
2. Worker `@Scheduled` claim job **atomic**, gọi **Gemini** (structured JSON), lưu **assessment** idempotent.
3. Kết quả SAFE → chỉ lưu assessment. VIOLATION/UNCERTAIN đạt ngưỡng → tạo/attach **moderation case** `report_source=AUTOMATED` (dedup, không overwrite USER case).
4. Moderator có workflow **PENDING → IN_REVIEW (claim) → RESOLVED/DISMISSED**, xem AI evidence, gửi **feedback** đồng ý/không đồng ý.
5. SYSTEM_ADMIN quản lý **AI moderation policies** (tách biệt khỏi sàn an toàn y tế tích hợp trong code), xem Gemini status + job metrics, sandbox test.

### 1.1 Current-state findings (đã kiểm chứng trên code/migrations, 2026-07-26)

| # | Fact | Kết quả kiểm chứng |
|---|------|--------------------|
| A | `moderation_cases` = entity `ContentReport`; `report_source` USER/AUTOMATED | ✅ `ContentReport.java` map `@Table(name="moderation_cases")`, cột `report_source`, enum `ReportSource {USER, AUTOMATED}` |
| B | Status chỉ PENDING/RESOLVED/DISMISSED, chưa có IN_REVIEW | ✅ `ReportStatus.java` |
| C | `assigned_moderator_id` chỉ gán khi resolve | ✅ `ModerationServiceImpl.resolveReport()` (BR-MOD-009) |
| D | API queue đã trả `reportSource` nhưng FE chưa dùng | ✅ `ModerationQueueItemResponse` có `reportSource`; web `ModerationQueueItem` (models/moderation.ts) KHÔNG khai báo; chỉ `RelatedReportItem` có và chỉ render text trong `RelatedReportsCard.tsx:33` |
| E | Sàn an toàn y tế độc lập AI moderation | ✅ `TriageRedFlagPolicy` và `CommunitySafetyPolicy.autoReportIfRedFlag()` giữ định tuyến/cảnh báo bảo thủ trong code; catalog quản trị động đã retire |
| F | `/moderator/safety-rules` copy gây hiểu nhầm | ✅ Title "Quản lý quy tắc an toàn" / "…ngăn chặn nội dung vi phạm" trong khi rules là medical keywords ("chảy máu nhiều") |
| G | `GeminiHttpClient` là stub luôn throw | ✅ constructor bỏ config, `generate()` luôn `throw GeminiUnavailableException` |
| H | GEMINI_ENABLED/GEMINI_MODEL chưa bind | ✅ application.yaml chỉ bind `GEMINI_API_KEY`; `model: gemini-1.5-flash` hard-code; không có key `enabled`. `.env`/`.env.example` có đủ 3 biến |
| I | Chưa có luồng tự gọi Gemini khi tạo nội dung | ✅ Không có scan/enqueue nào; `GeminiClient` chỉ được gọi bởi RAG/triage/extraction adapter (3 consumer) |
| J | Không quét dữ liệu consent-protected | ✅ Thiết kế chỉ enqueue từ community QUESTION/ANSWER + CONTENT publish; không đụng chat/health-memory/triage |
| + | Hạ tầng sẵn có | `@EnableScheduling` + pattern `*/job/*Job` (notification outbox jobs); RestClient convention (CompreFace adapter); `system_configurations.ai_moderation_enabled` (default true) đã có UI toggle |

### 1.2 Gap analysis

1. Không có bảng policy kiểm duyệt AI (chỉ có medical red-flag).
2. Không có persistence assessment/scan job; không có worker; Gemini client là stub.
3. Thiếu IN_REVIEW/claim; thiếu filter source/priority; FE thiếu badge AI.
4. Copywriting `/moderator/safety-rules` sai bản chất.
5. Không có Gemini health/status API; env chưa bind đủ.

---

## 2. ARCHITECTURE DECISION RECORDS

| ID | Quyết định | Lý do / Hệ quả |
|----|-----------|-----------------|
| ADR-001 | **Domain package mới `aimoderation`** (entity/repository/service/controller/dto/mapper/policy/job) thay vì nhét vào `content` | Giữ modular monolith, blast radius nhỏ; `content` giữ nguyên workflow case hiện có. `aimoderation` phụ thuộc `content` (tạo ContentReport) — một chiều. |
| ADR-002 | **Durable DB-backed job** bảng `ai_content_scan_jobs`, enqueue **cùng transaction** với tạo/sửa nội dung; worker `@Scheduled` theo pattern outbox job sẵn có | Không broker mới; publish không phụ thuộc Gemini latency; mất app giữa chừng không mất job. |
| ADR-003 | **Claim job không dùng `FOR UPDATE SKIP LOCKED`** mà dùng guarded UPDATE (`SET status='PROCESSING' … WHERE id=? AND status='QUEUED'`, ret 0 = thua race) | Portable H2 (test) + Postgres; atomic ở row level; tránh native SQL Postgres-only trong repo test H2. |
| ADR-004 | **Idempotency key** = (target_type, target_id, content_hash, policy_set_hash, model); partial unique index Postgres `WHERE status='COMPLETED'` + check service-level | Rescan cùng nội dung không tạo assessment trùng; content đổi → hash mới → scan mới. |
| ADR-005 | **Case dedup attach-first**: nếu target đã có case PENDING/IN_REVIEW (bất kể source) → attach assessment vào case đó (nâng priority nếu cần), KHÔNG tạo case mới, KHÔNG đổi `report_source`/`reason_code` của case USER | Không overwrite nguồn USER; UI hiển thị "Người dùng + AI" qua sự tồn tại assessment gắn case. Không thêm DB unique constraint trên automated case vì sẽ phá luồng revert hiện có (revert đưa case về PENDING). |
| ADR-006 | **Thêm `IN_REVIEW`** vào `ReportStatus` + cột `claimed_at`, `priority` trên `moderation_cases`; claim/release qua guarded UPDATE atomic | resolve chấp nhận PENDING (giữ tương thích) hoặc IN_REVIEW bởi chính người claim; IN_REVIEW của người khác → MOD-034; revert từ IN_REVIEW bị từ chối (chưa resolved). |
| ADR-007 | **GeminiHttpClient implement thật** giữ nguyên interface `GeminiClient.generate(String)`; `enabled=false`/thiếu key → `GeminiUnavailableException` — **giữ nguyên hành vi hiện tại** cho 3 consumer (RAG, triage adapter, extraction adapter) khi chưa bật | Không đổi behavior mặc định của các flow khác. Client moderation riêng `GeminiModerationClient` dùng `responseMimeType=application/json` + `responseSchema`. |
| ADR-008 | **Phân loại lỗi**: 408/timeout/429/5xx → retryable (`GeminiUnavailableException`); 400/401/403/404 → non-retryable (`GeminiConfigurationException` mới, sanitized, không chứa key); malformed/schema-invalid JSON → FAILED retryable có đếm attempt; **FAILED không bao giờ là SAFE** | Đúng yêu cầu §4.4; model `gemini-3.5-flash` không hợp lệ sẽ surface `GEMINI_MODEL_INVALID` chứ không âm thầm đổi model. |
| ADR-009 | **Server-owned prompt + server-side decision**: FE không gửi prompt; `recommendedAction` của model chỉ tham khảo — quyết định tạo case do `AiModerationDecisionPolicy` tính từ classification + matched policy severity/confidence + threshold | Chống prompt injection điều khiển hình phạt; AI không tự đình chỉ tài khoản. Evidence phải là substring thật của nội dung (server verify, drop nếu không khớp). |
| ADR-010 | **Hai cổng bật/tắt**: enqueue gate theo `system_configurations.ai_moderation_enabled` (business); worker gate thêm `carebridge.gemini.enabled` (infra). Gemini disabled → job nằm QUEUED (không đốt attempt), admin thấy queue depth | Bật lại là xử lý backlog; tạo nội dung không bao giờ bị chặn. |
| ADR-011 | **Không jsonb trong entity mới** — matched policies chuẩn hoá bảng `ai_assessment_matches` | H2 test datasource không có jsonb; tránh converter mới. |
| ADR-012 | Giữ nguyên `CommunitySafetyPolicy.autoReportIfRedFlag()` (medical red-flag → AUTOMATED report) | Là safety floor hiện hữu; AI moderation là tín hiệu bổ sung, dedup ADR-005 xử lý va chạm. |
| ADR-013 | System-default policy: không hard-delete (không có DELETE endpoint), mọi thay đổi audit + `version+1` khi thay đổi ảnh hưởng phân loại (guidance/category/severity/threshold/targetTypes/active) | Yêu cầu §4.1. |
| ADR-014 | Audit qua `AuditService` hiện có, detail chỉ chứa ID/hash/error-code/policy-code — không raw content | Yêu cầu §4.8. |

---

## 3. DATA MODEL (Flyway `V20260726100000__ai_content_moderation.sql` — additive)

### 3.1 `ai_moderation_policies`
| Cột | Kiểu | Ghi chú |
|-----|------|---------|
| policy_id | uuid PK default gen_random_uuid() | |
| policy_code | varchar(60) NOT NULL UNIQUE | stable code, vd `SPAM_ADVERTISING` |
| name | varchar(150) NOT NULL | |
| detection_guidance | text NOT NULL | validate ≤ 2000 chars ở service (không phải system prompt tuỳ ý) |
| violation_category | varchar(40) NOT NULL | enum `AiViolationCategory` |
| report_category | varchar(40) NOT NULL | map về `ReportCategory` hiện có cho `reason_code` case |
| severity | varchar(20) NOT NULL | LOW/MEDIUM/HIGH/CRITICAL |
| applicable_target_types | varchar(100) NOT NULL | CSV con của QUESTION,ANSWER,CONTENT |
| confidence_threshold | numeric(4,3) NOT NULL DEFAULT 0.700 CHECK 0..1 | |
| active | boolean NOT NULL DEFAULT true | |
| system_default | boolean NOT NULL DEFAULT false | |
| version | int NOT NULL DEFAULT 1 | |
| created_by / updated_by | uuid NULL FK users | seed = NULL (system) |
| created_at / updated_at | timestamptz NOT NULL DEFAULT now() | |

Seed 10 system-default policies (`ON CONFLICT (policy_code) DO NOTHING`): SPAM_ADVERTISING, HARASSMENT_BULLYING, HATE_SPEECH, CHILD_SAFETY (CRITICAL), SELF_HARM_ENCOURAGEMENT (CRITICAL), DANGEROUS_MEDICAL_ADVICE (HIGH), EXPERT_IMPERSONATION (HIGH), HARMFUL_MISINFORMATION, PII_DOXXING, SCAM_FRAUD; + PROMPT_INJECTION (MEDIUM). Guidance nêu rõ: mô tả triệu chứng của chính mình KHÔNG phải vi phạm.

### 3.2 `ai_content_scan_jobs`
job_id uuid PK; target_type varchar(20) NOT NULL; target_id uuid NOT NULL; content_hash varchar(64) NOT NULL; status varchar(20) NOT NULL DEFAULT 'QUEUED' (QUEUED/PROCESSING/COMPLETED/FAILED/SKIPPED); attempt_count int NOT NULL DEFAULT 0; max attempts = config (4); next_attempt_at timestamptz NOT NULL DEFAULT now(); locked_by varchar(100); locked_at timestamptz; last_error_code varchar(80); created_at/updated_at/completed_at.
Indexes: (status, next_attempt_at); (target_type, target_id, status).

### 3.3 `ai_content_assessments`
assessment_id uuid PK; job_id uuid FK NULL; target_type; target_id; content_hash varchar(64); policy_set_hash varchar(64); provider varchar(30) DEFAULT 'GEMINI'; model varchar(60); status varchar(20) (COMPLETED/FAILED); classification varchar(20) NULL (SAFE/VIOLATION/UNCERTAIN); overall_severity varchar(20) NULL; confidence numeric(4,3) NULL; recommended_action varchar(30) NULL; explanation varchar(1000) NULL; error_code varchar(80) NULL; attempt_count int DEFAULT 1; latency_ms bigint NULL; prompt_tokens int NULL; output_tokens int NULL; moderation_case_id uuid FK moderation_cases NULL; created_at; completed_at.
Partial unique (Postgres): `UNIQUE(target_type,target_id,content_hash,policy_set_hash,model) WHERE status='COMPLETED'`. Index (target_type,target_id,created_at DESC), (moderation_case_id).

### 3.4 `ai_assessment_matches`
match_id uuid PK; assessment_id FK NOT NULL ON DELETE CASCADE; policy_code varchar(60); category varchar(40); severity varchar(20); confidence numeric(4,3); evidence varchar(1000) (≤3 trích đoạn, mỗi đoạn cap 200 chars, đã verify substring); explanation varchar(500). Index (assessment_id).

### 3.5 `ai_assessment_feedback`
feedback_id uuid PK; assessment_id FK NOT NULL; moderation_case_id FK NULL; moderator_user_id FK users NOT NULL; verdict varchar(20) (AGREE/DISAGREE); note varchar(500); created_at. UNIQUE(assessment_id, moderator_user_id).

### 3.6 ALTER `moderation_cases`
`ADD COLUMN priority varchar(20) NOT NULL DEFAULT 'NORMAL'` (NORMAL/HIGH/URGENT); `ADD COLUMN claimed_at timestamptz NULL`. Index (report_source, status, priority).

`policy_set_hash` = sha256 của chuỗi sorted `code:version` các policy active + global config — tính bởi `AiPolicySetService`.

---

## 4. LUỒNG XỬ LÝ (sequence tóm tắt)

```
User đăng/sửa QUESTION|ANSWER|CONTENT(publish)
  └─ (same TX) CommunityQuestionService/AnswerService/ContentApproval → AiScanEnqueueService.enqueue(targetType, targetId, text)
        - gate: system_configurations.ai_moderation_enabled
        - hash = sha256(normalize(text)); nếu đã có job QUEUED cùng (target,hash) → skip
        - INSERT ai_content_scan_jobs (QUEUED)
COMMIT  → API trả về ngay, KHÔNG chờ Gemini

AiContentScanWorker (@Scheduled fixedDelay, gate: gemini.enabled && ai_moderation_enabled)
  ├─ findClaimable(limit): QUEUED & next_attempt_at<=now, hoặc PROCESSING stale (locked_at < now-10m)
  ├─ per job: guarded UPDATE claim → PROCESSING (0 row = thua race → bỏ qua)
  ├─ re-fetch target: gone → SKIPPED(TARGET_GONE); hash ≠ job.hash → SKIPPED(STALE)
  ├─ idempotency: đã có COMPLETED assessment cùng key → COMPLETED (không gọi Gemini)
  ├─ GeminiModerationClient.classify(systemPrompt(policies), content)
  │     ├─ OK → validate JSON/schema/enum/range/evidence-substring → save assessment+matches
  │     │       → AiModerationDecisionPolicy → (không case | attach case | tạo case AUTOMATED priority X)
  │     │       → job COMPLETED (+audit AI_CASE_CREATED nếu tạo case)
  │     ├─ retryable (timeout/429/5xx/invalid-json) → attempt<max: job QUEUED + backoff 30s·2^n (cap 15m)
  │     │       attempt≥max: job FAILED + assessment FAILED(error_code) + audit AI_SCAN_FAILED
  │     └─ non-retryable (401/403/404/400 model/key) → job FAILED ngay + assessment FAILED + audit
```

Decision matrix (server-side, `AiModerationDecisionPolicy`):
- FAILED/SAFE → không tạo case.
- Matches lọc theo `confidence ≥ policy.confidence_threshold`.
- VIOLATION có match: CRITICAL→URGENT, HIGH→HIGH, còn lại NORMAL; tạo/attach case PENDING `report_source=AUTOMATED`, `reason_code=report_category` của match nghiêm trọng nhất, description = explanation ngắn (không raw content).
- UNCERTAIN và `confidence ≥ 0.5` (config `review-threshold`) → case NORMAL.
- Không bao giờ warn/suspend/hide tự động — chỉ tạo case chờ human review.

## 5. API CONTRACT

Base FE hiện dùng `/api/v1/admin/moderation/**` cho luồng kiểm duyệt.

### 5.1 SYSTEM_ADMIN — `/api/v1/admin/ai-moderation/**` (controller mới, dùng ApiResponse envelope chuẩn)
- `GET /policies?active=&page=&size=` — list
- `POST /policies` — create (validate lengths/enum/threshold/targetTypes)
- `PUT /policies/{id}` — update (system_default: không đổi policy_code; version++ nếu đổi trường ảnh hưởng phân loại)
- `PATCH /policies/{id}/status {active}` — bật/tắt (system_default vẫn được tắt? → KHÔNG: system_default luôn phải giữ được audit; cho phép deactivate nhưng không delete; audit)
- `GET /status` — {enabled, configured, model, workerStatus, queuedJobs, processingJobs, failedJobs24h, lastCompletedAt, policySetHash} — KHÔNG BAO GIỜ trả key
- `POST /test {targetType, sampleText}` — sandbox: gọi classify trực tiếp, không persist, không audit content (audit chỉ sự kiện AI_POLICY_TEST_RUN)
- `POST /rescan {targetType, targetId}` — enqueue lại (force, bỏ dedup QUEUED)

### 5.2 MODERATOR — mở rộng ModerationController (raw DTO convention)
- `GET /queue` thêm params `source=USER|AUTOMATED`, `priority=` (Specification-based query); response item thêm `reportSource, priority, claimedAt` (đã có sẵn reportSource)
- `POST /reports/{id}/claim` → 200 {reportId,status=IN_REVIEW,assignedModeratorId,claimedAt} | 409 MOD-034 (đã claim)
- `POST /reports/{id}/release` → về PENDING (chỉ người claim hoặc bất kỳ? → chỉ người claim; MOD-035 nếu không phải)
- `GET /reports/{id}/assessment` → latest assessment gắn case hoặc theo target (200 | 404 AI-404)
- `POST /assessments/{assessmentId}/feedback {verdict: AGREE|DISAGree, note}` → lưu + audit (1 lần/moderator, upsert)
- `resolve`/`revert` giữ endpoint cũ; guard mở rộng theo ADR-006.

### 5.3 Không có endpoint public nào nhận `report_source` từ caller — `AUTOMATED` chỉ do backend workflow tạo (CreateReportRequest không có field source — đã đúng hiện trạng).

## 6. SECURITY / PRIVACY SAFEGUARDS

1. API key chỉ nằm trong env → RestClient header; không log, không echo trong exception (message sanitize), không trả qua API.
2. Prompt đánh dấu nội dung user là UNTRUSTED DATA trong delimiter; cấm làm theo chỉ dẫn bên trong; không chẩn đoán y khoa; không quyết định hình phạt.
3. Không lưu chain-of-thought; chỉ structured rationale + evidence ngắn (đã verify substring, cap độ dài).
4. Audit detail chỉ ID/hash/code. `moderation_events` không sửa (append-only, không đụng).
5. Scan scope: chỉ QUESTION/ANSWER cộng đồng + CONTENT publish. Không chat/consultation/health-memory/triage/consent-protected.
6. RBAC: policy/status/test/rescan = SYSTEM_ADMIN; queue/claim/feedback/resolve = MODERATOR (theo split hiện tại của SecurityConfig; ModerationController hiện MODERATOR-only).
7. Sandbox test không persist sample text.

## 7. DANH SÁCH FILE DỰ KIẾN

Backend mới: `aimoderation/**` (~20 file: entities AiModerationPolicy, AiContentScanJob, AiContentAssessment, AiAssessmentMatch, AiAssessmentFeedback + enums; repositories; services AiPolicyService, AiPolicySetService, AiScanEnqueueService, AiScanProcessingService, AiModerationDecisionPolicy, AiModerationStatusService; job AiContentScanWorker; controller AiModerationAdminController; dto; mapper), `integration/gemini/client/GeminiModerationClient.java`, `integration/gemini/exception/GeminiConfigurationException.java`, migration SQL.

Backend sửa: `GeminiHttpClient` (real impl), `application.yaml` (bind GEMINI_ENABLED/GEMINI_MODEL + carebridge.ai-moderation.*), `ReportStatus` (+IN_REVIEW), `ContentReport` (+priority/claimedAt), `ContentReportRepository` (+claim/release/spec), `ModerationServiceImpl` (+claim/release/assessment/feedback + guard), `ModerationService`, `ModerationController`, `ModerationMapper`, `ModerationQueueItemResponse` (+priority/claimedAt), `ModerationQueueFilter` (+source/priority), `ModerationException` (+MOD-034/035…), `AuditAction` (+9 values), `ReportServiceImpl`/`CommunitySafetyPolicy` (duplicate check gồm IN_REVIEW), community create/update services + content publish (enqueue hook).

Web sửa: `models/moderation.ts` (+reportSource/priority/claimedAt/IN_REVIEW/assessment types), `moderationApi.ts` (+claim/release/assessment/feedback + source filter), `ReportsQueuePage.tsx` (badge AI, filter source, claim), `ContentReportDetailPage.tsx` + `AccountReportDetailPage.tsx` (AI assessment panel + accept/reject), `SafetyRuleManagementPage.tsx` → AI & Safety Policy Hub 2 tab + đổi copy, mới `aiModerationPolicyApi.ts` + `models/aiModerationPolicy.ts`, `ModPortalSidebar.tsx` (label), router (không đổi cấu trúc route).

## 8. TEST PLAN — xem `AIContentModeration_Verification-Evidence.md` (22 backend scenarios + web build/tsc).
