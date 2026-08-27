# Hướng dẫn thực thi System Test CareBridge

Phiên bản: 1.0  
Ngày lập: 09/08/2026  
Nguồn test case: [Report5_System Test](https://docs.google.com/spreadsheets/d/1IylEsao-iPCiDFrmZVcYDPlBjP0ouQRi9aonoj2pfkw/edit)  
Phạm vi: 121 use case MF-01–MF-14, code backend/web/mobile/AI, workflow liên vai trò, privacy/consent, AWS–Cloudflare–GitLab Pages và NFR.

## 1. Kết quả cần đạt

Bộ System Test có 235 case:

| Nhóm | Số case | Ý nghĩa |
|---|---:|---|
| Truy vết UC-01…UC-121 | 121 | Mỗi use case có ít nhất một luồng chuẩn System E2E/API |
| Negative/risk theo module | 42 | Sai role/owner, validation/biên, retry/idempotency/dependency |
| Sinh từ code | 28 | Những hành vi cụ thể nhìn thấy trong controller/service/client/job |
| Cross-role | 12 | Luồng xuyên Mother, Family, Expert, Moderator, Content Admin, System Admin |
| Privacy & Consent | 12 | RBAC, IDOR, token, consent, file, log, retention |
| Resilience & NFR | 20 | Deploy, response time, load, CORS, cache, failure/recovery, CI |
| **Tổng** | **235** | Trạng thái ban đầu đều là `Pending` |

Baseline chính là SRS 121 use case/MF-01–MF-14. Tài liệu thiết kế cũ còn ghi 91 use case/MF-01–MF-10; khi gặp chênh lệch, dùng source đang chạy và SRS 121 UC làm căn cứ. Chức năng không có trên build không được đánh `Passed`: giữ `Pending`, ghi `Blocked: <lý do>` trong Note và tạo issue xác nhận phạm vi. Chỉ dùng `N/A` khi PO/BA xác nhận ngoài phạm vi release.

## 2. Cách dùng Google Sheet

Mỗi tab module có các cột:

- `A–E`: ID, mô tả, thủ tục, expected result và pre-condition.
- `F–H`: trạng thái, ngày, tester của vòng 1.
- `I–K`: vòng 2 sau khi sửa lỗi.
- `L–N`: vòng 3/regression cuối.
- `O`: trace, nguồn code, risk và ghi chú/evidence.

Giá trị hợp lệ cho status là `Pending`, `Passed`, `Failed`, `N/A`.

Quy tắc ghi:

1. Chọn đúng vòng test; không sửa kết quả vòng cũ.
2. Ghi ngày theo `dd/mm/yyyy` và tên tester.
3. `Passed`: tất cả expected result đều được chứng minh.
4. `Failed`: có ít nhất một expected result sai. Ghi bug ID, actual result và evidence link ở Note.
5. `Pending`: chưa chạy, bị block, hoặc NFR chưa có threshold. Với block, thêm `Blocked: ...`.
6. `N/A`: chỉ dùng khi có xác nhận phạm vi; ghi người xác nhận và ngày.
7. Không dán access token, refresh token, OTP, password, ảnh giấy tờ thật hoặc dữ liệu sức khỏe thật vào Sheet/evidence.

## 3. Môi trường và quyền truy cập

### 3.1 Môi trường nên có

| Môi trường | Dùng cho | Không được làm |
|---|---|---|
| Local/Docker | Debug, seed, API negative, job test | Không coi là bằng chứng deploy production |
| Staging giống AWS | Full regression, fault injection, load, worker/job, migration | Không dùng dữ liệu người thật |
| Production | Smoke read và một write/readback an toàn bằng account test | Không fault injection, load test, đổi clock, tắt dependency hoặc xóa dữ liệu diện rộng |

Topology cần ghi vào evidence:

`Browser → carebridgevn.site → Cloudflare → GitLab Pages`  
`Browser/Mobile → api.carebridgevn.site → Cloudflare → EC2/Nginx → Spring Boot/PostgreSQL → AI/FCM/Map/Call/Storage`

### 3.2 Tài khoản tối thiểu

Chuẩn bị account riêng, không dùng credential trong README trên production:

- Guest chưa đăng ký.
- Mother A và Mother B.
- Family A, Family B.
- Expert Applicant, Expert Verified A, Expert Verified B.
- Moderator.
- Content Admin.
- System Admin.
- Một account suspended và một account deactivated.

Mỗi tester dùng tiền tố dữ liệu: `ST-<ngày>-<tester>-<caseId>`, ví dụ `ST-20260809-LAM-ST-03-01`.

### 3.3 Dữ liệu tối thiểu

- Hai mother độc lập; mỗi mother có journey và ít nhất hai baby.
- Baby có daily logs, growth, milestone và vaccine ở PENDING/COMPLETED/POSTPONED.
- Care group có member với các permission khác nhau.
- Expert có hồ sơ ở DRAFT/REJECTED/PENDING/VERIFIED và availability.
- Community có question/answer/topic/report ở nhiều trạng thái.
- Content/checklist/exercise có draft, pending review, published, archived/unpublished và ≥2 version.
- Health record có attachment private; file corpus gồm ảnh/PDF hợp lệ, rỗng, hỏng, MIME giả và quá giới hạn.
- Reminder/appointment ở ranh ngày, recurrence và timezone khác nhau.
- Triage có normal input, red flag, timeout và malformed AI response.
- Safety có event countdown/cancel/alert/false positive.

Ghi toàn bộ UUID/ID test vào một file evidence riêng. Không thay ID trong Sheet bằng dữ liệu thật.

## 4. Evidence bắt buộc cho mỗi case

Tạo thư mục:

```text
evidence/
  <build-or-commit>/
    <test-round>/
      <case-id>/
        01-before.png
        02-action.png
        03-result.png
        network.har
        api.txt
        logs-redacted.txt
        notes.md
```

Trong `notes.md` ghi:

```text
Case ID:
Build/commit/image tag:
Environment and URL:
Tester/device/browser:
Start/end time and timezone:
Account role (không ghi credential):
Test data IDs:
Actual result:
Expected result:
Status:
Bug ID:
Evidence links:
Cleanup performed:
```

HAR và log phải được làm sạch Authorization, cookie, OTP, email/phone thật và payload y tế nhạy cảm.

## 5. Trình tự chạy một test case

1. Đọc đủ Description, Procedure, Expected Results, Pre-conditions và Note.
2. Kiểm tra build/commit đúng; ghi URL, app version, OS/browser và timezone.
3. Tạo dữ liệu riêng hoặc xác nhận dữ liệu seed chưa bị tester khác dùng.
4. Chụp trạng thái trước test, ghi các record ID và số lượng record/event hiện có.
5. Bật DevTools Network hoặc proxy mobile; với API ghi request method/path/status và body đã làm sạch.
6. Thực hiện từng bước đúng thứ tự. Không tự sửa dữ liệu giữa chừng để ép case qua.
7. So từng ý trong Expected Results ở UI, API và sau refresh/re-login.
8. Với case retry/idempotency, luôn đếm record, outbox, notification hoặc audit trước/sau.
9. Với case quyền, thử cả UI và API trực tiếp; route guard frontend không đủ để Passed.
10. Thu evidence, cập nhật đúng round trong Sheet và cleanup dữ liệu.

## 6. Thứ tự thực thi đề xuất

### Đợt A — Deploy smoke và P0 fail-fast

Chạy trước:

- `ST-17-01`: GitLab Pages → Cloudflare → AWS API.
- Các P0 auth/OTP/login/session ở tab Identity.
- `ST-16-01…04`: RBAC, IDOR, consent và token.
- P0 AI red-flag, emergency, private file, safety countdown.
- Các cross-role P0 ở tab 15.

Nếu deploy smoke, login, authorization hoặc database write/readback thất bại, dừng full regression và tạo blocker.

### Đợt B — 121 use case chuẩn

Chạy theo dependency:

1. Identity & Access Control.
2. Mother Care Journey.
3. Baby Care & Vaccination.
4. Family Sync.
5. Health Records, Reminders, Content, Expense, Device, Safety.
6. Community, Expert, AI Nurse, Emergency Map.

Mỗi case `[UC-xx]` phải kiểm tra thêm reload/readback và ownership dù mô tả use case chỉ nêu happy path.

### Đợt C — Negative và code-derived

Trong mỗi module, ba case cuối trước nhóm `[CODE]` bao phủ:

- sai role và cross-owner IDOR;
- dữ liệu thiếu/sai/biên;
- double-submit, retry và dependency failure.

Sau đó chạy hai case `[CODE]` cụ thể của module. Đây là các case bổ sung từ source code, không phải chỉ từ tài liệu.

### Đợt D — Cross-role, privacy và NFR

Chạy tabs 15–17. Fault injection, load, migration và worker race chỉ chạy staging. Production chỉ smoke/measurement không phá hoại.

## 7. Hướng dẫn theo module

| Tab | Case | Trọng tâm cần quan sát |
|---|---:|---|
| Identity & Access Control | 23 | OTP/rate limit, role routing, refresh rotation, 403 không logout, FCM deregister khi đổi account |
| Mother Care Journey | 18 | Stage/date calculation, session refresh, recommendation không cross-account |
| Baby Care & Vaccination | 19 | Ownership baby, timeline/summary consistency, vaccine regenerate idempotent |
| Community & Moderation | 19 | Visibility/moderation, like/bookmark counts, pagination, AI scan worker |
| Expert Network | 17 | Verification files, availability, consultation idempotency/expiry, chat/call |
| AI Nurse & Risk Triage | 10 | Consent version, red flag, no diagnosis, AI timeout, idempotency conflict |
| Emergency Map | 11 | Permission/GPS, non-dispatch disclaimer, FCM retry và handoff |
| Health Records | 11 | Private attachment, provenance, archive idempotent, cross-owner denial |
| Reminders & Care Plan | 10 | Timezone/recurrence, Today list, scheduler/outbox exactly-once |
| Family Sync | 13 | Invite reuse, permission scope/revoke, concurrent dashboard refresh |
| Content & Checklist | 12 | Version approval, AI scan version, unpublish/cache, image orphan cleanup |
| Expense Planner | 8 | Precision/rounding, total consistency, concurrent update, family visibility |
| Connected Device Data | 9 | Consent/disconnect, provenance, batch overlap/deduplicate, background sync |
| Safety Monitoring | 11 | Sensor readiness, raw IMU boundary, countdown cancel race, alert exactly-once |
| Cross-role Workflows | 12 | State/permission/notification hội tụ giữa nhiều actor |
| Privacy & Consent | 12 | Backend enforcement, IDOR, token, file, log, retention, mass assignment |
| Resilience & Non-functional | 20 | Deployment, response, load, CORS/cache, outages, DB, observability, CI |

## 8. Test độ phản hồi của web GitLab Pages và backend AWS

Có, đây là phần bắt buộc. Frontend và API ở hai origin/tầng triển khai khác nhau nên cần đo riêng frontend, network/preflight và backend.

### 8.1 Frontend

Với Chrome Incognito, chạy ba lần cold cache và ba lần warm cache:

1. DevTools → Network → Disable cache cho cold run.
2. Ghi DNS, connection, SSL, TTFB, content download, DOMContentLoaded và Load.
3. DevTools Performance hoặc Lighthouse: ghi FCP, LCP, CLS và Total Blocking Time.
4. Lặp ở trang login, dashboard theo role và một trang có danh sách lớn.
5. Kiểm tra JS/CSS hash và `cache-control`; reload deep route trực tiếp.

### 8.2 API warm/cold

Không ghi token vào history/screenshot. Có thể dùng PowerShell với biến session tạm:

```powershell
$apiBase = 'https://api.carebridgevn.site'
1..10 | ForEach-Object {
  $sw = [System.Diagnostics.Stopwatch]::StartNew()
  try {
    $response = Invoke-WebRequest -Uri "$apiBase/actuator/health" -Method Get -UseBasicParsing
    $status = $response.StatusCode
  } catch {
    $status = $_.Exception.Response.StatusCode.value__
  }
  $sw.Stop()
  [pscustomobject]@{ Run = $_; Status = $status; Milliseconds = $sw.ElapsedMilliseconds }
}
```

Với endpoint cần auth, lấy token bằng cách được dự án phê duyệt và chỉ giữ trong biến của terminal hiện tại. Đo ít nhất health, login, một list phân trang, detail và một create/readback synthetic. Tính p50/p95/max/error rate.

SRS hiện chưa chốt latency/concurrency/capacity. Vì vậy:

- vẫn phải thu baseline;
- không tự đặt số rồi đánh Passed;
- giữ case hiệu năng `Pending` với note `Acceptance threshold UNKNOWN` cho tới khi PO/Architect phê duyệt SLO;
- lỗi timeout, 5xx hoặc dữ liệu sai vẫn là Failed dù chưa có SLO.

### 8.3 Load test

Chỉ chạy staging:

- ramp 1 → 5 → 20 virtual users;
- read mix trước, write mix sau với dữ liệu có cleanup;
- ghi throughput, p50/p95/p99, error rate, EC2 CPU/RAM, DB connection pool và thời gian hồi phục;
- không load test production nếu chưa có phê duyệt vận hành.

## 9. Cách test các case đặc biệt từ code

### 9.1 Idempotency

Gửi cùng idempotency key và cùng payload hai lần: expected cùng outcome/record. Gửi cùng key nhưng payload khác: expected conflict, không ghi đè. Áp dụng nổi bật cho triage, consultation, direct message, checklist/today action, consent và vaccine generation.

### 9.2 Scheduled worker/outbox

Ghi record count trước test, trigger job hai vòng, rồi đếm lại. Với provider lỗi, domain transaction vẫn phải hợp lệ; outbox retry sau phục hồi nhưng không gửi hai notification logic. Áp dụng reminder/appointment, direct chat, consultation, vaccine, emergency, safety và AI moderation.

### 9.3 Multipart/private file

Không chỉ kiểm tra extension. Thử MIME giả, file signature sai, rỗng, quá lớn, filename chứa path và active content. Sau lỗi kiểm tra cả DB metadata lẫn blob orphan. Sau revoke/archive, URL hoặc file ID cũ không được bypass quyền.

### 9.4 Race condition

Dùng hai client hoặc API tool gửi request gần đồng thời. Các race bắt buộc: refresh token, consultation accept-vs-expire, safety cancel-vs-countdown, concurrent expense/task update và logout A-vs-login B trên mobile.

## 10. Tiêu chí release

- P0 pass rate = 100%.
- P1 pass rate ≥95%; mọi failure còn lại phải được triage và có waiver rõ.
- Không có risk score 9 đang mở.
- Không release khi có IDOR/RBAC bypass, lộ privacy/consent, red-flag không escalation, duplicate critical alert/notification, mất dữ liệu hoặc deploy smoke fail.
- 121/121 UC phải có trạng thái hợp lệ và evidence hoặc lý do N/A được duyệt.
- NFR phải có evidence source. Kết luận NFR số học chờ threshold chính thức.
- Mọi bug fixed phải retest ở round tiếp theo và chạy regression phạm vi ảnh hưởng.

## 11. Mẫu bug report

```markdown
# [SEVERITY] <tiêu đề ngắn>

- Case ID:
- Build/commit/image:
- Environment/URL:
- Role/account label:
- Device/browser/OS/timezone:
- Preconditions/test data IDs:
- Steps to reproduce:
  1.
  2.
  3.
- Actual result:
- Expected result:
- Reproducibility: x/y
- HTTP method/path/status:
- Correlation/request ID:
- Security/privacy impact:
- Data cleanup status:
- Evidence:
```

Severity gợi ý:

- `S1/Blocker`: lộ dữ liệu, auth bypass, dữ liệu hỏng/mất, safety escalation sai, production không dùng được.
- `S2/Critical`: core flow hỏng, không workaround, duplicate critical event.
- `S3/Major`: chức năng phụ hỏng hoặc có workaround khó.
- `S4/Minor`: cosmetic/copy/không ảnh hưởng outcome.

## 12. Kết thúc vòng test

1. Xác nhận subtotal trong tab `Test Statistics` khớp 235.
2. Lọc `Failed`, `Pending`, `N/A`; mọi dòng phải có lý do/evidence.
3. Đối chiếu bug status và case retest round kế.
4. Xóa dữ liệu synthetic, file/blob và notification test theo danh sách ID; không xóa dữ liệu chung.
5. Lưu report performance, HAR/log đã redact và build metadata.
6. Chốt release gate cùng QA, Dev, PO và DevOps; không chuyển `Pending` thành `Passed` chỉ để tăng coverage.
