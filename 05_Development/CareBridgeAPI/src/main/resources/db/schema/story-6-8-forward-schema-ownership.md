# Story 6.8 Forward Schema Ownership

This manifest is non-executable. It documents the schema ownership exception approved for
`UC-44 Share Summary with Expert` / Story 6.8.

- Executable owner: `db/migration/V20260723090000__create_consented_triage_expert_handoffs.sql`
- Owned tables: `consultation_context_shares`, `consultation_context_citations`
- Prerequisites created after V1: `intake_sessions`, `evidence_sources`, `consultation_requests`
- Applied migration rule: `db/migration/V1__init_schema.sql` and all other applied migrations
  remain byte-for-byte unchanged; Story 6.8 DDL must not be duplicated into them.
- Pre-Story V1 canonical digest: normalize CRLF or CR line endings to LF, encode UTF-8 without
  BOM, then SHA-256 must equal
  `EF0D1B28017BF32681924DED4AAF92D75427B5E5B8377B4A14F685A72CD62054`.
- Verification rule: fresh full-history migration, pre-Story-to-current Flyway validation/migrate,
  the pinned canonical V1 digest, and source scanning must prove one executable Story 6.8
  definition and no checksum drift.

Any future baseline squash must fold the prerequisites and both owned tables together in a
separately reviewed migration-baseline operation. This manifest does not authorize editing
Flyway history.
