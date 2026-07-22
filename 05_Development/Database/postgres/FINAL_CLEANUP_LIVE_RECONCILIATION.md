# Final Cleanup Live Reconciliation

Audit date: 2026-07-22

Baseline commit: `2b33b582`

Scope: read-only verification only; no Flyway migration was applied to live.

## Safety controls

- JDBC connection marked read-only.
- Transaction used `REPEATABLE READ READ ONLY`.
- `statement_timeout=20s` and `lock_timeout=3s`.
- Transaction was explicitly rolled back.
- A second read-only connection reproduced the schema fingerprint.

## Live evidence before deployment

| Measure | Result |
| --- | ---: |
| Public base tables | 127 |
| Successful Flyway history rows | 119 |
| Full schema fingerprint before audit | `2f2b51f2d00c7c1ee31793477d5bd63bb1b94625df98678cbf598e8223cf2ade` |
| Full schema fingerprint after rollback | `2f2b51f2d00c7c1ee31793477d5bd63bb1b94625df98678cbf598e8223cf2ade` |
| Candidate count/presence fingerprint before audit | `c0a81838bb488126b1f17bff27824138a463cc24dc3b5c2b80cbd63fe76ad6e9` |
| Candidate count/presence fingerprint after rollback | `c0a81838bb488126b1f17bff27824138a463cc24dc3b5c2b80cbd63fe76ad6e9` |
| Unchanged | Yes |

The full schema fingerprint covers relations, ordered columns and defaults,
constraints, indexes, and RLS policies. The candidate fingerprint independently
covers presence and exact row counts for every approved, blocked, and retained
candidate in the audit manifest.

All 17 approved cleanup tables still contain zero rows:

`consultation_requests`, `consultation_messages`, `consultation_disputes`,
`payment_transactions`, `refund_records`, `commission_config`,
`commission_records`, `settlement_records`, `expert_reviews`,
`partner_expert_links`, `partner_services`, `sponsored_campaigns`,
`impact_assessment_ratings`, `contribution_attachments`,
`expert_identity_verifications`, `expert_verification_documents`, and
`medical_contributions`.

## Retained live tables

| Table | Rows | Decision |
| --- | ---: | --- |
| `consultation_bookings` | 1 | Retain |
| `consultation_sessions` | 1 | Retain |
| `consultation_price_bands` | 1 | Retain |
| `expert_consultation_prices` | 1 | Retain |
| `direct_conversations` | 5 | Retain |
| `direct_messages` | 32 | Retain |
| `conversation_calls` | 43 | Retain |
| `partner_organizations` | 1 | Retain, including `care_facilities.partner_id` FK |
| `health_summaries` | 0 | Retain for UC-87/88; booking dependency remains pending consent cutover |

## Counting rule

The reconciled count uses names from schema `public` where `pg_class.relkind`
is `r` (ordinary/base table) or `p` (partitioned table). It excludes views,
materialized views, foreign tables, sequences, indexes, and TOAST relations.
`flyway_schema_history` **is included exactly once** because it is an ordinary
table in `public`.

Under this rule, the clean-bootstrap result of 99 already contains
`flyway_schema_history`; its domain-table subtotal is 98. The value 100 came
from treating 99 as a domain-only subtotal and adding `flyway_schema_history`
again. It is not a valid count for any of the three reconciled name sets.

## Exact sources and set construction

- **Live current (A): 127.** Exact names are the `Live rows != absent` rows in
  `FINAL_CLEANUP_TABLE_INVENTORY.md`, captured by the read-only, repeatable-read
  live audit documented above. No live query or migration was rerun for this
  reconciliation.
- **Clean bootstrap HEAD (B): 99.** Testcontainers applied all 104 repository
  migrations to PostgreSQL 16 and counted 99 public base tables. The exact set
  is the 113 `Repo = Yes` names in the inventory, less the 14 Final Cleanup
  names that exist on clean bootstrap. The other three approved Final Cleanup
  names are known live-only absences.
- **Expected live after upgrade (C): 104.** This exact set is A less the 27
  tables dropped by Batch 1 through Final Cleanup, plus the four canonical
  tables created by Batch 4-5. Existing per-batch Testcontainers upgrade
  fixtures verify those transformations and their fail-closed behavior. There
  is no single checked-in fixture that materializes all 127 live tables, so C
  is a deterministic name-set projection rather than a newly executed full
  live clone.

## Exact batch drops

| Batch | Exact tables dropped | Delta |
| --- | --- | ---: |
| Batch 1 | `notifications` | -1 |
| Batch 2 | `roles`, `user_roles` | -2 |
| Batch 3 | `triage_answers`, `triage_assessments` | -2 |
| Batch 4 | `safety_alerts`, `emergency_events`, `safety_events`, `safety_monitoring_settings` | -4 drops + 3 creates = **-1 net** |
| Batch 5 | `hospitals` | -1 drop + 1 create = **0 net** |
| Final Cleanup | `commission_config`, `commission_records`, `consultation_disputes`, `consultation_messages`, `consultation_requests`, `expert_reviews`, `payment_transactions`, `refund_records`, `settlement_records`, `partner_expert_links`, `partner_services`, `sponsored_campaigns`, `contribution_attachments`, `expert_identity_verifications`, `expert_verification_documents`, `impact_assessment_ratings`, `medical_contributions` | **-17 live; -14 clean** |

Batch 4 creates `emergency_alert_attempts`, `emergency_alert_deliveries`, and
`safety_event_responses`. Batch 5 creates `care_facility_legacy_ids`. Therefore
Batch 1-5 changes live by -6 net: 127 -> 121. Final Cleanup then removes 17:
121 -> 104. Clean bootstrap starts from 113 after Batch 1-5 and loses only 14
present tables: 113 -> 99.

## Exact name-set differences

| Table | Live current | Clean bootstrap | Expected upgrade | Reason |
| --- | :---: | :---: | :---: | --- |
| `baby_journey_link_cleanup_summary` | Yes | No | Yes | Retained live-only forensic table |
| `commission_config` | Yes | No | No | Final Cleanup drop |
| `commission_records` | Yes | No | No | Final Cleanup drop |
| `consultation_disputes` | Yes | No | No | Final Cleanup drop |
| `consultation_messages` | Yes | No | No | Final Cleanup drop |
| `consultation_requests` | Yes | No | No | Final Cleanup drop |
| `contribution_attachments` | Yes | No | No | Final Cleanup drop; known clean-bootstrap absence |
| `districts` | Yes | No | Yes | Retained live-only reference table |
| `emergency_events` | Yes | No | No | Batch 4 drop |
| `expert_identity_verifications` | Yes | No | No | Final Cleanup drop; known clean-bootstrap absence |
| `expert_reviews` | Yes | No | No | Final Cleanup drop |
| `expert_verification_documents` | Yes | No | No | Final Cleanup drop |
| `hospitals` | Yes | No | No | Batch 5 drop |
| `impact_assessment_ratings` | Yes | No | No | Final Cleanup drop |
| `medical_contributions` | Yes | No | No | Final Cleanup drop; known clean-bootstrap absence |
| `notifications` | Yes | No | No | Batch 1 drop |
| `partner_expert_links` | Yes | No | No | Final Cleanup drop |
| `partner_services` | Yes | No | No | Final Cleanup drop |
| `payment_transactions` | Yes | No | No | Final Cleanup drop |
| `provinces` | Yes | No | Yes | Retained live-only reference table |
| `refund_records` | Yes | No | No | Final Cleanup drop |
| `roles` | Yes | No | No | Batch 2 drop |
| `safety_alerts` | Yes | No | No | Batch 4 drop |
| `safety_events` | Yes | No | No | Batch 4 drop |
| `safety_monitoring_settings` | Yes | No | No | Batch 4 drop |
| `settlement_records` | Yes | No | No | Final Cleanup drop |
| `specialties` | Yes | No | Yes | Retained live-only reference table |
| `sponsored_campaigns` | Yes | No | No | Final Cleanup drop |
| `triage_answers` | Yes | No | No | Batch 3 drop |
| `triage_assessments` | Yes | No | No | Batch 3 drop |
| `user_roles` | Yes | No | No | Batch 2 drop |
| `wards` | Yes | No | Yes | Retained live-only reference table |
| `care_facility_legacy_ids` | No | Yes | Yes | Batch 5 canonical table creation |
| `emergency_alert_attempts` | No | Yes | Yes | Batch 4 canonical table creation |
| `emergency_alert_deliveries` | No | Yes | Yes | Batch 4 canonical table creation |
| `safety_event_responses` | No | Yes | Yes | Batch 4 canonical table creation |

The four requested diffs are therefore:

- A minus B: 32 names (the first 32 rows above).
- B minus A: 4 names (the final four rows above).
- C minus B: `baby_journey_link_cleanup_summary`, `districts`, `provinces`,
  `specialties`, `wards`.
- B minus C: empty.

Deployment remains a separate controlled operation. This audit did not mutate
live data or schema.

## External-consumer sign-off

| Evidence source | Result | Owner/date |
| --- | --- | --- |
| Repository runtime/client scan | No consumer of the approved 17 remains | Cleanup scope approved by repository owner, 2026-07-22 |
| PostgreSQL ACL and column ACL catalog | No external grantee found | Read-only audit, 2026-07-22 |
| PostgreSQL publication/partition catalogs | No approved candidate membership found | Read-only audit, 2026-07-22 |
| Frozen cleanup specification | Exact 17-table allowlist approved; no live execution authorized | Repository owner, 2026-07-22 |

The migrations repeat these checks transactionally and also require the
migration role to own each candidate. Any out-of-catalog ETL or BI consumer
introduced after this sign-off must be declared during deployment review; the
deployment must stop rather than infer that service credentials are disposable.

## Sanitized audit transcript

The following output contains no connection string, username, credential, row
content, or personal data and is committed so the verification result is
independently retrievable:

```text
TABLE_COUNT|127
FLYWAY_ROWS|119
SCHEMA_FINGERPRINT|2f2b51f2d00c7c1ee31793477d5bd63bb1b94625df98678cbf598e8223cf2ade
CANDIDATE_FINGERPRINT|c0a81838bb488126b1f17bff27824138a463cc24dc3b5c2b80cbd63fe76ad6e9
APPROVED_ZERO_ROW_TABLES|17
BLOCKED_COUNTS|bookings=1|sessions=1|price_bands=1|expert_prices=1|conversations=5|messages=32|calls=43|partner_organizations=1
HEALTH_SUMMARIES|present=true|rows=0|retained=true
ROLLED_BACK|true
SCHEMA_FINGERPRINT_AFTER|2f2b51f2d00c7c1ee31793477d5bd63bb1b94625df98678cbf598e8223cf2ade
CANDIDATE_FINGERPRINT_AFTER|c0a81838bb488126b1f17bff27824138a463cc24dc3b5c2b80cbd63fe76ad6e9
UNCHANGED|true|true
CANDIDATES_UNCHANGED|true|true
```
