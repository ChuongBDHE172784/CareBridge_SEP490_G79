# Handoff — PhuongNT2 — 2026-06-23

## Mục đích

Tài liệu này là điểm bắt đầu chuẩn (canonical handoff) để một AI Agent khác tiếp tục phiên làm việc mà không cần đọc lại toàn bộ lịch sử hội thoại. Hãy đọc hết file này trước khi sửa code hoặc chạy Git mutation.

## Tóm tắt nhanh

- Repository: `D:\SEP490\CareBridge_SEP490_G79`
- Branch: `PhuongNT2`
- HEAD tại thời điểm bàn giao: `179c1a9dc41a9ef22ff024c6736670e043ea0622`
- Worktree đang cố ý không sạch: `66` mục trong `git status --short`, `344` file untracked theo `git ls-files --others --exclude-standard`.
- Story 1.1 và Story 1.2 đã có commit.
- Story 1.3 mới dừng ở bản spec draft, chưa triển khai.
- Một follow-up cho Story 1.1 (cooldown race và thứ tự OTP) đã hoàn tất code, review, test và package ở local nhưng **chưa được stage/commit/push**.
- Việc cần làm ngay: xác minh không có drift, sau đó hỏi người dùng có muốn commit riêng follow-up Story 1.1 hay không. Tuyệt đối không tự commit hoặc push.

## Quy tắc an toàn bắt buộc

1. Không chạy `git reset --hard`, `git clean`, `git checkout --`, restore hàng loạt, hoặc thao tác phá hủy tương tự.
2. Không chạy `git add .` hay stage toàn bộ worktree. Có nhiều thay đổi requirements/design và file untracked thuộc về người dùng.
3. Không commit hoặc push nếu chưa có chỉ thị rõ ràng của người dùng trong phiên mới.
4. Chỉ sửa/stage đúng các file nằm trong phạm vi tác vụ đã được duyệt.
5. Trước khi làm tiếp, đối chiếu branch, HEAD và fingerprint worktree. Nếu khác dữ liệu bên dưới, dừng và điều tra drift trước.
6. Trao đổi với người dùng bằng tiếng Việt; source code và BMAD artifacts có thể giữ tiếng Anh theo convention hiện tại.

## Kiểm tra đầu phiên

Chạy từ repository root:

```powershell
git branch --show-current
git rev-parse HEAD
$status = @(git status --short); $status.Count
$untracked = @(git ls-files --others --exclude-standard); $untracked.Count
git log -3 --oneline
```

Giá trị bàn giao kỳ vọng:

```text
branch: PhuongNT2
HEAD: 179c1a9dc41a9ef22ff024c6736670e043ea0622
status count: 66
untracked count: 344
```

Ba commit gần nhất:

```text
179c1a9 docs(bmad): STORY-003 user login with credentials planning
51eaf52 fix(security): STORY-002 email-channel verify OTP end-to-end
9254ce8 fix(security): STORY-001 OTP resend test stabilization (EC-E6 cooldown release)
```

## Những gì đã hoàn thành

### Story 1.1 — User registration / OTP resend stabilization

- Phần ổn định ban đầu đã commit tại `9254ce8`.
- Các hành vi resend quan trọng đã được bảo vệ: mixed email+phone bị từ chối, dùng destination đã lưu của user, OTP cũ được đánh dấu used, chỉ một OTP thay thế pending, đúng một audit `OTP_RESENT`, cooldown trả HTTP 429 và route security đúng phạm vi.

### Story 1.2 — Verify registration OTP

- Luồng verify OTP email end-to-end đã commit tại `51eaf52`.

### Story 1.3 — Password-based login

- Planning/spec đã commit tại `179c1a9`.
- Spec hiện tại: `_bmad-output/implementation-artifacts/spec-1-3-password-based-login.md`.
- Trạng thái spec: `draft`, chưa được human approve và chưa triển khai.

### Follow-up Story 1.1 — Cooldown race và OTP ordering

Spec chuẩn:

`_bmad-output/implementation-artifacts/spec-1-1-fix-cooldown-bypass-and-otp-order.md`

Trạng thái:

- `status: done`
- `specLoopIteration: 2`
- Baseline triển khai: commit `179c1a9dc41a9ef22ff024c6736670e043ea0622`
- Code hoàn tất ở local nhưng chưa commit.

Thay đổi chính:

1. Khép race condition trong resend cooldown:
   - `RateLimitPolicy.tryConsumeResend` không còn chạy global cleanup trước per-key `compute`.
   - `cleanExpiredResends` duyệt key rồi dùng `computeIfPresent`, kiểm tra lại giá trị hiện hành atomically trước khi xóa.
   - Có test concurrent first consume, exact-expiry boundary và stress 100 vòng cleanup-vs-consume.

2. Làm deterministic việc chọn pending OTP mới nhất:
   - Repository method đổi thành `findTopByUserIdAndUsedAtIsNullOrderByCreatedAtDescIdDesc`.
   - Tất cả production/test call sites đã migrate.
   - `id DESC` là tie-breaker khi nhiều OTP có cùng `createdAt`.
   - Integration test tạo OTP cùng timestamp và xác nhận OTP có ID lớn hơn/mới hơn bị invalidate.

3. Review loop:
   - Iteration 1 phát hiện cleanup race còn tồn tại và trả về `bad_spec` để loopback.
   - Iteration 2 không còn current-story defect sau khi vá equal-timestamp tie-breaker.
   - Phân loại cuối: `_bmad-output/implementation-artifacts/review-1-1-cooldown-order-iteration-2-classification.md`.

## Các file local thuộc follow-up chưa commit

Tracked và modified:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/repository/OtpVerificationRepository.java`
- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/service/impl/AuthServiceImpl.java`
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/integration/RegistrationIntegrationTest.java`
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/service/AuthServiceResendOtpTest.java`

Untracked trong worktree rộng hơn, nhưng chứa thay đổi follow-up:

- `04_SourceCode/Backend/src/main/java/com/carebridge/backend/security/policy/RateLimitPolicy.java`
- `04_SourceCode/Backend/src/test/java/com/carebridge/backend/security/policy/RateLimitPolicyResendTest.java`

Artifacts liên quan:

- `_bmad-output/implementation-artifacts/spec-1-1-fix-cooldown-bypass-and-otp-order.md`
- `_bmad-output/implementation-artifacts/review-1-1-cooldown-order-iteration-2-classification.md`
- Các file `review-1-1-cooldown-order-*.md` và `review-1-1-cooldown-order-iteration-2-*.md`
- `_bmad-output/implementation-artifacts/deferred-work.md`

Lưu ý: một số `_bmad-output` artifacts bị ignore và sẽ cần `git add -f <exact-file>` nếu người dùng duyệt commit chúng. Không force-add cả thư mục.

## Bằng chứng verification cuối cùng

Môi trường:

- Java: `21.0.10`
- `JAVA_HOME=C:\Program Files\Java\jdk-21.0.10\`

Kết quả:

- Focused suite cuối: `39` tests, `0` failures, `0` errors, `0` skipped.
- Policy suite sau loopback: `13` tests, `0` failures, `0` errors, `0` skipped.
- `clean test`: `61` tests, `0` failures, `0` errors, `0` skipped; `BUILD SUCCESS`.
- `clean package`: cùng `61` tests pass; `BUILD SUCCESS`.
- Executable JAR: `04_SourceCode/Backend/target/backend-0.0.1-SNAPSHOT.jar`.
- JAR được xác minh có kích thước `63,119,490` bytes, timestamp `2026-06-23 01:33:17` (local).

Warnings đã biết, không làm build fail:

- Deprecated converter trong `WebMvcConfig`.
- Mockito/ByteBuddy self-attach warning.

## Cách chạy Maven đáng tin cậy

Thư mục làm việc:

`04_SourceCode/Backend`

Lệnh chuẩn:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

Trong PowerShell sandbox, Maven wrapper đôi lúc lỗi `icm: Cannot index into a null array`. Khi đó dùng Maven wrapper distribution đã cache:

```powershell
$maven = 'C:\Users\nguye\.m2\wrapper\dists\apache-maven-3.9.16\0daed3be3ebd1c706f0e69e8b07c6b73f5cc4ea3dfce72a8d0ec2e849ca2ddb0\bin\mvn.cmd'
$process = Start-Process -FilePath $maven -ArgumentList '-B','-ntp','clean','test' -NoNewWindow -Wait -PassThru
exit $process.ExitCode
```

Đổi `test` thành `package` cho full package build. Nếu sandbox chặn Maven cache/network, xin quyền chạy escalated thay vì sửa cấu hình project.

## Deferred work đã ghi nhận

Danh sách chuẩn nằm tại `_bmad-output/implementation-artifacts/deferred-work.md`, gồm:

- Outbox/after-commit delivery cho transactional resend.
- Shared coordination cho cooldown/pending OTP trong multi-instance deployment.
- OTP HMAC/constant-time comparison.
- Normalize registration email.
- Atomic verification-attempt counting.
- Audit PII handling.
- Eligibility của `USER_REGISTRATION_COMPLETED`.
- BCrypt strength 12.
- Quyết định có invalidate toàn bộ pending OTP cũ hay không.
- Chuyển O(N) rate-limit cleanup khỏi request path nếu profiling chứng minh cần thiết.

Không mở rộng follow-up hiện tại để xử lý các mục này nếu chưa có spec/approval riêng.

## Việc tiếp theo — theo thứ tự

### 1. Xác minh và bảo toàn trạng thái

- Chạy các kiểm tra đầu phiên.
- Xem diff chỉ của các file follow-up nêu trên.
- Nếu fingerprint hoặc HEAD khác, xác định thay đổi mới thuộc về ai trước khi làm tiếp.

### 2. Hỏi người dùng về commit follow-up Story 1.1

Follow-up đã hoàn tất về kỹ thuật nhưng chưa commit. Hỏi người dùng có muốn tạo local commit riêng hay không.

Commit message gợi ý:

```text
fix(security): STORY-001 close cooldown race and OTP ordering
```

Nếu được duyệt:

- Stage từng exact path trong phạm vi.
- Dùng `git add -f` chỉ cho đúng ignored artifact mà người dùng muốn đưa vào commit.
- Kiểm tra `git diff --cached --stat` và `git diff --cached` trước commit.
- Không push nếu chưa được yêu cầu riêng.

### 3. Làm mới và xin duyệt spec Story 1.3

Spec: `_bmad-output/implementation-artifacts/spec-1-3-password-based-login.md`.

Spec đang là draft và baseline bên trong có thể đã cũ. Trước khi implementation:

1. Re-investigate codebase theo HEAD mới nhất (và follow-up commit nếu đã tạo).
2. Cập nhật baseline/code map/acceptance nếu cần.
3. Trình spec cho người dùng duyệt.
4. Chỉ bắt đầu code sau human approval.

Phạm vi dự kiến của Story 1.3:

- Request nhận đúng một trong email/phone cùng password.
- Xác thực BCrypt password.
- Kiểm tra account status.
- Trả access token và refresh token trong một request.
- Ghi login audit.
- Rate limit 5 lần/15 phút.
- Invalid credentials dùng thông báo generic, không làm lộ tài khoản tồn tại hay không.
- Bổ sung focused unit/integration tests, sau đó chạy clean test/package.

Hiện trạng cần nhớ: login hiện tại là OTP-only; `LoginRequest` chỉ có phone và service đang gửi OTP. Không nhầm hiện trạng này với password-based login đã hoàn thành.

### 4. Các story sau Story 1.3

- Story 1.4: Manage Own Sessions.
- Story 1.5: Revoke Sessions on Other Devices.
- Story 1.6: Logout hardening.
- Tạo story còn thiếu cho Sprint 1 allocation:
  - Functional Spec 3.1.1.5 — Forgot Password.
  - Functional Spec 3.1.1.6 — Reset Password.
  - Functional Spec 3.1.1.7 — Change Password.

Sau đó mới chuyển sang profile/privacy/notification/security-admin theo sprint plan.

## Sprint/function status tóm tắt

- Epic 1: `in-progress`.
- Story 1.1: `done` (có follow-up local chờ quyết định commit).
- Story 1.2: `done`.
- Story 1.3: `backlog`, spec draft.
- Story 1.4–1.6: `backlog`.
- Epic 2–5: `backlog`.

Theo đối chiếu allocation Sprint 0 — thành viên 1:

- Đã có backend flow: Register, Verify OTP.
- Partial: Login (OTP-only, chưa password-based), Logout, View Profile, Update Profile mới giới hạn name/avatar.
- Skeleton/partial: Manage Own Sessions entity; Privacy mới ở consent subset; Notifications mới chủ yếu entity; audit/security mới có event log/search một phần.

## Tài liệu nên đọc theo thứ tự

1. File handoff này.
2. `_bmad-output/implementation-artifacts/spec-1-1-fix-cooldown-bypass-and-otp-order.md`.
3. `_bmad-output/implementation-artifacts/review-1-1-cooldown-order-iteration-2-classification.md`.
4. `_bmad-output/implementation-artifacts/deferred-work.md`.
5. `_bmad-output/implementation-artifacts/spec-1-3-password-based-login.md` khi bắt đầu Story 1.3.
6. `docs/bmad/function-spec-task-allocation.md` và `01_Requirements/SRS/3_Functional_Specification.md` khi lập kế hoạch function specs tiếp theo.

Handoff cũ chỉ dùng làm lịch sử bổ sung:

- `_bmad-output/implementation-artifacts/handoff-phuongnt2-2026-06-22.md`
- `_bmad-output/implementation-artifacts/handoff-1-1-backend-test-stabilization.md`

## Điều kiện kết thúc bàn giao

Agent tiếp nhận được xem là đã đồng bộ khi có thể xác nhận:

- Đúng branch/HEAD hoặc đã giải thích rõ drift.
- Không làm mất các thay đổi user-owned trong dirty worktree.
- Hiểu follow-up Story 1.1 đã pass 61 tests và package nhưng chưa commit.
- Không tự commit/push.
- Biết hành động tiếp theo là xin quyết định commit follow-up, rồi refresh và xin duyệt Story 1.3 spec trước khi code.
