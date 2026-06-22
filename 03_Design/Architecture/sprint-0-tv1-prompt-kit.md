# Bộ Prompt Thực Thi Sprint 0 - TV1 Shared Foundation

| Trường | Giá trị |
|---|---|
| Mã tài liệu | `CB-TV1-PROMPT-001` |
| Phiên bản | `1.0` |
| Trạng thái | `Draft - Pending Team Approval` |
| Workflow nguồn | `02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md` |
| Phạm vi | Sprint 0 - TV1 Shared Foundation Skeleton |
| Cập nhật | 2026-06-21 |

## Danh mục Work Package và Function Spec

| Work Package ID | Work Package Name | Function Specs liên quan |
|---|---|---|
| `WP01` | Schema and Identity Foundation | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` |
| `WP02` | API Response, Error and Security Contracts | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` |
| `WP03` | Authentication and Session Foundation | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16` |
| `WP04` | Privacy, Notification and Audit Ports | `3.1.4.1`, `3.1.5.1`, `3.1.5.2`, `3.1.5.3`, `3.1.5.4`, `3.2.5.1`, `3.2.5.2` |
| `WP05` | Web/Mobile Shared Clients and Contracts | `3.1.1.1`, `3.1.1.2`, `3.1.1.3`, `3.1.1.4`, `3.1.1.8`, `3.1.1.9`, `3.1.1.16`, `3.1.4.1`, `3.1.5.1`-`3.1.5.4`, `3.2.5.1`, `3.2.5.2` |

Function Spec có thể xuất hiện ở nhiều WP nền tảng, nhưng behavior chỉ được implement tại WP sở hữu: WP03 cho authentication/session và WP04 cho privacy/notification/audit.

## 1. Cách sử dụng

1. Mỗi work package nên được thực hiện trong một context mới.
2. Thay toàn bộ biến dạng `<...>` trước khi gửi prompt.
3. Đính kèm hoặc dẫn đúng đường dẫn TDS, Test Spec và source documents.
4. Không gọi bước Development trước khi TDS, Test Spec và Red Gate đã đạt yêu cầu.
5. Sau mỗi bước, lưu artifact vào đúng thư mục được quy định trong workflow.
6. Nếu agent phát hiện mâu thuẫn chưa có ADR, phải dừng và chuyển lại cho Architect.

Chu trình chuẩn:

```text
G0 Workspace Audit
 -> G1 Architecture/ADR
 -> G2 TDS Approval
 -> G3 Test Spec Approval
 -> G4 ATDD Red Gate
 -> G5 Development/Green
 -> G6 Code + Test Review
 -> G7 Traceability + DoD
```

## 2. Biến dùng chung

Sao chép block sau và điền trước mỗi phiên làm việc:

```text
PROJECT_ROOT: D:\SEP490\CareBridge_SEP490_G79
SPRINT: Sprint 0
OWNER: TV1 - Shared Foundation
WORK_PACKAGE_ID: <WP01|WP02|WP03|WP04|WP05>
WORK_PACKAGE_NAME: <Tên work package>
FUNCTION_SPEC_IDS: <Danh sách ID>
TDS_PATH: <Đường dẫn TDS>
TEST_SPEC_PATH: <Đường dẫn Test Spec>
EVIDENCE_PATH: 05_Testing/Evidence/TV1/<WORK_PACKAGE_ID>/
BRANCH: <Tên branch hiện tại>
```

Các nguồn bắt buộc:

```text
01_Requirements/SRS/Functional-Specifications.md
02_Design/Architecture/function-spec-task-allocation.md
02_Design/Architecture/project-structure-design.md
02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md
02_Design/Database/CareBridge_ERD_Description_and_Data_Dictionary_Updated.docx.md
02_Design/Database/CareBridge_ERD_Logical_Model_Updated.puml
_bmad-output/project-context.md
_bmad-output/template/PHASE-3_TDS.md
_bmad-output/template/PHASE-4_Test-Spec.md
```

## 3. Prompt G0 - Kiểm tra workspace và đầu vào

**Agent/skill:** Codex hoặc `bmad-investigate`

```text
Hãy kiểm tra mức độ sẵn sàng để triển khai <WORK_PACKAGE_ID> -
<WORK_PACKAGE_NAME> của Sprint 0 TV1.

Đọc đầy đủ:
- _bmad-output/project-context.md
- 02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md
- 02_Design/Architecture/function-spec-task-allocation.md
- Các tài liệu nguồn liên quan được liệt kê trong workflow.

Thực hiện kiểm tra chỉ đọc:
1. Kiểm tra branch, git status và các thay đổi chưa commit.
2. Liệt kê source files, migrations, entities, tests và contracts liên quan.
3. Đối chiếu code hiện tại với Function Spec IDs: <FUNCTION_SPEC_IDS>.
4. Phát hiện xung đột giữa SRS, kiến trúc, ERD, migration và entity.
5. Phân loại phát hiện thành: blocker, rủi ro, khoảng trống và phần đã có.
6. Xác nhận work package có thể review/merge độc lập hay không.

Không sửa file và không triển khai code.

Đầu ra bắt buộc:
- Kết luận GO hoặc NO-GO cho G0.
- Danh sách blocker kèm bằng chứng file/dòng.
- Danh sách tài liệu/ADR cần tạo trước khi tiếp tục.
- Phạm vi chính xác đề xuất cho TDS.
```

## 4. Prompt G1 - Architect tạo ADR và TDS

**Agent/skill:** Winston / `bmad-agent-architect`

```text
Bạn là System Architect của CareBridge. Hãy thiết kế
<WORK_PACKAGE_ID> - <WORK_PACKAGE_NAME> cho Sprint 0 TV1.

Đọc đầy đủ các tài liệu nguồn bắt buộc, báo cáo G0 và code hiện tại.
Sử dụng _bmad-output/template/PHASE-3_TDS.md làm cấu trúc tài liệu.
Tạo hoặc cập nhật tài liệu tại:
<TDS_PATH>

Yêu cầu:
1. Ánh xạ trực tiếp Function Spec IDs <FUNCTION_SPEC_IDS> tới component/file.
2. Ghi nhận mọi quyết định quan trọng bằng ADR: context, options,
   decision, consequences, trade-offs và security/privacy impact.
3. Chốt entity, migration, API, event, error và authorization contracts.
4. Mô tả happy path, failure path, security path và state transition.
5. Xác định NFR có thể đo được; không sao chép SLA mẫu khi chưa có cơ sở.
6. Viết implementation steps theo dependency và kèm file path cụ thể.
7. Viết migration, verification, rollback và incident runbook.
8. Điều chỉnh toàn bộ ví dụ template sang Java 21, Spring Boot, JPA,
   Flyway, PostgreSQL, React TypeScript và Flutter.
9. Section không áp dụng phải ghi N/A và nêu lý do, không xóa im lặng.
10. Không triển khai production code.

Quy tắc bắt buộc:
- Không chỉnh sửa migration đã áp dụng; chỉ dùng forward migration.
- Không tạo package trùng nghĩa với security, identity, consent, audit.
- Không suy đoán luật/compliance từ dữ liệu mẫu trong template.
- Dừng nếu thiếu quyết định có thể thay đổi schema hoặc public contract.

Đầu ra:
- TDS hoàn chỉnh với trạng thái In Review.
- Danh sách ADR cần người có thẩm quyền phê duyệt.
- Checklist G1/G2 và các mục chưa đạt.
```

## 5. Prompt review và phê duyệt TDS

**Agent/skill:** Winston / `bmad-review-adversarial-general`

```text
Hãy review TDS tại <TDS_PATH> theo workflow
02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md.

Đối chiếu TDS với SRS, task allocation, project structure, ERD, code,
migrations và _bmad-output/project-context.md.

Kiểm tra:
- Traceability có đầy đủ và đúng phạm vi không.
- ADR có giải quyết hết xung đột không.
- Entity, migration và foreign key có đồng nhất không.
- API/event/error/RBAC contracts có đủ rõ để implement không.
- Security/privacy/audit invariants có fail-closed không.
- Implementation và rollback steps có thể thực thi không.
- Có placeholder, TBD, giả định ẩn hoặc scope creep không.

Không sửa production code.

Đầu ra:
- APPROVE hoặc CHANGES REQUIRED.
- Phát hiện theo Must Fix, Should Fix, Follow-up.
- File/section cụ thể cần sửa.
- Chỉ đề nghị chuyển trạng thái TDS sang Approved khi toàn bộ Must Fix đã đóng.
```

## 6. Prompt G3 - Test Architect tạo Test Spec

**Agent/skill:** Murat / `bmad-testarch-test-design`

```text
Hãy tạo Test Specification cho <WORK_PACKAGE_ID> -
<WORK_PACKAGE_NAME> dựa trên TDS đã Approved tại <TDS_PATH>.

Sử dụng _bmad-output/template/PHASE-4_Test-Spec.md và lưu kết quả tại:
<TEST_SPEC_PATH>

Đọc thêm SRS, ADR, business rules, project-context.md, code/test hiện tại
và workflow TV1.

Yêu cầu:
1. Ghi rõ các sai lệch logic giữa tài liệu và code/schema hiện tại.
2. Derive test conditions từ Function Spec IDs <FUNCTION_SPEC_IDS>,
   TDS, ADR và approved contracts.
3. Mỗi acceptance criterion phải ánh xạ tới ít nhất một test case.
4. Bao phủ unit, policy, service, repository, controller, security,
   migration và integration theo mức rủi ro.
5. Bao phủ validation, boundary, state transition, ownership,
   unauthorized, forbidden, expiry, replay và concurrency khi liên quan.
6. Mỗi expected result phải ghi Oracle Source; không dùng AI assumption.
7. Chỉ dùng dữ liệu giả, không dùng PII, OTP, token hoặc secret thật.
8. Ghi đúng đường dẫn test file dự kiến cho từng test case.
9. Định nghĩa Entry, Suspension và Exit Criteria.
10. Chuyển ví dụ Prisma/NestJS/npm sang stack CareBridge thực tế.

Quy ước CareBridge:
- Backend: JUnit/Spring tests, file *Test.java, Maven Wrapper.
- Database: PostgreSQL/Flyway, không giả định H2 tương đương.
- Frontend: lint/build; không tạo test runner ad-hoc.
- Mobile: flutter_test, file *_test.dart, flutter analyze.

Không viết production code.

Đầu ra:
- Test Spec trạng thái In Review.
- Risk/coverage summary.
- Danh sách test cần viết ở Red phase.
- Checklist G3 và các mục chưa đạt.
```

## 7. Prompt review Test Spec

**Agent/skill:** `bmad-testarch-test-review`

```text
Review Test Spec tại <TEST_SPEC_PATH> dựa trên TDS Approved
<TDS_PATH>, source requirements và workflow TV1.

Kiểm tra:
- Mọi test condition có test case tương ứng.
- Mọi expected value có Oracle Source.
- Không có Green-from-Birth, tautology hoặc shared mutable state.
- Test security bao phủ đúng attack surface.
- Test migration bao phủ database mới và database đã chạy V1-V5.
- Test paths và commands phù hợp với Java/React/Flutter hiện tại.
- Không import contract chưa tồn tại mà không có kế hoạch stub rõ ràng.
- Entry/Exit Criteria có thể kiểm chứng.

Đầu ra:
- APPROVE hoặc CHANGES REQUIRED.
- Must Fix, Should Fix, Follow-up.
- Chỉ đề nghị chuyển Test Spec sang Approved khi toàn bộ Must Fix đã đóng.
```

## 8. Prompt G4 - ATDD và Red Gate

**Agent/skill:** `bmad-testarch-atdd`

```text
Thực hiện ATDD Red phase cho <WORK_PACKAGE_ID> theo:
- TDS Approved: <TDS_PATH>
- Test Spec Approved: <TEST_SPEC_PATH>
- Workflow: 02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md

Yêu cầu:
1. Tạo các automated tests đã được duyệt trong Test Spec.
2. Chỉ tạo contract/stub tối thiểu để tests compile khi cần.
3. Không triển khai production behavior dùng để làm test pass.
4. Chạy targeted tests và xác nhận mỗi test mới FAIL vì đúng behavior
   còn thiếu, không phải vì import/config/environment bị hỏng.
5. Nếu test PASS với empty/throw stub, dừng, phân tích và viết lại test.
6. Không sửa, skip hoặc làm yếu assertion để đạt Red Gate.
7. Lưu command, timestamp, test IDs, kết quả và root cause tại:
   <EVIDENCE_PATH>/red-gate-evidence.log
8. Cập nhật Red-Green-Refactor Tracker trong Test Spec.

Đầu ra:
- Danh sách test files được tạo/sửa.
- Bảng test ID -> failure mong đợi -> failure thực tế.
- Kết luận RED GATE PASS hoặc FAIL.
- Blocker cần Architect/Test Architect xử lý.
```

## 9. Prompt G5 - Developer triển khai code

**Agent/skill:** Amelia / `bmad-dev-story` hoặc `bmad-quick-dev`

```text
Triển khai <WORK_PACKAGE_ID> - <WORK_PACKAGE_NAME> cho Sprint 0 TV1.

Nguồn thực thi duy nhất:
- TDS Approved: <TDS_PATH>
- Test Spec Approved: <TEST_SPEC_PATH>
- Red Gate evidence: <EVIDENCE_PATH>/red-gate-evidence.log
- _bmad-output/project-context.md
- 02_Design/Architecture/sprint-0-tv1-code-implementation-workflow.md

Trước khi sửa code:
1. Kiểm tra git status và bảo toàn thay đổi không liên quan.
2. Xác minh TDS/Test Spec đã Approved.
3. Xác minh Red Gate PASS và failures có ý nghĩa.
4. Liệt kê files dự kiến thay đổi và đối chiếu với scope.

Triển khai theo Red-Green-Refactor:
1. Viết tối thiểu production code để targeted tests pass.
2. Chạy lại targeted tests sau từng phần phụ thuộc.
3. Refactor nhưng không đổi behavior.
4. Chạy toàn bộ validation của module bị ảnh hưởng.
5. Cập nhật contracts, TDS/Test Spec changelog và traceability khi cần.

Quy tắc bắt buộc:
- Java 21, jakarta.*, constructor injection và domain-first packages.
- Controller không chứa business logic và không trả JPA entity.
- Service chịu transaction/authorization/consent boundary.
- Endpoint /api/v1/** mặc định authenticated trừ khi TDS cho phép public.
- Không sửa migration đã áp dụng.
- Không log/lộ OTP, refresh token, JWT secret, password hash hoặc PII nhạy cảm.
- Audit append-only; notification payload theo minimum necessary.
- Không tự mở rộng scope hoặc đưa ra quyết định kiến trúc chưa có ADR.

Nếu phát hiện thiếu quyết định, dừng và trả về Architect thay vì tự đoán.

Đầu ra:
- Tóm tắt thay đổi theo acceptance criterion.
- Danh sách files đã sửa.
- Commands và kết quả test/build/analyze.
- Phân biệt code failure và environment failure.
- Mục còn lại hoặc deferred work.
- Kết luận GREEN GATE PASS hoặc FAIL.
```

## 10. Prompt vòng sửa lỗi Development

**Agent/skill:** Amelia / `bmad-dev-story`

```text
Tiếp tục sửa <WORK_PACKAGE_ID> dựa trên các finding đã được duyệt dưới đây:
<DÁN DANH SÁCH FINDING>

Nguồn chuẩn vẫn là <TDS_PATH>, <TEST_SPEC_PATH>, project-context.md và
workflow TV1. Không thay đổi behavior ngoài các finding trong phạm vi.

Với mỗi finding:
1. Xác nhận root cause bằng code/test evidence.
2. Bổ sung hoặc điều chỉnh test để tái hiện lỗi nếu Test Spec yêu cầu.
3. Xác nhận test FAIL vì lỗi trước khi sửa production code.
4. Sửa tối thiểu, chạy targeted tests và full affected suite.
5. Báo trạng thái RESOLVED, DEFERRED hoặc BLOCKED kèm lý do.

Không đóng finding chỉ bằng giải thích; phải có evidence phù hợp.
```

## 11. Prompt G6 - Code Review

**Agent/skill:** `bmad-code-review`

```text
Review implementation của <WORK_PACKAGE_ID> theo cách đối kháng.

Đối chiếu:
- Git diff và code hiện tại.
- TDS Approved: <TDS_PATH>.
- Test Spec Approved: <TEST_SPEC_PATH>.
- Red/Green evidence tại <EVIDENCE_PATH>.
- project-context.md và workflow TV1.

Ưu tiên kiểm tra:
- Acceptance criteria và traceability bị thiếu.
- Entity-migration mismatch, dữ liệu mất hoặc rollback không an toàn.
- Public endpoint quá rộng, thiếu authentication/authorization.
- OTP expiry/attempt/single-use.
- Refresh-token hashing, rotation, replay và concurrency.
- Role/permission expansion và ownership.
- Consent fail-closed.
- Audit append-only và sensitive-data sanitization.
- Notification idempotency và minimum-necessary payload.
- ApiResponse/ErrorResponse không nhất quán.
- Test yếu, assertion sai hoặc chỉ test happy path.

Không sửa code trong lượt review trừ khi tôi yêu cầu rõ.

Đầu ra:
- Findings theo Must Fix, Should Fix, Follow-up.
- Mỗi finding có severity, file/dòng, bằng chứng, tác động và hướng sửa.
- Kết luận APPROVED hoặc CHANGES REQUIRED.
```

## 12. Prompt G6 - Review chất lượng test sau khi sửa code

**Agent/skill:** `bmad-testarch-test-review`

```text
Đánh giá chất lượng tests thực tế của <WORK_PACKAGE_ID> sau implementation.

Đối chiếu test files với <TEST_SPEC_PATH>, <TDS_PATH>, Red Gate evidence
và production diff.

Kiểm tra:
- Test có thực sự bắt được lỗi hay chỉ xác nhận mock.
- Assertion có đủ cụ thể và có oracle source.
- Boundary/state/security/concurrency paths có đủ.
- Không shared mutable state, order dependency, sleep tùy tiện hoặc flaky setup.
- Không test private implementation detail thay vì observable behavior.
- Không skip, disable hoặc weaken test sau Red phase.
- Test data không chứa PII/secret thật.

Đầu ra:
- Điểm/chất lượng tổng quan.
- Must Fix, Should Fix, Follow-up.
- Kết luận TEST REVIEW PASS hoặc FAIL.
```

## 13. Prompt G7 - Traceability và Definition of Done

**Agent/skill:** `bmad-testarch-trace`

```text
Tạo traceability matrix và quality-gate decision cho <WORK_PACKAGE_ID>.

Nguồn:
- Function Spec IDs: <FUNCTION_SPEC_IDS>
- TDS: <TDS_PATH>
- Test Spec: <TEST_SPEC_PATH>
- Production code và automated tests.
- Evidence tại <EVIDENCE_PATH>.
- Code Review và Test Review findings.
- Workflow TV1 Definition of Done.

Lập mapping:
Function Spec ID
 -> TDS section/ADR
 -> production file/component
 -> test condition/test ID
 -> automated test file
 -> verification evidence

Kiểm tra toàn bộ G0-G7 và phân loại khoảng trống:
- Blocker.
- Risk accepted có người chịu trách nhiệm.
- Deferred có scope/sprint rõ ràng.
- Không áp dụng có lý do.

Lưu báo cáo tại:
<EVIDENCE_PATH>/traceability-report.md

Đầu ra:
- PASS, PASS WITH RISKS hoặc FAIL.
- Khoảng trống traceability.
- Trạng thái từng Definition of Done item.
- Khuyến nghị merge hoặc không merge.
```

## 14. Prompt tạo handoff sau mỗi phiên

**Agent/skill:** Codex hiện tại

```text
Tạo handoff ngắn cho <WORK_PACKAGE_ID> để context mới tiếp tục chính xác.

Nội dung bắt buộc:
1. Mục tiêu và scope đã duyệt.
2. Trạng thái hiện tại của G0-G7.
3. TDS/Test Spec/evidence paths.
4. ADR và quyết định quan trọng.
5. Files đã thay đổi.
6. Commands đã chạy và kết quả.
7. Findings chưa đóng.
8. Bước kế tiếp duy nhất được phép thực hiện.
9. Stop conditions cần lưu ý.

Không thêm quyết định mới trong handoff.
```

## 15. Prompt riêng cho từng Work Package

Các block dưới đây bổ sung vào prompt Architect, Test Architect và Developer tương ứng.

### WP01 - Schema and Identity Foundation

```text
WORK_PACKAGE_ID: WP01
WORK_PACKAGE_NAME: Schema and Identity Foundation
FUNCTION_SPEC_IDS: 3.1.1.1, 3.1.1.2, 3.1.1.3, 3.1.1.4,
3.1.1.8, 3.1.1.9, 3.1.1.16, 3.1.4.1, 3.1.5.1, 3.1.5.2,
3.1.5.3, 3.1.5.4, 3.2.5.1, 3.2.5.2

Phạm vi trọng tâm:
- Xác định UUID là schema chuẩn theo ERD hay đưa ra ADR khác có bằng chứng.
- Forward migration từ V1-V5, không chỉnh sửa migration đã áp dụng.
- users, roles, user_roles, user_sessions và refresh-token/session model.
- Đồng bộ consent_grants, audit_logs và security_events với identifier strategy.
- Hibernate ddl-auto=validate phải thành công.
- Kiểm thử database sạch và database đã ở V1-V5.

Ngoài phạm vi:
- Màn hình auth hoàn chỉnh.
- Firebase/OTP provider thật.
- Session management UI hoàn chỉnh.
```

### WP02 - API Response, Error and Security Contracts

```text
WORK_PACKAGE_ID: WP02
WORK_PACKAGE_NAME: API Response, Error and Security Contracts
FUNCTION_SPEC_IDS: 3.1.1.1, 3.1.1.2, 3.1.1.3, 3.1.1.4,
3.1.1.8, 3.1.1.9, 3.1.1.16, 3.1.4.1, 3.1.5.1, 3.1.5.2,
3.1.5.3, 3.1.5.4, 3.2.5.1, 3.2.5.2

Phạm vi trọng tâm:
- ApiResponse, ErrorResponse, ErrorDetail và error-code catalog.
- GlobalExceptionHandler và Spring Security authentication/authorization errors.
- Public endpoint allowlist hẹp; /api/v1/** authenticated mặc định.
- RoleCode, Permission và RBAC matrix.
- Method-level authorization cho luồng nhạy cảm.
- CORS lấy từ external configuration.

Ngoài phạm vi:
- Business endpoint của TV2-TV5.
- Admin role-management UI hoàn chỉnh.
```

### WP03 - Authentication and Session Foundation

```text
WORK_PACKAGE_ID: WP03
WORK_PACKAGE_NAME: Authentication and Session Foundation
FUNCTION_SPEC_IDS: 3.1.1.1, 3.1.1.2, 3.1.1.3, 3.1.1.4,
3.1.1.8, 3.1.1.9, 3.1.1.16

Phạm vi trọng tâm:
- Register/login/verify OTP/refresh/logout/profile contracts.
- OTP provider interface và local-only mock an toàn.
- OTP expiry, attempt limit và single-use.
- Refresh-token hash, rotation, revoke, replay và concurrency.
- SessionService create/validate/revoke-current/revoke-all skeleton.
- Audit login/logout/OTP/token security events.

Ngoài phạm vi:
- Password recovery hoàn chỉnh.
- Auth screens hoàn chỉnh.
- Provider OTP/email/push production.
```

### WP04 - Privacy, Notification and Audit Ports

```text
WORK_PACKAGE_ID: WP04
WORK_PACKAGE_NAME: Privacy, Notification and Audit Ports
FUNCTION_SPEC_IDS: 3.1.4.1, 3.1.5.1, 3.1.5.2, 3.1.5.3,
3.1.5.4, 3.2.5.1, 3.2.5.2

Phạm vi trọng tâm:
- PrivacyDecisionService/ConsentCheckPolicy contract.
- Data type, purpose, recipient, expiry và revocation semantics.
- NotificationPublisher và NotificationProvider interfaces.
- Typed reminder/community/consultation/emergency notification events.
- AuditEvent và SecurityEvent typed contracts.
- Append-only audit, idempotency, correlation ID và payload sanitization.

Ngoài phạm vi:
- Notification center CRUD hoàn chỉnh.
- Firebase provider thật.
- Admin audit/security screens hoàn chỉnh.
```

### WP05 - Web/Mobile Shared Clients and Contracts

```text
WORK_PACKAGE_ID: WP05
WORK_PACKAGE_NAME: Web/Mobile Shared Clients and Contracts
FUNCTION_SPEC_IDS: 3.1.1.1, 3.1.1.2, 3.1.1.3, 3.1.1.4,
3.1.1.8, 3.1.1.9, 3.1.1.16, 3.1.4.1, 3.1.5.1, 3.1.5.2,
3.1.5.3, 3.1.5.4, 3.2.5.1, 3.2.5.2

Phạm vi trọng tâm:
- OpenAPI auth/shared contract.
- Notification/audit JSON schemas.
- RBAC matrix, consent scope và error-code catalogs.
- Frontend Axios client, response/error parsing, auth/session abstraction.
- Mobile base API client, environment, auth/session và token-storage abstraction.
- Authorization Bearer convention và refresh behavior contract.
- Frontend lint/build và Mobile test/analyze.

Ngoài phạm vi:
- Hoàn thiện tất cả auth screens.
- Tạo frontend test runner ad-hoc.
- Lưu service-role key trên web/mobile.
```

## 16. Prompt kiểm tra toàn Sprint 0 TV1

**Agent/skill:** `bmad-checkpoint-preview` hoặc `bmad-check-implementation-readiness`

```text
Thực hiện checkpoint cuối cho toàn bộ Sprint 0 TV1 Shared Foundation.

Đọc workflow, TDS/Test Specs WP01-WP05, contracts, code, tests,
traceability reports và git diff tổng.

Kiểm tra:
- Các work package đã hoàn thành đúng thứ tự dependency.
- Shared contracts ổn định và TV2-TV5 có thể tiêu thụ mà không sửa internals.
- Không còn entity-migration blocker.
- Auth/session/RBAC/privacy/notification/audit skeleton nhất quán.
- Web/mobile client conventions khớp OpenAPI/error contract.
- Mọi G0-G7 đều có evidence.
- Phần thuộc Sprint 1-4 không bị kéo sai vào Sprint 0.

Đầu ra:
- Sprint 0 TV1 READY hoặc NOT READY.
- Blocker theo work package.
- Risks/deferred items theo sprint đích.
- Thứ tự review cho người duyệt.
- Danh sách validation commands và evidence cuối.
```

## 17. Checklist trước khi gửi bất kỳ prompt nào

- [ ] Đã thay mọi biến `<...>`.
- [ ] Đã chọn đúng work package.
- [ ] Đã mở context mới nếu chuyển agent/skill.
- [ ] Đã dẫn đúng TDS/Test Spec mới nhất.
- [ ] Đã nêu rõ trạng thái Approved khi phù hợp.
- [ ] Đã giới hạn hành động: design, test, implement hay review.
- [ ] Đã yêu cầu bảo toàn thay đổi không liên quan.
- [ ] Đã yêu cầu evidence và stop conditions.
- [ ] Không yêu cầu agent tự phê duyệt quyết định cần con người ký.

## 18. Quy tắc vàng

```text
Không có ADR rõ ràng       -> Không chốt kiến trúc.
Không có TDS Approved      -> Không tạo production code.
Không có Test Spec Approved -> Không viết ATDD chính thức.
Không có Red Gate PASS     -> Không implement behavior.
Không có Green + Review    -> Không đề nghị merge.
Không có Traceability      -> Không xem là Done.
```
