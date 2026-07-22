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

## Verified post-migration model

Testcontainers applied all 104 repository migrations to PostgreSQL 16 and
confirmed exactly 99 public base tables on a clean bootstrap. Upgrade fixtures
also confirmed that all cleanup waves are fail-closed for non-empty tables and
retained dependencies.

Expected deployed-live count after applying Batch 1 through Final Cleanup is
104 public base tables: 127 current tables minus 6 net tables from Batch 1-5
minus the 17 approved Final Cleanup tables. The five-table difference from the
99-table clean bootstrap is retained live-only reference/forensic schema:
`baby_journey_link_cleanup_summary`, `provinces`, `districts`, `wards`, and
`specialties`.

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
