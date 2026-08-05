Bạn đang đóng vai Senior Software Architect + AI Safety Reviewer + Clinical Triage Logic Reviewer.
Hãy review chéo toàn bộ phần AI Triage của dự án CareBridge tại:
D:\FU\Term 9\CareBridge_SEP490_G79
Đây là vòng REVIEW READ-ONLY. Không sửa code, không tạo migration, không thay đổi dữ liệu Supabase, không commit và không push.
==================================================
1. MỤC TIÊU
==================================================
Đánh giá phần AI Triage sau các thay đổi mới nhất, đặc biệt bảo đảm:
1. Hệ thống trích xuất đúng intent và triệu chứng từ câu tự nhiên của người dùng, ví dụ:
- "Tôi đau bụng"
- "Tôi bị tiêu chảy"
- "Tôi đau bụng và tiêu chảy"
- "Tôi bị đau đầu"
- "Tôi đau tai"
- "Tôi tiểu buốt"
- "Tôi bị ngứa"
- Câu có dấu/không dấu.
- Viết hoa/viết thường.
- Câu chứa phủ định.
- Câu có nhiều triệu chứng.
- Triệu chứng chưa được hệ thống nhận diện.
- Nội dung giống prompt injection hoặc chỉ dẫn dành cho AI.
2. Câu hỏi follow-up phải dựa trên:
- Intent vừa trích xuất.
- Nhóm triệu chứng.
- Những dữ kiện còn thiếu.
- Stage đang được chọn.
- Đối tượng đang được hỏi là mẹ hay bé.
- Các quy tắc an toàn deterministic.
3. Không được xảy ra:
- Chọn "Đang mang thai" nhưng hỏi bằng ngôn ngữ dành cho bé.
- Người dùng báo tiêu chảy/đau bụng nhưng hệ thống hỏi các câu không liên quan.
- Flutter/Python thêm field nhưng Java DTO/allowlist/serializer không hỗ trợ, gây HTTP 500.
- Câu trả lời hợp lệ bị mất giữa các follow-up round.
- AI tự thay đổi stage, subject, risk level hoặc recommendation.
- Gemini tự thêm question key ngoài allowlist.
- Gemini hạ cấp hoặc override kết quả RED.
- Unknown symptom bị đoán thành một bệnh cụ thể.
- Raw backend error, stack trace, JWT, API key hoặc thông tin sức khỏe bị lộ ra UI/log.
4. Xác định rõ giới hạn hiện tại:
- Nhóm triệu chứng nào đang được hỗ trợ tốt.
- Nhóm nào chỉ được hỏi làm rõ.
- Nhóm nào chưa được hỗ trợ.
- Không tuyên bố hệ thống xử lý được mọi bệnh hoặc mọi tình huống.
- Không tự thêm clinical threshold khi chưa có clinical approval.
AI Triage là hệ thống phân loại nguy cơ ban đầu, không phải công cụ chẩn đoán, kê thuốc hoặc thay thế nhân viên y tế.
==================================================
2. TRẠNG THÁI GIT CẦN REVIEW
==================================================
- Branch hiện tại: `ChuongBD`
- HEAD: `f0fdc911 fix(triage): restore common symptom intake contract`
- So với `gitlab/ChuongBD`, branch local đang ahead 7 commit.
- Kết quả xác minh gần nhất:
  `git rev-list --left-right --count gitlab/ChuongBD...HEAD`
  trả về:
  `0 7`
Các commit AI Triage gần nhất:
- `9c057c11 fix(triage): harden intent-driven follow-ups`
- `ae10e794 feat(triage): route common symptom intents`
- `f0fdc911 fix(triage): restore common symptom intake contract`
Trước khi kết luận:
- Đọc `AGENTS.md`, `CLAUDE.md`, project context và specification liên quan nếu tồn tại.
- Kiểm tra lại HEAD và worktree.
- Không chỉ tin nội dung prompt này.
- Mọi nhận định phải được xác minh bằng implementation, diff hoặc test.
- Không fetch/pull nếu chưa cần thiết.
- Không chuyển branch.
Để tránh quét toàn repository, hãy dùng path-filter khi xem diff:
- `05_Development/CareBridgeMobileApp/lib/features/aiTriage`
- `05_Development/CareBridgeMobileApp/test/features/aiTriage`
- `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage`
- `05_Development/CareBridgeAPI/src/test/java/com/carebridge/backend/triage`
- `05_Development/CareBridgeAITriageService/app`
- `05_Development/CareBridgeAITriageService/tests`
Ví dụ:
`git show <commit> -- <path>`
Không dùng diff toàn repository nếu không cần thiết.
==================================================
3. KIẾN TRÚC HIỆN TẠI CẦN XÁC MINH
==================================================
Luồng dự kiến:
Flutter UI
→ Spring Boot API
→ consent/stage/session validation
→ deterministic RED pre-screen
→ Python FastAPI AI Triage
→ Java kiểm tra response envelope
→ persistence/RAG enrichment
→ trả kết quả cho Flutter
Khi Python/Gemini lỗi hoặc trả dữ liệu không an toàn:
Spring Boot API
→ Java deterministic fallback
→ trả kết quả bảo thủ và đúng stage.
Đây chỉ là kiến trúc dự kiến. Hãy xác nhận hoặc phủ định từng phần bằng code, không chép lại nguyên mô tả.
==================================================
4. FLUTTER FRONTEND
==================================================
Thư mục:
`05_Development/CareBridgeMobileApp/lib/features/aiTriage/`
Các file chính:
1. `screens/symptom_intake_screen.dart`
Cần kiểm tra:
- Stage selection và explicit stage confirmation.
- Consent flow.
- Cách tạo `currentIntake`.
- Cách gửi mô tả triệu chứng.
- Render dynamic questions.
- Maternal/pediatric response guard.
- Error handling.
- Không hiển thị raw backend details.
- Các field:
  - `painSeverity`
  - `urinarySymptoms`
  - `hydrationStatus`
  - `vomiting`
2. `services/triage_service.dart`
Các endpoint:
- `GET /api/v1/triage/consent`
- `POST /api/v1/triage/consent/accept`
- `POST /api/v1/triage/intake/conversation/start`
- `POST /api/v1/triage/intake/conversation/continue`
Cần kiểm tra:
- Canonical stage nằm ở top-level.
- `clientRequestId` và idempotency.
- Lifecycle fields không bị client giả mạo.
- Timeout.
- Consent-required mapping.
- Continuation persistence/restore.
- Request start/continue có mất field nào không.
3. `models/triage_entry_context.dart`
Các stage:
- `PRECONCEPTION`
- `PREGNANCY`
- `POSTPARTUM`
- `INFANT`
- `TODDLER`
Cần kiểm tra:
- Direct/floating entry phải xác nhận stage.
- Trusted lifecycle source.
- Không dùng stage mặc định gây nhầm mẹ và bé.
4. Các model/service/widget liên quan:
- `models/triage_intake_flow_model.dart`
- `models/triage_result_model.dart`
- `models/triage_history_model.dart`
- `models/triage_consent_status.dart`
- `models/triage_continuation.dart`
- `services/triage_continuation_store.dart`
- `services/triage_continuation_restore_coordinator.dart`
- `widgets/floating_ai_triage_host.dart`
- `widgets/triage_safety_entry_action.dart`
5. Network/auth:
`05_Development/CareBridgeMobileApp/lib/core/network/api_client.dart`
Kiểm tra:
- JWT.
- Token refresh.
- Account/session switching.
- 401 behavior.
- Timeout.
- Safe API error mapping.
==================================================
5. SPRING BOOT BACKEND
==================================================
Thư mục:
`05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/triage/`
1. `controller/IntakeController.java`
Base path:
`/api/v1/triage/intake`
Kiểm tra:
- One-shot intake.
- Conversation start.
- Conversation continue.
- Session get/list.
- Continuation resolve/acknowledge.
- Role `MOTHER` và `FAMILY`.
- Auth/session ownership.
2. `service/impl/TriageService.java`
Đây là orchestrator chính. Kiểm tra:
- Consent gate.
- Canonical stage.
- Lifecycle binding.
- Session ownership.
- Idempotency.
- Input size/depth validation.
- Closed `currentIntake` field set.
- Deterministic RED pre-screen.
- Health-memory context.
- Python service call.
- AI response envelope validation.
- Question-key allowlist.
- Maternal/pediatric separation.
- Persistence.
- Sanitization.
- RAG enrichment.
- Java fallback.
- Safe exception mapping.
3. `dto/request/StartIntakeConversationRequest.java`
Kiểm tra:
- Top-level stage và nested stage phải khớp.
- Unknown top-level fields.
- `clientRequestId`.
- Field length/type validation.
4. `dto/request/RunIntakeRequest.java`
Kiểm tra typed contract, đặc biệt:
- `painSeverity`
- `urinarySymptoms`
- `hydrationStatus`
- `vomiting`
Mỗi field phải có:
- Type.
- Size bound.
- Nullable/required behavior.
- Stage applicability.
- Serializer mapping.
- Fallback mapping.
- Test.
5. `service/impl/HttpChildTriageAiClient.java`
Kiểm tra các endpoint Python:
- `/triage/child`
- `/triage/intake/start`
- `/triage/intake/continue`
Xác minh serializer có forward đầy đủ:
- `painSeverity`
- `urinarySymptoms`
- `hydrationStatus`
- `vomiting`
- Các field khác trong closed intake contract.
6. Deterministic risk engine:
- `engine/TriageGraphService.java`
- `engine/SymptomNormalizer.java`
- `engine/UniversalMaternalRedRules.java`
- `engine/MaternalPregnancyRiskRules.java`
- `engine/PostpartumRiskRules.java`
- `engine/PreconceptionRiskRules.java`
- `engine/PediatricInfantRiskRules.java`
- `engine/PediatricToddlerRiskRules.java`
- `engine/PediatricRiskRules.java`
- `engine/RiskRuleFactory.java`
7. Safety/pre-screen:
- `policy/TriageRedFlagPreScreenPolicy.java`
- `policy/TriageRedFlagPolicy.java`
- `policy/TriageDisclaimerPolicy.java`
8. Consent:
- `controller/TriageConsentController.java`
- `service/impl/TriageConsentService.java`
9. RAG/evidence:
- `service/TriageRagEnrichmentService.java`
- `controller/EvidenceSourceAdminController.java`
- `controller/InternalEvidenceSourceController.java`
- `service/impl/EvidenceSourceServiceImpl.java`
- Evidence source/review-log entities và repositories.
Java phải tiếp tục là authority của risk. RAG chỉ được bổ sung giải thích và hướng dẫn từ nguồn được duyệt.
10. Health memory:
- `HealthMemoryContextItem`
- `HealthMemoryService`
- `HealthMemoryServiceImpl`
Kiểm tra:
- Context do server nạp.
- Client không được tự gửi trusted health context.
- Health memory không tự nâng/hạ risk.
==================================================
6. PYTHON AI TRIAGE SERVICE
==================================================
Thư mục:
`05_Development/CareBridgeAITriageService/`
1. `app/main.py`
Endpoint:
- `/health`
- `/triage/child`
- `/triage/intake/start`
- `/triage/intake/continue`
Kiểm tra:
- Stage reconciliation.
- Stage mismatch rejection.
- Không âm thầm đổi subject.
- RED precedence.
- Start/continue merge behavior.
2. `app/schemas.py`
Kiểm tra:
- `ChildTriageRequest`.
- Conversation request/response models.
- `TriageIntent`.
- `extra="forbid"`.
- Field bounds.
- Unknown nested fields.
- Strictness của các endpoint có thể được gọi trực tiếp.
`TriageIntent` dự kiến chứa:
- `subject`
- `careGoal`
- symptom families
- known facts
- confidence
3. `app/symptom_normalizer.py`
Kiểm tra:
- Tiếng Việt có dấu/không dấu.
- Chuẩn hóa `đ` thành `d`.
- Synonym mapping.
- Negation.
- Instruction-like text.
- Prompt injection.
- Unknown symptom behavior.
Các symptom code đã được mở rộng dự kiến gồm:
- `abdominal_pain`
- `nausea`
- `constipation`
- `itching`
- `urinary_discomfort`
- `ear_pain`
- `headache`
- `cold_symptoms`
Hãy xác minh danh sách thực tế trong code.
4. `app/intake_question_engine.py`
Kiểm tra:
- `extract_triage_intent`.
- `MOTHER`/`CHILD`.
- `ASSESS_REPORTED_SYMPTOM`.
- `CLARIFY_SYMPTOM`.
- Missing-fact selection.
- Question ordering.
- Question deduplication.
- Mixed symptoms.
- Maximum questions per round.
- Maximum follow-up rounds.
- Gemini naturalization guard.
Các symptom family dự kiến:
- `ABDOMINAL`
- `DIARRHEA`
- `VOMITING`
- `FEVER`
- `RASH`
- `RESPIRATORY`
- `URINARY`
- `EAR`
- `HEADACHE`
Hãy xác minh danh sách thực tế.
5. `app/risk_rules.py` và `app/graph.py`
Kiểm tra:
- Deterministic rules.
- RED precedence.
- Maternal/pediatric parity với Java.
- Gemini không có quyền override.
6. `app/gemini_client.py`
Kiểm tra:
- Structured output.
- Timeout.
- Schema validation.
- Fallback.
- Gemini chỉ hỗ trợ normalization, naturalization, explanation và summary.
7. `app/config.py`
Kiểm tra:
- Gemini default model.
- Timeout budget.
- Environment variable.
- Model/config có thống nhất với Java hay không.
- Không hardcode secret.
8. Evidence/RAG:
- `evidence_registry_client.py`
- `official_source_searcher.py`
- `source_retriever.py`
- `source_validator.py`
- `evidence_cache.py`
==================================================
7. CÔNG NGHỆ HIỆN TẠI
==================================================
Frontend:
- Flutter/Dart.
- Dart SDK `>=3.10.0 <4.0.0`.
- `http`.
- `flutter_secure_storage`.
- `go_router`.
- Firebase Auth/Firestore/Messaging trong ứng dụng.
Backend:
- Java 21.
- Spring Boot 4.1.0.
- Spring MVC.
- Spring Security.
- Spring Validation.
- Spring Data JPA.
- Jackson.
- Maven.
- PostgreSQL/Supabase.
- Flyway.
- JWT/RBAC.
- Micrometer/metrics.
AI service:
- Python.
- FastAPI/Uvicorn.
- Pydantic.
- LangGraph/LangChain Core.
- Google GenAI/Gemini.
- pytest/httpx.
- BeautifulSoup/requests.
Database:
- Supabase PostgreSQL.
- Không sửa/xóa dữ liệu Supabase trong quá trình review.
- Không in URL, password, token, JWT, API key hoặc health data ra báo cáo.
==================================================
8. CẤU HÌNH PORT VÀ CORS CHÍNH THỨC
==================================================
Các development origin hợp lệ mặc định là:
- `http://localhost:5173`
- `http://127.0.0.1:5173`
- `http://localhost:5000`
- `http://127.0.0.1:5000`
Không sử dụng port `5050`.
Nếu phát hiện:
- Runtime command dùng port 5050.
- Tài liệu dùng port 5050.
- Script launch dùng port 5050.
- Test fixture dùng port 5050.
- CORS được mở thêm port 5050.
Thì phải báo đây là cấu hình không đúng với port development chính thức.
Không đề xuất thêm port 5050 vào CORS. Hướng sửa phải là đưa frontend/runtime về port 5173 hoặc 5000.
Kiểm tra:
- `application.yaml`.
- `SecurityConfig.java`.
- Local/dev/staging/prod profiles.
- Docker/deployment environment.
- Startup documentation.
- Flutter web launch command.
- API base URL và CORS behavior.
Production phải dùng exact origin từ environment, không dùng wildcard.
==================================================
9. NGUYÊN TẮC NGHIỆP VỤ VÀ AN TOÀN
==================================================
Risk level:
- `RED`
- `YELLOW`
- `GREEN`
- `NEED_MORE_INFO`
Nguyên tắc:
- Danger signs trước follow-up thông thường.
- Maternal và pediatric phải tách biệt.
- Stage/subject do hệ thống tin cậy quyết định.
- AI không được override deterministic risk.
- Unknown symptom phải hỏi làm rõ trung lập.
- Không đoán bệnh.
- Không kê thuốc.
- Không thay đổi clinical threshold khi chưa có clinical approval.
- Bounded questions/rounds.
- Fail closed.
- Conservative fallback.
- RED precedence.
- Auditability.
- Data minimization.
Cách tổ chức "danger signs first, sau đó symptom-specific questions" mang định hướng kiểu WHO IMCI, nhưng không được tuyên bố hệ thống đã được WHO chứng nhận hoặc clinically validated nếu chưa có bằng chứng.
==================================================
10. THAY ĐỔI MỚI NHẤT CẦN XÁC MINH
==================================================
Commit `ae10e794` dự kiến đã:
- Bổ sung intent routing cho triệu chứng thông thường.
- Mở rộng symptom normalization.
- Thêm symptom families.
- Tách maternal/pediatric question policy.
- Hỏi theo triệu chứng thay vì dùng một bộ câu hỏi chung.
- Thêm test cho:
  - Đau bụng.
  - Tiêu chảy.
  - Tiểu tiện.
  - Đau tai.
  - Đau đầu.
  - Ngứa.
  - Mixed symptoms.
  - Viết hoa.
  - Không dấu.
  - Phủ định.
Commit `f0fdc911` dự kiến đã:
- Sửa lỗi HTTP 500 do cross-service contract drift.
- Thêm:
  - `painSeverity`
  - `urinarySymptoms`
  - `hydrationStatus`
- Forward các field từ Java sang Python.
- Giữ lại `vomiting` trong maternal flow.
- Không còn xem `vomiting` là pediatric-only ở Flutter.
- Đồng bộ maternal allowlist.
- Unknown/malformed intake trả safe 400 `TRIAGE-010`.
- Flutter phân biệt malformed input và unavailable service.
- Không hiển thị raw backend error.
- Thêm regression tests.
Không mặc định các thay đổi trên đã đúng hoàn toàn. Hãy xác minh từng điểm bằng diff và implementation.
==================================================
11. KẾT QUẢ TEST GẦN NHẤT
==================================================
Các kết quả dưới đây là kết quả của lần chạy gần nhất tại HEAD `f0fdc911`, không phải fact cố định. Hãy tự chạy lại nếu môi trường cho phép.
Spring:
`.\mvnw.cmd '-Dtest=TriageServiceTest,IntakeControllerTest,HttpChildTriageAiClientTest' test`
Kết quả gần nhất:
- 70 tests passed.
- 0 failures.
- 0 errors.
Python:
`.\.venv\Scripts\python.exe -m pytest -q`
Kết quả gần nhất:
- 321 passed.
- 2 dependency deprecation warnings.
Flutter:
`flutter test test/features/aiTriage/symptom_intake_screen_test.dart --reporter compact`
Kết quả gần nhất:
- 41 passed.
Chỉ chạy unit/widget/mock-based tests không ghi vào Supabase thật.
Không chạy integration test nếu chưa chứng minh datasource sử dụng:
- Mock.
- Embedded database.
- Testcontainers.
- Một test database riêng, không phải Supabase thật.
==================================================
12. PHẠM VI 1 — REVIEW DELTA BẮT BUỘC
==================================================
Review sâu ba commit:
- `9c057c11`
- `ae10e794`
- `f0fdc911`
Tập trung:
1. Intent extraction.
2. Vietnamese normalization.
3. Negation.
4. Mixed symptoms.
5. Maternal/pediatric separation.
6. Question relevance.
7. Cross-service contract parity.
8. New descriptive fields.
9. Continuation merge/persistence.
10. Unknown/malformed input.
11. RED precedence.
12. Python/Gemini failure.
13. Java fallback.
14. AI envelope validation.
15. Safe Flutter error behavior.
Phải trace tối thiểu:
- Một maternal start flow.
- Một maternal continuation flow.
- Một infant flow.
- Một toddler flow.
- Một unknown-symptom flow.
- Một mixed-symptom flow.
- Một AI-unavailable fallback flow.
- Một malformed-contract flow.
Đánh giá độc lập trường hợp:
- Stage: `PREGNANCY`
- Input: "tôi bị đau bụng tiêu chảy"
- Response gần nhất:
  - `ASK_MORE`
  - `painSeverity`
  - `duration`
  - `vomiting`
Không mặc định đây là bộ câu hỏi tối ưu. Hãy đánh giá:
- `vomiting` có thực sự là một trong ba câu quan trọng nhất không?
- Có câu nào liên quan hơn bị bỏ qua không?
- Việc ưu tiên câu hỏi có nhất quán với policy không?
- Phần nào cần clinical sign-off?
==================================================
13. PHẠM VI 2 — AUDIT TOÀN HỆ THỐNG
==================================================
Chỉ thực hiện sau khi hoàn thành review delta.
Audit:
1. Consent.
2. JWT và role.
3. Session ownership.
4. Idempotency.
5. Retry/concurrency.
6. Token expiry/account switching.
7. CORS với port chính thức 5173/5000.
8. Python direct-access security.
9. Input size/depth limits.
10. Prompt injection.
11. PII/PHI minimization.
12. Secret/health-data logging.
13. RAG/evidence governance.
14. Citation integrity.
15. Evidence cache freshness.
16. Health-memory trust boundary.
17. Gemini timeout/retry.
18. Gemini rate limiting.
19. Abuse/DoS/cost control.
20. Data retention.
21. Right-to-erasure/PDPA.
22. Clinical rule versioning.
23. Clinical approval process.
24. Full Flutter → Spring → Python → persistence E2E.
==================================================
14. CROSS-SERVICE CONTRACT MATRIX
==================================================
Lập bảng cho từng intake field:
- Field name.
- Ý nghĩa.
- Type.
- Maximum size/range.
- Nullable/required.
- Stage áp dụng.
- Flutter initialization.
- Flutter serialization.
- Java closed allowlist.
- Java DTO.
- Java validation.
- Java-to-Python serializer.
- Python schema.
- Question engine.
- Persistence behavior.
- Fallback behavior.
- Test coverage.
Đặc biệt kiểm tra:
- `painSeverity`
- `urinarySymptoms`
- `hydrationStatus`
- `vomiting`
- `duration`
- `temperatureC`
- `breathingStatus`
- `consciousnessStatus`
- `seizure`
- `parentFreeText`
- `symptomList`
- Pediatric-only fields.
- Maternal-only fields.
Tìm schema drift giữa ba codebase.
Đánh giá khả năng:
- Shared JSON Schema/OpenAPI.
- Contract-test matrix.
- Generated types.
- Một canonical field registry.
Không yêu cầu thực hiện thay đổi ở vòng review này.
==================================================
15. STAGE × SYMPTOM FAMILY MATRIX
==================================================
Lập ma trận:
Stages:
- PRECONCEPTION
- PREGNANCY
- POSTPARTUM
- INFANT
- TODDLER
Symptom families:
- ABDOMINAL
- DIARRHEA
- VOMITING
- FEVER
- RASH
- RESPIRATORY
- URINARY
- EAR
- HEADACHE
- Unknown/clarification
Với mỗi ô, ghi:
- Có hỗ trợ không.
- Câu hỏi canonical dự kiến.
- Question keys được phép.
- RED pre-screen liên quan.
- Fallback behavior.
- Test hiện có.
- Test còn thiếu.
- Có cần clinical approval không.
==================================================
16. CÁC TÌNH HUỐNG PHẢI KIỂM TRA
==================================================
- Có dấu/không dấu.
- Viết hoa/viết thường.
- Phủ định.
- Mixed symptoms.
- Unknown symptom.
- Instruction-like text.
- Prompt injection.
- Duplicate symptom.
- Contradictory facts.
- Wrong nested stage.
- Wrong top-level stage.
- Maternal nhận pediatric key.
- Pediatric nhận maternal key.
- Python timeout.
- Gemini timeout.
- Invalid JSON.
- Invalid AI envelope.
- Wrong response stage.
- Unknown question key.
- `ASK_MORE` nhưng không có câu hỏi hợp lệ.
- Continue sau khi trả lời:
  - `hydrationStatus`
  - `vomiting`
  - `painSeverity`
  - `urinarySymptoms`
- Retry cùng `clientRequestId`.
- Concurrent continuation.
- Expired token.
- Account switch.
- Consent version change.
Không tự thêm clinical threshold mới để làm test pass.
==================================================
17. SEVERITY RUBRIC
==================================================
P0 — Critical safety failure:
- Bỏ sót hoặc hạ cấp RED.
- Sai emergency action.
- Hỏi sai hoàn toàn đối tượng mẹ/bé gây nguy cơ.
- AI/Gemini override deterministic safety.
- Lộ secret hoặc health data nghiêm trọng.
- Bypass trust boundary dẫn đến thay đổi risk.
P1 — Release-blocking logic/contract failure:
- Contract drift gây HTTP 500.
- Mất dữ kiện quan trọng giữa các round.
- Fallback không an toàn.
- Stage/subject mismatch không bị chặn.
- Bypass auth/session ownership.
- Sai question routing nghiệp vụ quan trọng.
- Lỗi có khả năng chặn luồng triage thông thường.
P2 — Important hardening gap:
- Thiếu test quan trọng.
- Resilience weakness.
- Security/privacy weakness chưa gây ảnh hưởng trực tiếp.
- Schema duplication dễ tạo regression.
- Thiếu rate limit/retention/auditability.
- Thiếu E2E evidence.
P3 — Minor issue:
- UX nhỏ.
- Naming.
- Documentation.
- Duplication mức thấp.
- Cleanup/style.
==================================================
18. VERDICT GATE
==================================================
- Có P0 → `NOT READY`.
- Không có P0 nhưng còn P1 chưa xử lý → `NOT READY`.
- P1 có mitigation rõ ràng, không ảnh hưởng safety và được chứng minh bằng test → tối đa `READY WITH CONDITIONS`.
- Chỉ còn P2/P3 → `READY WITH CONDITIONS`.
- Không có actionable finding và có đủ test/E2E evidence → `READY`.
Không dùng cảm tính để chọn verdict.
==================================================
19. ĐỊNH DẠNG BÁO CÁO BẮT BUỘC
==================================================
Trình bày theo thứ tự:
A. Executive verdict
- `READY`
- `READY WITH CONDITIONS`
- `NOT READY`
Giải thích ngắn gọn theo verdict gate.
B. Findings-first
Sắp xếp:
- P0
- P1
- P2
- P3
Mỗi finding phải có:
- Severity.
- Tiêu đề.
- File và line chính xác.
- Bằng chứng code/test.
- Điều kiện tái hiện.
- Root cause.
- Ảnh hưởng clinical/logic/security/privacy/UX.
- Đề xuất sửa.
- Test cần bổ sung.
- Phân biệt verified fact và inference.
Nếu không có lỗi, ghi:
`No actionable findings`
Không tạo finding chỉ để lấp báo cáo.
C. Delta review
- Commit nào thay đổi gì.
- Phần nào đúng.
- Phần nào chưa đúng.
- Regression risk.
D. Cross-service contract matrix
E. Stage × symptom-family matrix
F. Architecture/trust-boundary validation
- Thành phần.
- Trách nhiệm.
- Nơi quyết định risk.
- Nơi Gemini chỉ được hỗ trợ.
- Fallback path.
G. Clinical governance gaps
- Rule đã deterministic.
- Rule cần bác sĩ xác nhận.
- Evidence thiếu.
- Threshold chưa được phép thay đổi.
H. Test evidence
- Test đã chạy.
- Kết quả.
- Test không chạy được.
- Lý do.
- Test còn thiếu.
- Xác nhận test không chạm Supabase thật.
I. Recommended roadmap
Chia thành:
- Must fix before release.
- Should improve next.
- Optional hardening.
- Không nên thay đổi nếu chưa có clinical approval.
J. Final conclusion
Trả lời rõ:
1. AI Triage hiện xử lý đúng đến mức nào?
2. Có còn khả năng hỏi sai đối tượng không?
3. Có còn khả năng hỏi sai triệu chứng không?
4. Có contract drift hoặc lỗi 500 tiềm ẩn không?
5. Fallback có đủ an toàn không?
6. Port/CORS đã đúng với 5173/5000 chưa?
7. Có cần sửa ngay trước release không?
8. Thứ tự triển khai đề xuất là gì?
==================================================
20. RÀNG BUỘC CUỐI
==================================================
- Không sửa code.
- Không sửa CORS.
- Không thêm port 5050.
- Không thay đổi Supabase.
- Không chạy test có nguy cơ ghi vào database thật.
- Không commit.
- Không push.
- Không tự tạo clinical guideline.
- Không tự thay đổi clinical threshold.
- Không đưa secret/health data vào báo cáo.
- Không kết luận dựa trên specification nếu implementation không khớp.
- Mọi finding phải có bằng chứng cụ thể.
