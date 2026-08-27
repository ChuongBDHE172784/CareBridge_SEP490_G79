# CHK-041 Performance Harness

## Purpose

This is the opt-in, PostgreSQL-backed pre-release harness for the checklist distribution performance gates. It deliberately does not run in the ordinary PR suite.

On 2026-07-30 the owner approved a deterministic synthetic production-representative dataset and local disposable PostgreSQL 18 as the final CHK-041 reference environment. The approval replaces the unavailable sanitized external backup/host, not the gates: a fresh sealed run must still use the exact approved fixture/host profile and prove all thresholds, count/hash/quarantine controls, cohort-disabled abort, same-threshold forward-only correction, roll-forward, cleanup and independent review. Results must not be generalized beyond the recorded local host profile.

## Reference loads and gates

| Gate | Seed/load | Assertion |
|---|---:|---:|
| Unified Today | 500 active tasks, 20 care groups, 250 requests offered at 50 RPS | p95 <= 500 ms; p99 <= 1 s |
| Reconciliation coordinator | 10,000 resolved candidate outcomes persisted to PostgreSQL | elapsed <= 15 min |
| Legacy backfill and occurrence repair | 10,000 retained legacy checklist rows, measured separately through V70000 and V14000 | each >= 500 rows/s |
| Full migration | Initial schema through latest, including the 10,000-row backfill | <= 30 min |
| Flyway lock | Controlled one-second contention on `flyway_schema_history` before the latest migration | completion <= 5 s |

The executable tests are:

- `ChecklistPerformanceEmbeddedPostgresTest`
- `ChecklistMigrationPerformanceEmbeddedPostgresTest`

Run from `05_Development/CareBridgeAPI`:

```powershell
mvn -q "-Dmaven.compiler.testIncludes=**/AbstractEmbeddedPostgresIntegrationTest.java,**/ChecklistPerformanceEmbeddedPostgresTest.java,**/ChecklistMigrationPerformanceEmbeddedPostgresTest.java" "-Dtest=ChecklistPerformanceEmbeddedPostgresTest,ChecklistMigrationPerformanceEmbeddedPostgresTest" "-Dchecklist.performance.enabled=true" test
```

Each test prints `CHK-041 ...` measurement lines. Surefire XML/TXT reports are the durable automated evidence.

## Evidence boundary

Passing on embedded PostgreSQL 18.1 is local pre-release evidence, not final production-like certification. The Today test uses the actual Spring endpoint, authorization policy, JPA repositories, JSON serialization, and PostgreSQL data. The reconciliation test measures the real coordinator and candidate repositories but stubs candidate resolution/distribution so that it isolates the 10,000-candidate coordinator persistence load. The Flyway lock test is controlled contention evidence, not a substitute for observing blocking locks during the production-volume rehearsal.

The harness verifies measured thresholds only. It does not simulate a cohort flag or prove abort/roll-forward orchestration, migration-error rollback, count/hash mismatch handling, or quarantine-rate alerting; those remain open CHK-041 evidence gates. Release must remain blocked until those criteria and the same thresholds pass on the approved production-like reference environment with representative CPU, connection pool, network, and observability instrumentation. A threshold breach is not waived by this harness: abort cohort enablement, retain the expanded schema, and roll forward after correction.
