# Checklist Migration Production-like Rehearsal Runbook

## Existing Supabase history (`20260731020000`)

Run the approved privileged pre-finalizer first, then start the backend with the opt-in profile so Flyway scans only the compatibility location:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'supabase-roll-forward'
& "$HOME\.m2\wrapper\dists\apache-maven-3.9.16\56ba1f9f\bin\mvn.cmd" spring-boot:run
```

The expected Flyway result is one migration, `20260731070000`, followed by a no-op replay. Do not start this profile against a clean database.

> **Canonical migration note (2026-07-31):** The active clean-bootstrap Flyway location contains one post-baseline migration, `V20260731060000__canonical_post_20260719180000_schema.sql`. The CHK-041 `psql` verifier below remains a historical staged-chain rehearsal and therefore uses the test-only `db/migration-legacy` fixtures through `20260730050000`; its post-query intentionally reads the pre-retirement support catalog. Do not point that verifier at the canonical final schema, where the nine support tables are retired atomically. For an existing database at `20260731020000`, start the backend with profile `supabase-roll-forward`; it scans only `db/migration-roll-forward` and applies `V20260731070000` after the approved pre-finalizer. Canonical clean bootstrap/replay/retirement evidence is provided by `ChecklistFlywayEmbeddedPostgresTest` and the PostgreSQL 18 canonical integration gate.

> **Approved local reference decision — 2026-07-30:** the owner approved a deterministic synthetic production-representative fixture on a local disposable PostgreSQL 18 cluster in place of the unavailable sanitized external backup/host. All other controls in this runbook remain mandatory and unchanged. The final evidence must bind the sealed fixture and exact local host profile, and the result makes no capacity claim beyond that profile.

## Purpose and release rule

This runbook produces the missing CHK-041 count/hash/quarantine, abort, and forward-only recovery evidence on a disposable production-representative PostgreSQL clone. The verifier is deliberately read-only: it does not run Flyway, change a feature flag, enable a cohort, repair data, or delete quarantine records.


Release remains blocked unless one evidence set proves all of the following on the approved reference dataset and environment:

- blocking lock duration is at most 5 seconds;
- legacy backfill throughput is at least 500 rows/second;
- full migration duration is at most 1,800 seconds;
- the source count and SHA-256 remain identical;
- the normalized eligible destination count and SHA-256 match expected values;
- every legacy source has either a matching target or an unresolved quarantine outcome;
- unresolved quarantine count and legacy-source quarantine rate do not exceed the reviewed fixture baseline;
- an intentional failed gate produces `ABORT_PROVEN` before cohort enablement;
- a later corrected, forward-only run references that abort and produces `ROLL_FORWARD_PASS`.

Local embedded-PostgreSQL results are useful preflight evidence but do not satisfy this production-like gate.

## Safety prerequisites

1. Restore the sanitized production-representative backup into a disposable database. Never point this workflow at the live production primary.
2. Connect the verifier as the existing `checklist_operations` database login. The quarantine table uses forced RLS and its full-visibility SELECT policy is assigned to this role; a generic role with only table-level `GRANT SELECT` can see zero rows and create a false pass. On the disposable clone only, grant `checklist_operations` the additional read privileges needed for `flyway_schema_history`, `preparation_checklist_items`, `care_item_templates`, `checklist_instances`, and `checklist_task_instances`. Do not create a replacement policy or bypass RLS. The script fails closed unless the current role, SELECT privilege, named RLS policy, and forced-RLS flags all attest correctly.
3. Store authentication in a protected `PGPASSFILE` or libpq service. Do not put a password in the database URL, command line, metrics file, or artifact directory.
4. Keep `checklist_distribution_v2` and all rollout cohorts disabled until the final verdict is independently reviewed.
5. Record the backup identifier, sanitized dataset fingerprint, PostgreSQL version, application commit, Flyway checksums, and operator/change reference outside the database.

Required environment variables:

```powershell
$env:CAREBRIDGE_PSQL_PATH = 'C:\Program Files\PostgreSQL\18\bin\psql.exe'
$env:CAREBRIDGE_CHECKLIST_REHEARSAL_DB_URL = 'postgresql://checklist_operations@127.0.0.1:5432/carebridge_rehearsal'
$env:PGPASSFILE = 'D:\secure\rehearsal.pgpass'
```

The URL must not contain a password. The script ignores `PGPASSWORD` while invoking `psql`.

The application-side readiness verifier independently defaults to zero unresolved records. If the reviewed dataset has an approved non-zero absolute baseline, supply it explicitly as Spring property `carebridge.checklist.migration.max-unresolved-quarantine-records` (environment form `CAREBRIDGE_CHECKLIST_MIGRATION_MAX_UNRESOLVED_QUARANTINE_RECORDS`). This value is not a waiver: it must equal the reviewed artifact baseline, and the external rate gate below still applies.

The script validates integrity and cross-run binding; it does not establish artifact authenticity by itself. Before the database operator starts, an independent release reviewer must record the sanitized backup's dataset fingerprint, expected Flyway history hash and quarantine baselines in an access-controlled immutable change record. The operator must copy those reviewed values into the commands and must not derive replacements from the database under test. A script `PASS` without this separation-of-duties evidence is not release approval.

An authenticated `OPERATIONS` principal can execute the same production verifier at `GET /api/v1/operations/checklist-migration/readiness`. A blocked report must prevent the separate rollout procedure from enabling a cohort; the endpoint itself never changes rollout state.

## 1. Apply and time the expand/pre-backfill chain

Start the full-migration timer immediately before Flyway runs on the freshly restored legacy clone. Apply the approved chain only through target `20260729060000`, whose final migration is `V20260729060000__add_unified_task_origin_target.sql`. This target includes the quarantine table and its operations RLS policy but stops immediately before `V20260729070000__backfill_legacy_checklist_v2.sql`.

Record the expansion command result and `expandMigrationSeconds`. Do not include backup restore, verifier queries, or operator think time. Abort if expansion or Flyway validation fails. The verifier does not perform this step.

## 2. Capture the pre-backfill manifest

Create a new evidence directory and capture the source hash after target `20260729060000` succeeds and immediately before the legacy backfill starts:

```powershell
$evidence = 'D:\tmp\checklist-rehearsal-20260730'
# Copied from the immutable change record prepared by the independent reviewer.
$datasetFingerprint = '<64-char-approved-dataset-sha256>'
& .\05_Development\Deployment\database\Invoke-ChecklistMigrationRehearsal.ps1 `
  -Phase CapturePre `
  -EvidenceDirectory $evidence `
  -ReferenceDatasetFingerprint $datasetFingerprint
```

Seal the resulting `*-pre-manifest.json` with the change record. It contains counts, SHA-256 values, Flyway version, and quarantine aggregates only; it never contains checklist titles or connection details.

`CapturePre` fails unless Flyway is exactly at `20260729060000`, has no failed history row, and quarantine visibility is attested through `checklist_operations` under forced RLS.

## 3. Run the remainder externally and collect metrics

The database/change operator, not this verifier, continues the historical staged fixture from `20260729060000` through `20260730050000` (the last pre-retirement target used by this verifier). Record `remainderMigrationSeconds`. Sample `pg_locks` from a separate observer connection and record the longest blocking interval. For the active canonical migration, measure a single Flyway run through `20260731060000` with the clean-chain gate instead of using this staged split.

Set `fullMigrationSeconds = expandMigrationSeconds + remainderMigrationSeconds`. The verifier permits only 0.01 seconds of rounding difference and excludes manifest capture time. This prevents a fast remainder-only measurement from being presented as full-chain evidence.

Copy `checklist-migration-rehearsal-metrics.template.json` to the evidence directory and replace every placeholder with observed values. `expandMigrationSucceeded`, `remainderMigrationSucceeded`, and `flywayValidationSuccessful` must reflect command results, not operator judgment. Keep raw sanitized Flyway output and lock samples beside the metrics JSON.

Before the rehearsal, approve the exact final Flyway version and the SHA-256 of the normalized `flyway_schema_history` rows `(installed_rank, version, description, type, script, checksum, success)` from a known-good clean chain. For the current reviewed chain the final version is `20260730050000`; if migrations change, create a new reviewed approval instead of reusing this value. The expected history hash is supplied separately and must not be copied from the database under test after migration.

## 4. Verify the post-migration gate

Use reviewed quarantine baselines from the approved fixture manifest. Defaults are zero and therefore fail closed:

```powershell
& .\05_Development\Deployment\database\Invoke-ChecklistMigrationRehearsal.ps1 `
  -Phase VerifyPost `
  -EvidenceDirectory $evidence `
  -PreManifestPath "$evidence\<run>-pre-manifest.json" `
  -MetricsPath "$evidence\migration-metrics.json" `
  -ReferenceDatasetFingerprint $datasetFingerprint `
  -ExpectedFinalFlywayVersion '20260730050000' `
  -ExpectedFlywayHistorySha256 '<64-char-approved-history-sha256>' `
  -MaxUnresolvedQuarantineCount 2 `
  -MaxLegacyQuarantineRatePercent 0.02
```

Exit code `0` and verdict `PASS` mean only that this post-migration run met the supplied, reviewable gates. Exit code `2` and verdict `ABORT` prohibit cohort enablement. Preserve the expanded schema; do not edit or roll back an applied Flyway migration.

## 5. Prove abort behavior

Before accepting a passing run, demonstrate fail-closed behavior without changing the database. Make a copy of the pre-manifest and replace its source SHA-256 with a clearly invalid 64-character test value, or use an intentionally stricter reviewed quarantine threshold. Run:

```powershell
& .\05_Development\Deployment\database\Invoke-ChecklistMigrationRehearsal.ps1 `
  -Phase ProveAbort `
  -EvidenceDirectory $evidence `
  -PreManifestPath "$evidence\intentional-mismatch-pre-manifest.json" `
  -MetricsPath "$evidence\migration-metrics.json" `
  -ReferenceDatasetFingerprint $datasetFingerprint `
  -ExpectedFinalFlywayVersion '20260730050000' `
  -ExpectedFlywayHistorySha256 '<64-char-approved-history-sha256>' `
  -MaxUnresolvedQuarantineCount 0 `
  -MaxLegacyQuarantineRatePercent 0
```

The proof succeeds only with exit code `0`, verdict `ABORT_PROVEN`, and at least one named failed check. Confirm separately that rollout flags/cohorts are still disabled. The verifier never changes them.

## 6. Correct and roll forward

Correct the cause using a new reviewed Flyway migration or an approved, audited data-correction procedure. Never modify an already applied migration. Restore a fresh clone from the same reference backup, time the expansion through `20260729060000`, recapture the pre-manifest, time the remainder through the corrected latest target, and collect a new metrics JSON whose full duration is the sum of both actual Flyway intervals.

After `ABORT_PROVEN`, the independent release reviewer—not the roll-forward database operator—hashes the abort artifact, stores that hash with the artifact in the immutable change record, and confirms that rollout remains disabled. The corrected-run operator receives the recorded hash as an approved input and must not recompute or replace it in the roll-forward session:

```powershell
$abortArtifact = "$evidence\<abort-run>-proveabort-verdict.json"
$abortArtifactSha256 = '<64-char-abort-sha256-from-immutable-change-record>'
```

```powershell
& .\05_Development\Deployment\database\Invoke-ChecklistMigrationRehearsal.ps1 `
  -Phase VerifyRollForward `
  -EvidenceDirectory $evidence `
  -PreManifestPath "$evidence\<new-run>-pre-manifest.json" `
  -MetricsPath "$evidence\roll-forward-metrics.json" `
  -PreviousAbortArtifact $abortArtifact `
  -PreviousAbortArtifactSha256 $abortArtifactSha256 `
  -ReferenceDatasetFingerprint $datasetFingerprint `
  -ExpectedFinalFlywayVersion '<corrected-approved-final-version>' `
  -ExpectedFlywayHistorySha256 '<64-char-corrected-approved-history-sha256>' `
  -MaxUnresolvedQuarantineCount 2 `
  -MaxLegacyQuarantineRatePercent 0.02
```

Only `ROLL_FORWARD_PASS` links the corrected run to prior abort evidence. It proves file integrity against the reviewer-supplied hash, not reviewer identity; authenticity remains the responsibility of the access-controlled change record and separation of duties. Recheck seeded `IN_PROGRESS` rows with the functional PostgreSQL migration suites before release.

## Artifact review template

The independent reviewer records:

```text
Reference backup / fixture fingerprint:
Application commit and Flyway checksums:
PostgreSQL / host profile:
Pre-manifest artifact + SHA-256:
Raw Flyway log artifact + SHA-256:
Lock-sample artifact + SHA-256:
Metrics artifact + SHA-256:
Post-verdict artifact + SHA-256:
Abort-proof artifact + SHA-256:
Roll-forward artifact + SHA-256:
Rollout flags/cohorts confirmed disabled by:
Reviewer / UTC timestamp:
Decision: PASS | ABORT
Open exceptions or approved waivers:
```

Do not record passwords, access tokens, full connection URLs, checklist titles, or source payloads in this template.
