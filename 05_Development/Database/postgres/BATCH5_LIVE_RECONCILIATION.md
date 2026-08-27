# Batch 5 Live Reconciliation Evidence

Date: 2026-07-22
Decision: `PASS_LIVE_UNCHANGED`

This audit was executed against the connected development database in a
`REPEATABLE READ, READ ONLY` transaction with statement and lock timeouts. The
transaction was explicitly rolled back. The application was not started and no
Flyway command, DDL, DML, or live migration was executed.

## Before/after evidence

| Evidence | Before | After |
| --- | --- | --- |
| Public base tables | 127 | 127 |
| Flyway history rows | 119 | 119 |
| Flyway fingerprint | `4383e274423b292c91212cf23b3ea43c` | unchanged |
| Schema fingerprint | `ca2a171dc1b6ba4b6e2e52b24f582251` | unchanged |
| `hospitals` rows | 20 | 20 |
| `hospitals` row fingerprint | `4590fa479ec424c37efc2eb95a72154b` | unchanged |
| `care_facilities` rows | 5 | 5 |
| `care_facilities` row fingerprint | `d5da9e4b008777c5ec37b81899ab30d8` | unchanged |

Rollback was confirmed. `expert_profiles.hospital_id` still had two populated
references and zero orphans. The only inbound hospital FK remained
`fk_expert_profile_hospital`. Facility partner and emergency-event FK definitions
were unchanged, with zero populated emergency facility references.

This evidence proves that Batch 5 development and verification did not mutate the
live database. It does not claim that the new migration has been applied there.
