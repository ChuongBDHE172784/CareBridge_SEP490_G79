---
title: Release 1 Database Gate 0 Audit
project: CareBridge_SEP490_G79
applies_to: Development, test, staging, and production-like PostgreSQL environments
created: 2026-07-21
---

# Release 1 Database Gate 0 Audit

## Purpose

Gate 0 produces repeatable evidence before any Release 1 database cleanup or
normalization is approved. It inventories repository migrations, proves whether a
clean PostgreSQL database can be built, and audits an existing environment without
mutating it.

Gate 0 is opt-in. A normal `mvn test` run does not execute these three test classes
and never requires external database credentials.

## Safety guarantees

- The external audit sets JDBC read-only mode, disables autocommit, executes
  `SET TRANSACTION ISOLATION LEVEL REPEATABLE READ, READ ONLY`, applies connection,
  socket, statement, and lock timeouts, and rolls back in `finally`.
- The audit never runs Flyway `migrate`, `clean`, or `repair` against the target.
- Manifests never contain the JDBC URL, username, or password. The endpoint is
  represented only by a SHA-256 hash.
- A non-zero removal-candidate count, retained inbound foreign key, or database
  object reference from any non-system schema is a gate failure. Catalog
  dependencies are primary evidence; function text is scanned conservatively for
  procedural/dynamic SQL references.
- Historical migration files and runtime Flyway/Hibernate settings are outside this
  automation's scope.

Do not use `flyway:repair` to hide a checksum or schema mismatch. Recover the exact
historical artifact and reconcile every environment first. `repair` changes history
metadata; it does not create missing schema objects.

## Prerequisites

- Java 21 and the Maven wrapper.
- Docker for the clean-bootstrap test.
- A database account that is read-only at the PostgreSQL authorization layer for
  the external audit. Transaction read-only mode is an additional safeguard, not a
  replacement for least privilege.
- One independent manifest for each environment. Never infer staging or production
  state from a development database.

## Environment variables

| Variable | Required | Meaning |
| --- | --- | --- |
| `GATE0_DB_URL` | External audit | PostgreSQL JDBC URL; never written to artifacts |
| `GATE0_DB_USERNAME` | External audit | Read-only database principal |
| `GATE0_DB_PASSWORD` | External audit | Password for the read-only principal |
| `GATE0_DB_SCHEMA` | No | Intended schema; defaults to `public` |
| `GATE0_FLYWAY_TABLE` | No | Flyway history table; defaults to `flyway_schema_history` |
| `GATE0_ENVIRONMENT` | No | Non-secret label used in the artifact filename |

Credentials embedded in JDBC URL query parameters are rejected. Supply them only
through `GATE0_DB_USERNAME` and `GATE0_DB_PASSWORD`.

On GitLab, define the three credential variables as masked and protected. Restrict
the job to protected refs and authorized operators according to the deployment
environment's access policy.

## Local commands

Run from `05_Development/CareBridgeAPI`.

PowerShell:

```powershell
.\mvnw.cmd -B "-Dtest=FlywayMigrationChainTest" "-Dgate0.enabled=true" test
.\mvnw.cmd -B "-Dtest=DatabaseGate0IntegrationTest" "-Dgate0.enabled=true" test
.\mvnw.cmd -B "-Dtest=DatabaseGate0ManifestTest" "-Dgate0.enabled=true" test
```

Bash:

```bash
./mvnw -B -Dtest=FlywayMigrationChainTest -Dgate0.enabled=true test
./mvnw -B -Dtest=DatabaseGate0IntegrationTest -Dgate0.enabled=true test
./mvnw -B -Dtest=DatabaseGate0ManifestTest -Dgate0.enabled=true test
```

Generated artifacts are written to `target/gate0/`:

- `repository-manifest.json`
- `clean-bootstrap-manifest.json`
- `external-<environment>-<endpoint-hash-prefix>.json`

The known repository collision at version `20260720100000` intentionally makes the
enabled repository gate fail with `REPOSITORY_DUPLICATE_VERSION` until environment
evidence determines the safe reconciliation. Both colliding paths remain in the
manifest.

## GitLab execution

The `database_gate0_audit` job is absent unless the ref is protected,
`GATE0_ENABLED=true`, and all three credential variables are present. When eligible,
it remains manual and non-blocking. Protect the `gate0-audit` environment in GitLab
and limit deployment access to authorized operators. Starting the job runs all three
gates and always retains JSON and Surefire artifacts, including on a controlled
failure.

Do not make this job mandatory until the migration-history matrix is approved and
the known repository/live drift is reconciled.

## Manifest comparison

For each environment, review and retain:

1. `flywayHistory`, ordered by installed rank, including version, script, checksum,
   timestamp, duration, and success.
2. Repository/live missing-script lists, duplicate history versions, and checksum
   mismatches.
3. `schemaFingerprintSha256`, computed from ordered tables, columns, types, defaults,
   nullability, constraints, and indexes.
4. All 22 candidate table presence flags and exact row counts.
5. Retained inbound foreign keys and cross-schema database object references.
6. `transactionReadOnly=true`, `rollbackConfirmed=true`, and an empty
   `gateFailures` list.

Timestamp and environment labels may differ. Endpoint hashes should differ when
environments use different endpoints. Schema fingerprints and Flyway histories must
match the approved environment matrix or have an explicit reconciliation decision.

## Stop conditions

Stop database rollout when any manifest contains a gate failure, including:

- malformed or duplicate repository migration versions;
- unsuccessful or duplicate live Flyway history rows;
- an applied script missing from the repository;
- a repository script missing from live history without an approved adoption path;
- a checksum mismatch;
- a non-zero candidate count;
- a retained inbound foreign key or database object reference;
- failure to establish a read-only transaction or confirm rollback;
- failure to bootstrap a clean database; or
- missing restore-rehearsal evidence for a later destructive wave.

Gate 0 evidence does not authorize a drop, migration, `repair`, or runtime
configuration change. Those actions require their separate approval checkpoints.
External ETL/BI consumers are not visible in PostgreSQL catalogs; attach their
owner-confirmed inventory separately before any destructive wave.
