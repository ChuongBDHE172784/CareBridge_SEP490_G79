# TEST-DRIVEN DEVELOPMENT SPECIFICATION
# Dọn ảnh nội dung mồ côi trên Cloudinary (Orphaned Public Content Image Cleanup)

**Document ID:** `CB-FILE-IMP-013-TS`
**Version:** `1.0`
**Date:** `2026-07-23`
**Status:** `Approved`
**Author:** `AI Agent — Claude`
**Reviewed by:** `[x] HuyND`
**DPO Sign-off:** `N/A — Internal/Public, không có PII`
**Approved by:** `[x] HuyND — 2026-07-23`

**References:**
- `04_Implement/ContentImageOrphanCleanup/ContentImageOrphanCleanup_TDS.md` (bắt buộc đọc §3 ADR trước khi implement)
- `04_Implement/UC169_DeleteFile/UC169_DeleteFile_TDS.md` (ADR-FILE-008, OI-169-3 — gốc rễ của tính năng này)
- `04_Implement/ContentRichTextEditor/ContentRichTextEditor_TDS.md` (ADR-RTE-007 addendum — quyết định giữ ảnh cho content ARCHIVED, lý do job này tồn tại)

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi                             |
| ---------- | ------------------ | ---------------------------------------------- |
| 2026-07-23 | AI Agent — Claude  | Khởi tạo tài liệu — Draft, chờ review cùng TDS |
| 2026-07-23 | AI Agent — Claude  | **Tự sửa trước khi trình user** (phát hiện qua advisor review, chưa có code): thêm `L4` vào Logic Issues, viết lại `TC-COND-005`/`TC-CLEAN-005` từ "loại theo purpose" (test sai mục tiêu — mối đe doạ đó purpose một mình đã chặn được) sang đúng mối đe doạ thật: `purpose=PUBLIC_CONTENT_IMAGE` nhưng `accessMode != PUBLIC`. Đổi tên method repository trong toàn bộ test case sang `findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore`. |
| 2026-07-23 | AI Agent — Claude  | **User duyệt (qua tài liệu `ContentRichTextEditor`): "chọn đáp án bạn cảm thấy tốt nhất, không phức tạp quá, rồi bắt đầu code đi" — implement xong theo TDD Red→Green.** RED xác nhận thật: `./mvnw -o test-compile` fail (`cannot find symbol: class PublicContentImageCleanupJob`). Implement `PublicContentImageCleanupJob` + 2 repository method + `AuditAction.FILE_ORPHAN_PURGED`. GREEN: `PublicContentImageCleanupJobTest` 8/8 (`TC-CLEAN-001,002,004-009`). `TC-CLEAN-003` viết thành `ContentRepositoryIntegrationTest.java` (Testcontainers) đúng theo spec nhưng CHƯA tự chạy được trong môi trường này — `IllegalStateException: Could not find a valid Docker environment` (Docker không cài/không chạy trên máy này, khác lỗi `SchemaManagementException` đã ghi nhận trước đó cho integration test khác trong project, nhưng cùng nhóm gap Testcontainers pre-existing) — bù bằng đọc trực tiếp JPQL của `existsByBodyContaining()`, xác nhận không có filter status, **nhưng đây chỉ là bằng chứng đọc mã nguồn, chưa phải thực nghiệm trên Postgres thật**. `./mvnw test` toàn repo: `git stash` baseline 2394/9/120 → sau code 2406/9/121, không regression. |

---

## 1. Thông tin Module

| Field | Value |
|-------|-------|
| **Feature / Gap ID** | Hoàn thiện `OI-169-3` |
| **Module** | `file` (job), phụ thuộc `content` (đọc `ContentRepository`) |
| **Data Classification** | `Internal/Public` — chỉ `FilePurpose.PUBLIC_CONTENT_IMAGE` AND `FileAccessMode.PUBLIC` (bắt buộc cả 2, xem TC-CLEAN-005) |
| **Upstream Dependencies** | `UploadedFileRepository`, `ContentRepository`, `CloudinaryStorageService`, `AuditService` |
| **Downstream Consumers** | Không có (job nội bộ, không API/UI) |

---

## 2. Logic Issues Resolved

| # | Spec gốc (giả định ban đầu) | Thực tế (đã đọc code trực tiếp) | Fix áp dụng trong test |
|---|------------------------|--------------------------|------------------------|
| L1 | Giả định ban đầu: "xoá ảnh khi content bị xoá" | Content **không bao giờ hard-delete** — nút "Xóa" chỉ gọi `archiveContent()`/`hideContent()` (soft-delete, `ContentStatus.ARCHIVED`). Xác nhận bằng grep: không có `repository.delete(...)` nào trong package `content`. | Test case KHÔNG được gắn "xoá ảnh" vào bất kỳ content action nào — job chạy độc lập theo cron, chỉ dựa vào việc ảnh có còn được `body` nào tham chiếu hay không, bất kể `ContentStatus` |
| L2 | Giả định: `uploaded_files` có FK tới `content_items` | Không có FK/bảng liên kết nào — mối liên kết duy nhất là public_id xuất hiện dạng substring trong `content_items.body` (TEXT) | Test phải seed `content_items.body` với chuỗi chứa/không chứa đúng public_id, không dùng FK giả định |
| L3 | Giả định: `IFileService.purgeFile()` có thể tái dùng trực tiếp cho job | `purgeFile(fileId, callerId)` là **owner-scoped** (so `file.getOwnerUserId().equals(callerId)`) — thiết kế cho user tự xoá file của chính họ qua HTTP request có principal thật, không phù hợp cho job hệ thống không có `callerId` | Test KHÔNG gọi qua `IFileService`/`FileServiceImpl` — test trực tiếp `PublicContentImageCleanupJob` với mock `CloudinaryStorageService`/`UploadedFileRepository` |
| L4 | Bản nháp TDS đầu tiên: lọc ứng viên chỉ bằng `purpose = PUBLIC_CONTENT_IMAGE` | `FileServiceImpl.uploadUsing()` (dòng 189-201) mặc định gán `purpose = PUBLIC_CONTENT_IMAGE` cho upload ảnh KHÔNG liên quan content editor khi caller không truyền `purpose` tường minh — `accessMode` lúc đó là `AUTHENTICATED`/`PRIVATE`, không phải `PUBLIC`. Chỉ `uploadPublicFile()` và request tường minh `accessMode=PUBLIC` mới đúng là ảnh content editor (verify bằng `grep FileAccessMode.PUBLIC` toàn backend) | TC-CLEAN-005 phải verify filter dùng **cả** `purpose` VÀ `accessMode=PUBLIC` — thiếu 1 trong 2 là false-positive nguy hiểm (hard-delete nhầm dữ liệu người dùng khác) |

---

## 3. Test Design Specification (TDS)

### TDS-01 — Scope

```
PublicContentImageCleanupJob (mới) bao gồm:
├── Unit (mock UploadedFileRepository, ContentRepository, CloudinaryStorageService, AuditService, Clock)
└── Integration (Testcontainers — CHƯA chạy được, cùng gap môi trường pre-existing đã ghi nhận
    ở ContentRichTextEditor_Test-Spec.md §5.1, không lặp lại chi tiết ở đây)
```

### TDS-02 — Test Basis
- `ContentImageOrphanCleanup_TDS.md` §3 (ADR-CLEAN-001, ADR-CLEAN-002), §5 (Static Modeling — chữ ký method chính xác)
- `V1__init_schema.sql` + migration tạo `uploaded_files`/`content_items` — cột `body` (TEXT), `storage_key`, `purpose`, `status`, `created_at`

### TDS-03 — Test Conditions

| Condition ID | Test Condition | Test Case |
| ------------ | --------------- | --------- |
| TC-COND-001  | Ảnh PUBLIC_CONTENT_IMAGE, ACTIVE, quá grace period, KHÔNG có content nào (mọi status) chứa public_id trong `body` → bị xoá (Cloudinary `delete()` gọi đúng key, DB row hard-delete) | `TC-CLEAN-001` |
| TC-COND-002  | Ảnh được tham chiếu bởi content `DRAFT` → KHÔNG bị xoá | `TC-CLEAN-002` |
| TC-COND-003  | Ảnh được tham chiếu bởi content `ARCHIVED` → KHÔNG bị xoá (regression guard cho quyết định giữ ảnh archived — ADR-RTE-007 addendum) | `TC-CLEAN-003` |
| TC-COND-004  | Ảnh mới upload, chưa đủ grace period (vd 1 giờ trước) dù không content nào tham chiếu → KHÔNG bị xoá | `TC-CLEAN-004` |
| TC-COND-005  | File `purpose = PUBLIC_CONTENT_IMAGE` NHƯNG `accessMode != PUBLIC` (vd `AUTHENTICATED` — do `FileServiceImpl.uploadUsing()` mặc định gán purpose này cho upload ảnh không liên quan content editor) → job hoàn toàn bỏ qua, KHÔNG nằm trong tập ứng viên (đây là false-positive nguy hiểm nhất nếu lọc thiếu `accessMode`, xem TDS ADR-CLEAN-001 phương án A0 bị loại) | `TC-CLEAN-005` |
| TC-COND-006  | `dry-run=true` → phát hiện đúng ứng viên mồ côi nhưng KHÔNG gọi `delete()`, KHÔNG xoá row | `TC-CLEAN-006` |
| TC-COND-007  | `enabled=false` → job return ngay, không query gì cả | `TC-CLEAN-007` |
| TC-COND-008  | 1 ảnh trong batch gây exception khi xoá (Cloudinary lỗi) → job KHÔNG crash, các ảnh còn lại trong batch vẫn được xử lý | `TC-CLEAN-008` |
| TC-COND-009  | Xoá thành công → `AuditService.log(FILE_ORPHAN_PURGED, ...)` được gọi đúng 1 lần với đúng `fileId` | `TC-CLEAN-009` |

### TDS-04 — Test Techniques

| Technique | Applied To | Rationale |
|-----------|------------|-----------|
| Equivalence Partitioning | `ContentStatus`: DRAFT / PUBLISHED / ARCHIVED — đều phải bảo vệ ảnh như nhau | Đảm bảo "còn tham chiếu" không bị hiểu nhầm là "chỉ tính PUBLISHED" (đúng bug tiềm ẩn nếu implement sai — xem ADR-CLEAN-001 Phương án B bị bác) |
| Boundary | Grace period: đúng ngưỡng, trước ngưỡng, sau ngưỡng | TC-CLEAN-004 |
| Fault injection | Exception giữa batch | TC-CLEAN-008 — best-effort hygiene job không được phép crash vì 1 phần tử lỗi |

### TDS-05 — Test Data

Toàn bộ `SYNTHETIC` — `UploadedFile`/`ContentItem` dựng bằng builder trong test, không dùng dữ liệu Cloudinary/DB thật cho unit test (chỉ integration/manual QA khi implement mới chạm Cloudinary thật, theo đúng thói quen đã thiết lập ở `ContentRichTextEditor`).

---

## 4. Test Case Specification

> **Cập nhật 2026-07-23 — implement xong theo TDD Red→Green.** RED xác nhận thật trước khi code (`./mvnw -o test-compile` fail: `cannot find symbol: class PublicContentImageCleanupJob`). Sau khi implement: `PublicContentImageCleanupJobTest` — `Tests run: 8, Failures: 0, Errors: 0` (`TC-CLEAN-001,002,004-009`). `TC-CLEAN-003` viết đúng theo TDD nhưng chưa tự chạy được trong môi trường này (không có Docker) — xem ghi chú riêng ở mục đó.

### TC-CLEAN-001 — Ảnh mồ côi thật sự (quá grace period, không ai tham chiếu) bị xoá đúng

**Severity:** `HIGH`
**Feature Under Test:** `PublicContentImageCleanupJob.cleanupOrphanedImages()`
**Test File:** `PublicContentImageCleanupJobTest.java` (mới)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-001`

**Test Steps:** Mock `fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(...)` trả về 1 `UploadedFile` (`storageKey="carebridge/abc|image|PUBLIC"`, `createdAt` = 48h trước). Mock `contentRepository.existsByBodyContaining("carebridge/abc")` trả `false`. Act `job.cleanupOrphanedImages()`. Assert: `cloudinaryStorageService.delete("carebridge/abc|image|PUBLIC")` được gọi đúng 1 lần; `fileRepository.delete(file)` được gọi đúng 1 lần.
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_orphanPastGracePeriod_getsPurged`

---

### TC-CLEAN-002 — Ảnh được tham chiếu bởi content DRAFT → không xoá

**Severity:** `CRITICAL` — false positive ở đây nghĩa là xoá nhầm ảnh đang được soạn dở
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-002`

**Test Steps:** Cùng setup TC-CLEAN-001 nhưng `contentRepository.existsByBodyContaining(...)` trả `true`. Assert: `cloudinaryStorageService.delete(...)` KHÔNG được gọi; `fileRepository.delete(...)` KHÔNG được gọi.
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_stillReferenced_isNotPurged`

---

### TC-CLEAN-003 — Ảnh được tham chiếu bởi content ARCHIVED → không xoá (regression guard)

**Severity:** `CRITICAL` — đây chính là hành vi user đã yêu cầu tường minh, sai ở đây là vi phạm trực tiếp quyết định của user
**TDD Phase:** 🟡 Đã viết test thật (`ContentRepositoryIntegrationTest.java`), chưa tự chạy được — môi trường này không có Docker
**Condition Ref:** `TC-COND-003`

**Test Steps:** Integration-style (hoặc unit với `existsByBodyContaining` thật, không mock query logic): seed 1 `ContentItem` với `status=ARCHIVED`, `body` chứa public_id của ảnh ứng viên. Assert `existsByBodyContaining(publicId)` trả `true` **không lọc theo status** — tức câu JPQL không có điều kiện `WHERE c.status = ...`. Đây là test quan trọng nhất để bắt lỗi nếu ai đó (kể cả AI) vô tình thêm filter status vào query.
**Current Status:** 🟡 Test viết đúng theo spec (`ContentRepositoryIntegrationTest.existsByBodyContaining_archivedContent_stillCountsAsReferenced`, extends `AbstractPostgresIntegrationTest`), nhưng chạy thử thất bại với `IllegalStateException: Could not find a valid Docker environment` — khác lỗi `SchemaManagementException` đã ghi nhận trước đó cho các integration test khác trong project (đó là do thiếu migration `expert_profiles.display_name`; đây là do Docker không cài/không chạy trên máy này), nhưng cùng nhóm "Testcontainers pre-existing gap", không phải bug của code này. **Bù bằng verify trực tiếp mã nguồn:** đọc `ContentRepository.existsByBodyContaining()` — JPQL chỉ có `WHERE c.body LIKE CONCAT('%', :publicId, '%')`, không có bất kỳ điều kiện `c.status = ...` nào — về mặt logic không thể lọc theo status dù chạy trên dữ liệu thật hay không. **Chưa phải bằng chứng thực nghiệm (chưa chạy trên Postgres thật)** — chỉ là bằng chứng đọc mã nguồn, cần chạy `ContentRepositoryIntegrationTest` thật khi có Docker khả dụng để xác nhận hoàn toàn.

---

### TC-CLEAN-004 — Ảnh chưa đủ grace period → không xoá dù không ai tham chiếu

**Severity:** `HIGH`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-004`

**Test Steps:** Dùng `Clock` cố định (test constructor, cùng pattern `FirebaseEventRetentionJob`). Mock repository trả về ảnh có `createdAt` = 1 giờ trước `now` (grace period 24h). Assert: `findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore` được gọi với `cutoff = now - 24h` — verify **tham số cutoff đúng**, không verify hành vi DB thật (đó là việc của Postgres, method là derived query).
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_usesConfiguredGracePeriodAsCutoff`

---

### TC-CLEAN-005 — `purpose=PUBLIC_CONTENT_IMAGE` nhưng `accessMode != PUBLIC` → hoàn toàn bỏ qua (false-positive nguy hiểm nhất)

**Severity:** `CRITICAL` — đây là lỗi thật đã tìm thấy khi review bản nháp TDS trước khi trình user (xem TDS CHANGELOG + ADR-CLEAN-001 phương án A0). Lọc chỉ theo `purpose` KHÔNG đủ: `FileServiceImpl.uploadUsing()` mặc định gán `purpose=PUBLIC_CONTENT_IMAGE` cho các upload ảnh không hề liên quan content editor (khi caller không truyền `purpose` tường minh), với `accessMode=AUTHENTICATED`/`PRIVATE`. Nếu job chỉ lọc `purpose`, những ảnh này sẽ luôn có `existsByBodyContaining()=false` (không nội dung nào tham chiếu chúng — chúng chưa từng được chèn vào bài viết) → bị hard-xoá nhầm. Test này PHẢI fail nếu ai đó (kể cả AI lúc implement) vô tình bỏ điều kiện `accessMode` khi viết method thật.
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-005`

**Test Steps:** Verify lời gọi thật tới repository trong `cleanupOrphanedImages()` luôn truyền đủ 2 tham số `FilePurpose.PUBLIC_CONTENT_IMAGE` VÀ `FileAccessMode.PUBLIC` cho `findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(...)` — dùng `verify(fileRepository).findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(eq(FilePurpose.PUBLIC_CONTENT_IMAGE), eq(FileAccessMode.PUBLIC), eq(FileStatus.ACTIVE), any())`. Nếu implement sau này đổi lại thành method chỉ lọc `purpose` (bỏ `accessMode`), lời gọi Mockito `verify` với chữ ký cũ sẽ không compile/không match — bắt lỗi ngay ở biên dịch hoặc ở runtime, tuỳ cách đổi.
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_alwaysFiltersByPurposeAndPublicAccessMode`. Vì chữ ký method thật (`findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore`) đã bao gồm `accessMode` ngay từ đầu (bug được sửa trước khi code, xem TDS CHANGELOG), test này về bản chất verify "không ai xoá bớt tham số" — vẫn giữ nguyên giá trị regression-guard cho tương lai.

---

### TC-CLEAN-006 — `dry-run=true` chỉ log, không xoá gì thật

**Severity:** `HIGH` — đây là cơ chế an toàn chính khi mới deploy (TDS §7 Bước 1)
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-006`

**Test Steps:** Set field `dryRun=true` (qua constructor test hoặc reflection, cùng pattern `firestoreEnabled`/`retentionHours` của `FirebaseEventRetentionJob`). Setup giống TC-CLEAN-001 (ứng viên hợp lệ để xoá). Assert: `cloudinaryStorageService.delete(...)` KHÔNG được gọi; `fileRepository.delete(...)` KHÔNG được gọi.
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_dryRun_detectsButDoesNotDelete`

---

### TC-CLEAN-007 — `enabled=false` → job không làm gì cả

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-007`

**Test Steps:** Set `enabled=false`. Act `job.cleanupOrphanedImages()`. Assert: `fileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(...)` KHÔNG được gọi (return sớm trước khi query).
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_disabled_doesNothing`

---

### TC-CLEAN-008 — Lỗi giữa batch không làm crash job, các phần tử còn lại vẫn xử lý

**Severity:** `HIGH` — best-effort hygiene job, giống nguyên tắc `FirebaseEventRetentionJob`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-008`

**Test Steps:** Mock repository trả về 2 ứng viên hợp lệ. Mock `cloudinaryStorageService.delete(...)` throw exception cho ảnh đầu tiên. Assert: exception KHÔNG propagate ra khỏi `cleanupOrphanedImages()` (method không throw); `fileRepository.delete(...)` VẪN được gọi cho ảnh thứ hai (batch không dừng giữa chừng).
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_errorOnOneFile_stillProcessesTheRest`

---

### TC-CLEAN-009 — Audit log ghi đúng khi xoá thành công

**Severity:** `MEDIUM`
**TDD Phase:** 🟢 GREEN
**Condition Ref:** `TC-COND-009`

**Test Steps:** Setup giống TC-CLEAN-001. Assert: `auditService.log(AuditAction.FILE_ORPHAN_PURGED, isNull(), eq("UploadedFile"), eq(file.getId().toString()), any())` được gọi đúng 1 lần (callerId = null vì không có principal — job hệ thống, cần xác nhận `AuditService.log()` chấp nhận `null` callerId, hoặc dùng 1 UUID hằng số "SYSTEM" nếu không — verify khi implement, ghi vào Logic Issues nếu khác).
**Current Status:** 🟢 Passing — `cleanupOrphanedImages_successfulPurge_logsAudit`. Đã xác nhận `callerId=null` an toàn: `audit_logs.actor_user_id` trong `V1__init_schema.sql` không có ràng buộc `NOT NULL`, và `AuditServiceImpl.log()` gán trực tiếp không validate — không cần UUID "SYSTEM" giả.

---

## 5. Red-Green-Refactor Tracker

| TC ID          | Test File                          | 🔴 RED confirmed | 🟢 GREEN (commit) | 🔵 REFACTOR note |
| -------------- | ----------------------------------- | ---------------- | ------------------ | ----------------- |
| `TC-CLEAN-001` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-002` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-003` | `ContentRepositoryIntegrationTest.java` | `[ ]`         | Viết xong, chặn bởi Docker không khả dụng trong môi trường này — bù bằng verify đọc mã nguồn JPQL | |
| `TC-CLEAN-004` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-005` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-006` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-007` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-008` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |
| `TC-CLEAN-009` | `PublicContentImageCleanupJobTest.java` | `[x]`         | Passed              |                    |

### 5.1 Red Gate Protocol

**Đã chạy thật, không phải kế hoạch:** `./mvnw -o test-compile` → `[ERROR] cannot find symbol: class PublicContentImageCleanupJob` — RED hợp lệ (lỗi biên dịch, class chưa tồn tại). Sau khi implement: `./mvnw -o test -Dtest="PublicContentImageCleanupJobTest"` → `Tests run: 8, Failures: 0, Errors: 0` — GREEN, `BUILD SUCCESS`.

---

## 6. Entry / Exit Criteria

### Entry Criteria
- [x] `ContentImageOrphanCleanup_TDS.md` §3 (ADR-CLEAN-001, ADR-CLEAN-002) đã `Approved` (Status: `Accepted` cho cả 2 ADR)
- [x] 3 câu hỏi mở ở TDS §8 đã được xác nhận (tên `AuditAction`, grace period, có cần manual-trigger endpoint không) — user: "chọn đáp án bạn cảm thấy tốt nhất, không phức tạp quá"

### Exit Criteria (Definition of Done)
- [x] `TC-CLEAN-001,002,004-009` GREEN (8/8). **`TC-CLEAN-003` CHƯA GREEN bằng thực nghiệm** — viết đúng test nhưng chặn bởi Docker không khả dụng trong môi trường này; bù bằng verify đọc mã nguồn (xem mục đó) — không tự nhận là hoàn thành 100% cho tiêu chí này.
- [x] `./mvnw test` toàn repo không regression so với baseline (`git stash` so sánh: 2394/9/120 → 2406/9/121, chênh lệch đúng bằng test mới + 1 lỗi Docker đã giải thích)
- [ ] Dry-run chưa chạy thử trên dev/staging thật (job hiện `dry-run=true` mặc định, an toàn để deploy, nhưng chưa có dữ liệu ảnh mồ côi thật để quan sát log — follow-up khi có nhu cầu thật)

---

## 7. Rollback Plan

Không có migration DB (TDS §5) — rollback = set `carebridge.content.image-cleanup.enabled=false` trong `.env`, không cần revert code.

---

## 8. CASE 2.0 Anti-Pattern Detection

| AP-ID | Anti-Pattern | Kiểm tra cho tài liệu này | Trạng thái |
|-------|-------------|---------------------------|------------|
| AP-AI-003 | Implicit Decision — code giả định kiến trúc không có trong ADR | Mọi quyết định (accessMode filter, dry-run, grace period, hard-delete) đều truy vết về ADR-CLEAN-001/002 và verify được trong code thật | `[x]` Verify sau implement |
| AP-AI-005 | Hallucinated Contract — import service/method không tồn tại | Đã verify trực tiếp: `UploadedFile`, `FilePurpose.PUBLIC_CONTENT_IMAGE`, `FileStatus`, `ContentItem.body`, `ContentRepository`, `CloudinaryStorageService.delete()`, `FirebaseEventRetentionJob` (pattern tham chiếu) — tất cả đọc trực tiếp từ source, không suy đoán | `[x]` Đã verify lúc viết TDS, tái xác nhận lúc code chạy GREEN |

*Tài liệu này đang `Draft` — CHƯA code. Chờ duyệt cùng TDS trước khi chuyển Phase 3.*
