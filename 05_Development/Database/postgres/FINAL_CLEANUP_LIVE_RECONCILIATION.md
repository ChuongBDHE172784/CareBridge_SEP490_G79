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
| Fingerprint before audit | `4e414b1bbc88a5fef9bbea73e83d7bcac2d2d6cf10a70cb0d23c216e462d5a7c` |
| Fingerprint after rollback | `4e414b1bbc88a5fef9bbea73e83d7bcac2d2d6cf10a70cb0d23c216e462d5a7c` |
| Unchanged | Yes |

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
