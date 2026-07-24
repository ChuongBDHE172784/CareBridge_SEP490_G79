# OV-01 — Hướng dẫn manual test luồng Mother Lifecycle Orchestration

| Trường | Giá trị |
| --- | --- |
| Workflow | OV-01 — Mother Lifecycle Orchestration Overview |
| Epic | Epic 6 — Close OV-01 Mother Lifecycle Orchestration Gaps |
| Phạm vi | Flutter Mobile + Backend API + kiểm tra dữ liệu chỉ đọc |
| Phiên bản | 1.0 |
| Ngày tạo | 2026-07-18 |
| Trạng thái | Draft — sẵn sàng dùng làm acceptance suite |
| Tình trạng triển khai | Stories 6.1–6.8 `DONE`; Stories 6.9–6.10 `BACKLOG` |

## 1. Mục tiêu

Tài liệu này dùng để kiểm thử luồng nghiệp vụ xuyên suốt:

`Đăng nhập → vai trò Mẹ → baseline/consent → chọn giai đoạn → tiền thai kỳ → thai kỳ → kết quả thai kỳ → hậu sản → liên kết em bé tùy chọn → safety triage → tiếp tục hoặc đóng hành trình`.

Mục tiêu chính:

- chứng minh chỉ có một lifecycle hiện hành chuẩn cho mỗi Mẹ;
- bảo toàn nguồn gốc, lịch sử và quy tắc chuyển giai đoạn;
- hỗ trợ hậu sản độc lập với hồ sơ em bé;
- bảo vệ quyền sở hữu khi liên kết 0..n em bé;
- bảo đảm GREEN/YELLOW/RED và fallback an toàn hoạt động nhất quán;
- không hiển thị nội dung chưa được duyệt hoặc chia sẻ quá mức dữ liệu sức khỏe;
- bảo toàn trạng thái khi retry, mất mạng, khởi động lại và đổi tài khoản.

## 2. Cách hiểu trạng thái hiện tại

`_bmad-output/implementation-artifacts/sprint-status.yaml` là nguồn trạng thái triển khai hiện tại. `_bmad-output/implementation-artifacts/ov01-gap-tracking.yaml` giữ scope/traceability gốc của proposal và báo cáo investigation cũ chỉ là bằng chứng lịch sử.

- `READY`: có thể chạy ngay trên build hiện tại nhưng chưa được ghi nhận như một execution PASS riêng.
- `PASS`: đã thực thi và có evidence được tham chiếu trong bảng hoặc phần chi tiết.
- `BLOCKED — STORY NOT IMPLEMENTED`: test là tiêu chí chấp nhận của Story backlog; chưa có màn hình/API không được ghi là defect.
- `DEFERRED`: chủ động chuyển sang Story/gate sau theo ranh giới scope đã phê duyệt.
- `FAIL`: Story đã được đánh dấu sẵn sàng nhưng hành vi thực tế khác kết quả mong đợi.
- `NOT RUN`: chưa thực thi dù môi trường và Story đã sẵn sàng.

Hiện tại có **17/34 ca READY**, **13/34 ca PASS**, **3/34 ca BLOCKED** và **1/34 ca DEFERRED**. Full OV-01 quality gate vẫn `BLOCKED` bởi Story 6.10; riêng gate Stories 6.6–6.9 đã có manual evidence PASS, không có ca FAIL.

## 3. Phạm vi và ngoài phạm vi

### Trong phạm vi

- FR43–FR54 và Stories 6.1–6.10.
- Luồng Mobile của tài khoản Mẹ và các contract backend hỗ trợ.
- Quyền sở hữu, consent, provenance, idempotency, retry, accessibility và privacy.
- Đối chiếu API/database chỉ đọc cho invariant không thể chứng minh chỉ bằng UI.

### Ngoài phạm vi

- Chẩn đoán y khoa hoặc xác nhận tính đúng đắn lâm sàng của mô hình AI.
- Dữ liệu sức khỏe thật, OTP/token/mật khẩu thật.
- Kết luận hiệu năng PASS khi chưa có SLO/load threshold được phê duyệt.
- Web portal như bề mặt chính của hành trình Mẹ.

## 4. Điều kiện tiên quyết

1. Backend, PostgreSQL và mobile build từ cùng commit.
2. Thiết bị Android đã được nhận diện:

   ```powershell
   adb devices
   ```

3. Với thiết bị thật qua USB:

   ```powershell
   adb reverse tcp:8080 tcp:8080
   cd 05_Development/CareBridgeMobileApp
   flutter pub get
   flutter run --dart-define=API_BASE_URL=http://127.0.0.1:8080
   ```

4. Backend local:

   ```powershell
   cd 05_Development/CareBridgeAPI
   .\mvnw.cmd spring-boot:run
   ```

5. Có Postman hoặc công cụ API tương đương để chạy các ca 004–008 và xác minh các invariant backend.
6. Chỉ dùng database disposable/local khi cần reset. Không sửa/xóa trực tiếp dữ liệu shared hoặc staging để làm xanh kết quả.
7. Tạo thư mục evidence cho đợt chạy:

   ```text
   06_Testing/TestResults/epic-6/ov-01/manual-YYYY-MM-DD/
   ```

## 5. Bộ dữ liệu test tối thiểu

| Bí danh | Dữ liệu yêu cầu |
| --- | --- |
| `MOTHER_NEW` | Mẹ mới, baseline chưa hoàn tất, chưa có lifecycle |
| `MOTHER_PRE` | Có một `PRE_PREGNANCY ACTIVE` và history hợp lệ |
| `MOTHER_PREG` | Có một `PREGNANCY ACTIVE`, LMP/EDD và provenance hợp lệ |
| `MOTHER_POST_ZERO` | Có `POSTPARTUM ACTIVE`, không có baby profile |
| `MOTHER_LOSS` | Pregnancy-ended/loss recovery, không có baby profile |
| `MOTHER_OTHER` | Tài khoản Mẹ khác để kiểm tra ownership/cache |
| `NON_MOTHER` | Tài khoản đã xác thực nhưng không có vai trò Mẹ |
| `BABY_A`, `BABY_B` | Hai baby profile đủ điều kiện thuộc cùng `MOTHER_POST_ZERO` hoặc fixture tương đương |
| `FOREIGN_BABY` | Baby profile thuộc `MOTHER_OTHER` |
| `EXPERT_VERIFIED` | Chuyên gia đã xác minh và có lịch khả dụng |
| `EXPERT_UNVERIFIED` | Chuyên gia chưa xác minh |
| `CONTENT_*` | Template ở các trạng thái `APPROVED`, `DRAFT`, `REJECTED`, `ARCHIVED` |
| `TRIAGE_*` | Input tổng hợp tạo GREEN, YELLOW, RED và tình huống AI unavailable |

Biến API tối thiểu: `baseUrl`, `motherAToken`, `motherBToken`, `motherAUserId`, `journeyAId`, `journeyAVersion`. Không export environment chứa token hoặc mật khẩu.

## 6. Quy tắc evidence

Sau mỗi ca, lưu tối thiểu:

- ảnh trước và sau hành động chính;
- ID tổng hợp của lifecycle/triage/emergency/baby nếu có;
- HTTP status và response đã che token/PII;
- truy vấn chỉ đọc hoặc audit/log đã lọc dữ liệu nhạy cảm;
- kết quả thực tế, commit/build, thiết bị và thời gian chạy.

Không đưa access token, refresh token, mật khẩu, OTP, email/số điện thoại thật hoặc nội dung sức khỏe thật vào ảnh/log/tài liệu.

## 7. Thứ tự chạy

1. Chạy `READY` theo thứ tự 004 → 005 → 006 → 007 → 008.
2. Story 6.2 đã chạy 001–003 và 010; 009 được defer theo waiver sang Story 6.9.
3. Khi Stories 6.3–6.5 sẵn sàng, chạy 011–022.
4. Khi Stories 6.6–6.9 sẵn sàng, chạy 023–029.
5. Khi Story 6.10 sẵn sàng, chạy 030–034 và full regression 001–034.
6. Ưu tiên P0 trước P1 trong từng nhóm.

## 8. Bảng tổng hợp ca kiểm thử

| ID | Kịch bản | Story | Ưu tiên | Hiện tại | Kết quả lần chạy |
| --- | --- | --- | --- | --- | --- |
| OV01-MAN-001 | Đăng nhập và chọn vai trò Mẹ | 6.2 | P1 | READY | PASS — physical device |
| OV01-MAN-002 | Baseline thiếu trường bắt buộc | 6.2 | P1 | READY | PASS — blank validation and valid PostgreSQL submit |
| OV01-MAN-003 | Consent thiếu/hết hạn/thu hồi | 6.2 | P0 | READY | PASS — missing, expired, and revoked physical-device evidence |
| OV01-MAN-004 | Khởi tạo một canonical Mother journey | 6.1 | P0 | READY | `[điền]` |
| OV01-MAN-005 | Retry/concurrent create không tạo trùng | 6.1 | P0 | READY | `[điền]` |
| OV01-MAN-006 | Transition hợp lệ và append-only history | 6.1 | P1 | READY | `[điền]` |
| OV01-MAN-007 | No-op/stale/illegal transition bị từ chối | 6.1 | P1 | READY | `[điền]` |
| OV01-MAN-008 | Tài khoản khác không đọc/sửa journey | 6.1 | P0 | READY | `[điền]` |
| OV01-MAN-009 | Preconception dashboard và vòng lặp “chưa” | 6.2/6.9 | P1 | PASS | Final round-3 JAR `B18DF006...C5A4F` / installed APK `7FC34F65...34A4D9`; not-yet, force-stop/cold-start, PRE UI and byte-identical DB invariant passed. Evidence: `final-round2-b18df006-7fc34f65-r3/`. |
| OV01-MAN-010 | PRE xác nhận mang thai trên cùng journey | 6.1/6.2 | P1 | READY | PASS — physical device + PostgreSQL evidence |
| OV01-MAN-011 | Dating thai kỳ từ LMP | 6.2/6.3 | P1 | READY | PASS — physical device + PostgreSQL evidence |
| OV01-MAN-012 | EDD/unknown/revision bảo toàn provenance | 6.3 | P1 | READY | PASS — clinician EDD revision + automated unknown-date coverage |
| OV01-MAN-013 | Thai kỳ đang tiếp diễn quay lại dashboard | 6.3 | P1 | READY | PASS — physical device + PostgreSQL evidence |
| OV01-MAN-014 | Live birth chuyển sang postpartum | 6.3 | P0 | READY | PASS — physical device + PostgreSQL evidence |
| OV01-MAN-015 | Pregnancy loss vào recovery, không tạo baby | 6.3 | P0 | READY | PASS — physical device + PostgreSQL evidence |
| OV01-MAN-016 | Vào postpartum trực tiếp với zero baby | 6.4 | P1 | READY | PASS — physical Android device + PostgreSQL evidence |
| OV01-MAN-017 | Recovery độc lập dữ liệu baby | 6.4 | P1 | READY | PASS — physical Android device + PostgreSQL evidence |
| OV01-MAN-018 | Hoãn tạo baby | 6.5 | P1 | PASS | `story-6-5-manual/manual-run-summary.md` |
| OV01-MAN-019 | Tạo và liên kết baby mới | 6.5 | P1 | PASS | `story-6-5-manual/manual-run-summary.md` |
| OV01-MAN-020 | Liên kết baby có sẵn cùng tài khoản | 6.5 | P1 | PASS | `story-6-5-manual/manual-run-summary.md` |
| OV01-MAN-021 | Liên kết nhiều baby | 6.5 | P1 | PASS | `story-6-5-manual/manual-run-summary.md` |
| OV01-MAN-022 | Chặn cross-account/incompatible baby | 6.5 | P0 | PASS | `story-6-5-manual/manual-run-summary.md` |
| OV01-MAN-023 | GREEN từ mọi active stage và trở về origin | 6.6/6.7 | P1 | PASS | Five production-origin Android GREEN round trips passed: PRECONCEPTION, PREGNANCY, POSTPARTUM returned to the exact Mother Journey; INFANT and TODDLER returned to the exact baby profile. Sanitized UI/PostgreSQL evidence: `_bmad-output/test-artifacts/story-6-7-manual/`. |
| OV01-MAN-024 | YELLOW, verified expert và consent tối thiểu | 6.8 | P1 | PASS | Final Android 35 APK: verified-only discovery, explicit four-field consent, refusal/offline/eligibility-loss no-side-effect, stable-key exactly-once retry, minimal expert detail, account-switch/restart isolation, and GREEN/RED regression. Evidence: `_bmad-output/test-artifacts/story-6-8-manual/manual-run-summary.md`. |
| OV01-MAN-025 | RED gọi emergency xác định | 6.6 | P0 | PASS | Android UI thật từ đủ năm production origin; evidence sanitized trong `story-6-6-manual/manual-run-summary.md` |
| OV01-MAN-026 | RED lặp lại dùng một emergency session | 6.6 | P0 | PASS | CTA lặp + background/resume; snapshot DB trước/sau không tăng ACTIVE, association, outbox, attempt hoặc audit |
| OV01-MAN-027 | AI unavailable dùng safe fallback | 6.6 | P0 | PASS | Python unavailable/recovery chạy từ đủ năm origin; fallback RED và retry không tạo side effect trùng |
| OV01-MAN-028 | Safety outcome exactly-once và đúng origin | 6.7 | P0 | PASS | Android POSTPARTUM RED force-stop/relaunch reopened the authoritative emergency, returned to the exact origin, and preserved `1/1/1/1` intake/outcome/emergency/outbox cardinality. Sanitized evidence: `_bmad-output/test-artifacts/story-6-7-manual/`. |
| OV01-MAN-029 | Chỉ dùng APPROVED content/checklist | 6.9 | P0 | PASS | Exact B18D/7FC: API/DB `55/55`, UI `5/5`; Round4 `Semantics/BinaryBinding/ClosedSet/AuthenticatedByteSnapshots=true`, `36/36`, 13 critical files. Tooling `76/76`, parser `7/7`, accepted-log scan `27/27` zero, independent review `15/15 APPROVE`. Evidence: `final-round2-b18df006-7fc34f65-r3/`. |
| OV01-MAN-030 | Continue/complete/archive giữ history | 6.3/6.10 | P1 | DEFERRED | PASS phần continue của 6.3; complete/archive execute với Story 6.10 |
| OV01-MAN-031 | Offline/retry không mất input hoặc ghi trùng | 6.2–6.10 | P1 | READY | PASS cho Story 6.2 onboarding và Story 6.7 safety-projection slice |
| OV01-MAN-032 | Đổi tài khoản xóa cache chéo | 6.10 | P0 | BLOCKED | `[điền]` |
| OV01-MAN-033 | TalkBack/font/risk cue accessibility | 6.10 | P1 | BLOCKED | `[điền]` |
| OV01-MAN-034 | Không lộ token hoặc excessive health payload | 6.10 | P0 | BLOCKED | `[điền]` |

## 9. Chi tiết ca kiểm thử

Nếu ca đang `BLOCKED`, chỉ thực thi sau khi Story tương ứng được chuyển sang trạng thái testable. Nếu UI/API đầu tiên của Story chưa tồn tại, ghi đúng `BLOCKED — STORY NOT IMPLEMENTED` và dừng ca.

### OV01-MAN-001 — Đăng nhập và chọn vai trò Mẹ

**Fixture:** `MOTHER_NEW`.  
**Thực hiện:** Đăng nhập → chọn vai trò Mẹ → tiếp tục onboarding → đóng/mở lại app.  
**Mong đợi:** Vai trò được lưu đúng tài khoản; app đi tới baseline/consent, không bỏ qua sang dashboard; mở lại không yêu cầu chọn lại role; không thấy cache của tài khoản trước.  
**Thực tế/evidence:** PASS trên SM-N986N/API 33. Sau chọn vai trò Mẹ, app mở baseline/consent; force-stop/cold-start vẫn quay lại onboarding, không hỏi role và không vào dashboard. Evidence: `_bmad-output/test-artifacts/story-6-2-manual/ov01-man-001-role.png`, `ov01-man-001-onboarding.png`, `ov01-man-001-cold-start-fixed.png`, `story62-fixed.xml`.

### OV01-MAN-002 — Baseline thiếu trường bắt buộc

**Fixture:** `MOTHER_NEW`, consent hợp lệ.  
**Thực hiện:** Mở baseline → để trống lần lượt từng trường bắt buộc → nhập giá trị biên/không hợp lệ → sửa hợp lệ và submit.  
**Mong đợi:** Không khởi tạo lifecycle khi baseline chưa đủ; lỗi gắn đúng trường và không xóa dữ liệu hợp lệ; submit hợp lệ chỉ tạo một baseline revision có nguồn/thời gian.  
**Thực tế/evidence:** PASS trên PostgreSQL-backed physical-device runtime. Submit trống hiển thị lỗi đúng cho lifecycle goal và support preference, giữ màn hình và không điều hướng. Submit hợp lệ chuyển đúng sang stage selection; truy vấn PostgreSQL xác nhận đúng một `MOTHER_BASELINE_V1` revision và một consent `PERSONALIZE`/`MOTHER_LIFECYCLE_V1` còn hiệu lực. Evidence: `_bmad-output/test-artifacts/story-6-2-manual/ov01-man-002-blank-validation.png`, `ov01-man-002.xml`, `ov01-man-002-valid-submit-pg.png`, `story62-pg-submit.xml`.

### OV01-MAN-003 — Consent thiếu, hết hạn hoặc bị thu hồi

**Fixture:** bốn biến thể `missing`, `denied`, `expired`, `revoked`.  
**Thực hiện:** Với từng biến thể, thử hoàn tất onboarding/khởi tạo lifecycle; sau đó cấp consent hợp lệ và retry; cuối cùng thu hồi consent rồi thử hành động yêu cầu consent.  
**Mong đợi:** Tất cả trạng thái không hợp lệ fail closed; không tạo side effect; nội dung giải thích trung lập; consent hợp lệ cho phép tiếp tục; thu hồi có hiệu lực và có audit nhưng không xóa lịch sử nghiệp vụ.  
**Thực tế/evidence:** PASS trên physical device cho `missing`, persisted `expired`, và persisted `revoked`. Cả expired/revoked đều bị authoritative routing đưa về onboarding, consent không được preselect và không thể bypass sang stage selection. PostgreSQL xác nhận mỗi tài khoản synthetic vẫn giữ đúng một baseline revision, có 0 journey và tổng 0 transition. Trong contract đã duyệt, `denied` là thao tác từ chối/không grant và được xử lý như `missing`, không phải persisted status riêng. Evidence: `_bmad-output/test-artifacts/story-6-2-manual/ov01-man-003-consent-error.png`, `ov01-man-003-scrolled.xml`, `ov01-man-003-expired-final.png`, `story62-expired-final.xml`, `ov01-man-003-revoked-onboarding.png`, `story62-revoked-onboarding.xml`; automated: `JourneyOnboardingIntegrationTest.deniedConsentRollsBackWithoutBaselineConsentOrAuditSideEffects`, `JourneyOnboardingIntegrationTest.persistedInvalidConsentIsExcludedByPostgresQuery`.

### OV01-MAN-004 — Khởi tạo một canonical Mother journey

**Fixture:** Mẹ đã xác thực, chưa có lifecycle hiện hành.  
**Thực hiện:** Lấy token bằng `POST /api/v1/auth/login-direct` trong local/test; gửi:

```http
POST {{baseUrl}}/api/v1/journeys
Authorization: Bearer {{motherAToken}}
Content-Type: application/json
```

```json
{
  "journeyType": "PRE_PREGNANCY",
  "startDate": "2026-07-18",
  "changeReason": "OV01 manual initial lifecycle",
  "effectiveAt": "2026-07-18T02:00:00Z",
  "notes": "Synthetic test record"
}
```

Sau đó gọi `GET /api/v1/journeys/{{journeyAId}}/history`.  
**Mong đợi:** create HTTP 201; `ACTIVE`, version 0, UUID hợp lệ; history có đúng một `CREATED`, `toStage=PRE_PREGNANCY`, reason/effective time đúng và không lộ notes/token/actor nội bộ.  
**Thực tế/evidence:** PASS trên SM-N986N với LMP synthetic `01/07/2026`, chu kỳ 28 ngày. Review hiển thị tuổi thai `2 tuần 4 ngày`, EDD `07/04/2027`; dashboard reload giữ tuần thai/EDD. PostgreSQL lưu `SELF_REPORTED` + `ESTIMATED`. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-011-review.png`, `ov01-man-011-dashboard.png`, `story63-lmp-review.xml`, `story63-preg-dashboard.xml`.

### OV01-MAN-005 — Retry/concurrent create không tạo trùng

**Fixture:** tài khoản mới chưa có lifecycle; hai request create đồng thời hoặc retry cùng ý định.  
**Thực hiện:** Gửi gần đồng thời hai `POST /api/v1/journeys` với PRE và POSTPARTUM; sau đó gọi dashboard/history và chạy truy vấn canonical ở Mục 10.  
**Mong đợi:** chỉ một request thành công; request còn lại nhận conflict an toàn (`JOURNEY-015` hoặc contract đã duyệt); đúng một canonical `ACTIVE`, một `CREATED`; không có partial/duplicate row.  
**Thực tế/evidence:** PASS cho clinician EDD revision trên physical device; EDD đổi `08/03/2027 → 15/03/2027`, version `0→1`, transition `DATES_CHANGED` giữ previous/new values, source `CLINICIAN_CONFIRMED`, confidence `CONFIRMED`, reason `DATE_CORRECTION`, và `last_menstrual_date` vẫn null nên không có stale field. Nhánh unknown-date được giữ bởi automated contract coverage. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-012-review.png`, `story63-edd-review.xml`, `story63-edd-result.xml`, `ov01-man-012-db.txt`.

### OV01-MAN-006 — Transition hợp lệ và append-only history

**Fixture:** kết quả OV01-MAN-004.  
**Thực hiện:** Gửi `PUT /api/v1/journeys/{{journeyAId}}` chuyển PRE sang PREG với LMP, `dateSource`, `dateConfidence`, reason và effective time; tải lại history.  
**Mong đợi:** HTTP 200; cùng `journeyAId`; version tăng đúng một; EDD/date context nhất quán; history mới có `STAGE_CHANGED` từ PRE sang PREG; bản ghi `CREATED` không đổi; provenance đầy đủ.  
**Thực tế/evidence:** PASS trên physical device. Xác nhận ongoing giữ canonical stage `PREGNANCY`, version tăng đúng một lần, tạo `OUTCOME_RECORDED PREGNANCY→PREGNANCY`; refresh/cold navigation giữ dating và không tạo baby. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-013-dashboard.png`, `story63-after-ongoing.xml`.

### OV01-MAN-007 — No-op, stale và illegal transition bị từ chối

**Fixture:** một PREG journey version hiện tại đã biết.  
**Thực hiện:** Lần lượt gửi update không thay đổi dữ liệu, update với version cũ theo contract, và chuyển ngược PREG → PRE. Sau mỗi request, tải lại journey/history.  
**Mong đợi:** mỗi request bị từ chối bằng business/conflict response đã duyệt; stage/version/history không đổi; không có audit giả hoặc partial write; app hiển thị lỗi có thể retry thay vì crash.  
**Thực tế/evidence:** PASS sau khi manual gate phát hiện và sửa lỗi UI thiếu `correction=true` khi đổi outcome đã tồn tại. APK fixed ghi `OUTCOME_CORRECTED PREGNANCY→POSTPARTUM`, version `1→2`, outcome/date/delivery date `LIVE_BIRTH/19-07-2026`; `baby_count=0` và UI nêu hồ sơ baby là tùy chọn. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-014-pass.png`, `story63-live-pass.xml`, `ov01-man-014-db.txt`.

### OV01-MAN-008 — Tài khoản khác không đọc hoặc sửa journey

**Fixture:** `journeyAId` của Mother A và token Mother B.  
**Thực hiện:** Dùng token B gọi history, dashboard-by-ID nếu có và update journey A; sau đó dùng token A kiểm tra lại dữ liệu.  
**Mong đợi:** B nhận 403 hoặc 404 theo contract; response không lộ stage, dates, notes hoặc history của A; không có thay đổi; A vẫn đọc được dữ liệu nguyên vẹn; audit bảo mật có correlation ID nếu được hỗ trợ.  
**Thực tế/evidence:** PASS trên physical device với fixture synthetic độc lập. UI dùng copy trung lập và xác nhận chuyển sang hỗ trợ hồi phục; PostgreSQL ghi `PREGNANCY_LOSS`, chuyển `PREGNANCY→POSTPARTUM`, để `pregnancy_outcome_date` và `delivery_date` null, `baby_count=0`. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-015-pass2.png`, `story63-loss-pass2.xml`, `ov01-man-015-db.txt`.

### OV01-MAN-009 — Preconception dashboard và vòng lặp “chưa”

**Fixture:** `MOTHER_PRE`, reviewed content/checklist.  
**Thực hiện:** Mở dashboard PRE → xem mục tiêu/checklist/nội dung → chọn “chưa/không” khi được hỏi tình trạng mang thai → đóng/mở lại app.  
**Mong đợi:** vẫn ở PRE trên cùng lifecycle; không tạo PREG/baby; chỉ nội dung phù hợp PRE và đã duyệt được hiển thị; lựa chọn và dashboard được khôi phục đúng.  
**Thực tế/evidence:** `PASS` ngày 2026-07-24 trên exact final JAR SHA-256 `B18DF0066E81EA896EA000E37F4661CD6DA9566DEE3B0749D8D4CBA5C06C5A4F` (`124081154` bytes) và installed APK SHA-256 `7FC34F65EAD4566D83D5BEDD4C6D186249A238727F08C4513E9896B56B34A4D9` (`226838368` bytes). Android hiển thị PRE journey, reviewed-content entry và snackbar “not yet”; force-stop làm app PID rỗng, cold-start khôi phục PRE home/journey. DB trước/sau byte-identical: `69000000-0000-0000-0000-000000000001|PRE_PREGNANCY|ACTIVE|0|1|0|0|1`. Evidence: `_bmad-output/test-artifacts/story-6-9-manual/final-round2-b18df006-7fc34f65-r3/`.

### OV01-MAN-010 — PRE xác nhận mang thai trên cùng journey

**Fixture:** `MOTHER_PRE`, dữ liệu dating hợp lệ.  
**Thực hiện:** Từ PRE dashboard chọn xác nhận mang thai → nhập dating/provenance → xác nhận → mở Hành trình và history.  
**Mong đợi:** lifecycle ID không đổi; stage thành PREGNANCY; đúng một active canonical; có transition PRE→PREG với actor/reason/source/effective time; không tạo journey thứ hai.  
**Thực tế/evidence:** PASS trên PostgreSQL-backed physical-device runtime. Từ fixture PRE, chọn EDD clinician-confirmed `2027-03-19` và hoàn tất wizard; app mở pregnancy journey tuần 5. DB xác nhận vẫn đúng một journey ACTIVE, version tăng `0→1`, có đúng một transition `PRE_PREGNANCY→PREGNANCY`, source `CLINICIAN_CONFIRMED`, confidence `CONFIRMED`, đồng thời giữ transition CREATED ban đầu. Evidence: `_bmad-output/test-artifacts/story-6-2-manual/story62-pre-next.xml`, `story62-preg-review.xml`, `story62-pre-preg-result.xml`, `ov01-man-010-pre-preg.png`.

### OV01-MAN-011 — Dating thai kỳ từ LMP

**Fixture:** Mẹ đi vào PREG trực tiếp hoặc từ PRE; LMP synthetic hợp lệ.  
**Thực hiện:** Chọn phương pháp LMP → nhập ngày và độ dài chu kỳ theo UI → xác nhận → mở pregnancy dashboard.  
**Mong đợi:** week/trimester/EDD được tính nhất quán với quy tắc đã duyệt; UI nêu đây là ước tính, không chẩn đoán; source/confidence được lưu; reload không thay đổi dữ liệu.  
**Thực tế/evidence:** PASS phần áp dụng cho Story 6.3: ongoing/continue quay lại active pregnancy dashboard; live birth/loss hoàn tất outcome và giữ append-only history khi vào recovery. Biến thể lifecycle `complete/archive` là ownership của Story 6.10 và được DEFERRED tới runtime của Story 6.10, không dùng waiver cho phần 6.3. Evidence dùng chung: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-013-dashboard.png`, `ov01-man-014-db.txt`, `ov01-man-015-db.txt`.

### OV01-MAN-012 — EDD, unknown dates và revision bảo toàn provenance

**Fixture:** `MOTHER_PREG`.  
**Thực hiện:** Cập nhật bằng clinician EDD → kiểm tra dashboard/history; chạy biến thể không biết ngày; sau đó sửa ngày với reason mới.  
**Mong đợi:** best available source được dùng; unknown không tạo ngày giả; mỗi revision tăng version và giữ giá trị/source/confidence/reason/effective time trước đó trong history; không còn trường dating stale của phương pháp cũ.  
**Thực tế/evidence:** PASS trên physical device + PostgreSQL. Clinician EDD được sửa từ `2027-03-08` sang `2027-03-15`, journey version tăng và history giữ `DATES_CHANGED` với source `CLINICIAN_CONFIRMED`, confidence `CONFIRMED`, reason `DATE_CORRECTION`; biến thể unknown-date được bao phủ bởi regression tự động. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-012-review.png`, `ov01-man-012-db.txt`.

### OV01-MAN-013 — Thai kỳ đang tiếp diễn

**Fixture:** `MOTHER_PREG`.  
**Thực hiện:** Mở outcome/status → chọn “đang tiếp diễn” → quay lại dashboard → refresh và mở lại app.  
**Mong đợi:** stage vẫn PREGNANCY; không tạo postpartum/baby; dashboard giữ dating hiện hành; không tạo transition không cần thiết hoặc duplicate event.  
**Thực tế/evidence:** PASS trên physical device + PostgreSQL. Chọn thai kỳ đang tiếp diễn quay lại đúng pregnancy dashboard, giữ dating hiện hành và không tạo baby/transition thừa. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-013.png`, `ov01-man-013-dashboard.png`, `story63-after-ongoing.xml`.

### OV01-MAN-014 — Live birth chuyển sang postpartum

**Fixture:** `MOTHER_PREG`, dữ liệu sinh synthetic.  
**Thực hiện:** Chọn outcome live birth → nhập ngày/nguồn/reason → xác nhận → quan sát recovery và lời mời tạo/link baby.  
**Mong đợi:** cùng canonical lifecycle chuyển sang POSTPARTUM; outcome provenance và history được giữ; baby vẫn là tùy chọn; từ chối/hoãn baby không chặn recovery; không duplicate transition khi bấm lặp.  
**Thực tế/evidence:** PASS trên physical device + PostgreSQL sau khi sửa propagation của outcome correction. Canonical journey chuyển PREGNANCY → POSTPARTUM, version `2`, outcome/date và transition history được giữ, bấm lặp không tạo duplicate, `baby_count=0` vẫn hợp lệ. Evidence: `_bmad-output/test-artifacts/story-6-3-manual/ov01-man-014.png`, `ov01-man-014-pass.png`, `ov01-man-014-fixed.png`, `ov01-man-014-db.txt`.

### OV01-MAN-015 — Pregnancy loss vào recovery, không tạo baby

**Fixture:** `MOTHER_PREG`, dữ liệu loss synthetic và thuật ngữ đã duyệt.  
**Thực hiện:** Chọn pregnancy ended/loss → nhập ngày recovery/source/reason → xác nhận → kiểm tra dashboard và danh sách baby.  
**Mong đợi:** vào postpartum/recovery an toàn; không bắt buộc delivery date hoặc baby; không tự tạo baby; copy trung lập, đồng cảm, không chẩn đoán; history lưu outcome/provenance và không thể ghi đè.  
**Thực tế/evidence:** `[điền]`

### OV01-MAN-016 — Vào postpartum trực tiếp với zero baby

**Fixture:** `MOTHER_NEW` có baseline/consent hợp lệ.  
**Thực hiện:** Chọn giai đoạn postpartum/parenting → nhập birth date hoặc pregnancy-end/recovery-start theo nhánh phù hợp → chọn chưa tạo baby.  
**Mong đợi:** tạo POSTPARTUM canonical và mở recovery dashboard; zero baby là trạng thái hợp lệ; không ép dữ liệu live birth cho loss; không chuyển sang màn hình lỗi/trống vô hạn.  
**Thực tế/evidence:** `PASS` ngày 2026-07-20 trên Samsung SM-N986N. Tài khoản synthetic có baseline/consent hợp lệ đã chọn nhánh “Đang hồi phục sau sinh”, nhập recovery start 2026-07-19 với nguồn self-reported/confirmed và đi thẳng tới recovery dashboard, không qua baby creation. Probe PostgreSQL xác nhận đúng một canonical `POSTPARTUM/ACTIVE`, đúng một `POSTPARTUM_CREATED` transition và `active_babies=0`. Bằng chứng: `_bmad-output/test-artifacts/story-6-4-manual/ov01-man-016-stage.png`, `ov01-man-016-setup.png`, `ov01-man-016-dashboard.png`, `db-final-verification.txt`.

### OV01-MAN-017 — Recovery độc lập dữ liệu baby

**Fixture:** `MOTHER_POST_ZERO`.  
**Thực hiện:** Tạo/cập nhật một postpartum recovery log → refresh → mở lại app → xác nhận vẫn không có baby profile.  
**Mong đợi:** recovery log được lưu đúng Mother lifecycle; không cần baby ID; zero-baby dashboard hoạt động; không tạo baby ngầm; cảnh báo có thể gọi safety flow.  
**Thực tế/evidence:** `PASS` ngày 2026-07-20 trên Samsung SM-N986N. Tạo recovery log qua UI với pain 3, bleeding LIGHT, mood 4, sleep 6.5 và symptom note synthetic; log xuất hiện sau save/refresh và vẫn còn sau force-stop/cold-start. Safety action mở intake trung lập dành cho postpartum, truyền typed `POSTPARTUM` và không hiển thị infant fields. Probe PostgreSQL xác nhận `active_postpartum_logs=1`, `matching_manual_logs=1`, `active_babies=0`. Bằng chứng: `_bmad-output/test-artifacts/story-6-4-manual/ov01-man-017-form.png`, `ov01-man-017-saved.png`, `ov01-man-017-safety.png`, `ov01-man-017-postpartum-intake.png`, `ov01-man-017-cold-start.png`, `db-final-verification.txt`.

### OV01-MAN-018 — Hoãn tạo baby

**Fixture:** `MOTHER_POST_ZERO`.  
**Thực hiện:** Từ lời mời create/link baby chọn “để sau/không bây giờ” → tiếp tục recovery → mở lại app.  
**Mong đợi:** quay đúng postpartum dashboard; Mother state và recovery data không đổi; lời mời có thể mở lại sau; không có baby/profile/link rỗng được tạo.  
**Thực tế/evidence:** `PASS` ngày 2026-07-21 trên Samsung SM-A725F. Thiết bị xác nhận tài khoản synthetic đủ điều kiện có ba hành động ngang hàng, chọn “Để sau” quay về recovery, và zero-baby ready state vẫn đúng sau force-stop/cold-start; không render baby/link rỗng. Việc mở lại lời mời và giữ zero-baby state qua simulated widget-tree rebuild được xác nhận bởi hai focused widget tests tương ứng. Bằng chứng sanitized: `_bmad-output/test-artifacts/story-6-5-manual/m5-linkage-actions.xml`, `m5-later-return.xml`, `m5-cold-start-ready.xml` và `manual-run-summary.md`.

### OV01-MAN-019 — Tạo và liên kết baby mới

**Fixture:** `MOTHER_POST_ZERO`, dữ liệu baby synthetic tối thiểu.  
**Thực hiện:** Chọn tạo baby → nhập minimum required data → lưu → quay lại recovery và mở baby selector.  
**Mong đợi:** đúng một baby profile thuộc Mother; liên kết tới journey đủ điều kiện; POSTPARTUM state/version không bị đổi ngoài audit liên kết đã duyệt; retry không tạo baby trùng.  
**Thực tế/evidence:** `PASS` ngày 2026-07-21 trên Samsung SM-A725F. Flow journey-scoped tạo dữ liệu tối thiểu và authoritative reload hiển thị đúng một baby liên kết. Aggregate-only read-only diagnostic xác nhận một active journey với version không đổi, một active/linked baby, một create submission và một accepted audit. Bằng chứng sanitized: `_bmad-output/test-artifacts/story-6-5-manual/m5-linked-final.xml`, `m5-db-verification.md` và `manual-run-summary.md`.

### OV01-MAN-020 — Liên kết baby có sẵn cùng tài khoản

**Fixture:** một baby eligible cùng owner nhưng chưa liên kết journey hiện tại.  
**Thực hiện:** Chọn link existing → chọn baby → xác nhận → reload recovery/baby journey.  
**Mong đợi:** liên kết thành công một lần; không sao chép profile; baby data giữ nguyên; Mother recovery không reset; audit ghi actor/source/link target.  
**Thực tế/evidence:** `PASS` ngày 2026-07-21 trên Samsung SM-A725F. Selector chỉ hiển thị candidate ACTIVE cùng owner và chưa liên kết; chọn Baby A tạo đúng một liên kết rồi authoritative refresh hiển thị Baby A một lần, không gọi switch-active legacy. Bằng chứng sanitized: `_bmad-output/test-artifacts/story-6-5-manual/m6-candidates.xml`, `m6-one-linked.xml`; audit/command contract được xác nhận bởi bộ PostgreSQL và widget test Story 6.5.

### OV01-MAN-021 — Liên kết nhiều baby

**Fixture:** `BABY_A`, `BABY_B` cùng owner và compatible.  
**Thực hiện:** Liên kết lần lượt hai baby → chuyển selector A/B nhiều lần → quay lại recovery.  
**Mong đợi:** cả hai liên kết tồn tại, không trùng; dữ liệu mỗi baby được cô lập; Mother vẫn chỉ có một POSTPARTUM active; late response không làm đổi baby đang chọn.  
**Thực tế/evidence:** `PASS` ngày 2026-07-21 trên Samsung SM-A725F. Baby A và Baby B tồn tại đúng một lần trong linked set; selector cuối có đúng một actionable semantics node cho mỗi baby, chọn Baby B vẫn được giữ sau authoritative refresh, và quay lại đúng postpartum recovery. Bằng chứng sanitized: `_bmad-output/test-artifacts/story-6-5-manual/m6-two-linked.xml`, `m6-selector-final-a.xml`, `m6-selector-final-b-refresh.xml`, `m6-recovery-return.xml`.

### OV01-MAN-022 — Chặn cross-account hoặc incompatible baby

**Fixture:** `FOREIGN_BABY` và một baby không tương thích stage/outcome.  
**Thực hiện:** Dùng deep link/API test support đã duyệt để thử liên kết từng fixture; sau đó kiểm tra bằng owner thật.  
**Mong đợi:** 403/404 hoặc business error trung lập; không lộ tên/ngày/notes của foreign baby; không tạo/sửa link; attempt được audit; owner thật vẫn thấy dữ liệu nguyên vẹn.  
**Thực tế/evidence:** `PASS` ngày 2026-07-21 bằng API test support trên PostgreSQL 16. Case `foreignAndIncompatibleLinkAttemptsLeaveOwnerDataUnchangedAndSanitizedAudits` xác nhận foreign trả `LINK_RESOURCE_NOT_FOUND`, pregnancy-loss/incompatible trả `LINK_NOT_ELIGIBLE`; không tạo submission/link, row owner không đổi và mỗi rejection có đúng một audit không chứa nickname, birth date hoặc token. Tổng hợp sanitized: `_bmad-output/test-artifacts/story-6-5-manual/manual-run-summary.md`.

### OV01-MAN-023 — GREEN từ mọi active stage và trở về origin

**Fixture:** PRE, PREG, POST; baby-linked INFANT/TODDLER; và `TRIAGE_GREEN`.  
**Thực hiện:** Từ từng Mother dashboard và từng baby journey INFANT/TODDLER mở safety triage → nhập GREEN phù hợp context → hoàn tất guidance/monitor → quay lại.  
**Mong đợi:** cả năm safety context được gửi/đọc đúng; kết quả GREEN nhất quán và non-diagnostic; không tạo emergency/consultation; quay đúng Mother dashboard hoặc đúng baby/context ban đầu; dữ liệu giữa stage/baby không bị trộn.  
**Thực tế/evidence:** `PASS` ngày 2026-07-23 trên AVD Android 15/API 35 với APK build từ working tree cuối. Đã chạy lại đủ năm production origin: PRECONCEPTION, PREGNANCY, POSTPARTUM từ Mother Journey và INFANT/TODDLER từ đúng baby profile. Cả năm hoàn tất GREEN, quay về đúng typed origin, hiển thị confirmation tại destination và acknowledge continuation; hai pediatric request có top-level `babyProfileId` đúng. PostgreSQL latest-run matrix xác nhận mỗi stage có đúng một intake/projection, `acknowledged=true`, đúng journey/baby origin/action và zero emergency. Evidence sanitized: `_bmad-output/test-artifacts/story-6-7-manual/manual-run-summary.md`, `db-evidence.md`, các bộ `pre-queryfix-return.*`, `preg-queryfix-return.*`, `post-queryfix-return.*`, `infant-final-return.*`, `toddler-final-return.*`.

### OV01-MAN-024 — YELLOW, verified expert và consent tối thiểu

**Fixture:** `TRIAGE_YELLOW`, expert verified/unverified, consent approve/deny.  
**Thực hiện:** Hoàn tất YELLOW → kiểm tra danh sách expert → từ chối chia sẻ → chạy lại và đồng ý minimum context → đặt lịch/liên hệ.  
**Mong đợi:** chỉ verified expert được đưa ra; từ chối consent không chia sẻ context và vẫn có hướng dẫn an toàn; đồng ý chỉ gửi trường tối thiểu đã hiển thị; có trace/source ID và reviewed citations; raw triage payload không bị sao chép quá mức; không còn doctor/clinic card, CTA hoặc danh tính placeholder.  
**Thực tế/evidence:** `PASS` ngày 2026-07-23 trên Android 35 AVD với APK cuối SHA-256 `2B1942FF374F959422A6B9817AEF36FACCA2988F0D3866CCABF3CC8BEB4B7066`. YELLOW từ production Mother Journey chỉ hiển thị expert `APPROVED`/eligible; expert `PENDING` không xuất hiện. Consent ban đầu chưa chọn, submit bị khóa, mô tả đúng bốn trường chia sẻ và mười loại dữ liệu bị loại trừ. Refusal, offline timeout khoảng 10.23 giây và mất eligibility đều không tạo side effect. Retry cùng stable key tạo đúng một request/consent/context-share/citation cho mỗi synthetic owner. Expert accepted queue mở được detail chỉ gồm YELLOW, PREGNANCY, sanitized risk summary và nguồn WHO đã duyệt. Late response + account switch và force-stop/relaunch không làm lộ context tài khoản trước. GREEN không có expert-handoff CTA và quay lại đúng journey; RED vẫn mở phiên hỗ trợ khẩn cấp 115. PostgreSQL cuối giữ cardinality `2/2/2/2` cho request/expert-consent/context-share/citation với `2` distinct idempotency keys. Evidence sanitized: `_bmad-output/test-artifacts/story-6-8-manual/manual-run-summary.md`, `db-evidence.md`, và các XML `final-*` được liệt kê trong biên bản.

### OV01-MAN-025 — RED gọi emergency xác định

**Fixture:** PRE, PREG, POST; baby-linked INFANT/TODDLER; và `TRIAGE_RED`.  
**Thực hiện:** Từ cả năm safety context gửi RED input → quan sát điều hướng/call-to-action → mở emergency session và quay lại khi an toàn.  
**Mong đợi:** context Mother/baby chính xác; RED không chờ hoặc gọi một AI interpretation thứ hai; emergency được tạo/mở deterministically; copy khẩn cấp rõ ràng, không chẩn đoán; origin lifecycle/baby context được giữ; POSTPARTUM, INFANT và TODDLER hoạt động end-to-end.  
**Thực tế/evidence:** `PASS` ngày 2026-07-22 trên AVD Android 35 với APK build từ working tree cuối. Đã đăng nhập năm fixture account và thao tác từ đúng production origin: PRECONCEPTION/PREGNANCY/POSTPARTUM từ Mother Journey, INFANT/TODDLER từ baby profile nạp qua API thật. Mỗi CTA khóa stage, RED mở backend ACTIVE emergency, và back navigation trả về đúng typed origin trong current session. PostgreSQL xác nhận mọi RED intake tại checkpoint đều có association và mỗi owner chỉ dùng một linked emergency. Evidence sanitized: `_bmad-output/test-artifacts/story-6-6-manual/manual-run-summary.md`, `db-evidence.md`, cùng các bộ `pre-*valid`, `preg-*valid`, `post-*valid`, `infant-*`, `toddler-*` PNG/XML.

### OV01-MAN-026 — RED lặp lại dùng một emergency session

**Fixture:** RED context đã có emergency session.  
**Thực hiện:** Bấm lặp CTA, retry request, background/resume và gửi lại cùng source/idempotency context.  
**Mong đợi:** chỉ một emergency session được create/reuse; response đều trỏ cùng session; không có duplicate notification/audit nghiệp vụ; trạng thái cuối nhất quán.  
**Thực tế/evidence:** `PASS` ngày 2026-07-22. Trên PREGNANCY RED screen đã mở CTA lặp, nhấn Home để background, bring-to-front để resume, rồi mở CTA lặp lại. Trước/sau PostgreSQL đều giữ `1 ACTIVE`, `1 linked emergency`, `1 outbox`, `attempt_count=1`, và `family_alert_log=0`. Giá trị zero là đúng với fixture không có family-contact/FCM recipient và không tăng sau replay/resume. Evidence sanitized: `preg-background.*`, `preg-resume.*`, `preg-repeat-cta.*`, `_bmad-output/test-artifacts/story-6-6-manual/db-evidence.md`.

### OV01-MAN-027 — AI unavailable dùng safe fallback

**Fixture:** PRE, PREG, POST, baby-linked INFANT/TODDLER và failure injection được phê duyệt cho timeout/unavailable/malformed AI response.  
**Thực hiện:** Từ mỗi Mother/baby safety context gọi triage trong lúc AI unavailable → quan sát guidance/escalation → retry sau khi dịch vụ phục hồi.  
**Mong đợi:** app không crash/blank; fallback bảo thủ, rõ ràng, non-diagnostic và không hạ mức nguy cơ; context INFANT/TODDLER không bị quy về Mother; RED-like danger signs vẫn dẫn emergency deterministically; retry không tạo side effect trùng.  
**Thực tế/evidence:** `PASS` ngày 2026-07-22. Đã dừng process Python giữ port 8001 và xác nhận không còn listener, sau đó chạy từ đủ năm production origin. PRECONCEPTION, PREGNANCY, POSTPARTUM, INFANT và TODDLER đều hiển thị explicit safe-fallback copy, RED, emergency CTA và không crash/blank. Python được khởi động lại từ `.venv-story66`; `/health` trả HTTP 200, rồi cả năm origin được retry và trở lại normal RED copy. DB cuối chỉ tăng một intake association mỗi recovery retry; mỗi owner vẫn có đúng một ACTIVE/linked emergency, một outbox, `attempt_count=1` và không phát sinh recipient log. Evidence sanitized: `man027-outage-*-red.*`, `man027-recovery-*-red.*`, `manual-run-summary.md`, `db-evidence.md`.

### OV01-MAN-028 — Safety outcome exactly-once và đúng origin

**Fixture:** một GREEN, YELLOW, RED; duplicate callback và app restart.  
**Thực hiện:** Hoàn tất từng outcome → retry callback/projection → kill/reopen app → mở lifecycle timeline và tiếp tục.  
**Mong đợi:** mỗi outcome có đúng một timeline projection chứa minimum data, source ID, risk, origin action; duplicate callback không tạo bản ghi mới; continuation token không lộ dữ liệu và đưa về đúng dashboard/state.  
**Thực tế/evidence:** `PASS` ngày 2026-07-23. YELLOW PRE process-death restore vẫn có đúng một outcome, zero emergency và acknowledge sau destination render. Với RED POSTPARTUM, Android mở emergency thật, force-stop/relaunch rồi khôi phục đúng authoritative emergency; back trả về đúng Postpartum Journey, timeline hiển thị RED confirmation và continuation được acknowledge. PostgreSQL trước/sau restart/return đều giữ đúng `1 intake`, `1 outcome`, `1 emergency association`, `1 distinct emergency session`, `1 outbox`; không có second POST/association/outbox. Không token value nào được xuất ra evidence. Evidence sanitized: `pre-yellow.png`, `pre-restored.*`, `post-red-emergency-before-restart.*`, `post-red-restarted-authoritative.*`, `post-red-final-return.*`, `manual-run-summary.md`, `db-evidence.md`.

### OV01-MAN-029 — Chỉ dùng APPROVED content/checklist

**Fixture:** cùng stage có template `APPROVED`, `DRAFT`, `REJECTED`, `ARCHIVED`.  
**Thực hiện:** Mở content/checklist từ PRE/PREG/POST; thử direct ID/deep link/API cho từng status; thử import/use template.  
**Mong đợi:** chỉ APPROVED và đúng stage được đọc/dùng; các status còn lại bị từ chối cả khi biết ID; không lộ body/metadata nhạy cảm; content không tự chẩn đoán hoặc thay thế expert/emergency.  
**Thực tế/evidence:** `PASS` ngày 2026-07-24 trên exact final JAR `B18DF006...C5A4F` và installed APK `7FC34F65...34A4D9`. Auth `3/3`, API/DB `45/45 + 10/10 = 55/55`, UI `5/5`; rejected ContentItem `0`, checklist rejected/denial `3/3`. Round4 template SHA-256 `7470FD151E7C2DB91B02497B3F291309855B7B4C44283D2F1EA9BCBE6504E419` trả `Semantics=true`, `BinaryBinding=true`, `ClosedSet=true`, `AuthenticatedByteSnapshots=true`, `36/36`, 13 critical files. Tooling `76/76`, parser `7/7`, accepted-log scan `27/27` zero; independent fixed-scope review `15/15 PASS — APPROVE`. Historical manifests: final4983 `32/32`, final122 `40/40`, round2 `41/41`; current `36/36`. Runtime shutdown PASS.

### OV01-MAN-030 — Continue, complete và archive giữ history

**Fixture:** active PRE/PREG/POST và các outcome.  
**Thực hiện:** Chọn continue rồi mở lại; chạy biến thể complete/archive theo rule; thử loss/non-live-birth; kiểm tra history sau mỗi hành động.  
**Mong đợi:** continue trở về active dashboard; complete/archive là hành động rõ ràng và đúng rule; không ép delivery date/baby cho loss; history còn nguyên và lifecycle đã đóng không bị coi là active.  
**Thực tế/evidence:** `[điền]`

### OV01-MAN-031 — Offline/retry không mất input hoặc ghi trùng

**Fixture:** một form của mỗi phase và một safety projection test-safe.  
**Thực hiện:** Nhập dữ liệu → tắt mạng trước submit → submit → background/foreground → bật mạng → retry; lặp với response đến muộn.  
**Mong đợi:** input hợp lệ được giữ; thông báo offline/retry rõ ràng; không giả thành công; sau reconnect chỉ có một write/transition/link/session; late response không ghi đè state mới hơn.  
**Thực tế/evidence:** PASS cho phase onboarding Story 6.2 và lát cắt safety projection Story 6.7. Với Story 6.7, backend local đã được dừng sau khi nhập nhưng trước submit; Android hiển thị lỗi retry rõ ràng và giữ nguyên input synthetic. Sau khi khởi động lại cùng JAR/config, retry hoàn tất GREEN; PostgreSQL xác nhận failed attempt không tạo intake và request sau recovery chỉ có một intake/projection. Late account/request response được bao phủ bởi regression Flutter account-generation guard. Evidence: `_bmad-output/test-artifacts/story-6-7-manual/offline-error.png`, `offline-retry-success.png`, XML tương ứng, `manual-run-summary.md`, `db-evidence.md`; evidence Story 6.2 giữ nguyên tại `_bmad-output/test-artifacts/story-6-2-manual/`.

### OV01-MAN-032 — Đổi tài khoản xóa cache chéo

**Fixture:** Mother A/B có lifecycle, baby và safety history khác nhau.  
**Thực hiện:** Đăng nhập A và mở từng dữ liệu → logout → đăng nhập B → mở cùng màn hình khi mạng chậm/offline → quay lại online.  
**Mong đợi:** không có flash/cache/data của A; B chỉ thấy tài nguyên thuộc B; request cũ của A bị hủy/bỏ qua; screenshot, recent task và error không lộ dữ liệu A ngoài behavior đã được phê duyệt.  
**Thực tế/evidence:** `[điền]`

### OV01-MAN-033 — TalkBack, font scale và risk cue

**Fixture:** các màn hình onboarding, stage, outcome, baby, GREEN/YELLOW/RED.  
**Thực hiện:** Bật TalkBack → duyệt toàn màn hình → tăng font lên mức lớn hỗ trợ → xoay nếu build hỗ trợ → xem ở grayscale/color correction.  
**Mong đợi:** focus order hợp lý; control có label/state; target chạm đủ dùng; chữ không che CTA; cảnh báo nguy cơ không chỉ dựa vào màu; RED có tên/hành động khẩn cấp rõ; không mắc kẹt focus.  
**Thực tế/evidence:** `[điền]`

### OV01-MAN-034 — Không lộ token hoặc excessive health payload

**Fixture:** quyền xem network/log đã phê duyệt và dữ liệu synthetic có marker.  
**Thực hiện:** Chạy onboarding, outcome, baby, YELLOW/RED → kiểm tra UI, screenshot, app log, backend log và HTTP error được phép → tìm token, password/OTP, raw notes và dữ liệu tài khoản khác.  
**Mong đợi:** không có secret; error không có stack trace/SQL/internal ID không cần thiết; YELLOW chỉ chia sẻ trường đã consent; history/timeline chỉ chứa minimum payload; marker của tài khoản khác không xuất hiện.  
**Thực tế/evidence:** `[điền]`

## 10. Truy vấn xác minh chỉ đọc

Chỉ chạy trên local/disposable hoặc bằng tài khoản read-only đã được phê duyệt.

### 10.1 Một canonical lifecycle hiện hành

```sql
SELECT owner_user_id, count(*) AS active_canonical_count
FROM public.mother_journeys
WHERE status = 'ACTIVE'
  AND journey_type IN ('PRE_PREGNANCY', 'PREGNANCY', 'POSTPARTUM')
GROUP BY owner_user_id
HAVING count(*) > 1;
```

**Mong đợi:** 0 dòng.

### 10.2 Journey và provenance

```sql
SELECT id, owner_user_id, journey_type, status, version,
       start_date, last_menstrual_date, estimated_due_date,
       date_source, date_confidence
FROM public.mother_journeys
WHERE owner_user_id = '<SYNTHETIC_MOTHER_USER_ID>'
ORDER BY created_at;
```

### 10.3 Append-only transition history

```sql
SELECT event_type, from_stage, to_stage, changed_fields,
       source, confidence, reason, journey_version,
       effective_at, recorded_at
FROM public.mother_journey_transitions
WHERE journey_id = '<JOURNEY_ID>'
ORDER BY recorded_at;
```

Với baby, triage, emergency, consultation, content và safety projection, dùng API/audit contract của Story tương ứng. Không đoán tên cột hoặc sửa trực tiếp bảng khi Story chưa chốt migration.

## 11. Bằng chứng tự động bắt buộc

34 ca manual không thay thế automation gate của FR54. Đính kèm các evidence sau vào biên bản full run:

| Evidence ID | Story | Yêu cầu | Hiện tại |
| --- | --- | --- | --- |
| `OV01-AUTO-001` | 6.1 | Báo cáo backend chứa `JourneyCanonicalLifecycleIntegrationTest`; xác nhận Flyway áp dụng `V20260718090000__canonical_mother_lifecycle_history.sql` và `V20260718091000__enforce_mother_journey_transition_immutability.sql`, unique canonical index và append-only trigger | READY |
| `OV01-AUTO-002` | 6.10 | Automated E2E đi qua mọi nhánh OV-01 đã triển khai, có test ID ổn định và report từ clean deployment; lệnh runner chính thức phải được Story 6.10 ghi lại, không suy đoán trong guide này | BLOCKED |
| `OV01-AUTO-003` | 6.10 | Reconcile FR43–FR54 ↔ Stories 6.1–6.10 ↔ manual/auto test IDs ↔ execution reports; không còn requirement/branch mồ côi | BLOCKED |
| `OV01-AUTO-004` | 6.10 | Quality evidence: backend `.\mvnw.cmd test`; mobile `flutter test` và `flutter analyze`; web `npm run lint` và `npm run build`; mọi failure do Epic 6 gây ra phải được sửa, không waive | BLOCKED |

## 12. Ma trận truy vết

| Yêu cầu | Story | Ca kiểm thử |
| --- | --- | --- |
| FR43 — một canonical lifecycle và history | 6.1 | 004–010, 030–032; `OV01-AUTO-001` |
| FR44 — baseline, preferences và consent | 6.2 | 001–003, 009–010 |
| FR45 — dating source/confidence/revision/actor/reason/time | 6.1/6.3 | 006, 011–012 |
| FR46 — ongoing, unknown, live birth, loss/stillbirth wording | 6.3 | 012–015, 030 |
| FR47 — postpartum không bắt buộc baby | 6.4 | 014–018 |
| FR48 — optional 0..n baby và ownership/compatibility | 6.5 | 018–022 |
| FR49 — PRE/PREG/POST/INFANT/TODDLER safety context | 6.6 | 023, 025, 027 |
| FR50 — RED create/reuse emergency, không AI lần hai | 6.6 | 025–027 |
| FR51 — safety timeline và return-to-origin | 6.7 | 023, 028, 031 |
| FR52 — YELLOW verified expert và consent tối thiểu | 6.8 | 024, 034 |
| FR53 — public content/checklist chỉ APPROVED | 6.9 | 009, 029 |
| FR54 — automated E2E và traceability mọi nhánh | 6.10 | 001–034; `OV01-AUTO-002..004` |

## 13. Quality gate và báo cáo kết quả

Một full OV-01 run chỉ được `PASS` khi:

- P0 đạt 100%;
- P1 đạt ít nhất 95%;
- không còn lỗi unauthorized access, cross-account data, duplicate emergency hoặc clinical-safety blocker;
- toàn bộ 10 risk có score ≥6 (`R-OV01-01..09` và `R-OV01-11`) có evidence mitigation được chấp nhận;
- coverage tự động trong phạm vi thay đổi đạt ít nhất 80%; không waive failure do Epic 6 gây ra;
- `OV01-AUTO-001..004` có evidence tương ứng và FR54 không chỉ dựa vào manual test;
- Stories 6.2–6.10 và bốn release gate trong tracker đã đóng;
- NFR chưa có threshold/evidence phải được xử lý bằng `nfr-assess` hoặc waiver có thẩm quyền.

### Biên bản chạy

| Trường | Giá trị |
| --- | --- |
| Commit/build | `[điền]` |
| Backend/DB | `[điền]` |
| Thiết bị/Android | `[điền]` |
| Người test | `[điền]` |
| Ngày/giờ | `[điền]` |
| P0 PASS/Tổng | `[điền]` |
| P1 PASS/Tổng | `[điền]` |
| FAIL | `[điền]` |
| BLOCKED | `[điền]` |
| Quyết định gate | `[PASS / FAIL / BLOCKED]` |

## 14. Xác nhận

| Vai trò | Họ tên | Quyết định | Ngày | Ghi chú |
| --- | --- | --- | --- | --- |
| Người kiểm thử | `[điền]` | `[PASS/FAIL/BLOCKED]` | `[điền]` | `[điền]` |
| QA Lead | `[điền]` | `[PHÊ DUYỆT/TỪ CHỐI]` | `[điền]` | `[điền]` |
| Product/Tech Lead | `[điền]` | `[PHÊ DUYỆT/TỪ CHỐI]` | `[điền]` | `[điền]` |

## 15. Tài liệu tham chiếu

- `03_Design/ActivityDiagram/CareBridge-Main-Workflows.drawio` — trang OV-01.
- `_bmad-output/planning-artifacts/epics.md` — Epic 6, Stories 6.1–6.10.
- `_bmad-output/planning-artifacts/prd.md` — FR43–FR54.
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — trạng thái triển khai hiện tại.
- `_bmad-output/implementation-artifacts/ov01-gap-tracking.yaml` — scope/traceability gốc của proposal OV-01.
- `_bmad-output/implementation-artifacts/investigations/ov01-codebase-gap-investigation.md` — bằng chứng gap lịch sử.
- `06_Testing/TestCases/mobile/MF-01-Story-6.1-Mobile-Manual-Test-Guide.md` — chi tiết regression mobile Story 6.1.
- `06_Testing/TestCases/backend/MF-01-Story-6.1-Manual-Test-Guide.md` — contract/API canonical lifecycle.
