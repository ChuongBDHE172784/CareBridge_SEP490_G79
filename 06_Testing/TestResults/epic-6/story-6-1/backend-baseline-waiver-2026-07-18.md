# Story 6.1 Backend Baseline Waiver

## Decision

The failing full-backend baseline is **waived only for the Story 6.1 review gate**. This waiver does not declare the repository-wide backend suite green and does not waive any Journey failure.

- Command: `.\mvnw.cmd test`
- Executed: 2026-07-18 (Asia/Saigon)
- Result: 2,238 tests; 1 failure; 109 errors; 1 skipped; Maven build failed.
- Story isolation: **0 of 110 failed/error test cases are in `com.carebridge.backend.journey`**.
- Re-evaluated after the approved review patches on 2026-07-18.
- Story evidence: the focused Journey/Story 6.1 coverage run passed **45/45**, including all PostgreSQL/Flyway integration cases; changed service/policy line coverage is **90.83%**.

## Failure inventory

| Baseline family | Count | Evidence/root signature | Story 6.1 relationship |
| --- | ---: | --- | --- |
| Spring context bootstrap | 95 errors across 35 non-Journey suites | Representative root cause: unresolved `${carebridge.zego.app-id}`; subsequent cases are suppressed by Spring's ApplicationContext failure threshold | Zego/direct-call configuration and the listed security/content/family/etc. suites are outside the Story 6.1 Journey change set |
| Content mapping | 6 errors | `ContentMapperTest`: `ContentItem.getSources()` is null | `content` package, outside Journey |
| Notification view | 3 errors | `NotificationViewServiceTest`: mocked repository page is null | `notification` package, outside Journey |
| Account moderation | 3 errors | `WarnOrSuspendAccountServiceImplTest`: future `expiresAt` required | `moderation` package, outside Journey |
| Question moderation | 2 errors | lock requires an APPROVED question | `moderation` package, outside Journey |
| File upload | 1 failure | `FileServiceImplTest.uploadFile_presignedUrlTtlIs15Minutes`: presigned URL generated twice, expected once | `file` package, outside Journey |

The 95 context errors occur in these non-Journey suites/packages: application smoke, `carejourney`, `community`, `content`, `exercise`, `family`, `identity`, `integration`, `profile`, `search`, `security`, `testsupport`, and `triage`. Surefire XML under `05_Development/CareBridgeAPI/target/surefire-reports/` is the machine-readable source for the inventory.

## Waiver controls

- Scope: Story `6-1-establish-canonical-mother-lifecycle-and-transition-history` only.
- Owner: backend baseline maintainers for the affected packages/configuration.
- Expiry: the waiver must be re-evaluated on the next Story 6.1 review rerun or when any Journey test fails, whichever occurs first.
- Release condition: repository-wide release/merge gates must continue to report the backend suite as red until these baseline failures are fixed or separately approved at release level.
- Automatic invalidation: any failure in `com.carebridge.backend.journey`, the Story 6.1 migration, or a Story-scoped contract test invalidates this waiver.

## Rationale

The failed test classes and root signatures do not exercise or reference the Story 6.1 Journey package, while the focused Journey suites are green. The waiver therefore isolates known repository baseline debt without hiding Story-specific regression risk. This disposition is sufficient for the Story review gate only; it does not make the repository-wide release gate green.
