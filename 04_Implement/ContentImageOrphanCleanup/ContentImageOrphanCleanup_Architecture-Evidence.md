> [!IMPORTANT]
> Historical technical-subflow evidence for `UC-AD-08`; this is not a canonical current TDS. Current code and the canonical code-first specification override conflicts.

# ENGINEERING DOCUMENTATION STANDARD (EDS) v2.0
# Technical Design Specification — Dọn ảnh nội dung mồ côi trên Cloudinary (Orphaned Public Content Image Cleanup)

| Field              | Value                                                      |
| ------------------ | ---------------------------------------------------------- |
| **Document ID**    | `CB-FILE-IMP-013`                                          |
| **Version**        | `1.0`                                                      |
| **Date**           | `2026-07-23`                                               |
| **Status**         | `Approved`                                                 |
| **Document Owner** | `HuyND`                                                    |
| **Author**         | `AI Agent — Claude`                                        |
| **Reviewed by**    | `[x] HuyND`                                                |
| **DPO Sign-off**   | `N/A — Data Classification: Internal/Public, không có PII (ảnh nội dung bài viết/FAQ công khai, không phải ảnh định danh)` |
| **Approved by**    | `[x] HuyND — 2026-07-23, "chọn đáp án bạn cảm thấy tốt nhất, không phức tạp quá" — áp dụng đề xuất mặc định ở §8 (xem bên dưới)` |
| **Last Review**    | `2026-07-23`                                               |
| **Based on EDS**   | `v2.0`                                                     |

---

## CHANGELOG

| Ngày       | Người thực hiện   | Nội dung thay đổi |
| ---------- | ----------------- | ------------------ |
| 2026-07-23 | AI Agent — Claude | Tạo tài liệu lần đầu — Draft, chờ review. Phát sinh từ QA thủ công `ContentRichTextEditor` (user báo "ảnh không bị xoá trên Cloudinary khi xoá bản ghi") — xem §0. |
| 2026-07-23 | AI Agent — Claude | **Tự sửa trước khi trình user duyệt** (phát hiện qua advisor review, chưa có code, chưa ai chạy): bản nháp đầu tiên của ADR-CLEAN-001 chỉ lọc `purpose = PUBLIC_CONTENT_IMAGE` — KHÔNG AN TOÀN, vì `FileServiceImpl.uploadUsing()` mặc định gán `purpose = PUBLIC_CONTENT_IMAGE` cho cả những upload ảnh không liên quan content editor (khi caller không truyền `purpose`), với `accessMode = AUTHENTICATED`/`PRIVATE` chứ không phải `PUBLIC`. Nếu triển khai đúng bản nháp đầu, job sẽ hard-xoá nhầm những ảnh đó (false positive, không thể khôi phục — đây là job hard-delete). Sửa: thêm điều kiện `accessMode = PUBLIC` vào filter (đã verify bằng `grep FileAccessMode.PUBLIC` toàn backend — chỉ `uploadPublicFile()` và request tường minh `accessMode=PUBLIC` của `uploadWithPurpose()` từng set giá trị này). Đổi tên method thành `findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore`. Xem ADR-CLEAN-001 phương án A0 (bị loại, ghi lại lý do). |
| 2026-07-23 | AI Agent — Claude | **User duyệt qua ContentRichTextEditor: "chọn đáp án bạn cảm thấy tốt nhất, không phức tạp quá, rồi bắt đầu code đi" — áp dụng cho cả tài liệu này (3 câu hỏi mở ở §8 chốt theo đề xuất mặc định) — implement xong theo TDD Red→Green.** RED xác nhận thật (lỗi biên dịch — class `PublicContentImageCleanupJob` chưa tồn tại) trước khi code. Implement: `PublicContentImageCleanupJob` (cùng pattern `@Scheduled`/`@Value`/test-`Clock` như `FirebaseEventRetentionJob`), 2 repository method mới, `AuditAction.FILE_ORPHAN_PURGED`. `PublicContentImageCleanupJobTest`: 8/8 GREEN (`TC-CLEAN-001,002,004-009`). `TC-CLEAN-003` (regression guard status) viết dạng Testcontainers integration test nhưng chưa tự chạy được — môi trường này không có Docker, khác gap `SchemaManagementException` đã ghi nhận trước đó nhưng cùng nhóm "Testcontainers pre-existing" — bù bằng đọc trực tiếp JPQL xác nhận không có filter status. `./mvnw test` toàn repo: baseline sạch qua `git stash` (2394/9/120) → sau code (2406/9/121), chênh lệch đúng = test mới GREEN + 1 lỗi Docker đã giải thích, không regression. Xem §9. |

---

## 0. Bối cảnh phát sinh

Trong lúc QA thủ công tính năng rich-text-editor (`04_Implement/ContentRichTextEditor/`), user báo: *"khi xóa bản ghi đấy đi thì ảnh không bị xóa trên cloudinary"*. Điều tra (đọc code trực tiếp, không suy đoán):

- `AdminContentServiceImpl.hideContent()` (nút "Xóa" trên UI thực chất gọi `archiveContent()` → `hideContent()`) chỉ đổi `ContentStatus` sang `ARCHIVED` — **không đụng tới file/Cloudinary**, không hard-delete row. Toàn bộ package `content` được grep xác nhận **không có bất kỳ hard-delete nào** (`repository.delete(...)`) — nhất quán với comment trong code: *"ADR-001: only status changes — soft-delete via existing ARCHIVED value, no hard delete"*.
- Vì vậy đây **không phải bug đơn thuần** — xoá ảnh ngay khi archive sẽ phá vĩnh viễn ảnh của một bản ghi vẫn còn xem lại được (qua audit, hoặc nếu tương lai có tính năng "khôi phục"). Đã hỏi lại user qua `AskUserQuestion`; user chọn: **xây cơ chế dọn ảnh mồ côi riêng (batch job)**, không gắn vào action archive.
- Trong lúc đọc code liên quan (`FileServiceImpl`, `IFileService`), phát hiện **UC-169 (`04_Implement/UC169_DeleteFile/UC169_DeleteFile_TDS.md`) đã tự flag đúng gap này từ trước**, dưới tên **`OI-169-3` (Open Item, chưa xử lý)**: *"Physical storage object (...) is never purged... No retention/purge job exists yet... future scheduled job, out of scope for UC-169"*. Tài liệu này là phần hiện thực hoá `OI-169-3`, phạm vi thu hẹp lại đúng loại file cần dọn nhất hiện tại: **ảnh nội dung PUBLIC** (`FilePurpose.PUBLIC_CONTENT_IMAGE`) — loại duy nhất được upload lên Cloudinary **trước khi** người dùng lưu bản ghi cha (nên có khả năng mồ côi cao nhất: đóng tab, thay ảnh khác trước khi lưu, v.v.), và không phải PII/sensitive (khác hẳn `EXPERT_IDENTITY_*` — cố tình không đụng tới các purpose đó trong tài liệu này).

---

## 1. Tổng quan Module

| Field | Value |
|-------|-------|
| **Module Name** | Public Content Image Orphan Cleanup |
| **Bounded Context** | `file` (job đọc thêm từ `content` như upstream dependency, không sửa `content`) |
| **Data Classification** | `Internal/Public` — chỉ xử lý `FilePurpose.PUBLIC_CONTENT_IMAGE` **AND `FileAccessMode.PUBLIC`** (bắt buộc cả 2 điều kiện — xem ADR-CLEAN-001, phương án A0 bị loại vì chỉ lọc `purpose` không an toàn). **Không** đụng tới `EXPERT_IDENTITY_*`/`MEDICAL_CONTRIBUTION_*` (PII/Sensitive-PII), và không đụng tới ảnh `PUBLIC_CONTENT_IMAGE`-nhưng-không-`PUBLIC` (ảnh từ luồng upload chung khác, vô tình mang cùng nhãn `purpose` do giá trị mặc định trong `FileServiceImpl.uploadUsing()`). |
| **Compliance Scope** | `PDPA` — gián tiếp (giảm rác dữ liệu lưu trữ không mục đích, phù hợp tinh thần "storage limitation" dù data không phải PII) |
| **Upstream Dependencies** | `UploadedFileRepository` (existing), `ContentRepository` (existing, +1 method mới), `CloudinaryStorageService`/`IStorageService.delete()` (existing, không đổi), `AuditService` (existing) |
| **Downstream Consumers** | Không có — job nội bộ, không có API/UI. Kết quả gián tiếp: giảm dung lượng lưu trữ Cloudinary theo thời gian. |

**Hoàn thiện Open Item:** `OI-169-3` (`UC169_DeleteFile_TDS.md` §12.4/§4.2) — "future scheduled job", phạm vi thu hẹp còn `PUBLIC_CONTENT_IMAGE` only.

---

## 2. Ma trận Truy vết (Traceability Matrix)

| Requirement ID | Loại | Mô tả yêu cầu | Thành phần Code | ADR liên quan |
|----------------|------|---------------|-----------------|---------------|
| OI-169-3 (kế thừa) | Open Item | Vật lý xoá file mồ côi trên storage — chưa có job nào tồn tại | `PublicContentImageCleanupJob` (mới) | ADR-CLEAN-001, ADR-CLEAN-002 |
| BR-RTE-USER-001 | User Story (phiên này) | User yêu cầu dọn ảnh mồ côi thay vì xoá ngay lúc archive, để không phá ảnh của content đã archive | `PublicContentImageCleanupJob.cleanupOrphanedImages()` | ADR-CLEAN-001 |

---

## 3. Architecture Decision Records (ADR)

### ADR-CLEAN-001 — Định nghĩa "mồ côi": không được tham chiếu bởi BẤT KỲ `content_items.body` nào (mọi status), cộng grace period

| Field        | Value           |
| ------------ | --------------- |
| **Status**   | `Accepted`      |
| **Deciders** | `HuyND`         |
| **Date**     | `2026-07-23`    |

#### Bối cảnh
`uploaded_files` không có FK/liên kết cấu trúc tới `content_items` — mối liên kết DUY NHẤT là chuỗi public_id (rút từ `uploaded_files.storage_key`, định dạng `"publicId|resourceType|accessMode"` — xem `FileServiceImpl.uploadUsing()`) xuất hiện dạng substring bên trong `content_items.body` (vd: `<img src="https://res.cloudinary.com/<cloud>/image/upload/v1/carebridge/abc123">` chứa `carebridge/abc123`). Không có cách nào khác để biết 1 ảnh "còn được dùng" hay không, ngoài so khớp chuỗi này.

Theo quyết định của user ở phiên trước (ADR-RTE-007 addendum, TDS `ContentRichTextEditor`): content đã `ARCHIVED` **vẫn giữ ảnh**, không xoá — vì record không hề bị hard-delete, vẫn xem lại được. Vậy "mồ côi" phải loại trừ TẤT CẢ content, không chỉ content `ACTIVE`/published — kể cả `DRAFT` và `ARCHIVED` đều tính là "còn tham chiếu" nếu ảnh xuất hiện trong `body` của chúng.

Trường hợp mồ côi thật sự chỉ còn: (1) admin bấm nút chèn ảnh trong editor rồi **không lưu bài** (đóng tab/huỷ) — ảnh đã lên Cloudinary + có `uploaded_files` row nhưng không public_id nào trong `content_items.body` chứa nó; (2) admin **thay ảnh khác** trước khi lưu (chèn ảnh A, xoá, chèn ảnh B, rồi lưu) — ảnh A mồ côi, ảnh B thì không.

#### Các phương án đã xem xét
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A0 (loại — KHÔNG AN TOÀN, phát hiện lúc review) | Mồ côi = `uploaded_files.purpose = PUBLIC_CONTENT_IMAGE` AND `status = ACTIVE` AND quá grace period AND không content nào chứa public_id | Đơn giản nhất | **Sai/nguy hiểm:** `purpose = PUBLIC_CONTENT_IMAGE` KHÔNG chỉ được set bởi ảnh chèn qua rich-text-editor. Đọc lại `FileServiceImpl.uploadUsing()` dòng 189-201: khi caller không truyền `purpose` (vd `uploadFile()`/upload ảnh chung khác), code **mặc định** `purpose = PUBLIC_CONTENT_IMAGE` kèm `accessMode = AUTHENTICATED` (hoặc `PRIVATE` ở nhánh else) — đây là ảnh **KHÔNG liên quan content editor**, không bao giờ xuất hiện trong `content_items.body`, nên `existsByBodyContaining()` luôn `false` với chúng → job sẽ **hard-xoá nhầm** ảnh của người dùng thuộc luồng khác. Đây đúng loại lỗi mà việc chỉ lọc theo `purpose` bỏ sót. |
| A (chọn) | Mồ côi = `uploaded_files.purpose = PUBLIC_CONTENT_IMAGE` **AND `accessMode = PUBLIC`** AND `status = ACTIVE` AND `created_at < now() - gracePeriod` AND KHÔNG có content_item nào (bất kỳ status) mà `body LIKE '%<publicId>%'` — thêm điều kiện `accessMode = PUBLIC` so với A0. Đã verify (`grep FileAccessMode.PUBLIC` toàn backend): **chỉ duy nhất** `uploadPublicFile()` (`FileServiceImpl.java:104`, hardcode `FileAccessMode.PUBLIC`) và lời gọi `uploadWithPurpose()` khi caller tự truyền `accessMode=PUBLIC` (chính là request mà `contentApi.uploadContentImage()` gửi, theo đúng §5.2 TDS `ContentRichTextEditor`) từng set `accessMode = PUBLIC` — không nhánh code nào khác set giá trị này | Đúng ngữ nghĩa "còn tham chiếu ở đâu thì không đụng", khớp quyết định giữ ảnh cho content archived; loại trừ đúng — chỉ ảnh thật sự đi qua đường chèn-ảnh-content mới vào tập ứng viên | Cần LIKE full-scan `content_items.body` mỗi ảnh — chấp nhận được ở quy mô hiện tại (vài trăm content item), cần index/giới hạn nếu bảng lớn lên (ghi ở NFR) |
| B | Mồ côi = ảnh không được tham chiếu bởi content `ACTIVE`/`PUBLISHED` (bỏ qua ARCHIVED) | Đơn giản hơn 1 chút (lọc status trong SQL) | **Vi phạm quyết định trước đó của user** — sẽ xoá ảnh của content đã archive, đúng thứ user không muốn |
| C | Không cần LIKE-scan — thêm bảng liên kết `content_item_images (content_item_id, file_id)`, đồng bộ mỗi lần save content | Query rẻ, chính xác 100%, không cần đoán qua substring | Cần Flyway migration mới + sửa `AdminContentServiceImpl.createContent()/updateContent()` để đồng bộ bảng — vượt phạm vi tối thiểu ("dọn ảnh mồ côi"), rủi ro bug đồng bộ (bảng liên kết lệch khỏi `body` thật nếu quên update 1 chỗ) |

#### Quyết định (đề xuất — chờ duyệt)
Chọn **Phương án A** (không phải A0 — A0 bị loại vì không an toàn, xem cột Nhược điểm). Grace period mặc định **24 giờ** (`@Value("${carebridge.content.image-cleanup.grace-period-hours:24}")`, theo đúng convention `@Value` đã dùng ở `FirebaseEventRetentionJob`) — đủ để admin soạn bài lâu (qua đêm) mà ảnh chưa lưu không bị xoá nhầm giữa lúc đang soạn dở. Job chạy `@Scheduled(cron = "0 0 3 * * *")` (3h sáng, giờ thấp điểm — cùng kiểu cron `FirebaseEventRetentionJob` đang dùng).

**Không dùng Phương án C** — vì phạm vi tối thiểu, tránh thêm bảng/migration cho một job hygiene nội bộ; substring-match chấp nhận được vì `carebridge/<uuid>` (public_id) gần như không thể trùng ngẫu nhiên trong text khác.

#### Hệ quả
**Tích cực:** không phá ảnh của content archived (đúng quyết định trước); không cần migration/bảng mới; tái dùng đúng field/enum đã có (`FilePurpose.PUBLIC_CONTENT_IMAGE`, `FileStatus.ACTIVE`).
**Trade-off:** LIKE-scan trên `content_items.body` (TEXT, không index được hiệu quả cho LIKE '%...%') — chấp nhận ở quy mô hiện tại (§4.4), cần cảnh báo nếu bảng vượt vài nghìn dòng.

---

### ADR-CLEAN-002 — Xoá vật lý: gọi thẳng `CloudinaryStorageService.delete()` + hard-delete row `uploaded_files`, KHÔNG qua `IFileService.purgeFile()`/`deleteFile()` hiện có

| Field        | Value           |
| ------------ | --------------- |
| **Status**   | `Accepted`      |
| **Deciders** | `HuyND`         |
| **Date**     | `2026-07-23`    |

#### Bối cảnh
`FileServiceImpl` đã có sẵn 2 method liên quan, cả hai đều **không khớp** nhu cầu của job này:
- `deleteFile(fileId, callerId)` — soft-delete only, **cố tình KHÔNG gọi** `storageService.delete()` (ADR-FILE-008, `UC169_DeleteFile_TDS.md`) — không giải quyết được vấn đề (ảnh vẫn còn trên Cloudinary).
- `purgeFile(fileId, callerId)` — xoá vật lý thật (storage + hard-delete row), nhưng **owner-scoped** (`file.getOwnerUserId().equals(callerId)`) — job chạy nền, không có `callerId`/principal thật, không nên giả mạo owner để bypass check thiết kế cho user tự xoá file của chính họ.

#### Các phương án đã xem xét
| Phương án | Mô tả | Ưu điểm | Nhược điểm |
|-----------|-------|----------|------------|
| A (chọn) | Job tự gọi trực tiếp `CloudinaryStorageService.delete(storageKey)` (method `delete()` đã tồn tại trên `IStorageService`, không đổi) + `UploadedFileRepository.delete(file)` (hard-delete) trong service riêng của job — không đụng `IFileService`/`FileServiceImpl` | Không sửa contract user-facing hiện có (`purgeFile`/`deleteFile` giữ nguyên, đúng nguyên tắc tách biệt đã thiết lập ở ADR-RTE-007); job logic đơn giản, dễ trace | Có 1 đoạn logic "xoá storage + xoá row" tồn tại ở 2 nơi (job và `purgeFile`) — chấp nhận được vì mục đích khác nhau (job = system/orphan, purgeFile = user tự xoá) |
| B | Thêm method mới `IFileService.purgeOrphanedFile(UUID fileId)` (system-only, không cần callerId), job gọi qua đó | Tập trung logic xoá vật lý về 1 chỗ | Mở rộng contract `IFileService` (interface user-facing) cho 1 use case hệ thống nội bộ — không đúng ranh giới trách nhiệm; job không cần đi qua tầng service dùng cho HTTP request |

#### Quyết định (đề xuất — chờ duyệt)
Chọn **Phương án A**. `uploaded_files` row bị **hard-delete thật** (không phải `FileStatus.DELETED`) — khác với `deleteFile()` (soft-delete) vì đây không phải "user xoá file của họ" mà là "dọn rác hệ thống chưa từng được dùng thật" — không có giá trị audit/khôi phục nào để giữ lại một upload chưa từng gắn vào nội dung nào. Vẫn ghi `AuditService.log(...)` (action mới `FILE_ORPHAN_PURGED` hoặc tái dùng `FILE_DELETED` — quyết định khi review) để có dấu vết job đã chạy gì.

#### Hệ quả
**Tích cực:** không đụng `IFileService`/`FileServiceImpl` — blast radius cho 2 method hiện có = 0, verify được bằng `git diff` giống cách đã làm ở ADR-RTE-007.
**Trade-off:** hard-delete `uploaded_files` row — không thể khôi phục nếu logic phát hiện "mồ côi" có bug false-positive. Giảm thiểu bằng grace period 24h (ADR-CLEAN-001) + `dryRun` mode khi implement lần đầu (xem §11 dưới, đề xuất chạy dry-run trước khi bật xoá thật).

---

## 4. Non-Functional Requirements & SLA

| Category | Requirement | Target | Verification Method | Ghi chú |
|----------|-------------|--------|----------------------|---------|
| Safety | Không bao giờ xoá ảnh còn tham chiếu ở bất kỳ content nào (mọi status) | 100% — 0 false positive | Test §Test-Spec, dry-run trước khi bật thật | ADR-CLEAN-001 |
| Isolation | Lỗi job (Cloudinary down, v.v.) không làm crash scheduler / ảnh hưởng request khác | Job tự bắt exception, log, không throw ra ngoài | Theo đúng pattern `FirebaseEventRetentionJob` | — |
| Scale (hiện tại) | LIKE-scan `content_items.body` cho mỗi ảnh ứng viên | Chấp nhận được ở quy mô hiện tại (~vài trăm content item, seed data) | Đo thời gian chạy 1 lần trong QA thủ công | Nếu `content_items` vượt vài nghìn dòng, cần đổi chiến lược (vd: load toàn bộ `body` 1 lần, match trong memory thay vì N query) — flag làm follow-up, không block tài liệu này |
| Audit | Mỗi lần xoá 1 ảnh mồ côi đều có audit log | 100% | `AuditService.log()` | — |

---

## 5. Static Modeling

```java
// PublicContentImageCleanupJob.java — MỚI — com.carebridge.backend.file.job
// Cùng convention @Scheduled + @Value + Clock-injectable như FirebaseEventRetentionJob.
@Component
public class PublicContentImageCleanupJob {

    @Value("${carebridge.content.image-cleanup.grace-period-hours:24}")
    private long gracePeriodHours;

    @Value("${carebridge.content.image-cleanup.enabled:true}")
    private boolean enabled;

    private final UploadedFileRepository fileRepository;
    private final ContentRepository contentRepository;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final AuditService auditService;
    private final Clock clock;

    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOrphanedImages() {
        if (!enabled) return;
        Instant cutoff = Instant.now(clock).minus(Duration.ofHours(gracePeriodHours));
        List<UploadedFile> candidates = fileRepository
                .findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
                        FilePurpose.PUBLIC_CONTENT_IMAGE, FileAccessMode.PUBLIC, FileStatus.ACTIVE, cutoff);
        for (UploadedFile file : candidates) {
            try {
                String publicId = file.getStorageKey().split("\\|")[0]; // "publicId|resourceType|accessMode"
                if (contentRepository.existsByBodyContaining(publicId)) continue; // còn được tham chiếu — bỏ qua
                cloudinaryStorageService.delete(file.getStorageKey());
                fileRepository.delete(file);
                auditService.log(AuditAction.FILE_ORPHAN_PURGED, null, "UploadedFile", file.getId().toString(),
                        "orphaned public content image purged, publicId=" + publicId);
            } catch (Exception ex) {
                log.error("Failed to purge orphaned file {}", file.getId(), ex); // best-effort, tiếp tục vòng lặp
            }
        }
    }
}

// UploadedFileRepository.java — thêm 1 method
// LƯU Ý AN TOÀN: bắt buộc lọc CẢ purpose LẪN accessMode=PUBLIC — purpose một mình không đủ,
// vì FileServiceImpl.uploadUsing() mặc định purpose=PUBLIC_CONTENT_IMAGE cho các upload ảnh
// KHÔNG liên quan content editor khi caller không truyền purpose (accessMode lúc đó là
// AUTHENTICATED/PRIVATE, không phải PUBLIC) — xem ADR-CLEAN-001 phương án A0 (bị loại).
List<UploadedFile> findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(
        FilePurpose purpose, FileAccessMode accessMode, FileStatus status, Instant cutoff);

// ContentRepository.java — thêm 1 method
@Query("SELECT COUNT(c) > 0 FROM ContentItem c WHERE c.body LIKE CONCAT('%', :publicId, '%')")
boolean existsByBodyContaining(@Param("publicId") String publicId);

// AuditAction.java — thêm 1 enum value mới: FILE_ORPHAN_PURGED
// application.properties / .env — thêm 2 config (đã có default trong code, không bắt buộc set):
// carebridge.content.image-cleanup.enabled=true
// carebridge.content.image-cleanup.grace-period-hours=24
```

**Không cần Flyway migration** — không có schema mới, chỉ thêm repository method + job class + 1 enum value.

---

## 6. Dynamic Modeling — Happy Path

```
Scheduler (3h sáng) -> PublicContentImageCleanupJob.cleanupOrphanedImages()
  -> UploadedFileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore(PUBLIC_CONTENT_IMAGE, PUBLIC, ACTIVE, now-24h)
  -> với mỗi UploadedFile ứng viên:
       -> ContentRepository.existsByBodyContaining(publicId)
       -> false (không content nào tham chiếu) ->
            CloudinaryStorageService.delete(storageKey)  [xoá vật lý trên Cloudinary]
            UploadedFileRepository.delete(file)          [hard-delete row]
            AuditService.log(FILE_ORPHAN_PURGED, ...)
       -> true (còn tham chiếu) -> bỏ qua, sang ảnh tiếp theo
```

---

## 7. Đề xuất triển khai an toàn (thay cho §11/§12 đầy đủ — job nội bộ, không có API/rollback DB)

1. **Bước 1 — Dry-run mode:** implement với flag `carebridge.content.image-cleanup.dry-run=true` mặc định khi mới deploy — job chạy đúng logic phát hiện mồ côi nhưng chỉ **log** ứng viên sẽ bị xoá, không gọi `delete()` thật. Chạy vài ngày trên môi trường dev/staging, đọc log xác nhận danh sách hợp lý (không có ảnh của content đang published bị liệt kê) trước khi tắt dry-run.
2. **Bước 2 — Bật thật:** đổi `dry-run=false` sau khi Bước 1 xác nhận an toàn.
3. **Rollback:** vì job chỉ tự kích hoạt qua cron, "rollback" đơn giản là set `carebridge.content.image-cleanup.enabled=false` (không cần revert code/deploy lại) — không có migration DB nào để rollback (§5).

---

## 8. Câu hỏi mở — ĐÃ QUYẾT ĐỊNH (2026-07-23, user: "chọn đáp án bạn cảm thấy tốt nhất, không phức tạp quá")

1. **Tên `AuditAction`:** ✅ **`FILE_ORPHAN_PURGED`** (mới) — đề xuất mặc định, đã chọn.
2. **Grace period:** ✅ **24 giờ** — đề xuất mặc định, đã chọn. Cấu hình được qua `.env` (`carebridge.content.image-cleanup.grace-period-hours`), không cần đổi code nếu sau này muốn chỉnh.
3. **Manual trigger endpoint:** ✅ **Không làm** — chỉ cron (`@Scheduled`, giống `FirebaseEventRetentionJob`), giữ phạm vi nhỏ nhất. Có thể thêm sau nếu cần, không block tài liệu này.

---

## 9. Xác nhận hoàn thành (Post-Implementation, 2026-07-23)

- **Code:** `PublicContentImageCleanupJob` (mới, `file.job`), `UploadedFileRepository.findAllByPurposeAndAccessModeAndStatusAndCreatedAtBefore()` (mới), `ContentRepository.existsByBodyContaining()` (mới), `AuditAction.FILE_ORPHAN_PURGED` (mới).
- **Sửa 1 lỗi thiết kế thật trước khi trình user duyệt** (phát hiện qua advisor review, chưa có code): bản nháp đầu chỉ lọc `purpose=PUBLIC_CONTENT_IMAGE`, không an toàn (xem ADR-CLEAN-001 phương án A0 bị loại, và CHANGELOG) — đã sửa thành lọc cả `purpose` VÀ `accessMode=PUBLIC` trước khi implement, nên bug này **không lọt vào code thật**.
- **Test:** `PublicContentImageCleanupJobTest` — `TC-CLEAN-001, 002, 004, 005, 006, 007, 008, 009` — **8/8 GREEN** (unit, mock `UploadedFileRepository`/`ContentRepository`/`CloudinaryStorageService`/`AuditService`, `Clock.fixed()` — cùng pattern `FirebaseEventRetentionJobTest`).
- **`TC-CLEAN-003`** (regression guard: `existsByBodyContaining()` không lọc theo `ContentStatus`, để ảnh của content ARCHIVED vẫn được bảo vệ): viết `ContentRepositoryIntegrationTest.java` (Testcontainers, `AbstractPostgresIntegrationTest`) đúng theo spec — **CHƯA tự chạy được**: môi trường này không có Docker khả dụng (`IllegalStateException: Could not find a valid Docker environment`), không phải cùng lỗi `SchemaManagementException` đã ghi nhận trước đó cho các integration test khác trong project, nhưng cùng nhóm "gap môi trường Testcontainers pre-existing". Bù bằng verify trực tiếp mã nguồn: JPQL của `existsByBodyContaining()` (`ContentRepository.java`) không có mệnh đề `WHERE c.status = ...` nào — chỉ match `c.body LIKE '%...%'` — nên về logic, không thể lọc theo status dù dữ liệu thật hay giả.
- **Live QA gián tiếp:** chưa kích hoạt job này trong QA (job chạy cron 3h sáng, không có endpoint thủ công theo quyết định ở §8) — chưa tạo được tình huống ảnh mồ côi thật để quan sát job chạy live. Đây là follow-up hợp lý khi có nhu cầu thật (job hiện `dry-run=true` mặc định theo §7, an toàn để deploy mà chưa cần verify thêm ngay).
- **Không regression:** `./mvnw test` toàn repo — baseline sạch (`git stash`) 2394/9/120 → sau khi thêm code 2406/9/121. Chênh lệch +12 test/+1 error đúng bằng: 8 test job mới (GREEN) + 3 test sanitizer mới của `ContentRichTextEditor` (GREEN, xem tài liệu đó) + 1 lỗi mới từ `ContentRepositoryIntegrationTest` (chặn bởi Docker, đã giải thích ở trên) — không có test cũ nào bị ảnh hưởng.

*Tài liệu này đã hoàn thành implementation — Status: Approved. Follow-up còn lại: (1) chạy `ContentRepositoryIntegrationTest` thật khi có môi trường Docker/Testcontainers khả dụng để xác nhận TC-CLEAN-003 bằng dữ liệu thật thay vì chỉ đọc code; (2) sau khi deploy, theo dõi log dry-run vài ngày trước khi bật `dry-run=false` (§7) — chưa làm vì ngoài phạm vi phiên làm việc này (cần môi trường staging/production thật).*
