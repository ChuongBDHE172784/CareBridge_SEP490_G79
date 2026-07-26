# BÁO CÁO REVIEW TIẾN ĐỘ OV01 - EPIC 6

**Epic:** Epic 6 - Mother Lifecycle Orchestration Remediation
**Phạm vi:** OV-01 - Điều phối vòng đời người mẹ
**Ngày chốt số liệu:** 22/07/2026
**Nguồn trạng thái chính thức:** `_bmad-output/implementation-artifacts/sprint-status.yaml` (cập nhật 22/07/2026)

## 1. Mục tiêu Epic 6

Epic 6 khép kín luồng OV-01 từ lúc người dùng chọn vai trò Mẹ, khai báo thông tin nền và đồng thuận, tạo hành trình chuẩn, theo dõi thai kỳ, ghi nhận kết quả thai kỳ, phục hồi sau sinh, liên kết em bé tùy chọn, cho đến các nhánh an toàn, hỗ trợ chuyên gia, nội dung đã duyệt và kiểm thử end-to-end.

Nguyên tắc cốt lõi:

- Mỗi tài khoản chỉ có tối đa một hành trình Mẹ đang hoạt động.
- Mọi chuyển trạng thái quan trọng phải có lịch sử append-only.
- Không tạo hành trình nếu thiếu thông tin nền hoặc đồng thuận hợp lệ.
- Phục hồi sau sinh không phụ thuộc bắt buộc vào hồ sơ em bé.
- Việc liên kết em bé phải đúng chủ sở hữu và đúng ngữ nghĩa vòng đời.
- Các story còn lại về safety projection, chuyên gia, nội dung và E2E chưa hoàn tất không được trình bày như chức năng đã đóng.

## 2. Tổng quan tiến độ

| Chỉ số | Kết quả |
|---|---:|
| Trạng thái Epic 6 | **Đang thực hiện** |
| Tổng số story | 10 |
| Đã hoàn thành | 6/10 (60%) |
| Đang thực hiện | 0/10 (0%) |
| Chưa bắt đầu/backlog | 4/10 (40%) |
| Story point đã hoàn thành | 39/63 (61,9%) |
| Story point đang thực hiện | 0/63 (0%) |
| Story point chưa bắt đầu | 24/63 (38,1%) |

> Lưu ý: phần trăm theo số story và theo story point là hai cách đo khác nhau. Stories 6.5 và 6.6 đã hoàn tất sau kiểm thử thiết bị/PostgreSQL và review độc lập; Epic vẫn `in-progress` vì Stories 6.7–6.10 còn backlog.

### 2.1. Trạng thái từng story

| Story | Chức năng | Điểm | Trạng thái chính thức | Nhận định review |
|---|---|---:|---|---|
| 6.1 | Thiết lập vòng đời Mẹ chuẩn và lịch sử chuyển trạng thái | 8 | **Done** | Backend, mobile, migration, test và review đã hoàn tất |
| 6.2 | Thông tin nền và cổng đồng thuận bắt buộc | 5 | **Done** | Luồng fail-closed, retry/idempotency và kiểm thử thiết bị đã hoàn tất |
| 6.3 | Kết quả thai kỳ và chuyển sang hậu sản | 8 | **Done** | Có evidence append-only, policy chuyển trạng thái và luồng mobile nhạy cảm với mất thai |
| 6.4 | Phục hồi hậu sản trực tiếp, không phụ thuộc em bé | 5 | **Done** | Tạo POSTPARTUM với 0 em bé và recovery log CRUD đã hoàn tất |
| 6.5 | Liên kết tùy chọn 0..n em bé với hành trình | 5 | **Done** | OV01-MAN-018–022 PASS; cleanup legacy đã được phê duyệt/áp dụng; review cuối không còn High/Medium |
| 6.6 | Điều phối RED an toàn và triage hậu sản | 8 | **Done** | RED tạo/reuse emergency xác định, POSTPARTUM và năm origin chạy end-to-end; OV01-MAN-025–027 PASS |
| 6.7 | Lưu kết quả an toàn và quay về đúng màn hình nguồn | 8 | Backlog | Chưa triển khai trọn vẹn |
| 6.8 | Chuyển YELLOW tới chuyên gia đã xác minh, có đồng thuận | 5 | Backlog | Chưa triển khai trọn vẹn |
| 6.9 | Nội dung/checklist đã duyệt theo giai đoạn | 3 | Backlog | Chưa triển khai trọn vẹn |
| 6.10 | Traceability OV-01 và cổng chất lượng E2E | 8 | Backlog | Chưa đóng release gate |

### 2.2. Tiến độ theo wave

| Wave | Phạm vi | Trạng thái |
|---|---|---|
| Wave 0 - Contract foundation | 6.1, 6.2 | **Hoàn thành** |
| Wave 1 - Outcome and recovery | 6.3, 6.4, 6.5 | **Hoàn thành** |
| Wave 2 - Safety round trip | 6.6, 6.7 | **Đang thực hiện**; 6.6 done, 6.7 backlog |
| Wave 3 - Assisted support | 6.8, 6.9 | Chưa bắt đầu |
| Wave 4 - Release closure | 6.10 | Chưa bắt đầu |

## 3. Dataflow và bảng dữ liệu của các chức năng đã triển khai

Quy ước dataflow chung:

`Flutter Screen → Mobile Service → REST Controller → Application Service/Policy → Repository → PostgreSQL → Response/authoritative refresh → UI`

Quy ước đường dẫn trong các bảng bên dưới:

- Tiền tố Mobile `.../` = `05_Development/CareBridgeMobileApp/lib/features/`.
- Tiền tố Backend `.../` = `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/`.
- Tên migration nằm dưới `05_Development/CareBridgeAPI/src/main/resources/db/migration/`.

### 3.1. Story 6.1 - Vòng đời Mẹ chuẩn và lịch sử chuyển trạng thái

#### Dataflow

1. `mother_journey_screen.dart` gọi `journey_service.dart`.
2. Mobile gọi:
   - `POST /api/v1/journeys` để tạo hành trình.
   - `PUT /api/v1/journeys/{journeyId}` để chuyển/cập nhật trạng thái.
   - `GET /api/v1/journeys/{journeyId}/history` để đọc lịch sử.
   - `GET /api/v1/journeys/me/dashboard` để lấy projection hiện tại.
3. `JourneyController.java` nhận request và chuyển cho `JourneyServiceImpl.java`.
4. Mutation và history đi qua `JourneyTransitionServiceImpl.java`, nơi khóa/kiểm tra hành trình, cập nhật canonical state, ghi transition append-only, audit và phát event sau commit.
5. Repository ghi/đọc PostgreSQL; response được trả về màn hình và dashboard.

#### File code chính

| Tầng | File |
|---|---|
| Mobile UI | `05_Development/CareBridgeMobileApp/lib/features/journey/screens/mother_journey_screen.dart` |
| Mobile service | `05_Development/CareBridgeMobileApp/lib/features/journey/services/journey_service.dart` |
| Controller | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/controller/JourneyController.java` |
| Service | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/service/impl/JourneyServiceImpl.java` |
| Transition service | `05_Development/CareBridgeAPI/src/main/java/com/carebridge/backend/journey/service/impl/JourneyTransitionServiceImpl.java` |
| Repository | `.../journey/repository/MotherJourneyRepository.java` |
| Repository | `.../journey/repository/MotherJourneyTransitionRepository.java` |

#### Database table

| Table | Vai trò |
|---|---|
| `mother_journeys` | Trạng thái vòng đời chuẩn, stage, version và ownership |
| `mother_journey_transitions` | Lịch sử chuyển trạng thái append-only |
| `users` | Chủ sở hữu và actor của hành trình |
| `audit_logs` | Audit tạo/cập nhật hành trình |

Migration chính:

- `V20260718090000__canonical_mother_lifecycle_history.sql`
- `V20260718091000__enforce_mother_journey_transition_immutability.sql`

### 3.2. Story 6.2 - Thông tin nền và đồng thuận bắt buộc

#### Dataflow

1. `journey_onboarding_screen.dart` quản lý form; bản nháp được giữ bởi `journey_onboarding_draft_storage.dart`.
2. `journey_onboarding_service.dart` gọi:
   - `GET /api/v1/journey-onboarding/status`.
   - `POST /api/v1/journey-onboarding`.
3. `JourneyOnboardingController.java` chuyển request sang `JourneyOnboardingServiceImpl.java`.
4. Service khóa theo owner, kiểm tra submission replay/conflict, ghi một baseline revision và consent evidence hợp lệ trong transaction.
5. Status trả về router/UI. Khi tạo hoặc chuyển hành trình, consent gate được kiểm tra lại tại service boundary; thiếu/hết hạn/thu hồi/sai scope sẽ fail-closed.

#### File code chính

| Tầng | File |
|---|---|
| Mobile UI | `.../journey/screens/journey_onboarding_screen.dart` |
| Mobile service | `.../journey/services/journey_onboarding_service.dart` |
| Mobile draft | `.../journey/services/journey_onboarding_draft_storage.dart` |
| Controller | `.../journey/controller/JourneyOnboardingController.java` |
| Service | `.../journey/service/impl/JourneyOnboardingServiceImpl.java` |
| Repository | `.../journey/repository/MotherBaselineContextRepository.java` |
| Repository | `.../consent/repository/ConsentGrantRepository.java` |

#### Database table

| Table | Vai trò |
|---|---|
| `mother_baseline_contexts` | Các revision thông tin nền, mục tiêu vòng đời, preference và provenance |
| `consent_grants` | Evidence đồng thuận theo purpose/scope/policy version và hiệu lực |
| `mother_journeys` | Chỉ được tạo/chuyển khi qua cổng baseline/consent |
| `mother_journey_transitions` | Ghi lịch sử khi tiếp tục sang lifecycle |
| `users` | Ownership |
| `audit_logs` | Audit baseline submitted và consent granted |

Migration chính:

- `V20260718170000__mother_baseline_and_lifecycle_consent.sql`
- `V20260719120000__add_mother_baseline_provenance.sql`

### 3.3. Story 6.3 - Kết quả thai kỳ và chuyển sang hậu sản

#### Dataflow

1. `pregnancy_outcome_screen.dart` thu thập outcome; draft account-scoped được giữ trong `pregnancy_outcome_draft_store.dart`.
2. `journey_service.dart` gọi `POST /api/v1/journeys/{journeyId}/pregnancy-outcomes`.
3. `JourneyController.recordPregnancyOutcome()` chuyển request cho `JourneyTransitionServiceImpl.recordPregnancyOutcome()`.
4. Service khóa journey, kiểm tra submission/version và outcome policy; ghi evidence append-only, cập nhật canonical journey khi cần, ghi transition và audit/event.
5. Mobile đọc lại dashboard/history authoritative để hiển thị trạng thái hiện tại. Nhánh mất thai có thể vào recovery mà không tạo baby profile.

#### File code chính

| Tầng | File |
|---|---|
| Mobile UI | `.../journey/screens/pregnancy_outcome_screen.dart` |
| Mobile service | `.../journey/services/journey_service.dart` |
| Mobile draft | `.../journey/services/pregnancy_outcome_draft_store.dart` |
| Controller | `.../journey/controller/JourneyController.java` |
| Service/policy | `.../journey/service/impl/JourneyTransitionServiceImpl.java` |
| Repository | `.../journey/repository/PregnancyOutcomeEvidenceRepository.java` |
| Repository | `.../journey/repository/MotherJourneyRepository.java` |
| Repository | `.../journey/repository/MotherJourneyTransitionRepository.java` |

#### Database table

| Table | Vai trò |
|---|---|
| `pregnancy_outcome_evidence` | Evidence kết quả thai kỳ append-only, revision và submission |
| `mother_journeys` | Canonical state/outcome projection |
| `mother_journey_transitions` | Transition do kết quả thai kỳ tạo ra |
| `users` | Owner/actor |
| `audit_logs` | Audit outcome và lifecycle mutation |

Migration chính:

- `V20260719150000__pregnancy_outcome_evidence.sql`
- `V20260719150100__enforce_pregnancy_outcome_evidence_owner.sql`

### 3.4. Story 6.4 - Phục hồi hậu sản trực tiếp, không phụ thuộc em bé

#### Dataflow A - Tạo hành trình POSTPARTUM với 0 em bé

`postpartum_recovery_setup_screen.dart → JourneyService.createJourney() → POST /api/v1/journeys → JourneyController → JourneyServiceImpl/JourneyTransitionServiceImpl → consent validation → MotherJourneyRepository + MotherJourneyTransitionRepository → PostgreSQL`

Luồng này tạo/cập nhật hành trình hậu sản và transition nhưng **không tạo `baby_profiles`**.

#### Dataflow B - Recovery log CRUD

1. Các màn hình list/detail/form gọi `postpartum_log_service.dart`.
2. Endpoint:
   - `GET /api/v1/postpartum-logs?journeyId=...`
   - `GET /api/v1/postpartum-logs/{logId}`
   - `POST /api/v1/journeys/{journeyId}/postpartum-logs`
   - `PATCH /api/v1/postpartum-logs/{logId}`
   - `DELETE /api/v1/postpartum-logs/{logId}`
3. `PostpartumLogController.java` gọi `PostpartumLogServiceImpl.java`.
4. Service xác minh owner, canonical POSTPARTUM và consent; create dùng submission id để idempotent, delete là soft-delete.

#### File code chính

| Tầng | File |
|---|---|
| Setup UI | `.../journey/screens/postpartum_recovery_setup_screen.dart` |
| Dashboard | `.../journey/screens/mother_journey_screen.dart` |
| Log UI | `.../healthRecords/screens/postpartum_log_list_screen.dart` |
| Log UI | `.../healthRecords/screens/postpartum_log_detail_screen.dart` |
| Log UI | `.../healthRecords/screens/postpartum_log_form_screen.dart` |
| Mobile service | `.../healthRecords/services/postpartum_log_service.dart` |
| Controller | `.../health/controller/PostpartumLogController.java` |
| Service | `.../health/service/impl/PostpartumLogServiceImpl.java` |
| Repository | `.../health/repository/PostpartumLogRepository.java` |

#### Database table

| Table | Vai trò |
|---|---|
| `mother_journeys` | Hành trình POSTPARTUM canonical |
| `mother_journey_transitions` | CREATED/stage transition của hành trình hậu sản |
| `postpartum_logs` | Recovery log, submission id, trạng thái soft-delete |
| `mother_baseline_contexts` | Baseline hiện hành |
| `consent_grants` | Consent gate trước lifecycle/log operation |
| `intake_sessions` | Schema dependency: nhận thêm stage POSTPARTUM cho seam triage; recovery-log CRUD không ghi trực tiếp |
| `health_memory_entries` | Schema dependency: nhận thêm profile/stage POSTPARTUM; recovery-log CRUD không ghi trực tiếp |
| `users` | Ownership |
| `audit_logs` | Audit recovery log |

Migration chính:

- `V20260719170000__postpartum_log_idempotency.sql`
- `V20260719180000__add_postpartum_triage_stage.sql`

### 3.5. Story 6.5 - Liên kết tùy chọn 0..n em bé với hành trình

#### Dataflow

1. `mother_journey_screen.dart` chỉ mở entry khi server-authoritative journey đủ điều kiện; route tới `baby_journey_linkage_screen.dart`.
2. `baby_service.dart` gọi:
   - `POST /api/v1/babies` để create-with-link.
   - `PUT /api/v1/babies/{babyId}/journey-link` để link baby có sẵn.
   - `GET /api/v1/journeys/{journeyId}/babies` để refresh danh sách authoritative.
3. `BabyController.java` hoặc `JourneyBabiesController.java` gọi `BabyServiceImpl.java`.
4. Service khóa transaction/submission/baby, áp dụng policy: cùng owner, journey canonical active, outcome `LIVE_BIRTH`, không replay/conflict sai lệch.
5. Sau create/link, mobile tải lại danh sách từ server để cập nhật selector/dashboard. Hành động “Để sau” giữ nguyên trạng thái 0 em bé.

#### File code chính

| Tầng | File |
|---|---|
| Mobile UI | `.../baby/screens/baby_journey_linkage_screen.dart` |
| Mobile entry | `.../journey/screens/mother_journey_screen.dart` |
| Mobile service | `.../baby/services/baby_service.dart` |
| Controller | `.../baby/controller/BabyController.java` |
| Controller | `.../baby/controller/JourneyBabiesController.java` |
| Service | `.../baby/service/impl/BabyServiceImpl.java` |
| Eligibility policy | `.../baby/policy/BabyJourneyLinkagePolicy.java` |
| Access policy | `.../baby/policy/BabyAccessPolicy.java` |
| Rejection audit | `.../baby/service/BabyLinkRejectionAuditService.java` |
| Boundary audit filter | `.../baby/security/BabyLinkBoundaryAuditFilter.java` |
| Repository | `.../baby/repository/BabyProfileRepository.java` |
| Repository | `.../baby/repository/BabyLinkSubmissionRepository.java` |

#### Database table

| Table | Vai trò |
|---|---|
| `baby_profiles` | Hồ sơ em bé; `related_journey_id` là liên kết tùy chọn |
| `baby_link_submissions` | Idempotency và kết quả command create/link |
| `mother_journeys` | Journey được khóa và kiểm tra eligibility |
| `pregnancy_outcome_evidence` | Xác minh outcome tương thích `LIVE_BIRTH` |
| `users` | Owner/authorization |
| `audit_logs` | Audit accepted/rejected, tối thiểu hóa dữ liệu |
| `baby_journey_link_cleanup_summary` | Ghi kết quả tổng hợp đã tối thiểu hóa dữ liệu của migration cleanup legacy được phê duyệt |

Migration và assessment đã xác minh:

- `V20260720100000__secure_baby_journey_linkage.sql` — persistence/idempotency/audit boundary.
- `V20260722020000__quarantine_invalid_legacy_baby_journey_links.sql` — đã áp dụng sau Product/Data approval; `MISSING_OUTCOME_EVIDENCE=1`, `remainingInvalid=0`.
- `V20260722020100__quarantine_outcome_date_inconsistent_baby_journey_links.sql` — follow-up fail-closed; đã áp dụng với `affected=0`, `remainingMismatch=0`.
- Read-only assessment: `db/assessment/story_6_5_legacy_baby_links.sql`.

Xác minh đóng Story 6.5:

- Backend baby/linkage 51/51, PostgreSQL migration 11/11 và backend package PASS.
- Flutter focused cuối 25/25; manual OV01-MAN-018–022 PASS trên Android với evidence sanitized.
- User đã chấp nhận waiver cho lượt full Flutter sau notifier cuối và residual concurrency risk của migration đã áp dụng; review độc lập không còn High/Medium.

### 3.6. Story 6.6 - Điều phối RED xác định và triage hậu sản

#### Dataflow A - RED authoritative đến emergency

1. Năm production origin PRECONCEPTION, PREGNANCY, POSTPARTUM, INFANT và TODDLER mở safety intake bằng typed route context.
2. Python triage hoặc Java deterministic fallback đánh giá danger sign; universal maternal RED rules chạy trước Gemini và không cho AI prose hạ mức RED.
3. `TriageService.java` xác minh consent/stage, persist terminal RED và phát `EmergencyEscalationTriggered` trước general completion side work.
4. `EmergencyEscalationHandler.java` chuyển `intakeSessionId` và owner sang `EmergencyService.openOrReuseFromTriage()`.
5. Service lấy PostgreSQL advisory lock theo owner, replay association cũ hoặc reuse/tạo một ACTIVE emergency, rồi ghi `triage_emergency_escalations` trước khi RED success được trả về.
6. Chỉ session mới phát `EmergencySessionOpened`; PENDING outbox row được ghi atomically trong cùng transaction với emergency/association, còn delivery chạy AFTER_COMMIT nên failure downstream không rollback dữ liệu authoritative.

#### Dataflow B - Mobile mở emergency authoritative

`triage_safety_entry_action.dart → symptom_intake_screen.dart/risk_triage_result_screen.dart → GET /api/v1/emergency/sessions/active → emergency_map_screen.dart`

Mobile không POST một emergency thứ hai cho RED handoff. CTA lặp, retry và background/resume tải lại cùng ACTIVE session; typed current-session origin được giữ để quay về Mother journey hoặc đúng baby profile. Nếu không tải được authoritative session, UI hiển thị lỗi recoverable cùng fallback gọi 115/chăm sóc trực tiếp.

#### File code chính

| Tầng | File |
|---|---|
| Mobile origin | `.../aiTriage/widgets/triage_safety_entry_action.dart` |
| Mobile route context | `.../aiTriage/models/triage_entry_context.dart` |
| Mobile intake/result | `.../aiTriage/screens/symptom_intake_screen.dart`, `risk_triage_result_screen.dart` |
| Mobile emergency | `.../emergency/screens/emergency_map_screen.dart` |
| Spring triage | `.../triage/service/impl/TriageService.java` |
| Deterministic maternal rules | `.../triage/engine/UniversalMaternalRedRules.java` |
| Emergency handler/service | `.../emergency/service/EmergencyEscalationHandler.java`, `impl/EmergencyService.java` |
| Python triage | `05_Development/CareBridgeAITriageService/app/risk_rules.py`, `main.py` |

#### Database table

| Table | Vai trò |
|---|---|
| `intake_sessions` | Canonical stage, trạng thái hội thoại và terminal risk đã persist |
| `triage_emergency_escalations` | Association bền vững một intake RED đến authoritative emergency; khóa replay theo `intake_session_id` |
| `emergency_sessions` | Emergency session; partial unique index bảo đảm tối đa một ACTIVE session mỗi user |
| `emergency_notification_outbox` | PENDING delivery intent ghi cùng transaction; worker claim/delivery/retry chạy sau commit và theo dõi attempt cardinality |
| `family_alert_log` | Idempotency/audit theo session khi có recipient hợp lệ |
| `structured_intake_data` | Minimum deterministic UC-131 record cho RED mà không gọi Gemini lần hai |
| `users` | Ownership/isolation của intake và emergency |

Migration chính:

- `V20260722120000__guarantee_triage_emergency_idempotency.sql`

Migration đã được xác minh trên PostgreSQL 16/Testcontainers với toàn bộ 97 Flyway migrations và 10/10 integration tests, gồm duplicate reconciliation, PK/FK/check/index, partial ACTIVE uniqueness, rollback và referenced-history preservation.

## 4. Các chức năng chưa triển khai trọn vẹn

| Story | Dataflow/table dự kiến | Trạng thái báo cáo |
|---|---|---|
| 6.7 | Safety outcome → lifecycle timeline → continuation/origin state | Backlog; chưa được tính là đã triển khai |
| 6.8 | YELLOW → verified expert → consented minimum context → consultation | Backlog |
| 6.9 | Canonical stage → APPROVED-only checklist/content query | Backlog |
| 6.10 | Full OV-01 E2E + traceability + quality reports | Backlog |

Không liệt kê các table dự kiến của 6.7-6.10 như table “đã triển khai”, vì dễ tạo sai lệch trong buổi review. Story 6.6 đã được chuyển sang phần chức năng đã triển khai tại mục 3.6.

## 5. Kết quả kiểm thử và chất lượng đáng chú ý

| Story | Bằng chứng tiêu biểu |
|---|---|
| 6.1 | Backend Journey 45/45; mobile targeted 15/15; full mobile 187/187; manual composite 16/16 |
| 6.2 | Sau review: backend Story 6.1/6.2 46 test pass; Flutter focused 9/9; device missing/expired/revoked fail-closed |
| 6.3 | Focused backend, PostgreSQL outcome/concurrency, full Flutter 209 test và analyze đều pass |
| 6.4 | Backend focused 110/110; Flutter full 230/230; manual OV01-MAN-016/017 hoàn tất |
| 6.5 | Backend baby/linkage 51/51 gồm PostgreSQL 9/9; migration 11/11; Flutter focused cuối 25/25, full trước notifier delta 247/247; OV01-MAN-018–022 PASS; waiver cuối được ghi nhận |
| 6.6 | Backend focused 80 PASS và package PASS; PostgreSQL/Testcontainers 10/10 với 97 migrations; Python 213/213; Flutter origin 13/13 và full 287/287; OV01-MAN-025–027 PASS trên Android |

Manual suite OV-01 hiện có **17/34 READY, 8/34 PASS, 7/34 BLOCKED và 2/34 DEFERRED**. Gate riêng Story 6.6 đã PASS; full OV-01 gate vẫn BLOCKED bởi Stories 6.7–6.10, không phải do ca FAIL.

Phạm vi “sanitized evidence” trong báo cáo này chỉ gồm các artifact được commit hoặc được trích dẫn trực tiếp (Markdown/XML/PNG/DB aggregate). Transient local runtime logs không phải acceptance artifact và có thể chứa UUID synthetic đầy đủ, nên không được đính kèm/phát hành như evidence đã sanitize.

Project-wide backend regression ở một số lần chạy vẫn đỏ do baseline ngoài phạm vi Epic 6 (cấu hình Zego và các suite/module cũ). Không dùng kết quả focused pass để khẳng định toàn bộ backend project đang xanh.

## 6. Rủi ro và điểm cần nhấn mạnh trong buổi review

1. **Epic chưa đạt release-ready:** Wave 0–1 đã hoàn thành, Wave 2 mới hoàn tất 6.6; Stories 6.7–6.10 còn backlog.
2. **Story 6.5 đã đóng với residual risk được chấp nhận:** full Flutter rerun sau notifier cuối và migration concurrency window có waiver rõ ràng; không được diễn giải thành bằng chứng “không có rủi ro”.
3. **Safety projection chưa hoàn chỉnh:** Story 6.6 đã đóng RED escalation, emergency idempotency và current-session return; Story 6.7 vẫn là release blocker cho timeline projection, restart-safe continuation và exactly-once outcome persistence.
4. **Privacy/content chưa khép kín:** Story 6.8-6.9 còn thiếu verified expert handoff và APPROVED-only content boundary.
5. **Chưa có full OV-01 E2E closure:** Story 6.10 chưa triển khai; traceability và release quality gate còn mở.
6. **Provider-send exactly-once còn là boundary đã defer:** Story 6.6 bảo đảm một emergency/outbox theo session, nhưng loại bỏ hoàn toàn cửa sổ irreversible-send/database-commit cần provider idempotency hoặc per-recipient delivery protocol.

## 7. Thông điệp trình bày đề xuất

- “Epic 6 đang đạt **6/10 story done**, tương đương **39/63 story point**; bốn story 6.7–6.10 còn backlog.”
- “Nền tảng lifecycle, consent, pregnancy outcome, zero-baby postpartum và liên kết 0..n em bé đã được triển khai và có bằng chứng kiểm thử.”
- “Story 6.6 đã đóng P0 RED safety gap: năm production origin đều mở authoritative emergency, fallback khi Python unavailable không hạ mức nguy cơ và retry không tạo emergency/outbox trùng.”
- “Safety projection còn lại ở 6.7, expert/content ở 6.8–6.9 và E2E release closure ở 6.10; Epic 6 chưa release-ready.”

## 8. Nguồn bằng chứng

- `_bmad-output/implementation-artifacts/sprint-status.yaml`
- `_bmad-output/implementation-artifacts/6-2-add-baseline-context-and-required-consent-gate.md`
- `_bmad-output/implementation-artifacts/6-3-model-pregnancy-outcomes-and-postpartum-transition.md`
- `_bmad-output/implementation-artifacts/6-4-deliver-direct-postpartum-recovery-without-baby-dependency.md`
- `_bmad-output/implementation-artifacts/6-5-secure-optional-0-n-baby-to-journey-linkage.md`
- `_bmad-output/implementation-artifacts/6-6-guarantee-deterministic-safety-escalation-and-postpartum-triage.md`
- `_bmad-output/planning-artifacts/sprint-change-proposal-2026-07-17.md` — story-point baseline đã được phê duyệt (63 điểm)
- `04_Implement/UC22 - Canonical Mother Lifecycle and Transition History/`
- `_bmad-output/test-artifacts/test-design-epic-6.md`
- `_bmad-output/test-artifacts/test-design-progress.md`
- `_bmad-output/test-artifacts/story-6-5-manual/manual-run-summary.md`
- `_bmad-output/test-artifacts/story-6-6-manual/manual-run-summary.md` — chỉ các artifact committed/cited được tính là sanitized evidence
- `_bmad-output/test-artifacts/story-6-6-manual/db-evidence.md`
- `06_Testing/TestCases/mobile/OV-01-Mother-Lifecycle-Orchestration-Manual-Test-Guide.md`
- `06_Testing/TestResults/epic-6/`

---

**Kết luận:** Epic 6 đã hoàn thành 6/10 story (39/63 story point), bao phủ lifecycle từ onboarding đến postpartum, optional baby linkage và deterministic RED emergency round trip cho cả năm context. Phần còn lại là safety outcome projection/continuation, assisted support, reviewed content và E2E release closure ở Stories 6.7–6.10; trạng thái phù hợp tại thời điểm review vẫn là **IN PROGRESS**, chưa phải release-ready.
