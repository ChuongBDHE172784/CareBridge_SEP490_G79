# CareBridge — Test status snapshot

**Đo ngày** 2026-08-09 · **Commit** `a92f502a` (branch `ChuongBD`, đã merge `dev` qua `d7fa7abf`)
**Mục đích** Đầu vào cho việc viết Integration Test. Mọi số liệu dưới đây là kết quả chạy thật trên máy local, không phải ước lượng.

> ⚠️ Snapshot này được đo với **một số thay đổi chưa commit** trong working tree (xem §6). Nếu bạn checkout sạch từ `a92f502a`, kết quả sẽ **xấu hơn** đáng kể — cụ thể là 135 test Python và 4 test class Java sẽ đỏ vì lỗi digest ở §6.1.

---

## 1. Tổng quan theo stack

| Stack | Lệnh chạy | Kết quả | Thư mục test |
|---|---|---|---|
| Backend (Java 21 / Spring Boot) | `./mvnw test` | **4157 test — 13 failures, 15 errors, 30 skipped** | `05_Development/CareBridgeAPI/src/test/java/` |
| Mobile (Flutter) | `flutter test` | **939 pass, 0 fail** | `05_Development/CareBridgeMobileApp/test/` |
| Mobile — static analysis | `flutter analyze` | **No issues found** | — |
| Web (React + Vite) | `npx vitest run` | **137 pass, 0 fail** (31 file) | `05_Development/CareBridgeWebApp/src/**/*.test.tsx` |
| Web — typecheck / lint | `npx tsc -b --noEmit` · `npx eslint .` | sạch / sạch | — |
| AI Triage (Python / FastAPI) | `python -m pytest` | **1013 pass, 0 fail** | `05_Development/CareBridgeAITriageService/tests/` |

Số lượng file test hiện có:

| Stack | Số file | Ghi chú |
|---|---|---|
| Backend | 672 class `*Test.java` | trong đó **140** là integration/Postgres/migration |
| Flutter | 159 file `*_test.dart` | + 3 file trong `integration_test/` |
| Web | 31 file `*.test.tsx` | + 5 spec Playwright trong `e2e/` |
| Python | 49 file `test_*.py` | |

---

## 2. Điều kiện tiên quyết để chạy Integration Test backend

Đây là phần quan trọng nhất khi viết test mới — bỏ qua là test sẽ đỏ vì môi trường chứ không phải vì code.

### 2.1 Docker phải chạy
Toàn bộ test kế thừa `AbstractPostgresIntegrationTest` dùng Testcontainers. Docker tắt ⇒ ~128 error kiểu
`NoClassDefFoundError: Could not initialize class ...AbstractPostgresIntegrationTest`.

```bash
docker ps
```

### 2.2 Bốn role database phải tồn tại TRƯỚC khi Flyway chạy
`V20260731070000__canonical_post_20260719180000_schema.sql` (dòng 3095–3157) khẳng định 4 role tồn tại với **thuộc tính chính xác**, nếu không thì raise `42501`:

| Role | LOGIN | Bắt buộc thêm |
|---|---|---|
| `carebridge_checklist_schema_owner` | NOLOGIN | `NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT NOREPLICATION NOBYPASSRLS` |
| `carebridge_checklist_retention_owner` | NOLOGIN | như trên |
| `checklist_operations` | LOGIN | như trên |
| `carebridge_application` | LOGIN | như trên |

**`NOINHERIT` không phải mặc định của PostgreSQL** — role tạo tay mà thiếu nó vẫn fail migration.
Migration cũng từ chối nếu Flyway kết nối *bằng chính* một trong 4 role đó (`CHECKLIST_FLYWAY_ROLE_MUST_BE_SEPARATE`).

Trong test, provision bằng helper có sẵn:

```java
// cùng package com.carebridge.backend.testsupport nên không cần import khi ở trong đó
EmbeddedPostgresRoleFixture.provision(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
```

- Nếu kế thừa `AbstractPostgresIntegrationTest` → **đã provision sẵn**, không phải làm gì.
- Nếu tự tạo `@Container PostgreSQLContainer` → **phải tự gọi** trong `@BeforeEach` (container instance) hoặc `static {}` (container static), **trước** khi Flyway chạy.

Trên Supabase / môi trường thật, chạy một lần:
```bash
psql "$SUPABASE_DB_URL" -v ON_ERROR_STOP=1 -f 05_Development/Deployment/database/00_provision_checklist_roles.sql
```

### 2.3 Hạ tầng test có sẵn

| File | Vai trò |
|---|---|
| `src/test/java/com/carebridge/backend/testsupport/AbstractPostgresIntegrationTest.java` | Base cho integration test dùng Testcontainers (singleton container, Flyway thật, `ddl-auto=validate`). **Ưu tiên kế thừa class này.** |
| `src/test/java/com/carebridge/backend/testsupport/AbstractEmbeddedPostgresIntegrationTest.java` | Base cho embedded Postgres (zonky) |
| `src/test/java/com/carebridge/backend/testsupport/EmbeddedPostgresRoleFixture.java` | Tạo 4 role bắt buộc (idempotent) |
| `src/test/java/com/carebridge/backend/testsupport/CanonicalUserFixture.java` | Seed user chuẩn |
| `src/test/java/com/carebridge/backend/testsupport/CanonicalAuditFixture.java` | Seed audit chuẩn |
| `src/test/java/com/carebridge/backend/testsupport/MigrationLocator.java` | Định vị file migration |
| `src/test/java/com/carebridge/backend/testsupport/DatabaseGate0Support.java` | Kiểm tra chuỗi migration (Gate 0) |
| `src/test/resources/application.yaml` | Mặc định H2, Flyway tắt, `create-drop` — base class ở trên override sang Postgres |

### 2.4 Ràng buộc khi viết test mới
- **Không sửa file migration đã apply.** DDL cần quyền owner phải đi qua finalizer riêng — xem `05_Development/Deployment/database/CHECKLIST_RETENTION_SECURITY_RUNBOOK.md`.
- Unit test mock repository **không phát hiện được** vi phạm NOT NULL / FK. Feature động tới schema cần ít nhất một test ghi row thật (bài học đã ghi trong `lessons.md`).
- `AbstractPostgresIntegrationTest` set `carebridge.dev-seed.enabled=false`; mỗi class tự seed dữ liệu của mình.
- Hikari pool giới hạn 4 connection/context vì mọi Spring context dùng chung một container.

---

## 3. Backend — 28 vấn đề còn lại (13 fail + 15 error)

Tất cả đều nằm trong mảng **database / migration / checklist**. Nhóm theo nguyên nhân gốc.

### 3.1 Migration version không tồn tại (4 test)

Test trỏ tới version đã bị xoá hoặc đổi tên trong `src/main/resources/db/migration/`.

| Test | Path | Lỗi |
|---|---|---|
| `CommunityTopicIntegrationTest.hierarchyInversion_preservesFollowRowsAndExistingTopicIds` | `src/test/java/com/carebridge/backend/community/CommunityTopicIntegrationTest.java:224` | `Flyway: No migration with a target version 20260727010000` |
| `JourneyCanonicalLifecycleIntegrationTest.jrnTcInt004_migrationCreatesSchemaAndHistoryIsReadable` | `src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java:249` | `Flyway: No migration with a target version 20260727010000` |
| `ContentStageConsolidationMigrationTest.v4ConsolidatesStagesTopicsContentAndDuplicateInteractions` | `src/test/java/com/carebridge/backend/content/integration/ContentStageConsolidationMigrationTest.java:27` | `Flyway: No migration with a target version 3` |

Thư mục migration hiện có 46 file; version cao nhất là `V20260808100000`, và chỉ có `V1__`, `V2__` (không có `V3__`, `V4__`).

### 3.2 Embedded Postgres thiếu bảng (5 test)

Chuỗi migration trên embedded Postgres không tạo được các bảng này ⇒ `BadSqlGrammar`.

| Test | Path | Bảng thiếu |
|---|---|---|
| `ChecklistImportPostgresIntegrationTest.defaultFlagOffStillReturnsGoneAndLeavesLegacyTableUnchanged` | `src/test/java/com/carebridge/backend/checklist/ChecklistImportPostgresIntegrationTest.java:25` | `preparation_checklist_items` |
| `ChecklistTemplateTypeEmbeddedPostgresTest` (2 test) | `src/test/java/com/carebridge/backend/checklist/ChecklistTemplateTypeEmbeddedPostgresTest.java:29,79` | `care_item_templates` |
| `ChecklistPersonalScopeMigrationEmbeddedPostgresTest.tearDown` (3 lần) | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistPersonalScopeMigrationEmbeddedPostgresTest.java:66` | `checklist_care_group_contexts` |
| `ReminderJourneyNullCareSubjectPostgresTest.cleanFixture` | `src/test/java/com/carebridge/backend/reminder/entity/ReminderJourneyNullCareSubjectPostgresTest.java:76` | `checklist_context_authorities` |

### 3.3 Finalizer chưa chạy — `CHECKLIST_RETENTION_PRIVILEGED_FUNCTION_INTEGRITY_FAILED` (2 test)

| Test | Path |
|---|---|
| `ChecklistRetirementLiveUpgradeEmbeddedPostgresTest.finalizedLiveUpgradePreservesCoreDataAndActionLedgerWhileRetiringSupportCatalog` | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistRetirementLiveUpgradeEmbeddedPostgresTest.java:99` |
| `ChecklistRollForwardEmbeddedPostgresTest.appliesOnlyTheOptInRollForwardAfterTheExistingHistory` | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistRollForwardEmbeddedPostgresTest.java:54` |

Cần chạy `V20260729150001__finalize_checklist_retention_security.sql` qua `Invoke-ChecklistRetentionFinalizer.ps1`.

### 3.4 Checksum / số lượng bị pin lệch (4 test)

| Test | Path | Kỳ vọng → thực tế |
|---|---|---|
| `Postgresql18CanonicalSchemaIntegrationTest.cleanBootstrapKeepsCanonicalTableCountAndPassesHibernateValidation` | `src/test/java/com/carebridge/backend/migration/Postgresql18CanonicalSchemaIntegrationTest.java:62` | **57 → 63 bảng** |
| `ChecklistTemplateMigrationTest.uc82_69_int_005_appliedMigrationRemainsByteIdentical` | `src/test/java/com/carebridge/backend/content/integration/ChecklistTemplateMigrationTest.java:29` | SHA `CA629ACE…` → `29D413A5…` |
| `ChecklistRetentionDeploymentRunnerContractTest.runnerPinsChecksumStopsOnPsqlFailureAndVerifiesAfterCommit` | `src/test/java/com/carebridge/backend/checklist/operations/ChecklistRetentionDeploymentRunnerContractTest.java:28` | SHA `fc3fe025…` → `4e1e2e14…` |
| `LifecycleContentPostgresIntegrationTest.uc82_69_int_005_realPostgresPersistsFiveStatusesAndDeterministicCardinality` | `src/test/java/com/carebridge/backend/content/integration/LifecycleContentPostgresIntegrationTest.java:197` | `1L → 0L` |

> ⚠️ `Postgresql18CanonicalSchemaIntegrationTest` **mới lộ ra hôm nay**: trước đây nó chết sớm ở lỗi role nên không ai thấy schema đã thêm 6 bảng. Con số 57 là canary — **không tự sửa thành 63**, cần DB owner xác nhận 6 bảng mới là hợp lệ.

### 3.5 Guard/constraint không kích hoạt (4 test)

| Test | Path | Mong đợi |
|---|---|---|
| `JourneyCanonicalLifecycleIntegrationTest.jrnTcInt005_uniqueIndexRejectsDuplicateCanonicalActiveOwner` | `src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java:277` | unique index `uq_mother_journeys_one_canonical_active` phải chặn — thực tế không có root cause |
| `JourneyCanonicalLifecycleIntegrationTest.transitionHistoryRejectsDirectUpdateAndDelete` | `src/test/java/com/carebridge/backend/journey/JourneyCanonicalLifecycleIntegrationTest.java:342` | trigger `IMMUTABLE_TABLE: public.audit_events` phải chặn UPDATE/DELETE |
| `ChecklistBusinessAuditAtomicityPostgresTest.requiredAuditFailureRollsBackBusinessStateAndEveryPriorAudit` (3 lần) | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistBusinessAuditAtomicityPostgresTest.java:218` | `Expecting code to raise a throwable` |
| `ChecklistPersonalScopeMigrationEmbeddedPostgresTest.personalInstanceCannotSpoofCanonicalContextOwner` | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistPersonalScopeMigrationEmbeddedPostgresTest.java:101` | thông điệp exception khác kỳ vọng |

### 3.6 Lỗi lẻ (5 test)

| Test | Path | Lỗi |
|---|---|---|
| `DirectChatIntegrationTest.fullLifecycle_findOrCreate_sendMessage_timeline_callLifecycle` | `src/test/java/com/carebridge/backend/directchat/integration/DirectChatIntegrationTest.java:87` | HTTP **expected 201 but was 404** |
| `PostpartumLogPostgresIntegrationTest.listLogs_equalDateAndTimestamp_usesIdAsStablePostgresPageBoundary` | `src/test/java/com/carebridge/backend/health/PostpartumLogPostgresIntegrationTest.java:148` | `NullPointerException: Cannot invoke "java.lang.Short.shortValue()"` |
| `HealthObservationRepositoryIntegrationTest.familySummaryQueriesUseCanonicalActiveSubjectScopedObservationsAndInclusiveBounds` | `src/test/java/com/carebridge/backend/health/repository/HealthObservationRepositoryIntegrationTest.java:42` | `UnsupportedOperationException` |
| `EmergencyTriageLinkPostgresIntegrationTest.retryCandidateQueryUsesStableCreatedAtOrderingAndLimitFifty` | `src/test/java/com/carebridge/backend/emergency/EmergencyTriageLinkPostgresIntegrationTest.java:160` | `DataIntegrityViolation` khi `INSERT INTO safety_events` |
| `ChecklistAuthorizationAndTodayApiEmbeddedPostgresTest.acceptedMembershipRemainsAuthorizedAfterInvitationExpiry` | `src/test/java/com/carebridge/backend/checklist/today/ChecklistAuthorizationAndTodayApiEmbeddedPostgresTest.java:420` | `Expecting size ≥ 3 but was 0` |

### 3.7 Thiếu file ngoài source tree (1 test)

| Test | Path | File thiếu |
|---|---|---|
| `ChecklistAuditQueriesEmbeddedPostgresTest.completeAuditExecutesAgainstCurrentPostgresSchema` | `src/test/java/com/carebridge/backend/checklist/distribution/ChecklistAuditQueriesEmbeddedPostgresTest.java:33` | `_bmad-output/planning-artifacts/architecture/architecture-CareBridge_SEP490_G79-2026-07-31/AUDIT-QUERIES.sql` |

### 3.8 Test đang bị skip (30 test — vùng chưa có coverage)

Đây là những chỗ **đáng viết Integration Test nhất** vì hiện đang bỏ trống:

| Số test | Class |
|---|---|
| 7 | `security.CanonicalRoleMigrationIntegrationTest` |
| 6 | `safety.Mf14CanonicalPersistencePostgresTest` |
| 3 | `notification.NotificationCanonicalMigrationIntegrationTest` |
| 3 | `safety.SafetyPersistenceMigrationIntegrationTest` |
| 2 | `checklist.distribution.ChecklistMigrationPerformanceEmbeddedPostgresTest` |
| 2 | `checklist.distribution.ChecklistSyntheticFixtureEmbeddedPostgresTest` |
| 2 | `security.CanonicalRoleSchemaIntegrationTest` |
| 1 | `checklist.distribution.ChecklistDisposableLiveApiHostTest` |
| 1 | `checklist.distribution.ChecklistPerformanceEmbeddedPostgresTest` |
| 1 | `content.integration.ContentBodySanitizeIntegrationTest` |
| 1 | `notification.NotificationCanonicalSchemaIntegrationTest` |
| 1 | `testsupport.PrintSupabaseTablesTest` |

---

## 4. Mobile / Web / Python — không còn lỗi

| Stack | Chi tiết |
|---|---|
| Flutter | 939/939 pass. `flutter analyze`: 0 issue. Golden test được gắn tag `golden`, khai báo tại `05_Development/CareBridgeMobileApp/dart_test.yaml` — chạy `flutter test --exclude-tags golden` nếu máy không phải "golden authority". |
| Web | 137/137 pass, 31 file. `tsc` và `eslint` sạch. |
| Python | 1013/1013 pass, 49 file. |

### Điểm cần biết khi viết widget/integration test Flutter
`find.byType(T)` khớp **đúng runtime type**. Các factory như `ElevatedButton.icon` trả về subclass private (`_ElevatedButtonWithIcon extends ElevatedButton`) nên `find.byType(ElevatedButton)` trả về **0 kết quả**. Dùng:

```dart
find.byWidgetPredicate((widget) => widget is ElevatedButton)
```

Đây chính là nguyên nhân 2 test đã fail và vừa được sửa (xem §6.2).

---

## 5. Lệnh chạy

```bash
# Backend — cần Docker chạy
cd 05_Development/CareBridgeAPI && ./mvnw test

# Backend — một class
cd 05_Development/CareBridgeAPI && ./mvnw test -Dtest=DirectChatIntegrationTest

# Mobile
cd 05_Development/CareBridgeMobileApp && flutter test

# Mobile — bỏ qua golden
cd 05_Development/CareBridgeMobileApp && flutter test --exclude-tags golden

# Web
cd 05_Development/CareBridgeWebApp && npx vitest run

# AI Triage
cd 05_Development/CareBridgeAITriageService && ./.venv/Scripts/python.exe -m pytest -q

# Kiểm tra digest rule registry (phải in "OK")
python 05_Development/DevTools/sync_triage_rule_registry.py --check
```

---

## 6. Thay đổi chưa commit tại thời điểm đo

### 6.1 Bug thật, chưa có trên bất kỳ nhánh nào — digest sidecar lệch

8 file `.sha256` + `artifact_integrity_manifest.json` ghi digest của **bản CRLF**, trong khi file trên đĩa là LF.

- Ở `HEAD`: sidecar ghi `7cb7ae3b…`, file thật hash ra `dd230a92…`
- Hậu quả nếu không sửa: **85 fail + 50 error** ở Python, và 4 class Java (`ParityResultFingerprintTest`, `TriageRuleParityV2Test`, `TriageV2ReadinessTest`, `ZeroTrustCalculatorTest`) đỏ; runtime thì Triage V2 fail-closed với `RULESET_HASH_MISMATCH`.
- Cách sửa: `python 05_Development/DevTools/sync_triage_rule_registry.py` (chỉ đổi digest, **không** đổi nội dung rule).

### 6.2 Các fix đã áp dụng để có snapshot này

| Nhóm | File |
|---|---|
| Provision role cho 4 test tự tạo container | `src/test/java/com/carebridge/backend/testsupport/DatabaseGate0IntegrationTest.java`, `src/test/java/com/carebridge/backend/testsupport/HermeticDatasourceTestcontainersSmokeTest.java`, `src/test/java/com/carebridge/backend/migration/Postgresql18CanonicalSchemaIntegrationTest.java`, `src/test/java/com/carebridge/backend/baby/BabyJourneyLinkageRemovalMigrationPostgresTest.java` |
| Sửa `find.byType(ElevatedButton)` | `05_Development/CareBridgeMobileApp/test/features/checklist/checklist_detail_screen_test.dart`, `05_Development/CareBridgeMobileApp/test/features/community/view_content_lifecycle_screen_test.dart` |
| Khôi phục golden + khai báo tag | `05_Development/CareBridgeMobileApp/test/features/aiTriage/goldens/*.png`, `05_Development/CareBridgeMobileApp/dart_test.yaml` |
| Script provision role Supabase | `05_Development/Deployment/database/00_provision_checklist_roles.sql` (mới), `05_Development/Deployment/database/CHECKLIST_RETENTION_SECURITY_RUNBOOK.md` (thêm Step 0) |
| Sửa 40 Dart analyze + 3 ESLint | 19 file `CareBridgeMobileApp/lib/`, 3 file `CareBridgeWebApp/src/` |

Kết quả của riêng nhóm role provisioning: `CHECKLIST_RETENTION_OWNER_ROLE_REQUIRED` từ **32 lần → 0 lần**.
